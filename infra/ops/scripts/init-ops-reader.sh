#!/bin/sh
set -eu

: "${DB_HOST:?Defina DB_HOST en infra/.env}"
: "${DB_PORT:?Defina DB_PORT en infra/.env}"
: "${DB_NAME:?Defina DB_NAME en infra/.env}"
: "${DB_USER:?Defina DB_USER en infra/.env}"
: "${DB_PASSWORD:?Defina DB_PASSWORD en infra/.env}"
: "${OPS_DB_USER:?Defina OPS_DB_USER en infra/ops/.env}"
: "${OPS_DB_PASSWORD:?Defina OPS_DB_PASSWORD en infra/ops/.env}"

export PGPASSWORD="$DB_PASSWORD"
export PGSSLMODE="${DB_SSLMODE:-disable}"

psql \
  --set ON_ERROR_STOP=1 \
  --set "ops_reader=$OPS_DB_USER" \
  --set "ops_password=$OPS_DB_PASSWORD" \
  --host "$DB_HOST" \
  --port "$DB_PORT" \
  --username "$DB_USER" \
  --dbname "$DB_NAME" \
  --file /opt/kontora/init-ops-reader.sql
