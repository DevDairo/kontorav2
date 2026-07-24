# 08. Deposito y servicios

## Objetivo

Administrar el saldo acumulado que recibe efectivo de los cierres y registrar sus salidas por consignaciones bancarias o pagos de servicios.

## Requisitos cubiertos

- RF-48 a RF-53.

## Funcionalidades

- Consulta de saldo actual de deposito.
- Registro de consignacion bancaria con evidencia.
- Registro de pago de servicio con tipo, valor y evidencia.
- Descuento automatico de cada salida y conservacion de saldo anterior y posterior.
- Historial trasladado a Consultas para evitar duplicar informacion entre interfaces.
- Vista previa y descarga autenticada de las evidencias del historial desde
  `Consultas y evidencias`, con desplazamiento al visor seleccionado.

## Permisos

| Rol | Funcionalidad |
| --- | --- |
| Vendedor | No recibe interfaz de Deposito. |
| Administrador | Consulta saldo y registra consignaciones o pagos de servicios. |
| Gerente | Tiene las mismas capacidades administrativas. |

## Reglas clave

- El deposito recibe solo efectivo proveniente de cierre; la base de caja esta excluida.
- No se permite una salida mayor al saldo disponible.
- Las consignaciones y pagos de servicios generan movimiento de deposito y auditoria.
- Las evidencias se adjuntan a la consignacion o pago de servicio, no al saldo.

## Endpoints principales

- `GET /api/deposito/saldo`
- `POST /api/deposito/consignaciones-bancarias`
- `POST /api/deposito/pagos-servicios`
- `GET /api/consultas/deposito/movimientos`
