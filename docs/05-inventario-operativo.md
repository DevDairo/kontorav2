# 05. Inventario operativo

## Objetivo

Gestionar stock general, stock diario de vasos, consumos manuales, perdidas y ajustes autorizados.

## Requisitos cubiertos

- RF-38 a RF-47.

## Funcionalidades

- Consulta del stock diario de vasos de la caja abierta.
- Navegacion por pestañas independientes para stock diario, carga diaria,
  consumo, devolución al general, vasos rotos, ajuste general y movimientos.
- Encabezado contextual que identifica el objetivo de la pestaña activa.
- Desglose informativo de vasos vendidos en la caja abierta por tipo de granizado y tamano de vaso.
- Orden visual de vasos por capacidad ascendente: 8, 12, 16, 20 y 24 onzas en apertura de paquetes, ingreso de stock general y stock diario.
- Registro de paquetes completos de vasos abiertos, con 20 unidades por paquete.
- Reabastecimiento del stock diario con unidades sueltas ya contabilizadas en el stock general.
- Devolución de vasos cargados por error desde el stock diario al general,
  expresada como paquetes de 20 o vasos por unidad.
- Registro de vasos rotos o perdidos por tamaño, con motivo, confirmación y
  evidencia fotográfica opcional al crear.
- El formulario de paquetes no muestra unidades rotas: registra siempre cero
  para evitar ambiguedades operativas, sin retirar el campo historico del
  contrato backend.
- Descuento automatico de vasos por ventas y restauracion por anulaciones.
- Descuento de vasos por cortesías y pérdidas, con movimientos diferenciados.
- Registro de conteo fisico final, diferencia y cantidad teorica.
- Arrastre del remanente de vasos al abrir la siguiente caja: se conserva el conteo fisico final o, en su ausencia, el saldo teorico.
- Consumo manual de dulces, desechables y bolsas de producto con o sin licor desde stock general.
- Separación del consumo diario por categoría: dulces, desechables, producto con
  licor y producto sin licor.
- Separación del ajuste general por categoría, conservando además los vasos.
- Solicitudes administrativas de entrada o salida del stock general y decisión
  gerencial de aprobar o rechazar.
- Aplicacion directa de entradas o salidas del stock general por gerente.
- Ocultamiento de items inactivos en todos los formularios operativos, sin
  borrar su historial.
- Presentación de categorías e items sin guiones bajos y con capitalización
  legible, manteniendo sus identificadores técnicos sin cambios.
- Sincronización del nombre y la categoría visibles con el registro vigente de
  Catálogos cada vez que se actualiza Inventario.
- La sincronización por `idItemInventario` también cubre stock diario, consumo,
  devoluciones, pérdidas, solicitudes y movimientos. El nombre visible conserva
  exactamente la escritura vigente en Catálogos.
- Los registros históricos y la gestión de evidencias de vasos rotos resuelven
  igualmente el nombre vigente por `idItemInventario`; no reconstruyen la
  etiqueta desde las onzas.
- Stock diario, carga diaria, devolución al general, vasos rotos, ajuste general
  y movimientos muestran únicamente ese nombre canónico; no vuelven a anexar
  `· N oz` cuando el tamaño ya forma parte del nombre.
- Acciones principales de carga diaria, consumo y ajuste general con ancho
  compacto en escritorio, equivalente a `Devolver al general`, y ancho completo
  solo cuando la pantalla pequeña requiere apilarlas.

## Permisos

| Rol | Funcionalidad |
| --- | --- |
| Vendedor | No recibe interfaz independiente de Inventario. |
| Administrador | Registra paquetes, consumos y pérdidas; solicita entradas, salidas, reabastecimientos y devoluciones. |
| Gerente | Ejecuta operaciones directas, gestiona pérdidas y decide solicitudes pendientes. |

## Reglas clave

- El ingreso al stock general no requiere caja abierta.
- El ajuste general permite entrada o salida. El gerente lo aplica directamente;
  el administrador deja una solicitud pendiente.
- Los filtros de categoría solo organizan la interfaz. Las operaciones continúan
  enviando el `idItemInventario` y el backend mantiene todas las validaciones.
- Cambiar el nombre en Catálogos no crea otro producto ni modifica inventario:
  todas las vistas resuelven la etiqueta vigente usando el mismo
  `idItemInventario`.
- Al cambiar de categoría también cambia la selección de item, evitando enviar
  accidentalmente un producto oculto de la categoría anterior.
- La caja abierta condiciona las operaciones diarias: paquetes, unidades
  sueltas, devoluciones, pérdidas, consumos y conteos de vasos.
- El stock diario de vasos no queda vacio por cambio de jornada cuando existe remanente.
- El orden visual de los vasos no modifica existencias, movimientos ni calculos; solo facilita la operacion por tamano.
- El desglose de ventas de vasos de Inventario considera solo ventas registradas de la caja abierta; las ventas anuladas no se incluyen.
- La equivalencia de 20 vasos por paquete se presenta como referencia para el conteo fisico y no modifica existencias, movimientos ni la cantidad teorica.
- Un reabastecimiento con unidades sueltas descuenta atomicamente el stock general e incrementa en igual cantidad el stock diario; por ejemplo, `107 general -> 100 general + 7 diario`.
- Cada reabastecimiento aprobado genera dos movimientos enlazados al mismo ajuste: salida general y entrada diaria. No crea paquetes ficticios ni utiliza unidades rotas.
- Las unidades reabastecidas se suman visualmente a `Ingresada` en el stock
  diario; no se presenta una magnitud separada llamada `Trasladada`.
- La carga neta diaria suma paquetes y ajustes. Una devolución disminuye
  `cantidad_ajustada` y aumenta el stock general por la misma cantidad.
- El gerente aplica el reabastecimiento directamente. El administrador crea una solicitud pendiente que solo modifica existencias cuando un gerente la aprueba y la caja asociada continua abierta.
- El reabastecimiento solo admite vasos configurados por paquetes, cantidades positivas y stock general suficiente.
- La devolución solo admite vasos configurados por paquetes, cantidades
  positivas y stock diario suficiente.
- El selector `Paquetes (20 vasos)` es una ayuda visual: el backend recibe y
  registra siempre la cantidad real de vasos.
- Una pérdida aumenta `cantidad_perdida`, crea movimiento `perdida` y reduce la
  disponibilidad diaria; no vuelve a descontar el stock general.
- La evidencia de una pérdida puede adjuntarse ahora o después del cierre, pero
  una pérdida anulada no recibe soportes nuevos.
- La anulación de una pérdida solo procede con la misma caja abierta y cuando se
  confirma que el vaso no estaba roto ni fue perdido.
- Las cortesías aumentan `cantidad_cortesia`; su anulación válida revierte ese
  contador y restaura el stock diario.
- Un ajuste aprobado no puede dejar el stock general negativo.
- Solicitud, aprobacion y rechazo de ajustes, cortesías, pérdidas y evidencias
  generan auditoria.

## Endpoints principales

- `GET /api/inventario/existencias/general`
- `GET /api/inventario/existencias/diarias/abierta`
- `GET /api/inventario/ventas-vasos/diaria-abierta`
- `POST /api/inventario/paquetes-vasos`
- `POST /api/inventario/consumos-diarios`
- `POST /api/inventario/ajustes`
- `POST /api/inventario/ajustes/{idAjusteInventario}/aprobar`
- `POST /api/inventario/ajustes/{idAjusteInventario}/rechazar`
- `POST /api/inventario/perdidas-vasos`
- `GET /api/inventario/perdidas-vasos/caja-abierta`
- `GET /api/inventario/perdidas-vasos`
- `POST /api/inventario/perdidas-vasos/{idPerdidaInventario}/anular`
- `POST /api/evidencias/perdidas-inventario/{idPerdidaInventario}`
- `GET /api/evidencias/perdidas-inventario/{idPerdidaInventario}`
