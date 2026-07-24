# 13. Auditoria

## Objetivo

Permitir al gerente revisar la trazabilidad de acciones sensibles sin modificar el historial registrado.

## Requisitos cubiertos

- RF-05 y RF-56.

## Funcionalidades

- Consulta por periodo de registros en `auditoria_operaciones`.
- Visualizacion de usuario responsable, fecha, entidad afectada, accion, IP, descripcion y snapshots de valores.
- Presentacion legible de snapshots JSON mediante puntos de lectura.
- Resolucion de identificadores de usuario a nombre y usuario cuando la informacion esta disponible.
- Registro de accesos, sesiones, aperturas, cierres, anulaciones, cambios administrativos, ajustes, transferencias y evidencias segun el flujo correspondiente.

## Permisos

| Rol | Funcionalidad |
| --- | --- |
| Vendedor | No recibe interfaz ni permiso de auditoria. |
| Administrador | No recibe interfaz ni permiso de auditoria transversal. |
| Gerente | Consulta toda la auditoria, incluida la de seguridad. |

## Reglas clave

- Auditoria registra operaciones sensibles; no es el historial general de ventas, gastos o movimientos.
- Para ventas, gastos, inventario, cierre o deposito diarios se usa Consultas.
- Un resultado vacio puede significar que no ocurrio una accion sensible para el periodo elegido.
- La fecha filtrada es `fecha_accion`, no la fecha operativa de caja.

## Endpoint principal

- `GET /api/consultas/auditoria?fechaInicio=YYYY-MM-DD&fechaFin=YYYY-MM-DD`
