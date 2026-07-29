import assert from "node:assert/strict";
import test from "node:test";
import {
  BackupExecutorError,
  createBackupsClient,
  loadBackupsConfig,
} from "../server/backups.mjs";

const TOKEN = "x".repeat(43);

test("cliente de respaldos usa únicamente la URL y credencial configuradas", async () => {
  const calls = [];
  const config = loadBackupsConfig({
    OPS_BACKUPS_ENABLED: "true",
    OPS_EXECUTOR_URL: "http://ops-executor:8091/",
    OPS_EXECUTOR_TOKEN: TOKEN,
    OPS_EXECUTOR_TIMEOUT_MS: "5000",
  });
  const client = createBackupsClient(config, {
    fetchImpl: async (url, options) => {
      calls.push({ url, options });
      return new Response(JSON.stringify({
        accepted: true,
        backup: { id: "job", state: "running" },
      }), {
        status: 202,
        headers: { "Content-Type": "application/json" },
      });
    },
  });

  await client.start({
    jobId: "d58c0e26-c790-4d0f-b9f8-4fe209e86c12",
    operator: "operador-local",
  });
  assert.equal(calls[0].url, "http://ops-executor:8091/v1/backups");
  assert.equal(calls[0].options.method, "POST");
  assert.equal(calls[0].options.headers.Authorization, `Bearer ${TOKEN}`);
  assert.deepEqual(JSON.parse(calls[0].options.body), {
    jobId: "d58c0e26-c790-4d0f-b9f8-4fe209e86c12",
    operator: "operador-local",
  });
});

test("módulo deshabilitado no intenta contactar el ejecutor", async () => {
  const config = loadBackupsConfig({
    OPS_BACKUPS_ENABLED: "false",
  });
  const client = createBackupsClient(config, {
    fetchImpl: async () => {
      throw new Error("no debe llamarse");
    },
  });
  await assert.rejects(
    client.list(),
    (error) => error instanceof BackupExecutorError && error.status === 503,
  );
});

test("un conflicto del ejecutor se conserva sin exponer su respuesta", async () => {
  const config = loadBackupsConfig({
    OPS_BACKUPS_ENABLED: "true",
    OPS_EXECUTOR_TOKEN: TOKEN,
  });
  const client = createBackupsClient(config, {
    fetchImpl: async () => new Response(
      JSON.stringify({ message: "detalle interno" }),
      { status: 409, headers: { "Content-Type": "application/json" } },
    ),
  });
  await assert.rejects(
    client.list(),
    (error) =>
      error instanceof BackupExecutorError
      && error.status === 409
      && !error.message.includes("detalle interno"),
  );
});

