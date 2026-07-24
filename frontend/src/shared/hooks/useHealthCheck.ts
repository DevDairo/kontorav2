import { useCallback, useEffect, useState } from "react";
import { getBackendHealth } from "../services/healthService";
import type { HealthResponse } from "../types/api";

export type HealthStatus = "idle" | "loading" | "online" | "offline";

export type UseHealthCheckResult = {
  status: HealthStatus;
  label: string;
  data: HealthResponse | null;
  errorMessage: string | null;
  checkedAt: string | null;
  refresh: () => Promise<void>;
};

const labels: Record<HealthStatus, string> = {
  idle: "Pendiente",
  loading: "Validando",
  online: "Disponible",
  offline: "Sin conexion",
};

export function useHealthCheck(): UseHealthCheckResult {
  const [status, setStatus] = useState<HealthStatus>("idle");
  const [data, setData] = useState<HealthResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [checkedAt, setCheckedAt] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setStatus("loading");
    setErrorMessage(null);

    try {
      const response = await getBackendHealth();
      setData(response);
      setStatus(response.status === "ok" ? "online" : "offline");
    } catch (error) {
      setData(null);
      setStatus("offline");
      setErrorMessage(error instanceof Error ? error.message : "Error desconocido");
    } finally {
      setCheckedAt(new Intl.DateTimeFormat("es-CO", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
      }).format(new Date()));
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  return {
    status,
    label: labels[status],
    data,
    errorMessage,
    checkedAt,
    refresh,
  };
}
