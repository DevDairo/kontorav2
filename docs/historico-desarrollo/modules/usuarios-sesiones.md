# Modulo: Seguridad, usuarios y sesiones

## Objetivo

Implementar autenticacion propia para usuarios internos de Kontora POS usando `nombre_usuario`, contrasena hasheada, JWT y sesiones persistidas.

## Tablas involucradas

- `roles`
- `usuarios`
- `credenciales_usuario`
- `sesiones_usuario`

## Fuente de verdad del modelo

El modulo se implementa sobre las tablas reales del schema:

- `usuarios.nombre_usuario` es el identificador de login.
- `credenciales_usuario.contrasena_hash` guarda BCrypt, nunca texto plano.
- `sesiones_usuario.token_identificador` guarda el identificador `jti` del JWT, no el token completo.
- `sesiones_usuario.estado_sesion` controla si un token sigue activo.

## Endpoints

| Metodo | Ruta | Autenticacion | Proposito |
| :- | :- | :- | :- |
| POST | `/api/auth/login` | No | Iniciar sesion con `nombreUsuario` y `contrasena`. |
| GET | `/api/auth/me` | Si | Consultar usuario autenticado. |
| POST | `/api/auth/logout` | Si | Cerrar la sesion actual. |
| GET | `/api/usuarios` | Gerente | Consultar usuarios registrados. |
| GET | `/api/usuarios/roles` | Gerente | Consultar roles activos asignables. |
| POST | `/api/usuarios` | Gerente | Crear un usuario con su credencial inicial. |
| PUT | `/api/usuarios/{idUsuario}` | Gerente | Editar nombre, usuario y rol. |
| PUT | `/api/usuarios/{idUsuario}/estado` | Gerente | Activar, inactivar o bloquear sin eliminar historial. |

## Reglas de negocio implementadas

- Solo usuarios con `usuarios.estado = 'activo'` pueden iniciar sesion.
- Solo credenciales con `credenciales_usuario.estado = 'activa'` permiten login.
- La contrasena se valida con BCrypt.
- Al iniciar sesion se crea un registro en `sesiones_usuario`.
- El JWT contiene `jti`, pero la base de datos no guarda el token completo.
- Para acceder a endpoints protegidos, el token debe estar firmado, no expirado y tener una sesion `activa`.
- Al cerrar sesion, `sesiones_usuario.estado_sesion` pasa a `cerrada` y se registra `fecha_cierre`.
- Luego del logout, el mismo token ya no autoriza endpoints protegidos.
- Solo el rol `gerente` puede gestionar usuarios; `administrador` y `vendedor` reciben `403`.
- La creacion acepta los roles activos `vendedor`, `administrador` y `gerente`; el nombre de usuario es alfanumerico de 3 a 50 caracteres.
- La contrasena inicial se almacena con BCrypt, nunca se devuelve al cliente y marca la credencial con `requiere_cambio_contrasena = true`.
- No existe eliminacion de usuarios. Los estados `activo`, `inactivo` y `bloqueado` conservan el historial operativo.
- Un gerente no puede inactivar ni bloquear su propio usuario.
- Crear, editar y cambiar estado genera auditoria sobre `usuarios`, con snapshots sin contrasena.

## Estados usados

- Usuario: `activo`, `inactivo`, `bloqueado`.
- Credencial: `activa`, `bloqueada`, `expirada`, `revocada`.
- Sesion: `activa`, `cerrada`, `expirada`, `revocada`.

## Validaciones realizadas

- `mvn clean test`.
- Login exitoso con usuario activo.
- Creacion de sesion activa en `sesiones_usuario`.
- Consulta de usuario autenticado con `GET /api/auth/me`.
- Logout de sesion actual.
- Rechazo del token despues del logout.
- Rechazo de login para usuario bloqueado.
- `GestionUsuariosIntegrationTest`: gerente crea, edita y bloquea un usuario; su token deja de autorizar y se conservan tres eventos de auditoria.
- `GestionUsuariosIntegrationTest`: administrador y vendedor reciben `403` al gestionar usuarios; gerente consulta usuarios y roles activos.
- `BootstrapManagerInitializerTest`: el gerente inicial se crea solo con la tabla `usuarios` vacia y no altera instalaciones existentes.
- `npm run build` en `frontend/`: exitoso.
- Validacion manual del usuario: panel `/usuarios` revisado correctamente con rol gerente.

## Alcance cerrado

- RF-03, RF-04 y RF-05 quedan cubiertos para la gestion gerencial de usuarios conforme a CU-GER-01.
- La pantalla `/usuarios` queda como `Base lista` y solo se muestra al gerente; el backend conserva la autorizacion final.
- El cambio o restablecimiento de contrasena no pertenece a esta interfaz porque no existe aun un flujo ni endpoint dedicado. Se tratara como un modulo posterior sin modificar las reglas actuales.

## Provision inicial para despliegue

- El backend puede crear `gerenteLocal` al primer arranque de una base vacia cuando `BOOTSTRAP_MANAGER_ENABLED=true` y las variables `BOOTSTRAP_MANAGER_*` estan definidas en `infra/.env`.
- La configuracion de la maquina virtual se documenta en `docs/deployment/guia-despliegue-poc.md`; las contrasenas reales no se versionan.

## Actualizaciones posteriores

- En el modulo "Auditoria transversal" se implemento auditoria explicita de login/logout sobre `auditoria_operaciones`.

