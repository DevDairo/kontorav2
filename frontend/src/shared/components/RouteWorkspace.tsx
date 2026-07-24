import {
  getRouteDescriptionForRole,
  roleLabels,
  type AppRoute,
  type UserRole,
} from "../../app/routes/appRoutes";

type RouteWorkspaceProps = {
  route: AppRoute;
  role: UserRole | null;
};

export function RouteWorkspace({ route, role }: RouteWorkspaceProps) {
  const Icon = route.icon;
  const roleLabel = role ? roleLabels[role] : "Rol no reconocido";
  const description = getRouteDescriptionForRole(route, role);

  return (
    <>
      <section className="section-heading" aria-labelledby={`${route.id}-title`}>
        <div>
          <p className="eyebrow">{roleLabel}</p>
          <h1 id={`${route.id}-title`}>{route.label}</h1>
          <p className="lead">{description}</p>
        </div>
      </section>

      <div className="route-workspace-grid">
        <article className="panel route-focus-panel">
          <div className="route-focus-icon">
            <Icon size={28} strokeWidth={2.1} />
          </div>
          <div>
            <h2>{route.label}</h2>
            <p>{description}</p>
          </div>
        </article>

        <article className="panel">
          <div className="panel-title">
            <div>
              <h2>Roles visibles</h2>
              <p>Filtro de experiencia</p>
            </div>
          </div>
          <div className="role-chip-list">
            {route.roles.map((routeRole) => (
              <span className={routeRole === role ? "role-chip active" : "role-chip"} key={routeRole}>
                {roleLabels[routeRole]}
              </span>
            ))}
          </div>
        </article>
      </div>
    </>
  );
}
