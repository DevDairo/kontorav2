# Supabase Storage autoalojado

## Resultado

Kontora utiliza únicamente Supabase Storage API:

```text
backend -> storage:5000 -> volumen Docker
                |
                +-> esquema storage en PostgreSQL
```

No se instala Supabase Studio, Auth, Realtime, Kong ni PostgREST. El bucket
`kontoraimagenes` es privado y solamente el backend conoce la clave
`service_role`.

## Preparar el entorno local

Si `infra/.env` todavía no existe:

```powershell
Copy-Item infra\.env.example infra\.env
```

Generar las claves de Storage:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\infra\scripts\New-StorageSecrets.ps1
```

Copiar los dos valores generados a `infra/.env`:

```env
STORAGE_JWT_SECRET=<VALOR-GENERADO>
STORAGE_SERVICE_ROLE_KEY=<VALOR-GENERADO>
```

No reemplazar una sola clave: ambas deben generarse juntas.

## Orden exacto de inicio

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml config --quiet
docker compose --env-file infra\.env -f infra\compose.local.yml up -d postgres
docker compose --env-file infra\.env -f infra\compose.local.yml up -d storage
docker compose --env-file infra\.env -f infra\compose.local.yml ps -a
docker compose --env-file infra\.env -f infra\compose.local.yml up storage-bucket-init
docker compose --env-file infra\.env -f infra\compose.local.yml ps -a
```

`storage-db-init` y `storage-bucket-init` deben terminar con código `0`.
`postgres` y `storage` deben aparecer como `healthy`.

Confirmar el bucket:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres psql -U kontora_pos -d kontora_pos -c "SELECT id, name, public, file_size_limit, allowed_mime_types FROM storage.buckets;"
```

El resultado esperado es:

```text
id: kontoraimagenes
public: false
file_size_limit: 13631488
allowed_mime_types: image/*, application/pdf
```

## Iniciar backend

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml build backend
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --no-deps --no-build --force-recreate backend
curl.exe --fail http://127.0.0.1:8080/api/health
```

El backend usa internamente:

```env
SUPABASE_STORAGE_API_URL=http://storage:5000
SUPABASE_STORAGE_BUCKET=kontoraimagenes
```

Storage no debe consumirse directamente desde React.

## Probar el cliente Java contra Storage real

Con PostgreSQL y Storage activos:

```powershell
docker run --rm --name kontora_pos_backend_storage_it --network infra_application --env-file infra\.env --env SUPABASE_STORAGE_API_URL=http://storage:5000 --mount "type=bind,source=$PWD\backend,target=/app" --workdir /app maven:3.9-eclipse-temurin-21 mvn -B -Dtest=SupabaseStorageClientRuntimeIT test
```

La prueba carga, descarga, compara y elimina un objeto temporal.

## Respaldo de Storage

Primero crear el respaldo de PostgreSQL indicado en
[PostgreSQL y backend](./01-fase-postgresql-backend.md). Después respaldar el
volumen de archivos:

```powershell
New-Item -ItemType Directory -Force .\backups
docker run --rm --mount "type=volume,source=kontora_pos_storage_local_data,target=/data,readonly" --mount "type=bind,source=$PWD\backups,target=/backups" postgres:16-alpine tar -czf /backups/kontora_storage.tar.gz -C /data .
```

Conservar juntos:

```text
backups/kontora_pos.dump
backups/kontora_storage.tar.gz
```

La base contiene la metadata; el archivo comprimido contiene los binarios.
Restaurar solo uno de los dos deja evidencias incompletas.

## Restaurar en una instalación nueva

Ejecutar antes de iniciar Storage:

```bash
docker volume create kontora_pos_storage_prod_data
docker run --rm --mount type=volume,source=kontora_pos_storage_prod_data,target=/restore --mount type=bind,source="$PWD/backups",target=/backups,readonly postgres:16-alpine tar -xzf /backups/kontora_storage.tar.gz -C /restore
```

Después restaurar PostgreSQL y arrancar los servicios en el orden indicado en
la guía del VPS.

## Límites configurados

- tamaño máximo: 13 MB;
- tipos: imágenes y PDF;
- backend de archivos local;
- transformaciones desactivadas;
- S3 desactivado;
- métricas y colas desactivadas;
- máximo de cinco conexiones a PostgreSQL.

## Errores frecuentes

| Error | Corrección |
| --- | --- |
| `403 Invalid Compact JWS` | Generar nuevamente `STORAGE_JWT_SECRET` y `STORAGE_SERVICE_ROLE_KEY` como pareja. |
| Storage no queda `healthy` | Revisar `storage-db-init`, conexión a PostgreSQL y las dos claves. |
| Bucket inexistente | Ejecutar otra vez `up storage-bucket-init`. |
| Backend no descarga | Confirmar `http://storage:5000`, bucket y `service_role`. |
| Evidencia sin binario | Restaurar el volumen correspondiente al mismo respaldo de PostgreSQL. |

No usar `down -v`: elimina el volumen de Storage y el de PostgreSQL.
