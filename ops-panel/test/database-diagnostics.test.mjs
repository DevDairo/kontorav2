import assert from "node:assert/strict";
import test from "node:test";
import {
  collectDatabaseDiagnostics,
  loadDatabaseDiagnosticsConfig,
} from "../server/database-diagnostics.mjs";

function fakeClient({ readOnly = "on" } = {}) {
  const statements = [];
  return {
    statements,
    async connect() {},
    async end() {},
    async query(text, values = []) {
      statements.push({ text, values });
      if (/current_database\(\)/.test(text)) {
        return {
          rows: [{
            database_name: "kontora_pos",
            reader_name: "kontora_ops_reader",
            server_version: "16.6",
            read_only: readOnly,
          }],
        };
      }
      if (/flyway_snapshot/.test(text)) {
        return {
          rows: [
            {
              installed_rank: 1,
              version: "1",
              description: "schema inicial kontora pos",
              installed_on: "2026-07-27T12:00:00Z",
              execution_time: 120,
              success: true,
            },
            {
              installed_rank: 2,
              version: "2",
              description: "una caja diaria abierta",
              installed_on: "2026-07-27T12:00:01Z",
              execution_time: 15,
              success: true,
            },
          ],
        };
      }
      if (/bucket_snapshot/.test(text)) {
        return {
          rows: [{
            id: "kontoraimagenes",
            name: "kontoraimagenes",
            public: false,
            file_size_limit: "13631488",
            allowed_mime_types: ["image/*", "application/pdf"],
            object_count: "0",
          }],
        };
      }
      if (/evidence_snapshot/.test(text)) {
        return {
          rows: [{
            reference_total: "0",
            invalid_reference_total: "0",
            missing_object_total: "0",
            unreferenced_object_total: "0",
          }],
        };
      }
      return { rows: [] };
    },
  };
}

test("diagnóstico interno usa una sesión de solo lectura y resume Flyway y Storage", async () => {
  const config = loadDatabaseDiagnosticsConfig({
    OPS_DB_DIAGNOSTICS_ENABLED: "true",
    OPS_DB_PASSWORD: "credencial-de-prueba",
  });
  const client = fakeClient();
  const result = await collectDatabaseDiagnostics(config, {
    clientFactory: async () => client,
  });

  assert.equal(result.summary.overall, "operational");
  assert.equal(result.database.reader, "kontora_ops_reader");
  assert.equal(result.flyway.installed, 2);
  assert.equal(result.flyway.latest.version, "2");
  assert.equal(result.bucket.present, true);
  assert.equal(result.bucket.public, false);
  assert.equal(result.evidence.consistent, true);
  assert.equal(
    client.statements.filter(({ text }) => text === "BEGIN TRANSACTION READ ONLY").length,
    4,
  );
  assert.equal(
    client.statements.some(({ text }) => /\b(INSERT|UPDATE|DELETE|TRUNCATE|DROP)\b/i.test(text)),
    false,
  );
});

test("diagnóstico interno se degrada sin filtrar el error de conexión", async () => {
  const config = loadDatabaseDiagnosticsConfig({
    OPS_DB_DIAGNOSTICS_ENABLED: "true",
    OPS_DB_PASSWORD: "credencial-de-prueba",
  });
  const result = await collectDatabaseDiagnostics(config, {
    clientFactory: async () => ({
      async connect() {
        throw new Error("password=secreto no debe exponerse");
      },
      async end() {},
    }),
  });

  assert.equal(result.summary.overall, "attention");
  assert.equal(result.database.reachable, false);
  assert.equal(JSON.stringify(result).includes("secreto"), false);
});

test("diagnóstico interno permanece deshabilitado sin credencial", async () => {
  const config = loadDatabaseDiagnosticsConfig({});
  const result = await collectDatabaseDiagnostics(config);

  assert.equal(result.enabled, false);
  assert.equal(result.summary.overall, "disabled");
});
