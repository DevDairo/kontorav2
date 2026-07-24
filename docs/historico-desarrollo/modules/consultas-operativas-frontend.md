# Pantalla: Consultas operativas frontend

## Estado

Desarrollada e integrada contra el backend real. La compilacion y las comprobaciones tecnicas en navegador finalizaron correctamente; queda pendiente la confirmacion manual final del usuario antes de marcar la ruta como `Base lista`.

## Objetivo

Centralizar consultas internas de solo lectura por periodo, sin duplicar historiales dentro de las pantallas que registran operaciones.

## Actores y visibilidad

- `vendedor`: solo pestañas de Ventas y Gastos, con la restriccion de registros propios aplicada por backend.
- `administrador` y `gerente`: Ventas, Gastos, Inventario, Cierre y Deposito.
- Inventario, Cierre y Deposito no se renderizan como vistas internas para `vendedor`; el backend conserva la autorizacion final.

## Archivos principales

```text
frontend/src/modules/consultas/components/ConsultasPanel.tsx
frontend/src/modules/consultas/services/consultasService.ts
frontend/src/modules/consultas/types.ts
frontend/src/modules/consultas/index.ts
frontend/src/App.tsx
frontend/src/app/routes/appRoutes.ts
frontend/src/index.css
```

## Endpoints consumidos

- `GET /api/consultas/ventas?fechaInicio&fechaFin`.
- `GET /api/consultas/gastos?fechaInicio&fechaFin`.
- `GET /api/consultas/inventario/actual`.
- `GET /api/consultas/inventario/movimientos?fechaInicio&fechaFin`.
- `GET /api/consultas/cierre?fecha`.
- `GET /api/consultas/deposito/movimientos?fechaInicio&fechaFin`.

## Comportamiento de interfaz

- El periodo se edita en los controles de fecha y solo se aplica al pulsar `Consultar`; cambiar una fecha no dispara una consulta intermedia.
- Las consultas no realizan escrituras ni simulan saldos.
- Un `404` de cierre se presenta como una jornada sin cierre registrado, sin tratarlo como error de pantalla.
- Inventario concentra las existencias actuales y los movimientos por periodo; la operacion diaria de paquetes, consumos y ajustes queda en `/inventario`.
- Deposito concentra su historial por periodo, con `Landmark` para entradas por cierre y `Building2` para salidas de consignacion o pago de servicio. Cada registro muestra importe, saldo posterior, usuario, fecha y datos de servicio u observacion cuando existen.
- La ruta `/deposito` conserva saldo y formularios de consignacion y pago de servicio, sin duplicar el historial.

## Limite documentado

Los requisitos separan granizados con licor y sin licor, pero el stock diario canonico de vasos se controla por tamano. No se implemento un contador frontend por tipo de granizado: la consulta actual de ventas no expone sus detalles por tipo y tamano, y no se modificaron backend ni schema para preservar las reglas ya validadas.

## Validacion tecnica realizada

- `npx tsc -b --pretty false`: exitoso.
- `npm run build`: exitoso.
- Navegador con gerente: ventas por `$24.000`, inventario actual, estado sin cierre, movimientos de deposito y manejo de estados vacios verificados contra el backend activo.
- Navegador: `/inventario` ya no renderiza ni consulta movimientos; `/deposito` ya no renderiza ni consulta historial; ambos historiales se visualizan desde `/consultas`.
- Se verifico que el registro de deposito renderiza un SVG para una entrada real por cierre.
- La barra de periodo se unifico visualmente con los filtros de Catalogos, Transferencias y Evidencias.

## Pendiente de cierre

- Confirmacion manual final del usuario sobre filtros de fecha y visibilidad de vendedor, administrador y gerente.
- Actualizar el estado de la ruta `consultas` a `base` solo despues de esa confirmacion.
