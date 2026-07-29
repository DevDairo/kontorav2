const DEFAULT_TIMEOUT_MS = 10_000;

function envValue(env, key, fallback = "") {
  return env[key]?.trim() || fallback;
}

function booleanValue(value, name) {
  const normalized = value.trim().toLowerCase();
  if (normalized === "true") {
    return true;
  }
  if (normalized === "false") {
    return false;
  }
  throw new Error(`${name} debe ser true o false`);
}

function positiveInteger(value, name) {
  const parsed = Number.parseInt(value, 10);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(`${name} debe ser un entero positivo`);
  }
  return parsed;
}

export function loadBackupsConfig(env = process.env) {
  const enabled = booleanValue(
    envValue(env, "OPS_BACKUPS_ENABLED", "false"),
    "OPS_BACKUPS_ENABLED",
  );
  const executorUrl = envValue(env, "OPS_EXECUTOR_URL", "http://ops-executor:8091")
    .replace(/\/+$/, "");
  const parsedUrl = new URL(executorUrl);
  if (!["http:", "https:"].includes(parsedUrl.protocol)) {
    throw new Error("OPS_EXECUTOR_URL debe usar HTTP o HTTPS");
  }
  const token = envValue(env, "OPS_EXECUTOR_TOKEN");
  if (enabled && token.length < 32) {
    throw new Error(
      "OPS_EXECUTOR_TOKEN debe contener al menos 32 caracteres cuando los respaldos están habilitados",
    );
  }
  return {
    enabled,
    executorUrl,
    token,
    timeoutMs: positiveInteger(
      envValue(env, "OPS_EXECUTOR_TIMEOUT_MS", `${DEFAULT_TIMEOUT_MS}`),
      "OPS_EXECUTOR_TIMEOUT_MS",
    ),
  };
}

export class BackupExecutorError extends Error {
  constructor(message, status = 502) {
    super(message);
    this.name = "BackupExecutorError";
    this.status = status;
  }
}

async function request(config, pathname, options = {}, fetchImpl = fetch) {
  if (!config.enabled) {
    throw new BackupExecutorError("El módulo de respaldos está deshabilitado", 503);
  }
  let response;
  try {
    response = await fetchImpl(`${config.executorUrl}${pathname}`, {
      ...options,
      headers: {
        Accept: "application/json",
        Authorization: `Bearer ${config.token}`,
        ...(options.headers || {}),
      },
      signal: AbortSignal.timeout(config.timeoutMs),
    });
  } catch {
    throw new BackupExecutorError("El ejecutor de respaldos no está disponible");
  }
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    const safeStatus = response.status === 409 ? 409 : 502;
    throw new BackupExecutorError(
      safeStatus === 409
        ? "Ya existe otro respaldo en ejecución"
        : "El ejecutor no pudo completar la solicitud",
      safeStatus,
    );
  }
  return body;
}

export function createBackupsClient(config, { fetchImpl = fetch } = {}) {
  return {
    enabled: config.enabled,
    list() {
      return request(config, "/v1/backups", {}, fetchImpl);
    },
    start({ jobId, operator }) {
      return request(config, "/v1/backups", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ jobId, operator }),
      }, fetchImpl);
    },
  };
}

