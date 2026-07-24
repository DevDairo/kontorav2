
\
**KONTORA POS\
Documento reconstruido de requisitos funcionales, no funcionales y casos de uso**\
\
Aplicacion web para ventas, caja, inventario y deposito

Version: 2.0\
Fecha: 25/06/2026\
Estado: Documento reconstruido para validacion


# **1. Objetivo del documento**
El presente documento reconstruye y ordena los requisitos funcionales, requisitos no funcionales y casos de uso del sistema Kontora POS. La reconstruccion se realiza con base en el analisis final de los procesos operativos del negocio: ventas en mostrador, pagos, caja diaria, inventario general, stock diario operativo de vasos, deposito, evidencias fotograficas, roles, seguridad y auditoria.

El objetivo es contar con una especificacion coherente antes de continuar con el diseno tecnico, la implementacion del backend y la integracion con la base de datos.
# **2. Alcance del sistema**
- El sistema funcionara para un unico local o punto fisico de venta.
- El sistema manejara una sola caja diaria por jornada.
- La aplicacion sera 100% web y se accedera mediante navegador desde computadores o dispositivos moviles.
- El frontend debera comunicarse con el backend; el frontend no debera operar directamente sobre la base de datos para procesos criticos.
- La base de datos estara alojada en Supabase/PostgreSQL y el backend en una VM independiente.
- El sistema administrara ventas, pagos, comprobantes, inventario, caja, deposito, gastos, pagos de servicios, roles y auditoria.
- La exportacion formal a Excel/PDF y reportes avanzados queda prevista como fase posterior, salvo consultas internas en pantalla.
# **3. Actores del sistema**

|**Actor**|**Descripcion general**|**Nivel de acceso**|
| :- | :- | :- |
|Vendedor / Trabajador|Usuario operativo que atiende ventas, registra pagos, adjunta comprobantes y registra gastos diarios.|Operativo|
|Administrador|Usuario encargado de supervisar la operacion diaria, caja, inventario, productos, precios, promociones, gastos, consignaciones y pagos de servicios.|Administrativo|
|Gerente|Usuario con nivel superior. Tiene visibilidad total, gestiona usuarios, valida operaciones criticas y aprueba ajustes de inventario.|Directivo / maximo|

# **4. Requisitos funcionales**
Los requisitos funcionales describen las capacidades que el sistema debe ofrecer para automatizar los procesos del negocio.
## **4.1. Seguridad, usuarios y sesiones**

|**Codigo**|**Requisito funcional**|
| :- | :- |
|RF-01|El sistema debe permitir el inicio de sesion mediante un nombre de usuario alfanumerico y contrasena.|
|RF-02|El sistema debe permitir cerrar sesion de forma efectiva, invalidando la sesion activa para impedir el uso posterior mediante retroceso del navegador.|
|RF-03|El sistema debe permitir gestionar usuarios con los roles vendedor, administrador y gerente.|
|RF-04|El sistema debe permitir activar, inactivar o bloquear usuarios sin eliminar su historial operativo.|
|RF-05|El sistema debe registrar la trazabilidad de accesos, cierres de sesion y acciones sensibles realizadas por los usuarios.|

## **4.2. Caja diaria**

|**Codigo**|**Requisito funcional**|
| :- | :- |
|RF-06|El sistema debe permitir abrir una caja diaria antes de iniciar operaciones, registrando fecha, hora, usuario responsable y valor de base.|
|RF-07|El sistema debe impedir registrar ventas, gastos, paquetes abiertos o consumos diarios si no existe una caja diaria abierta.|
|RF-08|El sistema debe permitir cerrar la caja diaria, calculando ventas, efectivo, transferencias, gastos, adiciones, pago de trabajadores, diferencia de caja y valor a deposito.|
|RF-09|El sistema debe bloquear nuevas ventas y anulaciones una vez la caja diaria haya sido cerrada.|
|RF-10|El sistema debe permitir consultar el historial de aperturas, cierres, arqueos y diferencias de caja.|
|RF-11|El sistema debe mostrar en el cierre las transferencias pendientes, validadas y rechazadas asociadas a la jornada.|
|RF-12|El sistema debe excluir la base de caja del dinero contado y del valor enviado al deposito.|

## **4.3. Ventas y promociones**

|**Codigo**|**Requisito funcional**|
| :- | :- |
|RF-13|El sistema debe permitir registrar ventas realizadas en el mostrador del negocio.|
|RF-14|Al registrar una venta, el sistema debe permitir seleccionar tipo de granizado, tamano de vaso y cantidad.|
|RF-15|El sistema debe permitir vender granizados con licor y granizados sin licor, sin exigir seleccion de sabor en el flujo de venta.|
|RF-16|El sistema debe manejar los tamanos de vaso 8 oz, 12 oz, 16 oz, 20 oz, 24 oz y 32 oz.|
|RF-17|El sistema debe aplicar precios normales segun tipo de granizado y tamano de vaso, conservando el precio historico aplicado en cada venta.|
|RF-18|El sistema debe gestionar promociones configurables para granizados con licor, conservando vigencias historicas.|
|RF-19|El sistema debe aplicar la promocion 2x solo cuando existan pares de vasos del mismo tamano; las unidades sobrantes se cobraran a precio normal.|
|RF-20|El sistema debe aplicar promociones a clientes solo martes y miercoles, y permitir promociones a trabajadores cualquier dia de la semana.|
|RF-21|El sistema debe permitir anular ventas solo mientras la caja diaria este abierta, registrando usuario, fecha y motivo de anulacion.|
|RF-22|Al anular una venta, el sistema debe restaurar al stock diario operativo la cantidad de vasos asociada a la venta anulada.|

## **4.4. Pagos y comprobantes**

|**Codigo**|**Requisito funcional**|
| :- | :- |
|RF-23|El sistema debe permitir registrar pagos en efectivo.|
|RF-24|El sistema debe permitir registrar pagos por transferencia, cargando evidencia fotografica y valor transferido.|
|RF-25|El sistema debe permitir pagos hibridos mediante la combinacion de un pago en efectivo y un pago por transferencia asociados a una misma venta.|
|RF-26|El sistema debe discriminar los valores recibidos en efectivo y por transferencia para el cierre de caja.|
|RF-27|El sistema debe permitir validar o rechazar transferencias posteriormente, conservando monto, evidencia, estado, usuario validador y observacion cuando aplique.|
|RF-28|El sistema debe permitir consultar evidencias de transferencias asociadas a ventas y pagos hibridos.|

## **4.5. Adiciones, gastos y pagos diarios**

|**Codigo**|**Requisito funcional**|
| :- | :- |
|RF-29|El sistema debe permitir registrar un unico total diario de adiciones mientras la caja este abierta.|
|RF-30|El sistema debe permitir editar el total diario de adiciones mientras la caja este abierta.|
|RF-31|El sistema debe permitir registrar gastos diarios tomados de la caja, con valor, descripcion y evidencia opcional.|
|RF-32|El sistema debe permitir que vendedores registren gastos, pero solo administrador o gerente puedan editarlos o anularlos.|
|RF-33|El sistema debe registrar un pago total diario a trabajadores y exigir su confirmacion antes del cierre de caja.|
|RF-34|Si el pago diario a trabajadores es cero, el sistema debe exigir confirmacion explicita antes del cierre.|

## **4.6. Inventario general y stock diario de vasos**

|**Codigo**|**Requisito funcional**|
| :- | :- |
|RF-35|El sistema debe administrar categorias de inventario como vasos, dulces, desechables, producto con licor y producto sin licor.|
|RF-36|El sistema debe permitir crear, editar, consultar e inhabilitar items de inventario dentro de cada categoria.|
|RF-37|El sistema debe mantener stock general para todos los items inventariables.|
|RF-38|El sistema debe manejar stock diario operativo solo para vasos, debido a que estos se descuentan automaticamente por venta.|
|RF-39|El sistema debe permitir que el administrador o gerente registre, mediante una interfaz, los paquetes de vasos abiertos durante la jornada, indicando el tamaño del vaso, la cantidad de paquetes abiertos y la cantidad de vasos dañados si existen. Cada paquete registrado debe generar 20 unidades en el stock diario y descontarse del stock general.|
|RF-40|Si al abrir un paquete de vasos se encuentran vasos rotos, el sistema debe registrar la entrada de 20 unidades, la perdida y el disponible real.|
|RF-41|El sistema debe descontar automaticamente un vaso del stock diario operativo por cada granizado vendido, segun el tamano de vaso.|
|RF-42|El sistema debe permitir registrar el stock final contado de vasos y calcular diferencias frente al stock teorico.|
|RF-43|El sistema debe permitir registrar consumo diario manual de dulces, desechables y bolsas de producto con licor o sin licor, descontandolos del stock general.|
|RF-44|El sistema debe permitir consultar inventario actual e historial de movimientos por item.|
|RF-45|El sistema debe permitir configurar cantidades minimas para generar alertas de bajo inventario.|
|RF-46|El sistema debe permitir registrar perdidas o mermas, inicialmente enfocadas en vasos, conservando trazabilidad del usuario y fecha.|
|RF-47|El sistema debe permitir solicitar ajustes manuales de inventario y exigir aprobacion del gerente antes de aplicarlos.|
##
## **4.7. Deposito, consignaciones y servicios**

|**Codigo**|**Requisito funcional**|
| :- | :- |
|RF-48|El sistema debe acumular en el depósito diario el dinero efectivo disponible al finalizar cada cierre de caja, descontando siempre el valor de la base. La base de caja no debe sumarse al depósito, ya que corresponde a un monto fijo reservado para iniciar la operación del siguiente día.|
|RF-49|El sistema debe permitir consultar el saldo actual y el historial completo de movimientos del deposito.|
|RF-50|El sistema debe permitir registrar consignaciones bancarias realizadas desde el deposito, adjuntando evidencia fotografica.|
|RF-51|El sistema debe permitir registrar pagos de servicios realizados desde el deposito, como arriendo, energia, agua, internet u otros.|
|RF-52|El sistema debe descontar automaticamente del deposito los valores de consignaciones bancarias y pagos de servicios.|
|RF-53|El sistema debe conservar evidencias fotograficas de consignaciones bancarias y pagos de servicios para consulta posterior.|

## **4.8. Configuracion, auditoria y consultas**

|**Codigo**|**Requisito funcional**|
| :- | :- |
|RF-54|El sistema debe permitir configurar valores como base de caja, valor de adiciones, precios, promociones y pago diario a trabajadores.|
|RF-55|El sistema debe conservar historial de cambios de precios, promociones y configuraciones para evitar inconsistencias con operaciones historicas.|
|RF-56|El sistema debe registrar auditoria de operaciones sensibles como anulaciones, validaciones, rechazos, ajustes, cambios de precio, cambios de configuracion y cierres.|
|RF-57|El sistema debe permitir consultar ventas, gastos, inventario, cierres, comprobantes, deposito y movimientos por dia.|
|RF-58|El sistema debe permitir consultar diariamente la información operativa y financiera de la jornada, incluyendo el dinero ingresado, los métodos de pago utilizados, las ventas clasificadas por tamaño y tipo de granizado, el estado de la caja, el estado del inventario y las diferencias identificadas durante el cierre.|
|RF-59|El sistema debe permitir conservar y consultar archivos de evidencia asociados a transferencias, gastos, consignaciones y pagos de servicios.|
|RF-60|El sistema debe permitir comprimir imagenes desde el backend antes de almacenarlas, conservando informacion del archivo original y comprimido.|

# **5. Requisitos no funcionales**
Los requisitos no funcionales describen atributos de calidad, restricciones tecnicas y condiciones operativas del sistema.

|**Codigo**|**Requisito no funcional**|
| :- | :- |
|RNF-01|El sistema debe operar como aplicacion 100% web alojada en servidor externo, accesible por internet para usuarios autorizados.|
|RNF-02|El sistema debe funcionar correctamente en navegadores modernos desde computadores de escritorio, portatiles, tablets y dispositivos moviles.|
|RNF-03|La interfaz debe ser clara, rapida y sencilla para registrar ventas, pagos y operaciones de caja durante la atencion al cliente.|
|RNF-04|El sistema debe restringir el acceso a funcionalidades segun el rol del usuario.|
|RNF-05|Las contrasenas deben almacenarse mediante hash seguro; nunca deben almacenarse en texto plano.|
|RNF-06|El sistema debe implementar autenticacion segura con JWT y control de sesiones activas, cerradas, expiradas o revocadas.|
|RNF-07|El cierre de sesion debe impedir el uso posterior de operaciones protegidas aunque el usuario retroceda en el navegador.|
|RNF-08|El sistema debe proteger la comunicacion entre cliente, backend y base de datos mediante mecanismos seguros acordes al despliegue.|
|RNF-09|El sistema debe garantizar integridad transaccional en ventas, pagos, inventario, caja diaria y deposito.|
|RNF-10|El sistema debe mantener trazabilidad de las operaciones realizadas por los usuarios para efectos de auditoria.|
|RNF-11|Las evidencias fotograficas deben conservarse indefinidamente y permanecer asociadas al registro que soportan.|
|RNF-12|El acceso a evidencias fotograficas debe limitarse a usuarios autorizados.|
|RNF-13|El backend debe comprimir imagenes antes de almacenarlas para reducir consumo de almacenamiento y ancho de banda.|
|RNF-14|La base de datos debe permitir agregar nuevas categorias e items de inventario sin modificar la estructura principal del sistema.|
|RNF-15|El sistema debe evitar eliminaciones fisicas de registros con historial operativo, priorizando inhabilitacion o anulacion auditada.|
|RNF-16|El sistema debe permitir restaurar informacion desde copias de seguridad en caso de fallos, perdida de datos o dano del sistema.|
|RNF-17|Las copias de seguridad deben generarse de forma periodica segun la estrategia definida para Supabase/PostgreSQL y el servidor de backend.|
|RNF-18|El sistema debe reducir errores de registro, descuadres de caja e inconsistencias de inventario mediante validaciones y calculos automaticos.|
|RNF-19|El sistema debe conservar consistencia entre ventas, pagos, stock diario de vasos, caja diaria y deposito.|
|RNF-20|El diseno debe ser mantenible, separando responsabilidades entre frontend, backend, base de datos, almacenamiento y autenticacion.|
|RNF-21|El sistema debe responder de forma adecuada durante la operacion diaria del punto de venta, priorizando rapidez en ventas y pagos.|
|RNF-22|Los mensajes de error y validacion deben ser comprensibles para usuarios operativos y administrativos.|
|RNF-23|La arquitectura debe permitir despliegue en una VM independiente para el backend y uso de Supabase como servicio de base de datos y almacenamiento.|
|RNF-24|El sistema debe evitar acceso directo del frontend a operaciones criticas de base de datos; dichas operaciones deben pasar por el backend.|

# **6. Casos de uso**
Los casos de uso describen las interacciones principales entre los actores y el sistema. Se presentan de forma agrupada por actor, considerando la jerarquia de permisos definida para Kontora POS.
## **6.1. Casos de uso del vendedor / trabajador**

|**Codigo**|**Caso de uso**|**Descripcion**|
| :- | :- | :- |
|CU-VEN-01|Iniciar sesion|Acceder al sistema mediante nombre de usuario y contrasena.|
|CU-VEN-02|Cerrar sesion|Finalizar la sesion activa e impedir operaciones posteriores con el token anterior.|
|CU-VEN-03|Registrar venta|Registrar una venta seleccionando tipo de granizado, tamano y cantidad.|
|CU-VEN-04|Registrar pago en efectivo|Registrar el valor pagado en efectivo y el cambio entregado cuando aplique.|
|CU-VEN-05|Registrar pago por transferencia|Registrar valor transferido y cargar evidencia fotografica.|
|CU-VEN-06|Registrar pago hibrido|Registrar una parte en efectivo y otra por transferencia dentro de la misma venta.|
|CU-VEN-07|Registrar adiciones del dia|Registrar o actualizar el total diario de adiciones mientras la caja este abierta, segun permisos definidos.|
|CU-VEN-08|Registrar gasto diario|Registrar gastos tomados de la caja, con descripcion y evidencia opcional.|
|CU-VEN-09|Consultar estado basico de caja abierta|Ver informacion operativa necesaria para registrar ventas y pagos del dia.|
|CU-VEN-10|Consultar ventas registradas propias|Consultar ventas registradas durante la jornada, si el rol lo permite.|

## **6.2. Casos de uso del administrador**
El administrador puede ejecutar los casos de uso del vendedor y, adicionalmente, los siguientes:

|**Codigo**|**Caso de uso**|**Descripcion**|
| :- | :- | :- |
|CU-ADM-01|Abrir caja diaria|Crear la caja de la jornada antes de iniciar ventas.|
|CU-ADM-02|Cerrar caja diaria|Realizar arqueo, calcular diferencias y cerrar la caja del dia.|
|CU-ADM-03|Registrar pago diario a trabajadores|Registrar y confirmar el valor total pagado a trabajadores antes del cierre.|
|CU-ADM-04|Anular venta|Anular una venta mientras la caja este abierta y restaurar vasos al stock diario.|
|CU-ADM-05|Editar o anular gasto|Corregir o anular gastos registrados, dejando trazabilidad.|
|CU-ADM-06|Gestionar categorias de inventario|Crear, consultar, editar e inhabilitar categorias.|
|CU-ADM-07|Gestionar items de inventario|Crear, consultar, editar e inhabilitar items inventariables.|
|CU-ADM-08|Gestionar tamanos de vaso|Consultar y administrar presentaciones de vasos utilizadas por el sistema.|
|CU-ADM-09|Registrar paquetes de vasos abiertos|Registrar paquetes por tamano y perdidas por vasos rotos al abrirlos.|
|CU-ADM-10|Registrar stock final de vasos|Registrar conteo fisico final y consultar diferencias.|
|CU-ADM-11|Registrar consumo diario de inventario|Descontar del stock general dulces, desechables y bolsas de producto consumidas durante el dia.|
|CU-ADM-12|Registrar perdidas de vasos|Registrar mermas o danos de vasos, afectando el stock diario.|
|CU-ADM-13|Solicitar ajuste de inventario|Solicitar un ajuste manual que debera ser aprobado por el gerente.|
|CU-ADM-14|Gestionar precios|Crear nuevos precios con vigencia, sin alterar ventas historicas.|
|CU-ADM-15|Gestionar promociones|Crear y modificar promociones con condiciones de aplicacion y vigencia.|
|CU-ADM-16|Consultar transferencias y comprobantes|Revisar pagos por transferencia, comprobantes y estados.|
|CU-ADM-17|Registrar consignacion bancaria|Registrar salida de dinero del deposito hacia consignacion bancaria.|
|CU-ADM-18|Registrar pago de servicio|Registrar pagos de servicios realizados desde el deposito.|
|CU-ADM-19|Consultar deposito|Consultar saldo actual e historial de movimientos del deposito.|
|CU-ADM-20|Consultar reportes internos|Consultar ventas, inventario, cierres, gastos y caja por dia o periodo.|

## **6.3. Casos de uso del gerente**
El gerente es el nivel superior del sistema. Puede ejecutar los casos de uso del vendedor y del administrador, y adicionalmente:

|**Codigo**|**Caso de uso**|**Descripcion**|
| :- | :- | :- |
|CU-GER-01|Gestionar usuarios|Crear, editar, inactivar, bloquear usuarios y asignar roles.|
|CU-GER-02|Aprobar ajuste de inventario|Aprobar o rechazar ajustes manuales de inventario solicitados.|
|CU-GER-03|Validar transferencia|Marcar transferencias como validadas o rechazadas despues de verificar el ingreso en la cuenta.|
|CU-GER-04|Consultar toda la informacion del negocio|Acceder a ventas, cierres, inventario, deposito, gastos, comprobantes y auditoria.|
|CU-GER-05|Consultar auditoria|Revisar acciones sensibles realizadas por los usuarios.|
|CU-GER-06|Configurar valores generales|Modificar base de caja, valor de adiciones, pago diario de trabajadores, precios y promociones.|
|CU-GER-07|Supervisar deposito|Controlar entradas por cierre, salidas por consignaciones y pagos de servicios.|
|CU-GER-08|Consultar transferencias pendientes o rechazadas|Revisar montos, evidencias, usuarios involucrados y estados de validacion.|
|CU-GER-09|Supervisar cierres historicos|Consultar cierres diarios, diferencias, efectivo esperado, efectivo contado y valor a deposito.|
|CU-GER-10|Supervisar seguridad de usuarios|Consultar usuarios activos, bloqueados, inactivos y sesiones segun alcance implementado.|

## **6.4. Relaciones generales entre actores y casos de uso**

|**Actor**|**Casos de uso principales**|**Restricciones relevantes**|
| :- | :- | :- |
|Vendedor|Ventas, pagos, comprobantes, gastos y cierre de sesion.|No puede cerrar caja, aprobar ajustes, gestionar usuarios ni modificar configuraciones criticas.|
|Administrador|Caja diaria, inventario, productos, precios, promociones, gastos, deposito y consultas operativas.|No debe aprobar ajustes que requieran nivel gerencial si la regla exige aprobacion del gerente.|
|Gerente|Todos los casos de uso, gestion de usuarios, aprobaciones, auditoria y control total del negocio.|Debe conservar trazabilidad en operaciones criticas.|

# **7. Requisitos diferidos o fase posterior**
Los siguientes elementos quedan documentados como alcance futuro o fase posterior para evitar que se mezclen con el alcance funcional inicial:

- Exportacion formal a Excel y PDF de reportes administrativos y contables.
- Reportes avanzados semanales, mensuales y anuales con filtros especializados.
- Administracion grafica de copias de seguridad desde la aplicacion, si se decide no gestionarlas desde Supabase o infraestructura.
- Registro detallado de perdidas para todos los productos diferentes a vasos, si el negocio decide activar esa funcion visible para usuarios administrativos.
- Modulos multi-sucursal o multiples cajas por turno; actualmente estan fuera de alcance.

Kontora POS - Documento de requisitos reconstruido
