import { apiClient } from "../../../shared/services/apiClient";
import type { AbrirCajaDiariaRequest, CajaDiaria } from "../types";

function jsonBody<T>(payload: T) {
  return JSON.stringify(payload);
}

export function obtenerCajaAbierta(token: string) {
  return apiClient.get<CajaDiaria>("/cajas-diarias/abierta", { token });
}

export function abrirCajaDiaria(request: AbrirCajaDiariaRequest, token: string) {
  return apiClient.post<CajaDiaria>("/cajas-diarias", jsonBody(request), { token });
}
