# Modulo frontend: Inventario operativo

## Estado

Completado y validado manualmente en navegador el 2026-07-09.

- Ruta: `/inventario`.
- Estado de ruta: `Base lista`.
- Roles visibles: `administrador` y `gerente`.
- El backend mantiene la autorizacion definitiva.

## Objetivo

Presentar el control operativo de stock general, stock diario de vasos, movimientos y ajustes de inventario usando los contratos reales del backend.

## Componentes y contratos

```text
frontend/src/modules/inventario/components/InventarioPanel.tsx
frontend/src/modules/inventario/services/inventarioService.ts
frontend/src/modules/inventario/types.ts
```

El panel consume:

- `GET /api/inventario/existencias/general`
- `GET /api/inventario/existencias/diarias/abierta`
- `POST /api/inventario/paquetes-vasos`
- `POST /api/inventario/consumos-diarios`
- `GET /api/inventario/movimientos`
- `GET /api/inventario/ajustes`
- `POST /api/inventario/ajustes`
- `POST /api/inventario/ajustes/{idAjusteInventario}/aprobar`
- `POST /api/inventario/ajustes/{idAjusteInventario}/rechazar`

## Reglas representadas en la interfaz

- Gerente usa `Aplicar stock` para registrar directamente el stock general. La operacion no depende de una caja diaria abierta.
- Administrador usa `Solicitar ajuste`; la solicitud queda pendiente y no modifica stock hasta la decision del gerente.
- Gerente aprueba o rechaza solicitudes administrativas de RF-47. El ajuste es la trazabilidad para corroborar diferencias entre inventario calculado y fisico.
- Paquetes de vasos y consumos diarios son operaciones de jornada: se deshabilitan sin caja abierta.
- El remanente diario de cada item de vaso se arrastra al abrir la siguiente caja. La interfaz no interpreta una caja nueva como stock diario vacio.
- Vendedor no recibe la ruta de Inventario. Tampoco recibe Catalogos ni Evidencias como interfaces independientes.

## Actualizacion visual del 2026-07-11

- `/inventario` conserva solo stock diario, formularios operativos y ajustes; el stock general y los movimientos historicos se consultan desde `/consultas`.
- Los nombres visibles de vasos y consumibles se presentan sin guiones bajos y con capitalizacion legible.
- Se retiraron de la interfaz los identificadores de caja y las rutas API que acompanaban el stock diario y los ajustes.
- Los tres formularios operativos mantienen boton principal alineado y altura uniforme en escritorio; en movil se apilan.

## Validacion realizada

### Backend y contratos

- Backend Docker reconstruido y `GET /api/health` respondio correctamente.
- `GET /api/inventario/ajustes` respondio `200` con token de gerente y administrador, y `403` con token de vendedor.
- Se ejecutaron datos controlados: administrador solicito entrada, gerente aprobo; una segunda solicitud fue rechazada; gerente aplico una salida para restaurar el stock inicial.
- Se verificaron auditoria y movimiento de ajuste en el flujo controlado.
- `mvn -Dtest=InventarioIntegrationTest,CajaDiariaIntegrationTest test` completo 16 pruebas sin fallos ni errores.

### Frontend y navegador

- `npx tsc -b --pretty false` completo correctamente.
- `npm run build` completo correctamente fuera del sandbox por la limitacion conocida de `esbuild` dentro del sandbox.
- Gerente: aplicacion directa de stock general disponible aun sin caja abierta.
- Administrador: solicitud de ajuste disponible, sin acciones de aprobacion o rechazo.
- Vendedor: no ve Inventario, Catalogos ni Evidencias; las rutas directas restringidas redirigen al inicio.
- El usuario confirmo la validacion manual final el 2026-07-09.

## Alcance pendiente de otros modulos

- Conteo fisico final y diferencias de cierre: Cierre de caja.
- Alertas de cantidad minima: no aplicables mientras el schema canonico no incluya `cantidad_minima_alerta`.
