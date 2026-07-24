import { AlertCircle, Ban, CheckCircle2, CircleOff, KeyRound, Pencil, Plus, RefreshCw, Save, Search, ShieldCheck, UserPlus, UsersRound } from "lucide-react";
import { type FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { ConfirmationDialog } from "../../../shared/components/ConfirmationDialog";
import { ApiClientError } from "../../../shared/services/apiClient";
import {
  actualizarEstadoUsuario,
  actualizarUsuario,
  crearUsuario,
  listarRolesGestion,
  listarUsuarios,
  restablecerContrasenaUsuario,
} from "../services/usuariosService";
import type { ActualizarUsuarioRequest, CrearUsuarioRequest, EstadoUsuario, RolGestion, UsuarioGestion } from "../types";

type LoadState = "loading" | "success" | "error";
type FormMode = "create" | "edit";

type UsuariosPanelProps = {
  currentUserId: string;
  token: string;
};

type UserForm = {
  confirmacionContrasena: string;
  contrasena: string;
  nombreCompleto: string;
  nombreRol: string;
  nombreUsuario: string;
};

type PasswordForm = {
  confirmacionContrasena: string;
  nuevaContrasena: string;
};

const ESTADOS: EstadoUsuario[] = ["activo", "inactivo", "bloqueado"];

function emptyForm(nombreRol = ""): UserForm {
  return {
    confirmacionContrasena: "",
    contrasena: "",
    nombreCompleto: "",
    nombreRol,
    nombreUsuario: "",
  };
}

function emptyPasswordForm(): PasswordForm {
  return {
    confirmacionContrasena: "",
    nuevaContrasena: "",
  };
}

function messageFor(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.message;
  }
  return error instanceof Error ? error.message : "No fue posible completar la gestion de usuarios.";
}

function statusLabel(estado: EstadoUsuario) {
  if (estado === "activo") {
    return "Activo";
  }
  if (estado === "inactivo") {
    return "Inactivo";
  }
  return "Bloqueado";
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("es-CO", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function actionLabel(estado: EstadoUsuario) {
  if (estado === "activo") {
    return "Activar";
  }
  if (estado === "inactivo") {
    return "Inactivar";
  }
  return "Bloquear";
}

export function UsuariosPanel({ currentUserId, token }: UsuariosPanelProps) {
  const [usuarios, setUsuarios] = useState<UsuarioGestion[]>([]);
  const [roles, setRoles] = useState<RolGestion[]>([]);
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [formMessage, setFormMessage] = useState<string | null>(null);
  const [formMode, setFormMode] = useState<FormMode>("create");
  const [form, setForm] = useState<UserForm>(emptyForm());
  const [passwordForm, setPasswordForm] = useState<PasswordForm>(emptyPasswordForm());
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [estadoFiltro, setEstadoFiltro] = useState<"todos" | EstadoUsuario>("todos");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [pendingState, setPendingState] = useState<EstadoUsuario | null>(null);
  const [isChangingState, setIsChangingState] = useState(false);
  const [isResettingPassword, setIsResettingPassword] = useState(false);
  const [pendingPasswordChange, setPendingPasswordChange] = useState(false);

  const cargarUsuarios = useCallback(async () => {
    setLoadState("loading");
    setErrorMessage(null);

    try {
      const [usuariosResponse, rolesResponse] = await Promise.all([
        listarUsuarios(token),
        listarRolesGestion(token),
      ]);
      setUsuarios(usuariosResponse);
      setRoles(rolesResponse);
      setForm((current) => current.nombreRol ? current : emptyForm(rolesResponse[0]?.nombreRol ?? ""));
      setLoadState("success");
    } catch (error) {
      setUsuarios([]);
      setRoles([]);
      setLoadState("error");
      setErrorMessage(messageFor(error));
    }
  }, [token]);

  useEffect(() => {
    void cargarUsuarios();
  }, [cargarUsuarios]);

  const selectedUser = useMemo(
    () => usuarios.find((usuario) => usuario.idUsuario === selectedId) ?? null,
    [selectedId, usuarios],
  );

  const usuariosFiltrados = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    return usuarios.filter((usuario) => {
      const matchesState = estadoFiltro === "todos" || usuario.estado === estadoFiltro;
      const matchesQuery = !normalizedQuery || [usuario.nombreCompleto, usuario.nombreUsuario, usuario.nombreRol]
        .some((value) => value.toLowerCase().includes(normalizedQuery));
      return matchesState && matchesQuery;
    });
  }, [estadoFiltro, query, usuarios]);

  const resumen = useMemo(() => [
    { detail: "Usuarios registrados", label: "Total", value: String(usuarios.length) },
    { detail: "Pueden iniciar sesion", label: "Activos", value: String(usuarios.filter((usuario) => usuario.estado === "activo").length) },
    { detail: "Sin acceso operativo", label: "Inactivos", value: String(usuarios.filter((usuario) => usuario.estado === "inactivo").length) },
    { detail: "Acceso restringido", label: "Bloqueados", value: String(usuarios.filter((usuario) => usuario.estado === "bloqueado").length) },
  ], [usuarios]);

  function actualizarCampo<K extends keyof UserForm>(campo: K, valor: UserForm[K]) {
    setForm((current) => ({ ...current, [campo]: valor }));
    setFormMessage(null);
  }

  function iniciarCreacion() {
    setFormMode("create");
    setSelectedId(null);
    setForm(emptyForm(roles[0]?.nombreRol ?? ""));
    setPasswordForm(emptyPasswordForm());
    setFormMessage(null);
  }

  function seleccionarUsuario(usuario: UsuarioGestion) {
    setSelectedId(usuario.idUsuario);
    setFormMode("edit");
    setForm({
      confirmacionContrasena: "",
      contrasena: "",
      nombreCompleto: usuario.nombreCompleto,
      nombreRol: usuario.nombreRol,
      nombreUsuario: usuario.nombreUsuario,
    });
    setPasswordForm(emptyPasswordForm());
    setFormMessage(null);
  }

  async function guardarUsuario(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormMessage(null);

    if (formMode === "create" && form.contrasena !== form.confirmacionContrasena) {
      setFormMessage("La confirmacion de contrasena no coincide.");
      return;
    }

    setIsSubmitting(true);
    try {
      const request: ActualizarUsuarioRequest = {
        nombreCompleto: form.nombreCompleto.trim(),
        nombreRol: form.nombreRol,
        nombreUsuario: form.nombreUsuario.trim(),
      };
      const response = formMode === "create"
        ? await crearUsuario(token, { ...request, contrasena: form.contrasena })
        : await actualizarUsuario(token, selectedUser?.idUsuario ?? "", request);

      setUsuarios((current) => formMode === "create"
        ? [...current, response].sort((left, right) => left.nombreCompleto.localeCompare(right.nombreCompleto))
        : current.map((usuario) => usuario.idUsuario === response.idUsuario ? response : usuario));
      setSelectedId(response.idUsuario);
      setFormMode("edit");
      setForm({
        confirmacionContrasena: "",
        contrasena: "",
        nombreCompleto: response.nombreCompleto,
        nombreRol: response.nombreRol,
        nombreUsuario: response.nombreUsuario,
      });
      setFormMessage(formMode === "create" ? "Usuario creado correctamente." : "Cambios guardados correctamente.");
    } catch (error) {
      setFormMessage(messageFor(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function confirmarCambioEstado() {
    if (!selectedUser || !pendingState) {
      return;
    }

    setIsChangingState(true);
    setFormMessage(null);
    try {
      const response = await actualizarEstadoUsuario(token, selectedUser.idUsuario, pendingState);
      setUsuarios((current) => current.map((usuario) => usuario.idUsuario === response.idUsuario ? response : usuario));
      setFormMessage(`Usuario ${statusLabel(response.estado).toLowerCase()} correctamente.`);
      setPendingState(null);
    } catch (error) {
      setFormMessage(messageFor(error));
      setPendingState(null);
    } finally {
      setIsChangingState(false);
    }
  }

  function actualizarCampoContrasena<K extends keyof PasswordForm>(campo: K, valor: PasswordForm[K]) {
    setPasswordForm((current) => ({ ...current, [campo]: valor }));
    setFormMessage(null);
  }

  function solicitarCambioContrasena(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormMessage(null);

    if (passwordForm.nuevaContrasena !== passwordForm.confirmacionContrasena) {
      setFormMessage("La confirmacion de contrasena no coincide.");
      return;
    }

    setPendingPasswordChange(true);
  }

  async function confirmarCambioContrasena() {
    if (!selectedUser) {
      return;
    }

    setIsResettingPassword(true);
    setFormMessage(null);
    try {
      await restablecerContrasenaUsuario(token, selectedUser.idUsuario, {
        nuevaContrasena: passwordForm.nuevaContrasena,
      });
      setPasswordForm(emptyPasswordForm());
      setPendingPasswordChange(false);
      setFormMessage(selectedUser.idUsuario === currentUserId
        ? "Contrasena actualizada. Tu sesion actual se conserva y las demas se revocaron."
        : "Contrasena actualizada. Las sesiones activas del usuario fueron revocadas.");
    } catch (error) {
      setFormMessage(messageFor(error));
      setPendingPasswordChange(false);
    } finally {
      setIsResettingPassword(false);
    }
  }

  const pendingStateDescription = selectedUser && pendingState
    ? `${actionLabel(pendingState)} el acceso de ${selectedUser.nombreCompleto}. La accion se registra en auditoria y no elimina su historial.`
    : "";

  return (
    <>
      <section className="usuarios-panel" aria-label="Gestion de usuarios">
        <header className="module-header usuarios-header">
          <div>
            <span className="eyebrow">Control de acceso</span>
            <h1>Usuarios</h1>
            <p>Crea usuarios, asigna roles y conserva su historial al cambiar su estado de acceso.</p>
          </div>
          <button className="icon-button" type="button" onClick={() => void cargarUsuarios()} disabled={loadState === "loading"} aria-label="Actualizar usuarios" title="Actualizar usuarios">
            <RefreshCw size={18} aria-hidden="true" />
          </button>
        </header>

        {errorMessage ? (
          <div className="error-alert usuarios-alert" role="alert">
            <AlertCircle size={18} aria-hidden="true" />
            <span>{errorMessage}</span>
          </div>
        ) : null}

        <div className="usuarios-summary-grid" aria-label="Resumen de usuarios">
          {resumen.map((item) => (
            <article className="usuarios-summary-card" key={item.label}>
              <span>{item.label}</span>
              <strong>{item.value}</strong>
              <small>{item.detail}</small>
            </article>
          ))}
        </div>

        <div className="usuarios-workspace">
          <section className="usuarios-data-panel usuarios-form-panel" aria-labelledby="usuarios-form-title">
            <div className="compact-heading">
              <div>
                <span className="eyebrow">{formMode === "create" ? "Alta" : "Edicion"}</span>
                <h2 id="usuarios-form-title">{formMode === "create" ? "Nuevo usuario" : "Datos del usuario"}</h2>
              </div>
              {formMode === "edit" ? (
                <button className="icon-button" type="button" onClick={iniciarCreacion} aria-label="Crear otro usuario" title="Crear otro usuario">
                  <Plus size={18} aria-hidden="true" />
                </button>
              ) : <UserPlus size={22} aria-hidden="true" />}
            </div>

            <form className="usuarios-form" onSubmit={guardarUsuario}>
              <label className="form-field usuarios-form-full">
                <span>Nombre completo</span>
                <input className="field-control plain" value={form.nombreCompleto} onChange={(event) => actualizarCampo("nombreCompleto", event.target.value)} autoComplete="name" required maxLength={120} />
              </label>
              <label className="form-field">
                <span>Nombre de usuario</span>
                <input className="field-control plain" value={form.nombreUsuario} onChange={(event) => actualizarCampo("nombreUsuario", event.target.value.replace(/[^A-Za-z0-9]/g, ""))} autoComplete="username" required minLength={3} maxLength={50} />
              </label>
              <label className="form-field">
                <span>Rol</span>
                <select className="field-control plain" value={form.nombreRol} onChange={(event) => actualizarCampo("nombreRol", event.target.value)} required>
                  <option value="" disabled>Selecciona un rol</option>
                  {roles.map((rol) => <option key={rol.idRol} value={rol.nombreRol}>{rol.nombreRol}</option>)}
                </select>
              </label>
              {formMode === "create" ? (
                <>
                  <label className="form-field">
                    <span>Contrasena inicial</span>
                    <input className="field-control plain" type="password" value={form.contrasena} onChange={(event) => actualizarCampo("contrasena", event.target.value)} autoComplete="new-password" required minLength={8} maxLength={72} />
                  </label>
                  <label className="form-field">
                    <span>Confirmar contrasena</span>
                    <input className="field-control plain" type="password" value={form.confirmacionContrasena} onChange={(event) => actualizarCampo("confirmacionContrasena", event.target.value)} autoComplete="new-password" required minLength={8} maxLength={72} />
                  </label>
                </>
              ) : null}
              <div className="usuarios-form-actions usuarios-form-full">
                <button className="primary-button" type="submit" disabled={isSubmitting || loadState !== "success"}>
                  {formMode === "create" ? <UserPlus size={18} aria-hidden="true" /> : <Save size={18} aria-hidden="true" />}
                  {isSubmitting ? "Guardando" : formMode === "create" ? "Crear usuario" : "Guardar cambios"}
                </button>
                {formMode === "edit" ? (
                  <button className="ghost-button" type="button" onClick={iniciarCreacion} disabled={isSubmitting}>
                    Nuevo
                  </button>
                ) : null}
              </div>
            </form>

            {selectedUser && formMode === "edit" ? (
              <form className="usuarios-password-reset" onSubmit={solicitarCambioContrasena}>
                <div className="usuarios-password-heading">
                  <div>
                    <span className="eyebrow">Seguridad</span>
                    <h3>Restablecer contrasena</h3>
                  </div>
                  <KeyRound size={20} aria-hidden="true" />
                </div>
                <div className="usuarios-password-fields">
                  <label className="form-field">
                    <span>Nueva contrasena</span>
                    <input className="field-control plain" type="password" value={passwordForm.nuevaContrasena} onChange={(event) => actualizarCampoContrasena("nuevaContrasena", event.target.value)} autoComplete="new-password" required minLength={8} maxLength={72} />
                  </label>
                  <label className="form-field">
                    <span>Confirmar contrasena</span>
                    <input className="field-control plain" type="password" value={passwordForm.confirmacionContrasena} onChange={(event) => actualizarCampoContrasena("confirmacionContrasena", event.target.value)} autoComplete="new-password" required minLength={8} maxLength={72} />
                  </label>
                </div>
                <div className="usuarios-form-actions">
                  <button className="ghost-button" type="submit" disabled={isResettingPassword || loadState !== "success"}>
                    <KeyRound size={17} aria-hidden="true" />
                    {isResettingPassword ? "Actualizando" : "Cambiar contrasena"}
                  </button>
                </div>
              </form>
            ) : null}

            {formMessage ? <p className="usuarios-form-message" role="status">{formMessage}</p> : null}

            {selectedUser && formMode === "edit" ? (
              <div className="usuarios-state-controls">
                <div>
                  <span>Estado de acceso</span>
                  <strong className={`usuarios-status ${selectedUser.estado}`}>{statusLabel(selectedUser.estado)}</strong>
                </div>
                <div className="usuarios-state-actions">
                  {ESTADOS.map((estado) => {
                    const isOwnProtectedState = selectedUser.idUsuario === currentUserId && estado !== "activo";
                    return (
                      <button
                        className={`usuarios-state-button ${estado}`}
                        type="button"
                        key={estado}
                        onClick={() => setPendingState(estado)}
                        disabled={selectedUser.estado === estado || isOwnProtectedState || isChangingState}
                        title={isOwnProtectedState ? "No puedes restringir tu propio acceso" : actionLabel(estado)}
                      >
                        {estado === "activo" ? <CheckCircle2 size={16} aria-hidden="true" /> : estado === "inactivo" ? <CircleOff size={16} aria-hidden="true" /> : <Ban size={16} aria-hidden="true" />}
                        {actionLabel(estado)}
                      </button>
                    );
                  })}
                </div>
              </div>
            ) : null}
          </section>

          <section className="usuarios-data-panel usuarios-directory-panel" aria-labelledby="usuarios-directory-title">
            <div className="compact-heading">
              <div>
                <span className="eyebrow">Directorio</span>
                <h2 id="usuarios-directory-title">Usuarios registrados</h2>
              </div>
              <UsersRound size={22} aria-hidden="true" />
            </div>

            <div className="usuarios-filter-bar">
              <label className="field-control usuarios-search-control">
                <Search size={18} aria-hidden="true" />
                <input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Buscar usuario o rol" />
              </label>
              <label className="form-field usuarios-filter-select">
                <span>Estado</span>
                <select className="field-control plain" value={estadoFiltro} onChange={(event) => setEstadoFiltro(event.target.value as "todos" | EstadoUsuario)}>
                  <option value="todos">Todos</option>
                  {ESTADOS.map((estado) => <option key={estado} value={estado}>{statusLabel(estado)}</option>)}
                </select>
              </label>
            </div>

            {loadState === "loading" ? <p className="loading-copy">Cargando usuarios...</p> : null}
            {loadState === "success" && usuariosFiltrados.length === 0 ? <p className="empty-copy">No hay usuarios para el filtro seleccionado.</p> : null}

            <ul className="usuarios-record-list">
              {usuariosFiltrados.map((usuario) => (
                <li key={usuario.idUsuario}>
                  <button className={`usuarios-record-row ${selectedUser?.idUsuario === usuario.idUsuario ? "selected" : ""}`} type="button" onClick={() => seleccionarUsuario(usuario)} aria-pressed={selectedUser?.idUsuario === usuario.idUsuario}>
                    <span>
                      <strong>{usuario.nombreCompleto}</strong>
                      <small>{usuario.nombreUsuario} · {usuario.nombreRol}</small>
                    </span>
                    <span className={`usuarios-status ${usuario.estado}`}>{statusLabel(usuario.estado)}</span>
                    <Pencil size={17} aria-hidden="true" />
                  </button>
                </li>
              ))}
            </ul>

            {selectedUser ? <p className="usuarios-selected-meta">Actualizado {formatDateTime(selectedUser.fechaActualizacion)}</p> : null}
          </section>
        </div>
      </section>

      <ConfirmationDialog
        confirmLabel={pendingState ? actionLabel(pendingState) : "Confirmar"}
        description={pendingStateDescription}
        isConfirming={isChangingState}
        onCancel={() => setPendingState(null)}
        onConfirm={() => void confirmarCambioEstado()}
        open={pendingState !== null}
        title={pendingState ? `${actionLabel(pendingState)} usuario` : "Cambiar estado"}
      />

      <ConfirmationDialog
        confirmLabel="Cambiar contrasena"
        description={selectedUser?.idUsuario === currentUserId
          ? "Se actualizara tu contrasena. La sesion actual se conserva y las demas sesiones activas se revocaran."
          : `Se actualizara la contrasena de ${selectedUser?.nombreCompleto ?? "este usuario"} y se revocaran sus sesiones activas.`}
        isConfirming={isResettingPassword}
        onCancel={() => setPendingPasswordChange(false)}
        onConfirm={() => void confirmarCambioContrasena()}
        open={pendingPasswordChange}
        title="Cambiar contrasena"
      />
    </>
  );
}
