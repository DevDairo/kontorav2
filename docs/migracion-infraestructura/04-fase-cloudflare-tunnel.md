# Fase 4: Cloudflare Tunnel

## Resultado

Kontora publica un unico origen mediante un tunnel administrado remotamente:

```text
Internet
  -> https://kontora-pos.store
  -> Cloudflare Tunnel
  -> cloudflared: red Docker tunnel
  -> http://frontend:8080
  -> Nginx
       |-- React SPA
       +-- /api -> backend:8080
```

PostgreSQL, Storage y Spring Boot no se publican directamente. El navegador
usa el mismo hostname para la interfaz y `/api`.

## Direcciones

| Uso | Direccion |
| --- | --- |
| Navegador en el equipo local | `http://127.0.0.1:8081/login` |
| Origen interno del tunnel | `http://frontend:8080` |
| Navegador por Internet | `https://kontora-pos.store/login` |

`127.0.0.1:8081` no es temporal: queda disponible en la maquina que ejecuta
Docker. `frontend:8080` solo existe dentro de las redes Docker y nunca se
entrega al usuario.

## Reglas de seguridad

- Crear un tunnel nombrado y administrado remotamente, no un Quick Tunnel.
- Usar un solo tunnel y un solo registro DNS para el hostname.
- Guardar únicamente el token en `infra/.env`.
- No pegar el token en comandos, capturas, tickets o documentacion.
- Si el token se expone, rotarlo y desconectar las conexiones anteriores.
- No ejecutar `docker compose config` sin `--quiet`: la salida completa puede
  contener secretos resueltos.
- No publicar `5432`, `5000`, `8080` o `8081` hacia Internet.
- Autorizar en CORS el origen HTTPS exacto, sin ruta, comodin o barra final.

## Creacion limpia del tunnel

Este orden fue validado con la variante del panel que exige una replica
conectada antes de habilitar **Published application**.

### 1. Comprobar la aplicacion local

Desde la raiz del proyecto:

```powershell
docker compose --env-file infra\.env `
    -f infra\compose.local.yml `
    config --quiet

docker compose --env-file infra\.env `
    -f infra\compose.local.yml `
    ps

Invoke-WebRequest -UseBasicParsing `
    http://127.0.0.1:8081/healthz |
    Select-Object StatusCode, Content

Invoke-WebRequest -UseBasicParsing `
    http://127.0.0.1:8081/api/health |
    Select-Object StatusCode, Content
```

Los dos health deben responder `200`. No continuar si frontend, backend,
PostgreSQL o Storage no estan activos.

### 2. Crear el tunnel sin ruta

En Cloudflare:

1. Abrir **Networking > Tunnels**.
2. Seleccionar **Create tunnel**.
3. Elegir **Cloudflared**.
4. Asignar un nombre unico, por ejemplo `kontora-pos`.
5. Guardar.
6. Elegir **Docker** en las instrucciones del conector.
7. Copiar temporalmente el comando a un editor privado.
8. Extraer solo la cadena que aparece despues de `--token`.

La variable contiene únicamente el JWT:

```env
CLOUDFLARED_IMAGE=cloudflare/cloudflared:2026.7.2
CLOUDFLARE_TUNNEL_TOKEN=<token>
```

No debe contener `docker run`, el nombre de la imagen, `tunnel run` ni
`--token`.

### 3. Iniciar la replica

Guardar esas dos variables en `infra/.env` y ejecutar:

```powershell
docker compose --env-file infra\.env `
    -f infra\compose.local.yml `
    --profile tunnel `
    config --quiet

docker compose --env-file infra\.env `
    -f infra\compose.local.yml `
    --profile tunnel `
    up -d --no-deps cloudflared

Start-Sleep -Seconds 15

docker compose --env-file infra\.env `
    -f infra\compose.local.yml `
    --profile tunnel `
    ps cloudflared

docker compose --env-file infra\.env `
    -f infra\compose.local.yml `
    --profile tunnel `
    logs --tail=120 cloudflared
```

El panel debe mostrar una replica `Healthy`. Los logs deben mostrar conexiones
registradas y no errores de token, DNS o puerto `7844`.

### 4. Crear Published application

Dentro del mismo tunnel:

1. Abrir **Routes**.
2. Seleccionar **Add route > Published application**.
3. Elegir el hostname. Para este proyecto:

```text
Hostname: kontora-pos.store
Type: HTTP
Service URL: http://frontend:8080
```

4. Guardar y permitir que Cloudflare cree el CNAME administrado.
5. En DNS, comprobar que no sobreviven registros A o CNAME de Vercel para el
   mismo hostname.
6. Confirmar que el target contiene el UUID del tunnel actual:
   `<UUID>.cfargotunnel.com`.

No usar `http://localhost:8081`: dentro de `cloudflared`, `localhost` identifica
al propio contenedor.

### 5. Confirmar la red Docker

Los dos contenedores deben compartir la red `infra_tunnel`:

```powershell
$frontend = @(docker inspect kontora_pos_frontend_local | ConvertFrom-Json)[0]
$tunnel = @(docker inspect kontora_pos_cloudflared_local | ConvertFrom-Json)[0]

$frontendNetworks = @(
    $frontend.NetworkSettings.Networks.PSObject.Properties.Name
)
$tunnelNetworks = @(
    $tunnel.NetworkSettings.Networks.PSObject.Properties.Name
)

[PSCustomObject]@{
    Frontend = $frontendNetworks -join ', '
    Cloudflared = $tunnelNetworks -join ', '
    CompartenTunnel = (
        $frontendNetworks -contains 'infra_tunnel' -and
        $tunnelNetworks -contains 'infra_tunnel'
    )
} | Format-List
```

Si `CompartenTunnel=False`, el frontend fue creado antes de que Compose
incluyera la red. Recrear exclusivamente frontend:

```powershell
docker compose --env-file infra\.env `
    -f infra\compose.local.yml `
    up -d --no-deps --no-build --force-recreate frontend
```

No recrear PostgreSQL, Storage, backend o cloudflared para corregir este punto.

### 6. Aplicar CORS

En `infra/.env`, conservar los origenes locales y agregar el hostname exacto:

```env
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173,http://localhost:8081,http://127.0.0.1:8081,https://kontora-pos.store
```

Recrear solo backend:

```powershell
docker compose --env-file infra\.env `
    -f infra\compose.local.yml `
    up -d --no-deps --no-build --force-recreate backend

Start-Sleep -Seconds 20

docker compose --env-file infra\.env `
    -f infra\compose.local.yml `
    ps backend
```

No es necesario reconstruir la imagen: Spring carga CORS al iniciar.

### 7. Validar desde Internet

```powershell
$publicOrigin = 'https://kontora-pos.store'

$frontendHealth = Invoke-WebRequest -UseBasicParsing `
    "$publicOrigin/healthz"

$apiHealth = Invoke-WebRequest -UseBasicParsing `
    "$publicOrigin/api/health"

$preflight = Invoke-WebRequest -UseBasicParsing `
    -Method Options `
    -Uri "$publicOrigin/api/auth/login" `
    -Headers @{
        Origin = $publicOrigin
        'Access-Control-Request-Method' = 'POST'
        'Access-Control-Request-Headers' = 'content-type'
    }

[PSCustomObject]@{
    Frontend = $frontendHealth.StatusCode
    Api = $apiHealth.StatusCode
    Cors = $preflight.StatusCode
    Origen = $preflight.Headers['Access-Control-Allow-Origin']
} | Format-List
```

Los estados deben ser `200` y `Origen` debe coincidir exactamente con el
hostname HTTPS. Despues:

1. Abrir `https://kontora-pos.store/login`.
2. Recargar con `Ctrl+F5` si el dominio se uso antes con Vercel.
3. Iniciar sesion.
4. Recargar directamente una ruta SPA como `/inventario`.
5. Validar una vista previa y una descarga de evidencia.
6. Confirmar que ninguna solicitud usa `localhost`, `127.0.0.1` o Storage.

`ERR_BLOCKED_BY_CLIENT` para
`static.cloudflareinsights.com/beacon.min.js` corresponde a telemetria opcional
de Cloudflare bloqueada por el navegador; no es un error de Kontora.

## Detener la demostracion

```powershell
docker compose --env-file infra\.env `
    -f infra\compose.local.yml `
    --profile tunnel `
    stop cloudflared

docker compose --env-file infra\.env `
    -f infra\compose.local.yml `
    --profile tunnel `
    rm -f cloudflared
```

Esto no elimina los datos. No usar `down -v`.

Al terminar:

1. Deshabilitar la cuenta temporal de revision.
2. Rotar el token antes de instalar el conector en el VPS.
3. Retirar el token anterior de `infra/.env`.
4. Mantener desactivado el bootstrap si ya existen usuarios.

## Migrar el conector al VPS

No dejar el equipo local y el VPS conectados simultaneamente al mismo tunnel si
usan bases distintas: Cloudflare puede repartir solicitudes entre replicas.

Orden:

1. Respaldar y restaurar PostgreSQL y Storage en el VPS, o inicializar una
   instalacion nueva.
2. Validar localmente en el VPS `/healthz` y `/api/health`.
3. Detener `cloudflared` en el equipo local.
4. Rotar el token en Cloudflare.
5. Guardar el token nuevo solo en `infra/.env` del VPS.
6. Mantener la ruta `http://frontend:8080`.
7. Iniciar en el VPS:

```bash
docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  --profile tunnel \
  up -d --no-deps cloudflared

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  --profile tunnel \
  logs --tail=120 cloudflared
```

8. Repetir las validaciones HTTPS y el login.

La instalacion completa del servidor esta en
[05-despliegue-vps.md](./05-despliegue-vps.md).

## Errores frecuentes

| Sintoma | Causa probable | Correccion |
| --- | --- | --- |
| `1033` | El hostname apunta a un tunnel sin replica activa o a un UUID anterior. | Dejar un solo tunnel, iniciar su replica y verificar que el CNAME use su UUID. |
| `502` y `lookup frontend ... no such host` | `cloudflared` y frontend no comparten la red `tunnel`. | Recrear solo frontend y confirmar `CompartenTunnel=True`. |
| `502` y `connection refused` | Service URL o puerto interno incorrecto. | Usar `http://frontend:8080`; validar Nginx `healthy`. |
| `403` en login o preflight | El hostname exacto no esta en CORS o backend no fue recreado. | Corregir `CORS_ALLOWED_ORIGINS` y recrear solo backend. |
| `DEPLOYMENT_NOT_FOUND` de Vercel | DNS o cache todavia dirige el hostname a Vercel. | Retirar registros anteriores, crear la ruta del tunnel y recargar DNS/cache. |
| Token invalido | La variable contiene el comando Docker completo o un token rotado. | Guardar solo `eyJ...` y recrear cloudflared. |
| Fallo en `7844` | Firewall o proveedor bloquea QUIC/HTTP2 saliente. | Permitir salida UDP y TCP por `7844`; revisar los prechecks de cloudflared. |
| Pull falla por DNS | Docker no resuelve Docker Hub o CloudFront. | Verificar DNS del host y del daemon; reiniciar Docker y repetir el pull. |

## Referencias oficiales

- [Crear un tunnel administrado remotamente](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/get-started/create-remote-tunnel/)
- [Aplicaciones publicadas](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/routing-to-tunnel/)
- [Tokens y rotacion](https://developers.cloudflare.com/tunnel/advanced/tunnel-tokens/)
- [Puertos de salida](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/configure-tunnels/tunnel-with-firewall/)
- [Diagnostico del tunnel](https://developers.cloudflare.com/tunnel/troubleshooting/)

## Cierre validado

- Tunnel nombrado y replica `Healthy`.
- DNS administrado por Cloudflare.
- `cloudflared` y frontend comparten `infra_tunnel`.
- Nginx es el unico origen publicado.
- CORS exacto aplicado.
- `/healthz`, `/api/health`, preflight y login publicos respondieron
  correctamente.
- El procedimiento conserva datos y separa la futura migracion al VPS.
