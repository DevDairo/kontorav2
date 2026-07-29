# Reinicio total local de PostgreSQL y Storage

## Objetivo

Este procedimiento elimina todos los datos locales de Kontora POS y reconstruye
el sistema desde cero con:

- una base PostgreSQL nueva, creada por Flyway;
- el esquema de Supabase Storage nuevo;
- el bucket privado `kontoraimagenes` vacío;
- un único gerente inicial;
- backend, frontend y Cloudflare Tunnel funcionando nuevamente.

El reinicio elimina usuarios, credenciales, ventas, cajas, inventario,
auditoría, metadatos de evidencias y archivos almacenados.

> **Advertencia:** este proceso es irreversible cuando se ejecuta sin respaldo.
> Está diseñado para Windows local con `infra/compose.local.yml`. No se debe
> copiar tal cual en un VPS de producción.

## Alcance y valores predeterminados

Ejecutar todos los comandos desde:

```powershell
C:\Users\corre\Documentos\kontora
```

Los valores predeterminados del entorno local son:

| Recurso | Valor |
| --- | --- |
| Base | `kontora_pos` |
| Usuario PostgreSQL | `kontora_pos` |
| Volumen PostgreSQL | `kontora_pos_postgres_local_data` |
| Volumen de archivos Storage | `kontora_pos_storage_local_data` |
| Bucket privado | `kontoraimagenes` |
| Archivo Compose | `infra\compose.local.yml` |
| Variables del entorno | `infra\.env` |

Si `infra/.env` contiene nombres diferentes, se deben sustituir en todos los
comandos de inspección y eliminación.

## Reglas obligatorias

1. Ejecutar los comandos en el orden indicado y uno por uno.
2. Detenerse ante el primer error.
3. No compartir contraseñas, tokens ni el contenido completo de `infra/.env`.
4. Confirmar los nombres de los volúmenes antes de eliminarlos.
5. Reiniciar PostgreSQL y Storage juntos. PostgreSQL contiene la metadata de
   Storage, mientras los archivos viven en otro volumen.
6. No usar `docker compose down -v`.
7. No usar `docker volume prune`.
8. No regenerar `STORAGE_JWT_SECRET` ni `STORAGE_SERVICE_ROLE_KEY` durante el
   reinicio.
9. `storage-db-init` y `storage-bucket-init` son servicios transitorios:
   `Exited (0)` significa que terminaron correctamente.

Si existe información que se deba conservar, detener este procedimiento y
crear primero un respaldo coordinado de PostgreSQL y Storage. Para el flujo de
respaldos operativos, consultar
[`docs/panel-operaciones/README.md`](../panel-operaciones/README.md).

---

## Fase 1. Comprobar el entorno

Abrir Docker Desktop y entrar al repositorio:

```powershell
cd C:\Users\corre\Documentos\kontora
```

Comprobar Docker:

```powershell
docker info
```

Validar Compose sin imprimir secretos:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml config --quiet
```

Revisar el estado actual:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel ps -a
```

Confirmar los nombres configurados:

```powershell
Select-String -Path infra\.env -Pattern '^(POSTGRES_VOLUME_NAME|STORAGE_VOLUME_NAME|DB_NAME|DB_USER|SUPABASE_STORAGE_BUCKET)='
```

Inspeccionar los dos volúmenes que se eliminarán:

```powershell
docker volume inspect kontora_pos_postgres_local_data
```

```powershell
docker volume inspect kontora_pos_storage_local_data
```

Preparar las imágenes antes de borrar datos:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml pull postgres storage
```

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml build backend frontend
```

### Criterio de cierre

- Todos los comandos terminan con código `0`.
- Los dos volúmenes existen y coinciden con `infra/.env`.
- Las imágenes de PostgreSQL y Storage están disponibles.
- Backend y frontend se construyen correctamente.

No continuar si algún punto falla.

---

## Fase 2. Preparar el gerente inicial

El backend crea el gerente únicamente cuando la tabla `usuarios` está vacía y
el bootstrap está activado.

Abrir el archivo:

```powershell
notepad infra\.env
```

Configurar:

```env
BOOTSTRAP_MANAGER_ENABLED=true
BOOTSTRAP_MANAGER_USERNAME=gerenteLocal
BOOTSTRAP_MANAGER_FULL_NAME=Gerente Local
BOOTSTRAP_MANAGER_PASSWORD=<CONTRASEÑA-TEMPORAL-SEGURA>
```

La contraseña debe tener entre 8 y 72 caracteres. No se debe guardar en Git,
documentación, scripts SQL ni conversaciones.

No modificar durante este proceso:

- `DB_PASSWORD`
- `STORAGE_DATABASE_URL`
- `STORAGE_JWT_SECRET`
- `STORAGE_SERVICE_ROLE_KEY`

Validar Compose:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml config --quiet
```

Confirmar únicamente las variables no secretas:

```powershell
Select-String -Path infra\.env -Pattern '^(BOOTSTRAP_MANAGER_ENABLED|BOOTSTRAP_MANAGER_USERNAME|BOOTSTRAP_MANAGER_FULL_NAME)='
```

### Criterio de cierre

- `BOOTSTRAP_MANAGER_ENABLED=true`.
- El usuario y nombre completo son correctos.
- `docker compose ... config --quiet` termina con código `0`.

---

## Fase 3. Eliminar PostgreSQL y Storage

> **Punto irreversible:** los comandos `docker volume rm` eliminan
> definitivamente la base, la metadata de Storage y los archivos del bucket.

Detener el stack sin eliminar automáticamente los volúmenes:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel down
```

Confirmar que ningún contenedor continúa usando PostgreSQL:

```powershell
docker ps -a --filter volume=kontora_pos_postgres_local_data
```

Confirmar que ningún contenedor continúa usando Storage:

```powershell
docker ps -a --filter volume=kontora_pos_storage_local_data
```

Los dos comandos anteriores deben mostrar solamente el encabezado.

Inspeccionar una última vez los objetivos:

```powershell
docker volume inspect kontora_pos_postgres_local_data
```

```powershell
docker volume inspect kontora_pos_storage_local_data
```

Eliminar únicamente el volumen PostgreSQL confirmado:

```powershell
docker volume rm kontora_pos_postgres_local_data
```

Eliminar únicamente el volumen de archivos Storage confirmado:

```powershell
docker volume rm kontora_pos_storage_local_data
```

Confirmar que desaparecieron:

```powershell
docker volume ls --filter name=kontora_pos_postgres_local_data
```

```powershell
docker volume ls --filter name=kontora_pos_storage_local_data
```

### Criterio de cierre

- Ambos comandos `docker volume rm` muestran el nombre eliminado.
- Los dos listados finales muestran solamente el encabezado.

---

## Fase 4. Recrear PostgreSQL, Storage y el bucket

Iniciar PostgreSQL:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml up -d postgres
```

Comprobar su estado:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml ps postgres
```

Si aparece `health: starting`, repetir el comando anterior hasta que PostgreSQL
muestre `healthy`. No iniciar Storage antes de ese momento.

Iniciar Storage y esperar sus comprobaciones:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --wait --wait-timeout 120 storage
```

Revisar los componentes creados:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml ps -a
```

El estado requerido es:

- `postgres`: `healthy`;
- `storage`: `healthy`;
- `storage-db-init`: `Exited (0)`.

Crear el bucket sin volver a ejecutar dependencias:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml run --rm --no-deps storage-bucket-init
```

La salida esperada es:

```text
Bucket privado 'kontoraimagenes' preparado correctamente.
```

No sustituir este comando por `up storage-bucket-init`. Después de iniciar
Storage, `up` puede volver a ejecutar `storage-db-init` mientras Storage
modifica el esquema y provocar:

```text
psql:/opt/kontora/init-storage-roles.sql:24:
ERROR: tuple concurrently updated
```

### Recuperación de `tuple concurrently updated`

No volver a borrar los volúmenes. Revisar:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml logs --no-color --tail=200 storage-db-init
```

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml ps -a
```

Si PostgreSQL y Storage están `healthy` y una primera ejecución de
`storage-db-init` terminó correctamente, retirar solamente los
inicializadores:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml rm -f storage-db-init storage-bucket-init
```

Crear nuevamente el bucket sin dependencias:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml run --rm --no-deps storage-bucket-init
```

### Criterio de cierre

- PostgreSQL y Storage están `healthy`.
- `storage-db-init` terminó con `Exited (0)`.
- El comando del bucket terminó con código `0` y mostró el mensaje esperado.

---

## Fase 5. Iniciar backend y frontend

Iniciar únicamente el backend. `--no-deps` evita relanzar los inicializadores
de Storage:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --no-deps --no-build backend
```

Revisar su estado:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml ps backend
```

Revisar Flyway, el arranque y el bootstrap:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml logs --no-color --tail=100 backend
```

Los logs deben mostrar:

- las migraciones aplicadas correctamente;
- `Started KontoraPosApplication`;
- la creación del gerente inicial.

No deben mostrar `Migration failed`, `Application run failed` ni errores de
arranque.

Comprobar la API:

```powershell
curl.exe --fail http://127.0.0.1:8080/api/health
```

La salida esperada es:

```json
{"status":"ok","service":"kontora-pos-backend"}
```

Un `Empty reply from server` inmediatamente después de recrear el backend puede
indicar que todavía está arrancando. Confirmar que el contenedor siga `Up`,
revisar sus logs y repetir la petición solamente cuando aparezca
`Started KontoraPosApplication`.

Iniciar el frontend:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --no-deps --no-build frontend
```

Revisar el stack:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml ps -a
```

Si el frontend muestra `health: starting`, repetir la consulta hasta que
aparezca `healthy`.

Comprobar Nginx:

```powershell
curl.exe --fail http://127.0.0.1:8081/healthz
```

Comprobar el proxy hacia el backend:

```powershell
curl.exe --fail http://127.0.0.1:8081/api/health
```

### Criterio de cierre

- El backend está `Up`.
- El frontend está `healthy`.
- `/healthz` responde `ok`.
- `/api/health` responde con el estado del backend.

Todavía no iniciar Cloudflare Tunnel.

---

## Fase 6. Validar la base, el bucket y el gerente

### 6.1. Migraciones Flyway

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres psql -X -U kontora_pos -d kontora_pos -c "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

Todas las migraciones deben mostrar `success = t`.

### 6.2. Bucket privado

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres psql -X --csv -U kontora_pos -d kontora_pos -c "SELECT id, name, public, file_size_limit, allowed_mime_types FROM storage.buckets;"
```

Debe existir solamente `kontoraimagenes` y `public` debe ser `false`.

### 6.3. Bucket vacío

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres psql -X -A -t -U kontora_pos -d kontora_pos -c "SELECT count(*) FROM storage.objects;"
```

El resultado debe ser:

```text
0
```

### 6.4. Gerente inicial

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml exec -T postgres psql -X -U kontora_pos -d kontora_pos -c "SELECT u.nombre_usuario, u.estado AS estado_usuario, r.nombre_rol, c.estado AS estado_credencial FROM usuarios u JOIN roles r ON r.id_rol = u.id_rol JOIN credenciales_usuario c ON c.id_usuario = u.id_usuario;"
```

Debe existir exactamente un usuario con:

- nombre `gerenteLocal`;
- rol `gerente`;
- usuario `activo`;
- credencial `activa`.

### 6.5. Primer inicio de sesión

Abrir:

```text
http://127.0.0.1:8081/login
```

Iniciar sesión con el gerente configurado. Abrir la pantalla de login no es
suficiente: se debe ingresar al panel principal.

### Criterio de cierre

- Flyway no tiene migraciones fallidas.
- El bucket es privado y contiene cero objetos.
- Existe solamente el gerente inicial esperado.
- El inicio de sesión funciona.

---

## Fase 7. Desactivar el bootstrap

Después de comprobar el primer inicio de sesión, abrir:

```powershell
notepad infra\.env
```

Cambiar únicamente:

```env
BOOTSTRAP_MANAGER_ENABLED=false
BOOTSTRAP_MANAGER_PASSWORD=
```

Confirmar sin imprimir secretos:

```powershell
Select-String -Path infra\.env -Pattern '^(BOOTSTRAP_MANAGER_ENABLED=false|BOOTSTRAP_MANAGER_PASSWORD=)$'
```

Validar Compose:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml config --quiet
```

Recrear solamente el backend:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --no-deps --no-build --force-recreate backend
```

Comprobar la API:

```powershell
curl.exe --fail http://127.0.0.1:8080/api/health
```

Cerrar la sesión de la aplicación e iniciar nuevamente con `gerenteLocal`. La
cuenta debe continuar funcionando aunque el bootstrap esté desactivado.

### Criterio de cierre

- El bootstrap está desactivado.
- La contraseña temporal quedó vacía en `infra/.env`.
- El backend responde correctamente después de recrearse.
- El gerente puede cerrar sesión y volver a ingresar.

---

## Fase 8. Restablecer Cloudflare Tunnel

Iniciar únicamente el túnel:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel up -d --no-deps cloudflared
```

Revisar el stack completo:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel ps -a
```

Estado requerido:

- `postgres`: `healthy`;
- `storage`: `healthy`;
- `storage-db-init`: `Exited (0)`;
- `backend`: `Up`;
- `frontend`: `healthy`;
- `cloudflared`: `Up`.

Comprobar la URL pública:

```powershell
curl.exe --fail https://kontora-pos.store/healthz
```

```powershell
curl.exe --fail https://kontora-pos.store/api/health
```

Abrir:

```text
https://kontora-pos.store/login
```

Confirmar que el gerente puede iniciar sesión desde la URL pública.

---

## Lista de cierre

El reinicio se considera completo solamente cuando:

- [ ] PostgreSQL usa un volumen nuevo y está `healthy`.
- [ ] Storage usa un volumen nuevo y está `healthy`.
- [ ] `storage-db-init` terminó con `Exited (0)`.
- [ ] Todas las migraciones Flyway muestran `success = t`.
- [ ] Existe únicamente el bucket privado `kontoraimagenes`.
- [ ] `storage.objects` contiene `0` filas.
- [ ] Existe exactamente un gerente inicial activo.
- [ ] El acceso local funciona.
- [ ] `BOOTSTRAP_MANAGER_ENABLED=false`.
- [ ] `BOOTSTRAP_MANAGER_PASSWORD=` quedó vacío.
- [ ] El gerente conserva el acceso después de desactivar el bootstrap.
- [ ] Cloudflare Tunnel está `Up`.
- [ ] La URL pública y el inicio de sesión funcionan.

## Errores frecuentes

| Situación | Acción |
| --- | --- |
| `docker` no se reconoce | Abrir Docker Desktop y una terminal nueva antes de repetir la Fase 1. |
| `volume is in use` | No forzar la eliminación. Ejecutar `down` y consultar `docker ps -a --filter volume=NOMBRE`. |
| `tuple concurrently updated` | No borrar los volúmenes nuevos. Aplicar la recuperación descrita en la Fase 4. |
| `Empty reply from server` al recrear backend | Confirmar que el contenedor siga `Up`, esperar el mensaje de arranque en logs y repetir el `curl`. |
| Flyway falla | Revisar `docker compose ... logs --tail=200 backend`; no editar migraciones ya aplicadas. |
| El bucket no existe | Ejecutar `run --rm --no-deps storage-bucket-init`. |
| `storage.objects` no está vacío | Detenerse y confirmar que se usan los volúmenes nuevos y que ningún cliente escribió durante el reinicio. |
| No se crea el gerente | Confirmar tabla `usuarios` vacía y las cuatro variables `BOOTSTRAP_MANAGER_*`. |
| La contraseña no funciona | El bootstrap no actualiza usuarios existentes; confirmar que la base realmente se recreó. |

## Producción

Este documento no autoriza eliminar volúmenes de producción sin respaldo. En un
VPS se deben crear nombres nuevos para PostgreSQL y Storage, restaurar o
inicializar sobre esos recursos, validar localmente y mediante el túnel, y
conservar los volúmenes anteriores hasta comprobar una recuperación completa.
Los dos recursos siempre se gestionan como una pareja.
