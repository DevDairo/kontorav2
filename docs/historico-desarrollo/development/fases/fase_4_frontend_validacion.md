# Ruta de Inicialización y Desarrollo Secuencial - Kontora POS

Este archivo forma parte de la ruta dividida por fases para inicializar y desarrollar Kontora POS de forma secuencial, didáctica y controlada.

# Fase 4: Desarrollo de la Interfaz Frontend y Validación

## 4.1. Objetivo de la fase

Desarrollar la interfaz de usuario una vez que el backend esté validado y documentado. El frontend debe consumir la API del backend, respetar roles, mostrar mensajes claros y facilitar la operación diaria del negocio.

El frontend no debe contener reglas críticas que puedan comprometer la integridad del sistema. Puede validar formularios para mejorar la experiencia, pero la validación definitiva vive en el backend.

---

## 4.2. Resultado esperado de la fase

Al finalizar esta fase debe existir:

- Proyecto React + TypeScript + Vite inicializado.
- Estructura modular del frontend.
- Cliente HTTP configurado.
- Manejo de autenticación en frontend.
- Pantallas por módulo.
- Validación de formularios.
- Manejo de errores.
- Documentación de interfaz por módulo.
- Integración con backend validada.
- Preparación para despliegue en Vercel.

---

## 4.3. Inicialización del frontend

Rama recomendada:

```text
chore/inicializacion-frontend
```

Estructura sugerida:

```text
frontend/
├── src/
│   ├── app/
│   │   ├── routes/
│   │   └── providers/
│   ├── modules/
│   │   ├── auth/
│   │   ├── caja/
│   │   ├── ventas/
│   │   ├── inventario/
│   │   ├── deposito/
│   │   ├── evidencias/
│   │   └── auditoria/
│   ├── shared/
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── services/
│   │   ├── types/
│   │   └── utils/
│   ├── main.tsx
│   └── App.tsx
├── public/
├── package.json
├── vite.config.ts
└── .env.example
```

Variable mínima:

```env
VITE_API_URL=http://localhost:8080/api
```

---

## 4.4. Orden recomendado de pantallas

El orden de interfaz debe seguir el orden del backend:

```text
1. Login.
2. Layout principal por rol.
3. Panel de caja abierta.
4. Catálogos necesarios para formularios.
5. Registro de venta y pagos.
6. Inventario operativo.
7. Gastos, adiciones y pago trabajadores.
8. Cierre de caja.
9. Depósito, consignaciones y servicios.
10. Evidencias.
11. Auditoría y consultas.
```

---

## 4.5. Estrategia de validación frontend

Cada pantalla debe validarse en tres niveles:

### 1. Validación visual

- La pantalla carga correctamente.
- Los formularios son claros.
- Los botones tienen texto comprensible.
- Los errores se muestran de forma entendible.
- La interfaz funciona en escritorio y móvil.

### 2. Validación funcional

- El formulario envía datos correctos.
- El frontend consume la API correcta.
- El estado se actualiza después de cada operación.
- La información persistida se refleja al recargar.

### 3. Validación de seguridad básica

- Usuario sin token no entra a rutas protegidas.
- Vendedor no ve opciones administrativas.
- Administrador no ve opciones exclusivas del gerente si aplica.
- Token inválido o expirado redirige al login.

---

## 4.6. Documentación requerida en esta fase

Crear:

```text
docs/frontend/estructura-frontend.md
docs/frontend/flujo-autenticacion-frontend.md
docs/frontend/guia-componentes.md
docs/frontend/pantallas.md
```

Por cada módulo visual, actualizar o crear:

```text
docs/modules/[modulo]-frontend.md
```

Contenido mínimo por pantalla:

```markdown
# Pantalla: [Nombre]

## Objetivo

Qué operación permite realizar.

## Actor principal

Vendedor / Administrador / Gerente.

## Endpoint consumido

- Método y ruta.

## Campos del formulario

- Campo 1.
- Campo 2.

## Validaciones de interfaz

- Validación 1.
- Validación 2.

## Respuestas esperadas

- Caso exitoso.
- Caso con error.

## Evidencia de prueba

- Descripción de prueba manual realizada.
```

### Registro actual: gestion administrativa de catalogos

La pantalla `/catalogos` conserva la consulta de datos maestros y agrega una vista de gestion para administrador y gerente. Esta vista integra los endpoints reales para crear, editar, activar o inactivar items de inventario y registrar vigencias de precio.

La validacion de interfaz distingue los dos controles de item definidos por los requisitos: `manual_por_consumo` para consumibles y `automatico_por_venta` para vasos. El segundo exige categoria `vasos`, tamano activo y paquetes fijos de 20 unidades. La configuracion de precios se realiza separadamente por tipo de granizado y tamano, y no altera el stock ni el control automatico de vasos.

La autoridad final se mantiene en backend: permisos, existencia inicial en cero, restriccion de inactivacion con stock, proteccion de estructura cuando hay movimientos, vigencia e historial de precios y auditoria de cambios.

---

## 4.7. Pull Requests recomendadas del frontend

### PR 1: Inicialización frontend

Rama:

```text
chore/inicializacion-frontend
```

Título:

```text
chore: inicializar frontend React con Vite y TypeScript
```

Debe incluir:

- Proyecto Vite.
- Estructura base.
- `.env.example`.
- Cliente HTTP base.
- Conexión con `/api/health`.
- Documentación inicial del frontend.

---

### PR 2: Autenticación frontend

Rama:

```text
feature/frontend-auth
```

Título:

```text
feat: implementar login y manejo de sesión en frontend
```

Debe incluir:

- Pantalla de login.
- Almacenamiento controlado del token.
- Logout.
- Protección de rutas.
- Consulta de usuario autenticado.

---

### PR 3: Panel por rol

Rama:

```text
feature/frontend-layout-roles
```

Título:

```text
feat: implementar layout principal y navegación por rol
```

Debe incluir:

- Menú principal.
- Navegación por rol.
- Ocultamiento de opciones no permitidas.
- Pantalla base de inicio.

---

### PRs posteriores por módulo

```text
feature/frontend-caja-diaria
feature/frontend-ventas-pagos
feature/frontend-inventario
feature/frontend-gastos-adiciones
feature/frontend-cierre-caja
feature/frontend-deposito
feature/frontend-evidencias
feature/frontend-auditoria-consultas
```

---

## 4.8. Plantilla de PR para frontend

```markdown
## Objetivo

Describir qué pantalla o flujo visual se implementa.

## Pantallas incluidas

- Pantalla 1.
- Pantalla 2.

## Endpoints consumidos

- `GET /api/...`
- `POST /api/...`

## Validaciones de interfaz

- Validación 1.
- Validación 2.

## Pruebas realizadas

- Carga visual.
- Envío de formulario.
- Manejo de error.
- Validación de permisos por rol.
- Prueba en navegador.

## Documentación actualizada

- `docs/frontend/...`
- `docs/modules/...`

## Variables de entorno nuevas

- Ninguna / Detallar.

## Riesgos o pendientes

- Pendiente 1.
```

Buenas prácticas para el aprendiz:

- No duplicar reglas críticas del backend como si fueran definitivas.
- Validar en frontend solo para mejorar la experiencia de usuario.
- Confirmar siempre la respuesta real del backend.
- Probar con roles diferentes.
- Mantener componentes pequeños.
- Documentar cada pantalla.
- No mezclar muchas pantallas en una sola PR.

---

# 5. Ruta general consolidada

La secuencia completa del proyecto será:

```text
FASE 1 - Creación del Proyecto y Entorno Docker
1. Crear repositorio.
2. Crear estructura base.
3. Configurar `.gitignore` y `.env.example`.
4. Preparar infraestructura Docker.
5. Ubicar scripts SQL.
6. Documentar entorno y decisiones técnicas.
7. Crear PR de inicialización del entorno.

FASE 2 - Infraestructura Base y Estructura del Proyecto
1. Crear backend Spring Boot.
2. Configurar conexión por variables de entorno.
3. Configurar Flyway y JPA validate.
4. Crear endpoint `/api/health`.
5. Crear Dockerfile.
6. Validar ejecución local y Docker.
7. Documentar arquitectura backend.
8. Crear PR de inicialización backend.

FASE 3 - Desarrollo de Lógica Backend por Módulos
1. Seguridad, usuarios y sesiones.
2. Caja diaria.
3. Catálogos base.
4. Ventas y pagos.
5. Inventario operativo.
6. Gastos, adiciones y pago a trabajadores.
7. Cierre de caja y depósito.
8. Evidencias y almacenamiento.
9. Auditoría transversal.
10. Consultas operativas.

FASE 4 - Desarrollo de Interfaz Frontend y Validación
1. Inicializar frontend React + TypeScript + Vite.
2. Implementar login.
3. Implementar layout por rol.
4. Implementar pantallas por módulo.
5. Validar integración con backend.
6. Documentar pantallas y flujos.
7. Preparar despliegue en Vercel.
```

---

# 6. Criterio final para avanzar a despliegue

Antes de desplegar usando la guía de la prueba de concepto, debe cumplirse:

- [ ] Backend compila sin errores.
- [ ] Frontend compila sin errores.
- [ ] Variables de entorno documentadas.
- [ ] Docker ejecuta backend correctamente.
- [ ] Supabase contiene esquema actualizado.
- [ ] Autenticación funcional.
- [ ] Caja diaria funcional.
- [ ] Ventas y pagos funcionales.
- [ ] Inventario refleja ventas y anulaciones.
- [ ] Cierre de caja genera depósito correctamente.
- [ ] Evidencias se almacenan correctamente.
- [ ] Auditoría registra operaciones sensibles.
- [ ] Frontend consume API pública o local según entorno.
- [ ] No hay secretos en Git.
- [ ] Documentación actualizada.
- [ ] PRs revisadas e integradas.

---

# 7. Regla de oro del proyecto

No avanzar por cantidad de código, sino por módulos funcionales validados.

Cada avance debe responder cuatro preguntas:

1. ¿Qué requisito cumple?
2. ¿Qué regla de negocio implementa?
3. ¿Cómo se probó?
4. ¿Dónde quedó documentado?

Si una de estas preguntas no tiene respuesta clara, el módulo no debe considerarse terminado.
