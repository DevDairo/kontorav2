import { RefreshCw, WalletCards } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { ApiClientError } from "../../../shared/services/apiClient";
import { obtenerGastosSnapshot } from "../../gastos/services/gastosService";
import type { GastosSnapshot } from "../../gastos/types";

type LoadState = "loading" | "success" | "no-cash-box" | "error";

type CajaOperacionesPanelProps = {
  token: string;
};

function emptySnapshot(): GastosSnapshot {
  return { adicion: null, gastos: [], pagoTrabajadores: null, resumenCaja: null };
}

function formatCurrency(value: number | null | undefined) {
  return new Intl.NumberFormat("es-CO", {
    currency: "COP",
    maximumFractionDigits: 0,
    style: "currency",
  }).format(Number(value ?? 0));
}

function messageFor(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.message;
  }

  return error instanceof Error ? error.message : "No fue posible consultar las operaciones de caja";
}

function SummaryCard({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <article className="gastos-summary-card">
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{detail}</small>
    </article>
  );
}

export function CajaOperacionesPanel({ token }: CajaOperacionesPanelProps) {
  const [snapshot, setSnapshot] = useState<GastosSnapshot>(emptySnapshot);
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const loadSnapshot = useCallback(async () => {
    setLoadState("loading");
    setErrorMessage(null);

    try {
      const response = await obtenerGastosSnapshot(token, true);
      setSnapshot(response);
      setLoadState("success");
    } catch (error) {
      setSnapshot(emptySnapshot());

      if (error instanceof ApiClientError && error.status === 409) {
        setLoadState("no-cash-box");
        setErrorMessage(error.message);
        return;
      }

      setLoadState("error");
      setErrorMessage(messageFor(error));
    }
  }, [token]);

  useEffect(() => {
    void loadSnapshot();
  }, [loadSnapshot]);

  const resumenCaja = snapshot.resumenCaja;
  const pagoTrabajadores = snapshot.pagoTrabajadores;
  const gastosActivos = useMemo(
    () => snapshot.gastos.filter((gasto) => gasto.estadoGasto !== "anulado"),
    [snapshot.gastos],
  );

  return (
    <section className="caja-operaciones-section" aria-labelledby="caja-operaciones-title">
      <div className="caja-operaciones-heading">
        <div>
          <p className="eyebrow">Control financiero</p>
          <h2 id="caja-operaciones-title">Operaciones de caja diaria</h2>
          <p>Proyeccion de efectivo y obligaciones registradas para la caja abierta.</p>
        </div>
        <button className="ghost-button" type="button" onClick={loadSnapshot} disabled={loadState === "loading"}>
          <RefreshCw size={17} strokeWidth={2.2} />
          Actualizar
        </button>
      </div>

      {errorMessage ? (
        <div className="form-alert" role="status">
          <WalletCards size={18} strokeWidth={2.2} />
          <span>{errorMessage}</span>
        </div>
      ) : null}

      <section className="gastos-summary-grid" aria-label="Resumen de operaciones de caja">
        <SummaryCard
          label="Gastos activos"
          value={formatCurrency(resumenCaja?.totalGastos)}
          detail={`${gastosActivos.length} registro${gastosActivos.length === 1 ? "" : "s"}; se gestionan desde Gastos`}
        />
        <SummaryCard
          label="Adiciones"
          value={formatCurrency(resumenCaja?.totalAdiciones)}
          detail={snapshot.adicion ? `${snapshot.adicion.cantidadAdiciones} adiciones registradas desde Ventas` : "Sin registro desde Ventas"}
        />
        <SummaryCard
          label="Pago trabajadores"
          value={formatCurrency(pagoTrabajadores?.valorTotalPagado)}
          detail={pagoTrabajadores?.confirmadoParaCierre ? "Confirmado; descuenta efectivo esperado" : "Pendiente de confirmar"}
        />
      </section>

      {resumenCaja ? (
        <section className="gastos-cash-flow" aria-labelledby="caja-cash-flow-title">
          <div className="gastos-cash-flow-heading">
            <div>
              <p className="eyebrow">Caja diaria</p>
              <h2 id="caja-cash-flow-title">Proyeccion de efectivo fisico</h2>
            </div>
            <span className={`badge ${resumenCaja.listoParaCierre ? "success" : "warning"}`}>
              {resumenCaja.listoParaCierre ? "Requisitos de cierre completos" : "Pendiente para cierre"}
            </span>
          </div>

          <div className="gastos-cash-flow-content">
            <div className="gastos-cash-flow-lines" aria-label="Formula de efectivo esperado sin base">
              <div className="gastos-flow-line income">
                <span>Ventas en efectivo</span>
                <strong>+ {formatCurrency(resumenCaja.totalVentasEfectivo)}</strong>
              </div>
              <div className="gastos-flow-line income">
                <span>Adiciones</span>
                <strong>+ {formatCurrency(resumenCaja.totalAdiciones)}</strong>
              </div>
              <div className="gastos-flow-line expense">
                <span>Gastos activos</span>
                <strong>- {formatCurrency(resumenCaja.totalGastos)}</strong>
              </div>
              <div className="gastos-flow-line expense">
                <span>Pago a trabajadores</span>
                <strong>- {formatCurrency(resumenCaja.totalPagoTrabajadores)}</strong>
              </div>
              <div className="gastos-flow-line total">
                <span>Efectivo esperado sin base</span>
                <strong>{formatCurrency(resumenCaja.efectivoEsperadoSinBase)}</strong>
              </div>
            </div>

            <dl className="gastos-cash-context">
              <div>
                <dt>Total de ventas</dt>
                <dd>{formatCurrency(resumenCaja.totalVentas)}</dd>
              </div>
              <div>
                <dt>Transferencias</dt>
                <dd>{formatCurrency(resumenCaja.totalVentasTransferencia)}</dd>
              </div>
              <div>
                <dt>Base de caja</dt>
                <dd>{formatCurrency(resumenCaja.valorBase)}</dd>
              </div>
            </dl>
          </div>

          <p className="gastos-cash-flow-note">
            Las transferencias no son efectivo fisico y la base se conserva. El deposito se define en Cierre con el efectivo contado sin base, no con esta proyeccion.
          </p>
          {!resumenCaja.listoParaCierre ? (
            <p className="gastos-cash-flow-warning">
              {!resumenCaja.adicionDiariaRegistrada ? "Falta registrar las adiciones desde Ventas. " : ""}
              {!resumenCaja.pagoTrabajadoresRegistrado
                ? "Falta registrar el pago diario a trabajadores."
                : !resumenCaja.pagoTrabajadoresConfirmado
                  ? "Falta confirmar el pago diario a trabajadores."
                  : resumenCaja.efectivoEsperadoSinBase < 0
                    ? "El efectivo esperado sin base no puede ser negativo."
                    : ""}
            </p>
          ) : null}
        </section>
      ) : null}

    </section>
  );
}
