#!/bin/sh
#
# RUNTIME, READ ONLY — prints the database credentials found in
# application.properties as shell assignments, so the database container can be
# configured from the very same file the application uses. Nothing is written:
# application.properties is produced once, at `docker compose build`.
#
# Usage:  eval "$(read-db-config.sh /etc/status/application.properties)"
# Sets:   DB_NAME, DB_USER, DB_PASSWORD

set -eu

CONFIG_FILE="${1:-/etc/status/application.properties}"
[ -f "$CONFIG_FILE" ] || { echo "no such properties file: $CONFIG_FILE" >&2; exit 1; }

# Value of the first uncommented assignment of a key, with the Java properties
# backslash escaping undone.
read_property() {
    PROP_KEY="$1" awk '
        BEGIN { key = ENVIRON["PROP_KEY"]; found = 0 }
        !found {
            line = $0
            sub(/^[ \t]+/, "", line)
            if (index(line, key "=") == 1 || index(line, key " =") == 1) {
                sub(/^[^=]*=[ \t]*/, "", line)
                print line
                found = 1
            }
        }
    ' "$CONFIG_FILE" | sed 's/\\\\/\\/g'
}

# Single-quotes a value for safe eval by the caller.
shell_quote() {
    printf "'%s'" "$(printf '%s' "$1" | sed "s/'/'\\\\''/g")"
}

URL="$(read_property 'spring.datasource.url')"
DB_USER="$(read_property 'spring.datasource.username')"
DB_PASSWORD="$(read_property 'spring.datasource.password')"

# jdbc:postgresql://host:port/database[?params] -> database
DB_NAME="${URL##*/}"
DB_NAME="${DB_NAME%%\?*}"

[ -n "$DB_NAME" ]     || { echo "spring.datasource.url has no database name: $URL" >&2; exit 1; }
[ -n "$DB_USER" ]     || { echo "spring.datasource.username is empty in $CONFIG_FILE" >&2; exit 1; }
[ -n "$DB_PASSWORD" ] || { echo "spring.datasource.password is empty in $CONFIG_FILE" >&2; exit 1; }

echo "DB_NAME=$(shell_quote "$DB_NAME")"
echo "DB_USER=$(shell_quote "$DB_USER")"
echo "DB_PASSWORD=$(shell_quote "$DB_PASSWORD")"
