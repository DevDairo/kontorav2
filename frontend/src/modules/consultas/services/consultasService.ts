import { apiClient } from "../../../shared/services/apiClient";
import type {
  ConsultaCierreDiario,
  ConsultaGastoCaja,
  ConsultaInventarioActual,
  ConsultaMovimientoDeposito,
  ConsultaMovimientoInventario,
  ConsultaVenta,
  ConsultaVentasVasos,
  FiltroPeriodo,
} from "../types";

function queryPeriodo(filtro: FiltroPeriodo) {
  const params = new URLSearchParams({ fechaInicio: filtro.fechaInicio });

  if (filtro.fechaFin) {
    params.set("fechaFin", filtro.fechaFin);
  }

  return `?${params.toString()}`;
}

function queryPeriodoOpcional(filtro: Partial<FiltroPeriodo>) {
  const params = new URLSearchParams();

  if (filtro.fechaInicio) {
    params.set("fechaInicio", filtro.fechaInicio);
  }
  if (filtro.fechaFin) {
    params.set("fechaFin", filtro.fechaFin);
  }

  const query = params.toString();
  return query ? `?${query}` : "";
}

export function consultarVentas(token: string, filtro: FiltroPeriodo) {
  return apiClient.get<ConsultaVenta[]>(`/consultas/ventas${queryPeriodo(filtro)}`, { token });
}

export function consultarGastos(token: string, filtro: FiltroPeriodo) {
  return apiClient.get<ConsultaGastoCaja[]>(`/consultas/gastos${queryPeriodo(filtro)}`, { token });
}

export function consultarInventarioActual(token: string) {
  return apiClient.get<ConsultaInventarioActual[]>("/consultas/inventario/actual", { token });
}

export function consultarMovimientosInventario(token: string, filtro: Partial<FiltroPeriodo>) {
  return apiClient.get<ConsultaMovimientoInventario[]>(
    `/consultas/inventario/movimientos${queryPeriodoOpcional(filtro)}`,
    { token },
  );
}

export function consultarVentasVasos(token: string, filtro: FiltroPeriodo) {
  return apiClient.get<ConsultaVentasVasos[]>(
    `/consultas/inventario/ventas-vasos${queryPeriodo(filtro)}`,
    { token },
  );
}

export function consultarCierrePorFecha(token: string, fecha: string) {
  return apiClient.get<ConsultaCierreDiario>(`/consultas/cierre?fecha=${encodeURIComponent(fecha)}`, { token });
}

export function consultarMovimientosDeposito(token: string, filtro: Partial<FiltroPeriodo>) {
  return apiClient.get<ConsultaMovimientoDeposito[]>(
    `/consultas/deposito/movimientos${queryPeriodoOpcional(filtro)}`,
    { token },
  );
}
