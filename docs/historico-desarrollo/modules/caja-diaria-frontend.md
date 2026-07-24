# Pantalla: Panel de caja abierta

## Objetivo

Consultar y mostrar la caja diaria abierta dentro del frontend autenticado de Kontora POS.

## Actor principal

Vendedor / Administrador / Gerente.

## Endpoints consumidos

- `GET /api/cajas-diarias/abierta`
- `POST /api/cajas-diarias`

## Contrato usado

Fuente principal: `docs/modules/caja-diaria.md`.

Respuesta esperada de caja abierta:

```json
{
  "idCajaDiaria": "uuid",
  "fechaOperacion": "2200-01-01",
  "estadoCaja": "abierta",
  "valorBase": 300000.00,
  "fechaApertura": "2026-07-07T07:17:00-05:00",
  "fechaCierre": null,
  "idUsuarioApertura": "uuid",
  "nombreUsuarioApertura": "usuario",
  "idUsuarioCierre": null,
  "nombreUsuarioCierre": null,
  "observaciones": "texto"
}
```

Request de apertura:

```json
{
  "fechaOperacion": "2026-07-07",
  "valorBase": 300000,
  "observaciones": "texto opcional"
}
```

## Validaciones de interfaz

- La consulta se realiza con token activo.
- Se muestra estado de carga mientras se consulta la API.
- Si existe caja abierta, se muestran sus datos.
- Si no existe caja abierta, `vendedor` ve mensaje de solo lectura.
- Si no existe caja abierta, `administrador` y `gerente` ven formulario de apertura.
- `valorBase` debe ser mayor o igual a cero antes de enviar.
- El valor base inicia en `300000` como valor operativo habitual, pero sigue siendo editable antes de abrir la caja.
- La apertura muestra una confirmacion explicita; cancelar no envia la solicitud al backend.
- La fecha seleccionada identifica la jornada de negocio. El backend bloquea una caja duplicada para esa fecha y tambien una segunda caja mientras exista otra en estado `abierta`.
- El mensaje real de error del backend se muestra al usuario.

## Reglas que no debe duplicar el frontend

- Permiso final para abrir caja.
- Deteccion definitiva de caja duplicada.
- Validez final de `fechaOperacion`.
- Persistencia de auditoria por apertura.

El frontend solo mejora la experiencia visual; el backend conserva la autoridad. La restriccion de una sola caja abierta tambien existe como indice parcial en la base de datos, para proteger operaciones concurrentes.

## Respuestas esperadas

- Caja abierta: se muestra `estadoCaja`, `fechaOperacion`, `valorBase`, `fechaApertura`, `nombreUsuarioApertura` y `observaciones`.
- Sin caja abierta: se muestra mensaje o formulario segun rol.
- Apertura exitosa: se muestra la caja devuelta por backend.
- Apertura rechazada: se conserva el formulario y se muestra el mensaje de API.

## Evidencia de prueba

- Build frontend:

```powershell
cd C:\Users\corre\Desktop\Kontora\frontend
npm run build
```

Resultado: exitoso.

- Validacion API local:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/cajas-diarias/abierta"
```

Resultado observado:

```json
{
  "estadoCaja": "abierta",
  "fechaOperacion": "2200-01-01",
  "valorBase": 300000.00
}
```

- Validacion en navegador integrado:

```text
Login vendedor -> /caja muestra panel de caja abierta
Login administrador -> /caja muestra panel de caja abierta
Logout -> vuelve a /login
Consola -> sin errores ni advertencias
```

- Validacion manual del usuario:

```text
El usuario confirmo que se puede continuar despues de verificar el panel en navegador.
```

## Estado posterior

La secuencia inicial de Catalogos, Ventas, Inventario, Gastos y Cierre ya fue completada y validada. El siguiente modulo funcional pendiente de Fase 4 es Deposito, consignaciones y servicios.

## Actualizacion: operaciones financieras de caja

`CajaOperacionesPanel` extiende la pantalla `/caja` para administrador y gerente con:

- Consulta de `GET /api/cajas-diarias/abierta/resumen` para la proyeccion de efectivo fisico sin base.
- Lectura de gastos activos y pago a trabajadores como insumos del cuadre, sin duplicar sus formularios de escritura.
- Separacion visible entre efectivo, transferencias y base de caja.

El pago a trabajadores se administra en `/gastos` y las Adiciones diarias en `/ventas`; Caja conserva ambos datos solo para el resumen financiero. Vendedor no ve estas operaciones administrativas dentro de Caja.

## Actualizacion: apertura segura y jornada nocturna

- Administrador y gerente confirman la apertura antes de ejecutar `POST /api/cajas-diarias`.
- Una caja de la jornada `10` puede cerrarse despues de medianoche: `fechaOperacion` sigue siendo `10` y `fechaCierre` registra la fecha y hora real del cierre.
- Solo despues de cerrar esa jornada puede abrirse la jornada `11`. Mientras la jornada `10` permanezca abierta, backend y base de datos bloquean cualquier nueva apertura.
- El cierre de la jornada no depende de que la fecha fisica coincida con `fechaOperacion`.
