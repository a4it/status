-- Add health check configuration fields to status_platforms table
ALTER TABLE status_platforms ADD COLUMN IF NOT EXISTS check_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE status_platforms ADD COLUMN IF NOT EXISTS check_type VARCHAR(50) DEFAULT 'NONE';
ALTER TABLE status_platforms ADD COLUMN IF NOT EXISTS check_url VARCHAR(500);
ALTER TABLE status_platforms ADD COLUMN IF NOT EXISTS check_interval_seconds INTEGER DEFAULT 60;
ALTER TABLE status_platforms ADD COLUMN IF NOT EXISTS check_timeout_seconds INTEGER DEFAULT 10;
ALTER TABLE status_platforms ADD COLUMN IF NOT EXISTS check_expected_status INTEGER DEFAULT 200;
ALTER TABLE status_platforms ADD COLUMN IF NOT EXISTS check_failure_threshold INTEGER DEFAULT 3;
ALTER TABLE status_platforms ADD COLUMN IF NOT EXISTS last_check_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE status_platforms ADD COLUMN IF NOT EXISTS last_check_success BOOLEAN;
ALTER TABLE status_platforms ADD COLUMN IF NOT EXISTS last_check_message VARCHAR(1000);
ALTER TABLE status_platforms ADD COLUMN IF NOT EXISTS consecutive_failures INTEGER DEFAULT 0;

-- Add platform_id foreign key to status_uptime_history table
ALTER TABLE status_uptime_history ADD COLUMN IF NOT EXISTS platform_id UUID;
ALTER TABLE status_uptime_history ADD CONSTRAINT fk_status_uptime_history_platform
    FOREIGN KEY (platform_id) REFERENCES status_platforms(id) ON DELETE CASCADE;
