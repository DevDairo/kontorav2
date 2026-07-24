# Fase 3: React + Vite servido por Nginx

## Resultado esperado

El frontend se compila dentro de Docker y el resultado estatico se copia a una
imagen Nginx independiente. El host no necesita Node.js, npm ni Nginx.

```text
Navegador -> 127.0.0.1:8081 -> Nginx
                                 |-- /, /login, /ventas... -> React SPA
                                 +-- /api/* -> backend:8080
```

El frontend se construye con `VITE_API_URL=/api`. El navegador usa un solo
origen y Nginx reenvia la API por la red Docker `edge`. PostgreSQL y
Storage no son accesibles desde el contenedor frontend.

Nginx usa el resolvedor DNS interno `127.0.0.11` y un upstream con
`backend:8080 resolve`. Si Docker reemplaza el backend y cambia su direccion
interna, Nginx actualiza el destino sin necesitar ser recreado.

Cloudflare Tunnel no forma parte de la imagen Nginx. En produccion Nginx
permanece enlazado a `127.0.0.1` para diagnostico y la Fase 4 publica
internamente `http://frontend:8080`.

## Archivos

- `frontend/Dockerfile`: compilacion multietapa con Node y runtime Nginx.
- `frontend/.dockerignore`: excluye entornos, dependencias y builds locales.
- `frontend/nginx.conf`: SPA, proxy de API, healthcheck y limites de carga.
- `infra/compose.local.yml`: servicio `frontend` y red `edge` en desarrollo.
- `infra/compose.prod.yml`: servicio `frontend` limitado a loopback en la VM.

Se fijan las imagenes oficiales `node:22.23.1-alpine3.24` y
`nginx:1.30.4-alpine3.24`. La etapa Node se descarta al terminar el build. La
imagen final se ejecuta como el usuario no privilegiado `nginx` y escucha el
puerto interno `8080`.

## Configuracion

Agregar en `infra/.env`:

```env
FRONTEND_BIND_ADDRESS=127.0.0.1
FRONTEND_PORT=8081
FRONTEND_API_URL=/api
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173,http://localhost:8081,http://127.0.0.1:8081
```

`FRONTEND_API_URL` es una variable de compilacion publica, no un secreto. No se
deben agregar claves JWT, claves de Storage ni credenciales de base al build del
frontend.

El navegador usa un unico origen, pero Nginx conserva la cabecera `Origin` al
reenviar la solicitud y el filtro CORS de Spring valida su valor. Por eso el
entorno local debe incluir los dos nombres de Nginx. En produccion se sustituyen
por los hostnames HTTPS exactos publicados mediante Cloudflare; nunca se usa
`*`.

Nginx limita las solicitudes a `14m`. Spring Boot admite solicitudes multipart
de hasta 13 MB; el margen adicional cubre los encabezados del formulario sin
ampliar el limite real aceptado por el backend.

## Paso 1: validar Compose sin crear contenedores

Desde la raiz del proyecto:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml config --quiet
docker compose --env-file infra\.env -f infra\compose.local.yml config --services
```

La lista esperada es:

```text
postgres
storage-db-init
storage
storage-bucket-init
backend
frontend
```

No compartir `docker compose config` completo porque expande los secretos.

## Paso 2: construir solo el frontend

Registrar primero los servicios que no deben reemplazarse:

```powershell
$postgresAntes = docker inspect --format '{{.Id}}|{{.State.StartedAt}}' kontora_pos_postgres_local
$storageAntes = docker inspect --format '{{.Id}}|{{.State.StartedAt}}' kontora_pos_storage_local
$backendAntes = docker inspect --format '{{.Id}}|{{.State.StartedAt}}' kontora_pos_backend_local

docker compose --env-file infra\.env -f infra\compose.local.yml build --pull frontend
```

El build debe ejecutar `npm ci`, TypeScript y Vite dentro de la etapa Node y
terminar sin instalar dependencias en Windows.

La primera incorporacion de `frontend` puede requerir recrear backend para
conectarlo a la nueva red `edge`. Esto es un cambio deliberado de red, no una
dependencia entre sus ciclos de vida. PostgreSQL y Storage no se recrean.

## Paso 3: iniciar Nginx

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml up -d frontend
Start-Sleep -Seconds 15
docker compose --env-file infra\.env -f infra\compose.local.yml ps
docker compose --env-file infra\.env -f infra\compose.local.yml logs --tail=100 frontend
```

`kontora_pos_frontend_local` debe quedar `healthy` y publicar solamente
`127.0.0.1:8081`.

## Paso 4: validar Nginx, SPA y API

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml exec frontend nginx -t
docker compose --env-file infra\.env -f infra\compose.local.yml exec frontend id

$nginxHealth = Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8081/healthz
$rootResponse = Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8081/
$deepResponse = Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8081/ventas
$apiResponse = Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8081/api/health

[PSCustomObject]@{
    NginxHealth = $nginxHealth.StatusCode
    Root = $rootResponse.StatusCode
    RutaSPA = $deepResponse.StatusCode
    MismoIndex = ($rootResponse.Content -eq $deepResponse.Content)
    ApiProxy = $apiResponse.StatusCode
    ApiContent = $apiResponse.Content
} | Format-List
```

Resultados requeridos:

- `nginx -t` exitoso;
- usuario efectivo `nginx`;
- health, raiz, ruta SPA y API con `200`;
- `MismoIndex=True`;
- API con `{"status":"ok","service":"kontora-pos-backend"}`.

Verificar que el build no contiene una URL local fija:

```powershell
$forbiddenPattern = 'localhost:8080|127\.0\.0\.1:8080'

$forbiddenMatches = docker compose --env-file infra\.env `
    -f infra\compose.local.yml exec -T frontend `
    grep -R -E -- $forbiddenPattern /usr/share/nginx/html 2>$null

$grepExitCode = $LASTEXITCODE

if ($grepExitCode -eq 0) {
    $forbiddenMatches
    throw "El build contiene una URL local no permitida"
}

if ($grepExitCode -ne 1) {
    throw "La inspeccion del build termino con codigo inesperado $grepExitCode"
}

"BuildSinUrlsLocales=True"
```

El codigo `1` de `grep` significa que no encontro coincidencias y es el
resultado esperado.

Verificar una respuesta y sus cabeceras:

```powershell
$rootResponse.Headers |
    Format-List 'Content-Type', 'X-Content-Type-Options', 'X-Frame-Options', 'Referrer-Policy'
```

## Paso 5: comprobar independencia

```powershell
$postgresDespues = docker inspect --format '{{.Id}}|{{.State.StartedAt}}' kontora_pos_postgres_local
$storageDespues = docker inspect --format '{{.Id}}|{{.State.StartedAt}}' kontora_pos_storage_local
$backendDespues = docker inspect --format '{{.Id}}|{{.State.StartedAt}}' kontora_pos_backend_local

"PostgreSQL: $postgresAntes / $postgresDespues"
"Storage:    $storageAntes / $storageDespues"
"Backend:    $backendAntes / $backendDespues"
```

Los tres ID y `StartedAt` deben permanecer iguales. Reconstruir o reiniciar el
frontend no debe reemplazar datos, Storage ni backend.

Despues de validar el flujo normal, comprobar que Nginx renueva el destino
cuando backend es reemplazado. La funcion usa el JSON de `docker inspect`
porque PowerShell puede retirar las comillas internas de un template Go:

```powershell
function Get-ContainerNetworkIp {
    param(
        [Parameter(Mandatory)]
        [string]$ContainerName,

        [Parameter(Mandatory)]
        [string]$NetworkName
    )

    $inspectJson = docker inspect $ContainerName

    if ($LASTEXITCODE -ne 0) {
        throw "No se pudo inspeccionar $ContainerName"
    }

    $inspection = @($inspectJson | ConvertFrom-Json)[0]
    $network = $inspection.NetworkSettings.Networks.PSObject.Properties[$NetworkName].Value

    if ($null -eq $network -or [string]::IsNullOrWhiteSpace($network.IPAddress)) {
        throw "$ContainerName no tiene una IP en $NetworkName"
    }

    return $network.IPAddress
}

$guardName = "kontora_edge_dns_guard"
$frontendAntes = docker inspect --format '{{.Id}}|{{.State.StartedAt}}' kontora_pos_frontend_local
$backendAntes = docker inspect --format '{{.Id}}|{{.State.StartedAt}}' kontora_pos_backend_local
$backendIpAntes = Get-ContainerNetworkIp kontora_pos_backend_local infra_edge

docker compose --env-file infra\.env -f infra\compose.local.yml stop backend
docker compose --env-file infra\.env -f infra\compose.local.yml rm -f backend

docker run -d --name $guardName --network infra_edge --ip $backendIpAntes `
    nginx:1.30.4-alpine3.24 sleep 300

if ($LASTEXITCODE -ne 0) {
    docker compose --env-file infra\.env -f infra\compose.local.yml `
        up -d --no-build --no-deps backend
    throw "No se pudo reservar la IP anterior"
}

try {
    docker compose --env-file infra\.env -f infra\compose.local.yml `
        up -d --no-build --no-deps backend

    Start-Sleep -Seconds 20

    $backendIpDespues = Get-ContainerNetworkIp kontora_pos_backend_local infra_edge
    $directo = Invoke-WebRequest -UseBasicParsing -ErrorAction Stop `
        http://127.0.0.1:8080/api/health

    Start-Sleep -Seconds 12

    $proxy = Invoke-WebRequest -UseBasicParsing -ErrorAction Stop `
        http://127.0.0.1:8081/api/health
}
finally {
    docker rm -f $guardName 2>$null | Out-Null
}

$frontendDespues = docker inspect --format '{{.Id}}|{{.State.StartedAt}}' kontora_pos_frontend_local
$backendDespues = docker inspect --format '{{.Id}}|{{.State.StartedAt}}' kontora_pos_backend_local

[PSCustomObject]@{
    IpBackendAnterior  = $backendIpAntes
    IpBackendNueva     = $backendIpDespues
    IpCambio           = ($backendIpAntes -ne $backendIpDespues)
    BackendRecreado    = ($backendAntes -ne $backendDespues)
    FrontendConservado = ($frontendAntes -eq $frontendDespues)
    BackendDirecto     = $directo.StatusCode
    ProxyNginx         = $proxy.StatusCode
    ProxyContenido     = $proxy.Content
} | Format-List
```

Los resultados requeridos son `IpCambio=True`, `BackendRecreado=True`,
`FrontendConservado=True` y ambos estados HTTP en `200`. El `finally` elimina
el contenedor temporal aun si una solicitud falla.

## Reversion

El frontend no contiene datos persistentes. Para retirar solo Nginx:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml stop frontend
docker compose --env-file infra\.env -f infra\compose.local.yml rm -f frontend
```

Esto no elimina los volumenes de PostgreSQL ni Storage. No usar `down -v`.

## Cierre

La fase se cierra cuando build, health, fallback SPA, proxy `/api`, renovacion
DNS, cabeceras, usuario no privilegiado, aislamiento de los otros servicios y
representacion visual de `/login` esten validados y registrados en la bitacora.
