import { AlertCircle, Banknote, CalendarDays, RefreshCw, WalletCards } from "lucide-react";
import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import type { UserRole } from "../../../app/routes/appRoutes";
import { ConfirmationDialog } from "../../../shared/components/ConfirmationDialog";
import { ApiClientError } from "../../../shared/services/apiClient";
import { normalizeMoneyInput } from "../../../shared/utils/moneyInput";
import { abrirCajaDiaria, obtenerCajaAbierta } from "../services/cajaService";
import type { CajaDiaria } from "../types";
import { CajaOperacionesPanel } from "./CajaOperacionesPanel";

type LoadState = "loading" | "success" | "empty" | "error";

const DEFAULT_VALOR_BASE = "300000";

type CajaAbiertaPanelProps = {
  token: string;
  role: UserRole | null;
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

function formatDate(value: string) {
  return new Intl.DateTimeFormat("es-CO", {
    dateStyle: "medium",
  }).format(new Date(`${value}T00:00:00`));
}

function formatDateTime(value: string | null) {
  if (!value) {
    return "Sin registrar";
  }

  return new Intl.DateTimeFormat("es-CO", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function messageFor(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.message;
  }

  return error instanceof Error ? error.message : "No fue posible consultar la caja diaria";
}

export function CajaAbiertaPanel({ token, role }: CajaAbiertaPanelProps) {
  const [caja, setCaja] = useState<CajaDiaria | null>(null);
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [fechaOperacion, setFechaOperacion] = useState(todayLocalDate);
  const [valorBase, setValorBase] = useState(DEFAULT_VALOR_BASE);
  const [observaciones, setObservaciones] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isOpenConfirmationVisible, setIsOpenConfirmationVisible] = useState(false);

  const canOpenCashBox = role === "administrador" || role === "gerente";

  const loadCaja = useCallback(async () => {
    setLoadState("loading");
    setErrorMessage(null);

    try {
      const response = await obtenerCajaAbierta(token);
      setCaja(response);
      setLoadState("success");
    } catch (error) {
      setCaja(null);

      if (error instanceof ApiClientError && error.status === 404) {
        setLoadState("empty");
        setErrorMessage(error.message);
        return;
      }

      setLoadState("error");
      setErrorMessage(messageFor(error));
    }
  }, [token]);

  useEffect(() => {
    void loadCaja();
  }, [loadCaja]);

  const statusLabel = useMemo(() => {
    if (loadState === "loading") {
      return "Consultando";
    }

    if (loadState === "success") {
      return "Caja abierta";
    }

    if (loadState === "empty") {
      return "Sin caja abierta";
    }

    return "Error de consulta";
  }, [loadState]);

  const handleOpenCashBox = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const numericBase = Number(valorBase);
    if (!Number.isFinite(numericBase) || numericBase < 0) {
      setErrorMessage("El valor base debe ser un numero mayor o igual a cero");
      return;
    }

    setErrorMessage(null);
    setIsOpenConfirmationVisible(true);
  };

  const confirmOpenCashBox = async () => {
    const numericBase = Number(valorBase);

    if (!Number.isFinite(numericBase) || numericBase < 0) {
      setIsOpenConfirmationVisible(false);
      setErrorMessage("El valor base debe ser un numero mayor o igual a cero");
      return;
    }

    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      const response = await abrirCajaDiaria(
        {
          fechaOperacion,
          observaciones: observaciones.trim() || null,
          valorBase: numericBase,
        },
        token,
      );
      setCaja(response);
      setLoadState("success");
      setObservaciones("");
      setIsOpenConfirmationVisible(false);
    } catch (error) {
      setErrorMessage(messageFor(error));
      setIsOpenConfirmationVisible(false);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <>
      <section className="section-heading" aria-labelledby="caja-title">
        <div>
          <p className="eyebrow">Caja diaria</p>
          <h1 id="caja-title">Panel de caja abierta</h1>
          <p className="lead">Consulta la jornada actual y realiza la apertura cuando corresponda.</p>
        </div>
        <button className="ghost-button" type="button" onClick={loadCaja} disabled={loadState === "loading"}>
          <RefreshCw size={17} strokeWidth={2.2} />
          Reintentar
        </button>
      </section>

      <div className="caja-panel-grid">
        <article className="panel caja-status-card">
          <div className="caja-status-icon">
            <WalletCards size={28} strokeWidth={2.1} />
          </div>
          <div>
            <span className={`badge ${loadState === "success" ? "success" : "warning"}`}>{statusLabel}</span>
            <h2>{caja ? `Operacion ${formatDate(caja.fechaOperacion)}` : "No hay caja abierta cargada"}</h2>
            <p>
              {caja
                ? `Apertura registrada por ${caja.nombreUsuarioApertura}.`
                : "Aun no hay una jornada abierta."}
            </p>
          </div>
        </article>

        {errorMessage && loadState !== "success" ? (
          <div className="form-alert caja-alert" role="status">
            <AlertCircle size={18} strokeWidth={2.2} />
            <span>{errorMessage}</span>
          </div>
        ) : null}

        {caja ? (
          <article className="panel caja-detail-panel">
            <div className="panel-title">
              <div>
                <h2>Detalle de caja</h2>
                <p>Informacion de la jornada activa</p>
              </div>
              <span className="badge success">{caja.estadoCaja}</span>
            </div>

            <dl className="caja-detail-grid">
              <div>
                <dt>Fecha de operacion</dt>
                <dd>
                  <CalendarDays size={18} strokeWidth={2.2} />
                  {formatDate(caja.fechaOperacion)}
                </dd>
              </div>
              <div>
                <dt>Valor base</dt>
                <dd>
                  <Banknote size={18} strokeWidth={2.2} />
                  {formatCurrency(caja.valorBase)}
                </dd>
              </div>
              <div>
                <dt>Fecha de apertura</dt>
                <dd>{formatDateTime(caja.fechaApertura)}</dd>
              </div>
              <div>
                <dt>Usuario apertura</dt>
                <dd>{caja.nombreUsuarioApertura}</dd>
              </div>
              <div>
                <dt>Fecha de cierre</dt>
                <dd>{formatDateTime(caja.fechaCierre)}</dd>
              </div>
              <div>
                <dt>Observaciones</dt>
                <dd>{caja.observaciones || "Sin observaciones"}</dd>
              </div>
            </dl>
          </article>
        ) : (
          <article className="panel">
            <div className="panel-title">
              <div>
                <h2>Apertura de caja</h2>
                <p>{canOpenCashBox ? "Disponible para administrador o gerente" : "Solo lectura para vendedor"}</p>
              </div>
            </div>

            {canOpenCashBox ? (
              <form className="form-grid caja-form" onSubmit={handleOpenCashBox}>
                <label className="field-label">
                  Fecha de operacion
                  <div className="field-control plain">
                    <input
                      type="date"
                      value={fechaOperacion}
                      onChange={(event) => setFechaOperacion(event.target.value)}
                      required
                    />
                  </div>
                </label>

                <label className="field-label">
                  Valor base
                  <div className="field-control plain">
                    <input
                      inputMode="decimal"
                      type="text"
                      value={valorBase}
                      onChange={(event) => setValorBase(normalizeMoneyInput(event.target.value))}
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
                      placeholder="Opcional"
                    />
                  </div>
                </label>

                <button className="primary-button" type="submit" disabled={isSubmitting}>
                  {isSubmitting ? "Abriendo caja" : "Abrir caja diaria"}
                </button>
              </form>
            ) : (
              <p className="empty-copy">
                No hay caja abierta para operar. Un administrador o gerente debe realizar la apertura desde su sesion.
              </p>
            )}
          </article>
        )}
      </div>

      {caja && canOpenCashBox ? <CajaOperacionesPanel token={token} /> : null}

      <ConfirmationDialog
        confirmLabel="Abrir caja"
        description={`Abriras la caja de la jornada ${formatDate(fechaOperacion)} con una base de ${formatCurrency(Number(valorBase) || 0)}. No podras abrir otra caja para esa fecha ni otra jornada mientras esta caja permanezca abierta.`}
        isConfirming={isSubmitting}
        onCancel={() => setIsOpenConfirmationVisible(false)}
        onConfirm={() => void confirmOpenCashBox()}
        open={isOpenConfirmationVisible}
        title="Confirmar apertura de caja"
      />
    </>
  );
}
