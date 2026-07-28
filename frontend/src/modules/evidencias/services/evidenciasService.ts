import { ApiClientError, apiClient } from "../../../shared/services/apiClient";
import type { ArchivoEvidenciaResponse } from "../types";
import { prepararArchivoEvidencia } from "../utils/prepararArchivoEvidencia";

export function messageForEvidenceDownload(error: unknown) {
  if (error instanceof ApiClientError && (error.status === 401 || error.status === 404)) {
    return "La evidencia solicitada no esta disponible para descargar.";
  }

  if (error instanceof ApiClientError) {
    return error.message;
  }

  return error instanceof Error ? error.message : "No fue posible descargar la evidencia.";
}

async function crearFormularioEvidencia(archivo: File) {
  const formData = new FormData();
  formData.append("archivo", await prepararArchivoEvidencia(archivo));
  return formData;
}

export async function cargarEvidenciaPagoVenta(token: string, idPagoVenta: string, archivo: File) {
  return apiClient.post<ArchivoEvidenciaResponse>(
    `/evidencias/pagos-venta/${encodeURIComponent(idPagoVenta)}`,
    await crearFormularioEvidencia(archivo),
    { token },
  );
}

export async function cargarAjusteEvidenciaPagoVenta(token: string, idPagoVenta: string, archivo: File) {
  return apiClient.post<ArchivoEvidenciaResponse>(
    `/evidencias/pagos-venta/${encodeURIComponent(idPagoVenta)}/ajustes`,
    await crearFormularioEvidencia(archivo),
    { token },
  );
}

export async function cargarEvidenciaGastoCaja(token: string, idGastoCaja: string, archivo: File) {
  return apiClient.post<ArchivoEvidenciaResponse>(
    `/evidencias/gastos-caja/${encodeURIComponent(idGastoCaja)}`,
    await crearFormularioEvidencia(archivo),
    { token },
  );
}

export async function cargarEvidenciaConsignacionBancaria(token: string, idConsignacionBancaria: string, archivo: File) {
  return apiClient.post<ArchivoEvidenciaResponse>(
    `/evidencias/consignaciones-bancarias/${encodeURIComponent(idConsignacionBancaria)}`,
    await crearFormularioEvidencia(archivo),
    { token },
  );
}

export async function cargarEvidenciaPagoServicio(token: string, idPagoServicio: string, archivo: File) {
  return apiClient.post<ArchivoEvidenciaResponse>(
    `/evidencias/pagos-servicios/${encodeURIComponent(idPagoServicio)}`,
    await crearFormularioEvidencia(archivo),
    { token },
  );
}

export function listarEvidenciasPagoVenta(token: string, idPagoVenta: string) {
  return apiClient.get<ArchivoEvidenciaResponse[]>(
    `/evidencias/pagos-venta/${encodeURIComponent(idPagoVenta)}`,
    { token },
  );
}

export function listarEvidenciasGastoCaja(token: string, idGastoCaja: string) {
  return apiClient.get<ArchivoEvidenciaResponse[]>(
    `/evidencias/gastos-caja/${encodeURIComponent(idGastoCaja)}`,
    { token },
  );
}

export function listarEvidenciasConsignacionBancaria(token: string, idConsignacionBancaria: string) {
  return apiClient.get<ArchivoEvidenciaResponse[]>(
    `/evidencias/consignaciones-bancarias/${encodeURIComponent(idConsignacionBancaria)}`,
    { token },
  );
}

export function listarEvidenciasPagoServicio(token: string, idPagoServicio: string) {
  return apiClient.get<ArchivoEvidenciaResponse[]>(
    `/evidencias/pagos-servicios/${encodeURIComponent(idPagoServicio)}`,
    { token },
  );
}

export function descargarEvidencia(token: string, idArchivoEvidencia: string) {
  return apiClient.getBlob(
    `/evidencias/${encodeURIComponent(idArchivoEvidencia)}/descargar`,
    { token },
  );
}
