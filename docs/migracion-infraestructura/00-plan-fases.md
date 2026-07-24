# Migracion de infraestructura por contenedores

## Objetivo

Separar los componentes de Kontora POS para que cada responsabilidad tenga su
propio contenedor y pueda actualizarse, reiniciarse y respaldarse de forma
controlada en un VPS.

## Fases

1. PostgreSQL y backend Spring Boot en contenedores independientes.
2. Supabase Storage autoalojado y configurado solo para evidencias.
3. Compilacion de React + Vite y servicio estatico mediante Nginx.
4. Publicacion de frontend y API mediante Cloudflare Tunnel.
5. Validacion integral, copias de seguridad y recuperacion.
6. Limpieza de artefactos y preparacion de un repositorio Git nuevo.

No se inicia una fase mientras la anterior conserve criterios de aceptacion
pendientes.

## Estado actual

| Fase | Estado |
| --- | --- |
| 1. PostgreSQL y backend | Cerrada en desarrollo local |
| 2. Supabase Storage autoalojado | Cerrada en desarrollo local |
| 3. React + Vite mediante Nginx | Cerrada en desarrollo local |
| 4. Cloudflare Tunnel | Cerrada para demostracion local; migracion al VPS documentada |
| 5. Validacion integral y recuperacion | Cerrada localmente; debe repetirse sobre el VPS |
| 6. Limpieza Git y repositorio nuevo | Procedimiento listo; ejecucion manual pendiente |

Las comprobaciones equivalentes sobre el VPS se ejecutaran durante las fases de
despliegue y validacion integral.

## Guias de cierre

- [Despliegue completo en VPS](./05-despliegue-vps.md).
- [Limpieza Git y repositorio nuevo](./06-repositorio-git-nuevo.md).

## Arquitectura objetivo

```text
Internet
  |
Cloudflare Tunnel
  |
Nginx (frontend) ---- Spring Boot (API)
                         |       |
                         |   Storage API
                         |       |     |
                         +-- PostgreSQL |
                                  volumen de objetos
```

Storage comparte el servidor PostgreSQL con la aplicacion, pero usa su propio
esquema `storage`. Los binarios se conservan en un volumen distinto. La Fase 2
no instala el stack completo de Supabase: mantiene solo Storage API y las
dependencias que Kontora utiliza.

## Reglas de trabajo

- Los secretos reales solo se guardan en `infra/.env`, nunca en Git.
- Flyway es la unica herramienta que modifica el esquema de la aplicacion.
- Los datos persistentes deben vivir fuera del ciclo de vida del contenedor.
- Antes de migrar datos existentes se genera y verifica una copia de seguridad.
- Cada fase registra cambios, pruebas, errores y decisiones en la bitacora.
- No se usa `docker compose down -v` en un entorno con datos que deban conservarse.
