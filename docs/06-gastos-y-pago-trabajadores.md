# 06. Gastos y pago a trabajadores

## Objetivo

Registrar salidas tomadas de la caja diaria y confirmar el pago diario total a trabajadores antes del cierre.

## Requisitos cubiertos

- RF-29 a RF-34.

## Funcionalidades

- Registro de gastos de caja con valor, descripcion y evidencia opcional.
- Edicion y anulacion de gastos con motivo y trazabilidad.
- Consulta y carga de evidencias de gastos desde la operacion y desde
  `Consultas y evidencias`.
- Vista previa autenticada de imagen o PDF, con desplazamiento automatico al
  visor y descarga opcional.
- Descarga de soportes con mensaje uniforme cuando la evidencia solicitada no esta disponible.
- Registro de un unico pago diario total a trabajadores.
- Confirmacion explicita del pago, incluso cuando su valor es cero.
- Proyeccion de efectivo que descuenta gastos y pagos a trabajadores.

## Permisos

| Rol | Funcionalidad |
| --- | --- |
| Vendedor | Registra y consulta gastos de la caja abierta; adjunta soporte autorizado. |
| Administrador | Registra, edita y anula gastos; registra y confirma pago a trabajadores. |
| Gerente | Tiene las mismas capacidades administrativas. |

## Reglas clave

- Los gastos y pagos diarios pertenecen a una caja abierta.
- El pago a trabajadores debe existir y estar confirmado para cerrar caja.
- Gastos y pago a trabajadores reducen el efectivo esperado, no el deposito directamente.
- Las ediciones y anulaciones de gastos se registran en auditoria.

## Endpoints principales

- `GET /api/operaciones-caja/gastos-caja/abierta`
- `POST /api/operaciones-caja/gastos-caja`
- `PUT /api/operaciones-caja/gastos-caja/{idGastoCaja}`
- `POST /api/operaciones-caja/gastos-caja/{idGastoCaja}/anular`
- `POST /api/operaciones-caja/pagos-trabajadores-diarios`
- `POST /api/operaciones-caja/pagos-trabajadores-diarios/{idPagoTrabajadoresDiario}/confirmar`
