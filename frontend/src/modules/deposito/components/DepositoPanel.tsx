import { AlertCircle, Building2, CheckCircle2, FileUp, Landmark, RefreshCw, Save, ShieldCheck } from "lucide-react";
import { type FormEvent, useCallback, useEffect, useRef, useState } from "react";
import { obtenerTiposServicio } from "../../catalogos/services/catalogosService";
import type { CatalogoBasico } from "../../catalogos/types";
import { cargarEvidenciaConsignacionBancaria, cargarEvidenciaPagoServicio } from "../../evidencias/services/evidenciasService";
import type { ArchivoEvidenciaResponse } from "../../evidencias/types";
import { ConfirmationDialog } from "../../../shared/components/ConfirmationDialog";
import { ApiClientError } from "../../../shared/services/apiClient";
import { normalizeMoneyInput } from "../../../shared/utils/moneyInput";
import {
  obtenerSaldoDeposito,
  registrarConsignacionBancaria,
  registrarPagoServicio,
} from "../services/depositoService";
import type { SaldoDeposito } from "../types";

type LoadState = "loading" | "success" | "error";
type SubmitAction = "consignacion" | "servicio" | null;
type PendingAction =
  | { kind: "consignacion"; valor: number; observacion: string; evidencia: File }
  | { kind: "servicio"; valor: number; idTipoServicio: string; descripcion: string; evidencia: File }
  | null;
type PendingEvidence =
  | { kind: "consignacion"; idRegistro: string; archivo: File; label: string }
  | { kind: "servicio"; idRegistro: string; archivo: File; label: string }
  | null;

type DepositoPanelProps = {
  token: string;
};

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

  return error instanceof Error ? error.message : "No fue posible operar el deposito";
}

export function DepositoPanel({ token }: DepositoPanelProps) {
  const [saldo, setSaldo] = useState<SaldoDeposito | null>(null);
  const [tiposServicio, setTiposServicio] = useState<CatalogoBasico[]>([]);
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [evidenceMessage, setEvidenceMessage] = useState<string | null>(null);
  const [lastEvidence, setLastEvidence] = useState<ArchivoEvidenciaResponse | null>(null);
  const [submittingAction, setSubmittingAction] = useState<SubmitAction>(null);
  const [isUploadingEvidence, setIsUploadingEvidence] = useState(false);
  const [pendingAction, setPendingAction] = useState<PendingAction>(null);
  const [pendingEvidence, setPendingEvidence] = useState<PendingEvidence>(null);
  const [valorConsignacion, setValorConsignacion] = useState("");
  const [observacionConsignacion, setObservacionConsignacion] = useState("");
  const [evidenciaConsignacion, setEvidenciaConsignacion] = useState<File | null>(null);
  const [idTipoServicio, setIdTipoServicio] = useState("");
  const [valorServicio, setValorServicio] = useState("");
  const [descripcionServicio, setDescripcionServicio] = useState("");
  const [evidenciaServicio, setEvidenciaServicio] = useState<File | null>(null);
  const evidenciaConsignacionRef = useRef<HTMLInputElement>(null);
  const evidenciaServicioRef = useRef<HTMLInputElement>(null);

  const cargarDatos = useCallback(
    async () => {
      setLoadState("loading");
      setErrorMessage(null);

      try {
        const [saldoResponse, tiposServicioResponse] = await Promise.all([
          obtenerSaldoDeposito(token),
          obtenerTiposServicio(token),
        ]);
        setSaldo(saldoResponse);
        setTiposServicio(tiposServicioResponse);
        setLoadState("success");
      } catch (error) {
        setSaldo(null);
        setLoadState("error");
        setErrorMessage(messageFor(error));
      }
    },
    [token],
  );

  useEffect(() => {
    void cargarDatos();
  }, [cargarDatos]);

  function handleConsignacionSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const valor = Number(valorConsignacion);
    const observacion = observacionConsignacion.trim();
    const evidenciaSeleccionada =
      evidenciaConsignacionRef.current?.files?.[0] ?? evidenciaConsignacion;

    if (!Number.isFinite(valor) || valor <= 0) {
      setActionMessage("Ingresa un valor de consignacion mayor que cero.");
      return;
    }
    if (!evidenciaSeleccionada) {
      setActionMessage("Adjunta la evidencia de la consignacion antes de registrarla.");
      return;
    }
    if (saldo && valor > saldo.saldoActual) {
      setActionMessage("El valor de la consignacion supera el saldo disponible en deposito.");
      return;
    }

    setActionMessage(null);
    setEvidenceMessage(null);
    setPendingAction({
      evidencia: evidenciaSeleccionada,
      kind: "consignacion",
      observacion,
      valor,
    });
  }

  function handlePagoServicioSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const valor = Number(valorServicio);
    const descripcion = descripcionServicio.trim();
    const evidenciaSeleccionada =
      evidenciaServicioRef.current?.files?.[0] ?? evidenciaServicio;

    if (!idTipoServicio) {
      setActionMessage("Selecciona el tipo de servicio pagado.");
      return;
    }
    if (!Number.isFinite(valor) || valor <= 0) {
      setActionMessage("Ingresa un valor de pago mayor que cero.");
      return;
    }
    if (!evidenciaSeleccionada) {
      setActionMessage("Adjunta la evidencia del pago de servicio antes de registrarlo.");
      return;
    }
    if (saldo && valor > saldo.saldoActual) {
      setActionMessage("El valor del pago supera el saldo disponible en deposito.");
      return;
    }

    setActionMessage(null);
    setEvidenceMessage(null);
    setPendingAction({
      descripcion,
      evidencia: evidenciaSeleccionada,
      idTipoServicio,
      kind: "servicio",
      valor,
    });
  }

  async function cargarEvidencia(pending: Exclude<PendingEvidence, null>) {
    setIsUploadingEvidence(true);

    try {
      const evidencia = pending.kind === "consignacion"
        ? await cargarEvidenciaConsignacionBancaria(token, pending.idRegistro, pending.archivo)
        : await cargarEvidenciaPagoServicio(token, pending.idRegistro, pending.archivo);
      setLastEvidence(evidencia);
      setPendingEvidence(null);
      setEvidenceMessage("Evidencia adjunta correctamente mediante el sistema.");
      return true;
    } catch (error) {
      setPendingEvidence(pending);
      setEvidenceMessage(`${pending.label} registrada; evidencia pendiente: ${messageFor(error)}`);
      return false;
    } finally {
      setIsUploadingEvidence(false);
    }
  }

  async function confirmPendingAction() {
    if (!pendingAction) {
      return;
    }

    setSubmittingAction(pendingAction.kind);
    setActionMessage(null);

    try {
      if (pendingAction.kind === "consignacion") {
        const consignacion = await registrarConsignacionBancaria(token, {
          observacion: pendingAction.observacion || undefined,
          valorConsignado: pendingAction.valor,
        });
        setSaldo({ saldoActual: consignacion.movimientoDeposito.saldoPosterior });
        const evidenciaAdjuntada = await cargarEvidencia({
          archivo: pendingAction.evidencia,
          idRegistro: consignacion.idConsignacionBancaria,
          kind: "consignacion",
          label: "Consignacion bancaria",
        });
        setValorConsignacion("");
        setObservacionConsignacion("");
        if (evidenciaAdjuntada) {
          setEvidenciaConsignacion(null);
          if (evidenciaConsignacionRef.current) {
            evidenciaConsignacionRef.current.value = "";
          }
          setActionMessage("Consignacion registrada y saldo de deposito actualizado.");
        }
      } else {
        const pagoServicio = await registrarPagoServicio(token, {
          descripcion: pendingAction.descripcion || undefined,
          idTipoServicio: pendingAction.idTipoServicio,
          valorPagado: pendingAction.valor,
        });
        setSaldo({ saldoActual: pagoServicio.movimientoDeposito.saldoPosterior });
        const evidenciaAdjuntada = await cargarEvidencia({
          archivo: pendingAction.evidencia,
          idRegistro: pagoServicio.idPagoServicio,
          kind: "servicio",
          label: "Pago de servicio",
        });
        setValorServicio("");
        setDescripcionServicio("");
        if (evidenciaAdjuntada) {
          setEvidenciaServicio(null);
          if (evidenciaServicioRef.current) {
            evidenciaServicioRef.current.value = "";
          }
          setActionMessage("Pago de servicio registrado y saldo de deposito actualizado.");
        }
      }
    } catch (error) {
      setActionMessage(messageFor(error));
    } finally {
      setPendingAction(null);
      setSubmittingAction(null);
      void cargarDatos();
    }
  }

  async function retryEvidenceUpload() {
    if (!pendingEvidence) {
      return;
    }

    await cargarEvidencia(pendingEvidence);
  }

  const isSubmitting = submittingAction !== null || isUploadingEvidence;
  const confirmationDescription = pendingAction
    ? pendingAction.kind === "consignacion"
      ? `Registraras una consignacion de ${formatCurrency(pendingAction.valor)}. El sistema descontara este valor del saldo actual de deposito.`
      : `Registraras un pago de servicio por ${formatCurrency(pendingAction.valor)}. El sistema descontara este valor del saldo actual de deposito.`
    : "Confirmaras una salida de deposito.";

  return (
    <>
      <section className="section-heading" aria-labelledby="deposito-title">
        <div>
          <p className="eyebrow">Control financiero</p>
          <h1 id="deposito-title">Deposito, consignaciones y servicios</h1>
          <p className="lead">Consulta el saldo real del deposito y registra salidas administrativas con evidencia.</p>
        </div>
        <button className="ghost-button" type="button" onClick={() => void cargarDatos()} disabled={loadState === "loading" || isSubmitting}>
          <RefreshCw size={17} strokeWidth={2.2} />
          Actualizar
        </button>
      </section>

      {errorMessage ? (
        <div className="form-alert" role="status">
          <AlertCircle size={18} strokeWidth={2.2} />
          <span>{errorMessage}</span>
        </div>
      ) : null}

      <section className="deposito-summary-grid" aria-label="Resumen de deposito">
        <article className="deposito-summary-card balance">
          <span>Saldo actual</span>
          <strong>{formatCurrency(saldo?.saldoActual)}</strong>
          <small>Devuelto por el sistema</small>
        </article>
      </section>

      <section className="deposito-actions-grid" aria-label="Operaciones de deposito">
        <form className="panel deposito-form-panel" onSubmit={handleConsignacionSubmit}>
          <div className="panel-title">
            <div>
              <h2>Registrar consignacion</h2>
              <p>Salida de dinero desde el deposito hacia una cuenta bancaria.</p>
            </div>
            <Landmark size={22} strokeWidth={2.2} />
          </div>

          <label className="field-label">
            Valor consignado
            <div className="field-control plain">
              <input
                inputMode="decimal"
                type="text"
                value={valorConsignacion}
                onChange={(event) => setValorConsignacion(normalizeMoneyInput(event.target.value))}
                disabled={isSubmitting}
                required
              />
            </div>
          </label>

          <label className="field-label">
            Observacion
            <div className="field-control plain">
              <input
                maxLength={1000}
                type="text"
                value={observacionConsignacion}
                onChange={(event) => setObservacionConsignacion(event.target.value)}
                disabled={isSubmitting}
                placeholder="Opcional"
              />
            </div>
          </label>

          <label className="field-label">
            Evidencia de consignacion
            <div className="file-control">
              <FileUp size={18} strokeWidth={2.2} />
              <input
                accept="image/*,.pdf"
                type="file"
                ref={evidenciaConsignacionRef}
                onChange={(event) => setEvidenciaConsignacion(event.target.files?.[0] ?? null)}
                disabled={isSubmitting}
                required
              />
            </div>
            <small className="field-hint">Adjunta una imagen o PDF. El sistema la conservara como soporte de la consignacion.</small>
          </label>

          <button className="primary-button full" type="submit" disabled={isSubmitting || loadState !== "success"}>
            <Save size={18} strokeWidth={2.2} />
            {submittingAction === "consignacion" ? "Registrando" : "Registrar consignacion"}
          </button>
        </form>

        <form className="panel deposito-form-panel" onSubmit={handlePagoServicioSubmit}>
          <div className="panel-title">
            <div>
              <h2>Registrar pago de servicio</h2>
              <p>Salida de deposito para arriendo, energia, agua, internet u otro servicio activo.</p>
            </div>
            <Building2 size={22} strokeWidth={2.2} />
          </div>

          <label className="field-label">
            Tipo de servicio
            <div className="field-control plain">
              <select value={idTipoServicio} onChange={(event) => setIdTipoServicio(event.target.value)} disabled={isSubmitting} required>
                <option value="">Selecciona un servicio</option>
                {tiposServicio.map((tipoServicio) => (
                  <option key={tipoServicio.id} value={tipoServicio.id}>
                    {tipoServicio.nombre}
                  </option>
                ))}
              </select>
            </div>
          </label>

          <label className="field-label">
            Valor pagado
            <div className="field-control plain">
              <input
                inputMode="decimal"
                type="text"
                value={valorServicio}
                onChange={(event) => setValorServicio(normalizeMoneyInput(event.target.value))}
                disabled={isSubmitting}
                required
              />
            </div>
          </label>

          <label className="field-label">
            Descripcion
            <div className="field-control plain">
              <input
                maxLength={1000}
                type="text"
                value={descripcionServicio}
                onChange={(event) => setDescripcionServicio(event.target.value)}
                disabled={isSubmitting}
                placeholder="Opcional"
              />
            </div>
          </label>

          <label className="field-label">
            Evidencia del pago
            <div className="file-control">
              <FileUp size={18} strokeWidth={2.2} />
              <input
                accept="image/*,.pdf"
                type="file"
                ref={evidenciaServicioRef}
                onChange={(event) => setEvidenciaServicio(event.target.files?.[0] ?? null)}
                disabled={isSubmitting}
                required
              />
            </div>
            <small className="field-hint">Adjunta una imagen o PDF. El sistema la conservara como soporte del pago.</small>
          </label>

          <button className="primary-button full" type="submit" disabled={isSubmitting || loadState !== "success"}>
            <Save size={18} strokeWidth={2.2} />
            {submittingAction === "servicio" ? "Registrando" : "Registrar pago de servicio"}
          </button>
        </form>
      </section>

      {actionMessage ? (
        <div className="deposito-status" role="status">
          <ShieldCheck size={18} strokeWidth={2.2} />
          <span>{actionMessage}</span>
        </div>
      ) : null}

      {evidenceMessage ? (
        <div className={`deposito-status ${pendingEvidence ? "warning" : ""}`} role="status">
          <FileUp size={18} strokeWidth={2.2} />
          <span>{evidenceMessage}</span>
          {pendingEvidence ? (
            <button className="ghost-button" type="button" onClick={() => void retryEvidenceUpload()} disabled={isUploadingEvidence}>
              <RefreshCw size={17} strokeWidth={2.2} />
              {isUploadingEvidence ? "Adjuntando" : "Reintentar evidencia"}
            </button>
          ) : null}
        </div>
      ) : null}

      {lastEvidence ? (
        <div className="deposito-evidence-proof">
          <CheckCircle2 size={18} strokeWidth={2.2} />
          <span>Evidencia registrada: {lastEvidence.nombreArchivo}</span>
        </div>
      ) : null}

      <ConfirmationDialog
        confirmLabel={pendingAction?.kind === "consignacion" ? "Registrar consignacion" : "Registrar pago"}
        description={confirmationDescription}
        isConfirming={isSubmitting}
        onCancel={() => setPendingAction(null)}
        onConfirm={() => void confirmPendingAction()}
        open={pendingAction !== null}
        title={pendingAction?.kind === "consignacion" ? "Confirmar consignacion bancaria" : "Confirmar pago de servicio"}
      />
    </>
  );
}
