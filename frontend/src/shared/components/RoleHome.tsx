import { ArrowRight, CalendarDays } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { obtenerCajaAbierta } from "../../modules/caja/services/cajaService";
import type { CajaDiaria } from "../../modules/caja/types";
import {
  findRouteByPath,
  roleLabels,
  type AppRoute,
  type UserRole,
} from "../../app/routes/appRoutes";
import { ModuleOverview } from "./ModuleOverview";

type HomeAction = {
  routeId: string;
  label: string;
  detail: string;
};

type HomeContent = {
  eyebrow: string;
  lead: string;
  primaryAction: string;
  summary: HomeAction[];
  nextSteps: string[];
};

const roleHomeContent: Record<UserRole, HomeContent> = {
  vendedor: {
    eyebrow: "Operacion de hoy",
    lead: "Registra ventas y consulta la informacion necesaria para atender el mostrador.",
    primaryAction: "ventas",
    summary: [
      { routeId: "ventas", label: "Nueva venta", detail: "Registra productos y pagos." },
      { routeId: "caja", label: "Jornada", detail: "Consulta el estado de la caja." },
      { routeId: "gastos", label: "Registrar gasto", detail: "Anota un gasto de la jornada." },
      { routeId: "transferencias", label: "Transferencias", detail: "Revisa los pagos por transferencia." },
    ],
    nextSteps: ["ventas", "gastos", "consultas"],
  },
  administrador: {
    eyebrow: "Operacion de hoy",
    lead: "Organiza la jornada, los productos y el inventario antes de iniciar la atencion.",
    primaryAction: "caja",
    summary: [
      { routeId: "caja", label: "Jornada", detail: "Abre y revisa la caja del dia." },
      { routeId: "ventas", label: "Ventas", detail: "Registra ventas y sus pagos." },
      { routeId: "inventario", label: "Inventario", detail: "Controla insumos y consumos diarios." },
      { routeId: "catalogos", label: "Productos y precios", detail: "Actualiza productos, precios y promociones." },
    ],
    nextSteps: ["caja", "inventario", "gastos"],
  },
  gerente: {
    eyebrow: "Resumen de la jornada",
    lead: "Supervisa la operacion diaria y accede a los controles del negocio desde un solo lugar.",
    primaryAction: "caja",
    summary: [
      { routeId: "caja", label: "Jornada", detail: "Revisa la caja y la operacion del dia." },
      { routeId: "transferencias", label: "Transferencias", detail: "Verifica pagos y comprobantes pendientes." },
      { routeId: "cierre", label: "Cierre de caja", detail: "Consulta y finaliza la jornada." },
      { routeId: "auditoria", label: "Seguimiento", detail: "Revisa las acciones importantes." },
    ],
    nextSteps: ["transferencias", "cierre", "deposito"],
  },
};

type JornadaState =
  | { status: "loading"; caja: null }
  | { status: "open"; caja: CajaDiaria }
  | { status: "empty"; caja: null }
  | { status: "error"; caja: null };

type RoleHomeProps = {
  role: UserRole | null;
  routes: AppRoute[];
  token: string;
  user: {
    nombreCompleto: string;
  };
  onNavigate: (path: string) => void;
};

function formatCurrency(value: number) {
  return new Intl.NumberFormat("es-CO", {
    currency: "COP",
    maximumFractionDigits: 0,
    style: "currency",
  }).format(value);
}

function formatOperationDate(value: string) {
  return new Intl.DateTimeFormat("es-CO", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(new Date(`${value}T00:00:00`));
}

function firstName(fullName: string) {
  return fullName.trim().split(/\s+/)[0] || "";
}

function journeyCopy(journey: JornadaState, role: UserRole) {
  if (journey.status === "loading") {
    return { detail: "Actualizando el estado de hoy.", value: "Jornada" };
  }

  if (journey.status === "open") {
    return {
      detail: `${formatOperationDate(journey.caja.fechaOperacion)} · Base ${formatCurrency(journey.caja.valorBase)}`,
      value: "Abierta",
    };
  }

  if (journey.status === "error") {
    return { detail: "Actualiza esta vista para consultar la jornada.", value: "Sin actualizar" };
  }

  return {
    detail:
      role === "vendedor"
        ? "Un responsable debe abrir la caja para comenzar."
        : "Abre la caja para iniciar las operaciones del dia.",
    value: "Sin abrir",
  };
}

export function RoleHome({ role, routes, token, user, onNavigate }: RoleHomeProps) {
  const normalizedRole = role ?? "vendedor";
  const content = roleHomeContent[normalizedRole];
  const [journey, setJourney] = useState<JornadaState>({ caja: null, status: "loading" });

  const loadJourney = useCallback(async () => {
    setJourney({ caja: null, status: "loading" });

    try {
      const caja = await obtenerCajaAbierta(token);
      setJourney({ caja, status: "open" });
    } catch (error) {
      const status = error instanceof Error && "status" in error ? Number(error.status) : 0;
      setJourney({ caja: null, status: status === 404 ? "empty" : "error" });
    }
  }, [token]);

  useEffect(() => {
    void loadJourney();
  }, [loadJourney]);

  const routeById = useMemo(
    () => new Map(routes.map((route) => [route.id, route])),
    [routes],
  );
  const summary = content.summary.filter((action) => routeById.has(action.routeId));
  const nextSteps = content.nextSteps
    .map((routeId) => routeById.get(routeId))
    .filter((route): route is AppRoute => Boolean(route));
  const primaryRoute = routeById.get(content.primaryAction);
  const consultasRoute = findRouteByPath(routes, "/consultas");
  const cajaRoute = routeById.get("caja");
  const journeyStatus = journeyCopy(journey, normalizedRole);

  return (
    <>
      <section className="section-heading home-heading" aria-labelledby="home-title">
        <div>
          <p className="eyebrow">{content.eyebrow}</p>
          <h1 id="home-title">Hola, {firstName(user.nombreCompleto)}</h1>
          <p className="lead">{content.lead}</p>
        </div>

        <div className="home-heading-actions">
          {primaryRoute ? (
            <button className="primary-button" type="button" onClick={() => onNavigate(primaryRoute.path)}>
              {normalizedRole === "vendedor" ? "Registrar venta" : "Gestionar jornada"}
            </button>
          ) : null}
          {consultasRoute ? (
            <button className="ghost-button" type="button" onClick={() => onNavigate(consultasRoute.path)}>
              Ver registros
            </button>
          ) : null}
        </div>
      </section>

      <section className="home-summary-grid" aria-label={`Accesos principales de ${roleLabels[normalizedRole]}`}>
        {summary.map((action) => {
          const route = routeById.get(action.routeId);
          if (!route) {
            return null;
          }

          const Icon = route.icon;
          const isJourney = route.id === "caja";
          const value = isJourney ? journeyStatus.value : action.label;
          const detail = isJourney ? journeyStatus.detail : action.detail;

          return (
            <button
              className="home-summary-card"
              key={route.id}
              type="button"
              onClick={() => onNavigate(route.path)}
            >
              <span className="home-summary-icon" aria-hidden="true">
                <Icon size={20} strokeWidth={2.2} />
              </span>
              <span className="home-summary-copy">
                <small>{isJourney ? "Jornada" : action.label}</small>
                <strong>{value}</strong>
                <span>{detail}</span>
              </span>
              <ArrowRight className="home-summary-arrow" size={18} strokeWidth={2.2} aria-hidden="true" />
            </button>
          );
        })}
      </section>

      <div className="home-dashboard-grid">
        <section className="home-steps-section" aria-labelledby="home-next-steps-title">
          <div className="home-section-heading">
            <div>
              <p className="eyebrow">Para continuar</p>
              <h2 id="home-next-steps-title">Acciones de la jornada</h2>
            </div>
          </div>

          <div className="home-step-grid">
            {nextSteps.map((route) => {
              const Icon = route.icon;
              const action = content.summary.find((item) => item.routeId === route.id);
              return (
                <button className="home-step-card" key={route.id} type="button" onClick={() => onNavigate(route.path)}>
                  <span className="home-step-icon" aria-hidden="true">
                    <Icon size={21} strokeWidth={2.2} />
                  </span>
                  <span>
                    <strong>{action?.label ?? route.label}</strong>
                    <small>{action?.detail ?? "Accede a esta herramienta."}</small>
                  </span>
                  <ArrowRight size={18} strokeWidth={2.2} aria-hidden="true" />
                </button>
              );
            })}
          </div>
        </section>

        <aside className="panel home-journey-panel" aria-labelledby="home-journey-title">
          <div className="panel-title">
            <div>
              <p className="eyebrow">Jornada</p>
              <h2 id="home-journey-title">Estado de caja</h2>
            </div>
            <span className={`badge ${journey.status === "open" ? "success" : journey.status === "error" ? "danger" : "warning"}`}>
              {journeyStatus.value}
            </span>
          </div>

          <div className="home-journey-content">
            <span className="home-journey-icon" aria-hidden="true">
              <CalendarDays size={24} strokeWidth={2.2} />
            </span>
            <p>{journeyStatus.detail}</p>
          </div>

          {cajaRoute ? (
            <button className="ghost-button full" type="button" onClick={() => onNavigate(cajaRoute.path)}>
              {journey.status === "empty" && normalizedRole !== "vendedor" ? "Abrir jornada" : "Ver jornada"}
            </button>
          ) : null}
        </aside>
      </div>

      <ModuleOverview
        routes={routes}
        featuredRouteIds={summary.map((action) => action.routeId)}
        onNavigate={onNavigate}
      />
    </>
  );
}
