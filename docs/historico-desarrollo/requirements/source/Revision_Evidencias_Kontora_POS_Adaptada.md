**KONTORA POS**

**Documento de revisión de evidencias adaptado y consolidado**

Sistema POS web para granizados


|**Elemento**|**Descripción**|
| :- | :- |
|Proyecto|Kontora POS|
|Tipo de documento|Documento técnico en formato Word|
|Versión|2\.0 - Adaptada con requisitos, arquitectura, tecnologías, modelo relacional y diccionario de datos|
|Fecha|25/06/2026|
|Estado|Documento consolidado para validación académica y técnica|


# **Contenido del documento**

1. Objetivo, alcance y actores del sistema
1. Revisión de evidencias actualizada
1. Requisitos funcionales y no funcionales
1. Arquitectura del sistema
1. Tecnologías seleccionadas y justificación
1. Decisiones técnicas principales
1. Modelo relacional consolidado
1. Diccionario de datos
1. Riesgo técnico del proyecto y controles
1. Conclusiones
# **1. Objetivo, alcance y actores del sistema**
## **1.1 Objetivo del documento**
Establecer la documentación técnica general del proyecto Kontora POS, definiendo de manera organizada el alcance funcional, los requisitos funcionales y no funcionales, la arquitectura de software, las tecnologías seleccionadas, el modelo relacional, el diccionario de datos y los riesgos técnicos asociados a la solución. Este documento sirve como base para orientar el análisis, diseño, desarrollo, pruebas y despliegue de la aplicación web que se ejecutará para apoyar la gestión de ventas, caja, inventario, depósito, evidencias, usuarios y auditoría del negocio.
## **1.2 Alcance del sistema**
- El sistema funcionará para un único local o punto físico de venta.
- El sistema manejará una sola caja diaria por jornada.
- La aplicación será 100% web y se accederá mediante navegador desde computadores o dispositivos móviles.
- El frontend se comunicará con el backend mediante API REST y no ejecutará operaciones críticas directamente sobre la base de datos.
- La base de datos estará alojada en Supabase/PostgreSQL y el backend en una VM independiente.
- El sistema administrará ventas, pagos, comprobantes, inventario, caja, depósito, gastos, pagos de servicios, roles y auditoría.
- La exportación formal a Excel/PDF y los reportes avanzados quedan previstos como fase posterior.
## **1.3 Actores del sistema**

|**Actor**|**Descripción general**|**Nivel de acceso**|
| :- | :- | :- |
|Vendedor / Trabajador|Usuario operativo que atiende ventas, registra pagos, adjunta comprobantes y registra gastos diarios.|Operativo|
|Administrador|Usuario encargado de supervisar caja, inventario, productos, precios, promociones, gastos, consignaciones y pagos de servicios.|Administrativo|
|Gerente|Usuario con visibilidad total, responsable de usuarios, validaciones críticas, auditoría y aprobación de ajustes de inventario.|Directivo / máximo|
# **2. Revisión de evidencias actualizada**

|**No.**|**Evidencia**|**Cumple**|**Pregunta de revisión**|**Respuesta / observaciones actualizadas**|
| :- | :- | :- | :- | :- |
|1|Levantamiento de requerimientos|Sí|¿Qué funcionalidades tendrá el sistema?|Kontora POS permitirá registrar ventas en mostrador, manejar pagos en efectivo, transferencia e híbridos, administrar caja diaria, inventario general y stock diario de vasos, controlar gastos, depósito, consignaciones, pagos de servicios, evidencias, usuarios, roles y auditoría. La versión actualizada elimina el enfoque local/offline y confirma una aplicación 100% web.|
|2|Diagrama de Casos de Uso|Sí|¿Qué actores utilizarán cada módulo?|Los actores son vendedor/trabajador, administrador y gerente. El vendedor opera ventas, pagos, comprobantes y gastos. El administrador gestiona caja, inventario, precios, promociones, depósito y consultas. El gerente posee control superior sobre usuarios, aprobaciones, validaciones, auditoría y supervisión general.|
|3|Diagrama Entidad-Relación (DER)|Sí|¿Por qué diseñaron esas tablas?|Las tablas responden a procesos reales del negocio: seguridad, caja diaria, ventas, pagos, inventario, depósito, evidencias y auditoría. El diseño prioriza trazabilidad, integridad referencial y conservación histórica, evitando eliminación física de registros operativos.|
|4|Modelo relacional|Sí|¿Qué relación existe entre las entidades?|El modelo conecta roles con usuarios; usuarios con sesiones, cajas, ventas y auditoría; cajas con ventas, gastos, inventario diario y cierres; ventas con detalles y pagos; inventario con items, existencias y movimientos; cierres con movimientos de depósito; y evidencias con pagos, gastos, consignaciones o pagos de servicios.|
|5|Diccionario de datos|Sí|¿Cómo se documentarán los datos del sistema?|El diccionario documenta cada tabla con propósito, campos, llaves, obligatoriedad, tipo conceptual y descripción. Incluye entidades de seguridad, caja, ventas, inventario, depósito, evidencias, configuraciones y auditoría, usando nombres en español sin tildes ni caracteres especiales.|
|6|Mockups o prototipos|Sí|¿Cómo será el flujo del usuario?|El flujo inicia con login. Según el rol, el usuario accede a módulos autorizados. El vendedor se enfoca en ventas y pagos; el administrador en operación diaria, caja, inventario y depósito; el gerente en gestión superior, auditoría y aprobaciones. La interfaz debe ser clara para operación rápida en mostrador.|
|7|Arquitectura del sistema|Sí|¿Cómo estará organizada técnicamente la aplicación?|La arquitectura será web por capas: frontend React/TypeScript desplegado en Vercel, backend Java/Spring Boot ejecutado en Docker sobre VM Ubuntu Server, base de datos Supabase PostgreSQL, almacenamiento de evidencias en Supabase Storage y exposición segura de API mediante Cloudflare Tunnel.|
|8|Tecnologías utilizadas|Sí|¿Qué tecnologías utilizarán?|Se usarán React, TypeScript, Vite, Vercel, Java, Spring Boot, Spring Security, JWT, Spring Data JPA/Hibernate, Flyway, Supabase PostgreSQL, Supabase Storage, Docker, Docker Compose, VM Ubuntu Server, Cloudflare Tunnel, Cloudflare DNS, Git, GitHub, JUnit, Mockito y OpenAPI/Swagger.|
|9|Justificación de tecnologías|Sí|¿Qué tecnologías utilizarán y por qué?|El stack se selecciona por separación clara de capas, aprendizaje progresivo, costo bajo o cero, capacidad transaccional del backend, despliegue reproducible, seguridad por roles y continuidad con la prueba de concepto ya validada.|
|10|Diseño de navegación|Sí|¿Cómo será la navegación dentro del sistema?|La navegación se organizará por módulos visibles según rol: Dashboard, Ventas, Caja, Inventario, Depósito, Gastos, Reportes, Usuarios, Configuración y Auditoría. El menú debe ocultar acciones no autorizadas y el backend debe validar permisos antes de ejecutar operaciones.|
|11|Responsabilidades del equipo|Sí|¿Qué módulo desarrollará cada integrante?|La responsabilidad se asume de forma colectiva por el equipo, distribuida por frentes de trabajo: frontend, backend, base de datos, pruebas, documentación y despliegue. Todos los cambios deben versionarse, revisarse y documentarse antes de integrarse al proyecto principal.|
|12|Riesgo técnico principal del proyecto|Sí|¿Cuál es el mayor riesgo técnico del proyecto?|El mayor riesgo es la integración transaccional segura entre frontend, backend, base de datos, evidencias y despliegue. Se controla con prueba de concepto, separación de lógica en backend, migraciones Flyway, variables de entorno, auditoría, pruebas y despliegues versionados.|
# **3. Requisitos funcionales y no funcionales**
## **3.1 Requisitos funcionales**
Los requisitos funcionales se agrupan por dominio operativo para facilitar trazabilidad entre análisis, diseño, implementación y pruebas.

|**Codigo**|**Requisito funcional**|
| :- | :- |
|RF-01|El sistema debe permitir el inicio de sesion mediante un nombre de usuario alfanumerico y contrasena.|
|RF-02|El sistema debe permitir cerrar sesion de forma efectiva, invalidando la sesion activa para impedir el uso posterior mediante retroceso del navegador.|
|RF-03|El sistema debe permitir gestionar usuarios con los roles vendedor, administrador y gerente.|
|RF-04|El sistema debe permitir activar, inactivar o bloquear usuarios sin eliminar su historial operativo.|
|RF-05|El sistema debe registrar la trazabilidad de accesos, cierres de sesion y acciones sensibles realizadas por los usuarios.|

|**Codigo**|**Requisito funcional**|
| :- | :- |
|RF-06|El sistema debe permitir abrir una caja diaria antes de iniciar operaciones, registrando fecha, hora, usuario responsable y valor de base.|
|RF-07|El sistema debe impedir registrar ventas, gastos, paquetes abiertos o consumos diarios si no existe una caja diaria abierta.|
|RF-08|El sistema debe permitir cerrar la caja diaria, calculando ventas, efectivo, transferencias, gastos, adiciones, pago de trabajadores, diferencia de caja y valor a deposito.|
|RF-09|El sistema debe bloquear nuevas ventas y anulaciones una vez la caja diaria haya sido cerrada.|
|RF-10|El sistema debe permitir consultar el historial de aperturas, cierres, arqueos y diferencias de caja.|
|RF-11|El sistema debe mostrar en el cierre las transferencias pendientes, validadas y rechazadas asociadas a la jornada.|
|RF-12|El sistema debe excluir la base de caja del dinero contado y del valor enviado al deposito.|

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

|**Codigo**|**Requisito funcional**|
| :- | :- |
|RF-23|El sistema debe permitir registrar pagos en efectivo.|
|RF-24|El sistema debe permitir registrar pagos por transferencia, cargando evidencia fotografica y valor transferido.|
|RF-25|El sistema debe permitir pagos hibridos mediante la combinacion de un pago en efectivo y un pago por transferencia asociados a una misma venta.|
|RF-26|El sistema debe discriminar los valores recibidos en efectivo y por transferencia para el cierre de caja.|
|RF-27|El sistema debe permitir validar o rechazar transferencias posteriormente, conservando monto, evidencia, estado, usuario validador y observacion cuando aplique.|
|RF-28|El sistema debe permitir consultar evidencias de transferencias asociadas a ventas y pagos hibridos.|

|**Codigo**|**Requisito funcional**|
| :- | :- |
|RF-29|El sistema debe permitir registrar un unico total diario de adiciones mientras la caja este abierta.|
|RF-30|El sistema debe permitir editar el total diario de adiciones mientras la caja este abierta.|
|RF-31|El sistema debe permitir registrar gastos diarios tomados de la caja, con valor, descripcion y evidencia opcional.|
|RF-32|El sistema debe permitir que vendedores registren gastos, pero solo administrador o gerente puedan editarlos o anularlos.|
|RF-33|El sistema debe registrar un pago total diario a trabajadores y exigir su confirmacion antes del cierre de caja.|
|RF-34|Si el pago diario a trabajadores es cero, el sistema debe exigir confirmacion explicita antes del cierre.|

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

|**Codigo**|**Requisito funcional**|
| :- | :- |
|RF-48|El sistema debe acumular en el depósito diario el dinero efectivo disponible al finalizar cada cierre de caja, descontando siempre el valor de la base. La base de caja no debe sumarse al depósito, ya que corresponde a un monto fijo reservado para iniciar la operación del siguiente día.|
|RF-49|El sistema debe permitir consultar el saldo actual y el historial completo de movimientos del deposito.|
|RF-50|El sistema debe permitir registrar consignaciones bancarias realizadas desde el deposito, adjuntando evidencia fotografica.|
|RF-51|El sistema debe permitir registrar pagos de servicios realizados desde el deposito, como arriendo, energia, agua, internet u otros.|
|RF-52|El sistema debe descontar automaticamente del deposito los valores de consignaciones bancarias y pagos de servicios.|
|RF-53|El sistema debe conservar evidencias fotograficas de consignaciones bancarias y pagos de servicios para consulta posterior.|

|**Codigo**|**Requisito funcional**|
| :- | :- |
|RF-54|El sistema debe permitir configurar valores como base de caja, valor de adiciones, precios, promociones y pago diario a trabajadores.|
|RF-55|El sistema debe conservar historial de cambios de precios, promociones y configuraciones para evitar inconsistencias con operaciones historicas.|
|RF-56|El sistema debe registrar auditoria de operaciones sensibles como anulaciones, validaciones, rechazos, ajustes, cambios de precio, cambios de configuracion y cierres.|
|RF-57|El sistema debe permitir consultar ventas, gastos, inventario, cierres, comprobantes, deposito y movimientos por dia.|
|RF-58|El sistema debe permitir consultar diariamente la información operativa y financiera de la jornada, incluyendo el dinero ingresado, los métodos de pago utilizados, las ventas clasificadas por tamaño y tipo de granizado, el estado de la caja, el estado del inventario y las diferencias identificadas durante el cierre.|
|RF-59|El sistema debe permitir conservar y consultar archivos de evidencia asociados a transferencias, gastos, consignaciones y pagos de servicios.|
|RF-60|El sistema debe permitir comprimir imagenes desde el backend antes de almacenarlas, conservando informacion del archivo original y comprimido.|

## **3.2 Requisitos no funcionales**
Los requisitos no funcionales definen atributos de calidad, restricciones técnicas y condiciones operativas obligatorias para el sistema.

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
# **4. Arquitectura del sistema**
Kontora POS se implementará como una aplicación web separada por capas. El frontend cumple la función de interfaz de usuario; el backend concentra API REST, reglas de negocio, seguridad y transacciones; la base de datos conserva la persistencia relacional; el almacenamiento externo conserva evidencias; y la infraestructura de despliegue expone los servicios de forma controlada.

|**Capa / componente**|**Tecnología**|**Responsabilidad principal**|
| :- | :- | :- |
|Cliente web|Navegador en computador, portátil, tablet o móvil|Acceso al sistema para usuarios autorizados.|
|Frontend|React + TypeScript + Vite en Vercel|Interfaz, navegación, consumo de API y validaciones de experiencia de usuario.|
|API backend|Java + Spring Boot en Docker sobre VM Ubuntu Server|Reglas de negocio, seguridad, roles, transacciones, auditoría, cálculos y comunicación con base de datos.|
|Seguridad|Spring Security + JWT + sesiones persistidas|Autenticación propia, autorización por roles, cierre efectivo de sesión y control de tokens.|
|Persistencia|Supabase PostgreSQL mediante Session Pooler|Tablas, relaciones, integridad referencial, históricos y consulta de información.|
|Evidencias|Supabase Storage|Almacenamiento de comprobantes de transferencias, gastos, consignaciones y pagos de servicios.|
|Exposición API|Cloudflare Tunnel + Cloudflare DNS|Publicación segura de la API sin exponer directamente el puerto interno de la VM.|
|Control de cambios|Git + GitHub + Pull Requests|Versionamiento, trazabilidad de cambios, revisión e integración del código.|
## **4.1 Flujo de despliegue propuesto**
Usuario autorizado -> Frontend en Vercel -> Subdominio de API en Cloudflare -> Cloudflare Tunnel -> VM Ubuntu Server -> Contenedor Docker del backend Spring Boot -> Supabase PostgreSQL y Supabase Storage.
## **4.2 Separación de responsabilidades**

|**Componente**|**Responsabilidades**|
| :- | :- |
|Frontend|Presentación, captura de datos, navegación por rol, validaciones básicas y consumo de endpoints. No accede directamente a operaciones críticas de base de datos.|
|Backend|Validación de permisos, apertura/cierre de caja, ventas, promociones, pagos, evidencias, inventario, depósito, auditoría y transacciones.|
|Base de datos|Persistencia, llaves, relaciones, restricciones básicas, estados, saldos, históricos y soporte de consulta.|
|Storage|Conservación de archivos de evidencia; la base de datos almacena rutas y metadatos, no binarios pesados.|
# **5. Tecnologías seleccionadas y justificación**
## **5.1 Tecnologías utilizadas**

|**Área**|**Tecnología seleccionada**|**Uso principal**|
| :- | :- | :- |
|Editor de desarrollo|Visual Studio Code|Edición de frontend, backend, documentación y archivos de configuración.|
|Control de versiones|Git|Versionamiento local del código y documentación.|
|Repositorio remoto|GitHub|Fuente única de verdad, ramas, Pull Requests y revisión de cambios.|
|Documentación|Markdown|README, arquitectura, decisiones técnicas y documentación por módulo.|
|Frontend|React|Construcción de la interfaz web mediante componentes reutilizables.|
|Lenguaje frontend|TypeScript|Tipado estático para reducir errores en componentes, servicios y modelos de datos.|
|Herramienta frontend|Vite|Entorno de desarrollo rápido y generación del build del frontend.|
|Hosting frontend|Vercel|Despliegue del frontend y despliegues automáticos desde Git.|
|Backend|Java + Spring Boot|API REST, lógica de negocio, seguridad, transacciones y conexión con base de datos.|
|Seguridad backend|Spring Security + JWT|Autenticación propia, autorización por roles y control de sesiones.|
|Persistencia backend|Spring Data JPA / Hibernate|Mapeo entre entidades Java y tablas PostgreSQL.|
|Migraciones|Flyway|Control versionado de cambios de esquema de base de datos.|
|Base de datos|Supabase PostgreSQL|Persistencia relacional gestionada.|
|Conexión BD|Supabase Session Pooler|Conexión del backend hacia PostgreSQL gestionado usando pooler.|
|Almacenamiento de evidencias|Supabase Storage|Almacenamiento de imágenes y archivos asociados a pagos, gastos, consignaciones y servicios.|
|Contenedores|Docker|Empaquetado y ejecución reproducible del backend.|
|Orquestación local/VM|Docker Compose|Definición y ejecución del backend en la VM mediante archivo declarativo.|
|Servidor backend|VM Ubuntu Server|Alojamiento del backend contenedorizado.|
|Túnel/API pública|Cloudflare Tunnel|Exposición segura del backend sin abrir directamente el puerto interno a internet.|
|DNS/Dominio|Dominio propio + Cloudflare DNS|Separación de dominio frontend y subdominio API.|
|Variables de entorno|.env y .env.example|Configuración sin exponer secretos en el repositorio.|
|Pruebas backend|JUnit 5, Mockito y pruebas de integración|Validación de servicios, reglas de negocio y endpoints.|
|Documentación API|OpenAPI / Swagger|Consulta y validación de endpoints durante el desarrollo.|
## **5.2 Justificación por capa**
### **Frontend: React, TypeScript y Vite**
React permite construir interfaces por componentes reutilizables; TypeScript reduce errores al manejar modelos como ventas, pagos, cajas y movimientos; Vite simplifica el desarrollo y la generación del build para Vercel.
### **Backend: Java y Spring Boot**
Spring Boot permite construir una API REST modular con servicios, controladores, repositorios, seguridad y transacciones. Es la capa adecuada para reglas críticas como promociones, caja, inventario, depósito y auditoría.
### **Seguridad: Spring Security, JWT y sesiones persistidas**
La autenticación propia permite login con nombre de usuario y contraseña, autorización por roles y revocación de sesiones. Las contraseñas se almacenarán como hash seguro y los tokens no reemplazarán la validación de permisos en backend.
### **Base de datos: Supabase PostgreSQL**
PostgreSQL es adecuado por la necesidad de relaciones fuertes, integridad referencial e históricos. Supabase reduce la administración manual del motor y ofrece una base gestionada compatible con el enfoque relacional.
### **Evidencias: Supabase Storage**
Las evidencias se almacenarán como archivos externos y la base solo guardará metadatos y rutas. Esto evita cargar PostgreSQL con binarios y facilita conservar comprobantes asociados a operaciones financieras.
### **Docker y Docker Compose**
Docker hace reproducible la ejecución del backend. Docker Compose permite declarar variables, reinicio del servicio y configuración del contenedor en la VM.
### **Vercel, Cloudflare Tunnel y DNS**
Vercel simplifica despliegues del frontend desde GitHub. Cloudflare Tunnel permite exponer la API sin abrir directamente puertos internos de la VM. Cloudflare DNS separa dominio frontend y subdominio API.
### **GitHub, Flyway y documentación**
GitHub será fuente única de verdad; Flyway controlará cambios de esquema; Markdown documentará arquitectura, decisiones técnicas, módulos y API durante el ciclo de vida del software.
# **6. Decisiones técnicas principales**
La decisión técnica central es mantener la lógica de negocio compleja en el backend y dejar la base de datos como mecanismo de persistencia, integridad referencial, restricciones estructurales, trazabilidad e históricos.

|**Tema**|**Decisión adoptada**|**Impacto técnico**|
| :- | :- | :- |
|Lógica de negocio|Se implementa en servicios transaccionales del backend.|Facilita pruebas, depuración, mantenimiento y aprendizaje por módulos.|
|Base de datos|Conserva tablas, relaciones, llaves, estados, restricciones básicas, saldos, históricos y auditoría.|Reduce complejidad en SQL y protege integridad mínima.|
|Inventario|Descuentos, restauraciones, consumos, paquetes y ajustes aprobados se ejecutan desde backend.|Cada cambio debe generar movimiento de inventario auditable.|
|Inventario mínimo|Se agrega cantidad\_minima\_alerta a items\_inventario.|Permite alertas de bajo inventario sin cambiar la estructura principal.|
|Evidencias|La obligatoriedad de evidencia se valida en backend; la base guarda metadatos.|Evita pagos por transferencia sin soporte cuando la regla lo exija.|
|Aprobaciones|El backend verifica rol gerente antes de aprobar o rechazar ajustes.|Controla permisos antes de modificar inventario.|
|Depósito|El cierre calcula el valor a depósito excluyendo la base y registra movimiento.|Mantiene un libro histórico de entradas y salidas de depósito.|
|Auditoría|Las operaciones sensibles generan registros desde backend.|Permite rastrear anulaciones, cierres, validaciones, ajustes y configuraciones.|
|Eliminación física|No se eliminan registros históricos; se anulan, inactivan o cierran vigencias.|Protege trazabilidad operativa y financiera.|

# **7. Modelo relacional consolidado**
El modelo relacional está organizado por módulos para mantener trazabilidad entre procesos de negocio y persistencia. La relación entre entidades se define con llaves primarias, llaves foráneas, restricciones de unicidad y estados controlados.
## **7.1 Resumen de módulos y tablas principales**

|**Modulo**|**Tablas principales**|**Responsabilidad**|
| :- | :- | :- |
|Seguridad y usuarios|roles, usuarios, credenciales\_usuario, sesiones\_usuario|Gestionar acceso, roles, credenciales, sesiones y trazabilidad de autenticacion.|
|Caja diaria|cajas\_diarias, cierres\_caja|Controlar apertura, cierre y resumen financiero diario.|
|Ventas y pagos|tipos\_granizado, tamanos\_vaso, precios\_granizado, promociones, dias\_promocion, ventas, detalles\_venta, metodos\_pago, pagos\_venta|Registrar ventas, aplicar precios/promociones y soportar pagos efectivo, transferencia e hibridos.|
|Operaciones diarias de caja|adiciones\_diarias, pagos\_trabajadores\_diarios, gastos\_caja|Registrar valores que afectan el cuadre diario.|
|Inventario|categorias\_inventario, unidades\_medida, items\_inventario, existencias\_inventario\_general, existencias\_inventario\_diario, movimientos\_inventario, paquetes\_vasos\_abiertos, consumos\_diarios\_inventario, ajustes\_inventario|Controlar stock general, stock diario de vasos, consumos, perdidas y ajustes.|
|Deposito|movimientos\_deposito, consignaciones\_bancarias, tipos\_servicio, pagos\_servicios|Controlar efectivo acumulado y salidas desde el deposito.|
|Evidencias y auditoria|archivos\_evidencia, configuraciones\_sistema, auditoria\_operaciones|Conservar comprobantes, configuraciones historicas y registro de operaciones sensibles.|
## **7.2 Relaciones principales del modelo**

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
## **7.3 Reglas relacionales destacadas**
- Cada usuario pertenece a un rol y sus operaciones sensibles deben quedar trazadas.
- Cada caja diaria agrupa ventas, gastos, adiciones, pago a trabajadores, inventario diario y cierre.
- Cada venta mantiene detalle, pagos y estado; las ventas no se eliminan, se anulan cuando corresponde.
- Los pagos por transferencia conservan estado de validación y evidencias asociadas.
- El inventario tiene saldos de consulta rápida, pero el historial se reconstruye desde movimientos\_inventario.
- El cierre de caja genera movimientos de depósito y excluye la base de caja.
- Las evidencias se relacionan con pagos, gastos, consignaciones o pagos de servicios.
# **8. Diccionario de datos**
El diccionario de datos describe las tablas principales del modelo lógico. Los tipos se expresan de forma conceptual porque el documento corresponde al diseño lógico; el modelo físico en PostgreSQL deberá convertirlos a tipos concretos y migraciones Flyway.
## **8.1 Tabla: roles**
Propósito: Catalogo de roles operativos del sistema. Define los niveles de autorizacion basicos: vendedor, administrador y gerente.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_rol|PK|Si|Entero/UUID|Identificador unico del rol. Permite relacionar usuarios con permisos generales.|
|nombre\_rol|Unico|Si|Texto|Nombre del rol: Vendedor, Administrador o Gerente.|
|estado|-|Si|Texto/Enum|Controla si el rol esta activo o inactivo.|

Relaciones: Un rol puede estar asignado a muchos usuarios. Relacion: roles 1:N usuarios.

Importancia: Centraliza el control de acceso. Evita duplicar permisos directamente en cada usuario.
## **8.2 Tabla: usuarios**
Propósito: Registra las personas que operan o administran Kontora POS. El acceso sera por nombre de usuario, no por correo electronico.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_usuario|PK|Si|Entero/UUID|Identificador unico del usuario interno.|
|id\_rol|FK|Si|Entero/UUID|Referencia a roles.id\_rol. Define el nivel de acceso.|
|nombre\_usuario|Unico|Si|Texto|Identificador de login. Puede contener letras o numeros.|
|nombre\_completo|-|Si|Texto|Nombre real del trabajador o administrador.|
|estado|-|Si|Texto/Enum|Activo, inactivo o bloqueado segun la operacion.|
|fecha\_creacion|-|Si|Fecha/hora|Fecha en la que se creo el usuario.|
|fecha\_actualizacion|-|No|Fecha/hora|Ultima actualizacion del registro.|

Relaciones: Cada usuario pertenece a un rol. Un usuario puede abrir cajas, registrar ventas, validar transferencias, aprobar ajustes y generar auditoria.

Importancia: Es la tabla base para trazabilidad. Casi todas las operaciones sensibles deben registrar el usuario responsable.
## **8.3 Tabla: credenciales\_usuario**
Propósito: Guarda la informacion tecnica de autenticacion para login propio en Spring Boot con JWT. La contrasena nunca se almacena en texto plano.

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

Relaciones: Relacion 1:1 con usuarios. La FK id\_usuario garantiza que la credencial pertenezca a un usuario valido.

Importancia: Permite manejar autenticacion propia en una VM independiente de Supabase Auth y evita depender del correo electronico.
## **8.4 Tabla: sesiones\_usuario**
Propósito: Registra sesiones activas, cerradas, expiradas o revocadas para complementar JWT y evitar que una sesion siga siendo valida despues del cierre de sesion.

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

Relaciones: Un usuario puede tener muchas sesiones. Relacion: usuarios 1:N sesiones\_usuario.

Importancia: Permite invalidar sesiones desde backend aunque el navegador conserve paginas en cache o intente reutilizar un token.
## **8.5 Tabla: cajas\_diarias**
Propósito: Representa la jornada operativa diaria. Antes de vender, un administrador o gerente debe abrir la caja.

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

Relaciones: La caja diaria se relaciona con ventas, pagos indirectamente, gastos, adiciones, pago de trabajadores, inventario diario, consumos y cierre de caja.

Importancia: Es el eje operativo del sistema. Permite agrupar toda la actividad de un dia y bloquear operaciones despues del cierre.
## **8.6 Tabla: cierres\_caja**
Propósito: Guarda el resumen final de la jornada. Consolida ventas, pagos, transferencias, gastos, adiciones, pago de trabajadores y valor a deposito.

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

Relaciones: Relacion 1:1 con cajas\_diarias. Puede generar un movimiento de deposito de tipo entrada por cierre.

Importancia: Congela el resultado contable de la jornada y sirve como base para el deposito acumulado.
## **8.7 Tabla: tipos\_granizado**
Propósito: Catalogo de tipos de granizado vendidos.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_tipo\_granizado|PK|Si|Entero/UUID|Identificador unico del tipo.|
|nombre\_tipo|Unico|Si|Texto|Con licor o Sin licor.|
|estado|-|Si|Texto/Enum|Activo o inactivo.|

Relaciones: Un tipo de granizado se relaciona con precios, promociones y detalles de venta.

Importancia: Permite diferenciar reglas de precio y promocion sin registrar sabores.
## **8.8 Tabla: tamanos\_vaso**
Propósito: Catalogo de tamanos de vaso expresados en onzas.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_tamano\_vaso|PK|Si|Entero/UUID|Identificador unico del tamano.|
|onzas|Unico|Si|Entero|Valores esperados: 8, 12, 16, 20, 24, 32.|
|estado|-|Si|Texto/Enum|Activo o inactivo.|

Relaciones: Se relaciona con precios, promociones, detalles de venta e items de inventario tipo vaso.

Importancia: Permite que ventas e inventario usen el mismo catalogo de tamanos.
## **8.9 Tabla: precios\_granizado**
Propósito: Historial de precios normales de carta por tipo de granizado y tamano.

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

Relaciones: Un tipo y un tamano pueden tener muchos precios historicos. Relacion: tipos\_granizado 1:N precios\_granizado; tamanos\_vaso 1:N precios\_granizado.

Importancia: Evita que cambios futuros de precio alteren ventas historicas. Cada detalle de venta guarda el precio aplicado como snapshot.
## **8.10 Tabla: promociones**
Propósito: Configura promociones por tipo de granizado, tamano y tipo de beneficiario.

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

Relaciones: Se relaciona con tipos\_granizado, tamanos\_vaso, dias\_promocion y detalles\_venta.

Importancia: Permite aplicar promociones de clientes martes/miercoles y promociones a trabajadores cualquier dia, sin cambiar codigo.
## **8.11 Tabla: dias\_promocion**
Propósito: Define los dias de la semana en que aplica una promocion, especialmente para clientes.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_dia\_promocion|PK|Si|Entero/UUID|Identificador unico del dia configurado.|
|id\_promocion|FK|Si|Entero/UUID|Referencia a promociones.id\_promocion.|
|dia\_semana|-|Si|Texto/Enum|Martes, miercoles u otro dia aplicable.|

Relaciones: Una promocion puede tener varios dias asociados. Relacion: promociones 1:N dias\_promocion.

Importancia: Hace configurable la regla de promocion sin depender de valores fijos en el backend.
## **8.12 Tabla: ventas**
Propósito: Cabecera de cada venta registrada durante una caja diaria abierta.

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

Relaciones: Una caja diaria tiene muchas ventas. Una venta tiene uno o varios detalles y uno o varios pagos. Relacion: cajas\_diarias 1:N ventas; ventas 1:N detalles\_venta; ventas 1:N pagos\_venta.

Importancia: Conserva la trazabilidad completa de la transaccion y permite anular sin eliminar registros.
## **8.13 Tabla: detalles\_venta**
Propósito: Detalle transaccional de lo vendido. Aunque no se entregue recibo fisico, esta tabla es necesaria para auditoria, calculo e inventario.

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

Relaciones: Pertenece a una venta y referencia tipo de granizado, tamano y promocion opcional.

Importancia: Permite descontar vasos por tamano, reconstruir ventas, aplicar promociones por pares y auditar precios historicos.
## **8.14 Tabla: metodos\_pago**
Propósito: Catalogo de metodos reales de pago. El pago hibrido se representa combinando efectivo y transferencia en pagos\_venta.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_metodo\_pago|PK|Si|Entero/UUID|Identificador unico del metodo.|
|nombre\_metodo|Unico|Si|Texto|Efectivo o Transferencia.|
|estado|-|Si|Texto/Enum|Activo o inactivo.|

Relaciones: Un metodo de pago puede estar asociado a muchos pagos de venta. Relacion: metodos\_pago 1:N pagos\_venta.

Importancia: Evita modelar el pago hibrido como un metodo falso. Una venta hibrida tendra dos pagos: uno en efectivo y otro en transferencia.
## **8.15 Tabla: pagos\_venta**
Propósito: Registra los pagos asociados a una venta. Soporta efectivo, transferencia y combinaciones hibridas.

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

Relaciones: Una venta puede tener uno o varios pagos. La suma de pagos debe coincidir con total\_venta.

Importancia: Permite cierre de caja correcto, trazabilidad de transferencias pendientes y evidencia en pagos por transferencia.
## **8.16 Tabla: adiciones\_diarias**
Propósito: Registra el total de adiciones del dia. Es un unico registro editable mientras la caja este abierta.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_adicion\_diaria|PK|Si|Entero/UUID|Identificador unico del registro.|
|id\_caja\_diaria|FK/Unico|Si|Entero/UUID|Caja diaria asociada.|
|cantidad\_adiciones|-|Si|Entero|Cantidad total de adiciones del dia.|
|valor\_unitario|-|Si|Decimal|Valor unitario aplicado.|
|valor\_total|-|Si|Decimal|Cantidad por valor unitario.|
|id\_usuario\_registro|FK|Si|Entero/UUID|Usuario que registro o actualizo.|
|fecha\_registro|-|Si|Fecha/hora|Fecha del registro.|

Relaciones: Relacion 1:1 con cajas\_diarias.

Importancia: Suma ingresos al cuadre sin forzar que cada adicion este asociada a una venta individual.
## **8.17 Tabla: pagos\_trabajadores\_diarios**
Propósito: Registra el pago total diario a trabajadores, tomado fisicamente de la caja.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_pago\_trabajadores\_diario|PK|Si|Entero/UUID|Identificador unico.|
|id\_caja\_diaria|FK/Unico|Si|Entero/UUID|Caja diaria afectada.|
|valor\_total\_pagado|-|Si|Decimal|Valor total pagado a trabajadores.|
|descripcion|-|No|Texto|Nota opcional.|
|id\_usuario\_registro|FK|Si|Entero/UUID|Usuario que registro.|
|fecha\_registro|-|Si|Fecha/hora|Fecha del registro.|
|confirmado\_para\_cierre|-|Si|Booleano|Confirma que el valor fue revisado para cierre.|

Relaciones: Relacion 1:1 con cajas\_diarias.

Importancia: Es obligatorio antes de cerrar caja porque representa una salida real de efectivo.
## **8.18 Tabla: gastos\_caja**
Propósito: Registra gastos operativos tomados del dinero de la caja diaria.

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

Relaciones: Una caja diaria puede tener muchos gastos. Los archivos de evidencia pueden referenciar el gasto.

Importancia: Afecta directamente el efectivo esperado en el cierre, por eso debe ser auditable.
## **8.19 Tabla: categorias\_inventario**
Propósito: Catalogo de grupos de inventario.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_categoria\_inventario|PK|Si|Entero/UUID|Identificador unico de categoria.|
|nombre\_categoria|Unico|Si|Texto|Vasos, Dulces, Desechables, Producto con licor, Producto sin licor u otros.|
|estado|-|Si|Texto/Enum|Activa o inactiva.|

Relaciones: Una categoria agrupa muchos items de inventario. Relacion: categorias\_inventario 1:N items\_inventario.

Importancia: Permite ampliar el catalogo sin modificar la estructura de la base de datos.
## **8.20 Tabla: unidades\_medida**
Propósito: Catalogo de unidades utilizadas en inventario.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_unidad\_medida|PK|Si|Entero/UUID|Identificador unico.|
|nombre\_unidad|Unico|Si|Texto|Unidad, bolsa, paquete, rollo, etc.|
|abreviatura|-|Si|Texto|und, bolsa, paq, rollo, etc.|
|estado|-|Si|Texto/Enum|Activa o inactiva.|

Relaciones: Una unidad de medida puede ser usada por muchos items.

Importancia: Estandariza conteos y evita mezclar unidades no comparables.
## **8.21 Tabla: items\_inventario**
Propósito: Catalogo de todos los productos inventariables, vendibles o consumibles.

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
|cantidad\_minima\_alerta|-|Si|Entero|Cantidad minima para alerta de bajo inventario. No puede ser negativa. Por defecto puede iniciar en 0.|
|estado|-|Si|Texto/Enum|Activo o inactivo.|
|fecha\_creacion|-|Si|Fecha/hora|Fecha de creacion del item.|

Relaciones: Se relaciona con categorias, unidades, tamanos de vaso, existencias, movimientos, paquetes, consumos y ajustes.

Importancia: Es la base del inventario. Permite manejar vasos automaticamente y otros productos por consumo manual. El campo cantidad\_minima\_alerta permite que el backend genere alertas cuando la existencia general de un item este por debajo del umbral configurado.
## **8.22 Tabla: existencias\_inventario\_general**
Propósito: Saldo actual del stock general para todos los items de inventario.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_existencia\_general|PK|Si|Entero/UUID|Identificador unico de la existencia.|
|id\_item\_inventario|FK/Unico|Si|Entero/UUID|Item asociado.|
|cantidad\_actual|-|Si|Entero|Cantidad disponible en stock general.|
|fecha\_actualizacion|-|Si|Fecha/hora|Ultima actualizacion del saldo.|

Relaciones: Relacion 1:1 con items\_inventario.

Importancia: Permite consultar rapidamente existencias actuales. Los cambios deben estar respaldados por movimientos\_inventario.
## **8.23 Tabla: existencias\_inventario\_diario**
Propósito: Stock operativo diario exclusivo para vasos. Los vasos pasan del stock general al stock diario cuando se abren paquetes.

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

Relaciones: Una caja diaria tiene muchas existencias diarias, una por cada tamano de vaso usado.

Importancia: Permite controlar los vasos por jornada, descontarlos por venta y restaurarlos por anulacion antes del cierre.
## **8.24 Tabla: movimientos\_inventario**
Propósito: Libro de movimientos de inventario. Registra cada entrada, salida, venta, anulacion, perdida, ajuste o consumo.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_movimiento\_inventario|PK|Si|Entero/UUID|Identificador unico del movimiento.|
|id\_item\_inventario|FK|Si|Entero/UUID|Item afectado.|
|id\_caja\_diaria|FK|No|Entero/UUID|Caja diaria asociada si aplica.|
|tipo\_stock|-|Si|Texto/Enum|General o Diario.|
|tipo\_movimiento|-|Si|Texto/Enum|Entrada, salida, venta, anulacion\_venta, perdida, ajuste, consumo\_diario.|
|cantidad|-|Si|Entero|Cantidad movida.|
|sentido\_movimiento|-|Si|Texto/Enum|Entrada o salida.|
|referencia\_origen|-|Si|Texto|Origen obligatorio del movimiento: venta, anulacion\_venta, paquete, consumo\_diario, ajuste u otro proceso definido.|
|id\_referencia\_origen|-|Si|Texto/UUID|Identificador obligatorio del registro que origina el movimiento.|
|observacion|-|No|Texto|Nota opcional.|
|id\_usuario\_registro|FK|Si|Entero/UUID|Usuario que genero el movimiento.|
|fecha\_movimiento|-|Si|Fecha/hora|Fecha del movimiento.|

Relaciones: Un item tiene muchos movimientos. Una caja diaria puede tener muchos movimientos asociados.

Importancia: Es la fuente historica de verdad del inventario. Las existencias son saldos derivados o actualizados a partir de movimientos. Ningun movimiento debe quedar sin referencia al proceso que lo genero, porque esa referencia permite auditar ventas, anulaciones, paquetes abiertos, consumos y ajustes.
## **8.25 Tabla: paquetes\_vasos\_abiertos**
Propósito: Registra la apertura de paquetes de vasos para una caja diaria.

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

Relaciones: Cada registro genera salida del stock general, entrada al stock diario y perdida si hay vasos rotos.

Importancia: Modela el proceso fisico de abrir paquetes y evita mezclar tamanos de vaso.
## **8.26 Tabla: consumos\_diarios\_inventario**
Propósito: Registra el consumo manual diario de productos que no se descuentan por venta.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_consumo\_diario\_inventario|PK|Si|Entero/UUID|Identificador unico.|
|id\_caja\_diaria|FK|Si|Entero/UUID|Caja diaria asociada.|
|id\_item\_inventario|FK|Si|Entero/UUID|Item consumido.|
|cantidad\_consumida|-|Si|Entero|Cantidad consumida en el dia.|
|id\_usuario\_registro|FK|Si|Entero/UUID|Administrador o gerente que registra.|
|fecha\_registro|-|Si|Fecha/hora|Fecha del registro.|
|observacion|-|No|Texto|Nota opcional.|

Relaciones: Una caja diaria puede tener muchos consumos. Cada consumo genera salida del stock general.

Importancia: Aplica a dulces, desechables y bolsas de producto con/sin licor.
## **8.27 Tabla: ajustes\_inventario**
Propósito: Solicitudes de ajuste manual de inventario que requieren aprobacion del gerente.

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

Relaciones: Un ajuste aprobado genera un movimiento en movimientos\_inventario.

Importancia: Controla cambios manuales de inventario con autorizacion y trazabilidad.
## **8.28 Tabla: movimientos\_deposito**
Propósito: Libro de movimientos del deposito acumulado. Registra entradas por cierre y salidas por consignaciones o pagos de servicios.

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

Relaciones: Puede relacionarse 1:1 con un cierre de caja, una consignacion bancaria o un pago de servicio.

Importancia: Permite reconstruir el saldo del deposito y auditar cada salida de dinero acumulado.
## **8.29 Tabla: consignaciones\_bancarias**
Propósito: Registra consignaciones bancarias realizadas desde el deposito.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_consignacion\_bancaria|PK|Si|Entero/UUID|Identificador unico.|
|id\_movimiento\_deposito|FK/Unico|Si|Entero/UUID|Movimiento de deposito asociado.|
|valor\_consignado|-|Si|Decimal|Valor consignado.|
|fecha\_consignacion|-|Si|Fecha/hora|Fecha de consignacion.|
|id\_usuario\_registro|FK|Si|Entero/UUID|Administrador o gerente que registra.|
|observacion|-|No|Texto|Nota opcional.|
|estado|-|Si|Texto/Enum|Registrada o anulada.|

Relaciones: Cada consignacion bancaria corresponde a una salida del deposito.

Importancia: Separa transferencias recibidas de clientes de consignaciones bancarias realizadas por la empresa.
## **8.30 Tabla: tipos\_servicio**
Propósito: Catalogo de servicios que pueden pagarse desde el deposito.

|**Campo**|**Llave / restriccion**|**Obligatorio**|**Tipo conceptual**|**Descripcion e importancia**|
| :- | :- | :- | :- | :- |
|id\_tipo\_servicio|PK|Si|Entero/UUID|Identificador unico.|
|nombre\_servicio|Unico|Si|Texto|Arriendo, energia, agua, internet u otro.|
|estado|-|Si|Texto/Enum|Activo o inactivo.|

Relaciones: Un tipo de servicio puede estar asociado a muchos pagos de servicio.

Importancia: Estandariza pagos recurrentes y permite agregar nuevos servicios.
## **8.31 Tabla: pagos\_servicios**
Propósito: Registra pagos de servicios hechos con dinero del deposito.

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

Relaciones: Cada pago de servicio genera una salida del deposito.

Importancia: Permite controlar gastos no diarios que afectan el efectivo acumulado del negocio.
## **8.32 Tabla: archivos\_evidencia**
Propósito: Guarda referencias a fotografias o documentos de soporte. Los archivos se almacenan fuera de la base de datos, por ejemplo en Supabase Storage.

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

Relaciones: Puede relacionarse con pagos de venta, gastos, consignaciones o pagos de servicios. Debe pertenecer al menos a un proceso.

Importancia: Permite conservar comprobantes indefinidamente sin almacenar binarios pesados dentro de PostgreSQL.
## **8.33 Tabla: configuraciones\_sistema**
Propósito: Almacena valores configurables con vigencia historica.

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

Relaciones: Un usuario puede registrar muchas configuraciones. Relacion: usuarios 1:N configuraciones\_sistema.

Importancia: Evita sobrescribir valores historicos y protege cierres/ventas pasadas ante cambios de configuracion.
## **8.34 Tabla: auditoria\_operaciones**
Propósito: Registra operaciones sensibles para trazabilidad.

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

Relaciones: Un usuario puede generar muchas auditorias. Relacion: usuarios 1:N auditoria\_operaciones.

Importancia: Sostiene el control interno del sistema: anulaciones, ajustes, cierres, validaciones, cambios de precios y configuraciones.

# **9. Riesgo técnico del proyecto y controles**
El riesgo técnico principal del proyecto es la integración correcta y segura entre frontend, backend, base de datos, almacenamiento de evidencias y despliegue. Este riesgo aumenta porque el sistema debe mantener consistencia transaccional en ventas, pagos, caja, inventario y depósito, además de validar roles y conservar auditoría.
## **9.1 Riesgos y controles definidos**

|**Riesgo**|**Control definido**|
| :- | :- |
|Exposición de secretos|Uso estricto de .env, .env.example y variables de entorno en proveedores.|
|Reglas críticas dispersas|Centralizar lógica en servicios del backend.|
|Inconsistencias de inventario|Operaciones transaccionales y movimientos de inventario obligatorios.|
|Acceso indebido por rol|Autorización con Spring Security y validación antes de ejecutar acciones.|
|Descuadres de caja o depósito|Cierre transaccional y generación automática de movimiento de depósito.|
|Pérdida de trazabilidad|Auditoría desde backend para operaciones sensibles.|
|Despliegues no revisados|Uso obligatorio de Pull Requests.|
|Cambios manuales de base de datos|Migraciones versionadas con Flyway.|
## **9.2 Control específico del riesgo principal**

|**Frente de riesgo**|**Control propuesto**|
| :- | :- |
|Integración frontend-backend|Definir contratos de API, documentarlos con OpenAPI/Swagger y probar cada flujo antes de integrarlo a la rama principal.|
|Integración backend-base de datos|Usar Spring Data JPA/Hibernate, migraciones Flyway y transacciones de servicio para operaciones críticas.|
|Consistencia de inventario|Exigir movimientos\_inventario para ventas, anulaciones, paquetes, consumos y ajustes aprobados.|
|Caja y depósito|Ejecutar el cierre como operación transaccional y generar movimiento de depósito automáticamente excluyendo la base.|
|Seguridad y roles|Aplicar Spring Security, JWT, sesiones persistidas y validación de permisos antes de cada operación crítica.|
|Evidencias|Subir archivos mediante backend, comprimir imágenes y guardar metadatos en archivos\_evidencia.|
|Despliegue|Mantener Docker, .env.example, documentación de comandos, pruebas de arranque y control de cambios en GitHub.|

# **10. Conclusiones**
El proyecto Kontora POS se consolida como una solución web orientada a mejorar el control operativo y administrativo de un negocio de granizados, integrando en una misma plataforma los procesos de ventas, pagos, caja diaria, inventario, depósito, evidencias, usuarios, roles y auditoría. La definición de requisitos funcionales y no funcionales permite delimitar con claridad lo que el sistema debe realizar y las condiciones de calidad que debe cumplir durante su ejecución. La arquitectura seleccionada separa frontend, backend, base de datos, almacenamiento e infraestructura, lo que reduce el acoplamiento entre componentes y favorece la mantenibilidad del sistema. A nivel técnico, la decisión de centralizar la lógica crítica en el backend permite validar reglas de negocio, controlar permisos, ejecutar operaciones transaccionales y conservar trazabilidad sin sobrecargar la base de datos. El modelo relacional y el diccionario de datos proporcionan una base consistente para construir migraciones Flyway, entidades JPA, servicios transaccionales y consultas confiables. Aunque el principal riesgo técnico se encuentra en la integración segura y transaccional entre frontend, backend, base de datos, almacenamiento y despliegue, dicho riesgo puede mitigarse mediante la prueba de concepto validada, el uso de control de versiones, migraciones versionadas, pruebas automatizadas, auditoría de operaciones sensibles y despliegue reproducible. En conjunto, este documento proporciona una guía estructurada para continuar la ejecución del proyecto con criterios técnicos, académicos y de calidad coherentes con el ciclo de vida del desarrollo de software

