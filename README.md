# Status Monitoring Platform

> **License:** This project is licensed under the [Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)](https://creativecommons.org/licenses/by-nc/4.0/). Free for non-commercial use. For commercial licensing, please contact [Tim De Smedt](https://www.linkedin.com/in/timdesmedta/) or visit [A4IT.BE](https://a4it.be).

A comprehensive, multi-tenant status monitoring solution built with Spring Boot. Monitor platform uptime, manage incidents, schedule maintenance windows, and provide public status pages for your services.

![Java](https://img.shields.io/badge/Java-25-orange)
![GraalVM](https://img.shields.io/badge/GraalVM-25-red)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue)
![License](https://img.shields.io/badge/License-CC%20BY--NC%204.0-lightgrey)

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
| Java | 25 | Runtime |
| GraalVM | 25 | Native compilation |
| Spring Boot | 4 | Application framework |
| Spring Security | 7 | Authentication & authorization |
| Spring Data JPA | - | Data persistence |
| PostgreSQL | 17 | Database |
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

## Getting Started

### Prerequisites

- Java 25 (GraalVM 25 recommended)
- Maven 3.8+
- PostgreSQL 17+

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

## Security

### Role-Based Access Control

| Role | Permissions |
|------|-------------|
| `ADMIN` | Full system access, user management |
| `MANAGER` | Manage platforms, incidents, maintenance |
| `USER` | View and create incidents |
| `VIEWER` | Read-only access |

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

This project is licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0) - see the [LICENSE](LICENSE.md) file for details.

## Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Tabler.io](https://tabler.io/) - Admin dashboard template
- [Bootstrap 5](https://getbootstrap.com/)

---

**Made in Europe**

Developed by [Tim De Smedt](https://www.linkedin.com/in/timdesmedta/) | [A4IT.BE](https://a4it.be)
