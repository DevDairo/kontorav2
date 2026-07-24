import { apiClient } from "../../../shared/services/apiClient";
import type { PagoVentaResponse } from "../../ventas/types";
import type { ConsultaTransferencia, DecidirTransferenciaRequest, FiltroTransferencias } from "../types";

function construirQuery(filtro: FiltroTransferencias) {
  const params = new URLSearchParams({ estadoValidacion: filtro.estadoValidacion });

  if (filtro.fechaInicio) {
    params.set("fechaInicio", filtro.fechaInicio);
  }
  if (filtro.fechaFin) {
    params.set("fechaFin", filtro.fechaFin);
  }

  return `?${params.toString()}`;
}

export function consultarTransferencias(token: string, filtro: FiltroTransferencias) {
  return apiClient.get<ConsultaTransferencia[]>(
    `/consultas/transferencias${construirQuery(filtro)}`,
    { token },
  );
}

export function validarTransferencia(token: string, idPagoVenta: string, request: DecidirTransferenciaRequest) {
  return apiClient.post<PagoVentaResponse>(
    `/pagos-venta/${encodeURIComponent(idPagoVenta)}/validar`,
    JSON.stringify(request),
    { token },
  );
}

export function rechazarTransferencia(token: string, idPagoVenta: string, request: DecidirTransferenciaRequest) {
  return apiClient.post<PagoVentaResponse>(
    `/pagos-venta/${encodeURIComponent(idPagoVenta)}/rechazar`,
    JSON.stringify(request),
    { token },
  );
}
