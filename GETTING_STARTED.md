# Getting Started — Status Monitoring Solution

This guide walks you through setting up and running the Status Monitoring Solution from scratch.

## Prerequisites

| Requirement | Version |
|---|---|
| Java (JDK) | 25+ |
| Maven | 3.8+ |
| PostgreSQL | 14+ |

---

## 1. Database Setup

### Install PostgreSQL

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
```

**macOS (Homebrew):**
```bash
brew install postgresql@14
brew services start postgresql@14
```

**Windows:** Download from [postgresql.org](https://www.postgresql.org/download/windows/)

### Create the Database

Connect as the PostgreSQL superuser and create the database:

```bash
sudo -u postgres psql
```

```sql
CREATE DATABASE uptime;
CREATE USER statususer WITH ENCRYPTED PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE uptime TO statususer;

\c uptime

GRANT ALL ON SCHEMA public TO statususer;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO statususer;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO statususer;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO statususer;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO statususer;

\q
```

> The application uses Flyway to manage the full schema automatically on startup — no manual SQL execution needed.

---

## 2. Configure `application.properties`

Edit `src/main/resources/application.properties` before building. At minimum, update the database connection:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/uptime
spring.datasource.username=statususer
spring.datasource.password=your_secure_password
```

### Generate a Secure JWT Secret

The JWT secret must be Base64-encoded and at least 256 bits:

```bash
openssl rand -base64 32
```

Paste the output into:

```properties
jwt.secret=<your-base64-output>
```

### Server Port

Default is `8383`. Change if needed:

```properties
server.port=8383
```

---

## 3. Build

```bash
mvn clean package -DskipTests
```

The JAR is created at `target/status-<version>.jar`.

---

## 4. Run

**Development (Maven):**
```bash
mvn spring-boot:run
```

**Production (JAR):**
```bash
java -jar target/status-*.jar
```

**With external config:**
```bash
java -jar target/status-*.jar --spring.config.location=/etc/status/application.properties
```

---

## 5. First-Run Setup Wizard

On a fresh installation with `app.setup.completed=false`, all HTTP traffic is redirected to the setup wizard at `/setup`. Navigate to:

```
http://localhost:8383/setup
```

The wizard completes in 6 steps:

| Step | Action |
|---|---|
| 1 | Welcome and overview |
| 2 | Verify database connection and Flyway migration status |
| 3 | Review and save critical application properties |
| 4 | Create your first **Tenant** |
| 5 | Create your first **Organization** under the tenant |
| 6 | Create the **SUPERADMIN** user account |

When step 6 finishes, `app.setup.completed=true` is written to `application.properties` and redirects stop immediately — no restart required.

### Re-running the Wizard

If you need to re-run setup (e.g. fresh database):

1. Set `app.setup.completed=false` in `application.properties`
2. Restart the application
3. Navigate to `http://localhost:8383/setup`

> The wizard detects existing tenants and organizations and skips those creation steps if data is already present.

---

## 6. Log In

After setup completes, navigate to:

```
http://localhost:8383/login
```

Use the SUPERADMIN credentials created in step 6 of the wizard.

---

## Production Checklist

Before going live, update these settings in `application.properties`:

```properties
# Disable demo data seeding
data.initializer.enabled=false

# Disable Swagger UI
springdoc.swagger-ui.enabled=false

# Reduce log verbosity
logging.level.org.springframework.security=WARN
logging.level.org.automatize.status=INFO

# Restrict CORS to your actual domain
app.cors.allowed-origins=https://status.yourdomain.com

# Strong unique JWT secret (never reuse the default)
jwt.secret=<your-production-base64-secret>

# Production database
spring.datasource.url=jdbc:postgresql://db-host:5432/uptime
spring.datasource.username=statususer
spring.datasource.password=<your-db-password>
```

---

## Running as a Systemd Service (Linux)

Create `/etc/systemd/system/status.service`:

```ini
[Unit]
Description=Status Monitoring Solution
After=network.target postgresql.service

[Service]
Type=simple
User=status
Group=status
WorkingDirectory=/opt/status
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar /opt/status/status.jar --spring.config.location=/etc/status/application.properties
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable status
sudo systemctl start status
sudo systemctl status status
```

---

## Email Notifications (Optional)

To enable email alerts and password reset:

```properties
app.email.enabled=true
spring.mail.host=smtp.yourdomain.com
spring.mail.port=587
spring.mail.username=noreply@yourdomain.com
spring.mail.password=your-smtp-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

---

## Database Migrations

Schema is managed by Flyway and runs automatically on startup. Migration files are in `src/main/resources/db/migration/`.

| Migration | Description |
|---|---|
| V1 | Base schema — tenants, organizations, users, platforms, components, incidents, maintenance, health checks |
| V2 | Logs Hub tables |
| V11 | Drop legacy alert rules |
| V12 | Log API keys |
| V13 | Logs and metrics tables |
| V14 | Process mining data retention |
| V15 | API key hash column |
| V16 | Performance indexes |
| V17 | Fix log API key hash |
| V18 | BRIN indexes on timeseries columns |

To inspect applied migrations:

```bash
psql -U statususer -d uptime -c "SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;"
```

---

## Configuration Reference

| Property | Description | Default |
|---|---|---|
| `app.setup.completed` | Whether the setup wizard has run | `false` |
| `data.initializer.enabled` | Seed default admin on startup | `true` |
| `spring.datasource.url` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/uptime` |
| `spring.datasource.username` | Database username | `postgres` |
| `spring.datasource.password` | Database password | — |
| `server.port` | HTTP port | `8383` |
| `jwt.secret` | Base64-encoded JWT signing key | — |
| `jwt.expiration` | Access token expiry (ms) | `86400000` (24h) |
| `jwt.refresh.expiration` | Refresh token expiry (ms) | `604800000` (7d) |
| `app.registration.enabled` | Allow user self-registration | `true` |
| `app.cors.allowed-origins` | Allowed CORS origins | `http://localhost:8383` |
| `health-check.enabled` | Enable automated health checks | `true` |
| `health-check.scheduler-interval-ms` | Scheduler poll interval (ms) | `10000` |
| `health-check.default-interval-seconds` | Default check frequency (s) | `60` |
| `health-check.default-timeout-seconds` | Default HTTP timeout (s) | `10` |
| `health-check.thread-pool-size` | Concurrent health check threads | `10` |
| `app.email.enabled` | Enable SMTP email | `false` |
| `logs.retention.days` | Days to retain log entries | `30` |

---

## API Documentation

The OpenAPI spec is available at:

```
http://localhost:8383/v3/api-docs
```

Enable Swagger UI for development:

```properties
springdoc.swagger-ui.enabled=true
```

Then access at: `http://localhost:8383/swagger-ui.html`

---

## Troubleshooting

**PostgreSQL connection refused**
```bash
sudo systemctl status postgresql
sudo netstat -tlnp | grep 5432
```
Check that `pg_hba.conf` allows connections from your application host.

**Flyway migration errors**
```bash
psql -U statususer -d uptime -c "SELECT * FROM flyway_schema_history;"
```

**JWT authentication failures**
Ensure `jwt.secret` is Base64-encoded, at least 256 bits (32 bytes before encoding), and consistent across restarts.

**Port already in use**
```bash
sudo lsof -i :8383
# Or override the port at runtime:
java -jar target/status-*.jar --server.port=9090
```
