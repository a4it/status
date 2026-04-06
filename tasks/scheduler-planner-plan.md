# Scheduler / Planner Feature — Implementation Plan

**Project:** status  
**Feature:** Cron-based Job Scheduler with Program, SQL, REST, and SOAP execution  
**Version Target:** 0.0.92-SNAPSHOT  

---

## Table of Contents

1. [Overview & Goals](#1-overview--goals)
2. [Database Schema Design](#2-database-schema-design)
3. [Java Entity Model Layer](#3-java-entity-model-layer)
4. [Repository Layer](#4-repository-layer)
5. [Service Layer](#5-service-layer)
6. [REST API Controllers](#6-rest-api-controllers)
7. [MVC Controllers & Thymeleaf Templates](#7-mvc-controllers--thymeleaf-templates)
8. [Frontend JavaScript](#8-frontend-javascript)
9. [Cron Expression Wizard](#9-cron-expression-wizard)
10. [Security & Authorization](#10-security--authorization)
11. [Configuration & Properties](#11-configuration--properties)
12. [Flyway Migrations](#12-flyway-migrations)
13. [Maven Dependency Additions](#13-maven-dependency-additions)
14. [Implementation Order](#14-implementation-order)

---

## 1. Overview & Goals

### 1.1 Feature Summary

Add a full-featured scheduler/planner to the Status monitoring app that allows users to:
- Define cron-based jobs with a visual Cron Wizard for expression generation
- Execute three distinct job types: **Program** (shell/disk), **SQL** (JDBC), **REST/SOAP** (HTTP)
- Manage saved JDBC datasource configurations per tenant
- View full execution history with output capture, error capture, and duration
- Manually trigger any job on demand
- Pause / resume / delete jobs with immediate effect

### 1.2 Job Types

#### 1.2.1 PROGRAM (Execute programs on disk)
- Run any executable or shell script on the host machine
- Configure: command path, argument list, working directory, environment variables
- Capture: stdout, stderr, exit code, duration
- Timeout enforcement with process termination
- Support: bash scripts, Python scripts, Java JAR files, any OS executable

#### 1.2.2 SQL (JDBC execution)
- Execute any SQL statement against a configured JDBC datasource
- Supported drivers: PostgreSQL, MySQL, MariaDB, H2
- Datasource config: host, port, database, username, password, schema, connection pool settings
- Support both saved (named) datasources and inline one-off configurations
- Execution types: DML (INSERT/UPDATE/DELETE → returns rows affected), DML (SELECT → returns row count), DDL
- Capture: rows affected, rows returned (as JSON), execution plan, duration, error

#### 1.2.3 REST (HTTP REST calls)
- Full HTTP client with all methods: GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS
- Authentication: None, Basic Auth, Bearer Token, API Key (header or query param), OAuth2 Client Credentials
- Request body formats: JSON, XML, Form URL-encoded, Multipart, Raw text
- Custom headers management (key-value pairs)
- SSL configuration: default CA trust, trust all (dev), custom keystore/truststore
- Timeout: connect timeout, read timeout
- Follow redirects: configurable
- Response capture: status code, headers, body (truncated at configurable limit), duration

#### 1.2.4 SOAP (SOAP web service calls)
- WSDL-based service invocation
- Configure: WSDL URL, service name, port name, operation name
- Request: raw SOAP envelope XML (with syntax highlighting in UI)
- SOAP headers support (WS-Security, custom)
- Namespace-aware parsing
- Response capture: full SOAP response XML, HTTP status, duration

### 1.3 Multi-Tenancy
- All scheduler jobs are scoped to tenant + organization (same as existing entities)
- Users only see/manage jobs for their own tenant context
- ADMIN/MANAGER can create and manage jobs; USER can view; VIEWER read-only

### 1.4 Concurrency Model
- Use Spring's `ThreadPoolTaskScheduler` with configurable pool size
- Each job runs in its own thread with its own timeout
- Overlapping execution prevention: configurable per job (`allowConcurrent` flag)
- If a job is still running and the next trigger fires: skip or queue (configurable)

---

## 2. Database Schema Design

### 2.1 Core Scheduler Tables

#### 2.1.1 `scheduler_jobs`
```sql
CREATE TABLE scheduler_jobs (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL REFERENCES tenants(id),
    organization_id       UUID REFERENCES organizations(id),
    
    -- Identity
    name                  VARCHAR(255) NOT NULL,
    description           TEXT,
    job_type              VARCHAR(50)  NOT NULL,  -- PROGRAM, SQL, REST, SOAP
    
    -- Scheduling
    cron_expression       VARCHAR(255) NOT NULL,  -- Spring 6-field cron (sec min hr dom mon dow)
    time_zone             VARCHAR(100) NOT NULL DEFAULT 'UTC',
    enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
    allow_concurrent      BOOLEAN      NOT NULL DEFAULT FALSE,
    
    -- Lifecycle
    status                VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, PAUSED, DISABLED
    last_run_at           TIMESTAMP WITH TIME ZONE,
    next_run_at           TIMESTAMP WITH TIME ZONE,
    last_run_status       VARCHAR(50),  -- SUCCESS, FAILURE, TIMEOUT, SKIPPED
    consecutive_failures  INTEGER      NOT NULL DEFAULT 0,
    max_retry_attempts    INTEGER      NOT NULL DEFAULT 0,
    retry_delay_seconds   INTEGER      NOT NULL DEFAULT 60,
    timeout_seconds       INTEGER      NOT NULL DEFAULT 300,
    
    -- Output
    max_output_bytes      INTEGER      NOT NULL DEFAULT 102400,  -- 100KB default
    
    -- Tags (for filtering/grouping)
    tags                  TEXT[],
    
    -- Audit
    created_by            VARCHAR(255),
    created_date          TIMESTAMP WITH TIME ZONE,
    created_date_technical BIGINT,
    last_modified_by      VARCHAR(255),
    last_modified_date    TIMESTAMP WITH TIME ZONE,
    last_modified_date_technical BIGINT
);
CREATE INDEX idx_scheduler_jobs_tenant ON scheduler_jobs(tenant_id);
CREATE INDEX idx_scheduler_jobs_status ON scheduler_jobs(tenant_id, status);
CREATE INDEX idx_scheduler_jobs_next_run ON scheduler_jobs(next_run_at) WHERE enabled = TRUE;
```

#### 2.1.2 `scheduler_job_runs`
```sql
CREATE TABLE scheduler_job_runs (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id            UUID NOT NULL REFERENCES scheduler_jobs(id) ON DELETE CASCADE,
    tenant_id         UUID NOT NULL REFERENCES tenants(id),
    
    -- Execution tracking
    trigger_type      VARCHAR(50) NOT NULL,  -- SCHEDULED, MANUAL, RETRY
    status            VARCHAR(50) NOT NULL,  -- RUNNING, SUCCESS, FAILURE, TIMEOUT, CANCELLED
    attempt_number    INTEGER NOT NULL DEFAULT 1,
    
    -- Timing
    started_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at       TIMESTAMP WITH TIME ZONE,
    duration_ms       BIGINT,
    
    -- Output capture
    stdout_output     TEXT,
    stderr_output     TEXT,
    exit_code         INTEGER,          -- PROGRAM: OS exit code
    http_status_code  INTEGER,          -- REST/SOAP: HTTP status
    rows_affected     BIGINT,           -- SQL: rows affected
    response_body     TEXT,             -- REST/SOAP: truncated response
    error_message     TEXT,
    
    -- Metadata
    triggered_by      VARCHAR(255),     -- username if MANUAL
    created_date_technical BIGINT       -- epoch ms for BRIN index
);
CREATE INDEX idx_scheduler_runs_job ON scheduler_job_runs(job_id, started_at DESC);
CREATE INDEX idx_scheduler_runs_tenant ON scheduler_job_runs(tenant_id);
CREATE INDEX idx_scheduler_runs_status ON scheduler_job_runs(job_id, status);
CREATE INDEX idx_scheduler_runs_technical ON scheduler_job_runs USING BRIN(created_date_technical);
```

### 2.2 Job-Type Configuration Tables

#### 2.2.1 `scheduler_program_configs`
```sql
CREATE TABLE scheduler_program_configs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id              UUID NOT NULL UNIQUE REFERENCES scheduler_jobs(id) ON DELETE CASCADE,
    
    command             VARCHAR(2048) NOT NULL,         -- Full path to executable
    arguments           TEXT[],                         -- Ordered argument list
    working_directory   VARCHAR(1024),                  -- CWD for the process
    environment_vars    JSONB,                          -- {"KEY": "VALUE"} map
    shell_wrap          BOOLEAN NOT NULL DEFAULT FALSE, -- Wrap in /bin/bash -c
    shell_path          VARCHAR(512) DEFAULT '/bin/bash',
    run_as_user         VARCHAR(255)                    -- Optional: run as this OS user
);
```

#### 2.2.2 `scheduler_sql_configs`
```sql
CREATE TABLE scheduler_sql_configs (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id                UUID NOT NULL UNIQUE REFERENCES scheduler_jobs(id) ON DELETE CASCADE,
    
    -- Datasource reference (use saved OR inline)
    datasource_id         UUID REFERENCES scheduler_jdbc_datasources(id),
    
    -- Inline datasource (alternative to saved)
    inline_db_type        VARCHAR(50),   -- POSTGRESQL, MYSQL, MARIADB, H2
    inline_jdbc_url       VARCHAR(2048),
    inline_username       VARCHAR(255),
    inline_password_enc   VARCHAR(2048), -- AES-256 encrypted
    
    -- SQL execution
    sql_statement         TEXT NOT NULL,
    sql_type              VARCHAR(50) NOT NULL DEFAULT 'DML',  -- DML, DDL, QUERY
    capture_result_set    BOOLEAN NOT NULL DEFAULT FALSE,      -- store result rows as JSON
    max_result_rows       INTEGER NOT NULL DEFAULT 100,
    connection_timeout_ms INTEGER NOT NULL DEFAULT 5000,
    query_timeout_seconds INTEGER NOT NULL DEFAULT 60
);
```

#### 2.2.3 `scheduler_jdbc_datasources`
```sql
CREATE TABLE scheduler_jdbc_datasources (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    organization_id UUID REFERENCES organizations(id),
    
    name            VARCHAR(255) NOT NULL,              -- Human-readable name
    description     TEXT,
    db_type         VARCHAR(50)  NOT NULL,              -- POSTGRESQL, MYSQL, MARIADB, H2
    
    -- Connection details
    host            VARCHAR(1024),                      -- NULL for H2 embedded
    port            INTEGER,
    database_name   VARCHAR(255) NOT NULL,
    schema_name     VARCHAR(255),
    jdbc_url_override VARCHAR(2048),                    -- Explicit JDBC URL overrides host/port/db
    username        VARCHAR(255),
    password_enc    VARCHAR(2048),                      -- AES-256 encrypted
    
    -- Pool settings
    min_pool_size   INTEGER NOT NULL DEFAULT 1,
    max_pool_size   INTEGER NOT NULL DEFAULT 5,
    connection_timeout_ms INTEGER NOT NULL DEFAULT 5000,
    
    -- Extra JDBC properties
    extra_properties JSONB,                             -- {"ssl": "true", ...}
    
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    
    created_by      VARCHAR(255),
    created_date    TIMESTAMP WITH TIME ZONE,
    created_date_technical BIGINT,
    last_modified_by VARCHAR(255),
    last_modified_date TIMESTAMP WITH TIME ZONE,
    last_modified_date_technical BIGINT
);
CREATE INDEX idx_scheduler_datasources_tenant ON scheduler_jdbc_datasources(tenant_id);
```

#### 2.2.4 `scheduler_rest_configs`
```sql
CREATE TABLE scheduler_rest_configs (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id               UUID NOT NULL UNIQUE REFERENCES scheduler_jobs(id) ON DELETE CASCADE,
    
    -- Request
    http_method          VARCHAR(10)  NOT NULL DEFAULT 'GET',  -- GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS
    url                  VARCHAR(4096) NOT NULL,
    request_body         TEXT,                                  -- Raw body (JSON, XML, etc.)
    content_type         VARCHAR(255) DEFAULT 'application/json',
    headers              JSONB,                                  -- {"Header-Name": "value"}
    query_params         JSONB,                                  -- {"param": "value"}
    
    -- Authentication
    auth_type            VARCHAR(50)  NOT NULL DEFAULT 'NONE', -- NONE, BASIC, BEARER, API_KEY, OAUTH2_CLIENT
    auth_username        VARCHAR(255),                          -- BASIC
    auth_password_enc    VARCHAR(2048),                         -- BASIC (encrypted)
    auth_token_enc       VARCHAR(2048),                         -- BEARER (encrypted)
    auth_api_key_name    VARCHAR(255),                          -- API_KEY: header or param name
    auth_api_key_value_enc VARCHAR(2048),                      -- API_KEY (encrypted)
    auth_api_key_location VARCHAR(50),                          -- HEADER, QUERY_PARAM
    auth_oauth2_token_url  VARCHAR(4096),                      -- OAUTH2
    auth_oauth2_client_id  VARCHAR(1024),                      -- OAUTH2
    auth_oauth2_client_secret_enc VARCHAR(2048),               -- OAUTH2 (encrypted)
    auth_oauth2_scope    VARCHAR(1024),                         -- OAUTH2
    
    -- SSL
    ssl_verify           BOOLEAN NOT NULL DEFAULT TRUE,
    ssl_truststore_path  VARCHAR(2048),
    ssl_truststore_password_enc VARCHAR(2048),
    
    -- Timeouts & behavior
    connect_timeout_ms   INTEGER NOT NULL DEFAULT 5000,
    read_timeout_ms      INTEGER NOT NULL DEFAULT 30000,
    follow_redirects     BOOLEAN NOT NULL DEFAULT TRUE,
    max_response_bytes   INTEGER NOT NULL DEFAULT 102400,
    
    -- Assertions (optional — for success detection)
    assert_status_code   INTEGER,         -- Expected HTTP status, NULL = any 2xx
    assert_body_contains TEXT,            -- Optional string that must appear in body
    assert_json_path     VARCHAR(1024),   -- JSONPath expression that must match
    assert_json_value    VARCHAR(1024)    -- Expected value at JSONPath
);
```

#### 2.2.5 `scheduler_soap_configs`
```sql
CREATE TABLE scheduler_soap_configs (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id               UUID NOT NULL UNIQUE REFERENCES scheduler_jobs(id) ON DELETE CASCADE,
    
    -- Service definition
    wsdl_url             VARCHAR(4096),            -- WSDL URL (for documentation/discovery)
    endpoint_url         VARCHAR(4096) NOT NULL,   -- Actual endpoint to POST to
    service_name         VARCHAR(512),
    port_name            VARCHAR(512),
    operation_name       VARCHAR(512),
    soap_action          VARCHAR(1024),            -- SOAPAction HTTP header
    soap_version         VARCHAR(10) NOT NULL DEFAULT '1.1',  -- 1.1, 1.2
    
    -- Request
    soap_envelope        TEXT NOT NULL,            -- Full SOAP envelope XML
    extra_headers        JSONB,                    -- Additional HTTP headers
    
    -- Authentication (same options as REST)
    auth_type            VARCHAR(50) NOT NULL DEFAULT 'NONE',
    auth_username        VARCHAR(255),
    auth_password_enc    VARCHAR(2048),
    auth_token_enc       VARCHAR(2048),
    
    -- SSL
    ssl_verify           BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Timeouts
    connect_timeout_ms   INTEGER NOT NULL DEFAULT 5000,
    read_timeout_ms      INTEGER NOT NULL DEFAULT 60000,
    max_response_bytes   INTEGER NOT NULL DEFAULT 524288  -- 512KB for SOAP responses
);
```

### 2.3 Notification Hooks per Job (optional — 2.2 phase)
```sql
CREATE TABLE scheduler_job_notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id          UUID NOT NULL REFERENCES scheduler_jobs(id) ON DELETE CASCADE,
    notify_on       VARCHAR(50) NOT NULL,  -- SUCCESS, FAILURE, TIMEOUT, ALWAYS
    channel         VARCHAR(50) NOT NULL,  -- EMAIL, WEBHOOK
    target          VARCHAR(2048) NOT NULL -- email address or webhook URL
);
```

---

## 3. Java Entity Model Layer

### 3.1 Enumerations

#### 3.1.1 `JobType.java`
```java
public enum JobType {
    PROGRAM,  // Execute a program/script on disk
    SQL,      // Execute a SQL statement via JDBC
    REST,     // Execute an HTTP REST call
    SOAP      // Execute a SOAP web service call
}
```

#### 3.1.2 `JobStatus.java`
```java
public enum JobStatus {
    ACTIVE,    // Scheduled and running normally
    PAUSED,    // Temporarily suspended (keeps cron, won't fire)
    DISABLED   // Permanently off
}
```

#### 3.1.3 `JobRunStatus.java`
```java
public enum JobRunStatus {
    RUNNING,    // Currently executing
    SUCCESS,    // Completed successfully
    FAILURE,    // Completed with error
    TIMEOUT,    // Killed due to timeout
    CANCELLED,  // Cancelled by user
    SKIPPED     // Skipped (concurrent execution prevention)
}
```

#### 3.1.4 `JobTriggerType.java`
```java
public enum JobTriggerType {
    SCHEDULED,  // Fired by cron schedule
    MANUAL,     // Triggered by user via API
    RETRY       // Automatic retry attempt
}
```

#### 3.1.5 `DbType.java`
```java
public enum DbType {
    POSTGRESQL, MYSQL, MARIADB, H2;
    
    public String getDriverClass() { ... }
    public String buildJdbcUrl(String host, int port, String db) { ... }
    public int getDefaultPort() { ... }
}
```

#### 3.1.6 `HttpMethod.java`
```java
public enum HttpMethod { GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS }
```

#### 3.1.7 `AuthType.java`
```java
public enum AuthType { NONE, BASIC, BEARER, API_KEY, OAUTH2_CLIENT }
```

#### 3.1.8 `ApiKeyLocation.java`
```java
public enum ApiKeyLocation { HEADER, QUERY_PARAM }
```

#### 3.1.9 `SoapVersion.java`
```java
public enum SoapVersion { V1_1, V1_2 }
```

#### 3.1.10 `SqlType.java`
```java
public enum SqlType { DML, DDL, QUERY }
```

### 3.2 Core Entities

#### 3.2.1 `SchedulerJob.java`
- **Package:** `org.automatize.status.models`
- **Fields:**
  - `UUID id`
  - `Tenant tenant` (@ManyToOne)
  - `Organization organization` (@ManyToOne, nullable)
  - `String name`
  - `String description`
  - `JobType jobType`
  - `String cronExpression` — Spring 6-field cron
  - `String timeZone` (default: "UTC")
  - `Boolean enabled`
  - `Boolean allowConcurrent`
  - `JobStatus status`
  - `ZonedDateTime lastRunAt`
  - `ZonedDateTime nextRunAt`
  - `JobRunStatus lastRunStatus`
  - `Integer consecutiveFailures`
  - `Integer maxRetryAttempts`
  - `Integer retryDelaySeconds`
  - `Integer timeoutSeconds`
  - `Integer maxOutputBytes`
  - `String[] tags`
  - Audit fields (createdBy, createdDate, createdDateTechnical, etc.)
- **Relationships:**
  - `@OneToOne(cascade=ALL, orphanRemoval=true) SchedulerProgramConfig programConfig`
  - `@OneToOne(cascade=ALL, orphanRemoval=true) SchedulerSqlConfig sqlConfig`
  - `@OneToOne(cascade=ALL, orphanRemoval=true) SchedulerRestConfig restConfig`
  - `@OneToOne(cascade=ALL, orphanRemoval=true) SchedulerSoapConfig soapConfig`

#### 3.2.2 `SchedulerJobRun.java`
- **Package:** `org.automatize.status.models`
- **Fields:**
  - `UUID id`
  - `SchedulerJob job` (@ManyToOne, fetch=LAZY)
  - `Tenant tenant` (@ManyToOne)
  - `JobTriggerType triggerType`
  - `JobRunStatus status`
  - `Integer attemptNumber`
  - `ZonedDateTime startedAt`
  - `ZonedDateTime finishedAt`
  - `Long durationMs`
  - `String stdoutOutput` (@Lob or TEXT column)
  - `String stderrOutput` (@Lob)
  - `Integer exitCode`
  - `Integer httpStatusCode`
  - `Long rowsAffected`
  - `String responseBody` (@Lob)
  - `String errorMessage`
  - `String triggeredBy`
  - `Long createdDateTechnical`

#### 3.2.3 `SchedulerJdbcDatasource.java`
- **Package:** `org.automatize.status.models`
- **Fields:**
  - `UUID id`
  - `Tenant tenant` (@ManyToOne)
  - `Organization organization` (@ManyToOne, nullable)
  - `String name`
  - `String description`
  - `DbType dbType`
  - `String host`
  - `Integer port`
  - `String databaseName`
  - `String schemaName`
  - `String jdbcUrlOverride`
  - `String username`
  - `String passwordEnc` (encrypted at rest)
  - `Integer minPoolSize`
  - `Integer maxPoolSize`
  - `Integer connectionTimeoutMs`
  - `String extraProperties` (JSON stored as String, mapped with @Convert)
  - `Boolean enabled`
  - Audit fields

### 3.3 Job Config Entities

#### 3.3.1 `SchedulerProgramConfig.java`
- **Fields:** `id`, `job` (@OneToOne), `command`, `arguments` (String[]), `workingDirectory`, `environmentVars` (Map<String,String> as JSONB), `shellWrap` (Boolean), `shellPath`, `runAsUser`

#### 3.3.2 `SchedulerSqlConfig.java`
- **Fields:** `id`, `job`, `datasource` (@ManyToOne nullable), `inlineDbType` (DbType), `inlineJdbcUrl`, `inlineUsername`, `inlinePasswordEnc`, `sqlStatement`, `sqlType` (SqlType), `captureResultSet`, `maxResultRows`, `connectionTimeoutMs`, `queryTimeoutSeconds`

#### 3.3.3 `SchedulerRestConfig.java`
- **Fields:** `id`, `job`, `httpMethod` (HttpMethod), `url`, `requestBody`, `contentType`, `headers` (Map<String,String> as JSONB), `queryParams` (Map<String,String> as JSONB), `authType` (AuthType), `authUsername`, `authPasswordEnc`, `authTokenEnc`, `authApiKeyName`, `authApiKeyValueEnc`, `authApiKeyLocation` (ApiKeyLocation), `authOauth2TokenUrl`, `authOauth2ClientId`, `authOauth2ClientSecretEnc`, `authOauth2Scope`, `sslVerify`, `sslTruststorePath`, `sslTruststorePasswordEnc`, `connectTimeoutMs`, `readTimeoutMs`, `followRedirects`, `maxResponseBytes`, `assertStatusCode`, `assertBodyContains`, `assertJsonPath`, `assertJsonValue`

#### 3.3.4 `SchedulerSoapConfig.java`
- **Fields:** `id`, `job`, `wsdlUrl`, `endpointUrl`, `serviceName`, `portName`, `operationName`, `soapAction`, `soapVersion` (SoapVersion), `soapEnvelope`, `extraHeaders` (Map<String,String> as JSONB), `authType`, `authUsername`, `authPasswordEnc`, `authTokenEnc`, `sslVerify`, `connectTimeoutMs`, `readTimeoutMs`, `maxResponseBytes`

### 3.4 JSON Column Converters

#### 3.4.1 `JsonMapConverter.java`
```java
@Converter
public class JsonMapConverter implements AttributeConverter<Map<String,String>, String> {
    // Serialize/deserialize Map<String,String> ↔ JSON string using Jackson
}
```

#### 3.4.2 `StringArrayConverter.java` (if not using native `TEXT[]`)
```java
@Converter
public class StringArrayConverter implements AttributeConverter<String[], String> {
    // Serialize/deserialize String[] ↔ comma-delimited or JSON array
}
```

---

## 4. Repository Layer

### 4.1 `SchedulerJobRepository.java`
```java
public interface SchedulerJobRepository extends JpaRepository<SchedulerJob, UUID> {
    Page<SchedulerJob> findByTenantIdAndStatusIn(UUID tenantId, List<JobStatus> statuses, Pageable pageable);
    List<SchedulerJob> findByTenantIdAndEnabledTrueAndStatusNot(UUID tenantId, JobStatus status);
    Optional<SchedulerJob> findByIdAndTenantId(UUID id, UUID tenantId);
    List<SchedulerJob> findByEnabledTrueAndStatusAndNextRunAtBefore(JobStatus status, ZonedDateTime now);
    // Count by type for dashboard widget
    long countByTenantIdAndJobType(UUID tenantId, JobType type);
    long countByTenantIdAndLastRunStatus(UUID tenantId, JobRunStatus status);
}
```

### 4.2 `SchedulerJobRunRepository.java`
```java
public interface SchedulerJobRunRepository extends JpaRepository<SchedulerJobRun, UUID> {
    Page<SchedulerJobRun> findByJobIdOrderByStartedAtDesc(UUID jobId, Pageable pageable);
    Page<SchedulerJobRun> findByTenantIdOrderByStartedAtDesc(UUID tenantId, Pageable pageable);
    Optional<SchedulerJobRun> findTopByJobIdAndStatusOrderByStartedAtDesc(UUID jobId, JobRunStatus status);
    List<SchedulerJobRun> findByJobIdAndStatus(UUID jobId, JobRunStatus status);
    // For cleanup
    void deleteByJobIdAndStartedAtBefore(UUID jobId, ZonedDateTime cutoff);
    // Dashboard stats
    long countByTenantIdAndStatusAndStartedAtAfter(UUID tenantId, JobRunStatus status, ZonedDateTime since);
}
```

### 4.3 `SchedulerJdbcDatasourceRepository.java`
```java
public interface SchedulerJdbcDatasourceRepository extends JpaRepository<SchedulerJdbcDatasource, UUID> {
    List<SchedulerJdbcDatasource> findByTenantIdAndEnabled(UUID tenantId, boolean enabled);
    Optional<SchedulerJdbcDatasource> findByIdAndTenantId(UUID id, UUID tenantId);
}
```

### 4.4 Config Repositories
- `SchedulerProgramConfigRepository` — findByJobId
- `SchedulerSqlConfigRepository` — findByJobId
- `SchedulerRestConfigRepository` — findByJobId
- `SchedulerSoapConfigRepository` — findByJobId

---

## 5. Service Layer

### 5.1 `SchedulerJobService.java` (CRUD)
**Responsibilities:** Create, update, delete, list scheduler jobs; manages config sub-entities.

#### 5.1.1 `createJob(SchedulerJobRequest request, UUID tenantId, String username)`
- Validate cron expression via CronExpression.parse()
- Create SchedulerJob entity
- Based on jobType, create the corresponding config entity
- Encrypt sensitive fields (passwords, tokens) before saving
- Register with SchedulerEngineService after save
- Return the created job

#### 5.1.2 `updateJob(UUID jobId, SchedulerJobRequest request, UUID tenantId)`
- Load job (verify tenant ownership)
- Update all fields including config sub-entity
- Re-register with engine if cron expression changed
- Re-encrypt any updated sensitive fields

#### 5.1.3 `deleteJob(UUID jobId, UUID tenantId)`
- Load and verify ownership
- Unregister from SchedulerEngineService
- Delete (cascades to config and runs via ON DELETE CASCADE)

#### 5.1.4 `pauseJob(UUID jobId, UUID tenantId)` / `resumeJob(UUID jobId, UUID tenantId)`
- Set status to PAUSED / ACTIVE
- Notify engine to remove/add scheduling trigger

#### 5.1.5 `listJobs(UUID tenantId, JobStatus status, JobType type, Pageable pageable)`
- Returns `Page<SchedulerJob>` filtered by tenant, with optional status/type filters

#### 5.1.6 `getJobWithRuns(UUID jobId, UUID tenantId, int recentRunCount)`
- Return job + last N runs

### 5.2 `SchedulerEngineService.java` (Lifecycle management)
**Responsibilities:** Runtime registration and deregistration of scheduled tasks.

#### 5.2.1 Fields
```java
private final ThreadPoolTaskScheduler taskScheduler;
private final Map<UUID, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
```

#### 5.2.2 `@PostConstruct init()`
- On application startup, load all ACTIVE + ENABLED jobs from DB
- Register each as a scheduled task
- Calculate and update nextRunAt for each job

#### 5.2.3 `registerJob(SchedulerJob job)`
- Parse cron expression using `CronTrigger`
- Create Runnable that calls `JobDispatcherService.dispatch(job.getId())`
- Schedule with taskScheduler.schedule(runnable, trigger)
- Store ScheduledFuture in scheduledTasks map

#### 5.2.4 `unregisterJob(UUID jobId)`
- Look up ScheduledFuture from map
- Call future.cancel(false) (don't interrupt running)
- Remove from map

#### 5.2.5 `rescheduleJob(SchedulerJob job)`
- Unregister then register

#### 5.2.6 `calculateNextRunAt(String cronExpression, String timezone)`
- Use CronExpression from spring-context to compute next execution time
- Store on job entity

#### 5.2.7 `getActiveJobCount()` / `getRunningJobCount()`
- For dashboard metrics

### 5.3 `JobDispatcherService.java`
**Responsibilities:** Called by the scheduled trigger; decides to execute or skip; manages retries.

#### 5.3.1 `dispatch(UUID jobId)`
- Load job from DB
- Check if still ACTIVE and ENABLED (may have been paused since registration)
- Check allowConcurrent: if false, check if a RUNNING run exists
  - If concurrent run found: create SKIPPED run record, return
- Create SchedulerJobRun record with status=RUNNING
- Submit to job type executor
- On completion: update run record (status, duration, output)
- Update job.lastRunAt, job.lastRunStatus, job.consecutiveFailures
- Trigger retry if FAILURE and maxRetryAttempts > 0

#### 5.3.2 `triggerManually(UUID jobId, UUID tenantId, String username)`
- Validate ownership
- Same dispatch logic but with triggerType=MANUAL and triggeredBy=username

#### 5.3.3 `retryRun(UUID jobId, int attemptNumber)`
- Similar to dispatch but with triggerType=RETRY
- Delays by retryDelaySeconds (via ScheduledExecutorService.schedule with delay)

### 5.4 `ProgramExecutorService.java`
**Responsibilities:** Execute OS programs/scripts, capture stdout/stderr, enforce timeout.

#### 5.4.1 `execute(SchedulerProgramConfig config, SchedulerJobRun run)`
- Build ProcessBuilder:
  - If shellWrap=true: `[shellPath, "-c", command + " " + args.join(" ")]`
  - Else: `[command, ...args]`
- Set environment variables
- Set working directory
- Start process
- Stream stdout and stderr asynchronously to StringBuilders (via gobbler threads)
- Wait for process with timeout (waitFor(timeout, SECONDS))
- If timeout exceeded: process.destroyForcibly(), set status=TIMEOUT
- Capture exit code, truncate output to maxOutputBytes
- Return ExecutionResult with exitCode, stdout, stderr, status

#### 5.4.2 Output gobbling
```java
private class StreamGobbler implements Runnable {
    // Reads InputStream line by line into StringBuilder
    // Respects maxOutputBytes limit to prevent OOM
}
```

#### 5.4.3 Security considerations
- SSRF-style validation: restrict command to allowed paths (configurable allowlist)
- Log all program executions with audit trail
- Consider running as restricted user

### 5.5 `SqlExecutorService.java`
**Responsibilities:** Build JDBC connection from config, execute SQL, capture results.

#### 5.5.1 `execute(SchedulerSqlConfig config, SchedulerJobRun run)`
- Determine JDBC URL: from datasource_id, inline config, or jdbcUrlOverride
- Build HikariDataSource with minimal pool (minIdle=1, max=1 for one-shot jobs)
  - Or: use connection pool cache keyed by datasource_id
- Decrypt password
- Obtain connection with timeout
- Set query timeout on statement
- Execute based on sqlType:
  - QUERY: executeQuery() → convert ResultSet to JSON array, limit to maxResultRows
  - DML: executeUpdate() → return rows affected
  - DDL: execute()
- Close all resources in finally block
- Destroy transient datasource after use (or return to cache for named datasources)
- Return ExecutionResult with rowsAffected, resultSetJson, status

#### 5.5.2 `buildJdbcUrl(SchedulerSqlConfig config)`
- Uses DbType.buildJdbcUrl() helper to construct URL from host/port/db if no override

#### 5.5.3 Driver availability
- PostgreSQL driver: already present in pom.xml
- MySQL: add `mysql:mysql-connector-j` dependency
- MariaDB: add `org.mariadb.jdbc:mariadb-java-client` dependency
- H2: add `com.h2database:h2` dependency

#### 5.5.4 `testConnection(SchedulerJdbcDatasource datasource)`
- Used by API: validates connection is reachable without executing SQL
- Returns ConnectionTestResult with success/failure + latency

### 5.6 `RestExecutorService.java`
**Responsibilities:** Execute HTTP REST calls with full configuration support.

#### 5.6.1 `execute(SchedulerRestConfig config, SchedulerJobRun run)`
- Build `java.net.http.HttpClient` (Java 11+ built-in):
  - Set connect timeout
  - Set redirect policy
  - Set SSL context (based on sslVerify)
- Build HttpRequest:
  - Set URL with query params appended
  - Set method and body (based on requestBody and contentType)
  - Set headers
  - Apply authentication:
    - NONE: no auth headers
    - BASIC: Authorization: Basic base64(user:pass)
    - BEARER: Authorization: Bearer {decrypted token}
    - API_KEY (HEADER): {apiKeyName}: {decrypted value}
    - API_KEY (QUERY): append {apiKeyName}={decrypted value} to URL
    - OAUTH2_CLIENT: call token URL, cache token, apply as Bearer
- Send request with read timeout
- Capture: status code, response headers, body (truncated to maxResponseBytes)
- Evaluate assertions:
  - assertStatusCode: compare HTTP status
  - assertBodyContains: check body string
  - assertJsonPath: use a lightweight JSONPath evaluator
- Determine JobRunStatus: SUCCESS or FAILURE
- Return ExecutionResult

#### 5.6.2 OAuth2 token cache
```java
private final Map<String, CachedToken> tokenCache = new ConcurrentHashMap<>();
// Key = clientId+tokenUrl, CachedToken holds value + expiry
```

#### 5.6.3 SSL context builder
```java
private SSLContext buildSslContext(SchedulerRestConfig config) {
    if (!config.getSslVerify()) return trustAllSslContext();
    if (config.getSslTruststorePath() != null) return loadTruststoreContext(...);
    return SSLContext.getDefault();
}
```

### 5.7 `SoapExecutorService.java`
**Responsibilities:** Execute SOAP calls by posting raw XML to an endpoint.

#### 5.7.1 `execute(SchedulerSoapConfig config, SchedulerJobRun run)`
- Build HttpClient (reuse RestExecutorService client builder)
- Build request:
  - Method: always POST
  - Content-Type: text/xml; charset=utf-8 (SOAP 1.1) or application/soap+xml (SOAP 1.2)
  - SOAPAction header: config.soapAction (SOAP 1.1) or in Content-Type params (SOAP 1.2)
  - Body: config.soapEnvelope
  - Extra headers from config.extraHeaders
  - Apply auth (same auth types as REST)
- Send and capture response
- Parse response XML minimally (check for `<soap:Fault>` element to detect SOAP faults)
- Return ExecutionResult with responseBody (full XML), httpStatusCode, status

### 5.8 `CronValidationService.java`
**Responsibilities:** Parse and validate cron expressions; compute next execution times.

#### 5.8.1 `validate(String cronExpression)`
- Attempt `CronExpression.parse(cronExpression)` from spring-context
- Return ValidationResult with isValid, errorMessage

#### 5.8.2 `getNextExecutions(String cronExpression, String timezone, int count)`
- Parse CronExpression with ZoneId
- Compute next N execution times from now
- Return List<ZonedDateTime>

#### 5.8.3 `toHumanReadable(String cronExpression)`
- Map common cron patterns to natural language:
  - `0 0 * * * *` → "Every hour at :00"
  - `0 0 9 * * MON-FRI` → "Weekdays at 9:00 AM"
  - `0 0/5 * * * *` → "Every 5 minutes"
- Return human-readable string or fall back to displaying raw expression

### 5.9 `SchedulerEncryptionService.java`
**Responsibilities:** Encrypt/decrypt sensitive configuration values at rest.

#### 5.9.1 `encrypt(String plaintext)` → `String ciphertext`
- AES-256-GCM encryption
- Key derived from `scheduler.encryption.key` property (256-bit base64)
- Prepend IV to ciphertext

#### 5.9.2 `decrypt(String ciphertext)` → `String plaintext`
- Extract IV, decrypt with AES-256-GCM

### 5.10 `SchedulerRunRetentionScheduler.java`
**Responsibilities:** Periodically clean up old run records.

#### 5.10.1 `@Scheduled(cron = "0 0 3 * * *")` cleanOldRuns()
- Per job: keep last N runs (configurable, default 100)
- Delete runs older than configured days (default 30)

---

## 6. REST API Controllers

### 6.1 `SchedulerJobApiController.java`
**Base path:** `/api/scheduler/jobs`
**Security:** `@PreAuthorize("isAuthenticated()")`

#### 6.1.1 `GET /api/scheduler/jobs` — List jobs
- Query params: `page`, `size`, `sort`, `status` (filter), `type` (filter), `search` (name filter)
- Returns `Page<SchedulerJobResponse>`
- Jobs scoped to current tenant via TenantContextService

#### 6.1.2 `GET /api/scheduler/jobs/{id}` — Get single job
- Returns `SchedulerJobResponse` with embedded config
- Includes last 5 runs summary

#### 6.1.3 `POST /api/scheduler/jobs` — Create job
- Request body: `SchedulerJobRequest`
- `@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")`
- Validates cron expression before saving
- Returns `SchedulerJobResponse` (201 Created)

#### 6.1.4 `PUT /api/scheduler/jobs/{id}` — Update job
- Request body: `SchedulerJobRequest`
- `@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")`
- Returns updated `SchedulerJobResponse`

#### 6.1.5 `DELETE /api/scheduler/jobs/{id}` — Delete job
- `@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")`
- Soft check: warn if currently RUNNING
- Returns 204 No Content

#### 6.1.6 `POST /api/scheduler/jobs/{id}/pause` — Pause job
- Sets status=PAUSED, deregisters from scheduler
- Returns updated job

#### 6.1.7 `POST /api/scheduler/jobs/{id}/resume` — Resume job
- Sets status=ACTIVE, re-registers with scheduler
- Returns updated job

#### 6.1.8 `POST /api/scheduler/jobs/{id}/trigger` — Manual trigger
- `@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")`
- Async: submits job to executor pool
- Returns `SchedulerJobRunResponse` for the newly created run (status=RUNNING)

#### 6.1.9 `GET /api/scheduler/jobs/{id}/next-runs` — Preview next runs
- Query param: `count` (default 5, max 20)
- Returns `List<ZonedDateTime>` for next executions

### 6.2 `SchedulerRunApiController.java`
**Base path:** `/api/scheduler/runs`

#### 6.2.1 `GET /api/scheduler/runs` — List all runs for tenant
- Query params: `page`, `size`, `jobId`, `status`, `from`, `to` (date range)
- Returns `Page<SchedulerJobRunResponse>`

#### 6.2.2 `GET /api/scheduler/runs/{id}` — Get single run details
- Full output content (stdout, stderr, response body)
- Returns `SchedulerJobRunResponse`

#### 6.2.3 `GET /api/scheduler/jobs/{id}/runs` — Runs for specific job
- Query params: `page`, `size`
- Returns `Page<SchedulerJobRunResponse>`

#### 6.2.4 `DELETE /api/scheduler/runs/{id}` — Delete run record
- `@PreAuthorize("hasRole('ADMIN')")`
- Returns 204

### 6.3 `SchedulerDatasourceApiController.java`
**Base path:** `/api/scheduler/datasources`

#### 6.3.1 `GET /api/scheduler/datasources` — List datasources
- Returns `List<SchedulerJdbcDatasourceResponse>` (passwords never returned, masked)

#### 6.3.2 `GET /api/scheduler/datasources/{id}` — Get datasource
- Returns config (masked password)

#### 6.3.3 `POST /api/scheduler/datasources` — Create datasource
- Encrypts password before saving
- Returns created datasource

#### 6.3.4 `PUT /api/scheduler/datasources/{id}` — Update datasource
- If password field is empty/null: keep existing encrypted password
- Returns updated datasource

#### 6.3.5 `DELETE /api/scheduler/datasources/{id}` — Delete datasource
- Check: reject if any job references this datasource

#### 6.3.6 `POST /api/scheduler/datasources/{id}/test` — Test connection
- Runs `SqlExecutorService.testConnection()`
- Returns `{ success: true/false, latencyMs: 42, error: "..." }`

#### 6.3.7 `POST /api/scheduler/datasources/test-inline` — Test inline config
- Body: `InlineDatasourceTestRequest` with all connection fields
- Same as test above but without saving

### 6.4 `CronWizardApiController.java`
**Base path:** `/api/scheduler/cron`

#### 6.4.1 `POST /api/scheduler/cron/validate` — Validate cron expression
- Body: `{ "expression": "0 0 * * * *" }`
- Returns `{ valid: true, humanReadable: "Every hour", nextRuns: [...] }`

#### 6.4.2 `POST /api/scheduler/cron/preview` — Preview next N runs
- Body: `{ "expression": "...", "timezone": "America/New_York", "count": 5 }`
- Returns `List<String>` of formatted datetimes

#### 6.4.3 `GET /api/scheduler/cron/presets` — List preset expressions
- Returns `List<CronPreset>` with name, expression, humanReadable

---

## 7. MVC Controllers & Thymeleaf Templates

### 7.1 `SchedulerMvcController.java`
**Base path:** `/admin/scheduler`

#### 7.1.1 `GET /admin/scheduler` → `admin/scheduler/jobs.html`
- Model: applicationName, buildNumber, buildDate, copyright only
- Data loaded via JS + API

#### 7.1.2 `GET /admin/scheduler/datasources` → `admin/scheduler/datasources.html`
- Model: same minimal config

### 7.2 Navigation Update
- Add "Scheduler" menu item to the admin sidebar (inside admin layout fragment)
- Icon: `ti ti-calendar-time` (Tabler icon)
- Sub-menu: "Jobs", "Datasources"

### 7.3 Thymeleaf Templates

#### 7.3.1 `templates/admin/scheduler/jobs.html`
```
Structure:
├── Page header: "Scheduler Jobs" + [+ New Job] button
├── Stats bar (4 cards):
│   ├── Total Jobs
│   ├── Running Now
│   ├── Succeeded Today
│   └── Failed Today
├── Filter row:
│   ├── Status filter (All / Active / Paused / Disabled)
│   ├── Type filter (All / Program / SQL / REST / SOAP)
│   └── Search by name
├── Jobs table:
│   ├── Name
│   ├── Type (badge with icon)
│   ├── Cron Expression
│   ├── Next Run At
│   ├── Last Run At
│   ├── Last Status (badge)
│   └── Actions: [Trigger] [Edit] [Pause/Resume] [Delete]
├── Job Create/Edit Modal (wizard-style, 3 steps):
│   ├── Step 1: Basic Info (name, description, type, tags)
│   ├── Step 2: Schedule (cron wizard widget)
│   └── Step 3: Job Configuration (conditional by type)
└── Run History Modal (per job)
```

#### 7.3.2 `templates/admin/scheduler/datasources.html`
```
Structure:
├── Page header: "JDBC Datasources" + [+ New Datasource] button
├── Datasources table:
│   ├── Name
│   ├── Type (badge: PostgreSQL / MySQL / MariaDB / H2)
│   ├── Host:Port/Database
│   ├── Status
│   └── Actions: [Test] [Edit] [Delete]
└── Create/Edit Modal
```

---

## 8. Frontend JavaScript

### 8.1 File Structure
```
/static/js/admin/scheduler/
├── jobs/jobs.js              — Job list, CRUD modals, table
├── datasources/datasources.js — Datasource list, CRUD modals
└── shared/
    ├── cron-wizard.js         — Reusable cron expression wizard widget
    ├── job-type-forms.js      — Dynamic form sections per job type
    └── run-history.js         — Run history table + output viewer
```

### 8.2 `jobs/jobs.js` — Main scheduler jobs page

#### 8.2.1 Initialization
- `document.addEventListener('DOMContentLoaded', init)`
- Load stats (counts by status + type)
- Load jobs table (page 1)
- Bind filter change events → reload table
- Bind [+ New Job] button → openCreateModal()

#### 8.2.2 `loadStats()`
- `GET /api/scheduler/jobs` + count queries
- Render 4 stat cards

#### 8.2.3 `loadJobs(page, filters)`
- `GET /api/scheduler/jobs?page={page}&status={status}&type={type}&search={q}`
- Render table rows with:
  - Type badge: PROGRAM=orange, SQL=blue, REST=green, SOAP=purple
  - Status badge: ACTIVE=green, PAUSED=yellow, DISABLED=gray
  - Last run status badge: SUCCESS=green, FAILURE=red, TIMEOUT=orange, RUNNING=spinning blue
  - Next run: relative time (e.g., "in 5 minutes") with full datetime on hover
  - Action buttons (always visible on row)

#### 8.2.4 Modal wizard flow
- Uses 3-step Bootstrap modal with prev/next navigation
- Step 1: Basic info fields + type selector (radio buttons with icons)
- Step 2: Cron Wizard (renders CronWizard widget) + timezone selector
- Step 3: Dynamically renders the appropriate job type form via `job-type-forms.js`
- Validation on each step before allowing next

#### 8.2.5 `triggerJob(jobId)`
- Confirm dialog: "Manually trigger this job now?"
- `POST /api/scheduler/jobs/{id}/trigger`
- Show toast: "Job triggered — Run #{runId} started"
- Open run history sidebar after 500ms delay to show RUNNING status

#### 8.2.6 `pauseResumeJob(jobId, currentStatus)`
- Toggle: ACTIVE→PAUSED or PAUSED→ACTIVE
- `POST /api/scheduler/jobs/{id}/pause` or `/resume`
- Refresh table row in place

#### 8.2.7 `deleteJob(jobId, jobName)`
- Confirm: "Delete job '{name}'? This will also delete all run history."
- `DELETE /api/scheduler/jobs/{id}`
- Remove row from table with animation

#### 8.2.8 Row click → Run history
- Clicking a job row opens Run History modal for that job
- Load via `run-history.js`

### 8.3 `shared/cron-wizard.js` — Cron Expression Wizard Widget

#### 8.3.1 Widget Architecture
```
CronWizard class:
├── constructor(container, onChange)  — initializes into DOM container
├── render()                          — creates wizard HTML
├── bindEvents()                      — attaches all event listeners
├── getExpression()                   — returns current 6-field cron string
├── setExpression(expr)               — parses and populates UI from expression
└── private helpers
```

#### 8.3.2 Wizard UI structure
```
Mode selector row:
  [Simple] [Advanced] [Presets]

--- Simple Mode ---
Frequency: [Every minute ▼]
  Options based on frequency:
  - Every minute:    (no extra fields)
  - Every N minutes: [N] minutes
  - Every hour:      At minute [0]
  - Every N hours:   Every [N] hours at minute [0]
  - Daily:           At [09:00] time
  - Weekly:          On [Mon] [Tue] [Wed] ... at [09:00]
  - Monthly:         On day [1] at [09:00]
  - Custom:          → Switch to Advanced

--- Advanced Mode ---
6 fields with helper text:
  Seconds:    [*   ] ← input with validation, picker icon
  Minutes:    [0   ]
  Hours:      [9   ]
  Day/Month:  [*   ]
  Month:      [*   ]
  Day/Week:   [MON-FRI]
  
  [Validate] button
  Result: "0 9 * * MON-FRI" ← editable raw input

--- Presets Mode ---
Grid of preset buttons:
  [Every minute] [Every 5 min] [Every 15 min] [Hourly]
  [Daily at midnight] [Daily 9am] [Weekdays 9am] [Weekly Mon]
  [Monthly 1st] [Quarterly] [Yearly]

Preview section (always visible):
  Human readable: "Weekdays at 9:00 AM UTC"
  Next runs:
    • Mon, 7 Apr 2026 09:00:00 UTC
    • Tue, 8 Apr 2026 09:00:00 UTC
    • Wed, 9 Apr 2026 09:00:00 UTC
    • Thu, 10 Apr 2026 09:00:00 UTC
    • Fri, 11 Apr 2026 09:00:00 UTC
  [Copy expression]
```

#### 8.3.3 Cron field input helpers
- Each field shows syntax hint on focus: e.g., Minutes: "0-59, */5, 0,30"
- Inline validation: red border + error tooltip for invalid patterns
- Real-time preview update on any change (debounced 300ms)
- Tab key moves between fields in order

#### 8.3.4 Next-runs preview
- Calls `POST /api/scheduler/cron/preview` after debounce
- Renders as a small list of upcoming datetimes
- Shows timezone label next to each datetime

#### 8.3.5 Expression parsing (setExpression)
- Split on whitespace into 6 parts
- Map parts back to Simple mode frequency if possible
- Otherwise: switch to Advanced mode and populate fields

### 8.4 `shared/job-type-forms.js` — Job Type Configuration Forms

#### 8.4.1 `renderProgramForm(container, existingConfig)`
```
Form fields:
├── Command path      [/usr/bin/python3        ] + [Browse] (file path input)
├── Arguments         [script.py --verbose     ] (space-separated or JSON array)
├── Working Directory [/opt/scripts/            ]
├── Shell wrap        [x] Wrap in shell
├── Shell path        [/bin/bash               ] (shown if shell wrap checked)
├── Environment vars  [+ Add Variable] table:
│   ├── KEY   VALUE  [Delete]
│   └── ...
└── Run as user       [                        ] (optional)
```

#### 8.4.2 `renderSqlForm(container, existingConfig, datasources)`
```
Form fields:
├── Datasource        [Select saved datasource ▼] OR [Use inline config]
│   ├── Saved:        dropdown of tenant's datasources (with [Test] button)
│   └── Inline:       toggle reveals:
│       ├── DB Type   [PostgreSQL ▼]
│       ├── Host      [localhost   ] Port [5432]
│       ├── Database  [mydb       ]
│       ├── Username  [admin      ]
│       └── Password  [••••••••   ]
├── SQL Type          [DML ▼] (DML, DDL, QUERY)
├── SQL Statement     [<textarea with monospace font and syntax hint>]
├── Capture Result    [x] Store result rows as JSON
├── Max Result Rows   [100]
└── Query Timeout     [60] seconds
```

#### 8.4.3 `renderRestForm(container, existingConfig)`
```
Form fields:
├── Method + URL      [GET ▼] [https://api.example.com/endpoint  ]
├── Tabs:
│   ├── Headers      — Key/value table + [+ Add Header]
│   ├── Query Params — Key/value table + [+ Add Param]
│   ├── Body         — Content-Type selector + textarea
│   ├── Auth         — Auth type selector + conditional fields:
│   │   ├── None:         (no extra fields)
│   │   ├── Basic:        Username + Password
│   │   ├── Bearer:       Token field
│   │   ├── API Key:      Key Name + Key Value + Location (Header/Query)
│   │   └── OAuth2:       Token URL + Client ID + Client Secret + Scope
│   └── Advanced     — Timeouts, SSL verify, redirect, max response bytes
├── Assertions (optional):
│   ├── Expected Status Code [200]
│   ├── Body must contain    [success]
│   ├── JSONPath             [$.status] = [ok]
└── [Test Now] button → modal shows raw request + response
```

#### 8.4.4 `renderSoapForm(container, existingConfig)`
```
Form fields:
├── Endpoint URL      [https://service.example.com/ws  ]
├── WSDL URL          [https://service.example.com/ws?wsdl] (optional, for docs)
├── Service Name      [MyService   ]
├── Port Name         [MySoap     ]
├── Operation Name    [GetData     ]
├── SOAP Action       [urn:GetData ] (for SOAP 1.1)
├── SOAP Version      [1.1 ▼] / [1.2]
├── SOAP Envelope     [<textarea with XML formatting>]
├── Auth (same as REST auth section)
├── Additional Headers — Key/value table
└── [Test Now] button
```

### 8.5 `shared/run-history.js` — Run History Viewer

#### 8.5.1 `openRunHistoryModal(jobId, jobName)`
- Opens a large modal with:
  - Job name in header
  - Paginated table of runs (newest first)
  - Auto-refreshes if any run is RUNNING (poll every 3 seconds)

#### 8.5.2 Run table columns
- Run # (attempt number)
- Trigger (badge: SCHEDULED/MANUAL/RETRY)
- Status (badge with icon)
- Started At
- Duration
- Exit/HTTP Code
- Actions: [View Output]

#### 8.5.3 `viewRunOutput(runId)`
- Loads `GET /api/scheduler/runs/{id}`
- Opens output viewer panel (split: stdout/stderr tabs for PROGRAM; response body for REST/SOAP; result rows for SQL)
- Monospace font, syntax highlighting for JSON/XML (via highlight.js or simple regex)
- "Copy to clipboard" button
- "Download" button (download raw output as .txt or .json)

### 8.6 `datasources/datasources.js`

#### 8.6.1 Load and display datasources
- Table with type badge, connection info, enabled status
- [Test Connection] button calls `/api/scheduler/datasources/{id}/test` → shows latency or error

#### 8.6.2 Create/Edit modal
- DB type selector (changes port default automatically)
- Connection fields
- [Test Connection] in modal before saving
- Password field: shows ●●●●●● for existing, clear for new entry

---

## 9. Cron Expression Wizard — Detailed Specification

### 9.1 Spring Cron Format (6 fields)
```
┌───────────── second (0-59)
│ ┌───────────── minute (0-59)
│ │ ┌───────────── hour (0-23)
│ │ │ ┌───────────── day of month (1-31)
│ │ │ │ ┌───────────── month (1-12 or JAN-DEC)
│ │ │ │ │ ┌───────────── day of week (0-7, SUN-SAT, or MON-FRI)
│ │ │ │ │ │
* * * * * *
```

### 9.2 Supported Special Characters
| Symbol | Meaning | Example |
|--------|---------|---------|
| `*`    | Every value | `* * * * * *` |
| `?`    | No specific value (day-of-month or day-of-week only) | `0 0 12 ? * MON` |
| `-`    | Range | `0 0 9-17 * * MON-FRI` |
| `,`    | List | `0 0 9,12,17 * * *` |
| `/`    | Step | `0 0/5 * * * *` (every 5 min) |
| `L`    | Last day | `0 0 0 L * *` (last day of month) |
| `W`    | Weekday nearest | `0 0 0 15W * *` |
| `#`    | Nth weekday | `0 0 0 ? * MON#1` (first Monday) |

### 9.3 Preset Cron Expressions
| Label | Expression | Description |
|-------|-----------|-------------|
| Every minute | `0 * * * * *` | Every minute at :00 seconds |
| Every 5 minutes | `0 0/5 * * * *` | Every 5 minutes |
| Every 15 minutes | `0 0/15 * * * *` | Every 15 minutes |
| Every 30 minutes | `0 0/30 * * * *` | Every 30 minutes |
| Hourly | `0 0 * * * *` | Top of every hour |
| Daily midnight | `0 0 0 * * *` | Every day at midnight |
| Daily 9am | `0 0 9 * * *` | Every day at 9:00 AM |
| Weekdays 9am | `0 0 9 * * MON-FRI` | Monday-Friday at 9:00 AM |
| Weekly Monday | `0 0 9 * * MON` | Every Monday at 9:00 AM |
| Monthly 1st | `0 0 9 1 * *` | 1st of every month at 9:00 AM |
| Quarterly | `0 0 9 1 1,4,7,10 *` | 1st of Jan, Apr, Jul, Oct |
| Yearly | `0 0 9 1 1 *` | January 1st at 9:00 AM |

### 9.4 Timezone Support
- Dropdown of all Java `ZoneId` values (user-friendly display names)
- Groups: UTC, US zones, European zones, Asian zones, etc.
- Default: UTC
- Preview shows local time in selected timezone

### 9.5 Validation Rules
- Must have exactly 6 space-separated fields
- Second field: 0-59 only (no month-style values)
- Day-of-month and day-of-week: at least one must use `?` or `*`
- Step values must be positive integers
- Range start must be ≤ range end
- Named months/days must be valid abbreviations

---

## 10. Security & Authorization

### 10.1 Role-Based Access Control
| Operation | ADMIN | MANAGER | USER | VIEWER |
|-----------|-------|---------|------|--------|
| View jobs | ✓ | ✓ | ✓ | ✓ |
| Create job | ✓ | ✓ | ✗ | ✗ |
| Edit job | ✓ | ✓ | ✗ | ✗ |
| Delete job | ✓ | ✗ | ✗ | ✗ |
| Trigger manually | ✓ | ✓ | ✗ | ✗ |
| Pause/Resume | ✓ | ✓ | ✗ | ✗ |
| View run history | ✓ | ✓ | ✓ | ✓ |
| View run output | ✓ | ✓ | ✓ | ✗ |
| Delete run | ✓ | ✗ | ✗ | ✗ |
| Manage datasources | ✓ | ✓ | ✗ | ✗ |

### 10.2 Tenant Isolation
- All API endpoints filter by tenant from `TenantContextService.getCurrentTenantId()`
- Ownership check on all update/delete operations (if id belongs to different tenant → 403)

### 10.3 Program Execution Security
- Command allowlist: configurable `scheduler.program.allowed-paths` property (regex patterns)
- If allowlist is empty: all commands allowed (must be explicitly configured otherwise)
- Shell injection prevention: arguments passed as array (not shell-interpolated string)
- Filesystem access: optionally sandbox via OS-level restrictions

### 10.4 SQL Execution Security
- JDBC URL allowlist: optional `scheduler.sql.allowed-hosts` to restrict which DB hosts are reachable
- Passwords encrypted at rest using AES-256-GCM
- SSRF protection: inline JDBC URLs validated against SsrfValidator

### 10.5 REST/SOAP Security
- URL validation: existing SsrfValidator applied to all REST/SOAP target URLs
- API keys and tokens: encrypted at rest, never returned in API responses (masked as ●●●●)
- OAuth2 token caching: in-memory only (never persisted)
- SSL trust-all: only allowed when `scheduler.rest.allow-insecure-ssl=true` (default: false)

### 10.6 Sensitive Field Masking
- API responses: replace `passwordEnc`, `authPasswordEnc`, `authTokenEnc`, `authApiKeyValueEnc`, `authOauth2ClientSecretEnc` with `"●●●●●●●●"` or null
- Frontend: password fields use `type="password"` with placeholder `••••••` if existing value present
- "Change password" pattern: empty = keep existing

---

## 11. Configuration & Properties

### 11.1 New properties in `application.properties`
```properties
# Scheduler Engine
scheduler.enabled=true
scheduler.thread-pool-size=10
scheduler.run-retention-days=30
scheduler.max-runs-per-job=100

# Program Executor
scheduler.program.allowed-paths=
scheduler.program.default-timeout-seconds=300
scheduler.program.max-output-bytes=102400

# SQL Executor  
scheduler.sql.allowed-hosts=
scheduler.sql.default-query-timeout-seconds=60
scheduler.sql.connection-pool-cache-size=20

# REST Executor
scheduler.rest.allow-insecure-ssl=false
scheduler.rest.default-connect-timeout-ms=5000
scheduler.rest.default-read-timeout-ms=30000
scheduler.rest.max-response-bytes=102400

# Encryption
scheduler.encryption.key=<base64-encoded-256-bit-key>
```

### 11.2 Application Context Properties
- Thread pool size configurable via application.properties without code change
- All timeout defaults can be overridden globally or per-job

---

## 12. Flyway Migrations

### 12.1 `V19__scheduler_core.sql`
- Creates: `scheduler_jobs`, `scheduler_job_runs`, `scheduler_jdbc_datasources`
- All indexes

### 12.2 `V20__scheduler_configs.sql`
- Creates: `scheduler_program_configs`, `scheduler_sql_configs`, `scheduler_rest_configs`, `scheduler_soap_configs`

### 12.3 `V21__scheduler_notifications.sql` (phase 2)
- Creates: `scheduler_job_notifications`

---

## 13. Maven Dependency Additions

```xml
<!-- MySQL JDBC driver -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- MariaDB JDBC driver -->
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- H2 embedded database -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- HikariCP (already included via spring-boot-starter-data-jpa, but make explicit) -->
<!-- No new dependency needed — already on classpath -->

<!-- Spring Scheduling (already included via spring-boot-starter) -->
<!-- No new dependency needed -->
```

**Note:** Java 11+ `java.net.http.HttpClient` is used for REST/SOAP — no additional HTTP client library needed.
PostgreSQL driver already present in pom.xml.

---

## 14. Implementation Order

### Phase 1 — Foundation (Database + Models + Engine)
- [ ] Write V19 + V20 Flyway migrations
- [ ] Create all enumerations (JobType, JobStatus, etc.)
- [ ] Create all entity classes (SchedulerJob, SchedulerJobRun, SchedulerJdbcDatasource, configs)
- [ ] Create JSON converters (JsonMapConverter)
- [ ] Create all repository interfaces
- [ ] Create SchedulerEncryptionService
- [ ] Create CronValidationService
- [ ] Create SchedulerEngineService (with @PostConstruct init)
- [ ] Create JobDispatcherService

### Phase 2 — Executors
- [ ] Implement ProgramExecutorService
- [ ] Implement SqlExecutorService + testConnection
- [ ] Implement RestExecutorService + OAuth2 caching
- [ ] Implement SoapExecutorService

### Phase 3 — CRUD Services + APIs
- [ ] Implement SchedulerJobService
- [ ] Implement SchedulerRunRetentionScheduler
- [ ] Implement SchedulerJobApiController
- [ ] Implement SchedulerRunApiController
- [ ] Implement SchedulerDatasourceApiController
- [ ] Implement CronWizardApiController
- [ ] Add request/response classes (SchedulerJobRequest, SchedulerJobResponse, etc.)

### Phase 4 — Frontend
- [ ] Add SchedulerMvcController
- [ ] Create jobs.html template
- [ ] Create datasources.html template
- [ ] Implement cron-wizard.js
- [ ] Implement job-type-forms.js
- [ ] Implement run-history.js
- [ ] Implement jobs/jobs.js (main page)
- [ ] Implement datasources/datasources.js
- [ ] Update admin navigation sidebar

### Phase 5 — Properties, Security, Cleanup
- [ ] Add application.properties scheduler config
- [ ] Implement command allowlist validation in ProgramExecutorService
- [ ] SSRF validation for SQL + REST + SOAP URLs
- [ ] Sensitive field masking in response classes
- [ ] Add scheduler.encryption.key to application.properties (auto-generate if missing)

### Phase 6 — Polish & Notifications (optional)
- [ ] V21 migration for scheduler_job_notifications
- [ ] Implement notification hooks (email on failure, webhook)
- [ ] Add "Test" button functionality in REST/SOAP forms
- [ ] Syntax highlighting in output viewer
- [ ] Export run history as CSV

---

## Appendix A: API Request/Response Classes

### `SchedulerJobRequest.java`
```java
public class SchedulerJobRequest {
    @NotBlank String name;
    String description;
    @NotNull JobType jobType;
    @NotBlank String cronExpression;
    String timeZone;           // default: UTC
    Boolean enabled;           // default: true
    Boolean allowConcurrent;   // default: false
    Integer maxRetryAttempts;  // default: 0
    Integer retryDelaySeconds; // default: 60
    Integer timeoutSeconds;    // default: 300
    Integer maxOutputBytes;    // default: 102400
    String[] tags;
    
    // Exactly one of these should be non-null based on jobType:
    ProgramConfigRequest programConfig;
    SqlConfigRequest sqlConfig;
    RestConfigRequest restConfig;
    SoapConfigRequest soapConfig;
}
```

### `SchedulerJobResponse.java`
```java
public class SchedulerJobResponse {
    UUID id;
    String name;
    String description;
    JobType jobType;
    String cronExpression;
    String timeZone;
    Boolean enabled;
    Boolean allowConcurrent;
    JobStatus status;
    ZonedDateTime lastRunAt;
    ZonedDateTime nextRunAt;
    JobRunStatus lastRunStatus;
    Integer consecutiveFailures;
    Integer maxRetryAttempts;
    Integer retryDelaySeconds;
    Integer timeoutSeconds;
    String[] tags;
    
    // Embedded config (type-specific):
    Object config;  // ProgramConfigResponse | SqlConfigResponse | RestConfigResponse | SoapConfigResponse
    
    // Recent runs summary:
    List<SchedulerJobRunSummary> recentRuns;  // last 5
    
    // Audit:
    String createdBy;
    ZonedDateTime createdDate;
    String lastModifiedBy;
    ZonedDateTime lastModifiedDate;
}
```

### `SchedulerJobRunResponse.java`
```java
public class SchedulerJobRunResponse {
    UUID id;
    UUID jobId;
    String jobName;
    JobTriggerType triggerType;
    JobRunStatus status;
    Integer attemptNumber;
    ZonedDateTime startedAt;
    ZonedDateTime finishedAt;
    Long durationMs;
    String stdoutOutput;    // may be truncated
    String stderrOutput;    // may be truncated
    Integer exitCode;
    Integer httpStatusCode;
    Long rowsAffected;
    String responseBody;    // may be truncated
    String errorMessage;
    String triggeredBy;
}
```

---

## Appendix B: Cron Wizard JavaScript API

```javascript
// Initialize wizard
const wizard = new CronWizard(document.getElementById('cron-wizard-container'), {
    onChange: (expression) => {
        console.log('New expression:', expression);
        myForm.querySelector('#cronExpression').value = expression;
    },
    initialExpression: '0 0 9 * * MON-FRI',
    timezone: 'UTC',
    previewCount: 5,
    apiBase: '/api/scheduler/cron'
});

// Get current expression
const expr = wizard.getExpression(); // "0 0 9 * * MON-FRI"

// Set expression programmatically
wizard.setExpression('0 0/15 * * * *');

// Destroy
wizard.destroy();
```

---

*End of Plan — Ready for implementation in phases.*
