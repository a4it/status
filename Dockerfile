# syntax=docker/dockerfile:1
#
# Status Monitoring — application and database images.
#
# All configuration lives in application.properties. There is no .env and no
# environment-variable configuration: the credentials are generated once, during
# `docker compose build`, and written into application.properties by the
# "config" stage below. Container start-up (`docker compose up`) never writes to
# that file — both images only read it.
#
# Stages:
#   build       Maven build of the fat jar + target/application.properties
#   credentials random database name / user / password / JWT secret / AES key
#   config      folds the credentials into application.properties (WRITE happens here)
#   db          PostgreSQL, reads its POSTGRES_* from that same file
#   app         Spring Boot application, reads that same file as ./application.properties

# -----------------------------------------------------------------------------
# Stage 1 — build
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS build

WORKDIR /build

# Dependency layer: resolved from the POM alone so it stays cached while only
# application sources change.
COPY .mvn/ .mvn/
COPY mvnw pom.xml build.properties ./
RUN chmod +x mvnw
# Cache warming only — go-offline cannot always resolve every plugin, and a miss
# here must not fail the build; the package step below is the real gate.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -DskipTests dependency:go-offline || true

COPY src/ src/

# -DskipTests: the tests need a live PostgreSQL and are run by CI, not by the image build.
# The build also copies the filtered application.properties to target/ (see pom.xml).
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -DskipTests package

# Normalise the versioned artifact name (target/status-<version>.jar) to app.jar.
RUN set -eux; \
    jar="$(find target -maxdepth 1 -name '*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -n 1)"; \
    test -n "$jar"; \
    cp "$jar" /build/app.jar; \
    test -f target/application.properties

# -----------------------------------------------------------------------------
# Stage 2 — credentials
# -----------------------------------------------------------------------------
# Deliberately depends on nothing but its own script, so a normal rebuild reuses
# the cached credentials and keeps matching the existing database volume.
# `docker compose build --no-cache` mints new ones — see DOCKER.md.
FROM alpine:3.21 AS credentials

COPY docker/generate-credentials.sh /usr/local/bin/generate-credentials.sh
RUN chmod 0755 /usr/local/bin/generate-credentials.sh \
    && /usr/local/bin/generate-credentials.sh /credentials/credentials

# -----------------------------------------------------------------------------
# Stage 3 — config
# -----------------------------------------------------------------------------
# The one and only place where application.properties is written. The result is
# the exact file that both the app and the db image ship, which is what keeps
# the two in agreement about the database name, user and password.
FROM alpine:3.21 AS config

COPY docker/write-config.sh /usr/local/bin/write-config.sh
COPY --from=build /build/target/application.properties /config/application.properties
COPY --from=credentials /credentials/credentials /credentials/credentials
RUN chmod 0755 /usr/local/bin/write-config.sh \
    && /usr/local/bin/write-config.sh /config/application.properties /credentials/credentials db 5432 \
    && rm -f /credentials/credentials

# -----------------------------------------------------------------------------
# Stage 4 — db
# -----------------------------------------------------------------------------
FROM postgres:17-alpine AS db

# Read-only copy of the same application.properties the application uses; the
# entrypoint derives POSTGRES_DB / POSTGRES_USER / POSTGRES_PASSWORD from it.
COPY --from=config /config/application.properties /etc/status/application.properties
COPY docker/read-db-config.sh   /usr/local/bin/read-db-config.sh
COPY docker/db-entrypoint.sh    /usr/local/bin/status-db-entrypoint.sh
COPY docker/db-healthcheck.sh   /usr/local/bin/status-db-healthcheck.sh
RUN chmod 0755 /usr/local/bin/read-db-config.sh \
                /usr/local/bin/status-db-entrypoint.sh \
                /usr/local/bin/status-db-healthcheck.sh \
    && chmod 0640 /etc/status/application.properties \
    && chown root:postgres /etc/status/application.properties

ENV POSTGRES_INITDB_ARGS="--auth-host=scram-sha-256 --auth-local=scram-sha-256" \
    POSTGRES_HOST_AUTH_METHOD=scram-sha-256

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=10 \
    CMD /usr/local/bin/status-db-healthcheck.sh

ENTRYPOINT ["/usr/local/bin/status-db-entrypoint.sh"]
CMD ["postgres"]

# -----------------------------------------------------------------------------
# Stage 5 — app (default target)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jre AS app

# curl is used by the container healthcheck.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Unprivileged, no login shell. /app is the working directory, which is where
# Spring Boot looks for an external application.properties ("file:./").
RUN useradd --system --uid 10001 --create-home --home-dir /app --shell /usr/sbin/nologin status

COPY --from=build  /build/app.jar                 /opt/status/app.jar
# Owned by the application user so the /setup wizard can still edit it at
# runtime; those edits live in the container's ephemeral layer and are gone on
# recreate, which is what keeps this container stateless.
COPY --from=config --chown=10001:10001 --chmod=0640 /config/application.properties /app/application.properties
COPY docker/app-healthcheck.sh /usr/local/bin/status-app-healthcheck.sh
RUN chmod 0755 /usr/local/bin/status-app-healthcheck.sh

# No SERVER_PORT or SPRING_* variables here on purpose: Spring's relaxed binding
# would let an environment variable outrank application.properties, and that file
# is the single source of truth for this deployment.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"

WORKDIR /app
USER 10001
EXPOSE 8383

# The port comes from application.properties, see the script.
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
    CMD /usr/local/bin/status-app-healthcheck.sh

# exec keeps the JVM as PID 1 so it receives SIGTERM directly.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /opt/status/app.jar"]
