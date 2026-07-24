import { apiClient } from "../../../shared/services/apiClient";
import type { ConsultaAuditoria, FiltroAuditoria } from "../types";

function queryAuditoria(filtro: FiltroAuditoria) {
  const params = new URLSearchParams({ fechaInicio: filtro.fechaInicio });

  if (filtro.fechaFin) {
    params.set("fechaFin", filtro.fechaFin);
  }
  if (filtro.tablaAfectada) {
    params.set("tablaAfectada", filtro.tablaAfectada);
  }
  if (filtro.accion) {
    params.set("accion", filtro.accion);
  }

  return `?${params.toString()}`;
}

export function consultarAuditoria(token: string, filtro: FiltroAuditoria) {
  return apiClient.get<ConsultaAuditoria[]>(`/consultas/auditoria${queryAuditoria(filtro)}`, { token });
}
