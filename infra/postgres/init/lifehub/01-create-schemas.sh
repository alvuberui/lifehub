#!/usr/bin/env bash
set -Eeuo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'SQL'
CREATE SCHEMA IF NOT EXISTS identity AUTHORIZATION lifehubuser;
CREATE SCHEMA IF NOT EXISTS movements AUTHORIZATION lifehubuser;
CREATE SCHEMA IF NOT EXISTS tags AUTHORIZATION lifehubuser;
SQL
