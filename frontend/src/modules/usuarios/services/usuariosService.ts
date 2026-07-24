import { apiClient } from "../../../shared/services/apiClient";
import type {
  ActualizarUsuarioRequest,
  CrearUsuarioRequest,
  EstadoUsuario,
  RolGestion,
  RestablecerContrasenaUsuarioRequest,
  UsuarioGestion,
} from "../types";

export function listarUsuarios(token: string) {
  return apiClient.get<UsuarioGestion[]>("/usuarios", { token });
}

export function listarRolesGestion(token: string) {
  return apiClient.get<RolGestion[]>("/usuarios/roles", { token });
}

export function crearUsuario(token: string, request: CrearUsuarioRequest) {
  return apiClient.post<UsuarioGestion>("/usuarios", JSON.stringify(request), { token });
}

export function actualizarUsuario(token: string, idUsuario: string, request: ActualizarUsuarioRequest) {
  return apiClient.put<UsuarioGestion>(`/usuarios/${idUsuario}`, JSON.stringify(request), { token });
}

export function actualizarEstadoUsuario(token: string, idUsuario: string, estado: EstadoUsuario) {
  return apiClient.put<UsuarioGestion>(`/usuarios/${idUsuario}/estado`, JSON.stringify({ estado }), { token });
}

export function restablecerContrasenaUsuario(
  token: string,
  idUsuario: string,
  request: RestablecerContrasenaUsuarioRequest,
) {
  return apiClient.put<void>(`/usuarios/${idUsuario}/contrasena`, JSON.stringify(request), { token });
}
