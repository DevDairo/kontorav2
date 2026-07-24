export type EstadoUsuario = "activo" | "inactivo" | "bloqueado";

export type RolGestion = {
  idRol: string;
  nombreRol: string;
};

export type UsuarioGestion = {
  estado: EstadoUsuario;
  fechaActualizacion: string;
  fechaCreacion: string;
  idRol: string;
  idUsuario: string;
  nombreCompleto: string;
  nombreRol: string;
  nombreUsuario: string;
};

export type CrearUsuarioRequest = {
  contrasena: string;
  nombreCompleto: string;
  nombreRol: string;
  nombreUsuario: string;
};

export type ActualizarUsuarioRequest = Omit<CrearUsuarioRequest, "contrasena">;

export type RestablecerContrasenaUsuarioRequest = {
  nuevaContrasena: string;
};
