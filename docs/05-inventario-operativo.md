# 05. Inventario operativo

## Objetivo

Gestionar stock general, stock diario de vasos, consumos manuales, perdidas y ajustes autorizados.

## Requisitos cubiertos

- RF-38 a RF-47.

## Funcionalidades

- Consulta del stock diario de vasos de la caja abierta.
- Navegacion por pestañas independientes para stock diario, carga diaria, consumo, ingreso general y movimientos.
- Encabezado contextual que identifica el objetivo de la pestaña activa.
- Desglose informativo de vasos vendidos en la caja abierta por tipo de granizado y tamano de vaso.
- Orden visual de vasos por capacidad ascendente: 8, 12, 16, 20 y 24 onzas en apertura de paquetes, ingreso de stock general y stock diario.
- Registro de paquetes completos de vasos abiertos, con 20 unidades por paquete.
- Reabastecimiento del stock diario con unidades sueltas ya contabilizadas en el stock general.
- El formulario de paquetes no muestra unidades rotas: registra siempre cero
  para evitar ambiguedades operativas, sin retirar el campo historico del
  contrato backend.
- Descuento automatico de vasos por ventas y restauracion por anulaciones.
- Registro de conteo fisico final, diferencia y cantidad teorica.
- Arrastre del remanente de vasos al abrir la siguiente caja: se conserva el conteo fisico final o, en su ausencia, el saldo teorico.
- Consumo manual de dulces, desechables y bolsas de producto con o sin licor desde stock general.
- Solicitudes administrativas de ingreso al stock general y decision gerencial de aprobar o rechazar.
- Aplicacion directa de ingresos al stock general por gerente.

## Permisos

| Rol | Funcionalidad |
| --- | --- |
| Vendedor | No recibe interfaz independiente de Inventario. |
| Administrador | Registra paquetes y consumos; solicita ingresos al stock general y reabastecimientos del stock diario. |
| Gerente | Ejecuta las operaciones administrativas y aprueba, rechaza o aplica ingresos y reabastecimientos. |

## Reglas clave

- El ingreso al stock general no requiere caja abierta.
- La interfaz de ingreso general registra exclusivamente movimientos de entrada; no presenta correcciones ni salidas.
- La caja abierta condiciona las operaciones diarias: paquetes, unidades sueltas, consumos y conteos de vasos.
- El stock diario de vasos no queda vacio por cambio de jornada cuando existe remanente.
- El orden visual de los vasos no modifica existencias, movimientos ni calculos; solo facilita la operacion por tamano.
- El desglose de ventas de vasos de Inventario considera solo ventas registradas de la caja abierta; las ventas anuladas no se incluyen.
- La equivalencia de 20 vasos por paquete se presenta como referencia para el conteo fisico y no modifica existencias, movimientos ni la cantidad teorica.
- Un reabastecimiento con unidades sueltas descuenta atomicamente el stock general e incrementa en igual cantidad el stock diario; por ejemplo, `107 general -> 100 general + 7 diario`.
- Cada reabastecimiento aprobado genera dos movimientos enlazados al mismo ajuste: salida general y entrada diaria. No crea paquetes ficticios ni utiliza unidades rotas.
- Las unidades reabastecidas se suman visualmente a `Ingresada` en el stock
  diario; no se presenta una magnitud separada llamada `Trasladada`.
- El gerente aplica el reabastecimiento directamente. El administrador crea una solicitud pendiente que solo modifica existencias cuando un gerente la aprueba y la caja asociada continua abierta.
- El reabastecimiento solo admite vasos configurados por paquetes, cantidades positivas y stock general suficiente.
- Un ajuste aprobado no puede dejar el stock general negativo.
- Solicitud, aprobacion y rechazo de ajustes generan auditoria.

## Endpoints principales

- `GET /api/inventario/existencias/general`
- `GET /api/inventario/existencias/diarias/abierta`
- `GET /api/inventario/ventas-vasos/diaria-abierta`
- `POST /api/inventario/paquetes-vasos`
- `POST /api/inventario/consumos-diarios`
- `POST /api/inventario/ajustes`
- `POST /api/inventario/ajustes/{idAjusteInventario}/aprobar`
- `POST /api/inventario/ajustes/{idAjusteInventario}/rechazar`
