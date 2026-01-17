-------------------------------------------------------------------------------
-- TABLE: public.status_platforms
-- Represents a higher-level platform that can group multiple status apps
-------------------------------------------------------------------------------

create table public.status_platforms
(
    id                           uuid primary key not null default gen_random_uuid(),
    name                         varchar(255) not null,
    description                  text,
    slug                         varchar(255) not null,
    logo_url                     varchar(500),
    website_url                  varchar(500),
    status                       varchar(50) default 'OPERATIONAL' not null,
    is_public                    boolean default true not null,
    position                     int default 0,
    tenant_id                    uuid references public.tenants (id),
    organization_id              uuid references public.organizations (id),
    created_by                   varchar(255) not null,
    created_date                 timestamp with time zone not null,
    last_modified_by             varchar(255) not null,
    last_modified_date           timestamp with time zone not null,
    created_date_technical       bigint not null,
    last_modified_date_technical bigint not null
);

create unique index uk_status_platforms_tenant_slug
    on public.status_platforms (tenant_id, slug);

create index idx_status_platforms_name
    on public.status_platforms (name);

create index idx_status_platforms_tenant
    on public.status_platforms (tenant_id);

create index idx_status_platforms_org
    on public.status_platforms (organization_id);

create index idx_status_platforms_status
    on public.status_platforms (status);

comment on table public.status_platforms is 'Higher-level platforms that can group multiple status applications together';

comment on column public.status_platforms.id is 'Primary key identifier for the platform';
comment on column public.status_platforms.name is 'Display name of the platform';
comment on column public.status_platforms.description is 'Detailed description of the platform';
comment on column public.status_platforms.slug is 'URL-friendly slug for the platform (e.g., atlassian-cloud)';
comment on column public.status_platforms.logo_url is 'URL to the platform logo image';
comment on column public.status_platforms.website_url is 'External website URL for the platform';
comment on column public.status_platforms.status is 'Aggregated current status of the platform (e.g., OPERATIONAL, DEGRADED, MAJOR_OUTAGE)';
comment on column public.status_platforms.is_public is 'Flag indicating whether the platform is publicly visible';
comment on column public.status_platforms.position is 'Display order position for sorting platforms';
comment on column public.status_platforms.tenant_id is 'Reference to the tenant that owns this platform';
comment on column public.status_platforms.organization_id is 'Reference to the organization associated with this platform';
comment on column public.status_platforms.created_by is 'User or system that created this platform record';
comment on column public.status_platforms.created_date is 'Timestamp when this platform record was created';
comment on column public.status_platforms.last_modified_by is 'User or system that last modified this platform record';
comment on column public.status_platforms.last_modified_date is 'Timestamp when this platform record was last modified';
comment on column public.status_platforms.created_date_technical is 'Technical timestamp in milliseconds when this platform record was created';
comment on column public.status_platforms.last_modified_date_technical is 'Technical timestamp in milliseconds when this platform record was last modified';

-------------------------------------------------------------------------------
-- Add platform reference to status_apps table
-------------------------------------------------------------------------------

alter table public.status_apps
    add column platform_id uuid references public.status_platforms (id)
        on update cascade
        on delete set null;

create index idx_status_apps_platform
    on public.status_apps (platform_id);

comment on column public.status_apps.platform_id is 'Reference to the parent platform this application belongs to';

-------------------------------------------------------------------------------
-- TABLE: platform_events
-- Platform Events table for logging events from platforms and components
-------------------------------------------------------------------------------

CREATE TABLE platform_events (
    id UUID PRIMARY KEY,
    app_id UUID NOT NULL REFERENCES status_apps(id) ON DELETE CASCADE,
    component_id UUID REFERENCES status_components(id) ON DELETE CASCADE,
    severity VARCHAR(20) NOT NULL,
    source VARCHAR(255),
    message TEXT NOT NULL,
    details TEXT,
    event_time TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical BIGINT NOT NULL
);

-- Indexes for efficient querying
CREATE INDEX idx_platform_events_app_id ON platform_events(app_id);
CREATE INDEX idx_platform_events_component_id ON platform_events(component_id);
CREATE INDEX idx_platform_events_severity ON platform_events(severity);
CREATE INDEX idx_platform_events_event_time ON platform_events(event_time);
CREATE INDEX idx_platform_events_created_date_technical ON platform_events(created_date_technical);

-- Full-text search index on message and details
CREATE INDEX idx_platform_events_message_search ON platform_events USING gin(to_tsvector('english', message));
