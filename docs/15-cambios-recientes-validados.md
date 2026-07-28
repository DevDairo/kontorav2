# 15. Cambios recientes validados

## Objetivo

Relacionar los ajustes funcionales de la migracion con su implementacion,
documentacion activa y evidencia tecnica. Este documento resume cambios; las
reglas completas permanecen en la guia de cada modulo.

## Ventas y anulaciones

| Cambio | Comportamiento vigente | Documento |
| --- | --- | --- |
| Comprobante de venta | Muestra hora, tipo con o sin licor, tamanos, cantidades, equivalencia en paquetes de 20 vasos y metodo de pago. | [Ventas y pagos](./04-ventas-y-pagos.md) |
| Confirmacion visible | Despues de registrar, la pantalla desplaza el comprobante nuevo al area visible y respeta `prefers-reduced-motion`. | [Ventas y pagos](./04-ventas-y-pagos.md) |
| Anulacion informada | El selector y el detalle muestran hora, tipo de venta, vasos, equivalencia y pagos antes de confirmar. | [Ventas y pagos](./04-ventas-y-pagos.md) |
| Jornada operativa | El registro toma el `idCajaDiaria` abierto y usa su `fechaOperacion` para resolver precios y promociones, incluso si el contenedor ya cambio de dia en UTC. | [Ventas y pagos](./04-ventas-y-pagos.md) |
| Consultas | Cada venta conserva esos datos y las anuladas permanecen como trazabilidad sin aportar a totales vigentes. | [Consultas y evidencias](./11-consultas.md) |

La API de consultas fue validada con ocho pruebas de integracion exitosas. El
frontend y backend se construyeron en imagenes Docker despues de estos cambios.
La correccion de jornada operativa agrego una prueba unitaria de regresion; las
tres pruebas de `VentasServiceTest` finalizaron sin fallos ni errores. La imagen
del backend se reconstruyo, se recreo exclusivamente ese servicio y los health
locales y publicos respondieron correctamente.

## Evidencias

| Cambio | Comportamiento vigente | Documento |
| --- | --- | --- |
| Modulo unificado | La entrada independiente de Evidencias se retiro. Gastos, consignaciones y servicios se gestionan en `/consultas`; comprobantes de venta en `/transferencias`. | [Evidencias integradas](./09-evidencias.md) |
| Vista previa | Imagenes y PDF se obtienen por la API autenticada. La imagen conserva proporcion y dimensiones naturales, se centra y solo se reduce si no cabe. | [Evidencias integradas](./09-evidencias.md) |
| Orientacion movil | El backend aplica la orientacion EXIF antes de comprimir. Vista previa y descarga reciben el mismo archivo ya corregido. | [Evidencias integradas](./09-evidencias.md) |
| Navegacion al visor | Al elegir un soporte, la pagina desplaza suavemente su vista previa al area visible. | [Evidencias integradas](./09-evidencias.md) |
| Transferencias | Pendientes, validadas y rechazadas conservan visor, descarga y soportes historicos segun el rol. | [Transferencias](./10-transferencias.md) |
| Lenguaje operativo | La interfaz usa `Sistema` y mensajes funcionales; no expone nombres de API ni referencias RF al usuario. | [Consultas y evidencias](./11-consultas.md) |

El cliente de Storage fue validado contra el contenedor real y el bucket
privado. Los respaldos coordinados de PostgreSQL y objetos se restauraron y el
hash de una evidencia coincidio con el original.

La correccion de orientacion fue validada con 11 pruebas exitosas. Tres pruebas
unitarias recorren las ocho orientaciones EXIF y la prueba de integracion
confirma que una matriz JPEG horizontal con EXIF `6` llega a Storage como una
imagen vertical.

## Inventario

| Cambio | Comportamiento vigente | Documento |
| --- | --- | --- |
| Pestañas | `Stock diario`, `Cargar stock diario`, `Consumo diario`, `Ingreso al stock general` y `Movimientos y solicitudes` tienen vistas independientes. | [Inventario operativo](./05-inventario-operativo.md) |
| Encabezado contextual | Titulo, categoria e instruccion cambian con la pestaña activa. | [Inventario operativo](./05-inventario-operativo.md) |
| Seleccion de item | Cada operacion selecciona un producto desde una lista y luego solicita solo sus campos necesarios. | [Inventario operativo](./05-inventario-operativo.md) |
| Paquetes | Cada paquete mueve 20 vasos de stock general a stock diario. El formulario no muestra unidades rotas y envia cero. | [Inventario operativo](./05-inventario-operativo.md) |
| Unidades sueltas | Reabastecen stock diario sin crear inventario nuevo: salida general y entrada diaria atomicas por igual cantidad. | [Inventario operativo](./05-inventario-operativo.md) |
| Roles | Gerente aplica directamente; administrador crea solicitud pendiente para decision gerencial. | [Inventario operativo](./05-inventario-operativo.md) |
| Presentacion | El reabastecimiento se suma a `Ingresada`; no existe un indicador separado `Trasladada`. | [Inventario operativo](./05-inventario-operativo.md) |
| Ingreso general | Solo permite entradas. No muestra correcciones o salidas en la interfaz. | [Inventario operativo](./05-inventario-operativo.md) |

Las reglas de inventario fueron validadas con 17 pruebas de integracion
exitosas, incluidos reabastecimiento directo, solicitud/aprobacion, caja
cerrada, stock insuficiente y dos movimientos enlazados.

## Infraestructura relacionada

- PostgreSQL, backend, Storage, Nginx y cloudflared tienen ciclos de vida
  independientes.
- El frontend usa `/api`; ninguna clave de Storage llega al navegador.
- Storage mantiene bucket privado y el backend es su unico consumidor.
- Cloudflare publica solamente `http://frontend:8080`.
- Login publico, preflight CORS, `/healthz` y `/api/health` respondieron `200`.

La evidencia detallada, incluidos errores y correcciones, se conserva en la
[bitacora de migracion](./migracion-infraestructura/bitacora.md).

## Validacion antes de cada version

1. Ejecutar las pruebas backend afectadas.
2. Construir backend y frontend.
3. Validar `/healthz` y `/api/health`.
4. Probar permisos con vendedor, administrador y gerente.
5. Revisar en escritorio y movil ventas, anulacion, evidencias e inventario.
6. Confirmar que no existen solicitudes del navegador hacia `localhost`,
   `127.0.0.1`, Storage o el backend directo cuando se usa el dominio publico.
