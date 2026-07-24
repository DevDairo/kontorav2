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
- Control de stock general para todos los items creados.
- Configuracion de cantidades minimas para alertas de inventario.
- Registro de nuevas vigencias de precio por tipo de granizado y tamano de vaso, sin alterar ventas historicas.
- Los vasos disponibles mantienen tamanos fijos; se modifican precios, no se crean tamanos desde el flujo de precios.
- Los tipos de granizado se presentan con nombres legibles en selectores e historiales, sin alterar el valor tecnico almacenado.

## Permisos

| Rol | Funcionalidad |
| --- | --- |
| Vendedor | No recibe interfaz de Catalogos. |
| Administrador | Consulta y administra items y configuraciones autorizadas. |
| Gerente | Tiene las mismas capacidades administrativas. |

## Reglas clave

- Un item con historial o stock no se elimina fisicamente.
- Los vasos usan control `automatico_por_venta`; los demas consumibles usan `manual_por_consumo`.
- Un paquete de vasos contiene 20 unidades.
- Los cambios de precio conservan su vigencia historica y se auditan.

## Endpoints principales

- `GET /api/catalogos/items-inventario`
- `POST /api/catalogos/items-inventario`
- `PUT /api/catalogos/items-inventario/{idItemInventario}`
- `PUT /api/catalogos/items-inventario/{idItemInventario}/estado`
- `GET /api/catalogos/precios-granizado/vigentes`
- `POST /api/catalogos/precios-granizado`
