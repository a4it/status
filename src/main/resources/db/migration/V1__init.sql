-- ================================================================================
-- Status Monitoring Application - Complete Database Schema
-- ================================================================================
-- This file contains the complete database schema for the status monitoring
-- application, consolidated from all migration files.
-- ================================================================================

-- Ensure gen_random_uuid() is available for UUID defaults
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-------------------------------------------------------------------------------
-- TABLE: public.tenants
-------------------------------------------------------------------------------

CREATE TABLE public.tenants (
    id                           UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    name                         VARCHAR(255) NOT NULL,
    domain                       VARCHAR(255) NOT NULL,
    settings                     JSONB DEFAULT '{}'::jsonb,
    is_active                    BOOLEAN DEFAULT true,
    created_by                   VARCHAR(255) NOT NULL,
    created_date                 TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by             VARCHAR(255) NOT NULL,
    last_modified_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical       BIGINT NOT NULL,
    last_modified_date_technical BIGINT NOT NULL
);

CREATE UNIQUE INDEX tenants_domain_key
    ON public.tenants USING btree (domain);

COMMENT ON TABLE public.tenants IS 'Top-level tenants that own organizations, users, and status apps';

COMMENT ON COLUMN public.tenants.id IS 'Primary key identifier for the tenant';
COMMENT ON COLUMN public.tenants.name IS 'Human-readable name of the tenant';
COMMENT ON COLUMN public.tenants.domain IS 'Domain associated with the tenant (e.g., company.com)';
COMMENT ON COLUMN public.tenants.settings IS 'JSONB object storing tenant-specific configuration settings';
COMMENT ON COLUMN public.tenants.is_active IS 'Flag indicating whether the tenant is active';
COMMENT ON COLUMN public.tenants.created_by IS 'Identifier of the user or system that created this tenant record';
COMMENT ON COLUMN public.tenants.created_date IS 'Timestamp when this tenant record was created';
COMMENT ON COLUMN public.tenants.last_modified_by IS 'Identifier of the user or system that last modified this tenant record';
COMMENT ON COLUMN public.tenants.last_modified_date IS 'Timestamp when this tenant record was last modified';
COMMENT ON COLUMN public.tenants.created_date_technical IS 'Technical timestamp in milliseconds when this tenant record was created';
COMMENT ON COLUMN public.tenants.last_modified_date_technical IS 'Technical timestamp in milliseconds when this tenant record was last modified';

-------------------------------------------------------------------------------
-- TABLE: public.organizations
-------------------------------------------------------------------------------

CREATE TABLE public.organizations
(
    id                           UUID NOT NULL PRIMARY KEY,
    name                         VARCHAR(255) NOT NULL UNIQUE,
    description                  TEXT,
    email                        VARCHAR(255),
    phone                        VARCHAR(50),
    website                      VARCHAR(255),
    address                      TEXT,
    logo_url                     VARCHAR(255),
    status                       VARCHAR(50) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_by                   VARCHAR(255) NOT NULL,
    created_date                 TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by             VARCHAR(255) NOT NULL,
    last_modified_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical       BIGINT NOT NULL,
    last_modified_date_technical BIGINT NOT NULL,
    version                      BIGINT,
    subscription_exempt          BOOLEAN NOT NULL,
    throttling_enabled           BOOLEAN DEFAULT true NOT NULL,
    organization_type            VARCHAR(20) NOT NULL,
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

ALTER TABLE public.organizations OWNER TO postgres;

CREATE INDEX idx_organization_name ON public.organizations (name);
CREATE INDEX idx_organization_status ON public.organizations (status);
CREATE INDEX idx_organization_tenant ON public.organizations (tenant_id);

COMMENT ON TABLE public.organizations IS 'Organizations registered in the system, belonging to a tenant';

COMMENT ON COLUMN public.organizations.id IS 'Primary key identifier for the organization';
COMMENT ON COLUMN public.organizations.name IS 'Unique name of the organization';
COMMENT ON COLUMN public.organizations.description IS 'Free-text description of the organization';
COMMENT ON COLUMN public.organizations.email IS 'Primary contact email address for the organization';
COMMENT ON COLUMN public.organizations.phone IS 'Primary contact phone number for the organization';
COMMENT ON COLUMN public.organizations.website IS 'Website URL of the organization';
COMMENT ON COLUMN public.organizations.address IS 'Postal or physical address of the organization';
COMMENT ON COLUMN public.organizations.logo_url IS 'URL of the organization''s logo';
COMMENT ON COLUMN public.organizations.status IS 'Lifecycle status of the organization (e.g., ACTIVE, INACTIVE)';
COMMENT ON COLUMN public.organizations.created_by IS 'Identifier of the user or system that created this organization record';
COMMENT ON COLUMN public.organizations.created_date IS 'Timestamp when this organization record was created';
COMMENT ON COLUMN public.organizations.last_modified_by IS 'Identifier of the user or system that last modified this organization record';
COMMENT ON COLUMN public.organizations.last_modified_date IS 'Timestamp when this organization record was last modified';
COMMENT ON COLUMN public.organizations.created_date_technical IS 'Technical timestamp in milliseconds when this organization record was created';
COMMENT ON COLUMN public.organizations.last_modified_date_technical IS 'Technical timestamp in milliseconds when this organization record was last modified';
COMMENT ON COLUMN public.organizations.version IS 'Optional version field for optimistic locking or auditing';
COMMENT ON COLUMN public.organizations.subscription_exempt IS 'Flag indicating whether this organization is exempt from subscription billing';
COMMENT ON COLUMN public.organizations.throttling_enabled IS 'Flag indicating whether throttling or rate limiting is enabled for this organization';
COMMENT ON COLUMN public.organizations.organization_type IS 'Type of organization (e.g., CUSTOMER, INTERNAL)';
COMMENT ON COLUMN public.organizations.vat_number IS 'VAT (Value Added Tax) identification number for the organization';
COMMENT ON COLUMN public.organizations.country IS 'Country where the organization is based';
COMMENT ON COLUMN public.organizations.postalcode IS 'Postal code for the organization''s address';
COMMENT ON COLUMN public.organizations.community IS 'Community or region associated with the organization';
COMMENT ON COLUMN public.organizations.type IS 'Additional classification or subtype of the organization';
COMMENT ON COLUMN public.organizations.tenant_id IS 'Reference to the tenant that owns this organization';

-------------------------------------------------------------------------------
-- TABLE: public.users
-------------------------------------------------------------------------------

CREATE TABLE public.users
(
    id                           UUID NOT NULL PRIMARY KEY,
    created_by                   VARCHAR(255),
    created_date                 TIMESTAMP(6) WITH TIME ZONE,
    created_date_technical       BIGINT,
    last_modified_by             VARCHAR(255),
    last_modified_date           TIMESTAMP(6) WITH TIME ZONE,
    last_modified_date_technical BIGINT,
    version                      BIGINT,
    enabled                      BOOLEAN NOT NULL,
    full_name                    VARCHAR(255),
    password                     VARCHAR(255) NOT NULL,
    refresh_token                VARCHAR(255),
    username                     VARCHAR(255) NOT NULL
        CONSTRAINT uk_r43af9ap4edm43mdmtq01oddj6 UNIQUE,
    email                        VARCHAR(255) UNIQUE,
    role                         VARCHAR(20),
    status                       VARCHAR(30),
    organization_id              UUID
        CONSTRAINT fk_users_organization
            REFERENCES public.organizations (id)
            ON UPDATE CASCADE
            ON DELETE SET NULL,
    type                         VARCHAR(255)
);

ALTER TABLE public.users OWNER TO postgres;

CREATE INDEX idx_users_organization ON public.users (organization_id);

COMMENT ON TABLE public.users IS 'Stores admin users who can access the web administration interface';

COMMENT ON COLUMN public.users.id IS 'Primary key identifier for the user';
COMMENT ON COLUMN public.users.created_by IS 'User or system that created this user record';
COMMENT ON COLUMN public.users.created_date IS 'Timestamp when this user record was first created';
COMMENT ON COLUMN public.users.created_date_technical IS 'Technical timestamp in milliseconds when this user record was created';
COMMENT ON COLUMN public.users.last_modified_by IS 'User or system that last modified this user record';
COMMENT ON COLUMN public.users.last_modified_date IS 'Timestamp when this user record was last updated';
COMMENT ON COLUMN public.users.last_modified_date_technical IS 'Technical timestamp in milliseconds when this user record was last updated';
COMMENT ON COLUMN public.users.version IS 'Optional version field for optimistic locking or auditing';
COMMENT ON COLUMN public.users.enabled IS 'Flag indicating whether the user account is enabled';
COMMENT ON COLUMN public.users.full_name IS 'Full name of the user';
COMMENT ON COLUMN public.users.password IS 'Securely hashed password for the user account';
COMMENT ON COLUMN public.users.refresh_token IS 'JWT refresh token for the user''s session';
COMMENT ON COLUMN public.users.username IS 'Unique username used for login to the admin interface';
COMMENT ON COLUMN public.users.email IS 'Unique email address of the administrator user';
COMMENT ON COLUMN public.users.role IS 'Role of the user determining their permissions (e.g., ADMIN, USER)';
COMMENT ON COLUMN public.users.status IS 'Current status of the user account (e.g., ACTIVE, INACTIVE, DELETED)';
COMMENT ON COLUMN public.users.organization_id IS 'Reference to the organization that this user belongs to';
COMMENT ON COLUMN public.users.type IS 'Additional classification or type of user';

-------------------------------------------------------------------------------
-- TABLE: public.status_platforms
-- Represents a higher-level platform that can group multiple status apps
-------------------------------------------------------------------------------

CREATE TABLE public.status_platforms
(
    id                           UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    name                         VARCHAR(255) NOT NULL,
    description                  TEXT,
    slug                         VARCHAR(255) NOT NULL,
    logo_url                     VARCHAR(500),
    website_url                  VARCHAR(500),
    status                       VARCHAR(50) DEFAULT 'OPERATIONAL' NOT NULL,
    is_public                    BOOLEAN DEFAULT true NOT NULL,
    position                     INT DEFAULT 0,
    tenant_id                    UUID REFERENCES public.tenants (id),
    organization_id              UUID REFERENCES public.organizations (id),
    -- Health check configuration fields
    check_enabled                BOOLEAN DEFAULT false,
    check_type                   VARCHAR(50) DEFAULT 'NONE',
    check_url                    VARCHAR(500),
    check_interval_seconds       INTEGER DEFAULT 60,
    check_timeout_seconds        INTEGER DEFAULT 10,
    check_expected_status        INTEGER DEFAULT 200,
    check_failure_threshold      INTEGER DEFAULT 3,
    last_check_at                TIMESTAMP WITH TIME ZONE,
    last_check_success           BOOLEAN,
    last_check_message           VARCHAR(1000),
    consecutive_failures         INTEGER DEFAULT 0,
    -- Audit fields
    created_by                   VARCHAR(255) NOT NULL,
    created_date                 TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by             VARCHAR(255) NOT NULL,
    last_modified_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical       BIGINT NOT NULL,
    last_modified_date_technical BIGINT NOT NULL
);

CREATE UNIQUE INDEX uk_status_platforms_tenant_slug ON public.status_platforms (tenant_id, slug);
CREATE INDEX idx_status_platforms_name ON public.status_platforms (name);
CREATE INDEX idx_status_platforms_tenant ON public.status_platforms (tenant_id);
CREATE INDEX idx_status_platforms_org ON public.status_platforms (organization_id);
CREATE INDEX idx_status_platforms_status ON public.status_platforms (status);

COMMENT ON TABLE public.status_platforms IS 'Higher-level platforms that can group multiple status applications together';

COMMENT ON COLUMN public.status_platforms.id IS 'Primary key identifier for the platform';
COMMENT ON COLUMN public.status_platforms.name IS 'Display name of the platform';
COMMENT ON COLUMN public.status_platforms.description IS 'Detailed description of the platform';
COMMENT ON COLUMN public.status_platforms.slug IS 'URL-friendly slug for the platform (e.g., atlassian-cloud)';
COMMENT ON COLUMN public.status_platforms.logo_url IS 'URL to the platform logo image';
COMMENT ON COLUMN public.status_platforms.website_url IS 'External website URL for the platform';
COMMENT ON COLUMN public.status_platforms.status IS 'Aggregated current status of the platform (e.g., OPERATIONAL, DEGRADED, MAJOR_OUTAGE)';
COMMENT ON COLUMN public.status_platforms.is_public IS 'Flag indicating whether the platform is publicly visible';
COMMENT ON COLUMN public.status_platforms.position IS 'Display order position for sorting platforms';
COMMENT ON COLUMN public.status_platforms.tenant_id IS 'Reference to the tenant that owns this platform';
COMMENT ON COLUMN public.status_platforms.organization_id IS 'Reference to the organization associated with this platform';
COMMENT ON COLUMN public.status_platforms.check_enabled IS 'Whether automatic health checking is enabled for this platform';
COMMENT ON COLUMN public.status_platforms.check_type IS 'Type of health check: NONE, PING, HTTP_GET, SPRING_BOOT_HEALTH, TCP_PORT';
COMMENT ON COLUMN public.status_platforms.check_url IS 'URL or host:port to check';
COMMENT ON COLUMN public.status_platforms.check_interval_seconds IS 'How often to perform the health check in seconds';
COMMENT ON COLUMN public.status_platforms.check_timeout_seconds IS 'Timeout for the health check in seconds';
COMMENT ON COLUMN public.status_platforms.check_expected_status IS 'Expected HTTP status code for HTTP_GET checks';
COMMENT ON COLUMN public.status_platforms.check_failure_threshold IS 'Number of consecutive failures before status change';
COMMENT ON COLUMN public.status_platforms.last_check_at IS 'Timestamp of the last health check';
COMMENT ON COLUMN public.status_platforms.last_check_success IS 'Whether the last health check was successful';
COMMENT ON COLUMN public.status_platforms.last_check_message IS 'Message from the last health check (error details or success info)';
COMMENT ON COLUMN public.status_platforms.consecutive_failures IS 'Current count of consecutive failed health checks';
COMMENT ON COLUMN public.status_platforms.created_by IS 'User or system that created this platform record';
COMMENT ON COLUMN public.status_platforms.created_date IS 'Timestamp when this platform record was created';
COMMENT ON COLUMN public.status_platforms.last_modified_by IS 'User or system that last modified this platform record';
COMMENT ON COLUMN public.status_platforms.last_modified_date IS 'Timestamp when this platform record was last modified';
COMMENT ON COLUMN public.status_platforms.created_date_technical IS 'Technical timestamp in milliseconds when this platform record was created';
COMMENT ON COLUMN public.status_platforms.last_modified_date_technical IS 'Technical timestamp in milliseconds when this platform record was last modified';

-------------------------------------------------------------------------------
-- TABLE: public.status_apps
-------------------------------------------------------------------------------

CREATE TABLE public.status_apps
(
    id                           UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    name                         VARCHAR(255) NOT NULL,
    description                  TEXT,
    slug                         VARCHAR(255) NOT NULL,
    is_public                    BOOLEAN DEFAULT true NOT NULL,
    status                       VARCHAR(50) DEFAULT 'OPERATIONAL' NOT NULL,
    tenant_id                    UUID REFERENCES public.tenants (id),
    organization_id              UUID REFERENCES public.organizations (id),
    platform_id                  UUID REFERENCES public.status_platforms (id)
        ON UPDATE CASCADE
        ON DELETE SET NULL,
    api_key                      VARCHAR(64),
    -- Health check configuration fields
    check_enabled                BOOLEAN DEFAULT false,
    check_type                   VARCHAR(50) DEFAULT 'NONE',
    check_url                    VARCHAR(500),
    check_interval_seconds       INTEGER DEFAULT 60,
    check_timeout_seconds        INTEGER DEFAULT 10,
    check_expected_status        INTEGER DEFAULT 200,
    check_failure_threshold      INTEGER DEFAULT 3,
    last_check_at                TIMESTAMP WITH TIME ZONE,
    last_check_success           BOOLEAN,
    last_check_message           VARCHAR(1000),
    consecutive_failures         INTEGER DEFAULT 0,
    -- Audit fields
    created_by                   VARCHAR(255) NOT NULL,
    created_date                 TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by             VARCHAR(255) NOT NULL,
    last_modified_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical       BIGINT NOT NULL,
    last_modified_date_technical BIGINT NOT NULL
);

CREATE UNIQUE INDEX uk_status_apps_tenant_slug ON public.status_apps (tenant_id, slug);
CREATE UNIQUE INDEX idx_status_apps_api_key ON public.status_apps(api_key) WHERE api_key IS NOT NULL;
CREATE INDEX idx_status_apps_name ON public.status_apps (name);
CREATE INDEX idx_status_apps_tenant ON public.status_apps (tenant_id);
CREATE INDEX idx_status_apps_org ON public.status_apps (organization_id);
CREATE INDEX idx_status_apps_platform ON public.status_apps (platform_id);

COMMENT ON TABLE public.status_apps IS 'Applications that expose their own status pages (e.g., Jira Software, Jira Service Management)';

COMMENT ON COLUMN public.status_apps.id IS 'Primary key identifier for the status application';
COMMENT ON COLUMN public.status_apps.name IS 'Display name of the status application (e.g., Jira Software)';
COMMENT ON COLUMN public.status_apps.description IS 'Detailed description of the status application';
COMMENT ON COLUMN public.status_apps.slug IS 'URL-friendly slug for the status application (e.g., jira-software)';
COMMENT ON COLUMN public.status_apps.is_public IS 'Flag indicating whether the status page for this application is publicly visible';
COMMENT ON COLUMN public.status_apps.status IS 'Aggregated current status of the application (e.g., OPERATIONAL, DEGRADED, MAJOR_OUTAGE)';
COMMENT ON COLUMN public.status_apps.tenant_id IS 'Reference to the tenant that owns this status application';
COMMENT ON COLUMN public.status_apps.organization_id IS 'Reference to the organization associated with this status application';
COMMENT ON COLUMN public.status_apps.platform_id IS 'Reference to the parent platform this application belongs to';
COMMENT ON COLUMN public.status_apps.api_key IS 'API key for authenticating event logging requests';
COMMENT ON COLUMN public.status_apps.check_enabled IS 'Whether automatic health checking is enabled for this app';
COMMENT ON COLUMN public.status_apps.check_type IS 'Type of health check: NONE, PING, HTTP_GET, SPRING_BOOT_HEALTH, TCP_PORT';
COMMENT ON COLUMN public.status_apps.check_url IS 'URL or host:port to check';
COMMENT ON COLUMN public.status_apps.check_interval_seconds IS 'How often to perform the health check in seconds';
COMMENT ON COLUMN public.status_apps.check_timeout_seconds IS 'Timeout for the health check in seconds';
COMMENT ON COLUMN public.status_apps.check_expected_status IS 'Expected HTTP status code for HTTP_GET checks';
COMMENT ON COLUMN public.status_apps.check_failure_threshold IS 'Number of consecutive failures before status change';
COMMENT ON COLUMN public.status_apps.last_check_at IS 'Timestamp of the last health check';
COMMENT ON COLUMN public.status_apps.last_check_success IS 'Whether the last health check was successful';
COMMENT ON COLUMN public.status_apps.last_check_message IS 'Message from the last health check (error details or success info)';
COMMENT ON COLUMN public.status_apps.consecutive_failures IS 'Current count of consecutive failed health checks';
COMMENT ON COLUMN public.status_apps.created_by IS 'User or system that created this status application record';
COMMENT ON COLUMN public.status_apps.created_date IS 'Timestamp when this status application record was created';
COMMENT ON COLUMN public.status_apps.last_modified_by IS 'User or system that last modified this status application record';
COMMENT ON COLUMN public.status_apps.last_modified_date IS 'Timestamp when this status application record was last modified';
COMMENT ON COLUMN public.status_apps.created_date_technical IS 'Technical timestamp in milliseconds when this status application record was created';
COMMENT ON COLUMN public.status_apps.last_modified_date_technical IS 'Technical timestamp in milliseconds when this status application record was last modified';

-------------------------------------------------------------------------------
-- TABLE: public.status_components
-------------------------------------------------------------------------------

CREATE TABLE public.status_components
(
    id                           UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    app_id                       UUID NOT NULL
        REFERENCES public.status_apps (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,
    name                         VARCHAR(255) NOT NULL,
    description                  TEXT,
    status                       VARCHAR(50) DEFAULT 'OPERATIONAL' NOT NULL,
    position                     INT DEFAULT 0,
    group_name                   VARCHAR(255),
    api_key                      VARCHAR(64),
    -- Health check configuration fields
    check_inherit_from_app       BOOLEAN DEFAULT true,
    check_enabled                BOOLEAN DEFAULT false,
    check_type                   VARCHAR(50) DEFAULT 'NONE',
    check_url                    VARCHAR(500),
    check_interval_seconds       INTEGER DEFAULT 60,
    check_timeout_seconds        INTEGER DEFAULT 10,
    check_expected_status        INTEGER DEFAULT 200,
    check_failure_threshold      INTEGER DEFAULT 3,
    last_check_at                TIMESTAMP WITH TIME ZONE,
    last_check_success           BOOLEAN,
    last_check_message           VARCHAR(1000),
    consecutive_failures         INTEGER DEFAULT 0,
    -- Audit fields
    created_by                   VARCHAR(255) NOT NULL,
    created_date                 TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by             VARCHAR(255) NOT NULL,
    last_modified_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical       BIGINT NOT NULL,
    last_modified_date_technical BIGINT NOT NULL
);

CREATE UNIQUE INDEX uk_status_components_app_name ON public.status_components (app_id, name);
CREATE UNIQUE INDEX idx_status_components_api_key ON public.status_components(api_key) WHERE api_key IS NOT NULL;
CREATE INDEX idx_status_components_app ON public.status_components (app_id);
CREATE INDEX idx_status_components_status ON public.status_components (status);

COMMENT ON TABLE public.status_components IS 'Logical components or subsystems of a status application (e.g., API, Web UI, Database)';

COMMENT ON COLUMN public.status_components.id IS 'Primary key identifier for the status component';
COMMENT ON COLUMN public.status_components.app_id IS 'Reference to the status application to which this component belongs';
COMMENT ON COLUMN public.status_components.name IS 'Display name of the component';
COMMENT ON COLUMN public.status_components.description IS 'Detailed description of the component and its responsibilities';
COMMENT ON COLUMN public.status_components.status IS 'Current status of the component (e.g., OPERATIONAL, DEGRADED, PARTIAL_OUTAGE, MAJOR_OUTAGE)';
COMMENT ON COLUMN public.status_components.position IS 'Ordering index used to sort components on the status page';
COMMENT ON COLUMN public.status_components.group_name IS 'Optional grouping label used to group related components on the status page';
COMMENT ON COLUMN public.status_components.api_key IS 'API key for authenticating event logging requests';
COMMENT ON COLUMN public.status_components.check_inherit_from_app IS 'Whether to inherit health check config from parent app';
COMMENT ON COLUMN public.status_components.check_enabled IS 'Whether automatic health checking is enabled for this component';
COMMENT ON COLUMN public.status_components.check_type IS 'Type of health check: NONE, PING, HTTP_GET, SPRING_BOOT_HEALTH, TCP_PORT';
COMMENT ON COLUMN public.status_components.check_url IS 'URL or host:port to check';
COMMENT ON COLUMN public.status_components.check_interval_seconds IS 'How often to perform the health check in seconds';
COMMENT ON COLUMN public.status_components.check_timeout_seconds IS 'Timeout for the health check in seconds';
COMMENT ON COLUMN public.status_components.check_expected_status IS 'Expected HTTP status code for HTTP_GET checks';
COMMENT ON COLUMN public.status_components.check_failure_threshold IS 'Number of consecutive failures before status change';
COMMENT ON COLUMN public.status_components.last_check_at IS 'Timestamp of the last health check';
COMMENT ON COLUMN public.status_components.last_check_success IS 'Whether the last health check was successful';
COMMENT ON COLUMN public.status_components.last_check_message IS 'Message from the last health check (error details or success info)';
COMMENT ON COLUMN public.status_components.consecutive_failures IS 'Current count of consecutive failed health checks';
COMMENT ON COLUMN public.status_components.created_by IS 'User or system that created this component record';
COMMENT ON COLUMN public.status_components.created_date IS 'Timestamp when this component record was created';
COMMENT ON COLUMN public.status_components.last_modified_by IS 'User or system that last modified this component record';
COMMENT ON COLUMN public.status_components.last_modified_date IS 'Timestamp when this component record was last modified';
COMMENT ON COLUMN public.status_components.created_date_technical IS 'Technical timestamp in milliseconds when this component record was created';
COMMENT ON COLUMN public.status_components.last_modified_date_technical IS 'Technical timestamp in milliseconds when this component record was last modified';

-------------------------------------------------------------------------------
-- TABLE: public.status_incidents
-------------------------------------------------------------------------------

CREATE TABLE public.status_incidents
(
    id                           UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    app_id                       UUID NOT NULL
        REFERENCES public.status_apps (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,
    title                        VARCHAR(255) NOT NULL,
    description                  TEXT,
    status                       VARCHAR(50) NOT NULL,
    severity                     VARCHAR(50) NOT NULL,
    impact                       VARCHAR(50),
    started_at                   TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at                  TIMESTAMP WITH TIME ZONE,
    is_public                    BOOLEAN DEFAULT true NOT NULL,
    created_by                   VARCHAR(255) NOT NULL,
    created_date                 TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by             VARCHAR(255) NOT NULL,
    last_modified_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical       BIGINT NOT NULL,
    last_modified_date_technical BIGINT NOT NULL
);

CREATE INDEX idx_status_incidents_app ON public.status_incidents (app_id);
CREATE INDEX idx_status_incidents_status ON public.status_incidents (status);
CREATE INDEX idx_status_incidents_started ON public.status_incidents (started_at);

COMMENT ON TABLE public.status_incidents IS 'Incidents representing service disruptions or outages for an application';

COMMENT ON COLUMN public.status_incidents.id IS 'Primary key identifier for the incident';
COMMENT ON COLUMN public.status_incidents.app_id IS 'Reference to the status application affected by this incident';
COMMENT ON COLUMN public.status_incidents.title IS 'Short, human-readable title describing the incident';
COMMENT ON COLUMN public.status_incidents.description IS 'Detailed description of the incident and its context';
COMMENT ON COLUMN public.status_incidents.status IS 'Current status of the incident (e.g., INVESTIGATING, IDENTIFIED, MONITORING, RESOLVED)';
COMMENT ON COLUMN public.status_incidents.severity IS 'Severity level of the incident (e.g., MINOR, MAJOR, CRITICAL)';
COMMENT ON COLUMN public.status_incidents.impact IS 'High-level description of the incident impact (e.g., PARTIAL_OUTAGE, MAJOR_OUTAGE)';
COMMENT ON COLUMN public.status_incidents.started_at IS 'Timestamp when the incident started or was first detected';
COMMENT ON COLUMN public.status_incidents.resolved_at IS 'Timestamp when the incident was fully resolved, if applicable';
COMMENT ON COLUMN public.status_incidents.is_public IS 'Flag indicating whether the incident is visible on the public status page';
COMMENT ON COLUMN public.status_incidents.created_by IS 'User or system that created this incident record';
COMMENT ON COLUMN public.status_incidents.created_date IS 'Timestamp when this incident record was created';
COMMENT ON COLUMN public.status_incidents.last_modified_by IS 'User or system that last modified this incident record';
COMMENT ON COLUMN public.status_incidents.last_modified_date IS 'Timestamp when this incident record was last modified';
COMMENT ON COLUMN public.status_incidents.created_date_technical IS 'Technical timestamp in milliseconds when this incident record was created';
COMMENT ON COLUMN public.status_incidents.last_modified_date_technical IS 'Technical timestamp in milliseconds when this incident record was last modified';

-------------------------------------------------------------------------------
-- TABLE: public.status_incident_updates
-------------------------------------------------------------------------------

CREATE TABLE public.status_incident_updates
(
    id                           UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    incident_id                  UUID NOT NULL
        REFERENCES public.status_incidents (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,
    status                       VARCHAR(50) NOT NULL,
    message                      TEXT NOT NULL,
    update_time                  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by                   VARCHAR(255) NOT NULL,
    created_date                 TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by             VARCHAR(255) NOT NULL,
    last_modified_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical       BIGINT NOT NULL,
    last_modified_date_technical BIGINT NOT NULL
);

CREATE INDEX idx_status_incident_updates_incident ON public.status_incident_updates (incident_id);
CREATE INDEX idx_status_incident_updates_time ON public.status_incident_updates (update_time);

COMMENT ON TABLE public.status_incident_updates IS 'Timeline of status updates and messages associated with an incident';

COMMENT ON COLUMN public.status_incident_updates.id IS 'Primary key identifier for the incident update';
COMMENT ON COLUMN public.status_incident_updates.incident_id IS 'Reference to the incident this update belongs to';
COMMENT ON COLUMN public.status_incident_updates.status IS 'Incident status at the time of this update (e.g., INVESTIGATING, IDENTIFIED, MONITORING, RESOLVED)';
COMMENT ON COLUMN public.status_incident_updates.message IS 'Human-readable message describing the update or progress on the incident';
COMMENT ON COLUMN public.status_incident_updates.update_time IS 'Timestamp when this update was recorded or made public';
COMMENT ON COLUMN public.status_incident_updates.created_by IS 'User or system that created this incident update record';
COMMENT ON COLUMN public.status_incident_updates.created_date IS 'Timestamp when this incident update record was created';
COMMENT ON COLUMN public.status_incident_updates.last_modified_by IS 'User or system that last modified this incident update record';
COMMENT ON COLUMN public.status_incident_updates.last_modified_date IS 'Timestamp when this incident update record was last modified';
COMMENT ON COLUMN public.status_incident_updates.created_date_technical IS 'Technical timestamp in milliseconds when this incident update record was created';
COMMENT ON COLUMN public.status_incident_updates.last_modified_date_technical IS 'Technical timestamp in milliseconds when this incident update record was last modified';

-------------------------------------------------------------------------------
-- TABLE: public.status_incident_components
-------------------------------------------------------------------------------

CREATE TABLE public.status_incident_components
(
    id                UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    incident_id       UUID NOT NULL
        REFERENCES public.status_incidents (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,
    component_id      UUID NOT NULL
        REFERENCES public.status_components (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,
    component_status  VARCHAR(50) NOT NULL
);

CREATE UNIQUE INDEX uk_status_incident_component ON public.status_incident_components (incident_id, component_id);
CREATE INDEX idx_status_incident_components_incident ON public.status_incident_components (incident_id);
CREATE INDEX idx_status_incident_components_component ON public.status_incident_components (component_id);

COMMENT ON TABLE public.status_incident_components IS 'Mapping between incidents and the components they affect, including per-component status during the incident';

COMMENT ON COLUMN public.status_incident_components.id IS 'Primary key identifier for the incident-component mapping record';
COMMENT ON COLUMN public.status_incident_components.incident_id IS 'Reference to the incident affecting the component';
COMMENT ON COLUMN public.status_incident_components.component_id IS 'Reference to the component affected by the incident';
COMMENT ON COLUMN public.status_incident_components.component_status IS 'Status of the component for this incident (e.g., DEGRADED, PARTIAL_OUTAGE, MAJOR_OUTAGE)';

-------------------------------------------------------------------------------
-- TABLE: public.status_maintenance
-------------------------------------------------------------------------------

CREATE TABLE public.status_maintenance
(
    id                           UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    app_id                       UUID NOT NULL
        REFERENCES public.status_apps (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,
    title                        VARCHAR(255) NOT NULL,
    description                  TEXT,
    status                       VARCHAR(50) NOT NULL,
    starts_at                    TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at                      TIMESTAMP WITH TIME ZONE NOT NULL,
    is_public                    BOOLEAN DEFAULT true NOT NULL,
    created_by                   VARCHAR(255) NOT NULL,
    created_date                 TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by             VARCHAR(255) NOT NULL,
    last_modified_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical       BIGINT NOT NULL,
    last_modified_date_technical BIGINT NOT NULL
);

CREATE INDEX idx_status_maintenance_app ON public.status_maintenance (app_id);
CREATE INDEX idx_status_maintenance_status ON public.status_maintenance (status);
CREATE INDEX idx_status_maintenance_starts ON public.status_maintenance (starts_at);

COMMENT ON TABLE public.status_maintenance IS 'Scheduled maintenance windows for an application';

COMMENT ON COLUMN public.status_maintenance.id IS 'Primary key identifier for the scheduled maintenance record';
COMMENT ON COLUMN public.status_maintenance.app_id IS 'Reference to the status application for which maintenance is scheduled';
COMMENT ON COLUMN public.status_maintenance.title IS 'Short, human-readable title describing the maintenance activity';
COMMENT ON COLUMN public.status_maintenance.description IS 'Detailed description of the maintenance and its expected impact';
COMMENT ON COLUMN public.status_maintenance.status IS 'Current status of the maintenance (e.g., SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED)';
COMMENT ON COLUMN public.status_maintenance.starts_at IS 'Planned start time of the maintenance window';
COMMENT ON COLUMN public.status_maintenance.ends_at IS 'Planned end time of the maintenance window';
COMMENT ON COLUMN public.status_maintenance.is_public IS 'Flag indicating whether the maintenance is visible on the public status page';
COMMENT ON COLUMN public.status_maintenance.created_by IS 'User or system that created this maintenance record';
COMMENT ON COLUMN public.status_maintenance.created_date IS 'Timestamp when this maintenance record was created';
COMMENT ON COLUMN public.status_maintenance.last_modified_by IS 'User or system that last modified this maintenance record';
COMMENT ON COLUMN public.status_maintenance.last_modified_date IS 'Timestamp when this maintenance record was last modified';
COMMENT ON COLUMN public.status_maintenance.created_date_technical IS 'Technical timestamp in milliseconds when this maintenance record was created';
COMMENT ON COLUMN public.status_maintenance.last_modified_date_technical IS 'Technical timestamp in milliseconds when this maintenance record was last modified';

-------------------------------------------------------------------------------
-- TABLE: public.status_maintenance_components
-------------------------------------------------------------------------------

CREATE TABLE public.status_maintenance_components
(
    id              UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    maintenance_id  UUID NOT NULL
        REFERENCES public.status_maintenance (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,
    component_id    UUID NOT NULL
        REFERENCES public.status_components (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_status_maintenance_component ON public.status_maintenance_components (maintenance_id, component_id);
CREATE INDEX idx_status_maintenance_components_maint ON public.status_maintenance_components (maintenance_id);
CREATE INDEX idx_status_maintenance_components_comp ON public.status_maintenance_components (component_id);

COMMENT ON TABLE public.status_maintenance_components IS 'Mapping between scheduled maintenance entries and the components they affect';

COMMENT ON COLUMN public.status_maintenance_components.id IS 'Primary key identifier for the maintenance-component mapping record';
COMMENT ON COLUMN public.status_maintenance_components.maintenance_id IS 'Reference to the scheduled maintenance affecting the component';
COMMENT ON COLUMN public.status_maintenance_components.component_id IS 'Reference to the component affected by the scheduled maintenance';

-------------------------------------------------------------------------------
-- TABLE: public.status_uptime_history
-- Stores daily uptime statistics for apps, components, and platforms
-------------------------------------------------------------------------------

CREATE TABLE public.status_uptime_history
(
    id                           UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    app_id                       UUID NOT NULL
        REFERENCES public.status_apps (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,
    component_id                 UUID
        REFERENCES public.status_components (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,
    platform_id                  UUID
        REFERENCES public.status_platforms (id)
            ON DELETE CASCADE,
    record_date                  DATE NOT NULL,
    status                       VARCHAR(50) NOT NULL DEFAULT 'OPERATIONAL',
    uptime_percentage            DECIMAL(6, 3) NOT NULL DEFAULT 100.000,
    total_minutes                INT NOT NULL DEFAULT 1440,
    operational_minutes          INT NOT NULL DEFAULT 1440,
    degraded_minutes             INT NOT NULL DEFAULT 0,
    outage_minutes               INT NOT NULL DEFAULT 0,
    maintenance_minutes          INT NOT NULL DEFAULT 0,
    incident_count               INT NOT NULL DEFAULT 0,
    maintenance_count            INT NOT NULL DEFAULT 0,
    created_date                 TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    last_modified_date           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Unique constraint: one record per day per app (when component_id is null)
CREATE UNIQUE INDEX uk_uptime_history_app_date
    ON public.status_uptime_history (app_id, record_date)
    WHERE component_id IS NULL;

-- Unique constraint: one record per day per component
CREATE UNIQUE INDEX uk_uptime_history_component_date
    ON public.status_uptime_history (component_id, record_date)
    WHERE component_id IS NOT NULL;

-- Indexes for efficient date range queries
CREATE INDEX idx_uptime_history_app_date ON public.status_uptime_history (app_id, record_date);
CREATE INDEX idx_uptime_history_component_date ON public.status_uptime_history (component_id, record_date);
CREATE INDEX idx_uptime_history_date ON public.status_uptime_history (record_date);

COMMENT ON TABLE public.status_uptime_history IS 'Daily uptime history for apps, components, and platforms, used to display the 90-day uptime chart';

COMMENT ON COLUMN public.status_uptime_history.id IS 'Primary key identifier for the uptime history record';
COMMENT ON COLUMN public.status_uptime_history.app_id IS 'Reference to the status application';
COMMENT ON COLUMN public.status_uptime_history.component_id IS 'Reference to the component (null for app-level records)';
COMMENT ON COLUMN public.status_uptime_history.platform_id IS 'Reference to the platform (for platform-level records)';
COMMENT ON COLUMN public.status_uptime_history.record_date IS 'Date of this uptime record';
COMMENT ON COLUMN public.status_uptime_history.status IS 'Overall status for the day (OPERATIONAL, DEGRADED, OUTAGE, MAINTENANCE)';
COMMENT ON COLUMN public.status_uptime_history.uptime_percentage IS 'Calculated uptime percentage for the day';
COMMENT ON COLUMN public.status_uptime_history.total_minutes IS 'Total minutes in the day (typically 1440)';
COMMENT ON COLUMN public.status_uptime_history.operational_minutes IS 'Minutes the service was fully operational';
COMMENT ON COLUMN public.status_uptime_history.degraded_minutes IS 'Minutes the service had degraded performance';
COMMENT ON COLUMN public.status_uptime_history.outage_minutes IS 'Minutes the service was in outage';
COMMENT ON COLUMN public.status_uptime_history.maintenance_minutes IS 'Minutes the service was under maintenance';
COMMENT ON COLUMN public.status_uptime_history.incident_count IS 'Number of incidents on this day';
COMMENT ON COLUMN public.status_uptime_history.maintenance_count IS 'Number of maintenance windows on this day';
COMMENT ON COLUMN public.status_uptime_history.created_date IS 'Timestamp when this record was created';
COMMENT ON COLUMN public.status_uptime_history.last_modified_date IS 'Timestamp when this record was last modified';

-------------------------------------------------------------------------------
-- TABLE: public.notification_subscribers
-- Stores email addresses of users who want to be notified about incidents
-------------------------------------------------------------------------------

CREATE TABLE public.notification_subscribers (
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_id                          UUID NOT NULL REFERENCES status_apps(id) ON DELETE CASCADE,
    email                           VARCHAR(255) NOT NULL,
    name                            VARCHAR(255),
    is_active                       BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified                     BOOLEAN NOT NULL DEFAULT FALSE,
    verification_token              VARCHAR(255),
    verification_token_expires_at   TIMESTAMP WITH TIME ZONE,
    created_by                      VARCHAR(255) NOT NULL,
    created_date                    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by                VARCHAR(255) NOT NULL,
    last_modified_date              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_date_technical          BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000)::BIGINT,
    last_modified_date_technical    BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000)::BIGINT,
    UNIQUE(app_id, email)
);

CREATE INDEX idx_notification_subscribers_app_id ON notification_subscribers(app_id);
CREATE INDEX idx_notification_subscribers_email ON notification_subscribers(email);
CREATE INDEX idx_notification_subscribers_verification_token ON notification_subscribers(verification_token);
CREATE INDEX idx_notification_subscribers_active_verified ON notification_subscribers(app_id, is_active, is_verified);

COMMENT ON TABLE public.notification_subscribers IS 'Stores notification subscribers for status applications';

COMMENT ON COLUMN public.notification_subscribers.app_id IS 'Reference to the status application this subscriber is monitoring';
COMMENT ON COLUMN public.notification_subscribers.email IS 'Email address to send notifications to';
COMMENT ON COLUMN public.notification_subscribers.name IS 'Optional display name of the subscriber';
COMMENT ON COLUMN public.notification_subscribers.is_active IS 'Whether the subscriber should receive notifications';
COMMENT ON COLUMN public.notification_subscribers.is_verified IS 'Whether the email address has been verified';
COMMENT ON COLUMN public.notification_subscribers.verification_token IS 'Token for email verification';
COMMENT ON COLUMN public.notification_subscribers.verification_token_expires_at IS 'Expiration time for the verification token';

-------------------------------------------------------------------------------
-- TABLE: public.platform_events
-- Platform Events table for logging events from platforms and components
-------------------------------------------------------------------------------

CREATE TABLE public.platform_events (
    id                           UUID PRIMARY KEY,
    app_id                       UUID NOT NULL REFERENCES status_apps(id) ON DELETE CASCADE,
    component_id                 UUID REFERENCES status_components(id) ON DELETE CASCADE,
    severity                     VARCHAR(20) NOT NULL,
    source                       VARCHAR(255),
    message                      TEXT NOT NULL,
    details                      TEXT,
    event_time                   TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date                 TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical       BIGINT NOT NULL
);

CREATE INDEX idx_platform_events_app_id ON platform_events(app_id);
CREATE INDEX idx_platform_events_component_id ON platform_events(component_id);
CREATE INDEX idx_platform_events_severity ON platform_events(severity);
CREATE INDEX idx_platform_events_event_time ON platform_events(event_time);
CREATE INDEX idx_platform_events_created_date_technical ON platform_events(created_date_technical);

-- Full-text search index on message and details
CREATE INDEX idx_platform_events_message_search ON platform_events USING gin(to_tsvector('english', message));

COMMENT ON TABLE public.platform_events IS 'Events logged from platforms and components for monitoring and debugging';

COMMENT ON COLUMN public.platform_events.id IS 'Primary key identifier for the event';
COMMENT ON COLUMN public.platform_events.app_id IS 'Reference to the status application that generated this event';
COMMENT ON COLUMN public.platform_events.component_id IS 'Reference to the component that generated this event (optional)';
COMMENT ON COLUMN public.platform_events.severity IS 'Severity level of the event (INFO, WARNING, ERROR, CRITICAL)';
COMMENT ON COLUMN public.platform_events.source IS 'Source or origin of the event';
COMMENT ON COLUMN public.platform_events.message IS 'Event message text';
COMMENT ON COLUMN public.platform_events.details IS 'Additional event details (can contain JSON or structured data)';
COMMENT ON COLUMN public.platform_events.event_time IS 'Timestamp when the event occurred';
COMMENT ON COLUMN public.platform_events.created_date IS 'Timestamp when this event record was created';
COMMENT ON COLUMN public.platform_events.created_date_technical IS 'Technical timestamp in milliseconds when this event record was created';

-------------------------------------------------------------------------------
-- TABLE: public.health_check_settings
-- Global configuration for health check system
-------------------------------------------------------------------------------

CREATE TABLE public.health_check_settings (
    id                           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key                  VARCHAR(100) UNIQUE NOT NULL,
    setting_value                VARCHAR(500) NOT NULL,
    description                  VARCHAR(500),
    created_date                 TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_modified_date           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE public.health_check_settings IS 'Global configuration settings for the health check system';

COMMENT ON COLUMN public.health_check_settings.id IS 'Primary key identifier for the setting';
COMMENT ON COLUMN public.health_check_settings.setting_key IS 'Unique key for the setting';
COMMENT ON COLUMN public.health_check_settings.setting_value IS 'Value of the setting';
COMMENT ON COLUMN public.health_check_settings.description IS 'Human-readable description of the setting';
COMMENT ON COLUMN public.health_check_settings.created_date IS 'Timestamp when this setting was created';
COMMENT ON COLUMN public.health_check_settings.last_modified_date IS 'Timestamp when this setting was last modified';

-- Insert default values matching application.properties defaults
INSERT INTO public.health_check_settings (setting_key, setting_value, description) VALUES
('enabled', 'true', 'Enable or disable all automated health checks'),
('scheduler_interval_ms', '10000', 'Scheduler polling interval in milliseconds'),
('thread_pool_size', '10', 'Number of threads for concurrent health checks'),
('default_interval_seconds', '60', 'Default check interval for new entities in seconds'),
('default_timeout_seconds', '10', 'Default timeout for health checks in seconds');
