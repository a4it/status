# Status Monitoring Solution

A comprehensive Spring Boot application for monitoring platform uptime statistics, managing incidents, and tracking maintenance windows.

## Features

- **Platform Monitoring** - Track uptime statistics across multiple platforms
- **Incident Management** - Log and manage incidents with cause analysis
- **Maintenance Windows** - Schedule and display planned maintenance
- **Public Status Page** - Public-facing status page for end users
- **Multi-tenant Architecture** - Tenant → Organization → User hierarchy
- **JWT Authentication** - Secure API access with bearer tokens
- **Admin Dashboard** - Comprehensive management interface

## Technology Stack

- **Backend**: Spring Boot 3.2, Spring Security 6, Spring Data JPA
- **Database**: PostgreSQL with Flyway migrations
- **Frontend**: Thymeleaf, Bootstrap 5 (Tabler.io), Vanilla JavaScript
- **Authentication**: JWT (io.jsonwebtoken)
- **API Documentation**: OpenAPI 3 / Swagger UI

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL

## Getting Started

### Database Setup

```bash
# Create PostgreSQL database
createdb status
```

### Configuration

Configure your database connection in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/status
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Or run the packaged JAR
mvn clean package
java -jar target/status-0.0.1-SNAPSHOT.jar
```

The application will be available at `http://localhost:8080`

## Development Commands

```bash
# Run tests
mvn test

# Run specific test class
mvn test -Dtest=StatusApplicationTests

# Build without tests
mvn clean install -DskipTests
```

## Project Structure

```
src/
├── main/
│   ├── java/org/automatize/status/
│   │   ├── api/              # Request/Response classes
│   │   ├── config/           # Spring configuration
│   │   ├── controllers/      # MVC controllers
│   │   │   └── api/          # REST API controllers
│   │   ├── models/           # JPA entities
│   │   ├── repositories/     # Spring Data repositories
│   │   ├── security/         # Security components
│   │   └── services/         # Business logic
│   └── resources/
│       ├── db/migration/     # Flyway SQL migrations
│       ├── static/           # CSS, JS, images
│       └── templates/        # Thymeleaf templates
└── test/                     # Test classes
```

## API Documentation

Swagger UI is available at `/swagger-ui.html` when the application is running.

## License

Proprietary
