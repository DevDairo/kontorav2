import { apiClient } from "../../../shared/services/apiClient";
import type { CerrarCajaRequest, CierreCaja, ConsultaCierreDiario, ResumenCajaDiaria } from "../types";

function jsonBody<T>(payload: T) {
  return JSON.stringify(payload);
}

export function obtenerResumenCajaAbierta(token: string) {
  return apiClient.get<ResumenCajaDiaria>("/cajas-diarias/abierta/resumen", { token });
}

export function cerrarCajaDiaria(token: string, idCajaDiaria: string, request: CerrarCajaRequest) {
  return apiClient.post<CierreCaja>(`/cajas-diarias/${idCajaDiaria}/cerrar`, jsonBody(request), { token });
}

export function obtenerCierreCaja(token: string, idCajaDiaria: string) {
  return apiClient.get<CierreCaja>(`/cajas-diarias/${idCajaDiaria}/cierre`, { token });
}

export function consultarCierrePorFecha(token: string, fechaOperacion: string) {
  return apiClient.get<ConsultaCierreDiario>(`/consultas/cierre?fecha=${encodeURIComponent(fechaOperacion)}`, { token });
}
