import { apiClient } from "../../../shared/services/apiClient";
import type {
  AnularCortesiaRequest,
  Cortesia,
  RegistrarCortesiaRequest,
} from "../types";

export function registrarCortesia(token: string, request: RegistrarCortesiaRequest) {
  return apiClient.post<Cortesia>("/cortesias", JSON.stringify(request), { token });
}

export function obtenerCortesiasCajaAbierta(token: string) {
  return apiClient.get<Cortesia[]>("/cortesias/caja-abierta", { token });
}

export function obtenerCortesiasPeriodo(
  token: string,
  filters: { fechaInicio: string; fechaFin?: string },
) {
  const query = new URLSearchParams({ fechaInicio: filters.fechaInicio });
  if (filters.fechaFin) {
    query.set("fechaFin", filters.fechaFin);
  }
  return apiClient.get<Cortesia[]>(`/cortesias?${query.toString()}`, { token });
}

export function anularCortesia(
  token: string,
  idCortesia: string,
  request: AnularCortesiaRequest,
) {
  return apiClient.post<Cortesia>(
    `/cortesias/${encodeURIComponent(idCortesia)}/anular`,
    JSON.stringify(request),
    { token },
  );
}
