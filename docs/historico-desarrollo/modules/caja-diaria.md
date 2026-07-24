# Modulo: Caja diaria

## Objetivo

Permitir abrir y consultar la caja diaria como eje operativo de ventas, gastos, inventario diario y cierre posterior.

## Tablas involucradas

- `cajas_diarias`
- `cierres_caja`

## Fuente de verdad del modelo

El modulo se implementa sobre las tablas reales del schema:

- `cajas_diarias.fecha_operacion` es unica.
- `cajas_diarias.estado_caja` usa el enum `estado_caja_enum`.
- `cajas_diarias.valor_base` existe como base fija de apertura y se excluye de efectivo contado y deposito.
- `cajas_diarias.id_usuario_apertura` registra el usuario que abre la caja.
- `cierres_caja` queda mapeada para el cierre posterior, pero el cierre contable completo corresponde al modulo de cierre de caja y deposito.

## Endpoints

| Metodo | Ruta | Autenticacion | Proposito |
| :- | :- | :- | :- |
| POST | `/api/cajas-diarias` | Si | Abrir una caja diaria. |
| GET | `/api/cajas-diarias/abierta` | Si | Consultar la caja abierta mas reciente. |
| GET | `/api/cajas-diarias/fecha/{fechaOperacion}` | Si | Consultar caja por fecha de operacion. |

## Flujo de apertura

1. El usuario inicia sesion.
2. El backend valida que el rol sea `administrador` o `gerente`.
3. El backend valida que no exista caja para la misma `fecha_operacion`.
4. Se crea un registro en `cajas_diarias` con:
   - `estado_caja = 'abierta'`.
   - `fecha_apertura` actual.
   - `id_usuario_apertura` del usuario autenticado.
   - `valor_base` recibido en la solicitud.
5. La caja queda lista para que otros modulos registren ventas, gastos e inventario operativo.

## Roles permitidos

- `administrador`
- `gerente`

El rol `vendedor` no puede abrir caja diaria.

## Reglas de negocio implementadas

- Sin usuario autenticado no se puede abrir caja.
- Solo `administrador` o `gerente` puede abrir caja.
- No se permite abrir mas de una caja para la misma `fecha_operacion`.
- La caja nueva queda en estado `abierta`.
- El usuario de apertura queda asociado en `id_usuario_apertura`.
- No se agregan migraciones nuevas: el schema canonico ya contiene `cajas_diarias` y `cierres_caja`.

## Pruebas realizadas

- `mvn clean test`.
- Rechazo de apertura sin autenticacion.
- Rechazo de apertura con rol `vendedor`.
- Apertura exitosa con rol `administrador`.
- Apertura exitosa con rol `gerente` en escenario de duplicidad inicial.
- Persistencia de caja en `cajas_diarias`.
- Consulta por fecha.
- Consulta de caja abierta.
- Rechazo de segunda caja para la misma fecha.

## Pendientes

- El cierre contable completo se implementara en el modulo "Cierre de caja y deposito".
- Las reglas transversales de bloqueo para ventas, gastos e inventario se reforzaran en sus modulos correspondientes, apoyadas por los triggers ya definidos en la base de datos.
