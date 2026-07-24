# DEC-001: Separacion entre logica backend y base de datos

## Estado

Aceptada.

## Contexto

Kontora POS necesita trazabilidad y consistencia en ventas, caja diaria, pagos, inventario, deposito, evidencias y auditoria. La base de datos ya esta disenada y debe ser la fuente estructural de verdad.

## Decision

La base de datos conserva persistencia, relaciones, restricciones basicas e historicos. El backend concentra autenticacion, autorizacion, reglas de negocio, transacciones, validaciones operativas y auditoria aplicativa.

## Consecuencias

- Hibernate no debe crear ni modificar tablas automaticamente.
- JPA debe usar nombres reales con `@Table` y `@Column`.
- Las migraciones deben respetar el SQL canonico.
- No se permiten nombres alternativos para tablas o columnas ya definidas.

