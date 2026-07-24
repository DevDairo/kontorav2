# Pantalla: Transferencias y validacion administrativa

## Estado

Completado y validado manualmente en navegador el 2026-07-10.

## Requisitos cubiertos

- RF-24: el pago por transferencia se origina en Ventas y conserva su valor transferido.
- RF-27: administrador y gerente validan o rechazan transferencias pendientes, conservando estado, usuario, fecha y observacion cuando se informa.
- RF-28 y RF-59: la pantalla consulta la metadata de los soportes asociados a cada pago por transferencia.
- RF-56: cada decision se registra en `auditoria_operaciones` mediante backend.

## Actores y permisos

- `vendedor`: consulta solo sus transferencias y sus metadatos autorizados por backend. No recibe controles de decision.
- `administrador` y `gerente`: consultan las transferencias del negocio y pueden validar o rechazar las que estan pendientes. Backend confirma el permiso definitivo.

## Archivos principales

```text
frontend/src/modules/transferencias/components/TransferenciasPanel.tsx
frontend/src/modules/transferencias/services/transferenciasService.ts
frontend/src/modules/transferencias/types.ts
```

## Endpoints consumidos

- `GET /api/consultas/transferencias?estadoValidacion=pendiente&fechaInicio={fechaInicio}&fechaFin={fechaFin}`.
- `GET /api/consultas/transferencias?estadoValidacion=rechazada&fechaInicio={fechaInicio}&fechaFin={fechaFin}`.
- `GET /api/evidencias/pagos-venta/{idPagoVenta}`.
- `POST /api/pagos-venta/{idPagoVenta}/validar` con `observacionValidacion` opcional.
- `POST /api/pagos-venta/{idPagoVenta}/rechazar` con `observacionValidacion` opcional.

## Comportamiento de la interfaz

- Propone el periodo desde el primer dia del mes hasta el dia actual, ambos editables.
- Presenta contadores y valores de transferencias pendientes, rechazadas y soportes disponibles.
- Carga la metadata de evidencia solo al seleccionar un registro. Muestra nombre, formato, tamanos, compresion, usuario, fecha y estado.
- Las acciones Validar y Rechazar piden confirmacion antes de llamar al backend. La observacion es opcional porque asi lo define el contrato real.
- Luego de validar, la transferencia deja de aparecer en la consulta: `GET /api/consultas/transferencias` solo expone estados `pendiente` y `rechazada`. La decision validada se conserva en `pagos_venta` y auditoria.
- No se previsualizan ni descargan archivos desde `supabase://...`; backend expone metadata, no contenido binario.
- El filtro de periodo usa la misma barra visual que Catalogos y aplica el rango solo al pulsar `Consultar`.

## Validacion realizada

- `npx tsc -b --pretty false` y `npm run build`: exitosos.
- Con gerente, la pantalla cargo sin errores de consola y presento correctamente los filtros, estados vacios y controles de decision antes de crear las pruebas controladas.
- El usuario abrio la caja de la jornada `2026-07-11`, registro una venta de 12 oz con transferencia pura por `$12.000` y una venta mixta con transferencia por `$8.000` y efectivo por `$4.000`.
- El usuario valido ambas transferencias desde la interfaz.
- La consola confirmo que las dos ventas siguen registradas, no quedan transferencias pendientes ni rechazadas para esa jornada y existen dos auditorias `validar` sobre `pagos_venta`, ambas con cambio de `pendiente` a `validada` y gerente como validador.
- Las consultas de metadata de ambos pagos respondieron `200` con listas vacias, coherentes con Supabase Storage sin configurar localmente.

## Limite de entorno

La carga real de comprobantes y su contenido se valida en despliegue con `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` y `SUPABASE_STORAGE_BUCKET` configurados exclusivamente en backend. El frontend no contiene secretos ni accede directamente a Storage.
