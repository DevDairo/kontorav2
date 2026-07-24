import { AlertCircle, ArchiveRestore, Ban, ClipboardPlus, History, Pencil, Plus, RefreshCw, Save, Search, Tag } from "lucide-react";
import { type FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { ConfirmationDialog } from "../../../shared/components/ConfirmationDialog";
import { ApiClientError } from "../../../shared/services/apiClient";
import { formatDisplayName } from "../../../shared/utils/displayText";
import {
  actualizarEstadoItemInventario,
  actualizarItemInventario,
  crearItemInventario,
  crearPrecioGranizado,
  obtenerItemsInventarioGestion,
  obtenerPreciosGranizadoGestion,
} from "../services/catalogosService";
import type {
  CatalogoBasico,
  CatalogosFormulario,
  ItemInventario,
  ItemInventarioGestionRequest,
  PrecioGranizado,
  PrecioGranizadoGestionRequest,
  TamanoVaso,
  UnidadMedida,
} from "../types";

type LoadState = "loading" | "success" | "error";
type ItemMode = "create" | "edit";

type CatalogosGestionPanelProps = {
  catalogos: CatalogosFormulario | null;
  onCatalogosChanged: () => void;
  token: string;
};

type ItemForm = {
  idCategoriaInventario: string;
  idUnidadMedida: string;
  nombreItem: string;
};

type PriceForm = {
  fechaInicioVigencia: string;
  idTamanoVaso: string;
  idTipoGranizado: string;
  valorPrecio: string;
};

function todayLocalDate() {
  const date = new Date();
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function emptyItemForm(categorias: CatalogoBasico[], unidades: UnidadMedida[]): ItemForm {
  return {
    idCategoriaInventario: categorias[0]?.id ?? "",
    idUnidadMedida: unidades[0]?.idUnidadMedida ?? "",
    nombreItem: "",
  };
}

function emptyPriceForm(tipos: CatalogoBasico[], tamanos: TamanoVaso[]): PriceForm {
  return {
    fechaInicioVigencia: todayLocalDate(),
    idTamanoVaso: tamanos[0]?.idTamanoVaso ?? "",
    idTipoGranizado: tipos[0]?.id ?? "",
    valorPrecio: "",
  };
}

function messageFor(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.message;
  }
  return error instanceof Error ? error.message : "No fue posible completar la gestion de catalogos.";
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("es-CO", {
    currency: "COP",
    maximumFractionDigits: 0,
    style: "currency",
  }).format(value);
}

function formatDate(value: string | null) {
  if (!value) {
    return "Sin cierre";
  }
  return new Intl.DateTimeFormat("es-CO", { dateStyle: "medium" }).format(new Date(`${value}T00:00:00`));
}

function statusLabel(estado: string) {
  return estado === "activo" ? "Activo" : "Inactivo";
}

export function CatalogosGestionPanel({ catalogos, onCatalogosChanged, token }: CatalogosGestionPanelProps) {
  const categorias = catalogos?.categoriasInventario ?? [];
  const unidades = catalogos?.unidadesMedida ?? [];
  const tamanos = catalogos?.tamanosVaso ?? [];
  const tiposGranizado = catalogos?.tiposGranizado ?? [];
  const categoriasConsumibles = useMemo(
    () => categorias.filter((categoria) => categoria.nombre !== "vasos"),
    [categorias],
  );
  const [items, setItems] = useState<ItemInventario[]>([]);
  const [precios, setPrecios] = useState<PrecioGranizado[]>([]);
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [itemMessage, setItemMessage] = useState<string | null>(null);
  const [priceMessage, setPriceMessage] = useState<string | null>(null);
  const [itemMode, setItemMode] = useState<ItemMode>("create");
  const [itemForm, setItemForm] = useState<ItemForm>(() => emptyItemForm(categorias, unidades));
  const [priceForm, setPriceForm] = useState<PriceForm>(() => emptyPriceForm(tiposGranizado, tamanos));
  const [selectedItemId, setSelectedItemId] = useState<string | null>(null);
  const [itemQuery, setItemQuery] = useState("");
  const [priceQuery, setPriceQuery] = useState("");
  const [isSavingItem, setIsSavingItem] = useState(false);
  const [isSavingPrice, setIsSavingPrice] = useState(false);
  const [pendingState, setPendingState] = useState<"activo" | "inactivo" | null>(null);
  const [isChangingState, setIsChangingState] = useState(false);

  const loadGestion = useCallback(async () => {
    setLoadState("loading");
    setErrorMessage(null);
    try {
      const [itemsResponse, preciosResponse] = await Promise.all([
        obtenerItemsInventarioGestion(token),
        obtenerPreciosGranizadoGestion(token),
      ]);
      setItems(itemsResponse);
      setPrecios(preciosResponse);
      setLoadState("success");
    } catch (error) {
      setLoadState("error");
      setErrorMessage(messageFor(error));
    }
  }, [token]);

  useEffect(() => {
    void loadGestion();
  }, [loadGestion]);

  useEffect(() => {
    if (itemMode === "create") {
      setItemForm(emptyItemForm(categoriasConsumibles, unidades));
    }
  }, [categoriasConsumibles, itemMode, unidades]);

  useEffect(() => {
    setPriceForm((current) => current.idTipoGranizado && current.idTamanoVaso
      ? current
      : emptyPriceForm(tiposGranizado, tamanos));
  }, [tamanos, tiposGranizado]);

  const selectedItem = useMemo(
    () => items.find((item) => item.idItemInventario === selectedItemId) ?? null,
    [items, selectedItemId],
  );

  const filteredItems = useMemo(() => {
    const query = itemQuery.trim().toLowerCase();
    const itemsManual = items.filter((item) => item.tipoControl === "manual_por_consumo" && item.nombreCategoria !== "vasos");
    if (!query) {
      return itemsManual;
    }
    return itemsManual.filter((item) => [item.nombreItem, item.nombreCategoria, item.nombreUnidad, item.estado]
      .some((value) => value.toLowerCase().includes(query)));
  }, [itemQuery, items]);

  const filteredPrices = useMemo(() => {
    const query = priceQuery.trim().toLowerCase();
    if (!query) {
      return precios;
    }
    return precios.filter((precio) => `${precio.nombreTipo} ${precio.onzas} ${precio.estado}`.toLowerCase().includes(query));
  }, [precios, priceQuery]);

  function iniciarCreacionItem() {
    setSelectedItemId(null);
    setItemMode("create");
    setItemForm(emptyItemForm(categoriasConsumibles, unidades));
    setItemMessage(null);
  }

  function seleccionarItem(item: ItemInventario) {
    setSelectedItemId(item.idItemInventario);
    setItemMode("edit");
    setItemForm({
      idCategoriaInventario: item.idCategoriaInventario,
      idUnidadMedida: item.idUnidadMedida,
      nombreItem: item.nombreItem,
    });
    setItemMessage(null);
  }

  function requestItem(): ItemInventarioGestionRequest | null {
    const nombreItem = itemForm.nombreItem.trim();
    if (!nombreItem || !itemForm.idCategoriaInventario || !itemForm.idUnidadMedida) {
      setItemMessage("Completa los datos obligatorios del item.");
      return null;
    }
    return {
      idCategoriaInventario: itemForm.idCategoriaInventario,
      idUnidadMedida: itemForm.idUnidadMedida,
      manejaPaquetes: false,
      nombreItem,
      tipoControl: "manual_por_consumo",
    };
  }

  async function guardarItem(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setItemMessage(null);
    const request = requestItem();
    if (!request) {
      return;
    }
    setIsSavingItem(true);
    try {
      const response = itemMode === "create"
        ? await crearItemInventario(token, request)
        : await actualizarItemInventario(token, selectedItem?.idItemInventario ?? "", request);
      await loadGestion();
      onCatalogosChanged();
      setSelectedItemId(response.idItemInventario);
      setItemMode("edit");
      setItemForm({
        idCategoriaInventario: response.idCategoriaInventario,
        idUnidadMedida: response.idUnidadMedida,
        nombreItem: response.nombreItem,
      });
      setItemMessage(itemMode === "create" ? "Item creado con existencia general en cero." : "Cambios guardados correctamente.");
    } catch (error) {
      setItemMessage(messageFor(error));
    } finally {
      setIsSavingItem(false);
    }
  }

  async function confirmarCambioEstado() {
    if (!selectedItem || !pendingState) {
      return;
    }
    setIsChangingState(true);
    setItemMessage(null);
    try {
      const response = await actualizarEstadoItemInventario(token, selectedItem.idItemInventario, pendingState);
      await loadGestion();
      onCatalogosChanged();
      setItemMessage(`Item ${statusLabel(response.estado).toLowerCase()} correctamente.`);
      setPendingState(null);
    } catch (error) {
      setItemMessage(messageFor(error));
      setPendingState(null);
    } finally {
      setIsChangingState(false);
    }
  }

  async function guardarPrecio(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPriceMessage(null);
    const valorPrecio = Number(priceForm.valorPrecio);
    if (!priceForm.idTipoGranizado || !priceForm.idTamanoVaso || !priceForm.fechaInicioVigencia || !Number.isFinite(valorPrecio) || valorPrecio <= 0) {
      setPriceMessage("Completa tipo, tamano, valor y fecha de vigencia.");
      return;
    }
    const request: PrecioGranizadoGestionRequest = {
      fechaInicioVigencia: priceForm.fechaInicioVigencia,
      idTamanoVaso: priceForm.idTamanoVaso,
      idTipoGranizado: priceForm.idTipoGranizado,
      valorPrecio,
    };
    setIsSavingPrice(true);
    try {
      await crearPrecioGranizado(token, request);
      await loadGestion();
      onCatalogosChanged();
      setPriceForm((current) => ({ ...current, valorPrecio: "" }));
      setPriceMessage("Nueva vigencia registrada. El precio anterior conserva su historial.");
    } catch (error) {
      setPriceMessage(messageFor(error));
    } finally {
      setIsSavingPrice(false);
    }
  }

  return (
    <section className="catalog-management" aria-label="Gestion administrativa de catalogos">
      <div className="catalog-management-heading">
        <div>
          <span className="eyebrow">Administracion</span>
          <h2>Productos y precios</h2>
          <p>Los productos se crean con stock general en cero. Las cantidades se gestionan desde Inventario.</p>
        </div>
        <button className="icon-button" type="button" onClick={() => void loadGestion()} disabled={loadState === "loading"} aria-label="Actualizar gestion de catalogos" title="Actualizar gestion de catalogos">
          <RefreshCw size={18} aria-hidden="true" />
        </button>
      </div>

      {errorMessage ? (
        <div className="error-alert" role="alert">
          <AlertCircle size={18} aria-hidden="true" />
          <span>{errorMessage}</span>
        </div>
      ) : null}

      <div className="catalog-management-grid">
        <section className="panel catalog-management-form-panel" aria-labelledby="catalog-item-form-title">
          <div className="panel-title">
            <div>
              <h2 id="catalog-item-form-title">{itemMode === "create" ? "Nuevo item inventariable" : "Editar item inventariable"}</h2>
              <p>{itemMode === "create" ? "Alta en inventario general" : "Nombre, categoria, unidad y estado operativo"}</p>
            </div>
            {itemMode === "create" ? <ClipboardPlus size={21} aria-hidden="true" /> : <Pencil size={21} aria-hidden="true" />}
          </div>

          <form className="catalog-management-form" onSubmit={guardarItem}>
            <label className="field-label catalog-management-full">
              Nombre del item
              <div className="field-control plain">
                <input value={itemForm.nombreItem} onChange={(event) => setItemForm((current) => ({ ...current, nombreItem: event.target.value }))} required maxLength={120} />
              </div>
            </label>

            <label className="field-label">
              Categoria
              <div className="field-control plain">
                <select value={itemForm.idCategoriaInventario} onChange={(event) => setItemForm((current) => ({ ...current, idCategoriaInventario: event.target.value }))} required>
                  <option value="" disabled>Selecciona categoria</option>
                  {categoriasConsumibles.map((categoria) => <option key={categoria.id} value={categoria.id}>{categoria.nombre}</option>)}
                </select>
              </div>
            </label>

            <label className="field-label">
              Unidad de medida
              <div className="field-control plain">
                <select value={itemForm.idUnidadMedida} onChange={(event) => setItemForm((current) => ({ ...current, idUnidadMedida: event.target.value }))} required>
                  <option value="" disabled>Selecciona unidad</option>
                  {unidades.map((unidad) => <option key={unidad.idUnidadMedida} value={unidad.idUnidadMedida}>{unidad.nombreUnidad} ({unidad.abreviatura})</option>)}
                </select>
              </div>
            </label>

            <div className="catalog-management-actions catalog-management-full">
              <button className="primary-button" type="submit" disabled={isSavingItem || loadState !== "success"}>
                {itemMode === "create" ? <Plus size={18} aria-hidden="true" /> : <Save size={18} aria-hidden="true" />}
                {isSavingItem ? "Guardando" : itemMode === "create" ? "Crear item" : "Guardar cambios"}
              </button>
              {itemMode === "edit" ? (
                <button className="ghost-button" type="button" onClick={iniciarCreacionItem} disabled={isSavingItem}>
                  Nuevo
                </button>
              ) : null}
            </div>
          </form>

          {itemMessage ? <p className="catalog-management-message" role="status">{itemMessage}</p> : null}

          {selectedItem && itemMode === "edit" ? (
            <div className="catalog-item-state">
              <div>
                <span>Estado operativo</span>
                <strong className={`catalog-status ${selectedItem.estado}`}>{statusLabel(selectedItem.estado)}</strong>
              </div>
              <button className="ghost-button catalog-state-button" type="button" onClick={() => setPendingState(selectedItem.estado === "activo" ? "inactivo" : "activo")} disabled={isChangingState}>
                {selectedItem.estado === "activo" ? <Ban size={17} aria-hidden="true" /> : <ArchiveRestore size={17} aria-hidden="true" />}
                {selectedItem.estado === "activo" ? "Inhabilitar" : "Reactivar"}
              </button>
            </div>
          ) : null}
        </section>

        <section className="panel catalog-management-list-panel" aria-labelledby="catalog-item-list-title">
          <div className="panel-title">
            <div>
              <h2 id="catalog-item-list-title">Items inventariables</h2>
              <p>Incluye activos e inactivos para conservar trazabilidad.</p>
            </div>
            <span className="badge">{loadState === "loading" ? "..." : filteredItems.length}</span>
          </div>

          <label className="field-control catalog-management-search">
            <Search size={18} aria-hidden="true" />
            <input type="search" value={itemQuery} onChange={(event) => setItemQuery(event.target.value)} placeholder="Buscar item o categoria" />
          </label>

          {loadState === "loading" ? <p className="loading-copy">Cargando items...</p> : null}
          {loadState === "success" && filteredItems.length === 0 ? <p className="empty-copy">No hay items para el filtro actual.</p> : null}
          <ul className="catalog-management-list">
            {filteredItems.map((item) => (
              <li key={item.idItemInventario}>
                <button className={`catalog-management-row ${selectedItem?.idItemInventario === item.idItemInventario ? "selected" : ""}`} type="button" onClick={() => seleccionarItem(item)} aria-pressed={selectedItem?.idItemInventario === item.idItemInventario}>
                  <span>
                    <strong>{item.nombreItem}</strong>
                    <small>{item.nombreCategoria} · {item.nombreUnidad}{item.onzas ? ` · ${item.onzas} oz` : ""}</small>
                  </span>
                  <b className={`catalog-status ${item.estado}`}>{statusLabel(item.estado)}</b>
                  <Pencil size={17} aria-hidden="true" />
                </button>
              </li>
            ))}
          </ul>
        </section>

        <section className="panel catalog-management-form-panel" aria-labelledby="catalog-price-form-title">
          <div className="panel-title">
            <div>
              <h2 id="catalog-price-form-title">Nueva vigencia de precio</h2>
              <p>El valor anterior se cierra sin alterar las ventas ya registradas.</p>
            </div>
            <Tag size={21} aria-hidden="true" />
          </div>

          <form className="catalog-management-form" onSubmit={guardarPrecio}>
            <label className="field-label">
              Tipo de granizado
              <div className="field-control plain">
                <select value={priceForm.idTipoGranizado} onChange={(event) => setPriceForm((current) => ({ ...current, idTipoGranizado: event.target.value }))} required>
                  <option value="" disabled>Selecciona tipo</option>
                  {tiposGranizado.map((tipo) => <option key={tipo.id} value={tipo.id}>{formatDisplayName(tipo.nombre)}</option>)}
                </select>
              </div>
            </label>

            <label className="field-label">
              Tamano de vaso
              <div className="field-control plain">
                <select value={priceForm.idTamanoVaso} onChange={(event) => setPriceForm((current) => ({ ...current, idTamanoVaso: event.target.value }))} required>
                  <option value="" disabled>Selecciona tamano</option>
                  {tamanos.map((tamano) => <option key={tamano.idTamanoVaso} value={tamano.idTamanoVaso}>{tamano.onzas} oz</option>)}
                </select>
              </div>
            </label>

            <label className="field-label">
              Valor en pesos
              <div className="field-control plain">
                <input inputMode="numeric" value={priceForm.valorPrecio} onChange={(event) => setPriceForm((current) => ({ ...current, valorPrecio: event.target.value.replace(/[^0-9]/g, "") }))} placeholder="Ej. 12000" required />
              </div>
            </label>

            <label className="field-label">
              Inicia el
              <div className="field-control plain">
                <input type="date" value={priceForm.fechaInicioVigencia} min={todayLocalDate()} onChange={(event) => setPriceForm((current) => ({ ...current, fechaInicioVigencia: event.target.value }))} required />
              </div>
            </label>

            <div className="catalog-management-actions catalog-management-full">
              <button className="primary-button" type="submit" disabled={isSavingPrice || loadState !== "success"}>
                <Save size={18} aria-hidden="true" />
                {isSavingPrice ? "Guardando" : "Registrar vigencia"}
              </button>
            </div>
          </form>
          {priceMessage ? <p className="catalog-management-message" role="status">{priceMessage}</p> : null}
        </section>

        <section className="panel catalog-management-list-panel" aria-labelledby="catalog-price-list-title">
          <div className="panel-title">
            <div>
              <h2 id="catalog-price-list-title">Historial de precios</h2>
              <p>Vigencias por tipo y tamano, incluidas las cerradas.</p>
            </div>
            <History size={21} aria-hidden="true" />
          </div>

          <label className="field-control catalog-management-search">
            <Search size={18} aria-hidden="true" />
            <input type="search" value={priceQuery} onChange={(event) => setPriceQuery(event.target.value)} placeholder="Buscar tipo o tamano" />
          </label>

          {loadState === "loading" ? <p className="loading-copy">Cargando precios...</p> : null}
          <ul className="catalog-management-list catalog-price-history">
            {filteredPrices.map((precio) => (
              <li className="catalog-price-history-row" key={precio.idPrecioGranizado}>
                <span>
                  <strong>{formatDisplayName(precio.nombreTipo)} · {precio.onzas} oz</strong>
                  <small>Desde {formatDate(precio.fechaInicioVigencia)} · Hasta {formatDate(precio.fechaFinVigencia)}</small>
                </span>
                <b>{formatCurrency(precio.valorPrecio)}</b>
                <em className={`catalog-status ${precio.fechaFinVigencia ? "inactivo" : precio.estado}`}>{precio.fechaFinVigencia ? "Cerrado" : statusLabel(precio.estado)}</em>
              </li>
            ))}
          </ul>
        </section>
      </div>

      <ConfirmationDialog
        confirmLabel={pendingState === "inactivo" ? "Inhabilitar" : "Reactivar"}
        description={pendingState === "inactivo"
          ? "El producto dejara de estar disponible para nuevas operaciones. Solo se permite si su stock general es cero."
          : "El producto volvera a estar disponible para operaciones nuevas."}
        isConfirming={isChangingState}
        onCancel={() => setPendingState(null)}
        onConfirm={() => void confirmarCambioEstado()}
        open={pendingState !== null}
        title={pendingState === "inactivo" ? "Inhabilitar producto" : "Reactivar producto"}
      />
    </section>
  );
}
