import { createReadStream } from "node:fs";
import { access, stat } from "node:fs/promises";
import { randomUUID } from "node:crypto";
import { createServer } from "node:http";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { createAuditLog, loadAuditConfig } from "./audit-log.mjs";
import { AuthError, createAuthenticator, loadAuthConfig } from "./auth.mjs";
import {
  BackupExecutorError,
  createBackupsClient,
  loadBackupsConfig,
} from "./backups.mjs";
import {
  collectDatabaseDiagnostics,
  loadDatabaseDiagnosticsConfig,
} from "./database-diagnostics.mjs";
import { collectDiagnostics, loadDiagnosticsConfig } from "./diagnostics.mjs";
import { createCsrfGuard } from "./csrf.mjs";

const currentDirectory = path.dirname(fileURLToPath(import.meta.url));
const publicDirectory = path.resolve(currentDirectory, "../public");
const port = Number.parseInt(process.env.OPS_PORT || "8090", 10);
const bindAddress = process.env.OPS_BIND_ADDRESS || "0.0.0.0";
const authConfig = loadAuthConfig();
const diagnosticsConfig = loadDiagnosticsConfig();
const databaseDiagnosticsConfig = loadDatabaseDiagnosticsConfig();
const auditConfig = loadAuditConfig();
const backupsConfig = loadBackupsConfig();
const auditLog = createAuditLog(auditConfig);
const authenticate = createAuthenticator(authConfig);
const backupsClient = createBackupsClient(backupsConfig);
const csrfGuard = backupsConfig.enabled
  ? createCsrfGuard(backupsConfig.token)
  : null;
const monitoredBackups = new Set();
const MAX_JSON_BODY_BYTES = 16_384;

const contentTypes = new Map([
  [".css", "text/css; charset=utf-8"],
  [".html", "text/html; charset=utf-8"],
  [".ico", "image/x-icon"],
  [".js", "text/javascript; charset=utf-8"],
  [".json", "application/json; charset=utf-8"],
  [".png", "image/png"],
  [".txt", "text/plain; charset=utf-8"],
  [".webmanifest", "application/manifest+json; charset=utf-8"],
]);

function securityHeaders(extra = {}) {
  return {
    "Cache-Control": "no-store",
    "Content-Security-Policy": "default-src 'self'; connect-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'",
    "Cross-Origin-Opener-Policy": "same-origin",
    "Cross-Origin-Resource-Policy": "same-origin",
    "Referrer-Policy": "no-referrer",
    "X-Content-Type-Options": "nosniff",
    "X-Frame-Options": "DENY",
    ...extra,
  };
}

function sendJson(response, status, body) {
  response.writeHead(status, securityHeaders({
    "Content-Type": "application/json; charset=utf-8",
  }));
  response.end(JSON.stringify(body));
}

function sendText(response, status, body) {
  response.writeHead(status, securityHeaders({
    "Content-Type": "text/plain; charset=utf-8",
  }));
  response.end(body);
}

async function requireIdentity(request) {
  return authenticate(request);
}

async function readJson(request) {
  const chunks = [];
  let bytes = 0;
  for await (const chunk of request) {
    bytes += chunk.length;
    if (bytes > MAX_JSON_BODY_BYTES) {
      const error = new Error("La solicitud supera el límite permitido");
      error.status = 413;
      throw error;
    }
    chunks.push(chunk);
  }
  try {
    return JSON.parse(Buffer.concat(chunks).toString("utf8") || "{}");
  } catch {
    const error = new Error("El cuerpo JSON no es válido");
    error.status = 400;
    throw error;
  }
}

function requireCsrf(request, identity) {
  if (!csrfGuard?.verify(identity, request.headers["x-kontora-csrf"])) {
    const error = new Error("La confirmación de seguridad expiró");
    error.status = 403;
    throw error;
  }
}

async function monitorBackup(jobId, operator) {
  if (monitoredBackups.has(jobId)) {
    return;
  }
  monitoredBackups.add(jobId);
  try {
    for (let attempt = 0; attempt < 1800; attempt += 1) {
      await new Promise((resolve) => setTimeout(resolve, 2000));
      const data = await backupsClient.list();
      const backup = data.backups.find((item) => item.id === jobId);
      if (!backup || backup.state === "running") {
        continue;
      }
      const bytes = backup.files.reduce((sum, file) => sum + (file.bytes || 0), 0);
      await auditLog.append({
        category: "backup",
        action: backup.state === "success" ? "backup.completed" : "backup.failed",
        outcome: backup.state === "success" ? "success" : "failure",
        operator,
        details: {
          jobId,
          state: backup.state,
          files: backup.files.length,
          bytes,
          externalCopy: backup.externalCopy?.state || "pending",
          restoreVerification: backup.restoreVerification?.state || "pending",
        },
      });
      return;
    }
    await auditLog.append({
      category: "backup",
      action: "backup.monitor-timeout",
      outcome: "attention",
      operator,
      details: { jobId },
    });
  } catch {
    await auditLog.append({
      category: "backup",
      action: "backup.monitor-failed",
      outcome: "attention",
      operator,
      details: { jobId },
    }).catch(() => {});
  } finally {
    monitoredBackups.delete(jobId);
  }
}

async function handleApi(request, response, url) {
  const { pathname } = url;

  if (pathname === "/api/health") {
    if (request.method !== "GET" && request.method !== "HEAD") {
      response.setHeader("Allow", "GET, HEAD");
      sendJson(response, 405, { message: "Método no permitido" });
      return;
    }
    sendJson(response, 200, {
      status: "ok",
      service: "kontora-ops-panel",
      mode: backupsConfig.enabled ? "controlled-operations" : "read-only",
      authentication: authConfig.mode,
      audit: auditLog.status(),
      backups: { enabled: backupsConfig.enabled },
    });
    return;
  }

  const identity = await requireIdentity(request);
  if (pathname === "/api/v1/session") {
    if (request.method !== "GET" && request.method !== "HEAD") {
      response.setHeader("Allow", "GET, HEAD");
      sendJson(response, 405, { message: "Método no permitido" });
      return;
    }
    sendJson(response, 200, {
      email: identity.email,
      provider: identity.provider,
      environment: diagnosticsConfig.environment,
      mode: backupsConfig.enabled ? "controlled-operations" : "read-only",
      csrfToken: csrfGuard?.issue(identity) || null,
    });
    return;
  }

  if (pathname === "/api/v1/diagnostics") {
    if (request.method !== "GET" && request.method !== "HEAD") {
      response.setHeader("Allow", "GET, HEAD");
      sendJson(response, 405, { message: "Método no permitido" });
      return;
    }
    const [diagnostics, databaseDiagnostics] = await Promise.all([
      collectDiagnostics(diagnosticsConfig),
      collectDatabaseDiagnostics(databaseDiagnosticsConfig),
    ]);
    const databaseAttention = databaseDiagnostics.enabled
      && databaseDiagnostics.summary.overall !== "operational";
    const responseBody = {
      ...diagnostics,
      summary: {
        ...diagnostics.summary,
        overall: diagnostics.summary.overall === "operational" && !databaseAttention
          ? "operational"
          : "attention",
        persistenceChecks: databaseDiagnostics.summary.checks,
        operationalPersistenceChecks: databaseDiagnostics.summary.operationalChecks,
        attentionPersistenceChecks: databaseDiagnostics.summary.attentionChecks,
      },
      databaseDiagnostics,
      operator: identity.email,
    };
    if (request.method === "GET") {
      await auditLog.append({
        category: "diagnostics",
        action: "diagnostics.snapshot",
        outcome: responseBody.summary.overall === "operational" ? "success" : "attention",
        operator: identity.email,
        details: {
          overall: responseBody.summary.overall,
          operationalServices: responseBody.summary.operationalServices,
          requiredServices: responseBody.summary.requiredServices,
          operationalPersistenceChecks:
            responseBody.summary.operationalPersistenceChecks,
          persistenceChecks: responseBody.summary.persistenceChecks,
        },
      });
    }
    sendJson(response, 200, responseBody);
    return;
  }

  if (pathname === "/api/v1/audit") {
    if (request.method !== "GET" && request.method !== "HEAD") {
      response.setHeader("Allow", "GET, HEAD");
      sendJson(response, 405, { message: "Método no permitido" });
      return;
    }
    const requestedLimit = url.searchParams.get("limit") || `${auditConfig.defaultLimit}`;
    if (!/^\d+$/.test(requestedLimit) || Number.parseInt(requestedLimit, 10) < 1) {
      sendJson(response, 400, { message: "El límite de bitácora no es válido" });
      return;
    }
    const audit = await auditLog.list(requestedLimit);
    sendJson(response, 200, {
      ...audit,
      operator: identity.email,
      environment: diagnosticsConfig.environment,
    });
    return;
  }

  if (pathname === "/api/v1/backups") {
    if (request.method === "GET" || request.method === "HEAD") {
      if (!backupsConfig.enabled) {
        sendJson(response, 200, {
          generatedAt: new Date().toISOString(),
          enabled: false,
          activeJob: null,
          backups: [],
        });
        return;
      }
      sendJson(response, 200, {
        ...(await backupsClient.list()),
        enabled: true,
      });
      return;
    }
    if (request.method === "POST") {
      if (request.headers["content-type"] !== "application/json") {
        sendJson(response, 415, { message: "Se requiere application/json" });
        return;
      }
      requireCsrf(request, identity);
      const body = await readJson(request);
      if (body.confirmation !== "RESPALDAR") {
        sendJson(response, 400, { message: "La confirmación del respaldo no coincide" });
        return;
      }
      const jobId = request.headers["idempotency-key"] || randomUUID();
      if (
        !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
          .test(jobId)
      ) {
        sendJson(response, 400, { message: "Idempotency-Key no es válido" });
        return;
      }
      await auditLog.append({
        category: "backup",
        action: "backup.requested",
        outcome: "success",
        operator: identity.email,
        details: { jobId },
      });
      let result;
      try {
        result = await backupsClient.start({
          jobId,
          operator: identity.email,
        });
      } catch (error) {
        await auditLog.append({
          category: "backup",
          action: "backup.rejected",
          outcome: "failure",
          operator: identity.email,
          details: { jobId },
        });
        throw error;
      }
      void monitorBackup(jobId, identity.email);
      sendJson(response, result.accepted ? 202 : 200, result);
      return;
    }
    response.setHeader("Allow", "GET, HEAD, POST");
    sendJson(response, 405, { message: "Método no permitido" });
    return;
  }

  sendJson(response, 404, { message: "Recurso no encontrado" });
}

function safeStaticPath(pathname) {
  const requested = pathname === "/" ? "index.html" : pathname.replace(/^\/+/, "");
  const resolved = path.resolve(publicDirectory, requested);
  if (resolved !== publicDirectory && !resolved.startsWith(`${publicDirectory}${path.sep}`)) {
    return null;
  }
  return resolved;
}

async function serveStatic(request, response, pathname) {
  if (request.method !== "GET" && request.method !== "HEAD") {
    response.setHeader("Allow", "GET, HEAD");
    sendText(response, 405, "Método no permitido");
    return;
  }

  let filePath = safeStaticPath(pathname);
  if (!filePath) {
    sendText(response, 404, "Recurso no encontrado");
    return;
  }

  try {
    await access(filePath);
    const fileStats = await stat(filePath);
    if (!fileStats.isFile()) {
      filePath = path.join(publicDirectory, "index.html");
    }
  } catch {
    filePath = path.join(publicDirectory, "index.html");
  }

  const extension = path.extname(filePath).toLowerCase();
  response.writeHead(200, securityHeaders({
    "Content-Type": contentTypes.get(extension) || "application/octet-stream",
  }));
  if (request.method === "HEAD") {
    response.end();
    return;
  }
  createReadStream(filePath).pipe(response);
}

const server = createServer(async (request, response) => {
  try {
    const host = request.headers.host || "localhost";
    const url = new URL(request.url || "/", `http://${host}`);
    if (url.pathname === "/healthz") {
      sendText(response, 200, "ok\n");
      return;
    }
    if (url.pathname.startsWith("/api/")) {
      await handleApi(request, response, url);
      return;
    }
    await serveStatic(request, response, url.pathname);
  } catch (error) {
    if (error instanceof AuthError) {
      sendJson(response, error.status, { message: error.message });
      return;
    }
    if (error instanceof BackupExecutorError || Number.isInteger(error.status)) {
      sendJson(response, error.status || 500, { message: error.message });
      return;
    }
    console.error("Solicitud fallida", {
      method: request.method,
      url: request.url,
      error: error instanceof Error ? error.message : "Error desconocido",
    });
    sendJson(response, 500, { message: "No fue posible completar la consulta" });
  }
});

let shuttingDown = false;

async function shutdown(signal) {
  if (shuttingDown) {
    return;
  }
  shuttingDown = true;
  try {
    await auditLog.append({
      category: "system",
      action: "panel.stopped",
      outcome: "success",
      operator: "system",
      details: { signal },
    });
  } catch (error) {
    console.error("No fue posible registrar la detención del panel", {
      error: error instanceof Error ? error.message : "Error desconocido",
    });
  }
  server.close(() => process.exit(0));
}

async function start() {
  await auditLog.initialize();
  await auditLog.append({
    category: "system",
    action: "panel.started",
    outcome: "success",
    operator: "system",
    details: {
      authentication: authConfig.mode,
      mode: backupsConfig.enabled ? "controlled-operations" : "read-only",
    },
  });
  server.listen(port, bindAddress, () => {
    console.log(
      `Kontora Ops Panel escuchando en ${bindAddress}:${port} (${authConfig.mode}, operaciones controladas)`,
    );
  });
}

process.on("SIGINT", () => void shutdown("SIGINT"));
process.on("SIGTERM", () => void shutdown("SIGTERM"));

start().catch((error) => {
  console.error("No fue posible iniciar el panel", {
    error: error instanceof Error ? error.message : "Error desconocido",
  });
  process.exitCode = 1;
});
