# Modulo: Consultas operativas

## Objetivo

Permitir consultas internas de solo lectura sobre ventas, caja, gastos, inventario, deposito, transferencias y auditoria, respetando el alcance de visibilidad de cada rol.

## Tablas involucradas

- `ventas`
- `pagos_venta`
- `metodos_pago`
- `cajas_diarias`
- `cierres_caja`
- `gastos_caja`
- `items_inventario`
- `categorias_inventario`
- `unidades_medida`
- `tamanos_vaso`
- `existencias_inventario_general`
- `existencias_inventario_diario`
- `movimientos_inventario`
- `movimientos_deposito`
- `archivos_evidencia`
- `auditoria_operaciones`
- `usuarios`

## Fuente de verdad del modelo

El modulo se implementa sobre las tablas y columnas reales del schema canonico:

- `ventas.id_usuario_vendedor` filtra ventas propias del rol `vendedor`.
- `pagos_venta.valor_pago` alimenta totales y transferencias.
- `pagos_venta.estado_validacion` filtra transferencias `pendiente` y `rechazada`.
- `existencias_inventario_general` y `existencias_inventario_diario` se consultan sin modificar stock.
- `movimientos_deposito` conserva el historial del deposito.
- `auditoria_operaciones` se expone con filtros por fecha, tabla y accion.

No se agregan migraciones nuevas. Todas las consultas son `GET` y se ejecutan dentro de transacciones `readOnly`.

## Endpoints

| Metodo | Ruta | Autenticacion | Proposito |
| :- | :- | :- | :- |
| GET | `/api/consultas/ventas?fechaInicio=YYYY-MM-DD&fechaFin=YYYY-MM-DD` | Si | Consultar ventas por dia o periodo. |
| GET | `/api/consultas/cierre?fecha=YYYY-MM-DD` | Si | Consultar cierre registrado para una fecha. |
| GET | `/api/consultas/gastos?fechaInicio=YYYY-MM-DD&fechaFin=YYYY-MM-DD` | Si | Consultar gastos por dia o periodo. |
| GET | `/api/consultas/inventario/actual` | Si | Consultar inventario general y stock diario de la caja abierta. |
| GET | `/api/consultas/inventario/movimientos` | Si | Consultar movimientos de inventario con filtros opcionales de fecha, caja e item. |
| GET | `/api/consultas/deposito/movimientos?fechaInicio=YYYY-MM-DD&fechaFin=YYYY-MM-DD` | Si | Consultar historial de deposito completo o filtrado por periodo. |
| GET | `/api/consultas/transferencias` | Si | Consultar transferencias pendientes o rechazadas, con filtros opcionales. |
| GET | `/api/consultas/auditoria?fechaInicio=YYYY-MM-DD&fechaFin=YYYY-MM-DD` | Si | Consultar auditoria por periodo, tabla o accion. |

## Reglas de permisos

- `vendedor` puede consultar ventas propias, gastos propios, transferencias propias, inventario actual y movimientos de inventario.
- `vendedor` no puede consultar cierres, deposito ni auditoria.
- `administrador` puede consultar ventas, gastos, cierres, deposito, transferencias, inventario y auditoria operativa.
- `administrador` no recibe registros de auditoria de seguridad sobre `sesiones_usuario`.
- `gerente` tiene visibilidad completa, incluida auditoria de seguridad.

## Pruebas realizadas

- `mvn -Dtest=ConsultasOperativasIntegrationTest test`.
- Resultado: `BUILD SUCCESS`.
- Pruebas ejecutadas: 5.
- Fallos: 0.
- Errores: 0.
- Omitidas: 0.
- `mvn clean test`.
- Resultado: `BUILD SUCCESS`.
- Pruebas ejecutadas: 64.
- Fallos: 0.
- Errores: 0.
- Omitidas: 0.

## Casos validados

- Un `vendedor` solo ve sus ventas, gastos y transferencias.
- Un `vendedor` no puede consultar cierre, deposito ni auditoria.
- Un `administrador` consulta cierre, deposito y auditoria operativa.
- Un `administrador` no ve auditoria de seguridad sobre `sesiones_usuario`.
- Un `gerente` consulta auditoria completa.
- Las transferencias rechazadas se consultan por estado.
- El inventario actual muestra stock general y stock diario de la caja abierta.
- Los movimientos de inventario se consultan por fecha y caja.
- El historial de deposito puede consultarse sin fechas para recuperar todos los movimientos, o con ambas fechas para limitar el periodo.
- Cada movimiento de deposito asociado a consignacion o pago de servicio expone su identificador de registro y, cuando aplica, el nombre del servicio para enlazar evidencias y consultas posteriores.

## Pendientes

- Definir en la siguiente fase si se agregaran reportes exportables o endpoints agregados para tableros administrativos.

## Actualizacion frontend

- La ruta `/transferencias` consume `GET /api/consultas/transferencias` por periodo y solicita en paralelo los estados `pendiente` y `rechazada` para presentar sus contadores.
- El endpoint no devuelve transferencias `validada`; una vez decidida, la pantalla actualiza la lista y la trazabilidad se consulta mediante auditoria.
- Para cada transferencia seleccionada, el frontend consulta `GET /api/evidencias/pagos-venta/{idPagoVenta}` y presenta metadata autorizada sin exponer la ruta interna de Storage como enlace publico.

## Implementacion frontend de Consultas

- La pantalla React se implemento en `docs/modules/consultas-operativas-frontend.md` y consume exclusivamente los endpoints `GET` de este modulo.
- La consulta por periodo centraliza el historial de movimientos de inventario y de deposito, evitando duplicar esas listas en `/inventario` y `/deposito`.
- El historial de deposito conserva señales visuales: `Landmark` para `entrada_cierre` y `Building2` para `salida_consignacion` o `salida_pago_servicio`.
- `vendedor` recibe solo Ventas y Gastos en la interfaz; administrador y gerente reciben ademas Inventario, Cierre y Deposito. Esta visibilidad es de experiencia de usuario y no sustituye los permisos backend.
- La ruta permanece con estado `pendiente` hasta completar la confirmacion manual final de filtros de fecha y roles.
