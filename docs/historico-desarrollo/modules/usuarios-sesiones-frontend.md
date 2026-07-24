# Modulo: Autenticacion, sesion y gestion de usuarios frontend

## Objetivo

Permitir que usuarios internos ingresen a Kontora POS usando la autenticacion real del backend y proteger la shell principal hasta confirmar la sesion con `/api/auth/me`.

## Actor principal

Vendedor / Administrador / Gerente.

## Endpoints consumidos

- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/logout`

## Campos del formulario

- `nombreUsuario`
- `contrasena`

## Contrato usado

Request de login:

```json
{
  "nombreUsuario": "usuario",
  "contrasena": "contrasena"
}
```

Respuesta esperada de login:

```json
{
  "token": "jwt",
  "tipoToken": "Bearer",
  "expiraEnMinutos": 480,
  "fechaExpiracion": "2026-07-07T00:00:00Z",
  "idUsuario": "uuid",
  "nombreUsuario": "usuario",
  "nombreCompleto": "Nombre Usuario",
  "nombreRol": "vendedor",
  "requiereCambioContrasena": false
}
```

Respuesta esperada de `/auth/me`:

```json
{
  "idUsuario": "uuid",
  "nombreUsuario": "usuario",
  "nombreCompleto": "Nombre Usuario",
  "nombreRol": "vendedor"
}
```

## Validaciones de interfaz

- `nombreUsuario` es obligatorio antes de enviar.
- `contrasena` es obligatoria antes de enviar.
- El frontend no decide si una credencial es valida; esa respuesta viene del backend.
- Si el backend devuelve error con `mensaje`, se muestra en el formulario.
- Si no hay token local, se muestra `/login`.
- Si hay token local, se consulta `GET /api/auth/me` antes de mostrar la shell principal.
- Si `/auth/me` rechaza el token, se limpia la sesion local.
- El token se almacena en `sessionStorage`, no en estado global suelto.
- En escritorio, el panel decorativo derecho usa un patron diagonal sobre fondo `#f5f8fc`; no representa datos ni contratos y se oculta en movil.

## Respuestas esperadas

- Login exitoso: se guarda token y se muestra la shell correspondiente al usuario y rol.
- Login fallido: se conserva la pantalla de login y se muestra el mensaje del backend.
- Token invalido o expirado: se limpia el token local y se vuelve a `/login`.
- Logout exitoso: el backend cierra la sesion y el frontend limpia token local.
- Logout con token ya invalido: el frontend limpia token local igualmente.

## Evidencia de prueba

- Build frontend:

```powershell
cd C:\Users\corre\Desktop\Kontora\frontend
npm run build
```

Resultado: exitoso.

- Validacion API local:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/health" | ConvertTo-Json -Compress
```

Resultado:

```json
{"status":"ok","service":"kontora-pos-backend"}
```

- Validacion auth contra backend real con fixture local existente:

```text
POST /api/auth/login -> ok
GET /api/auth/me -> usuario test_auth_activo, rol vendedor
POST /api/auth/logout -> ok
```

- Validacion en navegador integrado:

```text
/login muestra Iniciar sesion
Login redirige a /
/ muestra Sesion activa
La topbar muestra usuario y rol
Logout vuelve a /login
Consola sin errores durante el flujo
```

## Pantalla: Gestion de usuarios

### Objetivo

Permitir que el gerente cree, edite, asigne roles y cambie el estado de acceso de los usuarios sin eliminar su historial.

### Actor principal

Gerente.

### Endpoints consumidos

- `GET /api/usuarios`.
- `GET /api/usuarios/roles`.
- `POST /api/usuarios`.
- `PUT /api/usuarios/{idUsuario}`.
- `PUT /api/usuarios/{idUsuario}/estado`.

### Controles y reglas de interfaz

- El directorio permite buscar por nombre, usuario o rol y filtrar por estado.
- El formulario crea usuarios con nombre completo, nombre de usuario alfanumerico, rol, contrasena inicial y confirmacion.
- La edicion permite cambiar nombre completo, usuario y rol; nunca muestra ni reenvia el hash de contrasena.
- Activar, inactivar y bloquear requieren confirmacion previa. La propia cuenta gerente no puede inactivarse ni bloquearse desde la interfaz.
- La navegacion muestra `/usuarios` solo al gerente. El backend sigue validando el rol en todos los endpoints.
- La interfaz no ofrece eliminar usuarios porque RF-04 exige conservar el historial operativo.

### Archivos principales

```text
frontend/src/modules/usuarios/components/UsuariosPanel.tsx
frontend/src/modules/usuarios/services/usuariosService.ts
frontend/src/modules/usuarios/types.ts
frontend/src/modules/usuarios/index.ts
frontend/src/app/routes/appRoutes.ts
```

### Evidencia de prueba

- `npm run build`: exitoso.
- Pruebas backend: `GestionUsuariosIntegrationTest` y `BootstrapManagerInitializerTest`, sin fallos.
- El usuario confirmo manualmente que el apartado de usuarios funciona correctamente con gerente.
- La ruta queda marcada como `Base lista`.

## Validacion manual confirmada

El usuario confirmo el funcionamiento del acceso y de la gestion de usuarios en navegador. El modulo queda cerrado para RF-03, RF-04 y RF-05; un futuro flujo de cambio o restablecimiento de contrasena queda fuera de este alcance.
