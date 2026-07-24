# Pantalla: Cierre de caja y deposito

## Objetivo

Permitir a administrador y gerente consultar el resumen financiero de la jornada abierta, registrar el efectivo fisico contado sin base y revisar cierres persistidos por fecha.

## Actores

- Administrador.
- Gerente.

Vendedor no ve la ruta `/cierre`; backend conserva la validacion definitiva de permisos.

## Archivos principales

```text
frontend/src/modules/cierre/components/CierreCajaPanel.tsx
frontend/src/modules/cierre/services/cierreService.ts
frontend/src/modules/cierre/types.ts
frontend/src/shared/components/ConfirmationDialog.tsx
```

## Endpoints consumidos

- `GET /api/cajas-diarias/abierta`.
- `GET /api/cajas-diarias/abierta/resumen`.
- `POST /api/cajas-diarias/{idCajaDiaria}/cerrar`.
- `GET /api/cajas-diarias/{idCajaDiaria}/cierre`.
- `GET /api/consultas/cierre?fecha={fechaOperacion}`.

La consulta por fecha es la fuente de persistencia para recuperar un cierre despues de recargar la pagina. El valor guardado en `sessionStorage` solo propone la ultima fecha consultada; no sustituye la respuesta del backend.

## Flujo operativo

1. La pantalla consulta la caja abierta y su resumen calculado por backend.
2. Muestra efectivo, transferencias, gastos, adiciones, pago a trabajadores y base de caja por separado.
3. Administrador o gerente ingresa el efectivo contado sin base y confirma que no incluyo el valor base.
4. El cuadro de confirmacion muestra la jornada y el efectivo contado antes de enviar el cierre. Cancelar no ejecuta ninguna solicitud.
5. Backend registra el cierre, la diferencia y, cuando el deposito es positivo, el movimiento automatico `entrada_cierre`.
6. La pantalla muestra el resultado persistido y permite abrir el historial por fecha sin alterar una caja abierta posterior.

## Validaciones de interfaz

- El conteo debe ser numerico y mayor o igual a cero.
- El conteo se bloquea cuando el efectivo esperado sin base es negativo.
- Se requiere la confirmacion de que el conteo excluye la base de caja.
- El boton de cierre permanece deshabilitado hasta que backend informe que existen adiciones y pago a trabajadores confirmado.
- El formulario muestra una confirmacion antes de ejecutar el cierre. Despues de confirmarlo, backend conserva la autoridad sobre caja abierta, permisos y reglas contables.
- Los importes aceptan escritura y pegado mediante la normalizacion compartida de importes.

## Jornada nocturna y apertura siguiente

- `fechaOperacion` identifica la jornada que se abrio, no la hora fisica del cierre.
- Una caja abierta para el dia `10` se puede cerrar el dia `11` despues de medianoche; `fechaCierre` guarda el instante real de la operacion y la jornada sigue siendo `10`.
- Una vez cerrada la jornada `10`, puede abrirse la del `11`.
- Mientras exista una caja `abierta`, no se puede abrir otra caja para la misma fecha ni para una fecha distinta. Esta regla esta protegida por servicio backend y por el indice parcial `uq_cajas_diarias_una_abierta`.

## Respuestas visibles

- Cierre exitoso: efectivo esperado, efectivo contado, diferencia, valor a deposito, responsable y desglose de jornada.
- Deposito positivo: se muestra el saldo anterior y posterior devuelto por el movimiento creado.
- Deposito en cero: se informa que backend no crea movimiento de deposito.
- Error de API: se conserva el formulario y se muestra el mensaje real de backend.
- Consulta historica: muestra el cierre de la fecha seleccionada y permite volver a la operacion actual.

## Evidencia de prueba

- `mvn clean test`: 60 pruebas, sin fallos ni errores.
- `npx tsc -b --pretty false`: exitoso.
- `npm run build`: exitoso.
- Backend Docker reconstruido; `GET /api/health` respondio `200`.
- `/caja` y `/cierre` respondieron `200` desde el servidor React local.
- El usuario confirmo en navegador el cierre, el valor base por defecto editable, la consulta de cierres persistidos, el regreso desde historial, y las confirmaciones de apertura y cierre.
