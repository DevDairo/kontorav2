# DEC-002: Tecnologias seleccionadas

## Estado

Aceptada.

## Decision

Kontora POS usara:

- Frontend: React + TypeScript + Vite.
- Backend: Java + Spring Boot.
- Seguridad: Spring Security, JWT y sesiones persistidas.
- Persistencia: Spring Data JPA, Hibernate en modo `validate` y Flyway.
- Base de datos: Supabase PostgreSQL y PostgreSQL local para desarrollo.
- Evidencias: Supabase Storage.
- Contenedores: Docker y Docker Compose.
- Pruebas: JUnit y Mockito.
- Documentacion API: OpenAPI/Swagger en fases posteriores.

## Consecuencias

El desarrollo debe avanzar por fases. Antes de pasar a un modulo backend se debe compilar y probar localmente.

