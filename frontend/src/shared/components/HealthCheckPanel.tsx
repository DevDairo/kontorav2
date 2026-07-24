import { RefreshCw, ServerCog } from "lucide-react";
import type { UseHealthCheckResult } from "../hooks/useHealthCheck";

type HealthCheckPanelProps = {
  health: UseHealthCheckResult;
};

export function HealthCheckPanel({ health }: HealthCheckPanelProps) {
  const badgeClass =
    health.status === "online"
      ? "success"
      : health.status === "offline"
        ? "danger"
        : "warning";

  return (
    <article className="panel">
      <div className="panel-title">
        <div>
          <h2>Estado del sistema</h2>
          <p>Conexion principal</p>
        </div>
        <span className={`badge ${badgeClass}`}>{health.label}</span>
      </div>

      <div className="health-body">
        <div className="health-main">
          <div className="health-icon">
            <ServerCog size={26} strokeWidth={2.2} />
          </div>
          <div>
            <strong>Servicios disponibles</strong>
            <span>Validacion automatica</span>
          </div>
        </div>

        <dl className="summary-list">
          <div>
            <dt>Estado</dt>
            <dd>{health.data?.status ?? health.errorMessage ?? "Pendiente"}</dd>
          </div>
          <div>
            <dt>Ultima validacion</dt>
            <dd>{health.checkedAt ?? "Sin registro"}</dd>
          </div>
        </dl>

        <button
          className="primary-button"
          type="button"
          onClick={health.refresh}
          disabled={health.status === "loading"}
        >
          <RefreshCw size={18} strokeWidth={2.2} />
          Reintentar
        </button>
      </div>
    </article>
  );
}
