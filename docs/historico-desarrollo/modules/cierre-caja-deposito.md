# Modulo: Cierre de caja y deposito

## Objetivo

Cerrar la caja diaria, consolidar los resultados financieros del dia y registrar automaticamente el movimiento de deposito excluyendo la base de caja.

## Tablas involucradas

- `cajas_diarias`
- `cierres_caja`
- `ventas`
- `pagos_venta`
- `metodos_pago`
- `gastos_caja`
- `adiciones_diarias`
- `pagos_trabajadores_diarios`
- `movimientos_deposito`
- `usuarios`

## Fuente de verdad del modelo

El modulo se implementa sobre las tablas reales del schema:

- `cierres_caja.id_caja_diaria` es unico: una caja solo puede tener un cierre.
- `cajas_diarias.valor_base` se excluye del efectivo contado y del deposito.
- `pagos_venta.valor_pago` es la base para consolidar pagos por metodo.
- `pagos_venta.estado_validacion` permite separar transferencias `pendiente`, `validada` y `rechazada`.
- `gastos_caja.estado_gasto = 'anulado'` no suma al cierre.
- `adiciones_diarias.valor_total` es columna generada por PostgreSQL.
- `pagos_trabajadores_diarios.confirmado_para_cierre` debe estar en `true` antes del cierre.
- `movimientos_deposito.valor_movimiento` debe ser mayor que cero, por lo que solo se crea movimiento cuando `valor_a_deposito > 0`.

## Endpoints

| Metodo | Ruta | Autenticacion | Proposito |
| :- | :- | :- | :- |
| POST | `/api/cajas-diarias/{idCajaDiaria}/cerrar` | Si | Cerrar una caja diaria y crear entrada automatica de deposito. |
| GET | `/api/cajas-diarias/{idCajaDiaria}/cierre` | Si | Consultar el cierre registrado para una caja diaria. |

## Reglas de cierre

- Solo `administrador` y `gerente` pueden cerrar caja.
- La caja debe existir y estar en estado `abierta`.
- No se permite registrar mas de un cierre para la misma caja.
- Debe existir registro en `adiciones_diarias`.
- Debe existir pago diario a trabajadores y `confirmado_para_cierre` debe ser `true`.
- Solo suman ventas con `estado_venta = 'registrada'`.
- Solo suman gastos con `estado_gasto <> 'anulado'`.
- Las transferencias se muestran separadas por `pendiente`, `validada` y `rechazada`.
- `efectivo_esperado_sin_base` se calcula como efectivo recibido mas adiciones, menos gastos vigentes y pago a trabajadores.
- `efectivo_contado_sin_base` lo registra el usuario que cierra.
- `diferencia_caja` corresponde a `efectivo_contado_sin_base - efectivo_esperado_sin_base`.
- `valor_a_deposito` corresponde al efectivo contado sin base.
- El trigger canonico de base de datos cambia `cajas_diarias.estado_caja` a `cerrada` al insertar en `cierres_caja`.

## Reglas de deposito

- El cierre crea un movimiento en `movimientos_deposito` con `tipo_movimiento_deposito = 'entrada_cierre'`.
- `valor_movimiento` coincide con `cierres_caja.valor_a_deposito`.
- `saldo_anterior` se toma del ultimo `saldo_posterior` registrado en `movimientos_deposito`.
- `saldo_posterior` se calcula como `saldo_anterior + valor_movimiento`.
- No se suma `cajas_diarias.valor_base` al deposito.

## Pruebas realizadas

- `mvn clean test`.
- Resultado: `BUILD SUCCESS`.
- Pruebas ejecutadas: 34.
- Fallos: 0.
- Errores: 0.
- Omitidas: 0.
- Cierre exitoso con calculo de ventas, pagos, transferencias, gastos, adiciones y pago a trabajadores.
- Creacion automatica de `movimientos_deposito`.
- Validacion de que la base de caja no se suma al deposito.
- Bloqueo de cierre sin adiciones diarias.
- Bloqueo de cierre sin pago a trabajadores confirmado.
- Bloqueo de cierre por rol `vendedor`.
- Bloqueo de nuevas ventas despues del cierre.
- Bloqueo de anulaciones despues del cierre.
- Ajuste de fixtures de pruebas existentes para borrar `movimientos_deposito` antes de `cierres_caja`.

## Pendientes

- Evidencias de transferencias, gastos, consignaciones y pagos de servicios quedan para el modulo "Evidencias y almacenamiento".
- Consignaciones bancarias y pagos de servicios desde deposito quedan para los modulos posteriores definidos en la fase 3.

## Actualizaciones posteriores

- En el modulo "Auditoria transversal" se implemento auditoria explicita del cierre de caja y del movimiento automatico de deposito.

## Actualizacion frontend Fase 4

- La pantalla validada se documenta en `docs/modules/cierre-caja-deposito-frontend.md`.
- Administrador y gerente revisan el resumen calculado por backend, registran el efectivo contado sin base y confirman el cierre antes de enviar la solicitud.
- El cierre persistido se consulta por `fecha_operacion`, por lo que permanece disponible despues de refrescar la pagina.
- La jornada puede cerrarse despues de medianoche sin cambiar su fecha de operacion; solo se permite abrir otra caja cuando la anterior ya esta cerrada.
