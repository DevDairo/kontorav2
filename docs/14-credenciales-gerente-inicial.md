# 14. Credenciales del gerente inicial

## Objetivo

Configurar o cambiar las credenciales del usuario `gerenteLocal` que el backend crea automaticamente en una instalacion nueva.

## Regla fundamental

El bootstrap crea el gerente solo cuando la tabla `usuarios` esta vacia. Cambiar las variables de entorno y reiniciar el backend no modifica, reemplaza ni restablece un usuario que ya existe. Esta proteccion evita sobrescribir accesos de una instalacion operativa.

## Caso 1: antes del primer arranque

1. Crear `infra/.env` a partir de `infra/.env.example`.
2. Editar estas variables con los datos deseados:

```env
BOOTSTRAP_MANAGER_ENABLED=true
BOOTSTRAP_MANAGER_USERNAME=gerenteLocal
BOOTSTRAP_MANAGER_FULL_NAME=Gerente Local
BOOTSTRAP_MANAGER_PASSWORD=<contrasena-de-8-a-72-caracteres>
```

3. Guardar el archivo sin versionarlo.
4. Iniciar o recrear el backend local:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --build backend
```

5. Verificar que el backend inicio:

```powershell
Invoke-WebRequest http://127.0.0.1:8080/api/health
```

6. Iniciar sesion en `/login` con `BOOTSTRAP_MANAGER_USERNAME` y `BOOTSTRAP_MANAGER_PASSWORD`.
7. Tras confirmar el acceso, cambiar `BOOTSTRAP_MANAGER_ENABLED=false` y recrear el backend con el mismo comando del paso 4.

## Caso 2: el gerente ya existe

No cambies las variables bootstrap esperando que actualicen la cuenta: no tendran efecto mientras haya usuarios registrados.

Para modificar datos o contrasena:

1. Inicia sesion con un gerente activo.
2. Abre `/usuarios`.
3. Edita el usuario para cambiar nombre o estado, o usa el formulario de restablecimiento para asignar una nueva contrasena.
4. Confirma el cambio e inicia sesion nuevamente con la nueva contrasena.

Solo el rol `gerente` puede ejecutar este flujo. El cambio queda auditado y la contrasena no se muestra ni se almacena en texto plano.

## Caso 3: reiniciar una instalacion local de prueba

Usa este caso solo en desarrollo local, cuando no se necesita conservar ninguna venta, usuario, caja, evidencia ni dato del volumen Docker. No usar en produccion.

1. Cambia las variables `BOOTSTRAP_MANAGER_*` en `infra/.env`.
2. Elimina el volumen local y levanta una base limpia:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml down -v
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --build
```

3. Comprueba salud e inicia sesion con las nuevas credenciales.

```powershell
Invoke-WebRequest http://127.0.0.1:8080/api/health
```

## Servidor de produccion

Antes de la primera ejecucion en el contenedor PostgreSQL vacio, definir las
mismas variables en `infra/.env`, iniciar PostgreSQL y despues el backend. Ver
la [Fase 1 de infraestructura](migracion-infraestructura/01-fase-postgresql-backend.md)
y el [README principal](../README.md#primer-gerente-en-una-instalacion-nueva).

En produccion no se debe borrar la base para cambiar credenciales. Usar siempre la gestion de usuarios por un gerente activo, conservar respaldos y mantener `BOOTSTRAP_MANAGER_ENABLED=false` despues del primer acceso.

## Caso 4: reiniciar por completo PostgreSQL de produccion

Este proceso es destructivo. Usarlo solo si se desea iniciar otra vez con los datos predeterminados del proyecto. Elimina la totalidad de las tablas, tipos y datos de la aplicacion en el esquema `public`: usuarios, catalogos, precios, promociones, existencias, ventas, cajas, movimientos, auditoria y metadatos de evidencias.

Los objetos fisicos de Storage no se eliminan con este proceso. Quedaran sin
referencia si pertenecian a registros eliminados y se deben revisar por
separado.

1. Crear y verificar el respaldo descrito en el
   [Paso 7 de la Fase 1](migracion-infraestructura/01-fase-postgresql-backend.md#paso-7-crear-y-comprobar-un-respaldo-local).
2. En la VM, editar `~/apps/kontora/infra/.env` y preparar el gerente que se creara al finalizar las migraciones:

```env
BOOTSTRAP_MANAGER_ENABLED=true
BOOTSTRAP_MANAGER_USERNAME=gerenteLocal
BOOTSTRAP_MANAGER_FULL_NAME=Gerente Local
BOOTSTRAP_MANAGER_PASSWORD=<contrasena-segura-de-8-a-72-caracteres>
```

3. Detener solo el backend:

```bash
cd ~/apps/kontora
docker compose --env-file infra/.env -f infra/compose.prod.yml stop backend
```

4. En `infra/.env`, asignar un nombre de volumen nuevo y unico. No reutilizar
   el nombre anterior:

```env
POSTGRES_VOLUME_NAME=kontora_pos_postgres_prod_reset_YYYYMMDDHHMM
```

5. Crear PostgreSQL sobre el volumen nuevo y reconstruir el backend. Flyway
   aplicara las migraciones desde cero y el bootstrap creara el gerente porque
   `usuarios` estara vacia:

```bash
cd ~/apps/kontora
docker compose --env-file infra/.env -f infra/compose.prod.yml up -d postgres
docker compose --env-file infra/.env -f infra/compose.prod.yml up -d --build --force-recreate backend
docker compose --env-file infra/.env -f infra/compose.prod.yml logs --tail=150 backend | grep -Ei "Started Kontora|provision|gerente"
curl -i http://127.0.0.1:8080/api/health
```

La salida debe indicar que se provisiono el gerente inicial y mencionar `gerenteLocal`; el health debe responder `HTTP/1.1 200`.

6. Verificar usuario, rol y credencial dentro de PostgreSQL:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml exec postgres \
  sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "
  SELECT u.nombre_usuario, u.estado, r.nombre_rol, c.estado
  FROM usuarios u
  JOIN roles r ON r.id_rol = u.id_rol
  JOIN credenciales_usuario c ON c.id_usuario = u.id_usuario
  WHERE u.nombre_usuario = '\''gerenteLocal'\'';
  "'
```

7. Iniciar sesion en la aplicacion con las credenciales configuradas. La
   contrasena no puede consultarse en PostgreSQL porque se almacena como hash
   BCrypt.
8. Desactivar el bootstrap para impedir que un futuro reinicio cree una cuenta nueva si alguna vez se vacia la tabla `usuarios`:

```bash
cd ~/apps/kontora
nano infra/.env
```

Cambiar solamente esta linea y guardar:

```env
BOOTSTRAP_MANAGER_ENABLED=false
```

9. Recrear solo el backend para que lea la nueva variable y validar su salud:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml up -d --force-recreate backend
curl -i http://127.0.0.1:8080/api/health
```

El bootstrap queda desactivado y el gerente ya creado conserva sus credenciales
y permisos. El volumen anterior no se borra automaticamente: conservarlo como
reversion hasta verificar el nuevo entorno y retirarlo solo durante una
operacion de limpieza aprobada.

## Validacion realizada

El 2026-07-12 se valido el bootstrap en contenedores aislados con una base PostgreSQL vacia. El backend creo un unico usuario `gerenteLocal`, con rol `gerente`, usuario y credencial activos, y el login respondio correctamente.
