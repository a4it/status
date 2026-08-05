#!/bin/sh
#
# BUILD TIME ONLY — runs inside the "credentials" stage of the Dockerfile,
# i.e. during `docker compose build`. Never runs when a container starts.
#
# Emits a random database name, database user, database password, JWT signing
# secret and scheduler encryption key as a small key=value file that the
# "config" build stage folds into application.properties. The file never leaves
# the build; the only artefact that reaches the images is application.properties.
#
# Usage: generate-credentials.sh <output-file>

set -eu

OUT="${1:?usage: generate-credentials.sh <output-file>}"

# Random alphanumeric string of the requested length. Alphanumeric only: the
# values end up in a PostgreSQL identifier, a JDBC URL and a Java properties
# file, which avoids every quoting and escaping edge case in all three.
# Fixed-size reads rather than an endless /dev/urandom piped into head, which
# would take down the pipeline with SIGPIPE.
random_alnum() {
    _want="$1"
    _out=""
    while [ "${#_out}" -lt "$_want" ]; do
        _out="${_out}$(head -c 256 /dev/urandom | base64 | LC_ALL=C tr -cd 'A-Za-z0-9')"
    done
    printf '%s' "$_out" | cut -c "1-${_want}"
}

# Base64 of N random bytes: the JWT secret (HS256 wants >= 32 bytes) and the
# AES-256-GCM scheduler key (exactly 32 bytes).
random_base64() {
    head -c "$1" /dev/urandom | base64 | tr -d '\n'
}

DB_NAME="status_$(random_alnum 12 | tr '[:upper:]' '[:lower:]')"
DB_USER="status_$(random_alnum 12 | tr '[:upper:]' '[:lower:]')"
DB_PASSWORD="$(random_alnum 40)"

mkdir -p "$(dirname "$OUT")"
umask 077
cat > "$OUT" <<EOF
DB_NAME=${DB_NAME}
DB_USER=${DB_USER}
DB_PASSWORD=${DB_PASSWORD}
JWT_SECRET=$(random_base64 48)
SCHEDULER_ENCRYPTION_KEY=$(random_base64 32)
EOF

echo "[credentials] generated database '${DB_NAME}' with user '${DB_USER}' and a 40 character password"
