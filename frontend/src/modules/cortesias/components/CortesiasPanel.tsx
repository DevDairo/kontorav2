import {
  AlertCircle,
  CheckCircle2,
  Gift,
  Minus,
  Plus,
  RefreshCw,
  Trash2,
  UserRound,
  UsersRound,
  XCircle,
} from "lucide-react";
import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import type { CatalogosFormulario } from "../../catalogos/types";
import type { TrabajadorVenta } from "../../ventas/types";
import { ConfirmationDialog } from "../../../shared/components/ConfirmationDialog";
import { ApiClientError } from "../../../shared/services/apiClient";
import { formatDisplayName } from "../../../shared/utils/displayText";
import {
  anularCortesia,
  obtenerCortesiasCajaAbierta,
  registrarCortesia,
} from "../services/cortesiasService";
import type {
  Cortesia,
  RegistrarDetalleCortesiaRequest,
  TipoBeneficiarioCortesia,
} from "../types";

type CortesiasPanelProps = {
  catalogos: CatalogosFormulario | null;
  disabled?: boolean;
  token: string;
  trabajadores: TrabajadorVenta[];
};

type PendingCancellation = {
  cortesia: Cortesia;
  motivo: string;
};

function messageFor(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.message;
  }
  return error instanceof Error ? error.message : "No fue posible gestionar la cortesia.";
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("es-CO", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function detailKey(detail: RegistrarDetalleCortesiaRequest) {
  return `${detail.idTipoGranizado}-${detail.idTamanoVaso}`;
}

export function CortesiasPanel({
  catalogos,
  disabled = false,
  token,
  trabajadores,
}: CortesiasPanelProps) {
  const [cortesias, setCortesias] = useState<Cortesia[]>([]);
  const [tipoBeneficiario, setTipoBeneficiario] =
    useState<TipoBeneficiarioCortesia>("trabajador");
  const [idUsuarioBeneficiario, setIdUsuarioBeneficiario] = useState("");
  const [referenciaOtro, setReferenciaOtro] = useState("");
  const [motivoOtro, setMotivoOtro] = useState("");
  const [idTipoGranizado, setIdTipoGranizado] = useState("");
  const [idTamanoVaso, setIdTamanoVaso] = useState("");
  const [cantidad, setCantidad] = useState("1");
  const [detalles, setDetalles] = useState<RegistrarDetalleCortesiaRequest[]>([]);
  const [cancellationReasons, setCancellationReasons] = useState<Record<string, string>>({});
  const [pendingRegistration, setPendingRegistration] = useState(false);
  const [pendingCancellation, setPendingCancellation] = useState<PendingCancellation | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    setIdTipoGranizado((current) => current || catalogos?.tiposGranizado[0]?.id || "");
    setIdTamanoVaso((current) => current || catalogos?.tamanosVaso[0]?.idTamanoVaso || "");
  }, [catalogos]);

  useEffect(() => {
    setIdUsuarioBeneficiario((current) =>
      trabajadores.some((trabajador) => trabajador.idUsuario === current)
        ? current
        : (trabajadores[0]?.idUsuario ?? ""),
    );
  }, [trabajadores]);

  const loadCortesias = useCallback(async () => {
    setIsLoading(true);
    try {
      setCortesias(await obtenerCortesiasCajaAbierta(token));
    } catch (error) {
      if (
        error instanceof ApiClientError
        && (error.status === 404 || error.status === 409)
      ) {
        setCortesias([]);
      } else {
        setErrorMessage(messageFor(error));
      }
    } finally {
      setIsLoading(false);
    }
  }, [token]);

  useEffect(() => {
    void loadCortesias();
  }, [loadCortesias]);

  const selectedType = catalogos?.tiposGranizado.find((item) => item.id === idTipoGranizado);
  const selectedSize = catalogos?.tamanosVaso.find((item) => item.idTamanoVaso === idTamanoVaso);
  const totalVasos = useMemo(
    () => detalles.reduce((total, detail) => total + detail.cantidad, 0),
    [detalles],
  );

  function addDetail() {
    setErrorMessage(null);
    const parsedQuantity = Number(cantidad);
    if (!idTipoGranizado || !idTamanoVaso || !Number.isInteger(parsedQuantity) || parsedQuantity < 1) {
      setErrorMessage("Selecciona el tipo, el tamano y una cantidad valida.");
      return;
    }

    const detail = {
      cantidad: parsedQuantity,
      idTamanoVaso,
      idTipoGranizado,
    };
    setDetalles((current) => {
      const existing = current.find((item) => detailKey(item) === detailKey(detail));
      return existing
        ? current.map((item) =>
            detailKey(item) === detailKey(detail)
              ? { ...item, cantidad: item.cantidad + parsedQuantity }
              : item,
          )
        : [...current, detail];
    });
    setCantidad("1");
  }

  function adjustDetail(key: string, change: number) {
    setDetalles((current) =>
      current
        .map((detail) =>
          detailKey(detail) === key
            ? { ...detail, cantidad: detail.cantidad + change }
            : detail,
        )
        .filter((detail) => detail.cantidad > 0),
    );
  }

  function requestRegistration(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    setMessage(null);

    if (detalles.length === 0) {
      setErrorMessage("Agrega al menos un granizado a la cortesia.");
      return;
    }
    if (tipoBeneficiario === "trabajador" && !idUsuarioBeneficiario) {
      setErrorMessage("Selecciona el trabajador beneficiario.");
      return;
    }
    if (tipoBeneficiario === "otro" && !motivoOtro.trim()) {
      setErrorMessage("Indica el motivo de la cortesia para el beneficiario otro.");
      return;
    }
    setPendingRegistration(true);
  }

  async function confirmRegistration() {
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      const response = await registrarCortesia(token, {
        confirmaRegistro: true,
        detalles,
        idUsuarioBeneficiario:
          tipoBeneficiario === "trabajador" ? idUsuarioBeneficiario : undefined,
        motivoOtro: tipoBeneficiario === "otro" ? motivoOtro.trim() : undefined,
        referenciaOtro:
          tipoBeneficiario === "otro" ? referenciaOtro.trim() || undefined : undefined,
        tipoBeneficiario,
      });
      setMessage(`Cortesia registrada: ${response.detalles.reduce((sum, item) => sum + item.cantidad, 0)} vasos descontados del stock diario.`);
      setDetalles([]);
      setReferenciaOtro("");
      setMotivoOtro("");
      await loadCortesias();
    } catch (error) {
      setErrorMessage(messageFor(error));
    } finally {
      setIsSubmitting(false);
      setPendingRegistration(false);
    }
  }

  function requestCancellation(cortesia: Cortesia) {
    const motivo = cancellationReasons[cortesia.idCortesia]?.trim() ?? "";
    setErrorMessage(null);
    if (!motivo) {
      setErrorMessage("Indica el motivo de anulacion de la cortesia.");
      return;
    }
    setPendingCancellation({ cortesia, motivo });
  }

  async function confirmCancellation() {
    if (!pendingCancellation) {
      return;
    }
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      await anularCortesia(token, pendingCancellation.cortesia.idCortesia, {
        confirmaNoEntregada: true,
        motivoAnulacion: pendingCancellation.motivo,
      });
      setMessage("Cortesia anulada y vasos restaurados al stock diario.");
      setCancellationReasons((current) => {
        const next = { ...current };
        delete next[pendingCancellation.cortesia.idCortesia];
        return next;
      });
      await loadCortesias();
    } catch (error) {
      setErrorMessage(messageFor(error));
    } finally {
      setIsSubmitting(false);
      setPendingCancellation(null);
    }
  }

  return (
    <section className="panel operation-panel courtesy-panel" aria-labelledby="courtesy-title">
      <div className="panel-title">
        <div>
          <h2 id="courtesy-title">Cortesias</h2>
          <p>Operacion sin precio ni pago; descuenta exclusivamente el stock diario.</p>
        </div>
        <Gift size={22} strokeWidth={2.2} />
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

      <form className="inventory-workspace-form courtesy-registration-form" onSubmit={requestRegistration}>
        <section className="inventory-item-selector courtesy-type-selector" aria-label="Tipos de granizado">
          <div className="inventory-item-selector-heading">
            <strong>Selecciona un producto</strong>
            <small>Elige el tipo de granizado que se entregara como cortesia.</small>
          </div>
          <div className="inventory-item-table">
            <div className="inventory-item-table-header" aria-hidden="true">
              <span>N.º</span>
              <span>Producto</span>
              <span>Estado</span>
            </div>
            <div className="inventory-item-options" role="group">
              {catalogos?.tiposGranizado.map((item, index) => (
                <button
                  aria-pressed={idTipoGranizado === item.id}
                  className={idTipoGranizado === item.id ? "selected" : ""}
                  key={item.id}
                  onClick={() => setIdTipoGranizado(item.id)}
                  type="button"
                >
                  <span className="inventory-item-number">{index + 1}</span>
                  <span className="inventory-item-identity">
                    <strong>{formatDisplayName(item.nombre)}</strong>
                    <small>Granizado</small>
                  </span>
                  <b>{idTipoGranizado === item.id ? "Actual" : "Elegir"}</b>
                </button>
              ))}
            </div>
          </div>
          {!catalogos || catalogos.tiposGranizado.length === 0 ? (
            <p className="inventory-empty">No hay tipos de granizado disponibles.</p>
          ) : null}
        </section>

        <div className="inventory-action-fields courtesy-registration-fields">
          <div className="inventory-selected-item">
            <span>Seleccion actual</span>
            <strong>
              {selectedType ? formatDisplayName(selectedType.nombre) : "Sin tipo"}
              {selectedSize ? ` · ${selectedSize.onzas} oz` : ""}
            </strong>
            <small>La disponibilidad final siempre la valida el sistema.</small>
          </div>

          <div className="operation-form-grid courtesy-beneficiary-grid">
            <div className="operation-field-group">
              <span className="operation-field-title">Beneficiario</span>
              <div className="operation-choice-grid two-options" role="group" aria-label="Tipo de beneficiario">
                <button
                  className={tipoBeneficiario === "trabajador" ? "selected" : ""}
                  type="button"
                  onClick={() => setTipoBeneficiario("trabajador")}
                >
                  <UsersRound size={18} />
                  Trabajador
                </button>
                <button
                  className={tipoBeneficiario === "otro" ? "selected" : ""}
                  type="button"
                  onClick={() => setTipoBeneficiario("otro")}
                >
                  <UserRound size={18} />
                  Otro
                </button>
              </div>
            </div>

            {tipoBeneficiario === "trabajador" ? (
              <label className="field-label">
                Trabajador beneficiario
                <div className="field-control plain">
                  <select
                    value={idUsuarioBeneficiario}
                    onChange={(event) => setIdUsuarioBeneficiario(event.target.value)}
                  >
                    <option value="">Selecciona un trabajador</option>
                    {trabajadores.map((trabajador) => (
                      <option key={trabajador.idUsuario} value={trabajador.idUsuario}>
                        {trabajador.nombreCompleto} ({trabajador.nombreUsuario})
                      </option>
                    ))}
                  </select>
                </div>
              </label>
            ) : (
              <>
                <label className="field-label">
                  Persona o entidad
                  <div className="field-control plain">
                    <input
                      maxLength={250}
                      placeholder="Referencia opcional"
                      value={referenciaOtro}
                      onChange={(event) => setReferenciaOtro(event.target.value)}
                    />
                  </div>
                </label>
                <label className="field-label operation-form-wide">
                  Motivo
                  <div className="field-control plain">
                    <input
                      maxLength={1000}
                      placeholder="Motivo obligatorio"
                      value={motivoOtro}
                      onChange={(event) => setMotivoOtro(event.target.value)}
                    />
                  </div>
                </label>
              </>
            )}
          </div>

          <div className="operation-field-group">
            <span className="operation-field-title">Tamano del vaso</span>
            <div className="operation-choice-grid cup-size-grid" role="group" aria-label="Tamano del vaso">
              {catalogos?.tamanosVaso.map((item) => (
                <button
                  className={idTamanoVaso === item.idTamanoVaso ? "selected" : ""}
                  key={item.idTamanoVaso}
                  type="button"
                  onClick={() => setIdTamanoVaso(item.idTamanoVaso)}
                >
                  <strong>{item.onzas} oz</strong>
                </button>
              ))}
            </div>
          </div>

          <div className="operation-add-row courtesy-add-row">
            <label className="field-label">
              Cantidad
              <div className="field-control plain">
                <input
                  min="1"
                  step="1"
                  type="number"
                  value={cantidad}
                  onChange={(event) => setCantidad(event.target.value)}
                />
              </div>
            </label>
            <button className="ghost-button" disabled={disabled} onClick={addDetail} type="button">
              <Plus size={17} />
              Agregar al detalle
            </button>
          </div>

          {detalles.length > 0 ? (
            <ul className="operation-detail-list">
              {detalles.map((detail) => {
                const type = catalogos?.tiposGranizado.find((item) => item.id === detail.idTipoGranizado);
                const size = catalogos?.tamanosVaso.find((item) => item.idTamanoVaso === detail.idTamanoVaso);
                return (
                  <li key={detailKey(detail)}>
                    <span>
                      <strong>{type ? formatDisplayName(type.nombre) : "Granizado"}</strong>
                      <small>{size?.onzas ?? "-"} oz</small>
                    </span>
                    <div className="quantity-stepper">
                      <button type="button" onClick={() => adjustDetail(detailKey(detail), -1)}>
                        <Minus size={14} />
                      </button>
                      <span>{detail.cantidad}</span>
                      <button type="button" onClick={() => adjustDetail(detailKey(detail), 1)}>
                        <Plus size={14} />
                      </button>
                    </div>
                    <button
                      aria-label="Eliminar detalle"
                      className="icon-button"
                      onClick={() => setDetalles((current) => current.filter((item) => detailKey(item) !== detailKey(detail)))}
                      type="button"
                    >
                      <Trash2 size={16} />
                    </button>
                  </li>
                );
              })}
            </ul>
          ) : (
            <p className="inventory-empty courtesy-detail-empty">
              Agrega al menos un granizado antes de registrar la cortesia.
            </p>
          )}
        </div>

        <div className="inventory-workspace-actions courtesy-registration-actions">
          <button
            className="primary-button inventory-workspace-submit courtesy-registration-submit"
            disabled={disabled || isSubmitting || detalles.length === 0}
            type="submit"
          >
            <Gift size={18} />
            Registrar cortesia ({totalVasos} vasos)
          </button>
        </div>
      </form>

      <div className="operation-history-heading">
        <div>
          <h3>Cortesias de la caja abierta</h3>
          <p>Las anulaciones solo proceden si el producto no fue entregado.</p>
        </div>
        <button className="ghost-button" disabled={isLoading} onClick={() => void loadCortesias()} type="button">
          <RefreshCw size={16} />
          Actualizar
        </button>
      </div>

      <ul className="operation-record-list">
        {cortesias.length > 0 ? cortesias.map((cortesia) => (
          <li key={cortesia.idCortesia}>
            <div className="operation-record-main">
              <span>
                <strong>
                  {cortesia.tipoBeneficiario === "trabajador"
                    ? cortesia.nombreUsuarioBeneficiario
                    : cortesia.referenciaOtro || "Otro beneficiario"}
                </strong>
                <small>
                  {formatDateTime(cortesia.fechaRegistro)} · {cortesia.nombreUsuarioRegistro}
                </small>
              </span>
              <b className={`badge ${cortesia.estado === "anulada" ? "danger" : "success"}`}>
                {cortesia.estado}
              </b>
            </div>
            <p>
              {cortesia.detalles
                .map((detail) => `${formatDisplayName(detail.nombreTipoGranizado)} · ${detail.onzas} oz × ${detail.cantidad}`)
                .join(" | ")}
            </p>
            {cortesia.estado === "registrada" ? (
              <div className="operation-record-actions">
                <input
                  maxLength={1000}
                  placeholder="Motivo de anulacion"
                  value={cancellationReasons[cortesia.idCortesia] ?? ""}
                  onChange={(event) => setCancellationReasons((current) => ({
                    ...current,
                    [cortesia.idCortesia]: event.target.value,
                  }))}
                />
                <button
                  className="danger-button"
                  disabled={isSubmitting}
                  onClick={() => requestCancellation(cortesia)}
                  type="button"
                >
                  <XCircle size={16} />
                  Anular
                </button>
              </div>
            ) : null}
          </li>
        )) : (
          <li className="inventory-empty">
            {isLoading ? "Consultando cortesias." : "No hay cortesias registradas en la caja abierta."}
          </li>
        )}
      </ul>

      <ConfirmationDialog
        confirmLabel="Registrar cortesia"
        description={`Se descontaran ${totalVasos} vasos del stock diario. Esta operacion no crea venta, pago ni promocion.`}
        isConfirming={isSubmitting}
        onCancel={() => setPendingRegistration(false)}
        onConfirm={() => void confirmRegistration()}
        open={pendingRegistration}
        title="Confirmar cortesia"
      />
      <ConfirmationDialog
        confirmLabel="Anular cortesia"
        description="Confirma que la cortesia no fue entregada ni consumida. Los vasos regresaran al stock diario."
        isConfirming={isSubmitting}
        onCancel={() => setPendingCancellation(null)}
        onConfirm={() => void confirmCancellation()}
        open={pendingCancellation !== null}
        title="Confirmar anulacion"
      />
    </section>
  );
}
