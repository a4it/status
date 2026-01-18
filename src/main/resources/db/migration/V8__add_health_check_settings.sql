-- Health Check Settings table for storing global configuration
CREATE TABLE health_check_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key VARCHAR(100) UNIQUE NOT NULL,
    setting_value VARCHAR(500) NOT NULL,
    description VARCHAR(500),
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Insert default values matching application.properties defaults
INSERT INTO health_check_settings (setting_key, setting_value, description) VALUES
('enabled', 'true', 'Enable or disable all automated health checks'),
('scheduler_interval_ms', '10000', 'Scheduler polling interval in milliseconds'),
('thread_pool_size', '10', 'Number of threads for concurrent health checks'),
('default_interval_seconds', '60', 'Default check interval for new entities in seconds'),
('default_timeout_seconds', '10', 'Default timeout for health checks in seconds');
