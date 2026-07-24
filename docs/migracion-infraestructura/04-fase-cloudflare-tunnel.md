# Cloudflare Tunnel

## Resultado

```text
Internet
  -> https://kontora-pos.store
  -> Cloudflare Tunnel
  -> cloudflared
  -> http://frontend:8080
  -> Nginx
       |-- React
       +-- /api -> backend:8080
```

PostgreSQL, Storage y backend no se publican directamente.

| Uso | Dirección |
| --- | --- |
| Acceso local | `http://127.0.0.1:8081/login` |
| Origen interno del tunnel | `http://frontend:8080` |
| Acceso público | `https://kontora-pos.store/login` |

## Reglas

- Crear un tunnel nombrado, no un Quick Tunnel.
- Usar un único tunnel para el hostname.
- Guardar solamente el token en `infra/.env`.
- No pegar el comando completo entregado por Cloudflare.
- No publicar los puertos `5432`, `5000`, `8080` o `8081` en Internet.
- Usar como origen `http://frontend:8080`, nunca `localhost`.
- Rotar el token antes de mover el tunnel al VPS.

## Orden exacto para crear el tunnel

### 1. Confirmar la aplicación local

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml config --quiet
docker compose --env-file infra\.env -f infra\compose.local.yml ps
curl.exe --fail http://127.0.0.1:8081/healthz
curl.exe --fail http://127.0.0.1:8081/api/health
```

No continuar si uno de los comandos falla.

### 2. Crear el tunnel

En Cloudflare:

1. Abrir **Networking > Tunnels**.
2. Seleccionar **Create tunnel**.
3. Elegir **Cloudflared**.
4. Usar el nombre `kontora-pos`.
5. Seleccionar Docker como tipo de conector.
6. Copiar únicamente el valor que aparece después de `--token`.

Guardar en `infra/.env`:

```env
CLOUDFLARED_IMAGE=cloudflare/cloudflared:2026.7.2
CLOUDFLARE_TUNNEL_TOKEN=<TOKEN>
```

El valor no debe contener `docker run`, la imagen, `tunnel run` ni `--token`.

### 3. Conectar cloudflared

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel up -d --no-deps cloudflared
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel ps cloudflared
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel logs --tail=100 cloudflared
```

Esperar hasta que Cloudflare muestre la réplica como `Healthy`. No crear la
ruta pública antes de ese estado.

### 4. Crear la ruta pública

Dentro del mismo tunnel:

1. Abrir **Routes**.
2. Seleccionar **Add route > Published application**.
3. Configurar:

```text
Hostname: kontora-pos.store
Type: HTTP
Service URL: http://frontend:8080
```

4. Guardar.
5. Dejar que Cloudflare cree el CNAME hacia
   `<UUID-DEL-TUNNEL>.cfargotunnel.com`.
6. Eliminar cualquier registro anterior de Vercel para el mismo hostname.

No crear manualmente un segundo CNAME.

### 5. Aplicar CORS

En el entorno local:

```env
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173,http://localhost:8081,http://127.0.0.1:8081,https://kontora-pos.store
```

Recrear únicamente backend:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --no-deps --no-build --force-recreate backend
```

### 6. Probar el dominio

```powershell
curl.exe --fail https://kontora-pos.store/healthz
curl.exe --fail https://kontora-pos.store/api/health
curl.exe --fail https://kontora-pos.store/login
```

Después probar en el navegador:

1. iniciar sesión;
2. recargar directamente `/inventario`;
3. registrar una operación controlada;
4. abrir y descargar una evidencia;
5. revisar la interfaz en un teléfono.

## Detener la demostración

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel stop cloudflared
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel rm -f cloudflared
```

Esto no elimina PostgreSQL, Storage ni sus volúmenes. No usar `down -v`.

## Mover el tunnel al VPS

Ejecutar en este orden:

1. preparar y validar la aplicación en el VPS;
2. detener cloudflared en el equipo local;
3. rotar el token en Cloudflare;
4. guardar el token nuevo en `infra/.env` del VPS;
5. conservar `http://frontend:8080` como Service URL;
6. iniciar cloudflared en el VPS:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml --profile tunnel up -d --no-deps cloudflared
docker compose --env-file infra/.env -f infra/compose.prod.yml --profile tunnel logs --tail=100 cloudflared
curl --fail https://kontora-pos.store/healthz
curl --fail https://kontora-pos.store/api/health
```

No mantener simultáneamente el equipo local y el VPS conectados al mismo
tunnel cuando usan bases de datos diferentes.

## Errores frecuentes

| Error | Corrección |
| --- | --- |
| `1033` | Iniciar la réplica correcta y confirmar que el CNAME usa el UUID del tunnel actual. |
| `502` con `lookup frontend` | Recrear frontend para conectarlo a la red `tunnel`. |
| `502` con `connection refused` | Corregir la Service URL a `http://frontend:8080`. |
| Login `403` | Agregar el origen HTTPS exacto a CORS y recrear backend. |
| `DEPLOYMENT_NOT_FOUND` | Eliminar el DNS anterior de Vercel y volver a cargar el dominio. |
| Token inválido | Guardar solo el token nuevo y recrear cloudflared. |
| Error del puerto `7844` | Permitir salida TCP y UDP por `7844`. |
| `ERR_BLOCKED_BY_CLIENT` de Cloudflare Insights | Es telemetría opcional bloqueada por el navegador; no afecta la aplicación. |

Si aparece `502` porque frontend no está en la red del tunnel:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --no-deps --no-build --force-recreate frontend
```

## Referencias oficiales

- [Crear un tunnel administrado remotamente](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/get-started/create-remote-tunnel/)
- [Aplicaciones publicadas](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/routing-to-tunnel/)
- [Tokens y rotación](https://developers.cloudflare.com/tunnel/advanced/tunnel-tokens/)
- [Puertos de salida](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/configure-tunnels/tunnel-with-firewall/)
- [Diagnóstico](https://developers.cloudflare.com/tunnel/troubleshooting/)
