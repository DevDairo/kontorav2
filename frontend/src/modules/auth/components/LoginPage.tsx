import { AlertCircle, KeyRound, LogIn, UserRound } from "lucide-react";
import { FormEvent, useState } from "react";
import { useAuth } from "../hooks/useAuth";

export function LoginPage() {
  const auth = useAuth();
  const [nombreUsuario, setNombreUsuario] = useState("");
  const [contrasena, setContrasena] = useState("");
  const [validationError, setValidationError] = useState<string | null>(null);

  const displayedError = validationError ?? auth.errorMessage;

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    auth.clearError();

    if (!nombreUsuario.trim() || !contrasena) {
      setValidationError("Ingresa usuario y contrasena.");
      return;
    }

    setValidationError(null);

    try {
      await auth.login({ nombreUsuario, contrasena });
    } catch {
      // El provider conserva el mensaje real entregado por la API.
    }
  }

  return (
    <main className="login-page" aria-labelledby="login-title">
      <section className="login-card">
        <div className="brand brand-centered">
          <span className="brand-mark">K</span>
          <span>Kontora POS</span>
        </div>

        <div>
          <p className="eyebrow">Acceso seguro</p>
          <h1 id="login-title">Iniciar sesion</h1>
          <p className="lead">
            Ingresa al sistema para gestionar la operacion diaria del punto de venta.
          </p>
        </div>

        <form className="form-grid" onSubmit={handleSubmit}>
          <label className="field-label" htmlFor="nombreUsuario">
            Usuario
            <span className="field-control">
              <UserRound size={18} strokeWidth={2.2} />
              <input
                id="nombreUsuario"
                autoComplete="username"
                type="text"
                value={nombreUsuario}
                onChange={(event) => setNombreUsuario(event.target.value)}
                placeholder="Nombre de usuario"
              />
            </span>
          </label>

          <label className="field-label" htmlFor="contrasena">
            Contrasena
            <span className="field-control">
              <KeyRound size={18} strokeWidth={2.2} />
              <input
                id="contrasena"
                autoComplete="current-password"
                type="password"
                value={contrasena}
                onChange={(event) => setContrasena(event.target.value)}
                placeholder="Contrasena"
              />
            </span>
          </label>

          {displayedError ? (
            <div className="form-alert" role="alert">
              <AlertCircle size={18} strokeWidth={2.2} />
              <span>{displayedError}</span>
            </div>
          ) : null}

          <button className="primary-button full" type="submit" disabled={auth.isSubmitting}>
            <LogIn size={18} strokeWidth={2.2} />
            {auth.isSubmitting ? "Ingresando" : "Ingresar"}
          </button>
        </form>
      </section>

      <section className="login-preview" aria-hidden="true" />
    </main>
  );
}
