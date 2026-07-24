# Uso del script SQL

## Fuente de verdad

El archivo `database/schema/kontora_pos_schema_v1_1.sql` es una copia del SQL fisico recibido en la documentacion inicial. Este archivo define tablas, columnas, enums, restricciones, triggers y datos base.

## Regla principal

El backend debe adaptarse al schema. No se deben cambiar nombres como:

- `existencias_inventario_general`
- `existencias_inventario_diario`
- `valor_pago`
- `estado_validacion`
- `id_usuario_vendedor`

## Base vacia

Si la base esta vacia, ejecutar el script completo:

```text
database/schema/kontora_pos_schema_v1_1.sql
```

Para Flyway, la copia versionada inicial queda en:

```text
database/migrations/V1__schema_inicial_kontora_pos.sql
```

La migracion usada por Spring Boot en runtime queda en:

```text
backend/src/main/resources/db/migration/V1__schema_inicial_kontora_pos.sql
```

Esa copia conserva el contenido estructural del SQL canonico. Solo omite `BEGIN;` y `COMMIT;`, porque Flyway administra la transaccion de la migracion.

## Base existente

Si Supabase ya contiene una version previa, no se debe ejecutar una migracion inventada. Se requiere un script incremental real y revisado contra el estado actual de la base.
