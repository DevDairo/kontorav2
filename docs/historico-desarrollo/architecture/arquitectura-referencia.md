# Arquitectura de referencia

Kontora POS se implementara como una aplicacion web por capas:

```text
Usuario final
  -> Frontend React + TypeScript + Vite en Vercel
  -> API Spring Boot en VM Ubuntu Server
  -> Supabase PostgreSQL / PostgreSQL local de desarrollo
  -> Supabase Storage para evidencias
```

## Responsabilidades

- Frontend: interfaz de usuario y experiencia operativa por rol.
- Backend: API REST, seguridad, autorizacion, reglas de negocio, transacciones y auditoria.
- Base de datos: persistencia relacional, integridad, restricciones basicas e historicos.
- Storage: archivos de evidencia; PostgreSQL solo guarda rutas y metadatos.

## Principio rector

El modelo fisico de base de datos es la fuente principal de verdad. El backend y el frontend deben adaptarse a los nombres, relaciones y reglas documentadas.

