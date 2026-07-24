# Cierre archivado: Ventas y pagos frontend

## Estado

- Cierre confirmado el 2026-07-09.
- Ruta `Ventas`: `Base lista`.
- La validacion manual final en navegador fue confirmada por el usuario.

## Referencia vigente

```text
docs/modules/ventas-pagos-frontend.md
docs/modules/ventas-pagos.md
docs/frontend/pantallas.md
```

## Pendiente de despliegue, no de la interfaz local

- La carga real de comprobantes requiere `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` y `SUPABASE_STORAGE_BUCKET` configurados solo en el backend o servidor.
- En local sin esas variables, `POST /api/evidencias/pagos-venta/{idPagoVenta}` responde `503` con el estado esperado `Supabase Storage no esta configurado`.
- El frontend no expone secretos ni carga directamente a Supabase.

El contenido de pendientes anterior queda sustituido por este cierre para no reabrir validaciones ya completadas.
