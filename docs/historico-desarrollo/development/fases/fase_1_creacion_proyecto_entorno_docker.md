# Ruta de Inicialización y Desarrollo Secuencial - Kontora POS

## 1. Propósito del documento

Este documento define la ruta operativa para iniciar y desarrollar Kontora POS de forma secuencial, didáctica y controlada. Su objetivo es orientar el trabajo desde la preparación del entorno técnico hasta el desarrollo modular del backend, la construcción posterior del frontend y la validación integral del sistema.

La ruta se basa en la documentación técnica ya consolidada del proyecto, que incluye alcance funcional, requisitos funcionales y no funcionales, arquitectura de software, tecnologías seleccionadas, modelo relacional, diccionario de datos, riesgos técnicos y guía de despliegue derivada de la prueba de concepto.

El desarrollo debe realizarse de forma progresiva, usando Git y Pull Requests como mecanismo de control. Cada fase debe dejar evidencia en el repositorio mediante código, configuración, documentación y pruebas ejecutadas.

---

## 2. Principios de trabajo del proyecto

Antes de iniciar el desarrollo, se establecen los siguientes principios obligatorios:

1. El repositorio será la fuente única de verdad.
2. Todo cambio se realizará en una rama de trabajo.
3. Todo avance relevante deberá integrarse mediante Pull Request.
4. Ninguna credencial real debe subirse al repositorio.
5. Todo archivo `.env` real debe estar excluido por `.gitignore`.
6. Toda variable necesaria debe documentarse en `.env.example`.
7. La lógica crítica del negocio vivirá en el backend.
8. La base de datos será responsable de persistencia, relaciones, restricciones básicas e historial.
9. El frontend no accederá directamente a operaciones críticas de base de datos.
10. Cada módulo debe documentarse antes de considerarse terminado.
11. Cada módulo debe probarse localmente antes de avanzar al siguiente.
12. No se debe iniciar un módulo que dependa de otro si el módulo base no ha sido validado.

---

## 3. Arquitectura objetivo de referencia

La arquitectura general del sistema será:

```text
Usuario final
    -> Dominio principal
        -> Frontend en Vercel
            -> Subdominio API
                -> Túnel seguro
                    -> VM de aplicación
                        -> Backend Dockerizado
                            -> Supabase PostgreSQL / Supabase Storage
```

Componentes principales:

- Frontend: React + TypeScript + Vite.
- Backend: Java + Spring Boot.
- Base de datos: Supabase PostgreSQL usando Session Pooler.
- Almacenamiento de evidencias: Supabase Storage.
- Contenedores: Docker y Docker Compose.
- Infraestructura backend: VM Ubuntu Server.
- Exposición segura de API: túnel seguro y subdominio API.
- Despliegue frontend: Vercel.
- Control de versiones: Git y GitHub.
- Gestión de cambios: Pull Requests.
- Documentación: archivos Markdown versionados en el repositorio.

---

---

# Fase 1: Creación del Proyecto y Entorno Docker

## 1.1. Objetivo de la fase

Crear la base técnica mínima sobre la cual se ejecutará la lógica de negocio. En esta fase se prepara el repositorio, el entorno contenedorizado, las variables de entorno, la conexión con Supabase y los archivos iniciales de infraestructura.

Esta fase no debe desarrollar todavía lógica funcional del sistema. Su propósito es garantizar que el entorno técnico pueda sostener el backend antes de implementar módulos como usuarios, caja, ventas o inventario.

---

## 1.2. Resultado esperado de la fase

Al finalizar esta fase debe existir:

- Repositorio inicial creado.
- Estructura base de carpetas.
- Carpeta `infra/` preparada.
- Archivo `compose.local.yml` o `compose.dev.yml` definido.
- Archivo `.env.example` creado.
- Archivo `.gitignore` configurado.
- Carpeta `database/` creada con scripts o migraciones iniciales.
- Conexión planificada hacia Supabase.
- Documentación mínima del entorno.
- Pull Request de inicialización del entorno.

---

## 1.3. Estructura inicial recomendada del repositorio

```text
kontora-pos/
├── backend/
├── frontend/
├── infra/
│   ├── compose.local.yml
│   ├── compose.prod.yml
│   └── .env.example
├── database/
│   ├── schema/
│   │   └── kontora_pos_schema_v1_1.sql
│   └── migrations/
│       └── V1_1__ajustes_inventario_alertas_y_referencias.sql
├── docs/
│   ├── requirements/
│   ├── database/
│   ├── architecture/
│   ├── decisions/
│   ├── modules/
│   └── deployment/
├── README.md
├── .gitignore
└── .env.example
```

Nota: en esta fase las carpetas `backend/` y `frontend/` pueden estar vacías o contener solo archivos `.gitkeep` si aún no se inicializan los proyectos técnicos.

---

## 1.4. Paso a paso de la fase

### Paso 1. Crear o clonar el repositorio

Crear el repositorio en GitHub y clonarlo en la máquina local de desarrollo.

```bash
git clone [URL_REPOSITORIO] kontora-pos
cd kontora-pos
```

Crear una rama para esta fase:

```bash
git checkout -b chore/inicializacion-entorno-docker
```

---

### Paso 2. Crear estructura base de carpetas

Crear las carpetas principales:

```bash
mkdir backend frontend infra database docs
mkdir -p database/schema database/migrations
mkdir -p docs/requirements docs/database docs/architecture docs/decisions docs/modules docs/deployment
```

Si alguna carpeta queda vacía temporalmente, agregar un archivo `.gitkeep`:

```bash
touch backend/.gitkeep frontend/.gitkeep
```

---

### Paso 3. Crear `.gitignore`

El archivo `.gitignore` debe impedir subir dependencias, builds y secretos.

Contenido sugerido:

```gitignore
# Entorno
.env
*.env
!*.env.example

# Java / Spring Boot
target/
*.class
*.jar
*.war

# Node / Frontend
node_modules/
dist/
build/

# IDEs
.vscode/
.idea/
*.iml

# Sistema operativo
.DS_Store
Thumbs.db

# Logs
*.log
logs/
```

---

### Paso 4. Crear `.env.example` general

Este archivo documenta las variables necesarias sin exponer valores reales.

```env
# Backend
APP_PORT=8080
SPRING_PROFILES_ACTIVE=local

# Supabase PostgreSQL / Session Pooler
DB_HOST=
DB_PORT=5432
DB_NAME=postgres
DB_USER=
DB_PASSWORD=
DB_SSLMODE=require

# JWT
JWT_SECRET=
JWT_EXPIRATION_MINUTES=60

# Supabase Storage
SUPABASE_URL=
SUPABASE_SERVICE_ROLE_KEY=
SUPABASE_STORAGE_BUCKET=

# Frontend
VITE_API_URL=http://localhost:8080/api
```

Regla: este archivo sí se sube al repositorio. Los archivos `.env` reales no se suben.

---

### Paso 5. Preparar `infra/compose.local.yml`

Como la base de datos real estará en Supabase, el entorno Docker local puede iniciar solo el backend cuando este exista. Técnicamente, antes de crear el backend no habrá contenedor funcional que ejecutar. Por eso, en esta fase se puede dejar el archivo preparado y ejecutarlo inmediatamente después de inicializar el backend.

Estructura inicial sugerida:

```yaml
services:
  backend:
    build:
      context: ../backend
      dockerfile: Dockerfile
    container_name: kontora_pos_backend_local
    restart: unless-stopped
    env_file:
      - .env
    ports:
      - "8080:8080"
```

Nota técnica: este compose no se podrá ejecutar correctamente hasta que exista `backend/Dockerfile`. Por eso se documenta en esta fase, pero se valida después de inicializar el backend.

---

### Paso 6. Preparar `infra/.env.example`

```env
APP_PORT=8080
SPRING_PROFILES_ACTIVE=local

DB_HOST=
DB_PORT=5432
DB_NAME=postgres
DB_USER=
DB_PASSWORD=
DB_SSLMODE=require

JWT_SECRET=
JWT_EXPIRATION_MINUTES=60

SUPABASE_URL=
SUPABASE_SERVICE_ROLE_KEY=
SUPABASE_STORAGE_BUCKET=
```

---

### Paso 7. Ubicar el script SQL actualizado

Guardar el script físico actualizado en:

```text
database/schema/kontora_pos_schema_v1_1.sql
```

Guardar la migración incremental en:

```text
database/migrations/V1_1__ajustes_inventario_alertas_y_referencias.sql
```

Regla de uso:

- Si Supabase está vacío: ejecutar el script completo.
- Si Supabase ya tiene la versión anterior: ejecutar la migración incremental.

---

### Paso 8. Verificar conexión con Supabase

Antes de desarrollar lógica funcional, debe confirmarse que existen los datos de conexión:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USER
DB_PASSWORD
DB_SSLMODE
```

Validaciones esperadas:

- El proyecto Supabase existe.
- El Session Pooler está identificado.
- El puerto corresponde al modo de conexión elegido.
- La contraseña no está expuesta en documentación.
- La base de datos acepta conexión desde el entorno de desarrollo o desde la VM.

---

## 1.5. Documentación requerida en esta fase

Crear o actualizar:

```text
README.md
docs/deployment/guia-despliegue-poc.md
docs/database/uso-script-sql.md
docs/decisions/DEC-001-separacion-logica-backend-base-datos.md
docs/decisions/DEC-002-tecnologias-seleccionadas.md
```

Contenido mínimo de `README.md` en esta fase:

```markdown
# Kontora POS

Aplicación web para apoyar la gestión de ventas, caja, inventario, depósito, evidencias, usuarios y auditoría del negocio.

## Stack principal

- Frontend: React + TypeScript + Vite.
- Backend: Java + Spring Boot.
- Base de datos: Supabase PostgreSQL.
- Contenedores: Docker y Docker Compose.
- Despliegue frontend: Vercel.
- Backend: VM Ubuntu Server.

## Estado del proyecto

Fase inicial: preparación de entorno, documentación y estructura base.
```

---

## 1.6. Validación antes de cerrar la fase

Checklist:

- [ ] Repositorio creado y clonado.
- [ ] Rama de trabajo creada.
- [ ] Carpetas principales creadas.
- [ ] `.gitignore` configurado.
- [ ] `.env.example` creado.
- [ ] `infra/compose.local.yml` preparado.
- [ ] Script SQL ubicado en `database/schema/`.
- [ ] Migración incremental ubicada en `database/migrations/`.
- [ ] Documentación inicial creada.
- [ ] No existen credenciales reales en el repositorio.
- [ ] `git status` muestra solo archivos esperados.

---

## 1.7. Pull Request de la fase 1

Nombre recomendado de la rama:

```text
chore/inicializacion-entorno-docker
```

Título recomendado del PR:

```text
chore: inicializar entorno Docker y estructura base del repositorio
```

Descripción sugerida del PR:

```markdown
## Objetivo

Preparar la estructura inicial del repositorio, los archivos base de entorno, la carpeta de infraestructura Docker y la ubicación inicial de scripts SQL y documentación técnica.

## Cambios realizados

- Se creó la estructura base del repositorio.
- Se agregó `.gitignore`.
- Se agregó `.env.example`.
- Se creó la carpeta `infra/`.
- Se agregó archivo base de Docker Compose.
- Se creó la estructura inicial de documentación.
- Se ubicaron scripts SQL y migraciones iniciales.

## Pruebas realizadas

- Se verificó que no existan credenciales reales en el repositorio.
- Se ejecutó `git status` para validar archivos incluidos.
- Se revisó que los archivos `.env` reales estén ignorados.

## Documentación actualizada

- `README.md`
- `docs/deployment/guia-despliegue-poc.md`
- `docs/database/uso-script-sql.md`
- `docs/decisions/DEC-001-separacion-logica-backend-base-datos.md`
- `docs/decisions/DEC-002-tecnologias-seleccionadas.md`

## Variables de entorno nuevas

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `DB_SSLMODE`
- `JWT_SECRET`
- `VITE_API_URL`

## Riesgos o pendientes

- El archivo Docker Compose quedará validado completamente después de crear el backend y su Dockerfile.
```

Buenas prácticas para el aprendiz:

- No mezclar esta PR con código funcional.
- No crear todavía controladores, entidades ni pantallas.
- Revisar visualmente que no se haya subido ningún `.env` real.
- Escribir un mensaje de commit claro, por ejemplo:

```bash
git add .
git commit -m "chore: inicializar estructura base e infraestructura"
git push origin chore/inicializacion-entorno-docker
```

---
