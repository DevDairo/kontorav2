# Fase 1: separar PostgreSQL y Spring Boot

## Resultado esperado

El VPS ejecuta dos contenedores independientes:

- `kontora_pos_postgres`: PostgreSQL 16 y el volumen persistente
  `kontora_pos_postgres_prod_data`.
- `kontora_pos_backend`: Spring Boot, sin datos persistentes propios.

PostgreSQL no publica `5432` en el host. El backend llega a la base mediante la
red privada de Docker y el nombre DNS `postgres`. La API solo se enlaza a
`127.0.0.1:8080`.

Flyway, incluido en el backend, crea el esquema cuando la base esta vacia y
aplica migraciones pendientes. PostgreSQL ya no monta el SQL canonico en
`docker-entrypoint-initdb.d`; asi se evita tener dos mecanismos distintos
intentando inicializar el mismo esquema.

> Estado actual: esta fase se valido originalmente cuando Compose solo
> contenia `postgres` y `backend`. El archivo vigente ya incorpora la Fase 2;
> por eso, en una instalacion nueva, se deben completar tambien las variables y
> los pasos de
> [Storage local](./02-fase-supabase-storage-local.md) antes de iniciar
> `backend`.

## Paso 0: decidir si existen datos que conservar

Antes de iniciar:

- Si Supabase PostgreSQL nunca recibio datos reales, seguir la ruta de
  **base nueva**.
- Si existen usuarios, ventas, caja, inventario u otros datos reales, seguir
  la ruta de **base existente** y no iniciar el backend nuevo hasta restaurar
  el respaldo.

Los archivos del bucket no forman parte del respaldo PostgreSQL. Se migraran en
la Fase 2.

## Paso 1: validar primero en el equipo Windows limpio

No se necesitan imagenes Docker previas. Con Docker Desktop iniciado, abrir
PowerShell en la raiz del proyecto y preparar el entorno local:

```powershell
Copy-Item infra\.env.example infra\.env
```

Editar `infra/.env` y asignar un `JWT_SECRET` propio. Despues validar y crear
cada contenedor por separado:

```powershell
docker version
docker compose version
docker compose --env-file infra\.env -f infra\compose.local.yml config --quiet
docker compose --env-file infra\.env -f infra\compose.local.yml up -d postgres
docker compose --env-file infra\.env -f infra\compose.local.yml ps
docker compose --env-file infra\.env -f infra\compose.local.yml logs --tail=100 postgres
```

La primera ejecucion descarga `postgres:16-alpine`. Cuando PostgreSQL aparezca
`healthy`, construir el backend desde el repositorio:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --build backend
docker compose --env-file infra\.env -f infra\compose.local.yml logs --tail=200 backend
Invoke-WebRequest http://127.0.0.1:8080/api/health
```

Verificar las migraciones:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml exec postgres psql -U kontora_pos -d kontora_pos -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

La consulta debe mostrar `V1` y `V2` exitosas. Reiniciar solo el backend y
confirmar que la base permanece activa:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml restart backend
docker compose --env-file infra\.env -f infra\compose.local.yml ps
Invoke-WebRequest http://127.0.0.1:8080/api/health
```

Si un comando falla, no ejecutar `down -v`. Guardar la salida y agregarla a la
bitacora antes de corregir.

## Paso 2: preparar una instalacion limpia en el VPS

En Ubuntu:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl
```

Instalar Docker Engine y el complemento Compose siguiendo la documentacion
oficial de Docker para la version de Ubuntu utilizada. Confirmar:

```bash
docker version
docker compose version
```

No es necesario conservar ni copiar imagenes del equipo anterior. Compose
descarga `postgres:16-alpine` y construye el backend desde `backend/Dockerfile`.

## Paso 3: crear la configuracion privada

Desde la raiz del proyecto:

```bash
cp infra/.env.production.example infra/.env
chmod 600 infra/.env
```

Editar `infra/.env` y completar como minimo:

```env
DB_NAME=kontora_pos
DB_USER=kontora_pos
DB_PASSWORD=<contrasena-larga-y-exclusiva>
JWT_SECRET=<secreto-aleatorio-largo>
BOOTSTRAP_MANAGER_PASSWORD=<contrasena-inicial-si-aplica>
```

Mantener estos valores:

```env
DB_HOST=postgres
DB_PORT=5432
DB_SSLMODE=disable
APP_BIND_ADDRESS=127.0.0.1
POSTGRES_VOLUME_NAME=kontora_pos_postgres_prod_data
```

`DB_SSLMODE=disable` solo se usa porque el trafico permanece dentro de la red
privada del mismo host Docker. PostgreSQL no se publica en Internet.

## Paso 4: validar la configuracion antes de crear recursos

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml config --quiet
docker compose --env-file infra/.env -f infra/compose.prod.yml config --services
```

La segunda orden debe listar:

```text
postgres
storage-db-init
storage
storage-bucket-init
backend
```

Revisar la configuracion renderizada sin compartir su salida, porque contiene
los secretos expandidos:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml config
```

Comprobar que `postgres` no tiene una seccion `ports`.

## Paso 5A: ruta de base nueva

Iniciar solo PostgreSQL:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml up -d postgres
docker compose --env-file infra/.env -f infra/compose.prod.yml ps
docker compose --env-file infra/.env -f infra/compose.prod.yml logs --tail=100 postgres
```

Esperar a que aparezca `healthy`. Despues construir e iniciar solo el backend:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml up -d --build backend
docker compose --env-file infra/.env -f infra/compose.prod.yml logs --tail=200 backend
```

En el log debe verse que Flyway aplico `V1` y `V2`, y que Spring Boot inicio sin
errores de validacion de Hibernate.

## Paso 5B: ruta de base existente

Antes de cambiar el origen actual, crear un dump de la base remota con
PostgreSQL 16. Exportar solo el esquema de la aplicacion:

```bash
pg_dump "$DATABASE_URL_ORIGEN" \
  --format=custom \
  --schema=public \
  --no-owner \
  --no-acl \
  --file=kontora_public.dump
```

Validar que el archivo se puede listar:

```bash
pg_restore --list kontora_public.dump > kontora_public.contents.txt
test -s kontora_public.contents.txt
```

Iniciar solo PostgreSQL local, copiar el dump y restaurarlo:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml up -d postgres
docker cp kontora_public.dump kontora_pos_postgres:/tmp/kontora_public.dump
docker compose --env-file infra/.env -f infra/compose.prod.yml exec postgres \
  sh -c 'pg_restore --exit-on-error --no-owner --no-acl \
  -U "$POSTGRES_USER" -d "$POSTGRES_DB" /tmp/kontora_public.dump'
docker compose --env-file infra/.env -f infra/compose.prod.yml exec postgres \
  rm -f /tmp/kontora_public.dump
```

Solo despues de una restauracion exitosa se construye e inicia el backend:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml up -d --build backend
```

Si el origen ya tenia `flyway_schema_history`, Flyway validara su historial. Si
el esquema existia sin historial, la configuracion actual crea una linea base
en version `0` y ejecuta las migraciones idempotentes.

## Paso 6: validar la separacion

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml ps
curl -i http://127.0.0.1:8080/api/health
docker compose --env-file infra/.env -f infra/compose.prod.yml exec backend \
  sh -c 'getent hosts postgres'
docker compose --env-file infra/.env -f infra/compose.prod.yml port postgres 5432
```

Criterios:

- Ambos contenedores estan activos y PostgreSQL aparece `healthy`.
- El health local responde `HTTP 200` y contiene `"status":"ok"`.
- El backend resuelve el nombre `postgres`.
- `docker compose port postgres 5432` no devuelve un puerto publicado.
- Reiniciar el backend no reinicia ni elimina PostgreSQL.
- Reiniciar PostgreSQL no elimina sus datos.

Prueba de independencia:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml restart backend
docker compose --env-file infra/.env -f infra/compose.prod.yml ps
curl -i http://127.0.0.1:8080/api/health
```

## Paso 7: crear y comprobar un respaldo local

```bash
mkdir -p backups
docker compose --env-file infra/.env -f infra/compose.prod.yml exec -T postgres \
  sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom' \
  > backups/kontora_pos.dump
test -s backups/kontora_pos.dump
docker run --rm -v "$PWD/backups:/backups:ro" postgres:16-alpine \
  pg_restore --list /backups/kontora_pos.dump > /dev/null
```

En Windows PowerShell no se redirige el dump binario con `>` porque Windows
PowerShell puede tratar la salida nativa como texto. Generar el archivo dentro
del contenedor y copiarlo con `docker cp`:

```powershell
New-Item -ItemType Directory -Force .\backups | Out-Null
$backupPath = ".\backups\kontora_pos_$(Get-Date -Format 'yyyyMMdd_HHmmss').dump"
$restoreDb = "kontora_pos_restore_test_$(Get-Date -Format 'yyyyMMddHHmmss')"

docker compose --env-file infra\.env -f infra\compose.local.yml exec postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom --file=/tmp/kontora_pos.dump'
docker cp kontora_pos_postgres_local:/tmp/kontora_pos.dump $backupPath
docker compose --env-file infra\.env -f infra\compose.local.yml exec postgres createdb -U kontora_pos $restoreDb
docker compose --env-file infra\.env -f infra\compose.local.yml exec postgres pg_restore --exit-on-error --no-owner --no-acl -U kontora_pos -d $restoreDb /tmp/kontora_pos.dump
docker compose --env-file infra\.env -f infra\compose.local.yml exec postgres psql -U kontora_pos -d $restoreDb -c "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank;"
docker compose --env-file infra\.env -f infra\compose.local.yml exec postgres dropdb --force -U kontora_pos $restoreDb
docker compose --env-file infra\.env -f infra\compose.local.yml exec postgres rm -f /tmp/kontora_pos.dump

Get-Item $backupPath | Select-Object FullName, Length, LastWriteTime
Get-FileHash $backupPath -Algorithm SHA256
```

Mover el respaldo cifrado a otro host o almacenamiento. Un volumen en el mismo
VPS no sustituye una copia externa.

## Operacion normal

Actualizar solo el backend:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml up -d --build backend
```

Revisar PostgreSQL:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml ps postgres
docker compose --env-file infra/.env -f infra/compose.prod.yml logs --tail=100 postgres
```

Detener servicios conservando el volumen:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml down
```

No agregar `-v`: esa opcion elimina el volumen nombrado y sus datos.

## Reversion

Si el backend no inicia:

1. No eliminar ni recrear el volumen.
2. Guardar `logs` de `backend` y `postgres`.
3. Detener solo `backend`.
4. Si se migro una base existente, conservar el servicio anterior sin cambios
   hasta verificar el nuevo.
5. Restaurar la version anterior del codigo/configuracion y reconstruir solo
   `backend`.

Una migracion Flyway que ya modifico el esquema no se revierte restaurando una
imagen anterior. Para ese caso se restaura el respaldo verificado en una base
limpia.

## Cierre de la Fase 1

La fase se considera terminada solo cuando todos los criterios del Paso 6 y el
respaldo del Paso 7 se hayan verificado en la maquina que ejecuta Docker. Hasta
entonces no se implementa Supabase Storage local.
