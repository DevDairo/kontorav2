# Ruta de Inicialización y Desarrollo Secuencial - Kontora POS

Este archivo forma parte de la ruta dividida por fases para inicializar y desarrollar Kontora POS de forma secuencial, didáctica y controlada.

# Fase 3: Desarrollo de la Lógica Backend por Módulos

## 3.1. Objetivo de la fase

Desarrollar la lógica de negocio del backend de forma modular, secuencial y validada. Cada módulo debe implementarse, probarse y documentarse antes de avanzar al siguiente.

La regla principal es que el backend concentra la lógica crítica del sistema: permisos, validaciones, transacciones, cálculos, promociones, inventario, cierre de caja, depósito, evidencias y auditoría.

---

## 3.2. Orden obligatorio de módulos backend

El orden recomendado es:

```text
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
```

No se debe iniciar ventas antes de tener autenticación, caja diaria y catálogos base funcionales.

---

## 3.3. Módulo 1: Seguridad, usuarios y sesiones

### Objetivo

Implementar autenticación propia con usuarios internos, roles, credenciales, JWT, sesiones persistidas y cierre de sesión efectivo.

### Funcionalidades mínimas

- Login con `nombre_usuario` y contraseña.
- Validación de contraseña hasheada.
- Generación de JWT.
- Registro de sesión en `sesiones_usuario`.
- Logout con cierre o revocación de sesión.
- Endpoint para consultar usuario autenticado.
- Validación inicial de roles.
- Bloqueo o rechazo de usuarios inactivos/bloqueados.

### Validaciones antes de avanzar

- Un usuario activo puede iniciar sesión.
- Un usuario inactivo o bloqueado no puede iniciar sesión.
- El token permite acceder a endpoints protegidos.
- Al cerrar sesión, el token no debe permitir operaciones protegidas.
- La sesión queda registrada en base de datos.
- Se registra auditoría de login/logout si ya existe soporte básico de auditoría.

### Documentación del módulo

Crear:

```text
docs/modules/usuarios-sesiones.md
```

Contenido mínimo:

- Objetivo del módulo.
- Tablas involucradas.
- Endpoints.
- Reglas de negocio.
- Estados posibles.
- Pruebas realizadas.
- Pendientes.

### Rama y PR

Rama:

```text
feature/usuarios-sesiones
```

Título PR:

```text
feat: implementar autenticación, usuarios y sesiones
```

---

## 3.4. Módulo 2: Caja diaria

### Objetivo

Permitir abrir y consultar la caja diaria, que será el eje operativo para ventas, gastos, inventario diario y cierre.

### Funcionalidades mínimas

- Abrir caja diaria.
- Consultar caja abierta.
- Consultar caja por fecha.
- Impedir más de una caja por fecha.
- Permitir apertura solo a administrador o gerente.
- Asociar usuario de apertura.
- Preparar estructura para cierre posterior.

### Validaciones antes de avanzar

- Sin usuario autenticado no se puede abrir caja.
- Un vendedor no puede abrir caja.
- Administrador o gerente sí pueden abrir caja.
- No se puede abrir una segunda caja para la misma fecha.
- La caja queda en estado `abierta`.

### Documentación del módulo

Crear:

```text
docs/modules/caja-diaria.md
```

Contenido mínimo:

- Flujo de apertura.
- Roles permitidos.
- Endpoints.
- Reglas de bloqueo.
- Pruebas locales.

### Rama y PR

Rama:

```text
feature/caja-diaria
```

Título PR:

```text
feat: implementar apertura y consulta de caja diaria
```

---

## 3.5. Módulo 3: Catálogos base

### Objetivo

Exponer los datos maestros requeridos por los módulos operativos.

### Catálogos incluidos

- Roles.
- Métodos de pago.
- Tipos de granizado.
- Tamaños de vaso.
- Categorías de inventario.
- Unidades de medida.
- Items de inventario.
- Precios vigentes.
- Promociones vigentes.
- Tipos de servicio.

### Validaciones antes de avanzar

- Los catálogos base se consultan correctamente.
- Solo usuarios autenticados pueden consultar catálogos internos si así se define.
- Precios vigentes se filtran correctamente.
- Promociones vigentes se consultan correctamente.
- Items inactivos no aparecen en operaciones normales.

### Documentación del módulo

Crear:

```text
docs/modules/catalogos-base.md
```

Contenido mínimo:

- Catálogos disponibles.
- Tablas involucradas.
- Endpoints.
- Filtros.
- Reglas de vigencia.

### Rama y PR

Rama:

```text
feature/catalogos-base
```

Título PR:

```text
feat: implementar consultas de catálogos base
```

---

## 3.6. Módulo 4: Ventas y pagos

### Objetivo

Registrar ventas realizadas en mostrador, calcular totales, aplicar promociones y registrar pagos en efectivo, transferencia o pago híbrido.

### Funcionalidades mínimas

- Registrar venta asociada a caja abierta.
- Registrar detalle de venta.
- Calcular precio normal por tipo y tamaño.
- Aplicar promoción 2x cuando corresponda.
- Registrar pago en efectivo.
- Registrar pago por transferencia.
- Registrar pago híbrido.
- Validar que la suma de pagos coincida con el total de venta.
- Conservar precio histórico aplicado.
- Validar que no se venda si no hay caja abierta.

### Validaciones antes de avanzar

- No se puede vender sin caja abierta.
- La venta queda asociada a una caja diaria.
- El total de venta coincide con los detalles.
- La suma de pagos coincide con el total.
- La promoción 2x aplica solo a pares del mismo tamaño.
- Las unidades sobrantes se cobran a precio normal.
- Transferencias quedan pendientes de validación.

### Documentación del módulo

Crear:

```text
docs/modules/ventas-pagos.md
```

Contenido mínimo:

- Flujo de venta normal.
- Flujo de pago efectivo.
- Flujo de pago transferencia.
- Flujo de pago híbrido.
- Reglas de promoción.
- Validaciones.
- Endpoints.
- Casos de prueba.

### Rama y PR

Rama:

```text
feature/ventas-pagos
```

Título PR:

```text
feat: implementar registro de ventas y pagos
```

---

## 3.7. Módulo 5: Inventario operativo

### Objetivo

Controlar el stock general, el stock diario de vasos y los movimientos de inventario generados por ventas, anulaciones, paquetes abiertos, consumos y ajustes aprobados.

### Funcionalidades mínimas

- Consultar stock general.
- Consultar stock diario de vasos.
- Registrar paquetes de vasos abiertos.
- Registrar vasos rotos al abrir paquetes.
- Descontar vasos por venta.
- Restaurar vasos por anulación.
- Registrar consumo manual diario.
- Registrar movimientos de inventario con referencia obligatoria.
- Consultar alertas por `cantidad_minima_alerta`.

### Validaciones antes de avanzar

- Todo cambio de stock genera movimiento de inventario.
- No se permiten cantidades negativas.
- Solo vasos pueden manejar stock diario automático.
- Los consumos manuales no deben aplicarse a items automáticos por venta.
- Los movimientos tienen `referencia_origen` e `id_referencia_origen`.
- Las alertas se generan si el stock general es menor o igual a la cantidad mínima definida.

### Documentación del módulo

Crear:

```text
docs/modules/inventario-operativo.md
```

Contenido mínimo:

- Diferencia entre stock general y stock diario.
- Reglas para vasos.
- Reglas para consumos manuales.
- Reglas de movimientos.
- Reglas de alertas.
- Endpoints.
- Casos de prueba.

### Rama y PR

Rama:

```text
feature/inventario-operativo
```

Título PR:

```text
feat: implementar inventario operativo y movimientos de stock
```

---

## 3.8. Módulo 6: Gastos, adiciones y pago a trabajadores

### Objetivo

Registrar operaciones diarias que afectan el cuadre de caja.

### Funcionalidades mínimas

- Registrar total diario de adiciones.
- Editar total diario de adiciones mientras la caja está abierta.
- Registrar gastos de caja.
- Permitir que vendedores registren gastos.
- Permitir que solo administrador o gerente editen/anulen gastos.
- Registrar pago total diario a trabajadores.
- Exigir confirmación del pago a trabajadores antes del cierre.

### Validaciones antes de avanzar

- No se registran gastos sin caja abierta.
- Vendedor puede registrar gasto, pero no anularlo ni editarlo.
- Administrador o gerente pueden editar/anular gasto.
- El pago a trabajadores debe estar confirmado.
- Si el pago es cero, debe existir confirmación explícita.

### Documentación del módulo

Crear:

```text
docs/modules/gastos-adiciones-pago-trabajadores.md
```

### Rama y PR

Rama:

```text
feature/gastos-adiciones-pago-trabajadores
```

Título PR:

```text
feat: implementar gastos, adiciones y pago diario a trabajadores
```

---

## 3.9. Módulo 7: Cierre de caja y depósito

### Objetivo

Cerrar la caja diaria, calcular resultados financieros y registrar automáticamente el movimiento de depósito excluyendo la base de caja.

### Funcionalidades mínimas

- Calcular ventas totales.
- Calcular pagos en efectivo.
- Calcular pagos por transferencia.
- Mostrar transferencias pendientes, validadas y rechazadas.
- Calcular gastos.
- Calcular adiciones.
- Calcular pago a trabajadores.
- Calcular efectivo esperado sin base.
- Registrar efectivo contado sin base.
- Calcular diferencia de caja.
- Calcular valor a depósito.
- Crear movimiento de depósito automáticamente.
- Cambiar caja a estado cerrada.
- Bloquear nuevas ventas y anulaciones.

### Validaciones antes de avanzar

- No se puede cerrar caja sin pago a trabajadores confirmado.
- Debe existir registro de adiciones diarias.
- La base de caja no se suma al depósito.
- El movimiento de depósito coincide con el valor calculado.
- Después del cierre no se permiten ventas.
- Después del cierre no se permiten anulaciones.

### Documentación del módulo

Crear:

```text
docs/modules/cierre-caja-deposito.md
```

### Rama y PR

Rama:

```text
feature/cierre-caja-deposito
```

Título PR:

```text
feat: implementar cierre de caja y movimiento automático de depósito
```

---

## 3.10. Módulo 8: Evidencias y almacenamiento

### Objetivo

Gestionar evidencias fotográficas o documentales asociadas a transferencias, gastos, consignaciones y pagos de servicios.

### Funcionalidades mínimas

- Cargar evidencia de transferencia.
- Cargar evidencia de gasto.
- Cargar evidencia de consignación.
- Cargar evidencia de pago de servicio.
- Comprimir imagen desde backend.
- Guardar archivo en Supabase Storage.
- Guardar metadatos en `archivos_evidencia`.
- Controlar acceso a evidencias por rol.

### Validaciones antes de avanzar

- El archivo se almacena fuera de PostgreSQL.
- PostgreSQL guarda solo ruta y metadatos.
- Una evidencia queda asociada a un solo proceso.
- Transferencias no deben quedar completas sin evidencia si la regla lo exige.
- Solo usuarios autorizados pueden consultar evidencias.

### Documentación del módulo

Crear:

```text
docs/modules/evidencias-storage.md
```

### Rama y PR

Rama:

```text
feature/evidencias-storage
```

Título PR:

```text
feat: implementar gestión de evidencias con Supabase Storage
```

---

## 3.11. Módulo 9: Auditoría transversal

### Objetivo

Registrar operaciones sensibles realizadas por usuarios del sistema.

### Operaciones mínimas auditables

- Login.
- Logout.
- Apertura de caja.
- Cierre de caja.
- Anulación de ventas.
- Edición o anulación de gastos.
- Validación o rechazo de transferencias.
- Solicitud, aprobación o rechazo de ajustes de inventario.
- Cambios de precios.
- Cambios de promociones.
- Cambios de configuración.
- Movimientos de depósito.

### Validaciones antes de avanzar

- Cada operación sensible genera registro en `auditoria_operaciones`.
- La auditoría registra usuario responsable.
- La auditoría registra tabla o entidad afectada.
- La auditoría registra acción realizada.
- La auditoría registra valores anteriores y nuevos cuando aplica.

### Documentación del módulo

Crear:

```text
docs/modules/auditoria-operaciones.md
```

### Rama y PR

Rama:

```text
feature/auditoria-operaciones
```

Título PR:

```text
feat: implementar auditoría de operaciones sensibles
```

---

## 3.12. Módulo 10: Consultas operativas

### Objetivo

Permitir consultas internas por día o periodo sobre ventas, caja, inventario, depósito, gastos, comprobantes y auditoría.

### Funcionalidades mínimas

- Consultar ventas por día.
- Consultar cierre por día.
- Consultar gastos por día.
- Consultar inventario actual.
- Consultar movimientos de inventario.
- Consultar depósito e historial.
- Consultar transferencias pendientes o rechazadas.
- Consultar auditoría según rol.

### Validaciones antes de avanzar

- Vendedor solo ve información operativa permitida.
- Administrador ve consultas operativas y administrativas.
- Gerente tiene visibilidad completa.
- Las consultas no modifican información.

### Documentación del módulo

Crear:

```text
docs/modules/consultas-operativas.md
```

### Rama y PR

Rama:

```text
feature/consultas-operativas
```

Título PR:

```text
feat: implementar consultas operativas del sistema
```

---

## 3.13. Regla de Pull Request para cada módulo backend

Cada módulo debe tener su propia PR. No mezclar varios módulos grandes en una sola PR.

Plantilla recomendada:

```markdown
## Objetivo

Describir qué módulo se implementa y qué problema resuelve.

## Alcance

- Funcionalidad 1.
- Funcionalidad 2.
- Funcionalidad 3.

## Cambios técnicos

- Entidades creadas o utilizadas.
- Repositorios creados.
- Servicios creados.
- Controladores creados.
- Validaciones implementadas.
- Manejo de errores agregado.

## Reglas de negocio implementadas

- Regla 1.
- Regla 2.
- Regla 3.

## Pruebas realizadas

- Prueba manual con endpoint.
- Prueba de error esperado.
- Prueba de permisos.
- Prueba de persistencia en base de datos.

## Documentación actualizada

- `docs/modules/[nombre-modulo].md`
- Otros archivos modificados.

## Variables de entorno nuevas

- Ninguna / Detallar.

## Riesgos o pendientes

- Pendiente 1.
- Pendiente 2.
```

Buenas prácticas para el aprendiz:

- Trabajar módulos pequeños y verificables.
- Probar primero el caso exitoso.
- Probar después errores esperados.
- Probar permisos por rol.
- Confirmar que la base de datos refleja correctamente la operación.
- Actualizar documentación antes de abrir la PR.
- No avanzar al siguiente módulo si el anterior no compila o no está probado.

---
