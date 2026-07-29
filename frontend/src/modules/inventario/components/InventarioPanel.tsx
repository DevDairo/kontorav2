import {
  ArrowDown,
  ArrowUp,
  Boxes,
  CheckCircle2,
  ClipboardList,
  PackageOpen,
  RefreshCw,
  SlidersHorizontal,
  XCircle,
} from "lucide-react";
import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import type { UserRole } from "../../../app/routes/appRoutes";
import { ApiClientError } from "../../../shared/services/apiClient";
import { formatDisplayName } from "../../../shared/utils/displayText";
import {
  aprobarAjusteInventario,
  obtenerInventarioSnapshot,
  rechazarAjusteInventario,
  registrarConsumoDiario,
  registrarPaqueteVasos,
  solicitarAjusteInventario,
} from "../services/inventarioService";
import type {
  AjusteInventario,
  ConsumoDiarioInventarioResponse,
  ExistenciaInventarioDiario,
  ExistenciaInventarioGeneral,
  InventarioSnapshot,
  PaqueteVasosAbiertoResponse,
  VentasVasosDiarias,
} from "../types";

type LoadState = "loading" | "success" | "error";
type InventoryView = "consulta" | "paquetes" | "consumo" | "ajuste" | "movimientos";
type DailyStockEntryMode = "paquetes" | "unidades";
type GeneralStockAdjustmentDirection = "entrada" | "salida";

type InventarioPanelProps = {
  token: string;
  role: UserRole | null;
};

type LastAction =
  | { type: "paquete"; response: PaqueteVasosAbiertoResponse }
  | { type: "reabastecimiento-diario"; response: AjusteInventario }
  | { type: "consumo"; response: ConsumoDiarioInventarioResponse }
  | { type: "ajuste"; response: AjusteInventario }
  | { type: "ajuste-aprobado"; response: AjusteInventario }
  | { type: "ajuste-rechazado"; response: AjusteInventario };

const DEFAULT_STOCK_ADJUSTMENT_REASON: Record<GeneralStockAdjustmentDirection, string> = {
  entrada: "Reabastecimiento",
  salida: "Retiro de stock general",
};
const DAILY_STOCK_REPLENISHMENT_REASON = "Reabastecimiento de stock diario";
const UNIDADES_POR_PAQUETE = 20;

function messageFor(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.message;
  }

  return error instanceof Error ? error.message : "No fue posible consultar inventario";
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return "Sin registrar";
  }

  return new Intl.DateTimeFormat("es-CO", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function itemLabel(item: { nombreItem: string; onzas: number | null }) {
  return `${formatDisplayName(item.nombreItem)}${item.onzas ? ` · ${item.onzas} oz` : ""}`;
}

function equivalenciaPaquetes(vasosVendidos: number) {
  const paquetes = Math.floor(vasosVendidos / UNIDADES_POR_PAQUETE);
  const vasosRestantes = vasosVendidos % UNIDADES_POR_PAQUETE;

  if (paquetes === 0) {
    return `${vasosRestantes} ${vasosRestantes === 1 ? "vaso" : "vasos"}`;
  }

  const etiquetaPaquetes = `${paquetes} ${paquetes === 1 ? "paquete" : "paquetes"}`;
  if (vasosRestantes === 0) {
    return etiquetaPaquetes;
  }

  return `${etiquetaPaquetes} + ${vasosRestantes} ${vasosRestantes === 1 ? "vaso" : "vasos"}`;
}

function orderInventoryItemsForDisplay<T extends { nombreItem: string; onzas: number | null }>(items: T[]) {
  return [...items].sort((left, right) => {
    const leftOunces = left.onzas;
    const rightOunces = right.onzas;
    const leftIsCup = leftOunces !== null;
    const rightIsCup = rightOunces !== null;

    if (leftIsCup && rightIsCup) {
      return leftOunces - rightOunces;
    }

    if (leftIsCup) {
      return -1;
    }

    if (rightIsCup) {
      return 1;
    }

    return left.nombreItem.localeCompare(right.nombreItem, "es");
  });
}

function emptySnapshot(): InventarioSnapshot {
  return {
    ajustes: [],
    existenciasDiarias: [],
    existenciasGenerales: [],
    ventasVasosDiarias: [],
  };
}

function SummaryCard({ label, value, detail }: { label: string; value: number | string; detail: string }) {
  return (
    <article className="inventario-summary-card">
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{detail}</small>
    </article>
  );
}

function inventoryControlLabel(item: ExistenciaInventarioGeneral) {
  return item.tipoControl === "automatico_por_venta" ? "Vaso" : "Consumo manual";
}

function InventoryItemSelector({
  items,
  selectedId,
  onSelect,
  ariaLabel,
  emptyMessage,
}: {
  items: ExistenciaInventarioGeneral[];
  selectedId: string;
  onSelect: (idItemInventario: string) => void;
  ariaLabel: string;
  emptyMessage: string;
}) {
  return (
    <section className="inventory-item-selector" aria-label={ariaLabel}>
      <div className="inventory-item-selector-heading">
        <strong>Selecciona un item</strong>
        <small>Presiona una fila para continuar con el movimiento.</small>
      </div>

      {items.length > 0 ? (
        <div className="inventory-item-table">
          <div className="inventory-item-table-header" aria-hidden="true">
            <span>N.º</span>
            <span>Item</span>
            <span>Unidades</span>
          </div>
          <div className="inventory-item-options" role="group" aria-label={ariaLabel}>
            {items.map((item, index) => {
              const isSelected = item.idItemInventario === selectedId;

              return (
                <button
                  key={item.idItemInventario}
                  className={isSelected ? "selected" : ""}
                  type="button"
                  aria-pressed={isSelected}
                  onClick={() => onSelect(item.idItemInventario)}
                >
                  <span className="inventory-item-number">{index + 1}</span>
                  <span className="inventory-item-identity">
                    <strong>{itemLabel(item)}</strong>
                    <small>{inventoryControlLabel(item)}</small>
                  </span>
                  <b>{item.cantidadActual}</b>
                </button>
              );
            })}
          </div>
        </div>
      ) : (
        <p className="inventory-empty">{emptyMessage}</p>
      )}
    </section>
  );
}

function SelectedInventoryItem({
  item,
  emptyMessage,
}: {
  item: ExistenciaInventarioGeneral | undefined;
  emptyMessage: string;
}) {
  return (
    <div className="inventory-selected-item" aria-live="polite">
      <span>Item seleccionado</span>
      <strong>{item ? itemLabel(item) : "Sin seleccionar"}</strong>
      <small>{item ? `Stock general: ${item.cantidadActual} unidades` : emptyMessage}</small>
    </div>
  );
}

function DailyRow({ item }: { item: ExistenciaInventarioDiario }) {
  return (
    <li className="inventory-row daily">
      <span>
        <strong>{itemLabel(item)}</strong>
        <small>Control de jornada</small>
      </span>
      <dl>
        <div>
          <dt>Inicial</dt>
          <dd>{item.cantidadInicial}</dd>
        </div>
        <div>
          <dt>Ingresada</dt>
          <dd>{item.cantidadIngresada + item.cantidadAjustada}</dd>
        </div>
        <div>
          <dt>Vendida</dt>
          <dd>{item.cantidadVendida}</dd>
        </div>
        <div>
          <dt>Perdida</dt>
          <dd>{item.cantidadPerdida}</dd>
        </div>
        <div>
          <dt>Teorica</dt>
          <dd>{item.cantidadFinalTeorica}</dd>
        </div>
      </dl>
    </li>
  );
}

function DailySalesRow({ sale }: { sale: VentasVasosDiarias }) {
  return (
    <li className="daily-sales-row">
      <span>
        <strong>{formatDisplayName(sale.nombreTipo)} · {sale.onzas} oz</strong>
        <small>{sale.vasosVendidos} {sale.vasosVendidos === 1 ? "vaso vendido" : "vasos vendidos"}</small>
      </span>
      <b>{equivalenciaPaquetes(sale.vasosVendidos)}</b>
    </li>
  );
}

function AdjustmentRow({
  adjustment,
  canApprove,
  isResolving,
  note,
  onNoteChange,
  onApprove,
  onReject,
}: {
  adjustment: AjusteInventario;
  canApprove: boolean;
  isResolving: boolean;
  note: string;
  onNoteChange: (value: string) => void;
  onApprove: () => void;
  onReject: () => void;
}) {
  const isPending = adjustment.estadoAprobacion === "pendiente";
  const isEntrada = adjustment.sentidoAjuste === "entrada";
  const movementLabel = adjustment.tipoStock === "diario"
    ? "Reabastecimiento de stock diario"
    : `Stock general · ${formatDisplayName(adjustment.sentidoAjuste)}`;
  const Icon = isEntrada ? ArrowUp : ArrowDown;

  return (
    <li className="adjustment-row">
      <div className={`movement-icon ${isEntrada ? "entrada" : "salida"}`}>
        <Icon size={18} strokeWidth={2.3} />
      </div>
      <span>
        <strong>{formatDisplayName(adjustment.nombreItem)}</strong>
        <small>
          {movementLabel} · {adjustment.cantidadAjuste} unidades · {adjustment.nombreUsuarioSolicitante}
        </small>
        <em>{adjustment.motivoAjuste}</em>
      </span>
      <b className={`badge ${adjustment.estadoAprobacion === "aprobado" ? "success" : "warning"}`}>
        {adjustment.estadoAprobacion}
      </b>
      <time>{formatDateTime(adjustment.fechaSolicitud)}</time>
      {canApprove && isPending ? (
        <div className="adjustment-actions">
          <input
            type="text"
            value={note}
            onChange={(event) => onNoteChange(event.target.value)}
            placeholder="Observacion opcional"
            maxLength={1000}
          />
          <button className="icon-action success" type="button" onClick={onApprove} disabled={isResolving}>
            <CheckCircle2 size={17} strokeWidth={2.2} />
            Aprobar
          </button>
          <button className="icon-action danger" type="button" onClick={onReject} disabled={isResolving}>
            <XCircle size={17} strokeWidth={2.2} />
            Rechazar
          </button>
        </div>
      ) : adjustment.observacionAprobacion ? (
        <small className="adjustment-note">{adjustment.observacionAprobacion}</small>
      ) : null}
    </li>
  );
}

export function InventarioPanel({ token, role }: InventarioPanelProps) {
  const [snapshot, setSnapshot] = useState<InventarioSnapshot>(emptySnapshot);
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [view, setView] = useState<InventoryView>("consulta");
  const [idItemPaquete, setIdItemPaquete] = useState("");
  const [dailyStockEntryMode, setDailyStockEntryMode] = useState<DailyStockEntryMode>("paquetes");
  const [cantidadPaquetes, setCantidadPaquetes] = useState("1");
  const [cantidadUnidadesSueltas, setCantidadUnidadesSueltas] = useState("1");
  const [idItemConsumo, setIdItemConsumo] = useState("");
  const [cantidadConsumida, setCantidadConsumida] = useState("1");
  const [observacionConsumo, setObservacionConsumo] = useState("");
  const [idItemAjuste, setIdItemAjuste] = useState("");
  const [sentidoAjuste, setSentidoAjuste] = useState<GeneralStockAdjustmentDirection>("entrada");
  const [cantidadAjuste, setCantidadAjuste] = useState("1");
  const [motivoAjuste, setMotivoAjuste] = useState(() =>
    role === "gerente" ? DEFAULT_STOCK_ADJUSTMENT_REASON.entrada : "",
  );
  const [adjustmentNotes, setAdjustmentNotes] = useState<Record<string, string>>({});
  const [resolvingAdjustmentId, setResolvingAdjustmentId] = useState<string | null>(null);
  const [submitMessage, setSubmitMessage] = useState<string | null>(null);
  const [lastAction, setLastAction] = useState<LastAction | null>(null);
  const [isSubmittingPackage, setIsSubmittingPackage] = useState(false);
  const [isSubmittingLooseUnits, setIsSubmittingLooseUnits] = useState(false);
  const [isSubmittingConsumption, setIsSubmittingConsumption] = useState(false);
  const [isSubmittingAdjustment, setIsSubmittingAdjustment] = useState(false);

  const canManageInventory = role === "administrador" || role === "gerente";
  const canApproveAdjustments = role === "gerente";
  const isManager = role === "gerente";
  const isStockEntry = sentidoAjuste === "entrada";
  const adjustmentFormTitle = isStockEntry ? "Entrada al stock general" : "Salida del stock general";
  const adjustmentFormDetail = isManager
    ? isStockEntry
      ? "Registra directamente las unidades recibidas con trazabilidad"
      : "Retira directamente unidades disponibles con trazabilidad"
    : `La solicitud de ${sentidoAjuste} queda pendiente de aprobacion gerencial`;
  const adjustmentSubmitLabel = isManager
    ? isStockEntry
      ? "Registrar entrada"
      : "Registrar salida"
    : isStockEntry
      ? "Solicitar entrada"
      : "Solicitar salida";
  const adjustmentSubmittingLabel = isManager ? "Registrando" : "Solicitando";
  const managementValue = isManager ? "Control" : canManageInventory ? "Solicitudes" : "Solo lectura";
  const openCashBoxId = snapshot.existenciasDiarias[0]?.idCajaDiaria ?? "";
  const hasOpenCashBox = Boolean(openCashBoxId);
  const viewHeading = {
    consulta: {
      eyebrow: "Inventario de la jornada",
      title: "Stock diario",
      lead: "Consulta las existencias y las ventas de vasos de la jornada abierta.",
    },
    paquetes: {
      eyebrow: "Reabastecimiento diario",
      title: "Cargar stock diario",
      lead: "Registra paquetes completos o unidades sueltas que ya existen en el stock general.",
    },
    consumo: {
      eyebrow: "Consumo operativo",
      title: "Consumo diario",
      lead: "Registra los desechables consumidos durante la jornada abierta.",
    },
    ajuste: {
      eyebrow: "Inventario general",
      title: "Ajustar stock general",
      lead: isManager
        ? "Registra entradas o salidas de productos directamente en el inventario."
        : "Solicita entradas o salidas de productos para aprobacion gerencial.",
    },
    movimientos: {
      eyebrow: "Trazabilidad de inventario",
      title: "Movimientos y solicitudes",
      lead: "Consulta los movimientos registrados y gestiona las solicitudes pendientes segun tu rol.",
    },
  }[view];

  const loadInventory = useCallback(async () => {
    setLoadState("loading");
    setErrorMessage(null);

    try {
      const response = await obtenerInventarioSnapshot(token);
      const orderedGeneralItems = orderInventoryItemsForDisplay(response.existenciasGenerales);
      const orderedPackageItems = orderedGeneralItems.filter(
        (item) => item.tipoControl === "automatico_por_venta" && Boolean(item.idTamanoVaso),
      );
      const orderedManualItems = orderedGeneralItems.filter(
        (item) => item.tipoControl === "manual_por_consumo",
      );
      setSnapshot(response);
      setLoadState("success");
      setIdItemPaquete((current) =>
        orderedPackageItems.some((item) => item.idItemInventario === current)
          ? current
          : (firstPackageItem(orderedGeneralItems)?.idItemInventario ?? ""),
      );
      setIdItemConsumo((current) =>
        orderedManualItems.some((item) => item.idItemInventario === current)
          ? current
          : (firstManualItem(orderedGeneralItems)?.idItemInventario ?? ""),
      );
      setIdItemAjuste((current) =>
        orderedGeneralItems.some((item) => item.idItemInventario === current)
          ? current
          : (orderedGeneralItems[0]?.idItemInventario ?? ""),
      );
    } catch (error) {
      setLoadState("error");
      setErrorMessage(messageFor(error));
    }
  }, [token]);

  useEffect(() => {
    void loadInventory();
  }, [loadInventory]);

  const generalItems = useMemo(
    () => orderInventoryItemsForDisplay(snapshot.existenciasGenerales),
    [snapshot.existenciasGenerales],
  );
  const packageItems = useMemo(
    () =>
      generalItems.filter(
        (item) => item.tipoControl === "automatico_por_venta" && Boolean(item.idTamanoVaso),
      ),
    [generalItems],
  );
  const manualItems = useMemo(
    () => generalItems.filter((item) => item.tipoControl === "manual_por_consumo"),
    [generalItems],
  );

  const pendingAdjustments = useMemo(
    () => snapshot.ajustes.filter((adjustment) => adjustment.estadoAprobacion === "pendiente").length,
    [snapshot.ajustes],
  );

  const totalDiario = useMemo(
    () => snapshot.existenciasDiarias.reduce((total, item) => total + item.cantidadFinalTeorica, 0),
    [snapshot.existenciasDiarias],
  );
  const dailyItems = useMemo(
    () => orderInventoryItemsForDisplay(snapshot.existenciasDiarias),
    [snapshot.existenciasDiarias],
  );
  const selectedPackageItem = packageItems.find((item) => item.idItemInventario === idItemPaquete);
  const selectedManualItem = manualItems.find((item) => item.idItemInventario === idItemConsumo);
  const selectedAdjustmentItem = generalItems.find((item) => item.idItemInventario === idItemAjuste);

  async function handlePackageSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitMessage(null);
    setLastAction(null);

    const paquetes = Number(cantidadPaquetes);

    if (!hasOpenCashBox) {
      setSubmitMessage("Abre una caja diaria para registrar paquetes de la jornada.");
      return;
    }

    if (!idItemPaquete || !Number.isInteger(paquetes) || paquetes < 1) {
      setSubmitMessage("Selecciona un vaso y una cantidad de paquetes valida.");
      return;
    }

    setIsSubmittingPackage(true);

    try {
      const response = await registrarPaqueteVasos(token, {
        cantidadPaquetes: paquetes,
        idItemInventario: idItemPaquete,
        unidadesRotas: 0,
      });
      setLastAction({ response, type: "paquete" });
      setCantidadPaquetes("1");
      await loadInventory();
    } catch (error) {
      setSubmitMessage(messageFor(error));
    } finally {
      setIsSubmittingPackage(false);
    }
  }

  async function handleLooseUnitsSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitMessage(null);
    setLastAction(null);

    const cantidad = Number(cantidadUnidadesSueltas);

    if (!hasOpenCashBox) {
      setSubmitMessage("Abre una caja diaria para reabastecer el stock diario.");
      return;
    }

    if (!idItemPaquete || !Number.isInteger(cantidad) || cantidad < 1) {
      setSubmitMessage("Selecciona un vaso y una cantidad de unidades valida.");
      return;
    }

    setIsSubmittingLooseUnits(true);

    try {
      const response = await solicitarAjusteInventario(token, {
        cantidadAjuste: cantidad,
        idItemInventario: idItemPaquete,
        motivoAjuste: DAILY_STOCK_REPLENISHMENT_REASON,
        sentidoAjuste: "entrada",
        tipoStock: "diario",
      });
      setLastAction({ response, type: "reabastecimiento-diario" });
      setCantidadUnidadesSueltas("1");
      await loadInventory();
    } catch (error) {
      setSubmitMessage(messageFor(error));
    } finally {
      setIsSubmittingLooseUnits(false);
    }
  }

  async function handleConsumptionSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitMessage(null);
    setLastAction(null);

    const cantidad = Number(cantidadConsumida);

    if (!hasOpenCashBox) {
      setSubmitMessage("Abre una caja diaria para registrar consumos de la jornada.");
      return;
    }

    if (!idItemConsumo || !Number.isInteger(cantidad) || cantidad < 1) {
      setSubmitMessage("Selecciona un item manual y una cantidad valida.");
      return;
    }

    setIsSubmittingConsumption(true);

    try {
      const response = await registrarConsumoDiario(token, {
        cantidadConsumida: cantidad,
        idItemInventario: idItemConsumo,
        observacion: observacionConsumo.trim() || undefined,
      });
      setLastAction({ response, type: "consumo" });
      setCantidadConsumida("1");
      setObservacionConsumo("");
      await loadInventory();
    } catch (error) {
      setSubmitMessage(messageFor(error));
    } finally {
      setIsSubmittingConsumption(false);
    }
  }

  async function handleAdjustmentSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitMessage(null);
    setLastAction(null);

    const cantidad = Number(cantidadAjuste);

    if (!idItemAjuste || !Number.isInteger(cantidad) || cantidad < 1) {
      setSubmitMessage("Selecciona un item y una cantidad de ajuste valida.");
      return;
    }

    if (!motivoAjuste.trim()) {
      setSubmitMessage("Indica el motivo del ajuste de inventario.");
      return;
    }

    setIsSubmittingAdjustment(true);

    try {
      const response = await solicitarAjusteInventario(token, {
        cantidadAjuste: cantidad,
        idItemInventario: idItemAjuste,
        motivoAjuste: motivoAjuste.trim(),
        sentidoAjuste,
        tipoStock: "general",
      });
      setLastAction({ response, type: "ajuste" });
      setCantidadAjuste("1");
      setMotivoAjuste(isManager ? DEFAULT_STOCK_ADJUSTMENT_REASON[sentidoAjuste] : "");
      await loadInventory();
    } catch (error) {
      setSubmitMessage(messageFor(error));
    } finally {
      setIsSubmittingAdjustment(false);
    }
  }

  function updateAdjustmentNote(idAjusteInventario: string, value: string) {
    setAdjustmentNotes((current) => ({ ...current, [idAjusteInventario]: value }));
  }

  async function handleResolveAdjustment(idAjusteInventario: string, action: "aprobar" | "rechazar") {
    setSubmitMessage(null);
    setLastAction(null);
    setResolvingAdjustmentId(idAjusteInventario);

    try {
      const request = {
        observacionAprobacion: adjustmentNotes[idAjusteInventario]?.trim() || undefined,
      };
      const response =
        action === "aprobar"
          ? await aprobarAjusteInventario(token, idAjusteInventario, request)
          : await rechazarAjusteInventario(token, idAjusteInventario, request);
      setLastAction({ response, type: action === "aprobar" ? "ajuste-aprobado" : "ajuste-rechazado" });
      setAdjustmentNotes((current) => {
        const next = { ...current };
        delete next[idAjusteInventario];
        return next;
      });
      await loadInventory();
    } catch (error) {
      setSubmitMessage(messageFor(error));
    } finally {
      setResolvingAdjustmentId(null);
    }
  }

  return (
    <>
      <section className="section-heading" aria-labelledby="inventario-title">
        <div>
          <p className="eyebrow">{viewHeading.eyebrow}</p>
          <h1 id="inventario-title">{viewHeading.title}</h1>
          <p className="lead">{viewHeading.lead}</p>
        </div>
        <button className="ghost-button" type="button" onClick={loadInventory} disabled={loadState === "loading"}>
          <RefreshCw size={17} strokeWidth={2.2} />
          Reintentar
        </button>
      </section>

      {errorMessage && loadState === "error" ? (
        <div className="form-alert" role="status">
          <Boxes size={18} strokeWidth={2.2} />
          <span>{errorMessage}</span>
        </div>
      ) : null}

      {submitMessage ? (
        <div className="form-alert" role="status">
          <ClipboardList size={18} strokeWidth={2.2} />
          <span>{submitMessage}</span>
        </div>
      ) : null}

      {lastAction ? (
        <div className="success-alert" role="status">
          <PackageOpen size={18} strokeWidth={2.2} />
          <span>
            {lastAction.type === "paquete"
              ? `Paquete registrado: ${formatDisplayName(lastAction.response.nombreItem)}, ${lastAction.response.unidadesDisponibles} unidades disponibles.`
              : lastAction.type === "reabastecimiento-diario"
                ? lastAction.response.estadoAprobacion === "aprobado"
                  ? `Stock diario reabastecido: ${formatDisplayName(lastAction.response.nombreItem)}, ${lastAction.response.cantidadAjuste} unidades ingresadas.`
                  : `Solicitud de reabastecimiento registrada: ${formatDisplayName(lastAction.response.nombreItem)}, ${lastAction.response.cantidadAjuste} unidades pendientes de aprobacion.`
              : lastAction.type === "consumo"
                ? `Consumo registrado: ${formatDisplayName(lastAction.response.nombreItem)}, ${lastAction.response.cantidadConsumida} unidades.`
              : lastAction.type === "ajuste"
                ? lastAction.response.estadoAprobacion === "aprobado"
                    ? lastAction.response.sentidoAjuste === "entrada"
                      ? `Entrada registrada: ${formatDisplayName(lastAction.response.nombreItem)}, ${lastAction.response.cantidadAjuste} unidades agregadas al stock general.`
                      : `Salida registrada: ${formatDisplayName(lastAction.response.nombreItem)}, ${lastAction.response.cantidadAjuste} unidades retiradas del stock general.`
                    : `Solicitud de ${lastAction.response.sentidoAjuste} registrada: ${formatDisplayName(lastAction.response.nombreItem)}, ${lastAction.response.cantidadAjuste} unidades pendientes de aprobacion.`
                  : lastAction.type === "ajuste-aprobado"
                    ? `Ajuste aprobado: ${formatDisplayName(lastAction.response.nombreItem)}, ${lastAction.response.cantidadAjuste} unidades.`
                    : `Ajuste rechazado: ${formatDisplayName(lastAction.response.nombreItem)}.`}
          </span>
        </div>
      ) : null}

      {canManageInventory ? (
        <div className="catalog-view-tabs inventory-view-tabs" role="tablist" aria-label="Vistas de inventario">
          <button
            className={view === "consulta" ? "active" : ""}
            type="button"
            role="tab"
            aria-selected={view === "consulta"}
            onClick={() => setView("consulta")}
          >
            Stock diario
          </button>
          <button
            className={view === "paquetes" ? "active" : ""}
            type="button"
            role="tab"
            aria-selected={view === "paquetes"}
            onClick={() => setView("paquetes")}
          >
            Cargar stock diario
          </button>
          <button
            className={view === "consumo" ? "active" : ""}
            type="button"
            role="tab"
            aria-selected={view === "consumo"}
            onClick={() => setView("consumo")}
          >
            Consumo diario
          </button>
          <button
            className={view === "ajuste" ? "active" : ""}
            type="button"
            role="tab"
            aria-selected={view === "ajuste"}
            onClick={() => setView("ajuste")}
          >
            Ajustar stock general
          </button>
          <button
            className={view === "movimientos" ? "active" : ""}
            type="button"
            role="tab"
            aria-selected={view === "movimientos"}
            onClick={() => setView("movimientos")}
          >
            Movimientos y solicitudes
          </button>
        </div>
      ) : null}

      <div className={`inventario-summary-grid ${view === "consulta" ? "" : "inventory-operation-hidden"}`}>
        <SummaryCard label="Referencias" value={snapshot.existenciasDiarias.length} detail="Productos en la jornada" />
        <SummaryCard label="Stock diario" value={totalDiario} detail="Unidades teoricas disponibles" />
        <SummaryCard label="Caja diaria" value={hasOpenCashBox ? "Abierta" : "Sin abrir"} detail="Estado de la jornada actual" />
      </div>

      <div className={`inventario-summary-grid ${view === "movimientos" ? "" : "inventory-operation-hidden"}`}>
        <SummaryCard label="Movimientos" value={snapshot.ajustes.length} detail="Registros de inventario" />
        <SummaryCard label="Pendientes" value={pendingAdjustments} detail="Solicitudes por resolver" />
        <SummaryCard label="Gestion" value={managementValue} detail="Acciones segun tu rol" />
      </div>

      {canManageInventory ? (
        <div
          className={`inventory-actions-grid ${
            view === "paquetes" || view === "consumo" || view === "ajuste"
              ? ""
              : "inventory-operation-hidden"
          }`}
        >
          <form
            className={`panel inventory-action-form ${view === "paquetes" ? "" : "inventory-operation-hidden"}`}
            onSubmit={dailyStockEntryMode === "paquetes" ? handlePackageSubmit : handleLooseUnitsSubmit}
          >
            <div className="panel-title">
              <div>
                <h2>Cargar vasos al stock diario</h2>
                <p>Reabastece la jornada con existencias ya contabilizadas en el stock general</p>
              </div>
              <PackageOpen size={22} strokeWidth={2.2} />
            </div>

            <InventoryItemSelector
              items={packageItems}
              selectedId={idItemPaquete}
              onSelect={setIdItemPaquete}
              ariaLabel="Vasos disponibles para abrir paquetes"
              emptyMessage="No hay vasos configurados para apertura de paquetes."
            />

            <div className="inventory-action-fields">
              <div className="inventory-entry-mode" role="group" aria-label="Tipo de carga al stock diario">
                <button
                  className={dailyStockEntryMode === "paquetes" ? "active" : ""}
                  type="button"
                  aria-pressed={dailyStockEntryMode === "paquetes"}
                  onClick={() => {
                    setDailyStockEntryMode("paquetes");
                    setSubmitMessage(null);
                    setLastAction(null);
                  }}
                >
                  Paquetes completos
                </button>
                <button
                  className={dailyStockEntryMode === "unidades" ? "active" : ""}
                  type="button"
                  aria-pressed={dailyStockEntryMode === "unidades"}
                  onClick={() => {
                    setDailyStockEntryMode("unidades");
                    setSubmitMessage(null);
                    setLastAction(null);
                  }}
                >
                  Unidades sueltas
                </button>
              </div>

              <SelectedInventoryItem
                item={selectedPackageItem}
                emptyMessage="Selecciona un vaso de la lista."
              />
              {dailyStockEntryMode === "paquetes" ? (
                <>
                  <div className="inventory-form-row single-field">
                    <label className="field-label">
                      Cantidad de paquetes
                      <div className="field-control plain">
                        <input
                          min="1"
                          step="1"
                          type="number"
                          value={cantidadPaquetes}
                          onChange={(event) => setCantidadPaquetes(event.target.value)}
                        />
                      </div>
                    </label>
                  </div>
                  <p className="inventory-operation-note">
                    Cada paquete descuenta {UNIDADES_POR_PAQUETE} vasos del stock general y los agrega al stock diario.
                  </p>
                </>
              ) : (
                <>
                  <div className="inventory-form-row single-field">
                    <label className="field-label">
                      Cantidad de unidades sueltas
                      <div className="field-control plain">
                        <input
                          min="1"
                          step="1"
                          type="number"
                          value={cantidadUnidadesSueltas}
                          onChange={(event) => setCantidadUnidadesSueltas(event.target.value)}
                        />
                      </div>
                    </label>
                  </div>
                  <p className="inventory-operation-note">
                    El sistema descuenta la misma cantidad del stock general y la suma al stock diario.
                    {isManager
                      ? " El reabastecimiento se aplica inmediatamente."
                      : " La solicitud queda pendiente de aprobacion gerencial."}
                  </p>
                </>
              )}
              {!hasOpenCashBox ? (
                <p className="inventory-operation-note">
                  Debes abrir la caja diaria antes de cargar vasos al stock de la jornada.
                </p>
              ) : null}
            </div>

            <button
              className="primary-button full"
              type="submit"
              disabled={
                packageItems.length === 0
                || !hasOpenCashBox
                || (dailyStockEntryMode === "paquetes" ? isSubmittingPackage : isSubmittingLooseUnits)
              }
            >
              <PackageOpen size={18} strokeWidth={2.2} />
              {dailyStockEntryMode === "paquetes"
                ? isSubmittingPackage
                  ? "Registrando"
                  : "Registrar paquetes"
                : isSubmittingLooseUnits
                  ? "Reabasteciendo"
                  : isManager
                    ? "Reabastecer stock diario"
                    : "Solicitar reabastecimiento"}
            </button>
          </form>

          <form
            className={`panel inventory-action-form ${view === "consumo" ? "" : "inventory-operation-hidden"}`}
            onSubmit={handleConsumptionSubmit}
          >
            <div className="panel-title">
              <div>
                <h2>Consumo diario de desechables</h2>
                <p>Items manuales desde stock general</p>
              </div>
              <ClipboardList size={22} strokeWidth={2.2} />
            </div>

            <InventoryItemSelector
              items={manualItems}
              selectedId={idItemConsumo}
              onSelect={setIdItemConsumo}
              ariaLabel="Desechables disponibles para registrar consumo"
              emptyMessage="No hay items configurados para consumo manual."
            />

            <div className="inventory-action-fields">
              <SelectedInventoryItem
                item={selectedManualItem}
                emptyMessage="Selecciona un item de la lista."
              />
              <label className="field-label">
                Cantidad consumida
                <div className="field-control plain">
                  <input
                    min="1"
                    step="1"
                    type="number"
                    value={cantidadConsumida}
                    onChange={(event) => setCantidadConsumida(event.target.value)}
                  />
                </div>
              </label>

              <label className="field-label">
                Observacion
                <div className="field-control plain">
                  <input
                    type="text"
                    value={observacionConsumo}
                    onChange={(event) => setObservacionConsumo(event.target.value)}
                    placeholder="Opcional"
                  />
                </div>
              </label>
            </div>

            <button
              className="primary-button full"
              type="submit"
              disabled={isSubmittingConsumption || manualItems.length === 0 || !hasOpenCashBox}
            >
              <ClipboardList size={18} strokeWidth={2.2} />
              {isSubmittingConsumption ? "Registrando" : "Registrar consumo"}
            </button>
          </form>

          <form
            className={`panel inventory-action-form ${view === "ajuste" ? "" : "inventory-operation-hidden"}`}
            onSubmit={handleAdjustmentSubmit}
          >
            <div className="panel-title">
              <div>
                <h2>{adjustmentFormTitle}</h2>
                <p>{adjustmentFormDetail}</p>
              </div>
              <SlidersHorizontal size={22} strokeWidth={2.2} />
            </div>

            <InventoryItemSelector
              items={generalItems}
              selectedId={idItemAjuste}
              onSelect={setIdItemAjuste}
              ariaLabel={`Productos disponibles para ${sentidoAjuste} en el stock general`}
              emptyMessage="No hay productos de inventario general disponibles."
            />

            <div className="inventory-action-fields">
              <SelectedInventoryItem
                item={selectedAdjustmentItem}
                emptyMessage="Selecciona un item de la lista."
              />
              <div className="inventory-form-row">
                <label className="field-label">
                  Tipo de movimiento
                  <div className="field-control plain">
                    <select
                      value={sentidoAjuste}
                      onChange={(event) => {
                        const nextDirection = event.target.value as GeneralStockAdjustmentDirection;
                        setSentidoAjuste(nextDirection);
                        setMotivoAjuste(
                          isManager ? DEFAULT_STOCK_ADJUSTMENT_REASON[nextDirection] : "",
                        );
                      }}
                    >
                      <option value="entrada">Entrada</option>
                      <option value="salida">Salida</option>
                    </select>
                  </div>
                </label>
                <label className="field-label">
                  {isStockEntry ? "Cantidad recibida" : "Cantidad retirada"}
                  <div className="field-control plain">
                    <input
                      min="1"
                      step="1"
                      type="number"
                      value={cantidadAjuste}
                      onChange={(event) => setCantidadAjuste(event.target.value)}
                    />
                  </div>
                </label>
              </div>

              <label className="field-label">
                Motivo
                <div className="field-control plain">
                  <input
                    type="text"
                    value={motivoAjuste}
                    onChange={(event) => setMotivoAjuste(event.target.value)}
                    placeholder={
                      isStockEntry
                        ? "Compra, reabastecimiento o recepcion"
                        : "Merma, devolucion, daño o retiro"
                    }
                    maxLength={1000}
                  />
                </div>
              </label>
            </div>

            <button
              className="primary-button full"
              type="submit"
              disabled={isSubmittingAdjustment || snapshot.existenciasGenerales.length === 0}
            >
              <SlidersHorizontal size={18} strokeWidth={2.2} />
              {isSubmittingAdjustment ? adjustmentSubmittingLabel : adjustmentSubmitLabel}
            </button>
          </form>
        </div>
      ) : (
        <article className="panel inventory-readonly-note">
          <Boxes size={22} strokeWidth={2.2} />
          <div>
            <h2>Gestion reservada</h2>
            <p>
              El acceso operativo a inventario queda reservado para administrador o gerente, con validacion final del
              sistema.
            </p>
          </div>
        </article>
      )}

      <div className={`inventory-panel-grid ${view === "consulta" ? "" : "inventory-operation-hidden"}`}>
        <article className="panel">
          <div className="panel-title">
              <div>
                <h2>Stock diario</h2>
              <p>Conteo operativo de la jornada abierta</p>
            </div>
            <span className="badge">{loadState === "loading" ? "Cargando" : `${snapshot.existenciasDiarias.length}`}</span>
          </div>
          <ul className="inventory-list">
            {dailyItems.length > 0 ? (
              dailyItems.map((item) => <DailyRow key={item.idExistenciaDiaria} item={item} />)
            ) : (
              <li className="inventory-empty">Sin stock diario cargado para la caja abierta.</li>
            )}
          </ul>
          {hasOpenCashBox ? (
            <section className="daily-sales-breakdown" aria-labelledby="ventas-vasos-title">
              <div className="daily-sales-heading">
                <div>
                  <h3 id="ventas-vasos-title">Ventas por tipo y tamano</h3>
                  <p>Referencia informativa: cada paquete equivale a {UNIDADES_POR_PAQUETE} vasos.</p>
                </div>
              </div>
              {snapshot.ventasVasosDiarias.length > 0 ? (
                <ul className="daily-sales-list">
                  {snapshot.ventasVasosDiarias.map((sale) => (
                    <DailySalesRow key={`${sale.nombreTipo}-${sale.onzas}`} sale={sale} />
                  ))}
                </ul>
              ) : (
                <p className="daily-sales-empty">No hay granizados vendidos en esta jornada.</p>
              )}
            </section>
          ) : null}
        </article>
      </div>

      <div className={`inventory-panel-grid ${view === "movimientos" ? "" : "inventory-operation-hidden"}`}>
        <article className="panel">
          <div className="panel-title">
            <div>
              <h2>Movimientos y solicitudes</h2>
              <p>Ajustes de stock general y reabastecimientos del stock diario</p>
            </div>
            <span className="badge">{loadState === "loading" ? "Cargando" : `${snapshot.ajustes.length}`}</span>
          </div>
          <ul className="adjustment-list">
            {snapshot.ajustes.length > 0 ? (
              snapshot.ajustes.slice(0, 20).map((adjustment) => (
                <AdjustmentRow
                  key={adjustment.idAjusteInventario}
                  adjustment={adjustment}
                  canApprove={canApproveAdjustments}
                  isResolving={resolvingAdjustmentId === adjustment.idAjusteInventario}
                  note={adjustmentNotes[adjustment.idAjusteInventario] ?? ""}
                  onNoteChange={(value) => updateAdjustmentNote(adjustment.idAjusteInventario, value)}
                  onApprove={() => void handleResolveAdjustment(adjustment.idAjusteInventario, "aprobar")}
                  onReject={() => void handleResolveAdjustment(adjustment.idAjusteInventario, "rechazar")}
                />
              ))
            ) : (
              <li className="inventory-empty">Sin ajustes de inventario registrados.</li>
            )}
          </ul>
        </article>
      </div>
    </>
  );
}

function firstPackageItem(items: ExistenciaInventarioGeneral[]) {
  return items.find((item) => item.tipoControl === "automatico_por_venta" && Boolean(item.idTamanoVaso));
}

function firstManualItem(items: ExistenciaInventarioGeneral[]) {
  return items.find((item) => item.tipoControl === "manual_por_consumo");
}
