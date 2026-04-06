-- V19: Scheduler core tables
-- Creates scheduler_jdbc_datasources first (referenced by scheduler_sql_configs in V20),
-- then scheduler_jobs and scheduler_job_runs.

-- ─── scheduler_jdbc_datasources ───────────────────────────────────────────────
CREATE TABLE scheduler_jdbc_datasources (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id),
    organization_id         UUID REFERENCES organizations(id),
    name                    VARCHAR(255) NOT NULL,
    description             TEXT,
    db_type                 VARCHAR(50) NOT NULL,
    host                    VARCHAR(1024),
    port                    INTEGER,
    database_name           VARCHAR(255),
    schema_name             VARCHAR(255),
    jdbc_url_override       VARCHAR(2048),
    username                VARCHAR(255),
    password_enc            VARCHAR(2048),
    min_pool_size           INTEGER NOT NULL DEFAULT 1,
    max_pool_size           INTEGER NOT NULL DEFAULT 5,
    connection_timeout_ms   INTEGER NOT NULL DEFAULT 5000,
    extra_properties        TEXT,
    enabled                 BOOLEAN NOT NULL DEFAULT TRUE,
    created_by              VARCHAR(255),
    created_date            TIMESTAMP WITH TIME ZONE,
    created_date_technical  BIGINT,
    last_modified_by        VARCHAR(255),
    last_modified_date      TIMESTAMP WITH TIME ZONE,
    last_modified_date_technical BIGINT
);

CREATE INDEX idx_scheduler_datasources_tenant ON scheduler_jdbc_datasources(tenant_id);

-- ─── scheduler_jobs ───────────────────────────────────────────────────────────
CREATE TABLE scheduler_jobs (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                   UUID NOT NULL REFERENCES tenants(id),
    organization_id             UUID REFERENCES organizations(id),
    name                        VARCHAR(255) NOT NULL,
    description                 TEXT,
    job_type                    VARCHAR(50) NOT NULL,
    cron_expression             VARCHAR(255) NOT NULL,
    time_zone                   VARCHAR(100) NOT NULL DEFAULT 'UTC',
    enabled                     BOOLEAN NOT NULL DEFAULT TRUE,
    allow_concurrent            BOOLEAN NOT NULL DEFAULT FALSE,
    status                      VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    last_run_at                 TIMESTAMP WITH TIME ZONE,
    next_run_at                 TIMESTAMP WITH TIME ZONE,
    last_run_status             VARCHAR(50),
    consecutive_failures        INTEGER NOT NULL DEFAULT 0,
    max_retry_attempts          INTEGER NOT NULL DEFAULT 0,
    retry_delay_seconds         INTEGER NOT NULL DEFAULT 60,
    timeout_seconds             INTEGER NOT NULL DEFAULT 300,
    max_output_bytes            INTEGER NOT NULL DEFAULT 102400,
    tags                        TEXT,
    created_by                  VARCHAR(255),
    created_date                TIMESTAMP WITH TIME ZONE,
    created_date_technical      BIGINT,
    last_modified_by            VARCHAR(255),
    last_modified_date          TIMESTAMP WITH TIME ZONE,
    last_modified_date_technical BIGINT
);

CREATE INDEX idx_scheduler_jobs_tenant   ON scheduler_jobs(tenant_id);
CREATE INDEX idx_scheduler_jobs_status   ON scheduler_jobs(tenant_id, status);
CREATE INDEX idx_scheduler_jobs_next_run ON scheduler_jobs(next_run_at) WHERE enabled = TRUE;

-- ─── scheduler_job_runs ───────────────────────────────────────────────────────
CREATE TABLE scheduler_job_runs (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id                  UUID NOT NULL REFERENCES scheduler_jobs(id) ON DELETE CASCADE,
    tenant_id               UUID NOT NULL REFERENCES tenants(id),
    trigger_type            VARCHAR(50),
    status                  VARCHAR(50),
    attempt_number          INTEGER NOT NULL DEFAULT 1,
    started_at              TIMESTAMP WITH TIME ZONE,
    finished_at             TIMESTAMP WITH TIME ZONE,
    duration_ms             BIGINT,
    stdout_output           TEXT,
    stderr_output           TEXT,
    exit_code               INTEGER,
    http_status_code        INTEGER,
    rows_affected           BIGINT,
    response_body           TEXT,
    error_message           TEXT,
    triggered_by            VARCHAR(255),
    created_date_technical  BIGINT
);

CREATE INDEX idx_scheduler_runs_job       ON scheduler_job_runs(job_id, started_at DESC);
CREATE INDEX idx_scheduler_runs_tenant    ON scheduler_job_runs(tenant_id);
CREATE INDEX idx_scheduler_runs_status    ON scheduler_job_runs(job_id, status);
CREATE INDEX idx_scheduler_runs_technical ON scheduler_job_runs USING BRIN(created_date_technical);
