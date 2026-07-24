# Pantallas frontend

## Pantalla: Login

### Objetivo

Permitir el inicio de sesion contra la API real del backend y abrir la shell protegida solo cuando la sesion sea valida.

### Actor principal

Vendedor / Administrador / Gerente.

### Endpoint consumido

- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/logout`

### Campos del formulario

- `nombreUsuario`
- `contrasena`

### Validaciones de interfaz

- Usuario y contrasena son obligatorios antes de enviar.
- El error real de API se muestra al usuario cuando el backend rechaza el login.
- Sin token valido se muestra `/login`.
- Con token valido se muestra la shell principal.
- El logout limpia la sesion local y vuelve a `/login`.
- En escritorio el panel derecho usa un patron diagonal continuo sobre fondo `#f5f8fc`; en movil se oculta.

### Respuestas esperadas

- Caso exitoso: el backend devuelve JWT y datos del usuario; el frontend abre la shell correspondiente al rol.
- Caso con error: el backend devuelve `mensaje` y el formulario conserva el estado no autenticado.

### Evidencia de prueba

- `npm run build`.
- Login, `GET /api/auth/me` y logout validados contra backend real en `http://localhost:8080/api`.
- Flujo validado en navegador integrado con el fixture local `test_auth_activo`.
- Pendiente confirmacion manual final del usuario en navegador.

## Pantalla: Inicio de inicializacion (referencia tecnica)

### Objetivo

Validar que la app React carga y que puede consultar el endpoint publico de salud del backend.

### Actor principal

Equipo de desarrollo.

### Endpoint consumido

- `GET /api/health`

### Campos del formulario

- No aplica.

### Validaciones de interfaz

- El endpoint se conserva para comprobaciones tecnicas del entorno.
- No se presenta como tarjeta ni indicador visible en la interfaz operativa.

### Respuestas esperadas

- Caso exitoso: `status = ok` y `service = kontora-pos-backend`.
- Caso con error: se muestra el mensaje de error HTTP o de conexion.

### Evidencia de prueba

- `npm run build`.
- Consulta manual contra `http://localhost:8080/api/health` cuando el backend este activo.

## Pantalla: Layout principal por rol

### Objetivo

Mostrar la shell principal despues de login y presentar navegacion visible segun el rol autenticado.

### Actor principal

Vendedor / Administrador / Gerente.

### Endpoint consumido

- `GET /api/auth/me`
- `POST /api/auth/logout`

La pantalla no expone endpoints de negocio ni indicadores tecnicos en la interfaz.

### Campos del formulario

- No aplica.

### Validaciones de interfaz

- Sin sesion valida se muestra `/login`.
- Con sesion valida se muestra el panel correspondiente a `nombreRol`.
- `vendedor` ve navegacion operativa: ventas, caja, gastos, transferencias y consultas. No ve interfaces independientes de Inventario, Catalogos ni Evidencias.
- `administrador` ve Inventario y Catalogos, ademas de las rutas administrativas de cierre, deposito, evidencias y auditoria.
- `gerente` ve navegacion gerencial con visibilidad administrativa completa, incluida la gestion de Usuarios.
- El frontend no decide permisos finales; el backend sigue siendo autoridad.
- En escritorio el menu lateral permanece visible; en movil se abre desde el icono de barras y se cierra al navegar.
- Usuario y cierre de sesion permanecen en la esquina superior derecha.

### Respuestas esperadas

- Caso exitoso: la shell muestra usuario, rol y menu visible segun rol.
- Caso con token invalido: el frontend limpia sesion y vuelve a `/login`.

### Evidencia de prueba

- `npm run build`.
- Login validado contra backend real con roles `vendedor`, `administrador` y `gerente`.
- Navegacion visible revisada en navegador integrado.
- Consola del navegador sin errores ni advertencias.
- Verificacion manual del usuario completada antes de documentar el cierre.

## Pantalla: Panel de caja abierta

### Objetivo

Consultar la caja diaria abierta desde la API real y mostrar su estado dentro de la shell autenticada.

### Actor principal

Administrador / Gerente.

### Endpoint consumido

- `GET /api/cajas-diarias/abierta`
- `POST /api/cajas-diarias`

### Campos del formulario

Solo cuando no existe caja abierta y el rol visible es `administrador` o `gerente`:

- `fechaOperacion`
- `valorBase`
- `observaciones`

### Validaciones de interfaz

- La consulta usa el token de sesion confirmado por `/api/auth/me`.
- Si existe caja abierta, se muestran los datos reales del backend.
- Si no existe caja abierta, `vendedor` ve un mensaje de solo lectura.
- Si no existe caja abierta, `administrador` y `gerente` ven formulario de apertura.
- `valorBase` no puede ser negativo en la validacion de interfaz.
- La autorizacion final para abrir caja queda en backend.

### Respuestas esperadas

- Caja abierta: se muestra estado, fecha de operacion, valor base, fecha de apertura, usuario de apertura y observaciones.
- Sin caja abierta: se muestra mensaje o formulario segun rol.
- Error backend: se muestra el mensaje devuelto por la API.

### Evidencia de prueba

- `npm run build`.
- `GET /api/cajas-diarias/abierta` validado contra backend real.
- Panel `/caja` validado en navegador integrado con `vendedor` y `administrador`.
- Consola del navegador sin errores ni advertencias.
- Verificacion manual del usuario completada antes de documentar el cierre.

## Pantalla: Registro de ventas y pagos

### Objetivo

Registrar ventas con catalogos reales, promociones vigentes y pagos en efectivo, transferencia o modalidad mixta.

### Actor principal

Vendedor / Administrador / Gerente.

### Endpoints consumidos

- `POST /api/ventas`
- `POST /api/evidencias/pagos-venta/{idPagoVenta}`
- Endpoints de catalogos necesarios para el formulario.

### Campos del formulario

- `tipoComprador`
- `idUsuarioComprador` cuando el comprador es trabajador
- Tipo de granizado, tamano y cantidad
- Metodo de pago y valores recibidos o transferidos
- Comprobante opcional para transferencia

### Validaciones de interfaz

- El total de pagos no puede ser inferior ni superior al total de la venta.
- El efectivo recibido no puede ser menor al efectivo que se registra como pago.
- La transferencia pura informa faltante o excedente y bloquea el envio si el valor no coincide.
- En pago mixto, el payload aplica solo el valor necesario de efectivo y transferencia; el excedente de efectivo se presenta como cambio.
- La evidencia se envia despues de registrar una venta con transferencia y nunca se carga directamente a Supabase desde el navegador.

### Evidencia de prueba

- `npx tsc -b --pretty false` y `npm run build` completados correctamente.
- Navegador validado con catalogos reales, pago en efectivo, transferencia y pago mixto; consola sin errores ni advertencias.
- Efectivo por `10000` sobre una venta de `8000`: backend devolvio cambio de `2000`.
- Pago mixto con transferencia de `6000` y efectivo recibido de `3000`: la interfaz registro `6000` por transferencia y `2000` por efectivo, con cambio de `1000`.
- Los valores de transferencia por debajo o por encima del total se bloquearon antes de enviar el formulario.
- La subida local de evidencia devolvio `503`, `Supabase Storage no esta configurado`; es el comportamiento esperado hasta configurar el almacenamiento en el backend de despliegue.
- El usuario confirmo manualmente la interfaz el 2026-07-09.

## Pantalla: Inventario operativo

### Objetivo

Gestionar stock diario de vasos, apertura de paquetes, consumos manuales y ajustes auditables desde contratos reales del backend.

### Actor principal

Administrador / Gerente.

### Endpoints consumidos

- `GET /api/inventario/existencias/general`
- `GET /api/inventario/existencias/diarias/abierta`
- `POST /api/inventario/paquetes-vasos`
- `POST /api/inventario/consumos-diarios`
- `GET /api/inventario/ajustes`
- `POST /api/inventario/ajustes`
- `POST /api/inventario/ajustes/{idAjusteInventario}/aprobar`
- `POST /api/inventario/ajustes/{idAjusteInventario}/rechazar`

### Validaciones de interfaz

- Gerente registra y aplica directamente ajustes de stock general, incluso si no hay caja diaria abierta.
- Administrador solicita ajustes; solo gerente puede aprobarlos o rechazarlos.
- Sin caja abierta, la pantalla conserva ajustes y deshabilita apertura de paquetes y consumos diarios.
- El stock diario del nuevo dia conserva el remanente del dia anterior por item de vaso; no se inicializa vacio por el cambio de caja.
- Vendedor no ve la ruta ni puede acceder a `/inventario`; el backend conserva la autoridad final de permisos.
- El listado consolidado de stock general y los movimientos se consultan desde `/consultas`; Inventario conserva las existencias generales solo para seleccionar y validar operaciones.

### Evidencia de prueba

- `npx tsc -b --pretty false` y `npm run build` completados correctamente.
- Pruebas backend focalizadas de Inventario y Caja: 16 pruebas, sin fallos ni errores.
- `GET /api/inventario/ajustes` respondio `200` para administrador y gerente, y `403` para vendedor.
- Se verifico con datos controlados una solicitud de administrador, su aprobacion y rechazo por gerente, y una aplicacion directa del gerente.
- La validacion en navegador cubrio gerente, administrador y vendedor; la verificacion manual final del usuario fue confirmada el 2026-07-09.

## Pantalla: Gastos y pago diario a trabajadores

### Objetivo

Registrar gastos de la jornada y administrar el pago total diario a trabajadores sin duplicar la proyeccion financiera que se consulta desde Caja.

### Actor principal

Vendedor / Administrador / Gerente.

### Endpoints consumidos

- `GET` y `POST /api/operaciones-caja/gastos-caja/abierta` y `/api/operaciones-caja/gastos-caja`.
- `PUT /api/operaciones-caja/gastos-caja/{idGastoCaja}`.
- `POST /api/operaciones-caja/gastos-caja/{idGastoCaja}/anular`.
- `GET` y `POST /api/operaciones-caja/pagos-trabajadores-diarios/abierta` y `/api/operaciones-caja/pagos-trabajadores-diarios`.
- `POST /api/operaciones-caja/pagos-trabajadores-diarios/{idPagoTrabajadoresDiario}/confirmar`.

### Validaciones de interfaz

- Vendedor registra y consulta gastos, pero no ve pago a trabajadores ni acciones de edicion o anulacion.
- Administrador y gerente pueden editar o anular gastos, con el motivo obligatorio que exige el backend.
- Administrador y gerente registran, confirman o actualizan el pago diario a trabajadores mientras la caja este abierta.
- Un pago en cero requiere confirmacion explicita antes de enviarse.
- Los importes aceptan teclado o pegado y normalizan separadores decimales o de miles antes de enviar el valor numerico.
- Los controles se deshabilitan cuando no hay caja abierta; backend conserva la autorizacion y la validacion definitiva.

### Evidencia de prueba

- `npx tsc -b --pretty false` y `npm run build` completados correctamente.
- `/gastos` respondio `200` desde el servidor React local.
- El usuario confirmo manualmente el flujo, el pago a trabajadores para administrador y gerente, la ausencia de ese panel para vendedor y la uniformidad visual final.

## Pantalla: Operaciones financieras de caja

### Objetivo

Presentar el estado financiero de la caja abierta y permitir registrar adiciones sin convertir Caja en una segunda interfaz de gastos o pago a trabajadores.

### Actor principal

Administrador / Gerente.

### Endpoints consumidos

- `GET /api/cajas-diarias/abierta/resumen`.
- `GET` y `POST /api/operaciones-caja/adiciones-diarias/abierta` y `/api/operaciones-caja/adiciones-diarias`.
- `GET /api/operaciones-caja/pagos-trabajadores-diarios/abierta` como dato de lectura para la proyeccion.

### Validaciones de interfaz

- La proyeccion usa ventas en efectivo, adiciones, gastos activos y pago a trabajadores calculados por backend.
- Transferencias y base de caja se muestran por separado porque no corresponden al efectivo fisico disponible para deposito.
- La proyeccion informa los requisitos pendientes para cierre, sin reemplazar las validaciones del endpoint de cierre.
- Vendedor no ve esta superficie administrativa.

### Evidencia de prueba

- `GET /api/cajas-diarias/abierta/resumen` respondio `200` para administrador y gerente, y `403` para vendedor.
- `/caja` respondio `200` desde el servidor React local.
- El usuario confirmo manualmente la distribucion final entre Caja y Gastos.

## Pantalla: Cierre de caja y deposito

### Objetivo

Consolidar el efectivo de la jornada abierta, registrar el conteo fisico sin base y consultar resultados de cierre persistidos por fecha.

### Actor principal

Administrador / Gerente.

### Endpoints consumidos

- `GET /api/cajas-diarias/abierta`.
- `GET /api/cajas-diarias/abierta/resumen`.
- `POST /api/cajas-diarias/{idCajaDiaria}/cerrar`.
- `GET /api/cajas-diarias/{idCajaDiaria}/cierre`.
- `GET /api/consultas/cierre?fecha={fechaOperacion}`.

### Campos y controles

- `efectivoContadoSinBase`.
- `observaciones` opcionales.
- Confirmacion de que el conteo no incluye la base de caja.
- Confirmacion previa al cierre.
- Consulta de cierre por fecha y retorno a la operacion actual.

### Validaciones de interfaz

- Solo se muestra para administrador y gerente.
- El efectivo contado es numerico, no negativo y no incluye la base de caja.
- El boton permanece deshabilitado hasta que backend confirme adiciones y pago a trabajadores para el cierre.
- La confirmacion puede cancelarse sin enviar la solicitud.
- El historial se consulta a backend y permanece disponible despues de recargar.
- La jornada puede cerrar despues de medianoche; la fecha de operacion no se reemplaza por la fecha fisica del cierre.

### Respuestas esperadas

- Cierre exitoso: arqueo, diferencia y valor a deposito calculados por backend.
- Deposito positivo: movimiento creado con saldo anterior y posterior.
- Deposito en cero: resultado sin movimiento de deposito.
- Consulta historica: resultado recuperado para la jornada seleccionada.
- Error: mensaje real de backend sin borrar los datos del formulario.

### Evidencia de prueba

- `mvn clean test`: 60 pruebas sin fallos ni errores.
- `npx tsc -b --pretty false` y `npm run build`: exitosos.
- Backend Docker y rutas `/caja` y `/cierre`: respuesta `200`.
- El usuario confirmo manualmente el flujo de cierre, persistencia, historial, valor base inicial editable y cuadros de confirmacion.

## Pantalla: Deposito, consignaciones y servicios

### Objetivo

Consultar el saldo real acumulado por cierres de caja y registrar las salidas administrativas de consignaciones y pagos de servicios.

### Actor principal

Administrador / Gerente.

### Endpoints consumidos

- `GET /api/deposito/saldo`.
- `POST /api/deposito/consignaciones-bancarias`.
- `POST /api/deposito/pagos-servicios`.
- `GET /api/catalogos/tipos-servicio`.
- Endpoints de Evidencias para consignaciones y pagos de servicios.

### Campos y controles

- Saldo actual devuelto por backend.
- `valorConsignado`, `observacion` y evidencia de consignacion.
- `idTipoServicio`, `valorPagado`, `descripcion` y evidencia de pago de servicio.
- Confirmacion antes de cada salida y accion de reintento de evidencia pendiente.

### Validaciones de interfaz

- Solo se muestra para administrador y gerente; vendedor no ve la ruta.
- Cada importe es positivo y no puede exceder el saldo devuelto por backend.
- Una consignacion o pago no se envia hasta seleccionar evidencia y confirmar la operacion.
- El saldo se actualiza desde backend despues de crear la salida.
- La falta de Supabase Storage local no revierte el registro financiero: se informa evidencia pendiente y se conserva su reintento.
- El historial por periodo se consulta desde `/consultas`, pestaña Deposito, para evitar duplicar movimientos en una pantalla de registro.

### Evidencia de prueba

- `mvn clean test`: 64 pruebas sin fallos ni errores.
- `npx tsc -b --pretty false` y `npm run build`: exitosos.
- Administrador cerro una caja de prueba con `$50.000` sin base, generando `entrada_cierre` por el mismo valor.
- Pago de servicio de `$2.500` y consignacion de `$28.000` dejaron saldo disponible de `$19.500`, coincidente con historial y backend.
- Gerente inicio sesion y visualizo la ruta y el panel `/deposito` con sus controles habilitados.
- El usuario confirmo manualmente la validacion del modulo.

## Pantalla: Consultas operativas

### Objetivo

Centralizar reportes internos de solo lectura por periodo, sin duplicar historiales en Inventario ni Deposito.

### Actor principal

Vendedor / Administrador / Gerente, segun la vista autorizada.

### Endpoints consumidos

- `GET /api/consultas/ventas`.
- `GET /api/consultas/gastos`.
- `GET /api/consultas/inventario/actual`.
- `GET /api/consultas/inventario/movimientos`.
- `GET /api/consultas/cierre`.
- `GET /api/consultas/deposito/movimientos`.

### Campos y controles

- Fecha inicial, fecha final y accion `Consultar`.
- Pestañas de Ventas y Gastos para vendedor.
- Pestañas adicionales de Inventario, Cierre y Deposito para administrador y gerente.
- Los movimientos de deposito conservan iconos SVG para distinguir entradas por cierre de salidas administrativas.

### Validaciones de interfaz

- Ninguna vista modifica ventas, gastos, inventario, cierres ni deposito.
- El periodo se aplica de forma explicita con `Consultar`.
- Un cierre inexistente se muestra como estado vacio controlado.
- Inventario concentra existencias consolidadas y movimientos; Deposito concentra historial y saldo posterior de cada movimiento.
- El desglose de stock diario por tipo de granizado no se implementa: el stock fisico canonico sigue siendo unico por tamano y no se modificaron backend ni schema.

### Evidencia de prueba

- `npx tsc -b --pretty false` y `npm run build`: exitosos.
- Navegador con gerente verifico ventas, gastos, inventario, cierre sin registro y movimientos de deposito reales.
- La ruta mantiene estado pendiente hasta la confirmacion manual final de filtros y roles.

## Pantalla: Evidencias administrativas

### Objetivo

Consultar metadata y adjuntar soportes a transferencias, gastos y movimientos administrativos de Deposito mediante los endpoints reales del backend.

### Actor principal

Administrador / Gerente.

### Endpoints consumidos

- `GET /api/consultas/transferencias`.
- `GET /api/consultas/gastos`.
- `GET /api/consultas/deposito/movimientos`.
- `GET` y `POST /api/evidencias/pagos-venta/{idPagoVenta}`.
- `GET` y `POST /api/evidencias/gastos-caja/{idGastoCaja}`.
- `GET` y `POST /api/evidencias/consignaciones-bancarias/{idConsignacionBancaria}`.
- `GET` y `POST /api/evidencias/pagos-servicios/{idPagoServicio}`.

### Campos y controles

- Filtros `fechaInicio` y `fechaFin`, propuestos desde el primer dia del mes hasta la fecha actual.
- Pestañas de Transferencias, Gastos y Deposito con sus contadores.
- Seleccion de registro, lista de metadata y selector multipart de archivo.
- Acciones Adjuntar y Reintentar cuando un archivo no se puede subir en el entorno local.

### Validaciones de interfaz

- Solo administrador y gerente reciben la ruta independiente; vendedor no ve Evidencias en navegacion.
- El frontend consulta metadata bajo demanda y presenta el error real del backend sin ocultar el registro seleccionado.
- Los movimientos `entrada_cierre` no se presentan como destino de evidencia; se usan solo consignaciones y pagos de servicios.
- La fecha final se propone para evitar que una sola fecha limite la consulta a un solo dia en backend.
- `supabase://...` se conserva como metadata interna, sin vista previa ni descarga en frontend.

### Evidencia de prueba

- `npx tsc -b --pretty false` y `npm run build`: exitosos.
- Gerente consulto `/evidencias` con el rango `2026-07-01` a `2026-07-10` y visualizo la consignacion por `$28.000` y el pago de arriendo por `$2.500`.
- El detalle de consignacion consulto su metadata y mostro que no hay evidencia registrada.
- El usuario confirmo manualmente la visualizacion en navegador.
- Supabase no se configura localmente; la carga real queda preparada para despliegue mediante backend.

## Pantalla: Transferencias y validacion administrativa

### Objetivo

Revisar transferencias pendientes o rechazadas, consultar la metadata de sus soportes y permitir a los roles administrativos decidir sobre una transferencia pendiente.

### Actor principal

Vendedor para consulta propia; Administrador / Gerente para consulta y decision.

### Endpoints consumidos

- `GET /api/consultas/transferencias` con filtros de estado y periodo.
- `GET /api/evidencias/pagos-venta/{idPagoVenta}`.
- `POST /api/pagos-venta/{idPagoVenta}/validar`.
- `POST /api/pagos-venta/{idPagoVenta}/rechazar`.

### Campos y controles

- Filtros `fechaInicio` y `fechaFin`, propuestos para el mes en curso.
- Pestañas Pendientes y Rechazadas, con contadores de monto y soportes.
- Detalle de transferencia, metadata de evidencia y campo opcional `observacionValidacion`.
- Acciones Validar y Rechazar con confirmacion previa.

### Validaciones de interfaz

- Vendedor no recibe acciones de decision; el backend preserva la autorizacion real por rol y usuario.
- Solo una transferencia pendiente recibe controles para decidir; el mensaje backend se muestra si el estado ya cambio.
- Una transferencia validada desaparece de la lista porque la consulta backend solo expone pendientes y rechazadas.
- La metadata se consulta bajo demanda. No hay vista previa ni descarga de `supabase://...`.

### Evidencia de prueba

- `npx tsc -b --pretty false` y `npm run build`: exitosos.
- El usuario registro y valido una transferencia pura de `$12.000` y una transferencia de `$8.000` dentro de un pago mixto.
- Consola: las dos ventas de la jornada `2026-07-11` conservan sus valores, no quedan transferencias pendientes o rechazadas y ambas decisiones dejaron auditoria `validar` sobre `pagos_venta`.
- Las consultas de metadata de los dos pagos respondieron correctamente sin soportes, resultado esperado sin Supabase local.

## Pantalla: Catalogos y gestion administrativa

### Objetivo

Consultar catalogos base activos desde la API real y gestionar items de inventario y vigencias de precios.

### Actor principal

Administrador / Gerente. El vendedor no accede a la ruta independiente de Catalogos.

### Endpoint consumido

- `GET /api/catalogos/metodos-pago`
- `GET /api/catalogos/tipos-granizado`
- `GET /api/catalogos/tamanos-vaso`
- `GET /api/catalogos/categorias-inventario`
- `GET /api/catalogos/unidades-medida`
- `GET /api/catalogos/items-inventario`
- `GET /api/catalogos/precios-granizado/vigentes`
- `GET /api/catalogos/promociones/vigentes`
- `GET /api/catalogos/tipos-servicio`
- `GET /api/catalogos/gestion/items-inventario`
- `POST /api/catalogos/gestion/items-inventario`
- `PUT /api/catalogos/gestion/items-inventario/{idItemInventario}`
- `PUT /api/catalogos/gestion/items-inventario/{idItemInventario}/estado`
- `GET /api/catalogos/gestion/precios-granizado`
- `POST /api/catalogos/gestion/precios-granizado`

### Campos del formulario

- `fechaVigencia`
- `buscar`
- Item de inventario: nombre, categoria y unidad de medida; el vaso automatico exige tamano y paquetes fijos de 20 unidades.
- Precio: tipo de granizado, tamano, valor y fecha de inicio.

### Validaciones de interfaz

- La consulta usa token activo.
- La fecha de vigencia se envia como filtro para precios y promociones.
- La busqueda es local sobre datos ya consultados.
- Si la API devuelve error, se muestra el mensaje real.
- La vista de gestion solo se muestra a administrador y gerente.
- El formulario conserva consumo manual y vaso por venta. El vaso automatico usa categoria `vasos`, tamano y paquetes fijos de 20 unidades.
- La configuracion de precios se realiza de forma independiente por tipo de granizado y tamano, sin cambiar existencias ni control de los vasos.
- El cambio de estado requiere confirmacion y las reglas finales permanecen en backend.

### Respuestas esperadas

- Consulta: se muestran conteos y listas de catalogos, precios, promociones e items.
- Gestion: se registra un item manual o un vaso automatico con stock inicial en cero, o una nueva vigencia que conserva el precio anterior en historial.
- Caso con error: se muestra mensaje de API y se permite reintento.

### Evidencia de prueba

- `npm run build`.
- Endpoints de catalogos validados contra backend real con token.
- Panel `/catalogos` validado en navegador integrado.
- Backend de gestion validado con gerente: 16 items y 12 precios recuperados.
- Pendiente de revalidacion visual: el control `Vaso por venta` debe conservar categoria `vasos`, tamano y paquetes fijos de 20 unidades.

## Pantalla: Gestion de usuarios

### Objetivo

Permitir que el gerente gestione los usuarios internos conforme a RF-03 y RF-04, sin eliminar su historial operativo.

### Actor principal

Gerente.

### Endpoints consumidos

- `GET /api/usuarios`.
- `GET /api/usuarios/roles`.
- `POST /api/usuarios`.
- `PUT /api/usuarios/{idUsuario}`.
- `PUT /api/usuarios/{idUsuario}/estado`.

### Validaciones de interfaz

- La ruta solo es visible para gerente y el backend mantiene la autorizacion.
- El formulario permite crear usuarios con roles activos, editar sus datos y cambiar su estado entre activo, inactivo y bloqueado.
- La interfaz filtra el directorio por texto y estado, y confirma antes de aplicar un cambio de acceso.
- No hay accion de eliminacion. La propia cuenta gerente no puede bloquearse ni inactivarse.
- Las contrasenas iniciales se envian solo al crear y no se muestran despues.

### Evidencia de prueba

- `npm run build`: exitoso.
- `GestionUsuariosIntegrationTest` y `BootstrapManagerInitializerTest`: exitosos.
- El usuario reviso manualmente el apartado `/usuarios` y confirmo su funcionamiento correcto.

## Orden previsto de pantallas

Fuente: `docs/development/fases/fase_4_frontend_validacion.md`.

1. Login.
2. Layout principal por rol. Implementado y validado.
3. Panel de caja abierta. Implementado y validado.
4. Catalogos necesarios para formularios. Implementado y validado.
5. Registro de venta y pagos. Implementado y validado.
6. Inventario operativo. Implementado y validado; consultas consolidadas trasladadas a Consultas.
7. Gastos, adiciones y pago trabajadores. Implementado y validado.
8. Cierre de caja. Implementado y validado.
9. Deposito, consignaciones y servicios. Implementado y validado; historial trasladado a Consultas.
10. Evidencias. Implementado y validado.
11. Transferencias y validacion administrativa. Implementado y validado.
12. Consultas operativas. Desarrollado y con validacion tecnica; pendiente confirmacion manual final.
13. Gestion de usuarios. Implementado y validado; exclusivo del gerente.
14. Auditoria. Pendiente.

Los modulos listados como implementados cuentan con validacion manual del usuario, salvo Consultas, que requiere confirmacion manual final antes de marcarse como base. Gestion de usuarios queda como Base lista. El siguiente cierre funcional pendiente es Consultas, seguido de Auditoria.
