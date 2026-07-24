# Avance del proyecto Kontora POS

Este documento registra el avance real del proyecto para mantener control de contexto entre fases. Debe actualizarse al cerrar cada fase o modulo importante.

## Estado actual

- Fecha de registro: 2026-07-12.
- Rama actual: `chore/inicializacion-frontend`.
- Fase actual: Fase 4, Gestion administrativa de catalogos implementada y validada; Consultas y Auditoria conservan sus validaciones pendientes documentadas.
- Fase anterior validada: Fase 4, Gestion de usuarios frontend.
- Siguiente hito: completar las validaciones manuales pendientes de Consultas y Auditoria frontend.

## Fase 1: Creacion del proyecto y entorno Docker

Estado: completada y validada operativamente.

Cambios realizados:

- Se inicializo el repositorio local.
- Se creo la estructura base: `backend/`, `frontend/`, `infra/`, `database/` y `docs/`.
- Se agrego `.gitignore`.
- Se agrego `.env.example`.
- Se preparo `infra/compose.local.yml`.
- Se preparo `infra/compose.prod.yml`.
- Se copio el schema canonico recibido a `database/schema/kontora_pos_schema_v1_1.sql`.
- Se creo la migracion inicial Flyway como copia exacta del schema canonico en `database/migrations/V1__schema_inicial_kontora_pos.sql`.
- Se versiono documentacion base de arquitectura, despliegue, base de datos y decisiones tecnicas.
- Se copiaron documentos fuente relevantes dentro de `docs/`.

Validaciones realizadas:

- El hash del SQL original coincide con `database/schema/kontora_pos_schema_v1_1.sql`.
- El hash del SQL original coincide con `database/migrations/V1__schema_inicial_kontora_pos.sql`.
- `docker compose -f infra/compose.local.yml config` valido correctamente la configuracion.
- Se confirmo que la tabla `ventas` conserva `id_usuario_vendedor`.
- Se confirmo que la tabla `pagos_venta` conserva `valor_pago` y `estado_validacion`.
- Se confirmo que existen las tablas canonicas `existencias_inventario_general` y `existencias_inventario_diario`.
- El usuario levanto manualmente `kontora_pos_postgres_local`.
- PostgreSQL finalizo la inicializacion con `COMMIT`.
- La base local reporto 34 tablas publicas.
- La consulta de tablas canonicas devolvio `ventas`, `pagos_venta`, `existencias_inventario_general` y `existencias_inventario_diario`.

Observacion operativa:

- Codex no pudo levantar PostgreSQL local porque Docker rechazo la conexion al motor local desde esta sesion.
- Error observado: permiso denegado al acceder a `npipe:////./pipe/docker_engine`.
- Tambien se observo advertencia de acceso denegado a `C:\Users\corre\.docker\config.json`.
- Se reintento despues de confirmar que el usuario pertenece a `docker-users`, pero Codex siguio sin poder acceder al motor Docker desde esta sesion.
- La validacion operativa se completo manualmente desde PowerShell.

## Fase 2: Infraestructura base y estructura del proyecto

Estado: completada y validada.

Objetivo:

- Crear backend Spring Boot en `backend/`.
- Configurar Maven, Spring Web, JPA, PostgreSQL, Flyway, Spring Security, Validation y pruebas.
- Crear endpoint `GET /api/health`.
- Mantener `ddl-auto=validate`.
- Preparar `Dockerfile`.
- Compilar con `mvn clean test` antes de avanzar a modulos de negocio.

Cambios realizados:

- Se creo `backend/pom.xml` con Spring Boot, Spring Web, Spring Data JPA, PostgreSQL, Flyway, Spring Security, Validation, Actuator y pruebas.
- Se creo `backend/src/main/java/com/kontora/pos/KontoraPosApplication.java`.
- Se creo `GET /api/health`.
- Se configuro seguridad base stateless, permitiendo `/api/health` y endpoints de salud de Actuator.
- Se configuro `application.yml` con variables de entorno, JPA `ddl-auto=validate`, Flyway y PostgreSQL.
- Se agrego migracion Flyway de runtime en `backend/src/main/resources/db/migration/V1__schema_inicial_kontora_pos.sql`.
- La migracion de runtime conserva el SQL canonico y solo elimina las lineas externas `BEGIN;` y `COMMIT;` porque Flyway controla la transaccion.
- Se creo `backend/Dockerfile`.
- Se creo documentacion inicial del backend y estructura de paquetes.

Validaciones realizadas:

- `mvn clean test` ejecutado correctamente.
- Compilacion principal: 16 archivos Java.
- Pruebas compiladas: 2 archivos Java.
- Pruebas ejecutadas: 2.
- Fallos: 0.
- Spring Boot arranco en prueba de integracion con Java 21.
- La aplicacion conecto a PostgreSQL local `kontora_pos`.
- Flyway valido migraciones y dejo el schema en version 1.
- JPA/Hibernate inicio con `ddl-auto=validate`.
- `GET /api/health` respondio con `status=ok` y `service=kontora-pos-backend` dentro de la prueba de integracion.

Observaciones:

- El `PATH` de Windows prioriza Java 8, pero Maven usa Java 21 por `JAVA_HOME`.
- Se fijo el `maven-compiler-plugin` al compilador de `${java.home}/bin/javac` para evitar usar accidentalmente Java 8.
- Codex sigue sin poder acceder al motor Docker por `npipe:////./pipe/docker_engine`, por lo que la validacion Docker del backend debe ejecutarla el usuario manualmente.
- El usuario intento construir el backend en Docker y la construccion fallo antes de compilar el proyecto porque Docker Desktop no pudo resolver `registry-1.docker.io`.
- Error observado: `lookup registry-1.docker.io: no such host`.
- No se creo el contenedor `kontora_pos_backend_local`.
- El bloqueo actual de Docker corresponde a red/DNS/proxy de Docker Desktop, no al codigo del backend.
- El usuario corrigio el acceso a imagenes base de Docker y repitio la validacion.
- Docker construyo correctamente la imagen `infra-backend`.
- `kontora_pos_postgres_local` quedo saludable.
- `kontora_pos_backend_local` inicio correctamente.
- La primera llamada a `GET /api/health` termino la conexion de forma inesperada, probablemente porque el backend aun estaba terminando de arrancar.
- La segunda llamada a `GET /api/health` respondio correctamente:

```json
{"status":"ok","service":"kontora-pos-backend"}
```

Conclusion:

- Fase 2 queda cerrada.
- Se puede iniciar Fase 3, modulo 1: Seguridad, usuarios y sesiones.

## Fase 3: Modulo 1 - Seguridad, usuarios y sesiones

Estado: completado y validado.

Rama de trabajo:

- `feature/usuarios-sesiones`.

Tablas canonicas usadas:

- `roles`.
- `usuarios`.
- `credenciales_usuario`.
- `sesiones_usuario`.

Cambios realizados:

- Se implementaron entidades JPA respetando `@Table`, `@Column` y nombres reales del schema.
- Se agregaron repositorios para usuarios, credenciales y sesiones.
- Se implemento autenticacion con `nombre_usuario` y contrasena BCrypt almacenada en `credenciales_usuario.contrasena_hash`.
- Se implemento generacion y validacion de JWT con identificador `jti`.
- Se persiste la sesion en `sesiones_usuario.token_identificador` usando el `jti`, no el token completo.
- Se agrego filtro de autenticacion para validar token, expiracion, usuario activo y sesion `activa`.
- Se agregaron endpoints:
  - `POST /api/auth/login`.
  - `GET /api/auth/me`.
  - `POST /api/auth/logout`.
- Se agrego manejo de errores API con respuesta JSON.
- Se documento el modulo en `docs/modules/usuarios-sesiones.md`.

Reglas validadas:

- Usuario activo puede iniciar sesion.
- Usuario bloqueado no puede iniciar sesion.
- Login exitoso crea una sesion activa en `sesiones_usuario`.
- Token valido permite consultar `GET /api/auth/me`.
- Logout cambia la sesion a cerrada.
- El mismo token deja de autorizar despues del logout.

Validacion realizada:

- Comando: `mvn clean test`.
- Resultado: exitoso.
- Pruebas ejecutadas: 4.
- Fallos: 0.
- Errores: 0.

Observaciones:

- No se modifico el schema para acomodarlo al codigo.
- La auditoria explicita de login/logout quedo implementada posteriormente en el modulo "Auditoria transversal".
- La administracion de usuarios se implemento posteriormente en Fase 4. El cambio o restablecimiento de contrasena conserva un flujo pendiente independiente.

## Fase 3: Modulo 2 - Caja diaria

Estado: completado y validado.

Rama de trabajo:

- `feature/caja-diaria`.

Tablas canonicas usadas:

- `cajas_diarias`.
- `cierres_caja`.

Cambios realizados:

- Se implemento entidad JPA `CajaDiaria` respetando la tabla `cajas_diarias`.
- Se implemento entidad JPA `CierreCaja` respetando la tabla `cierres_caja` como preparacion para el modulo de cierre.
- Se agregaron repositorios para caja diaria y cierres de caja.
- Se agrego endpoint `POST /api/cajas-diarias` para apertura.
- Se agrego endpoint `GET /api/cajas-diarias/abierta`.
- Se agrego endpoint `GET /api/cajas-diarias/fecha/{fechaOperacion}`.
- Se implemento validacion de rol para apertura: solo `administrador` y `gerente`.
- Se implemento validacion para impedir mas de una caja por `fecha_operacion`.
- Se documento el modulo en `docs/modules/caja-diaria.md`.

Reglas validadas:

- Sin usuario autenticado no se puede abrir caja.
- Un `vendedor` no puede abrir caja.
- Un `administrador` puede abrir caja.
- Un `gerente` puede abrir caja.
- No se puede abrir una segunda caja para la misma fecha.
- La caja queda en estado `abierta`.
- La apertura registra `id_usuario_apertura`.
- La caja se puede consultar por fecha.
- La caja abierta se puede consultar.

Validacion realizada:

- Comando: `mvn clean test`.
- Resultado: exitoso.
- Pruebas ejecutadas: 8.
- Fallos: 0.
- Errores: 0.

Observaciones:

- No se agregaron migraciones nuevas porque el schema canonico ya contiene `cajas_diarias` y `cierres_caja`.
- `valor_base` se conserva en apertura porque existe en el schema, pero sigue excluido de efectivo contado y deposito segun la documentacion.
- El cierre contable completo queda pendiente para el modulo "Cierre de caja y deposito".

## Fase 3: Modulo 3 - Catalogos base

Estado: completado y validado.

Rama de trabajo:

- `feature/catalogos-base`.

Tablas canonicas usadas:

- `roles`.
- `metodos_pago`.
- `tipos_granizado`.
- `tamanos_vaso`.
- `precios_granizado`.
- `promociones`.
- `dias_promocion`.
- `categorias_inventario`.
- `unidades_medida`.
- `items_inventario`.
- `tipos_servicio`.

Cambios realizados:

- Se implementaron entidades JPA para los catalogos definidos por el schema.
- Se agregaron repositorios de lectura con consultas nativas para filtrar enums por valores canonicos.
- Se agrego endpoint `GET /api/catalogos/roles`.
- Se agrego endpoint `GET /api/catalogos/metodos-pago`.
- Se agrego endpoint `GET /api/catalogos/tipos-granizado`.
- Se agrego endpoint `GET /api/catalogos/tamanos-vaso`.
- Se agrego endpoint `GET /api/catalogos/categorias-inventario`.
- Se agrego endpoint `GET /api/catalogos/unidades-medida`.
- Se agrego endpoint `GET /api/catalogos/items-inventario`.
- Se agrego endpoint `GET /api/catalogos/precios-granizado/vigentes`.
- Se agrego endpoint `GET /api/catalogos/promociones/vigentes`.
- Se agrego endpoint `GET /api/catalogos/tipos-servicio`.
- Se documento el modulo en `docs/modules/catalogos-base.md`.

Reglas validadas:

- Sin usuario autenticado no se pueden consultar catalogos internos.
- Los catalogos base activos se consultan correctamente.
- Los precios vigentes se filtran por fecha y estado.
- Las promociones vigentes se filtran por fecha y estado.
- Las promociones devuelven sus dias configurados en `dias_promocion`.
- Los items inactivos no aparecen en operaciones normales.

Validacion realizada:

- Comando: `mvn clean test`.
- Resultado: exitoso.
- Pruebas ejecutadas: 12.
- Fallos: 0.
- Errores: 0.

Observaciones:

- No se agregaron migraciones nuevas porque el schema canonico ya contiene las tablas y datos iniciales de catalogos.
- No se implemento `items_inventario.cantidad_minima_alerta` porque esa columna aparece en documentacion adaptada, pero no existe en `kontora_pos_schema.txt`.
- La aplicacion exacta de promociones por tipo de comprador y dia de semana queda para el modulo "Ventas y pagos".

## Fase 3: Modulo 4 - Ventas y pagos

Estado: completado y validado.

Rama de trabajo:

- `feature/ventas-pagos`.

Tablas canonicas usadas:

- `ventas`.
- `detalles_venta`.
- `pagos_venta`.
- `cajas_diarias`.
- `usuarios`.
- `tipos_granizado`.
- `tamanos_vaso`.
- `precios_granizado`.
- `promociones`.
- `dias_promocion`.
- `metodos_pago`.

Cambios realizados:

- Se implementaron entidades JPA para `ventas`, `detalles_venta` y `pagos_venta`.
- Se agregaron repositorios para ventas, detalles y pagos.
- Se agrego endpoint `POST /api/ventas`.
- Se implemento calculo de precio vigente por tipo de granizado y tamano.
- Se implemento aplicacion de promocion 2x por conjuntos completos.
- Se implemento pago en efectivo con `valor_recibido_efectivo` y `cambio_entregado`.
- Se implemento pago por transferencia con `estado_validacion = 'pendiente'`.
- Se implemento pago hibrido como multiples filas en `pagos_venta`.
- Se corrigio aislamiento de pruebas entre caja diaria y ventas.
- Se documento el modulo en `docs/modules/ventas-pagos.md`.

Reglas validadas:

- No se puede vender sin caja abierta.
- La venta queda asociada a una caja diaria abierta.
- El total de venta coincide con los detalles calculados.
- La suma de pagos coincide con `ventas.total_venta`.
- La promocion 2x aplica por pares del mismo tamano.
- Las unidades sobrantes se cobran a precio normal.
- Transferencias quedan pendientes de validacion.

Validacion realizada:

- Comando: `mvn clean test`.
- Resultado: exitoso.
- Pruebas ejecutadas: 16.
- Fallos: 0.
- Errores: 0.

Observaciones:

- No se agregaron migraciones nuevas porque el schema canonico ya contiene ventas, detalles y pagos.
- La anulacion con restauracion de stock diario se implemento posteriormente en el modulo "Inventario operativo", junto con movimientos de inventario.
- Evidencias de transferencia quedan para el modulo "Evidencias y almacenamiento".

## Fase 3: Modulo 5 - Inventario operativo

Estado: completado y validado.

Rama de trabajo:

- `feature/inventario-operativo`.

Tablas canonicas usadas:

- `items_inventario`.
- `existencias_inventario_general`.
- `existencias_inventario_diario`.
- `movimientos_inventario`.
- `paquetes_vasos_abiertos`.
- `consumos_diarios_inventario`.
- `ajustes_inventario`.
- `ventas`.
- `detalles_venta`.
- `cajas_diarias`.
- `usuarios`.

Cambios realizados:

- Se implementaron entidades JPA para existencias generales, existencias diarias, movimientos, paquetes de vasos abiertos y consumos diarios.
- Se agregaron repositorios para inventario operativo con bloqueo pesimista en filas de existencia modificables.
- Se agrego endpoint `GET /api/inventario/existencias/general`.
- Se agrego endpoint `GET /api/inventario/existencias/diarias/abierta`.
- Se agrego endpoint `GET /api/inventario/existencias/diarias/caja/{idCajaDiaria}`.
- Se agrego endpoint `POST /api/inventario/paquetes-vasos`.
- Se agrego endpoint `POST /api/inventario/consumos-diarios`.
- Se agrego endpoint `GET /api/inventario/movimientos`.
- Se agrego endpoint `GET /api/inventario/ajustes`.
- Se agrego endpoint `POST /api/inventario/ajustes`.
- Se agrego endpoint `POST /api/inventario/ajustes/{idAjusteInventario}/aprobar`.
- Se agrego endpoint `POST /api/inventario/ajustes/{idAjusteInventario}/rechazar`.
- Se agrego endpoint `POST /api/ventas/{idVenta}/anular`.
- Se integro `POST /api/ventas` con descuento automatico de vasos en stock diario.
- Se integro anulacion de venta con restauracion de vasos en stock diario.
- Se implemento RF-47 usando `ajustes_inventario` para solicitud, aprobacion y rechazo de ajustes de stock general.
- La aprobacion de ajustes actualiza `existencias_inventario_general` y crea movimiento `ajuste` en `movimientos_inventario`.
- La solicitud, aprobacion y rechazo se registran en `auditoria_operaciones`.
- Se documento el modulo en `docs/modules/inventario-operativo.md`.
- Se actualizo `docs/modules/ventas-pagos.md` para reflejar la anulacion implementada en este modulo.

Reglas validadas:

- Sin usuario autenticado no se puede consultar inventario.
- Solo administrador o gerente puede registrar paquetes de vasos y consumos manuales.
- La apertura de paquetes descuenta stock general y aumenta stock diario.
- Los vasos rotos al abrir paquetes aumentan `cantidad_perdida`.
- Todo cambio de stock genera movimiento de inventario.
- Cada movimiento registra `referencia_origen` e `id_referencia_origen`.
- No se permite consumo manual sobre items `automatico_por_venta`.
- No se permiten movimientos que dejen stock general o diario negativo.
- Administrador solicita ajustes de stock general; gerente aplica directamente sus propios ajustes como control de stock general.
- Solo gerente puede aprobar o rechazar solicitudes pendientes de administrador.
- La solicitud administrativa de ajuste no modifica stock.
- La aprobacion de ajuste modifica stock general y registra movimiento `ajuste`.
- El rechazo de ajuste no modifica stock ni crea movimiento.
- No se aprueban ajustes que dejen stock general negativo.
- Una venta descuenta vasos de `existencias_inventario_diario`.
- Una anulacion restaura vasos de `existencias_inventario_diario`.
- Al abrir una nueva caja, el stock diario de cada item de vaso conserva el remanente de la jornada anterior; no inicia en cero por el cambio de dia.

Validacion realizada:

- Comando: `mvn clean test`.
- Resultado: exitoso, `BUILD SUCCESS`.
- Pruebas ejecutadas: 53.
- Fallos: 0.
- Errores: 0.
- Omitidas: 0.
- Fecha/hora de finalizacion: 2026-07-08T23:03:11-05:00.

Observaciones:

- No se agregaron migraciones nuevas porque el schema canonico ya contiene las tablas de inventario operativo.
- No se implemento `items_inventario.cantidad_minima_alerta` porque esa columna no existe en `kontora_pos_schema.txt`.
- El conteo fisico final y diferencias de inventario diario se completaran con el modulo "Cierre de caja y deposito".

## Fase 3: Modulo 6 - Gastos, adiciones y pago a trabajadores

Estado: completado y validado.

Rama de trabajo:

- `feature/gastos-adiciones-pago-trabajadores`.

Tablas canonicas usadas:

- `adiciones_diarias`.
- `pagos_trabajadores_diarios`.
- `gastos_caja`.
- `cajas_diarias`.
- `usuarios`.

Cambios realizados:

- Se implementaron entidades JPA para `adiciones_diarias`, `pagos_trabajadores_diarios` y `gastos_caja`.
- Se agregaron repositorios para operaciones diarias de caja.
- Se agrego servicio transaccional `OperacionesCajaService`.
- Se agrego endpoint `POST /api/operaciones-caja/adiciones-diarias`.
- Se agrego endpoint `GET /api/operaciones-caja/adiciones-diarias/abierta`.
- Se agrego endpoint `POST /api/operaciones-caja/gastos-caja`.
- Se agrego endpoint `GET /api/operaciones-caja/gastos-caja/abierta`.
- Se agrego endpoint `PUT /api/operaciones-caja/gastos-caja/{idGastoCaja}`.
- Se agrego endpoint `POST /api/operaciones-caja/gastos-caja/{idGastoCaja}/anular`.
- Se agrego endpoint `POST /api/operaciones-caja/pagos-trabajadores-diarios`.
- Se agrego endpoint `GET /api/operaciones-caja/pagos-trabajadores-diarios/abierta`.
- Se agrego endpoint `POST /api/operaciones-caja/pagos-trabajadores-diarios/{idPagoTrabajadoresDiario}/confirmar`.
- Se documento el modulo en `docs/modules/gastos-adiciones-pago-trabajadores.md`.

Reglas validadas:

- Sin usuario autenticado no se pueden consultar operaciones de caja.
- No se registran gastos sin caja abierta.
- El rol `vendedor` puede registrar gastos.
- El rol `vendedor` no puede editar ni anular gastos.
- `administrador` y `gerente` pueden editar y anular gastos.
- Un gasto editado conserva usuario, fecha y motivo de edicion.
- Un gasto anulado conserva usuario, fecha y motivo de anulacion.
- Las adiciones tienen un unico registro por caja y se pueden actualizar mientras la caja esta abierta.
- El pago diario a trabajadores puede registrarse y confirmarse para cierre.
- Si el pago a trabajadores es cero, requiere confirmacion explicita.
- El rol `vendedor` no puede registrar pago diario a trabajadores.

Validacion realizada:

- Comando: `mvn clean test`.
- Resultado: exitoso.
- Pruebas ejecutadas: 29.
- Fallos: 0.
- Errores: 0.
- Omitidas: 0.

Observaciones:

- No se agregaron migraciones nuevas porque el schema canonico ya contiene las tablas de operaciones diarias de caja.
- La consolidacion contable de gastos, adiciones y pago a trabajadores queda para el modulo "Cierre de caja y deposito".
- Evidencias de gastos quedan para el modulo "Evidencias y almacenamiento".
- La auditoria explicita de ediciones y anulaciones quedo implementada posteriormente en el modulo "Auditoria transversal".

## Fase 3: Modulo 7 - Cierre de caja y deposito

Estado: completado y validado.

Rama de trabajo:

- `feature/cierre-caja-deposito`.

Tablas canonicas usadas:

- `cajas_diarias`.
- `cierres_caja`.
- `ventas`.
- `pagos_venta`.
- `metodos_pago`.
- `gastos_caja`.
- `adiciones_diarias`.
- `pagos_trabajadores_diarios`.
- `movimientos_deposito`.
- `usuarios`.

Cambios realizados:

- Se completo la entidad JPA `CierreCaja` para persistir los totales del cierre.
- Se implemento entidad JPA `MovimientoDeposito` respetando la tabla `movimientos_deposito`.
- Se agrego repositorio para `movimientos_deposito`.
- Se agregaron consultas agregadas para ventas, pagos por metodo, transferencias por `estado_validacion` y gastos vigentes.
- Se agrego servicio transaccional `CierreCajaService`.
- Se agrego endpoint `POST /api/cajas-diarias/{idCajaDiaria}/cerrar`.
- Se agrego endpoint `GET /api/cajas-diarias/{idCajaDiaria}/cierre`.
- Se calcula `efectivo_esperado_sin_base`.
- Se registra `efectivo_contado_sin_base`.
- Se calcula `diferencia_caja`.
- Se calcula `valor_a_deposito` excluyendo `cajas_diarias.valor_base`.
- Se crea automaticamente `movimientos_deposito` con `tipo_movimiento_deposito = 'entrada_cierre'` cuando `valor_a_deposito > 0`.
- Se ajustaron fixtures de pruebas existentes para borrar `movimientos_deposito` antes de `cierres_caja`.
- Se documento el modulo en `docs/modules/cierre-caja-deposito.md`.

Reglas validadas:

- Solo `administrador` o `gerente` puede cerrar caja.
- No se puede cerrar una caja inexistente o que no este abierta.
- No se puede cerrar una caja sin registro de adiciones diarias.
- No se puede cerrar una caja sin pago diario a trabajadores confirmado.
- El cierre consolida ventas registradas.
- El cierre separa pagos en efectivo y transferencias.
- El cierre muestra transferencias pendientes, validadas y rechazadas.
- El cierre consolida gastos no anulados.
- La base de caja no se suma al deposito.
- El movimiento de deposito coincide con `valor_a_deposito`.
- Despues del cierre no se permiten nuevas ventas.
- Despues del cierre no se permiten anulaciones.

Validacion realizada:

- Comando inicial: `mvn clean test`.
- Error encontrado: fixtures de pruebas antiguas intentaban borrar `cierres_caja` antes de borrar `movimientos_deposito`, violando la FK `movimientos_deposito_id_cierre_caja_fkey`.
- Correccion aplicada: las limpiezas de pruebas borran primero `movimientos_deposito` para cajas de prueba.
- Comando final: `mvn clean test`.
- Resultado: exitoso.
- Pruebas ejecutadas: 34.
- Fallos: 0.
- Errores: 0.
- Omitidas: 0.

Observaciones:

- No se agregaron migraciones nuevas porque el schema canonico ya contiene `cierres_caja` y `movimientos_deposito`.
- El trigger canonico de base de datos actualiza `cajas_diarias` a estado `cerrada` despues de insertar un cierre.
- Si `valor_a_deposito` es cero, no se crea movimiento de deposito porque `movimientos_deposito.valor_movimiento` exige valor mayor que cero.
- Evidencias de transferencias, gastos, consignaciones y pagos de servicios quedan para el modulo "Evidencias y almacenamiento".
- La auditoria explicita del cierre y movimientos de deposito quedo implementada posteriormente en el modulo "Auditoria transversal".

## Fase 3: Modulo 8 - Evidencias y almacenamiento

Estado: completado y validado.

Rama de trabajo:

- `feature/evidencias-storage`.

Tablas canonicas usadas:

- `archivos_evidencia`.
- `pagos_venta`.
- `gastos_caja`.
- `consignaciones_bancarias`.
- `pagos_servicios`.
- `movimientos_deposito`.
- `usuarios`.

Cambios realizados:

- Se implemento entidad JPA `ArchivoEvidencia` respetando la tabla `archivos_evidencia`.
- Se implementaron entidades JPA `ConsignacionBancaria` y `PagoServicio` para soportar las relaciones reales del schema.
- Se agregaron repositorios para evidencias, consignaciones bancarias y pagos de servicios.
- Se agrego cliente configurable para Supabase Storage.
- Se agregaron variables de configuracion `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` y `SUPABASE_STORAGE_BUCKET`.
- Se agrego servicio transaccional `EvidenciasService`.
- Se agrego endpoint `POST /api/evidencias/pagos-venta/{idPagoVenta}`.
- Se agrego endpoint `GET /api/evidencias/pagos-venta/{idPagoVenta}`.
- Se agrego endpoint `POST /api/evidencias/gastos-caja/{idGastoCaja}`.
- Se agrego endpoint `GET /api/evidencias/gastos-caja/{idGastoCaja}`.
- Se agrego endpoint `POST /api/evidencias/consignaciones-bancarias/{idConsignacionBancaria}`.
- Se agrego endpoint `GET /api/evidencias/consignaciones-bancarias/{idConsignacionBancaria}`.
- Se agrego endpoint `POST /api/evidencias/pagos-servicios/{idPagoServicio}`.
- Se agrego endpoint `GET /api/evidencias/pagos-servicios/{idPagoServicio}`.
- Se agrego endpoint `GET /api/evidencias/{idArchivoEvidencia}`.
- Se implemento carga multipart con parte `archivo`.
- Se implemento compresion backend para imagenes `jpg`, `jpeg` y `png`, guardandolas como `jpg`.
- Se guardan rutas y metadatos en `archivos_evidencia`, sin binarios en PostgreSQL.
- Se documento el modulo en `docs/modules/evidencias-storage.md`.

Reglas validadas:

- Sin usuario autenticado no se pueden consultar evidencias.
- El archivo se almacena fuera de PostgreSQL.
- PostgreSQL guarda solo ruta y metadatos.
- Una evidencia queda asociada a un solo proceso.
- Solo se cargan evidencias de `pagos_venta` si el metodo de pago real es `transferencia`.
- No se permite cargar evidencia a un pago en efectivo.
- Un vendedor solo puede consultar evidencias de pagos o gastos propios desde el flujo que las origina; no dispone de una interfaz independiente de Evidencias.
- `administrador` y `gerente` pueden gestionar evidencias de deposito.
- Las imagenes se comprimen desde backend.
- Los PDF se conservan sin compresion.

Validacion realizada:

- Comando de modulo: `mvn -Dtest=EvidenciasIntegrationTest test`.
- Resultado: exitoso.
- Pruebas ejecutadas: 5.
- Fallos: 0.
- Errores: 0.
- Omitidas: 0.
- Comando completo: `mvn clean test`.
- Resultado: exitoso.
- Pruebas ejecutadas: 39.
- Fallos: 0.
- Errores: 0.
- Omitidas: 0.

Observaciones:

- No se agregaron migraciones nuevas porque el schema canonico ya contiene `archivos_evidencia`, `consignaciones_bancarias` y `pagos_servicios`.
- La base de datos conserva la restriccion `chk_archivos_relacion_unica`, que exige una unica relacion por evidencia.
- Las pruebas mockean el cliente de storage para no depender de red ni credenciales reales de Supabase.
- La validacion y rechazo formal de transferencias quedo implementada posteriormente en el modulo "Auditoria transversal".
- La implementacion operativa completa de consignaciones bancarias y pagos de servicios queda para modulos posteriores.

## Fase 3: Modulo 9 - Auditoria transversal

Estado: completado y validado.

Rama de trabajo:

- `feature/auditoria-operaciones`.

Tablas canonicas usadas:

- `auditoria_operaciones`.
- `usuarios`.
- `sesiones_usuario`.
- `cajas_diarias`.
- `cierres_caja`.
- `ventas`.
- `pagos_venta`.
- `gastos_caja`.
- `movimientos_deposito`.

Cambios realizados:

- Se implemento entidad JPA `AuditoriaOperacion` respetando la tabla `auditoria_operaciones`.
- Se agrego repositorio para auditoria.
- Se agrego servicio transversal `AuditoriaService`.
- Se implemento persistencia de `valor_anterior` y `valor_nuevo` como JSONB.
- Se captura `direccion_ip` desde el request HTTP, priorizando `X-Forwarded-For`.
- Se agrego helper `AuditoriaValores` para construir snapshots de auditoria.
- Se audita login sobre `sesiones_usuario` con accion `login`.
- Se audita logout sobre `sesiones_usuario` con accion `logout`.
- Se audita apertura de caja sobre `cajas_diarias` con accion `abrir`.
- Se audita cierre de caja sobre `cierres_caja` con accion `cerrar`.
- Se audita el movimiento automatico de deposito sobre `movimientos_deposito` con accion `crear`.
- Se auditan ediciones de gastos sobre `gastos_caja` con accion `editar`.
- Se auditan anulaciones de gastos sobre `gastos_caja` con accion `anular`.
- Se auditan anulaciones de ventas sobre `ventas` con accion `anular`.
- Se agrego endpoint `POST /api/pagos-venta/{idPagoVenta}/validar`.
- Se agrego endpoint `POST /api/pagos-venta/{idPagoVenta}/rechazar`.
- Se implemento validacion y rechazo de transferencias pendientes usando `pagos_venta.estado_validacion`.
- Se registran `id_usuario_validacion`, `fecha_validacion` y `observacion_validacion` al validar o rechazar transferencias.
- Se ajustaron limpiezas de pruebas para borrar `auditoria_operaciones` antes de borrar usuarios de fixture.
- Se documento el modulo en `docs/modules/auditoria-operaciones.md`.
- Se actualizaron documentos de modulos relacionados para reflejar pendientes resueltos.

Reglas validadas:

- Login genera registro en `auditoria_operaciones`.
- Logout genera registro en `auditoria_operaciones`.
- La auditoria registra usuario responsable.
- La auditoria registra tabla afectada.
- La auditoria registra accion realizada.
- La auditoria registra valores anteriores y nuevos cuando aplica.
- Apertura de caja genera auditoria.
- Cierre de caja genera auditoria.
- Movimiento automatico de deposito genera auditoria.
- Edicion y anulacion de gastos generan auditoria.
- Anulacion de ventas genera auditoria.
- Solo `administrador` y `gerente` pueden validar o rechazar transferencias.
- Solo pagos por transferencia pueden validarse o rechazarse.
- Solo transferencias pendientes pueden pasar a `validada` o `rechazada`.
- Validacion y rechazo de transferencias generan auditoria.

Validacion realizada:

- Comando de modulo: `mvn -Dtest=AuditoriaIntegrationTest test`.
- Resultado: exitoso, `BUILD SUCCESS` reportado por el usuario.
- Pruebas ejecutadas: 4.
- Fallos: 0.
- Errores: 0.
- Omitidas: 0.
- Comando completo: `mvn clean test`.
- Resultado: exitoso, `BUILD SUCCESS` reportado por el usuario.
- Pruebas ejecutadas: 43.
- Fallos: 0.
- Errores: 0.
- Omitidas: 0.
- Tiempo total reportado: 01:03 min.
- Fecha/hora de finalizacion reportada: 2026-07-06T22:25:04-05:00.

Observaciones:

- No se agregaron migraciones nuevas porque el schema canonico ya contiene `auditoria_operaciones` y los campos de validacion de `pagos_venta`.
- La auditoria se implemento desde backend, no con triggers de base de datos.
- Solicitud, aprobacion y rechazo de ajustes de inventario fueron resueltos posteriormente en la ampliacion RF-47 del modulo de inventario operativo.
- Cambios de precios, promociones y configuraciones quedan pendientes hasta implementar sus flujos administrativos.
- La consulta filtrada de auditoria queda para el modulo "Consultas operativas".

## Fase 3: Modulo 10 - Consultas operativas

Estado: completado y validado.

Rama de trabajo:

- `feature/consultas-operativas`.

Tablas canonicas usadas:

- `ventas`.
- `pagos_venta`.
- `metodos_pago`.
- `cajas_diarias`.
- `cierres_caja`.
- `gastos_caja`.
- `items_inventario`.
- `categorias_inventario`.
- `unidades_medida`.
- `tamanos_vaso`.
- `existencias_inventario_general`.
- `existencias_inventario_diario`.
- `movimientos_inventario`.
- `movimientos_deposito`.
- `archivos_evidencia`.
- `auditoria_operaciones`.
- `usuarios`.

Cambios realizados:

- Se implemento el modulo backend `consultas` para consultas de solo lectura.
- Se agrego controlador `GET /api/consultas/...`.
- Se agrego servicio transaccional de lectura con validacion de permisos por rol.
- Se agrego repositorio de consultas nativas tipadas con `NamedParameterJdbcTemplate`.
- Se agrego endpoint `GET /api/consultas/ventas`.
- Se agrego endpoint `GET /api/consultas/cierre`.
- Se agrego endpoint `GET /api/consultas/gastos`.
- Se agrego endpoint `GET /api/consultas/inventario/actual`.
- Se agrego endpoint `GET /api/consultas/inventario/movimientos`.
- Se agrego endpoint `GET /api/consultas/deposito/movimientos`.
- Se agrego endpoint `GET /api/consultas/transferencias`.
- Se agrego endpoint `GET /api/consultas/auditoria`.
- Se reforzo la limpieza de `AuditoriaIntegrationTest` para eliminar gastos residuales asociados a usuarios de prueba antes de borrar dichos usuarios.
- Se documento el modulo en `docs/modules/consultas-operativas.md`.

Reglas validadas:

- Las consultas no modifican informacion.
- Un `vendedor` solo consulta ventas, gastos y transferencias propias.
- Un `vendedor` puede recibir informacion operativa de inventario solo dentro de las consultas permitidas por backend; esto no habilita una interfaz independiente de Inventario.
- Un `vendedor` no puede consultar cierre, deposito ni auditoria.
- Un `administrador` consulta cierre, deposito y auditoria operativa.
- Un `administrador` no recibe auditoria de seguridad sobre `sesiones_usuario`.
- Un `gerente` tiene visibilidad completa de auditoria.
- Las transferencias pendientes o rechazadas se consultan usando `pagos_venta.estado_validacion`.
- El inventario actual consulta `existencias_inventario_general` y `existencias_inventario_diario`.

Validacion realizada:

- Comando de modulo: `mvn -Dtest=ConsultasOperativasIntegrationTest test`.
- Resultado: exitoso.
- Pruebas ejecutadas: 5.
- Fallos: 0.
- Errores: 0.
- Omitidas: 0.
- Comando completo: `mvn clean test`.
- Resultado: exitoso, `BUILD SUCCESS`.
- Pruebas ejecutadas: 48.
- Fallos: 0.
- Errores: 0.
- Omitidas: 0.
- Tiempo total reportado: 01:33 min.
- Fecha/hora de finalizacion reportada: 2026-07-06T22:48:34-05:00.

Observaciones:

- No se agregaron migraciones nuevas porque el schema canonico ya contiene todas las tablas consultadas.
- Las consultas usan endpoints `GET` y transacciones `readOnly`.
- El modulo no crea, edita ni anula informacion.
- Reportes exportables o agregados para tableros administrativos quedan pendientes para definicion de la siguiente fase.

## Fase 4: PR 1 - Inicializacion frontend

Estado: completada y validada.

Rama de trabajo:

- `chore/inicializacion-frontend`.

Objetivo:

- Inicializar frontend React + TypeScript + Vite en `frontend/`.
- Crear estructura modular base para Fase 4.
- Configurar `VITE_API_URL=http://localhost:8080/api`.
- Crear cliente HTTP base.
- Conectar contra `GET /api/health`.
- Crear documentacion inicial del frontend.

Documentacion incorporada:

- Se copio `C:\Users\corre\Downloads\fase_4_frontend_validacion.md` dentro del repo como `docs/development/fases/fase_4_frontend_validacion.md`.
- Se crearon:
  - `docs/frontend/estructura-frontend.md`.
  - `docs/frontend/flujo-autenticacion-frontend.md`.
  - `docs/frontend/guia-componentes.md`.
  - `docs/frontend/pantallas.md`.
  - `docs/frontend/integracion-backend-local.md`.

Cambios realizados:

- Se inicializo `frontend/package.json` con React, TypeScript, Vite y `lucide-react`.
- Se agrego `frontend/.env.example` con `VITE_API_URL=http://localhost:8080/api`.
- Se creo `frontend/src/app` para providers y rutas.
- Se creo `frontend/src/modules` con modulos base de autenticacion, caja, catalogos, ventas, inventario, gastos, deposito, evidencias y auditoria.
- Se creo `frontend/src/shared` con componentes, hooks, servicios, tipos y utilidades.
- Se implemento cliente HTTP base en `frontend/src/shared/services/apiClient.ts`.
- Se implemento servicio y hook de salud backend usando `GET /api/health`.
- Se creo una pantalla inicial operativa inspirada visualmente en la maqueta, sin copiarla como HTML/CSS estatico final.
- Se configuro CORS backend para permitir el frontend local de Vite en `http://localhost:5173` y `http://127.0.0.1:5173`.
- Se mantuvo la autenticacion backend: solo `OPTIONS /api/**`, `GET /api/health` y `POST /api/auth/login` quedan sin token; el resto sigue protegido por `anyRequest().authenticated()`.

Validacion realizada:

- Comando: `npm install`.
- Resultado: exitoso.
- Auditoria npm: 0 vulnerabilidades.
- Comando: `npm run build`.
- Resultado: exitoso.
- Vite generado en `frontend/dist`.
- Validacion HTTP local:

```json
{"status":"ok","service":"kontora-pos-backend"}
```
- Validacion CORS local desde navegador:

```javascript
{ status: "ok", service: "kontora-pos-backend" }
```

- Validacion enfocada de backend:

```text
mvn "-Dtest=HealthEndpointIntegrationTest,AutenticacionIntegrationTest" test
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- Validacion completa de backend tras CORS:

```text
mvn clean test
Tests run: 49, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Observaciones:

- El primer intento de `npm install` dentro del sandbox excedio el tiempo sin generar `package-lock.json`; se repitio con permisos aprobados para descargar dependencias.
- El primer intento de `npm run build` dentro del sandbox fallo por permisos de lectura de esbuild hacia directorios superiores; se repitio con permisos aprobados y compilo correctamente.
- La primera prueba en navegador detecto bloqueo CORS: el backend respondia `200 OK`, pero sin `Access-Control-Allow-Origin` para `http://127.0.0.1:5173`.
- `mvn spring-boot:run` no pudo iniciar en `8080` porque el contenedor Docker `kontora_pos_backend_local` ya estaba ocupando ese puerto.
- El contenedor Docker activo estaba construido con una imagen anterior al ajuste CORS; se reconstruyo con `docker compose --env-file infra\.env -f infra\compose.local.yml --profile backend up -d --build backend`.
- No se implementaron pantallas funcionales por modulo en esta PR.
- La siguiente PR sugerida por Fase 4 es `feature/frontend-auth`.

## Fase 4: Autenticacion frontend

Estado: completada y validada manualmente en navegador.

Rama de trabajo:

- `chore/inicializacion-frontend`.

Objetivo:

- Implementar login consumiendo `POST /api/auth/login`.
- Manejar token de forma controlada en frontend.
- Reconstruir sesion con `GET /api/auth/me`.
- Cerrar sesion con `POST /api/auth/logout`.
- Proteger la shell principal cuando no exista una sesion valida.

Documentacion actualizada:

- `docs/frontend/estructura-frontend.md`.
- `docs/frontend/flujo-autenticacion-frontend.md`.
- `docs/frontend/guia-componentes.md`.
- `docs/frontend/pantallas.md`.
- `docs/modules/usuarios-sesiones-frontend.md`.

Cambios realizados:

- Se implemento `AuthProvider` para centralizar estado de sesion, usuario autenticado, token, login, logout y reconstruccion de sesion.
- Se implemento `LoginPage` con formulario para `nombreUsuario` y `contrasena`.
- Se implemento `authService` para consumir endpoints reales `/auth/login`, `/auth/me` y `/auth/logout`.
- Se almacena el JWT en `sessionStorage` mediante `tokenStorage.ts`.
- Se protegio la app: sin token valido se muestra `/login`; con token valido se confirma sesion contra backend antes de mostrar la shell.
- Se actualizo `AppShell` para mostrar usuario autenticado, rol, estado de API y boton de logout.
- Se marco autenticacion como implementada dentro del resumen de modulos frontend.

Validacion realizada:

- Comando: `npm run build`.
- Resultado: exitoso.
- Validacion HTTP local:

```json
{"status":"ok","service":"kontora-pos-backend"}
```

- Validacion auth contra backend real con fixture local existente:

```text
POST /api/auth/login -> ok
GET /api/auth/me -> usuario test_auth_activo, rol vendedor
POST /api/auth/logout -> ok
```

- Validacion en navegador integrado:
  - `/login` muestra `Iniciar sesion`.
  - La pantalla muestra `API disponible`.
  - Login redirige a `/`.
  - La shell protegida muestra `Sesion activa`, usuario y rol.
  - Logout vuelve a `/login`.
  - Consola sin errores durante login/logout.
- Validacion manual del usuario:
  - El login funciona sin errores aparentes en navegador.

Observaciones:

- Por decision operativa del usuario, el frontend se continuara trabajando sobre `chore/inicializacion-frontend`.
- No se hizo merge ni cambio de rama.
- El siguiente modulo frontend sera layout principal por rol.

## Fase 4: Layout principal por rol

Estado: completado y validado manualmente en navegador.

Rama de trabajo:

- `chore/inicializacion-frontend`.

Objetivo:

- Implementar shell principal navegable despues de autenticacion.
- Mostrar navegacion visible segun rol (`vendedor`, `administrador`, `gerente`).
- Mantener backend como autoridad de permisos y reglas criticas.
- Dejar pantallas de negocio como pendientes hasta implementarlas por modulo.

Documentacion actualizada:

- `docs/frontend/estructura-frontend.md`.
- `docs/frontend/guia-componentes.md`.
- `docs/frontend/pantallas.md`.
- `docs/modules/layout-principal-roles-frontend.md`.

Cambios realizados:

- Se centralizo la declaracion de rutas frontend en `frontend/src/app/routes/appRoutes.ts`.
- Cada ruta define roles visibles, estado de pantalla, descripcion y endpoints documentados.
- Se agrego normalizacion de rol para consumir `nombreRol` devuelto por `/api/auth/me`.
- Se actualizo `AppShell` para filtrar navegacion por rol y permitir navegacion interna.
- Se agregaron paneles de inicio especificos para `vendedor`, `administrador` y `gerente`.
- Se agrego `RouteWorkspace` como vista base para modulos pendientes.
- Se ajusto `ModuleOverview` para mostrar solo la navegacion visible del rol autenticado.
- Se ajustaron estilos responsive para sidebar, tarjetas navegables, paneles por rol y vistas base.

Reglas respetadas:

- El frontend oculta o muestra opciones solo como mejora de experiencia.
- Los permisos finales siguen siendo responsabilidad del backend.
- No se inventaron endpoints nuevos; las rutas visibles referencian contratos ya documentados.
- Las pantallas de modulos operativos permanecen marcadas como pendientes.

Validacion realizada:

- Comando: `npm run build`.
- Resultado: exitoso.
- Validacion HTTP local:

```json
{"status":"ok","service":"kontora-pos-backend"}
```

- Validacion en navegador integrado contra backend real:
  - Login con `vendedor` muestra panel de vendedor y navegacion operativa.
  - Login con `administrador` muestra panel de administrador y navegacion administrativa.
  - Login con `gerente` muestra panel de gerente y navegacion gerencial.
  - La ruta `Transferencias` para `vendedor` muestra solo consulta documentada, no acciones de validacion.
  - Logout vuelve a `/login`.
  - Consola del navegador sin errores ni advertencias.

- Validacion manual del usuario:
  - El usuario confirmo que la verificacion en navegador quedo lista para continuar.

Observaciones:

- No se hizo commit, merge ni cambio de rama.
- No se marca ninguna pantalla de negocio como completada.
- El siguiente modulo frontend sera panel de caja abierta.

## Fase 4: Panel de caja abierta

Estado: completado y validado manualmente en navegador.

Rama de trabajo:

- `chore/inicializacion-frontend`.

Objetivo:

- Mostrar la caja diaria abierta dentro del layout autenticado.
- Consumir la API real `GET /api/cajas-diarias/abierta`.
- Permitir apertura de caja desde frontend solo para `administrador` y `gerente` cuando no exista caja abierta.
- Mantener backend como autoridad de permisos.

Documentacion actualizada:

- `docs/frontend/estructura-frontend.md`.
- `docs/frontend/guia-componentes.md`.
- `docs/frontend/pantallas.md`.
- `docs/modules/caja-diaria-frontend.md`.

Cambios realizados:

- Se agrego `frontend/src/modules/caja/types.ts` con el contrato real `CajaDiaria`.
- Se agrego `frontend/src/modules/caja/services/cajaService.ts`.
- Se implemento `CajaAbiertaPanel` para consultar y mostrar caja abierta.
- Se conecto la ruta `Caja` del layout al panel funcional.
- Se agregaron estados de carga, caja abierta, ausencia de caja y error de consulta.
- Se agrego formulario de apertura para `administrador` y `gerente` cuando no hay caja abierta.

Reglas respetadas:

- `vendedor` puede consultar el estado de caja, pero no ve formulario de apertura.
- La apertura visible en frontend es solo una mejora de experiencia.
- El backend sigue validando que solo `administrador` o `gerente` puedan abrir caja.
- No se inventaron campos; se usaron los campos reales de `CajaDiariaResponse`.

Validacion realizada:

- Comando: `npm run build`.
- Resultado: exitoso.
- Validacion HTTP local:

```json
{"status":"ok","service":"kontora-pos-backend"}
```

- Validacion API real:

```json
{
  "estadoCaja": "abierta",
  "fechaOperacion": "2200-01-01",
  "valorBase": 300000.00
}
```

- Validacion en navegador integrado contra backend real:
  - Login con `vendedor` muestra `/caja` con caja abierta.
  - Login con `administrador` muestra `/caja` con caja abierta.
  - El panel muestra fecha de operacion, estado, valor base, fecha de apertura, usuario de apertura y observaciones.
  - Logout vuelve a `/login`.
  - Consola del navegador sin errores ni advertencias.

- Validacion manual del usuario:
  - El usuario confirmo que se puede continuar despues de verificar el panel en navegador.

Observaciones:

- No se hizo commit, merge ni cambio de rama.
- Como ya existe caja abierta en la base local, la validacion no creo una caja nueva.
- El siguiente modulo frontend sera catalogos necesarios para formularios.

## Fase 4: Catalogos para formularios

Estado: completado y validado manualmente en navegador.

Rama de trabajo:

- `chore/inicializacion-frontend`.

Objetivo:

- Consultar catalogos base activos desde la API real para preparar formularios operativos.
- Mostrar precios vigentes, promociones vigentes, inventario activo y listas base.
- Mantener el modulo como solo lectura; las reglas definitivas quedan en backend.

Documentacion actualizada:

- `docs/frontend/estructura-frontend.md`.
- `docs/frontend/guia-componentes.md`.
- `docs/frontend/pantallas.md`.
- `docs/modules/catalogos-base-frontend.md`.

Cambios realizados:

- Se agrego `frontend/src/modules/catalogos/types.ts` con contratos reales de catalogos.
- Se agrego `frontend/src/modules/catalogos/services/catalogosService.ts`.
- Se implemento `CatalogosPanel` para consultar catalogos con token real.
- Se conecto la ruta `Catalogos` del layout al panel funcional.
- Se agrego filtro local por nombre de granizado, item o promocion.
- Se agrego selector de fecha para consultar precios y promociones vigentes.
- Se actualizaron estados visibles del layout para reflejar `Caja` y `Catalogos` como base lista.

Reglas respetadas:

- La pantalla no crea, edita ni elimina catalogos.
- La vigencia definitiva de precios y promociones queda en backend.
- La aplicacion real de promociones queda en el modulo de ventas y pagos.
- No se inventaron endpoints; se usaron los contratos de `docs/modules/catalogos-base.md`.

Validacion realizada:

- Comando: `npm run build`.
- Resultado: exitoso.
- Validacion HTTP local:

```json
{"status":"ok","service":"kontora-pos-backend"}
```

- Validacion API real con token:

```text
GET /api/catalogos/metodos-pago -> 2 registros
GET /api/catalogos/tipos-granizado -> 2 registros
GET /api/catalogos/tamanos-vaso -> 6 registros
GET /api/catalogos/categorias-inventario -> 5 registros
GET /api/catalogos/unidades-medida -> 4 registros
GET /api/catalogos/items-inventario -> 16 registros
GET /api/catalogos/precios-granizado/vigentes?fecha=2026-07-07 -> 12 registros
GET /api/catalogos/promociones/vigentes?fecha=2026-07-07 -> 12 registros
GET /api/catalogos/tipos-servicio -> 5 registros
```

- Validacion en navegador integrado contra backend real:
  - Login con `test_auth_activo` muestra la shell protegida.
  - `/catalogos` muestra datos reales de precios, promociones, inventario y listas base.
  - La ruta `Caja` aparece como `Base lista`.
  - La ruta `Catalogos` aparece como `Base lista` despues de la confirmacion manual.
  - Consola del navegador sin errores ni advertencias.

- Validacion manual del usuario:
  - El usuario confirmo continuar despues de revisar el panel en navegador.

Observaciones:

- No se hizo commit, merge ni cambio de rama.
- El siguiente modulo frontend sera registro de venta y pagos.

## Fase 4: Inventario operativo frontend

Estado: completado y validado manualmente en navegador.

Rama de trabajo:

- `chore/inicializacion-frontend`.

Cambios realizados:

- Se implemento `InventarioPanel` con stock general, stock diario opcional, movimientos, paquetes, consumos y ajustes RF-47.
- Se marco la ruta `Inventario` como `Base lista` para administrador y gerente.
- Se restringieron las rutas independientes de Inventario, Catalogos y Evidencias para vendedor.
- Gerente aplica directamente ajustes de stock general; administrador solicita y gerente aprueba o rechaza.
- La ausencia de caja abierta deja disponibles stock general, movimientos y ajustes, y bloquea solo operaciones diarias.
- La apertura de caja inicializa el stock diario de vasos con el remanente de la jornada anterior.

Documentacion actualizada:

- `docs/modules/inventario-operativo.md`.
- `docs/modules/inventario-operativo-frontend.md`.
- `docs/modules/inventario-operativo-frontend-pendientes.md`.
- `docs/modules/inventario-operativo-finalizacion-contexto.md`.
- `docs/frontend/estructura-frontend.md`.
- `docs/frontend/guia-componentes.md`.
- `docs/frontend/pantallas.md`.

Validacion realizada:

- `mvn -Dtest=InventarioIntegrationTest,CajaDiariaIntegrationTest test`: 16 pruebas, sin fallos ni errores.
- `npx tsc -b --pretty false`: exitoso.
- `npm run build`: exitoso fuera del sandbox por la restriccion conocida de `esbuild` dentro del sandbox.
- Backend Docker reconstruido; `GET /api/health` respondio correctamente.
- `GET /api/inventario/ajustes` respondio `200` para gerente y administrador, y `403` para vendedor.
- Se probaron solicitud, aprobacion, rechazo y aplicacion directa con datos controlados; el stock se restauro al valor inicial.
- Navegador validado con gerente, administrador y vendedor; el usuario confirmo la verificacion manual final sin errores aparentes.

Observaciones:

- No se hizo commit, merge ni cambio de rama.
- Ventas y pagos quedo validado manualmente y cerrado; el siguiente modulo de interfaz es Gastos, adiciones y pago a trabajadores.
- Ajuste posterior de interfaz: `/inventario` conserva stock diario, operaciones y ajustes; el stock general consolidado y los movimientos se centralizan en `/consultas`.

## Fase 4: Ventas y pagos frontend

Estado: completado y validado manualmente en navegador.

Rama de trabajo:

- `chore/inicializacion-frontend`.

Cambios realizados:

- Se implemento `VentasPanel` con detalles de venta, catalogos reales, promociones y pagos en efectivo, transferencia y modalidad mixta.
- Se marco la ruta `Ventas` como `Base lista` para vendedor, administrador y gerente.
- Se muestra el cambio estimado antes de registrar y el resultado devuelto por `VentaResponse` despues de la venta.
- Los faltantes y excedentes de transferencia se bloquean antes de enviar un payload invalido.
- El comprobante de transferencia se envia al backend como `FormData` despues de registrar la venta.

Documentacion actualizada:

- `docs/modules/ventas-pagos-frontend.md`.
- `docs/modules/ventas-pagos-frontend-pendientes.md`.
- `docs/frontend/estructura-frontend.md`.
- `docs/frontend/guia-componentes.md`.
- `docs/frontend/pantallas.md`.

Validacion realizada:

- `npx tsc -b --pretty false`: exitoso.
- `npm run build`: exitoso fuera del sandbox por la restriccion conocida de `esbuild` dentro del sandbox.
- Catalogos reales, pago en efectivo, transferencia y pago mixto validados en navegador sin errores de consola.
- Efectivo `10000` sobre total `8000`: cambio real `2000`.
- Pago mixto con transferencia `6000` y efectivo recibido `3000`: efectivo aplicado `2000` y cambio `1000`.
- La evidencia de transferencia fue preparada para backend; sin Supabase local configurado, la respuesta `503` es esperada y no bloquea el cierre de la interfaz.
- El usuario confirmo la validacion manual final el 2026-07-09.

Observaciones:

- No se hizo commit, merge ni cambio de rama.
- La carga real de evidencias queda pendiente de entorno de despliegue con Supabase Storage configurado solo en backend.
- El siguiente modulo frontend es Gastos, adiciones y pago a trabajadores.

## Fase 4: Gastos, adiciones y pago a trabajadores frontend

Estado: completado y validado manualmente en navegador.

Rama de trabajo:

- `chore/inicializacion-frontend`.

Cambios realizados:

- Se implemento `GastosPanel` con registro y consulta de gastos para los tres roles; administrador y gerente tambien pueden editar o anular gastos con su motivo auditado.
- Se ubico el formulario de pago diario a trabajadores en Gastos, visible solo para administrador y gerente; el pago confirmado sigue siendo actualizable mientras la caja este abierta.
- Se agrego `CajaOperacionesPanel` dentro de Caja para adiciones diarias, resumen de operaciones y proyeccion de efectivo fisico.
- Caja conserva el pago a trabajadores como dato de lectura para el cuadre, sin duplicar su formulario de escritura.
- Se agrego `GET /api/cajas-diarias/abierta/resumen` para obtener la proyeccion calculada por backend: ventas en efectivo, adiciones, gastos activos, pago a trabajadores, transferencias y base de caja.
- Los importes aceptan entrada por teclado y pegado con normalizacion de separadores; los paneles y sus botones mantienen altura y alineacion uniforme.
- Vendedor no ve ni consulta los controles administrativos de adiciones, pago a trabajadores o proyeccion financiera.

Documentacion actualizada:

- `docs/modules/gastos-adiciones-pago-trabajadores.md`.
- `docs/modules/gastos-adiciones-pago-trabajadores-frontend.md`.
- `docs/modules/caja-diaria-frontend.md`.
- `docs/frontend/estructura-frontend.md`.
- `docs/frontend/pantallas.md`.

Validacion realizada:

- `mvn clean test`: 58 pruebas, sin fallos ni errores.
- `npx tsc -b --pretty false`: exitoso.
- `npm run build`: exitoso fuera del sandbox por la restriccion conocida de `esbuild` dentro del sandbox.
- Backend Docker reconstruido y `GET /api/health` respondio correctamente.
- `GET /api/cajas-diarias/abierta/resumen` respondio `200` para administrador y gerente, y `403` para vendedor.
- `/caja` y `/gastos` respondieron `200` desde el servidor React local.
- El usuario confirmo manualmente el flujo, la ubicacion final del pago a trabajadores y la uniformidad visual de los paneles.

Observaciones:

- No se hizo commit, merge ni cambio de rama.
- La proyeccion no define el deposito: Cierre de caja usara el efectivo contado sin base devuelto y validado por backend.
- El siguiente modulo frontend es Cierre de caja y deposito.

## Fase 4: Cierre de caja y deposito frontend

Estado: completado y validado manualmente en navegador.

Rama de trabajo:

- `chore/inicializacion-frontend`.

Cambios realizados:

- Se implemento `CierreCajaPanel` para administrador y gerente con resumen financiero de la caja abierta, desglose de efectivo, transferencias, gastos, adiciones, pago a trabajadores y base reservada.
- Se registro el conteo fisico sin base, la diferencia de caja y el valor a deposito mediante el contrato real de cierre backend.
- Se implemento la consulta persistente de cierre por `fechaOperacion`, con historial y retorno a la operacion actual.
- La informacion del ultimo cierre se recupera desde backend despues de refrescar; `sessionStorage` solo conserva una fecha de consulta sugerida.
- Se agrego el valor base inicial de `300000`, editable al abrir caja.
- Se agregaron cuadros de confirmacion para apertura y cierre, con cancelacion sin efecto y bloqueo visual mientras se procesa la solicitud.
- Se reforzo la regla de una sola caja abierta en backend y base de datos mediante `uq_cajas_diarias_una_abierta`.
- Se marco Gastos y Cierre como rutas `Base lista`.

Reglas operativas confirmadas:

- La base de caja no hace parte del efectivo contado ni del deposito.
- Una jornada con `fechaOperacion` del dia `10` puede cerrarse despues de medianoche el dia `11`; `fechaCierre` registra el momento real sin alterar la jornada.
- Tras cerrar la jornada `10`, se puede abrir la jornada `11`.
- Mientras una caja este `abierta`, no se puede abrir otra caja, incluso si se intenta con otra fecha.

Documentacion actualizada:

- `docs/modules/cierre-caja-deposito-frontend.md`.
- `docs/modules/caja-diaria-frontend.md`.
- `docs/modules/cierre-caja-deposito.md`.
- `docs/database/kontora_pos_schema.txt`.
- `docs/frontend/estructura-frontend.md`.
- `docs/frontend/guia-componentes.md`.
- `docs/frontend/pantallas.md`.

Validacion realizada:

- `mvn clean test`: 60 pruebas, sin fallos ni errores.
- `npx tsc -b --pretty false`: exitoso.
- `npm run build`: exitoso fuera del sandbox por la restriccion conocida de `esbuild` dentro del sandbox.
- Backend Docker reconstruido; `GET /api/health` respondio `200`.
- `/caja` y `/cierre` respondieron `200` desde el servidor React local.
- El usuario confirmo en navegador el cierre, la persistencia tras refrescar, el historial por fecha, el regreso a la operacion actual, el valor base por defecto editable y los cuadros de confirmacion.

Observaciones:

- No se hizo commit, merge ni cambio de rama.
- El siguiente modulo frontend es Deposito, consignaciones y servicios.

## Fase 4: Deposito, consignaciones y servicios frontend

Estado: completado y validado manualmente en navegador.

Rama de trabajo:

- `chore/inicializacion-frontend`.

Cambios realizados:

- Se implementaron saldo actual, historial completo o por periodo, consignaciones bancarias y pagos de servicios contra endpoints reales.
- Se agregaron contratos backend para registrar salidas, obtener saldo e integrar el historial de consultas con los identificadores necesarios para Evidencias.
- Se serializan entradas de cierre y salidas de deposito mediante bloqueo transaccional para preservar el saldo posterior.
- La ruta Deposito queda como `Base lista` para administrador y gerente; vendedor queda excluido por frontend y backend.
- Cada salida solicita evidencia y confirmacion previa. Ante un `503` de Storage local, el registro financiero permanece creado y la interfaz conserva el reintento de evidencia.

Documentacion actualizada:

- `docs/modules/deposito-consignaciones-servicios.md`.
- `docs/modules/evidencias-storage.md`.
- `docs/modules/consultas-operativas.md`.
- `docs/frontend/estructura-frontend.md`.
- `docs/frontend/guia-componentes.md`.
- `docs/frontend/pantallas.md`.

Validacion realizada:

- `mvn clean test`: 64 pruebas, sin fallos ni errores.
- `npx tsc -b --pretty false` y `npm run build`: exitosos.
- Backend Docker activo y `GET /api/health` responde `200`.
- Administrador cerro caja de prueba con entrada automatica de deposito por `$50.000`, sin incluir la base de `$300.000`.
- Pago de servicio de `$2.500` y consignacion de `$28.000` actualizaron saldo e historial a `$19.500`.
- Gerente inicio sesion y accedio a `/deposito`; administrador y gerente operan el modulo con datos backend reales.
- Supabase Storage se mantiene intencionalmente sin configurar en local; si responde `503` al adjuntar una evidencia, no altera saldos ni movimientos financieros.
- El usuario confirmo la validacion manual final el 2026-07-10.

Observaciones:

- No se hizo commit, merge ni cambio de rama.
- El siguiente modulo frontend es Evidencias. Su validacion local debe comprobar estados, reintentos, consulta de metadata y permisos; la carga real a Supabase queda para despliegue.
- Ajuste posterior de interfaz: `/deposito` conserva saldo y formularios de salida; el historial por periodo se centraliza en `/consultas`, con iconos SVG para entradas y salidas.

## Fase 4: Evidencias frontend

Estado: completado y validado manualmente en navegador.

Rama de trabajo:

- `chore/inicializacion-frontend`.

Cambios realizados:

- Se implemento `EvidenciasPanel` para consultar los destinos de soporte de transferencias, gastos, consignaciones y pagos de servicios mediante los contratos reales de Consultas y Evidencias.
- Se agregaron servicios tipados para filtros de periodo, metadata y carga multipart de los cuatro tipos de evidencia soportados por backend.
- La pantalla propone desde el primer dia del mes hasta la fecha actual. El final por defecto evita que una unica fecha reduzca la consulta backend al primer dia del periodo.
- Los metadatos se cargan al seleccionar un registro, sin solicitar una evidencia por cada fila inicial.
- La ruta Evidencias queda como `Base lista` para administrador y gerente. Vendedor no recibe una interfaz independiente; sus soportes propios permanecen en los flujos de Ventas y Gastos.
- Se muestra el estado de Storage local y se conserva el archivo seleccionado para reintento durante la sesion cuando backend responde `503`.
- No se implemento vista previa ni descarga: el backend solo expone metadata y `urlArchivo` es una ruta interna `supabase://...`.

Documentacion actualizada:

- `docs/modules/evidencias-storage.md`.
- `docs/modules/evidencias-storage-frontend.md`.
- `docs/frontend/estructura-frontend.md`.
- `docs/frontend/pantallas.md`.

Validacion realizada:

- `npx tsc -b --pretty false` y `npm run build`: exitosos.
- React local se conecto a la instancia backend activa mediante `VITE_API_URL=http://127.0.0.1:8080/api` en un archivo local ignorado por Git.
- Gerente consulto `/evidencias` para el rango `2026-07-01` a `2026-07-10` y visualizo dos destinos de Deposito: consignacion por `$28.000` y pago de arriendo por `$2.500`.
- La seleccion de consignacion consulto metadata y mostro que no habia evidencia cargada.
- El usuario confirmo manualmente en navegador que los registros se muestran correctamente.

Observaciones:

- No se hizo commit, merge ni cambio de rama.
- Supabase Storage sigue intencionalmente sin configurar en local. La carga real se valida solo en despliegue, con secretos exclusivamente en backend.
- El siguiente modulo frontend es Transferencias y validacion administrativa.

## Fase 4: Transferencias y validacion administrativa frontend

Estado: completado y validado manualmente en navegador.

Rama de trabajo:

- `chore/inicializacion-frontend`.

Cambios realizados:

- Se implemento `TransferenciasPanel` con consulta de transferencias pendientes y rechazadas por periodo, contadores y detalle por pago.
- El detalle consulta bajo demanda la metadata de evidencias de cada `idPagoVenta` y conserva el limite de no mostrar contenido de rutas `supabase://...`.
- Vendedor solo recibe consulta de transferencias propias; administrador y gerente reciben controles de Validar y Rechazar conforme al backend.
- Las decisiones incluyen observacion opcional y confirmacion previa mediante `ConfirmationDialog`.
- Despues de validar, la lista se refresca. La transferencia validada deja de aparecer porque el endpoint de consultas solo expone `pendiente` y `rechazada`; la trazabilidad se conserva en `pagos_venta` y `auditoria_operaciones`.
- La ruta Transferencias queda como `Base lista` para los roles autorizados por la navegacion.

Documentacion actualizada:

- `docs/modules/transferencias-validacion-frontend.md`.
- `docs/modules/ventas-pagos-frontend.md`.
- `docs/modules/auditoria-operaciones.md`.
- `docs/modules/consultas-operativas.md`.
- `docs/frontend/estructura-frontend.md`.
- `docs/frontend/pantallas.md`.

Validacion realizada:

- `npx tsc -b --pretty false` y `npm run build`: exitosos.
- Con gerente, `/transferencias` cargo el rango de la jornada y no presento errores de consola.
- El usuario abrio la caja de la jornada `2026-07-11` y registro una venta de 12 oz con transferencia pura por `$12.000`, junto con una venta mixta de `$12.000` que incluyo `$8.000` por transferencia y `$4.000` en efectivo.
- El usuario valido ambas transferencias desde la interfaz.
- La consola confirmo dos auditorias `validar` sobre `pagos_venta`: cada una paso de `pendiente` a `validada`, con `test_deposito_gerente` como validador y fecha de validacion registrada.
- Las consultas de evidencia de ambos pagos respondieron `200` y listas vacias, coherentes con Storage local sin configurar.

Observaciones:

- No se hizo commit, merge ni cambio de rama.
- La accion Rechazar queda implementada con el mismo contrato y confirmacion; la prueba manual controlada de esta iteracion uso dos validaciones, no genero un rechazo adicional.
- El siguiente modulo frontend es Consultas operativas.

## Fase 4: Consultas operativas frontend

Estado: desarrollado e integrado contra backend real; pendiente confirmacion manual final antes de marcar la ruta como `Base lista`.

Rama de trabajo:

- `chore/inicializacion-frontend`.

Cambios realizados:

- Se implemento `ConsultasPanel` con filtros de periodo y vistas de Ventas, Gastos, Inventario, Cierre y Deposito.
- Vendedor recibe solo Ventas y Gastos; administrador y gerente reciben las cinco vistas administrativas autorizadas por backend.
- Inventario concentra stock general, stock diario y movimientos por periodo; `/inventario` conserva operaciones diarias, stock diario y ajustes.
- Deposito concentra movimientos por periodo con `Landmark` para entradas de cierre y `Building2` para salidas; `/deposito` conserva saldo y formularios de consignacion y pago de servicio.
- Los filtros se aplican de forma explicita con `Actualizar`; un cierre inexistente se muestra como resultado vacio controlado.
- No se modificaron schema ni backend. Se documento y se mantuvo sin implementar el posible desglose de stock diario por tipo de granizado, porque el stock fisico canonico sigue siendo unico por tamano y la consulta actual de ventas no devuelve sus detalles historicos por tipo y tamano.

Documentacion actualizada:

- `docs/modules/consultas-operativas.md`.
- `docs/modules/consultas-operativas-frontend.md`.
- `docs/modules/inventario-operativo.md`.
- `docs/modules/deposito-consignaciones-servicios.md`.
- `docs/frontend/estructura-frontend.md`.
- `docs/frontend/pantallas.md`.
- `docs/AVANCE_PROYECTO.md`.

Validacion tecnica realizada:

- `npx tsc -b --pretty false`: exitoso.
- `npm run build`: exitoso.
- Navegador con gerente confirmo ventas reales, inventario actual, estado de cierre sin registro, ausencia de movimientos en `/inventario`, ausencia de historial en `/deposito` y movimientos de deposito desde `/consultas`.
- Se verifico un registro real `entrada_cierre` en Consultas con icono SVG `Landmark`.

Pendiente de cierre:

- Confirmacion manual final de filtros de fecha y de visibilidad con vendedor, administrador y gerente.
- Actualizar `appRoutes.ts` para marcar `consultas` como `base` solo despues de esa confirmacion.

## Fase 4: Ajustes responsive y limpieza visual de interfaz

Estado: implementado; compilacion exitosa y patron de inicio de sesion revisado visualmente.

Rama de trabajo:

- `chore/inicializacion-frontend`.

Cambios realizados:

- Se actualizo la shell para usar menu desplegable en movil, con usuario y cierre de sesion en la parte superior derecha.
- Se retiraron indicadores visuales de API, integracion local, estado de ruta y endpoints internos.
- Se unificaron las barras de filtros de Transferencias, Evidencias y Consultas con la accion `Consultar`.
- Se traslado el formulario de Adiciones diarias a Ventas. Caja conserva el resumen financiero y la proyeccion de efectivo sin duplicar la accion.
- Se mejoro la presentacion de Inventario y Catalogos: nombres legibles sin guiones bajos, sin UUIDs ni rutas API visibles, y grids uniformes.
- Se reemplazo la maqueta decorativa de `/login` por un patron diagonal que llena el panel derecho sobre fondo `#f5f8fc`.

Documentacion actualizada:

- `docs/frontend/ajustes-responsive-interfaz.md`.
- `docs/frontend/estructura-frontend.md`.
- `docs/frontend/guia-componentes.md`.
- `docs/frontend/pantallas.md`.
- Documentos frontend de Layout, Ventas, Gastos, Inventario, Catalogos, Transferencias, Evidencias y Consultas.

Validacion tecnica realizada:

- `npx tsc -b --pretty false`: exitoso.
- `npm run build`: exitoso.
- `/login` revisado visualmente en escritorio con el nuevo patron.

Observaciones:

- No se modificaron backend, schema, contratos ni permisos.
- La validacion funcional autenticada por rol se conserva como verificacion manual antes de cerrar por completo la iteracion visual.
- No se hizo commit, merge ni cambio de rama.

## Reglas activas para las siguientes fases

- La base de datos sigue siendo la fuente principal de verdad.
- No se deben renombrar tablas ni columnas para acomodarlas al codigo.
- El entorno base ya fue validado con PostgreSQL local.
- Antes de pasar de fase o modulo se debe compilar y verificar en la maquina local.
- En Fase 2 se debe crear el backend Spring Boot sin implementar todavia modulos de negocio.
- La rama `main` representa el estado validado del proyecto.
- Cada modulo de Fase 3 debe desarrollarse en su propia rama `feature/[nombre-modulo]`.
- Al terminar un modulo, se debe ejecutar `mvn clean test`.
- Solo si compila y las pruebas pasan, se hace merge del modulo hacia `main`.
- No se inicia el siguiente modulo desde una rama anterior; siempre se parte de `main` actualizado.
- El frontend debe consumir la API real del backend.
- Las reglas criticas de negocio y permisos viven en backend.
- La maqueta visual es referencia de experiencia, no contrato de endpoints, campos ni reglas.

## Flujo Git por modulo

1. Confirmar que `main` contiene el ultimo estado validado.
2. Crear la rama del modulo desde `main`.
3. Implementar codigo y documentacion del modulo.
4. Ejecutar pruebas.
5. Corregir errores si los hay.
6. Hacer commit del modulo.
7. Volver a `main`.
8. Hacer merge de la rama del modulo.
9. Actualizar este documento con la validacion.

## Proxima validacion esperada

La ruta `/consultas` permanece en estado `pendiente` dentro de `chore/inicializacion-frontend` hasta que el usuario confirme manualmente:

- Aplicacion del periodo con fechas reales.
- Visibilidad de solo Ventas y Gastos para vendedor.
- Visibilidad administrativa de Inventario, Cierre y Deposito para administrador y gerente.
- Historiales centralizados: movimientos de inventario y de deposito visibles en Consultas, no en sus pantallas de registro.

Despues de esa confirmacion se podra marcar `consultas` como `base` y continuar con Fase 4: Auditoria frontend.

- `npm run build` en `frontend/`.
- Verificacion en navegador de consultas de ventas, gastos, inventario, cierres y deposito con los filtros disponibles por contrato.
- Confirmar que vendedor recibe solo sus datos permitidos y no accede a cierre, deposito ni auditoria.
- Confirmar que administrador y gerente conservan las diferencias documentadas de visibilidad en consultas y auditoria.
- Consola del navegador sin errores.

## Comandos manuales para validar Docker/PostgreSQL

Si Codex no puede acceder al motor Docker, ejecutar desde PowerShell:

```powershell
cd C:\Users\corre\Desktop\Kontora
docker compose --env-file infra\.env -f infra\compose.local.yml up -d postgres
docker ps --filter "name=kontora_pos_postgres_local"
docker logs kontora_pos_postgres_local --tail 80
docker exec kontora_pos_postgres_local psql -U kontora_pos -d kontora_pos -c "SELECT COUNT(*) AS tablas_publicas FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE';"
docker exec kontora_pos_postgres_local psql -U kontora_pos -d kontora_pos -c "SELECT to_regclass('public.ventas') AS ventas, to_regclass('public.pagos_venta') AS pagos_venta, to_regclass('public.existencias_inventario_general') AS existencias_general, to_regclass('public.existencias_inventario_diario') AS existencias_diario;"
```

Validacion esperada:

- El contenedor `kontora_pos_postgres_local` aparece en estado saludable o en ejecucion.
- Los logs terminan sin errores de SQL.
- La consulta de tablas devuelve un numero mayor que cero.
- Las tablas canonicas consultadas aparecen con nombre completo.

## Comandos manuales para validar backend en Docker

Ejecutar desde PowerShell:

```powershell
cd C:\Users\corre\Desktop\Kontora
docker compose --env-file infra\.env -f infra\compose.local.yml --profile backend up -d --build backend
docker ps --filter "name=kontora_pos_backend_local"
docker logs kontora_pos_backend_local --tail 120
Invoke-RestMethod -Uri "http://localhost:8080/api/health" | ConvertTo-Json -Compress
docker compose --env-file infra\.env -f infra\compose.local.yml --profile backend stop backend
```

Respuesta esperada del endpoint:

```json
{"status":"ok","service":"kontora-pos-backend"}
```

Si Docker no puede descargar imagenes desde Docker Hub, validar primero:

```powershell
docker pull eclipse-temurin:21-jre
docker pull maven:3.9-eclipse-temurin-21
```

Si esos comandos fallan con `lookup registry-1.docker.io: no such host`, se debe corregir DNS/proxy de Docker Desktop antes de repetir la validacion del backend.

## Fase 4: Gestion de usuarios frontend

Estado: base lista, validada tecnicamente y confirmada manualmente por el usuario.

Rama de trabajo:

- `chore/inicializacion-frontend`.

Requisitos cubiertos:

- RF-03: gestion de usuarios con roles `vendedor`, `administrador` y `gerente`.
- RF-04: activacion, inactivacion y bloqueo sin eliminar historial operativo.
- RF-05: trazabilidad de creacion, edicion y cambio de estado sobre `auditoria_operaciones`.
- CU-GER-01: crear, editar, inactivar, bloquear usuarios y asignar roles.

Cambios realizados:

- Se implemento `UsuariosPanel` y la ruta `/usuarios`, visible solo al gerente.
- Se integraron los endpoints reales de lista, roles activos, creacion, edicion y cambio de estado.
- La interfaz incluye directorio filtrable, formulario de alta y edicion, confirmacion de cambios de acceso y proteccion para que un gerente no restrinja su propia cuenta.
- Se marco la ruta `usuarios` como `Base lista` en `appRoutes.ts`.
- Se preparo el inicializador opcional del gerente inicial para una base vacia, gobernado por `BOOTSTRAP_MANAGER_*` en `infra/.env` y documentado para la maquina virtual.

Documentacion actualizada:

- `docs/modules/usuarios-sesiones.md`.
- `docs/modules/usuarios-sesiones-frontend.md`.
- `docs/frontend/estructura-frontend.md`.
- `docs/frontend/pantallas.md`.
- `docs/deployment/guia-despliegue-poc.md`.

Validacion realizada:

- `BootstrapManagerInitializerTest`: 2 pruebas, sin fallos ni errores.
- `GestionUsuariosIntegrationTest`: 3 pruebas, sin fallos ni errores.
- `npm run build` en `frontend/`: exitoso.
- El usuario confirmo manualmente que el apartado de usuarios funciona correctamente con rol gerente.

Observaciones:

- Solo gerente puede gestionar usuarios. Administrador y vendedor permanecen excluidos por frontend y backend.
- No se implemento cambio ni restablecimiento de contrasena porque requiere un flujo y contrato propios.
- No se hicieron commits, merges ni cambios de rama.

## Fase 4: Gestion administrativa de catalogos

Estado: implementada y validada tecnicamente. Pendiente una comprobacion visual puntual del control de vasos restaurado.

Rama de trabajo:

- `chore/inicializacion-frontend`.

Requisitos cubiertos:

- RF-36: crear, editar, consultar e inhabilitar items de inventario.
- RF-37: crear cada item con stock general inicial.
- RF-38: conservar stock diario operativo para vasos descontados automaticamente por venta.
- RF-39: registrar paquetes de vasos de 20 unidades para abastecer el stock diario.
- RF-54: configurar precios de granizado.
- RF-55: conservar historial de precios.
- RF-56: auditar operaciones sensibles de items y precios.

Cambios realizados:

- Se implementaron rutas administrativas para listar, crear, editar y cambiar el estado de items de inventario.
- Un item creado genera su existencia general inicial en cero; no se permite inactivarlo mientras conserve stock.
- Se impide alterar la estructura de un item que ya tiene movimientos, para preservar trazabilidad.
- Se implementaron consulta historica y alta de vigencias de precio. El precio abierto anterior se cierra sin modificar ventas historicas.
- Se registran en `auditoria_operaciones` la creacion, edicion, cambio de estado y cambio de precio.
- Se agrego la vista `Gestion` en `/catalogos` para administrador y gerente, mientras la consulta de catalogos se conserva.
- El formulario de productos conserva consumo manual y vaso por venta, con categoria `vasos`, tamano requerido y paquetes fijos de 20 unidades para el control automatico.
- La configuracion de precios se mantiene en un panel independiente por tipo de granizado y tamano; no modifica existencias ni el control de vasos de RF-38 y RF-39.

Documentacion actualizada:

- `docs/modules/catalogos-base.md`.
- `docs/modules/catalogos-base-frontend.md`.
- `docs/frontend/estructura-frontend.md`.
- `docs/frontend/guia-componentes.md`.
- `docs/frontend/pantallas.md`.
- `docs/development/fases/fase_4_frontend_validacion.md`.

Validacion realizada:

- `GestionCatalogosIntegrationTest`: 4 pruebas, sin fallos ni errores.
- `mvn clean test`: 73 pruebas, sin fallos ni errores.
- `npm run build` en `frontend/`: exitoso.
- Backend local con token de gerente: `GET /api/catalogos/gestion/items-inventario` devolvio 16 items y `GET /api/catalogos/gestion/precios-granizado` devolvio 12 precios.
- Pendiente: confirmar en navegador que el control `Vaso por venta` muestra categoria `vasos`, tamano requerido y paquetes fijos de 20 unidades.

Observaciones:

- No se agregaron migraciones ni se modifico el schema canonico.
- Administrador y gerente pueden gestionar catalogos; vendedor permanece sin acceso a la ruta independiente.
- No se hicieron commits, merges ni cambios de rama.
