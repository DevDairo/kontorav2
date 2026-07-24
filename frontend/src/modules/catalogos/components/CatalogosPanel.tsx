import { Database, RefreshCw, Search, Tags } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { ApiClientError } from "../../../shared/services/apiClient";
import { formatDisplayName } from "../../../shared/utils/displayText";
import { CatalogosGestionPanel } from "./CatalogosGestionPanel";
import { obtenerCatalogosFormulario } from "../services/catalogosService";
import type { CatalogosFormulario, ItemInventario, PrecioGranizado, Promocion } from "../types";

type LoadState = "loading" | "success" | "error";

type CatalogosPanelProps = {
  canManage: boolean;
  token: string;
};

function todayLocalDate() {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const day = String(today.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("es-CO", {
    currency: "COP",
    maximumFractionDigits: 0,
    style: "currency",
  }).format(Number(value));
}

function formatDate(value: string | null) {
  if (!value) {
    return "Sin fin";
  }

  return new Intl.DateTimeFormat("es-CO", { dateStyle: "medium" }).format(new Date(`${value}T00:00:00`));
}

function messageFor(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.message;
  }

  return error instanceof Error ? error.message : "No fue posible cargar los catalogos";
}

function byName<T extends { nombre?: string; nombreItem?: string; nombrePromocion?: string; nombreTipo?: string }>(
  items: T[],
  query: string,
) {
  const normalized = query.trim().toLowerCase();
  if (!normalized) {
    return items;
  }

  return items.filter((item) =>
    [item.nombre, item.nombreItem, item.nombrePromocion, item.nombreTipo]
      .filter(Boolean)
      .some((value) => value?.toLowerCase().includes(normalized)),
  );
}

function PriceRow({ precio }: { precio: PrecioGranizado }) {
  return (
    <li className="catalog-row">
      <span>
        <strong>{formatDisplayName(precio.nombreTipo)}</strong>
        <small>{precio.onzas} oz</small>
      </span>
      <b>{formatCurrency(precio.valorPrecio)}</b>
    </li>
  );
}

function PromoRow({ promocion }: { promocion: Promocion }) {
  return (
    <li className="catalog-row stacked">
      <span>
        <strong>{formatDisplayName(promocion.nombrePromocion)}</strong>
        <small>
          {formatDisplayName(promocion.nombreTipo)} · {promocion.onzas} oz · {formatDisplayName(promocion.tipoBeneficiario)}
        </small>
      </span>
      <b>{formatCurrency(promocion.valorPromocional)}</b>
      <em>{promocion.diasPromocion.length > 0 ? promocion.diasPromocion.join(", ") : "Sin dias configurados"}</em>
    </li>
  );
}

function ItemRow({ item }: { item: ItemInventario }) {
  return (
    <li className="catalog-row stacked">
      <span>
        <strong>{formatDisplayName(item.nombreItem)}</strong>
        <small>
          {formatDisplayName(item.nombreCategoria)} · {formatDisplayName(item.nombreUnidad)}
          {item.onzas ? ` · ${item.onzas} oz` : ""}
        </small>
      </span>
      <b>{item.tipoControl}</b>
      <em>{item.manejaPaquetes ? `${item.unidadesPorPaquete ?? 0} por paquete` : "Sin paquetes"}</em>
    </li>
  );
}

export function CatalogosPanel({ canManage, token }: CatalogosPanelProps) {
  const [catalogos, setCatalogos] = useState<CatalogosFormulario | null>(null);
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [fechaVigencia, setFechaVigencia] = useState(todayLocalDate);
  const [query, setQuery] = useState("");
  const [view, setView] = useState<"consulta" | "gestion">("consulta");

  const loadCatalogos = useCallback(async () => {
    setLoadState("loading");
    setErrorMessage(null);

    try {
      const response = await obtenerCatalogosFormulario(token, fechaVigencia);
      setCatalogos(response);
      setLoadState("success");
    } catch (error) {
      setLoadState("error");
      setErrorMessage(messageFor(error));
    }
  }, [fechaVigencia, token]);

  useEffect(() => {
    void loadCatalogos();
  }, [loadCatalogos]);

  const filteredPrices = useMemo(
    () => byName(catalogos?.preciosVigentes ?? [], query),
    [catalogos?.preciosVigentes, query],
  );
  const filteredPromos = useMemo(
    () => byName(catalogos?.promocionesVigentes ?? [], query),
    [catalogos?.promocionesVigentes, query],
  );
  const filteredItems = useMemo(
    () => byName(catalogos?.itemsInventario ?? [], query).slice(0, 8),
    [catalogos?.itemsInventario, query],
  );

  return (
    <>
      <section className="section-heading" aria-labelledby="catalogos-title">
        <div>
          <p className="eyebrow">Catalogos base</p>
          <h1 id="catalogos-title">Catalogos para formularios</h1>
          <p className="lead">Consulta precios, promociones e insumos disponibles para la operacion diaria.</p>
        </div>
        <button className="ghost-button" type="button" onClick={loadCatalogos} disabled={loadState === "loading"}>
          <RefreshCw size={17} strokeWidth={2.2} />
          Reintentar
        </button>
      </section>

      {canManage ? (
        <div className="catalog-view-tabs" role="tablist" aria-label="Vistas de catalogos">
          <button className={view === "consulta" ? "active" : ""} type="button" role="tab" aria-selected={view === "consulta"} onClick={() => setView("consulta")}>
            Consulta
          </button>
          <button className={view === "gestion" ? "active" : ""} type="button" role="tab" aria-selected={view === "gestion"} onClick={() => setView("gestion")}>
            Administrar
          </button>
        </div>
      ) : null}

      {view === "gestion" && canManage ? (
        <CatalogosGestionPanel catalogos={catalogos} onCatalogosChanged={() => void loadCatalogos()} token={token} />
      ) : (
        <>
          <div className="catalog-toolbar panel">
            <label className="field-label">
              Fecha de vigencia
              <div className="field-control plain">
                <input type="date" value={fechaVigencia} onChange={(event) => setFechaVigencia(event.target.value)} />
              </div>
            </label>

            <label className="field-label catalog-search">
              Buscar
              <div className="field-control">
                <Search size={18} strokeWidth={2.2} />
                <input
                  type="search"
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  placeholder="Granizado, item o promocion"
                />
              </div>
            </label>
          </div>

          {errorMessage && loadState === "error" ? (
            <div className="form-alert" role="status">
              <Database size={18} strokeWidth={2.2} />
              <span>{errorMessage}</span>
            </div>
          ) : null}

          <div className="catalog-summary-grid">
            <article className="catalog-summary-card">
              <Database size={22} strokeWidth={2.2} />
              <span>Metodos de pago</span>
              <strong>{catalogos?.metodosPago.length ?? 0}</strong>
            </article>
            <article className="catalog-summary-card">
              <Tags size={22} strokeWidth={2.2} />
              <span>Tipos granizado</span>
              <strong>{catalogos?.tiposGranizado.length ?? 0}</strong>
            </article>
            <article className="catalog-summary-card">
              <Database size={22} strokeWidth={2.2} />
              <span>Items inventario</span>
              <strong>{catalogos?.itemsInventario.length ?? 0}</strong>
            </article>
            <article className="catalog-summary-card">
              <Tags size={22} strokeWidth={2.2} />
              <span>Promociones vigentes</span>
              <strong>{catalogos?.promocionesVigentes.length ?? 0}</strong>
            </article>
          </div>

          <div className="catalog-panel-grid">
            <article className="panel">
              <div className="panel-title">
                <div>
                  <h2>Precios vigentes</h2>
                  <p>Valores disponibles para ventas</p>
                </div>
                <span className="badge">{loadState === "loading" ? "Cargando" : `${filteredPrices.length}`}</span>
              </div>
              <ul className="catalog-list">
                {filteredPrices.map((precio) => (
                  <PriceRow key={precio.idPrecioGranizado} precio={precio} />
                ))}
              </ul>
            </article>

            <article className="panel">
              <div className="panel-title">
                <div>
                  <h2>Promociones</h2>
                  <p>Beneficios aplicables en la jornada</p>
                </div>
                <span className="badge">{loadState === "loading" ? "Cargando" : `${filteredPromos.length}`}</span>
              </div>
              <ul className="catalog-list">
                {filteredPromos.map((promocion) => (
                  <PromoRow key={promocion.idPromocion} promocion={promocion} />
                ))}
              </ul>
            </article>

            <article className="panel">
              <div className="panel-title">
                <div>
                  <h2>Inventario activo</h2>
                  <p>Insumos y vasos registrados</p>
                </div>
                <span className="badge">{loadState === "loading" ? "Cargando" : `${filteredItems.length}`}</span>
              </div>
              <ul className="catalog-list">
                {filteredItems.map((item) => (
                  <ItemRow key={item.idItemInventario} item={item} />
                ))}
              </ul>
            </article>

            <article className="panel">
              <div className="panel-title">
                <div>
                  <h2>Listas base</h2>
                  <p>Referencias disponibles</p>
                </div>
              </div>
              <dl className="catalog-base-list">
                <div>
                  <dt>Tamanos vaso</dt>
                  <dd>{catalogos?.tamanosVaso.map((tamano) => `${tamano.onzas} oz`).join(", ") || "Sin datos"}</dd>
                </div>
                <div>
                  <dt>Unidades</dt>
                  <dd>{catalogos?.unidadesMedida.map((unidad) => unidad.abreviatura).join(", ") || "Sin datos"}</dd>
                </div>
                <div>
                  <dt>Categorias</dt>
                  <dd>{catalogos?.categoriasInventario.map((categoria) => formatDisplayName(categoria.nombre)).join(", ") || "Sin datos"}</dd>
                </div>
                <div>
                  <dt>Servicios</dt>
                  <dd>{catalogos?.tiposServicio.map((servicio) => formatDisplayName(servicio.nombre)).join(", ") || "Sin datos"}</dd>
                </div>
              </dl>
            </article>
          </div>
        </>
      )}
    </>
  );
}
