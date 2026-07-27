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

Usa este caso solo en desarrollo local, cuando no se necesita conservar ninguna
venta, usuario, caja, evidencia ni dato de los volúmenes Docker. El reinicio
debe borrar de forma coordinada PostgreSQL y los archivos de Storage.

Seguir la
[variante Windows de la guía de reinicio total](reinicio-total-datos/README.md#variante-a-windows-local).

## Servidor de produccion

Antes de la primera ejecucion en el contenedor PostgreSQL vacio, definir las
mismas variables en `infra/.env`, iniciar PostgreSQL y despues el backend. Ver
la [Fase 1 de infraestructura](migracion-infraestructura/01-fase-postgresql-backend.md)
y el [README principal](../README.md#primer-gerente-en-una-instalacion-nueva).

En produccion no se debe borrar la base para cambiar credenciales. Usar siempre la gestion de usuarios por un gerente activo, conservar respaldos y mantener `BOOTSTRAP_MANAGER_ENABLED=false` despues del primer acceso.

## Caso 4: reiniciar por completo PostgreSQL de produccion

Este proceso debe reiniciar de forma coordinada la base, la metadata del bucket
y los archivos físicos. No se debe cambiar únicamente el volumen PostgreSQL,
porque los objetos anteriores quedarían inconsistentes.

Seguir la
[variante VPS de la guía de reinicio total](reinicio-total-datos/README.md#variante-b-vps-de-producción).

## Validacion realizada

El 2026-07-12 se valido el bootstrap en contenedores aislados con una base PostgreSQL vacia. El backend creo un unico usuario `gerenteLocal`, con rol `gerente`, usuario y credencial activos, y el login respondio correctamente.
