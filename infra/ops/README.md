# Ejecución local del panel de operaciones

Esta fase expone únicamente consultas `GET` y `HEAD`. El proxy Docker bloquea
`POST` y solo permite leer contenedores, volúmenes, ping y versión.

## Preparar el entorno

Desde la raíz del repositorio:

Para una instalación nueva:

```powershell
Copy-Item infra\ops\.env.example infra\ops\.env
```

No ejecutar `Copy-Item` si `infra\ops\.env` ya existe, porque reemplazaría las
credenciales locales.

Generar la credencial de acceso:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\infra\ops\scripts\New-OpsToken.ps1
```

Copiar la línea generada a `infra\ops\.env`, sustituyendo
`OPS_LOCAL_TOKEN=`.

Generar una credencial diferente para el lector PostgreSQL:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\infra\ops\scripts\New-OpsToken.ps1 -Name OPS_DB_PASSWORD
```

Copiar la línea generada a `infra\ops\.env`, sustituyendo
`OPS_DB_PASSWORD=`. No reutilizar `DB_PASSWORD`, `OPS_LOCAL_TOKEN` ni una
credencial de producción.

Confirmar que `infra\ops\.env` contiene:

```env
OPS_POS_DATABASE_NETWORK=infra_database
OPS_DB_DIAGNOSTICS_ENABLED=true
OPS_DB_HOST=postgres
OPS_DB_PORT=5432
OPS_DB_NAME=kontora_pos
OPS_DB_USER=kontora_ops_reader
OPS_DB_PASSWORD=
OPS_DB_SSLMODE=disable
OPS_STORAGE_BUCKET=kontoraimagenes
OPS_AUDIT_VOLUME_NAME=kontora_ops_audit_local_data
OPS_AUDIT_DEFAULT_LIMIT=100
OPS_AUDIT_MAX_BYTES=52428800
```

La contraseña real queda únicamente después de `OPS_DB_PASSWORD=` en el
archivo ignorado por Git.

## Preparar el lector PostgreSQL

PostgreSQL, Storage y las migraciones Flyway deben estar operativos antes de
continuar.

Comprobar la red creada por el Compose principal:

```powershell
docker network inspect infra_database --format "{{.Name}}"
```

Debe responder exactamente `infra_database`. Si se usó `docker compose -p`,
configurar en `OPS_POS_DATABASE_NETWORK` el nombre real.

Crear o rotar el rol lector mediante el contenedor temporal:

```powershell
docker compose --env-file infra\ops\.env -f infra\ops\compose.local.yml --profile setup run --rm ops-db-init
```

El comando debe terminar sin `ERROR`. No continuar si falla. El servicio
temporal se elimina y el panel no recibe `DB_PASSWORD`. La ejecución también
crea las funciones cerradas del esquema `kontora_ops`, retira los permisos
directos sobre tablas y conserva `NOBYPASSRLS`.

## Validar y construir

Ejecutar un comando a la vez:

```powershell
docker compose --env-file infra\ops\.env -f infra\ops\compose.local.yml config --quiet
```

```powershell
docker compose --env-file infra\ops\.env -f infra\ops\compose.local.yml build ops-panel
```

```powershell
docker compose --env-file infra\ops\.env -f infra\ops\compose.local.yml up -d docker-api-proxy ops-panel
```

```powershell
docker compose --env-file infra\ops\.env -f infra\ops\compose.local.yml ps -a
```

```powershell
curl.exe --fail http://127.0.0.1:8090/healthz
```

```powershell
curl.exe --fail http://127.0.0.1:8090/api/health
```

Abrir `http://127.0.0.1:8090` e ingresar la credencial local. El navegador la
guarda solo en `sessionStorage`.

## Validar Flyway, bucket y evidencias

```powershell
docker compose --env-file infra\ops\.env -f infra\ops\compose.local.yml exec -T ops-panel node -e "fetch('http://127.0.0.1:8090/api/v1/diagnostics',{headers:{Authorization:'Bearer '+process.env.OPS_LOCAL_TOKEN}}).then(async r=>{const b=await r.json();console.log(r.status);console.log(JSON.stringify(b.databaseDiagnostics,null,2));process.exit(r.ok?0:1)})"
```

La aceptación exige:

- HTTP `200`;
- `database.reachable=true`;
- `summary.overall=operational`;
- Flyway con todas las migraciones exitosas;
- bucket presente y `public=false`;
- cero referencias inválidas, objetos faltantes y objetos sin referencia.

Con una base recién reiniciada es correcto obtener cero referencias y cero
objetos.

## Validar la bitácora persistente

La nueva imagen crea un volumen separado del POS:

```text
kontora_ops_audit_local_data
```

Después de construir y recrear `ops-panel`, consultar:

```powershell
docker compose --env-file infra\ops\.env -f infra\ops\compose.local.yml exec -T ops-panel node -e "fetch('http://127.0.0.1:8090/api/v1/audit?limit=100',{headers:{Authorization:'Bearer '+process.env.OPS_LOCAL_TOKEN}}).then(async r=>{console.log(r.status);console.log(JSON.stringify(await r.json(),null,2));process.exit(r.ok?0:1)})"
```

La aceptación exige HTTP `200`, almacenamiento `volume`, integridad
`verified`, un evento `panel.started` y al menos un
`diagnostics.snapshot`.

Para probar persistencia, recrear exclusivamente `ops-panel` y repetir la
consulta. Los eventos anteriores deben permanecer, el total debe aumentar y la
cadena debe seguir `verified`.

## Resolver DNS de Docker Desktop

Si una construcción falla con `lookup auth.docker.io: no such host`,
`lookup registry-1.docker.io: no such host` o un contenedor devuelve
`ENOTFOUND`, pero `nslookup` desde Windows sí responde, el problema está en el
DNS interno de Docker Desktop.

En **Docker Desktop > Settings > Docker Engine**, conservar el JSON existente y
agregar en el nivel principal:

```json
{
  "dns": [
    "1.1.1.1",
    "8.8.8.8"
  ]
}
```

No reemplazar otras propiedades. Aplicar con **Apply & restart**, esperar el
arranque de los contenedores y comprobar desde el panel:

```powershell
docker compose --env-file infra\ops\.env -f infra\ops\compose.local.yml exec -T ops-panel node -e "require('node:dns').promises.lookup('registry-1.docker.io',{all:true}).then(a=>console.log(a)).catch(e=>{console.error(e.code||e.message);process.exit(1)})"
```

No ejecutar `docker compose down` ni eliminar volúmenes para corregir este
problema.

## Comprobar la restricción de escritura

Este comando debe responder `403 Forbidden`:

```powershell
docker compose --env-file infra\ops\.env -f infra\ops\compose.local.yml exec -T ops-panel node -e "fetch('http://docker-api-proxy:2375/containers/kontora_ops_write_test_does_not_exist/stop',{method:'POST'}).then(async r=>{console.log(r.status);process.exit(r.status===403?0:1)})"
```

No continuar a la siguiente subfase si devuelve un estado diferente de `403`.
El identificador es deliberadamente inexistente para que una configuración
incorrecta tampoco pueda detener un servicio real.

## Detener solo el panel

```powershell
docker compose --env-file infra\ops\.env -f infra\ops\compose.local.yml down
```

Este comando no detiene ni elimina servicios, redes o volúmenes del POS.
Tampoco elimina el volumen de bitácora mientras no se agregue `--volumes` o
`-v`. No usar esas opciones salvo durante una restauración expresamente
planificada.

## Referencias de seguridad

- [Docker Socket Proxy y permisos por sección](https://github.com/Tecnativa/docker-socket-proxy)
- [Versión v0.4.2 utilizada](https://github.com/Tecnativa/docker-socket-proxy/releases/tag/v0.4.2)
- [Validación del JWT de Cloudflare Access](https://developers.cloudflare.com/cloudflare-one/access-controls/applications/http-apps/authorization-cookie/validating-json/)
- [Cliente PostgreSQL `pg` fijado en el panel](https://www.npmjs.com/package/pg)
- [Opciones DNS del daemon Docker](https://docs.docker.com/reference/cli/dockerd/#daemon-dns-options)
