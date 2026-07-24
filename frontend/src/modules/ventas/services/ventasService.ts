import { apiClient } from "../../../shared/services/apiClient";
import type { AnularVentaRequest, RegistrarVentaRequest, TrabajadorVenta, VentaResponse } from "../types";

export function listarTrabajadoresVenta(token: string) {
  return apiClient.get<TrabajadorVenta[]>("/ventas/trabajadores", { token });
}

export function registrarVenta(token: string, request: RegistrarVentaRequest) {
  return apiClient.post<VentaResponse>("/ventas", JSON.stringify(request), { token });
}

export function consultarVentaParaAnulacion(token: string, idVenta: string) {
  return apiClient.get<VentaResponse>(`/ventas/${idVenta}/anulacion`, { token });
}

export function anularVenta(token: string, idVenta: string, request: AnularVentaRequest) {
  return apiClient.post<VentaResponse>(`/ventas/${idVenta}/anular`, JSON.stringify(request), { token });
}
