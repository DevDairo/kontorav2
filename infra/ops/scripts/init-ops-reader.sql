\set ON_ERROR_STOP on

SELECT format(
    'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS CONNECTION LIMIT 5',
    :'ops_reader',
    :'ops_password'
)
WHERE NOT EXISTS (
    SELECT 1
    FROM pg_roles
    WHERE rolname = :'ops_reader'
)
\gexec

SELECT format(
    'ALTER ROLE %I WITH LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS CONNECTION LIMIT 5',
    :'ops_reader',
    :'ops_password'
)
\gexec

SELECT format(
    'ALTER ROLE %I SET default_transaction_read_only = on',
    :'ops_reader'
)
\gexec

SELECT format(
    'GRANT CONNECT ON DATABASE %I TO %I',
    current_database(),
    :'ops_reader'
)
\gexec

CREATE SCHEMA IF NOT EXISTS kontora_ops;
REVOKE ALL ON SCHEMA kontora_ops FROM PUBLIC;

CREATE OR REPLACE FUNCTION kontora_ops.flyway_snapshot()
RETURNS TABLE (
    installed_rank INTEGER,
    version TEXT,
    description TEXT,
    installed_on TIMESTAMP WITHOUT TIME ZONE,
    execution_time INTEGER,
    success BOOLEAN
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog
AS $function$
    SELECT
        history.installed_rank::INTEGER,
        history.version::TEXT,
        history.description::TEXT,
        history.installed_on::TIMESTAMP WITHOUT TIME ZONE,
        history.execution_time::INTEGER,
        history.success::BOOLEAN
    FROM public.flyway_schema_history AS history
    ORDER BY history.installed_rank
$function$;

CREATE OR REPLACE FUNCTION kontora_ops.bucket_snapshot(expected_bucket TEXT)
RETURNS TABLE (
    id TEXT,
    name TEXT,
    public BOOLEAN,
    file_size_limit BIGINT,
    allowed_mime_types TEXT[],
    object_count BIGINT
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog
AS $function$
    SELECT
        bucket.id::TEXT,
        bucket.name::TEXT,
        bucket.public::BOOLEAN,
        bucket.file_size_limit::BIGINT,
        bucket.allowed_mime_types::TEXT[],
        (
            SELECT count(*)::BIGINT
            FROM storage.objects AS object
            WHERE object.bucket_id = bucket.id
        ) AS object_count
    FROM storage.buckets AS bucket
    WHERE bucket.id = expected_bucket
$function$;

CREATE OR REPLACE FUNCTION kontora_ops.evidence_snapshot(expected_bucket TEXT)
RETURNS TABLE (
    reference_total BIGINT,
    invalid_reference_total BIGINT,
    missing_object_total BIGINT,
    unreferenced_object_total BIGINT
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog
AS $function$
    WITH evidence_references AS (
        SELECT
            CASE
                WHEN evidence.url_archivo LIKE 'supabase://' || expected_bucket || '/%'
                    THEN substring(
                        evidence.url_archivo
                        FROM length('supabase://' || expected_bucket || '/') + 1
                    )
                ELSE NULL
            END AS object_name
        FROM public.archivos_evidencia AS evidence
    ),
    bucket_objects AS (
        SELECT object.name
        FROM storage.objects AS object
        WHERE object.bucket_id = expected_bucket
    )
    SELECT
        (SELECT count(*)::BIGINT FROM evidence_references),
        (
            SELECT count(*)::BIGINT
            FROM evidence_references
            WHERE object_name IS NULL OR object_name = ''
        ),
        (
            SELECT count(*)::BIGINT
            FROM evidence_references AS reference
            WHERE reference.object_name IS NOT NULL
              AND reference.object_name <> ''
              AND NOT EXISTS (
                  SELECT 1
                  FROM bucket_objects AS object
                  WHERE object.name = reference.object_name
              )
        ),
        (
            SELECT count(*)::BIGINT
            FROM bucket_objects AS object
            WHERE NOT EXISTS (
                SELECT 1
                FROM evidence_references AS reference
                WHERE reference.object_name = object.name
            )
        )
$function$;

REVOKE ALL ON FUNCTION kontora_ops.flyway_snapshot() FROM PUBLIC;
REVOKE ALL ON FUNCTION kontora_ops.bucket_snapshot(TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION kontora_ops.evidence_snapshot(TEXT) FROM PUBLIC;

SELECT format('REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM %I', :'ops_reader')
\gexec

SELECT format('REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA storage FROM %I', :'ops_reader')
\gexec

SELECT format(
    'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public REVOKE SELECT ON TABLES FROM %I',
    current_user,
    :'ops_reader'
)
\gexec

SELECT format('GRANT USAGE ON SCHEMA kontora_ops TO %I', :'ops_reader')
\gexec

SELECT format('GRANT EXECUTE ON FUNCTION kontora_ops.flyway_snapshot() TO %I', :'ops_reader')
\gexec

SELECT format(
    'GRANT EXECUTE ON FUNCTION kontora_ops.bucket_snapshot(TEXT) TO %I',
    :'ops_reader'
)
\gexec

SELECT format(
    'GRANT EXECUTE ON FUNCTION kontora_ops.evidence_snapshot(TEXT) TO %I',
    :'ops_reader'
)
\gexec
