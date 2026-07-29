import {
  AlertCircle,
  Camera,
  CheckCircle2,
  ImagePlus,
  Search,
  TriangleAlert,
  XCircle,
} from "lucide-react";
import { FormEvent, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ConfirmationDialog } from "../../../shared/components/ConfirmationDialog";
import { ApiClientError } from "../../../shared/services/apiClient";
import { obtenerItemsInventarioGestion } from "../../catalogos/services/catalogosService";
import { EvidenceGallery } from "../../evidencias/components/EvidenceGallery";
import { cargarEvidenciaPerdidaInventario } from "../../evidencias/services/evidenciasService";
import {
  anularPerdidaInventario,
  obtenerPerdidasCajaAbierta,
  obtenerPerdidasPeriodo,
  registrarPerdidaInventario,
} from "../services/inventarioService";
import type { ExistenciaInventarioDiario, PerdidaInventario } from "../types";

type PerdidasVasosPanelProps = {
  items: ExistenciaInventarioDiario[];
  onInventoryChanged: () => Promise<void>;
  openCashBoxId: string;
  token: string;
};

type PendingCancellation = {
  motivo: string;
  perdida: PerdidaInventario;
};

function messageFor(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.message;
  }
  return error instanceof Error ? error.message : "No fue posible gestionar la perdida.";
}

function bogotaDate() {
  return new Intl.DateTimeFormat("en-CA", {
    day: "2-digit",
    month: "2-digit",
    timeZone: "America/Bogota",
    year: "numeric",
  }).format(new Date());
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("es-CO", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function mergeLosses(...groups: PerdidaInventario[][]) {
  const records = new Map<string, PerdidaInventario>();
  groups.flat().forEach((item) => records.set(item.idPerdidaInventario, item));
  return [...records.values()].sort(
    (left, right) => new Date(right.fechaRegistro).getTime() - new Date(left.fechaRegistro).getTime(),
  );
}

export function PerdidasVasosPanel({
  items,
  onInventoryChanged,
  openCashBoxId,
  token,
}: PerdidasVasosPanelProps) {
  const cupItems = useMemo(
    () => items
      .filter((item) => item.idTamanoVaso && item.onzas && item.cantidadFinalTeorica > 0)
      .sort((left, right) => (left.onzas ?? 0) - (right.onzas ?? 0)),
    [items],
  );
  const [idTamanoVaso, setIdTamanoVaso] = useState("");
  const [cantidad, setCantidad] = useState("1");
  const [motivo, setMotivo] = useState("");
  const [registrationEvidence, setRegistrationEvidence] = useState<File | null>(null);
  const registrationFileRef = useRef<HTMLInputElement>(null);
  const [fechaInicio, setFechaInicio] = useState(bogotaDate);
  const [fechaFin, setFechaFin] = useState(bogotaDate);
  const [records, setRecords] = useState<PerdidaInventario[]>([]);
  const [selectedRecordId, setSelectedRecordId] = useState("");
  const [evidenceFiles, setEvidenceFiles] = useState<Record<string, File | null>>({});
  const [cancellationReasons, setCancellationReasons] = useState<Record<string, string>>({});
  const [pendingRegistration, setPendingRegistration] = useState(false);
  const [pendingCancellation, setPendingCancellation] = useState<PendingCancellation | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [uploadingId, setUploadingId] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    setIdTamanoVaso((current) =>
      cupItems.some((item) => item.idTamanoVaso === current)
        ? current
        : (cupItems[0]?.idTamanoVaso ?? ""),
    );
  }, [cupItems]);

  const selectedItem = cupItems.find((item) => item.idTamanoVaso === idTamanoVaso);
  const selectedRecord = records.find((item) => item.idPerdidaInventario === selectedRecordId);
  const canCancelSelectedRecord = Boolean(
    selectedRecord
    && selectedRecord.estado === "registrada"
    && selectedRecord.idCajaDiaria === openCashBoxId,
  );
  const parsedQuantity = Number(cantidad);

  const loadRecords = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const historyPromise = obtenerPerdidasPeriodo(token, {
        fechaFin: fechaFin || undefined,
        fechaInicio,
      });
      const openPromise = openCashBoxId
        ? obtenerPerdidasCajaAbierta(token)
        : Promise.resolve([] as PerdidaInventario[]);
      const [history, open, itemsCatalogo] = await Promise.all([
        historyPromise,
        openPromise,
        obtenerItemsInventarioGestion(token),
      ]);
      const nombresPorItem = new Map(
        itemsCatalogo.map((item) => [item.idItemInventario, item.nombreItem]),
      );
      const mergedRecords = mergeLosses(history, open).map((record) => ({
        ...record,
        nombreItem: nombresPorItem.get(record.idItemInventario) ?? record.nombreItem,
      }));
      setRecords(mergedRecords);
      setSelectedRecordId((current) =>
        mergedRecords.some((item) => item.idPerdidaInventario === current)
          ? current
          : (mergedRecords[0]?.idPerdidaInventario ?? ""),
      );
    } catch (error) {
      setErrorMessage(messageFor(error));
    } finally {
      setIsLoading(false);
    }
  }, [fechaFin, fechaInicio, openCashBoxId, token]);

  useEffect(() => {
    void loadRecords();
  }, [loadRecords]);

  function requestRegistration(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    setMessage(null);

    if (!openCashBoxId) {
      setErrorMessage("Debe existir una caja diaria abierta para registrar vasos rotos.");
      return;
    }
    if (
      !selectedItem
      || !Number.isInteger(parsedQuantity)
      || parsedQuantity < 1
      || parsedQuantity > selectedItem.cantidadFinalTeorica
    ) {
      setErrorMessage("Selecciona un tamano y una cantidad disponible valida.");
      return;
    }
    if (!motivo.trim()) {
      setErrorMessage("Indica el motivo de la perdida.");
      return;
    }
    setPendingRegistration(true);
  }

  async function confirmRegistration() {
    if (!selectedItem?.idTamanoVaso) {
      return;
    }
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      const loss = await registrarPerdidaInventario(token, {
        cantidad: parsedQuantity,
        confirmaRegistro: true,
        idTamanoVaso: selectedItem.idTamanoVaso,
        motivo: motivo.trim(),
      });

      let evidenceMessage = "La evidencia queda pendiente y puede adjuntarse incluso despues del cierre.";
      if (registrationEvidence) {
        try {
          await cargarEvidenciaPerdidaInventario(
            token,
            loss.idPerdidaInventario,
            registrationEvidence,
          );
          evidenceMessage = "La evidencia fotografica tambien quedo adjunta.";
        } catch (error) {
          evidenceMessage = `El descuento fue registrado, pero la foto sigue pendiente: ${messageFor(error)}`;
        }
      }

      setMessage(
        `${loss.cantidad} ${loss.cantidad === 1 ? "vaso descontado" : "vasos descontados"} del stock diario. ${evidenceMessage}`,
      );
      setCantidad("1");
      setMotivo("");
      setRegistrationEvidence(null);
      if (registrationFileRef.current) {
        registrationFileRef.current.value = "";
      }
      await Promise.all([onInventoryChanged(), loadRecords()]);
    } catch (error) {
      setErrorMessage(messageFor(error));
    } finally {
      setIsSubmitting(false);
      setPendingRegistration(false);
    }
  }

  async function uploadEvidence(perdida: PerdidaInventario) {
    const file = evidenceFiles[perdida.idPerdidaInventario];
    setErrorMessage(null);
    setMessage(null);
    if (!file) {
      setErrorMessage("Selecciona una fotografia antes de adjuntarla.");
      return;
    }

    setUploadingId(perdida.idPerdidaInventario);
    try {
      await cargarEvidenciaPerdidaInventario(token, perdida.idPerdidaInventario, file);
      setEvidenceFiles((current) => ({ ...current, [perdida.idPerdidaInventario]: null }));
      setMessage(`Evidencia adjunta a la perdida de ${perdida.nombreItem}.`);
      await loadRecords();
    } catch (error) {
      setErrorMessage(messageFor(error));
    } finally {
      setUploadingId(null);
    }
  }

  function requestCancellation(perdida: PerdidaInventario) {
    const motivoAnulacion = cancellationReasons[perdida.idPerdidaInventario]?.trim() ?? "";
    setErrorMessage(null);
    if (!motivoAnulacion) {
      setErrorMessage("Indica el motivo de anulacion de la perdida.");
      return;
    }
    setPendingCancellation({ motivo: motivoAnulacion, perdida });
  }

  async function confirmCancellation() {
    if (!pendingCancellation) {
      return;
    }
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      await anularPerdidaInventario(
        token,
        pendingCancellation.perdida.idPerdidaInventario,
        {
          confirmaVasoNoRoto: true,
          motivoAnulacion: pendingCancellation.motivo,
        },
      );
      setMessage("Perdida anulada; los vasos fueron restaurados al stock diario.");
      setCancellationReasons((current) => {
        const next = { ...current };
        delete next[pendingCancellation.perdida.idPerdidaInventario];
        return next;
      });
      await Promise.all([onInventoryChanged(), loadRecords()]);
    } catch (error) {
      setErrorMessage(messageFor(error));
    } finally {
      setIsSubmitting(false);
      setPendingCancellation(null);
    }
  }

  return (
    <section className="panel operation-panel loss-panel" aria-labelledby="loss-title">
      <div className="panel-title">
        <div>
          <h2 id="loss-title">Vasos rotos</h2>
          <p>Registra la perdida por tamano; la fotografia puede adjuntarse ahora o posteriormente.</p>
        </div>
        <TriangleAlert size={22} strokeWidth={2.2} />
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

      <form className="inventory-workspace-form loss-registration-form" onSubmit={requestRegistration}>
        <section className="inventory-item-selector loss-cup-selector" aria-label="Tamanos de vaso disponibles">
          <div className="inventory-item-selector-heading">
            <strong>Selecciona un tamano</strong>
            <small>Presiona una fila para registrar la perdida.</small>
          </div>
          <div className="inventory-item-table">
            <div className="inventory-item-table-header" aria-hidden="true">
              <span>N.º</span>
              <span>Vaso</span>
              <span>Disponible</span>
            </div>
            <div className="inventory-item-options" role="group">
              {cupItems.map((item, index) => (
                <button
                  aria-pressed={item.idTamanoVaso === idTamanoVaso}
                  className={item.idTamanoVaso === idTamanoVaso ? "selected" : ""}
                  key={item.idItemInventario}
                  onClick={() => setIdTamanoVaso(item.idTamanoVaso ?? "")}
                  type="button"
                >
                  <span className="inventory-item-number">{index + 1}</span>
                  <span className="inventory-item-identity">
                    <strong>{item.nombreItem}</strong>
                    <small>Stock diario</small>
                  </span>
                  <b>{item.cantidadFinalTeorica}</b>
                </button>
              ))}
            </div>
          </div>
          {cupItems.length === 0 ? (
            <p className="inventory-empty">No hay vasos disponibles en el stock diario de la caja abierta.</p>
          ) : null}
        </section>

        <div className="inventory-action-fields loss-registration-fields">
          <div className="inventory-selected-item">
            <span>Vaso seleccionado</span>
            <strong>
              {selectedItem
                ? selectedItem.nombreItem
                : "Sin seleccionar"}
            </strong>
            <small>
              {selectedItem
                ? `Stock diario: ${selectedItem.cantidadFinalTeorica} vasos disponibles`
                : "Selecciona un tamano de la lista."}
            </small>
          </div>

          <label className="field-label">
            Cantidad de vasos rotos
            <div className="field-control plain">
              <input
                max={selectedItem?.cantidadFinalTeorica}
                min="1"
                step="1"
                type="number"
                value={cantidad}
                onChange={(event) => setCantidad(event.target.value)}
              />
            </div>
          </label>

          <label className="field-label">
            Motivo
            <div className="field-control plain">
              <input
                maxLength={1000}
                placeholder="Ej. Vaso roto durante la preparacion"
                value={motivo}
                onChange={(event) => setMotivo(event.target.value)}
              />
            </div>
          </label>

          <section className="loss-registration-evidence" aria-labelledby="loss-registration-evidence-title">
            <div className="loss-registration-evidence-heading">
              <Camera size={20} />
              <div>
                <strong id="loss-registration-evidence-title">Evidencia fotografica</strong>
                <small>Puede adjuntarse ahora o completarse posteriormente.</small>
              </div>
            </div>
            <label className="file-control">
              <ImagePlus size={18} />
              <input
                accept="image/*"
                ref={registrationFileRef}
                type="file"
                onChange={(event) => setRegistrationEvidence(event.target.files?.[0] ?? null)}
              />
            </label>
            <small className="field-hint">
              {registrationEvidence
                ? `Lista para adjuntar: ${registrationEvidence.name}`
                : "Opcional al registrar. Administrador o gerente puede adjuntarla despues del cierre."}
            </small>
          </section>
        </div>

        <div className="inventory-workspace-actions loss-registration-actions">
          <button
            className="primary-button inventory-workspace-submit loss-registration-submit"
            disabled={isSubmitting || !openCashBoxId || cupItems.length === 0}
            type="submit"
          >
            <TriangleAlert size={18} />
            Registrar vasos rotos
          </button>
        </div>
      </form>

      <div className="operation-history-heading">
        <div>
          <h3>Perdidas y evidencias</h3>
          <p>Consulta por fecha para completar evidencias de cajas abiertas o cerradas.</p>
        </div>
      </div>
      <div className="loss-history-workspace">
        <section className="loss-history-selector" aria-label="Registros de vasos rotos">
          <form
            className="module-filter-bar loss-filter-form"
            onSubmit={(event) => {
              event.preventDefault();
              void loadRecords();
            }}
          >
            <label className="field-label">
              Fecha inicial
              <div className="field-control plain">
                <input type="date" value={fechaInicio} onChange={(event) => setFechaInicio(event.target.value)} />
              </div>
            </label>
            <label className="field-label">
              Fecha final
              <div className="field-control plain">
                <input type="date" value={fechaFin} onChange={(event) => setFechaFin(event.target.value)} />
              </div>
            </label>
            <button className="ghost-button" disabled={isLoading} type="submit">
              <Search size={16} />
              Consultar
            </button>
          </form>

          <div className="loss-record-table">
            <div className="loss-record-table-header" aria-hidden="true">
              <span>Registro</span>
              <span>Soporte</span>
            </div>
            <div className="loss-record-options" role="listbox" aria-label="Perdidas registradas">
              {records.length > 0 ? records.map((perdida) => (
                <button
                  aria-selected={perdida.idPerdidaInventario === selectedRecordId}
                  className={perdida.idPerdidaInventario === selectedRecordId ? "selected" : ""}
                  key={perdida.idPerdidaInventario}
                  onClick={() => setSelectedRecordId(perdida.idPerdidaInventario)}
                  role="option"
                  type="button"
                >
                  <span className="loss-record-identity">
                    <strong>
                      {perdida.nombreItem} · {perdida.cantidad} {perdida.cantidad === 1 ? "vaso" : "vasos"}
                    </strong>
                    <small>{formatDateTime(perdida.fechaRegistro)} · {perdida.nombreUsuarioRegistro}</small>
                  </span>
                  <span className="loss-record-support">
                    <b className={`badge ${perdida.estado === "anulada" ? "danger" : "success"}`}>
                      {perdida.estado}
                    </b>
                    <small>
                      {perdida.evidencias.length > 0
                        ? `${perdida.evidencias.length} foto(s)`
                        : "Pendiente"}
                    </small>
                  </span>
                </button>
              )) : (
                <p className="inventory-empty">
                  {isLoading ? "Consultando perdidas." : "No hay perdidas registradas en el periodo."}
                </p>
              )}
            </div>
          </div>
        </section>

        <section className="loss-evidence-workspace" aria-label="Detalle y evidencias de la perdida">
          {selectedRecord ? (
            <>
              <div className="operation-record-main">
                <span>
                  <strong>{selectedRecord.nombreItem}</strong>
                  <small>
                    {selectedRecord.cantidad} {selectedRecord.cantidad === 1 ? "vaso descontado" : "vasos descontados"}
                    {" · "}
                    {formatDateTime(selectedRecord.fechaRegistro)}
                  </small>
                </span>
                <b className={`badge ${selectedRecord.estado === "anulada" ? "danger" : "success"}`}>
                  {selectedRecord.estado}
                </b>
              </div>

              <div className="loss-record-reason">
                <span>Motivo registrado</span>
                <p>{selectedRecord.motivo}</p>
              </div>

              <div className="loss-evidence-status">
                <Camera size={16} />
                <span>
                  {selectedRecord.evidencias.length > 0
                    ? `${selectedRecord.evidencias.length} evidencia(s) adjunta(s)`
                    : "Evidencia pendiente"}
                </span>
              </div>

              {selectedRecord.estado === "registrada" ? (
                <div className="loss-evidence-upload">
                  <div>
                    <strong>Agregar evidencia</strong>
                    <small>La foto puede cargarse aunque la caja asociada ya este cerrada.</small>
                  </div>
                  <div className="operation-record-actions evidence-actions">
                    <label className="file-control">
                      <ImagePlus size={17} />
                      <input
                        accept="image/*"
                        type="file"
                        onChange={(event) => setEvidenceFiles((current) => ({
                          ...current,
                          [selectedRecord.idPerdidaInventario]: event.target.files?.[0] ?? null,
                        }))}
                      />
                    </label>
                    <button
                      className="ghost-button"
                      disabled={uploadingId === selectedRecord.idPerdidaInventario}
                      onClick={() => void uploadEvidence(selectedRecord)}
                      type="button"
                    >
                      <Camera size={16} />
                      {uploadingId === selectedRecord.idPerdidaInventario ? "Adjuntando" : "Adjuntar foto"}
                    </button>
                  </div>
                </div>
              ) : null}

              <div className="loss-evidence-gallery">
                <div className="loss-evidence-gallery-heading">
                  <strong>Evidencias adjuntas</strong>
                  <small>Selecciona una fotografia para verla o descargarla.</small>
                </div>
                <EvidenceGallery
                  emptyMessage="Aun no se ha adjuntado evidencia a este registro."
                  evidencias={selectedRecord.evidencias}
                  token={token}
                />
              </div>

              {canCancelSelectedRecord ? (
                <div className="loss-cancellation-workspace">
                  <strong>Anular registro</strong>
                  <div className="operation-record-actions">
                    <input
                      maxLength={1000}
                      placeholder="Motivo de anulacion"
                      value={cancellationReasons[selectedRecord.idPerdidaInventario] ?? ""}
                      onChange={(event) => setCancellationReasons((current) => ({
                        ...current,
                        [selectedRecord.idPerdidaInventario]: event.target.value,
                      }))}
                    />
                    <button
                      className="danger-button"
                      disabled={isSubmitting}
                      onClick={() => requestCancellation(selectedRecord)}
                      type="button"
                    >
                      <XCircle size={16} />
                      Anular
                    </button>
                  </div>
                </div>
              ) : null}
            </>
          ) : (
            <p className="inventory-empty">Selecciona un registro para consultar sus evidencias.</p>
          )}
        </section>
      </div>

      <ConfirmationDialog
        confirmLabel="Registrar perdida"
        description={
          selectedItem
            ? `Se descontaran ${parsedQuantity || 0} vasos de ${selectedItem.nombreItem} del stock diario. La evidencia podra adjuntarse posteriormente y el cierre no volvera a descontar inventario.`
            : ""
        }
        isConfirming={isSubmitting}
        onCancel={() => setPendingRegistration(false)}
        onConfirm={() => void confirmRegistration()}
        open={pendingRegistration}
        title="Confirmar vasos rotos"
      />
      <ConfirmationDialog
        confirmLabel="Anular perdida"
        description="Confirma que los vasos no estaban rotos ni se perdieron. Esta accion restaurara el stock diario y quedara auditada."
        isConfirming={isSubmitting}
        onCancel={() => setPendingCancellation(null)}
        onConfirm={() => void confirmCancellation()}
        open={pendingCancellation !== null}
        title="Confirmar anulacion"
      />
    </section>
  );
}
