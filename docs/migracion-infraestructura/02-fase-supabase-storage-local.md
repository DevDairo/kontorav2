# Fase 2: Supabase Storage autoalojado

## Resultado esperado

La instalacion ejecuta tres servicios permanentes:

- `postgres`: datos relacionales y metadata del esquema `storage`.
- `storage`: API REST `supabase/storage-api:v1.60.4` y volumen de objetos.
- `backend`: unico consumidor autorizado del bucket privado.

Compose tambien ejecuta dos inicializadores transitorios:

- `storage-db-init`: prepara de forma idempotente los roles PostgreSQL usados
  por RLS y termina.
- `storage-bucket-init`: crea o actualiza `kontoraimagenes` como bucket privado,
  limita cada objeto a 13 MiB, admite `image/*` y `application/pdf`, y termina.

Estos inicializadores no quedan consumiendo CPU ni memoria. En produccion
Storage no publica ningun puerto en el host ni se expone mediante Cloudflare
Tunnel.

## Por que esta es la variante minima

La version fijada de Storage puede operar en modo de un solo tenant conectada
directamente a PostgreSQL y usando el backend de archivos local. Kontora no
necesita las siguientes piezas del despliegue completo:

- Kong: el backend usa directamente `http://storage:5000`.
- PostgREST: la version fijada administra metadata mediante su conexion
  `DATABASE_URL`.
- Studio, Auth, Realtime y Analytics: no intervienen en el flujo de evidencias.
- imgproxy: Spring Boot ya comprime las imagenes y no solicita transformaciones.
- MinIO o S3: los objetos se guardan en el volumen
  `kontora_pos_storage_*_data`.

Se desactivan transformaciones, protocolo S3, colas, rate limiter y telemetria
OTEL. La configuracion se basa en la
[guia oficial de autoalojamiento](https://supabase.com/docs/guides/self-hosting/docker),
la [configuracion oficial de Storage](https://supabase.com/docs/guides/self-hosting/storage/config)
y el [repositorio oficial de Storage](https://github.com/supabase/storage).

## Paso 1: generar claves exclusivas

No reutilizar `JWT_SECRET` de las sesiones de Kontora. En Windows, desde la
raiz del proyecto:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\infra\scripts\New-StorageSecrets.ps1
```

Copiar las dos lineas generadas en `infra/.env`:

```env
STORAGE_JWT_SECRET=<secreto-generado>
STORAGE_SERVICE_ROLE_KEY=<jwt-generado>
```

El uso puntual de `-ExecutionPolicy Bypass` evita modificar la politica global
del equipo. Generar valores diferentes para desarrollo y produccion. La clave
de servicio nunca se coloca en el frontend.

## Paso 2: configurar almacenamiento y base

Desarrollo local usa:

```env
STORAGE_BIND_ADDRESS=127.0.0.1
STORAGE_PORT=5000
STORAGE_VOLUME_NAME=kontora_pos_storage_local_data
STORAGE_DATABASE_URL=postgresql://kontora_pos:kontora_pos_local_password@postgres:5432/kontora_pos
STORAGE_DATABASE_MAX_CONNECTIONS=5
STORAGE_NAMESPACE=kontora-storage
STORAGE_REGION=local
STORAGE_FILE_SIZE_LIMIT=13631488
STORAGE_LOG_LEVEL=info
SUPABASE_STORAGE_BUCKET=kontoraimagenes
```

En el VPS no se configuran `STORAGE_BIND_ADDRESS` ni `STORAGE_PORT`, porque el
Compose de produccion no publica Storage. Completar:

```env
STORAGE_VOLUME_NAME=kontora_pos_storage_prod_data
STORAGE_DATABASE_URL=postgresql://kontora_pos:<DB_PASSWORD_URL_ENCODED>@postgres:5432/kontora_pos
STORAGE_DATABASE_MAX_CONNECTIONS=5
STORAGE_NAMESPACE=kontora-storage
STORAGE_REGION=local
STORAGE_FILE_SIZE_LIMIT=13631488
STORAGE_LOG_LEVEL=warn
SUPABASE_STORAGE_BUCKET=kontoraimagenes
```

`STORAGE_DATABASE_URL` debe apuntar a la misma base definida por `DB_*`. Si
`DB_PASSWORD` contiene caracteres reservados como `@`, `:`, `/`, `?` o `#`,
codificar la contrasena dentro de la URL. La opcion mas simple es generar la
contrasena con el alfabeto base64url.

`STORAGE_NAMESPACE` evita que el adaptador de archivos use una carpeta
ambigua. Debe conservarse sin cambios durante toda la vida del volumen.

## Paso 3: validar Compose sin crear contenedores

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml config --quiet
docker compose --env-file infra\.env -f infra\compose.local.yml config --services
```

La segunda orden debe listar:

```text
postgres
storage-db-init
storage
storage-bucket-init
backend
```

No compartir la salida completa de `config`: contiene las claves expandidas.

## Paso 4: descargar e iniciar Storage

No se instala Supabase, Node.js ni PostgreSQL en Windows. Docker descarga y
ejecuta las imagenes dentro de Docker Desktop.

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml pull storage storage-bucket-init
docker compose --env-file infra\.env -f infra\compose.local.yml up -d storage
docker compose --env-file infra\.env -f infra\compose.local.yml ps -a
docker compose --env-file infra\.env -f infra\compose.local.yml logs --tail=200 storage-db-init storage
```

El resultado correcto es:

- `storage-db-init` termina con codigo `0`.
- `storage` queda `Up` y `healthy`.
- El puerto local aparece solo como `127.0.0.1:5000->5000/tcp`.

Comprobar el health:

```powershell
$storageKey = (Get-Content infra\.env |
    Where-Object { $_ -like 'STORAGE_SERVICE_ROLE_KEY=*' } |
    Select-Object -First 1).Split('=', 2)[1]
$healthHeaders = @{ Authorization = "Bearer $storageKey"; apikey = $storageKey }

Invoke-WebRequest -UseBasicParsing `
    -Uri http://127.0.0.1:5000/health `
    -Headers $healthHeaders |
    Select-Object StatusCode, Content

Remove-Variable storageKey, healthHeaders
```

En `v1.60.4`, `/health` exige autenticacion y comprueba tambien PostgreSQL.
Consultarlo sin `Authorization` devuelve `403 Invalid Compact JWS`; no significa
que el proceso Storage este detenido.

## Paso 5: preparar el bucket y reconstruir el backend

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml up storage-bucket-init
docker compose --env-file infra\.env -f infra\compose.local.yml logs storage-bucket-init
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --build backend
```

El inicializador debe imprimir:

```text
Bucket privado 'kontoraimagenes' preparado correctamente.
```

El backend se configura de forma explicita con
`SUPABASE_STORAGE_API_URL=http://storage:5000`. El cliente Java conserva
compatibilidad con `SUPABASE_URL` para una reversion temporal a Supabase Cloud,
pero la configuracion nueva tiene prioridad.

## Paso 6: comprobar metadata y aislamiento

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml exec postgres psql -U kontora_pos -d kontora_pos -c "SELECT id, name, public, file_size_limit, allowed_mime_types FROM storage.buckets;"
docker compose --env-file infra\.env -f infra\compose.local.yml exec postgres psql -U kontora_pos -d kontora_pos -c "SELECT count(*) AS migraciones_storage FROM storage.migrations;"
docker compose --env-file infra\.env -f infra\compose.local.yml ps
```

Criterios:

- existe `kontoraimagenes`;
- `public` es `false`;
- el limite es `13631488`;
- existen migraciones de Storage;
- PostgreSQL, Storage y backend aparecen saludables o activos;
- solo PostgreSQL, Storage y backend son procesos permanentes.

## Paso 7: prueba controlada de carga y descarga

Esta prueba crea un PNG diminuto, lo carga con la clave que permanece en
`infra/.env`, lo descarga, compara sus hashes y elimina el objeto:

```powershell
$storageKey = (Get-Content infra\.env |
    Where-Object { $_ -like 'STORAGE_SERVICE_ROLE_KEY=*' } |
    Select-Object -First 1).Split('=', 2)[1]
$headers = @{ Authorization = "Bearer $storageKey"; apikey = $storageKey }
$objectPath = "pruebas/fase2_$(Get-Date -Format 'yyyyMMdd_HHmmss').png"
$sourceFile = Join-Path $env:TEMP "kontora_storage_source.png"
$downloadFile = Join-Path $env:TEMP "kontora_storage_download.png"
$png = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII='
[IO.File]::WriteAllBytes($sourceFile, [Convert]::FromBase64String($png))

Invoke-WebRequest -UseBasicParsing `
    -Uri "http://127.0.0.1:5000/object/kontoraimagenes/$objectPath" `
    -Method Post -Headers $headers -ContentType 'image/png' -InFile $sourceFile
Invoke-WebRequest -UseBasicParsing `
    -Uri "http://127.0.0.1:5000/object/kontoraimagenes/$objectPath" `
    -Headers $headers -OutFile $downloadFile

Get-FileHash $sourceFile -Algorithm SHA256
Get-FileHash $downloadFile -Algorithm SHA256

Invoke-WebRequest -UseBasicParsing `
    -Uri "http://127.0.0.1:5000/object/kontoraimagenes/$objectPath" `
    -Method Delete -Headers $headers
Remove-Item -LiteralPath $sourceFile, $downloadFile
Remove-Variable storageKey
```

Los dos hashes deben ser iguales. No imprimir `$storageKey`.

## Paso 7B: probar el cliente Java contra Storage real

Esta prueba bajo demanda usa exactamente `SupabaseStorageClient`, el DNS
interno `storage` y la clave guardada en `infra/.env`. Carga, descarga, compara
y elimina un objeto temporal sin crear registros operativos:

```powershell
$backendPath = (Resolve-Path .\backend).Path

docker run --rm `
    --name kontora_pos_backend_storage_it `
    --network infra_application `
    --env-file infra\.env `
    --env SUPABASE_STORAGE_API_URL=http://storage:5000 `
    --mount "type=bind,source=$backendPath,target=/app" `
    --workdir /app `
    maven:3.9-eclipse-temurin-21 `
    mvn -B -Dtest=SupabaseStorageClientRuntimeIT test
```

La clase termina en `IT`, por lo que no se ejecuta accidentalmente dentro de
la suite unitaria normal. El resultado esperado es una prueba ejecutada y
`BUILD SUCCESS`.

## Paso 8: persistencia e independencia

Primero cargar un objeto de prueba sin eliminarlo. Despues:

```powershell
$postgresAntes = docker inspect --format '{{.Id}}|{{.State.StartedAt}}' kontora_pos_postgres_local
$backendAntes = docker inspect --format '{{.Id}}|{{.State.StartedAt}}' kontora_pos_backend_local

docker compose --env-file infra\.env -f infra\compose.local.yml up -d --force-recreate storage
Start-Sleep -Seconds 20

$postgresDespues = docker inspect --format '{{.Id}}|{{.State.StartedAt}}' kontora_pos_postgres_local
$backendDespues = docker inspect --format '{{.Id}}|{{.State.StartedAt}}' kontora_pos_backend_local
"PostgreSQL antes/despues: $postgresAntes / $postgresDespues"
"Backend antes/despues: $backendAntes / $backendDespues"
```

Los ID y `StartedAt` de PostgreSQL y backend deben permanecer iguales. Descargar
otra vez el objeto confirma que el volumen sobrevivio a la recreacion del
contenedor Storage. Eliminar el objeto al finalizar.

## Respaldo

El dump PostgreSQL conserva la metadata del esquema `storage`, pero no los
binarios. Para un respaldo consistente se necesitan ambos:

1. dump PostgreSQL verificado;
2. archivo del volumen `kontora_pos_storage_*_data`.

El backend de archivos de Storage `v1.60.4` guarda `content-type` y
`cache-control` en los atributos extendidos Linux
`user.supabase.content-type` y `user.supabase.cache-control`. Un `tar` creado
sin soporte de atributos extendidos conserva el binario, pero Storage responde
`500 ENODATA` al intentar descargarlo despues de restaurarlo. Por eso se usa
GNU tar dentro de un contenedor Debian y son obligatorios `--xattrs` y
`--xattrs-include=user.supabase.*` tanto al crear como al extraer.

Detener temporalmente escrituras antes de crear ambos:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml stop backend storage
New-Item -ItemType Directory -Force .\backups | Out-Null
$backupDir = (Resolve-Path .\backups).Path
$storageBackupName = "kontora_storage_files_$(Get-Date -Format 'yyyyMMdd_HHmmss').tar.gz"

docker run --rm `
    --mount "type=volume,source=kontora_pos_storage_local_data,target=/source,readonly" `
    --mount "type=bind,source=$backupDir,target=/backup" `
    debian:bookworm-slim `
    tar --xattrs --xattrs-include=user.supabase.* `
        -czf "/backup/$storageBackupName" -C /source .

docker compose --env-file infra\.env -f infra\compose.local.yml up -d storage
docker compose --env-file infra\.env -f infra\compose.local.yml up storage-bucket-init
docker compose --env-file infra\.env -f infra\compose.local.yml up -d backend

Get-Item ".\backups\$storageBackupName" |
    Select-Object FullName, Length, LastWriteTime
Get-FileHash ".\backups\$storageBackupName" -Algorithm SHA256
```

Aplicar tambien el procedimiento de dump y restauracion de la Fase 1. Copiar
ambos respaldos cifrados fuera del VPS.

Para restaurar los binarios en un volumen nuevo:

```powershell
docker volume create $restoreVolume
docker run --rm `
    --mount "type=bind,source=$backupDir,target=/backup,readonly" `
    --mount "type=volume,source=$restoreVolume,target=/restore" `
    debian:bookworm-slim `
    tar --xattrs --xattrs-include=user.supabase.* `
        -xzf "/backup/$storageBackupName" -C /restore
```

La restauracion solo se considera valida despues de iniciar una instancia
Storage aislada contra la base restaurada, descargar un objeto conocido y
comparar su SHA-256 con el archivo de origen.

## Reversion

Si Storage no inicia:

1. no ejecutar `down -v`;
2. guardar logs de `storage-db-init`, `storage` y `storage-bucket-init`;
3. conservar los volumenes de PostgreSQL y Storage;
4. corregir variables o volver a la version anterior del Compose;
5. si es imprescindible volver temporalmente a Supabase Cloud, retirar
   `SUPABASE_STORAGE_API_URL` y configurar `SUPABASE_URL` con la URL del
   proyecto y una clave de servicio valida.

## Cierre de la fase

La fase se cierra cuando Compose, health, bucket privado, carga/descarga,
persistencia, independencia y respaldo/restauracion hayan sido comprobados. No
se inicia la fase Nginx antes de registrar esas evidencias en la bitacora.

La fase de desarrollo local se cerro el 23 de julio de 2026 con todos esos
criterios resueltos. La pareja de respaldo aceptada y los hashes de verificacion
estan registrados en la bitacora. Las mismas comprobaciones se repetiran sobre
el VPS durante la validacion integral.
