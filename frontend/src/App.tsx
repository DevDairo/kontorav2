import { useCallback, useEffect, useMemo, useState } from "react";
import { AppProviders } from "./app/providers/AppProviders";
import {
  findRouteByPath,
  getVisibleRoutes,
  normalizeRole,
} from "./app/routes/appRoutes";
import { LoginPage, useAuth } from "./modules/auth";
import { AuditoriaPanel } from "./modules/auditoria";
import { CajaAbiertaPanel } from "./modules/caja";
import { CatalogosPanel } from "./modules/catalogos";
import { CierreCajaPanel } from "./modules/cierre";
import { ConsultasPanel } from "./modules/consultas";
import { DepositoPanel } from "./modules/deposito";
import { GastosPanel } from "./modules/gastos";
import { InventarioPanel } from "./modules/inventario";
import { TransferenciasPanel } from "./modules/transferencias";
import { UsuariosPanel } from "./modules/usuarios";
import { VentasPanel } from "./modules/ventas";
import { AppShell } from "./shared/components/AppShell";
import { RoleHome } from "./shared/components/RoleHome";
import { RouteWorkspace } from "./shared/components/RouteWorkspace";

function setBrowserPath(path: string, replace = false) {
  if (window.location.pathname === path) {
    return;
  }

  if (replace) {
    window.history.replaceState(null, "", path);
    return;
  }

  window.history.pushState(null, "", path);
}

function SessionLoading() {
  return (
    <main className="session-loading" aria-live="polite">
      <div className="brand brand-centered">
        <span className="brand-mark">K</span>
        <span>Kontora POS</span>
      </div>
      <div className="loading-bar" aria-hidden="true" />
      <span>Validando sesion</span>
    </main>
  );
}

function AppContent() {
  const auth = useAuth();
  const [activePath, setActivePath] = useState(() => window.location.pathname);

  const role = useMemo(() => normalizeRole(auth.user?.nombreRol ?? ""), [auth.user?.nombreRol]);
  const canManageCatalogos = role === "administrador" || role === "gerente";
  const visibleRoutes = useMemo(() => getVisibleRoutes(auth.user?.nombreRol ?? ""), [auth.user?.nombreRol]);
  const activeRoute = findRouteByPath(visibleRoutes, activePath) ?? visibleRoutes[0];

  const navigate = useCallback((path: string, replace = false) => {
    setBrowserPath(path, replace);
    setActivePath(path);
  }, []);

  useEffect(() => {
    const handlePopState = () => {
      setActivePath(window.location.pathname);
    };

    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  useEffect(() => {
    if (auth.status === "authenticated") {
      if (activePath === "/evidencias") {
        navigate("/consultas", true);
        return;
      }
      if (activePath === "/login" || !findRouteByPath(visibleRoutes, activePath)) {
        navigate("/", true);
      }
    }

    if (auth.status === "unauthenticated") {
      navigate("/login", true);
    }
  }, [activePath, auth.status, navigate, visibleRoutes]);

  if (auth.status === "checking" && !auth.user) {
    return <SessionLoading />;
  }

  if (auth.status !== "authenticated" || !auth.user) {
    return <LoginPage />;
  }

  return (
    <AppShell
      routes={visibleRoutes}
      activePath={activeRoute.path}
      user={auth.user}
      onNavigate={navigate}
      onLogout={auth.logout}
      isLoggingOut={auth.isLoggingOut}
    >
      {activeRoute.id === "inicio" ? (
        <RoleHome
          role={role}
          routes={visibleRoutes}
          token={auth.token ?? ""}
          user={auth.user}
          onNavigate={navigate}
        />
      ) : activeRoute.id === "caja" ? (
        <CajaAbiertaPanel token={auth.token ?? ""} role={role} />
      ) : activeRoute.id === "ventas" ? (
        <VentasPanel token={auth.token ?? ""} role={role} />
      ) : activeRoute.id === "inventario" ? (
        <InventarioPanel token={auth.token ?? ""} role={role} />
      ) : activeRoute.id === "gastos" ? (
        <GastosPanel token={auth.token ?? ""} role={role} />
      ) : activeRoute.id === "transferencias" ? (
        <TransferenciasPanel token={auth.token ?? ""} role={role} />
      ) : activeRoute.id === "cierre" ? (
        <CierreCajaPanel token={auth.token ?? ""} />
      ) : activeRoute.id === "deposito" ? (
        <DepositoPanel token={auth.token ?? ""} />
      ) : activeRoute.id === "catalogos" ? (
        <CatalogosPanel token={auth.token ?? ""} canManage={canManageCatalogos} />
      ) : activeRoute.id === "consultas" ? (
        <ConsultasPanel token={auth.token ?? ""} role={role} />
      ) : activeRoute.id === "usuarios" ? (
        <UsuariosPanel token={auth.token ?? ""} currentUserId={auth.user.idUsuario} />
      ) : activeRoute.id === "auditoria" ? (
        <AuditoriaPanel token={auth.token ?? ""} />
      ) : (
        <RouteWorkspace route={activeRoute} role={role} />
      )}
    </AppShell>
  );
}

export default function App() {
  return (
    <AppProviders>
      <AppContent />
    </AppProviders>
  );
}
