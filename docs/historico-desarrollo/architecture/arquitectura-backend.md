# Arquitectura backend

El backend de Kontora POS se construye con Java 21 y Spring Boot. Su responsabilidad es exponer la API REST, centralizar reglas de negocio, validar permisos, ejecutar transacciones y persistir contra el schema canonico de PostgreSQL.

## Principios

- La base de datos es la fuente estructural de verdad.
- JPA debe validar el schema con `ddl-auto=validate`.
- Flyway controla migraciones, sin permitir que Hibernate cree o cambie tablas.
- La seguridad se basa en Spring Security, JWT y sesiones persistidas en fases posteriores.
- Los endpoints de negocio se implementaran por modulo y en orden secuencial.

## Estado actual

La Fase 2 solo crea la infraestructura base:

- Aplicacion Spring Boot.
- Configuracion de datasource.
- Flyway habilitado.
- JPA en modo validate.
- Seguridad base stateless.
- Endpoint `GET /api/health`.

No se implementa todavia login, caja, ventas, pagos ni inventario.

