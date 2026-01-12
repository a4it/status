-- Ensure gen_random_uuid() is available for UUID defaults
create extension if not exists pgcrypto;

-------------------------------------------------------------------------------
-- TABLE: public.tenants
-------------------------------------------------------------------------------

create table public.tenants (
                                id                           uuid primary key not null default gen_random_uuid(),
                                name                         varchar(255) not null,
                                domain                       varchar(255) not null,
                                settings                     jsonb default '{}'::jsonb,
                                is_active                    boolean default true,
                                created_by                   varchar(255) not null,
                                created_date                 timestamp with time zone not null,
                                last_modified_by             varchar(255) not null,
                                last_modified_date           timestamp with time zone not null,
                                created_date_technical       bigint not null,
                                last_modified_date_technical bigint not null
);

create unique index tenants_domain_key
    on public.tenants using btree (domain);

comment on table public.tenants is 'Top-level tenants that own organizations, users, and status apps';

comment on column public.tenants.id is 'Primary key identifier for the tenant';
comment on column public.tenants.name is 'Human-readable name of the tenant';
comment on column public.tenants.domain is 'Domain associated with the tenant (e.g., company.com)';
comment on column public.tenants.settings is 'JSONB object storing tenant-specific configuration settings';
comment on column public.tenants.is_active is 'Flag indicating whether the tenant is active';
comment on column public.tenants.created_by is 'Identifier of the user or system that created this tenant record';
comment on column public.tenants.created_date is 'Timestamp when this tenant record was created';
comment on column public.tenants.last_modified_by is 'Identifier of the user or system that last modified this tenant record';
comment on column public.tenants.last_modified_date is 'Timestamp when this tenant record was last modified';
comment on column public.tenants.created_date_technical is 'Technical timestamp in milliseconds when this tenant record was created';
comment on column public.tenants.last_modified_date_technical is 'Technical timestamp in milliseconds when this tenant record was last modified';

-------------------------------------------------------------------------------
-- TABLE: public.organizations
-------------------------------------------------------------------------------

create table public.organizations
(
    id                           uuid                                            not null
        primary key,
    name                         varchar(255)                                    not null
        unique,
    description                  text,
    email                        varchar(255),
    phone                        varchar(50),
    website                      varchar(255),
    address                      text,
    logo_url                     varchar(255),
    status                       varchar(50) default 'ACTIVE'::character varying not null,
    created_by                   varchar(255)                                    not null,
    created_date                 timestamp with time zone                        not null,
    last_modified_by             varchar(255)                                    not null,
    last_modified_date           timestamp with time zone                        not null,
    created_date_technical       bigint                                          not null,
    last_modified_date_technical bigint                                          not null,
    version                      bigint,
    subscription_exempt          boolean                                         not null,
    throttling_enabled           boolean     default true                        not null,
    organization_type            varchar(20)                                     not null,
    vat_number                   varchar(50),
    country                      varchar(100),
    postalcode                   text,
    community                    text,
    type                         varchar(255),
    tenant_id                    uuid
        constraint fk_tenants_organization
            references public.tenants (id)
            on update cascade
            on delete set null
);

comment on table public.organizations is 'Organizations registered in the system, belonging to a tenant';

alter table public.organizations
    owner to postgres;

create index idx_organization_name
    on public.organizations (name);

create index idx_organization_status
    on public.organizations (status);

create index idx_organization_tenant
    on public.organizations (tenant_id);

comment on column public.organizations.id is 'Primary key identifier for the organization';
comment on column public.organizations.name is 'Unique name of the organization';
comment on column public.organizations.description is 'Free-text description of the organization';
comment on column public.organizations.email is 'Primary contact email address for the organization';
comment on column public.organizations.phone is 'Primary contact phone number for the organization';
comment on column public.organizations.website is 'Website URL of the organization';
comment on column public.organizations.address is 'Postal or physical address of the organization';
comment on column public.organizations.logo_url is 'URL of the organization''s logo';
comment on column public.organizations.status is 'Lifecycle status of the organization (e.g., ACTIVE, INACTIVE)';
comment on column public.organizations.created_by is 'Identifier of the user or system that created this organization record';
comment on column public.organizations.created_date is 'Timestamp when this organization record was created';
comment on column public.organizations.last_modified_by is 'Identifier of the user or system that last modified this organization record';
comment on column public.organizations.last_modified_date is 'Timestamp when this organization record was last modified';
comment on column public.organizations.created_date_technical is 'Technical timestamp in milliseconds when this organization record was created';
comment on column public.organizations.last_modified_date_technical is 'Technical timestamp in milliseconds when this organization record was last modified';
comment on column public.organizations.version is 'Optional version field for optimistic locking or auditing';
comment on column public.organizations.subscription_exempt is 'Flag indicating whether this organization is exempt from subscription billing';
comment on column public.organizations.throttling_enabled is 'Flag indicating whether throttling or rate limiting is enabled for this organization';
comment on column public.organizations.organization_type is 'Type of organization (e.g., CUSTOMER, INTERNAL)';
comment on column public.organizations.vat_number is 'VAT (Value Added Tax) identification number for the organization';
comment on column public.organizations.country is 'Country where the organization is based';
comment on column public.organizations.postalcode is 'Postal code for the organization''s address';
comment on column public.organizations.community is 'Community or region associated with the organization';
comment on column public.organizations.type is 'Additional classification or subtype of the organization';
comment on column public.organizations.tenant_id is 'Reference to the tenant that owns this organization';

-------------------------------------------------------------------------------
-- TABLE: public.users
-------------------------------------------------------------------------------

create table public.users
(
    id                           uuid         not null
        primary key,
    created_by                   varchar(255),
    created_date                 timestamp(6) with time zone,
    created_date_technical       bigint,
    last_modified_by             varchar(255),
    last_modified_date           timestamp(6) with time zone,
    last_modified_date_technical bigint,
    version                      bigint,
    enabled                      boolean      not null,
    full_name                    varchar(255),
    password                     varchar(255) not null,
    refresh_token                varchar(255),
    username                     varchar(255) not null
        constraint uk_r43af9ap4edm43mdmtq01oddj6
            unique,
    email                        varchar(255)
        unique,
    role                         varchar(20),
    status                       varchar(30),
    organization_id              uuid
        constraint fk_users_organization
            references public.organizations (id)
            on update cascade
            on delete set null,
    type                         varchar(255)
);

comment on table public.users is 'Stores admin users who can access the web administration interface';

comment on column public.users.id is 'Primary key identifier for the user';
comment on column public.users.created_by is 'User or system that created this user record';
comment on column public.users.created_date is 'Timestamp when this user record was first created';
comment on column public.users.created_date_technical is 'Technical timestamp in milliseconds when this user record was created';
comment on column public.users.last_modified_by is 'User or system that last modified this user record';
comment on column public.users.last_modified_date is 'Timestamp when this user record was last updated';
comment on column public.users.last_modified_date_technical is 'Technical timestamp in milliseconds when this user record was last updated';
comment on column public.users.version is 'Optional version field for optimistic locking or auditing';
comment on column public.users.enabled is 'Flag indicating whether the user account is enabled';
comment on column public.users.full_name is 'Full name of the user';
comment on column public.users.password is 'Securely hashed password for the user account';
comment on column public.users.refresh_token is 'JWT refresh token for the user''s session';
comment on column public.users.username is 'Unique username used for login to the admin interface';
comment on column public.users.email is 'Unique email address of the administrator user';
comment on column public.users.role is 'Role of the user determining their permissions (e.g., ADMIN, USER)';
comment on column public.users.status is 'Current status of the user account (e.g., ACTIVE, INACTIVE, DELETED)';
comment on column public.users.organization_id is 'Reference to the organization that this user belongs to';
comment on column public.users.type is 'Additional classification or type of user';

alter table public.users
    owner to postgres;

create index idx_users_organization
    on public.users (organization_id);

-------------------------------------------------------------------------------
-- TABLE: public.status_apps
-------------------------------------------------------------------------------

create table public.status_apps
(
    id                           uuid primary key not null default gen_random_uuid(),
    name                         varchar(255) not null,
    description                  text,
    slug                         varchar(255) not null,
    is_public                    boolean default true not null,
    status                       varchar(50) default 'OPERATIONAL' not null,
    tenant_id                    uuid references public.tenants (id),
    organization_id              uuid references public.organizations (id),
    created_by                   varchar(255) not null,
    created_date                 timestamp with time zone not null,
    last_modified_by             varchar(255) not null,
    last_modified_date           timestamp with time zone not null,
    created_date_technical       bigint not null,
    last_modified_date_technical bigint not null
);

comment on table public.status_apps is 'Applications that expose their own status pages (e.g., Jira Software, Jira Service Management)';

create unique index uk_status_apps_tenant_slug
    on public.status_apps (tenant_id, slug);

create index idx_status_apps_name
    on public.status_apps (name);

create index idx_status_apps_tenant
    on public.status_apps (tenant_id);

create index idx_status_apps_org
    on public.status_apps (organization_id);

comment on column public.status_apps.id is 'Primary key identifier for the status application';
comment on column public.status_apps.name is 'Display name of the status application (e.g., Jira Software)';
comment on column public.status_apps.description is 'Detailed description of the status application';
comment on column public.status_apps.slug is 'URL-friendly slug for the status application (e.g., jira-software)';
comment on column public.status_apps.is_public is 'Flag indicating whether the status page for this application is publicly visible';
comment on column public.status_apps.status is 'Aggregated current status of the application (e.g., OPERATIONAL, DEGRADED, MAJOR_OUTAGE)';
comment on column public.status_apps.tenant_id is 'Reference to the tenant that owns this status application';
comment on column public.status_apps.organization_id is 'Reference to the organization associated with this status application';
comment on column public.status_apps.created_by is 'User or system that created this status application record';
comment on column public.status_apps.created_date is 'Timestamp when this status application record was created';
comment on column public.status_apps.last_modified_by is 'User or system that last modified this status application record';
comment on column public.status_apps.last_modified_date is 'Timestamp when this status application record was last modified';
comment on column public.status_apps.created_date_technical is 'Technical timestamp in milliseconds when this status application record was created';
comment on column public.status_apps.last_modified_date_technical is 'Technical timestamp in milliseconds when this status application record was last modified';

-------------------------------------------------------------------------------
-- TABLE: public.status_components
-------------------------------------------------------------------------------

create table public.status_components
(
    id                           uuid primary key not null default gen_random_uuid(),
    app_id                       uuid not null
        references public.status_apps (id)
            on update cascade
            on delete cascade,
    name                         varchar(255) not null,
    description                  text,
    status                       varchar(50) default 'OPERATIONAL' not null,
    position                     int default 0,
    group_name                   varchar(255),
    created_by                   varchar(255) not null,
    created_date                 timestamp with time zone not null,
    last_modified_by             varchar(255) not null,
    last_modified_date           timestamp with time zone not null,
    created_date_technical       bigint not null,
    last_modified_date_technical bigint not null
);

create index idx_status_components_app
    on public.status_components (app_id);

create index idx_status_components_status
    on public.status_components (status);

create unique index uk_status_components_app_name
    on public.status_components (app_id, name);

comment on table public.status_components is 'Logical components or subsystems of a status application (e.g., API, Web UI, Database)';

comment on column public.status_components.id is 'Primary key identifier for the status component';
comment on column public.status_components.app_id is 'Reference to the status application to which this component belongs';
comment on column public.status_components.name is 'Display name of the component';
comment on column public.status_components.description is 'Detailed description of the component and its responsibilities';
comment on column public.status_components.status is 'Current status of the component (e.g., OPERATIONAL, DEGRADED, PARTIAL_OUTAGE, MAJOR_OUTAGE)';
comment on column public.status_components.position is 'Ordering index used to sort components on the status page';
comment on column public.status_components.group_name is 'Optional grouping label used to group related components on the status page';
comment on column public.status_components.created_by is 'User or system that created this component record';
comment on column public.status_components.created_date is 'Timestamp when this component record was created';
comment on column public.status_components.last_modified_by is 'User or system that last modified this component record';
comment on column public.status_components.last_modified_date is 'Timestamp when this component record was last modified';
comment on column public.status_components.created_date_technical is 'Technical timestamp in milliseconds when this component record was created';
comment on column public.status_components.last_modified_date_technical is 'Technical timestamp in milliseconds when this component record was last modified';

-------------------------------------------------------------------------------
-- TABLE: public.status_incidents
-------------------------------------------------------------------------------

create table public.status_incidents
(
    id                           uuid primary key not null default gen_random_uuid(),
    app_id                       uuid not null
        references public.status_apps (id)
            on update cascade
            on delete cascade,
    title                        varchar(255) not null,
    description                  text,
    status                       varchar(50) not null,
    severity                     varchar(50) not null,
    impact                       varchar(50),
    started_at                   timestamp with time zone not null,
    resolved_at                  timestamp with time zone,
    is_public                    boolean default true not null,
    created_by                   varchar(255) not null,
    created_date                 timestamp with time zone not null,
    last_modified_by             varchar(255) not null,
    last_modified_date           timestamp with time zone not null,
    created_date_technical       bigint not null,
    last_modified_date_technical bigint not null
);

create index idx_status_incidents_app
    on public.status_incidents (app_id);

create index idx_status_incidents_status
    on public.status_incidents (status);

create index idx_status_incidents_started
    on public.status_incidents (started_at);

comment on table public.status_incidents is 'Incidents representing service disruptions or outages for an application';

comment on column public.status_incidents.id is 'Primary key identifier for the incident';
comment on column public.status_incidents.app_id is 'Reference to the status application affected by this incident';
comment on column public.status_incidents.title is 'Short, human-readable title describing the incident';
comment on column public.status_incidents.description is 'Detailed description of the incident and its context';
comment on column public.status_incidents.status is 'Current status of the incident (e.g., INVESTIGATING, IDENTIFIED, MONITORING, RESOLVED)';
comment on column public.status_incidents.severity is 'Severity level of the incident (e.g., MINOR, MAJOR, CRITICAL)';
comment on column public.status_incidents.impact is 'High-level description of the incident impact (e.g., PARTIAL_OUTAGE, MAJOR_OUTAGE)';
comment on column public.status_incidents.started_at is 'Timestamp when the incident started or was first detected';
comment on column public.status_incidents.resolved_at is 'Timestamp when the incident was fully resolved, if applicable';
comment on column public.status_incidents.is_public is 'Flag indicating whether the incident is visible on the public status page';
comment on column public.status_incidents.created_by is 'User or system that created this incident record';
comment on column public.status_incidents.created_date is 'Timestamp when this incident record was created';
comment on column public.status_incidents.last_modified_by is 'User or system that last modified this incident record';
comment on column public.status_incidents.last_modified_date is 'Timestamp when this incident record was last modified';
comment on column public.status_incidents.created_date_technical is 'Technical timestamp in milliseconds when this incident record was created';
comment on column public.status_incidents.last_modified_date_technical is 'Technical timestamp in milliseconds when this incident record was last modified';

-------------------------------------------------------------------------------
-- TABLE: public.status_incident_updates
-------------------------------------------------------------------------------

create table public.status_incident_updates
(
    id                           uuid primary key not null default gen_random_uuid(),
    incident_id                  uuid not null
        references public.status_incidents (id)
            on update cascade
            on delete cascade,
    status                       varchar(50) not null,
    message                      text not null,
    update_time                  timestamp with time zone not null,
    created_by                   varchar(255) not null,
    created_date                 timestamp with time zone not null,
    last_modified_by             varchar(255) not null,
    last_modified_date           timestamp with time zone not null,
    created_date_technical       bigint not null,
    last_modified_date_technical bigint not null
);

create index idx_status_incident_updates_incident
    on public.status_incident_updates (incident_id);

create index idx_status_incident_updates_time
    on public.status_incident_updates (update_time);

comment on table public.status_incident_updates is 'Timeline of status updates and messages associated with an incident';

comment on column public.status_incident_updates.id is 'Primary key identifier for the incident update';
comment on column public.status_incident_updates.incident_id is 'Reference to the incident this update belongs to';
comment on column public.status_incident_updates.status is 'Incident status at the time of this update (e.g., INVESTIGATING, IDENTIFIED, MONITORING, RESOLVED)';
comment on column public.status_incident_updates.message is 'Human-readable message describing the update or progress on the incident';
comment on column public.status_incident_updates.update_time is 'Timestamp when this update was recorded or made public';
comment on column public.status_incident_updates.created_by is 'User or system that created this incident update record';
comment on column public.status_incident_updates.created_date is 'Timestamp when this incident update record was created';
comment on column public.status_incident_updates.last_modified_by is 'User or system that last modified this incident update record';
comment on column public.status_incident_updates.last_modified_date is 'Timestamp when this incident update record was last modified';
comment on column public.status_incident_updates.created_date_technical is 'Technical timestamp in milliseconds when this incident update record was created';
comment on column public.status_incident_updates.last_modified_date_technical is 'Technical timestamp in milliseconds when this incident update record was last modified';

-------------------------------------------------------------------------------
-- TABLE: public.status_incident_components
-------------------------------------------------------------------------------

create table public.status_incident_components
(
    id                uuid primary key not null default gen_random_uuid(),
    incident_id       uuid not null
        references public.status_incidents (id)
            on update cascade
            on delete cascade,
    component_id      uuid not null
        references public.status_components (id)
            on update cascade
            on delete cascade,
    component_status  varchar(50) not null
);

create unique index uk_status_incident_component
    on public.status_incident_components (incident_id, component_id);

create index idx_status_incident_components_incident
    on public.status_incident_components (incident_id);

create index idx_status_incident_components_component
    on public.status_incident_components (component_id);

comment on table public.status_incident_components is 'Mapping between incidents and the components they affect, including per-component status during the incident';

comment on column public.status_incident_components.id is 'Primary key identifier for the incident-component mapping record';
comment on column public.status_incident_components.incident_id is 'Reference to the incident affecting the component';
comment on column public.status_incident_components.component_id is 'Reference to the component affected by the incident';
comment on column public.status_incident_components.component_status is 'Status of the component for this incident (e.g., DEGRADED, PARTIAL_OUTAGE, MAJOR_OUTAGE)';

-------------------------------------------------------------------------------
-- TABLE: public.status_maintenance
-------------------------------------------------------------------------------

create table public.status_maintenance
(
    id                           uuid primary key not null default gen_random_uuid(),
    app_id                       uuid not null
        references public.status_apps (id)
            on update cascade
            on delete cascade,
    title                        varchar(255) not null,
    description                  text,
    status                       varchar(50) not null,
    starts_at                    timestamp with time zone not null,
    ends_at                      timestamp with time zone not null,
    is_public                    boolean default true not null,
    created_by                   varchar(255) not null,
    created_date                 timestamp with time zone not null,
    last_modified_by             varchar(255) not null,
    last_modified_date           timestamp with time zone not null,
    created_date_technical       bigint not null,
    last_modified_date_technical bigint not null
);

create index idx_status_maintenance_app
    on public.status_maintenance (app_id);

create index idx_status_maintenance_status
    on public.status_maintenance (status);

create index idx_status_maintenance_starts
    on public.status_maintenance (starts_at);

comment on table public.status_maintenance is 'Scheduled maintenance windows for an application';

comment on column public.status_maintenance.id is 'Primary key identifier for the scheduled maintenance record';
comment on column public.status_maintenance.app_id is 'Reference to the status application for which maintenance is scheduled';
comment on column public.status_maintenance.title is 'Short, human-readable title describing the maintenance activity';
comment on column public.status_maintenance.description is 'Detailed description of the maintenance and its expected impact';
comment on column public.status_maintenance.status is 'Current status of the maintenance (e.g., SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED)';
comment on column public.status_maintenance.starts_at is 'Planned start time of the maintenance window';
comment on column public.status_maintenance.ends_at is 'Planned end time of the maintenance window';
comment on column public.status_maintenance.is_public is 'Flag indicating whether the maintenance is visible on the public status page';
comment on column public.status_maintenance.created_by is 'User or system that created this maintenance record';
comment on column public.status_maintenance.created_date is 'Timestamp when this maintenance record was created';
comment on column public.status_maintenance.last_modified_by is 'User or system that last modified this maintenance record';
comment on column public.status_maintenance.last_modified_date is 'Timestamp when this maintenance record was last modified';
comment on column public.status_maintenance.created_date_technical is 'Technical timestamp in milliseconds when this maintenance record was created';
comment on column public.status_maintenance.last_modified_date_technical is 'Technical timestamp in milliseconds when this maintenance record was last modified';

-------------------------------------------------------------------------------
-- TABLE: public.status_maintenance_components
-------------------------------------------------------------------------------

create table public.status_maintenance_components
(
    id              uuid primary key not null default gen_random_uuid(),
    maintenance_id  uuid not null
        references public.status_maintenance (id)
            on update cascade
            on delete cascade,
    component_id    uuid not null
        references public.status_components (id)
            on update cascade
            on delete cascade
);

create unique index uk_status_maintenance_component
    on public.status_maintenance_components (maintenance_id, component_id);

create index idx_status_maintenance_components_maint
    on public.status_maintenance_components (maintenance_id);

create index idx_status_maintenance_components_comp
    on public.status_maintenance_components (component_id);

comment on table public.status_maintenance_components is 'Mapping between scheduled maintenance entries and the components they affect';

comment on column public.status_maintenance_components.id is 'Primary key identifier for the maintenance-component mapping record';
comment on column public.status_maintenance_components.maintenance_id is 'Reference to the scheduled maintenance affecting the component';
comment on column public.status_maintenance_components.component_id is 'Reference to the component affected by the scheduled maintenance';
