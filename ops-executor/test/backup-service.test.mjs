import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import {
  mkdir,
  mkdtemp,
  readFile,
  writeFile,
} from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { BackupService, loadBackupConfig } from "../server/backup-service.mjs";

class FakeDocker {
  constructor() {
    this.states = {
      postgres: true,
      storage: true,
      backend: true,
      frontend: true,
      cloudflared: false,
    };
    this.nameToId = new Map([
      ["postgres-test", "postgres"],
      ["storage-test", "storage"],
      ["backend-test", "backend"],
      ["frontend-test", "frontend"],
      ["cloudflared-test", "cloudflared"],
    ]);
  }

  async ping() {}

  async inspectContainer(name) {
    const id = this.nameToId.get(name);
    if (!id) {
      throw new Error("Docker API respondió HTTP 404");
    }
    return {
      State: {
        Running: this.states[id],
        Status: this.states[id] ? "running" : "exited",
        Health: ["postgres", "storage", "frontend"].includes(id)
          ? { Status: this.states[id] ? "healthy" : "unhealthy" }
          : undefined,
      },
      Config: { Image: `${id}:test` },
      Image: `sha256:${id}`,
    };
  }

  async stopContainer(name) {
    this.states[this.nameToId.get(name)] = false;
  }

  async startContainer(name) {
    this.states[this.nameToId.get(name)] = true;
  }

  async waitForContainer(name, predicate) {
    const inspected = await this.inspectContainer(name);
    assert.equal(predicate(inspected), true);
    return inspected;
  }

  async execCapture() {
    return {
      exitCode: 0,
      stderr: "",
      stdout: JSON.stringify({
        database: { name: "kontora_pos", version: "16.14" },
        flyway: [
          {
            installedRank: 1,
            version: "1",
            description: "schema inicial",
            success: true,
          },
        ],
        bucket: {
          id: "kontoraimagenes",
          name: "kontoraimagenes",
          public: false,
          fileSizeLimit: 13_631_488,
          allowedMimeTypes: ["image/*", "application/pdf"],
          objectCount: 1,
        },
        evidence: { referenceTotal: 1 },
      }),
    };
  }

  async execToFile(_name, _command, outputPath) {
    await writeFile(outputPath, "PGDMP-test", { flag: "wx" });
    return { exitCode: 0, stderr: "" };
  }
}

async function fixture() {
  const root = await mkdtemp(path.join(os.tmpdir(), "kontora-backup-"));
  const backupRoot = path.join(root, "backups");
  const storageSource = path.join(root, "storage");
  await mkdir(storageSource);
  await writeFile(path.join(storageSource, "known-object.bin"), "evidencia");
  const config = loadBackupConfig({
    OPS_ENVIRONMENT: "test",
    OPS_RELEASE_VERSION: "test-commit",
    OPS_POS_COMPOSE_FILES: "infra/compose.local.yml",
    OPS_BACKUP_ROOT: backupRoot,
    OPS_STORAGE_SOURCE: storageSource,
    OPS_STORAGE_BUCKET: "kontoraimagenes",
    OPS_POSTGRES_VOLUME: "postgres-test-data",
    OPS_STORAGE_VOLUME: "storage-test-data",
    OPS_POSTGRES_CONTAINER: "postgres-test",
    OPS_STORAGE_CONTAINER: "storage-test",
    OPS_BACKEND_CONTAINER: "backend-test",
    OPS_FRONTEND_CONTAINER: "frontend-test",
    OPS_CLOUDFLARED_CONTAINER: "cloudflared-test",
    OPS_BACKUP_TIMEOUT_MS: "5000",
  });
  return { root, backupRoot, storageSource, config };
}

async function waitForTerminal(service) {
  for (let attempt = 0; attempt < 100; attempt += 1) {
    const list = await service.list();
    const backup = list.backups[0];
    if (backup && ["success", "failure", "interrupted"].includes(backup.state)) {
      return backup;
    }
    await new Promise((resolve) => setTimeout(resolve, 10));
  }
  throw new Error("El respaldo de prueba no terminó");
}

test("crea la pareja, el manifiesto y restaura el estado de servicios", async () => {
  const { config, backupRoot } = await fixture();
  const docker = new FakeDocker();
  const service = new BackupService(config, {
    docker,
    archiveStorage: async ({ destination }) => {
      await writeFile(destination, "tar-gzip-test", { flag: "wx" });
    },
    now: (() => {
      let second = 0;
      return () => new Date(`2026-07-28T00:00:0${second++}Z`);
    })(),
  });
  await service.initialize();
  const jobId = "d58c0e26-c790-4d0f-b9f8-4fe209e86c12";
  const started = await service.start({ jobId, operator: "operador@test.local" });
  assert.equal(started.accepted, true);

  const completed = await waitForTerminal(service);
  assert.equal(completed.state, "success", completed.error || "respaldo fallido");
  assert.equal(completed.files.length, 2);
  assert.deepEqual(docker.states, {
    postgres: true,
    storage: true,
    backend: true,
    frontend: true,
    cloudflared: false,
  });

  const jobDirectory = path.join(backupRoot, jobId);
  const manifestText = await readFile(path.join(jobDirectory, "manifest.json"), "utf8");
  const manifest = JSON.parse(manifestText);
  assert.equal(manifest.state, "verified-local");
  assert.equal(manifest.database.name, "kontora_pos");
  assert.equal(manifest.bucket.objectCount, 1);
  assert.equal(manifest.bucket.knownObject.relativePath, "known-object.bin");
  assert.equal(manifest.restoreVerification.state, "pending");
  const expectedManifestHash = createHash("sha256").update(manifestText).digest("hex");
  assert.equal(
    await readFile(path.join(jobDirectory, "manifest.sha256"), "utf8"),
    `${expectedManifestHash}  manifest.json\n`,
  );
});

test("una repetición del mismo identificador es idempotente", async () => {
  const { config } = await fixture();
  const service = new BackupService(config, {
    docker: new FakeDocker(),
    archiveStorage: async ({ destination }) => {
      await writeFile(destination, "archive", { flag: "wx" });
    },
  });
  await service.initialize();
  const jobId = "ba099595-73a1-4a09-a877-497160ce65d9";
  await service.start({ jobId, operator: "operador-local" });
  await waitForTerminal(service);
  const repeated = await service.start({ jobId, operator: "otro-operador" });
  assert.equal(repeated.accepted, false);
  assert.equal(repeated.idempotent, true);
  assert.equal(repeated.backup.operator, "operador-local");
});

test("recupera servicios y marca interrumpido un trabajo abandonado", async () => {
  const { config, backupRoot } = await fixture();
  const docker = new FakeDocker();
  Object.keys(docker.states).forEach((id) => {
    docker.states[id] = false;
  });
  const jobId = "77a98d41-3e56-4cb2-9223-0fb5c6da0d70";
  const jobDirectory = path.join(backupRoot, jobId);
  await mkdir(jobDirectory, { recursive: true });
  await writeFile(path.join(backupRoot, ".backup.lock"), `${jobId}\n`);
  await writeFile(path.join(jobDirectory, "job.json"), JSON.stringify({
    schemaVersion: 1,
    id: jobId,
    state: "running",
    startedAt: "2026-07-28T00:00:00Z",
    completedAt: null,
    operator: "operador-local",
    error: null,
    manifest: null,
    initialServices: {
      postgres: true,
      storage: true,
      backend: true,
      frontend: true,
      cloudflared: true,
    },
  }));
  const service = new BackupService(config, { docker });
  await service.initialize();

  assert.deepEqual(docker.states, {
    postgres: true,
    storage: true,
    backend: true,
    frontend: true,
    cloudflared: true,
  });
  const recovered = JSON.parse(
    await readFile(path.join(jobDirectory, "job.json"), "utf8"),
  );
  assert.equal(recovered.state, "interrupted");
  assert.match(recovered.error, /reinició/);
});

test("un fallo del archivado conserva el error y recupera los servicios", async () => {
  const { config, backupRoot } = await fixture();
  const docker = new FakeDocker();
  const service = new BackupService(config, {
    docker,
    archiveStorage: async () => {
      throw new Error("fallo controlado de tar");
    },
    logger: { error() {} },
  });
  await service.initialize();
  await service.start({
    jobId: "c00ccf75-c118-450c-b173-329af8440688",
    operator: "operador-local",
  });
  const completed = await waitForTerminal(service);
  assert.equal(completed.state, "failure");
  assert.match(completed.error, /revise los logs/);
  const internalJob = JSON.parse(await readFile(
    path.join(
      backupRoot,
      "c00ccf75-c118-450c-b173-329af8440688",
      "job.json",
    ),
    "utf8",
  ));
  assert.match(internalJob.error, /fallo controlado/);
  assert.deepEqual(docker.states, {
    postgres: true,
    storage: true,
    backend: true,
    frontend: true,
    cloudflared: false,
  });
});

test("rechaza rutas raíz y nombres de recursos no permitidos", () => {
  assert.throws(
    () => loadBackupConfig({
      OPS_BACKUP_ROOT: path.parse(process.cwd()).root,
      OPS_STORAGE_SOURCE: path.join(os.tmpdir(), "storage"),
    }),
    /raíz del sistema/,
  );
  assert.throws(
    () => loadBackupConfig({
      OPS_BACKUP_ROOT: path.join(os.tmpdir(), "backups"),
      OPS_STORAGE_SOURCE: path.join(os.tmpdir(), "storage"),
      OPS_STORAGE_VOLUME: "../otro",
    }),
    /no es un nombre de recurso válido/,
  );
  assert.throws(
    () => loadBackupConfig({
      OPS_ENVIRONMENT: "production",
      OPS_BACKUP_ROOT: path.join(os.tmpdir(), "backups"),
      OPS_STORAGE_SOURCE: path.join(os.tmpdir(), "storage"),
      OPS_RELEASE_VERSION: "local-working-tree",
    }),
    /identificar el despliegue/,
  );
  assert.throws(
    () => loadBackupConfig({
      OPS_BACKUP_ROOT: path.join(os.tmpdir(), "backups"),
      OPS_STORAGE_SOURCE: path.join(os.tmpdir(), "storage"),
      OPS_POS_COMPOSE_FILES: "../infra/compose.local.yml",
    }),
    /ruta no permitida/,
  );
});
