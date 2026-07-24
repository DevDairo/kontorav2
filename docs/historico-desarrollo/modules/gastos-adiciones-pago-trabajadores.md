# Modulo: Gastos, adiciones y pago a trabajadores

## Objetivo

Registrar operaciones diarias que afectan el cuadre de caja: adiciones, gastos de caja y pago total diario a trabajadores.

## Tablas involucradas

- `adiciones_diarias`
- `pagos_trabajadores_diarios`
- `gastos_caja`
- `cajas_diarias`
- `usuarios`

## Fuente de verdad del modelo

El modulo se implementa sobre las tablas reales del schema:

- `adiciones_diarias` tiene un registro unico por `id_caja_diaria`.
- `adiciones_diarias.valor_total` es columna generada por base de datos a partir de `cantidad_adiciones * valor_unitario`.
- `pagos_trabajadores_diarios` tiene un registro unico por `id_caja_diaria`.
- `pagos_trabajadores_diarios.confirmado_para_cierre` controla si el pago queda listo para cierre.
- `gastos_caja.estado_gasto` usa los estados `registrado`, `editado` y `anulado`.
- `gastos_caja` conserva usuario y fecha de registro, ultima edicion y anulacion.

## Endpoints

| Metodo | Ruta | Autenticacion | Proposito |
| :- | :- | :- | :- |
| POST | `/api/operaciones-caja/adiciones-diarias` | Si | Crear o actualizar el registro unico de adiciones de la caja abierta. |
| GET | `/api/operaciones-caja/adiciones-diarias/abierta` | Si | Consultar adiciones de la caja abierta. |
| POST | `/api/operaciones-caja/gastos-caja` | Si | Registrar gasto de caja. |
| GET | `/api/operaciones-caja/gastos-caja/abierta` | Si | Listar gastos de la caja abierta. |
| PUT | `/api/operaciones-caja/gastos-caja/{idGastoCaja}` | Si | Editar un gasto de caja. |
| POST | `/api/operaciones-caja/gastos-caja/{idGastoCaja}/anular` | Si | Anular un gasto de caja. |
| POST | `/api/operaciones-caja/pagos-trabajadores-diarios` | Si | Crear o actualizar pago diario a trabajadores. |
| GET | `/api/operaciones-caja/pagos-trabajadores-diarios/abierta` | Si | Consultar pago diario a trabajadores de la caja abierta. |
| POST | `/api/operaciones-caja/pagos-trabajadores-diarios/{idPagoTrabajadoresDiario}/confirmar` | Si | Confirmar pago diario a trabajadores para cierre. |

## Reglas de adiciones

- Las adiciones se registran contra la caja diaria abierta.
- Solo existe un registro de `adiciones_diarias` por caja.
- Reenviar el endpoint actualiza el registro existente mientras la caja esta abierta.
- `cantidad_adiciones` puede ser cero.
- Si no se envia `valor_unitario`, se usa `1000.00`.
- `valor_total` lo calcula PostgreSQL como columna generada.

## Reglas de gastos

- No se registran gastos sin caja abierta.
- El rol `vendedor` puede registrar gastos.
- Solo `administrador` y `gerente` pueden editar gastos.
- Solo `administrador` y `gerente` pueden anular gastos.
- Un gasto anulado no se puede volver a editar ni anular.
- La edicion cambia `estado_gasto` a `editado` y registra usuario, fecha y motivo de edicion.
- La anulacion cambia `estado_gasto` a `anulado` y registra usuario, fecha y motivo de anulacion.

## Reglas de pago a trabajadores

- El pago diario a trabajadores se registra contra la caja abierta.
- Solo `administrador` y `gerente` pueden registrar o confirmar el pago diario a trabajadores.
- Solo existe un registro de `pagos_trabajadores_diarios` por caja.
- Reenviar el endpoint actualiza el registro existente mientras la caja esta abierta.
- Si `valor_total_pagado` es cero, `confirmado_para_cierre` debe enviarse en `true`.
- El pago puede registrarse sin confirmar y confirmarse despues con el endpoint de confirmacion.

## Pruebas realizadas

- `mvn clean test`.
- Resultado: `BUILD SUCCESS`.
- Pruebas ejecutadas: 29.
- Fallos: 0.
- Errores: 0.
- Rechazo de operaciones de caja sin autenticacion.
- Rechazo de gasto sin caja abierta.
- Registro y actualizacion de adiciones diarias.
- Registro de gasto por rol `vendedor`.
- Rechazo de edicion y anulacion de gasto por rol `vendedor`.
- Edicion de gasto por rol `administrador`.
- Anulacion de gasto por rol `gerente`.
- Registro y confirmacion de pago diario a trabajadores.
- Rechazo de pago a trabajadores en cero sin confirmacion explicita.
- Rechazo de pago a trabajadores por rol `vendedor`.

## Pendientes

- La consolidacion contable de gastos, adiciones y pago a trabajadores se usara en el modulo "Cierre de caja y deposito".
- Evidencias asociadas a gastos se implementaran en el modulo "Evidencias y almacenamiento".

## Actualizaciones posteriores

- En el modulo "Auditoria transversal" se implemento auditoria explicita de ediciones y anulaciones de gastos.
- Se agrego `GET /api/cajas-diarias/abierta/resumen`, restringido a `administrador` y `gerente`, para exponer el calculo de efectivo esperado sin base que reutiliza las reglas del cierre de caja.
- La interfaz de Fase 4 queda documentada en `docs/modules/gastos-adiciones-pago-trabajadores-frontend.md`; Caja administra adiciones y proyeccion, mientras Gastos concentra gastos y pago a trabajadores.
