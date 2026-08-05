#!/bin/sh
#
# Container healthcheck of the database image: takes the database name and user
# from the same application.properties the entrypoint used, so the check follows
# the configuration automatically.

set -eu

eval "$(/usr/local/bin/read-db-config.sh "${STATUS_CONFIG_FILE:-/etc/status/application.properties}")"

exec pg_isready -q -U "$DB_USER" -d "$DB_NAME"
