# Pantalla: Evidencias administrativas

## Estado

Completado y validado manualmente en navegador el 2026-07-10.

## Objetivo

Consultar los soportes y sus metadatos asociados a transferencias, gastos de caja, consignaciones bancarias y pagos de servicios. La pantalla permite adjuntar o reintentar un archivo mediante el backend, sin acceder directamente a Supabase.

## Actor principal

Administrador / Gerente.

`vendedor` no ve la ruta independiente `/evidencias`. Sus soportes propios permanecen en los flujos de Ventas y Gastos, donde backend limita el acceso por usuario autenticado.

## Archivos principales

```text
frontend/src/modules/evidencias/components/EvidenciasPanel.tsx
frontend/src/modules/evidencias/services/evidenciasConsultaService.ts
frontend/src/modules/evidencias/services/evidenciasService.ts
frontend/src/modules/evidencias/types.ts
```

## Endpoints consumidos

### Origen de registros

- `GET /api/consultas/transferencias?fechaInicio={fechaInicio}&fechaFin={fechaFin}`.
- `GET /api/consultas/gastos?fechaInicio={fechaInicio}&fechaFin={fechaFin}`.
- `GET /api/consultas/deposito/movimientos?fechaInicio={fechaInicio}&fechaFin={fechaFin}`.

La pestaña Deposito solo admite como destino de evidencia los movimientos que tengan `idConsignacionBancaria` o `idPagoServicio`; la entrada automatica de cierre no requiere soporte adicional desde esta pantalla.

### Metadata y adjuntos

- `GET` y `POST /api/evidencias/pagos-venta/{idPagoVenta}`.
- `GET` y `POST /api/evidencias/gastos-caja/{idGastoCaja}`.
- `GET` y `POST /api/evidencias/consignaciones-bancarias/{idConsignacionBancaria}`.
- `GET` y `POST /api/evidencias/pagos-servicios/{idPagoServicio}`.
- `GET /api/evidencias/{idArchivoEvidencia}` para metadata individual cuando otro flujo lo requiere.

## Controles y comportamiento

- Filtro `fechaInicio` y `fechaFin`. Por defecto consulta desde el primer dia del mes hasta la fecha actual; los dos controles son editables.
- Pestañas Transferencias, Gastos y Deposito con contadores de registros disponibles.
- Seleccion de un registro para cargar en diferido su lista de evidencias y evitar consultas por cada fila al abrir la pantalla.
- Metadata visible: nombre, formato, tamanos original y comprimido, compresion, usuario de subida, fecha y estado.
- Selector de archivo y accion Adjuntar. El archivo se conserva en memoria durante la sesion cuando falla la carga, habilitando Reintentar.
- No hay vista previa ni descarga de archivo: la ruta `supabase://...` no es una URL publica y el backend actual no expone contenido binario.
- El filtro de periodo usa una barra uniforme y aplica el rango solo al pulsar `Consultar`.

## Reglas y limites de interfaz

- La interfaz no replica las reglas definitivas de tipo de pago, gasto anulado ni propiedad del usuario. El backend decide y devuelve su mensaje real.
- La capa de navegacion limita la ruta a administrador y gerente; backend conserva la autorizacion efectiva.
- La ausencia de `fechaFin` no se usa por defecto porque las consultas normalizan una unica fecha como un periodo de un solo dia.
- En local, Supabase no esta configurado intencionalmente. Un `503` de Storage se comunica como evidencia pendiente y no modifica ni revierte la operacion asociada.

## Evidencia de prueba

- `npx tsc -b --pretty false`: exitoso.
- `npm run build`: exitoso.
- Con gerente en `/evidencias`, el rango `2026-07-01` a `2026-07-10` mostro dos registros de Deposito: consignacion por `$28.000` y pago de arriendo por `$2.500`.
- La consulta de metadata de la consignacion mostro correctamente el estado sin evidencias registradas.
- El usuario confirmo manualmente la visualizacion de los registros en navegador.

## Pendiente de despliegue

La carga real contra Supabase se valida cuando el backend de despliegue reciba `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` y `SUPABASE_STORAGE_BUCKET`. El frontend no almacena ni expone esas credenciales.
