-- M-1: GIN trigram index for log message full-text search
-- Enables LIKE '%search%' queries to use an index instead of full table scans.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_logs_message_trgm
    ON public.logs USING gin (lower(message) gin_trgm_ops);

-- M-2: Composite index for log metric alert evaluation
-- Covers the (service, level, bucket) access pattern used by alert rule evaluation every minute.
CREATE INDEX IF NOT EXISTS idx_log_metrics_service_level_bucket
    ON public.log_metrics (service, level, bucket DESC);

-- L-4: Composite index for status_incidents (app_id, status) lookups
-- Covers findByAppIdAndStatus and findByAppIdAndStatusNot used by the public status page.
CREATE INDEX IF NOT EXISTS idx_status_incidents_app_status
    ON public.status_incidents (app_id, status);
