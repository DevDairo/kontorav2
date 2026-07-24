import { apiClient } from "../../../shared/services/apiClient";
import type {
  CatalogoBasico,
  CatalogosFormulario,
  ItemInventarioGestionRequest,
  ItemInventario,
  PrecioGranizadoGestionRequest,
  PrecioGranizado,
  Promocion,
  TamanoVaso,
  UnidadMedida,
} from "../types";

function fechaQuery(fecha?: string) {
  return fecha ? `?fecha=${encodeURIComponent(fecha)}` : "";
}

export function obtenerMetodosPago(token: string) {
  return apiClient.get<CatalogoBasico[]>("/catalogos/metodos-pago", { token });
}

export function obtenerTiposGranizado(token: string) {
  return apiClient.get<CatalogoBasico[]>("/catalogos/tipos-granizado", { token });
}

export function obtenerTamanosVaso(token: string) {
  return apiClient.get<TamanoVaso[]>("/catalogos/tamanos-vaso", { token });
}

export function obtenerCategoriasInventario(token: string) {
  return apiClient.get<CatalogoBasico[]>("/catalogos/categorias-inventario", { token });
}

export function obtenerUnidadesMedida(token: string) {
  return apiClient.get<UnidadMedida[]>("/catalogos/unidades-medida", { token });
}

export function obtenerItemsInventario(token: string) {
  return apiClient.get<ItemInventario[]>("/catalogos/items-inventario", { token });
}

export function obtenerPreciosVigentes(token: string, fecha?: string) {
  return apiClient.get<PrecioGranizado[]>(`/catalogos/precios-granizado/vigentes${fechaQuery(fecha)}`, {
    token,
  });
}

export function obtenerPromocionesVigentes(token: string, fecha?: string) {
  return apiClient.get<Promocion[]>(`/catalogos/promociones/vigentes${fechaQuery(fecha)}`, {
    token,
  });
}

export function obtenerTiposServicio(token: string) {
  return apiClient.get<CatalogoBasico[]>("/catalogos/tipos-servicio", { token });
}

export function obtenerItemsInventarioGestion(token: string) {
  return apiClient.get<ItemInventario[]>("/catalogos/gestion/items-inventario", { token });
}

export function crearItemInventario(token: string, request: ItemInventarioGestionRequest) {
  return apiClient.post<ItemInventario>("/catalogos/gestion/items-inventario", JSON.stringify(request), { token });
}

export function actualizarItemInventario(token: string, idItemInventario: string, request: ItemInventarioGestionRequest) {
  return apiClient.put<ItemInventario>(`/catalogos/gestion/items-inventario/${idItemInventario}`, JSON.stringify(request), { token });
}

export function actualizarEstadoItemInventario(token: string, idItemInventario: string, estado: "activo" | "inactivo") {
  return apiClient.put<ItemInventario>(`/catalogos/gestion/items-inventario/${idItemInventario}/estado`, JSON.stringify({ estado }), { token });
}

export function obtenerPreciosGranizadoGestion(token: string) {
  return apiClient.get<PrecioGranizado[]>("/catalogos/gestion/precios-granizado", { token });
}

export function crearPrecioGranizado(token: string, request: PrecioGranizadoGestionRequest) {
  return apiClient.post<PrecioGranizado>("/catalogos/gestion/precios-granizado", JSON.stringify(request), { token });
}

export async function obtenerCatalogosFormulario(token: string, fecha?: string): Promise<CatalogosFormulario> {
  const [
    metodosPago,
    tiposGranizado,
    tamanosVaso,
    categoriasInventario,
    unidadesMedida,
    itemsInventario,
    preciosVigentes,
    promocionesVigentes,
    tiposServicio,
  ] = await Promise.all([
    obtenerMetodosPago(token),
    obtenerTiposGranizado(token),
    obtenerTamanosVaso(token),
    obtenerCategoriasInventario(token),
    obtenerUnidadesMedida(token),
    obtenerItemsInventario(token),
    obtenerPreciosVigentes(token, fecha),
    obtenerPromocionesVigentes(token, fecha),
    obtenerTiposServicio(token),
  ]);

  return {
    categoriasInventario,
    itemsInventario,
    metodosPago,
    preciosVigentes,
    promocionesVigentes,
    tamanosVaso,
    tiposGranizado,
    tiposServicio,
    unidadesMedida,
  };
}
