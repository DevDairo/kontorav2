# Modulo: Inventario operativo

## Objetivo

Controlar el stock general, el stock diario de vasos y los movimientos de inventario generados por apertura de paquetes, ventas, anulaciones, consumos manuales y ajustes aprobados.

## Tablas involucradas

- `items_inventario`
- `existencias_inventario_general`
- `existencias_inventario_diario`
- `movimientos_inventario`
- `paquetes_vasos_abiertos`
- `consumos_diarios_inventario`
- `ajustes_inventario`
- `ventas`
- `detalles_venta`
- `cajas_diarias`
- `usuarios`

## Fuente de verdad del modelo

El modulo se implementa sobre las tablas reales del schema:

- `existencias_inventario_general.cantidad_actual` representa el stock general disponible por item.
- `existencias_inventario_diario` representa el stock operativo diario, usado en este modulo para vasos. Al abrir una caja nueva, cada item de vaso con paquetes conserva como `cantidad_inicial` el remanente de la jornada anterior: primero el conteo fisico final y, si no existe, el saldo teorico final.
- `movimientos_inventario` registra cada cambio de stock con `referencia_origen` e `id_referencia_origen`.
- `paquetes_vasos_abiertos` registra paquetes abiertos y vasos rotos al abrirlos.
- `consumos_diarios_inventario` registra consumos manuales de items no automaticos por venta.
- `ajustes_inventario` registra solicitudes de ajuste de stock y su estado de aprobacion.
- `items_inventario.tipo_control` separa `automatico_por_venta` de `manual_por_consumo`.
- No se implementa `cantidad_minima_alerta` porque esa columna no existe en `kontora_pos_schema.txt`.

## Endpoints

| Metodo | Ruta | Autenticacion | Proposito |
| :- | :- | :- | :- |
| GET | `/api/inventario/existencias/general` | Si | Consultar stock general de inventario. |
| GET | `/api/inventario/existencias/diarias/abierta` | Si | Consultar stock diario de la caja abierta. |
| GET | `/api/inventario/existencias/diarias/caja/{idCajaDiaria}` | Si | Consultar stock diario de una caja especifica. |
| POST | `/api/inventario/paquetes-vasos` | Si | Registrar paquetes de vasos abiertos. |
| POST | `/api/inventario/consumos-diarios` | Si | Registrar consumo manual diario de inventario. |
| GET | `/api/inventario/movimientos` | Si | Consultar movimientos de inventario. Permite filtros `idCajaDiaria` e `idItemInventario`. |
| GET | `/api/inventario/ajustes` | Administrador o gerente | Consultar ajustes de inventario. Permite filtro `estadoAprobacion`. |
| POST | `/api/inventario/ajustes` | Administrador o gerente | Administrador solicita un ajuste; gerente aplica directamente su propio ajuste de stock general. |
| POST | `/api/inventario/ajustes/{idAjusteInventario}/aprobar` | Si | Aprobar ajuste pendiente y aplicar cambio de stock general. |
| POST | `/api/inventario/ajustes/{idAjusteInventario}/rechazar` | Si | Rechazar ajuste pendiente sin modificar stock. |
| POST | `/api/ventas/{idVenta}/anular` | Si | Anular una venta registrada y restaurar vasos al stock diario. |

## Diferencia entre stock general y stock diario

- El stock general se conserva en `existencias_inventario_general`.
- El stock general no depende de que exista una caja diaria abierta. El gerente puede registrarlo o corregirlo como control general del inventario.
- Al abrir paquetes de vasos, se descuenta el total generado del stock general.
- El total generado entra al stock diario en `existencias_inventario_diario.cantidad_ingresada`.
- Los vasos rotos al abrir paquetes aumentan `cantidad_perdida`.
- Al abrir una nueva caja, el stock diario de cada item de vaso con paquetes se inicia con el remanente de la caja anterior. Por tanto, el siguiente dia no comienza con vasos en cero solo por abrir una caja nueva.
- El saldo diario teorico se calcula con:
  - `cantidad_inicial + cantidad_ingresada - cantidad_vendida - cantidad_perdida + cantidad_ajustada`.

## Regla de caja diaria

- La ausencia de una caja abierta bloquea solo las operaciones de jornada: apertura de paquetes de vasos y consumos diarios.
- Consultar y controlar stock general, movimientos historicos y ajustes no requiere una caja abierta.
- El cierre y el conteo fisico posterior corroboran el saldo calculado contra la existencia fisica. Las diferencias se gestionan con el flujo de ajustes y quedan auditadas; el conteo final completo pertenece al modulo de cierre de caja.

## Reglas para vasos

- Solo items activos con `tipo_control = 'automatico_por_venta'`, `maneja_paquetes = true` y `id_tamano_vaso` pueden registrarse como paquetes abiertos.
- La apertura de paquetes crea movimientos de inventario:
  - salida de stock general por `apertura_paquete`.
  - entrada de stock diario por `apertura_paquete`.
  - salida de stock diario por `perdida` si hay `unidades_rotas`.
- Cada venta descuenta vasos del stock diario y genera movimiento `venta`.
- Cada anulacion restaura vasos al stock diario y genera movimiento `anulacion_venta`.
- No se permite que el stock general o diario quede negativo.

## Reglas para consumos manuales

- Solo administrador o gerente puede registrar paquetes y consumos manuales.
- Los consumos manuales aplican a items con `tipo_control = 'manual_por_consumo'`.
- No se permite registrar consumo manual sobre items automaticos por venta.
- Cada consumo manual descuenta stock general y genera movimiento `consumo_diario`.

## Reglas para ajustes de stock general

- En el alcance actual, el backend solo acepta `tipo_stock = 'general'` para este flujo.
- `administrador` registra una solicitud en `ajustes_inventario` con `estado_aprobacion = 'pendiente'`; esta solicitud no modifica `existencias_inventario_general`.
- `gerente` registra directamente el stock general. Su ajuste queda aplicado con `estado_aprobacion = 'aprobado'`, sin requerir una solicitud propia.
- Solo `gerente` puede aprobar o rechazar solicitudes pendientes de `administrador`.
- Al aprobar un ajuste:
  - Se actualiza `existencias_inventario_general.cantidad_actual` segun `sentido_ajuste`.
  - Se genera un movimiento en `movimientos_inventario` con `tipo_movimiento = 'ajuste'`.
  - El movimiento usa `referencia_origen = 'ajustes_inventario'` e `id_referencia_origen = id_ajuste_inventario`.
- Al rechazar un ajuste no se modifica stock ni se genera movimiento de inventario.
- No se permite aprobar un ajuste que deje stock general negativo.
- La solicitud administrativa, la aplicacion directa del gerente, la aprobacion y el rechazo se registran en `auditoria_operaciones`.
- `vendedor` no dispone de interfaz independiente de Inventario ni puede consultar ajustes.

## Reglas de movimientos

- Todo cambio de stock implementado por el modulo genera registro en `movimientos_inventario`.
- Cada movimiento registra:
  - `tipo_stock`.
  - `tipo_movimiento`.
  - `cantidad`.
  - `sentido_movimiento`.
  - `referencia_origen`.
  - `id_referencia_origen`.
  - `id_usuario_registro`.
- Los movimientos quedan asociados a la caja diaria cuando el proceso corresponde a una operacion diaria.

## Pruebas realizadas

- `mvn clean test`.
- Resultado reportado por el usuario: `BUILD SUCCESS`.
- Rechazo de consulta de inventario sin autenticacion.
- Registro de paquete de vasos con vasos rotos.
- Descuento de stock general al abrir paquetes.
- Creacion de movimientos de apertura de paquete y perdida.
- Registro de consumo manual diario.
- Rechazo de consumo manual para item automatico por venta.
- Solicitud de ajuste de stock general.
- Aprobacion de ajuste de entrada por gerente con actualizacion de stock general.
- Creacion de movimiento `ajuste` al aprobar.
- Rechazo de ajuste sin modificar stock general.
- Rechazo de aprobacion por rol no gerente.
- Rechazo de aprobacion que dejaria stock general negativo.
- Auditoria de solicitud y aprobacion de ajuste.
- Aplicacion directa de ajuste de stock general por gerente.
- Rechazo de consulta de ajustes para `vendedor`.
- Inicializacion de stock diario de vasos con el remanente de la caja anterior.
- Descuento automatico de vasos por venta.
- Restauracion de vasos por anulacion de venta.
- Creacion de movimiento de anulacion de venta.

## Pendientes

- Alertas por cantidad minima quedan pendientes porque `cantidad_minima_alerta` no existe en el schema canonico actual.
- Conteo fisico final y diferencias de cierre se completaran en el modulo "Cierre de caja y deposito".

## Reorganizacion frontend posterior

- `/inventario` se concentra en las operaciones de jornada: stock diario de vasos, apertura de paquetes, consumo manual y ajustes de inventario.
- La pantalla sigue consultando existencias generales en segundo plano para poblar y validar los selectores de paquetes, consumos y ajustes; ya no presenta el listado ni el resumen consolidado de stock general.
- El historial de `movimientos_inventario` ya no se consume ni se renderiza desde `InventarioPanel`. La consulta por fecha se centraliza en `/consultas`, pestaña Inventario, mediante `GET /api/consultas/inventario/movimientos`.
- Los tres formularios operativos comparten una grilla que alinea sus botones de envio en escritorio.
- No se agregaron contadores por tipo de granizado ni se modificaron backend, schema o la formula canonica de stock diario. El stock fisico continua siendo unico por tamano de vaso.
- Validacion tecnica posterior: `npx tsc -b --pretty false`, `npm run build` y navegador confirmaron ausencia del historial en `/inventario` y su disponibilidad en Consultas.
