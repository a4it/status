CREATE TABLE IF NOT EXISTS log_api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id),
    name VARCHAR(255) NOT NULL,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    is_active BOOLEAN DEFAULT TRUE,
    created_date TIMESTAMPTZ NOT NULL,
    created_date_technical BIGINT NOT NULL
);
