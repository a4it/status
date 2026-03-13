# Getting Started

This guide walks you through setting up and running the Status Monitoring Solution.

## Prerequisites

- **Java 17+** - OpenJDK or Oracle JDK
- **Maven 3.8+** - Build tool
- **PostgreSQL 14+** - Database server

## PostgreSQL Setup

### 1. Install PostgreSQL

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

**Windows:**
Download and install from [postgresql.org](https://www.postgresql.org/download/windows/)

### 2. Create Database and User

Connect to PostgreSQL as the superuser:
```bash
sudo -u postgres psql
```

Create the database:
```sql
CREATE DATABASE uptime;
CREATE USER statususer WITH ENCRYPTED PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE uptime TO statususer;

-- Connect to the uptime database
\c uptime

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO statususer;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO statususer;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO statususer;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO statususer;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO statususer;

-- Enable required extension
CREATE EXTENSION IF NOT EXISTS pgcrypto;

\q
```

### 3. Verify Connection

```bash
psql -h localhost -U statususer -d uptime
```

## Application Configuration

### Using External application.properties

The application supports external configuration files. Create your own `application.properties` outside the project directory.

#### Option 1: Command Line Argument

```bash
java -jar target/status-*.jar --spring.config.location=/path/to/application.properties
```

#### Option 2: External Config Directory

Place `application.properties` in a `config/` directory next to the JAR:

```
/opt/status/
├── status-0.0.52-SNAPSHOT.jar
└── config/
    └── application.properties
```

Run:
```bash
cd /opt/status
java -jar status-*.jar
```

#### Option 3: Environment Variables

Override specific properties using environment variables:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/uptime
export SPRING_DATASOURCE_USERNAME=statususer
export SPRING_DATASOURCE_PASSWORD=your_secure_password
export JWT_SECRET=your_base64_encoded_secret_key

java -jar target/status-*.jar
```

### Sample External application.properties

Create a file at `/etc/status/application.properties`:

```properties
# Application Name
spring.application.name=status

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/uptime
spring.datasource.username=statususer
spring.datasource.password=your_secure_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

# Flyway Configuration
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true

# JWT Configuration
# Generate a secure base64-encoded secret (minimum 256 bits):
# openssl rand -base64 32
jwt.secret=YOUR_BASE64_ENCODED_SECRET_HERE
jwt.expiration=86400000
jwt.refresh.expiration=604800000

# Swagger/OpenAPI Configuration
springdoc.swagger-ui.enabled=false
springdoc.api-docs.enabled=true

# Server Configuration
server.port=8080

# Logging
logging.level.org.springframework.security=WARN
logging.level.org.automatize.status=INFO

# Build Info
app.build.date=@timestamp@
app.copyright=(c) A4IT BV & AUTOMATIZE BV - Developed by Tim De Smedt

# Registration Configuration
app.registration.enabled=true

# Health Check Configuration
health-check.enabled=true
health-check.scheduler-interval-ms=10000
health-check.default-interval-seconds=60
health-check.default-timeout-seconds=10
health-check.thread-pool-size=10

# Email Configuration
app.email.enabled=false
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=noreply@example.com
spring.mail.password=your-email-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

## Building the Application

```bash
# Clone the repository
git clone <repository-url>
cd status

# Build with Maven
mvn clean package

# Skip tests during build (optional)
mvn clean package -DskipTests
```

The JAR file will be created at `target/status-<version>.jar`.

## Running the Application

### Development Mode

```bash
mvn spring-boot:run
```

### Production Mode

```bash
# With default configuration
java -jar target/status-*.jar

# With external configuration
java -jar target/status-*.jar --spring.config.location=/etc/status/application.properties

# With specific profile
java -jar target/status-*.jar --spring.profiles.active=production
```

### As a Systemd Service (Linux)

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

Enable and start:
```bash
sudo systemctl daemon-reload
sudo systemctl enable status
sudo systemctl start status
sudo systemctl status status
```

## Default Admin Account

On first startup, the application automatically creates a default admin account through the `DataInitializer` component.

### Default Credentials

| Field    | Value                |
|----------|----------------------|
| Username | `admin`              |
| Password | `admin`              |
| Email    | `admin@status.local` |
| Role     | `ADMIN`              |

The initializer also creates:
- **Default Tenant**: "Default Tenant"
- **Default Organization**: "Default Organization"

### Securing After First Run

**IMPORTANT**: After successfully logging in with the default account, you should:

1. **Change the admin password** immediately through the user profile settings

2. **Disable the DataInitializer** to prevent it from running on subsequent startups:

   In your `application.properties`:
   ```properties
   # Disable data initialization after first successful run
   data.initializer.enabled=false
   ```

   Or via environment variable:
   ```bash
   export DATA_INITIALIZER_ENABLED=false
   ```

This ensures that even if the database is compromised or reset, the default credentials cannot be re-created automatically.

### How It Works

The `DataInitializer` component (`org.automatize.status.config.DataInitializer`):
- Runs automatically on application startup as a `CommandLineRunner`
- Checks if `data.initializer.enabled=true` (default)
- Checks if an admin user already exists
- If no admin exists, creates the default tenant, organization, and admin user
- Logs the initialization status

Logs to look for:
```
INFO  DataInitializer - Initializing default admin account...
INFO  DataInitializer - Admin account created successfully (username: admin, password: admin)
```

Or when disabled:
```
INFO  DataInitializer - Data initializer is disabled (data.initializer.enabled=false)
```

## Verifying the Installation

1. **Check application health:**
   ```bash
   curl http://localhost:8080/api/health
   ```

2. **Access the web interface:**
   Open `http://localhost:8080` in your browser

3. **Check logs:**
   ```bash
   # If running with systemd
   sudo journalctl -u status -f

   # Or check application logs
   tail -f /var/log/status/application.log
   ```

## Database Migrations

Database schema is managed by Flyway. Migrations run automatically on startup.

Migration files are located in: `src/main/resources/db/migration/`

Current migrations:
- `init.sql` - Base schema (tenants, organizations, users, status apps, components, incidents, maintenance)
- `V2__add_health_check_fields.sql` - Health check configuration
- `V3__add_uptime_history.sql` - Uptime history tracking
- `V4__add_notification_subscribers.sql` - Notification subscribers
- `V5__add_status_platforms_and_events.sql` - Platform and events
- `V6__add_api_keys.sql` - API key management
- `V7__generate_existing_api_keys.sql` - Generate API keys for existing records
- `V8__add_platform_health_check_fields.sql` - Platform health checks
- `V9__add_platform_id_to_uptime_history.sql` - Platform ID in uptime history
- `V10__add_health_check_settings.sql` - Health check settings

## Configuration Reference

| Property | Description | Default |
|----------|-------------|---------|
| `data.initializer.enabled` | Enable/disable default admin account creation | `true` |
| `spring.datasource.url` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/uptime` |
| `spring.datasource.username` | Database username | `postgres` |
| `spring.datasource.password` | Database password | - |
| `server.port` | HTTP port | `8080` |
| `jwt.secret` | Base64-encoded JWT signing key | - |
| `jwt.expiration` | JWT token expiration (ms) | `86400000` (24h) |
| `jwt.refresh.expiration` | Refresh token expiration (ms) | `604800000` (7d) |
| `health-check.enabled` | Enable health check scheduler | `true` |
| `health-check.scheduler-interval-ms` | Scheduler poll interval | `10000` |
| `health-check.default-interval-seconds` | Default check interval | `60` |
| `health-check.default-timeout-seconds` | Default check timeout | `10` |
| `health-check.thread-pool-size` | Health check thread pool | `10` |
| `app.email.enabled` | Enable email notifications | `false` |
| `app.registration.enabled` | Allow user registration | `true` |

## Generating a Secure JWT Secret

```bash
# Generate a 256-bit base64-encoded secret
openssl rand -base64 32
```

Use the output as your `jwt.secret` value.

## Troubleshooting

### Connection refused to PostgreSQL

1. Verify PostgreSQL is running:
   ```bash
   sudo systemctl status postgresql
   ```

2. Check PostgreSQL is listening:
   ```bash
   sudo netstat -tlnp | grep 5432
   ```

3. Verify `pg_hba.conf` allows local connections

### Flyway migration errors

If you encounter migration issues:

```bash
# Connect to database and check Flyway history
psql -U statususer -d uptime -c "SELECT * FROM flyway_schema_history;"
```

### JWT authentication failures

Ensure your JWT secret is:
1. Base64-encoded
2. At least 256 bits (32 bytes before encoding)
3. Consistent across application restarts

### Port already in use

```bash
# Find process using port 8080
sudo lsof -i :8080

# Or change the port
java -jar target/status-*.jar --server.port=9090
```
