-- ================================================================================
-- Logs Hub - Database Schema
-- ================================================================================
-- Adds tables for the Logs Hub feature: log ingestion, storage, filtering,
-- metrics aggregation, and alerting.
-- ================================================================================

-------------------------------------------------------------------------------
-- TABLE: public.log_api_keys
-- API keys used to authenticate log ingestion requests
-------------------------------------------------------------------------------

CREATE TABLE public.log_api_keys (
    id                     UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    tenant_id              UUID REFERENCES public.tenants (id) ON UPDATE CASCADE ON DELETE CASCADE,
    name                   VARCHAR(255) NOT NULL,
    api_key                VARCHAR(255) NOT NULL,
    is_active              BOOLEAN DEFAULT true,
    created_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical BIGINT NOT NULL
);

CREATE UNIQUE INDEX idx_log_api_keys_key ON public.log_api_keys (api_key);
CREATE INDEX idx_log_api_keys_tenant ON public.log_api_keys (tenant_id);

COMMENT ON TABLE public.log_api_keys IS 'API keys used to authenticate log ingestion via REST API';
COMMENT ON COLUMN public.log_api_keys.name IS 'Human-readable label for the API key';
COMMENT ON COLUMN public.log_api_keys.api_key IS 'The secret API key value sent in X-Log-Api-Key header';
COMMENT ON COLUMN public.log_api_keys.is_active IS 'Whether this key is currently valid for ingestion';

-------------------------------------------------------------------------------
-- TABLE: public.drop_rules
-- Rules evaluated before storing logs; matching logs are discarded
-------------------------------------------------------------------------------

CREATE TABLE public.drop_rules (
    id                     UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    tenant_id              UUID REFERENCES public.tenants (id) ON UPDATE CASCADE ON DELETE SET NULL,
    name                   VARCHAR(255) NOT NULL,
    level                  VARCHAR(20),
    service                VARCHAR(255),
    message_pattern        VARCHAR(500),
    is_active              BOOLEAN DEFAULT true,
    created_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date_technical BIGINT NOT NULL
);

CREATE INDEX idx_drop_rules_tenant ON public.drop_rules (tenant_id);
CREATE INDEX idx_drop_rules_active ON public.drop_rules (is_active);

COMMENT ON TABLE public.drop_rules IS 'Rules that reject logs before storage (e.g., level=INFO AND service=payments)';
COMMENT ON COLUMN public.drop_rules.level IS 'Log level to match (DEBUG, INFO, WARNING, ERROR, CRITICAL). NULL matches any.';
COMMENT ON COLUMN public.drop_rules.service IS 'Service name to match. NULL matches any service.';
COMMENT ON COLUMN public.drop_rules.message_pattern IS 'Substring or pattern to match in the log message. NULL matches any message.';

-------------------------------------------------------------------------------
-- TABLE: public.logs
-- Main log storage table
-------------------------------------------------------------------------------

CREATE TABLE public.logs (
    id                     UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    tenant_id              UUID REFERENCES public.tenants (id) ON UPDATE CASCADE ON DELETE SET NULL,
    log_timestamp          TIMESTAMP WITH TIME ZONE NOT NULL,
    level                  VARCHAR(20)  NOT NULL,
    service                VARCHAR(255) NOT NULL,
    message                TEXT         NOT NULL,
    metadata               TEXT,
    trace_id               VARCHAR(255),
    request_id             VARCHAR(255),
    created_date_technical BIGINT       NOT NULL
);

CREATE INDEX idx_logs_timestamp ON public.logs (log_timestamp DESC);
CREATE INDEX idx_logs_level     ON public.logs (level);
CREATE INDEX idx_logs_service   ON public.logs (service);
CREATE INDEX idx_logs_tenant    ON public.logs (tenant_id);
CREATE INDEX idx_logs_trace_id  ON public.logs (trace_id) WHERE trace_id IS NOT NULL;
CREATE INDEX idx_logs_request_id ON public.logs (request_id) WHERE request_id IS NOT NULL;

COMMENT ON TABLE public.logs IS 'Main log entry storage — accepts logs via REST ingestion endpoint';
COMMENT ON COLUMN public.logs.log_timestamp IS 'The timestamp of the log event (from the producing service)';
COMMENT ON COLUMN public.logs.level IS 'Severity level: DEBUG, INFO, WARNING, ERROR, CRITICAL';
COMMENT ON COLUMN public.logs.service IS 'Name of the service that produced the log';
COMMENT ON COLUMN public.logs.message IS 'Human-readable log message';
COMMENT ON COLUMN public.logs.metadata IS 'Optional JSON object with arbitrary per-service fields';
COMMENT ON COLUMN public.logs.trace_id IS 'Distributed tracing trace identifier';
COMMENT ON COLUMN public.logs.request_id IS 'Per-request identifier for log correlation';

-------------------------------------------------------------------------------
-- TABLE: public.log_metrics
-- Aggregated log counts by service + level per time bucket
-------------------------------------------------------------------------------

CREATE TABLE public.log_metrics (
    id                     UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    tenant_id              UUID REFERENCES public.tenants (id) ON UPDATE CASCADE ON DELETE SET NULL,
    service                VARCHAR(255) NOT NULL,
    level                  VARCHAR(20)  NOT NULL,
    bucket                 TIMESTAMP WITH TIME ZONE NOT NULL,
    bucket_type            VARCHAR(10)  NOT NULL DEFAULT 'MINUTE',
    count                  BIGINT       NOT NULL DEFAULT 0,
    created_date_technical BIGINT       NOT NULL
);

CREATE INDEX idx_log_metrics_bucket  ON public.log_metrics (bucket DESC);
CREATE INDEX idx_log_metrics_service ON public.log_metrics (service);
CREATE INDEX idx_log_metrics_tenant  ON public.log_metrics (tenant_id);
CREATE UNIQUE INDEX idx_log_metrics_unique
    ON public.log_metrics (tenant_id, service, level, bucket, bucket_type);

COMMENT ON TABLE public.log_metrics IS 'Pre-aggregated log counts per service+level per time bucket for dashboards and alerting';
COMMENT ON COLUMN public.log_metrics.bucket IS 'Start of the time bucket (truncated to minute or hour)';
COMMENT ON COLUMN public.log_metrics.bucket_type IS 'Granularity of the bucket: MINUTE or HOUR';
COMMENT ON COLUMN public.log_metrics.count IS 'Number of log entries in this bucket for this service+level';

-------------------------------------------------------------------------------
-- TABLE: public.alert_rules
-- Threshold-based alerting rules evaluated against log_metrics
-------------------------------------------------------------------------------

CREATE TABLE public.alert_rules (
    id                     UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    tenant_id              UUID REFERENCES public.tenants (id) ON UPDATE CASCADE ON DELETE SET NULL,
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

COMMENT ON TABLE public.alert_rules IS 'Rules that fire notifications when log counts exceed thresholds';
COMMENT ON COLUMN public.alert_rules.service IS 'Service to watch. NULL matches all services.';
COMMENT ON COLUMN public.alert_rules.level IS 'Log level to watch. NULL matches all levels.';
COMMENT ON COLUMN public.alert_rules.threshold_count IS 'Fire alert when count exceeds this value within window_minutes';
COMMENT ON COLUMN public.alert_rules.window_minutes IS 'Rolling time window in minutes for threshold evaluation';
COMMENT ON COLUMN public.alert_rules.cooldown_minutes IS 'Minimum minutes between repeated alerts for the same rule';
COMMENT ON COLUMN public.alert_rules.notification_type IS 'Delivery channel: EMAIL, SLACK, or WEBHOOK';
COMMENT ON COLUMN public.alert_rules.notification_target IS 'Email address, Slack webhook URL, or generic HTTP URL';
COMMENT ON COLUMN public.alert_rules.last_fired_at IS 'When this rule last triggered an alert (for cooldown check)';
