CREATE TABLE IF NOT EXISTS alert_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id),
    name VARCHAR(255) NOT NULL,
    service VARCHAR(255),
    level VARCHAR(20),
    threshold_count BIGINT NOT NULL,
    window_minutes INT NOT NULL,
    cooldown_minutes INT DEFAULT 15,
    notification_type VARCHAR(20) NOT NULL,
    notification_target TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    last_fired_at TIMESTAMPTZ,
    created_date TIMESTAMPTZ NOT NULL,
    created_date_technical BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS drop_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id),
    name VARCHAR(255) NOT NULL,
    level VARCHAR(20),
    service VARCHAR(255),
    message_pattern VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_date TIMESTAMPTZ NOT NULL,
    created_date_technical BIGINT NOT NULL
);
