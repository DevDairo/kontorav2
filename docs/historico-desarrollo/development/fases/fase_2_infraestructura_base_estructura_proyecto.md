# Ruta de Inicialización y Desarrollo Secuencial - Kontora POS

Este archivo forma parte de la ruta dividida por fases para inicializar y desarrollar Kontora POS de forma secuencial, didáctica y controlada.

# Fase 2: Infraestructura Base y Estructura del Proyecto

## 2.1. Objetivo de la fase

Inicializar la arquitectura base del backend y preparar la estructura técnica del proyecto. En esta fase se crea el proyecto Spring Boot, se configuran perfiles, conexión a base de datos, migraciones, Dockerfile, endpoint de salud y estructura interna de paquetes.

El objetivo no es desarrollar todavía módulos de negocio, sino demostrar que el backend puede compilar, iniciar, conectarse a Supabase y ejecutarse en Docker.

---

## 2.2. Resultado esperado de la fase

Al finalizar esta fase debe existir:

- Proyecto Spring Boot creado en `backend/`.
- `pom.xml` configurado.
- `application.yml` leyendo variables de entorno.
- Flyway habilitado.
- JPA configurado con `ddl-auto=validate`.
- Endpoint `/api/health` funcionando.
- `Dockerfile` del backend creado.
- Docker Compose validado con el backend.
- Estructura de paquetes definida.
- Documentación técnica inicial del backend.

---

## 2.3. Dependencias recomendadas del backend

Dependencias base:

- Spring Web.
- Spring Data JPA.
- PostgreSQL Driver.
- Flyway Migration.
- Spring Security.
- Validation.
- Lombok, si se decide usarlo.
- Spring Boot Actuator, si se decide usar health checks más formales.

Nota: aunque Spring Security se configure completamente después, se puede agregar desde el inicio porque el primer módulo funcional será autenticación y usuarios.

---

## 2.4. Estructura interna sugerida del backend

```text
backend/src/main/java/com/kontora/pos/
├── KontoraPosApplication.java
├── common/
│   ├── exception/
│   ├── response/
│   ├── security/
│   ├── audit/
│   └── config/
├── usuarios/
│   ├── domain/
│   ├── dto/
│   ├── repository/
│   ├── service/
│   └── controller/
├── caja/
├── catalogos/
├── ventas/
├── pagos/
├── inventario/
├── deposito/
├── evidencias/
└── auditoria/
```

Explicación:

- `common/`: clases transversales reutilizables.
- `usuarios/`: autenticación, usuarios, credenciales, sesiones y roles.
- `caja/`: apertura y cierre de caja diaria.
- `catalogos/`: catálogos base del sistema.
- `ventas/`: cabecera y detalles de venta.
- `pagos/`: pagos de venta y validación de transferencias.
- `inventario/`: stock general, stock diario, paquetes, consumos y movimientos.
- `deposito/`: movimientos de depósito, consignaciones y servicios.
- `evidencias/`: archivos y metadatos.
- `auditoria/`: registro de operaciones sensibles.

---

## 2.5. Configuración base esperada

El archivo `application.yml` debe leer variables de entorno. Conceptualmente debe incluir:

```yaml
server:
  port: ${APP_PORT:8080}

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:postgres}?sslmode=${DB_SSLMODE:require}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
```

Regla importante:

- `ddl-auto=validate` evita que Hibernate modifique la base automáticamente.
- Los cambios de estructura deben pasar por SQL controlado o migraciones.

---

## 2.6. Endpoint mínimo de salud

Crear un endpoint simple para validar que el backend responde.

Ruta esperada:

```text
GET /api/health
```

Respuesta conceptual:

```json
{
  "status": "ok",
  "service": "kontora-pos-backend"
}
```

Este endpoint permite probar:

- Backend local.
- Backend en Docker.
- Backend en VM.
- Túnel hacia API.
- Conexión inicial desde frontend.

---

## 2.7. Validación Docker

Una vez creado `backend/Dockerfile`, ejecutar:

```bash
cd infra
cp .env.example .env
# editar .env con valores reales locales o de prueba

docker compose -f compose.local.yml build backend
docker compose -f compose.local.yml up -d backend
docker ps
docker logs kontora_pos_backend_local
curl http://localhost:8080/api/health
```

Validación esperada:

- El contenedor debe iniciar.
- El backend no debe fallar por variables faltantes.
- El endpoint de salud debe responder.
- Si se prueba conexión real a Supabase, debe iniciar sin errores de datasource.

---

## 2.8. Documentación requerida en esta fase

Crear o actualizar:

```text
docs/architecture/arquitectura-backend.md
docs/architecture/estructura-paquetes-backend.md
docs/deployment/docker-local.md
docs/database/conexion-supabase.md
backend/README.md
README.md
```

Contenido mínimo de `backend/README.md`:

```markdown
# Backend - Kontora POS

Backend construido con Java y Spring Boot.

## Responsabilidades

- Exponer API REST.
- Gestionar autenticación y autorización.
- Ejecutar lógica de negocio transaccional.
- Persistir información en Supabase PostgreSQL.
- Registrar auditoría.
- Coordinar evidencias con Supabase Storage.

## Ejecución local

1. Configurar variables de entorno.
2. Compilar el proyecto.
3. Ejecutar la aplicación.
4. Validar `/api/health`.
```

---

## 2.9. Validación antes de cerrar la fase

Checklist:

- [ ] Proyecto Spring Boot creado.
- [ ] Backend compila correctamente.
- [ ] `application.yml` usa variables de entorno.
- [ ] Dockerfile creado.
- [ ] Compose local ejecuta el backend.
- [ ] `/api/health` responde localmente.
- [ ] No hay credenciales reales en el repositorio.
- [ ] Documentación del backend creada.
- [ ] README actualizado.

---

## 2.10. Pull Request de la fase 2

Nombre recomendado de la rama:

```text
chore/inicializacion-backend
```

Título recomendado del PR:

```text
chore: inicializar backend Spring Boot y ejecución Docker
```

Descripción sugerida:

```markdown
## Objetivo

Inicializar el backend de Kontora POS con Spring Boot, configuración base, conexión por variables de entorno, Dockerfile y endpoint mínimo de salud.

## Cambios realizados

- Se creó el proyecto Spring Boot en `backend/`.
- Se configuró `application.yml`.
- Se agregó Dockerfile.
- Se validó Docker Compose local.
- Se creó endpoint `/api/health`.
- Se definió estructura inicial de paquetes.
- Se agregó documentación técnica del backend.

## Pruebas realizadas

- Compilación del backend.
- Ejecución local.
- Ejecución en Docker.
- Prueba de `GET /api/health`.

## Documentación actualizada

- `backend/README.md`
- `docs/architecture/arquitectura-backend.md`
- `docs/architecture/estructura-paquetes-backend.md`
- `docs/deployment/docker-local.md`
- `docs/database/conexion-supabase.md`

## Variables de entorno nuevas

- Ninguna adicional / Detallar si aplica.

## Riesgos o pendientes

- La seguridad completa se implementará en el módulo de usuarios y sesiones.
- Los módulos de negocio aún no están implementados.
```

Buenas prácticas para el aprendiz:

- No implementar login todavía si el objetivo de la PR es solo inicialización.
- No mezclar frontend en esta PR.
- Confirmar que el backend arranca antes de crear la PR.
- Adjuntar evidencia textual de la prueba `/api/health` en la descripción del PR.

---
