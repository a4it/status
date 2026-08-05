#!/bin/sh
#
# Entrypoint of the database image. Reads the database name, user and password
# out of the application.properties baked in at build time, hands them to the
# stock PostgreSQL entrypoint as POSTGRES_*, and gets out of the way.
#
# Reading only: no file is written here, so `docker compose up` never changes
# configuration. The credentials are applied by PostgreSQL when it initialises
# an empty data volume; on later starts they are simply the values the
# application will authenticate with.

set -eu

CONFIG_FILE="${STATUS_CONFIG_FILE:-/etc/status/application.properties}"

eval "$(/usr/local/bin/read-db-config.sh "$CONFIG_FILE")"

export POSTGRES_DB="$DB_NAME"
export POSTGRES_USER="$DB_USER"
export POSTGRES_PASSWORD="$DB_PASSWORD"

echo "[db] using database '${POSTGRES_DB}' and user '${POSTGRES_USER}' from ${CONFIG_FILE}"

exec /usr/local/bin/docker-entrypoint.sh "$@"
