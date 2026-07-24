import { apiClient } from "../../../shared/services/apiClient";
import type { LoginRequest, LoginResponse, UsuarioAutenticado } from "../types";

function jsonBody<T>(payload: T) {
  return JSON.stringify(payload);
}

export function login(request: LoginRequest) {
  return apiClient.post<LoginResponse>("/auth/login", jsonBody(request));
}

export function getCurrentUser(token: string) {
  return apiClient.get<UsuarioAutenticado>("/auth/me", { token });
}

export function logout(token: string) {
  return apiClient.post<null>("/auth/logout", undefined, { token });
}
