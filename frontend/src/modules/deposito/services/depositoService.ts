import { apiClient } from "../../../shared/services/apiClient";
import type {
  ConsignacionBancaria,
  PagoServicio,
  RegistrarConsignacionBancariaRequest,
  RegistrarPagoServicioRequest,
  SaldoDeposito,
} from "../types";

function jsonBody<T>(payload: T) {
  return JSON.stringify(payload);
}

export function obtenerSaldoDeposito(token: string) {
  return apiClient.get<SaldoDeposito>("/deposito/saldo", { token });
}

export function registrarConsignacionBancaria(token: string, request: RegistrarConsignacionBancariaRequest) {
  return apiClient.post<ConsignacionBancaria>("/deposito/consignaciones-bancarias", jsonBody(request), { token });
}

export function registrarPagoServicio(token: string, request: RegistrarPagoServicioRequest) {
  return apiClient.post<PagoServicio>("/deposito/pagos-servicios", jsonBody(request), { token });
}
