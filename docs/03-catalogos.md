# 03. Catalogos

## Objetivo

Administrar los datos maestros que habilitan la operacion: items de inventario, categorias, tamanos de vaso, precios y promociones.

## Requisitos cubiertos

- RF-16 a RF-20.
- RF-35 a RF-37.
- RF-45, RF-54 y RF-55.

## Funcionalidades

- Consulta de metodos de pago, tipos de granizado, precios vigentes, promociones e items activos.
- Alta, edicion, activacion e inactivacion de items inventariables.
- Identificacion por nombre de dulces, desechables y bolsas de producto para su inventario posterior.
- Filtro por categoría en la administración de items: desechables, dulces,
  producto con licor y producto sin licor, combinado con la búsqueda textual y
  el conteo por grupo.
- Los vasos automáticos permanecen ocultos en la administración de items para
  proteger su configuración estructural. Los tamaños continúan disponibles en
  la gestión de precios y en los módulos operativos autorizados.
- Control de stock general para todos los items creados.
- Configuracion de cantidades minimas para alertas de inventario.
- Registro de nuevas vigencias de precio por tipo de granizado y tamano de vaso, sin alterar ventas historicas.
- Los vasos disponibles mantienen tamanos fijos; se modifican precios, no se crean tamanos desde el flujo de precios.
- Los tipos, categorías e items se presentan con nombres legibles en selectores
  e historiales, sin alterar el valor técnico almacenado.
- El nombre escrito para un item se conserva como etiqueta canónica: las demás
  pantallas lo muestran con las mismas mayúsculas, minúsculas y palabras, sin
  volver a transformarlo visualmente.
- El nombre vigente del vaso también se usa en los selectores de Ventas,
  Cortesías, Inventario y pérdidas. El tamaño en onzas se conserva como dato
  estructural para precios, promociones, resúmenes y validaciones.
- Inventario, movimientos y pérdidas resuelven el nombre por
  `idItemInventario`. Ventas y Cortesías enlazan cada `idTamanoVaso` con su item
  automático activo para obtener la misma etiqueta sin cambiar el contrato de
  registro.
- En particular, `producto_con_licor` y `producto_sin_licor` se muestran como
  `Producto Con Licor` y `Producto Sin Licor`: sin guiones bajos y con cada
  palabra capitalizada.
- Un item inactivado deja de aparecer en los formularios de carga, consumo,
  devolución, pérdida, ajuste de inventario, Ventas y Cortesías.
- La inactivación no elimina existencias, movimientos ni referencias
  históricas.

## Permisos

| Rol | Funcionalidad |
| --- | --- |
| Vendedor | No recibe interfaz de Catalogos. |
| Administrador | Consulta y administra items y configuraciones autorizadas. |
| Gerente | Tiene las mismas capacidades administrativas. |

## Reglas clave

- Un item con historial o stock no se elimina fisicamente.
- La acción operativa para retirar un producto del uso diario es inactivarlo.
  Si se reactiva, vuelve a aparecer en los formularios autorizados.
- Los vasos usan control `automatico_por_venta`; los demas consumibles usan `manual_por_consumo`.
- Un paquete de vasos contiene 20 unidades.
- Los cambios de precio conservan su vigencia historica y se auditan.

## Endpoints principales

- `GET /api/catalogos/items-inventario`
- `GET /api/catalogos/gestion/items-inventario`
- `POST /api/catalogos/gestion/items-inventario`
- `PUT /api/catalogos/gestion/items-inventario/{idItemInventario}`
- `PUT /api/catalogos/gestion/items-inventario/{idItemInventario}/estado`
- `GET /api/catalogos/precios-granizado/vigentes`
- `POST /api/catalogos/gestion/precios-granizado`
