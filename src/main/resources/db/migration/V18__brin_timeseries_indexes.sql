-- V18: Replace B-tree indexes on sequential timeseries columns with BRIN.
--
-- BRIN (Block Range INdex) summarises min/max values per page range instead of
-- indexing individual rows. Ideal when physical insertion order correlates with
-- column values (append-only timeseries). Benefits over B-tree here:
--   * ~100-1000x smaller index footprint
--   * Near-zero INSERT overhead (one summary entry per page range, not per row)
--   * Efficient range scans for time-window and retention queries
--
-- pages_per_range tuning:
--   Lower value = more precise (fewer false-positive pages) but larger index.
--   Higher volume tables use 32; lower-volume use 128 (PostgreSQL default).
--
-- B-tree indexes on UUID FKs, equality filters, and composite indexes with UUID
-- prefix are left unchanged — BRIN only helps when values are physically ordered.

-- ─── logs ─────────────────────────────────────────────────────────────────────
-- log_timestamp always advances as logs are ingested (append-only)
DROP INDEX IF EXISTS public.idx_logs_timestamp;
CREATE INDEX idx_logs_timestamp
    ON public.logs USING BRIN (log_timestamp) WITH (pages_per_range = 32);

-- ─── log_metrics ──────────────────────────────────────────────────────────────
-- bucket is a time-aligned aggregation window, always moves forward
DROP INDEX IF EXISTS public.idx_log_metrics_bucket;
CREATE INDEX idx_log_metrics_bucket
    ON public.log_metrics USING BRIN (bucket) WITH (pages_per_range = 64);

-- ─── platform_events ──────────────────────────────────────────────────────────
-- event_time: events recorded at occurrence time, sequential with ingestion
DROP INDEX IF EXISTS public.idx_platform_events_event_time;
CREATE INDEX idx_platform_events_event_time
    ON public.platform_events USING BRIN (event_time) WITH (pages_per_range = 32);

-- created_date_technical: epoch-millis insertion timestamp, strictly monotonic
DROP INDEX IF EXISTS public.idx_platform_events_created_date_technical;
CREATE INDEX idx_platform_events_created_date_technical
    ON public.platform_events USING BRIN (created_date_technical) WITH (pages_per_range = 32);

-- ─── status_uptime_history ────────────────────────────────────────────────────
-- record_date: one row per app/component per calendar day, always appended forward
DROP INDEX IF EXISTS public.idx_uptime_history_date;
CREATE INDEX idx_uptime_history_date
    ON public.status_uptime_history USING BRIN (record_date) WITH (pages_per_range = 128);

-- ─── status_incidents ─────────────────────────────────────────────────────────
-- started_at: incidents created when they occur, insertion order matches start time
DROP INDEX IF EXISTS public.idx_status_incidents_started;
CREATE INDEX idx_status_incidents_started
    ON public.status_incidents USING BRIN (started_at) WITH (pages_per_range = 128);

-- ─── status_maintenance ───────────────────────────────────────────────────────
-- starts_at: maintenance records created near the time they are scheduled;
-- insertion order tracks roughly with start time
DROP INDEX IF EXISTS public.idx_status_maintenance_starts;
CREATE INDEX idx_status_maintenance_starts
    ON public.status_maintenance USING BRIN (starts_at) WITH (pages_per_range = 128);

-- ─── status_incident_updates ──────────────────────────────────────────────────
-- update_time: updates written in chronological order as incidents progress
DROP INDEX IF EXISTS public.idx_status_incident_updates_time;
CREATE INDEX idx_status_incident_updates_time
    ON public.status_incident_updates USING BRIN (update_time) WITH (pages_per_range = 128);
