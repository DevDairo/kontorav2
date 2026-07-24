# Backend Kontora POS

Backend Java 21 y Spring Boot responsable de autenticacion, autorizacion, reglas transaccionales, auditoria y acceso a PostgreSQL y Supabase Storage.

La guia de configuracion, pruebas y despliegue se mantiene centralizada en el [README principal](../README.md). Los contratos funcionales por modulo estan en [docs/00-indice.md](../docs/00-indice.md).

Comandos principales:

```bash
mvn clean test
mvn spring-boot:run
```

Healthcheck:

```text
GET /api/health
```
