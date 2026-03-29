# Section 2: Getting Started / Developer Onboarding

---

## 11. Prerequisites

Before running the application you need:

| Requirement | Version | Notes |
|------------|---------|-------|
| **Java JDK** | 17+ | Set `JAVA_HOME` to the JDK root |
| **Maven** | 3.8+ | Ensure `mvn` is on your `PATH` |
| **PostgreSQL** | 14+ | Local or Docker instance; must have `pgcrypto` extension |
| **Git** | any | For cloning the repository |

**Environment variables used in production (set locally as needed):**

| Variable | Maps to property | Example |
|----------|-----------------|---------|
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | `jdbc:postgresql://localhost:5432/uptime` |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` | `your-password` |
| `JWT_SECRET` | `jwt.secret` | `<base64-encoded-256bit-key>` |
| `SPRING_MAIL_HOST` | `spring.mail.host` | `smtp.example.com` |
| `SPRING_MAIL_USERNAME` | `spring.mail.username` | `noreply@example.com` |
| `SPRING_MAIL_PASSWORD` | `spring.mail.password` | `smtp-password` |

---

## 12. Local Development Setup

```bash
# 1. Clone the repository
git clone <repo-url>
cd status

# 2. Create the PostgreSQL database (see item 14)
psql -U postgres -c "CREATE DATABASE uptime;"
psql -U postgres -d uptime -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;"

# 3. Configure credentials
#    Edit src/main/resources/application.properties:
#      spring.datasource.username=postgres
#      spring.datasource.password=your-password
#    Or set environment variables (preferred for production)

# 4. Build
mvn clean install -DskipTests

# 5. Run
mvn spring-boot:run
```

The application starts at **http://localhost:8383**.
On first run, `DataInitializer` creates the default admin account (see item 15).

---

## 13. `application.properties` Reference

All properties are in `src/main/resources/application.properties`. Environment variable equivalents use uppercase with underscores replacing dots (e.g., `spring.datasource.url` → `SPRING_DATASOURCE_URL`).

### Application Identity

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.application.name` | String | `status` | Application name used in logs and monitoring |

### Database

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.datasource.url` | String | `jdbc:postgresql://localhost:5432/uptime` | JDBC connection URL |
| `spring.datasource.username` | String | `postgres` | Database user |
| `spring.datasource.password` | String | _(set locally)_ | Database password — use secrets manager in production |
| `spring.datasource.driver-class-name` | String | `org.postgresql.Driver` | JDBC driver class |

### JPA / Hibernate

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.jpa.hibernate.ddl-auto` | Enum | `validate` | Schema management: `validate` for production, `create-drop` for tests only |
| `spring.jpa.show-sql` | Boolean | `false` | Log all SQL to console; keep false in production |
| `spring.jpa.properties.hibernate.dialect` | String | `PostgreSQLDialect` | Hibernate dialect for SQL generation |

### Flyway

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.flyway.locations` | String | `classpath:db/migration` | Migration script location |
| `spring.flyway.baseline-on-migrate` | Boolean | `true` | Create baseline for existing non-Flyway databases |
| `spring.flyway.ignore-missing-migrations` | Boolean | `true` | Allow gaps in migration version numbers |

### JWT

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `jwt.secret` | String | _(base64 key)_ | HMAC-SHA256 signing key. Replace in production. |
| `jwt.expiration` | Integer (ms) | `86400000` | Access token expiry: 24 hours |
| `jwt.refresh.expiration` | Integer (ms) | `604800000` | Refresh token expiry: 7 days |

### Server

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `server.port` | Integer | `8383` | HTTP listen port |

### Health Checks

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `health-check.enabled` | Boolean | `true` | Master toggle for automated health checks |
| `health-check.scheduler-interval-ms` | Long (ms) | `10000` | How often the scheduler polls for due checks |
| `health-check.default-interval-seconds` | Integer | `60` | Default check frequency per endpoint |
| `health-check.default-timeout-seconds` | Integer | `10` | HTTP timeout per check |
| `health-check.thread-pool-size` | Integer | `10` | Concurrent health check threads |

### Email

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `app.email.enabled` | Boolean | `false` | When false, emails are logged instead of sent |
| `spring.mail.host` | String | `smtp.example.com` | SMTP hostname |
| `spring.mail.port` | Integer | `587` | SMTP port (587 = STARTTLS) |
| `spring.mail.username` | String | — | SMTP sender address |
| `spring.mail.password` | String | — | SMTP password |

### Data & Features

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `data.initializer.enabled` | Boolean | `true` | Creates default admin user on startup |
| `app.registration.enabled` | Boolean | `true` | Allows self-registration via the register form |
| `logs.retention.days` | Integer | `30` | Days to keep raw log entries |

### Build Info (populated by Maven)

| Property | Type | Description |
|----------|------|-------------|
| `app.build.date` | String | `@timestamp@` — replaced at build time |
| `app.copyright` | String | Copyright notice shown in UI footer |

### OpenAPI

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `springdoc.swagger-ui.enabled` | Boolean | `false` | Enable Swagger UI at `/swagger-ui.html` |
| `springdoc.api-docs.enabled` | Boolean | `true` | Expose OpenAPI JSON at `/v3/api-docs` |

---

## 14. Database Setup

```sql
-- Connect as superuser
psql -U postgres

-- Create the database
CREATE DATABASE uptime;

-- Connect to the database
\c uptime

-- Enable pgcrypto extension (required for gen_random_uuid())
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Create application user (optional but recommended)
CREATE USER status_app WITH PASSWORD 'your-password';
GRANT ALL PRIVILEGES ON DATABASE uptime TO status_app;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO status_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO status_app;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO status_app;
```

Flyway will create all tables on first startup via the migration scripts in `src/main/resources/db/migration/`.

---

## 15. First-Run Behaviour

On startup, if `data.initializer.enabled=true` (default), `DataInitializer` runs as a `CommandLineRunner` and:

1. **Checks** if the `admin` user already exists — if yes, skips entirely.
2. **Creates** (or retrieves) a `Tenant` named `"Default Tenant"`.
3. **Creates** (or retrieves) an `Organization` named `"Default Organization"` under that tenant.
4. **Creates** user `admin` with password `admin`, email `admin@status.local`, role `ADMIN`, assigned to the default organisation.
5. **Creates** user `superadmin` with password `superadmin`, email `superadmin@status.local`, role `SUPERADMIN` — not assigned to any organisation (cross-tenant access).

**Security note:** Change both default passwords immediately after first login. Set `data.initializer.enabled=false` in production after initial setup.

---

## 16. Running the Application

```bash
# Standard development run
mvn spring-boot:run

# With a specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Via the packaged JAR
java -jar target/status-*.jar

# With external configuration
java -jar target/status-*.jar --spring.config.location=file:/etc/status/application.properties
```

**Application URL:** http://localhost:8383
**Admin login:** http://localhost:8383/login
**Health endpoint:** http://localhost:8383/actuator/health
**OpenAPI spec:** http://localhost:8383/v3/api-docs

---

## 17. Building a Production JAR

```bash
# Full build with tests
mvn clean package

# Skip tests (faster, use only when confident)
mvn clean package -DskipTests

# The JAR will be at:
target/status-<version>.jar

# Run the JAR
java -Xms256m -Xmx512m \
     -jar target/status-<version>.jar \
     --spring.config.location=file:/etc/status/application.properties \
     --spring.profiles.active=prod
```

The JAR is a self-contained fat JAR (Spring Boot executable) including embedded Tomcat. No external servlet container required.

---

## 18. Skipping Tests

```bash
mvn clean install -DskipTests
```

Use `-DskipTests` when:
- You are iterating on code and tests are not yet updated
- You need a fast build to check compilation only

**Never use `-DskipTests` in CI/CD pipelines.** Tests catch regressions that would otherwise reach production. If tests are failing in CI, fix the root cause rather than skipping tests.

---

## 19. IDE Setup

### IntelliJ IDEA (recommended)

1. **Open project:** File → Open → select the `pom.xml`
2. **JDK:** File → Project Structure → Project SDK → Java 17+
3. **Run configuration:** Add → Spring Boot → Main class: `org.automatize.status.StatusApplication`
4. **Plugins to install:**
   - Lombok (if used)
   - Spring Boot
   - Thymeleaf
   - Database Tools (for PostgreSQL introspection)
5. **Database panel:** Add a PostgreSQL data source pointing to `localhost:5432/uptime`

### VS Code

1. **Extensions:** Extension Pack for Java, Spring Boot Extension Pack, Thymeleaf Snippets
2. **Launch config:** Create `.vscode/launch.json` with a Spring Boot launch
3. **Settings:** Set `java.configuration.maven.globalSettings` to your Maven settings if behind a corporate proxy

---

## 20. Git Branching Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Stable, deployable code; target for PRs |
| `master` | Legacy main branch (being migrated to `main`) |
| `feature/*` | New feature development |
| `bugfix/*` | Bug fixes |
| `hotfix/*` | Production hotfixes |

**PR conventions:**
- PRs target `main`
- Squash or merge commits are acceptable
- Include a brief description of what changed and why

**Commit message format:**
```
<type>: <short summary>

<optional body>
```
Types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`

Example: `feat: add log retention scheduler with configurable retention period`
