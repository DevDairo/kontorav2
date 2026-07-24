# Estructura frontend

## Alcance de esta inicializacion

Esta inicializacion crea la base React + TypeScript + Vite para Fase 4. El frontend consume la API real del backend y mantiene las reglas criticas en backend.

## Estructura creada

```text
frontend/
|-- src/
|   |-- app/
|   |   |-- providers/
|   |   `-- routes/
|   |-- modules/
|   |   |-- auth/
|   |   |-- auditoria/
|   |   |-- caja/
|   |   |-- catalogos/
|   |   |-- cierre/
|   |   |-- deposito/
|   |   |-- evidencias/
|   |   |-- gastos/
|   |   |-- inventario/
|   |   |-- usuarios/
|   |   `-- ventas/
|   |-- shared/
|   |   |-- components/
|   |   |-- hooks/
|   |   |-- services/
|   |   |-- types/
|   |   `-- utils/
|   |-- App.tsx
|   |-- index.css
|   `-- main.tsx
|-- .env.example
|-- index.html
|-- package.json
|-- tsconfig.json
`-- vite.config.ts
```

## Responsabilidades

- `src/app`: composicion global, providers y rutas declaradas.
- `src/modules`: superficie reservada para pantallas por modulo funcional.
- `src/modules/auth`: login, proveedor de sesion, servicio de autenticacion y almacenamiento controlado de token.
- `src/modules/usuarios`: gestion exclusiva de gerente para crear, editar y cambiar el estado de acceso de usuarios.
- `src/shared/components`: componentes visuales reutilizables.
- `src/shared/hooks`: hooks transversales para comprobaciones tecnicas cuando una vista los requiere.
- `src/shared/services`: cliente HTTP y servicios de API.
- `src/shared/types`: tipos compartidos de contratos API.
- `src/shared/utils`: utilidades de configuracion, moneda y presentacion de texto.

## Layout principal por rol

El layout principal queda implementado sobre la sesion confirmada por backend.

Archivos principales:

```text
frontend/src/App.tsx
frontend/src/app/routes/appRoutes.ts
frontend/src/shared/components/AppShell.tsx
frontend/src/shared/components/ModuleOverview.tsx
frontend/src/shared/components/RouteWorkspace.tsx
```

Responsabilidades:

- `appRoutes.ts` centraliza rutas visibles, roles, estado de pantalla y endpoints documentados.
- `App.tsx` filtra rutas con `nombreRol` devuelto por `/api/auth/me`.
- `AppShell` renderiza sidebar de escritorio, menu desplegable en movil, topbar, usuario autenticado y navegacion filtrada por rol.
- `ModuleOverview` muestra los modulos visibles con descripciones de negocio.
- `RouteWorkspace` conserva la vista base para modulos pendientes sin exponer endpoints en la interfaz.

El filtro por rol en frontend es solo una mejora de experiencia. Los permisos finales se mantienen en backend.

## Ajuste responsive y limpieza visual

- `LoginPage` usa un panel decorativo diagonal a pantalla completa en escritorio, con fondo `#f5f8fc`; se oculta en movil.
- `index.css` concentra los breakpoints de la shell y de los filtros uniformes.
- `shared/utils/displayText.ts` transforma los nombres tecnicos de inventario en etiquetas legibles sin modificar los valores recibidos del backend.
- Las rutas operativas conservan sus contratos; la limpieza visual no cambia endpoints, DTOs ni autorizacion.

## Panel de caja abierta

El panel de caja abierta queda implementado dentro de `src/modules/caja`.

Archivos principales:

```text
frontend/src/modules/caja/components/CajaAbiertaPanel.tsx
frontend/src/modules/caja/services/cajaService.ts
frontend/src/modules/caja/types.ts
```

Responsabilidades:

- `CajaAbiertaPanel` consulta y muestra la caja diaria abierta.
- `cajaService` consume `GET /api/cajas-diarias/abierta` y `POST /api/cajas-diarias`.
- `types.ts` conserva el contrato real de `CajaDiariaResponse`.
- La apertura usa `ConfirmationDialog`; el valor base inicial es `300000`, editable antes de confirmar.

La apertura visible para `administrador` y `gerente` no reemplaza la validacion del backend.

## Catalogos para formularios

El panel de catalogos queda implementado dentro de `src/modules/catalogos`.

Archivos principales:

```text
frontend/src/modules/catalogos/components/CatalogosPanel.tsx
frontend/src/modules/catalogos/components/CatalogosGestionPanel.tsx
frontend/src/modules/catalogos/services/catalogosService.ts
frontend/src/modules/catalogos/types.ts
```

Responsabilidades:

- `CatalogosPanel` consulta y muestra datos maestros activos, y habilita la vista `Gestion` solo a administrador y gerente.
- `CatalogosGestionPanel` administra consumibles, vasos automaticos, estado de items y vigencias de precios mediante contratos reales.
- `catalogosService` consume endpoints de consulta y gestion en `GET`, `POST` y `PUT` bajo `/api/catalogos/...`.
- `types.ts` conserva los contratos reales de catalogos, precios, promociones e items de inventario.

La consulta es de solo lectura. La gestion no duplica reglas: backend conserva la autoridad sobre permisos, stock inicial, restricciones de inactivacion, estructura de items, vigencia y auditoria. La alta visual conserva consumo manual y vaso por venta con paquetes de 20 unidades; la configuracion de precios es un flujo separado por tipo y tamano.

## Registro de ventas y pagos

El panel de ventas queda implementado dentro de `src/modules/ventas`.

Archivos principales:

```text
frontend/src/modules/ventas/components/VentasPanel.tsx
frontend/src/modules/ventas/services/ventasService.ts
frontend/src/modules/ventas/types.ts
```

Responsabilidades:

- `VentasPanel` consulta catalogos reales, calcula una vista previa y registra ventas mediante `POST /api/ventas`.
- El panel construye pagos en efectivo, transferencia o mixtos sin enviar un valor mayor al total de la venta.
- El cambio se muestra antes de registrar y se contrasta con la respuesta real del backend.
- Una evidencia de transferencia se envia al backend con `FormData`; el frontend no usa credenciales de Supabase ni sube directamente al almacenamiento.
- La carga real de evidencia requiere Supabase Storage configurado en el entorno del backend y queda preparada para despliegue.

## Inventario operativo

El panel de inventario queda implementado dentro de `src/modules/inventario`.

Archivos principales:

```text
frontend/src/modules/inventario/components/InventarioPanel.tsx
frontend/src/modules/inventario/services/inventarioService.ts
frontend/src/modules/inventario/types.ts
```

Responsabilidades:

- `InventarioPanel` muestra stock diario, formularios de paquetes y consumos, y ajustes con los contratos reales.
- `inventarioService` consume existencias generales como dato de soporte para formularios, existencias diarias y ajustes; el historial se consulta desde el modulo de Consultas.
- Gerente aplica directamente ajustes de stock general; administrador solicita ajustes y gerente decide los pendientes.
- La ausencia de caja abierta solo bloquea operaciones de jornada. No bloquea los ajustes de stock general.
- La ruta se muestra solo a administrador y gerente; vendedor no recibe una interfaz independiente de Inventario.

## Gastos y operaciones de caja

La interfaz de gastos y operaciones financieras se distribuye entre `src/modules/gastos` y `src/modules/caja`.

Archivos principales:

```text
frontend/src/modules/gastos/components/GastosPanel.tsx
frontend/src/modules/gastos/services/gastosService.ts
frontend/src/modules/gastos/types.ts
frontend/src/modules/caja/components/CajaOperacionesPanel.tsx
frontend/src/shared/utils/moneyInput.ts
```

Responsabilidades:

- `GastosPanel` registra y lista gastos para los tres roles; administrador y gerente pueden editar o anularlos.
- El pago diario a trabajadores se administra desde `GastosPanel` solo para administrador y gerente, conservando la actualizacion de un pago confirmado mientras la caja siga abierta.
- `CajaOperacionesPanel` permite adiciones a administrador y gerente y presenta la proyeccion de efectivo calculada por backend.
- Caja muestra el pago a trabajadores como insumo de la proyeccion, pero no duplica los controles de escritura.
- `moneyInput.ts` normaliza importes escritos o pegados sin depender de los controles incrementales del navegador.

El frontend muestra estados y validaciones de experiencia; la caja abierta, los permisos, el calculo del efectivo esperado y las condiciones de cierre se mantienen en backend.

## Cierre de caja y consulta historica

El panel de cierre queda implementado dentro de `src/modules/cierre`.

Archivos principales:

```text
frontend/src/modules/cierre/components/CierreCajaPanel.tsx
frontend/src/modules/cierre/services/cierreService.ts
frontend/src/modules/cierre/types.ts
frontend/src/shared/components/ConfirmationDialog.tsx
```

Responsabilidades:

- `CierreCajaPanel` consume la caja abierta y el resumen financiero calculado por backend.
- `cierreService` registra el efectivo contado sin base y consulta cierres por identificador o por fecha de operacion.
- La pantalla separa efectivo, transferencias y base; muestra la diferencia y el movimiento automatico de deposito cuando backend lo retorna.
- El historial usa `GET /api/consultas/cierre?fecha=...`, por lo que un cierre sigue disponible despues de recargar la pagina.
- `ConfirmationDialog` evita que abrir o cerrar caja se ejecute por un clic accidental.

La fecha de operacion identifica la jornada de negocio. Una jornada puede cerrar despues de medianoche, pero no se puede abrir una segunda caja mientras exista una caja abierta.

## Deposito, consignaciones y servicios

El panel de deposito queda implementado dentro de `src/modules/deposito`.

Archivos principales:

```text
frontend/src/modules/deposito/components/DepositoPanel.tsx
frontend/src/modules/deposito/services/depositoService.ts
frontend/src/modules/deposito/types.ts
frontend/src/modules/evidencias/services/evidenciasService.ts
```

Responsabilidades:

- `DepositoPanel` consulta el saldo directamente desde backend, sin recalcularlo como fuente de verdad en frontend.
- Solo se muestra a `administrador` y `gerente`; el vendedor no recibe la ruta ni puede usar los endpoints protegidos.
- Registra consignaciones y pagos de servicio con confirmacion previa, validacion de saldo disponible y actualizacion posterior de los datos.
- Exige seleccionar evidencia antes de registrar una salida. Si Storage no esta configurado, conserva el identificador y el archivo en estado pendiente para reintento.
- El historial de deposito por periodo se concentra en `/consultas`, sin duplicarse en esta pantalla operativa.

## Evidencias administrativas

El panel de evidencias queda implementado en `src/modules/evidencias`.

Archivos principales:

```text
frontend/src/modules/evidencias/components/EvidenciasPanel.tsx
frontend/src/modules/evidencias/services/evidenciasConsultaService.ts
frontend/src/modules/evidencias/services/evidenciasService.ts
frontend/src/modules/evidencias/types.ts
```

Responsabilidades:

- Solo aparece para `administrador` y `gerente`; vendedor conserva sus evidencias dentro de los flujos que las originan.
- Consulta los registros origen con `consultas` y carga metadata solo al seleccionar una transferencia, gasto, consignacion o pago de servicio.
- Propone el rango desde el primer dia del mes hasta la fecha actual, porque el backend toma una fecha unica como consulta de un solo dia.
- Permite adjuntar o reintentar mediante los endpoints backend de Evidencias. El archivo permanece en memoria durante el reintento local.
- Muestra metadata, no contenido: una ruta `supabase://...` no se trata como enlace publico ni se expone un secreto de Storage.

## Transferencias y validacion

El panel de transferencias queda implementado en `src/modules/transferencias`.

Archivos principales:

```text
frontend/src/modules/transferencias/components/TransferenciasPanel.tsx
frontend/src/modules/transferencias/services/transferenciasService.ts
frontend/src/modules/transferencias/types.ts
```

Responsabilidades:

- Consulta en paralelo las transferencias `pendiente` y `rechazada` por periodo usando los contratos de Consultas.
- Vendedor recibe sus propios registros y metadata autorizada; administrador y gerente reciben tambien las acciones de validacion y rechazo.
- Carga los metadatos del comprobante al seleccionar una transferencia, sin convertir la ruta interna de Storage en enlace publico.
- Usa `ConfirmationDialog` antes de validar o rechazar e informa el resultado real de backend.
- Una transferencia validada se elimina de la lista actual porque el endpoint de consultas solo devuelve pendientes y rechazadas; la evidencia de la decision queda en auditoria.

## Consultas operativas

El panel de consultas queda implementado en `src/modules/consultas` y se integra en `App.tsx` cuando la ruta activa es `/consultas`.

Archivos principales:

```text
frontend/src/modules/consultas/components/ConsultasPanel.tsx
frontend/src/modules/consultas/services/consultasService.ts
frontend/src/modules/consultas/types.ts
frontend/src/modules/consultas/index.ts
```

Responsabilidades:

- Centraliza consultas de ventas, gastos, inventario, cierre y deposito por periodo, sin operaciones de escritura.
- Vendedor solo recibe Ventas y Gastos; administrador y gerente reciben todas las vistas habilitadas por backend.
- Inventario concentra existencias actuales y movimientos historicos; Deposito concentra su historial con iconos SVG para entradas y salidas.
- Un cierre inexistente se muestra como resultado vacio controlado; no se interpreta como fallo de la interfaz.
- La pantalla esta desarrollada y compilada; su estado visual se mantiene pendiente hasta la confirmacion manual final del usuario.

## Cliente HTTP

El cliente base esta en `frontend/src/shared/services/apiClient.ts`.

- Usa `VITE_API_URL` como URL base.
- Agrega `Accept: application/json`.
- Permite adjuntar token Bearer cuando los modulos autenticados lo necesiten.
- Respeta `FormData` para futuras cargas multipart de evidencias.
- Propaga errores HTTP mediante `ApiClientError`.

## Autenticacion frontend

La autenticacion se implementa en `frontend/src/modules/auth`.

Estructura principal:

```text
frontend/src/modules/auth/
|-- components/LoginPage.tsx
|-- context/AuthContext.tsx
|-- hooks/useAuth.ts
|-- services/authService.ts
|-- utils/tokenStorage.ts
|-- index.ts
`-- types.ts
```

El token JWT se almacena en `sessionStorage` mediante `tokenStorage.ts` y se adjunta a las llamadas protegidas usando la opcion `token` del cliente HTTP.

La app reconstruye la sesion con `GET /api/auth/me`; si el backend rechaza el token, el frontend limpia la sesion local y vuelve al login.

## Variable de entorno

```env
VITE_API_URL=http://localhost:8080/api
```

La configuracion versionada queda en `frontend/.env.example`. No se versionan archivos `.env` reales.

## Validacion esperada

```powershell
cd frontend
npm install
npm run build
```

Para validacion visual local:

```powershell
cd frontend
npm run dev
```

La primera conexion implementada consume `GET /api/health`.

## Integracion local con backend

El proceso de validacion local, errores encontrados y correccion CORS quedan documentados en:

```text
docs/frontend/integracion-backend-local.md
```
