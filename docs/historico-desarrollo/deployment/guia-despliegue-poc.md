# Guia de despliegue POC

## Objetivo

Documentar la arquitectura prevista para ejecutar Kontora POS con frontend desacoplado, backend dockerizado y persistencia administrada.

## Flujo objetivo

```text
Usuario autorizado
  -> Frontend en Vercel
  -> Subdominio API
  -> Tunel seguro
  -> VM Ubuntu Server
  -> Backend Spring Boot en Docker
  -> Supabase PostgreSQL y Supabase Storage
```

## Variables sensibles

Las credenciales reales nunca deben versionarse. Solo se versionan `.env.example`.

Variables principales:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `DB_SSLMODE`
- `JWT_SECRET`
- `SUPABASE_URL`
- `SUPABASE_SERVICE_ROLE_KEY`
- `SUPABASE_STORAGE_BUCKET`

## Gerente inicial en la VM

Antes del primer arranque del backend, crear `infra/.env` en la maquina virtual
a partir de `infra/.env.example` y definir estas variables con una contrasena
exclusiva del entorno:

```env
BOOTSTRAP_MANAGER_ENABLED=true
BOOTSTRAP_MANAGER_USERNAME=gerenteLocal
BOOTSTRAP_MANAGER_FULL_NAME=Gerente Local
BOOTSTRAP_MANAGER_PASSWORD=<contrasena-segura-no-versionada>
```

Con `BOOTSTRAP_MANAGER_ENABLED=true`, el backend crea el gerente solo si la
tabla `usuarios` esta vacia. En arranques posteriores no crea, modifica ni
restablece usuarios existentes. Tras confirmar el primer inicio, cambiar la
variable a `false` para dejar deshabilitada la provisión.

## Estado actual

En Fase 1 solo queda preparado el entorno. La validacion completa del backend en Docker se realizara en Fase 2 cuando exista el proyecto Spring Boot.
