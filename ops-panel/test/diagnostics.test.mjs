import assert from "node:assert/strict";
import test from "node:test";
import {
  collectDiagnostics,
  loadDiagnosticsConfig,
} from "../server/diagnostics.mjs";

function jsonResponse(body, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  };
}

test("resume contenedores esperados y volúmenes sin exponer otros recursos", async () => {
  const config = loadDiagnosticsConfig({
    OPS_ENVIRONMENT: "test",
    OPS_DOCKER_API_URL: "http://docker-proxy:2375",
  });

  const fetchImpl = async (url) => {
    if (url.endsWith("/version")) {
      return jsonResponse({
        Version: "28.0.0",
        ApiVersion: "1.51",
        Os: "linux",
        Arch: "amd64",
      });
    }
    if (url.includes("/containers/json")) {
      return jsonResponse([
        {
          Id: "postgres-id",
          Names: ["/kontora_pos_postgres_local"],
          State: "running",
          Status: "Up 2 minutes (healthy)",
          Image: "postgres:16-alpine",
        },
        {
          Id: "otro-id",
          Names: ["/contenedor_fuera_de_alcance"],
          State: "running",
          Status: "Up 1 hour",
          Image: "other:latest",
        },
      ]);
    }
    if (url.includes("/containers/postgres-id/json")) {
      return jsonResponse({
        State: {
          Status: "running",
          StartedAt: "2026-07-27T12:00:00Z",
          Health: { Status: "healthy" },
        },
        RestartCount: 0,
      });
    }
    if (url.includes("/volumes/kontora_pos_postgres_local_data")) {
      return jsonResponse({
        Name: "kontora_pos_postgres_local_data",
        Driver: "local",
        Scope: "local",
      });
    }
    return jsonResponse({}, 404);
  };

  const result = await collectDiagnostics(config, {
    fetchImpl,
    now: () => new Date("2026-07-27T12:05:00Z"),
  });

  assert.equal(result.engine.reachable, true);
  assert.equal(result.services.find((service) => service.id === "postgres").state, "healthy");
  assert.equal(result.services.some((service) => service.containerName === "contenedor_fuera_de_alcance"), false);
  assert.equal(result.volumes.find((volume) => volume.id === "postgres-data").present, true);
  assert.equal(result.volumes.find((volume) => volume.id === "ops-audit").present, false);
  assert.equal(result.summary.overall, "attention");
});

test("degrada de forma segura cuando Docker no está disponible", async () => {
  const config = loadDiagnosticsConfig({
    OPS_DOCKER_API_URL: "http://docker-proxy:2375",
  });
  const result = await collectDiagnostics(config, {
    fetchImpl: async () => {
      throw new Error("sin conexión");
    },
  });

  assert.equal(result.engine.reachable, false);
  assert.equal(result.summary.overall, "attention");
  assert.equal(result.services.every((service) => service.state === "missing"), true);
});
