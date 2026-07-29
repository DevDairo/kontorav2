import { AlertCircle, BadgeCheck, CheckCircle2, FileImage, FileUp, RefreshCw, XCircle } from "lucide-react";
import { type ChangeEvent, type FormEvent, useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { UserRole } from "../../../app/routes/appRoutes";
import { ConfirmationDialog } from "../../../shared/components/ConfirmationDialog";
import { ApiClientError } from "../../../shared/services/apiClient";
import { EvidenceGallery } from "../../evidencias/components/EvidenceGallery";
import {
  cargarAjusteEvidenciaPagoVenta,
  listarEvidenciasPagoVenta,
} from "../../evidencias/services/evidenciasService";
import type { ArchivoEvidenciaResponse } from "../../evidencias/types";
import {
  consultarTransferencias,
  rechazarTransferencia,
  validarTransferencia,
} from "../services/transferenciasService";
import type { ConsultaTransferencia, EstadoTransferencia, FiltroTransferencias } from "../types";

type LoadState = "loading" | "success" | "error";
type Decision = "validar" | "rechazar" | null;

type TransferenciasPanelProps = {
  role: UserRole | null;
  token: string;
};

function formatDateInput(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function firstDayOfMonth() {
  const now = new Date();
  return formatDateInput(new Date(now.getFullYear(), now.getMonth(), 1));
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("es-CO", {
    currency: "COP",
    maximumFractionDigits: 0,
    style: "currency",
  }).format(Number(value ?? 0));
}

function formatDateTime(value: string | null) {
  if (!value) {
    return "Sin registro";
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
  return error instanceof Error ? error.message : "No fue posible consultar las transferencias.";
}

function statusLabel(status: EstadoTransferencia) {
  if (status === "pendiente") {
    return "Pendiente";
  }

  return status === "validada" ? "Validada" : "Rechazada";
}

export function TransferenciasPanel({ role, token }: TransferenciasPanelProps) {
  const [fechaInicio, setFechaInicio] = useState(firstDayOfMonth);
  const [fechaFin, setFechaFin] = useState(() => formatDateInput(new Date()));
  const [activeStatus, setActiveStatus] = useState<EstadoTransferencia>("pendiente");
  const [pendientes, setPendientes] = useState<ConsultaTransferencia[]>([]);
  const [validadas, setValidadas] = useState<ConsultaTransferencia[]>([]);
  const [rechazadas, setRechazadas] = useState<ConsultaTransferencia[]>([]);
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [selectedTransferencia, setSelectedTransferencia] = useState<ConsultaTransferencia | null>(null);
  const [evidencias, setEvidencias] = useState<ArchivoEvidenciaResponse[]>([]);
  const [evidenceState, setEvidenceState] = useState<LoadState>("success");
  const [evidenceError, setEvidenceError] = useState<string | null>(null);
  const [observacion, setObservacion] = useState("");
  const [pendingDecision, setPendingDecision] = useState<Decision>(null);
  const [isDeciding, setIsDeciding] = useState(false);
  const [archivoAjuste, setArchivoAjuste] = useState<File | null>(null);
  const [isUploadingAdjustment, setIsUploadingAdjustment] = useState(false);
  const adjustmentInputRef = useRef<HTMLInputElement>(null);

  const canDownloadEvidence = role === "administrador" || role === "gerente";
  const canDecide = role === "gerente";
  const canAdjustEvidence = role === "gerente";

  const filtroBase = useMemo(
    () => ({ fechaFin: fechaFin || undefined, fechaInicio: fechaInicio || undefined }),
    [fechaFin, fechaInicio],
  );

  const cargarTransferencias = useCallback(async () => {
    setLoadState("loading");
    setErrorMessage(null);

    try {
      const [pendientesResponse, validadasResponse, rechazadasResponse] = await Promise.all([
        consultarTransferencias(token, { ...filtroBase, estadoValidacion: "pendiente" }),
        consultarTransferencias(token, { ...filtroBase, estadoValidacion: "validada" }),
        consultarTransferencias(token, { ...filtroBase, estadoValidacion: "rechazada" }),
      ]);
      setPendientes(pendientesResponse);
      setValidadas(validadasResponse);
      setRechazadas(rechazadasResponse);
      setLoadState("success");
    } catch (error) {
      setPendientes([]);
      setValidadas([]);
      setRechazadas([]);
      setLoadState("error");
      setErrorMessage(messageFor(error));
    }
  }, [filtroBase, token]);

  useEffect(() => {
    void cargarTransferencias();
  }, [cargarTransferencias]);

  const transferenciasActivas = activeStatus === "pendiente"
    ? pendientes
    : activeStatus === "validada"
      ? validadas
      : rechazadas;
  const valorPendiente = pendientes.reduce((total, transferencia) => total + transferencia.valorPago, 0);
  const valorValidado = validadas.reduce((total, transferencia) => total + transferencia.valorPago, 0);
  const valorRechazado = rechazadas.reduce((total, transferencia) => total + transferencia.valorPago, 0);

  async function cargarEvidencias(transferencia: ConsultaTransferencia) {
    setEvidenceState("loading");
    setEvidenceError(null);

    try {
      const response = await listarEvidenciasPagoVenta(token, transferencia.idPagoVenta);
      setEvidencias(response);
      setEvidenceState("success");
    } catch (error) {
      setEvidencias([]);
      setEvidenceState("error");
      setEvidenceError(messageFor(error));
    }
  }

  function seleccionarTransferencia(transferencia: ConsultaTransferencia) {
    setSelectedTransferencia(transferencia);
    setObservacion(transferencia.observacionValidacion ?? "");
    setArchivoAjuste(null);
    if (adjustmentInputRef.current) {
      adjustmentInputRef.current.value = "";
    }
    setActionMessage(null);
    void cargarEvidencias(transferencia);
  }

  function actualizarConsulta(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (fechaFin && fechaFin < fechaInicio) {
      setErrorMessage("La fecha final no puede ser anterior a la fecha inicial.");
      return;
    }

    setSelectedTransferencia(null);
    setEvidencias([]);
    setEvidenceError(null);
    setArchivoAjuste(null);
    setActionMessage(null);
    void cargarTransferencias();
  }

  function cambiarEstado(status: EstadoTransferencia) {
    setActiveStatus(status);
    setSelectedTransferencia(null);
    setEvidencias([]);
    setEvidenceError(null);
    setArchivoAjuste(null);
    setActionMessage(null);
  }

  function seleccionarArchivoAjuste(event: ChangeEvent<HTMLInputElement>) {
    setArchivoAjuste(event.target.files?.[0] ?? null);
    setEvidenceError(null);
  }

  async function adjuntarAjusteEvidencia() {
    if (!selectedTransferencia || !archivoAjuste) {
      return;
    }

    setIsUploadingAdjustment(true);
    setEvidenceError(null);
    setActionMessage(null);

    try {
      await cargarAjusteEvidenciaPagoVenta(token, selectedTransferencia.idPagoVenta, archivoAjuste);
      setArchivoAjuste(null);
      if (adjustmentInputRef.current) {
        adjustmentInputRef.current.value = "";
      }
      setActionMessage("Evidencia de ajuste adjunta. El soporte anterior se conserva para trazabilidad.");
      await cargarEvidencias(selectedTransferencia);
    } catch (error) {
      setEvidenceError(messageFor(error));
    } finally {
      setIsUploadingAdjustment(false);
    }
  }

  async function confirmarDecision() {
    if (!selectedTransferencia || !pendingDecision) {
      return;
    }

    setIsDeciding(true);
    setActionMessage(null);

    try {
      const request = { observacionValidacion: observacion.trim() || undefined };
      const response = pendingDecision === "validar"
        ? await validarTransferencia(token, selectedTransferencia.idPagoVenta, request)
        : await rechazarTransferencia(token, selectedTransferencia.idPagoVenta, request);
      const label = response.estadoValidacion === "validada" ? "validada" : "rechazada";

      setPendingDecision(null);
      setSelectedTransferencia(null);
      setEvidencias([]);
      setActionMessage(`Transferencia ${label}. La decision quedo registrada en auditoria.`);
      await cargarTransferencias();
    } catch (error) {
      setActionMessage(messageFor(error));
      setPendingDecision(null);
    } finally {
      setIsDeciding(false);
    }
  }

  const confirmationTitle = pendingDecision === "validar" ? "Validar transferencia" : "Rechazar transferencia";
  const confirmationDescription = selectedTransferencia
    ? `${pendingDecision === "validar" ? "Confirmas la validacion" : "Confirmas el rechazo"} de la transferencia de Venta #${selectedTransferencia.numeroVenta} por ${formatCurrency(selectedTransferencia.valorPago)}? Esta decision se registra en auditoria.`
    : "";

  return (
    <section className="transferencias-panel" aria-label="Transferencias y validacion">
      <header className="module-header transferencias-header">
        <div>
          <span className="eyebrow">Control de recaudos</span>
          <h1>Transferencias</h1>
          <p>Consulta pagos por transferencia, revisa sus soportes y registra decisiones administrativas auditadas.</p>
        </div>
      </header>

      <form className="transferencias-filter-form module-filter-bar panel" onSubmit={actualizarConsulta}>
        <label className="form-field">
          <span>Fecha inicial</span>
          <input className="field-control plain" type="date" value={fechaInicio} onChange={(event) => setFechaInicio(event.target.value)} />
        </label>
        <label className="form-field">
          <span>Fecha final</span>
          <input className="field-control plain" type="date" value={fechaFin} onChange={(event) => setFechaFin(event.target.value)} />
        </label>
        <button className="primary-button" type="submit" disabled={loadState === "loading"}>
          <RefreshCw size={18} aria-hidden="true" />
          Consultar
        </button>
      </form>

      <div className="transferencias-summary-grid" aria-label="Resumen de transferencias">
        <article className="transferencias-summary-card">
          <span>Pendientes</span>
          <strong>{pendientes.length}</strong>
          <small>{formatCurrency(valorPendiente)}</small>
        </article>
        <article className="transferencias-summary-card">
          <span>Validadas</span>
          <strong>{validadas.length}</strong>
          <small>{formatCurrency(valorValidado)}</small>
        </article>
        <article className="transferencias-summary-card">
          <span>Rechazadas</span>
          <strong>{rechazadas.length}</strong>
          <small>{formatCurrency(valorRechazado)}</small>
        </article>
        <article className="transferencias-summary-card">
          <span>Soportes</span>
          <strong>{[...pendientes, ...validadas, ...rechazadas].reduce((total, item) => total + item.cantidadEvidencias, 0)}</strong>
          <small>Adjuntos en el periodo</small>
        </article>
      </div>

      {errorMessage ? (
        <div className="error-alert transferencias-alert" role="alert">
          <AlertCircle size={18} aria-hidden="true" />
          <span>{errorMessage}</span>
        </div>
      ) : null}
      {actionMessage ? (
        <div className={actionMessage.startsWith("Transferencia") || actionMessage.startsWith("Evidencia") ? "success-alert transferencias-alert" : "error-alert transferencias-alert"} role="status">
          {actionMessage.startsWith("Transferencia") || actionMessage.startsWith("Evidencia") ? <CheckCircle2 size={18} aria-hidden="true" /> : <AlertCircle size={18} aria-hidden="true" />}
          <span>{actionMessage}</span>
        </div>
      ) : null}

      <div className="transferencias-tabs" role="tablist" aria-label="Estado de transferencia">
        <button className={activeStatus === "pendiente" ? "active" : ""} type="button" role="tab" aria-selected={activeStatus === "pendiente"} onClick={() => cambiarEstado("pendiente")}>
          Pendientes
        </button>
        <button className={activeStatus === "validada" ? "active" : ""} type="button" role="tab" aria-selected={activeStatus === "validada"} onClick={() => cambiarEstado("validada")}>
          Validadas
        </button>
        <button className={activeStatus === "rechazada" ? "active" : ""} type="button" role="tab" aria-selected={activeStatus === "rechazada"} onClick={() => cambiarEstado("rechazada")}>
          Rechazadas
        </button>
      </div>

      <div className="transferencias-workspace">
        <section className="transferencias-list-panel" aria-labelledby="transferencias-list-title">
          <div className="compact-heading">
            <div>
              <span className="eyebrow">Registros</span>
              <h2 id="transferencias-list-title">Transferencias {statusLabel(activeStatus).toLowerCase()}</h2>
            </div>
            <span className="count-badge">{transferenciasActivas.length}</span>
          </div>

          {loadState === "loading" ? <p className="loading-copy">Consultando transferencias...</p> : null}
          {loadState === "success" && transferenciasActivas.length === 0 ? <p className="empty-copy">No hay transferencias {statusLabel(activeStatus).toLowerCase()} para el periodo seleccionado.</p> : null}
          <ul className="transferencias-record-list">
            {transferenciasActivas.map((transferencia) => (
              <li key={transferencia.idPagoVenta}>
                <button
                  className={`transferencias-record-row ${selectedTransferencia?.idPagoVenta === transferencia.idPagoVenta ? "selected" : ""}`}
                  type="button"
                  onClick={() => seleccionarTransferencia(transferencia)}
                >
                  <BadgeCheck size={20} aria-hidden="true" />
                  <span>
                    <strong>Venta #{transferencia.numeroVenta}</strong>
                    <small>{transferencia.nombreUsuarioVendedor} · {transferencia.cantidadEvidencias} soporte(s)</small>
                  </span>
                  <span className="transferencias-record-value">
                    <b>{formatCurrency(transferencia.valorPago)}</b>
                    <small>{formatDateTime(transferencia.fechaRegistro)}</small>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        </section>

        <section className="transferencias-detail-panel" aria-labelledby="transferencias-detail-title">
          {!selectedTransferencia ? (
            <div className="transferencias-placeholder">
              <FileImage size={28} aria-hidden="true" />
              <div>
                <span className="eyebrow">Detalle</span>
                <h2 id="transferencias-detail-title">Selecciona una transferencia</h2>
              </div>
            </div>
          ) : (
            <>
              <div className="compact-heading transferencias-selected-heading">
                <div>
                  <span className="eyebrow">Venta #{selectedTransferencia.numeroVenta}</span>
                  <h2 id="transferencias-detail-title">{formatCurrency(selectedTransferencia.valorPago)}</h2>
                  <p>{selectedTransferencia.nombreUsuarioVendedor} · Jornada {selectedTransferencia.fechaOperacion}</p>
                </div>
                <span className={`transferencias-status ${selectedTransferencia.estadoValidacion}`}>{statusLabel(selectedTransferencia.estadoValidacion)}</span>
              </div>

              {selectedTransferencia.estadoValidacion === "rechazada" ? (
                <div className="transferencias-decision-summary">
                  <XCircle size={18} aria-hidden="true" />
                  <span>Rechazada por {selectedTransferencia.nombreUsuarioValidacion ?? "usuario no disponible"} el {formatDateTime(selectedTransferencia.fechaValidacion)}{selectedTransferencia.observacionValidacion ? `: ${selectedTransferencia.observacionValidacion}` : "."}</span>
                </div>
              ) : null}
              {selectedTransferencia.estadoValidacion === "validada" ? (
                <div className="transferencias-decision-summary validated">
                  <CheckCircle2 size={18} aria-hidden="true" />
                  <span>Validada por {selectedTransferencia.nombreUsuarioValidacion ?? "usuario no disponible"} el {formatDateTime(selectedTransferencia.fechaValidacion)}{selectedTransferencia.observacionValidacion ? `: ${selectedTransferencia.observacionValidacion}` : "."}</span>
                </div>
              ) : null}

              <div className="transferencias-support-heading">
                <FileImage size={18} aria-hidden="true" />
                <div>
                  <strong>Soportes asociados</strong>
                </div>
              </div>
              {evidenceState === "loading" ? <p className="loading-copy">Consultando soportes...</p> : null}
              {evidenceError ? (
                <div className="error-alert transferencias-alert" role="alert">
                  <AlertCircle size={18} aria-hidden="true" />
                  <span>{evidenceError}</span>
                </div>
              ) : null}
              {evidenceState === "success" ? (
                <EvidenceGallery
                  allowFileAccess={canDownloadEvidence}
                  autoScrollToPreview
                  emptyMessage="No hay soportes registrados para esta transferencia."
                  evidencias={evidencias}
                  token={token}
                />
              ) : null}

              {canAdjustEvidence ? (
                <div className="transferencias-evidence-adjustment">
                  <div>
                    <strong>Adjuntar evidencia de ajuste</strong>
                    <small>El nuevo soporte se conserva junto con los anteriores y queda registrado en auditoria.</small>
                  </div>
                  <div className="transferencias-evidence-adjustment-actions">
                    <label className="file-picker-control">
                      <FileUp size={18} aria-hidden="true" />
                      <span>{archivoAjuste ? archivoAjuste.name : "Seleccionar archivo"}</span>
                      <input
                        ref={adjustmentInputRef}
                        accept="image/*,.pdf"
                        type="file"
                        onChange={seleccionarArchivoAjuste}
                        disabled={isUploadingAdjustment}
                      />
                    </label>
                    <button
                      className="primary-button"
                      type="button"
                      onClick={() => void adjuntarAjusteEvidencia()}
                      disabled={!archivoAjuste || isUploadingAdjustment}
                    >
                      <FileUp size={18} aria-hidden="true" />
                      {isUploadingAdjustment ? "Adjuntando" : "Adjuntar ajuste"}
                    </button>
                  </div>
                </div>
              ) : null}

              {canDecide && selectedTransferencia.estadoValidacion === "pendiente" ? (
                <div className="transferencias-decision-form">
                  <label className="form-field">
                    <span>Observacion de la decision</span>
                    <textarea className="field-control plain" value={observacion} onChange={(event) => setObservacion(event.target.value)} placeholder="Opcional" rows={3} />
                  </label>
                  <div className="transferencias-decision-actions">
                    <button className="ghost-button" type="button" onClick={() => setPendingDecision("rechazar")}>
                      <XCircle size={18} aria-hidden="true" />
                      Rechazar
                    </button>
                    <button className="primary-button" type="button" onClick={() => setPendingDecision("validar")}>
                      <CheckCircle2 size={18} aria-hidden="true" />
                      Validar
                    </button>
                  </div>
                </div>
              ) : null}
            </>
          )}
        </section>
      </div>

      <ConfirmationDialog
        confirmLabel={pendingDecision === "validar" ? "Validar transferencia" : "Rechazar transferencia"}
        description={confirmationDescription}
        isConfirming={isDeciding}
        onCancel={() => setPendingDecision(null)}
        onConfirm={() => void confirmarDecision()}
        open={pendingDecision !== null}
        title={confirmationTitle}
      />
    </section>
  );
}
