DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        CREATE ROLE anon NOLOGIN NOINHERIT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        CREATE ROLE authenticated NOLOGIN NOINHERIT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'service_role') THEN
        CREATE ROLE service_role NOLOGIN NOINHERIT BYPASSRLS;
    END IF;
END
$$;

ALTER ROLE anon NOLOGIN NOINHERIT NOBYPASSRLS;
ALTER ROLE authenticated NOLOGIN NOINHERIT NOBYPASSRLS;
ALTER ROLE service_role NOLOGIN NOINHERIT BYPASSRLS;

CREATE SCHEMA IF NOT EXISTS storage;

GRANT USAGE ON SCHEMA storage TO anon, authenticated, service_role;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA storage TO anon, authenticated, service_role;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA storage TO anon, authenticated, service_role;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA storage TO anon, authenticated, service_role;

ALTER DEFAULT PRIVILEGES IN SCHEMA storage
    GRANT ALL PRIVILEGES ON TABLES TO anon, authenticated, service_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA storage
    GRANT ALL PRIVILEGES ON SEQUENCES TO anon, authenticated, service_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA storage
    GRANT ALL PRIVILEGES ON FUNCTIONS TO anon, authenticated, service_role;
