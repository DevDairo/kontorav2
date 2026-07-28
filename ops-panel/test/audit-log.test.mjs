import assert from "node:assert/strict";
import { appendFile, mkdtemp, readFile } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { AuditLog, loadAuditConfig } from "../server/audit-log.mjs";

async function temporaryConfig() {
  const directory = await mkdtemp(path.join(os.tmpdir(), "kontora-ops-audit-"));
  return loadAuditConfig({
    OPS_ENVIRONMENT: "test",
    OPS_AUDIT_FILE: path.join(directory, "audit.jsonl"),
    OPS_AUDIT_STORAGE: "test-volume",
    OPS_AUDIT_DEFAULT_LIMIT: "100",
    OPS_AUDIT_MAX_BYTES: "1048576",
  });
}

test("bitácora persiste eventos encadenados y verifica su integridad", async () => {
  const config = await temporaryConfig();
  const auditLog = new AuditLog(config);
  await auditLog.initialize();
  await auditLog.append({
    category: "system",
    action: "panel.started",
    outcome: "success",
    operator: "system",
    details: { mode: "read-only" },
    occurredAt: new Date("2026-07-27T12:00:00Z"),
  });
  await auditLog.append({
    category: "diagnostics",
    action: "diagnostics.snapshot",
    outcome: "success",
    operator: "operador-local",
    details: { overall: "operational" },
    occurredAt: new Date("2026-07-27T12:01:00Z"),
  });

  const firstRead = await auditLog.list();
  assert.equal(firstRead.summary.integrity, "verified");
  assert.equal(firstRead.summary.totalEntries, 2);
  assert.equal(firstRead.entries[0].sequence, 2);
  assert.equal(firstRead.entries[1].previousHash, "0".repeat(64));

  const restartedAuditLog = new AuditLog(config);
  await restartedAuditLog.initialize();
  const afterRestart = await restartedAuditLog.list();
  assert.equal(afterRestart.summary.totalEntries, 2);
  assert.equal(afterRestart.summary.headHash, firstRead.summary.headHash);
});

test("bitácora redacta secretos antes de persistir", async () => {
  const config = await temporaryConfig();
  const auditLog = new AuditLog(config);
  await auditLog.initialize();
  await auditLog.append({
    category: "security",
    action: "credential.checked",
    outcome: "success",
    operator: "operador-local",
    details: {
      token: "valor-que-no-debe-persistir",
      nested: { dbPassword: "otro-secreto", safe: "visible" },
    },
  });

  const persisted = await readFile(config.filePath, "utf8");
  assert.equal(persisted.includes("valor-que-no-debe-persistir"), false);
  assert.equal(persisted.includes("otro-secreto"), false);
  assert.equal(persisted.includes("[REDACTED]"), true);
  assert.equal(persisted.includes("visible"), true);
});

test("bitácora rechaza una cadena alterada al reiniciar", async () => {
  const config = await temporaryConfig();
  const auditLog = new AuditLog(config);
  await auditLog.initialize();
  await auditLog.append({
    category: "system",
    action: "panel.started",
    outcome: "success",
    operator: "system",
  });
  await appendFile(config.filePath, '{"sequence":2,"hash":"alterado"}\n', "utf8");

  const restartedAuditLog = new AuditLog(config);
  await assert.rejects(
    restartedAuditLog.initialize(),
    /cadena de auditoría es inválida/,
  );
});
