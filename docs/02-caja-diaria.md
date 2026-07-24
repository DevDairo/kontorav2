# 02. Caja diaria

## Objetivo

Controlar la jornada de trabajo mediante una unica caja abierta, su base de efectivo y los valores operativos necesarios para el cierre.

## Requisitos cubiertos

- RF-06 a RF-12.

## Funcionalidades

- Apertura de una caja por fecha, con base, responsable y observaciones.
- Valor de base sugerido de `$300.000`, editable antes de abrir la caja.
- Consulta de caja abierta y proyeccion de efectivo esperado sin base.
- Registro del total diario de adiciones mientras la caja esta abierta.
- Bloqueo de ventas, gastos, paquetes y consumos cuando no existe caja abierta.
- Confirmacion previa de apertura y control de una sola caja por fecha.

## Permisos

| Rol | Funcionalidad |
| --- | --- |
| Vendedor | Consulta la caja abierta requerida para su operacion. |
| Administrador | Abre caja, consulta resumen y registra operaciones de caja autorizadas. |
| Gerente | Tiene las mismas capacidades administrativas. |

## Reglas clave

- No puede existir mas de una caja para la misma fecha ni mas de una caja abierta.
- La jornada puede cerrarse despues de medianoche: la fecha operativa elegida permanece como referencia de la caja.
- La base de caja se conserva y no forma parte del deposito.
- Apertura y cierre quedan auditados.

## Endpoints principales

- `POST /api/cajas-diarias`
- `GET /api/cajas-diarias/abierta`
- `GET /api/cajas-diarias/abierta/resumen`
- `GET /api/operaciones-caja/adiciones-diarias/abierta`
- `POST /api/operaciones-caja/adiciones-diarias`
