import { ApiClientError, apiClient } from "../../../shared/services/apiClient";
import type {
  AdicionDiaria,
  AnularGastoCajaRequest,
  EditarGastoCajaRequest,
  GastoCaja,
  GastosSnapshot,
  PagoTrabajadoresDiario,
  RegistrarAdicionDiariaRequest,
  RegistrarGastoCajaRequest,
  RegistrarPagoTrabajadoresDiarioRequest,
  ResumenCajaDiaria,
} from "../types";

function jsonBody<T>(payload: T) {
  return JSON.stringify(payload);
}

export function obtenerAdicionDiariaCajaAbierta(token: string) {
  return apiClient.get<AdicionDiaria | null>("/operaciones-caja/adiciones-diarias/abierta", { token });
}

export function registrarAdicionDiaria(token: string, request: RegistrarAdicionDiariaRequest) {
  return apiClient.post<AdicionDiaria>("/operaciones-caja/adiciones-diarias", jsonBody(request), { token });
}

export function obtenerPagoTrabajadoresCajaAbierta(token: string) {
  return apiClient.get<PagoTrabajadoresDiario>("/operaciones-caja/pagos-trabajadores-diarios/abierta", { token });
}

export function registrarPagoTrabajadores(token: string, request: RegistrarPagoTrabajadoresDiarioRequest) {
  return apiClient.post<PagoTrabajadoresDiario>(
    "/operaciones-caja/pagos-trabajadores-diarios",
    jsonBody(request),
    { token },
  );
}

export function confirmarPagoTrabajadores(token: string, idPagoTrabajadoresDiario: string) {
  return apiClient.post<PagoTrabajadoresDiario>(
    `/operaciones-caja/pagos-trabajadores-diarios/${idPagoTrabajadoresDiario}/confirmar`,
    undefined,
    { token },
  );
}

export function listarGastosCajaAbierta(token: string) {
  return apiClient.get<GastoCaja[]>("/operaciones-caja/gastos-caja/abierta", { token });
}

export function registrarGastoCaja(token: string, request: RegistrarGastoCajaRequest) {
  return apiClient.post<GastoCaja>("/operaciones-caja/gastos-caja", jsonBody(request), { token });
}

export function editarGastoCaja(token: string, idGastoCaja: string, request: EditarGastoCajaRequest) {
  return apiClient.put<GastoCaja>(`/operaciones-caja/gastos-caja/${idGastoCaja}`, jsonBody(request), { token });
}

export function anularGastoCaja(token: string, idGastoCaja: string, request: AnularGastoCajaRequest) {
  return apiClient.post<GastoCaja>(
    `/operaciones-caja/gastos-caja/${idGastoCaja}/anular`,
    jsonBody(request),
    { token },
  );
}

export function obtenerResumenCajaAbierta(token: string) {
  return apiClient.get<ResumenCajaDiaria>("/cajas-diarias/abierta/resumen", { token });
}

async function optionalRecord<T>(request: () => Promise<T>) {
  try {
    return await request();
  } catch (error) {
    if (error instanceof ApiClientError && error.status === 404) {
      return null;
    }

    throw error;
  }
}

export async function obtenerGastosSnapshot(token: string, includeAdministrativeData: boolean): Promise<GastosSnapshot> {
  const gastos = await listarGastosCajaAbierta(token);

  if (!includeAdministrativeData) {
    return { adicion: null, gastos, pagoTrabajadores: null, resumenCaja: null };
  }

  const [adicion, pagoTrabajadores, resumenCaja] = await Promise.all([
    obtenerAdicionDiariaCajaAbierta(token),
    optionalRecord(() => obtenerPagoTrabajadoresCajaAbierta(token)),
    obtenerResumenCajaAbierta(token),
  ]);

  return { adicion, gastos, pagoTrabajadores, resumenCaja };
}
