# 10. Transferencias

## Objetivo

Consultar transferencias de ventas, revisar sus soportes y decidir las transferencias pendientes sin mezclar esta operacion con la pantalla general de evidencias.

## Requisitos cubiertos

- RF-24 a RF-28.
- RF-56 y RF-59.

## Funcionalidades

- Consulta por periodo de transferencias pendientes, validadas o rechazadas.
- Pestanas y totales separados para pendientes, validadas y rechazadas, disponibles tambien durante una caja abierta.
- Vista previa responsive de imagenes y PDF asociados, con selector para soportes historicos y descarga opcional.
- Desplazamiento automatico al visor cuando se selecciona un comprobante,
  respetando la preferencia de movimiento reducido del dispositivo.
- Mensaje de disponibilidad uniforme cuando un comprobante no puede descargarse.
- Validacion o rechazo con observacion opcional.
- Carga de una evidencia correctiva que conserva los soportes anteriores.
- Auditoria de validaciones, rechazos y ajustes de evidencia.

## Permisos

| Rol | Funcionalidad |
| --- | --- |
| Vendedor | Consulta el estado de sus propias transferencias. La carga inicial ocurre en Ventas. |
| Administrador | Consulta transferencias y descarga comprobantes para verificarlos. |
| Gerente | Consulta, descarga, valida, rechaza y adjunta evidencias de ajuste. |

## Reglas clave

- Solo pagos con metodo `transferencia` pueden decidirse.
- Solo una transferencia pendiente puede pasar a `validada` o `rechazada`.
- Una transferencia validada deja de requerir decision, pero permanece visible en la pestana `Validadas` para consultar su responsable, fecha, soportes y trazabilidad. Esto tambien permite revisarla durante la jornada con caja abierta.
- Las transferencias de una venta anulada se excluyen de las pestanas pendiente, validada y rechazada. Tampoco pueden validarse ni rechazarse; la venta y sus evidencias conservan trazabilidad en sus consultas correspondientes.
- El gerente puede adjuntar un ajuste de evidencia sin restriccion de fecha; el soporte anterior se conserva.
- El vendedor puede consultar el estado de sus transferencias, pero el visor y la descarga del comprobante continúan reservados para administrador y gerente.
- El visor obtiene el archivo mediante la API autenticada; nunca accede directamente al bucket privado.
- Si un comprobante no esta disponible en Storage o la descarga no puede completarse, la interfaz informa que la evidencia solicitada no esta disponible para descargar.

## Endpoints principales

- `GET /api/consultas/transferencias?estadoValidacion={pendiente|validada|rechazada}`
- `GET /api/evidencias/pagos-venta/{idPagoVenta}`
- `GET /api/evidencias/{idArchivoEvidencia}/descargar`
- `POST /api/pagos-venta/{idPagoVenta}/validar`
- `POST /api/pagos-venta/{idPagoVenta}/rechazar`
- `POST /api/evidencias/pagos-venta/{idPagoVenta}/ajustes`
