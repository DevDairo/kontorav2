import { timingSafeEqual } from "node:crypto";
import { createServer } from "node:http";
import { createBackupService, loadBackupConfig } from "./backup-service.mjs";

const MAX_BODY_BYTES = 16_384;
const port = Number.parseInt(process.env.OPS_EXECUTOR_PORT || "8091", 10);
const bindAddress = process.env.OPS_EXECUTOR_BIND_ADDRESS || "0.0.0.0";
const expectedToken = process.env.OPS_EXECUTOR_TOKEN?.trim() || "";
if (expectedToken.length < 32) {
  throw new Error("OPS_EXECUTOR_TOKEN debe contener al menos 32 caracteres");
}

const backupService = createBackupService(loadBackupConfig());

function headers(extra = {}) {
  return {
    "Cache-Control": "no-store",
    "Content-Type": "application/json; charset=utf-8",
    "X-Content-Type-Options": "nosniff",
    ...extra,
  };
}

function sendJson(response, status, body) {
  response.writeHead(status, headers());
  response.end(JSON.stringify(body));
}

function authorized(request) {
  const authorization = request.headers.authorization || "";
  const token = authorization.startsWith("Bearer ")
    ? authorization.slice("Bearer ".length)
    : "";
  const received = Buffer.from(token);
  const expected = Buffer.from(expectedToken);
  return received.length === expected.length && timingSafeEqual(received, expected);
}

async function readJson(request) {
  const chunks = [];
  let bytes = 0;
  for await (const chunk of request) {
    bytes += chunk.length;
    if (bytes > MAX_BODY_BYTES) {
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

const server = createServer(async (request, response) => {
  try {
    const url = new URL(request.url || "/", "http://localhost");
    if (request.method === "GET" && url.pathname === "/healthz") {
      sendJson(response, 200, {
        status: "ok",
        service: "kontora-ops-executor",
        backup: backupService.status(),
      });
      return;
    }
    if (!authorized(request)) {
      sendJson(response, 401, { message: "Credencial interna no válida" });
      return;
    }
    if (request.method === "GET" && url.pathname === "/v1/backups") {
      sendJson(response, 200, await backupService.list());
      return;
    }
    if (request.method === "POST" && url.pathname === "/v1/backups") {
      if (request.headers["content-type"] !== "application/json") {
        sendJson(response, 415, { message: "Se requiere application/json" });
        return;
      }
      const body = await readJson(request);
      const result = await backupService.start({
        jobId: body.jobId,
        operator: body.operator,
      });
      sendJson(response, result.accepted ? 202 : 200, result);
      return;
    }
    sendJson(response, 404, { message: "Operación no permitida" });
  } catch (error) {
    const status = error.code === "BACKUP_ACTIVE"
      ? 409
      : Number.isInteger(error.status)
        ? error.status
        : 500;
    console.error("Solicitud del ejecutor fallida", {
      method: request.method,
      url: request.url,
      error: error instanceof Error ? error.message : "Error desconocido",
    });
    sendJson(response, status, {
      message: status === 500
        ? "No fue posible ejecutar la operación controlada"
        : error.message,
    });
  }
});

async function start() {
  await backupService.initialize();
  server.listen(port, bindAddress, () => {
    console.log(`Kontora Ops Executor escuchando en ${bindAddress}:${port}`);
  });
}

let shuttingDown = false;

async function shutdown() {
  if (shuttingDown) {
    return;
  }
  shuttingDown = true;
  server.close();
  await backupService.waitForIdle(290_000);
  process.exit(0);
}

process.on("SIGINT", () => void shutdown());
process.on("SIGTERM", () => void shutdown());

start().catch((error) => {
  console.error("No fue posible iniciar el ejecutor", {
    error: error instanceof Error ? error.message : "Error desconocido",
  });
  process.exitCode = 1;
});
