import { AlertCircle, ChevronRight, Clock3, Database, History, RefreshCw, ShieldCheck, UserRound } from "lucide-react";
import { type FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { ApiClientError } from "../../../shared/services/apiClient";
import { formatDisplayName } from "../../../shared/utils/displayText";
import { listarUsuarios } from "../../usuarios/services/usuariosService";
import type { UsuarioGestion } from "../../usuarios/types";
import { consultarAuditoria } from "../services/auditoriaService";
import type { ConsultaAuditoria, FiltroAuditoria } from "../types";

type LoadState = "loading" | "success" | "error";

type AuditoriaPanelProps = {
  token: string;
};

const ACTION_OPTIONS = [
  { label: "Todas las acciones", value: "" },
  { label: "Crear", value: "crear" },
  { label: "Editar", value: "editar" },
  { label: "Anular", value: "anular" },
  { label: "Aprobar", value: "aprobar" },
  { label: "Rechazar", value: "rechazar" },
  { label: "Abrir", value: "abrir" },
  { label: "Cerrar", value: "cerrar" },
  { label: "Validar", value: "validar" },
  { label: "Revocar", value: "revocar" },
  { label: "Inicio de sesion", value: "login" },
  { label: "Cierre de sesion", value: "logout" },
] as const;

function todayLocalDate() {
  const date = new Date();
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("es-CO", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function messageFor(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.message;
  }
  return error instanceof Error ? error.message : "No fue posible consultar la auditoria.";
}

function tableLabel(value: string) {
  return formatDisplayName(value);
}

function actionLabel(value: string) {
  const option = ACTION_OPTIONS.find((item) => item.value === value);
  return option ? option.label : formatDisplayName(value);
}

function actionTone(value: string) {
  if (["anular", "rechazar", "revocar"].includes(value)) {
    return "critical";
  }
  if (["aprobar", "validar", "crear", "abrir"].includes(value)) {
    return "success";
  }
  if (["login", "logout"].includes(value)) {
    return "neutral";
  }
  return "info";
}

const SNAPSHOT_LABELS: Record<string, string> = {
  fecha_cambio_contrasena: "Fecha de cambio de contrasena",
  fecha_cierre: "Fecha de cierre",
  fecha_fin_vigencia: "Fin de vigencia",
  fecha_inicio: "Fecha de inicio",
  fecha_inicio_vigencia: "Inicio de vigencia",
  fecha_movimiento: "Fecha de movimiento",
  fecha_solicitud: "Fecha de solicitud",
  fecha_validacion: "Fecha de validacion",
  id_usuario: "Usuario",
  id_usuario_anulacion: "Usuario que anulo",
  id_usuario_apertura: "Usuario que abrio",
  id_usuario_cierre: "Usuario que cerro",
  id_usuario_comprador: "Trabajador comprador",
  id_usuario_validacion: "Usuario que valido",
  id_usuario_vendedor: "Vendedor",
  nombre_rol: "Rol",
  nombre_usuario: "Nombre de usuario",
  requiere_cambio_contrasena: "Requiere cambio de contrasena",
};

type SnapshotEntry = {
  label: string;
  value: string;
};

function formatSnapshotValue(key: string, value: unknown, usuariosPorId: Map<string, UsuarioGestion>): string {
  if (value === null || value === undefined) {
    return "Sin registro";
  }
  if (typeof value === "boolean") {
    return value ? "Si" : "No";
  }
  if (Array.isArray(value)) {
    return value.map((item) => formatSnapshotValue(key, item, usuariosPorId)).join(", ");
  }
  if (typeof value === "object") {
    return Object.entries(value as Record<string, unknown>)
      .map(([nestedKey, nestedValue]) => `${SNAPSHOT_LABELS[nestedKey] ?? formatDisplayName(nestedKey)}: ${formatSnapshotValue(nestedKey, nestedValue, usuariosPorId)}`)
      .join("; ");
  }
  if (typeof value === "string" && key.startsWith("id_usuario")) {
    const usuario = usuariosPorId.get(value);
    return usuario ? `${usuario.nombreCompleto} (${usuario.nombreUsuario})` : value;
  }
  if (typeof value === "string" && key.startsWith("fecha_") && value.includes("T")) {
    const parsedDate = Date.parse(value);
    if (!Number.isNaN(parsedDate)) {
      return formatDateTime(value);
    }
  }
  return String(value);
}

function snapshotEntries(value: string | null, usuariosPorId: Map<string, UsuarioGestion>): SnapshotEntry[] {
  if (!value) {
    return [{ label: "Estado", value: "Sin datos registrados" }];
  }

  try {
    const parsed = JSON.parse(value);
    if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) {
      return Object.entries(parsed).map(([key, entryValue]) => ({
        label: SNAPSHOT_LABELS[key] ?? formatDisplayName(key),
        value: formatSnapshotValue(key, entryValue, usuariosPorId),
      }));
    }
    return [{ label: "Estado", value: formatSnapshotValue("estado", parsed, usuariosPorId) }];
  } catch {
    return [{ label: "Contenido", value }];
  }
}

function shortIdentifier(value: string | null) {
  if (!value) {
    return "Sin registro";
  }
  return value.length > 18 ? `${value.slice(0, 8)}...${value.slice(-6)}` : value;
}

export function AuditoriaPanel({ token }: AuditoriaPanelProps) {
  const [fechaInicio, setFechaInicio] = useState(todayLocalDate);
  const [fechaFin, setFechaFin] = useState(todayLocalDate);
  const [filtroAplicado, setFiltroAplicado] = useState<FiltroAuditoria>(() => {
    const fechaActual = todayLocalDate();
    return { fechaFin: fechaActual, fechaInicio: fechaActual };
  });
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [registros, setRegistros] = useState<ConsultaAuditoria[]>([]);
  const [usuarios, setUsuarios] = useState<UsuarioGestion[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const cargarAuditoria = useCallback(async () => {
    setLoadState("loading");
    setErrorMessage(null);

    try {
      const response = await consultarAuditoria(token, filtroAplicado);
      setRegistros(response);
      setSelectedId((currentId) => response.some((item) => item.idAuditoriaOperacion === currentId)
        ? currentId
        : (response[0]?.idAuditoriaOperacion ?? null));
      setLoadState("success");
    } catch (error) {
      setRegistros([]);
      setSelectedId(null);
      setLoadState("error");
      setErrorMessage(messageFor(error));
    }
  }, [filtroAplicado, token]);

  useEffect(() => {
    void cargarAuditoria();
  }, [cargarAuditoria]);

  useEffect(() => {
    void listarUsuarios(token).then(setUsuarios).catch(() => setUsuarios([]));
  }, [token]);

  const selectedRecord = useMemo(
    () => registros.find((item) => item.idAuditoriaOperacion === selectedId) ?? null,
    [registros, selectedId],
  );

  const usuariosPorId = useMemo(
    () => new Map(usuarios.map((usuario) => [usuario.idUsuario, usuario])),
    [usuarios],
  );

  const resumen = useMemo(() => {
    const usuarios = new Set(registros.map((item) => item.idUsuario));
    const entidades = new Set(registros.map((item) => item.tablaAfectada));
    const acciones = new Set(registros.map((item) => item.accion));

    return [
      { detail: "Eventos consultados", label: "Registros", value: String(registros.length) },
      { detail: "Usuarios que ejecutaron acciones", label: "Usuarios", value: String(usuarios.size) },
      { detail: "Entidades afectadas", label: "Entidades", value: String(entidades.size) },
      { detail: "Tipos de accion", label: "Acciones", value: String(acciones.size) },
    ];
  }, [registros]);

  function actualizarConsulta(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (fechaFin && fechaFin < fechaInicio) {
      setErrorMessage("La fecha final no puede ser anterior a la fecha inicial.");
      return;
    }

    setFiltroAplicado({
      fechaFin: fechaFin || undefined,
      fechaInicio,
    });
  }

  return (
    <section className="auditoria-panel" aria-label="Auditoria operativa">
      <header className="module-header auditoria-header">
        <div>
          <span className="eyebrow">Control interno</span>
          <h1>Auditoria</h1>
          <p>Consulta los eventos sensibles registrados por la operacion. El historial diario se consulta en Consultas.</p>
        </div>
      </header>

      <form className="auditoria-filter-form panel" onSubmit={actualizarConsulta}>
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

      {errorMessage ? (
        <div className="error-alert auditoria-alert" role="alert">
          <AlertCircle size={18} aria-hidden="true" />
          <span>{errorMessage}</span>
        </div>
      ) : null}

      <div className="auditoria-summary-grid" aria-label="Resumen de auditoria">
        {resumen.map((item) => (
          <article className="auditoria-summary-card" key={item.label}>
            <span>{item.label}</span>
            <strong>{item.value}</strong>
            <small>{item.detail}</small>
          </article>
        ))}
      </div>

      <div className="auditoria-split-data">
        <section className="auditoria-data-panel auditoria-records-panel" aria-labelledby="auditoria-records-title">
          <div className="compact-heading">
            <div>
              <span className="eyebrow">Eventos</span>
              <h2 id="auditoria-records-title">Registro de actividad</h2>
            </div>
            <History size={22} aria-hidden="true" />
          </div>

          {loadState === "loading" ? <p className="loading-copy">Consultando auditoria...</p> : null}
          {loadState === "success" && registros.length === 0 ? <p className="empty-copy">No hay eventos para los filtros seleccionados.</p> : null}

          <ul className="auditoria-record-list">
            {registros.map((registro) => (
              <li key={registro.idAuditoriaOperacion}>
                <button
                  className={`auditoria-record-row ${selectedRecord?.idAuditoriaOperacion === registro.idAuditoriaOperacion ? "selected" : ""}`}
                  type="button"
                  onClick={() => setSelectedId(registro.idAuditoriaOperacion)}
                  aria-pressed={selectedRecord?.idAuditoriaOperacion === registro.idAuditoriaOperacion}
                >
                  <span className="auditoria-record-main">
                    <span className={`auditoria-action-badge ${actionTone(registro.accion)}`}>{actionLabel(registro.accion)}</span>
                    <strong>{tableLabel(registro.tablaAfectada)}</strong>
                    <small>{registro.nombreUsuario} · {formatDateTime(registro.fechaAccion)}</small>
                  </span>
                  <ChevronRight size={19} aria-hidden="true" />
                </button>
              </li>
            ))}
          </ul>
        </section>

        <section className="auditoria-data-panel auditoria-detail-panel" aria-labelledby="auditoria-detail-title">
          <div className="compact-heading">
            <div>
              <span className="eyebrow">Detalle</span>
              <h2 id="auditoria-detail-title">Evento seleccionado</h2>
            </div>
            <ShieldCheck size={22} aria-hidden="true" />
          </div>

          {!selectedRecord && loadState === "success" ? <p className="empty-copy">Selecciona un evento para ver su trazabilidad.</p> : null}

          {selectedRecord ? (
            <div className="auditoria-detail-content">
              <div className="auditoria-selected-heading">
                <span className={`auditoria-action-badge ${actionTone(selectedRecord.accion)}`}>{actionLabel(selectedRecord.accion)}</span>
                <div>
                  <strong>{tableLabel(selectedRecord.tablaAfectada)}</strong>
                  <small>{formatDateTime(selectedRecord.fechaAccion)}</small>
                </div>
              </div>

              {selectedRecord.descripcion ? <p className="auditoria-description">{selectedRecord.descripcion}</p> : null}

              <dl className="auditoria-metadata-grid">
                <div>
                  <dt><UserRound size={16} aria-hidden="true" /> Usuario</dt>
                  <dd>{selectedRecord.nombreUsuario}</dd>
                </div>
                <div>
                  <dt><Clock3 size={16} aria-hidden="true" /> Fecha y hora</dt>
                  <dd>{formatDateTime(selectedRecord.fechaAccion)}</dd>
                </div>
                <div>
                  <dt><Database size={16} aria-hidden="true" /> Registro afectado</dt>
                  <dd><code title={selectedRecord.idRegistroAfectado ?? undefined}>{shortIdentifier(selectedRecord.idRegistroAfectado)}</code></dd>
                </div>
                <div>
                  <dt><ShieldCheck size={16} aria-hidden="true" /> Direccion IP</dt>
                  <dd>{selectedRecord.direccionIp || "Sin registro"}</dd>
                </div>
              </dl>

              <div className="auditoria-snapshot-grid">
                <article>
                  <span>Valor anterior</span>
                  <ul className="auditoria-snapshot-list">
                    {snapshotEntries(selectedRecord.valorAnterior, usuariosPorId).map((entry) => (
                      <li key={entry.label}>
                        <strong>{entry.label}:</strong> {entry.value}
                      </li>
                    ))}
                  </ul>
                </article>
                <article>
                  <span>Valor nuevo</span>
                  <ul className="auditoria-snapshot-list">
                    {snapshotEntries(selectedRecord.valorNuevo, usuariosPorId).map((entry) => (
                      <li key={entry.label}>
                        <strong>{entry.label}:</strong> {entry.value}
                      </li>
                    ))}
                  </ul>
                </article>
              </div>
            </div>
          ) : null}
        </section>
      </div>
    </section>
  );
}
