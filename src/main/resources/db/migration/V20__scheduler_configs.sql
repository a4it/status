-- V20: Scheduler job-type configuration tables
-- Each config table has a unique FK to scheduler_jobs so only one config row
-- exists per job (enforced at DB level via UNIQUE constraint on job_id).

-- ─── scheduler_program_configs ────────────────────────────────────────────────
CREATE TABLE scheduler_program_configs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id              UUID NOT NULL UNIQUE REFERENCES scheduler_jobs(id) ON DELETE CASCADE,
    command             VARCHAR(2048),
    arguments           TEXT,
    working_directory   VARCHAR(1024),
    environment_vars    TEXT,
    shell_wrap          BOOLEAN NOT NULL DEFAULT FALSE,
    shell_path          VARCHAR(512) DEFAULT '/bin/bash',
    run_as_user         VARCHAR(255)
);

-- ─── scheduler_sql_configs ────────────────────────────────────────────────────
CREATE TABLE scheduler_sql_configs (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id                  UUID NOT NULL UNIQUE REFERENCES scheduler_jobs(id) ON DELETE CASCADE,
    datasource_id           UUID REFERENCES scheduler_jdbc_datasources(id),
    inline_db_type          VARCHAR(50),
    inline_jdbc_url         VARCHAR(2048),
    inline_username         VARCHAR(255),
    inline_password_enc     VARCHAR(2048),
    sql_statement           TEXT,
    sql_type                VARCHAR(50) NOT NULL DEFAULT 'DML',
    capture_result_set      BOOLEAN NOT NULL DEFAULT FALSE,
    max_result_rows         INTEGER NOT NULL DEFAULT 100,
    connection_timeout_ms   INTEGER NOT NULL DEFAULT 5000,
    query_timeout_seconds   INTEGER NOT NULL DEFAULT 60
);

-- ─── scheduler_rest_configs ───────────────────────────────────────────────────
CREATE TABLE scheduler_rest_configs (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id                      UUID NOT NULL UNIQUE REFERENCES scheduler_jobs(id) ON DELETE CASCADE,
    http_method                 VARCHAR(10) NOT NULL DEFAULT 'GET',
    url                         VARCHAR(4096),
    request_body                TEXT,
    content_type                VARCHAR(255) DEFAULT 'application/json',
    headers                     TEXT,
    query_params                TEXT,
    auth_type                   VARCHAR(50) NOT NULL DEFAULT 'NONE',
    auth_username               VARCHAR(255),
    auth_password_enc           VARCHAR(2048),
    auth_token_enc              VARCHAR(2048),
    auth_api_key_name           VARCHAR(255),
    auth_api_key_value_enc      VARCHAR(2048),
    auth_api_key_location       VARCHAR(50),
    auth_oauth2_token_url       VARCHAR(4096),
    auth_oauth2_client_id       VARCHAR(1024),
    auth_oauth2_client_secret_enc VARCHAR(2048),
    auth_oauth2_scope           VARCHAR(1024),
    ssl_verify                  BOOLEAN NOT NULL DEFAULT TRUE,
    ssl_truststore_path         VARCHAR(2048),
    ssl_truststore_password_enc VARCHAR(2048),
    connect_timeout_ms          INTEGER NOT NULL DEFAULT 5000,
    read_timeout_ms             INTEGER NOT NULL DEFAULT 30000,
    follow_redirects            BOOLEAN NOT NULL DEFAULT TRUE,
    max_response_bytes          INTEGER NOT NULL DEFAULT 102400,
    assert_status_code          INTEGER,
    assert_body_contains        TEXT,
    assert_json_path            VARCHAR(1024),
    assert_json_value           VARCHAR(1024)
);

-- ─── scheduler_soap_configs ───────────────────────────────────────────────────
CREATE TABLE scheduler_soap_configs (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id                  UUID NOT NULL UNIQUE REFERENCES scheduler_jobs(id) ON DELETE CASCADE,
    wsdl_url                VARCHAR(4096),
    endpoint_url            VARCHAR(4096),
    service_name            VARCHAR(512),
    port_name               VARCHAR(512),
    operation_name          VARCHAR(512),
    soap_action             VARCHAR(1024),
    soap_version            VARCHAR(10) NOT NULL DEFAULT '1.1',
    soap_envelope           TEXT,
    extra_headers           TEXT,
    auth_type               VARCHAR(50) NOT NULL DEFAULT 'NONE',
    auth_username           VARCHAR(255),
    auth_password_enc       VARCHAR(2048),
    auth_token_enc          VARCHAR(2048),
    ssl_verify              BOOLEAN NOT NULL DEFAULT TRUE,
    connect_timeout_ms      INTEGER NOT NULL DEFAULT 5000,
    read_timeout_ms         INTEGER NOT NULL DEFAULT 60000,
    max_response_bytes      INTEGER NOT NULL DEFAULT 524288
);
