# Docker Deployment

Deployment of the Status Monitoring application with a decoupled Docker
architecture: a **stateless application container**, a **stateful database
container**, and a **named volume** holding all persistent data.

There is **no `.env` file and no environment-variable configuration**. Everything
lives in `application.properties`, and that file is written exactly once — during
`docker compose build`. `docker compose up` never writes configuration.

```
        ┌──────────────────────────────┐        ┌──────────────────────────────┐
        │  app  (status-app)           │        │  db  (status-db)             │
        │  ─────────────────────────── │        │  ─────────────────────────── │
 :8383 ─┤  Spring Boot fat jar         ├───────►│  PostgreSQL                  │
        │  reads ./application.properties  db:  │  reads the same file for     │
        │  no volumes, nothing persisted│ 5432  │  POSTGRES_DB/USER/PASSWORD   │
        └──────────────────────────────┘        └───────────────┬──────────────┘
                                                                │
                                                  ┌─────────────▼──────────────┐
                                                  │ volume: status-db-data     │
                                                  │ /var/lib/postgresql/data   │
                                                  │ THE only persistent layer  │
                                                  └────────────────────────────┘
```

## Quick start

```bash
docker compose build      # generates random credentials and writes them into
                          # application.properties inside both images
docker compose up -d      # starts both containers; writes no configuration
docker compose logs -f app
```

The application is then available on <http://localhost:8383>.

## Where the configuration comes from

1. **Maven build.** `maven-resources-plugin` copies the build-filtered
   `src/main/resources/application.properties` to `target/application.properties`,
   next to the jar. That copy is the deployment configuration; Spring Boot reads
   `file:./application.properties` as a default config location, so it overrides
   the copy inside the jar.
2. **`docker compose build`.** Three stages of the `Dockerfile` do the work:

   | Stage | What it does |
   | --- | --- |
   | `build` | Runs `./mvnw package`, producing `app.jar` and `target/application.properties`. |
   | `credentials` | Generates a random database name, database user, 40-character password, Base64 JWT secret and 32-byte AES scheduler key. |
   | `config` | **The only write.** Folds those values into `application.properties` in place — comments, ordering and all other properties are preserved, exactly like the `/setup` wizard's own writer. |

   The properties touched are:

   | Property | Value |
   | --- | --- |
   | `spring.datasource.url` | `jdbc:postgresql://db:5432/<random database>` |
   | `spring.datasource.username` | random, `status_<12 chars>` |
   | `spring.datasource.password` | random, 40 characters |
   | `jwt.secret` | random Base64, 48 bytes |
   | `scheduler.encryption.key` | random Base64, 32 bytes |

3. **Both images ship that same file**, which is what keeps them in agreement:
   the app image has it at `/app/application.properties`, the db image at
   `/etc/status/application.properties`.
4. **`docker compose up`.** The db entrypoint *reads* the file and hands
   `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` to the stock PostgreSQL
   entrypoint; the app just starts the JVM. Neither writes anything.

Check what was generated:

```bash
docker compose run --rm --no-deps --entrypoint sh app \
  -c 'grep -E "^spring\.datasource" /app/application.properties'
```

## Changing settings

Edit `src/main/resources/application.properties`, then:

```bash
docker compose build
docker compose up -d
```

The `credentials` stage depends on nothing but its own script, so a rebuild
reuses the cached credentials and keeps matching the existing database volume —
only the settings you edited change.

To change a value **without** rebuilding, edit it inside the running container
(`docker compose exec app sh -c 'vi /app/application.properties'`) or through the
`/setup` wizard, and restart the container. Those edits live in the container's
ephemeral layer and are lost on recreate — by design; anything permanent belongs
in `src/main/resources/application.properties`.

Changing `server.port` also means changing the published port in
`docker-compose.yml`.

## Architecture guarantees

**Stateless app layer.** The app service mounts no volumes. Everything it writes
lives in the container's ephemeral layer and is gone on `docker compose up
--force-recreate`, which is therefore always safe.

**Stateful database layer.** PostgreSQL runs in its own container and publishes
no port — it is reachable only from the compose network. Add
`ports: ["127.0.0.1:5432:5432"]` to the `db` service only when an external client
genuinely needs it.

**Persistent data layer.** All persistence lives in the named volume
`status-db-data`, mounted at the image's default data directory
`/var/lib/postgresql/data`. `docker compose down` leaves it untouched; only
`docker compose down -v` destroys it.

**Service discovery.** The application connects to `db:5432` — the compose
service name resolved by Docker's embedded DNS. No IP address appears anywhere.

## Operations

```bash
# Status and logs
docker compose ps
docker compose logs -f app

# Restart just the application (database keeps running)
docker compose restart app

# Rebuild after a code or configuration change
docker compose build app && docker compose up -d app

# Stop everything, keep the data
docker compose down

# Stop everything and DESTROY the database volume
docker compose down -v
```

### Backup and restore

```bash
# Backup — credentials are read from application.properties inside the container
docker compose exec db sh -c \
  'eval "$(/usr/local/bin/read-db-config.sh)"; PGPASSWORD="$DB_PASSWORD" pg_dump -U "$DB_USER" -d "$DB_NAME"' > backup.sql

# Restore into a fresh volume
docker compose exec -T db sh -c \
  'eval "$(/usr/local/bin/read-db-config.sh)"; PGPASSWORD="$DB_PASSWORD" psql -U "$DB_USER" -d "$DB_NAME"' < backup.sql
```

### Rebuilding with `--no-cache`

`docker compose build --no-cache` re-runs the `credentials` stage and mints a
**new** database name, user and password. The existing volume still holds the old
ones, so the app will fail to authenticate. Either avoid `--no-cache`, or take
one of these routes afterwards:

```bash
# a) start over (destroys all data)
docker compose down -v && docker compose up -d

# b) keep the data: create the new role and database in the running server
docker compose up -d db
docker compose exec db sh -c 'eval "$(/usr/local/bin/read-db-config.sh)";
  psql -U postgres -c "CREATE ROLE \"$DB_USER\" LOGIN PASSWORD '\''$DB_PASSWORD'\''";
  psql -U postgres -c "CREATE DATABASE \"$DB_NAME\" OWNER \"$DB_USER\""'
```

## Troubleshooting

**App logs `FATAL: password authentication failed`.** The image and the volume
disagree about the credentials — almost always after a `--no-cache` rebuild. See
the section above.

**The app and db images disagree about the credentials.** Both take the file from
the same build stage, so this should not happen; if it ever does (a cold cache
with both services building in parallel), rebuild serially:

```bash
COMPOSE_PARALLEL_LIMIT=1 docker compose build
# verify the two files match
docker compose run --rm --no-deps --entrypoint sh app -c 'grep "^spring.datasource.username" /app/application.properties'
docker compose run --rm --no-deps --entrypoint sh db  -c 'grep "^spring.datasource.username" /etc/status/application.properties'
```

**Flyway fails on an existing database.** The volume holds a schema from a
different version. Back up, `docker compose down -v`, restore into a fresh volume.

**The build cannot download Maven or dependencies.** The Docker build needs
network access to `repo.maven.apache.org`. Behind a proxy:
`docker compose build --build-arg HTTP_PROXY=... --build-arg HTTPS_PROXY=...`.

## Production checklist

- [ ] `app.cors.allowed-origins` set to the real origin, not `localhost`
      (edit `src/main/resources/application.properties`, rebuild).
- [ ] TLS terminated by a reverse proxy in front of the `app` service; drop the
      `ports:` mapping and attach the proxy to `status-net`.
- [ ] `springdoc.swagger-ui.enabled=false` (already the default).
- [ ] Log levels lowered — `logging.level.org.springframework.security=INFO`.
- [ ] Backups of the `status-db-data` volume scheduled.
- [ ] Images treated as secrets: they contain the generated database password.
      Do not push them to a public registry.
