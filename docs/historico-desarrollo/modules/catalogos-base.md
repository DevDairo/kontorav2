# Modulo: Catalogos base y gestion administrativa

## Objetivo

Exponer datos maestros requeridos por los modulos operativos y permitir al administrador o gerente gestionar consumibles y vigencias de precios sin alterar el historial operativo.

## Tablas involucradas

- `roles`
- `metodos_pago`
- `tipos_granizado`
- `tamanos_vaso`
- `precios_granizado`
- `promociones`
- `dias_promocion`
- `categorias_inventario`
- `unidades_medida`
- `items_inventario`
- `tipos_servicio`

## Fuente de verdad del modelo

El modulo se implementa sobre las tablas reales del schema:

- Los catalogos operativos devuelven solo registros con `estado = 'activo'`.
- Los precios vigentes se filtran por `precios_granizado.fecha_inicio_vigencia`, `fecha_fin_vigencia` y `estado`.
- Las promociones vigentes se filtran por `promociones.fecha_inicio_vigencia`, `fecha_fin_vigencia` y `estado`.
- `dias_promocion` se devuelve como configuracion asociada a cada promocion.
- `items_inventario` no incluye `cantidad_minima_alerta` porque esa columna no existe en `kontora_pos_schema.txt`.

## Endpoints

| Metodo | Ruta | Autenticacion | Proposito |
| :- | :- | :- | :- |
| GET | `/api/catalogos/roles` | Si | Consultar roles activos. |
| GET | `/api/catalogos/metodos-pago` | Si | Consultar metodos reales de pago. |
| GET | `/api/catalogos/tipos-granizado` | Si | Consultar tipos de granizado activos. |
| GET | `/api/catalogos/tamanos-vaso` | Si | Consultar tamanos de vaso activos. |
| GET | `/api/catalogos/categorias-inventario` | Si | Consultar categorias de inventario activas. |
| GET | `/api/catalogos/unidades-medida` | Si | Consultar unidades de medida activas. |
| GET | `/api/catalogos/items-inventario` | Si | Consultar items activos para operaciones normales. |
| GET | `/api/catalogos/precios-granizado/vigentes` | Si | Consultar precios vigentes. |
| GET | `/api/catalogos/promociones/vigentes` | Si | Consultar promociones vigentes. |
| GET | `/api/catalogos/tipos-servicio` | Si | Consultar tipos de servicio activos. |
| GET | `/api/catalogos/gestion/items-inventario` | Administrador / Gerente | Consultar items activos e inactivos para gestion. |
| POST | `/api/catalogos/gestion/items-inventario` | Administrador / Gerente | Crear un consumible manual o un vaso automatico con existencia general inicial en cero. |
| PUT | `/api/catalogos/gestion/items-inventario/{idItemInventario}` | Administrador / Gerente | Editar datos permitidos de un item. |
| PUT | `/api/catalogos/gestion/items-inventario/{idItemInventario}/estado` | Administrador / Gerente | Activar o inactivar un item bajo sus restricciones. |
| GET | `/api/catalogos/gestion/precios-granizado` | Administrador / Gerente | Consultar el historial de precios. |
| POST | `/api/catalogos/gestion/precios-granizado` | Administrador / Gerente | Registrar una nueva vigencia de precio. |

## Filtros

- `GET /api/catalogos/precios-granizado/vigentes?fecha=YYYY-MM-DD`
- `GET /api/catalogos/promociones/vigentes?fecha=YYYY-MM-DD`

Si `fecha` no se envia, se usa la fecha actual del backend.

## Reglas de vigencia

- Un precio es vigente si:
  - `estado = 'activo'`.
  - `fecha_inicio_vigencia <= fecha`.
  - `fecha_fin_vigencia IS NULL` o `fecha_fin_vigencia >= fecha`.
  - Tipo de granizado y tamano de vaso asociados estan activos.
- Una promocion es vigente si:
  - `estado = 'activo'`.
  - `fecha_inicio_vigencia <= fecha`.
  - `fecha_fin_vigencia IS NULL` o `fecha_fin_vigencia >= fecha`.
  - Tipo de granizado y tamano de vaso asociados estan activos.
- Las reglas de aplicacion por tipo de comprador y dia de semana se completaran en el modulo de ventas y pagos.

## Reglas de negocio implementadas

- Solo usuarios autenticados pueden consultar catalogos internos.
- Los items inactivos no aparecen en consultas operativas normales.
- Los precios vigentes no incluyen tipos de granizado ni tamanos inactivos.
- Las promociones vigentes incluyen sus dias configurados en `dias_promocion`.
- No se agregan migraciones nuevas: el schema canonico ya contiene las tablas y datos iniciales.
- La gestion de items y precios solo admite los roles `administrador` y `gerente`; el vendedor no puede acceder a las rutas administrativas.
- Un item nuevo se crea activo con una fila en `existencias_inventario_general` en cantidad `0`.
- Los consumibles manuales usan `tipo_control = manual_por_consumo` y no manejan paquetes. Los vasos automaticos usan `tipo_control = automatico_por_venta`, categoria `vasos`, un tamano activo y paquetes fijos de 20 unidades.
- No se permite inactivar un item mientras su existencia general sea distinta de cero.
- Si un item ya posee movimientos de inventario, no se permite modificar su categoria, unidad, tipo de control, tamano de vaso ni configuracion de paquetes. El nombre puede actualizarse.
- La interfaz administrativa conserva ambos controles de item definidos por los requisitos. Los vasos automaticos participan en RF-38 y RF-39 para inventario diario y ventas; no se eliminan ni se reclasifican por cambios de precio.
- Una nueva vigencia cierra el precio abierto anterior el dia previo a su inicio y crea un nuevo registro. Las ventas historicas no se modifican.
- La creacion, edicion, cambio de estado y cambio de precio dejan auditoria mediante `auditoria_operaciones`.

## Pruebas realizadas

- `mvn clean test`.
- Rechazo de consulta sin autenticacion.
- Consulta de roles activos.
- Consulta de metodos de pago activos.
- Consulta de tipos de granizado activos.
- Consulta de tamanos de vaso activos.
- Consulta de tipos de servicio activos.
- Consulta de precios vigentes.
- Consulta de promociones vigentes con dias asociados.
- Verificacion de que un item inactivo no aparece en operaciones normales.
- `GestionCatalogosIntegrationTest`: alta con existencia inicial en cero, edicion de nombre, inactivacion con existencia cero y auditoria.
- `GestionCatalogosIntegrationTest`: rechazo de inactivacion con stock y de cambios estructurales despues de movimientos.
- `GestionCatalogosIntegrationTest`: nueva vigencia, cierre del precio anterior, conservacion de historial y auditoria.
- `GestionCatalogosIntegrationTest`: rechazo al vendedor con `403`.

## Pendientes

- La administracion historica de promociones se definira en un flujo administrativo posterior.
- La aplicacion exacta de promociones por venta se implementara en el modulo "Ventas y pagos".
