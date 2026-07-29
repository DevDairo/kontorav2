# 11. Consultas y evidencias

## Objetivo

Centralizar las consultas por periodo y la gestion autorizada de evidencias para no repetir historiales en pantallas independientes.

## Requisitos cubiertos

- RF-10, RF-44, RF-49, RF-57 y RF-58.

## Funcionalidades

- Consulta de ventas y gastos por periodo.
- Filtro de periodo con acción `Consultar` compacta y uniforme con Auditoría;
  en pantallas pequeñas conserva el ancho completo para facilitar la pulsación.
- Apertura de las evidencias de un gasto desde su fila, con vista previa responsive de imagen o PDF y descarga opcional.
- Desplazamiento automatico al visor de la evidencia elegida, respetando la
  preferencia de movimiento reducido.
- Administrador y gerente pueden adjuntar o reintentar evidencias de gastos desde el mismo visor; el archivo seleccionado se conserva si la carga falla.
- Las ventas anuladas se conservan en el listado de ventas con su estado para trazabilidad, pero se excluyen de registros vigentes, total vendido, efectivo y transferencias, sin importar si el pago original fue en efectivo, transferencia o mixto.
- Cada venta muestra su hora, método de pago y un resumen separado: `Tipo de
  vaso` contiene únicamente el tamaño y `Vasos vendidos` contiene únicamente la
  cantidad total.
- Las consultas operativas de transferencias solo consideran pagos de ventas registradas; una venta anulada conserva su evidencia, pero no permanece disponible para decision de transferencia.
- Consulta de inventario actual y movimientos por item o caja.
- Existencias y movimientos muestran el nombre vigente del item registrado en
  Catálogos, conservando su escritura y enlazándolo por `idItemInventario`.
- Indicadores de inventario diario para vendidos, cortesías, pérdidas y
  disponibilidad teórica.
- Vista administrativa de novedades con cortesías y pérdidas por periodo.
- Las novedades de cortesías y pérdidas muestran el nombre vigente del vaso
  registrado en Catálogos; los registros conservan sus identificadores
  originales para trazabilidad.
- Consulta y carga de evidencias de pérdidas, incluso después del cierre.
- Consulta historica de vasos vendidos por jornada, tipo de granizado y tamano de vaso, usando el periodo seleccionado.
- Consulta de cierres por fecha.
- Consulta de historial de deposito y sus movimientos.
- Apertura de evidencias desde consignaciones bancarias y pagos de servicios del historial de deposito; las entradas de cierre se identifican como movimientos sin evidencia aplicable.
- Administrador y gerente pueden adjuntar evidencias de consignaciones y pagos de servicios desde el mismo historial.
- Consulta de datos operativos y financieros sin modificar los registros; la unica escritura disponible en este modulo es adjuntar una evidencia autorizada.

## Permisos

| Rol | Funcionalidad |
| --- | --- |
| Vendedor | Consulta ventas, gastos propios y las evidencias de esos gastos por periodo. |
| Administrador | Consulta ventas, gastos, inventario, cierres y deposito; adjunta soportes de gastos y deposito. |
| Gerente | Tiene la misma consulta operativa completa y gestion documental. |

## Reglas clave

- Los historiales consultados no se modifican. Adjuntar una evidencia crea un soporte documental nuevo y conserva los anteriores.
- Una venta anulada no se elimina del historial: se muestra como evidencia de la operacion, sin afectar los indicadores financieros vigentes ni los valores del cierre de caja. Sus pagos y evidencias permanecen disponibles como trazabilidad.
- Una cortesía o pérdida anulada tampoco se elimina: conserva responsable,
  fecha, motivo, detalles y datos de anulación.
- Inventario y deposito muestran su historial aqui, no en las pantallas de registro.
- El visor de gastos respeta el alcance del backend: administrador y gerente consultan todos; el vendedor solo puede abrir evidencias de gastos que registro.
- Las evidencias del deposito son administrativas y solo aparecen en las vistas permitidas para administrador y gerente.
- El bucket permanece privado: Consultas solicita metadata y archivos exclusivamente a la API autenticada.
- La consulta historica de vasos se agrupa por `fecha_operacion` de la caja; cada jornada conserva sus propios totales aunque se consulte un rango de fechas.
- El desglose de vasos considera solo ventas registradas. Las anuladas no se incluyen porque ya no representan consumo vigente de vasos ni venta efectiva.
- La equivalencia de 20 vasos por paquete solo aparece en el desglose agregado
  de inventario como referencia para el conteo físico; no forma parte del
  resumen de cada venta ni altera stock, movimientos o reglas de negocio.
- Auditoria es una ruta gerencial separada: registra acciones sensibles, no reemplaza los historiales de ventas o movimientos.
- La carga de una evidencia de pérdida es administrativa y no requiere reabrir
  la caja.

## Endpoints principales

- `GET /api/consultas/ventas`
- `GET /api/consultas/gastos`
- `GET /api/consultas/inventario/actual`
- `GET /api/consultas/inventario/movimientos`
- `GET /api/consultas/inventario/ventas-vasos`
- `GET /api/consultas/cierre`
- `GET /api/consultas/deposito/movimientos`
- `GET /api/cortesias`
- `GET /api/inventario/perdidas-vasos`
- `GET /api/evidencias/perdidas-inventario/{idPerdidaInventario}`
- `GET /api/evidencias/gastos-caja/{idGastoCaja}`
- `GET /api/evidencias/consignaciones-bancarias/{idConsignacionBancaria}`
- `GET /api/evidencias/pagos-servicios/{idPagoServicio}`
- `GET /api/evidencias/{idArchivoEvidencia}/descargar`
- `POST /api/evidencias/gastos-caja/{idGastoCaja}`
- `POST /api/evidencias/consignaciones-bancarias/{idConsignacionBancaria}`
- `POST /api/evidencias/pagos-servicios/{idPagoServicio}`
- `POST /api/evidencias/perdidas-inventario/{idPerdidaInventario}`
