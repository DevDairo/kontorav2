# PostgreSQL y backend separados

## Resultado

PostgreSQL y Spring Boot se ejecutan en contenedores independientes:

```text
backend -> postgres:5432
```

El backend puede recrearse sin reiniciar PostgreSQL y los datos permanecen en
el volumen `kontora_pos_postgres_local_data`.

## Orden exacto en local

Ejecutar desde la raíz:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml config --quiet
docker compose --env-file infra\.env -f infra\compose.local.yml up -d postgres
docker compose --env-file infra\.env -f infra\compose.local.yml ps postgres
docker compose --env-file infra\.env -f infra\compose.local.yml build backend
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --no-deps --no-build backend
docker compose --env-file infra\.env -f infra\compose.local.yml ps backend
curl.exe --fail http://127.0.0.1:8080/api/health
```

No continuar cuando un comando falle.

Confirmar las migraciones:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres psql -U kontora_pos -d kontora_pos -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

## Aplicar únicamente un cambio backend

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml build backend
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --no-deps --no-build --force-recreate backend
```

Este procedimiento no recrea PostgreSQL.

## Crear un respaldo

El siguiente procedimiento genera
`backups\kontora_pos.dump`. Si ya existe, renombrarlo antes de comenzar.

```powershell
New-Item -ItemType Directory -Force .\backups
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom --file=/tmp/kontora_pos.dump'
docker cp kontora_pos_postgres_local:/tmp/kontora_pos.dump .\backups\kontora_pos.dump
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres rm -f /tmp/kontora_pos.dump
```

No guardar el respaldo dentro del repositorio Git.

## Probar la restauración

La prueba usa una base temporal y no modifica `kontora_pos`:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres dropdb --if-exists --force -U kontora_pos kontora_restore_test
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres createdb -U kontora_pos kontora_restore_test
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres psql -U kontora_pos -d kontora_restore_test -c "DROP SCHEMA public CASCADE;"
docker cp .\backups\kontora_pos.dump kontora_pos_postgres_local:/tmp/kontora_pos.dump
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres pg_restore --exit-on-error --no-owner --no-acl -U kontora_pos -d kontora_restore_test /tmp/kontora_pos.dump
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres psql -U kontora_pos -d kontora_restore_test -c "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank;"
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres dropdb --force -U kontora_pos kontora_restore_test
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres rm -f /tmp/kontora_pos.dump
```

Ejecutar todos los comandos en orden. No usar como nombre temporal
`kontora_pos`.

## Detener sin borrar datos

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml down
```

No usar `down -v`: elimina el volumen de PostgreSQL.

## Producción

- PostgreSQL no publica `5432` en el host.
- Backend escucha únicamente en `127.0.0.1:8080`.
- La contraseña se guarda solo en `infra/.env`.
- `DB_HOST` debe ser `postgres`.
- Antes de actualizar, crear un respaldo probado.

## Errores frecuentes

| Error | Corrección |
| --- | --- |
| Backend no conecta | Revisar que PostgreSQL esté `healthy`, `DB_HOST=postgres` y la contraseña. |
| Flyway falla | No editar migraciones aplicadas; crear una migración nueva. |
| Restauración falla porque `public` existe | Ejecutar el `DROP SCHEMA public CASCADE` indicado antes de `pg_restore`. |
| Datos desaparecen | Confirmar que se usa el volumen nombrado y evitar `down -v`. |
