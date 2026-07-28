const TOKEN_KEY = "kontora.ops.local-token";

const elements = {
  authDialog: document.querySelector("#localAuthDialog"),
  authError: document.querySelector("#authError"),
  authForm: document.querySelector("#localAuthForm"),
  tokenInput: document.querySelector("#localTokenInput"),
  menuButton: document.querySelector("#menuButton"),
  sidebar: document.querySelector("#opsSidebar"),
  logoutButton: document.querySelector("#logoutButton"),
  topbarStatus: document.querySelector("#topbarStatus"),
  topbarStatusText: document.querySelector("#topbarStatusText"),
  operatorName: document.querySelector("#operatorName"),
  operatorMode: document.querySelector("#operatorMode"),
  overallStatus: document.querySelector("#overallStatus"),
  generatedAt: document.querySelector("#generatedAt"),
  operationalServices: document.querySelector("#operationalServices"),
  servicesDetail: document.querySelector("#servicesDetail"),
  availableVolumes: document.querySelector("#availableVolumes"),
  volumesDetail: document.querySelector("#volumesDetail"),
  engineVersion: document.querySelector("#engineVersion"),
  engineDetail: document.querySelector("#engineDetail"),
  dashboardServicesList: document.querySelector("#dashboardServicesList"),
  systemServicesList: document.querySelector("#systemServicesList"),
  dashboardVolumesList: document.querySelector("#dashboardVolumesList"),
  persistenceVolumesList: document.querySelector("#persistenceVolumesList"),
  environmentBadge: document.querySelector("#environmentBadge"),
  databaseReaderBadge: document.querySelector("#databaseReaderBadge"),
  databaseDiagnosticsList: document.querySelector("#databaseDiagnosticsList"),
  flywayDashboardStatus: document.querySelector("#flywayDashboardStatus"),
  flywayDashboardDetail: document.querySelector("#flywayDashboardDetail"),
  bucketDashboardStatus: document.querySelector("#bucketDashboardStatus"),
  bucketDashboardDetail: document.querySelector("#bucketDashboardDetail"),
  evidenceDashboardStatus: document.querySelector("#evidenceDashboardStatus"),
  evidenceDashboardDetail: document.querySelector("#evidenceDashboardDetail"),
  auditTotalEntries: document.querySelector("#auditTotalEntries"),
  auditReturnedEntries: document.querySelector("#auditReturnedEntries"),
  auditIntegrity: document.querySelector("#auditIntegrity"),
  auditIntegrityDetail: document.querySelector("#auditIntegrityDetail"),
  auditStorage: document.querySelector("#auditStorage"),
  auditHeadHash: document.querySelector("#auditHeadHash"),
  auditEnvironmentBadge: document.querySelector("#auditEnvironmentBadge"),
  auditEntriesList: document.querySelector("#auditEntriesList"),
};

let authenticationMode = "local-token";

function currentToken() {
  return window.sessionStorage.getItem(TOKEN_KEY);
}

function authHeaders() {
  const token = currentToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function apiGet(path, { authenticated = true } = {}) {
  const response = await fetch(path, {
    headers: {
      Accept: "application/json",
      ...(authenticated ? authHeaders() : {}),
    },
    cache: "no-store",
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    const error = new Error(body.message || "No fue posible consultar el panel");
    error.status = response.status;
    throw error;
  }
  return body;
}

function stateLabel(state) {
  const labels = {
    healthy: "Saludable",
    running: "En ejecución",
    paused: "Pausado",
    stopped: "Detenido",
    unhealthy: "No saludable",
    missing: "No encontrado",
    unknown: "Sin confirmar",
  };
  return labels[state] || "Sin confirmar";
}

function stateClass(state) {
  if (state === "healthy" || state === "running") {
    return "success";
  }
  if (state === "paused" || state === "unknown") {
    return "warning";
  }
  return "danger";
}

function checkPresentation(state) {
  const presentations = {
    operational: { label: "Correcto", className: "success" },
    attention: { label: "Revisar", className: "warning" },
    unavailable: { label: "No disponible", className: "danger" },
    disabled: { label: "Deshabilitado", className: "warning" },
  };
  return presentations[state] || presentations.unavailable;
}

function setDashboardCheck(statusElement, detailElement, check, detail) {
  const presentation = checkPresentation(check?.state);
  statusElement.className = `badge ${presentation.className}`;
  statusElement.textContent = presentation.label;
  detailElement.textContent = detail || check?.message || "Sin información";
}

function integrityCardMarkup({ label, value, detail, state }) {
  const presentation = checkPresentation(state);
  return `
    <article class="integrity-card">
      <div class="integrity-card-heading">
        <strong>${escapeHtml(label)}</strong>
        <span class="badge ${presentation.className}">${presentation.label}</span>
      </div>
      <span class="integrity-card-value">${escapeHtml(value)}</span>
      <small>${escapeHtml(detail)}</small>
    </article>
  `;
}

function auditPresentation(outcome) {
  const presentations = {
    success: { label: "Correcto", className: "success" },
    attention: { label: "Atención", className: "warning" },
    failure: { label: "Falló", className: "danger" },
  };
  return presentations[outcome] || presentations.attention;
}

function auditActionLabel(action) {
  const labels = {
    "panel.started": "Panel iniciado",
    "panel.stopped": "Panel detenido",
    "diagnostics.snapshot": "Diagnóstico actualizado",
  };
  return labels[action] || action;
}

function auditDetail(entry) {
  if (entry.action === "diagnostics.snapshot") {
    const details = entry.details || {};
    return [
      `Estado ${details.overall || "sin confirmar"}`,
      `${details.operationalServices ?? 0}/${details.requiredServices ?? 0} servicios`,
      `${details.operationalPersistenceChecks ?? 0}/${details.persistenceChecks ?? 0} controles internos`,
    ].join(" · ");
  }
  if (entry.action === "panel.started") {
    return `Autenticación ${entry.details?.authentication || "configurada"} · modo solo lectura`;
  }
  if (entry.action === "panel.stopped") {
    return `Señal ${entry.details?.signal || "desconocida"}`;
  }
  return Object.entries(entry.details || {})
    .map(([key, value]) => `${key}: ${String(value)}`)
    .join(" · ") || "Sin detalle adicional";
}

function auditEntryMarkup(entry) {
  const presentation = auditPresentation(entry.outcome);
  return `
    <article class="audit-entry">
      <div>
        <div class="audit-entry-heading">
          <strong>${escapeHtml(auditActionLabel(entry.action))}</strong>
          <small>#${escapeHtml(entry.sequence)}</small>
        </div>
        <span class="audit-entry-detail">${escapeHtml(auditDetail(entry))}</span>
        <span class="audit-entry-hash">SHA-256 ${escapeHtml(entry.hash.slice(0, 16))}…</span>
      </div>
      <div class="audit-entry-meta">
        <span class="badge ${presentation.className}">${presentation.label}</span>
        <time datetime="${escapeHtml(entry.occurredAt)}">${escapeHtml(formatTimestamp(entry.occurredAt))}</time>
        <small>${escapeHtml(entry.operator)}</small>
      </div>
    </article>
  `;
}

function serviceIcon(service) {
  const icons = {
    postgres: "DB",
    storage: "ST",
    backend: "API",
    frontend: "UI",
    cloudflared: "CF",
  };
  return icons[service.id] || service.label.slice(0, 2).toUpperCase();
}

function serviceMarkup(service) {
  const metadata = [
    service.image,
    service.health ? `health: ${service.health}` : null,
    Number.isInteger(service.restartCount) ? `${service.restartCount} reinicios` : null,
  ].filter(Boolean).join(" · ");

  return `
    <article class="service-row">
      <div class="service-identity">
        <span class="service-icon ${escapeHtml(service.id)}">${serviceIcon(service)}</span>
        <div>
          <strong>${escapeHtml(service.label)}</strong>
          <small>${escapeHtml(service.containerName)}</small>
        </div>
      </div>
      <div class="service-meta">
        <span class="badge ${stateClass(service.state)}">${stateLabel(service.state)}</span>
        <small>${escapeHtml(metadata || service.statusText || "Sin metadatos")}</small>
      </div>
    </article>
  `;
}

function volumeMarkup(volume) {
  const detail = [
    volume.driver ? `Driver ${volume.driver}` : null,
    volume.scope ? `alcance ${volume.scope}` : null,
  ].filter(Boolean).join(" · ");

  return `
    <article class="volume-row">
      <span class="volume-icon ${volume.present ? "present" : "missing"}" aria-hidden="true">◫</span>
      <div>
        <strong>${escapeHtml(volume.label)}</strong>
        <small>${escapeHtml(volume.volumeName)}</small>
        <small>${escapeHtml(detail || "Sin metadatos")}</small>
      </div>
      <span class="badge ${volume.present ? "success" : "danger"}">
        ${volume.present ? "Disponible" : "No encontrado"}
      </span>
    </article>
  `;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function formatTimestamp(value) {
  try {
    return new Intl.DateTimeFormat("es-CO", {
      dateStyle: "medium",
      timeStyle: "medium",
    }).format(new Date(value));
  } catch {
    return value;
  }
}

function setTopbarState(state, text) {
  elements.topbarStatus.className = `status-pill ${state}`;
  elements.topbarStatusText.textContent = text;
}

function renderDatabaseDiagnostics(diagnostics) {
  const database = diagnostics?.database || {};
  const flyway = diagnostics?.flyway || { state: "unavailable" };
  const bucket = diagnostics?.bucket || { state: "unavailable" };
  const evidence = diagnostics?.evidence || { state: "unavailable" };

  elements.databaseReaderBadge.textContent = database.reachable
    ? database.reader
    : checkPresentation(database.state).label;
  elements.databaseReaderBadge.className = database.reachable
    ? "environment-badge"
    : `badge ${checkPresentation(database.state).className}`;

  const flywayDetail = flyway.state === "operational"
    ? `${flyway.installed} migraciones · última V${flyway.latest?.version || "—"}`
    : flyway.message || `${flyway.successful || 0}/${flyway.installed || 0} exitosas`;
  const bucketDetail = bucket.present
    ? `${bucket.id} · ${bucket.public ? "público" : "privado"} · ${bucket.objectCount} objetos`
    : bucket.message || `No se encontró ${bucket.expectedId || "el bucket"}`;
  const evidenceDetail = evidence.state === "operational"
    ? `${evidence.referenceTotal} referencias consistentes`
    : evidence.message
      || `${evidence.missingObjectTotal || 0} faltantes · ${evidence.unreferencedObjectTotal || 0} sin referencia`;

  setDashboardCheck(
    elements.flywayDashboardStatus,
    elements.flywayDashboardDetail,
    flyway,
    flywayDetail,
  );
  setDashboardCheck(
    elements.bucketDashboardStatus,
    elements.bucketDashboardDetail,
    bucket,
    bucketDetail,
  );
  setDashboardCheck(
    elements.evidenceDashboardStatus,
    elements.evidenceDashboardDetail,
    evidence,
    evidenceDetail,
  );

  const cards = [
    {
      label: "Flyway",
      value: flyway.state === "operational"
        ? `${flyway.successful}/${flyway.installed} exitosas`
        : checkPresentation(flyway.state).label,
      detail: flywayDetail,
      state: flyway.state,
    },
    {
      label: "Bucket privado",
      value: bucket.present ? bucket.id : checkPresentation(bucket.state).label,
      detail: bucket.present
        ? `${bucket.objectCount} objetos · límite ${bucket.fileSizeLimit ?? "sin definir"} bytes`
        : bucketDetail,
      state: bucket.state,
    },
    {
      label: "Evidencias",
      value: evidence.state === "operational"
        ? `${evidence.referenceTotal} referencias`
        : checkPresentation(evidence.state).label,
      detail: evidenceDetail,
      state: evidence.state,
    },
  ];
  elements.databaseDiagnosticsList.innerHTML = cards.map(integrityCardMarkup).join("");
}

function renderAudit(data) {
  const verified = data.summary.integrity === "verified";
  elements.auditTotalEntries.textContent = String(data.summary.totalEntries);
  elements.auditReturnedEntries.textContent =
    `${data.summary.returnedEntries} eventos mostrados`;
  elements.auditIntegrity.textContent = verified ? "Verificada" : "Alterada";
  elements.auditIntegrityDetail.textContent = verified
    ? "Cadena SHA-256 válida"
    : `Inconsistencia en línea ${data.summary.invalidLine || "desconocida"}`;
  elements.auditStorage.textContent = data.storage === "volume"
    ? "Volumen Docker"
    : data.storage;
  elements.auditHeadHash.textContent = data.summary.headHash
    ? `Hash ${data.summary.headHash.slice(0, 16)}…`
    : "Sin eventos";
  elements.auditEnvironmentBadge.textContent = data.environment;
  elements.auditEntriesList.innerHTML = data.entries.length
    ? data.entries.map(auditEntryMarkup).join("")
    : '<div class="empty-state">La bitácora todavía no contiene eventos.</div>';
}

function renderDiagnostics(data) {
  const operational = data.summary.overall === "operational";
  elements.overallStatus.textContent = operational ? "Operación estable" : "Requiere atención";
  elements.generatedAt.textContent = `Actualizado ${formatTimestamp(data.generatedAt)}`;
  elements.operationalServices.textContent =
    `${data.summary.operationalServices}/${data.summary.requiredServices}`;
  elements.servicesDetail.textContent = data.summary.attentionServices === 0
    ? "Todos los servicios requeridos"
    : `${data.summary.attentionServices} requieren revisión`;
  elements.availableVolumes.textContent =
    `${data.summary.volumes - data.summary.missingVolumes}/${data.summary.volumes}`;
  elements.volumesDetail.textContent = data.summary.missingVolumes === 0
    ? "Persistencia localizada"
    : `${data.summary.missingVolumes} no encontrados`;
  elements.engineVersion.textContent = data.engine.reachable
    ? `v${data.engine.version || "detectada"}`
    : "Sin conexión";
  elements.engineDetail.textContent = data.engine.reachable
    ? [data.engine.operatingSystem, data.engine.architecture].filter(Boolean).join(" · ")
    : "Proxy Docker no disponible";
  elements.environmentBadge.textContent = data.environment;
  elements.operatorName.textContent = data.operator;
  elements.operatorMode.textContent = `${data.environment} · solo lectura`;
  elements.dashboardServicesList.innerHTML = data.services.map(serviceMarkup).join("");
  elements.systemServicesList.innerHTML = data.services.map(serviceMarkup).join("");
  elements.dashboardVolumesList.innerHTML = data.volumes.map(volumeMarkup).join("");
  elements.persistenceVolumesList.innerHTML = data.volumes.map(volumeMarkup).join("");
  renderDatabaseDiagnostics(data.databaseDiagnostics);
  setTopbarState(operational ? "online" : "offline", operational ? "Operativo" : "Atención");
}

function renderFailure(error) {
  elements.overallStatus.textContent = "No fue posible consultar";
  elements.generatedAt.textContent = error.message;
  setTopbarState("offline", "Sin conexión");
}

function showLocalAuth() {
  elements.authError.textContent = "";
  if (!elements.authDialog.open) {
    elements.authDialog.showModal();
  }
}

async function loadDiagnostics({
  promptOnUnauthorized = true,
  throwOnError = false,
} = {}) {
  document.querySelectorAll(".refresh-button").forEach((button) => {
    button.disabled = true;
    button.textContent = "Consultando…";
  });
  setTopbarState("loading", "Consultando");
  try {
    const data = await apiGet("/api/v1/diagnostics");
    renderDiagnostics(data);
    await loadAudit({ promptOnUnauthorized: false });
    if (elements.authDialog.open) {
      elements.authDialog.close();
    }
    return data;
  } catch (error) {
    const unauthorized = error.status === 401 || error.status === 403;
    if (unauthorized && promptOnUnauthorized && authenticationMode === "local-token") {
      window.sessionStorage.removeItem(TOKEN_KEY);
      showLocalAuth();
    } else {
      renderFailure(error);
    }
    if (throwOnError) {
      throw error;
    }
    return null;
  } finally {
    document.querySelectorAll(".refresh-button").forEach((button) => {
      button.disabled = false;
      button.textContent = "Actualizar estado";
    });
  }
}

async function loadAudit({ promptOnUnauthorized = true } = {}) {
  const buttons = document.querySelectorAll(".audit-refresh-button");
  buttons.forEach((button) => {
    button.disabled = true;
    button.textContent = "Consultando…";
  });
  try {
    const data = await apiGet("/api/v1/audit?limit=100");
    renderAudit(data);
    return data;
  } catch (error) {
    const unauthorized = error.status === 401 || error.status === 403;
    if (unauthorized && promptOnUnauthorized && authenticationMode === "local-token") {
      window.sessionStorage.removeItem(TOKEN_KEY);
      showLocalAuth();
    } else {
      elements.auditEntriesList.innerHTML =
        `<div class="empty-state">${escapeHtml(error.message)}</div>`;
    }
    return null;
  } finally {
    buttons.forEach((button) => {
      button.disabled = false;
      button.textContent = "Actualizar bitácora";
    });
  }
}

function closeMobileMenu() {
  elements.sidebar.classList.remove("mobile-open");
  elements.menuButton.setAttribute("aria-expanded", "false");
  elements.menuButton.textContent = "☰";
  elements.menuButton.setAttribute("aria-label", "Abrir menú");
}

function navigateTo(view) {
  const target = document.querySelector(`[data-view-panel="${view}"]`);
  if (!target) {
    return;
  }
  document.querySelectorAll("[data-view-panel]").forEach((panel) => {
    panel.classList.toggle("active", panel === target);
  });
  document.querySelectorAll("[data-view]").forEach((button) => {
    const active = button.dataset.view === view;
    button.classList.toggle("active", active);
    if (active) {
      button.setAttribute("aria-current", "page");
    } else {
      button.removeAttribute("aria-current");
    }
  });
  window.location.hash = view;
  closeMobileMenu();
  const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  window.scrollTo({ top: 0, behavior: reducedMotion ? "auto" : "smooth" });
}

elements.authForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const token = elements.tokenInput.value.trim();
  if (!token) {
    elements.authError.textContent = "La credencial es obligatoria.";
    return;
  }
  window.sessionStorage.setItem(TOKEN_KEY, token);
  elements.authError.textContent = "";
  try {
    await loadDiagnostics({
      promptOnUnauthorized: false,
      throwOnError: true,
    });
    elements.tokenInput.value = "";
    elements.authDialog.close();
  } catch {
    window.sessionStorage.removeItem(TOKEN_KEY);
    elements.authError.textContent = "La credencial no fue aceptada.";
  }
});

elements.menuButton.addEventListener("click", () => {
  const open = elements.sidebar.classList.toggle("mobile-open");
  elements.menuButton.setAttribute("aria-expanded", String(open));
  elements.menuButton.textContent = open ? "×" : "☰";
  elements.menuButton.setAttribute("aria-label", open ? "Cerrar menú" : "Abrir menú");
});

elements.logoutButton.addEventListener("click", () => {
  window.sessionStorage.removeItem(TOKEN_KEY);
  elements.operatorName.textContent = "Operador";
  if (authenticationMode === "local-token") {
    showLocalAuth();
  }
});

document.querySelectorAll(".refresh-button").forEach((button) => {
  button.addEventListener("click", () => {
    void loadDiagnostics();
  });
});

document.querySelectorAll(".audit-refresh-button").forEach((button) => {
  button.addEventListener("click", () => {
    void loadAudit();
  });
});

document.querySelectorAll("[data-view]").forEach((button) => {
  button.addEventListener("click", () => navigateTo(button.dataset.view));
});

document.querySelectorAll("[data-view-link]").forEach((button) => {
  button.addEventListener("click", () => navigateTo(button.dataset.viewLink));
});

async function initialize() {
  try {
    const health = await apiGet("/api/health", { authenticated: false });
    authenticationMode = health.authentication || "local-token";
  } catch {
    authenticationMode = "local-token";
  }
  const requestedView = window.location.hash.replace(/^#/, "") || "dashboard";
  navigateTo(requestedView);
  await loadDiagnostics();
}

void initialize();
