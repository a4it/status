CREATE TABLE process_mining_retention_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE,
    platform_id UUID REFERENCES status_platforms(id) ON DELETE CASCADE,
    retention_days INT NOT NULL DEFAULT 30,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_run_at TIMESTAMPTZ,
    last_run_deleted_count INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_retention_rules_tenant ON process_mining_retention_rules(tenant_id);
CREATE INDEX idx_retention_rules_platform ON process_mining_retention_rules(platform_id);
