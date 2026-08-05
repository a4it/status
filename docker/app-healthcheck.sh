#!/bin/sh
#
# Container healthcheck of the application image. Takes the port from
# application.properties so the check follows the configuration instead of a
# second, separately maintained copy of it.
#
# /actuator/** requires an ADMIN role (SecurityConfig), so the permit-all login
# page is used as the readiness signal.

set -eu

CONFIG_FILE="${STATUS_CONFIG_FILE:-/app/application.properties}"

PORT="$(awk '
    BEGIN { found = 0 }
    !found {
        line = $0
        sub(/^[ \t]+/, "", line)
        if (index(line, "server.port=") == 1 || index(line, "server.port =") == 1) {
            sub(/^[^=]*=[ \t]*/, "", line)
            gsub(/[ \t\r]/, "", line)
            print line
            found = 1
        }
    }
' "$CONFIG_FILE" 2>/dev/null || true)"

exec curl -fsS -o /dev/null "http://127.0.0.1:${PORT:-8383}/login"
