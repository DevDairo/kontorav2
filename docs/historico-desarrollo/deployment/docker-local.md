# Docker local

## Servicios disponibles en Fase 1

`infra/compose.local.yml` define PostgreSQL local y el servicio backend para validacion local.

## Validar configuracion

```bash
docker compose -f infra/compose.local.yml config
```

## Levantar PostgreSQL local

```bash
docker compose -f infra/compose.local.yml up -d postgres
```

PostgreSQL inicializa una base local usando:

```text
database/schema/kontora_pos_schema_v1_1.sql
```

Si el volumen ya existe, Docker no reejecuta los scripts de inicializacion. Para recrear la base local en desarrollo, primero se debe eliminar el volumen local de forma intencional.

## Levantar backend local en Docker

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml --profile backend up -d --build backend
docker logs kontora_pos_backend_local --tail 120
Invoke-RestMethod -Uri "http://localhost:8080/api/health" | ConvertTo-Json -Compress
```

Respuesta esperada:

```json
{"status":"ok","service":"kontora-pos-backend"}
```

Para detener solo el backend:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml --profile backend stop backend
```
