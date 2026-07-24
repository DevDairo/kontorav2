# 09. Evidencias integradas

## Objetivo

Conservar y consultar soportes de transferencias, gastos, consignaciones y pagos de servicios mediante el backend y Supabase Storage autoalojado.

La capacidad documental no tiene una entrada de navegacion independiente. Los soportes de gastos, consignaciones y servicios se gestionan en `/consultas`; los comprobantes de pagos de venta se revisan en `/transferencias`. La ruta anterior `/evidencias` redirige a `/consultas`.

Al abrir un soporte, la interfaz desplaza suavemente su vista previa al area visible. El comportamiento se comparte entre Consultas, Gastos y Transferencias y respeta la preferencia del sistema para reducir movimiento.

## Requisitos cubiertos

- RF-24, RF-28 y RF-50.
- RF-53, RF-59 y RF-60.

## Funcionalidades

- Carga multipart de imagenes o PDF autorizados.
- Compresion de imagenes en backend y almacenamiento de metadata original y comprimida.
- Consulta de metadata por el registro que respalda la evidencia.
- Vista previa responsive de imagenes y PDF para usuarios autorizados, con selector cuando un registro conserva varios soportes.
- Cuando existe un solo soporte, el visor ocupa todo el ancho disponible. La imagen conserva sus dimensiones naturales, se centra sin recorte y solo se reduce si excede el espacio disponible; con varios soportes, el selector se presenta como una franja horizontal.
- Descarga protegida por backend desde el mismo archivo temporal usado por la vista previa.
- Mensaje uniforme de disponibilidad cuando una descarga no puede completarse: `La evidencia solicitada no esta disponible para descargar.`
- Conservacion de soportes previos cuando se agrega una correccion; no se reemplazan ni eliminan registros historicos.

## Permisos

| Rol | Funcionalidad |
| --- | --- |
| Vendedor | Adjunta la evidencia de transferencia durante la venta y consulta solo lo permitido por su flujo. |
| Administrador | Consulta, adjunta y descarga soportes administrativos de gastos y deposito desde Consultas y evidencias. |
| Gerente | Tiene las capacidades administrativas y puede realizar ajustes historicos de evidencias de transferencia. |

## Reglas clave

- El frontend nunca recibe claves de Storage ni sube directamente al bucket.
- El bucket privado activo es `kontoraimagenes`; solo el backend usa la clave `service_role`.
- Storage no se publica en Internet. Los archivos se guardan en un volumen Docker y la metadata vive en el esquema PostgreSQL `storage`.
- La vista previa solicita el archivo al endpoint autenticado del backend, crea una URL temporal en el navegador y la revoca al cambiar de soporte o cerrar el visor.
- Imagenes y PDF se representan dentro de la aplicacion; un formato no representable conserva la opcion de descarga.
- Cada archivo se relaciona con un unico registro operativo; un pago puede conservar varios archivos de evidencia para trazabilidad.
- La interfaz no diferencia visualmente entre un archivo inexistente y una descarga que no puede completarse; en ambos casos informa que la evidencia no esta disponible. El backend mantiene el estado tecnico real para trazabilidad y diagnostico.
- El ajuste de una evidencia de transferencia se registra en auditoria.

## Endpoints principales

- `POST /api/evidencias/pagos-venta/{idPagoVenta}`
- `POST /api/evidencias/gastos-caja/{idGastoCaja}`
- `POST /api/evidencias/consignaciones-bancarias/{idConsignacionBancaria}`
- `POST /api/evidencias/pagos-servicios/{idPagoServicio}`
- `GET /api/evidencias/pagos-venta/{idPagoVenta}`
- `GET /api/evidencias/gastos-caja/{idGastoCaja}`
- `GET /api/evidencias/consignaciones-bancarias/{idConsignacionBancaria}`
- `GET /api/evidencias/pagos-servicios/{idPagoServicio}`
- `GET /api/evidencias/{idArchivoEvidencia}/descargar`
