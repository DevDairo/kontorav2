import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from "react";
import { ApiClientError } from "../../../shared/services/apiClient";
import {
  getCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
} from "../services/authService";
import type { AuthStatus, LoginRequest, LoginResponse, UsuarioAutenticado } from "../types";
import { clearAuthToken, readAuthToken, saveAuthToken } from "../utils/tokenStorage";

type AuthContextValue = {
  status: AuthStatus;
  user: UsuarioAutenticado | null;
  token: string | null;
  errorMessage: string | null;
  isSubmitting: boolean;
  isLoggingOut: boolean;
  clearError: () => void;
  login: (credentials: LoginRequest) => Promise<LoginResponse>;
  logout: () => Promise<void>;
  refreshSession: () => Promise<void>;
};

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function errorMessageFor(error: unknown) {
  if (error instanceof ApiClientError) {
    return error.message;
  }

  return error instanceof Error ? error.message : "No fue posible completar la operacion";
}

function userFromLogin(response: LoginResponse): UsuarioAutenticado {
  return {
    idUsuario: response.idUsuario,
    nombreUsuario: response.nombreUsuario,
    nombreCompleto: response.nombreCompleto,
    nombreRol: response.nombreRol,
  };
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [status, setStatus] = useState<AuthStatus>("checking");
  const [token, setToken] = useState<string | null>(() => readAuthToken());
  const [user, setUser] = useState<UsuarioAutenticado | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  const clearLocalSession = useCallback(() => {
    clearAuthToken();
    setToken(null);
    setUser(null);
    setStatus("unauthenticated");
  }, []);

  const clearError = useCallback(() => {
    setErrorMessage(null);
  }, []);

  const refreshSession = useCallback(async () => {
    const storedToken = readAuthToken();

    if (!storedToken) {
      clearLocalSession();
      setErrorMessage(null);
      return;
    }

    setStatus("checking");
    setErrorMessage(null);

    try {
      const currentUser = await getCurrentUser(storedToken);
      setToken(storedToken);
      setUser(currentUser);
      setStatus("authenticated");
    } catch (error) {
      clearLocalSession();
      setErrorMessage(
        error instanceof ApiClientError && error.status === 401
          ? "Tu sesion ya no esta activa. Ingresa nuevamente."
          : errorMessageFor(error),
      );
    }
  }, [clearLocalSession]);

  const login = useCallback(async (credentials: LoginRequest) => {
    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      const response = await loginRequest({
        nombreUsuario: credentials.nombreUsuario.trim(),
        contrasena: credentials.contrasena,
      });

      saveAuthToken(response.token);
      setToken(response.token);
      setUser(userFromLogin(response));
      setStatus("authenticated");
      return response;
    } catch (error) {
      clearLocalSession();
      setErrorMessage(errorMessageFor(error));
      throw error;
    } finally {
      setIsSubmitting(false);
    }
  }, [clearLocalSession]);

  const logout = useCallback(async () => {
    const activeToken = token ?? readAuthToken();
    setIsLoggingOut(true);
    setErrorMessage(null);

    try {
      if (activeToken) {
        await logoutRequest(activeToken);
      }
    } catch (error) {
      if (!(error instanceof ApiClientError && error.status === 401)) {
        setErrorMessage(errorMessageFor(error));
      }
    } finally {
      clearLocalSession();
      setIsLoggingOut(false);
    }
  }, [clearLocalSession, token]);

  useEffect(() => {
    void refreshSession();
  }, [refreshSession]);

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      user,
      token,
      errorMessage,
      isSubmitting,
      isLoggingOut,
      clearError,
      login,
      logout,
      refreshSession,
    }),
    [
      status,
      user,
      token,
      errorMessage,
      isSubmitting,
      isLoggingOut,
      clearError,
      login,
      logout,
      refreshSession,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
