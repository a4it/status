CREATE TABLE IF NOT EXISTS public.logs (
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

CREATE INDEX IF NOT EXISTS idx_logs_timestamp  ON public.logs (log_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_logs_level      ON public.logs (level);
CREATE INDEX IF NOT EXISTS idx_logs_service    ON public.logs (service);
CREATE INDEX IF NOT EXISTS idx_logs_tenant     ON public.logs (tenant_id);
CREATE INDEX IF NOT EXISTS idx_logs_trace_id   ON public.logs (trace_id) WHERE trace_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_logs_request_id ON public.logs (request_id) WHERE request_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS public.log_metrics (
    id                     UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    tenant_id              UUID REFERENCES public.tenants (id) ON UPDATE CASCADE ON DELETE SET NULL,
    service                VARCHAR(255) NOT NULL,
    level                  VARCHAR(20)  NOT NULL,
    bucket                 TIMESTAMP WITH TIME ZONE NOT NULL,
    bucket_type            VARCHAR(10)  NOT NULL DEFAULT 'MINUTE',
    count                  BIGINT       NOT NULL DEFAULT 0,
    created_date_technical BIGINT       NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_log_metrics_bucket  ON public.log_metrics (bucket DESC);
CREATE INDEX IF NOT EXISTS idx_log_metrics_service ON public.log_metrics (service);
CREATE INDEX IF NOT EXISTS idx_log_metrics_tenant  ON public.log_metrics (tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_log_metrics_unique
    ON public.log_metrics (tenant_id, service, level, bucket, bucket_type);
