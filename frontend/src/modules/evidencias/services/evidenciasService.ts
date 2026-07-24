import { ApiClientError, apiClient } from "../../../shared/services/apiClient";
import type { ArchivoEvidenciaResponse } from "../types";

export function messageForEvidenceDownload(error: unknown) {
  if (error instanceof ApiClientError && (error.status === 401 || error.status === 404)) {
    return "La evidencia solicitada no esta disponible para descargar.";
  }

  if (error instanceof ApiClientError) {
    return error.message;
  }

  return error instanceof Error ? error.message : "No fue posible descargar la evidencia.";
}

export function cargarEvidenciaPagoVenta(token: string, idPagoVenta: string, archivo: File) {
  const formData = new FormData();
  formData.append("archivo", archivo);

  return apiClient.post<ArchivoEvidenciaResponse>(
    `/evidencias/pagos-venta/${encodeURIComponent(idPagoVenta)}`,
    formData,
    { token },
  );
}

export function cargarAjusteEvidenciaPagoVenta(token: string, idPagoVenta: string, archivo: File) {
  const formData = new FormData();
  formData.append("archivo", archivo);

  return apiClient.post<ArchivoEvidenciaResponse>(
    `/evidencias/pagos-venta/${encodeURIComponent(idPagoVenta)}/ajustes`,
    formData,
    { token },
  );
}

export function cargarEvidenciaGastoCaja(token: string, idGastoCaja: string, archivo: File) {
  const formData = new FormData();
  formData.append("archivo", archivo);

  return apiClient.post<ArchivoEvidenciaResponse>(
    `/evidencias/gastos-caja/${encodeURIComponent(idGastoCaja)}`,
    formData,
    { token },
  );
}

export function cargarEvidenciaConsignacionBancaria(token: string, idConsignacionBancaria: string, archivo: File) {
  const formData = new FormData();
  formData.append("archivo", archivo);

  return apiClient.post<ArchivoEvidenciaResponse>(
    `/evidencias/consignaciones-bancarias/${encodeURIComponent(idConsignacionBancaria)}`,
    formData,
    { token },
  );
}

export function cargarEvidenciaPagoServicio(token: string, idPagoServicio: string, archivo: File) {
  const formData = new FormData();
  formData.append("archivo", archivo);

  return apiClient.post<ArchivoEvidenciaResponse>(
    `/evidencias/pagos-servicios/${encodeURIComponent(idPagoServicio)}`,
    formData,
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
