# Status Monitoring Platform

A comprehensive, multi-tenant status monitoring solution built with Spring Boot. Monitor platform uptime, manage incidents, schedule maintenance windows, and provide public status pages for your services.

![Java](https://img.shields.io/badge/Java-17+-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

## Overview

Status Monitoring Platform is a production-ready application designed to track service availability across multiple platforms, log incidents with cause analysis, schedule maintenance windows, and display real-time status information on public status pages.

### Key Features

- **Multi-Platform Monitoring** - Hierarchical structure (Platform → App → Component) for comprehensive service tracking
- **Automated Health Checks** - Support for PING, HTTP GET, Spring Boot Actuator, and TCP port checks
- **Incident Management** - Full incident lifecycle tracking with updates, severity levels, and automatic notifications
- **Maintenance Windows** - Schedule and communicate planned maintenance to users
- **Public Status Pages** - Beautiful, no-auth-required status pages for end users
- **Multi-Tenant Architecture** - Tenant → Organization → User hierarchy with role-based access control
- **Real-Time Notifications** - Email notifications for incidents and status changes
- **Event Logging** - API key authenticated event logging from monitored platforms
- **Comprehensive Admin Dashboard** - Full management interface for all platform operations

## Technology Stack

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17+ | Runtime |
| Spring Boot | 3.2+ | Application framework |
| Spring Security | 6 | Authentication & authorization |
| Spring Data JPA | - | Data persistence |
| PostgreSQL | 15+ | Database |
| Flyway | - | Database migrations |
| JWT (io.jsonwebtoken) | 0.12.3 | Token-based authentication |
| OpenAPI 3 | - | API documentation |
| Maven | 3.8+ | Build tool |

### Frontend
| Technology | Purpose |
|------------|---------|
| Thymeleaf | Server-side templating |
| Bootstrap 5 | CSS framework |
| Tabler.io | Admin UI template |
| Vanilla JavaScript (ES6+) | Client-side logic |

> **Note:** All frontend assets are served locally - no CDN dependencies

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend        │    │   Database      │
│                 │    │                  │    │                 │
│ • Thymeleaf     │◄──►│ • MVC Controllers│◄──►│ • PostgreSQL    │
│ • Bootstrap 5   │    │ • REST APIs      │    │ • Flyway        │
│ • Tabler.io     │    │ • JWT Security   │    │   Migrations    │
│ • Vanilla JS    │    │ • Services       │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │
                              ▼
                      ┌──────────────────┐
                      │ Health Check     │
                      │ Engine           │
                      │                  │
                      │ • PING           │
                      │ • HTTP GET       │
                      │ • Spring Actuator│
                      │ • TCP Port       │
                      └──────────────────┘
```

### Data Model Hierarchy

```
Tenant
  └── Organization
        └── User (ADMIN, MANAGER, USER, VIEWER)

StatusPlatform
  └── StatusApp (with API Key)
        └── StatusComponent
              └── Health Check Config
              └── Uptime History

StatusIncident
  └── StatusIncidentUpdate
  └── Affected Components

StatusMaintenance
  └── Affected Components

NotificationSubscriber
PlatformEvent
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL 15+

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/status-monitoring.git
   cd status-monitoring
   ```

2. **Configure the database**

   Create a PostgreSQL database:
   ```sql
   CREATE DATABASE uptime;
   ```

3. **Update application properties**

   Edit `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/uptime
   spring.datasource.username=your_username
   spring.datasource.password=your_password

   # JWT Configuration (use your own secret)
   jwt.secret=your-base64-encoded-secret-key
   jwt.expiration=86400000
   jwt.refresh-expiration=604800000
   ```

4. **Build and run**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

5. **Access the application**

   | URL | Description |
   |-----|-------------|
   | `http://localhost:8080/` | Public Status Page |
   | `http://localhost:8080/admin` | Admin Dashboard |
   | `http://localhost:8080/login` | Login Page |
   | `http://localhost:8080/swagger-ui.html` | API Documentation |

## Configuration

### Application Properties

| Property | Description | Default |
|----------|-------------|---------|
| `jwt.secret` | JWT signing secret (Base64 encoded) | Required |
| `jwt.expiration` | Access token expiration (ms) | 86400000 (24h) |
| `jwt.refresh-expiration` | Refresh token expiration (ms) | 604800000 (7d) |
| `app.registration.enabled` | Enable user registration | true |
| `health.check.scheduler.enabled` | Enable automated health checks | true |
| `health.check.scheduler.interval` | Health check interval (ms) | 60000 |

### Health Check Configuration

Each platform, app, or component can be configured with its own health check:

| Field | Description | Default |
|-------|-------------|---------|
| `healthCheckEnabled` | Enable health checking | false |
| `healthCheckType` | PING, HTTP_GET, SPRING_BOOT_HEALTH, TCP_PORT | HTTP_GET |
| `healthCheckUrl` | URL/endpoint to check | - |
| `healthCheckInterval` | Check interval (seconds) | 60 |
| `healthCheckTimeout` | Request timeout (seconds) | 10 |
| `consecutiveFailuresThreshold` | Failures before marking down | 3 |
| `expectedStatusCode` | Expected HTTP status code | 200 |

### Email Notifications

```properties
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=your-email
spring.mail.password=your-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

## API Reference

### Authentication Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | User login, returns JWT tokens |
| POST | `/api/auth/register` | User registration |
| POST | `/api/auth/refresh` | Refresh access token |
| POST | `/api/auth/logout` | Invalidate session |
| GET | `/api/auth/me` | Get current user info |

### Platform Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/status-platforms` | List platforms (paginated) |
| GET | `/api/status-platforms/all` | Get all platforms |
| GET | `/api/status-platforms/{id}` | Get platform by ID |
| GET | `/api/status-platforms/slug/{slug}` | Get platform by slug |
| POST | `/api/status-platforms` | Create platform |
| PUT | `/api/status-platforms/{id}` | Update platform |
| DELETE | `/api/status-platforms/{id}` | Delete platform |
| PATCH | `/api/status-platforms/{id}/status` | Update status |

### Application Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/status-apps` | List applications |
| GET | `/api/status-apps/{id}` | Get application |
| POST | `/api/status-apps` | Create application |
| PUT | `/api/status-apps/{id}` | Update application |
| DELETE | `/api/status-apps/{id}` | Delete application |

### Component Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/status-components` | List components |
| GET | `/api/status-components/{id}` | Get component |
| POST | `/api/status-components` | Create component |
| PUT | `/api/status-components/{id}` | Update component |
| DELETE | `/api/status-components/{id}` | Delete component |

### Incident Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/incidents` | List incidents (with filtering) |
| GET | `/api/incidents/active` | Get active incidents |
| GET | `/api/incidents/{id}` | Get incident details |
| POST | `/api/incidents` | Create incident |
| PUT | `/api/incidents/{id}` | Update incident |
| DELETE | `/api/incidents/{id}` | Delete incident |
| POST | `/api/incidents/{id}/updates` | Add incident update |
| GET | `/api/incidents/{id}/updates` | Get incident updates |
| PATCH | `/api/incidents/{id}/resolve` | Resolve incident |

### Maintenance Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/maintenance` | List maintenance windows |
| GET | `/api/maintenance/{id}` | Get maintenance details |
| POST | `/api/maintenance` | Create maintenance |
| PUT | `/api/maintenance/{id}` | Update maintenance |
| DELETE | `/api/maintenance/{id}` | Delete maintenance |

### Public Endpoints (No Authentication)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/public/status` | Public status summary |
| GET | `/api/public/incidents` | Public incidents list |
| GET | `/api/public/maintenance` | Public maintenance list |
| GET | `/api/public/uptime/{slug}` | Public uptime history |

### Event Logging (API Key Authentication)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/events/log` | Log platform event |
| GET | `/api/events` | List events (admin only) |

**Example event logging request:**
```bash
curl -X POST http://localhost:8080/api/events/log \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{
    "appId": 1,
    "severity": "ERROR",
    "message": "Database connection timeout",
    "details": "Connection pool exhausted after 30s"
  }'
```

### Subscriber Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/subscribers` | List subscribers |
| POST | `/api/subscribers` | Add subscriber |
| DELETE | `/api/subscribers/{id}` | Remove subscriber |

## Status Values

### Component/Platform Status

| Status | Description |
|--------|-------------|
| `OPERATIONAL` | Service is running normally |
| `DEGRADED` | Service is experiencing performance issues |
| `PARTIAL_OUTAGE` | Some functionality is unavailable |
| `MAJOR_OUTAGE` | Service is completely unavailable |

### Incident Status (Lifecycle)

| Status | Description |
|--------|-------------|
| `INVESTIGATING` | Issue is being investigated |
| `IDENTIFIED` | Root cause has been identified |
| `MONITORING` | Fix applied, monitoring for stability |
| `RESOLVED` | Issue has been resolved |

### Incident Severity

| Severity | Description |
|----------|-------------|
| `CRITICAL` | Complete service failure |
| `MAJOR` | Significant functionality impacted |
| `MINOR` | Limited impact on functionality |
| `MAINTENANCE` | Planned maintenance activity |

### Event Severity

| Severity | Description |
|----------|-------------|
| `INFO` | Informational event |
| `WARNING` | Warning condition |
| `ERROR` | Error occurred |
| `CRITICAL` | Critical failure |

## Project Structure

```
src/
├── main/
│   ├── java/org/automatize/status/
│   │   ├── api/
│   │   │   ├── request/              # API request classes
│   │   │   └── response/             # API response classes
│   │   ├── config/
│   │   │   ├── SecurityConfig.java   # JWT & CORS configuration
│   │   │   ├── WebConfig.java        # Web configuration
│   │   │   ├── WebMvcConfig.java     # MVC configuration
│   │   │   └── SwaggerConfig.java    # OpenAPI configuration
│   │   ├── controllers/
│   │   │   ├── api/                  # REST API controllers
│   │   │   ├── AdminController.java  # Admin MVC controller
│   │   │   ├── AuthenticationController.java
│   │   │   └── PublicController.java # Public pages controller
│   │   ├── models/                   # JPA entities (14 entities)
│   │   ├── repositories/             # Spring Data repositories
│   │   ├── security/
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── JwtTokenProvider.java
│   │   │   └── UserDetailsServiceImpl.java
│   │   ├── services/                 # Business logic (16 services)
│   │   │   ├── AuthService.java
│   │   │   ├── HealthCheckService.java
│   │   │   ├── HealthCheckScheduler.java
│   │   │   ├── StatusPlatformService.java
│   │   │   ├── StatusIncidentService.java
│   │   │   └── ...
│   │   └── StatusApplication.java
│   └── resources/
│       ├── db/migration/             # Flyway migrations (V1-V9)
│       ├── static/
│       │   ├── css/                  # Bootstrap, Tabler, custom styles
│       │   ├── js/
│       │   │   ├── admin/            # Admin dashboard scripts
│       │   │   ├── public/           # Public status page scripts
│       │   │   ├── authentication/   # Login/register scripts
│       │   │   ├── shared/           # Common utilities
│       │   │   │   ├── api.js        # API client
│       │   │   │   ├── auth.js       # Auth utilities
│       │   │   │   └── notifications.js
│       │   │   └── vendor/           # Third-party libraries
│       │   └── images/
│       └── templates/
│           ├── admin/                # Admin Thymeleaf templates
│           ├── public/               # Public status templates
│           └── authentication/       # Auth page templates
└── test/
```

## Development Commands

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Package as JAR
mvn clean package

# Run the packaged JAR
java -jar target/status-*.jar

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=StatusApplicationTests

# Build without tests
mvn clean install -DskipTests

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Security

### Authentication Flow

```
1. Client POSTs credentials to /api/auth/login
          │
          ▼
2. Server validates credentials
          │
          ▼
3. Server returns JWT access token + refresh token
          │
          ▼
4. Client includes "Authorization: Bearer <token>" in requests
          │
          ▼
5. JwtAuthenticationFilter validates token on each request
          │
          ▼
6. When access token expires, use /api/auth/refresh
```

### Role-Based Access Control

| Role | Permissions |
|------|-------------|
| `ADMIN` | Full system access, user management |
| `MANAGER` | Manage platforms, incidents, maintenance |
| `USER` | View and create incidents |
| `VIEWER` | Read-only access |

### Public Endpoints (No Auth Required)

- `/`, `/login`, `/logout`, `/register`, `/forgot-password`
- `/api/auth/**` - Authentication endpoints
- `/api/public/**` - Public status data
- `/api/events/log` - Event logging (API key validated)
- `/admin/**`, `/incidents/**`, `/maintenance/**`, `/history`
- `/static/**`, `/css/**`, `/js/**`, `/images/**`

## Database Migrations

Migrations are managed by Flyway and run automatically on startup:

| Version | Description |
|---------|-------------|
| V1 | Initial schema |
| V2 | Health check fields |
| V3 | Uptime history |
| V4 | Notification subscribers |
| V5 | Status platforms and events |
| V6 | API keys |
| V7 | Generate existing API keys |
| V8 | Platform health check fields |
| V9 | Platform ID to uptime history |

Migration files are located in `src/main/resources/db/migration/`

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Guidelines

- Follow existing code patterns and conventions
- Services should work with model classes directly (not request/response classes)
- Do not use Lombok `@Builder` pattern
- Use existing packages, do not create new ones
- MVC controllers serve templates only, no business logic
- All business logic in service layer
- REST controllers use request/response classes from `api/` package

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Tabler.io](https://tabler.io/) - Admin dashboard template
- [Bootstrap 5](https://getbootstrap.com/)

---

**Built with Spring Boot**
