# 04. Ventas y pagos

## Objetivo

Registrar ventas de granizados, aplicar precios y promociones vigentes, distribuir pagos y actualizar el stock diario de vasos.

## Requisitos cubiertos

- RF-13 a RF-28.

## Funcionalidades

- Venta de granizados con o sin licor por tamano de vaso.
- Precio historico aplicado desde la vigencia activa.
- Promocion configurable para granizados con licor: pares del mismo tamano y reglas de dia segun comprador.
- Venta a cliente o a trabajador seleccionado desde usuarios activos.
- Pagos en efectivo, transferencia o mixtos.
- Registro de valor recibido y cambio para pagos en efectivo.
- En transferencia unica, el valor se completa automaticamente con el total de la venta.
- En pago mixto, se validan el aporte de transferencia y el efectivo restante antes de habilitar el registro.
- Carga inicial de evidencia cuando el pago incluye transferencia.
- El comprobante de la venta registrada se muestra inmediatamente despues del formulario e incluye hora, tipo con o sin licor, tamanos de vaso, equivalencia de vasos en paquetes de 20 y metodo de pago.
- Al completar el registro, la pantalla desplaza suavemente el comprobante nuevo al area visible para que el usuario confirme los datos de la operacion.
- Panel de anulacion para seleccionar una venta registrada de la jornada, consultar hora, tipo con o sin licor, tamanos y cantidades de vasos, equivalencia en paquetes y metodos de pago, indicar el motivo y confirmar la operacion.
- Anulacion autorizada de venta abierta, con motivo, trazabilidad y restauracion del stock diario de vasos, sin depender de si el pago fue en efectivo, transferencia o mixto.

## Permisos

| Rol | Funcionalidad |
| --- | --- |
| Vendedor | Registra ventas y consulta sus operaciones autorizadas. |
| Administrador | Registra ventas y puede anular ventas mientras la caja esta abierta. |
| Gerente | Tiene las mismas capacidades administrativas. |

## Reglas clave

- Toda venta exige una caja diaria abierta.
- La vigencia de precios y promociones se evalua con `fechaOperacion` de esa
  caja abierta. La venta queda asociada al mismo `idCajaDiaria` y no depende de
  la fecha UTC, de la zona horaria del contenedor ni de la fecha del dispositivo
  que usa el punto de venta. La vista previa del frontend consulta esa misma
  fecha operativa antes de cargar precios y promociones.
- La promocion general para clientes aplica por pares del mismo tamano durante
  martes y miercoles de la caja operativa.
- Fuera de los dias de promocion general, cada trabajador puede usar un unico
  beneficio 2x con licor por caja operativa. Si compra cuatro vasos del mismo
  tamano, solo dos reciben el beneficio y los otros dos se cobran a precio
  normal. Las ventas posteriores del mismo trabajador en esa caja se cobran a
  precio normal.
- En martes y miercoles, el trabajador participa en la promocion general por
  pares sin que el limite del beneficio laboral altere esa regla. Una promocion
  general no consume el beneficio especial del trabajador.
- El limite se valida en backend por `idCajaDiaria` e
  `idUsuarioComprador`. Las ventas anuladas no consumen el beneficio y las
  solicitudes simultaneas del mismo trabajador se serializan para impedir un
  uso doble.
- La suma de pagos debe coincidir con el total de la venta.
- El pago mixto requiere una transferencia mayor que cero y menor que el total; el efectivo recibido debe cubrir el saldo restante. Si la transferencia cubre el total, se usa el metodo Transferencia.
- Cada venta descuenta vasos segun el tamano; la anulacion devuelve al stock diario todos los vasos de sus lineas, para efectivo, transferencia y pago mixto.
- Solo administrador y gerente pueden anular; vendedor no visualiza la accion y el sistema tambien protege el endpoint.
- Solo se anulan ventas en estado `registrada` de una caja abierta; el registro permanece como `anulada` para consulta y auditoria. Sus pagos y evidencias no se eliminan, pues conservan la trazabilidad de la operacion.
- Una venta anulada deja de aportar al total vendido, al efectivo, a las transferencias y a los valores calculados para el cierre de caja.
- El panel de anulacion consulta la fecha operativa de la caja abierta. Por ello sigue mostrando las ventas de la jornada aunque esta termine despues de medianoche.
- Las transferencias se crean como pendientes hasta su decision posterior.
- El beneficio de trabajador esta disponible para usuarios activos, incluidos administrador y gerente, bajo la regla vigente de promociones.

## Endpoints principales

- `POST /api/ventas`
- `GET /api/ventas/{idVenta}/anulacion`
- `POST /api/ventas/{idVenta}/anular`
- `GET /api/ventas/trabajadores`
- `GET /api/consultas/ventas`
