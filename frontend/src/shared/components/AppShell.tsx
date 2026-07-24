import { LogOut, Menu, UserRound, X } from "lucide-react";
import { useState, type PropsWithChildren } from "react";
import type { AppRoute } from "../../app/routes/appRoutes";

type AppShellProps = PropsWithChildren<{
  routes: AppRoute[];
  activePath: string;
  user: {
    nombreCompleto: string;
    nombreUsuario: string;
    nombreRol: string;
  };
  onNavigate: (path: string) => void;
  onLogout: () => void;
  isLoggingOut?: boolean;
}>;

export function AppShell({
  routes,
  activePath,
  user,
  onNavigate,
  onLogout,
  isLoggingOut = false,
  children,
}: AppShellProps) {
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  function navigateTo(path: string) {
    onNavigate(path);
    setIsMenuOpen(false);
  }

  return (
    <div className="app-shell">
      <aside className={`sidebar ${isMenuOpen ? "mobile-open" : ""}`}>
        <div className="brand">
          <span className="brand-mark">K</span>
          <span>Kontora POS</span>
        </div>

        <nav className="menu" id="main-navigation" aria-label="Navegacion principal">
          {routes.map((route) => {
            const Icon = route.icon;
            const isActive = route.path === activePath;
            return (
              <button
                key={route.path}
                className={isActive ? "active" : undefined}
                type="button"
                aria-current={isActive ? "page" : undefined}
                onClick={() => navigateTo(route.path)}
              >
                <Icon size={18} strokeWidth={2.2} />
                <span className="menu-label">{route.label}</span>
              </button>
            );
          })}
        </nav>
      </aside>

      <div className="workspace">
        <header className="topbar">
          <div className="topbar-start">
            <button
              className="icon-button mobile-menu-button"
              type="button"
              onClick={() => setIsMenuOpen((current) => !current)}
              aria-label={isMenuOpen ? "Cerrar menu" : "Abrir menu"}
              aria-expanded={isMenuOpen}
              aria-controls="main-navigation"
              title={isMenuOpen ? "Cerrar menu" : "Abrir menu"}
            >
              {isMenuOpen ? <X size={20} strokeWidth={2.2} /> : <Menu size={20} strokeWidth={2.2} />}
            </button>
            <div className="brand mobile-brand">
              <span className="brand-mark">K</span>
              <span>Kontora POS</span>
            </div>
          </div>

          <div className="topbar-actions">
            <div className="user-chip" aria-label={`Sesion de ${user.nombreCompleto}`}>
              <span className="avatar" aria-hidden="true">
                <UserRound size={17} strokeWidth={2.2} />
              </span>
              <span>
                <strong>{user.nombreCompleto}</strong>
                <small>
                  {user.nombreRol} · {user.nombreUsuario}
                </small>
              </span>
            </div>

            <button
              className="icon-button"
              type="button"
              onClick={onLogout}
              disabled={isLoggingOut}
              aria-label="Cerrar sesion"
              title="Cerrar sesion"
            >
              <LogOut size={18} strokeWidth={2.2} />
            </button>
          </div>
        </header>

        <main className="content">{children}</main>
      </div>
    </div>
  );
}
