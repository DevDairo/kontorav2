# Frontend React servido por Nginx

## Resultado

El frontend se compila en una etapa Node y la imagen final contiene únicamente
Nginx y los archivos estáticos.

```text
Navegador
  -> Nginx
       |-- /, /ventas, /inventario... -> React SPA
       |-- /api/* -> backend:8080
       +-- /healthz -> 200
```

El navegador utiliza `/api`. No contiene direcciones `localhost` del backend y
no conoce Storage.

## Archivos

- `frontend/Dockerfile`
- `frontend/nginx.conf`
- `frontend/.dockerignore`
- `infra/compose.local.yml`
- `infra/compose.prod.yml`

## Orden exacto en local

Ejecutar desde la raíz del proyecto:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml config --quiet
docker compose --env-file infra\.env -f infra\compose.local.yml build frontend
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --no-deps --no-build --force-recreate frontend
docker compose --env-file infra\.env -f infra\compose.local.yml ps frontend
curl.exe --fail http://127.0.0.1:8081/healthz
curl.exe --fail http://127.0.0.1:8081/api/health
```

No ejecutar el siguiente comando si el anterior falla.

Abrir:

```text
http://127.0.0.1:8081/login
```

También se debe poder recargar directamente:

```text
http://127.0.0.1:8081/ventas
http://127.0.0.1:8081/inventario
http://127.0.0.1:8081/consultas
```

## Aplicar únicamente un cambio frontend

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml build frontend
docker compose --env-file infra\.env -f infra\compose.local.yml up -d --no-deps --no-build --force-recreate frontend
```

No es necesario recrear PostgreSQL, Storage o backend.

## Producción

```bash
docker compose --env-file infra/.env -f infra/compose.prod.yml config --quiet
docker compose --env-file infra/.env -f infra/compose.prod.yml build frontend
docker compose --env-file infra/.env -f infra/compose.prod.yml up -d --no-deps --no-build --force-recreate frontend
curl --fail http://127.0.0.1:8081/healthz
curl --fail http://127.0.0.1:8081/api/health
```

## Reglas de Nginx

- Las rutas de React regresan a `index.html`.
- `/api` se reenvía al servicio Docker `backend:8080`.
- Nginx renueva el DNS interno de Docker para tolerar recreaciones del backend.
- `/healthz` comprueba Nginx; `/api/health` comprueba también el proxy.
- La imagen final ejecuta Nginx con usuario no privilegiado.

## Errores frecuentes

| Error | Corrección |
| --- | --- |
| Una ruta React devuelve `404` | Confirmar el fallback a `/index.html` en `nginx.conf`. |
| `/healthz` funciona y `/api/health` falla | Revisar backend y la red Docker `edge`. |
| La interfaz llama a `localhost:8080` | Mantener `FRONTEND_API_URL=/api` y reconstruir frontend. |
| Nginx conserva un backend anterior | Confirmar el resolver Docker en `nginx.conf` y recrear frontend. |
| No aparecen cambios visuales | Reconstruir y recrear únicamente frontend en el orden indicado. |
