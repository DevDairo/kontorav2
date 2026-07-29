# 16. Cortesías, pérdidas y correcciones de inventario

## Objetivo

Documentar la implementación completa incorporada para:

- registrar cortesías sin convertirlas en ventas;
- devolver vasos cargados por error desde el stock diario al general;
- registrar vasos rotos o perdidos con evidencia fotográfica diferida;
- conservar la trazabilidad en inventario, consultas y auditoría;
- separar las operaciones sensibles en paneles propios y adaptables.

Este documento explica los cambios por capa. Las reglas funcionales de uso
diario permanecen en:

- [Ventas y pagos](./04-ventas-y-pagos.md);
- [Inventario operativo](./05-inventario-operativo.md);
- [Evidencias integradas](./09-evidencias.md);
- [Consultas y evidencias](./11-consultas.md);
- [Auditoría](./13-auditoria.md).

## Principios preservados

1. El backend continúa siendo la autoridad para permisos, caja abierta, stock y
   confirmaciones.
2. El frontend no calcula ni modifica precios, promociones o valores del cierre.
3. El stock general disminuye cuando los vasos pasan al stock diario; el cierre
   no vuelve a descontarlos.
4. Cortesías y pérdidas disminuyen únicamente el stock diario disponible.
5. El cierre financiero no crea movimientos de inventario.
6. Una corrección nunca borra el movimiento original: genera el movimiento
   inverso y conserva ambos.
7. Las evidencias permanecen en el bucket privado y se acceden mediante la API
   autenticada.

## Resumen por módulo

| Módulo | Base de datos | Backend | Frontend |
| --- | --- | --- | --- |
| Cortesías | Flyway V3 crea cabecera, detalles, estados y contador diario. | Registra, consulta y anula; descuenta o restaura stock diario y audita. | Vista propia dentro de Ventas, confirmación e histórico de caja abierta. |
| Devolución diario→general | Reutiliza `ajustes_inventario` y `movimientos_inventario`; no requiere migración. | Transfiere unidades de forma atómica y respeta aprobación por rol. | Panel propio con paquetes de 20 o vasos por unidad. |
| Vasos rotos | Flyway V4 crea pérdidas y las relaciona con evidencias. | Registra, consulta y anula pérdidas; permite evidencia posterior al cierre. | Panel propio de registro, consulta, evidencia y anulación. |
| Consultas | V3 agrega `cantidad_cortesia` al stock diario. | Proyecta cortesías, pérdidas y contadores en consultas autorizadas. | Vista de novedades e indicadores de vendidos, cortesías y pérdidas. |
| Auditoría | Usa `auditoria_operaciones` existente. | Registra creación, anulación, ajustes y evidencias. | El gerente consulta las acciones sin modificar los registros. |

---

## 1. Cambios de base de datos

### 1.1. Flyway V3: cortesías

Archivo:

```text
backend/src/main/resources/db/migration/V3__cortesias_inventario.sql
```

La migración:

- crea `estado_cortesia_enum` con `registrada` y `anulada`;
- crea `tipo_beneficiario_cortesia_enum` con `trabajador` y `otro`;
- agrega `cortesia` y `anulacion_cortesia` al tipo de movimientos;
- agrega `cantidad_cortesia INTEGER NOT NULL DEFAULT 0` a
  `existencias_inventario_diario`;
- crea `cortesias`;
- crea `detalles_cortesia`;
- agrega restricciones para beneficiario, cantidad, estado y anulación;
- evita repetir el mismo tipo y tamaño dentro de una cortesía;
- crea índices por caja, fecha, beneficiario y estado;
- exige caja abierta mediante triggers para la cabecera y sus detalles.

La tabla `cortesias` conserva:

- caja operativa;
- usuario que registra;
- tipo de beneficiario;
- trabajador beneficiario o referencia de otro;
- motivo cuando el beneficiario es otro;
- fecha y estado;
- usuario, fecha y motivo de anulación.

`detalles_cortesia` contiene solamente el producto, tamaño y cantidad. No
contiene precio, promoción, pago ni valor financiero.

### 1.2. Flyway V4: pérdidas y evidencias

Archivo:

```text
backend/src/main/resources/db/migration/V4__perdidas_vasos_con_evidencia.sql
```

La migración:

- crea `estado_perdida_inventario_enum` con `registrada` y `anulada`;
- agrega `anulacion_perdida` al tipo de movimientos;
- crea `perdidas_inventario`;
- permite relacionar opcionalmente la pérdida con un paquete abierto;
- conserva cantidad, motivo, usuario, fecha, estado y datos de anulación;
- valida que el paquete corresponda a la misma caja y al mismo vaso;
- agrega índices por caja, fecha, item, paquete y estado;
- agrega `id_perdida_inventario` a `archivos_evidencia`;
- actualiza la restricción de relación única de evidencias;
- crea el índice de evidencias por pérdida.

Una evidencia pertenece exactamente a uno de estos procesos:

- pago de venta;
- gasto de caja;
- consignación bancaria;
- pago de servicio;
- pérdida de inventario.

### 1.3. Devolución de stock diario

No se creó una migración adicional. La operación usa el modelo vigente:

- `ajustes_inventario.tipo_stock = 'diario'`;
- `ajustes_inventario.sentido_ajuste = 'salida'`;
- `movimientos_inventario` para las dos partes de la transferencia.

El mismo ajuste enlaza:

1. salida del stock diario;
2. entrada al stock general.

### 1.4. Fórmula del stock diario

La cantidad teórica queda definida como:

```text
inicial
+ ingresada
+ ajustada
- vendida
- perdida
- cortesia
= final teorica
```

`cantidad_ajustada` es neta:

- aumenta al trasladar vasos del general al diario;
- disminuye al devolver vasos del diario al general.

---

## 2. Cambios del backend

### 2.1. Módulo Cortesías

Paquete:

```text
backend/src/main/java/com/kontora/pos/cortesias
```

Incluye controlador, servicio, entidades, repositorios y DTOs.

Endpoints:

- `POST /api/cortesias`;
- `GET /api/cortesias/caja-abierta`;
- `GET /api/cortesias?fechaInicio=YYYY-MM-DD&fechaFin=YYYY-MM-DD`;
- `POST /api/cortesias/{idCortesia}/anular`.

Reglas:

- solo administrador o gerente;
- exige caja abierta;
- exige confirmación explícita;
- beneficiario `trabajador` requiere usuario activo;
- beneficiario `otro` requiere motivo y admite referencia opcional;
- solo admite productos y tamaños activos;
- valida stock diario suficiente con bloqueo transaccional;
- incrementa `cantidad_cortesia`;
- crea movimiento `cortesia`;
- no crea venta, precio, promoción ni pago;
- la anulación solo procede con la misma caja abierta;
- exige confirmar que no fue entregada ni consumida;
- restaura stock diario y crea `anulacion_cortesia`;
- conserva el registro como `anulada`;
- registra creación y anulación en auditoría.

### 2.2. Devolución del stock diario al general

Se amplió `InventarioService` para aceptar ajustes diarios en ambos sentidos.

Reglas:

- solo aplica a vasos controlados por paquetes;
- exige caja abierta y stock diario suficiente;
- recibe cantidades reales de vasos;
- el modo `paquetes` del frontend convierte cada paquete en 20 unidades antes
  de enviar la solicitud;
- el gerente aplica la devolución directamente;
- el administrador crea una solicitud pendiente;
- la aprobación vuelve a validar caja y existencias;
- en una transacción disminuye `cantidad_ajustada` diaria y aumenta la
  existencia general;
- recalcula la cantidad final teórica;
- crea dos movimientos enlazados al mismo ajuste;
- solicitud, aprobación o rechazo permanecen auditados.

La devolución corrige una carga equivocada. No debe utilizarse para registrar
ventas, cortesías, pérdidas o diferencias físicas.

### 2.3. Módulo de pérdidas de vasos

Componentes principales:

```text
backend/src/main/java/com/kontora/pos/inventario/controller/PerdidasInventarioController.java
backend/src/main/java/com/kontora/pos/inventario/service/PerdidasInventarioService.java
backend/src/main/java/com/kontora/pos/inventario/domain/PerdidaInventario.java
```

Endpoints:

- `POST /api/inventario/perdidas-vasos`;
- `GET /api/inventario/perdidas-vasos/caja-abierta`;
- `GET /api/inventario/perdidas-vasos?fechaInicio=YYYY-MM-DD&fechaFin=YYYY-MM-DD`;
- `POST /api/inventario/perdidas-vasos/{idPerdidaInventario}/anular`.

Reglas:

- solo administrador o gerente;
- el registro exige caja abierta, tamaño, cantidad, motivo y confirmación;
- el paquete abierto es opcional;
- si se relaciona un paquete, debe pertenecer a la misma caja y vaso;
- el stock diario se bloquea y se valida antes de descontar;
- aumenta `cantidad_perdida`;
- crea movimiento `perdida`;
- la evidencia es opcional al registrar;
- la pérdida permanece válida si Storage rechaza una carga posterior;
- la anulación solo procede mientras la misma caja sigue abierta;
- exige confirmar que el vaso no estaba roto ni fue perdido;
- disminuye `cantidad_perdida`, restaura el stock diario y crea
  `anulacion_perdida`;
- la anulación conserva las evidencias ya vinculadas;
- creación y anulación quedan auditadas.

### 2.4. Evidencias de pérdidas

Endpoints:

- `POST /api/evidencias/perdidas-inventario/{idPerdidaInventario}`;
- `GET /api/evidencias/perdidas-inventario/{idPerdidaInventario}`;
- `GET /api/evidencias/{idArchivoEvidencia}`;
- `GET /api/evidencias/{idArchivoEvidencia}/descargar`.

Reglas:

- solo administrador o gerente;
- solo admite imágenes;
- puede adjuntarse con la caja abierta o cerrada;
- no se admiten nuevas evidencias para una pérdida anulada;
- la carga se almacena bajo
  `perdidas-inventario/{idPerdidaInventario}/...`;
- si el objeto llega a Storage pero la transacción de metadata falla, el
  backend intenta eliminarlo de forma compensatoria;
- cada carga exitosa crea auditoría.

### 2.5. Consultas y cierre

- Las respuestas de inventario incluyen `cantidadCortesiaDiaria`.
- Consultas conserva cortesías y pérdidas anuladas para trazabilidad.
- El cierre puede completarse con una pérdida cuya evidencia siga pendiente.
- El cierre no modifica stock general ni diario.
- La fotografía pendiente puede adjuntarse después del cierre por administrador
  o gerente.

---

## 3. Cambios del frontend

### 3.1. Ventas

La ruta `/ventas` presenta navegación compacta y desplazable:

- `Ventas`;
- `Cortesía`;
- `Anulación`.

Las vistas son independientes para evitar acciones accidentales.

La vista de Cortesía:

- utiliza la plantilla visual de inventario;
- muestra productos a la izquierda y datos de operación a la derecha;
- selecciona beneficiario, producto, tamaño y cantidad;
- pide confirmación antes de registrar;
- muestra las cortesías de la caja abierta;
- permite anular únicamente bajo la confirmación de no entrega.

La vista de Anulación:

- ya no aparece dentro del formulario de venta;
- lista las ventas anulables a la izquierda;
- muestra vendedor, comprador, subtotal, descuento, total, hora, productos,
  vasos, cantidades, pagos y estados a la derecha;
- exige motivo y confirmación;
- conserva las reglas existentes del backend.

### 3.2. Inventario

La ruta `/inventario` presenta:

- `Stock diario`;
- `Cargar stock diario`;
- `Consumo diario`;
- `Devolver al general`;
- `Vasos rotos`;
- `Ajustar stock general`;
- `Movimientos y solicitudes`.

Devolución:

- ofrece `Paquetes (20 vasos)` y `Vasos por unidad`;
- muestra la equivalencia antes de confirmar;
- envía unidades reales al backend;
- presenta lista, selección, campos y acción con la plantilla común.

Vasos rotos:

- selecciona el tamaño desde el stock diario;
- registra cantidad y motivo;
- permite adjuntar una fotografía ahora o posteriormente;
- consulta pérdidas por periodo;
- muestra evidencias existentes;
- permite completar evidencia después del cierre;
- permite anular solo mientras la misma caja permanece abierta.

Ajustar stock general:

- el gerente registra entradas o salidas directamente;
- el administrador solicita entradas o salidas;
- el gerente aprueba o rechaza solicitudes;
- los items inactivos no aparecen en los formularios operativos;
- el historial permanece intacto aunque un item se inactive.

### 3.3. Consultas

La vista administrativa de novedades muestra:

- cortesías por periodo;
- pérdidas por periodo;
- estados registrados o anulados;
- cantidades, tamaños, responsable y motivo;
- evidencia pendiente o cantidad de evidencias;
- visor, descarga y carga autorizada.

El inventario actual presenta:

- vendidos;
- cortesías;
- pérdidas;
- disponibilidad teórica.

### 3.4. Diseño adaptable y lenguaje

La plantilla común usa:

- lista de selección;
- tarjeta del elemento seleccionado;
- campos de operación;
- acción compacta;
- confirmación.

En pantallas pequeñas los bloques se apilan en ese orden y pierden
explícitamente las posiciones de la cuadrícula de escritorio. Las pestañas de
Ventas se desplazan horizontalmente cuando no caben.

Los mensajes visibles usan lenguaje funcional. Por ejemplo:

```text
La disponibilidad final siempre la valida el sistema.
```

No se muestran términos internos como `backend` al usuario.

---

## 4. Permisos consolidados

| Operación | Vendedor | Administrador | Gerente |
| --- | --- | --- | --- |
| Registrar venta | Sí | Sí | Sí |
| Registrar cortesía | No | Sí | Sí |
| Anular cortesía | No | Sí | Sí |
| Devolver diario→general | No | Solicita | Directo |
| Registrar pérdida | No | Sí | Sí |
| Adjuntar evidencia de pérdida con caja cerrada | No | Sí | Sí |
| Anular pérdida con la misma caja abierta | No | Sí | Sí |
| Ajustar stock general | No | Solicita | Directo |
| Resolver solicitudes | No | No | Sí |
| Consultar auditoría transversal | No | No | Sí |

## 5. Validación técnica

Las pruebas agregadas cubren:

- cierre con pérdida y evidencia pendiente;
- carga de evidencia después del cierre;
- rechazo de permisos al vendedor;
- conservación de la pérdida si falla Storage;
- registro y anulación de cortesía;
- descuento y restauración de stock diario;
- devolución directa diario→general;
- solicitud y aprobación de devolución;
- stock insuficiente y caja cerrada;
- movimientos inversos y auditoría.

Se confirmó una ejecución de 21 pruebas de integración de cierre y evidencias
sin fallos. Después de los cambios de interfaz se reconstruyó el frontend y su
healthcheck respondió correctamente. Antes de publicar una versión definitiva
se debe ejecutar nuevamente toda la suite backend y la construcción completa
indicada en [Cambios recientes validados](./15-cambios-recientes-validados.md).

