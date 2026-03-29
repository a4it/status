-- ================================================================================
-- Status Monitoring Application — Complete Deployment DDL
-- ================================================================================
-- Run this file against a fresh PostgreSQL database to create the full schema.
-- Database: uptime (PostgreSQL 14+)
-- Generated: 2026-03-29
-- ================================================================================

-- Extension required for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ================================================================================
-- SECTION 1: MULTI-TENANT CORE
-- ================================================================================

-------------------------------------------------------------------------------
-- TABLE: tenants
-- Top-level tenants that own organizations, users, and status apps
-------------------------------------------------------------------------------
CREATE TABLE public.tenants (
    id                           UUID         PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    name                         VARCHAR(255) NOT NULL,
    domain                       VARCHAR(255) NOT NULL,
    settings                     JSONB        DEFAULT '{}'::jsonb,
    is_active                    BOOLEAN      DEFAULT true,
    created_by                   VARCHAR(255) NOT NULL,
    created_date                 TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by             VARCHAR(255) NOT NULL,
    last_modified_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical       BIGINT       NOT NULL,
    last_modified_date_technical BIGINT       NOT NULL
);

CREATE UNIQUE INDEX tenants_domain_key ON public.tenants (domain);

COMMENT ON TABLE  public.tenants        IS 'Top-level tenants that own organizations, users, and status apps';
COMMENT ON COLUMN public.tenants.id     IS 'Primary key identifier for the tenant';
COMMENT ON COLUMN public.tenants.name   IS 'Human-readable name of the tenant';
COMMENT ON COLUMN public.tenants.domain IS 'Domain associated with the tenant (e.g., company.com)';

-------------------------------------------------------------------------------
-- TABLE: organizations
-------------------------------------------------------------------------------
CREATE TABLE public.organizations (
    id                           UUID         NOT NULL PRIMARY KEY,
    name                         VARCHAR(255) NOT NULL UNIQUE,
    description                  TEXT,
    email                        VARCHAR(255),
    phone                        VARCHAR(50),
    website                      VARCHAR(255),
    address                      TEXT,
    logo_url                     VARCHAR(255),
    status                       VARCHAR(50)  DEFAULT 'ACTIVE' NOT NULL,
    created_by                   VARCHAR(255) NOT NULL,
    created_date                 TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by             VARCHAR(255) NOT NULL,
    last_modified_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical       BIGINT       NOT NULL,
    last_modified_date_technical BIGINT       NOT NULL,
    version                      BIGINT,
    subscription_exempt          BOOLEAN      NOT NULL,
    throttling_enabled           BOOLEAN      DEFAULT true NOT NULL,
    organization_type            VARCHAR(20)  NOT NULL,
    vat_number                   VARCHAR(50),
    country                      VARCHAR(100),
    postalcode                   TEXT,
    community                    TEXT,
    type                         VARCHAR(255),
    tenant_id                    UUID
        CONSTRAINT fk_tenants_organization
            REFERENCES public.tenants (id)
            ON UPDATE CASCADE
            ON DELETE SET NULL
);

CREATE INDEX idx_organization_name   ON public.organizations (name);
CREATE INDEX idx_organization_status ON public.organizations (status);
CREATE INDEX idx_organization_tenant ON public.organizations (tenant_id);

COMMENT ON TABLE  public.organizations              IS 'Organizations registered in the system, belonging to a tenant';
COMMENT ON COLUMN public.organizations.id           IS 'Primary key identifier for the organization';
COMMENT ON COLUMN public.organizations.name         IS 'Unique name of the organization';
COMMENT ON COLUMN public.organizations.status       IS 'Lifecycle status of the organization (e.g., ACTIVE, INACTIVE)';
COMMENT ON COLUMN public.organizations.tenant_id    IS 'Reference to the tenant that owns this organization';

-------------------------------------------------------------------------------
-- TABLE: users
-------------------------------------------------------------------------------
CREATE TABLE public.users (
    id                           UUID         NOT NULL PRIMARY KEY,
    created_by                   VARCHAR(255),
    created_date                 TIMESTAMP(6) WITH TIME ZONE,
    created_date_technical       BIGINT,
    last_modified_by             VARCHAR(255),
    last_modified_date           TIMESTAMP(6) WITH TIME ZONE,
    last_modified_date_technical BIGINT,
    version                      BIGINT,
    enabled                      BOOLEAN      NOT NULL,
    full_name                    VARCHAR(255),
    password                     VARCHAR(255) NOT NULL,
    refresh_token                VARCHAR(255),
    username                     VARCHAR(255) NOT NULL
        CONSTRAINT uk_r43af9ap4edm43mdmtq01oddj6 UNIQUE,
    email                        VARCHAR(255) UNIQUE,
    role                         VARCHAR(20),
    status                       VARCHAR(30),
    type                         VARCHAR(255),
    organization_id              UUID
        CONSTRAINT fk_users_organization
            REFERENCES public.organizations (id)
            ON UPDATE CASCADE
            ON DELETE SET NULL
);

CREATE INDEX idx_users_organization ON public.users (organization_id);

COMMENT ON TABLE  public.users                 IS 'Admin users who can access the web administration interface';
COMMENT ON COLUMN public.users.id              IS 'Primary key identifier for the user';
COMMENT ON COLUMN public.users.username        IS 'Unique username used for login to the admin interface';
COMMENT ON COLUMN public.users.email           IS 'Unique email address of the administrator user';
COMMENT ON COLUMN public.users.role            IS 'Role of the user determining their permissions (e.g., ADMIN, USER)';
COMMENT ON COLUMN public.users.refresh_token   IS 'JWT refresh token for the user''s session';
COMMENT ON COLUMN public.users.organization_id IS 'Reference to the organization that this user belongs to';

-- ================================================================================
-- SECTION 2: STATUS PLATFORM HIERARCHY
-- ================================================================================

-------------------------------------------------------------------------------
-- TABLE: status_platforms
-- Higher-level platforms that can group multiple status applications together
-------------------------------------------------------------------------------
CREATE TABLE public.status_platforms (
    id                           UUID         PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    name                         VARCHAR(255) NOT NULL,
    description                  TEXT,
    slug                         VARCHAR(255) NOT NULL,
    logo_url                     VARCHAR(500),
    website_url                  VARCHAR(500),
    status                       VARCHAR(50)  DEFAULT 'OPERATIONAL' NOT NULL,
    is_public                    BOOLEAN      DEFAULT true NOT NULL,
    position                     INT          DEFAULT 0,
    tenant_id                    UUID         REFERENCES public.tenants (id),
    organization_id              UUID         REFERENCES public.organizations (id),
    -- Health check configuration
    check_enabled                BOOLEAN      DEFAULT false,
    check_type                   VARCHAR(50)  DEFAULT 'NONE',
    check_url                    VARCHAR(500),
    check_interval_seconds       INTEGER      DEFAULT 60,
    check_timeout_seconds        INTEGER      DEFAULT 10,
    check_expected_status        INTEGER      DEFAULT 200,
    check_failure_threshold      INTEGER      DEFAULT 3,
    last_check_at                TIMESTAMP WITH TIME ZONE,
    last_check_success           BOOLEAN,
    last_check_message           VARCHAR(1000),
    consecutive_failures         INTEGER      DEFAULT 0,
    -- Audit fields
    created_by                   VARCHAR(255) NOT NULL,
    created_date                 TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by             VARCHAR(255) NOT NULL,
    last_modified_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical       BIGINT       NOT NULL,
    last_modified_date_technical BIGINT       NOT NULL
);

CREATE UNIQUE INDEX uk_status_platforms_tenant_slug ON public.status_platforms (tenant_id, slug);
CREATE INDEX idx_status_platforms_name   ON public.status_platforms (name);
CREATE INDEX idx_status_platforms_tenant ON public.status_platforms (tenant_id);
CREATE INDEX idx_status_platforms_org    ON public.status_platforms (organization_id);
CREATE INDEX idx_status_platforms_status ON public.status_platforms (status);

COMMENT ON TABLE  public.status_platforms                       IS 'Higher-level platforms that can group multiple status applications together';
COMMENT ON COLUMN public.status_platforms.slug                  IS 'URL-friendly slug for the platform (e.g., atlassian-cloud)';
COMMENT ON COLUMN public.status_platforms.status                IS 'Aggregated current status (OPERATIONAL, DEGRADED, MAJOR_OUTAGE)';
COMMENT ON COLUMN public.status_platforms.check_type            IS 'Type of health check: NONE, PING, HTTP_GET, SPRING_BOOT_HEALTH, TCP_PORT';
COMMENT ON COLUMN public.status_platforms.check_failure_threshold IS 'Number of consecutive failures before status change';

-------------------------------------------------------------------------------
-- TABLE: status_apps
-- Individual applications that expose their own status pages
-------------------------------------------------------------------------------
CREATE TABLE public.status_apps (
    id                           UUID         PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    name                         VARCHAR(255) NOT NULL,
    description                  TEXT,
    slug                         VARCHAR(255) NOT NULL,
    is_public                    BOOLEAN      DEFAULT true NOT NULL,
    status                       VARCHAR(50)  DEFAULT 'OPERATIONAL' NOT NULL,
    tenant_id                    UUID         REFERENCES public.tenants (id),
    organization_id              UUID         REFERENCES public.organizations (id),
    platform_id                  UUID         REFERENCES public.status_platforms (id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    api_key                      VARCHAR(64),
    -- Health check configuration
    check_enabled                BOOLEAN      DEFAULT false,
    check_type                   VARCHAR(50)  DEFAULT 'NONE',
    check_url                    VARCHAR(500),
    check_interval_seconds       INTEGER      DEFAULT 60,
    check_timeout_seconds        INTEGER      DEFAULT 10,
    check_expected_status        INTEGER      DEFAULT 200,
    check_failure_threshold      INTEGER      DEFAULT 3,
    last_check_at                TIMESTAMP WITH TIME ZONE,
    last_check_success           BOOLEAN,
    last_check_message           VARCHAR(1000),
    consecutive_failures         INTEGER      DEFAULT 0,
    -- Audit fields
    created_by                   VARCHAR(255) NOT NULL,
    created_date                 TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by             VARCHAR(255) NOT NULL,
    last_modified_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical       BIGINT       NOT NULL,
    last_modified_date_technical BIGINT       NOT NULL
);

CREATE UNIQUE INDEX uk_status_apps_tenant_slug ON public.status_apps (tenant_id, slug);
CREATE UNIQUE INDEX idx_status_apps_api_key    ON public.status_apps (api_key) WHERE api_key IS NOT NULL;
CREATE INDEX idx_status_apps_name     ON public.status_apps (name);
CREATE INDEX idx_status_apps_tenant   ON public.status_apps (tenant_id);
CREATE INDEX idx_status_apps_org      ON public.status_apps (organization_id);
CREATE INDEX idx_status_apps_platform ON public.status_apps (platform_id);

COMMENT ON TABLE  public.status_apps              IS 'Applications that expose their own status pages (e.g., Jira Software, Jira Service Management)';
COMMENT ON COLUMN public.status_apps.slug         IS 'URL-friendly slug for the status application (e.g., jira-software)';
COMMENT ON COLUMN public.status_apps.api_key      IS 'API key for authenticating event logging requests';
COMMENT ON COLUMN public.status_apps.platform_id  IS 'Reference to the parent platform this application belongs to';

-------------------------------------------------------------------------------
-- TABLE: status_components
-- Logical sub-components of a status application
-------------------------------------------------------------------------------
CREATE TABLE public.status_components (
    id                           UUID         PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    app_id                       UUID         NOT NULL
        REFERENCES public.status_apps (id) ON UPDATE CASCADE ON DELETE CASCADE,
    name                         VARCHAR(255) NOT NULL,
    description                  TEXT,
    status                       VARCHAR(50)  DEFAULT 'OPERATIONAL' NOT NULL,
    position                     INT          DEFAULT 0,
    group_name                   VARCHAR(255),
    api_key                      VARCHAR(64),
    -- Health check configuration
    check_inherit_from_app       BOOLEAN      DEFAULT true,
    check_enabled                BOOLEAN      DEFAULT false,
    check_type                   VARCHAR(50)  DEFAULT 'NONE',
    check_url                    VARCHAR(500),
    check_interval_seconds       INTEGER      DEFAULT 60,
    check_timeout_seconds        INTEGER      DEFAULT 10,
    check_expected_status        INTEGER      DEFAULT 200,
    check_failure_threshold      INTEGER      DEFAULT 3,
    last_check_at                TIMESTAMP WITH TIME ZONE,
    last_check_success           BOOLEAN,
    last_check_message           VARCHAR(1000),
    consecutive_failures         INTEGER      DEFAULT 0,
    -- Audit fields
    created_by                   VARCHAR(255) NOT NULL,
    created_date                 TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by             VARCHAR(255) NOT NULL,
    last_modified_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical       BIGINT       NOT NULL,
    last_modified_date_technical BIGINT       NOT NULL
);

CREATE UNIQUE INDEX uk_status_components_app_name  ON public.status_components (app_id, name);
CREATE UNIQUE INDEX idx_status_components_api_key  ON public.status_components (api_key) WHERE api_key IS NOT NULL;
CREATE INDEX idx_status_components_app    ON public.status_components (app_id);
CREATE INDEX idx_status_components_status ON public.status_components (status);

COMMENT ON TABLE  public.status_components                       IS 'Logical components or subsystems of a status application (e.g., API, Web UI, Database)';
COMMENT ON COLUMN public.status_components.app_id               IS 'Reference to the status application to which this component belongs';
COMMENT ON COLUMN public.status_components.group_name           IS 'Optional grouping label used to group related components on the status page';
COMMENT ON COLUMN public.status_components.check_inherit_from_app IS 'Whether to inherit health check configuration from the parent app';

-- ================================================================================
-- SECTION 3: INCIDENTS AND MAINTENANCE
-- ================================================================================

-------------------------------------------------------------------------------
-- TABLE: status_incidents
-------------------------------------------------------------------------------
CREATE TABLE public.status_incidents (
    id                           UUID         PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    app_id                       UUID         NOT NULL
        REFERENCES public.status_apps (id) ON UPDATE CASCADE ON DELETE CASCADE,
    title                        VARCHAR(255) NOT NULL,
    description                  TEXT,
    status                       VARCHAR(50)  NOT NULL,
    severity                     VARCHAR(50)  NOT NULL,
    impact                       VARCHAR(50),
    started_at                   TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at                  TIMESTAMP WITH TIME ZONE,
    is_public                    BOOLEAN      DEFAULT true NOT NULL,
    created_by                   VARCHAR(255) NOT NULL,
    created_date                 TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by             VARCHAR(255) NOT NULL,
    last_modified_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical       BIGINT       NOT NULL,
    last_modified_date_technical BIGINT       NOT NULL
);

CREATE INDEX idx_status_incidents_app     ON public.status_incidents (app_id);
CREATE INDEX idx_status_incidents_status  ON public.status_incidents (status);
CREATE INDEX idx_status_incidents_started ON public.status_incidents (started_at);

COMMENT ON TABLE  public.status_incidents          IS 'Incidents representing service disruptions or outages for an application';
COMMENT ON COLUMN public.status_incidents.status   IS 'Current status of the incident (INVESTIGATING, IDENTIFIED, MONITORING, RESOLVED)';
COMMENT ON COLUMN public.status_incidents.severity IS 'Severity level of the incident (MINOR, MAJOR, CRITICAL)';
COMMENT ON COLUMN public.status_incidents.impact   IS 'High-level description of the incident impact (PARTIAL_OUTAGE, MAJOR_OUTAGE)';

-------------------------------------------------------------------------------
-- TABLE: status_incident_updates
-------------------------------------------------------------------------------
CREATE TABLE public.status_incident_updates (
    id                           UUID         PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    incident_id                  UUID         NOT NULL
        REFERENCES public.status_incidents (id) ON UPDATE CASCADE ON DELETE CASCADE,
    status                       VARCHAR(50)  NOT NULL,
    message                      TEXT         NOT NULL,
    update_time                  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by                   VARCHAR(255) NOT NULL,
    created_date                 TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by             VARCHAR(255) NOT NULL,
    last_modified_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical       BIGINT       NOT NULL,
    last_modified_date_technical BIGINT       NOT NULL
);

CREATE INDEX idx_status_incident_updates_incident ON public.status_incident_updates (incident_id);
CREATE INDEX idx_status_incident_updates_time     ON public.status_incident_updates (update_time);

COMMENT ON TABLE  public.status_incident_updates             IS 'Timeline of status updates and messages associated with an incident';
COMMENT ON COLUMN public.status_incident_updates.incident_id IS 'Reference to the incident this update belongs to';
COMMENT ON COLUMN public.status_incident_updates.update_time IS 'Timestamp when this update was recorded or made public';

-------------------------------------------------------------------------------
-- TABLE: status_incident_components
-- Join table: incidents to affected components
-------------------------------------------------------------------------------
CREATE TABLE public.status_incident_components (
    id               UUID        PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    incident_id      UUID        NOT NULL
        REFERENCES public.status_incidents (id) ON UPDATE CASCADE ON DELETE CASCADE,
    component_id     UUID        NOT NULL
        REFERENCES public.status_components (id) ON UPDATE CASCADE ON DELETE CASCADE,
    component_status VARCHAR(50) NOT NULL
);

CREATE UNIQUE INDEX uk_status_incident_component             ON public.status_incident_components (incident_id, component_id);
CREATE INDEX idx_status_incident_components_incident  ON public.status_incident_components (incident_id);
CREATE INDEX idx_status_incident_components_component ON public.status_incident_components (component_id);

COMMENT ON TABLE  public.status_incident_components                  IS 'Mapping between incidents and the components they affect';
COMMENT ON COLUMN public.status_incident_components.component_status IS 'Status of the component for this incident (DEGRADED, PARTIAL_OUTAGE, MAJOR_OUTAGE)';

-------------------------------------------------------------------------------
-- TABLE: status_maintenance
-------------------------------------------------------------------------------
CREATE TABLE public.status_maintenance (
    id                           UUID         PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    app_id                       UUID         NOT NULL
        REFERENCES public.status_apps (id) ON UPDATE CASCADE ON DELETE CASCADE,
    title                        VARCHAR(255) NOT NULL,
    description                  TEXT,
    status                       VARCHAR(50)  NOT NULL,
    starts_at                    TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at                      TIMESTAMP WITH TIME ZONE NOT NULL,
    is_public                    BOOLEAN      DEFAULT true NOT NULL,
    created_by                   VARCHAR(255) NOT NULL,
    created_date                 TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by             VARCHAR(255) NOT NULL,
    last_modified_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical       BIGINT       NOT NULL,
    last_modified_date_technical BIGINT       NOT NULL
);

CREATE INDEX idx_status_maintenance_app    ON public.status_maintenance (app_id);
CREATE INDEX idx_status_maintenance_status ON public.status_maintenance (status);
CREATE INDEX idx_status_maintenance_starts ON public.status_maintenance (starts_at);

COMMENT ON TABLE  public.status_maintenance        IS 'Scheduled maintenance windows for an application';
COMMENT ON COLUMN public.status_maintenance.status IS 'Current status (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED)';

-------------------------------------------------------------------------------
-- TABLE: status_maintenance_components
-- Join table: maintenance to affected components
-------------------------------------------------------------------------------
CREATE TABLE public.status_maintenance_components (
    id             UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    maintenance_id UUID NOT NULL
        REFERENCES public.status_maintenance (id) ON UPDATE CASCADE ON DELETE CASCADE,
    component_id   UUID NOT NULL
        REFERENCES public.status_components (id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_status_maintenance_component            ON public.status_maintenance_components (maintenance_id, component_id);
CREATE INDEX idx_status_maintenance_components_maint ON public.status_maintenance_components (maintenance_id);
CREATE INDEX idx_status_maintenance_components_comp  ON public.status_maintenance_components (component_id);

COMMENT ON TABLE public.status_maintenance_components IS 'Mapping between scheduled maintenance entries and the components they affect';

-- ================================================================================
-- SECTION 4: UPTIME HISTORY AND EVENTS
-- ================================================================================

-------------------------------------------------------------------------------
-- TABLE: status_uptime_history
-- Daily uptime statistics for apps, components, and platforms
-------------------------------------------------------------------------------
CREATE TABLE public.status_uptime_history (
    id                   UUID          PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    app_id               UUID          NOT NULL
        REFERENCES public.status_apps (id) ON UPDATE CASCADE ON DELETE CASCADE,
    component_id         UUID
        REFERENCES public.status_components (id) ON UPDATE CASCADE ON DELETE CASCADE,
    platform_id          UUID
        REFERENCES public.status_platforms (id) ON DELETE CASCADE,
    record_date          DATE          NOT NULL,
    status               VARCHAR(50)   NOT NULL DEFAULT 'OPERATIONAL',
    uptime_percentage    DECIMAL(6, 3) NOT NULL DEFAULT 100.000,
    total_minutes        INT           NOT NULL DEFAULT 1440,
    operational_minutes  INT           NOT NULL DEFAULT 1440,
    degraded_minutes     INT           NOT NULL DEFAULT 0,
    outage_minutes       INT           NOT NULL DEFAULT 0,
    maintenance_minutes  INT           NOT NULL DEFAULT 0,
    incident_count       INT           NOT NULL DEFAULT 0,
    maintenance_count    INT           NOT NULL DEFAULT 0,
    created_date         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    last_modified_date   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- One record per day per app (when component_id is null)
CREATE UNIQUE INDEX uk_uptime_history_app_date
    ON public.status_uptime_history (app_id, record_date)
    WHERE component_id IS NULL;

-- One record per day per component
CREATE UNIQUE INDEX uk_uptime_history_component_date
    ON public.status_uptime_history (component_id, record_date)
    WHERE component_id IS NOT NULL;

CREATE INDEX idx_uptime_history_app_date       ON public.status_uptime_history (app_id, record_date);
CREATE INDEX idx_uptime_history_component_date ON public.status_uptime_history (component_id, record_date);
CREATE INDEX idx_uptime_history_date           ON public.status_uptime_history (record_date);

COMMENT ON TABLE  public.status_uptime_history                   IS 'Daily uptime history for apps and components — used to display the 90-day uptime chart';
COMMENT ON COLUMN public.status_uptime_history.uptime_percentage IS 'Calculated uptime percentage for the day';
COMMENT ON COLUMN public.status_uptime_history.total_minutes     IS 'Total minutes in the day (typically 1440)';

-------------------------------------------------------------------------------
-- TABLE: notification_subscribers
-- Email subscribers for incident notifications
-------------------------------------------------------------------------------
CREATE TABLE public.notification_subscribers (
    id                            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    app_id                        UUID         NOT NULL REFERENCES public.status_apps (id) ON DELETE CASCADE,
    email                         VARCHAR(255) NOT NULL,
    name                          VARCHAR(255),
    is_active                     BOOLEAN      NOT NULL DEFAULT true,
    is_verified                   BOOLEAN      NOT NULL DEFAULT false,
    verification_token            VARCHAR(255),
    verification_token_expires_at TIMESTAMP WITH TIME ZONE,
    created_by                    VARCHAR(255) NOT NULL,
    created_date                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by              VARCHAR(255) NOT NULL,
    last_modified_date            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_date_technical        BIGINT       NOT NULL DEFAULT (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000)::BIGINT,
    last_modified_date_technical  BIGINT       NOT NULL DEFAULT (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000)::BIGINT,
    UNIQUE (app_id, email)
);

CREATE INDEX idx_notification_subscribers_app_id             ON public.notification_subscribers (app_id);
CREATE INDEX idx_notification_subscribers_email              ON public.notification_subscribers (email);
CREATE INDEX idx_notification_subscribers_verification_token ON public.notification_subscribers (verification_token);
CREATE INDEX idx_notification_subscribers_active_verified    ON public.notification_subscribers (app_id, is_active, is_verified);

COMMENT ON TABLE  public.notification_subscribers                    IS 'Email subscribers who receive notifications for status application incidents';
COMMENT ON COLUMN public.notification_subscribers.is_verified        IS 'Whether the email address has been verified';
COMMENT ON COLUMN public.notification_subscribers.verification_token IS 'Token sent to the subscriber to verify their email address';

-------------------------------------------------------------------------------
-- TABLE: platform_events
-- Event logs from platforms and components
-------------------------------------------------------------------------------
CREATE TABLE public.platform_events (
    id                     UUID         PRIMARY KEY,
    app_id                 UUID         NOT NULL REFERENCES public.status_apps (id) ON DELETE CASCADE,
    component_id           UUID         REFERENCES public.status_components (id) ON DELETE CASCADE,
    severity               VARCHAR(20)  NOT NULL,
    source                 VARCHAR(255),
    message                TEXT         NOT NULL,
    details                TEXT,
    event_time             TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical BIGINT       NOT NULL
);

CREATE INDEX idx_platform_events_app_id                  ON public.platform_events (app_id);
CREATE INDEX idx_platform_events_component_id            ON public.platform_events (component_id);
CREATE INDEX idx_platform_events_severity                ON public.platform_events (severity);
CREATE INDEX idx_platform_events_event_time              ON public.platform_events (event_time);
CREATE INDEX idx_platform_events_created_date_technical  ON public.platform_events (created_date_technical);
CREATE INDEX idx_platform_events_message_search          ON public.platform_events USING gin (to_tsvector('english', message));

COMMENT ON TABLE  public.platform_events          IS 'Events logged from platforms and components for monitoring and debugging';
COMMENT ON COLUMN public.platform_events.severity IS 'Severity level: INFO, WARNING, ERROR, CRITICAL';
COMMENT ON COLUMN public.platform_events.details  IS 'Additional event details (can contain JSON or structured data)';

-------------------------------------------------------------------------------
-- TABLE: health_check_settings
-- Global configuration for the health check system
-------------------------------------------------------------------------------
CREATE TABLE public.health_check_settings (
    id                 UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key        VARCHAR(100)  UNIQUE NOT NULL,
    setting_value      VARCHAR(500)  NOT NULL,
    description        VARCHAR(500),
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  public.health_check_settings               IS 'Global configuration settings for the health check system';
COMMENT ON COLUMN public.health_check_settings.setting_key   IS 'Unique key for the setting';
COMMENT ON COLUMN public.health_check_settings.setting_value IS 'Value of the setting';

-- Default health-check settings
INSERT INTO public.health_check_settings (setting_key, setting_value, description) VALUES
    ('enabled',                   'true',  'Enable or disable all automated health checks'),
    ('scheduler_interval_ms',     '10000', 'Scheduler polling interval in milliseconds'),
    ('thread_pool_size',          '10',    'Number of threads for concurrent health checks'),
    ('default_interval_seconds',  '60',    'Default check interval for new entities in seconds'),
    ('default_timeout_seconds',   '10',    'Default timeout for health checks in seconds');

-- ================================================================================
-- SECTION 5: LOGS HUB
-- ================================================================================

-------------------------------------------------------------------------------
-- TABLE: log_api_keys
-- API keys used to authenticate log ingestion requests
-------------------------------------------------------------------------------
CREATE TABLE public.log_api_keys (
    id                     UUID         PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    tenant_id              UUID         REFERENCES public.tenants (id) ON UPDATE CASCADE ON DELETE CASCADE,
    name                   VARCHAR(255) NOT NULL,
    api_key                VARCHAR(255) NOT NULL,
    is_active              BOOLEAN      DEFAULT true,
    created_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical BIGINT       NOT NULL
);

CREATE UNIQUE INDEX idx_log_api_keys_key    ON public.log_api_keys (api_key);
CREATE INDEX        idx_log_api_keys_tenant ON public.log_api_keys (tenant_id);

COMMENT ON TABLE  public.log_api_keys         IS 'API keys used to authenticate log ingestion via REST API';
COMMENT ON COLUMN public.log_api_keys.api_key IS 'The secret API key value sent in the X-Log-Api-Key request header';

-------------------------------------------------------------------------------
-- TABLE: drop_rules
-- Rules evaluated before storing logs; matching logs are discarded
-------------------------------------------------------------------------------
CREATE TABLE public.drop_rules (
    id                     UUID         PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    tenant_id              UUID         REFERENCES public.tenants (id) ON UPDATE CASCADE ON DELETE SET NULL,
    name                   VARCHAR(255) NOT NULL,
    level                  VARCHAR(20),
    service                VARCHAR(255),
    message_pattern        VARCHAR(500),
    is_active              BOOLEAN      DEFAULT true,
    created_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical BIGINT       NOT NULL
);

CREATE INDEX idx_drop_rules_tenant ON public.drop_rules (tenant_id);
CREATE INDEX idx_drop_rules_active ON public.drop_rules (is_active);

COMMENT ON TABLE  public.drop_rules                 IS 'Rules that reject logs before storage (e.g., level=INFO AND service=payments)';
COMMENT ON COLUMN public.drop_rules.level           IS 'Log level to match (DEBUG, INFO, WARNING, ERROR, CRITICAL). NULL matches any.';
COMMENT ON COLUMN public.drop_rules.service         IS 'Service name to match. NULL matches any service.';
COMMENT ON COLUMN public.drop_rules.message_pattern IS 'Substring or pattern to match in the log message. NULL matches any message.';

-------------------------------------------------------------------------------
-- TABLE: logs
-- Main log storage table
-------------------------------------------------------------------------------
CREATE TABLE public.logs (
    id                     UUID         PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    tenant_id              UUID         REFERENCES public.tenants (id) ON UPDATE CASCADE ON DELETE SET NULL,
    log_timestamp          TIMESTAMP WITH TIME ZONE NOT NULL,
    level                  VARCHAR(20)  NOT NULL,
    service                VARCHAR(255) NOT NULL,
    message                TEXT         NOT NULL,
    metadata               TEXT,
    trace_id               VARCHAR(255),
    request_id             VARCHAR(255),
    created_date_technical BIGINT       NOT NULL
);

CREATE INDEX idx_logs_timestamp  ON public.logs (log_timestamp DESC);
CREATE INDEX idx_logs_level      ON public.logs (level);
CREATE INDEX idx_logs_service    ON public.logs (service);
CREATE INDEX idx_logs_tenant     ON public.logs (tenant_id);
CREATE INDEX idx_logs_trace_id   ON public.logs (trace_id)   WHERE trace_id IS NOT NULL;
CREATE INDEX idx_logs_request_id ON public.logs (request_id) WHERE request_id IS NOT NULL;

COMMENT ON TABLE  public.logs              IS 'Main log entry storage — accepts logs via REST ingestion endpoint';
COMMENT ON COLUMN public.logs.log_timestamp IS 'The timestamp of the log event (from the producing service)';
COMMENT ON COLUMN public.logs.level         IS 'Severity level: DEBUG, INFO, WARNING, ERROR, CRITICAL';
COMMENT ON COLUMN public.logs.metadata      IS 'Optional JSON object with arbitrary per-service fields';
COMMENT ON COLUMN public.logs.trace_id      IS 'Distributed tracing trace identifier';
COMMENT ON COLUMN public.logs.request_id    IS 'Per-request identifier for log correlation';

-------------------------------------------------------------------------------
-- TABLE: log_metrics
-- Aggregated log counts by service + level per time bucket
-------------------------------------------------------------------------------
CREATE TABLE public.log_metrics (
    id                     UUID         PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    tenant_id              UUID         REFERENCES public.tenants (id) ON UPDATE CASCADE ON DELETE SET NULL,
    service                VARCHAR(255) NOT NULL,
    level                  VARCHAR(20)  NOT NULL,
    bucket                 TIMESTAMP WITH TIME ZONE NOT NULL,
    bucket_type            VARCHAR(10)  NOT NULL DEFAULT 'MINUTE',
    count                  BIGINT       NOT NULL DEFAULT 0,
    created_date_technical BIGINT       NOT NULL
);

CREATE INDEX        idx_log_metrics_bucket  ON public.log_metrics (bucket DESC);
CREATE INDEX        idx_log_metrics_service ON public.log_metrics (service);
CREATE INDEX        idx_log_metrics_tenant  ON public.log_metrics (tenant_id);
CREATE UNIQUE INDEX idx_log_metrics_unique  ON public.log_metrics (tenant_id, service, level, bucket, bucket_type);

COMMENT ON TABLE  public.log_metrics             IS 'Pre-aggregated log counts per service+level per time bucket for dashboards and alerting';
COMMENT ON COLUMN public.log_metrics.bucket      IS 'Start of the time bucket (truncated to minute or hour)';
COMMENT ON COLUMN public.log_metrics.bucket_type IS 'Granularity of the bucket: MINUTE or HOUR';
COMMENT ON COLUMN public.log_metrics.count       IS 'Number of log entries in this bucket for this service+level';

-------------------------------------------------------------------------------
-- TABLE: alert_rules
-- Threshold-based alerting rules evaluated against log_metrics
-------------------------------------------------------------------------------
CREATE TABLE public.alert_rules (
    id                     UUID         PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    tenant_id              UUID         REFERENCES public.tenants (id) ON UPDATE CASCADE ON DELETE SET NULL,
    name                   VARCHAR(255) NOT NULL,
    service                VARCHAR(255),
    level                  VARCHAR(20),
    threshold_count        BIGINT       NOT NULL,
    window_minutes         INTEGER      NOT NULL,
    cooldown_minutes       INTEGER      DEFAULT 15,
    notification_type      VARCHAR(20)  NOT NULL,
    notification_target    TEXT,
    is_active              BOOLEAN      DEFAULT true,
    last_fired_at          TIMESTAMP WITH TIME ZONE,
    created_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical BIGINT       NOT NULL
);

CREATE INDEX idx_alert_rules_tenant ON public.alert_rules (tenant_id);
CREATE INDEX idx_alert_rules_active ON public.alert_rules (is_active);

COMMENT ON TABLE  public.alert_rules                    IS 'Rules that fire notifications when log counts exceed thresholds';
COMMENT ON COLUMN public.alert_rules.service            IS 'Service to watch. NULL matches all services.';
COMMENT ON COLUMN public.alert_rules.level              IS 'Log level to watch. NULL matches all levels.';
COMMENT ON COLUMN public.alert_rules.threshold_count    IS 'Fire alert when count exceeds this value within window_minutes';
COMMENT ON COLUMN public.alert_rules.window_minutes     IS 'Rolling time window in minutes for threshold evaluation';
COMMENT ON COLUMN public.alert_rules.cooldown_minutes   IS 'Minimum minutes between repeated alerts for the same rule';
COMMENT ON COLUMN public.alert_rules.notification_type  IS 'Delivery channel: EMAIL, SLACK, or WEBHOOK';
COMMENT ON COLUMN public.alert_rules.notification_target IS 'Email address, Slack webhook URL, or generic HTTP URL';
COMMENT ON COLUMN public.alert_rules.last_fired_at      IS 'When this rule last triggered an alert (for cooldown check)';

-- ================================================================================
-- SECTION 6: PROCESS MINING
-- ================================================================================

-------------------------------------------------------------------------------
-- TABLE: process_mining_retention_rules
-- Per-platform log retention policies
-------------------------------------------------------------------------------
CREATE TABLE public.process_mining_retention_rules (
    id                     UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id              UUID      REFERENCES public.tenants (id) ON DELETE CASCADE,
    platform_id            UUID      REFERENCES public.status_platforms (id) ON DELETE CASCADE,
    retention_days         INT       NOT NULL DEFAULT 30,
    enabled                BOOLEAN   NOT NULL DEFAULT true,
    last_run_at            TIMESTAMPTZ,
    last_run_deleted_count INT,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_retention_rules_tenant   ON public.process_mining_retention_rules (tenant_id);
CREATE INDEX idx_retention_rules_platform ON public.process_mining_retention_rules (platform_id);

COMMENT ON TABLE  public.process_mining_retention_rules                  IS 'Per-platform log retention policies for process mining data';
COMMENT ON COLUMN public.process_mining_retention_rules.retention_days   IS 'Number of days to retain logs for this platform';
COMMENT ON COLUMN public.process_mining_retention_rules.last_run_at      IS 'Timestamp of the last retention cleanup run';
COMMENT ON COLUMN public.process_mining_retention_rules.last_run_deleted_count IS 'Number of records deleted in the last retention run';

-- ================================================================================
-- END OF SCHEMA
-- ================================================================================
