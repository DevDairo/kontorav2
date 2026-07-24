# Integracion local frontend-backend

## Objetivo

Documentar la validacion local entre el frontend Vite y el backend Spring Boot durante la inicializacion de Fase 4.

## Contexto

- Frontend local: `http://127.0.0.1:5173`.
- Backend local: `http://localhost:8080/api`.
- Variable frontend: `VITE_API_URL=http://localhost:8080/api`.
- Variable backend CORS: `CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173`.

## Error observado en navegador

La pantalla React cargaba, pero el health check quedaba bloqueado por CORS.

Error relevante de consola:

```text
Access to fetch at 'http://localhost:8080/api/health' from origin 'http://127.0.0.1:5173' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
GET http://localhost:8080/api/health net::ERR_FAILED 200 (OK)
```

Interpretacion:

- El backend respondia `200 OK`.
- El navegador descartaba la respuesta porque faltaba `Access-Control-Allow-Origin`.
- El problema no estaba en el endpoint `/api/health`, sino en la configuracion CORS del backend.

## Errores encontrados durante la verificacion

### Maven dentro del sandbox

Al ejecutar Maven desde Codex sin permisos externos, fallo la resolucion de dependencias:

```text
Host desconocido (repo.maven.apache.org)
Permission denied: getsockopt
```

Accion:

- Las validaciones Maven se ejecutaron fuera del sandbox con aprobacion.

### Puerto 8080 ocupado

Al ejecutar:

```powershell
cd C:\Users\corre\Desktop\Kontora\backend
mvn spring-boot:run
```

Spring Boot fallo con:

```text
Web server failed to start. Port 8080 was already in use.
```

Causa:

- Ya estaba corriendo el contenedor Docker `kontora_pos_backend_local`.
- Ese contenedor respondia `GET /api/health`, pero estaba construido con una imagen anterior al ajuste CORS.

### Backend Docker desactualizado

Antes de reconstruir el contenedor:

- `GET /api/health` respondia correctamente.
- `OPTIONS /api/health` con `Origin: http://127.0.0.1:5173` respondia `403`.
- `GET /api/health` con `Origin` no incluia `Access-Control-Allow-Origin`.

Accion:

```powershell
docker compose --env-file infra\.env -f infra\compose.local.yml --profile backend up -d --build backend
```

## Cambio aplicado en backend

Archivo principal:

```text
backend/src/main/java/com/kontora/pos/common/config/SecurityConfig.java
```

Se agrego:

- Configuracion CORS para `/api/**`.
- Origenes permitidos configurables por `CORS_ALLOWED_ORIGINS`.
- Metodos permitidos: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`.
- Headers permitidos: `Authorization`, `Content-Type`, `Accept`.
- Permiso explicito para `OPTIONS /api/**`.
- Registro de `CorsFilter` con maxima precedencia para que el preflight se atienda antes de seguridad/JWT.

## Impacto en autenticacion

El cambio no abre endpoints de negocio.

Reglas conservadas:

- `GET /api/health` sigue publico.
- `POST /api/auth/login` sigue publico.
- `OPTIONS /api/**` queda publico solo para preflight CORS.
- El resto de endpoints sigue protegido por `anyRequest().authenticated()`.
- El header `Authorization` queda permitido por CORS para futuras llamadas autenticadas desde frontend.

Verificacion manual:

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/auth/me" -Headers @{ Origin = "http://127.0.0.1:5173" }
```

Resultado esperado sin token:

```text
401
```

## Validaciones realizadas

Pruebas enfocadas:

```powershell
cd C:\Users\corre\Desktop\Kontora\backend
mvn "-Dtest=HealthEndpointIntegrationTest,AutenticacionIntegrationTest" test
```

Resultado:

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Suite completa backend:

```powershell
cd C:\Users\corre\Desktop\Kontora\backend
mvn clean test
```

Resultado:

```text
Tests run: 49, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Build frontend:

```powershell
cd C:\Users\corre\Desktop\Kontora\frontend
npm run build
```

Resultado:

```text
vite build OK
```

## Validacion manual final

Health backend:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/health" | ConvertTo-Json -Compress
```

Resultado:

```json
{"status":"ok","service":"kontora-pos-backend"}
```

Preflight CORS:

```powershell
$r = Invoke-WebRequest `
  -Method Options `
  -Uri "http://localhost:8080/api/health" `
  -Headers @{
    Origin = "http://127.0.0.1:5173"
    "Access-Control-Request-Method" = "GET"
  }

$r.StatusCode
$r.Headers["Access-Control-Allow-Origin"]
$r.Headers["Access-Control-Allow-Methods"]
```

Resultado:

```text
200
http://127.0.0.1:5173
GET,POST,PUT,DELETE,OPTIONS
```

GET real con origen frontend:

```powershell
$r = Invoke-WebRequest `
  -Uri "http://localhost:8080/api/health" `
  -Headers @{ Origin = "http://127.0.0.1:5173" }

$r.StatusCode
$r.Headers["Access-Control-Allow-Origin"]
$r.Content
```

Resultado:

```text
200
http://127.0.0.1:5173
{"status":"ok","service":"kontora-pos-backend"}
```

Validacion en consola del navegador:

```javascript
fetch("http://localhost:8080/api/health")
  .then((r) => r.json())
  .then(console.log)
  .catch(console.error);
```

Resultado:

```javascript
{ status: "ok", service: "kontora-pos-backend" }
```

## Estado esperado en pantalla

La interfaz operativa ya no muestra un indicador de API. La comprobacion se realiza con la consola o las solicitudes de red, mientras que el login debe renderizar su formulario correctamente:

```text
Iniciar sesion
```
