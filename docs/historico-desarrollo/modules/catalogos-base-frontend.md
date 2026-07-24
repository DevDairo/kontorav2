# Pantalla: Catalogos y gestion administrativa

## Objetivo

Consultar catalogos base activos para los formularios operativos y permitir a administrador y gerente gestionar items de inventario y vigencias de precios.

## Actor principal

- Consulta: Administrador / Gerente.
- Gestion: Administrador / Gerente.
- Vendedor: sin acceso a la ruta independiente de Catalogos.

## Endpoints consumidos

- `GET /api/catalogos/metodos-pago`
- `GET /api/catalogos/tipos-granizado`
- `GET /api/catalogos/tamanos-vaso`
- `GET /api/catalogos/categorias-inventario`
- `GET /api/catalogos/unidades-medida`
- `GET /api/catalogos/items-inventario`
- `GET /api/catalogos/precios-granizado/vigentes?fecha=YYYY-MM-DD`
- `GET /api/catalogos/promociones/vigentes?fecha=YYYY-MM-DD`
- `GET /api/catalogos/tipos-servicio`
- `GET /api/catalogos/gestion/items-inventario`
- `POST /api/catalogos/gestion/items-inventario`
- `PUT /api/catalogos/gestion/items-inventario/{idItemInventario}`
- `PUT /api/catalogos/gestion/items-inventario/{idItemInventario}/estado`
- `GET /api/catalogos/gestion/precios-granizado`
- `POST /api/catalogos/gestion/precios-granizado`

## Contrato usado

Fuente principal: `docs/modules/catalogos-base.md`.

La pantalla consume catalogos autenticados y conserva los nombres reales expuestos por backend:

- Metodos de pago activos.
- Tipos de granizado activos.
- Tamanos de vaso activos.
- Categorias y unidades de inventario activas.
- Items de inventario activos.
- Precios vigentes por fecha.
- Promociones vigentes por fecha con `diasPromocion`.
- Tipos de servicio activos.

## Campos del formulario

- `fechaVigencia`: fecha usada para consultar precios y promociones vigentes.
- `buscar`: filtro local para precios, promociones e items ya recibidos.
- Gestion de producto: nombre, categoria y unidad de medida; para vasos automaticos, tamano de vaso y paquetes fijos de 20 unidades.
- Gestion de precio: tipo de granizado, tamano, valor y fecha de inicio de vigencia.

## Validaciones de interfaz

- La consulta se realiza con token activo.
- La fecha de vigencia se envia como query param `fecha`.
- El filtro de busqueda no modifica datos ni llama endpoints de escritura.
- Si la API rechaza la consulta, se muestra el mensaje real del backend.
- La pestana `Gestion` solo se muestra a administrador y gerente.
- El formulario permite `manual_por_consumo` para consumibles y `automatico_por_venta` para vasos, conforme a RF-38 y RF-39.
- Un vaso automatico fija la categoria `vasos`, exige un tamano y se envia con paquetes de 20 unidades.
- La vigencia de precio se gestiona aparte por tipo de granizado y tamano; no cambia el control ni las existencias de los vasos.
- El cambio de estado solicita confirmacion antes de invocar el endpoint.
- El valor de precio se ingresa como texto numerico para evitar depender de los controles de incremento del navegador.

## Reglas que no debe duplicar el frontend

- Vigencia definitiva de precios y promociones.
- Filtrado definitivo por estado activo.
- Reglas de aplicacion de promociones en ventas.
- Permisos finales de acceso.
- Restriccion de inactivar con stock, proteccion de estructura con movimientos y escritura de auditoria.

El frontend solo presenta datos maestros y mejora busqueda/seleccion; el backend conserva la autoridad.

## Respuestas esperadas

- Consulta: se muestran conteos y listas de metodos de pago, tipos de granizado, items, precios, promociones y listas base.
- Gestion: el item creado aparece con existencia general inicial en cero; una nueva vigencia aparece en el historial sin sobrescribir el precio anterior.
- Caso con error: se muestra el mensaje devuelto por la API y se permite reintentar.

## Evidencia de prueba

- Build frontend:

```powershell
cd C:\Users\corre\Desktop\Kontora\frontend
npm run build
```

Resultado: exitoso.

- Validacion API real con token de gerente:

```text
GET /api/catalogos/metodos-pago -> 2 registros
GET /api/catalogos/tipos-granizado -> 2 registros
GET /api/catalogos/tamanos-vaso -> 6 registros
GET /api/catalogos/categorias-inventario -> 5 registros
GET /api/catalogos/unidades-medida -> 4 registros
GET /api/catalogos/items-inventario -> 16 registros
GET /api/catalogos/precios-granizado/vigentes?fecha=2026-07-07 -> 12 registros
GET /api/catalogos/promociones/vigentes?fecha=2026-07-07 -> 12 registros
GET /api/catalogos/tipos-servicio -> 5 registros
GET /api/catalogos/gestion/items-inventario -> 16 registros
GET /api/catalogos/gestion/precios-granizado -> 12 registros
```

- Validacion en navegador integrado:

```text
/catalogos muestra las vistas Consulta y Gestion para administrador y gerente
La vista Gestion permite crear items manuales o vasos automaticos, editar nombre, cambiar estado y registrar vigencias de precio
Los vasos automaticos requieren tamano y paquetes fijos de 20 unidades
La configuracion de precios se conserva separada del control de inventario de vasos
```

- Validacion manual pendiente:

```text
Confirmar en `/catalogos` que el control `Vaso por venta` conserva categoria `vasos`, selector de tamano y paquetes fijos de 20 unidades, sin afectar el panel independiente de vigencias de precios.
```

## Pendiente siguiente

- Administracion historica de promociones.

## Actualizacion visual del 2026-07-11

- Se sustituyeron los textos de endpoints visibles por descripciones funcionales de precios, promociones e inventario.
- Los nombres visibles de items, tipos, categorias y servicios se presentan sin guiones bajos y con capitalizacion legible.
- Los paneles de Precios vigentes, Promociones, Inventario activo y Listas base mantienen alturas uniformes en escritorio y se apilan en movil.

## Actualizacion funcional del 2026-07-12

- Se agrego la vista `Gestion` dentro de `/catalogos`, sin retirar la vista de consulta de datos maestros.
- La interfaz permite alta, edicion permitida e inactivacion de items, junto con la consulta y registro de vigencias de precios.
- La alta conserva los dos controles de inventario requeridos: consumo manual y vaso por venta con paquetes de 20 unidades.
- La configuracion de precios permanece en un panel independiente y conserva historial por tipo de granizado y tamano.
- El backend conserva la autoridad sobre roles, stock inicial, inactivacion, historial de precios y auditoria.
