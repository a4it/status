# Section 1: Architecture & Overview

---

## 1. System Overview

The **Status Monitoring System** is a multi-tenant uptime and observability platform built on Spring Boot 3. It allows organisations to:

- Monitor the availability and health of platforms, applications, and components via automated HTTP/TCP health checks
- Publish and track incidents and scheduled maintenance windows on a public status page
- Ingest and analyse structured logs from external services (Logs Hub)
- Evaluate alert rules against incoming log streams and fire notifications
- Track uptime history and derive SLA metrics over time

**Intended audience:** Developers maintaining or extending the codebase, DevOps engineers deploying and operating the system, and architects reviewing the design.

---

## 2. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         Browser / API Client                │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP (port 8383)
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                  Spring Boot Application                    │
│                                                             │
│  ┌──────────────────┐      ┌──────────────────────────────┐ │
│  │   MVC Layer      │      │      REST API Layer          │ │
│  │                  │      │                              │ │
│  │  AdminController │      │  /api/auth/**                │ │
│  │  AuthController  │      │  /api/status-platforms/**    │ │
│  │  PublicController│      │  /api/status-apps/**         │ │
│  │                  │      │  /api/components/**          │ │
│  │  Thymeleaf       │      │  /api/incidents/**           │ │
│  │  Templates       │      │  /api/logs/**                │ │
│  └──────────────────┘      │  /api/public/**              │ │
│                            └──────────────────────────────┘ │
│                                       │                     │
│                    ┌──────────────────▼──────────────────┐  │
│                    │         Service Layer                │  │
│                    │  Business logic, validation,         │  │
│                    │  scheduling, email, async tasks      │  │
│                    └──────────────────┬──────────────────┘  │
│                                       │                     │
│                    ┌──────────────────▼──────────────────┐  │
│                    │       Repository Layer (JPA)         │  │
│                    │  Spring Data repositories per entity │  │
│                    └──────────────────┬──────────────────┘  │
└───────────────────────────────────────┼─────────────────────┘
                                        │ JDBC
                         ┌──────────────▼─────────────┐
                         │       PostgreSQL            │
                         │       database: uptime      │
                         └────────────────────────────┘
```

### Key Layers

| Layer | Package | Responsibility |
|-------|---------|----------------|
| MVC Controllers | `controllers/` | Serve Thymeleaf templates, handle redirects |
| REST API Controllers | `controllers/api/` | JSON API endpoints, request/response mapping |
| Service Layer | `services/` | Business logic, orchestration, scheduling |
| Repository Layer | `repositories/` | Database access via Spring Data JPA |
| Security | `security/` | JWT generation/validation, auth filters |
| Config | `config/` | Spring beans, Flyway, OpenAPI, data init |
| Models | `models/` | JPA entities |
| API Contracts | `api/request/`, `api/response/` | Request/response POJOs |

---

## 3. Multi-Tenant Data Model

```
Tenant
  └── Organization (1..N)
        └── User (1..N)
        └── StatusPlatform (1..N)
              └── StatusApp (1..N)
                    └── StatusComponent (1..N)
                    └── StatusIncident (1..N)
                          └── StatusIncidentUpdate (1..N)
                          └── StatusIncidentComponent (junction)
                    └── StatusMaintenance (1..N)
                          └── StatusMaintenanceComponent (junction)
                    └── StatusUptimeHistory (1..N)
                    └── NotificationSubscriber (1..N)
                    └── HealthCheckSettings (0..1)
              └── PlatformEvent (1..N)
              └── Log (1..N)
              └── LogMetric (1..N)
              └── LogApiKey (1..N)
              └── AlertRule (1..N)
              └── DropRule (1..N)
```

**Rules:**
- A **Tenant** is the top-level isolation boundary. Data never crosses tenant boundaries.
- An **Organization** belongs to one Tenant. All operational entities (platforms, users) belong to an Organisation.
- A **SUPERADMIN** user belongs to no organisation and can switch context across tenants/orgs.
- A **User** with role `ADMIN` or `MANAGER` or `USER` is always bound to exactly one Organisation.

---

## 4. Request Lifecycle

```
Browser / API Client
       │
       │  HTTP request (GET /admin/dashboard or POST /api/status-platforms)
       ▼
JwtAuthenticationFilter
  ├── Extracts Bearer token from Authorization header
  ├── Validates signature and expiration via JwtUtils.validateJwtToken()
  ├── Loads UserPrincipal via CustomUserDetailsService
  └── Sets SecurityContextHolder authentication
       │
       ▼
SecurityFilterChain URL matcher
  ├── /api/auth/** → skip auth (permitAll)
  ├── /api/public/** → skip auth (permitAll)
  └── everything else → must be authenticated
       │
       ▼
Controller (@RestController or @Controller)
  ├── REST: deserialise request body → Request POJO
  ├── MVC:  @AuthenticationPrincipal UserPrincipal
  └── Delegate to Service
       │
       ▼
Service
  ├── Business validation
  ├── JPA repository calls
  └── Return entity / result
       │
       ▼
Controller
  ├── REST: wrap in Response POJO → JSON
  └── MVC:  add model attributes → Thymeleaf template
       │
       ▼
Client receives JSON or HTML
```

---

## 5. MVC vs REST API Separation

The codebase enforces a strict architectural split between MVC controllers and REST API controllers.

### MVC Controllers (`controllers/`)
**Only do:**
- Serve a Thymeleaf template via `return "templateName"`
- Add `application.properties` values (app name, build info) to the model
- Validate JWT tokens from cookies and redirect to login if missing
- Return redirects on auth failure

**Never do:**
- Query the database
- Call service methods that return data
- Execute business logic
- Return `@ResponseBody` / JSON

### REST API Controllers (`controllers/api/`)
**Always:**
- Annotated with `@RestController`
- Mapped under `/api/...`
- Accept input via a `Request` class from `api/request/`
- Return output via a `Response` class from `api/response/`
- Delegate all logic to the service layer
- Handle errors with standardised HTTP status codes

**Why this split?** The frontend is a single-page-like application that fetches all data via REST APIs from JavaScript. Thymeleaf templates only provide the HTML shell and configuration. This keeps templates stateless and cacheable, enables easy API testing, and prevents logic duplication.

---

## 6. Technology Stack Decision Log

| Technology | Why chosen |
|------------|-----------|
| **Spring Boot 3.2+** | Industry-standard Java framework with embedded Tomcat, auto-configuration, and rich ecosystem. Reduces boilerplate dramatically. |
| **Spring Security 6 + JWT** | Stateless API authentication without server-side session storage. JWT enables horizontal scaling and eliminates session affinity requirements. |
| **Thymeleaf** | Server-side rendering with natural templates; easy to integrate with Spring MVC. No separate frontend build toolchain needed. |
| **Tabler.io (Bootstrap 5)** | Professional admin UI template that provides consistent, accessible components without custom CSS authoring. |
| **PostgreSQL** | Mature relational database with strong JSON support, full-text search, and `gen_random_uuid()` via pgcrypto. Required for multi-tenant isolation via row-level filtering. |
| **Flyway** | Database migration tool that version-controls schema changes. Automatically applies migrations on startup. |
| **io.jsonwebtoken (JJWT)** | Lightweight, well-maintained JWT library for Java. Supports HS256 signing out of the box. |
| **Vanilla ES6+ JavaScript** | No build toolchain complexity. Runs directly in browser. Easier for backend developers to contribute without npm/webpack knowledge. |
| **Maven** | Dependency management and build lifecycle. Widely understood in Java teams; build.properties for version tracking. |

---

## 7. Module Dependency Map

```
StatusApplication
  └── config.*
        ├── SecurityConfig → security.*
        ├── DataInitializer → services.UserService, services.TenantService, services.OrganizationService
        ├── ProcessMiningDataInitializer → services.ProcessMiningService
        └── OpenApiConfig, SwaggerConfig, WebConfig, WebMvcConfig

controllers.AdminController → (no service calls; template-only)
controllers.AuthenticationController → (no service calls; template-only)
controllers.PublicController → (no service calls; template-only)

controllers.api.* → services.*
services.* → repositories.*
repositories.* → models.*

security.JwtAuthenticationFilter → security.JwtUtils, security.CustomUserDetailsService
security.CustomUserDetailsService → repositories.UserRepository
```

**Key rule:** The dependency arrow is always `controller → service → repository → model`. No layer skips another.

---

## 8. Environment Topology

| Environment | DB | Port | Profiles | Notes |
|-------------|-----|------|----------|-------|
| **Development** | Local PostgreSQL `uptime` | 8383 | (default) | SQL logging DEBUG, email disabled, Swagger API docs enabled |
| **Staging** | Dedicated PostgreSQL | 8383 or 80 | `staging` | Mirror of prod; used for integration testing |
| **Production** | Managed PostgreSQL | 80/443 | `prod` | `data.initializer.enabled=false`, email enabled, Swagger UI disabled |

Profile-specific overrides go in `application-{profile}.properties`. Activate with `--spring.profiles.active=prod` at startup.

---

## 9. Async Processing Model

Operations that should not block the request thread are annotated with `@Async`:

| Service | Async operation | Reason |
|---------|-----------------|--------|
| `EmailService` | `sendEmail(...)` | SMTP can be slow; fire-and-forget |
| `IncidentNotificationService` | Subscriber notification dispatch | Fan-out to many emails |
| `HealthCheckService` | Individual HTTP/TCP checks | Parallel execution via thread pool |

The Spring `@EnableAsync` annotation is applied at the application level. The health check scheduler uses a dedicated `ThreadPoolTaskExecutor` with `health-check.thread-pool-size` threads (default: 10) to run checks concurrently without blocking the main scheduler thread.

---

## 10. Scheduled Jobs Inventory

| Class | Method / Trigger | Interval | Purpose |
|-------|-----------------|----------|---------|
| `HealthCheckScheduler` | `@Scheduled(fixedRateString)` | `health-check.scheduler-interval-ms` (default: 10s) | Polls for due health checks and dispatches them |
| `AlertEvaluatorScheduler` | `@Scheduled` | Configured in class | Evaluates alert rules against recent log data |
| `LogMetricScheduler` | `@Scheduled` | Configured in class | Aggregates raw logs into `log_metrics` buckets |
| `LogRetentionScheduler` | `@Scheduled(cron = "0 0 2 * * ?")` | Daily at 02:00 | Deletes log entries older than `logs.retention.days` (default: 30 days) |

All schedulers are conditional on configuration flags (e.g., `health-check.enabled=true`). Disabling the flag prevents the scheduler from dispatching work even if the schedule fires.
