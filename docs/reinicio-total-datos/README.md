# Reinicio total de PostgreSQL y Storage

## Objetivo

Eliminar todos los datos de Kontora POS y comenzar nuevamente con:

- una base PostgreSQL creada desde cero por Flyway;
- el esquema de Supabase Storage creado desde cero;
- el bucket privado configurado en `SUPABASE_STORAGE_BUCKET`;
- cero objetos dentro del bucket;
- un único gerente inicial creado por el backend.

Este procedimiento elimina usuarios, ventas, cajas, inventario, auditoría,
metadatos de evidencias y archivos almacenados. No sirve para cambiar solamente
una contraseña ni para recuperar servicios detenidos.

## Aclaración sobre el usuario root inicial

Kontora no necesita ni debe versionar un archivo `root`, un SQL con
credenciales o una contraseña predeterminada. El equivalente funcional es el
**gerente inicial**, que el backend crea al arrancar contra una tabla
`usuarios` vacía.

La configuración vive únicamente en `infra/.env`, que está ignorado por Git:

```env
BOOTSTRAP_MANAGER_ENABLED=true
BOOTSTRAP_MANAGER_USERNAME=gerenteLocal
BOOTSTRAP_MANAGER_FULL_NAME=Gerente Local
BOOTSTRAP_MANAGER_PASSWORD=<CONTRASENA-TEMPORAL-SEGURA>
```

Si se requiere que el nombre de inicio de sesión sea literalmente `root`, se
puede usar:

```env
BOOTSTRAP_MANAGER_USERNAME=root
```

Ese usuario sigue siendo un usuario de la aplicación con rol `gerente`; no es
el usuario `root` de Linux ni un superusuario de PostgreSQL. La contraseña debe
tener entre 8 y 72 caracteres y nunca debe guardarse en este README, en Git ni
en un archivo SQL.

## Reglas obligatorias

1. Ejecutar una sola variante: **Windows local** o **VPS de producción**.
2. Ejecutar los comandos desde la raíz del repositorio y en el orden indicado.
3. Si un comando falla, no ejecutar el siguiente hasta resolver el error.
4. Confirmar los nombres reales de los dos volúmenes antes de eliminarlos.
5. Mantener juntos el respaldo PostgreSQL y el respaldo del volumen Storage.
6. En producción, no eliminar los volúmenes anteriores hasta completar todas
   las validaciones y probar el inicio de sesión.
7. No regenerar `STORAGE_JWT_SECRET` y `STORAGE_SERVICE_ROLE_KEY` por separado.

PostgreSQL contiene la metadata de Storage, pero los archivos viven en otro
volumen. Reiniciar solamente uno de los dos deja evidencias inconsistentes.

## Nombres predeterminados de los volúmenes

| Entorno | PostgreSQL | Archivos de Storage |
| --- | --- | --- |
| Windows local | `kontora_pos_postgres_local_data` | `kontora_pos_storage_local_data` |
| VPS producción | `kontora_pos_postgres_prod_data` | `kontora_pos_storage_prod_data` |

Antes de continuar, comprobar si `infra/.env` reemplaza esos nombres.

Windows:

```powershell
Select-String -Path infra\.env -Pattern '^(POSTGRES_VOLUME_NAME|STORAGE_VOLUME_NAME)='
```

VPS:

```bash
grep -E '^(POSTGRES_VOLUME_NAME|STORAGE_VOLUME_NAME)=' infra/.env
```

Todos los comandos de esta guía muestran los nombres predeterminados. Si el
resultado anterior es diferente, sustituirlos en **todos** los comandos de
inspección, respaldo y eliminación.

---

## Variante A: Windows local

Usar esta variante únicamente para el entorno definido por
`infra/compose.local.yml`.

### Fase A1. Comprobar el entorno antes del borrado

Abrir Docker Desktop, entrar al repositorio y validar Compose:

```powershell
cd C:\Users\corre\Documentos\kontora
docker info
docker compose --env-file infra\.env -f infra\compose.local.yml config --quiet
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel ps -a
docker volume inspect kontora_pos_postgres_local_data
docker volume inspect kontora_pos_storage_local_data
```

Preparar las imágenes antes de borrar los datos:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml pull postgres storage
docker compose --env-file infra\.env -f infra\compose.local.yml build backend frontend
```

Criterio de cierre: todos los comandos terminan con código `0`.

### Fase A2. Crear un respaldo de seguridad

Si se acepta perder los datos locales sin posibilidad de recuperación, esta
fase puede omitirse. En cualquier otro caso, crear una carpeta nueva fuera del
repositorio y reemplazar `AAAA-MM-DD_HHMM` por la fecha y hora actuales:

```powershell
New-Item -ItemType Directory -Path "$env:USERPROFILE\kontora-backups\pre-reset-AAAA-MM-DD_HHMM"
```

Detener el acceso a la aplicación y Storage para evitar escrituras durante el
respaldo:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel stop cloudflared frontend backend storage
```

Respaldar PostgreSQL:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom --file=/tmp/kontora_pos_before_reset.dump'
docker cp kontora_pos_postgres_local:/tmp/kontora_pos_before_reset.dump "$env:USERPROFILE\kontora-backups\pre-reset-AAAA-MM-DD_HHMM\kontora_pos.dump"
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres rm -f /tmp/kontora_pos_before_reset.dump
```

Detener PostgreSQL; después respaldar los archivos:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml stop postgres
docker run --rm --mount "type=volume,source=kontora_pos_storage_local_data,target=/data,readonly" --mount "type=bind,source=$env:USERPROFILE\kontora-backups\pre-reset-AAAA-MM-DD_HHMM,target=/backups" postgres:16-alpine tar -czf /backups/kontora_storage.tar.gz -C /data .
```

Comprobar ambos respaldos:

```powershell
docker run --rm --mount "type=bind,source=$env:USERPROFILE\kontora-backups\pre-reset-AAAA-MM-DD_HHMM,target=/backups,readonly" postgres:16-alpine sh -c 'pg_restore --list /backups/kontora_pos.dump >/dev/null'
docker run --rm --mount "type=bind,source=$env:USERPROFILE\kontora-backups\pre-reset-AAAA-MM-DD_HHMM,target=/backups,readonly" postgres:16-alpine sh -c 'tar -tzf /backups/kontora_storage.tar.gz >/dev/null'
Get-FileHash "$env:USERPROFILE\kontora-backups\pre-reset-AAAA-MM-DD_HHMM\kontora_pos.dump"
Get-FileHash "$env:USERPROFILE\kontora-backups\pre-reset-AAAA-MM-DD_HHMM\kontora_storage.tar.gz"
```

Criterio de cierre: `pg_restore --list` y `tar -tzf` terminan sin error, y
existen los dos archivos fuera del repositorio.

### Fase A3. Preparar el gerente inicial

Editar `infra/.env` y definir:

```env
BOOTSTRAP_MANAGER_ENABLED=true
BOOTSTRAP_MANAGER_USERNAME=gerenteLocal
BOOTSTRAP_MANAGER_FULL_NAME=Gerente Local
BOOTSTRAP_MANAGER_PASSWORD=<CONTRASENA-TEMPORAL-SEGURA>
```

No cambiar `DB_PASSWORD`, `STORAGE_DATABASE_URL`,
`STORAGE_JWT_SECRET` ni `STORAGE_SERVICE_ROLE_KEY` durante este reinicio.

Validar otra vez la configuración:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml config --quiet
```

No imprimir `docker compose config` sin `--quiet`, porque mostraría secretos.

### Fase A4. Eliminar exactamente los dos volúmenes

Detener y retirar los contenedores sin borrar todavía los volúmenes:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel down
```

Confirmar que los objetivos existen y no están en uso:

```powershell
docker volume inspect kontora_pos_postgres_local_data
docker volume inspect kontora_pos_storage_local_data
```

Revisar visualmente que ambos nombres coincidan con `infra/.env`. Solo entonces
ejecutar:

```powershell
docker volume rm kontora_pos_postgres_local_data
docker volume rm kontora_pos_storage_local_data
```

Confirmar que ya no aparecen:

```powershell
docker volume ls --filter name=kontora_pos_postgres_local_data
docker volume ls --filter name=kontora_pos_storage_local_data
```

No usar `docker volume prune`: podría eliminar volúmenes ajenos a Kontora.

### Fase A5. Crear nuevamente la base y el bucket

Compose volverá a crear los volúmenes. Iniciar cada componente por separado:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml up -d postgres
docker compose --env-file infra\.env -f infra\compose.local.yml ps postgres
```

`postgres` debe aparecer como `healthy`. Después:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml up -d storage
docker compose --env-file infra\.env -f infra\compose.local.yml up storage-bucket-init
docker compose --env-file infra\.env -f infra\compose.local.yml ps -a
```

`storage-db-init` y `storage-bucket-init` deben terminar con código `0`, y
`storage` debe aparecer como `healthy`.

Iniciar el backend para que Flyway cree el esquema de la aplicación y el
bootstrap cree el gerente:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --no-build backend frontend
docker compose --env-file infra\.env -f infra\compose.local.yml ps -a
```

Si se usa el túnel local, iniciarlo solamente después de validar la aplicación:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel up -d --no-deps cloudflared
```

Continuar en la sección [Validación común](#validación-común).

---

## Variante B: VPS de producción

Usar esta variante únicamente para `infra/compose.prod.yml`. El procedimiento
crea dos volúmenes nuevos, valida el sistema y elimina los anteriores al final.
Esto permite revertir antes de ejecutar el borrado definitivo.

### Fase B1. Comprobar el entorno y preparar imágenes

```bash
cd /opt/kontora
docker info
docker compose --env-file infra/.env -f infra/compose.prod.yml config --quiet
docker compose --env-file infra/.env -f infra/compose.prod.yml --profile tunnel ps -a
docker volume inspect kontora_pos_postgres_prod_data
docker volume inspect kontora_pos_storage_prod_data
docker compose --env-file infra/.env -f infra/compose.prod.yml pull postgres storage
docker compose --env-file infra/.env -f infra/compose.prod.yml build backend frontend
```

Criterio de cierre: todos los comandos terminan con código `0`. No borrar datos
si no están disponibles las imágenes necesarias para volver a iniciar.

### Fase B2. Crear y comprobar el respaldo obligatorio

Reemplazar `AAAA-MM-DD_HHMM` por la fecha y hora actuales:

```bash
sudo install -d -m 700 -o "$USER" -g "$USER" /var/backups/kontora/pre-reset-AAAA-MM-DD_HHMM
```

Detener el acceso a la aplicación y Storage:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml --profile tunnel stop cloudflared frontend backend storage
```

Respaldar PostgreSQL:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom --file=/tmp/kontora_pos_before_reset.dump'
docker cp kontora_pos_postgres:/tmp/kontora_pos_before_reset.dump /var/backups/kontora/pre-reset-AAAA-MM-DD_HHMM/kontora_pos.dump
docker compose --env-file infra/.env -f infra/compose.prod.yml exec -T postgres rm -f /tmp/kontora_pos_before_reset.dump
```

Detener PostgreSQL; después respaldar los archivos:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml stop postgres
docker run --rm \
  --mount type=volume,source=kontora_pos_storage_prod_data,target=/data,readonly \
  --mount type=bind,source=/var/backups/kontora/pre-reset-AAAA-MM-DD_HHMM,target=/backups \
  postgres:16-alpine \
  tar -czf /backups/kontora_storage.tar.gz -C /data .
```

Comprobar los respaldos:

```bash
docker run --rm \
  --mount type=bind,source=/var/backups/kontora/pre-reset-AAAA-MM-DD_HHMM,target=/backups,readonly \
  postgres:16-alpine \
  sh -c 'pg_restore --list /backups/kontora_pos.dump >/dev/null'

docker run --rm \
  --mount type=bind,source=/var/backups/kontora/pre-reset-AAAA-MM-DD_HHMM,target=/backups,readonly \
  postgres:16-alpine \
  sh -c 'tar -tzf /backups/kontora_storage.tar.gz >/dev/null'

sha256sum /var/backups/kontora/pre-reset-AAAA-MM-DD_HHMM/kontora_pos.dump
sha256sum /var/backups/kontora/pre-reset-AAAA-MM-DD_HHMM/kontora_storage.tar.gz
```

Copiar ambos archivos y sus hashes a almacenamiento externo antes de continuar.
Un respaldo guardado únicamente en el mismo VPS no protege ante una falla del
servidor.

### Fase B3. Detener el stack y registrar los volúmenes anteriores

```bash
cd /opt/kontora
grep -E '^(POSTGRES_VOLUME_NAME|STORAGE_VOLUME_NAME)=' infra/.env
docker compose --env-file infra/.env -f infra/compose.prod.yml --profile tunnel down
docker volume inspect kontora_pos_postgres_prod_data
docker volume inspect kontora_pos_storage_prod_data
```

Los dos últimos comandos deben mostrar exactamente los volúmenes que contienen
los datos que se van a retirar. Anotar esos nombres como
`VOLUMEN_POSTGRES_ANTERIOR` y `VOLUMEN_STORAGE_ANTERIOR`.

### Fase B4. Configurar volúmenes nuevos y el gerente inicial

Editar `infra/.env`:

```bash
nano infra/.env
```

Asignar nombres nuevos y únicos, usando la misma marca de fecha y hora en ambos:

```env
POSTGRES_VOLUME_NAME=kontora_pos_postgres_prod_reset_AAAAMMDDHHMM
STORAGE_VOLUME_NAME=kontora_pos_storage_prod_reset_AAAAMMDDHHMM

BOOTSTRAP_MANAGER_ENABLED=true
BOOTSTRAP_MANAGER_USERNAME=gerenteLocal
BOOTSTRAP_MANAGER_FULL_NAME=Gerente Local
BOOTSTRAP_MANAGER_PASSWORD=<CONTRASENA-TEMPORAL-SEGURA>
```

No cambiar `DB_PASSWORD`, `STORAGE_DATABASE_URL`,
`STORAGE_JWT_SECRET` ni `STORAGE_SERVICE_ROLE_KEY` durante este reinicio.

Validar sin imprimir secretos:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml config --quiet
grep -E '^(POSTGRES_VOLUME_NAME|STORAGE_VOLUME_NAME|BOOTSTRAP_MANAGER_ENABLED|BOOTSTRAP_MANAGER_USERNAME)=' infra/.env
```

Criterio de cierre: los dos nombres nuevos son distintos de los anteriores,
ambos tienen la misma marca temporal y el bootstrap está activado.

### Fase B5. Crear nuevamente la base y el bucket

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml up -d postgres
docker compose --env-file infra/.env -f infra/compose.prod.yml ps postgres
```

`postgres` debe aparecer como `healthy`. Después:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml up -d storage
docker compose --env-file infra/.env -f infra/compose.prod.yml up storage-bucket-init
docker compose --env-file infra/.env -f infra/compose.prod.yml ps -a
```

`storage-db-init` y `storage-bucket-init` deben terminar con código `0`, y
`storage` debe aparecer como `healthy`.

Iniciar backend y frontend con las imágenes ya preparadas:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml up -d --no-build backend frontend
docker compose --env-file infra/.env -f infra/compose.prod.yml ps -a
```

No eliminar todavía los volúmenes anteriores. Continuar con la validación.

---

## Validación común

Usar el archivo Compose correspondiente al entorno. Los ejemplos siguientes
incluyen primero Windows y después VPS.

### 1. Confirmar las migraciones Flyway

Windows:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"'
```

VPS:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"'
```

Todas las filas deben mostrar `success = t`.

### 2. Confirmar el bucket nuevo y vacío

Windows:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT id, name, public, file_size_limit, allowed_mime_types FROM storage.buckets;"'
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT count(*) AS objetos_almacenados FROM storage.objects;"'
```

VPS:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT id, name, public, file_size_limit, allowed_mime_types FROM storage.buckets;"'
docker compose --env-file infra/.env -f infra/compose.prod.yml exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT count(*) AS objetos_almacenados FROM storage.objects;"'
```

Resultado esperado:

- existe un solo bucket con el nombre de `SUPABASE_STORAGE_BUCKET`;
- `public` es `false`;
- `objetos_almacenados` es `0`.

### 3. Confirmar el gerente inicial

Windows:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT u.nombre_usuario, u.estado AS estado_usuario, r.nombre_rol, c.estado AS estado_credencial FROM usuarios u JOIN roles r ON r.id_rol = u.id_rol JOIN credenciales_usuario c ON c.id_usuario = u.id_usuario;"'
```

VPS:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT u.nombre_usuario, u.estado AS estado_usuario, r.nombre_rol, c.estado AS estado_credencial FROM usuarios u JOIN roles r ON r.id_rol = u.id_rol JOIN credenciales_usuario c ON c.id_usuario = u.id_usuario;"'
```

Debe existir exactamente el usuario configurado, con rol `gerente`, usuario
`activo` y credencial `activa`.

### 4. Comprobar salud e inicio de sesión

Windows:

```powershell
curl.exe --fail http://127.0.0.1:8080/api/health
curl.exe --fail http://127.0.0.1:8081/healthz
curl.exe --fail http://127.0.0.1:8081/api/health
```

VPS:

```bash
curl --fail http://127.0.0.1:8080/api/health
curl --fail http://127.0.0.1:8081/healthz
curl --fail http://127.0.0.1:8081/api/health
```

Abrir `/login` e iniciar sesión con el gerente configurado. No continuar hasta
confirmar el acceso.

### 5. Desactivar el bootstrap

Después del primer inicio de sesión, editar `infra/.env`:

```env
BOOTSTRAP_MANAGER_ENABLED=false
BOOTSTRAP_MANAGER_PASSWORD=
```

Recrear solamente el backend.

Windows:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --no-deps --no-build --force-recreate backend
curl.exe --fail http://127.0.0.1:8080/api/health
```

VPS:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml up -d --no-deps --no-build --force-recreate backend
curl --fail http://127.0.0.1:8080/api/health
```

Volver a iniciar sesión. La cuenta debe conservarse aunque el bootstrap ya esté
desactivado.

### 6. Restablecer Cloudflare Tunnel en producción

Solo después de validar el acceso local del VPS:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml --profile tunnel up -d --no-deps cloudflared
docker compose --env-file infra/.env -f infra/compose.prod.yml --profile tunnel ps -a
curl --fail https://kontora-pos.store/healthz
curl --fail https://kontora-pos.store/api/health
```

No mantener al mismo tiempo otra instalación conectada al mismo túnel y a una
base distinta.

### 7. Eliminar los volúmenes anteriores del VPS

Esta es la eliminación irreversible final. Ejecutarla únicamente si:

- Flyway terminó correctamente;
- el bucket existe y contiene cero objetos;
- el gerente inicial puede iniciar sesión;
- el bootstrap quedó desactivado;
- los respaldos están comprobados y copiados fuera del VPS;
- los nombres nuevos de `infra/.env` son distintos de los anteriores.

Listar los volúmenes:

```bash
docker volume ls
```

Inspeccionar nuevamente los dos nombres anteriores anotados en la Fase B3:

```bash
docker volume inspect VOLUMEN_POSTGRES_ANTERIOR
docker volume inspect VOLUMEN_STORAGE_ANTERIOR
```

Confirmar que `infra/.env` ya no referencia ninguno de esos nombres. Después,
reemplazar los marcadores y eliminar únicamente esos dos volúmenes:

```bash
docker volume rm VOLUMEN_POSTGRES_ANTERIOR
docker volume rm VOLUMEN_STORAGE_ANTERIOR
```

No ejecutar `docker volume prune`.

## Criterios de cierre

El reinicio se considera completo solamente cuando:

- PostgreSQL y Storage aparecen como `healthy`;
- `storage-db-init` y `storage-bucket-init` terminaron con código `0`;
- todas las migraciones Flyway tienen `success = t`;
- existe el bucket privado configurado y `storage.objects` contiene `0` filas;
- existe exactamente un gerente inicial activo;
- el inicio de sesión funciona;
- `BOOTSTRAP_MANAGER_ENABLED=false`;
- `BOOTSTRAP_MANAGER_PASSWORD` quedó vacío;
- en producción, el túnel responde y los volúmenes anteriores fueron eliminados
  después de guardar los respaldos externos.

## Errores frecuentes

| Error | Acción |
| --- | --- |
| `volume is in use` | Ejecutar `docker compose ... down`, comprobar `docker ps -a --filter volume=NOMBRE` y no forzar la eliminación hasta identificar el contenedor. |
| Flyway falla | Revisar `docker compose ... logs --tail=200 backend`; no editar una migración ya versionada. |
| Storage no queda `healthy` | Revisar `storage-db-init`, `STORAGE_DATABASE_URL` y que las dos claves Storage pertenezcan a la misma pareja. |
| El bucket no existe | Ejecutar nuevamente `up storage-bucket-init` y exigir salida con código `0`. |
| Hay objetos en `storage.objects` | Se está usando una base anterior o algún cliente escribió durante el reinicio; detenerse y comprobar los nombres de los volúmenes. |
| No se crea el gerente | Confirmar que `usuarios` está vacía, el bootstrap está activo y las cuatro variables `BOOTSTRAP_MANAGER_*` son válidas. |
| El gerente ya existe pero la contraseña no funciona | Las variables bootstrap no actualizan usuarios existentes; usar la gestión de usuarios o repetir el reinicio con la base realmente vacía. |
