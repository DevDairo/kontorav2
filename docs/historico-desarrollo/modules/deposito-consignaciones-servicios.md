# Modulo: Deposito, consignaciones y servicios

## Estado tecnico

Backend y pantalla React implementados y validados manualmente con administrador y gerente. La ruta `/deposito` queda marcada como `Base lista`.

## Requisitos funcionales cubiertos

- RF-48: los cierres siguen creando `entrada_cierre` sin incluir la base de caja.
- RF-49: se expone saldo actual e historial completo o por periodo.
- RF-50: se registra consignacion bancaria y se devuelve su identificador para adjuntar evidencia.
- RF-51: se registra pago de servicio contra un tipo de servicio activo y se devuelve su identificador para adjuntar evidencia.
- RF-52: consignaciones y pagos de servicios crean una salida que descuenta el saldo de deposito.
- RF-53: la interfaz usa los endpoints existentes de Evidencias para consignaciones y pagos de servicios.

## Tablas canonicas

- `movimientos_deposito`.
- `consignaciones_bancarias`.
- `pagos_servicios`.
- `tipos_servicio`.
- `archivos_evidencia`.
- `auditoria_operaciones`.
- `usuarios`.

## Endpoints

| Metodo | Ruta | Roles | Proposito |
| :- | :- | :- | :- |
| GET | `/api/deposito/saldo` | Administrador, gerente | Obtener el saldo actual calculado desde el ultimo movimiento. |
| GET | `/api/consultas/deposito/movimientos` | Administrador, gerente | Consultar historial completo; `fechaInicio` y `fechaFin` son opcionales como filtro de periodo. |
| POST | `/api/deposito/consignaciones-bancarias` | Administrador, gerente | Registrar una consignacion y su salida de deposito. |
| POST | `/api/deposito/pagos-servicios` | Administrador, gerente | Registrar un pago de servicio y su salida de deposito. |
| POST | `/api/evidencias/consignaciones-bancarias/{idConsignacionBancaria}` | Administrador, gerente | Adjuntar evidencia multipart de una consignacion. |
| POST | `/api/evidencias/pagos-servicios/{idPagoServicio}` | Administrador, gerente | Adjuntar evidencia multipart de un pago de servicio. |

## Reglas de negocio

- Solo administrador y gerente consultan u operan deposito; vendedor recibe `403`.
- Cada salida requiere un importe positivo con maximo dos decimales.
- El saldo anterior se obtiene bajo un bloqueo transaccional de PostgreSQL para serializar entradas y salidas concurrentes.
- Una consignacion crea `salida_consignacion`; un pago de servicio crea `salida_pago_servicio`.
- El saldo posterior se calcula en backend como `saldo_anterior - valor_salida` y se rechaza la salida si el saldo no alcanza.
- Cada salida crea su detalle unico en `consignaciones_bancarias` o `pagos_servicios`, y audita tanto el movimiento como el registro financiero.
- El tipo de servicio debe existir y estar `activo`.
- La evidencia se carga despues de crear el registro financiero mediante el backend. La interfaz exige seleccionar el archivo y conserva un reintento visible cuando falla el almacenamiento externo.

## Contratos de registro

Consignacion:

```json
{
  "valorConsignado": 25000,
  "observacion": "Opcional"
}
```

Pago de servicio:

```json
{
  "idTipoServicio": "uuid",
  "valorPagado": 12000,
  "descripcion": "Opcional"
}
```

Ambas respuestas incluyen el detalle del movimiento con `saldoAnterior`, `saldoPosterior` e identificador del registro creado para usar en Evidencias.

## Pruebas tecnicas realizadas

- `DepositoIntegrationTest`: 4 pruebas, sin fallos ni errores.
- Se validaron consignacion, pago de servicio, descuento automatico, saldo insuficiente, auditoria, historial y bloqueo de vendedor.
- `mvn clean test`: 64 pruebas, sin fallos ni errores.
- `npx tsc -b --pretty false` y `npm run build`: exitosos.
- Backend Docker reconstruido: `GET /api/health` responde `200`.
- Sin token, `GET /api/deposito/saldo` y `GET /api/consultas/deposito/movimientos` responden `401`, confirmando que las rutas activas permanecen protegidas.

## Validacion manual de cierre frontend

- Administrador abrio una caja de prueba con base de `$300.000` y se registro una adicion controlada de `$50.000`.
- El cierre genero `entrada_cierre` por `$50.000`, sin incluir la base, y el saldo inicial de deposito quedo en `$50.000`.
- Se registraron, con datos controlados, un pago de servicio de `$2.500` y una consignacion bancaria de `$28.000`.
- El historial y el saldo devuelto por backend coincidieron con el calculo: `$50.000 - $2.500 - $28.000 = $19.500`.
- Gerente inicio sesion, visualizo la ruta `/deposito` y consulto saldo e historial; administrador y gerente pueden operar deposito. Vendedor permanece excluido por ruta y backend.
- Los gastos de caja y las salidas de deposito se mantienen separados: el pago de servicio reduce deposito, pero no crea un registro en `gastos_caja`.
- Supabase Storage no se configura localmente por decision de despliegue. Si la carga de evidencia recibe `503`, la operacion financiera ya creada conserva el reintento de evidencia; la carga real se validara al configurar Storage en servidor.

## Reorganizacion frontend posterior

- `/deposito` conserva el saldo actual y los formularios de consignacion bancaria y pago de servicio, con confirmacion y evidencia.
- El historial de `movimientos_deposito`, incluido su filtro por periodo, se centraliza en `/consultas`, pestaña Deposito, con `GET /api/consultas/deposito/movimientos`.
- La lista trasladada conserva iconografia SVG: `Landmark` identifica `entrada_cierre`; `Building2` identifica salidas por consignacion o pago de servicio. Tambien presenta importe, saldo posterior, usuario, fecha, servicio u observacion cuando aplica.
- `DepositoPanel` ya no consulta ese endpoint ni recalcula entradas o salidas desde el historial; el saldo sigue siendo devuelto por `GET /api/deposito/saldo`.
- Validacion tecnica posterior: `npx tsc -b --pretty false`, `npm run build` y navegador confirmaron un historial real de entrada por cierre visible desde Consultas y ausente de Deposito.
