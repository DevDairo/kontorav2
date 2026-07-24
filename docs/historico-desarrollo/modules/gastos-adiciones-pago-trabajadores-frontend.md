# Pantalla: Gastos y pago a trabajadores

## Estado

Completado y validado manualmente en navegador el 2026-07-10.

## Objetivo

Operar gastos de caja y pago total a trabajadores contra los contratos reales del backend, manteniendo el cuadre financiero visible en Caja. Las adiciones diarias se registran desde Ventas por su relacion con los productos vendidos.

## Distribucion de interfaz

| Ruta | Contenido | Roles visibles |
| :- | :- | :- |
| `/gastos` | Registrar, listar, editar y anular gastos; registrar, confirmar o actualizar pago a trabajadores. | Gastos: vendedor, administrador y gerente. Pago y acciones administrativas: administrador y gerente. |
| `/ventas` | Registro de ventas y Adiciones diarias de la jornada. | Vendedor, administrador y gerente; el formulario de adiciones se habilita solo con caja abierta. |
| `/caja` | Apertura o detalle de caja, resumen y proyeccion de efectivo fisico. | Apertura y proyeccion: administrador y gerente. |

Caja conserva el valor de adiciones y pago a trabajadores como datos de lectura para el cuadre; no duplica los formularios que se administran desde Ventas y Gastos.

## Endpoints consumidos

### Gastos

- `GET /api/operaciones-caja/gastos-caja/abierta`.
- `POST /api/operaciones-caja/gastos-caja`.
- `PUT /api/operaciones-caja/gastos-caja/{idGastoCaja}`.
- `POST /api/operaciones-caja/gastos-caja/{idGastoCaja}/anular`.
- `GET /api/operaciones-caja/pagos-trabajadores-diarios/abierta`.
- `POST /api/operaciones-caja/pagos-trabajadores-diarios`.
- `POST /api/operaciones-caja/pagos-trabajadores-diarios/{idPagoTrabajadoresDiario}/confirmar`.

### Ventas y Caja

- `GET /api/cajas-diarias/abierta`.
- `GET /api/operaciones-caja/adiciones-diarias/abierta`.
- `POST /api/operaciones-caja/adiciones-diarias`.
- `POST /api/cajas-diarias`.
- `GET /api/cajas-diarias/abierta/resumen`.

## Reglas de interfaz

- La ausencia de caja abierta deshabilita las operaciones diarias y muestra el error real del backend.
- Vendedor solo registra y consulta gastos; no ve pago a trabajadores, proyeccion ni acciones de edicion o anulacion. Las adiciones se administran desde Ventas si existe caja abierta.
- Administrador y gerente pueden editar o anular gastos con motivo obligatorio.
- El pago a trabajadores puede actualizarse aun confirmado mientras la caja permanezca abierta; un valor cero exige confirmacion explicita.
- Los importes aceptan entrada por teclado o pegado, incluyendo separadores de miles y hasta dos decimales.
- Los paneles de Gastos y Pago a trabajadores se estiran a la misma altura y anclan sus botones principales al pie en escritorio; en movil se apilan sin altura fija.
- La proyeccion de Caja diferencia ventas en efectivo, adiciones, gastos activos, pago a trabajadores, transferencias y base. El deposito se determina posteriormente en Cierre con el efectivo contado sin base.

## Archivos principales

```text
frontend/src/modules/gastos/components/GastosPanel.tsx
frontend/src/modules/gastos/services/gastosService.ts
frontend/src/modules/gastos/types.ts
frontend/src/modules/caja/components/CajaOperacionesPanel.tsx
frontend/src/modules/ventas/components/AdicionesDiariasPanel.tsx
frontend/src/shared/utils/moneyInput.ts
frontend/src/app/routes/appRoutes.ts
```

## Validacion realizada

- `mvn clean test`: 58 pruebas, sin fallos ni errores.
- `npx tsc -b --pretty false`: exitoso.
- `npm run build`: exitoso.
- `GET /api/cajas-diarias/abierta/resumen`: `200` para administrador y gerente, `403` para vendedor.
- Servidor React local: `/caja` y `/gastos` respondieron `200`.
- Validacion manual del usuario: flujo de gastos, pago a trabajadores, distribucion Caja/Gastos y uniformidad visual confirmados.

## Siguiente modulo

Fase 4: Cierre de caja y deposito, usando `POST /api/cajas-diarias/{idCajaDiaria}/cerrar` y `GET /api/cajas-diarias/{idCajaDiaria}/cierre`.
