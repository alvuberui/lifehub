#!/usr/bin/env bash
set -Eeuo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    -v identity_password="$IDENTITY_DB_PASSWORD" \
    -v movements_password="$MOVEMENTS_DB_PASSWORD" \
    -v tags_password="$TAGS_DB_PASSWORD" <<'SQL'
CREATE ROLE identityuser LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD :'identity_password';
CREATE ROLE movementsuser LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD :'movements_password';
CREATE ROLE tagsuser LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD :'tags_password';
CREATE DATABASE identitydb OWNER identityuser ENCODING 'UTF8' TEMPLATE template0;
CREATE DATABASE movementsdb OWNER movementsuser ENCODING 'UTF8' TEMPLATE template0;
CREATE DATABASE tagsdb OWNER tagsuser ENCODING 'UTF8' TEMPLATE template0;
SQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname identitydb <<'SQL'
REVOKE CONNECT ON DATABASE identitydb FROM PUBLIC;
GRANT CONNECT ON DATABASE identitydb TO identityuser;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA identity AUTHORIZATION identityuser;
GRANT USAGE, CREATE ON SCHEMA identity TO identityuser;
SQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname movementsdb <<'SQL'
REVOKE CONNECT ON DATABASE movementsdb FROM PUBLIC;
GRANT CONNECT ON DATABASE movementsdb TO movementsuser;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA movements AUTHORIZATION movementsuser;
GRANT USAGE, CREATE ON SCHEMA movements TO movementsuser;
SQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname tagsdb <<'SQL'
REVOKE CONNECT ON DATABASE tagsdb FROM PUBLIC;
GRANT CONNECT ON DATABASE tagsdb TO tagsuser;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA tags AUTHORIZATION tagsuser;
GRANT USAGE, CREATE ON SCHEMA tags TO tagsuser;
SQL
