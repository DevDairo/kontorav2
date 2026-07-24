import { AlertCircle, RefreshCw, Save } from "lucide-react";
import { type FormEvent, useCallback, useEffect, useState } from "react";
import { ApiClientError } from "../../../shared/services/apiClient";
import { normalizeMoneyInput } from "../../../shared/utils/moneyInput";
import { obtenerCajaAbierta } from "../../caja/services/cajaService";
import { obtenerAdicionDiariaCajaAbierta, registrarAdicionDiaria } from "../../gastos/services/gastosService";
import type { AdicionDiaria } from "../../gastos/types";

type LoadState = "loading" | "open" | "closed" | "error";

type AdicionesDiariasPanelProps = {
  token: string;
};

function formatCurrency(value: number) {
  return new Intl.NumberFormat("es-CO", {
    currency: "COP",
    maximumFractionDigits: 0,
    style: "currency",
  }).format(Number.isFinite(value) ? value : 0);
}

function messageFor(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.message;
  }

  return error instanceof Error ? error.message : "No fue posible consultar las adiciones diarias.";
}

export function AdicionesDiariasPanel({ token }: AdicionesDiariasPanelProps) {
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [adicion, setAdicion] = useState<AdicionDiaria | null>(null);
  const [cantidad, setCantidad] = useState("0");
  const [valorUnitario, setValorUnitario] = useState("1000");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const cargarAdiciones = useCallback(async () => {
    setLoadState("loading");
    setErrorMessage(null);

    try {
      await obtenerCajaAbierta(token);

      const response = await obtenerAdicionDiariaCajaAbierta(token);

      if (response) {
        setAdicion(response);
        setCantidad(String(response.cantidadAdiciones));
        setValorUnitario(String(response.valorUnitario));
      } else {
        setAdicion(null);
      }

      setLoadState("open");
    } catch (error) {
      setAdicion(null);

      if (error instanceof ApiClientError && error.status === 404) {
        setLoadState("closed");
        return;
      }

      setLoadState("error");
      setErrorMessage(messageFor(error));
    }
  }, [token]);

  useEffect(() => {
    void cargarAdiciones();
  }, [cargarAdiciones]);

  const totalCalculado = Number(cantidad || 0) * Number(valorUnitario || 0);
  const isCashBoxOpen = loadState === "open";

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const cantidadAdiciones = Number(cantidad);
    const valor = Number(valorUnitario || 0);

    if (!isCashBoxOpen) {
      setErrorMessage("Abre una caja diaria para registrar las adiciones de la jornada.");
      return;
    }

    if (!Number.isInteger(cantidadAdiciones) || cantidadAdiciones < 0 || !Number.isFinite(valor) || valor < 0) {
      setErrorMessage("La cantidad y el valor unitario deben ser valores validos iguales o mayores a cero.");
      return;
    }

    setIsSubmitting(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    try {
      const response = await registrarAdicionDiaria(token, {
        cantidadAdiciones,
        valorUnitario: valor,
      });
      setAdicion(response);
      setCantidad(String(response.cantidadAdiciones));
      setValorUnitario(String(response.valorUnitario));
      setSuccessMessage("Adiciones diarias actualizadas.");
    } catch (error) {
      setErrorMessage(messageFor(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="panel ventas-additions-panel" aria-labelledby="adiciones-title">
      <div className="panel-title">
        <div>
          <h2 id="adiciones-title">Adiciones diarias</h2>
          <p>{isCashBoxOpen ? "Registra los productos adicionales de la jornada abierta." : "Disponible cuando exista una caja diaria abierta."}</p>
        </div>
        <button
          className="icon-button"
          type="button"
          onClick={() => void cargarAdiciones()}
          disabled={loadState === "loading"}
          aria-label="Actualizar adiciones"
          title="Actualizar adiciones"
        >
          <RefreshCw size={18} strokeWidth={2.2} />
        </button>
      </div>

      <form className="ventas-additions-form" onSubmit={handleSubmit}>
        <div className="ventas-additions-fields">
          <label className="field-label">
            Cantidad
            <div className="field-control plain">
              <input
                min="0"
                step="1"
                type="number"
                value={cantidad}
                onChange={(event) => setCantidad(event.target.value)}
                disabled={!isCashBoxOpen}
                required
              />
            </div>
          </label>
          <label className="field-label">
            Valor unitario
            <div className="field-control plain">
              <input
                inputMode="decimal"
                type="text"
                value={valorUnitario}
                onChange={(event) => setValorUnitario(normalizeMoneyInput(event.target.value))}
                disabled={!isCashBoxOpen}
                required
              />
            </div>
          </label>
        </div>

        <div className="gastos-calculated-total">
          <span>Total calculado</span>
          <strong>{formatCurrency(adicion?.valorTotal ?? totalCalculado)}</strong>
        </div>

        <button className="primary-button full" type="submit" disabled={!isCashBoxOpen || isSubmitting}>
          <Save size={18} strokeWidth={2.2} />
          {isSubmitting ? "Guardando" : "Guardar adiciones"}
        </button>
      </form>

      {loadState === "closed" ? <p className="empty-copy">Abre una caja diaria para habilitar este registro.</p> : null}
      {errorMessage ? (
        <div className="form-alert" role="status">
          <AlertCircle size={18} strokeWidth={2.2} />
          <span>{errorMessage}</span>
        </div>
      ) : null}
      {successMessage ? <p className="success-copy">{successMessage}</p> : null}
    </section>
  );
}
