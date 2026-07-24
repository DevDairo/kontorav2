# Flujo de autenticacion frontend

## Estado actual

La autenticacion frontend queda implementada sobre la rama `chore/inicializacion-frontend` por decision operativa del proyecto.

Estado de cierre:

- Validacion tecnica realizada por Codex.
- Verificacion visual y funcional final en navegador pendiente de confirmacion manual del usuario.
- No se debe marcar el modulo como completado en `docs/AVANCE_PROYECTO.md` hasta recibir esa confirmacion manual.

## Contrato backend existente

Fuente: `docs/modules/usuarios-sesiones.md`.

| Metodo | Ruta | Uso |
| :- | :- | :- |
| POST | `/api/auth/login` | Iniciar sesion con `nombreUsuario` y `contrasena`. |
| GET | `/api/auth/me` | Consultar usuario autenticado. |
| POST | `/api/auth/logout` | Cerrar la sesion actual. |

## Flujo previsto

1. El usuario ingresa `nombreUsuario` y `contrasena`.
2. El frontend envia `POST /api/auth/login`.
3. El backend valida usuario activo, credencial activa y contrasena BCrypt.
4. El backend devuelve el JWT de sesion.
5. El frontend guarda el token de forma controlada en `sessionStorage`.
6. Las llamadas protegidas envian `Authorization: Bearer <token>`.
7. Al iniciar la app, el frontend consulta `GET /api/auth/me` para reconstruir el estado autenticado.
8. Al cerrar sesion, el frontend llama `POST /api/auth/logout` y descarta el token local.

## Implementacion frontend

Archivos principales:

- `frontend/src/modules/auth/context/AuthContext.tsx`
- `frontend/src/modules/auth/components/LoginPage.tsx`
- `frontend/src/modules/auth/services/authService.ts`
- `frontend/src/modules/auth/utils/tokenStorage.ts`
- `frontend/src/App.tsx`
- `frontend/src/shared/components/AppShell.tsx`

Responsabilidades:

- `AuthProvider` centraliza token, usuario autenticado, login, logout y reconstruccion de sesion.
- `LoginPage` muestra el formulario y consume `POST /api/auth/login`.
- `authService` encapsula llamadas reales a `/auth/login`, `/auth/me` y `/auth/logout`.
- `tokenStorage` limita el almacenamiento del JWT a `sessionStorage`.
- `App.tsx` protege la aplicacion: sin sesion valida redirige a `/login`; con sesion valida muestra la shell operativa.
- `AppShell` muestra usuario, rol, navegacion responsive y accion de cierre de sesion.

## Reglas que no debe duplicar el frontend

- Validacion definitiva de credenciales.
- Estado real del usuario.
- Estado real de la sesion persistida.
- Permisos finales por rol.
- Expiracion y revocacion del token.

El frontend puede validar campos vacios y formato basico para mejorar la experiencia, pero la decision final queda en backend.

## Rutas protegidas

La pantalla principal queda protegida por el estado confirmado desde backend:

- Sin token local: se muestra `/login`.
- Con token local: se consulta `GET /api/auth/me`.
- Si `/auth/me` responde correctamente: se muestra la shell principal.
- Si `/auth/me` responde `401` u otro error: se limpia el token local y se vuelve a `/login`.
- Al ejecutar logout: se llama `POST /api/auth/logout`, se descarta el token local y se vuelve a `/login`.

## Validaciones tecnicas realizadas

- `npm run build` en `frontend/`: exitoso.
- `GET http://localhost:8080/api/health`: exitoso.
- `POST /api/auth/login` con fixture local existente `test_auth_activo`: exitoso.
- `GET /api/auth/me` con token real: exitoso.
- `POST /api/auth/logout` con token real: exitoso.
- Validacion en navegador integrado:
  - Login muestra `Iniciar sesion`.
  - Login redirige a `/`.
  - Pantalla protegida muestra usuario, rol, navegacion y boton de logout.
  - Logout vuelve a `/login`.
  - Consola sin errores durante login/logout.

## Pendiente de cierre

- Confirmacion manual del usuario en navegador.
- Correccion y documentacion de cualquier error de consola que el usuario observe durante esa verificacion.
