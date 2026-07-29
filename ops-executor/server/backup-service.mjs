import { createHash, randomUUID } from "node:crypto";
import {
  access,
  mkdir,
  open,
  readFile,
  readdir,
  rename,
  stat,
  unlink,
  writeFile,
} from "node:fs/promises";
import path from "node:path";
import { spawn } from "node:child_process";
import { DockerApi } from "./docker-api.mjs";

const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const RESOURCE_PATTERN = /^[A-Za-z0-9][A-Za-z0-9_.-]{0,127}$/;
const RELEASE_PATTERN = /^[A-Za-z0-9][A-Za-z0-9._+-]{0,127}$/;
const COMPOSE_FILE_PATTERN = /^[A-Za-z0-9][A-Za-z0-9_./-]{0,199}\.ya?ml$/;
const TERMINAL_STATES = new Set(["success", "failure", "interrupted"]);

function envValue(env, key, fallback = "") {
  return env[key]?.trim() || fallback;
}

function positiveInteger(value, name) {
  const parsed = Number.parseInt(value, 10);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(`${name} debe ser un entero positivo`);
  }
  return parsed;
}

function resourceName(value, name) {
  if (!RESOURCE_PATTERN.test(value)) {
    throw new Error(`${name} no es un nombre de recurso válido`);
  }
  return value;
}

function releaseName(value) {
  if (!RELEASE_PATTERN.test(value)) {
    throw new Error("OPS_RELEASE_VERSION no tiene un formato permitido");
  }
  return value;
}

function composeFile(value) {
  if (
    !COMPOSE_FILE_PATTERN.test(value)
    || value.includes("..")
    || path.isAbsolute(value)
  ) {
    throw new Error("OPS_POS_COMPOSE_FILES contiene una ruta no permitida");
  }
  return value;
}

function safeRoot(value, name) {
  const resolved = path.resolve(value);
  if (resolved === path.parse(resolved).root) {
    throw new Error(`${name} no puede apuntar a la raíz del sistema`);
  }
  return resolved;
}

export function loadBackupConfig(env = process.env) {
  const backupRoot = safeRoot(
    envValue(env, "OPS_BACKUP_ROOT", "/var/lib/kontora-ops/backups"),
    "OPS_BACKUP_ROOT",
  );
  const storageSource = safeRoot(
    envValue(env, "OPS_STORAGE_SOURCE", "/source/storage"),
    "OPS_STORAGE_SOURCE",
  );
  if (backupRoot === storageSource) {
    throw new Error("El origen de Storage y el destino de respaldos deben ser distintos");
  }
  const environment = envValue(env, "OPS_ENVIRONMENT", "local");
  const releaseVersion = releaseName(envValue(
    env,
    "OPS_RELEASE_VERSION",
    "local-working-tree",
  ));
  if (environment === "production" && releaseVersion === "local-working-tree") {
    throw new Error(
      "OPS_RELEASE_VERSION debe identificar el despliegue en producción",
    );
  }
  return {
    environment,
    releaseVersion,
    composeFiles: envValue(env, "OPS_POS_COMPOSE_FILES", "infra/compose.local.yml")
      .split(",")
      .map((item) => item.trim())
      .filter(Boolean)
      .map(composeFile),
    backupRoot,
    storageSource,
    bucket: resourceName(
      envValue(env, "OPS_STORAGE_BUCKET", "kontoraimagenes"),
      "OPS_STORAGE_BUCKET",
    ),
    postgresVolume: resourceName(
      envValue(env, "OPS_POSTGRES_VOLUME", "kontora_pos_postgres_local_data"),
      "OPS_POSTGRES_VOLUME",
    ),
    storageVolume: resourceName(
      envValue(env, "OPS_STORAGE_VOLUME", "kontora_pos_storage_local_data"),
      "OPS_STORAGE_VOLUME",
    ),
    containers: {
      postgres: resourceName(
        envValue(env, "OPS_POSTGRES_CONTAINER", "kontora_pos_postgres_local"),
        "OPS_POSTGRES_CONTAINER",
      ),
      storage: resourceName(
        envValue(env, "OPS_STORAGE_CONTAINER", "kontora_pos_storage_local"),
        "OPS_STORAGE_CONTAINER",
      ),
      backend: resourceName(
        envValue(env, "OPS_BACKEND_CONTAINER", "kontora_pos_backend_local"),
        "OPS_BACKEND_CONTAINER",
      ),
      frontend: resourceName(
        envValue(env, "OPS_FRONTEND_CONTAINER", "kontora_pos_frontend_local"),
        "OPS_FRONTEND_CONTAINER",
      ),
      cloudflared: resourceName(
        envValue(env, "OPS_CLOUDFLARED_CONTAINER", "kontora_pos_cloudflared_local"),
        "OPS_CLOUDFLARED_CONTAINER",
      ),
    },
    timeoutMs: positiveInteger(
      envValue(env, "OPS_BACKUP_TIMEOUT_MS", "300000"),
      "OPS_BACKUP_TIMEOUT_MS",
    ),
  };
}

async function sha256File(filePath) {
  const hash = createHash("sha256");
  const handle = await open(filePath, "r");
  try {
    for await (const chunk of handle.createReadStream()) {
      hash.update(chunk);
    }
  } finally {
    await handle.close();
  }
  return hash.digest("hex");
}

async function fileDescriptor(filePath, name) {
  const fileStats = await stat(filePath);
  return {
    name,
    bytes: fileStats.size,
    sha256: await sha256File(filePath),
  };
}

async function writeJsonAtomic(filePath, value) {
  const temporaryPath = `${filePath}.${randomUUID()}.tmp`;
  const payload = `${JSON.stringify(value, null, 2)}\n`;
  const handle = await open(temporaryPath, "wx", 0o600);
  try {
    await handle.writeFile(payload, "utf8");
    await handle.sync();
  } finally {
    await handle.close();
  }
  try {
    await rename(temporaryPath, filePath);
  } catch (error) {
    if (!["EEXIST", "EPERM"].includes(error.code)) {
      throw error;
    }
    await unlink(filePath).catch((unlinkError) => {
      if (unlinkError.code !== "ENOENT") {
        throw unlinkError;
      }
    });
    await rename(temporaryPath, filePath);
  } finally {
    await unlink(temporaryPath).catch(() => {});
  }
}

function run(command, args, { timeoutMs }) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      stdio: ["ignore", "ignore", "pipe"],
    });
    const errors = [];
    let errorBytes = 0;
    child.stderr.on("data", (chunk) => {
      if (errorBytes < 32_768) {
        errors.push(chunk);
        errorBytes += chunk.length;
      }
    });
    const timer = setTimeout(() => {
      child.kill("SIGKILL");
    }, timeoutMs);
    child.on("error", (error) => {
      clearTimeout(timer);
      reject(error);
    });
    child.on("exit", (code, signal) => {
      clearTimeout(timer);
      if (code === 0) {
        resolve();
        return;
      }
      const detail = Buffer.concat(errors).toString("utf8").trim();
      reject(new Error(
        `${command} terminó con ${signal || `código ${code}`}${detail ? `: ${detail}` : ""}`,
      ));
    });
  });
}

async function defaultArchiveStorage({ source, destination, timeoutMs }) {
  await run("tar", [
    "--create",
    "--gzip",
    "--file",
    destination,
    "--xattrs",
    "--xattrs-include=user.supabase.*",
    "--numeric-owner",
    "--directory",
    source,
    ".",
  ], { timeoutMs });
  await run("tar", [
    "--list",
    "--gzip",
    "--file",
    destination,
  ], { timeoutMs });
}

async function storageSample(root) {
  const queue = [root];
  let files = 0;
  let bytes = 0;
  let sample = null;
  while (queue.length > 0) {
    const directory = queue.shift();
    const entries = await readdir(directory, { withFileTypes: true });
    entries.sort((left, right) => left.name.localeCompare(right.name));
    for (const entry of entries) {
      const absolute = path.join(directory, entry.name);
      if (entry.isDirectory()) {
        queue.push(absolute);
      } else if (entry.isFile()) {
        const fileStats = await stat(absolute);
        files += 1;
        bytes += fileStats.size;
        if (!sample) {
          sample = {
            relativePath: path.relative(root, absolute).split(path.sep).join("/"),
            bytes: fileStats.size,
            sha256: await sha256File(absolute),
          };
        }
      }
    }
  }
  return { files, bytes, sample };
}

function publicBackup(job) {
  const files = Array.isArray(job.manifest?.files) ? job.manifest.files : [];
  return {
    id: job.id,
    state: job.state,
    startedAt: job.startedAt,
    completedAt: job.completedAt || null,
    operator: job.operator,
    error: job.state === "failure" || job.state === "interrupted"
      ? "El respaldo no pudo completarse; revise los logs del ejecutor"
      : null,
    files: files.map((file) => ({
      name: file.name,
      bytes: file.bytes,
      sha256: file.sha256,
    })),
    externalCopy: job.manifest?.externalCopy || { state: "pending" },
    restoreVerification: job.manifest?.restoreVerification || { state: "pending" },
  };
}

export class BackupService {
  #config;
  #docker;
  #archiveStorage;
  #now;
  #logger;
  #activeJob = null;

  constructor(
    config,
    {
      docker = new DockerApi({ timeoutMs: config.timeoutMs }),
      archiveStorage = defaultArchiveStorage,
      now = () => new Date(),
      logger = console,
    } = {},
  ) {
    this.#config = config;
    this.#docker = docker;
    this.#archiveStorage = archiveStorage;
    this.#now = now;
    this.#logger = logger;
  }

  async initialize() {
    await mkdir(this.#config.backupRoot, { recursive: true, mode: 0o700 });
    await access(this.#config.storageSource);
    await this.#docker.ping();
    const lockPath = path.join(this.#config.backupRoot, ".backup.lock");
    try {
      const jobId = (await readFile(lockPath, "utf8")).trim();
      if (UUID_PATTERN.test(jobId)) {
        const jobPath = path.join(this.#config.backupRoot, jobId, "job.json");
        try {
          const job = JSON.parse(await readFile(jobPath, "utf8"));
          if (!TERMINAL_STATES.has(job.state)) {
            await this.#restoreInitialServices(job.initialServices || {});
            await writeJsonAtomic(jobPath, {
              ...job,
              state: "interrupted",
              completedAt: this.#now().toISOString(),
              error: "El ejecutor se reinició mientras el respaldo estaba activo",
            });
          }
        } catch (error) {
          if (error.code !== "ENOENT") {
            throw error;
          }
          // El directorio pudo no haberse creado antes de la interrupción.
        }
      }
      await unlink(lockPath);
    } catch (error) {
      if (error.code !== "ENOENT") {
        throw error;
      }
    }
  }

  status() {
    return {
      ready: true,
      activeJob: this.#activeJob,
      storage: "volume",
    };
  }

  async waitForIdle(timeoutMs = this.#config.timeoutMs) {
    const deadline = Date.now() + timeoutMs;
    while (this.#activeJob && Date.now() < deadline) {
      await new Promise((resolve) => setTimeout(resolve, 500));
    }
    return this.#activeJob === null;
  }

  async list() {
    const entries = await readdir(this.#config.backupRoot, { withFileTypes: true });
    const jobs = [];
    for (const entry of entries) {
      if (!entry.isDirectory() || !UUID_PATTERN.test(entry.name)) {
        continue;
      }
      try {
        const job = JSON.parse(await readFile(
          path.join(this.#config.backupRoot, entry.name, "job.json"),
          "utf8",
        ));
        jobs.push(publicBackup(job));
      } catch {
        if (entry.name === this.#activeJob) {
          jobs.push({
            id: entry.name,
            state: "running",
            startedAt: null,
            completedAt: null,
            operator: "system",
            error: null,
            files: [],
            externalCopy: { state: "pending" },
            restoreVerification: { state: "pending" },
          });
          continue;
        }
        jobs.push({
          id: entry.name,
          state: "failure",
          startedAt: null,
          completedAt: null,
          operator: "system",
          error: "Metadatos del respaldo no disponibles",
          files: [],
          externalCopy: { state: "pending" },
          restoreVerification: { state: "pending" },
        });
      }
    }
    jobs.sort((left, right) =>
      String(right.startedAt || "").localeCompare(String(left.startedAt || "")));
    return {
      generatedAt: this.#now().toISOString(),
      activeJob: this.#activeJob,
      backups: jobs,
    };
  }

  async start({ jobId = randomUUID(), operator }) {
    if (!UUID_PATTERN.test(jobId)) {
      throw new Error("El identificador idempotente no es válido");
    }
    const existingPath = path.join(this.#config.backupRoot, jobId, "job.json");
    try {
      const existing = JSON.parse(await readFile(existingPath, "utf8"));
      return { accepted: false, idempotent: true, backup: publicBackup(existing) };
    } catch (error) {
      if (error.code !== "ENOENT") {
        throw error;
      }
    }
    if (this.#activeJob) {
      const conflict = new Error(`Ya existe un respaldo activo: ${this.#activeJob}`);
      conflict.code = "BACKUP_ACTIVE";
      throw conflict;
    }
    const lockPath = path.join(this.#config.backupRoot, ".backup.lock");
    let lock;
    try {
      lock = await open(lockPath, "wx", 0o600);
      await lock.writeFile(`${jobId}\n`, "utf8");
      await lock.sync();
    } catch (error) {
      if (error.code === "EEXIST") {
        const conflict = new Error("Ya existe otro trabajo de respaldo");
        conflict.code = "BACKUP_ACTIVE";
        throw conflict;
      }
      throw error;
    } finally {
      await lock?.close();
    }
    this.#activeJob = jobId;
    void this.#run({ jobId, operator, lockPath }).catch((error) => {
      this.#logger.error("El respaldo terminó con error", {
        jobId,
        error: error instanceof Error ? error.message : "Error desconocido",
      });
    });
    return {
      accepted: true,
      idempotent: false,
      backup: {
        id: jobId,
        state: "running",
        startedAt: this.#now().toISOString(),
        operator,
      },
    };
  }

  async #run({ jobId, operator, lockPath }) {
    const jobDirectory = path.join(this.#config.backupRoot, jobId);
    const jobPath = path.join(jobDirectory, "job.json");
    const startedAt = this.#now().toISOString();
    const safeOperator = String(operator || "unknown").slice(0, 200);
    const initial = {};
    let job = {
      schemaVersion: 1,
      id: jobId,
      state: "running",
      startedAt,
      completedAt: null,
      operator: safeOperator,
      error: null,
      manifest: null,
      initialServices: null,
    };
    await mkdir(jobDirectory, { mode: 0o700 });
    await writeJsonAtomic(jobPath, job);
    try {
      const inspected = {};
      for (const [id, containerName] of Object.entries(this.#config.containers)) {
        try {
          inspected[id] = await this.#docker.inspectContainer(containerName);
          initial[id] = inspected[id].State?.Running === true;
        } catch (error) {
          if (id === "cloudflared" && /404/.test(error.message)) {
            initial[id] = false;
            continue;
          }
          throw error;
        }
      }
      for (const id of ["postgres", "storage", "backend", "frontend"]) {
        if (!initial[id]) {
          throw new Error(`El servicio requerido ${id} no está en ejecución`);
        }
      }
      job = {
        ...job,
        initialServices: initial,
      };
      await writeJsonAtomic(jobPath, job);

      for (const id of ["cloudflared", "frontend", "backend", "storage"]) {
        if (initial[id]) {
          await this.#docker.stopContainer(this.#config.containers[id]);
          await this.#docker.waitForContainer(
            this.#config.containers[id],
            (container) => container.State?.Running === false,
          );
        }
      }

      const dumpPath = path.join(jobDirectory, "kontora_pos.dump");
      const snapshotSql = `
SELECT json_build_object(
  'database', json_build_object(
    'name', current_database(),
    'version', current_setting('server_version')
  ),
  'flyway', COALESCE(
    (
      SELECT json_agg(item ORDER BY installed_rank)
      FROM (
        SELECT
          installed_rank,
          json_build_object(
            'installedRank', installed_rank,
            'version', version,
            'description', description,
            'installedOn', installed_on,
            'executionTimeMs', execution_time,
            'success', success
          ) AS item
        FROM public.flyway_schema_history
        WHERE success = TRUE
      ) AS migrations
    ),
    '[]'::json
  ),
  'bucket', (
    SELECT json_build_object(
      'id', bucket.id,
      'name', bucket.name,
      'public', bucket.public,
      'fileSizeLimit', bucket.file_size_limit,
      'allowedMimeTypes', bucket.allowed_mime_types,
      'objectCount', (
        SELECT count(*)
        FROM storage.objects AS stored_object
        WHERE stored_object.bucket_id = bucket.id
      )
    )
    FROM storage.buckets AS bucket
    WHERE bucket.id = '${this.#config.bucket}'
  ),
  'evidence', json_build_object(
    'referenceTotal', (SELECT count(*) FROM public.archivos_evidencia)
  )
)::text;
`.trim();
      const snapshotResult = await this.#docker.execCapture(
        this.#config.containers.postgres,
        [
          "sh",
          "-ceu",
          "exec psql -X -A -t --username=\"$POSTGRES_USER\" --dbname=\"$POSTGRES_DB\" --command=\"$1\"",
          "kontora-backup-snapshot",
          snapshotSql,
        ],
      );
      const databaseSnapshot = JSON.parse(snapshotResult.stdout.trim());
      if (!databaseSnapshot.bucket) {
        throw new Error(`El bucket ${this.#config.bucket} no existe`);
      }

      await this.#docker.execToFile(this.#config.containers.postgres, [
        "sh",
        "-ceu",
        "exec pg_dump --format=custom --no-owner --no-privileges --schema=public --schema=storage --username=\"$POSTGRES_USER\" --dbname=\"$POSTGRES_DB\"",
      ], dumpPath);

      await this.#docker.stopContainer(this.#config.containers.postgres);
      await this.#docker.waitForContainer(
        this.#config.containers.postgres,
        (container) => container.State?.Running === false,
      );

      const storageSnapshot = await storageSample(this.#config.storageSource);
      const storageArchivePath = path.join(jobDirectory, "kontora_storage.tar.gz");
      await this.#archiveStorage({
        source: this.#config.storageSource,
        destination: storageArchivePath,
        timeoutMs: this.#config.timeoutMs,
      });

      const files = [
        await fileDescriptor(dumpPath, "kontora_pos.dump"),
        await fileDescriptor(storageArchivePath, "kontora_storage.tar.gz"),
      ];

      await this.#restoreInitialServices(initial);

      const completedAt = this.#now().toISOString();
      const manifest = {
        schemaVersion: 1,
        id: jobId,
        state: "verified-local",
        environment: this.#config.environment,
        releaseVersion: this.#config.releaseVersion,
        startedAt,
        completedAt,
        operator: safeOperator,
        composeFiles: this.#config.composeFiles,
        containers: Object.fromEntries(
          Object.entries(inspected).map(([id, container]) => [
            id,
            {
              name: this.#config.containers[id],
              image: container?.Config?.Image || null,
              imageId: container?.Image || null,
            },
          ]),
        ),
        volumes: {
          postgres: this.#config.postgresVolume,
          storage: this.#config.storageVolume,
        },
        bucket: {
          ...databaseSnapshot.bucket,
          physicalFiles: storageSnapshot.files,
          physicalBytes: storageSnapshot.bytes,
          knownObject: storageSnapshot.sample,
        },
        database: databaseSnapshot.database,
        flyway: databaseSnapshot.flyway,
        evidence: databaseSnapshot.evidence,
        files,
        externalCopy: { state: "pending", verifiedAt: null },
        restoreVerification: { state: "pending", verifiedAt: null },
      };
      const manifestPath = path.join(jobDirectory, "manifest.json");
      await writeJsonAtomic(manifestPath, manifest);
      const manifestHash = await sha256File(manifestPath);
      await writeFile(
        path.join(jobDirectory, "manifest.sha256"),
        `${manifestHash}  manifest.json\n`,
        { encoding: "utf8", mode: 0o600, flag: "wx" },
      );
      job = {
        ...job,
        state: "success",
        completedAt,
        manifest,
        initialServices: null,
      };
      await writeJsonAtomic(jobPath, job);
    } catch (error) {
      try {
        await this.#restoreInitialServices(initial);
      } catch (recoveryError) {
        this.#logger.error("No fue posible recuperar todos los servicios", {
          jobId,
          error: recoveryError instanceof Error
            ? recoveryError.message
            : "Error desconocido",
        });
      }
      job = {
        ...job,
        state: "failure",
        completedAt: this.#now().toISOString(),
        error: error instanceof Error ? error.message.slice(0, 500) : "Error desconocido",
      };
      await writeJsonAtomic(jobPath, job);
      throw error;
    } finally {
      this.#activeJob = null;
      await unlink(lockPath).catch(() => {});
    }
  }

  async #restoreInitialServices(initial) {
    for (const id of ["postgres", "storage", "backend", "frontend", "cloudflared"]) {
      if (!initial[id]) {
        continue;
      }
      const containerName = this.#config.containers[id];
      const current = await this.#docker.inspectContainer(containerName);
      if (!current.State?.Running) {
        await this.#docker.startContainer(containerName);
      }
      await this.#docker.waitForContainer(
        containerName,
        (container) => {
          if (!container.State?.Running) {
            return false;
          }
          const health = container.State?.Health?.Status;
          return !health || health === "healthy";
        },
      );
    }
  }
}

export function createBackupService(
  config = loadBackupConfig(),
  dependencies,
) {
  return new BackupService(config, dependencies);
}
