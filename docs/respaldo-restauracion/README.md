# Respaldo y restauración local de Kontora POS

## Objetivo

Crear y restaurar una copia coherente de:

1. PostgreSQL, incluidos los esquemas `public` y `storage`;
2. los archivos físicos del volumen de Supabase Storage.

Los dos archivos forman una pareja inseparable:

```text
kontora_pos.dump
kontora_storage.tar.gz
```

No se debe restaurar únicamente la base si existen evidencias, porque la
metadata de `storage.objects` podría apuntar a archivos que no están en el
volumen. Tampoco se debe restaurar únicamente el volumen.

Esta guía corresponde al entorno local de Windows:

```text
C:\Users\corre\Documentos\kontora
infra\compose.local.yml
```

Los comandos usan los nombres locales predeterminados:

| Recurso | Nombre |
| --- | --- |
| Base | `kontora_pos` |
| Usuario PostgreSQL | `kontora_pos` |
| Volumen PostgreSQL | `kontora_pos_postgres_local_data` |
| Volumen Storage | `kontora_pos_storage_local_data` |
| Volumen de respaldos | `kontora_ops_backups_local_data` |
| Bucket | `kontoraimagenes` |

Antes de ejecutar, confirmar esos valores en `infra/.env` e
`infra/ops/.env`. Si alguno es diferente, sustituirlo de forma consistente en
todos los comandos.

> La creación del respaldo no elimina información. La restauración sí reemplaza
> por completo la base y el volumen Storage actuales.

## Reglas

1. Ejecutar un comando a la vez.
2. Detenerse ante el primer error.
3. No usar `docker compose down -v`.
4. No usar `docker volume prune`.
5. No compartir tokens, contraseñas ni archivos `.env`.
6. Conservar el paquete del respaldo fuera de los volúmenes que se reemplazan.
7. Mantener `BOOTSTRAP_MANAGER_ENABLED=false` durante una restauración.
8. No iniciar el backend hasta que PostgreSQL y Storage hayan sido restaurados.

---

# Parte A. Crear el respaldo

## Paso 1. Comprobar el POS

Entrar al proyecto:

```powershell
cd C:\Users\corre\Documentos\kontora
```

Validar Docker y Compose:

```powershell
docker info
```

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml config --quiet
```

Confirmar los nombres que se respaldarán:

```powershell
Select-String -Path infra\.env -Pattern '^(POSTGRES_VOLUME_NAME|STORAGE_VOLUME_NAME|DB_NAME|DB_USER|SUPABASE_STORAGE_BUCKET)='
```

```powershell
Select-String -Path infra\ops\.env -Pattern '^(OPS_BACKUP_VOLUME_NAME|OPS_POSTGRES_VOLUME|OPS_STORAGE_VOLUME|OPS_STORAGE_BUCKET)='
```

Comprobar los servicios:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel ps -a
```

Se requiere:

- PostgreSQL `healthy`;
- Storage `healthy`;
- backend `Up`;
- frontend `healthy`.

No continuar si un servicio requerido está detenido o reiniciándose.

## Paso 2. Comprobar el panel de operaciones

La configuración inicial del panel está en
[infra/ops/README.md](../../infra/ops/README.md). Si ya fue configurado, validar:

```powershell
docker compose --env-file infra\ops\.env -f infra\ops\compose.local.yml config --quiet
```

Iniciar o actualizar el panel:

```powershell
docker compose --env-file infra\ops\.env -f infra\ops\compose.local.yml up -d --build
```

Comprobar:

```powershell
docker compose --env-file infra\ops\.env -f infra\ops\compose.local.yml ps -a
```

```powershell
curl.exe --fail http://127.0.0.1:8090/healthz
```

No imprimir `OPS_LOCAL_TOKEN` ni `OPS_EXECUTOR_TOKEN` en la terminal o el chat.

## Paso 3. Crear el paquete

Abrir:

```text
http://127.0.0.1:8090
```

En **Respaldos**:

1. seleccionar **Crear respaldo**;
2. escribir exactamente `RESPALDAR`;
3. confirmar una sola vez;
4. esperar a que el trabajo termine.

Durante unos segundos el ejecutor detiene los escritores. Después restablece
únicamente los servicios que estaban activos.

El estado requerido es:

```text
success
```

El paquete debe mostrar:

```text
kontora_pos.dump
kontora_storage.tar.gz
manifest.json
manifest.sha256
job.json
```

Validar el listado por la API interna:

```powershell
docker compose --env-file infra\ops\.env -f infra\ops\compose.local.yml exec -T ops-panel node -e "fetch('http://127.0.0.1:8090/api/v1/backups',{headers:{Authorization:'Bearer '+process.env.OPS_LOCAL_TOKEN}}).then(async r=>{console.log(r.status);console.log(JSON.stringify(await r.json(),null,2));process.exit(r.ok?0:1)})"
```

Debe mostrar:

- HTTP `200`;
- `activeJob: null`;
- el respaldo con `state: success`;
- dos archivos con tamaño y SHA-256.

El estado `restoreVerification: pending` es normal: la creación no ejecuta una
restauración automática.

## Paso 4. Copiar el paquete fuera del volumen

Copiar de la interfaz el UUID del respaldo y asignarlo:

```powershell
$BackupId = "REEMPLAZAR-POR-EL-UUID-DEL-RESPALDO"
```

Crear una carpeta local ignorada por Git:

```powershell
$RestoreDir = (New-Item -ItemType Directory -Force -Path ".\backups\restore\$BackupId").FullName
```

Copiar el paquete:

```powershell
docker run --rm --mount type=volume,src=kontora_ops_backups_local_data,dst=/source,readonly --mount "type=bind,src=$RestoreDir,dst=/destination" alpine:3.20 sh -c "cp -a /source/$BackupId/. /destination/"
```

Comprobar los cinco archivos:

```powershell
Get-ChildItem -LiteralPath $RestoreDir
```

## Paso 5. Validar hashes

Leer el hash esperado del manifiesto:

```powershell
Get-Content -LiteralPath "$RestoreDir\manifest.sha256"
```

Calcular el hash real:

```powershell
Get-FileHash -Algorithm SHA256 -LiteralPath "$RestoreDir\manifest.json"
```

Los dos valores deben coincidir.

Leer los hashes esperados de los archivos:

```powershell
$Manifest = Get-Content -Raw -LiteralPath "$RestoreDir\manifest.json" | ConvertFrom-Json
```

```powershell
$Manifest.files | Format-Table name, bytes, sha256
```

Calcular los hashes reales:

```powershell
Get-FileHash -Algorithm SHA256 -LiteralPath "$RestoreDir\kontora_pos.dump"
```

```powershell
Get-FileHash -Algorithm SHA256 -LiteralPath "$RestoreDir\kontora_storage.tar.gz"
```

Los hashes reales deben coincidir con los de `manifest.json`. No restaurar si
alguno es diferente.

### Criterio de cierre del respaldo

- [ ] El trabajo terminó en `success`.
- [ ] Existen los cinco archivos.
- [ ] Coincide el hash de `manifest.json`.
- [ ] Coinciden los hashes del dump y del archivo Storage.
- [ ] PostgreSQL, Storage, backend y frontend volvieron a estar operativos.

---

# Parte B. Restaurar el respaldo

## Paso 1. Seleccionar y validar

Entrar al proyecto:

```powershell
cd C:\Users\corre\Documentos\kontora
```

Asignar el mismo identificador usado al copiar el respaldo:

```powershell
$BackupId = "REEMPLAZAR-POR-EL-UUID-DEL-RESPALDO"
```

Resolver la carpeta:

```powershell
$RestoreDir = (Resolve-Path ".\backups\restore\$BackupId").Path
```

Comprobarla:

```powershell
Get-ChildItem -LiteralPath $RestoreDir
```

Repetir la validación de hashes de la Parte A, Paso 5. No continuar si no
coinciden.

Leer del manifiesto los conteos que se verificarán después:

```powershell
$Manifest = Get-Content -Raw -LiteralPath "$RestoreDir\manifest.json" | ConvertFrom-Json
```

```powershell
$Manifest.bucket | Format-List id, public, objectCount, physicalFiles, physicalBytes, knownObject
```

```powershell
$Manifest.evidence | Format-List referenceTotal
```

Comprobar que el bootstrap permanezca desactivado:

```powershell
Select-String -Path infra\.env -Pattern '^(BOOTSTRAP_MANAGER_ENABLED=false|BOOTSTRAP_MANAGER_PASSWORD=)$'
```

Validar Compose:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml config --quiet
```

## Paso 2. Detener el sistema

> Desde este paso la operación es destructiva para los datos actuales.

Detener los contenedores sin eliminar automáticamente los volúmenes:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel down
```

Confirmar que ningún contenedor usa los volúmenes:

```powershell
docker ps -a --filter volume=kontora_pos_postgres_local_data
```

```powershell
docker ps -a --filter volume=kontora_pos_storage_local_data
```

Ambos comandos deben mostrar solamente el encabezado.

Inspeccionar los objetivos:

```powershell
docker volume inspect kontora_pos_postgres_local_data
```

```powershell
docker volume inspect kontora_pos_storage_local_data
```

## Paso 3. Recrear los volúmenes vacíos

Eliminar solamente los dos volúmenes confirmados:

```powershell
docker volume rm kontora_pos_postgres_local_data
```

```powershell
docker volume rm kontora_pos_storage_local_data
```

Crear los volúmenes vacíos:

```powershell
docker volume create kontora_pos_postgres_local_data
```

```powershell
docker volume create kontora_pos_storage_local_data
```

No eliminar `kontora_ops_backups_local_data`.

## Paso 4. Restaurar los archivos de Storage

Extraer el archivo con atributos extendidos:

```powershell
docker run --rm --mount type=volume,src=kontora_pos_storage_local_data,dst=/restore --mount "type=bind,src=$RestoreDir,dst=/backup,readonly" debian:bookworm-slim tar --extract --gzip --file=/backup/kontora_storage.tar.gz --xattrs --xattrs-include=user.supabase.* --numeric-owner --same-owner --directory=/restore
```

El comando debe terminar sin errores. No usar `postgres:16-alpine` para esta
extracción: su variante de `tar` no garantiza los atributos
`user.supabase.*` que Storage necesita.

## Paso 5. Restaurar PostgreSQL

Iniciar solamente PostgreSQL:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml up -d postgres
```

Repetir hasta que esté `healthy`:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml ps postgres
```

Copiar el dump al contenedor:

```powershell
docker cp "$RestoreDir\kontora_pos.dump" kontora_pos_postgres_local:/tmp/kontora_pos.dump
```

Restaurar:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres pg_restore --exit-on-error --clean --if-exists --no-owner --no-privileges -U kontora_pos -d kontora_pos /tmp/kontora_pos.dump
```

Eliminar la copia temporal:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres rm -f /tmp/kontora_pos.dump
```

Comprobar el historial restaurado:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres psql -X -U kontora_pos -d kontora_pos -c "SELECT installed_rank, version, description, success FROM public.flyway_schema_history ORDER BY installed_rank;"
```

Todas las filas existentes deben mostrar `success = t`.

## Paso 6. Preparar Storage y el bucket

Ejecutar el inicializador de roles una sola vez:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml run --rm --no-deps storage-db-init
```

Iniciar Storage sin relanzar dependencias:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --no-deps --wait --wait-timeout 120 storage
```

Preparar y validar el bucket existente:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml run --rm --no-deps storage-bucket-init
```

Salida esperada:

```text
Bucket privado 'kontoraimagenes' preparado correctamente.
```

## Paso 7. Iniciar la aplicación

Iniciar backend:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --no-deps --no-build backend
```

Esperar el arranque y comprobar:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml logs --no-color --tail=100 backend
```

```powershell
curl.exe --fail http://127.0.0.1:8080/api/health
```

Iniciar frontend:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --no-deps --no-build frontend
```

Comprobar:

```powershell
curl.exe --fail http://127.0.0.1:8081/healthz
```

```powershell
curl.exe --fail http://127.0.0.1:8081/api/health
```

## Paso 8. Validar datos y evidencias

Comprobar el bucket:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres psql -X --csv -U kontora_pos -d kontora_pos -c "SELECT id, name, public, file_size_limit, allowed_mime_types FROM storage.buckets;"
```

Contar objetos y referencias:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres psql -X -U kontora_pos -d kontora_pos -c "SELECT (SELECT count(*) FROM storage.objects) AS objetos_storage, (SELECT count(*) FROM archivos_evidencia) AS evidencias_registradas, (SELECT count(*) FROM usuarios) AS usuarios, (SELECT count(*) FROM ventas) AS ventas;"
```

Comparar:

- `objetos_storage` con `$Manifest.bucket.objectCount`;
- `evidencias_registradas` con `$Manifest.evidence.referenceTotal`.

Los conteos de usuarios y ventas son una comprobación funcional adicional. La
versión actual del manifiesto no guarda esos dos totales, por lo que deben
revisarse contra un valor conocido del respaldo y no declararse validados por
el manifiesto.

Abrir:

```text
http://127.0.0.1:8081/login
```

Validar:

1. inicio de sesión con un usuario restaurado;
2. consultas de ventas e inventario;
3. si el respaldo contenía evidencias, vista previa y descarga de una evidencia
   conocida;
4. cortesías y pérdidas históricas cuando existían en el respaldo.

Una restauración con evidencias no se considera cerrada hasta descargar
correctamente al menos un archivo conocido.

## Paso 9. Restablecer el túnel

Solo después de validar localmente:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel up -d --no-deps cloudflared
```

```powershell
curl.exe --fail https://kontora-pos.store/healthz
```

```powershell
curl.exe --fail https://kontora-pos.store/api/health
```

### Criterio de cierre de la restauración

- [ ] Los hashes del paquete coincidieron antes de restaurar.
- [ ] PostgreSQL está `healthy`.
- [ ] Storage está `healthy`.
- [ ] Flyway no contiene migraciones fallidas.
- [ ] El bucket `kontoraimagenes` es privado.
- [ ] Objetos de Storage y referencias de evidencia coinciden con el manifiesto.
- [ ] Un usuario restaurado puede iniciar sesión.
- [ ] Una evidencia conocida se puede abrir y descargar.
- [ ] Backend y frontend responden localmente.
- [ ] El túnel público responde después de la validación local.

## Errores frecuentes

| Situación | Acción |
| --- | --- |
| Un hash no coincide | No restaurar. Volver a copiar el paquete desde el volumen de respaldos. |
| `volume is in use` | No forzar. Repetir `down` y revisar `docker ps -a --filter volume=NOMBRE`. |
| `pg_restore` falla | Detenerse. No iniciar backend; conservar el paquete y revisar el primer error. |
| Storage no inicia | Revisar `storage-db-init`, la URL de base y los logs de Storage. |
| El bucket no existe | Ejecutar únicamente `run --rm --no-deps storage-bucket-init`. |
| La metadata existe pero una evidencia no descarga | Confirmar que el tar se extrajo con `--xattrs-include=user.supabase.*` y validar otra vez el hash del archivo Storage. |
| Flyway quiere aplicar migraciones nuevas | Confirmar que el código actual es la versión esperada. Flyway puede aplicar migraciones posteriores a un respaldo antiguo, pero nunca se deben editar migraciones ya aplicadas. |

## Validación de recuperación

La creación de un paquete con estado `success` confirma la integridad local de
sus archivos y hashes, pero no demuestra por sí sola que el sistema pueda
recuperarse. La estrategia se considera validada únicamente después de ejecutar
la Parte B completa sobre volúmenes de ensayo y comprobar inicio de sesión,
datos y al menos una evidencia conocida.

No realizar el primer ensayo directamente sobre el único entorno que contiene
la información operativa.
