# 12. Usuarios

## Objetivo

Administrar usuarios, roles, estados de acceso y restablecimientos de contrasena sin perder trazabilidad operativa.

## Requisitos cubiertos

- RF-03 a RF-05.

## Funcionalidades

- Directorio de usuarios y roles activos.
- Creacion y edicion de usuarios.
- Cambio de estado a activo, inactivo o bloqueado.
- Restablecimiento de contrasena por el gerente.
- Proteccion para impedir que el gerente bloquee o inhabilite su propia cuenta.
- Auditoria de cambios relevantes sin exponer contrasenas.

## Permisos

| Rol | Funcionalidad |
| --- | --- |
| Vendedor | No recibe interfaz de Usuarios. |
| Administrador | No recibe interfaz ni permisos de gestion de usuarios. |
| Gerente | Gestiona usuarios, roles, estados y contrasenas. |

## Reglas clave

- Los usuarios no se eliminan fisicamente.
- Las contrasenas se almacenan con BCrypt y nunca se devuelven al cliente.
- Usuarios inactivos o bloqueados conservan su historial y no pueden operar.
- El gerente inicial se crea solo sobre una instalacion sin usuarios, si el bootstrap esta configurado.

## Endpoints principales

- `GET /api/usuarios`
- `GET /api/usuarios/roles`
- `POST /api/usuarios`
- `PUT /api/usuarios/{idUsuario}`
- `PUT /api/usuarios/{idUsuario}/estado`
- `PUT /api/usuarios/{idUsuario}/contrasena`
