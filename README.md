# Kontora POS

Aplicacion web para operar un punto de venta de granizados. Integra ventas,
pagos, caja, inventario, gastos, deposito, evidencias, usuarios, consultas y
auditoria con reglas de negocio centralizadas en Spring Boot.

## Estado

- PostgreSQL, backend, Storage, frontend Nginx y cloudflared se ejecutan en
  contenedores separados.
- Supabase Storage API esta autoalojado; no se instala el stack completo de
  Supabase.
- El bucket `kontoraimagenes` es privado y solo el backend usa la clave
  `service_role`.
- React usa `/api` y Nginx reenvia al backend por la red Docker.
- La demostracion publica fue validada en
  `https://kontora-pos.store`.
- El despliegue definitivo conserva el mismo diseño en un VPS.

Los cambios funcionales recientes y sus pruebas estan en
[docs/15-cambios-recientes-validados.md](docs/15-cambios-recientes-validados.md).

## Arquitectura

```text
Internet
  |
  v
Cloudflare Tunnel
  |
  v
cloudflared -> Nginx (React + /api)
                        |
                        v
                  Spring Boot
                    |       |
                    v       v
              PostgreSQL  Storage API
                              |
                              v
                      volumen de evidencias
```

Puertos de diagnostico del host:

| Servicio | Local | Produccion |
| --- | --- | --- |
| Frontend Nginx | `127.0.0.1:8081` | `127.0.0.1:8081` |
| Backend | `127.0.0.1:8080` | `127.0.0.1:8080` |
| PostgreSQL | `127.0.0.1:5432` | No publicado |
| Storage | `127.0.0.1:5000` | No publicado |

Ninguno de esos puertos se abre a Internet. Cloudflare publica internamente
`http://frontend:8080`.

## Tecnologias

- React 19, TypeScript y Vite 7.
- Nginx `1.30.4-alpine3.24`.
- Java 21, Spring Boot 3, Spring Security, JPA y Flyway.
- PostgreSQL `16-alpine`.
- Supabase Storage API `v1.60.4`.
- cloudflared `2026.7.2`.
- Docker Engine y Docker Compose.

## Ejecucion local en Windows

Requisitos: Docker Desktop iniciado. Node, Java, Maven, PostgreSQL y Nginx no
se instalan en Windows para ejecutar el stack contenedorizado.

```powershell
Copy-Item infra\.env.example infra\.env
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\infra\scripts\New-StorageSecrets.ps1
```

Copiar los dos valores generados a `infra/.env`, definir un `JWT_SECRET`
exclusivo y validar:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml config --quiet
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --build
docker compose --env-file infra\.env -f infra\compose.local.yml ps -a
curl.exe --fail http://127.0.0.1:8081/healthz
curl.exe --fail http://127.0.0.1:8081/api/health
```

Abrir `http://127.0.0.1:8081/login`.

Ejecutar los comandos en ese orden y no continuar cuando uno falle.

Detener sin borrar datos:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml down
```

No usar `down -v`: elimina los volumenes de PostgreSQL y Storage.

## Restablecer los servicios

Estos procedimientos recuperan contenedores pausados o detenidos sin borrar
PostgreSQL, evidencias ni volúmenes.

### Windows

1. Abrir PowerShell, iniciar Docker Desktop y entrar al proyecto:

```powershell
docker desktop start
cd C:\Users\corre\Documentos\kontora
```

2. Consultar el estado:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel ps -a
```

3. Solo si algún servicio aparece como `Paused`, reanudarlo:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel unpause
```

Si no hay servicios en estado `Paused`, omitir este comando.

4. Iniciar los servicios detenidos o volver a crear los contenedores que falten:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel up -d
```

5. Confirmar el estado y probar la aplicación:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel ps -a
curl.exe --fail http://127.0.0.1:8081/healthz
curl.exe --fail http://127.0.0.1:8081/api/health
```

Si no se utiliza Cloudflare Tunnel, reemplazar los comandos de los pasos 2 a 5
por estos:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml ps -a
docker compose --env-file infra\.env -f infra\compose.local.yml unpause
docker compose --env-file infra\.env -f infra\compose.local.yml up -d
docker compose --env-file infra\.env -f infra\compose.local.yml ps -a
curl.exe --fail http://127.0.0.1:8081/healthz
curl.exe --fail http://127.0.0.1:8081/api/health
```

En este caso también se debe omitir `unpause` cuando ningún servicio aparezca
como `Paused`.

### VPS Ubuntu

1. Conectarse por SSH, iniciar Docker y entrar al proyecto:

```bash
sudo systemctl start docker
cd /opt/kontora
```

2. Consultar el estado:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml --profile tunnel ps -a
```

3. Solo si algún servicio aparece como `Paused`, reanudarlo:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml --profile tunnel unpause
```

Si no hay servicios en estado `Paused`, omitir este comando.

4. Iniciar los servicios detenidos o volver a crear los contenedores que falten:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml --profile tunnel up -d
```

5. Confirmar el estado y probar la aplicación:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml --profile tunnel ps -a
curl --fail http://127.0.0.1:8081/healthz
curl --fail http://127.0.0.1:8081/api/health
curl --fail https://kontora-pos.store/healthz
```

`unpause` reanuda los contenedores pausados. `up -d` inicia los detenidos y
vuelve a crear los que falten usando las imágenes existentes. Ninguno de estos
comandos elimina los datos persistentes.

Los servicios `storage-db-init` y `storage-bucket-init` pueden aparecer como
`Exited (0)`; ese es su estado normal después de completar su trabajo.

Si un comando falla, no ejecutar el siguiente. Revisar primero:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml --profile tunnel logs --tail=100
```

En el VPS:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml --profile tunnel logs --tail=100
```

No usar `docker compose down -v`, `docker volume rm` ni eliminar manualmente
los volúmenes para restablecer servicios.

## VPS nuevo desde cero

La guia completa, incluidas restauracion, seguridad, recuperacion y
actualizaciones, esta en
[docs/migracion-infraestructura/05-despliegue-vps.md](docs/migracion-infraestructura/05-despliegue-vps.md).

### 1. Instalar Docker en Ubuntu

```bash
sudo apt update
sudo apt upgrade -y
sudo apt install -y ca-certificates curl git
sudo timedatectl set-timezone America/Bogota

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

Cerrar y volver a abrir SSH; despues:

```bash
docker version
docker compose version
```

### 2. Clonar y crear el entorno

```bash
sudo install -d -o "$USER" -g "$USER" /opt/kontora
git clone <URL-DEL-REPOSITORIO-NUEVO> /opt/kontora
cd /opt/kontora

cp infra/.env.production.example infra/.env
chmod 600 infra/.env
```

Generar secretos sin instalar Node en el VPS:

```bash
docker run --rm \
  --mount type=bind,source="$PWD/infra/scripts",target=/scripts,readonly \
  node:22.23.1-alpine3.24 \
  node /scripts/New-ProductionSecrets.mjs \
  > /tmp/kontora-production-secrets.env

chmod 600 /tmp/kontora-production-secrets.env
nano infra/.env
```

Copiar los seis valores generados y completar:

```env
CORS_ALLOWED_ORIGINS=https://kontora-pos.store
CLOUDFLARED_IMAGE=cloudflare/cloudflared:2026.7.2
CLOUDFLARE_TUNNEL_TOKEN=

BOOTSTRAP_MANAGER_ENABLED=true
BOOTSTRAP_MANAGER_USERNAME=gerenteLocal
BOOTSTRAP_MANAGER_FULL_NAME=Gerente Local
BOOTSTRAP_MANAGER_PASSWORD=<valor-generado>
```

Eliminar el temporal:

```bash
shred -u /tmp/kontora-production-secrets.env
```

No publicar `infra/.env` ni compartir la salida completa de
`docker compose config`.

### 3. Construir e iniciar

```bash
cd /opt/kontora

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  config --quiet

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  build backend

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  build frontend

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
  up -d --no-deps --no-build backend

curl --fail http://127.0.0.1:8080/api/health

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  up -d --no-deps --no-build frontend

docker compose --env-file infra/.env \
  -f infra/compose.prod.yml \
  ps -a

curl --fail http://127.0.0.1:8080/api/health
curl --fail http://127.0.0.1:8081/healthz
curl --fail http://127.0.0.1:8081/api/health
```

`storage-db-init` y `storage-bucket-init` deben salir con codigo `0`. No
continuar si los health no responden `200`. Backend y frontend se inician por
separado con `--no-deps` para no volver a ejecutar los inicializadores de
Storage.

El Dockerfile del backend compila con `-DskipTests`; las pruebas se ejecutan
antes de publicar la version, no durante el build de produccion.

### 4. Crear y conectar Cloudflare Tunnel

Procedimiento detallado:
[docs/migracion-infraestructura/04-fase-cloudflare-tunnel.md](docs/migracion-infraestructura/04-fase-cloudflare-tunnel.md).

Orden limpio:

1. En Cloudflare, abrir **Networking > Tunnels**.
2. Crear un tunnel `Cloudflared` nombrado `kontora-pos`.
3. Elegir Docker y extraer solo la cadena posterior a `--token`.
4. Guardarla en `CLOUDFLARE_TUNNEL_TOKEN`.
5. Iniciar la replica:

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml --profile tunnel up -d --no-deps cloudflared
docker compose --env-file infra/.env -f infra/compose.prod.yml --profile tunnel logs --tail=100 cloudflared
```

6. Cuando el panel indique `Healthy`, crear:

```text
Route: Published application
Hostname: kontora-pos.store
Type: HTTP
Service URL: http://frontend:8080
```

7. Confirmar que no existen registros DNS anteriores de Vercel para ese
   hostname.
8. Validar:

```bash
curl --fail https://kontora-pos.store/healthz
curl --fail https://kontora-pos.store/api/health
curl --fail https://kontora-pos.store/login
```

Si se migra el tunnel de la demostracion, detener primero la replica local y
rotar el token. No mantener dos replicas contra bases de datos diferentes.

### 5. Desactivar el bootstrap

Despues del primer login:

```env
BOOTSTRAP_MANAGER_ENABLED=false
BOOTSTRAP_MANAGER_PASSWORD=
```

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml up -d --no-deps --no-build --force-recreate backend
```

## Errores frecuentes

| Error | Accion |
| --- | --- |
| Docker API no responde | Iniciar o reiniciar Docker y repetir `docker info`. |
| Pull falla con `no such host` | Revisar DNS del host/daemon y repetir el pull; no reconstruir con imagenes antiguas por accidente. |
| Storage `403 Invalid Compact JWS` | Regenerar `STORAGE_JWT_SECRET` y su `STORAGE_SERVICE_ROLE_KEY` como pareja. |
| Backend no inicia | Revisar PostgreSQL, Flyway, `DB_PASSWORD` y `STORAGE_DATABASE_URL`. |
| Tunnel `1033` | Verificar replica `Healthy` y CNAME con el UUID del tunnel actual. |
| Tunnel `502` | Usar `http://frontend:8080` y confirmar red `tunnel` compartida. |
| Login `403` | Agregar el origen HTTPS exacto a CORS y recrear solo backend. |
| Vercel `DEPLOYMENT_NOT_FOUND` | Retirar DNS anterior y recargar cache despues de crear la ruta del tunnel. |
| `ERR_BLOCKED_BY_CLIENT` de Cloudflare Insights | Telemetria opcional bloqueada por el navegador; no afecta Kontora. |

## Datos, respaldo y recuperacion

- [Reinicio total de PostgreSQL, bucket y gerente inicial](docs/reinicio-total-datos/README.md)
- [Panel web de operaciones, respaldos, exportaciones y evidencias](docs/panel-operaciones/README.md)
- [PostgreSQL y backend](docs/migracion-infraestructura/01-fase-postgresql-backend.md)
- [Storage autoalojado](docs/migracion-infraestructura/02-fase-supabase-storage-local.md)
- [Frontend Nginx](docs/migracion-infraestructura/03-fase-frontend-nginx.md)
- [Cloudflare Tunnel](docs/migracion-infraestructura/04-fase-cloudflare-tunnel.md)
- [VPS completo](docs/migracion-infraestructura/05-despliegue-vps.md)

Los respaldos de PostgreSQL no contienen los binarios del volumen de Storage.
Siempre respaldar y restaurar ambos componentes de forma coordinada. No usar
`down -v` en un entorno con datos.

## Crear el repositorio Git nuevo

La guia segura esta en
[docs/migracion-infraestructura/06-repositorio-git-nuevo.md](docs/migracion-infraestructura/06-repositorio-git-nuevo.md).

Conservar primero una copia del proyecto y crear un repositorio remoto privado
y vacio. Ejecutar en este orden:

```powershell
Remove-Item -LiteralPath .git -Recurse -Force
git init -b main
git add --all
git commit -m "feat: preparar Kontora POS contenedorizado"
git remote add origin <URL-DEL-REPOSITORIO-NUEVO>
git push -u origin main
```

Si `.git` no existe, omitir únicamente el primer comando. No continuar con el
siguiente comando cuando el anterior haya fallado.

No usar `--force` ni publicar un secreto. Si un token llega al remoto, debe
rotarse aunque el repositorio sea privado.

## Documentacion funcional

La guia vigente comienza en [docs/00-indice.md](docs/00-indice.md).

| Modulo | Documento |
| --- | --- |
| Login y sesion | [01](docs/01-login-y-sesion.md) |
| Caja | [02](docs/02-caja-diaria.md) |
| Catalogos | [03](docs/03-catalogos.md) |
| Ventas y pagos | [04](docs/04-ventas-y-pagos.md) |
| Inventario | [05](docs/05-inventario-operativo.md) |
| Gastos | [06](docs/06-gastos-y-pago-trabajadores.md) |
| Cierre | [07](docs/07-cierre-de-caja.md) |
| Deposito | [08](docs/08-deposito-y-servicios.md) |
| Evidencias integradas | [09](docs/09-evidencias.md) |
| Transferencias | [10](docs/10-transferencias.md) |
| Consultas y evidencias | [11](docs/11-consultas.md) |
| Usuarios | [12](docs/12-usuarios.md) |
| Auditoria | [13](docs/13-auditoria.md) |
| Gerente inicial | [14](docs/14-credenciales-gerente-inicial.md) |
| Cambios recientes validados | [15](docs/15-cambios-recientes-validados.md) |

`docs/historico-desarrollo` conserva requisitos y decisiones anteriores. Sus
referencias a Vercel o Supabase Cloud son historicas y no describen el
despliegue vigente.
