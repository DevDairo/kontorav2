# Modulo frontend: Ventas y pagos

## Estado

Completado y validado manualmente en navegador el 2026-07-09.

- Ruta: `/ventas`.
- Estado de ruta: `Base lista`.
- Roles visibles: `vendedor`, `administrador` y `gerente`.
- La autorizacion y el calculo definitivo permanecen en backend.

## Objetivo

Registrar ventas de mostrador con productos, precios y promociones reales, y representar pagos en efectivo, transferencia o combinacion mixta.

## Componentes y contratos

```text
frontend/src/modules/ventas/components/VentasPanel.tsx
frontend/src/modules/ventas/services/ventasService.ts
frontend/src/modules/ventas/types.ts
frontend/src/modules/evidencias/services/evidenciasService.ts
```

El panel usa:

- `POST /api/ventas`
- `POST /api/evidencias/pagos-venta/{idPagoVenta}`
- Catalogos de tipos de granizado, tamanos, precios, promociones y metodos de pago.

## Reglas representadas en la interfaz

- El pago en efectivo muestra el cambio estimado antes de registrar y el cambio devuelto por backend en la respuesta.
- La transferencia pura requiere el valor exacto de la venta. Un faltante o excedente bloquea el envio.
- El pago mixto registra solo el valor necesario por cada metodo. Un excedente en efectivo se trata como cambio; un excedente de transferencia se bloquea antes de enviar.
- El pago de transferencia se registra pendiente de validacion segun la respuesta de backend.
- El comprobante se envia como `FormData` al backend despues de registrar la venta. El navegador no contiene llaves de Supabase.
- Adiciones diarias se registran desde Ventas. El formulario se habilita solo con caja abierta y conserva el contrato real de operaciones de caja.

## Validacion realizada

### Frontend y API

- `npx tsc -b --pretty false`: exitoso.
- `npm run build`: exitoso fuera del sandbox por la restriccion conocida de `esbuild` dentro del sandbox.
- Catalogos reales cargaron correctamente en `/ventas`.
- Venta en efectivo: total `8000`, efectivo recibido `10000`, cambio real `2000`.
- Transferencia pura: faltantes y excedentes se informaron y bloquearon sin enviar payload invalido.
- Pago mixto: transferencia `6000`, efectivo recibido `3000`, efectivo registrado `2000`, cambio real `1000`.
- Consola del navegador sin errores ni advertencias durante las pruebas.

### Evidencias de transferencia

- La interfaz esta preparada para usar `POST /api/evidencias/pagos-venta/{idPagoVenta}` con la parte multipart `archivo`.
- La validacion local sin Supabase Storage devolvio `503`, `Supabase Storage no esta configurado`, que es el estado esperado de entorno.
- La carga real se valida al desplegar el backend con `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` y `SUPABASE_STORAGE_BUCKET`. Es una preparacion de despliegue, no una funcionalidad local confirmada.

### Validacion manual

El usuario confirmo la revision manual final el 2026-07-09.

### Validacion posterior de transferencias

- La consulta y decision administrativa de transferencias se implemento despues en `/transferencias`; ver `docs/modules/transferencias-validacion-frontend.md`.
- El 2026-07-10 se validaron desde la interfaz una transferencia pura de `$12.000` y la porcion transferida de `$8.000` de un pago mixto. Ambas quedaron en estado `validada` con auditoria sobre `pagos_venta`.

### Actualizacion visual del 2026-07-11

- El encabezado deja de mostrar el endpoint tecnico.
- `AdicionesDiariasPanel` se incorpora a la ruta `/ventas`; consulta la caja abierta y el registro existente antes de habilitar su formulario.
- No se modificaron los contratos `POST /api/ventas` ni `POST /api/operaciones-caja/adiciones-diarias`.
