# Modulo: Auditoria transversal

## Objetivo

Registrar operaciones sensibles realizadas por usuarios del sistema en `auditoria_operaciones`, conservando usuario responsable, tabla afectada, registro afectado, accion y valores anteriores/nuevos cuando aplica.

## Tablas involucradas

- `auditoria_operaciones`
- `usuarios`
- `sesiones_usuario`
- `cajas_diarias`
- `cierres_caja`
- `ventas`
- `pagos_venta`
- `gastos_caja`
- `movimientos_deposito`

## Fuente de verdad del modelo

El modulo se implementa sobre la tabla real del schema:

- `auditoria_operaciones.id_usuario` referencia al usuario responsable.
- `auditoria_operaciones.tabla_afectada` guarda la tabla o entidad impactada.
- `auditoria_operaciones.id_registro_afectado` guarda el identificador del registro auditado como texto.
- `auditoria_operaciones.accion` usa `accion_auditoria_enum`.
- `auditoria_operaciones.valor_anterior` y `auditoria_operaciones.valor_nuevo` guardan snapshots JSONB cuando aplica.
- `auditoria_operaciones.direccion_ip` se toma del request HTTP, usando primero `X-Forwarded-For`.

## Endpoints nuevos

| Metodo | Ruta | Autenticacion | Proposito |
| :- | :- | :- | :- |
| POST | `/api/pagos-venta/{idPagoVenta}/validar` | Si | Validar una transferencia pendiente. |
| POST | `/api/pagos-venta/{idPagoVenta}/rechazar` | Si | Rechazar una transferencia pendiente. |

## Operaciones auditadas

- `POST /api/auth/login` registra accion `login` sobre `sesiones_usuario`.
- `POST /api/auth/logout` registra accion `logout` sobre `sesiones_usuario`.
- `POST /api/cajas-diarias` registra accion `abrir` sobre `cajas_diarias`.
- `POST /api/cajas-diarias/{idCajaDiaria}/cerrar` registra accion `cerrar` sobre `cierres_caja`.
- El cierre con deposito mayor a cero registra accion `crear` sobre `movimientos_deposito`.
- `PUT /api/operaciones-caja/gastos-caja/{idGastoCaja}` registra accion `editar` sobre `gastos_caja`.
- `POST /api/operaciones-caja/gastos-caja/{idGastoCaja}/anular` registra accion `anular` sobre `gastos_caja`.
- `POST /api/ventas/{idVenta}/anular` registra accion `anular` sobre `ventas`.
- `POST /api/pagos-venta/{idPagoVenta}/validar` registra accion `validar` sobre `pagos_venta`.
- `POST /api/pagos-venta/{idPagoVenta}/rechazar` registra accion `rechazar` sobre `pagos_venta`.

## Reglas de negocio implementadas

- Solo `administrador` y `gerente` pueden validar o rechazar transferencias.
- Solo pagos reales con metodo `transferencia` pueden validarse o rechazarse.
- Solo transferencias en `estado_validacion = 'pendiente'` pueden cambiar a `validada` o `rechazada`.
- La validacion o rechazo registra `id_usuario_validacion`, `fecha_validacion` y `observacion_validacion` en `pagos_venta`.
- La auditoria de ediciones, anulaciones, validaciones, cierres y movimientos guarda `valor_anterior` y `valor_nuevo`.
- La auditoria de creaciones o eventos sin estado previo guarda `valor_nuevo`.
- No se agregan migraciones nuevas: el schema canonico ya contiene `auditoria_operaciones` y los campos de validacion de `pagos_venta`.

## Pruebas realizadas

- `mvn -Dtest=AuditoriaIntegrationTest test`.
- Resultado: `BUILD SUCCESS`.
- Pruebas ejecutadas: 4.
- Fallos: 0.
- Errores: 0.
- Omitidas: 0.
- `mvn clean test`.
- Resultado: `BUILD SUCCESS`.
- Pruebas ejecutadas: 43.
- Fallos: 0.
- Errores: 0.
- Omitidas: 0.

## Casos validados

- Login registra auditoria con usuario e IP.
- Logout registra auditoria de cierre de sesion.
- Apertura de caja registra auditoria sobre `cajas_diarias`.
- Cierre de caja registra auditoria sobre `cierres_caja`.
- Movimiento automatico de deposito registra auditoria sobre `movimientos_deposito`.
- Edicion y anulacion de gasto registran valores anteriores y nuevos.
- Validacion de transferencia actualiza `pagos_venta.estado_validacion` a `validada` y audita el cambio.
- Rechazo de transferencia actualiza `pagos_venta.estado_validacion` a `rechazada` y audita el cambio.

## Pendientes

- Solicitud, aprobacion y rechazo de ajustes de inventario quedan pendientes porque el flujo operativo de `ajustes_inventario` aun no esta implementado.
- Cambios de precios, promociones y configuraciones quedan pendientes hasta implementar sus flujos administrativos.
- Consulta de auditoria por filtros y permisos queda para el modulo "Consultas operativas".

## Actualizacion frontend

- La ruta `/transferencias` consume los endpoints de validacion y rechazo con una confirmacion previa y campo opcional `observacionValidacion`.
- En la validacion manual del 2026-07-10, dos decisiones `validar` generaron auditoria sobre `pagos_venta`: una transferencia pura de `$12.000` y una porcion transferida de `$8.000` de un pago mixto.
- La respuesta de auditoria conservo el estado anterior `pendiente`, el estado nuevo `validada`, el identificador del gerente validador y la fecha de decision.
