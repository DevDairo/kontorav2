# 01. Login y sesion

## Objetivo

Permitir el acceso autenticado al POS, mantener la sesion mediante JWT y limitar las rutas visibles segun el rol autenticado.

## Requisitos cubiertos

- RF-01 a RF-05.

## Funcionalidades

- Inicio de sesion con `nombreUsuario` y contrasena.
- Consulta de sesion actual, cierre de sesion e invalidacion del token en backend.
- Navegacion por rol: `vendedor`, `administrador` y `gerente`.
- Inicio personalizado por rol, con accesos de jornada expresados en lenguaje operativo.
- Consulta del estado real de la caja abierta para informar si la jornada esta abierta o pendiente de apertura.
- Accesos directos a las herramientas autorizadas y listado separado de herramientas adicionales.
- Registro de login y logout en auditoria.
- Gestion de usuarios separada y exclusiva del gerente.

## Permisos

| Rol | Acceso |
| --- | --- |
| Vendedor | Inicia y cierra sesion; usa las rutas operativas autorizadas. |
| Administrador | Inicia y cierra sesion; usa las rutas administrativas autorizadas. |
| Gerente | Inicia y cierra sesion; posee visibilidad total y gestion de usuarios. |

## Inicio autenticado por rol

La ruta `/` recibe al usuario por su nombre y presenta acciones acordes a su responsabilidad. No muestra nombres de servicios, rutas HTTP, endpoints ni estados tecnicos.

| Rol | Accesos principales | Acciones de jornada |
| --- | --- | --- |
| Vendedor | Nueva venta, jornada, registrar gasto y transferencias. | Registrar ventas, gastos y consultar sus registros. |
| Administrador | Jornada, ventas, inventario y productos/precios. | Gestionar caja, inventario y gastos. |
| Gerente | Jornada, transferencias, cierre de caja y seguimiento. | Revisar transferencias, cierre y deposito. |

El bloque **Estado de caja** consulta la caja abierta de la jornada. Cuando existe, informa la fecha operativa y el valor base; cuando no existe, adapta el mensaje al rol y dirige a la ruta `/caja`. Esta consulta no concede permisos: la autorizacion de apertura, consulta o cualquier accion posterior permanece en el backend.

Las herramientas no destacadas se muestran como **Herramientas disponibles**. El conjunto se deriva de las rutas visibles para el rol, por lo que un vendedor no ve Inventario, Catalogos, Usuarios ni Auditoria. Evidencias ya no es una ruta independiente: se integra en Consultas y el backend limita al vendedor a sus propios gastos.

### Diseno responsive

- Cuatro accesos principales en escritorio, dos en tableta y uno en movil.
- Acciones de jornada con altura uniforme y cambio a una columna en movil.
- Botones de cabecera apilables y menu lateral movil existentes conservados.
- Los accesos son botones de navegacion; no reemplazan formularios ni reglas de los modulos destino.

## Endpoints principales

- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `GET /api/health`
- `GET /api/cajas-diarias/abierta` para el estado de jornada en Inicio.

## Implementacion de interfaz

- `frontend/src/shared/components/RoleHome.tsx`: contenido, accesos y estado de jornada por rol.
- `frontend/src/shared/components/ModuleOverview.tsx`: herramientas adicionales con texto operativo.
- `frontend/src/App.tsx`: integra Inicio autenticado con token, usuario y rutas visibles.
- `frontend/src/index.css`: grillas uniformes y reglas responsive del Inicio.

La interfaz usa datos reales de caja cuando estan disponibles y evita cifras de ejemplo o indicadores tecnicos.

## Validacion del cambio de Inicio

- `npm run build` ejecutado el 2026-07-13: exitoso.
- `/login` carga sin errores de consola con frontend y backend locales activos.
- La confirmacion manual del usuario habilita la documentacion de este ajuste; se deben conservar las pruebas de acceso con vendedor, administrador y gerente en futuras modificaciones de Inicio.

## Gerente inicial para una instalacion nueva

El backend puede crear el primer usuario automaticamente solo si la tabla `usuarios` esta vacia. En `infra/.env` se debe definir antes del primer arranque:

```env
BOOTSTRAP_MANAGER_ENABLED=true
BOOTSTRAP_MANAGER_USERNAME=gerenteLocal
BOOTSTRAP_MANAGER_FULL_NAME=Gerente Local
BOOTSTRAP_MANAGER_PASSWORD=<contrasena-segura>
```

La comprobacion aislada del 2026-07-12 confirmo que una base vacia crea un unico usuario `gerenteLocal`, con rol `gerente`, usuario y credencial activos, y permite iniciar sesion. En arranques posteriores no modifica usuarios existentes. Tras el primer inicio se recomienda cambiar `BOOTSTRAP_MANAGER_ENABLED=false`.
