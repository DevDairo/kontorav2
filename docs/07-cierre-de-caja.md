# 07. Cierre de caja

## Objetivo

Consolidar la jornada, comparar el efectivo contado con el esperado, registrar diferencias y calcular el valor que pasa al deposito.

## Requisitos cubiertos

- RF-08 a RF-12.
- RF-48.

## Funcionalidades

- Resumen de ventas, efectivo, transferencias por estado, adiciones, gastos y pago a trabajadores.
- Ingreso de efectivo contado sin incluir la base de caja.
- Calculo de diferencia y valor a deposito.
- Confirmacion antes de cerrar.
- Consulta persistente del ultimo cierre y consulta historica por fecha.
- Refresco de valores al abrir una nueva caja.

## Permisos

| Rol | Funcionalidad |
| --- | --- |
| Vendedor | No recibe interfaz de Cierre. |
| Administrador | Consulta resumen, cierra caja y revisa historial autorizado. |
| Gerente | Tiene las mismas capacidades administrativas. |

## Reglas clave

- Deben existir adiciones y pago a trabajadores confirmado, incluso si este ultimo vale cero.
- El efectivo esperado sin base no puede ser negativo.
- Una caja puede cerrarse sin ventas si cumple los requisitos operativos.
- La base no se deposita.
- El cierre bloquea nuevas ventas y anulaciones de la jornada.
- Si hay efectivo disponible, se crea el movimiento de deposito correspondiente.

## Endpoints principales

- `GET /api/cajas-diarias/abierta/resumen`
- `POST /api/cajas-diarias/{idCajaDiaria}/cerrar`
- `GET /api/cajas-diarias/{idCajaDiaria}/cierre`
- `GET /api/consultas/cierre?fecha={fechaOperacion}`
