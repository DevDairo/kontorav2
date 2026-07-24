# Inventario operativo: cierre y contexto vigente

## Estado

El frontend de Inventario operativo quedo cerrado el 2026-07-09 en la rama `chore/inicializacion-frontend`.

- La ruta `/inventario` esta marcada como `Base lista`.
- Se valido el flujo con gerente, administrador y vendedor.
- El usuario confirmo la validacion manual final sin errores aparentes.
- No se realizo commit, merge ni cambio de rama desde esta sesion.

## Reglas operativas consolidadas

- El gerente determina y registra el stock general. Su ajuste se aplica directamente, queda aprobado y genera auditoria y movimiento de inventario.
- El administrador solicita ajustes de stock general. El gerente aprueba o rechaza esas solicitudes pendientes.
- RF-47 se usa para dejar trazabilidad de diferencias y corroborar el inventario calculado contra la existencia fisica; no convierte el ingreso ordinario de stock general del gerente en una solicitud.
- La ausencia de caja diaria abierta solo bloquea paquetes de vasos y consumos diarios. No bloquea consultar ni corregir stock general.
- Al abrir una caja nueva, cada item de vaso con paquetes conserva como stock diario inicial el remanente de la jornada anterior: conteo fisico final si existe y, en su ausencia, saldo teorico final.
- Vendedor no ve ni puede acceder a interfaces independientes de Inventario, Catalogos o Evidencias.

## Evidencia tecnica y funcional

- `mvn -Dtest=InventarioIntegrationTest,CajaDiariaIntegrationTest test`: 16 pruebas, sin fallos ni errores.
- `npx tsc -b --pretty false`: exitoso.
- `npm run build`: exitoso fuera del sandbox por la restriccion conocida de `esbuild` dentro del sandbox.
- Backend activo reconstruido y `GET /api/inventario/ajustes` verificado con token: `200` para gerente y administrador, `403` para vendedor.
- Datos controlados: solicitud administrativa de entrada, aprobacion por gerente, rechazo posterior y ajuste directo del gerente. El stock se restauro al valor inicial al finalizar la prueba.
- Navegador: gerente ve aplicacion directa de stock general sin caja abierta; administrador ve solicitud; vendedor no ve las rutas restringidas y las rutas directas redirigen al inicio.

## Documentacion de referencia

```text
docs/modules/inventario-operativo.md
docs/modules/inventario-operativo-frontend.md
docs/frontend/pantallas.md
docs/AVANCE_PROYECTO.md
```

## Siguiente punto de control frontend

Ventas y pagos ya tiene implementacion, validacion tecnica y pruebas asistidas, pero conserva pendiente la confirmacion manual final del usuario. Debe cerrarse esa validacion antes de abrir el siguiente modulo de interfaz.
