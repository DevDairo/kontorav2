import { ApiClientError, apiClient } from "../../../shared/services/apiClient";
import { obtenerItemsInventarioGestion } from "../../catalogos/services/catalogosService";
import type {
  AjusteInventario,
  ConsumoDiarioInventarioResponse,
  ExistenciaInventarioDiario,
  ExistenciaInventarioGeneral,
  InventarioSnapshot,
  AnularPerdidaInventarioRequest,
  PerdidaInventario,
  PaqueteVasosAbiertoResponse,
  RegistrarConsumoDiarioInventarioRequest,
  RegistrarPaqueteVasosRequest,
  RegistrarPerdidaInventarioRequest,
  ResolverAjusteInventarioRequest,
  SolicitarAjusteInventarioRequest,
  VentasVasosDiarias,
} from "../types";

type ExistenciaInventarioGeneralApi = Omit<
  ExistenciaInventarioGeneral,
  "idCategoriaInventario" | "nombreCategoria"
>;

function jsonBody<T>(payload: T) {
  return JSON.stringify(payload);
}

function optionalQuery(params: Record<string, string | undefined>) {
  const query = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value) {
      query.set(key, value);
    }
  });

  const value = query.toString();
  return value ? `?${value}` : "";
}

export function obtenerExistenciasGenerales(token: string) {
  return apiClient.get<ExistenciaInventarioGeneralApi[]>("/inventario/existencias/general", { token });
}

export function obtenerExistenciasDiariasAbierta(token: string) {
  return apiClient.get<ExistenciaInventarioDiario[]>("/inventario/existencias/diarias/abierta", { token });
}

export function obtenerVentasVasosDiariaAbierta(token: string) {
  return apiClient.get<VentasVasosDiarias[]>("/inventario/ventas-vasos/diaria-abierta", { token });
}

async function obtenerExistenciasDiariasAbiertaOpcional(token: string) {
  try {
    return await obtenerExistenciasDiariasAbierta(token);
  } catch (error) {
    if (
      error instanceof ApiClientError &&
      (error.status === 404 || (error.status === 409 && error.message.includes("caja diaria abierta")))
    ) {
      return [];
    }

    throw error;
  }
}

export function obtenerAjustesInventario(token: string, filters: { estadoAprobacion?: string } = {}) {
  return apiClient.get<AjusteInventario[]>(
    `/inventario/ajustes${optionalQuery(filters)}`,
    { token },
  );
}

export function registrarPaqueteVasos(token: string, request: RegistrarPaqueteVasosRequest) {
  return apiClient.post<PaqueteVasosAbiertoResponse>(
    "/inventario/paquetes-vasos",
    jsonBody(request),
    { token },
  );
}

export function registrarConsumoDiario(token: string, request: RegistrarConsumoDiarioInventarioRequest) {
  return apiClient.post<ConsumoDiarioInventarioResponse>(
    "/inventario/consumos-diarios",
    jsonBody(request),
    { token },
  );
}

export function registrarPerdidaInventario(
  token: string,
  request: RegistrarPerdidaInventarioRequest,
) {
  return apiClient.post<PerdidaInventario>(
    "/inventario/perdidas-vasos",
    jsonBody(request),
    { token },
  );
}

export function obtenerPerdidasCajaAbierta(token: string) {
  return apiClient.get<PerdidaInventario[]>("/inventario/perdidas-vasos/caja-abierta", { token });
}

export function obtenerPerdidasPeriodo(
  token: string,
  filters: { fechaInicio: string; fechaFin?: string },
) {
  return apiClient.get<PerdidaInventario[]>(
    `/inventario/perdidas-vasos${optionalQuery(filters)}`,
    { token },
  );
}

export function anularPerdidaInventario(
  token: string,
  idPerdidaInventario: string,
  request: AnularPerdidaInventarioRequest,
) {
  return apiClient.post<PerdidaInventario>(
    `/inventario/perdidas-vasos/${encodeURIComponent(idPerdidaInventario)}/anular`,
    jsonBody(request),
    { token },
  );
}

export function solicitarAjusteInventario(token: string, request: SolicitarAjusteInventarioRequest) {
  return apiClient.post<AjusteInventario>(
    "/inventario/ajustes",
    jsonBody(request),
    { token },
  );
}

export function aprobarAjusteInventario(
  token: string,
  idAjusteInventario: string,
  request: ResolverAjusteInventarioRequest = {},
) {
  return apiClient.post<AjusteInventario>(
    `/inventario/ajustes/${idAjusteInventario}/aprobar`,
    jsonBody(request),
    { token },
  );
}

export function rechazarAjusteInventario(
  token: string,
  idAjusteInventario: string,
  request: ResolverAjusteInventarioRequest = {},
) {
  return apiClient.post<AjusteInventario>(
    `/inventario/ajustes/${idAjusteInventario}/rechazar`,
    jsonBody(request),
    { token },
  );
}

export async function obtenerInventarioSnapshot(token: string): Promise<InventarioSnapshot> {
  const [
    existenciasGenerales,
    existenciasDiarias,
    ajustes,
    ventasVasosDiarias,
    itemsInventario,
  ] = await Promise.all([
    obtenerExistenciasGenerales(token),
    obtenerExistenciasDiariasAbiertaOpcional(token),
    obtenerAjustesInventario(token),
    obtenerVentasVasosDiariaAbierta(token),
    obtenerItemsInventarioGestion(token),
  ]);
  const itemsPorId = new Map(
    itemsInventario.map((item) => [item.idItemInventario, item]),
  );
  const existenciasGeneralesActivas = existenciasGenerales.flatMap((existencia) => {
    const itemCatalogo = itemsPorId.get(existencia.idItemInventario);

    if (!itemCatalogo || itemCatalogo.estado !== "activo") {
      return [];
    }

    return [{
      ...existencia,
      idCategoriaInventario: itemCatalogo.idCategoriaInventario,
      nombreCategoria: itemCatalogo.nombreCategoria,
      nombreItem: itemCatalogo.nombreItem,
    }];
  });
  const existenciasDiariasActualizadas = existenciasDiarias.map((existencia) => ({
    ...existencia,
    nombreItem: itemsPorId.get(existencia.idItemInventario)?.nombreItem ?? existencia.nombreItem,
  }));
  const ajustesActualizados = ajustes.map((ajuste) => ({
    ...ajuste,
    nombreItem: itemsPorId.get(ajuste.idItemInventario)?.nombreItem ?? ajuste.nombreItem,
  }));

  return {
    ajustes: ajustesActualizados,
    existenciasDiarias: existenciasDiariasActualizadas,
    existenciasGenerales: existenciasGeneralesActivas,
    ventasVasosDiarias,
  };
}
