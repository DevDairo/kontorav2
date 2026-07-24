# Modulo: Evidencias y almacenamiento

## Objetivo

Gestionar evidencias fotograficas o documentales asociadas a pagos por transferencia, gastos de caja, consignaciones bancarias y pagos de servicios, guardando los archivos fuera de PostgreSQL y persistiendo solo ruta y metadatos en `archivos_evidencia`.

## Tablas involucradas

- `archivos_evidencia`
- `pagos_venta`
- `gastos_caja`
- `consignaciones_bancarias`
- `pagos_servicios`
- `movimientos_deposito`
- `usuarios`

## Fuente de verdad del modelo

El modulo se implementa sobre las tablas reales del schema:

- `archivos_evidencia.id_pago_venta` asocia evidencias a pagos de venta.
- `archivos_evidencia.id_gasto_caja` asocia evidencias a gastos de caja.
- `archivos_evidencia.id_consignacion_bancaria` asocia evidencias a consignaciones bancarias.
- `archivos_evidencia.id_pago_servicio` asocia evidencias a pagos de servicios.
- `archivos_evidencia` exige una sola relacion por registro con `num_nonnulls(...) = 1`.
- `archivos_evidencia.url_archivo` guarda la ruta externa del archivo.
- `archivos_evidencia.tipo_archivo` usa `imagen`, `pdf` u `otro`.
- `archivos_evidencia.formato_archivo` usa `jpg`, `jpeg`, `png`, `webp`, `pdf` u `otro`.
- PostgreSQL no almacena binarios; solo guarda ruta y metadatos.

## Endpoints

| Metodo | Ruta | Autenticacion | Proposito |
| :- | :- | :- | :- |
| POST | `/api/evidencias/pagos-venta/{idPagoVenta}` | Si | Cargar evidencia multipart para un pago por transferencia. |
| GET | `/api/evidencias/pagos-venta/{idPagoVenta}` | Si | Listar evidencias de un pago de venta. |
| POST | `/api/evidencias/gastos-caja/{idGastoCaja}` | Si | Cargar evidencia multipart para un gasto de caja. |
| GET | `/api/evidencias/gastos-caja/{idGastoCaja}` | Si | Listar evidencias de un gasto de caja. |
| POST | `/api/evidencias/consignaciones-bancarias/{idConsignacionBancaria}` | Si | Cargar evidencia multipart para una consignacion bancaria. |
| GET | `/api/evidencias/consignaciones-bancarias/{idConsignacionBancaria}` | Si | Listar evidencias de una consignacion bancaria. |
| POST | `/api/evidencias/pagos-servicios/{idPagoServicio}` | Si | Cargar evidencia multipart para un pago de servicio. |
| GET | `/api/evidencias/pagos-servicios/{idPagoServicio}` | Si | Listar evidencias de un pago de servicio. |
| GET | `/api/evidencias/{idArchivoEvidencia}` | Si | Consultar la metadata de una evidencia. |

## Reglas de negocio implementadas

- Las evidencias se reciben como `multipart/form-data` en la parte `archivo`.
- Solo se aceptan evidencias de `pagos_venta` cuando el metodo real es `transferencia`.
- No se permite cargar evidencia para gastos con `estado_gasto = 'anulado'`.
- Una evidencia queda asociada a un solo proceso: pago de venta, gasto de caja, consignacion bancaria o pago de servicio.
- Los archivos de imagen `jpg`, `jpeg` y `png` se comprimen desde backend y se guardan como `jpg`.
- Los PDF se conservan sin compresion.
- Los metadatos guardan `tamano_original_kb`, `tamano_comprimido_kb` cuando aplica y `fue_comprimido`.
- El usuario autenticado queda registrado en `archivos_evidencia.id_usuario_subida`.
- El rol `vendedor` puede gestionar evidencias de pagos o gastos registrados por ese mismo usuario.
- `administrador` y `gerente` pueden gestionar evidencias de pagos, gastos, consignaciones y pagos de servicios.
- Las evidencias de deposito, consignaciones y pagos de servicios solo pueden ser gestionadas por `administrador` o `gerente`.

## Almacenamiento externo

- Se agrego un cliente de Supabase Storage para subir archivos usando el endpoint REST de Storage.
- El cliente se configura con:
  - `SUPABASE_URL`.
  - `SUPABASE_SERVICE_ROLE_KEY`.
  - `SUPABASE_STORAGE_BUCKET`, con valor por defecto `evidencias`.
- La ruta persistida en `archivos_evidencia.url_archivo` queda en formato `supabase://{bucket}/{ruta}`.
- Las pruebas usan un cliente de storage mockeado para validar el flujo sin depender de red ni credenciales reales.

## Pruebas realizadas

- `mvn -Dtest=EvidenciasIntegrationTest test`.
- Resultado: `BUILD SUCCESS`.
- Pruebas ejecutadas: 5.
- Fallos: 0.
- Errores: 0.
- Omitidas: 0.
- `mvn clean test`.
- Resultado: `BUILD SUCCESS`.
- Pruebas ejecutadas: 64.
- Fallos: 0.
- Errores: 0.
- Omitidas: 0.

## Pendientes

- La auditoria explicita de carga y consulta de evidencias queda pendiente para una ampliacion posterior de auditoria.
- La carga real y consulta de archivos contra Supabase Storage quedan pendientes del despliegue, cuando backend reciba `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` y `SUPABASE_STORAGE_BUCKET`.

## Actualizaciones posteriores

- En el modulo "Auditoria transversal" se implemento validacion y rechazo administrativo de transferencias.
- El modulo "Deposito, consignaciones y servicios" crea primero el registro financiero y devuelve su identificador; la interfaz adjunta enseguida la evidencia mediante estos endpoints. Si el almacenamiento externo no esta configurado, el registro queda trazable y la interfaz conserva una accion de reintento de evidencia.
- La interfaz de Deposito fue validada manualmente con administrador y gerente: pago de servicio y consignacion actualizan saldo e historial; si Storage local responde `503`, la evidencia conserva un reintento sin revertir la operacion financiera.

## Frontend Fase 4

Estado: completado y validado manualmente en navegador el 2026-07-10.

- Se implemento la ruta administrativa `/evidencias` con `EvidenciasPanel` para `administrador` y `gerente`; `vendedor` no recibe una interfaz independiente y conserva sus evidencias propias en los flujos que las originan.
- El panel consulta transferencias, gastos y movimientos de deposito por periodo, y solo presenta consignaciones o pagos de servicios como destinos de evidencia dentro de Deposito.
- La fecha inicial se propone como el primer dia del mes y la fecha final como el dia actual. Ambas son editables. El rango completo es necesario porque los endpoints de consultas interpretan una sola fecha como un periodo de ese mismo dia.
- Al seleccionar un registro, el frontend consulta en diferido sus metadatos mediante los `GET /api/evidencias/...` especificos y muestra nombre, formato, tamanos, compresion, usuario, fecha y estado.
- El formulario multipart usa los cuatro `POST /api/evidencias/...` reales. Si Storage local responde `503`, conserva el archivo seleccionado durante la sesion y deja disponible el reintento, sin alterar el registro financiero asociado.
- No se muestra vista previa ni descarga: `urlArchivo` es una ruta interna `supabase://...` y el backend actual expone metadata, no un endpoint de contenido.
- La ruta quedo como `Base lista` en la navegacion. La ocultacion por rol es una capa de experiencia; backend sigue siendo la autoridad de permisos.

Validacion frontend realizada:

- `npx tsc -b --pretty false`: exitoso.
- `npm run build`: exitoso.
- Con gerente, `/evidencias` consulto el rango `2026-07-01` a `2026-07-10` y mostro la consignacion de `$28.000` y el pago de arriendo de `$2.500` registrados en Deposito.
- La consulta de metadata para la consignacion respondio y mostro correctamente que no habia archivo cargado.
- El usuario confirmo manualmente la visualizacion de los registros en navegador.

Pendiente solo de despliegue:

- La carga real y la consulta de contenido contra Supabase requieren `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` y `SUPABASE_STORAGE_BUCKET` configurados exclusivamente en backend. No se expusieron secretos en frontend ni se realizo una carga financiera adicional durante la validacion local.
