# Infraestructura

## Servicios

| Servicio | Responsabilidad |
| --- | --- |
| `postgres` | Datos de aplicacion y metadata de Storage |
| `storage-db-init` | Roles y esquema requeridos por Storage |
| `storage` | API de objetos con backend de archivos local |
| `storage-bucket-init` | Bucket privado `kontoraimagenes` |
| `backend` | API Spring Boot, seguridad, reglas y Flyway |
| `frontend` | React compilado y servido por Nginx |
| `cloudflared` | Publicacion opcional mediante el perfil `tunnel` |

Los inicializadores son transitorios y deben terminar con codigo `0`. Los
demas servicios usan volumenes o se pueden recrear sin mezclar sus ciclos de
vida.

## Archivos

- `compose.local.yml`: Windows/desarrollo, puertos enlazados a loopback.
- `compose.prod.yml`: VPS, PostgreSQL y Storage sin puertos publicados.
- `.env.example`: plantilla local.
- `.env.production.example`: plantilla del VPS.
- `scripts/New-StorageSecrets.ps1`: claves Storage desde Windows.
- `scripts/New-ProductionSecrets.mjs`: secretos completos mediante Node o su
  imagen Docker.
- `storage/`: inicializacion de roles y bucket.

Los valores reales viven exclusivamente en `infra/.env`, ignorado por Git.

## Local

```powershell
Copy-Item infra\.env.example infra\.env

powershell.exe -NoProfile -ExecutionPolicy Bypass `
    -File .\infra\scripts\New-StorageSecrets.ps1

docker compose --env-file infra\.env `
    -f infra\compose.local.yml `
    config --quiet

docker compose --env-file infra\.env `
    -f infra\compose.local.yml `
    up -d --build
```

## Produccion

```bash
cp infra/.env.production.example infra/.env
chmod 600 infra/.env

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  config --quiet

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  up -d --build postgres storage backend frontend
```

El despliegue completo no se reduce a ese comando: primero se preparan
secretos, respaldo/restauracion, gerente inicial y health; luego se conecta
Cloudflare.

## Cloudflare

`cloudflared` es opcional y solo aparece con el perfil `tunnel`:

```bash
docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  --profile tunnel \
  up -d --no-deps cloudflared
```

La aplicacion publicada usa `http://frontend:8080`, no `localhost`. Frontend y
cloudflared comparten exclusivamente la red `tunnel`.

## Guias

- [Separacion PostgreSQL/backend](../docs/migracion-infraestructura/01-fase-postgresql-backend.md)
- [Storage autoalojado](../docs/migracion-infraestructura/02-fase-supabase-storage-local.md)
- [Frontend Nginx](../docs/migracion-infraestructura/03-fase-frontend-nginx.md)
- [Cloudflare Tunnel](../docs/migracion-infraestructura/04-fase-cloudflare-tunnel.md)
- [Despliegue VPS](../docs/migracion-infraestructura/05-despliegue-vps.md)
- [Repositorio Git nuevo](../docs/migracion-infraestructura/06-repositorio-git-nuevo.md)

No usar `docker compose down -v` si se deben conservar datos.
