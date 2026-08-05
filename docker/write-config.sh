#!/bin/sh
#
# BUILD TIME ONLY — runs inside the "config" stage of the Dockerfile, i.e.
# during `docker compose build`. This is the single place where
# application.properties is ever written for a Docker deployment; the running
# containers only read it.
#
# Takes the build-filtered application.properties produced by Maven
# (target/application.properties) and the credentials emitted by
# generate-credentials.sh, and writes the credentials into the matching
# properties in place — comments, ordering and every other property are
# preserved, exactly like the /setup wizard's own writer (SetupService).
#
# Usage: write-config.sh <application.properties> <credentials-file> [db-host] [db-port]

set -eu

CONFIG_FILE="${1:?usage: write-config.sh <application.properties> <credentials-file> [db-host] [db-port]}"
CREDENTIALS_FILE="${2:?usage: write-config.sh <application.properties> <credentials-file> [db-host] [db-port]}"
# Service discovery by compose service name — never an IP address.
DB_HOST="${3:-db}"
DB_PORT="${4:-5432}"

[ -f "$CONFIG_FILE" ]      || { echo "no such properties file: $CONFIG_FILE" >&2; exit 1; }
[ -f "$CREDENTIALS_FILE" ] || { echo "no such credentials file: $CREDENTIALS_FILE" >&2; exit 1; }

# shellcheck disable=SC1090 # generated file, plain KEY=VALUE lines
. "$CREDENTIALS_FILE"

# Escapes a value for java.util.Properties, where a backslash is the escape
# character and has to be doubled to survive a round trip.
escape_value() {
    printf '%s' "$1" | sed 's/\\/\\\\/g'
}

# Replaces the value of an existing key in place; appends the key when the file
# does not define it. Only an uncommented assignment of exactly this key matches.
set_property() {
    _key="$1"
    _value="$(escape_value "$2")"
    PROP_KEY="$_key" PROP_VALUE="$_value" awk '
        BEGIN { key = ENVIRON["PROP_KEY"]; value = ENVIRON["PROP_VALUE"]; replaced = 0 }
        {
            line = $0
            trimmed = line
            sub(/^[ \t]+/, "", trimmed)
            if (!replaced && (index(trimmed, key "=") == 1 || index(trimmed, key " =") == 1)) {
                print key "=" value
                replaced = 1
            } else {
                print line
            }
        }
        END { if (!replaced) print key "=" value }
    ' "$CONFIG_FILE" > "$CONFIG_FILE.tmp"
    mv "$CONFIG_FILE.tmp" "$CONFIG_FILE"
}

set_property "spring.datasource.url"      "jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
set_property "spring.datasource.username" "$DB_USER"
set_property "spring.datasource.password" "$DB_PASSWORD"
set_property "jwt.secret"                 "$JWT_SECRET"
set_property "scheduler.encryption.key"   "$SCHEDULER_ENCRYPTION_KEY"

echo "[config] application.properties points at jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME} as ${DB_USER}"
