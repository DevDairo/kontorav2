# Despliegue desde cero en un VPS

## Alcance

Procedimiento para una instalacion nueva de Kontora POS en Ubuntu Server con:

- PostgreSQL 16;
- Supabase Storage API con archivos locales;
- backend Spring Boot;
- frontend React compilado y servido por Nginx;
- Cloudflare Tunnel como unico acceso publico.

Para migrar datos existentes, crear y verificar primero los respaldos descritos
en [Fase 1](./01-fase-postgresql-backend.md) y
[Fase 2](./02-fase-supabase-storage-local.md). No inicializar una base vacia si
se deben conservar operaciones.

## 1. Preparar Ubuntu

Recomendado: Ubuntu Server 24.04 LTS de 64 bits, acceso SSH con llave, dominio
delegado a Cloudflare y espacio suficiente para imagenes, base, evidencias y
respaldos.

```bash
sudo apt update
sudo apt upgrade -y
sudo apt install -y ca-certificates curl git
sudo timedatectl set-timezone America/Bogota
timedatectl status
```

En el firewall del proveedor:

- permitir entrada SSH solo desde direcciones administrativas cuando sea
  posible;
- no abrir `5432`, `5000`, `8080` ni `8081`;
- permitir salida HTTPS `443/TCP`;
- permitir salida `7844/TCP` y `7844/UDP` para cloudflared.

Docker advierte que los puertos publicados pueden omitir reglas de UFW. Kontora
reduce ese riesgo enlazando backend y Nginx exclusivamente a `127.0.0.1`; aun
asi, usar tambien el firewall del proveedor.

## 2. Instalar Docker desde el repositorio oficial

```bash
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

sudo tee /etc/apt/sources.list.d/docker.sources >/dev/null <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF

sudo apt update
sudo apt install -y \
  docker-ce \
  docker-ce-cli \
  containerd.io \
  docker-buildx-plugin \
  docker-compose-plugin

sudo systemctl enable --now docker
sudo docker run --rm hello-world
sudo usermod -aG docker "$USER"
```

Cerrar y volver a abrir la sesion SSH para aplicar el grupo. Luego:

```bash
docker version
docker compose version
systemctl is-enabled docker
systemctl is-active docker
```

El grupo `docker` otorga privilegios equivalentes a root. Solo agregar usuarios
administrativos.

## 3. Clonar el repositorio nuevo

Crear el repositorio remoto vacio, sin README ni licencia generados por la
plataforma. En el VPS:

```bash
sudo install -d -o "$USER" -g "$USER" /opt/kontora
git clone <URL-DEL-REPOSITORIO-NUEVO> /opt/kontora
cd /opt/kontora
```

Comprobar que el archivo de entorno real no viene versionado:

```bash
test ! -f infra/.env
git status --short
```

## 4. Crear y proteger el entorno

```bash
cp infra/.env.production.example infra/.env
chmod 600 infra/.env
```

Generar contrasena de PostgreSQL, JWT de la aplicacion y claves de Storage sin
instalar Node en el host:

```bash
docker run --rm \
  --mount type=bind,source="$PWD/infra/scripts",target=/scripts,readonly \
  node:22.23.1-alpine3.24 \
  node /scripts/New-ProductionSecrets.mjs \
  > /tmp/kontora-production-secrets.env

chmod 600 /tmp/kontora-production-secrets.env
```

Abrir `infra/.env` y copiar los seis valores generados:

```bash
nano infra/.env
```

Completar como minimo:

```env
APP_BIND_ADDRESS=127.0.0.1
APP_PORT=8080
FRONTEND_BIND_ADDRESS=127.0.0.1
FRONTEND_PORT=8081
FRONTEND_API_URL=/api

TZ=America/Bogota
JAVA_TOOL_OPTIONS=-Duser.timezone=America/Bogota

DB_NAME=kontora_pos
DB_USER=kontora_pos
DB_PASSWORD=<valor-generado>
JWT_SECRET=<valor-generado>

CORS_ALLOWED_ORIGINS=https://kontora-pos.store

CLOUDFLARED_IMAGE=cloudflare/cloudflared:2026.7.2
CLOUDFLARE_TUNNEL_TOKEN=

STORAGE_DATABASE_URL=<valor-generado>
STORAGE_JWT_SECRET=<valor-generado>
STORAGE_SERVICE_ROLE_KEY=<valor-generado>
SUPABASE_STORAGE_BUCKET=kontoraimagenes

BOOTSTRAP_MANAGER_ENABLED=true
BOOTSTRAP_MANAGER_USERNAME=gerenteLocal
BOOTSTRAP_MANAGER_FULL_NAME=Gerente Local
BOOTSTRAP_MANAGER_PASSWORD=<valor-generado>
```

Si se usan otro `DB_USER` o `DB_NAME`, actualizar también
`STORAGE_DATABASE_URL`. No usar caracteres sin codificar dentro de esa URL.

Eliminar el archivo temporal:

```bash
shred -u /tmp/kontora-production-secrets.env
```

No mostrar `infra/.env` en capturas ni ejecutar `docker compose config` sin
`--quiet`.

## 5. Validar y construir

```bash
cd /opt/kontora

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  config --quiet

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  --profile tunnel \
  config --services
```

La lista debe contener:

```text
postgres
storage-db-init
storage
storage-bucket-init
backend
frontend
cloudflared
```

Construir secuencialmente para reducir picos de memoria:

```bash
docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  build backend

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  build frontend
```

El Dockerfile del backend usa `-DskipTests`: el build compila y empaqueta, pero
no reemplaza las pruebas realizadas antes de publicar una version. El frontend
si ejecuta TypeScript y Vite durante su build.

## 6. Iniciar el stack privado

```bash
docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  up -d postgres

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  up -d --wait --wait-timeout 120 storage

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  run --rm --no-deps storage-bucket-init

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  up -d --no-build backend frontend

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  ps -a
```

`storage-db-init` y `storage-bucket-init` deben terminar con codigo `0`.
PostgreSQL, Storage y frontend deben quedar `healthy`; backend debe estar `Up`.

No reemplazar `run --rm --no-deps storage-bucket-init` por
`up storage-bucket-init`: `up` puede relanzar `storage-db-init` mientras
Storage modifica el esquema y provocar `ERROR: tuple concurrently updated`.

Validar:

```bash
curl --fail --show-error http://127.0.0.1:8080/api/health
curl --fail --show-error http://127.0.0.1:8081/healthz
curl --fail --show-error http://127.0.0.1:8081/api/health

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  exec -T postgres \
  psql -U kontora_pos -d kontora_pos \
  -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

No continuar con Cloudflare si algun health falla.

## 7. Validar el gerente y desactivar bootstrap

Antes de crear el tunnel, abrir un reenvio SSH desde el equipo administrador:

```bash
ssh -L 8081:127.0.0.1:8081 <usuario>@<IP-DEL-VPS>
```

Mientras esa sesion permanezca abierta, visitar
`http://127.0.0.1:8081/login` en el equipo administrador e iniciar sesion con
el gerente configurado. No abrir `8081` en el firewall. Tras confirmar el
acceso:

```bash
nano infra/.env
```

Cambiar:

```env
BOOTSTRAP_MANAGER_ENABLED=false
BOOTSTRAP_MANAGER_PASSWORD=
```

Recrear solo backend:

```bash
docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  up -d --no-deps --no-build --force-recreate backend

curl --fail --show-error http://127.0.0.1:8080/api/health
```

## 8. Crear o migrar Cloudflare Tunnel

Seguir la [guia limpia de Cloudflare](./04-fase-cloudflare-tunnel.md).

Para un tunnel nuevo:

1. Crear el tunnel en **Networking > Tunnels**.
2. Copiar solo el token.
3. Guardarlo en `CLOUDFLARE_TUNNEL_TOKEN`.
4. Iniciar `cloudflared`.
5. Cuando aparezca `Healthy`, crear **Published application**:

```text
Hostname: kontora-pos.store
Service URL: http://frontend:8080
```

Para migrar el tunnel de demostracion:

1. Detener cloudflared en el equipo local.
2. Rotar el token.
3. Guardar el nuevo token en el VPS.
4. Mantener el mismo hostname y Service URL.

Iniciar:

```bash
docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  --profile tunnel \
  up -d --no-deps cloudflared

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  --profile tunnel \
  ps cloudflared

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  --profile tunnel \
  logs --tail=120 cloudflared
```

## 9. Validacion publica

```bash
curl --fail --show-error https://kontora-pos.store/healthz
curl --fail --show-error https://kontora-pos.store/api/health
curl --fail --show-error https://kontora-pos.store/login
```

Comprobar en navegador:

- login y logout;
- recarga directa de rutas SPA;
- permisos de los tres roles;
- venta controlada y anulacion;
- inventario y reabastecimiento;
- consultas, transferencias, vista previa y descarga de evidencias;
- responsive en telefono;
- ausencia de solicitudes a puertos o servicios internos.

## 10. Reinicio y recuperacion

Los servicios permanentes usan `restart: unless-stopped`. Comprobarlo con un
reinicio planificado:

```bash
sudo reboot
```

Despues de volver por SSH:

```bash
cd /opt/kontora

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  --profile tunnel \
  ps -a

curl --fail --show-error http://127.0.0.1:8081/healthz
curl --fail --show-error https://kontora-pos.store/healthz
```

Si un servicio no inicia:

```bash
docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  --profile tunnel \
  logs --tail=200 postgres storage backend frontend cloudflared
```

No usar `down -v`: elimina los volumenes de PostgreSQL y Storage.

## Errores previsibles

| Error | Revision |
| --- | --- |
| `permission denied` al usar Docker | Cerrar y abrir SSH despues de agregar el grupo, o usar `sudo` de forma consistente. |
| `no space left on device` | Revisar `df -h`, imagenes y logs antes de construir; no borrar volumenes con datos. |
| Build termina por falta de memoria | Construir backend y frontend por separado; revisar `free -h` y configurar swap controlado si el proveedor lo permite. |
| Backend no conecta a PostgreSQL | Confirmar `DB_HOST=postgres`, contrasena y health del contenedor. |
| Storage no inicia | Confirmar que `STORAGE_DATABASE_URL` usa la misma contrasena y que los dos JWT pertenecen al mismo `STORAGE_JWT_SECRET`. |
| Bucket no existe | Ejecutar `run --rm --no-deps storage-bucket-init`; debe confirmar la preparación correcta. |
| `tuple concurrently updated` en `storage-db-init` | No borrar volúmenes. Confirmar PostgreSQL y Storage sanos, retirar solo los inicializadores fallidos y usar `run --rm --no-deps storage-bucket-init`. |
| Login `403` por Internet | Agregar el origen HTTPS exacto a CORS y recrear solo backend. |
| Tunnel `502` | Confirmar `http://frontend:8080` y que frontend/cloudflared comparten la red `tunnel`. |
| Tunnel `1033` | Confirmar replica `Healthy` y que DNS usa el UUID del tunnel actual. |

## Actualizacion posterior

Antes de actualizar:

1. crear respaldo de PostgreSQL y Storage;
2. probar la version;
3. registrar las imagenes activas.

Luego:

```bash
cd /opt/kontora
git pull --ff-only

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  build backend

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  build frontend

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  up -d --no-deps --no-build backend frontend
```

No recrear PostgreSQL o Storage para aplicar solamente código de backend o
frontend.

## Referencias oficiales

- [Docker Engine en Ubuntu](https://docs.docker.com/engine/install/ubuntu/)
- [Postinstalacion de Docker](https://docs.docker.com/engine/install/linux-postinstall/)
- [Cloudflare Tunnel](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/)
- [Firewall de Cloudflare Tunnel](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/configure-tunnels/tunnel-with-firewall/)
