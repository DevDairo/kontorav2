# Conexion a Supabase PostgreSQL

## Objetivo

Preparar la conexion del backend hacia Supabase PostgreSQL usando variables de entorno, sin credenciales versionadas.

## Variables requeridas

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `DB_SSLMODE`

Para Supabase, `DB_SSLMODE` debe ser `require`.

## Diferencia con PostgreSQL local

PostgreSQL local se usa para desarrollo y validacion del SQL. Supabase sigue siendo la referencia de despliegue para persistencia administrada y almacenamiento de evidencias.

