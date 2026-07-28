import { createHash, randomUUID } from "node:crypto";
import path from "node:path";
import {
  chmod,
  mkdir,
  open,
  readFile,
  stat,
} from "node:fs/promises";

const GENESIS_HASH = "0".repeat(64);
const DEFAULT_MAX_ENTRIES = 100;
const DEFAULT_MAX_BYTES = 52_428_800;
const REDACTED = "[REDACTED]";
const SECRET_KEY_PATTERN =
  /(authorization|cookie|credential|password|secret|token|api[-_]?key|private[-_]?key)/i;

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

export function loadAuditConfig(env = process.env) {
  const filePath = path.resolve(
    envValue(env, "OPS_AUDIT_FILE", "/tmp/kontora-ops/audit.jsonl"),
  );
  return {
    filePath,
    environment: envValue(env, "OPS_ENVIRONMENT", "local"),
    storage: envValue(env, "OPS_AUDIT_STORAGE", "temporary"),
    defaultLimit: positiveInteger(
      envValue(env, "OPS_AUDIT_DEFAULT_LIMIT", `${DEFAULT_MAX_ENTRIES}`),
      "OPS_AUDIT_DEFAULT_LIMIT",
    ),
    maxBytes: positiveInteger(
      envValue(env, "OPS_AUDIT_MAX_BYTES", `${DEFAULT_MAX_BYTES}`),
      "OPS_AUDIT_MAX_BYTES",
    ),
  };
}

function sha256(value) {
  return createHash("sha256").update(value, "utf8").digest("hex");
}

function limitedString(value, maximum = 500) {
  const text = String(value);
  return text.length <= maximum ? text : `${text.slice(0, maximum)}…`;
}

function sanitizeValue(value, key = "", depth = 0) {
  if (SECRET_KEY_PATTERN.test(key)) {
    return REDACTED;
  }
  if (value === null || typeof value === "boolean" || typeof value === "number") {
    return value;
  }
  if (typeof value === "string") {
    return limitedString(value);
  }
  if (depth >= 4) {
    return "[MAX_DEPTH]";
  }
  if (Array.isArray(value)) {
    return value.slice(0, 50).map((item) => sanitizeValue(item, "", depth + 1));
  }
  if (typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value)
        .slice(0, 50)
        .map(([entryKey, entryValue]) => [
          limitedString(entryKey, 80),
          sanitizeValue(entryValue, entryKey, depth + 1),
        ]),
    );
  }
  return limitedString(value);
}

function eventHash(eventWithoutHash) {
  return sha256(JSON.stringify(eventWithoutHash));
}

function parseAndVerify(content) {
  const lines = content.split(/\r?\n/).filter((line) => line.trim());
  const entries = [];
  let previousHash = GENESIS_HASH;
  let expectedSequence = 1;

  for (let index = 0; index < lines.length; index += 1) {
    let entry;
    try {
      entry = JSON.parse(lines[index]);
    } catch {
      return {
        valid: false,
        invalidLine: index + 1,
        reason: "json-invalid",
        entries,
        headHash: previousHash,
      };
    }
    const { hash, ...eventWithoutHash } = entry;
    const valid =
      entry.sequence === expectedSequence
      && entry.previousHash === previousHash
      && typeof hash === "string"
      && hash === eventHash(eventWithoutHash);
    if (!valid) {
      return {
        valid: false,
        invalidLine: index + 1,
        reason: "hash-chain-invalid",
        entries,
        headHash: previousHash,
      };
    }
    entries.push(entry);
    previousHash = hash;
    expectedSequence += 1;
  }

  return {
    valid: true,
    invalidLine: null,
    reason: null,
    entries,
    headHash: previousHash,
  };
}

export class AuditLog {
  #config;
  #ready = false;
  #headHash = GENESIS_HASH;
  #sequence = 0;
  #queue = Promise.resolve();

  constructor(config) {
    this.#config = config;
  }

  async initialize() {
    await mkdir(path.dirname(this.#config.filePath), { recursive: true, mode: 0o700 });
    const handle = await open(this.#config.filePath, "a", 0o600);
    await handle.close();
    await chmod(this.#config.filePath, 0o600).catch(() => {});
    const verification = await this.#readVerified();
    if (!verification.valid) {
      throw new Error(
        `La cadena de auditoría es inválida en la línea ${verification.invalidLine}`,
      );
    }
    this.#headHash = verification.headHash;
    this.#sequence = verification.entries.length;
    this.#ready = true;
    return this.status();
  }

  status() {
    return {
      ready: this.#ready,
      storage: this.#config.storage,
      integrity: this.#ready ? "verified" : "unavailable",
    };
  }

  async append({
    category,
    action,
    outcome,
    operator,
    details = {},
    occurredAt = new Date(),
  }) {
    if (!this.#ready) {
      throw new Error("La bitácora operacional no está inicializada");
    }
    const operation = this.#queue.then(async () => {
      const fileStats = await stat(this.#config.filePath);
      if (fileStats.size >= this.#config.maxBytes) {
        throw new Error("La bitácora operacional alcanzó el límite configurado");
      }
      const eventWithoutHash = {
        schemaVersion: 1,
        id: randomUUID(),
        sequence: this.#sequence + 1,
        occurredAt: occurredAt.toISOString(),
        environment: this.#config.environment,
        category: limitedString(category, 80),
        action: limitedString(action, 120),
        outcome: limitedString(outcome, 40),
        operator: limitedString(operator, 200),
        details: sanitizeValue(details),
        previousHash: this.#headHash,
      };
      const entry = {
        ...eventWithoutHash,
        hash: eventHash(eventWithoutHash),
      };
      const handle = await open(this.#config.filePath, "a", 0o600);
      try {
        await handle.writeFile(`${JSON.stringify(entry)}\n`, "utf8");
        await handle.sync();
      } finally {
        await handle.close();
      }
      this.#sequence = entry.sequence;
      this.#headHash = entry.hash;
      return entry;
    });
    this.#queue = operation.catch(() => {});
    return operation;
  }

  async list(limit = this.#config.defaultLimit) {
    await this.#queue;
    const safeLimit = Math.min(
      positiveInteger(`${limit}`, "limit"),
      this.#config.defaultLimit,
    );
    const verification = await this.#readVerified();
    return {
      generatedAt: new Date().toISOString(),
      storage: this.#config.storage,
      summary: {
        totalEntries: verification.entries.length,
        returnedEntries: Math.min(safeLimit, verification.entries.length),
        integrity: verification.valid ? "verified" : "invalid",
        invalidLine: verification.invalidLine,
        headHash: verification.headHash,
      },
      entries: verification.entries.slice(-safeLimit).reverse(),
    };
  }

  async #readVerified() {
    const fileStats = await stat(this.#config.filePath);
    if (fileStats.size > this.#config.maxBytes) {
      throw new Error("La bitácora operacional supera el límite configurado");
    }
    const content = await readFile(this.#config.filePath, "utf8");
    return parseAndVerify(content);
  }
}

export function createAuditLog(config = loadAuditConfig()) {
  return new AuditLog(config);
}
