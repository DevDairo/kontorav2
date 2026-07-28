const DEFAULT_TIMEOUT_MS = 5000;

const serviceDefaults = [
  {
    id: "postgres",
    label: "PostgreSQL",
    envName: "OPS_POSTGRES_CONTAINER",
    localName: "kontora_pos_postgres_local",
    required: true,
  },
  {
    id: "storage",
    label: "Storage",
    envName: "OPS_STORAGE_CONTAINER",
    localName: "kontora_pos_storage_local",
    required: true,
  },
  {
    id: "backend",
    label: "Backend",
    envName: "OPS_BACKEND_CONTAINER",
    localName: "kontora_pos_backend_local",
    required: true,
  },
  {
    id: "frontend",
    label: "Frontend",
    envName: "OPS_FRONTEND_CONTAINER",
    localName: "kontora_pos_frontend_local",
    required: true,
  },
  {
    id: "cloudflared",
    label: "Cloudflare Tunnel",
    envName: "OPS_CLOUDFLARED_CONTAINER",
    localName: "kontora_pos_cloudflared_local",
    required: false,
  },
];

const volumeDefaults = [
  {
    id: "postgres-data",
    label: "Datos PostgreSQL",
    envName: "OPS_POSTGRES_VOLUME",
    localName: "kontora_pos_postgres_local_data",
  },
  {
    id: "storage-data",
    label: "Objetos Storage",
    envName: "OPS_STORAGE_VOLUME",
    localName: "kontora_pos_storage_local_data",
  },
  {
    id: "ops-audit",
    label: "Bitácora operativa",
    envName: "OPS_AUDIT_VOLUME",
    localName: "kontora_ops_audit_local_data",
  },
];

function envValue(env, key, fallback) {
  return env[key]?.trim() || fallback;
}

export function loadDiagnosticsConfig(env = process.env) {
  const dockerApiUrl = envValue(env, "OPS_DOCKER_API_URL", "http://docker-api-proxy:2375")
    .replace(/\/+$/, "");
  const parsedDockerUrl = new URL(dockerApiUrl);
  if (!["http:", "https:"].includes(parsedDockerUrl.protocol)) {
    throw new Error("OPS_DOCKER_API_URL debe usar HTTP o HTTPS");
  }

  return {
    environment: envValue(env, "OPS_ENVIRONMENT", "local"),
    dockerApiUrl,
    timeoutMs: Number.parseInt(envValue(env, "OPS_DIAGNOSTICS_TIMEOUT_MS", `${DEFAULT_TIMEOUT_MS}`), 10),
    services: serviceDefaults.map((service) => ({
      id: service.id,
      label: service.label,
      containerName: envValue(env, service.envName, service.localName),
      required: service.required,
    })),
    volumes: volumeDefaults.map((volume) => ({
      id: volume.id,
      label: volume.label,
      volumeName: envValue(env, volume.envName, volume.localName),
    })),
  };
}

async function fetchJson(fetchImpl, url, timeoutMs) {
  const response = await fetchImpl(url, {
    headers: { Accept: "application/json" },
    signal: AbortSignal.timeout(timeoutMs),
  });
  if (!response.ok) {
    throw new Error(`Docker API respondió HTTP ${response.status}`);
  }
  return response.json();
}

function normalizedContainerName(container) {
  return (container.Names || [])
    .map((name) => name.replace(/^\/+/, ""))
    .find(Boolean);
}

function statusForContainer(container, inspect) {
  if (!container) {
    return "missing";
  }
  const state = inspect?.State?.Status || container.State || "unknown";
  const health = inspect?.State?.Health?.Status || null;
  if (health === "unhealthy") {
    return "unhealthy";
  }
  if (state === "running" && health === "healthy") {
    return "healthy";
  }
  if (state === "running") {
    return "running";
  }
  if (state === "paused") {
    return "paused";
  }
  if (state === "exited" || state === "dead") {
    return "stopped";
  }
  return "unknown";
}

function serviceResponse(definition, container, inspect) {
  const state = statusForContainer(container, inspect);
  return {
    id: definition.id,
    label: definition.label,
    containerName: definition.containerName,
    required: definition.required,
    present: Boolean(container),
    state,
    runtimeState: inspect?.State?.Status || container?.State || null,
    health: inspect?.State?.Health?.Status || null,
    statusText: container?.Status || null,
    image: container?.Image || null,
    startedAt: inspect?.State?.StartedAt || null,
    restartCount: Number.isInteger(inspect?.RestartCount) ? inspect.RestartCount : null,
  };
}

function volumeResponse(definition, volume) {
  return {
    id: definition.id,
    label: definition.label,
    volumeName: definition.volumeName,
    present: Boolean(volume),
    driver: volume?.Driver || null,
    scope: volume?.Scope || null,
    createdAt: volume?.CreatedAt || null,
  };
}

function summarize(services, volumes, engineReachable) {
  const acceptableStates = new Set(["healthy", "running"]);
  const requiredServices = services.filter((service) => service.required);
  const attentionServices = requiredServices.filter(
    (service) => !acceptableStates.has(service.state),
  ).length;
  const missingVolumes = volumes.filter((volume) => !volume.present).length;
  return {
    overall: engineReachable && attentionServices === 0 && missingVolumes === 0
      ? "operational"
      : "attention",
    requiredServices: requiredServices.length,
    operationalServices: requiredServices.length - attentionServices,
    attentionServices,
    volumes: volumes.length,
    missingVolumes,
  };
}

export async function collectDiagnostics(
  config,
  {
    fetchImpl = fetch,
    now = () => new Date(),
  } = {},
) {
  const generatedAt = now().toISOString();
  try {
    const [version, containers] = await Promise.all([
      fetchJson(fetchImpl, `${config.dockerApiUrl}/version`, config.timeoutMs),
      fetchJson(fetchImpl, `${config.dockerApiUrl}/containers/json?all=true`, config.timeoutMs),
    ]);

    const containerByName = new Map(
      containers.map((container) => [normalizedContainerName(container), container]),
    );

    const services = await Promise.all(
      config.services.map(async (definition) => {
        const container = containerByName.get(definition.containerName);
        if (!container) {
          return serviceResponse(definition, null, null);
        }
        try {
          const inspect = await fetchJson(
            fetchImpl,
            `${config.dockerApiUrl}/containers/${encodeURIComponent(container.Id)}/json`,
            config.timeoutMs,
          );
          return serviceResponse(definition, container, inspect);
        } catch {
          return serviceResponse(definition, container, null);
        }
      }),
    );

    const volumes = await Promise.all(
      config.volumes.map(async (definition) => {
        try {
          const volume = await fetchJson(
            fetchImpl,
            `${config.dockerApiUrl}/volumes/${encodeURIComponent(definition.volumeName)}`,
            config.timeoutMs,
          );
          return volumeResponse(definition, volume);
        } catch {
          return volumeResponse(definition, null);
        }
      }),
    );

    return {
      generatedAt,
      mode: "read-only",
      environment: config.environment,
      engine: {
        reachable: true,
        version: version.Version || null,
        apiVersion: version.ApiVersion || null,
        operatingSystem: version.Os || null,
        architecture: version.Arch || null,
      },
      summary: summarize(services, volumes, true),
      services,
      volumes,
    };
  } catch {
    const services = config.services.map((definition) =>
      serviceResponse(definition, null, null));
    const volumes = config.volumes.map((definition) =>
      volumeResponse(definition, null));
    return {
      generatedAt,
      mode: "read-only",
      environment: config.environment,
      engine: {
        reachable: false,
        version: null,
        apiVersion: null,
        operatingSystem: null,
        architecture: null,
      },
      summary: summarize(services, volumes, false),
      services,
      volumes,
    };
  }
}
