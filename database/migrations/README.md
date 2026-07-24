# Migraciones Flyway

La base de datos debe guiar al backend. Por eso la primera migracion versionada conserva el SQL canonico entregado:

- `V1__schema_inicial_kontora_pos.sql`

No se creo una migracion incremental `V1_1__ajustes_inventario_alertas_y_referencias.sql` porque no existe un archivo incremental separado en la documentacion recibida. Si aparece ese script, debe agregarse como nueva migracion sin modificar el schema canonico ya versionado.

Reglas:

- No renombrar tablas o columnas para acomodarlas al codigo.
- No crear tablas paralelas si ya existe una tabla canonica.
- No mantener compatibilidad artificial con nombres incorrectos.

