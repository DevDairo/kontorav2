import { AlertCircle, ArrowLeft, Banknote, CalendarDays, CheckCircle2, ClipboardList, RefreshCw, Save, WalletCards } from "lucide-react";
import { type FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { obtenerCajaAbierta } from "../../caja/services/cajaService";
import type { CajaDiaria } from "../../caja/types";
import { ConfirmationDialog } from "../../../shared/components/ConfirmationDialog";
import { ApiClientError } from "../../../shared/services/apiClient";
import { normalizeMoneyInput } from "../../../shared/utils/moneyInput";
import { cerrarCajaDiaria, consultarCierrePorFecha, obtenerResumenCajaAbierta } from "../services/cierreService";
import type { CierreCaja, ConsultaCierreDiario, ResumenCajaDiaria } from "../types";

type LoadState = "loading" | "ready" | "no-cash-box" | "error" | "closed";
type CierreSource = "current" | "history" | null;

const LAST_CLOSURE_DATE_KEY = "kontora.cierre.ultima-fecha";

type CierreCajaPanelProps = {
  token: string;
};

function formatCurrency(value: number | null | undefined) {
  return new Intl.NumberFormat("es-CO", {
    currency: "COP",
    maximumFractionDigits: 0,
    style: "currency",
  }).format(Number(value ?? 0));
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("es-CO", { dateStyle: "medium" }).format(new Date(`${value}T00:00:00`));
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("es-CO", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function previousLocalDate() {
  const yesterday = new Date();
  yesterday.setDate(yesterday.getDate() - 1);
  const year = yesterday.getFullYear();
  const month = String(yesterday.getMonth() + 1).padStart(2, "0");
  const day = String(yesterday.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function defaultHistoryDate() {
  return window.sessionStorage.getItem(LAST_CLOSURE_DATE_KEY) ?? previousLocalDate();
}

function cierreFromConsulta(consulta: ConsultaCierreDiario): CierreCaja {
  return {
    idCierreCaja: consulta.idCierreCaja,
    idCajaDiaria: consulta.idCajaDiaria,
    totalVentas: consulta.totalVentas,
    totalVentasEfectivo: consulta.totalVentasEfectivo,
    totalVentasTransferencia: consulta.totalVentasTransferencia,
    totalTransferenciasPendientes: consulta.totalTransferenciasPendientes,
    totalTransferenciasValidadas: consulta.totalTransferenciasValidadas,
    totalTransferenciasRechazadas: consulta.totalTransferenciasRechazadas,
    totalGastos: consulta.totalGastos,
    totalAdiciones: consulta.totalAdiciones,
    totalPagoTrabajadores: consulta.totalPagoTrabajadores,
    efectivoEsperadoSinBase: consulta.efectivoEsperadoSinBase,
    efectivoContadoSinBase: consulta.efectivoContadoSinBase,
    diferenciaCaja: consulta.diferenciaCaja,
    valorADeposito: consulta.valorADeposito,
    fechaCierre: consulta.fechaCierre,
    idUsuarioCierre: consulta.idUsuarioCierre,
    nombreUsuarioCierre: consulta.nombreUsuarioCierre,
    observaciones: consulta.observaciones,
    movimientoDeposito: null,
  };
}

function messageFor(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.message;
  }

  return error instanceof Error ? error.message : "No fue posible consultar el cierre de caja";
}

function SummaryCard({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <article className="cierre-summary-card">
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{detail}</small>
    </article>
  );
}

export function CierreCajaPanel({ token }: CierreCajaPanelProps) {
  const [caja, setCaja] = useState<CajaDiaria | null>(null);
  const [resumen, setResumen] = useState<ResumenCajaDiaria | null>(null);
  const [cierre, setCierre] = useState<CierreCaja | null>(null);
  const [cierreSource, setCierreSource] = useState<CierreSource>(null);
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [efectivoContado, setEfectivoContado] = useState("");
  const [observaciones, setObservaciones] = useState("");
  const [confirmado, setConfirmado] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isCloseConfirmationVisible, setIsCloseConfirmationVisible] = useState(false);
  const [isHistoryMode, setIsHistoryMode] = useState(false);
  const [fechaConsultaCierre, setFechaConsultaCierre] = useState(defaultHistoryDate);

  const loadCierrePorFecha = useCallback(
    async (fechaOperacion: string) => {
      const consulta = await consultarCierrePorFecha(token, fechaOperacion);
      setCaja(null);
      setResumen(null);
      setCierre(cierreFromConsulta(consulta));
      setCierreSource("history");
      setIsHistoryMode(false);
      setFechaConsultaCierre(consulta.fechaOperacion);
      setLoadState("closed");
      setErrorMessage(null);
    },
    [token],
  );

  const loadSnapshot = useCallback(async () => {
    setLoadState("loading");
    setErrorMessage(null);
    setActionMessage(null);

    try {
      const cajaAbierta = await obtenerCajaAbierta(token);
      const resumenCaja = await obtenerResumenCajaAbierta(token);
      setCaja(cajaAbierta);
      setResumen(resumenCaja);
      setCierre(null);
      setCierreSource(null);
      setLoadState("ready");
    } catch (error) {
      setCaja(null);
      setResumen(null);

      if (error instanceof ApiClientError && error.status === 404) {
        const fechaRecordada = defaultHistoryDate();
        setFechaConsultaCierre(fechaRecordada);

        try {
          await loadCierrePorFecha(fechaRecordada);
        } catch (consultaError) {
          setCierre(null);
          setCierreSource(null);
          setLoadState("no-cash-box");
          setErrorMessage(
            consultaError instanceof ApiClientError && consultaError.status === 404
              ? "No existe una caja diaria abierta ni un cierre registrado para la fecha consultada."
              : messageFor(consultaError),
          );
        }
        return;
      }

      setLoadState("error");
      setErrorMessage(messageFor(error));
    }
  }, [loadCierrePorFecha, token]);

  useEffect(() => {
    void loadSnapshot();
  }, [loadSnapshot]);

  useEffect(() => {
    const refreshOnFocus = () => {
      if (!isHistoryMode) {
        void loadSnapshot();
      }
    };

    window.addEventListener("focus", refreshOnFocus);
    return () => window.removeEventListener("focus", refreshOnFocus);
  }, [isHistoryMode, loadSnapshot]);

  const efectivoContadoNumerico = Number(efectivoContado);
  const efectivoEsperadoNoNegativo = resumen !== null && resumen.efectivoEsperadoSinBase >= 0;
  const tieneConteoValido = efectivoContado.trim() !== "" && Number.isFinite(efectivoContadoNumerico) && efectivoContadoNumerico >= 0;
  const diferenciaEstimada = efectivoEsperadoNoNegativo && tieneConteoValido ? efectivoContadoNumerico - resumen.efectivoEsperadoSinBase : null;
  const requisitosPendientes = useMemo(() => {
    if (!resumen) {
      return [];
    }

    return [
      !resumen.adicionDiariaRegistrada ? "Falta registrar las adiciones de la jornada desde Ventas." : null,
      !resumen.pagoTrabajadoresRegistrado ? "Falta registrar el pago diario a trabajadores." : null,
      resumen.pagoTrabajadoresRegistrado && !resumen.pagoTrabajadoresConfirmado
        ? "Falta confirmar el pago diario a trabajadores."
        : null,
      resumen.efectivoEsperadoSinBase < 0 ? "El efectivo esperado sin base no puede ser negativo." : null,
    ].filter((item): item is string => item !== null);
  }, [resumen]);

  async function handleCerrarCaja(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!caja || !resumen || !tieneConteoValido) {
      setActionMessage("Ingresa un efectivo contado sin base valido para cerrar la caja.");
      return;
    }

    if (!efectivoEsperadoNoNegativo) {
      setActionMessage("No se puede registrar el conteo mientras el efectivo esperado sin base sea negativo.");
      return;
    }

    if (!resumen.listoParaCierre) {
      setActionMessage("Completa los requisitos operativos antes de cerrar la caja.");
      return;
    }

    if (!confirmado) {
      setActionMessage("Confirma que el conteo fisico no incluye la base de caja.");
      return;
    }

    setActionMessage(null);
    setIsCloseConfirmationVisible(true);
  }

  async function confirmCerrarCaja() {
    if (!caja || !resumen || !tieneConteoValido) {
      setIsCloseConfirmationVisible(false);
      setActionMessage("No fue posible confirmar el cierre con los datos actuales.");
      return;
    }

    setIsSubmitting(true);
    setActionMessage(null);

    try {
      const response = await cerrarCajaDiaria(token, caja.idCajaDiaria, {
        efectivoContadoSinBase: efectivoContadoNumerico,
        observaciones: observaciones.trim() || undefined,
      });
      setCierre(response);
      setCierreSource("current");
      setIsHistoryMode(false);
      setFechaConsultaCierre(caja.fechaOperacion);
      window.sessionStorage.setItem(LAST_CLOSURE_DATE_KEY, caja.fechaOperacion);
      setLoadState("closed");
      setIsCloseConfirmationVisible(false);
      setActionMessage("Caja cerrada y resultado registrado por el sistema.");
    } catch (error) {
      setActionMessage(messageFor(error));
      setIsCloseConfirmationVisible(false);
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleConsultarCierre(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!fechaConsultaCierre) {
      setErrorMessage("Selecciona la fecha de la caja cerrada que deseas consultar.");
      return;
    }

    setLoadState("loading");
    setErrorMessage(null);
    setActionMessage(null);

    try {
      await loadCierrePorFecha(fechaConsultaCierre);
    } catch (error) {
      setCierre(null);
      setCierreSource(null);
      setLoadState("no-cash-box");
      setErrorMessage(messageFor(error));
    }
  }

  function handleAbrirHistorial() {
    setCierre(null);
    setCierreSource(null);
    setErrorMessage(null);
    setActionMessage(null);
    setFechaConsultaCierre(defaultHistoryDate());
    setIsHistoryMode(true);
    setLoadState("no-cash-box");
  }

  function handleVolverOperacion() {
    setIsHistoryMode(false);
    void loadSnapshot();
  }

  const historySearchPanel = (
    <section className="panel cierre-history-panel" aria-labelledby="cierre-history-title">
      <div className="panel-title">
        <div>
          <h2 id="cierre-history-title">Consultar cierre por fecha</h2>
          <p>Selecciona una jornada cerrada para revisar su resultado sin modificar la operacion actual.</p>
        </div>
        <CalendarDays size={22} strokeWidth={2.2} />
      </div>
      <form className="cierre-history-form" onSubmit={handleConsultarCierre}>
        <label className="field-label">
          Fecha de operacion
          <div className="field-control plain">
            <input
              type="date"
              value={fechaConsultaCierre}
              onChange={(event) => setFechaConsultaCierre(event.target.value)}
              required
            />
          </div>
        </label>
        <button className="primary-button" type="submit">
          <ClipboardList size={18} strokeWidth={2.2} />
          Consultar cierre
        </button>
      </form>
    </section>
  );

  if (isHistoryMode) {
    return (
      <>
        <section className="section-heading" aria-labelledby="cierre-history-page-title">
          <div>
            <p className="eyebrow">Consultas operativas</p>
            <h1 id="cierre-history-page-title">Historial de cierres</h1>
            <p className="lead">Consulta una jornada cerrada por fecha. Esta vista no altera la caja que pueda estar abierta.</p>
          </div>
          <button className="ghost-button" type="button" onClick={handleVolverOperacion} disabled={loadState === "loading"}>
            <ArrowLeft size={17} strokeWidth={2.2} />
            Volver a operacion actual
          </button>
        </section>

        {errorMessage ? (
          <div className="form-alert" role="status">
            <AlertCircle size={18} strokeWidth={2.2} />
            <span>{errorMessage}</span>
          </div>
        ) : null}

        {historySearchPanel}
      </>
    );
  }

  if (cierre) {
    const hayDiferencia = cierre.diferenciaCaja !== 0;

    return (
      <>
        <section className="section-heading" aria-labelledby="cierre-title">
          <div>
            <p className="eyebrow">Cierre de jornada</p>
            <h1 id="cierre-title">Caja cerrada</h1>
            <p className="lead">
              {cierreSource === "history"
                ? "Consulta persistente del cierre de la fecha seleccionada."
                : "El sistema consolidó el arqueo y registró el resultado de depósito que corresponde."}
            </p>
          </div>
          <div className="cierre-heading-actions">
            <button className="ghost-button" type="button" onClick={handleAbrirHistorial}>
              <CalendarDays size={17} strokeWidth={2.2} />
              Consultar cierres
            </button>
            <button className="ghost-button" type="button" onClick={loadSnapshot} disabled={loadState === "loading"}>
              <RefreshCw size={17} strokeWidth={2.2} />
              Actualizar estado
            </button>
          </div>
        </section>

        {actionMessage ? (
          <div className="form-alert" role="status">
            <CheckCircle2 size={18} strokeWidth={2.2} />
            <span>{actionMessage}</span>
          </div>
        ) : null}

        <section className="cierre-result-panel" aria-labelledby="cierre-result-title">
          <div className="cierre-result-heading">
            <div>
              <p className="eyebrow">Resultado confirmado</p>
              <h2 id="cierre-result-title">Arqueo de {formatDateTime(cierre.fechaCierre)}</h2>
            </div>
            <span className={`badge ${hayDiferencia ? "warning" : "success"}`}>
              {hayDiferencia ? "Diferencia registrada" : "Cuadre exacto"}
            </span>
          </div>

          <div className="cierre-result-grid">
            <SummaryCard label="Efectivo esperado" value={formatCurrency(cierre.efectivoEsperadoSinBase)} detail="Sin incluir la base" />
            <SummaryCard label="Efectivo contado" value={formatCurrency(cierre.efectivoContadoSinBase)} detail="Valor reportado al cierre" />
            <SummaryCard label="Diferencia de caja" value={formatCurrency(cierre.diferenciaCaja)} detail="Contado menos esperado" />
            <SummaryCard label="Valor a deposito" value={formatCurrency(cierre.valorADeposito)} detail="Equivale al efectivo contado sin base" />
          </div>

          <div className="cierre-deposito-result">
            <Banknote size={22} strokeWidth={2.2} />
            <div>
              <strong>
                {cierre.movimientoDeposito
                  ? "Entrada de deposito creada"
                  : cierreSource === "history"
                    ? "Cierre recuperado por fecha"
                    : "Sin movimiento de deposito"}
              </strong>
              <span>
                {cierre.movimientoDeposito
                  ? `Saldo: ${formatCurrency(cierre.movimientoDeposito.saldoAnterior)} a ${formatCurrency(cierre.movimientoDeposito.saldoPosterior)}.`
                  : cierreSource === "history"
                    ? "Consulta el movimiento asociado en el modulo Deposito."
                    : "El efectivo contado fue cero, por lo que el sistema no crea un movimiento de deposito."}
              </span>
            </div>
          </div>

          <dl className="cierre-result-details">
            <div>
              <dt>Ventas en efectivo</dt>
              <dd>{formatCurrency(cierre.totalVentasEfectivo)}</dd>
            </div>
            <div>
              <dt>Transferencias</dt>
              <dd>{formatCurrency(cierre.totalVentasTransferencia)}</dd>
            </div>
            <div>
              <dt>Gastos activos</dt>
              <dd>{formatCurrency(cierre.totalGastos)}</dd>
            </div>
            <div>
              <dt>Adiciones</dt>
              <dd>{formatCurrency(cierre.totalAdiciones)}</dd>
            </div>
            <div>
              <dt>Pago trabajadores</dt>
              <dd>{formatCurrency(cierre.totalPagoTrabajadores)}</dd>
            </div>
            <div>
              <dt>Responsable</dt>
              <dd>{cierre.nombreUsuarioCierre}</dd>
            </div>
          </dl>
          {cierre.observaciones ? <p className="cierre-result-note">Observaciones: {cierre.observaciones}</p> : null}
        </section>
      </>
    );
  }

  return (
    <>
      <section className="section-heading" aria-labelledby="cierre-title">
        <div>
          <p className="eyebrow">Arqueo de jornada</p>
          <h1 id="cierre-title">Cierre de caja</h1>
          <p className="lead">Confirma el efectivo fisico contado sin base para consolidar el cierre y su depósito automático.</p>
        </div>
        <div className="cierre-heading-actions">
          <button className="ghost-button" type="button" onClick={handleAbrirHistorial} disabled={isSubmitting}>
            <CalendarDays size={17} strokeWidth={2.2} />
            Consultar cierres
          </button>
          <button className="ghost-button" type="button" onClick={loadSnapshot} disabled={loadState === "loading" || isSubmitting}>
            <RefreshCw size={17} strokeWidth={2.2} />
            Actualizar
          </button>
        </div>
      </section>

      {errorMessage ? (
        <div className="form-alert" role="status">
          <AlertCircle size={18} strokeWidth={2.2} />
          <span>{errorMessage}</span>
        </div>
      ) : null}

      {caja && resumen ? (
        <>
          <section className="cierre-summary-grid" aria-label="Resumen previo al cierre">
            <SummaryCard label="Efectivo esperado" value={formatCurrency(resumen.efectivoEsperadoSinBase)} detail="Calculado sin la base de caja" />
            <SummaryCard label="Ventas en efectivo" value={formatCurrency(resumen.totalVentasEfectivo)} detail={`Ventas totales: ${formatCurrency(resumen.totalVentas)}`} />
            <SummaryCard label="Gastos y pagos" value={formatCurrency(resumen.totalGastos + resumen.totalPagoTrabajadores)} detail="Descuenta gastos activos y pago a trabajadores" />
            <SummaryCard label="Estado operativo" value={resumen.listoParaCierre ? "Listo" : "Pendiente"} detail={resumen.listoParaCierre ? "Requisitos completos" : "Revisa los requisitos"} />
          </section>

          <section className="cierre-breakdown" aria-labelledby="cierre-breakdown-title">
            <div className="cierre-breakdown-heading">
              <div>
                <p className="eyebrow">Consolidado del sistema</p>
                <h2 id="cierre-breakdown-title">Efectivo esperado sin base</h2>
              </div>
              <span className="badge">Caja {formatDate(caja.fechaOperacion)}</span>
            </div>

            <div className="cierre-breakdown-content">
              <div className="cierre-flow-lines">
                <div className="cierre-flow-line income">
                  <span>Ventas en efectivo</span>
                  <strong>+ {formatCurrency(resumen.totalVentasEfectivo)}</strong>
                </div>
                <div className="cierre-flow-line income">
                  <span>Adiciones</span>
                  <strong>+ {formatCurrency(resumen.totalAdiciones)}</strong>
                </div>
                <div className="cierre-flow-line expense">
                  <span>Gastos activos</span>
                  <strong>- {formatCurrency(resumen.totalGastos)}</strong>
                </div>
                <div className="cierre-flow-line expense">
                  <span>Pago a trabajadores</span>
                  <strong>- {formatCurrency(resumen.totalPagoTrabajadores)}</strong>
                </div>
                <div className="cierre-flow-line total">
                  <span>Efectivo esperado sin base</span>
                  <strong>{formatCurrency(resumen.efectivoEsperadoSinBase)}</strong>
                </div>
              </div>

              <dl className="cierre-transfer-details">
                <div>
                  <dt>Transferencias pendientes</dt>
                  <dd>{formatCurrency(resumen.totalTransferenciasPendientes)}</dd>
                </div>
                <div>
                  <dt>Transferencias validadas</dt>
                  <dd>{formatCurrency(resumen.totalTransferenciasValidadas)}</dd>
                </div>
                <div>
                  <dt>Transferencias rechazadas</dt>
                  <dd>{formatCurrency(resumen.totalTransferenciasRechazadas)}</dd>
                </div>
                <div>
                  <dt>Base de caja reservada</dt>
                  <dd>{formatCurrency(resumen.valorBase)}</dd>
                </div>
              </dl>
            </div>
          </section>

          <section className="cierre-action-grid" aria-label="Confirmacion de cierre">
            <form className="panel cierre-form-panel" onSubmit={handleCerrarCaja}>
              <div className="panel-title">
                <div>
                  <h2>Conteo fisico final</h2>
                  <p>Ingresa solo el efectivo contado de la jornada; la base permanece reservada.</p>
                </div>
                <WalletCards size={22} strokeWidth={2.2} />
              </div>

              <label className="field-label">
                Efectivo contado sin base
                <div className="field-control plain">
                  <input
                    inputMode="decimal"
                    type="text"
                    value={efectivoContado}
                    onChange={(event) => setEfectivoContado(normalizeMoneyInput(event.target.value))}
                    disabled={isSubmitting || !efectivoEsperadoNoNegativo}
                    required
                  />
                </div>
              </label>

              <label className="field-label">
                Observaciones
                <div className="field-control plain">
                  <input
                    maxLength={1000}
                    type="text"
                    value={observaciones}
                    onChange={(event) => setObservaciones(event.target.value)}
                    disabled={isSubmitting}
                    placeholder="Opcional"
                  />
                </div>
              </label>

              {efectivoEsperadoNoNegativo ? (
                <div className="cierre-preview" aria-live="polite">
                  <span>Diferencia estimada</span>
                  <strong className={diferenciaEstimada === null || diferenciaEstimada === 0 ? "" : diferenciaEstimada > 0 ? "income" : "expense"}>
                    {diferenciaEstimada === null ? "Ingresa el conteo" : formatCurrency(diferenciaEstimada)}
                  </strong>
                  <small>
                    {diferenciaEstimada === null
                      ? "Si el conteo fisico coincide con el esperado, la diferencia sera cero."
                      : `Deposito automatico estimado: ${formatCurrency(efectivoContadoNumerico)}`}
                  </small>
                </div>
              ) : (
                <div className="cierre-preview blocked" role="status">
                  <span>Conteo fisico bloqueado</span>
                  <strong>Corrige las operaciones de caja</strong>
                  <small>El efectivo esperado es negativo; no existe un conteo fisico negativo ni un deposito valido para calcular.</small>
                </div>
              )}

              <label className="check-control">
                <input
                  type="checkbox"
                  checked={confirmado}
                  onChange={(event) => setConfirmado(event.target.checked)}
                  disabled={isSubmitting || !efectivoEsperadoNoNegativo}
                />
                <span>Confirmo que el conteo no incluye la base de caja.</span>
              </label>

              <button
                className="primary-button full"
                type="submit"
                disabled={!resumen.listoParaCierre || !efectivoEsperadoNoNegativo || !confirmado || !tieneConteoValido || isSubmitting}
              >
                <Save size={18} strokeWidth={2.2} />
                {isSubmitting ? "Cerrando caja" : "Cerrar caja diaria"}
              </button>
            </form>

            <article className="panel cierre-requirements-panel">
              <div className="panel-title">
                <div>
                  <h2>Requisitos de cierre</h2>
                  <p>El sistema valida estas condiciones antes de registrar el cierre.</p>
                </div>
                <ClipboardList size={22} strokeWidth={2.2} />
              </div>

              <ul className="cierre-requirements-list">
                <li className={resumen.adicionDiariaRegistrada ? "complete" : "pending"}>
                  <CheckCircle2 size={18} strokeWidth={2.2} />
                  <span>Adiciones registradas desde Ventas</span>
                </li>
                <li className={resumen.pagoTrabajadoresConfirmado ? "complete" : "pending"}>
                  <CheckCircle2 size={18} strokeWidth={2.2} />
                  <span>Pago diario a trabajadores confirmado</span>
                </li>
                <li className={resumen.efectivoEsperadoSinBase >= 0 ? "complete" : "pending"}>
                  <CheckCircle2 size={18} strokeWidth={2.2} />
                  <span>Efectivo esperado sin base no negativo</span>
                </li>
              </ul>

              {requisitosPendientes.length > 0 ? (
                <p className="cierre-requirements-warning">{requisitosPendientes.join(" ")}</p>
              ) : (
                <p className="cierre-requirements-ready">El arqueo puede cerrarse cuando registres el conteo fisico.</p>
              )}
            </article>
          </section>
        </>
      ) : null}

      {loadState === "no-cash-box" ? historySearchPanel : null}

      {actionMessage ? (
        <div className="form-alert" role="status">
          <ClipboardList size={18} strokeWidth={2.2} />
          <span>{actionMessage}</span>
        </div>
        ) : null}

      <ConfirmationDialog
        confirmLabel="Cerrar caja"
        description={caja ? `Cerrarás la jornada ${formatDate(caja.fechaOperacion)} con un efectivo contado de ${formatCurrency(efectivoContadoNumerico)}. Después del cierre no se podrán registrar ventas ni operaciones de esa caja.` : "Confirmarás el cierre de la caja diaria."}
        isConfirming={isSubmitting}
        onCancel={() => setIsCloseConfirmationVisible(false)}
        onConfirm={() => void confirmCerrarCaja()}
        open={isCloseConfirmationVisible}
        title="Confirmar cierre de caja"
      />
    </>
  );
}
