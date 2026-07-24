**KONTORA POS**

**Documento de Diseño Logico de Base de Datos**

Version preliminar para validacion antes del script SQL

Fecha de elaboracion: 2026-06-25

Este documento describe el modelo logico propuesto para la base de datos de la aplicacion web Kontora POS. No contiene codigo SQL. Su finalidad es servir como soporte de documentacion, revision academica y validacion tecnica antes de construir el modelo fisico en PostgreSQL/Supabase.

El modelo fue definido a partir del analisis de procesos manuales del negocio, las reglas operativas confirmadas y los ajustes realizados antes de pasar a implementacion.


# **1. Alcance del diseño**
El diseno cubre la gestion de usuarios y roles, autenticacion propia con JWT desde backend Java/Spring Boot, apertura y cierre de caja diaria, ventas, pagos, transferencias, promociones, inventario general, stock diario de vasos, gastos, adiciones, pago diario a trabajadores, deposito acumulado, consignaciones bancarias, pagos de servicios, evidencias fotograficas y auditoria.

El sistema esta orientado a un solo local y una sola caja diaria. No se incluye soporte multi-sucursal porque no forma parte del alcance actual.

El backend se alojara en una VM independiente. Supabase se considera como servicio externo para PostgreSQL y almacenamiento de evidencias, no como proveedor principal de autenticacion de usuarios.
# **2. Reglas de negocio consolidadas**
- Solo existe un local y una caja diaria por fecha de operacion.
- La caja debe ser abierta por administrador o gerente antes de iniciar ventas.
- No se pueden registrar ventas si no existe una caja abierta.
- Las ventas solo pueden anularse mientras la caja esta abierta.
- Despues del cierre no se pueden anular ventas.
- Cuando se anula una venta, el vaso asociado se restaura al stock diario.
- Los vasos se descuentan automaticamente por venta.
- Dulces, desechables y bolsas de producto con/sin licor se descuentan por consumo diario manual desde el stock general.
- La promocion 2x aplica a clientes solo martes y miercoles, granizados con licor y vasos del mismo tamano.
- Los trabajadores pueden recibir promocion cualquier dia de la semana.
- Las cantidades impares en promocion cobran pares promocionales y sobrantes a precio normal.
- Las adiciones se registran como un unico total diario editable mientras la caja esta abierta.
- El pago a trabajadores es un total diario obligatorio antes del cierre.
- La base de caja se excluye del efectivo contado y no se suma al deposito.
- La caja puede cerrarse con transferencias pendientes de validar, pero el cierre debe mostrarlas.
- Las transferencias rechazadas se conservan con monto, evidencia y estado consultable.
- Los ajustes manuales de inventario requieren aprobacion del gerente.
- Las fotografias o evidencias se conservan indefinidamente.
# **3. Convenciones de nombres y llaves**
Los nombres de tablas y campos se definen en espanol, sin tildes, espacios ni caracteres especiales. Esta convencion evita errores en migraciones, consultas, ORM y scripts de integracion.

- PK significa llave primaria. Identifica de forma unica cada registro de una tabla.
- FK significa llave foranea. Conecta una tabla con otra y garantiza integridad referencial.
- Los campos marcados como Unico no deben repetirse dentro de la tabla o dentro del alcance indicado.
- Los campos opcionales pueden quedar vacios cuando la regla de negocio lo permite.
- Los estados se documentan como Texto/Enum. En el script SQL se podran implementar como CHECK, tablas catalogo o tipos enumerados segun convenga.

Convencion de PK: cada tabla usa un identificador con el formato id\_nombre\_tabla en singular. Ejemplo: id\_usuario, id\_venta, id\_caja\_diaria.

Convencion de FK: la llave foranea conserva el nombre de la llave primaria referenciada. Ejemplo: ventas.id\_caja\_diaria referencia cajas\_diarias.id\_caja\_diaria.
# **4. Resumen de modulos**

|**Modulo**|**Tablas principales**|**Responsabilidad**|
| :- | :- | :- |
|Seguridad y usuarios|roles, usuarios, credenciales\_usuario, sesiones\_usuario|Gestionar acceso, roles, credenciales, sesiones y trazabilidad de autenticacion.|
|Caja diaria|cajas\_diarias, cierres\_caja|Controlar apertura, cierre y resumen financiero diario.|
|Ventas y pagos|tipos\_granizado, tamanos\_vaso, precios\_granizado, promociones, dias\_promocion, ventas, detalles\_venta, metodos\_pago, pagos\_venta|Registrar ventas, aplicar precios/promociones y soportar pagos efectivo, transferencia e hibridos.|
|Operaciones diarias de caja|adiciones\_diarias, pagos\_trabajadores\_diarios, gastos\_caja|Registrar valores que afectan el cuadre diario.|
|Inventario|categorias\_inventario, unidades\_medida, items\_inventario, existencias\_inventario\_general, existencias\_inventario\_diario, movimientos\_inventario, paquetes\_vasos\_abiertos, consumos\_diarios\_inventario, ajustes\_inventario|Controlar stock general, stock diario de vasos, consumos, perdidas y ajustes.|
|Deposito|movimientos\_deposito, consignaciones\_bancarias, tipos\_servicio, pagos\_servicios|Controlar efectivo acumulado y salidas desde el deposito.|
|Evidencias y auditoria|archivos\_evidencia, configuraciones\_sistema, auditoria\_operaciones|Conservar comprobantes, configuraciones historicas y registro de operaciones sensibles.|

# **5. Diccionario de tablas**
## **5.1. Tabla: roles**
**Proposito:** Catalogo de roles operativos del sistema. Define los niveles de autorizacion basicos: vendedor, administrador y gerente.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_rol|PK|Si|Entero/UUID|Identificador unico del rol. Permite relacionar usuarios con permisos generales.|
|nombre\_rol|Unico|Si|Texto|Nombre del rol: Vendedor, Administrador o Gerente.|
|estado|-|Si|Texto/Enum|Controla si el rol esta activo o inactivo.|

**Relaciones:** Un rol puede estar asignado a muchos usuarios. Relacion: roles 1:N usuarios.

**Importancia:** Centraliza el control de acceso. Evita duplicar permisos directamente en cada usuario.
## **5.2. Tabla: usuarios**
**Proposito:** Registra las personas que operan o administran Kontora POS. El acceso sera por nombre de usuario, no por correo electronico.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_usuario|PK|Si|Entero/UUID|Identificador unico del usuario interno.|
|id\_rol|FK|Si|Entero/UUID|Referencia a roles.id\_rol. Define el nivel de acceso.|
|nombre\_usuario|Unico|Si|Texto|Identificador de login. Puede contener letras o numeros.|
|nombre\_completo|-|Si|Texto|Nombre real del trabajador o administrador.|
|estado|-|Si|Texto/Enum|Activo, inactivo o bloqueado segun la operacion.|
|fecha\_creacion|-|Si|Fecha/hora|Fecha en la que se creo el usuario.|
|fecha\_actualizacion|-|No|Fecha/hora|Ultima actualizacion del registro.|

**Relaciones:** Cada usuario pertenece a un rol. Un usuario puede abrir cajas, registrar ventas, validar transferencias, aprobar ajustes y generar auditoria.

**Importancia:** Es la tabla base para trazabilidad. Casi todas las operaciones sensibles deben registrar el usuario responsable.
## **5.3. Tabla: credenciales\_usuario**
**Proposito:** Guarda la informacion tecnica de autenticacion para login propio en Spring Boot con JWT. La contrasena nunca se almacena en texto plano.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_credencial\_usuario|PK|Si|Entero/UUID|Identificador unico de la credencial.|
|id\_usuario|FK/Unico|Si|Entero/UUID|Referencia a usuarios.id\_usuario. Cada usuario debe tener una credencial activa.|
|contrasena\_hash|-|Si|Texto|Hash seguro de la contrasena, generado con BCrypt o algoritmo equivalente.|
|requiere\_cambio\_contrasena|-|Si|Booleano|Indica si el usuario debe cambiar la contrasena al iniciar sesion.|
|intentos\_fallidos|-|Si|Entero|Cantidad de intentos fallidos para control de bloqueo.|
|fecha\_ultimo\_acceso|-|No|Fecha/hora|Ultimo inicio de sesion exitoso.|
|fecha\_cambio\_contrasena|-|No|Fecha/hora|Fecha del ultimo cambio de contrasena.|
|estado|-|Si|Texto/Enum|Activa, bloqueada o inactiva.|

**Relaciones:** Relacion 1:1 con usuarios. La FK id\_usuario garantiza que la credencial pertenezca a un usuario valido.

**Importancia:** Permite manejar autenticacion propia en una VM independiente de Supabase Auth y evita depender del correo electronico.
## **5.4. Tabla: sesiones\_usuario**
**Proposito:** Registra sesiones activas, cerradas, expiradas o revocadas para complementar JWT y evitar que una sesion siga siendo valida despues del cierre de sesion.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_sesion\_usuario|PK|Si|Entero/UUID|Identificador unico de la sesion.|
|id\_usuario|FK|Si|Entero/UUID|Referencia a usuarios.id\_usuario.|
|token\_identificador|Unico|Si|Texto|Identificador unico del token JWT. No debe guardar el token completo.|
|fecha\_inicio|-|Si|Fecha/hora|Momento de inicio de sesion.|
|fecha\_expiracion|-|Si|Fecha/hora|Fecha de expiracion del token.|
|fecha\_cierre|-|No|Fecha/hora|Momento en que el usuario cerro sesion.|
|estado\_sesion|-|Si|Texto/Enum|Activa, cerrada, expirada o revocada.|
|direccion\_ip|-|No|Texto|IP desde la cual se inicio sesion.|
|user\_agent|-|No|Texto|Informacion del navegador o cliente.|

**Relaciones:** Un usuario puede tener muchas sesiones. Relacion: usuarios 1:N sesiones\_usuario.

**Importancia:** Permite invalidar sesiones desde backend aunque el navegador conserve paginas en cache o intente reutilizar un token.
## **5.5. Tabla: cajas\_diarias**
**Proposito:** Representa la jornada operativa diaria. Antes de vender, un administrador o gerente debe abrir la caja.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_caja\_diaria|PK|Si|Entero/UUID|Identificador unico de la caja diaria.|
|fecha\_operacion|Unico parcial|Si|Fecha|Dia de trabajo. Debe existir una sola caja por fecha.|
|estado\_caja|-|Si|Texto/Enum|Abierta o cerrada.|
|valor\_base|-|Si|Decimal|Base de caja del dia. Se excluye del efectivo contado y del deposito.|
|fecha\_apertura|-|Si|Fecha/hora|Momento de apertura.|
|fecha\_cierre|-|No|Fecha/hora|Momento de cierre.|
|id\_usuario\_apertura|FK|Si|Entero/UUID|Usuario administrador o gerente que abre la caja.|
|id\_usuario\_cierre|FK|No|Entero/UUID|Usuario que cierra la caja.|
|observaciones|-|No|Texto|Notas opcionales de apertura o cierre.|

**Relaciones:** La caja diaria se relaciona con ventas, pagos indirectamente, gastos, adiciones, pago de trabajadores, inventario diario, consumos y cierre de caja.

**Importancia:** Es el eje operativo del sistema. Permite agrupar toda la actividad de un dia y bloquear operaciones despues del cierre.

**Reglas particulares:**

- Solo administrador o gerente puede abrir caja.
- No se pueden registrar ventas sin caja abierta.
- Despues del cierre no se pueden anular ventas.
## **5.6. Tabla: cierres\_caja**
**Proposito:** Guarda el resumen final de la jornada. Consolida ventas, pagos, transferencias, gastos, adiciones, pago de trabajadores y valor a deposito.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_cierre\_caja|PK|Si|Entero/UUID|Identificador unico del cierre.|
|id\_caja\_diaria|FK/Unico|Si|Entero/UUID|Referencia a cajas\_diarias.id\_caja\_diaria. Una caja tiene un cierre.|
|total\_ventas|-|Si|Decimal|Total vendido en la jornada.|
|total\_ventas\_efectivo|-|Si|Decimal|Total recibido por pagos en efectivo.|
|total\_ventas\_transferencia|-|Si|Decimal|Total recibido o registrado por transferencia.|
|total\_transferencias\_pendientes|-|Si|Decimal|Transferencias no validadas al cierre.|
|total\_transferencias\_validadas|-|Si|Decimal|Transferencias validadas al cierre.|
|total\_transferencias\_rechazadas|-|Si|Decimal|Transferencias rechazadas consultables.|
|total\_gastos|-|Si|Decimal|Total de gastos de caja.|
|total\_adiciones|-|Si|Decimal|Total por adiciones diarias.|
|total\_pago\_trabajadores|-|Si|Decimal|Pago total diario a trabajadores.|
|efectivo\_esperado\_sin\_base|-|Si|Decimal|Efectivo esperado sin contar la base.|
|efectivo\_contado\_sin\_base|-|Si|Decimal|Efectivo fisico contado sin incluir la base.|
|diferencia\_caja|-|Si|Decimal|Diferencia entre esperado y contado.|
|valor\_a\_deposito|-|Si|Decimal|Valor que se suma al deposito. Excluye la base.|
|fecha\_cierre|-|Si|Fecha/hora|Fecha y hora del cierre.|
|id\_usuario\_cierre|FK|Si|Entero/UUID|Usuario que realizo el cierre.|
|observaciones|-|No|Texto|Notas opcionales del cierre.|

**Relaciones:** Relacion 1:1 con cajas\_diarias. Puede generar un movimiento de deposito de tipo entrada por cierre.

**Importancia:** Congela el resultado contable de la jornada y sirve como base para el deposito acumulado.
## **5.7. Tabla: tipos\_granizado**
**Proposito:** Catalogo de tipos de granizado vendidos.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_tipo\_granizado|PK|Si|Entero/UUID|Identificador unico del tipo.|
|nombre\_tipo|Unico|Si|Texto|Con licor o Sin licor.|
|estado|-|Si|Texto/Enum|Activo o inactivo.|

**Relaciones:** Un tipo de granizado se relaciona con precios, promociones y detalles de venta.

**Importancia:** Permite diferenciar reglas de precio y promocion sin registrar sabores.
## **5.8. Tabla: tamanos\_vaso**
**Proposito:** Catalogo de tamanos de vaso expresados en onzas.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_tamano\_vaso|PK|Si|Entero/UUID|Identificador unico del tamano.|
|onzas|Unico|Si|Entero|Valores esperados: 8, 12, 16, 20, 24, 32.|
|estado|-|Si|Texto/Enum|Activo o inactivo.|

**Relaciones:** Se relaciona con precios, promociones, detalles de venta e items de inventario tipo vaso.

**Importancia:** Permite que ventas e inventario usen el mismo catalogo de tamanos.
## **5.9. Tabla: precios\_granizado**
**Proposito:** Historial de precios normales de carta por tipo de granizado y tamano.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_precio\_granizado|PK|Si|Entero/UUID|Identificador unico del precio.|
|id\_tipo\_granizado|FK|Si|Entero/UUID|Referencia a tipos\_granizado.id\_tipo\_granizado.|
|id\_tamano\_vaso|FK|Si|Entero/UUID|Referencia a tamanos\_vaso.id\_tamano\_vaso.|
|valor\_precio|-|Si|Decimal|Precio normal aplicable.|
|fecha\_inicio\_vigencia|-|Si|Fecha|Fecha desde la que aplica el precio.|
|fecha\_fin\_vigencia|-|No|Fecha|Fecha final opcional. Puede quedar vacia para precio vigente.|
|estado|-|Si|Texto/Enum|Activo o inactivo.|
|id\_usuario\_creacion|FK|Si|Entero/UUID|Usuario que registro el precio.|

**Relaciones:** Un tipo y un tamano pueden tener muchos precios historicos. Relacion: tipos\_granizado 1:N precios\_granizado; tamanos\_vaso 1:N precios\_granizado.

**Importancia:** Evita que cambios futuros de precio alteren ventas historicas. Cada detalle de venta guarda el precio aplicado como snapshot.
## **5.10. Tabla: promociones**
**Proposito:** Configura promociones por tipo de granizado, tamano y tipo de beneficiario.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_promocion|PK|Si|Entero/UUID|Identificador unico de la promocion.|
|nombre\_promocion|-|Si|Texto|Nombre descriptivo de la promocion.|
|id\_tipo\_granizado|FK|Si|Entero/UUID|Tipo de granizado aplicable. Normalmente Con licor.|
|id\_tamano\_vaso|FK|Si|Entero/UUID|Tamano aplicable.|
|tipo\_beneficiario|-|Si|Texto/Enum|Cliente, Trabajador o Todos.|
|cantidad\_requerida|-|Si|Entero|Cantidad necesaria para aplicar promocion. Para 2x es 2.|
|valor\_promocional|-|Si|Decimal|Valor cobrado por el par o conjunto promocional.|
|fecha\_inicio\_vigencia|-|Si|Fecha|Inicio de vigencia.|
|fecha\_fin\_vigencia|-|No|Fecha|Fin opcional.|
|estado|-|Si|Texto/Enum|Activa o inactiva.|

**Relaciones:** Se relaciona con tipos\_granizado, tamanos\_vaso, dias\_promocion y detalles\_venta.

**Importancia:** Permite aplicar promociones de clientes martes/miercoles y promociones a trabajadores cualquier dia, sin cambiar codigo.
## **5.11. Tabla: dias\_promocion**
**Proposito:** Define los dias de la semana en que aplica una promocion, especialmente para clientes.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_dia\_promocion|PK|Si|Entero/UUID|Identificador unico del dia configurado.|
|id\_promocion|FK|Si|Entero/UUID|Referencia a promociones.id\_promocion.|
|dia\_semana|-|Si|Texto/Enum|Martes, miercoles u otro dia aplicable.|

**Relaciones:** Una promocion puede tener varios dias asociados. Relacion: promociones 1:N dias\_promocion.

**Importancia:** Hace configurable la regla de promocion sin depender de valores fijos en el backend.
## **5.12. Tabla: ventas**
**Proposito:** Cabecera de cada venta registrada durante una caja diaria abierta.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_venta|PK|Si|Entero/UUID|Identificador unico de la venta.|
|id\_caja\_diaria|FK|Si|Entero/UUID|Caja diaria donde se realizo la venta.|
|id\_usuario\_vendedor|FK|Si|Entero/UUID|Usuario que registro la venta.|
|tipo\_comprador|-|Si|Texto/Enum|Cliente o Trabajador.|
|id\_usuario\_comprador|FK|No|Entero/UUID|Usuario comprador si la venta fue a trabajador.|
|numero\_venta|Unico por caja|Si|Texto/Entero|Consecutivo visible o interno de la venta.|
|fecha\_venta|-|Si|Fecha/hora|Momento de registro.|
|estado\_venta|-|Si|Texto/Enum|Registrada o anulada.|
|subtotal\_venta|-|Si|Decimal|Valor antes de descuentos de promocion.|
|descuento\_promocion|-|Si|Decimal|Diferencia generada por promocion.|
|total\_venta|-|Si|Decimal|Valor final a pagar.|
|motivo\_anulacion|-|No|Texto|Motivo si la venta fue anulada.|
|fecha\_anulacion|-|No|Fecha/hora|Fecha de anulacion.|
|id\_usuario\_anulacion|FK|No|Entero/UUID|Usuario que anulo la venta.|

**Relaciones:** Una caja diaria tiene muchas ventas. Una venta tiene uno o varios detalles y uno o varios pagos. Relacion: cajas\_diarias 1:N ventas; ventas 1:N detalles\_venta; ventas 1:N pagos\_venta.

**Importancia:** Conserva la trazabilidad completa de la transaccion y permite anular sin eliminar registros.
## **5.13. Tabla: detalles\_venta**
**Proposito:** Detalle transaccional de lo vendido. Aunque no se entregue recibo fisico, esta tabla es necesaria para auditoria, calculo e inventario.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_detalle\_venta|PK|Si|Entero/UUID|Identificador unico del detalle.|
|id\_venta|FK|Si|Entero/UUID|Venta a la que pertenece.|
|id\_tipo\_granizado|FK|Si|Entero/UUID|Tipo vendido: con licor o sin licor.|
|id\_tamano\_vaso|FK|Si|Entero/UUID|Tamano vendido.|
|cantidad|-|Si|Entero|Cantidad total de vasos vendidos en la linea.|
|precio\_unitario\_normal|-|Si|Decimal|Precio normal aplicado en ese momento.|
|cantidad\_con\_promocion|-|Si|Entero|Cantidad de vasos que entraron en promocion.|
|cantidad\_sin\_promocion|-|Si|Entero|Cantidad cobrada a precio normal.|
|valor\_promocional\_aplicado|-|No|Decimal|Valor promocional aplicado por conjunto.|
|id\_promocion\_aplicada|FK|No|Entero/UUID|Referencia a promociones.id\_promocion si aplica.|
|subtotal\_linea|-|Si|Decimal|Valor antes de promocion.|
|total\_linea|-|Si|Decimal|Valor final de la linea.|

**Relaciones:** Pertenece a una venta y referencia tipo de granizado, tamano y promocion opcional.

**Importancia:** Permite descontar vasos por tamano, reconstruir ventas, aplicar promociones por pares y auditar precios historicos.
## **5.14. Tabla: metodos\_pago**
**Proposito:** Catalogo de metodos reales de pago. El pago hibrido se representa combinando efectivo y transferencia en pagos\_venta.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_metodo\_pago|PK|Si|Entero/UUID|Identificador unico del metodo.|
|nombre\_metodo|Unico|Si|Texto|Efectivo o Transferencia.|
|estado|-|Si|Texto/Enum|Activo o inactivo.|

**Relaciones:** Un metodo de pago puede estar asociado a muchos pagos de venta. Relacion: metodos\_pago 1:N pagos\_venta.

**Importancia:** Evita modelar el pago hibrido como un metodo falso. Una venta hibrida tendra dos pagos: uno en efectivo y otro en transferencia.
## **5.15. Tabla: pagos\_venta**
**Proposito:** Registra los pagos asociados a una venta. Soporta efectivo, transferencia y combinaciones hibridas.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_pago\_venta|PK|Si|Entero/UUID|Identificador unico del pago.|
|id\_venta|FK|Si|Entero/UUID|Venta pagada.|
|id\_metodo\_pago|FK|Si|Entero/UUID|Metodo usado: efectivo o transferencia.|
|valor\_pago|-|Si|Decimal|Valor cubierto por este pago.|
|valor\_recibido\_efectivo|-|No|Decimal|Dinero recibido cuando aplica efectivo.|
|cambio\_entregado|-|No|Decimal|Cambio devuelto al cliente.|
|estado\_validacion|-|No|Texto/Enum|Pendiente, validada o rechazada para transferencias.|
|id\_usuario\_validacion|FK|No|Entero/UUID|Administrador o gerente que valida/rechaza.|
|fecha\_validacion|-|No|Fecha/hora|Momento de validacion.|
|observacion\_validacion|-|No|Texto|Comentario de validacion o rechazo.|
|fecha\_registro|-|Si|Fecha/hora|Fecha de registro del pago.|

**Relaciones:** Una venta puede tener uno o varios pagos. La suma de pagos debe coincidir con total\_venta.

**Importancia:** Permite cierre de caja correcto, trazabilidad de transferencias pendientes y evidencia en pagos por transferencia.
## **5.16. Tabla: adiciones\_diarias**
**Proposito:** Registra el total de adiciones del dia. Es un unico registro editable mientras la caja este abierta.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_adicion\_diaria|PK|Si|Entero/UUID|Identificador unico del registro.|
|id\_caja\_diaria|FK/Unico|Si|Entero/UUID|Caja diaria asociada.|
|cantidad\_adiciones|-|Si|Entero|Cantidad total de adiciones del dia.|
|valor\_unitario|-|Si|Decimal|Valor unitario aplicado.|
|valor\_total|-|Si|Decimal|Cantidad por valor unitario.|
|id\_usuario\_registro|FK|Si|Entero/UUID|Usuario que registro o actualizo.|
|fecha\_registro|-|Si|Fecha/hora|Fecha del registro.|

**Relaciones:** Relacion 1:1 con cajas\_diarias.

**Importancia:** Suma ingresos al cuadre sin forzar que cada adicion este asociada a una venta individual.
## **5.17. Tabla: pagos\_trabajadores\_diarios**
**Proposito:** Registra el pago total diario a trabajadores, tomado fisicamente de la caja.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_pago\_trabajadores\_diario|PK|Si|Entero/UUID|Identificador unico.|
|id\_caja\_diaria|FK/Unico|Si|Entero/UUID|Caja diaria afectada.|
|valor\_total\_pagado|-|Si|Decimal|Valor total pagado a trabajadores.|
|descripcion|-|No|Texto|Nota opcional.|
|id\_usuario\_registro|FK|Si|Entero/UUID|Usuario que registro.|
|fecha\_registro|-|Si|Fecha/hora|Fecha del registro.|
|confirmado\_para\_cierre|-|Si|Booleano|Confirma que el valor fue revisado para cierre.|

**Relaciones:** Relacion 1:1 con cajas\_diarias.

**Importancia:** Es obligatorio antes de cerrar caja porque representa una salida real de efectivo.
## **5.18. Tabla: gastos\_caja**
**Proposito:** Registra gastos operativos tomados del dinero de la caja diaria.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_gasto\_caja|PK|Si|Entero/UUID|Identificador unico del gasto.|
|id\_caja\_diaria|FK|Si|Entero/UUID|Caja diaria afectada.|
|valor\_gasto|-|Si|Decimal|Valor del gasto.|
|descripcion|-|Si|Texto|Detalle de lo comprado o pagado.|
|estado\_gasto|-|Si|Texto/Enum|Registrado, editado o anulado.|
|id\_usuario\_registro|FK|Si|Entero/UUID|Usuario que registro.|
|fecha\_registro|-|Si|Fecha/hora|Fecha del gasto.|
|id\_usuario\_ultima\_edicion|FK|No|Entero/UUID|Usuario que edito.|
|fecha\_ultima\_edicion|-|No|Fecha/hora|Ultima edicion.|
|motivo\_edicion|-|No|Texto|Motivo opcional de edicion.|
|id\_usuario\_anulacion|FK|No|Entero/UUID|Usuario que anulo.|
|fecha\_anulacion|-|No|Fecha/hora|Fecha de anulacion.|
|motivo\_anulacion|-|No|Texto|Motivo opcional de anulacion.|

**Relaciones:** Una caja diaria puede tener muchos gastos. Los archivos de evidencia pueden referenciar el gasto.

**Importancia:** Afecta directamente el efectivo esperado en el cierre, por eso debe ser auditable.
## **5.19. Tabla: categorias\_inventario**
**Proposito:** Catalogo de grupos de inventario.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_categoria\_inventario|PK|Si|Entero/UUID|Identificador unico de categoria.|
|nombre\_categoria|Unico|Si|Texto|Vasos, Dulces, Desechables, Producto con licor, Producto sin licor u otros.|
|estado|-|Si|Texto/Enum|Activa o inactiva.|

**Relaciones:** Una categoria agrupa muchos items de inventario. Relacion: categorias\_inventario 1:N items\_inventario.

**Importancia:** Permite ampliar el catalogo sin modificar la estructura de la base de datos.
## **5.20. Tabla: unidades\_medida**
**Proposito:** Catalogo de unidades utilizadas en inventario.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_unidad\_medida|PK|Si|Entero/UUID|Identificador unico.|
|nombre\_unidad|Unico|Si|Texto|Unidad, bolsa, paquete, rollo, etc.|
|abreviatura|-|Si|Texto|und, bolsa, paq, rollo, etc.|
|estado|-|Si|Texto/Enum|Activa o inactiva.|

**Relaciones:** Una unidad de medida puede ser usada por muchos items.

**Importancia:** Estandariza conteos y evita mezclar unidades no comparables.
## **5.21. Tabla: items\_inventario**
**Proposito:** Catalogo de todos los productos inventariables, vendibles o consumibles.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_item\_inventario|PK|Si|Entero/UUID|Identificador unico del item.|
|id\_categoria\_inventario|FK|Si|Entero/UUID|Categoria del item.|
|id\_unidad\_medida|FK|Si|Entero/UUID|Unidad usada para contar el item.|
|id\_tamano\_vaso|FK|No|Entero/UUID|Solo aplica si el item es un vaso por tamano.|
|nombre\_item|-|Si|Texto|Nombre: Vaso 16 oz, Bolsa de chicles, etc.|
|tipo\_control|-|Si|Texto/Enum|Automatico\_por\_venta o Manual\_por\_consumo.|
|maneja\_paquetes|-|Si|Booleano|Indica si se controla por paquetes.|
|unidades\_por\_paquete|-|No|Entero|Para vasos, normalmente 20.|
|estado|-|Si|Texto/Enum|Activo o inactivo.|
|fecha\_creacion|-|Si|Fecha/hora|Fecha de creacion del item.|

**Relaciones:** Se relaciona con categorias, unidades, tamanos de vaso, existencias, movimientos, paquetes, consumos y ajustes.

**Importancia:** Es la base del inventario. Permite manejar vasos automaticamente y otros productos por consumo manual.
## **5.22. Tabla: existencias\_inventario\_general**
**Proposito:** Saldo actual del stock general para todos los items de inventario.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_existencia\_general|PK|Si|Entero/UUID|Identificador unico de la existencia.|
|id\_item\_inventario|FK/Unico|Si|Entero/UUID|Item asociado.|
|cantidad\_actual|-|Si|Entero|Cantidad disponible en stock general.|
|fecha\_actualizacion|-|Si|Fecha/hora|Ultima actualizacion del saldo.|

**Relaciones:** Relacion 1:1 con items\_inventario.

**Importancia:** Permite consultar rapidamente existencias actuales. Los cambios deben estar respaldados por movimientos\_inventario.
## **5.23. Tabla: existencias\_inventario\_diario**
**Proposito:** Stock operativo diario exclusivo para vasos. Los vasos pasan del stock general al stock diario cuando se abren paquetes.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_existencia\_diaria|PK|Si|Entero/UUID|Identificador unico.|
|id\_caja\_diaria|FK|Si|Entero/UUID|Caja diaria asociada.|
|id\_item\_inventario|FK|Si|Entero/UUID|Item tipo vaso.|
|cantidad\_inicial|-|Si|Entero|Cantidad inicial del dia para ese vaso.|
|cantidad\_ingresada|-|Si|Entero|Cantidad agregada por paquetes abiertos.|
|cantidad\_vendida|-|Si|Entero|Cantidad descontada automaticamente por ventas.|
|cantidad\_perdida|-|Si|Entero|Cantidad perdida o rota.|
|cantidad\_ajustada|-|Si|Entero|Ajustes aprobados.|
|cantidad\_final\_teorica|-|Si|Entero|Resultado esperado por sistema.|
|cantidad\_final\_contada|-|No|Entero|Conteo fisico final.|
|diferencia|-|No|Entero|Diferencia entre teorico y contado.|

**Relaciones:** Una caja diaria tiene muchas existencias diarias, una por cada tamano de vaso usado.

**Importancia:** Permite controlar los vasos por jornada, descontarlos por venta y restaurarlos por anulacion antes del cierre.
## **5.24. Tabla: movimientos\_inventario**
**Proposito:** Libro de movimientos de inventario. Registra cada entrada, salida, venta, anulacion, perdida, ajuste o consumo.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_movimiento\_inventario|PK|Si|Entero/UUID|Identificador unico del movimiento.|
|id\_item\_inventario|FK|Si|Entero/UUID|Item afectado.|
|id\_caja\_diaria|FK|No|Entero/UUID|Caja diaria asociada si aplica.|
|tipo\_stock|-|Si|Texto/Enum|General o Diario.|
|tipo\_movimiento|-|Si|Texto/Enum|Entrada, salida, venta, anulacion\_venta, perdida, ajuste, consumo\_diario.|
|cantidad|-|Si|Entero|Cantidad movida.|
|sentido\_movimiento|-|Si|Texto/Enum|Entrada o salida.|
|referencia\_origen|-|Si|Texto|Origen: venta, paquete, consumo, ajuste, etc.|
|id\_referencia\_origen|-|Si|Texto/UUID|Identificador del registro que origina el movimiento.|
|observacion|-|No|Texto|Nota opcional.|
|id\_usuario\_registro|FK|Si|Entero/UUID|Usuario que genero el movimiento.|
|fecha\_movimiento|-|Si|Fecha/hora|Fecha del movimiento.|

**Relaciones:** Un item tiene muchos movimientos. Una caja diaria puede tener muchos movimientos asociados.

**Importancia:** Es la fuente historica de verdad del inventario. Las existencias son saldos derivados o actualizados a partir de movimientos.
## **5.25. Tabla: paquetes\_vasos\_abiertos**
**Proposito:** Registra la apertura de paquetes de vasos para una caja diaria.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_paquete\_vasos\_abierto|PK|Si|Entero/UUID|Identificador unico.|
|id\_caja\_diaria|FK|Si|Entero/UUID|Caja diaria.|
|id\_item\_inventario|FK|Si|Entero/UUID|Item vaso por tamano.|
|cantidad\_paquetes|-|Si|Entero|Numero de paquetes abiertos.|
|unidades\_por\_paquete|-|Si|Entero|Cantidad por paquete. Normalmente 20.|
|unidades\_generadas|-|Si|Entero|Paquetes por unidades por paquete.|
|unidades\_rotas|-|Si|Entero|Vasos rotos al abrir.|
|unidades\_disponibles|-|Si|Entero|Generadas menos rotas.|
|id\_usuario\_registro|FK|Si|Entero/UUID|Administrador o gerente que registro.|
|fecha\_registro|-|Si|Fecha/hora|Fecha de registro.|

**Relaciones:** Cada registro genera salida del stock general, entrada al stock diario y perdida si hay vasos rotos.

**Importancia:** Modela el proceso fisico de abrir paquetes y evita mezclar tamanos de vaso.
## **5.26. Tabla: consumos\_diarios\_inventario**
**Proposito:** Registra el consumo manual diario de productos que no se descuentan por venta.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_consumo\_diario\_inventario|PK|Si|Entero/UUID|Identificador unico.|
|id\_caja\_diaria|FK|Si|Entero/UUID|Caja diaria asociada.|
|id\_item\_inventario|FK|Si|Entero/UUID|Item consumido.|
|cantidad\_consumida|-|Si|Entero|Cantidad consumida en el dia.|
|id\_usuario\_registro|FK|Si|Entero/UUID|Administrador o gerente que registra.|
|fecha\_registro|-|Si|Fecha/hora|Fecha del registro.|
|observacion|-|No|Texto|Nota opcional.|

**Relaciones:** Una caja diaria puede tener muchos consumos. Cada consumo genera salida del stock general.

**Importancia:** Aplica a dulces, desechables y bolsas de producto con/sin licor.
## **5.27. Tabla: ajustes\_inventario**
**Proposito:** Solicitudes de ajuste manual de inventario que requieren aprobacion del gerente.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_ajuste\_inventario|PK|Si|Entero/UUID|Identificador unico.|
|id\_item\_inventario|FK|Si|Entero/UUID|Item afectado.|
|id\_caja\_diaria|FK|No|Entero/UUID|Caja diaria si afecta stock diario.|
|tipo\_stock|-|Si|Texto/Enum|General o Diario.|
|cantidad\_ajuste|-|Si|Entero|Cantidad a ajustar.|
|sentido\_ajuste|-|Si|Texto/Enum|Entrada o salida.|
|motivo\_ajuste|-|Si|Texto|Justificacion del ajuste.|
|estado\_aprobacion|-|Si|Texto/Enum|Pendiente, aprobado o rechazado.|
|id\_usuario\_solicitante|FK|Si|Entero/UUID|Usuario que solicita.|
|id\_usuario\_aprobador|FK|No|Entero/UUID|Gerente que aprueba o rechaza.|
|fecha\_solicitud|-|Si|Fecha/hora|Fecha de solicitud.|
|fecha\_aprobacion|-|No|Fecha/hora|Fecha de decision.|
|observacion\_aprobacion|-|No|Texto|Nota opcional del gerente.|

**Relaciones:** Un ajuste aprobado genera un movimiento en movimientos\_inventario.

**Importancia:** Controla cambios manuales de inventario con autorizacion y trazabilidad.
## **5.28. Tabla: movimientos\_deposito**
**Proposito:** Libro de movimientos del deposito acumulado. Registra entradas por cierre y salidas por consignaciones o pagos de servicios.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_movimiento\_deposito|PK|Si|Entero/UUID|Identificador unico.|
|tipo\_movimiento\_deposito|-|Si|Texto/Enum|Entrada\_cierre, salida\_consignacion, salida\_pago\_servicio, ajuste.|
|valor\_movimiento|-|Si|Decimal|Valor movido.|
|saldo\_anterior|-|Si|Decimal|Saldo antes del movimiento.|
|saldo\_posterior|-|Si|Decimal|Saldo despues del movimiento.|
|id\_cierre\_caja|FK|No|Entero/UUID|Cierre que origina entrada al deposito.|
|id\_usuario\_registro|FK|Si|Entero/UUID|Usuario que registra.|
|fecha\_movimiento|-|Si|Fecha/hora|Fecha del movimiento.|
|observacion|-|No|Texto|Nota opcional.|

**Relaciones:** Puede relacionarse 1:1 con un cierre de caja, una consignacion bancaria o un pago de servicio.

**Importancia:** Permite reconstruir el saldo del deposito y auditar cada salida de dinero acumulado.
## **5.29. Tabla: consignaciones\_bancarias**
**Proposito:** Registra consignaciones bancarias realizadas desde el deposito.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_consignacion\_bancaria|PK|Si|Entero/UUID|Identificador unico.|
|id\_movimiento\_deposito|FK/Unico|Si|Entero/UUID|Movimiento de deposito asociado.|
|valor\_consignado|-|Si|Decimal|Valor consignado.|
|fecha\_consignacion|-|Si|Fecha/hora|Fecha de consignacion.|
|id\_usuario\_registro|FK|Si|Entero/UUID|Administrador o gerente que registra.|
|observacion|-|No|Texto|Nota opcional.|
|estado|-|Si|Texto/Enum|Registrada o anulada.|

**Relaciones:** Cada consignacion bancaria corresponde a una salida del deposito.

**Importancia:** Separa transferencias recibidas de clientes de consignaciones bancarias realizadas por la empresa.
## **5.30. Tabla: tipos\_servicio**
**Proposito:** Catalogo de servicios que pueden pagarse desde el deposito.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_tipo\_servicio|PK|Si|Entero/UUID|Identificador unico.|
|nombre\_servicio|Unico|Si|Texto|Arriendo, energia, agua, internet u otro.|
|estado|-|Si|Texto/Enum|Activo o inactivo.|

**Relaciones:** Un tipo de servicio puede estar asociado a muchos pagos de servicio.

**Importancia:** Estandariza pagos recurrentes y permite agregar nuevos servicios.
## **5.31. Tabla: pagos\_servicios**
**Proposito:** Registra pagos de servicios hechos con dinero del deposito.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_pago\_servicio|PK|Si|Entero/UUID|Identificador unico.|
|id\_movimiento\_deposito|FK/Unico|Si|Entero/UUID|Movimiento de salida asociado.|
|id\_tipo\_servicio|FK|Si|Entero/UUID|Tipo de servicio pagado.|
|valor\_pagado|-|Si|Decimal|Valor pagado.|
|descripcion|-|No|Texto|Detalle opcional, especialmente si el servicio es Otro.|
|fecha\_pago|-|Si|Fecha/hora|Fecha de pago.|
|id\_usuario\_registro|FK|Si|Entero/UUID|Administrador o gerente que registra.|
|estado|-|Si|Texto/Enum|Registrado o anulado.|

**Relaciones:** Cada pago de servicio genera una salida del deposito.

**Importancia:** Permite controlar gastos no diarios que afectan el efectivo acumulado del negocio.
## **5.32. Tabla: archivos\_evidencia**
**Proposito:** Guarda referencias a fotografias o documentos de soporte. Los archivos se almacenan fuera de la base de datos, por ejemplo en Supabase Storage.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_archivo\_evidencia|PK|Si|Entero/UUID|Identificador unico del archivo.|
|id\_pago\_venta|FK|No|Entero/UUID|Pago por transferencia asociado.|
|id\_gasto\_caja|FK|No|Entero/UUID|Gasto de caja asociado.|
|id\_consignacion\_bancaria|FK|No|Entero/UUID|Consignacion asociada.|
|id\_pago\_servicio|FK|No|Entero/UUID|Pago de servicio asociado.|
|url\_archivo|-|Si|Texto|Ruta o URL del archivo en almacenamiento.|
|nombre\_archivo|-|Si|Texto|Nombre original o generado.|
|tipo\_archivo|-|Si|Texto/Enum|Imagen, PDF u otro.|
|formato\_archivo|-|Si|Texto|JPG, PNG, WEBP, PDF, etc.|
|tamano\_original\_kb|-|No|Entero|Peso original antes de comprimir.|
|tamano\_comprimido\_kb|-|No|Entero|Peso posterior a compresion.|
|fue\_comprimido|-|Si|Booleano|Indica si el backend comprimio el archivo.|
|fecha\_subida|-|Si|Fecha/hora|Fecha de carga.|
|id\_usuario\_subida|FK|Si|Entero/UUID|Usuario que subio la evidencia.|
|estado|-|Si|Texto/Enum|Activo o inactivo.|

**Relaciones:** Puede relacionarse con pagos de venta, gastos, consignaciones o pagos de servicios. Debe pertenecer al menos a un proceso.

**Importancia:** Permite conservar comprobantes indefinidamente sin almacenar binarios pesados dentro de PostgreSQL.
## **5.33. Tabla: configuraciones\_sistema**
**Proposito:** Almacena valores configurables con vigencia historica.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_configuracion\_sistema|PK|Si|Entero/UUID|Identificador unico.|
|nombre\_configuracion|-|Si|Texto|Base de caja, valor adicion, pago trabajadores, etc.|
|valor\_configuracion|-|Si|Texto/Decimal|Valor configurado.|
|tipo\_valor|-|Si|Texto/Enum|Decimal, texto, booleano, entero.|
|fecha\_inicio\_vigencia|-|Si|Fecha|Inicio de vigencia.|
|fecha\_fin\_vigencia|-|No|Fecha|Fin opcional de vigencia.|
|estado|-|Si|Texto/Enum|Activa o inactiva.|
|id\_usuario\_registro|FK|Si|Entero/UUID|Usuario que registro el cambio.|
|fecha\_registro|-|Si|Fecha/hora|Fecha de registro.|

**Relaciones:** Un usuario puede registrar muchas configuraciones. Relacion: usuarios 1:N configuraciones\_sistema.

**Importancia:** Evita sobrescribir valores historicos y protege cierres/ventas pasadas ante cambios de configuracion.
## **5.34. Tabla: auditoria\_operaciones**
**Proposito:** Registra operaciones sensibles para trazabilidad.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_auditoria\_operacion|PK|Si|Entero/UUID|Identificador unico.|
|id\_usuario|FK|Si|Entero/UUID|Usuario que realizo la accion.|
|tabla\_afectada|-|Si|Texto|Tabla modificada.|
|id\_registro\_afectado|-|Si|Texto/UUID|Registro afectado.|
|accion|-|Si|Texto/Enum|Crear, editar, anular, aprobar, rechazar, cerrar, validar, etc.|
|valor\_anterior|-|No|Texto/JSON|Estado anterior del registro.|
|valor\_nuevo|-|No|Texto/JSON|Estado nuevo del registro.|
|fecha\_accion|-|Si|Fecha/hora|Fecha de la accion.|
|direccion\_ip|-|No|Texto|IP desde la que se realizo.|
|descripcion|-|No|Texto|Explicacion opcional.|

**Relaciones:** Un usuario puede generar muchas auditorias. Relacion: usuarios 1:N auditoria\_operaciones.

**Importancia:** Sostiene el control interno del sistema: anulaciones, ajustes, cierres, validaciones, cambios de precios y configuraciones.
# **6. Relaciones principales del modelo**

|**Tabla origen**|**Tabla destino**|**Cardinalidad**|**Explicacion**|
| :- | :- | :- | :- |
|roles|usuarios|1:N|Un rol puede pertenecer a muchos usuarios.|
|usuarios|credenciales\_usuario|1:1|Cada usuario tiene una credencial activa para login propio.|
|usuarios|sesiones\_usuario|1:N|Un usuario puede iniciar varias sesiones a lo largo del tiempo.|
|usuarios|cajas\_diarias|1:N|Un usuario administrador/gerente puede abrir o cerrar muchas cajas.|
|cajas\_diarias|ventas|1:N|Una caja diaria contiene todas las ventas del dia.|
|ventas|detalles\_venta|1:N|Una venta puede contener uno o mas detalles.|
|ventas|pagos\_venta|1:N|Una venta puede pagarse con uno o varios registros de pago.|
|metodos\_pago|pagos\_venta|1:N|Cada pago usa un metodo real: efectivo o transferencia.|
|cajas\_diarias|adiciones\_diarias|1:1|Una caja tiene un unico total de adiciones.|
|cajas\_diarias|pagos\_trabajadores\_diarios|1:1|Una caja tiene un pago total diario de trabajadores.|
|cajas\_diarias|gastos\_caja|1:N|Una caja puede tener varios gastos.|
|cajas\_diarias|cierres\_caja|1:1|Cada caja cerrada tiene un cierre.|
|cierres\_caja|movimientos\_deposito|1:1 opcional|El cierre genera una entrada al deposito cuando hay valor a depositar.|
|categorias\_inventario|items\_inventario|1:N|Una categoria agrupa muchos items.|
|unidades\_medida|items\_inventario|1:N|Una unidad se usa en muchos items.|
|items\_inventario|existencias\_inventario\_general|1:1|Cada item tiene una existencia general.|
|items\_inventario|existencias\_inventario\_diario|1:N|Un item tipo vaso puede tener existencias por caja diaria.|
|items\_inventario|movimientos\_inventario|1:N|Cada item puede tener muchos movimientos historicos.|
|cajas\_diarias|paquetes\_vasos\_abiertos|1:N|Una caja puede abrir varios paquetes de vasos.|
|cajas\_diarias|consumos\_diarios\_inventario|1:N|Una caja puede registrar muchos consumos manuales.|
|items\_inventario|ajustes\_inventario|1:N|Un item puede tener muchos ajustes solicitados.|
|movimientos\_deposito|consignaciones\_bancarias|1:1|Una consignacion es una salida del deposito.|
|movimientos\_deposito|pagos\_servicios|1:1|Un pago de servicio es una salida del deposito.|
|pagos\_venta/gastos/consignaciones/pagos\_servicios|archivos\_evidencia|1:N|Cada proceso puede tener una o mas evidencias.|
|usuarios|auditoria\_operaciones|1:N|Cada operacion sensible queda asociada al usuario responsable.|

# **7. Flujos operativos que soporta el modelo**

|**Flujo**|**Comportamiento soportado por la base de datos**|
| :- | :- |
|Apertura de caja|Administrador o gerente crea un registro en cajas\_diarias con estado abierta. Desde ese momento se habilitan ventas y registros operativos.|
|Venta normal|Se crea ventas, detalles\_venta y uno o varios pagos\_venta. Si el detalle involucra vasos, se genera movimiento de inventario de salida en stock diario.|
|Pago hibrido|No se registra como metodo. Se crean dos pagos\_venta: efectivo y transferencia. La suma debe coincidir con total\_venta.|
|Anulacion de venta|Solo si la caja esta abierta. La venta cambia a anulada, se registra motivo y se genera movimiento de entrada al stock diario para restaurar vasos.|
|Promocion 2x|El sistema consulta promociones, dias\_promocion, tipo\_comprador y tamano. Aplica pares promocionales y sobrantes a precio normal.|
|Apertura de paquetes de vasos|Se registra paquetes\_vasos\_abiertos. Se descuenta stock general, se suma stock diario y se registran perdidas si hay vasos rotos.|
|Consumo diario manual|Para dulces, desechables y bolsas de producto se registra consumos\_diarios\_inventario y se descuenta stock general.|
|Cierre de caja|Se consolida informacion en cierres\_caja. No se incluye la base en efectivo contado. Se permite cerrar con transferencias pendientes visibles.|
|Movimiento al deposito|El cierre genera movimiento de deposito por valor\_a\_deposito, excluyendo la base.|
|Consignaciones y pagos de servicios|Administrador o gerente registra la salida desde movimientos\_deposito y la evidencia correspondiente.|
|Auditoria|Cambios sensibles generan registros en auditoria\_operaciones para trazabilidad.|

# **8. Reglas de integridad recomendadas para el futuro SQL**
- No permitir mas de una caja abierta para la misma fecha de operacion.
- No permitir ventas si cajas\_diarias.estado\_caja no es Abierta.
- No permitir anulacion de ventas cuando la caja ya esta Cerrada.
- Garantizar que la suma de pagos\_venta.valor\_pago sea igual a ventas.total\_venta.
- No permitir cantidades negativas en ventas, consumos, paquetes, movimientos ni existencias.
- Garantizar que archivos\_evidencia pertenezca al menos a un proceso: pago, gasto, consignacion o pago de servicio.
- No permitir cerrar caja sin adiciones\_diarias y pagos\_trabajadores\_diarios confirmados.
- No permitir ajustes de inventario sin aprobacion cuando afecten existencias.
- Guardar historico de precios, promociones y configuraciones sin sobrescribir vigencias anteriores.
- Registrar auditoria para anulaciones, ediciones de gastos, validaciones de transferencias, cierres, ajustes y cambios de configuracion.
# **9. Consideraciones tecnicas para implementacion**
Autenticacion: se recomienda implementar login propio en Spring Boot con Spring Security, contrasena hasheada y JWT. La tabla sesiones\_usuario permite revocar sesiones y evitar que el usuario reutilice un token despues de cerrar sesion.

Evidencias: la base de datos solo debe guardar rutas y metadatos de archivos. El backend Java debe comprimir imagenes antes de subirlas al almacenamiento para reducir consumo de espacio. Es preferible convertir PNG pesados a JPG o WEBP cuando no se requiera transparencia.

Inventario: movimientos\_inventario debe ser la fuente historica de cambios. Las tablas de existencias funcionan como saldos de consulta rapida, pero todo cambio debe quedar respaldado por un movimiento.

Deposito: movimientos\_deposito funciona como libro mayor. El saldo actual debe poder reconstruirse desde los movimientos.

Auditoria: la auditoria puede implementarse desde el backend, triggers de base de datos o una combinacion de ambos. Para un proyecto academico y controlado, iniciar desde backend es suficiente, siempre que se aplique de forma consistente.
# **10. Conclusion**
El modelo propuesto coincide con el analisis funcional consolidado de Kontora POS y queda listo para revision antes de la generacion del script SQL. La estructura protege la trazabilidad de ventas, caja, inventario, deposito y usuarios, manteniendo flexibilidad para futuros reportes y crecimiento funcional.
Kontora POS - Diseno logico de base de datos
