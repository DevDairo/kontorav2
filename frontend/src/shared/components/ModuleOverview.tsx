import { ArrowRight } from "lucide-react";
import type { AppRoute } from "../../app/routes/appRoutes";

type ModuleOverviewProps = {
  routes: AppRoute[];
  featuredRouteIds?: string[];
  onNavigate: (path: string) => void;
};

const homeActionCopy: Record<string, string> = {
  auditoria: "Revisa las acciones importantes.",
  caja: "Consulta la jornada y el efectivo.",
  catalogos: "Administra productos, precios y promociones.",
  cierre: "Revisa y finaliza la jornada.",
  consultas: "Consulta los registros por fecha.",
  deposito: "Consulta el saldo y los movimientos.",
  evidencias: "Consulta y adjunta soportes.",
  gastos: "Registra y revisa gastos de caja.",
  inventario: "Controla existencias y consumos.",
  transferencias: "Consulta pagos y comprobantes.",
  usuarios: "Administra accesos y contrasenas.",
  ventas: "Registra ventas y sus pagos.",
};

export function ModuleOverview({ routes, featuredRouteIds = [], onNavigate }: ModuleOverviewProps) {
  const moduleRoutes = routes.filter(
    (route) => route.id !== "inicio" && !featuredRouteIds.includes(route.id),
  );

  if (moduleRoutes.length === 0) {
    return null;
  }

  return (
    <section className="module-overview" aria-labelledby="module-overview-title">
      <div className="home-section-heading">
        <div>
          <p className="eyebrow">Mas opciones</p>
          <h2 id="module-overview-title">Herramientas disponibles</h2>
        </div>
      </div>

      <div className="module-grid">
        {moduleRoutes.map((route) => {
          const Icon = route.icon;
          return (
            <button className="metric-card route-card-button" key={route.path} type="button" onClick={() => onNavigate(route.path)}>
              <div className="metric-icon">
                <Icon size={19} strokeWidth={2.2} />
              </div>
              <div>
                <h3>{route.label}</h3>
                <span>{homeActionCopy[route.id] ?? "Accede a esta herramienta."}</span>
              </div>
              <ArrowRight className="module-card-arrow" size={18} strokeWidth={2.2} aria-hidden="true" />
            </button>
          );
        })}
      </div>
    </section>
  );
}
