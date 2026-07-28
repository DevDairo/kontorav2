const DEFAULT_TIMEOUT_MS = 5000;

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

function enabledValue(value) {
  return ["1", "true", "yes", "on"].includes(value.trim().toLowerCase());
}

export function loadDatabaseDiagnosticsConfig(env = process.env) {
  const enabled = enabledValue(envValue(env, "OPS_DB_DIAGNOSTICS_ENABLED", "false"));
  const config = {
    enabled,
    host: envValue(env, "OPS_DB_HOST", "postgres"),
    port: positiveInteger(envValue(env, "OPS_DB_PORT", "5432"), "OPS_DB_PORT"),
    database: envValue(env, "OPS_DB_NAME", "kontora_pos"),
    user: envValue(env, "OPS_DB_USER", "kontora_ops_reader"),
    password: envValue(env, "OPS_DB_PASSWORD"),
    sslMode: envValue(env, "OPS_DB_SSLMODE", "disable").toLowerCase(),
    bucket: envValue(env, "OPS_STORAGE_BUCKET", "kontoraimagenes"),
    timeoutMs: positiveInteger(
      envValue(env, "OPS_DB_DIAGNOSTICS_TIMEOUT_MS", `${DEFAULT_TIMEOUT_MS}`),
      "OPS_DB_DIAGNOSTICS_TIMEOUT_MS",
    ),
  };

  if (!enabled) {
    return config;
  }
  if (!config.host || !config.database || !config.user || !config.password || !config.bucket) {
    throw new Error(
      "Los diagnósticos PostgreSQL requieren host, base, usuario, contraseña y bucket",
    );
  }
  if (!["disable", "require"].includes(config.sslMode)) {
    throw new Error("OPS_DB_SSLMODE debe ser disable o require");
  }
  return config;
}

async function defaultClientFactory(config) {
  const { Client } = await import("pg");
  return new Client({
    host: config.host,
    port: config.port,
    database: config.database,
    user: config.user,
    password: config.password,
    ssl: config.sslMode === "require" ? { rejectUnauthorized: true } : false,
    application_name: "kontora-ops-panel",
    connectionTimeoutMillis: config.timeoutMs,
    query_timeout: config.timeoutMs,
    statement_timeout: config.timeoutMs,
    options: "-c default_transaction_read_only=on",
  });
}

async function readOnlyQuery(client, text, values = []) {
  await client.query("BEGIN TRANSACTION READ ONLY");
  try {
    const result = await client.query(text, values);
    await client.query("COMMIT");
    return result;
  } catch (error) {
    await client.query("ROLLBACK").catch(() => {});
    throw error;
  }
}

function unavailableCheck(message) {
  return {
    state: "unavailable",
    message,
  };
}

async function collectFlyway(client) {
  try {
    const result = await readOnlyQuery(client, `
      SELECT
        installed_rank,
        version,
        description,
        installed_on,
        execution_time,
        success
      FROM kontora_ops.flyway_snapshot()
      ORDER BY installed_rank
    `);
    const migrations = result.rows.map((row) => ({
      installedRank: row.installed_rank,
      version: row.version,
      description: row.description,
      installedOn: row.installed_on,
      executionTimeMs: row.execution_time,
      success: row.success,
    }));
    const successful = migrations.filter((migration) => migration.success).length;
    const latest = migrations.at(-1) || null;
    return {
      state: migrations.length > 0 && successful === migrations.length
        ? "operational"
        : "attention",
      installed: migrations.length,
      successful,
      latest,
      migrations,
    };
  } catch {
    return unavailableCheck("No fue posible consultar el historial de Flyway");
  }
}

async function collectBucket(client, bucketId) {
  try {
    const bucketResult = await readOnlyQuery(client, `
      SELECT id, name, public, file_size_limit, allowed_mime_types
        , object_count::text
      FROM kontora_ops.bucket_snapshot($1)
    `, [bucketId]);
    const bucket = bucketResult.rows[0] || null;
    const objectCount = Number.parseInt(bucket?.object_count || "0", 10);
    return {
      state: bucket && bucket.public === false ? "operational" : "attention",
      expectedId: bucketId,
      present: Boolean(bucket),
      id: bucket?.id || null,
      name: bucket?.name || null,
      public: bucket?.public ?? null,
      fileSizeLimit: bucket?.file_size_limit === null
        ? null
        : Number(bucket?.file_size_limit),
      allowedMimeTypes: bucket?.allowed_mime_types || [],
      objectCount,
    };
  } catch {
    return unavailableCheck("No fue posible consultar el bucket de Storage");
  }
}

async function collectEvidenceConsistency(client, bucketId) {
  try {
    const result = await readOnlyQuery(client, `
      SELECT
        reference_total::text,
        invalid_reference_total::text,
        missing_object_total::text,
        unreferenced_object_total::text
      FROM kontora_ops.evidence_snapshot($1)
    `, [bucketId]);
    const row = result.rows[0];
    const referenceTotal = Number.parseInt(row.reference_total, 10);
    const invalidReferenceTotal = Number.parseInt(row.invalid_reference_total, 10);
    const missingObjectTotal = Number.parseInt(row.missing_object_total, 10);
    const unreferencedObjectTotal = Number.parseInt(row.unreferenced_object_total, 10);
    const consistent =
      invalidReferenceTotal === 0
      && missingObjectTotal === 0
      && unreferencedObjectTotal === 0;
    return {
      state: consistent ? "operational" : "attention",
      consistent,
      referenceTotal,
      invalidReferenceTotal,
      missingObjectTotal,
      unreferencedObjectTotal,
    };
  } catch {
    return unavailableCheck("No fue posible comprobar las referencias de evidencia");
  }
}

function disabledDiagnostics() {
  const check = {
    state: "disabled",
    message: "Diagnóstico PostgreSQL no habilitado",
  };
  return {
    enabled: false,
    summary: {
      overall: "disabled",
      checks: 3,
      operationalChecks: 0,
      attentionChecks: 0,
    },
    database: {
      reachable: false,
      state: "disabled",
      version: null,
      name: null,
      reader: null,
    },
    flyway: { ...check },
    bucket: { ...check },
    evidence: { ...check },
  };
}

function summarizeChecks(checks) {
  const operationalChecks = checks.filter((check) => check.state === "operational").length;
  return {
    overall: operationalChecks === checks.length ? "operational" : "attention",
    checks: checks.length,
    operationalChecks,
    attentionChecks: checks.length - operationalChecks,
  };
}

export async function collectDatabaseDiagnostics(
  config,
  {
    clientFactory = defaultClientFactory,
  } = {},
) {
  if (!config.enabled) {
    return disabledDiagnostics();
  }

  let client;
  try {
    client = await clientFactory(config);
    await client.connect();
    const databaseResult = await readOnlyQuery(client, `
      SELECT
        current_database() AS database_name,
        current_user AS reader_name,
        current_setting('server_version') AS server_version,
        current_setting('default_transaction_read_only') AS read_only
    `);
    const databaseRow = databaseResult.rows[0];
    if (databaseRow.read_only !== "on") {
      throw new Error("La conexión de diagnóstico no está en modo de solo lectura");
    }

    const flyway = await collectFlyway(client);
    const bucket = await collectBucket(client, config.bucket);
    const evidence = await collectEvidenceConsistency(client, config.bucket);
    const checks = [flyway, bucket, evidence];
    return {
      enabled: true,
      summary: summarizeChecks(checks),
      database: {
        reachable: true,
        state: "operational",
        version: databaseRow.server_version,
        name: databaseRow.database_name,
        reader: databaseRow.reader_name,
      },
      flyway,
      bucket,
      evidence,
    };
  } catch {
    const check = unavailableCheck("PostgreSQL no está disponible para el panel");
    return {
      enabled: true,
      summary: {
        overall: "attention",
        checks: 3,
        operationalChecks: 0,
        attentionChecks: 3,
      },
      database: {
        reachable: false,
        state: "unavailable",
        version: null,
        name: config.database,
        reader: config.user,
      },
      flyway: { ...check },
      bucket: { ...check },
      evidence: { ...check },
    };
  } finally {
    await client?.end().catch(() => {});
  }
}
