# Docker Deployment — Decoupled Architecture

## Goal
Deploy the Status Monitoring app with Docker: stateless app container, stateful
database container, persistent named volume, service-name discovery. Random
database name / user / password written into `application.properties`.

**Constraint (user, revised):** no `.env` and no environment-variable
configuration — everything lives in `application.properties`, and that file is
written **only during `docker compose build`**, never at `docker compose up`.

## Plan

- [x] 1. Maven: copy the filtered `application.properties` from the classpath to
      `target/application.properties` at build time.
- [x] 2. Maven wrapper (`mvnw`) so the Docker build stage is self-contained.
- [x] 3. `Dockerfile` stage `credentials`: generate random db name / user /
      password / JWT secret / AES key at build time.
- [x] 4. `Dockerfile` stage `config`: the single place that writes
      `application.properties`, in place, preserving comments and ordering.
- [x] 5. `Dockerfile` stage `db`: PostgreSQL image that *reads* those credentials
      out of the same `application.properties` and feeds the stock entrypoint.
- [x] 6. `Dockerfile` stage `app`: JRE image running the jar from `/app`, where
      the same `application.properties` sits. No config written at start-up.
- [x] 7. `docker-compose.yml` with zero `environment:` / `env_file:` entries,
      named volume for the database, private network, service-name discovery.
- [x] 8. Remove the `.env` machinery from the first iteration; update `DOCKER.md`.
- [x] 9. Verify the whole config pipeline locally (dash **and** busybox ash).

## Review

### What changed

| File | Purpose |
| --- | --- |
| `pom.xml` | `maven-resources-plugin` execution `copy-external-config` copies the build-filtered `application.properties` from `target/classes` into `${project.build.directory}` during `prepare-package`. |
| `mvnw`, `mvnw.cmd`, `.mvn/wrapper/` | Maven wrapper, so the Docker build stage needs no Maven in the image. |
| `Dockerfile` | Five stages: `build` (Maven), `credentials` (random values), `config` (**the only write** to `application.properties`), `db` (postgres:17-alpine + credential-reading entrypoint), `app` (temurin JRE, uid 10001, jar + the same properties file). |
| `docker/generate-credentials.sh` | Build-time. Random `status_<12>` database name and user, 40-char alphanumeric password, Base64 48-byte JWT secret, Base64 32-byte AES key. |
| `docker/write-config.sh` | Build-time. Line-preserving property writer (same semantics as `SetupService.writeProperty`) that folds those values into `application.properties`. |
| `docker/read-db-config.sh` | Runtime, read-only. Parses `spring.datasource.url/username/password` and prints shell-quoted `DB_NAME`/`DB_USER`/`DB_PASSWORD`. |
| `docker/db-entrypoint.sh` | Exports `POSTGRES_DB/USER/PASSWORD` from that parse and execs the stock `docker-entrypoint.sh`. Writes nothing. |
| `docker/db-healthcheck.sh`, `docker/app-healthcheck.sh` | Healthchecks that take the database name / HTTP port from the same properties file. |
| `docker-compose.yml` | `db` (target `db`, named volume `status-db-data` → `/var/lib/postgresql/data`, no published port) + `app` (target `app`, no volumes, `depends_on: db healthy`, `8383:8383`). No `environment:`, no `env_file:`. |
| `.dockerignore` | Keeps `target/`, `.git/`, IDE files out of the build context. |
| `DOCKER.md` | Architecture, config-flow tables, operations, backup, `--no-cache` caveat, troubleshooting. |
| removed | `docker/entrypoint.sh`, `docker/generate-secrets.sh`, `.env.example` and the `.gitignore` `.env` block from the first iteration. |

### Verification performed
- `mvn clean package -DskipTests` → exit 0; `target/application.properties`
  present and build-filtered (`app.build.date=2026-08-05 17:41`); jar produced.
- Full build-time pipeline run locally under **dash** and again under **busybox
  ash** (what Alpine runs): `generate-credentials.sh` → `write-config.sh` →
  `read-db-config.sh`. Credentials written into the real properties file land in
  the right keys (`spring.datasource.url/username/password`, `jwt.secret`,
  `scheduler.encryption.key`), line count unchanged (343 → 343), no duplicate
  keys, and the db-side parser reads back byte-identical values.
- Rendered file parsed with `java.util.Properties`: 57 keys, values exact.
- Parser hardened against hostile input, verified: a URL with `?sslmode=disable`
  yields the bare database name; a username containing `$` and a password
  containing `'`, `"` and an escaped backslash survive the shell-quoted `eval`
  intact.
- `docker-compose.yml` parsed with a YAML loader: no `environment`, no
  `env_file`, no volumes on `app`, named volume on `db`, `depends_on` healthy
  condition present. `sh -n` clean on all six scripts.
- **Not run: `docker compose build` / `up`.** Docker is not installed on this
  machine (`docker: command not found`), so the images were never actually built
  and the containers never started. Dockerfile and compose file are validated by
  parsing and inspection only; every shell script was executed for real.

### Notes / decisions
- Credentials are baked into both images by the shared `config` stage, so the app
  and the database cannot disagree about them.
- The `credentials` stage copies only its own script, so its cache key does not
  change when application code or properties change — normal rebuilds keep the
  credentials that match the existing database volume. `--no-cache` mints new
  ones; `DOCKER.md` documents the two recovery routes.
- No `SERVER_PORT`/`SPRING_*` variables are set in the images: Spring's relaxed
  binding would let an environment variable outrank `application.properties`, and
  that file is meant to be the single source of truth. The healthchecks read the
  port and database name out of the file instead.
- The app image keeps `/app/application.properties` owned by uid 10001 so the
  `/setup` wizard still works; those runtime edits are ephemeral, which is what
  keeps the container stateless.
- Consequence worth knowing: the images now contain the database password, so
  they must be treated as secrets and never pushed to a public registry.
