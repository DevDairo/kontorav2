import { AlertCircle, CheckCircle2, RotateCcw } from "lucide-react";
import { FormEvent, useEffect, useMemo, useState } from "react";
import type { UserRole } from "../../../app/routes/appRoutes";
import { ConfirmationDialog } from "../../../shared/components/ConfirmationDialog";
import { ApiClientError } from "../../../shared/services/apiClient";
import { solicitarAjusteInventario } from "../services/inventarioService";
import type { ExistenciaInventarioDiario } from "../types";

type DevolucionStockDiarioPanelProps = {
  items: ExistenciaInventarioDiario[];
  onCompleted: () => Promise<void>;
  role: UserRole | null;
  token: string;
};

type DailyStockReturnMode = "paquetes" | "unidades";

const UNIDADES_POR_PAQUETE = 20;

function messageFor(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.message;
  }
  return error instanceof Error ? error.message : "No fue posible devolver el inventario.";
}

function itemLabel(item: ExistenciaInventarioDiario) {
  return item.nombreItem;
}

export function DevolucionStockDiarioPanel({
  items,
  onCompleted,
  role,
  token,
}: DevolucionStockDiarioPanelProps) {
  const availableItems = useMemo(
    () => items.filter((item) => item.cantidadFinalTeorica > 0),
    [items],
  );
  const [idItemInventario, setIdItemInventario] = useState("");
  const [returnMode, setReturnMode] = useState<DailyStockReturnMode>("paquetes");
  const [cantidad, setCantidad] = useState("1");
  const [motivo, setMotivo] = useState("Correccion de carga al stock diario");
  const [pendingConfirmation, setPendingConfirmation] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const isManager = role === "gerente";

  useEffect(() => {
    setIdItemInventario((current) =>
      availableItems.some((item) => item.idItemInventario === current)
        ? current
        : (availableItems[0]?.idItemInventario ?? ""),
    );
  }, [availableItems]);

  const selectedItem = availableItems.find((item) => item.idItemInventario === idItemInventario);
  const enteredQuantity = Number(cantidad);
  const unitsToReturn = returnMode === "paquetes"
    ? enteredQuantity * UNIDADES_POR_PAQUETE
    : enteredQuantity;
  const maximumEnteredQuantity = selectedItem
    ? returnMode === "paquetes"
      ? Math.floor(selectedItem.cantidadFinalTeorica / UNIDADES_POR_PAQUETE)
      : selectedItem.cantidadFinalTeorica
    : undefined;
  const validQuantity = Boolean(
    selectedItem
    && Number.isInteger(enteredQuantity)
    && enteredQuantity >= 1
    && unitsToReturn <= selectedItem.cantidadFinalTeorica,
  );
  const enteredQuantityLabel = returnMode === "paquetes"
    ? `${Number.isInteger(enteredQuantity) && enteredQuantity > 0 ? enteredQuantity : 0} ${
      enteredQuantity === 1 ? "paquete" : "paquetes"
    } de ${UNIDADES_POR_PAQUETE} vasos (${Number.isFinite(unitsToReturn) ? unitsToReturn : 0} vasos)`
    : `${Number.isInteger(unitsToReturn) && unitsToReturn > 0 ? unitsToReturn : 0} ${
      unitsToReturn === 1 ? "vaso" : "vasos"
    } por unidad`;

  function selectReturnMode(nextMode: DailyStockReturnMode) {
    setReturnMode(nextMode);
    setCantidad("1");
    setMessage(null);
    setErrorMessage(null);
  }

  function requestReturn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(null);
    setErrorMessage(null);

    if (!selectedItem || !validQuantity) {
      setErrorMessage(
        returnMode === "paquetes"
          ? `Selecciona una cantidad valida de paquetes. Cada paquete equivale a ${UNIDADES_POR_PAQUETE} vasos y no puede superar el stock diario disponible.`
          : "Selecciona un producto y una cantidad disponible valida.",
      );
      return;
    }
    if (!motivo.trim()) {
      setErrorMessage("Indica el motivo de la devolucion.");
      return;
    }
    setPendingConfirmation(true);
  }

  async function confirmReturn() {
    if (!selectedItem) {
      return;
    }
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      const response = await solicitarAjusteInventario(token, {
        cantidadAjuste: unitsToReturn,
        idItemInventario: selectedItem.idItemInventario,
        motivoAjuste: motivo.trim(),
        sentidoAjuste: "salida",
        tipoStock: "diario",
      });
      setMessage(
        response.estadoAprobacion === "aprobado"
          ? `${enteredQuantityLabel} regresaron del stock diario al general.`
          : `Solicitud registrada por ${enteredQuantityLabel}; queda pendiente de aprobacion gerencial.`,
      );
      setCantidad("1");
      await onCompleted();
    } catch (error) {
      setErrorMessage(messageFor(error));
    } finally {
      setIsSubmitting(false);
      setPendingConfirmation(false);
    }
  }

  return (
    <section className="panel operation-panel inventory-return-panel" aria-labelledby="inventory-return-title">
      <div className="panel-title">
        <div>
          <h2 id="inventory-return-title">Devolver al stock general</h2>
          <p>Corrige una carga equivocada sin borrar movimientos ni alterar el historial.</p>
        </div>
        <RotateCcw size={22} strokeWidth={2.2} />
      </div>

      {errorMessage ? (
        <div className="form-alert" role="status">
          <AlertCircle size={18} />
          <span>{errorMessage}</span>
        </div>
      ) : null}
      {message ? (
        <div className="success-alert" role="status">
          <CheckCircle2 size={18} />
          <span>{message}</span>
        </div>
      ) : null}

      <form className="operation-form inventory-workspace-form inventory-return-form" onSubmit={requestReturn}>
        <section className="inventory-item-selector" aria-label="Stock diario disponible para devolver">
          <div className="inventory-item-selector-heading">
            <strong>Selecciona un producto</strong>
            <small>Solo aparecen referencias con existencia diaria disponible.</small>
          </div>
          <div className="inventory-item-table">
            <div className="inventory-item-table-header" aria-hidden="true">
              <span>N.º</span>
              <span>Producto</span>
              <span>Disponible</span>
            </div>
            <div className="inventory-item-options" role="group">
              {availableItems.map((item, index) => (
                <button
                  className={item.idItemInventario === idItemInventario ? "selected" : ""}
                  key={item.idItemInventario}
                  onClick={() => setIdItemInventario(item.idItemInventario)}
                  type="button"
                >
                  <span className="inventory-item-number">{index + 1}</span>
                  <span className="inventory-item-identity">
                    <strong>{itemLabel(item)}</strong>
                    <small>Stock diario</small>
                  </span>
                  <b>{item.cantidadFinalTeorica}</b>
                </button>
              ))}
            </div>
          </div>
          {availableItems.length === 0 ? (
            <p className="inventory-empty">No hay unidades diarias disponibles para devolver.</p>
          ) : null}
        </section>

        <div className="inventory-action-fields">
          <div className="inventory-entry-mode" role="group" aria-label="Forma de calcular la devolucion">
            <button
              aria-pressed={returnMode === "paquetes"}
              className={returnMode === "paquetes" ? "active" : ""}
              onClick={() => selectReturnMode("paquetes")}
              type="button"
            >
              Paquetes ({UNIDADES_POR_PAQUETE} vasos)
            </button>
            <button
              aria-pressed={returnMode === "unidades"}
              className={returnMode === "unidades" ? "active" : ""}
              onClick={() => selectReturnMode("unidades")}
              type="button"
            >
              Vasos por unidad
            </button>
          </div>
          <div className="inventory-selected-item">
            <span>Producto seleccionado</span>
            <strong>{selectedItem ? itemLabel(selectedItem) : "Sin seleccionar"}</strong>
            <small>
              {selectedItem
                ? `${selectedItem.cantidadFinalTeorica} vasos disponibles · ${Math.floor(
                  selectedItem.cantidadFinalTeorica / UNIDADES_POR_PAQUETE,
                )} paquetes completos`
                : "Selecciona una referencia."}
            </small>
          </div>
          <label className="field-label">
            {returnMode === "paquetes" ? "Cantidad de paquetes" : "Cantidad de vasos"}
            <div className="field-control plain">
              <input
                aria-describedby="inventory-return-equivalence"
                max={maximumEnteredQuantity}
                min="1"
                step="1"
                type="number"
                value={cantidad}
                onChange={(event) => setCantidad(event.target.value)}
              />
            </div>
          </label>
          <p
            className="inventory-return-equivalence"
            id="inventory-return-equivalence"
            aria-live="polite"
          >
            {returnMode === "paquetes"
              ? `${enteredQuantityLabel}. Máximo disponible: ${maximumEnteredQuantity ?? 0} paquetes completos.`
              : `${enteredQuantityLabel}. Máximo disponible: ${maximumEnteredQuantity ?? 0} vasos.`}
          </p>
          <label className="field-label">
            Motivo
            <div className="field-control plain">
              <input
                maxLength={1000}
                value={motivo}
                onChange={(event) => setMotivo(event.target.value)}
              />
            </div>
          </label>
          <p className="inventory-operation-note">
            {isManager
              ? "La devolucion se aplica inmediatamente: resta del diario y suma al general."
              : "La solicitud no cambia existencias hasta que el gerente la apruebe."}
          </p>
        </div>

        <div className="inventory-workspace-actions inventory-return-actions">
          <button
            className="primary-button inventory-workspace-submit inventory-return-submit"
            disabled={isSubmitting || availableItems.length === 0 || !validQuantity}
            type="submit"
          >
            <RotateCcw size={18} />
            {isManager ? "Devolver al general" : "Solicitar devolucion"}
          </button>
        </div>
      </form>

      <ConfirmationDialog
        confirmLabel={isManager ? "Confirmar devolucion" : "Enviar solicitud"}
        description={
          selectedItem
            ? `${enteredQuantityLabel} de ${itemLabel(selectedItem)} saldran del stock diario y regresaran al stock general${isManager ? "" : " cuando el gerente apruebe la solicitud"}.`
            : ""
        }
        isConfirming={isSubmitting}
        onCancel={() => setPendingConfirmation(false)}
        onConfirm={() => void confirmReturn()}
        open={pendingConfirmation}
        title="Confirmar movimiento de inventario"
      />
    </section>
  );
}
