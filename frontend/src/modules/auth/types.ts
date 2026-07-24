export type LoginRequest = {
  nombreUsuario: string;
  contrasena: string;
};

export type UsuarioAutenticado = {
  idUsuario: string;
  nombreUsuario: string;
  nombreCompleto: string;
  nombreRol: string;
};

export type LoginResponse = UsuarioAutenticado & {
  token: string;
  tipoToken: string;
  expiraEnMinutos: number;
  fechaExpiracion: string;
  requiereCambioContrasena: boolean;
};

export type AuthStatus = "checking" | "authenticated" | "unauthenticated";
