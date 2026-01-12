-------------------------------------------------------------------------------
-- TABLE: public.status_uptime_history
-- Stores daily uptime statistics for apps and components
-------------------------------------------------------------------------------

create table public.status_uptime_history
(
    id                           uuid primary key not null default gen_random_uuid(),
    app_id                       uuid not null
        references public.status_apps (id)
            on update cascade
            on delete cascade,
    component_id                 uuid
        references public.status_components (id)
            on update cascade
            on delete cascade,
    record_date                  date not null,
    status                       varchar(50) not null default 'OPERATIONAL',
    uptime_percentage            decimal(6, 3) not null default 100.000,
    total_minutes                int not null default 1440,
    operational_minutes          int not null default 1440,
    degraded_minutes             int not null default 0,
    outage_minutes               int not null default 0,
    maintenance_minutes          int not null default 0,
    incident_count               int not null default 0,
    maintenance_count            int not null default 0,
    created_date                 timestamp with time zone not null default now(),
    last_modified_date           timestamp with time zone not null default now()
);

comment on table public.status_uptime_history is 'Daily uptime history for apps and components, used to display the 90-day uptime chart';

comment on column public.status_uptime_history.id is 'Primary key identifier for the uptime history record';
comment on column public.status_uptime_history.app_id is 'Reference to the status application';
comment on column public.status_uptime_history.component_id is 'Reference to the component (null for app-level records)';
comment on column public.status_uptime_history.record_date is 'Date of this uptime record';
comment on column public.status_uptime_history.status is 'Overall status for the day (OPERATIONAL, DEGRADED, OUTAGE, MAINTENANCE)';
comment on column public.status_uptime_history.uptime_percentage is 'Calculated uptime percentage for the day';
comment on column public.status_uptime_history.total_minutes is 'Total minutes in the day (typically 1440)';
comment on column public.status_uptime_history.operational_minutes is 'Minutes the service was fully operational';
comment on column public.status_uptime_history.degraded_minutes is 'Minutes the service had degraded performance';
comment on column public.status_uptime_history.outage_minutes is 'Minutes the service was in outage';
comment on column public.status_uptime_history.maintenance_minutes is 'Minutes the service was under maintenance';
comment on column public.status_uptime_history.incident_count is 'Number of incidents on this day';
comment on column public.status_uptime_history.maintenance_count is 'Number of maintenance windows on this day';
comment on column public.status_uptime_history.created_date is 'Timestamp when this record was created';
comment on column public.status_uptime_history.last_modified_date is 'Timestamp when this record was last modified';

-- Unique constraint: one record per day per app (when component_id is null)
create unique index uk_uptime_history_app_date
    on public.status_uptime_history (app_id, record_date)
    where component_id is null;

-- Unique constraint: one record per day per component
create unique index uk_uptime_history_component_date
    on public.status_uptime_history (component_id, record_date)
    where component_id is not null;

-- Index for efficient date range queries
create index idx_uptime_history_app_date
    on public.status_uptime_history (app_id, record_date);

create index idx_uptime_history_component_date
    on public.status_uptime_history (component_id, record_date);

create index idx_uptime_history_date
    on public.status_uptime_history (record_date);
