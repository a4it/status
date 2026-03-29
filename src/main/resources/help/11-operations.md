# Section 11: Operations & Deployment

---

## 148. Production Deployment Guide

### Build

```bash
# Full build with tests
mvn clean package

# Output:
target/status-<version>.jar
```

### Deploy

```bash
# Copy the JAR and external config to the server
scp target/status-*.jar user@server:/opt/status/status.jar
scp application-prod.properties user@server:/opt/status/application.properties

# Run
java -Xms256m -Xmx512m \
     -jar /opt/status/status.jar \
     --spring.config.location=file:/opt/status/application.properties \
     --spring.profiles.active=prod
```

### Systemd Service (Linux)

Create `/etc/systemd/system/status.service`:
```ini
[Unit]
Description=Status Monitoring Application
After=network.target postgresql.service

[Service]
User=status
Group=status
WorkingDirectory=/opt/status
ExecStart=/usr/bin/java -Xms256m -Xmx512m \
    -jar /opt/status/status.jar \
    --spring.config.location=file:/opt/status/application.properties \
    --spring.profiles.active=prod
SuccessExitStatus=143
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

```bash
systemctl daemon-reload
systemctl enable status
systemctl start status
systemctl status status
```

---

## 149. Environment Variable Override Reference

In production, sensitive values should be provided via environment variables instead of the properties file:

| Environment Variable | Property | Required in Prod |
|---------------------|----------|-----------------|
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | Yes |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` | Yes |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` | Yes |
| `JWT_SECRET` | `jwt.secret` | Yes — change from default |
| `SPRING_MAIL_HOST` | `spring.mail.host` | If email is enabled |
| `SPRING_MAIL_USERNAME` | `spring.mail.username` | If email is enabled |
| `SPRING_MAIL_PASSWORD` | `spring.mail.password` | If email is enabled |
| `APP_EMAIL_ENABLED` | `app.email.enabled` | Recommended: `true` |
| `DATA_INITIALIZER_ENABLED` | `data.initializer.enabled` | Set to `false` after setup |
| `SERVER_PORT` | `server.port` | If not using default 8383 |

Set environment variables in your deployment environment (systemd `EnvironmentFile`, Docker `env_file`, Kubernetes secrets).

**Generating a production JWT secret:**
```bash
openssl rand -base64 32
```

---

## 150. Docker Setup

### `Dockerfile`

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/status-*.jar app.jar
EXPOSE 8383
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
```

### `docker-compose.yml`

```yaml
version: '3.8'

services:
  db:
    image: postgres:14-alpine
    environment:
      POSTGRES_DB: uptime
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: your-db-password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  status:
    build: .
    depends_on:
      - db
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/uptime
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: your-db-password
      JWT_SECRET: your-production-jwt-secret-base64
      SERVER_PORT: 8383
      APP_EMAIL_ENABLED: "false"
    ports:
      - "8383:8383"

volumes:
  postgres_data:
```

```bash
# Build and start
mvn clean package -DskipTests
docker-compose up -d

# View logs
docker-compose logs -f status
```

---

## 151. Health Endpoint

Spring Actuator provides a health endpoint:

**URL:** `GET /actuator/health`

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

**Production safety:** The `/actuator/**` pattern is `permitAll` in `SecurityConfig`. For production, consider restricting actuator access:
```properties
# Only expose health endpoint
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=when-authorized
```

Or restrict via reverse proxy — only allow `/actuator/health` from internal network IPs.

---

## 152. Log Configuration

The application uses Spring Boot's default Logback configuration.

**Current log levels** (`application.properties`):
```properties
logging.level.org.springframework.security=DEBUG
logging.level.org.automatize.status=DEBUG
```

**Production recommendations:**
```properties
# Reduce verbosity in production
logging.level.root=WARN
logging.level.org.springframework.security=WARN
logging.level.org.automatize.status=INFO
```

**File logging (add to application-prod.properties):**
```properties
logging.file.name=/var/log/status/application.log
logging.file.max-size=100MB
logging.file.max-history=30
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

**Log rotation:** Spring Boot's embedded Logback handles log rotation automatically when `logging.file.max-history` is set.

---

## 153. JVM Memory Tuning

| JVM Flag | Recommended Value | Notes |
|----------|-----------------|-------|
| `-Xms` | `256m` | Initial heap — set equal to `-Xmx` for stable performance |
| `-Xmx` | `512m–1g` | Max heap — increase if seeing `OutOfMemoryError` |
| `-XX:+UseG1GC` | enabled | G1 GC is best for latency-sensitive workloads |
| `-XX:MaxGCPauseMillis=200` | `200` | Target GC pause time |

**Scheduler thread pool:** The health check thread pool uses `health-check.thread-pool-size` threads (default: 10). Each thread consumes ~512KB of stack by default. For 10 threads: ~5MB overhead.

**Async task pool:** `@Async` tasks use Spring's default task executor. For high email volume, configure a dedicated executor bean with bounded queue size to prevent OOM from unbounded task queuing.

---

## 154. Database Backup Strategy

**Recommended: daily pg_dump**

```bash
# Daily backup script (add to cron)
#!/bin/bash
DATE=$(date +%Y-%m-%d)
pg_dump -U postgres -d uptime -F c -f /backups/uptime_${DATE}.dump

# Keep 30 days of backups
find /backups -name "uptime_*.dump" -mtime +30 -delete
```

**Cron entry:**
```
0 3 * * * /opt/scripts/backup-db.sh >> /var/log/db-backup.log 2>&1
```

**Point-in-time recovery:** Enable WAL archiving in PostgreSQL for point-in-time recovery between full dumps. This requires `wal_level = replica` and `archive_mode = on` in `postgresql.conf`.

**Test restores periodically:**
```bash
pg_restore -U postgres -d uptime_test -F c /backups/uptime_2026-03-28.dump
```

---

## 155. Zero-Downtime Deployment Considerations

### Flyway Migration Safety

Before deploying a new version with migrations:

1. **Verify the migration is backward-compatible:** The new migration must not break the running old version
2. **Additive changes only:** Adding columns with defaults, adding tables — these are safe
3. **Destructive changes:** Dropping/renaming columns must be done in phases:
   - Phase 1: Deploy new code that no longer uses the old column (but don't drop it yet)
   - Phase 2: After confirming deployment is stable, add a migration to drop the old column

### Connection Pool Draining

Before stopping the running instance:
1. Send SIGTERM — Spring Boot registers a shutdown hook that waits for in-flight requests
2. Configure `server.shutdown=graceful` to enable graceful shutdown
3. Set `spring.lifecycle.timeout-per-shutdown-phase=30s` — max wait for active requests to complete

```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

---

## 156. Startup Checklist

Run through this before starting the application in a new environment:

- [ ] **PostgreSQL is running** and the `uptime` database exists
- [ ] **pgcrypto extension** is installed: `SELECT * FROM pg_extension WHERE extname = 'pgcrypto';`
- [ ] **Database credentials** are correct in config / environment variables
- [ ] **JWT secret** is set and is a non-default, randomly generated value
- [ ] **Flyway migrations** will run cleanly (check version history: `SELECT * FROM flyway_schema_history ORDER BY installed_rank;`)
- [ ] **DataInitializer flag** is appropriate: `true` for first run, `false` for subsequent runs
- [ ] **Email configuration** is correct if `app.email.enabled=true`
- [ ] **CORS origins** are restricted to production domains
- [ ] **Log level** is set to `INFO` or `WARN` (not `DEBUG`) in production
- [ ] **Application starts** without errors at `http://your-host:8383/actuator/health`
- [ ] **Login works** at `http://your-host:8383/login` with the admin account
