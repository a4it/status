# Status Monitoring Solution

A comprehensive Spring Boot application for document processing and transformation management with advanced JWT authentication, modern frontend architecture, and robust multi-tenant capabilities, built according to the document flow data model with enhanced architectural patterns.

## Project Overview

Status monitoring is a monitoring app that will manage the uptime statistics of multiple paltforms and also handle issues, cause analysis etc.

- **Advanced JWT Authentication** - Bearer tokens for API, secure cookies for MVC
- **Multi-tenant Architecture** - Tenant → Organization → User hierarchy with role-based access
- **RESTful API** - Complete CRUD operations with comprehensive Swagger documentation
- **Modern Web Interface** - Bootstrap 5 with Tabler.io template and vanilla JavaScript
- **Audit System** - Comprehensive logging and monitoring capabilities

## Technology Stack

- **Backend**: Spring Boot 3.2+, Spring Security 6, Spring Data JPA
- **Authentication**: JWT with io.jsonwebtoken, secure session management
- **Database**: PostgreSQL (production), PostgreSQL (development)
- **Frontend**: Thymeleaf, Bootstrap 5 with Tabler.io template, Vanilla ES6+ JavaScript, No CDN, only local files
- **Documentation**: OpenAPI 3 / Swagger UI with comprehensive API docs
- **Build**: Maven 3.8+
- **Java**: 17+

## Common Development Commands

### Building and Running
```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Package as JAR
mvn clean package

# Skip tests during build
mvn clean install -DskipTests
```

### Testing
```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=StatusApplicationTests

# Run tests with specific profile
mvn test -Dspring.profiles.active=test
```

### Database
```bash
# Database migrations are handled automatically by Flyway on startup
# Migration files: src/main/resources/db/migration/
# Connection configured in: src/main/resources/application.properties
```

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend        │    │   Database      │
│                 │    │                  │    │                 │
│ • Thymeleaf     │◄──►│ • MVC Controllers│◄──►│ • PostgreSQL    │
│ • JavaScript    │    │ • REST APIs      │    │ • Document      │
│ • Bootstrap 5   │    │ • Services       │    │   Processing    │
│   (Tabler.io)   │    │ • Security       │    │ • Multi-tenant  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │
                              ▼
                      ┌──────────────────┐
                      │ Processing Engine│
                      │                  │
                      │ • Transformations│
                      │ • Built-in SFTP  │
                      │ • Audit Trail    │
                      │ • Real-time      │
                      └──────────────────┘
```

## Key Constraints and Patterns

### MVC Controller Constraints

MVC controllers must **ONLY**:
- Serve Thymeleaf templates with minimal configuration data
- Pass `application.properties` values to templates
- Handle JWT token validation and session management
- Perform authentication-based redirects
- Provide template configuration objects

MVC controllers must **NEVER**:
- Query databases directly
- Call business logic services
- Perform data transformations
- Execute business operations
- Return JSON responses

### REST API Controller Requirements

- Place in `controllers/api/` package with clear naming conventions
- Use `@RestController` and `@RequestMapping("/api/...")`
- Encapsulate all inputs in Request classes (from `api/request/` package)
- Encapsulate all outputs in Response classes (from `api/response/` package)
- All business logic delegated to service layer
- Comprehensive error handling with standardized responses
- Support for bulk operations where applicable

### Frontend Architecture Pattern

- **Thymeleaf Templates**: Receive **only** configuration values from MVC controllers
- **Dynamic Data**: All fetched via JavaScript calls to REST APIs
- **Styling**: Bootstrap 5 with Tabler.io template for modern UI/UX
- **JavaScript**: Vanilla ES6+ only - no frameworks, organized by controller/method
- **File Organization**: `/static/js/controllername/methodname/methodname.js`
- **Shared Utilities**: `/static/js/shared/` (auth.js, notifications.js, api.js)
- **Component Pattern**: Each MVC controller method has its own dedicated JavaScript file

### Entity and DTO Policy

- **NO DTOs**: Do not create or use DTOs - work directly with entity classes
- **Entities**: JPA-annotated classes in `models/` package with proper relationships
- **Direct Entity Usage**: Use entities directly in controllers and services
- **API Requests/Responses**: Use entities directly for API contracts
- **Validation**: Apply validation annotations directly on entity classes

## Enhanced Features

### Advanced Authentication & Security
- **Multi-factor Authentication** support
- **Role-based Access Control** with fine-grained permissions
- **Session Management** with secure cookie handling
- **API Rate Limiting** and request throttling
- **Audit Logging** for all security events

## Frontend Implementation

### Modern JavaScript Architecture with Tabler.io

The frontend utilizes modern web development patterns with:

- **Thymeleaf Templates**: Server-side rendering with configuration data
- **Bootstrap 5 with Tabler.io**: Professional UI components and responsive design
- **Vanilla ES6+ JavaScript**: Organized by controller/method structure
- **Local Dependencies**: All CSS, JavaScript libraries served locally - no CDN dependencies
- **Flash Messages**: Server-side flash messages for form submissions and redirects
- **Stylized Popups**: All interactive messages displayed in elegant modal popups
- **Component-Based Architecture**: Reusable UI components and utilities

#### Key Frontend Requirements

**Local Assets Policy**
- All CSS frameworks (Bootstrap, Tabler.io, icons) served from `/static/css/`
- All JavaScript libraries served from `/static/js/vendor/`
- No external CDN dependencies allowed for security and offline capability
- All fonts and icons bundled locally

**Message System**
- **Flash Messages**: Server-side flash messages using Spring's RedirectAttributes for form submissions, login/logout, and page redirects
- **Interactive Popups**: Client-side modal dialogs for confirmations (delete operations, form validations, complex workflows)
- **Consistent Styling**: All messages follow Tabler.io design language with appropriate icons and color schemes
- **Accessibility**: Proper ARIA labels, screen reader support, and keyboard navigation for all message types
- **Message Types**: Success (green), Error (red), Warning (yellow), Info (blue) with matching Tabler.io alert styles
- **Auto-dismiss**: Configurable auto-dismiss timers for non-critical messages
- **Persistent Messages**: Important messages remain visible until user acknowledgment

**JavaScript Organization**
- Each MVC controller method gets dedicated JS file: `/static/js/controllername/methodname/methodname.js`
- Shared utilities in `/static/js/shared/` (auth.js, notifications.js, api.js)
- No external JavaScript frameworks - pure ES6+ vanilla JavaScript
- Modular design with proper error handling and validation