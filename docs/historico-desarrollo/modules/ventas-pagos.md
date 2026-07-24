# Modulo: Ventas y pagos

## Objetivo

Registrar ventas realizadas en mostrador, calcular precios y promociones, y guardar pagos en efectivo, transferencia o combinacion hibrida.

## Tablas involucradas

- `ventas`
- `detalles_venta`
- `pagos_venta`
- `cajas_diarias`
- `usuarios`
- `tipos_granizado`
- `tamanos_vaso`
- `precios_granizado`
- `promociones`
- `dias_promocion`
- `metodos_pago`

## Fuente de verdad del modelo

El modulo se implementa sobre las tablas reales del schema:

- `ventas.id_usuario_vendedor` registra el usuario autenticado que vende.
- `ventas.tipo_comprador` usa `cliente` o `trabajador`.
- `detalles_venta.precio_unitario_normal` conserva el precio historico aplicado.
- `pagos_venta.valor_pago` es el valor cubierto por cada pago.
- `pagos_venta.estado_validacion` queda en `no_aplica` para efectivo y `pendiente` para transferencia.
- El pago hibrido no existe como metodo: se representa con dos filas en `pagos_venta`.

## Endpoints

| Metodo | Ruta | Autenticacion | Proposito |
| :- | :- | :- | :- |
| POST | `/api/ventas` | Si | Registrar venta con detalles y pagos. |
| POST | `/api/ventas/{idVenta}/anular` | Si | Anular una venta registrada. La restauracion de stock diario se implementa en el modulo "Inventario operativo". |
| POST | `/api/pagos-venta/{idPagoVenta}/validar` | Si | Validar una transferencia pendiente. Implementado en el modulo "Auditoria transversal". |
| POST | `/api/pagos-venta/{idPagoVenta}/rechazar` | Si | Rechazar una transferencia pendiente. Implementado en el modulo "Auditoria transversal". |

## Flujo de venta normal

1. El usuario autenticado envia una venta con detalles y pagos.
2. El backend busca una caja diaria `abierta`.
3. El backend calcula precio normal vigente por tipo de granizado y tamano de vaso.
4. El backend aplica promocion vigente si corresponde.
5. El backend valida que la suma de `pagos_venta.valor_pago` coincida con `ventas.total_venta`.
6. Se guardan `ventas`, `detalles_venta` y `pagos_venta` dentro de una transaccion.

## Flujo de pago efectivo

- Usa `metodos_pago.nombre_metodo = 'efectivo'`.
- Registra `valor_pago`.
- Registra `valor_recibido_efectivo`.
- Calcula `cambio_entregado`.
- Guarda `estado_validacion = 'no_aplica'`.

## Flujo de pago transferencia

- Usa `metodos_pago.nombre_metodo = 'transferencia'`.
- Registra `valor_pago`.
- No usa `valor_recibido_efectivo`.
- Guarda `estado_validacion = 'pendiente'`.
- La validacion o rechazo posterior se implemento en el modulo "Auditoria transversal".

## Flujo de pago hibrido

- Se registran dos pagos para la misma venta:
  - Uno con metodo `efectivo`.
  - Uno con metodo `transferencia`.
- La suma de ambos debe coincidir con `ventas.total_venta`.

## Reglas de promocion

- Se consulta `precios_granizado` vigente por fecha.
- Se consulta `promociones` vigente por fecha.
- Para `cliente`, la promocion aplica solo si el dia actual esta configurado en `dias_promocion`.
- Para `trabajador`, la promocion de trabajador aplica cualquier dia.
- La promocion 2x aplica por conjuntos completos del mismo tipo y tamano.
- Las unidades sobrantes quedan en `cantidad_sin_promocion` y se cobran a precio normal.

## Reglas de negocio implementadas

- No se puede vender sin caja diaria abierta.
- La venta queda asociada a `cajas_diarias`.
- El total de venta coincide con la suma de detalles.
- La suma de pagos debe coincidir con `ventas.total_venta`.
- Transferencias quedan pendientes de validacion.
- Se conserva precio historico aplicado en `detalles_venta.precio_unitario_normal`.
- No se agregan migraciones nuevas: el schema canonico ya contiene las tablas de ventas y pagos.

## Pruebas realizadas

- `mvn clean test`.
- Rechazo de venta sin caja abierta.
- Registro de venta normal con pago en efectivo.
- Calculo de cambio entregado.
- Registro de venta hibrida con efectivo y transferencia.
- Transferencia queda en `estado_validacion = 'pendiente'`.
- Aplicacion de promocion 2x para trabajador.
- Rechazo cuando la suma de pagos no coincide con el total de venta.
- Correccion de aislamiento entre pruebas de caja diaria y ventas.

## Pendientes

- Evidencias de transferencia ya estan soportadas por el modulo "Evidencias y almacenamiento" mediante `POST /api/evidencias/pagos-venta/{idPagoVenta}`.
- Antes de continuar con el siguiente modulo frontend o preparar despliegue, revisar los pendientes operativos de Supabase Storage en `docs/modules/ventas-pagos-frontend-pendientes.md`, seccion "Como activar Supabase Storage para despliegue".
- La subida real de comprobantes requiere configurar `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` solo en backend/servidor y `SUPABASE_STORAGE_BUCKET`; el frontend no debe subir directo a Supabase ni exponer secretos.

## Actualizaciones posteriores

- En el modulo "Inventario operativo" se implemento la anulacion con restauracion de stock diario.
- En el modulo "Auditoria transversal" se implemento auditoria explicita de anulaciones de ventas.
- En el modulo "Auditoria transversal" se implemento validacion y rechazo de transferencias con auditoria sobre `pagos_venta`.
- En el modulo "Evidencias y almacenamiento" se implemento la carga de comprobantes para pagos de venta por transferencia; queda pendiente validar la carga real en despliegue con Supabase Storage configurado.
