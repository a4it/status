-- Add health check configuration fields to status_apps
ALTER TABLE status_apps
    ADD COLUMN check_enabled BOOLEAN DEFAULT false,
    ADD COLUMN check_type VARCHAR(50) DEFAULT 'NONE',
    ADD COLUMN check_url VARCHAR(500),
    ADD COLUMN check_interval_seconds INTEGER DEFAULT 60,
    ADD COLUMN check_timeout_seconds INTEGER DEFAULT 10,
    ADD COLUMN check_expected_status INTEGER DEFAULT 200,
    ADD COLUMN check_failure_threshold INTEGER DEFAULT 3,
    ADD COLUMN last_check_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN last_check_success BOOLEAN,
    ADD COLUMN last_check_message VARCHAR(1000),
    ADD COLUMN consecutive_failures INTEGER DEFAULT 0;

COMMENT ON COLUMN status_apps.check_enabled IS 'Whether automatic health checking is enabled for this app';
COMMENT ON COLUMN status_apps.check_type IS 'Type of health check: NONE, PING, HTTP_GET, SPRING_BOOT_HEALTH, TCP_PORT';
COMMENT ON COLUMN status_apps.check_url IS 'URL or host:port to check';
COMMENT ON COLUMN status_apps.check_interval_seconds IS 'How often to perform the health check in seconds';
COMMENT ON COLUMN status_apps.check_timeout_seconds IS 'Timeout for the health check in seconds';
COMMENT ON COLUMN status_apps.check_expected_status IS 'Expected HTTP status code for HTTP_GET checks';
COMMENT ON COLUMN status_apps.check_failure_threshold IS 'Number of consecutive failures before status change';
COMMENT ON COLUMN status_apps.last_check_at IS 'Timestamp of the last health check';
COMMENT ON COLUMN status_apps.last_check_success IS 'Whether the last health check was successful';
COMMENT ON COLUMN status_apps.last_check_message IS 'Message from the last health check (error details or success info)';
COMMENT ON COLUMN status_apps.consecutive_failures IS 'Current count of consecutive failed health checks';

-- Add health check configuration fields to status_components
ALTER TABLE status_components
    ADD COLUMN check_inherit_from_app BOOLEAN DEFAULT true,
    ADD COLUMN check_enabled BOOLEAN DEFAULT false,
    ADD COLUMN check_type VARCHAR(50) DEFAULT 'NONE',
    ADD COLUMN check_url VARCHAR(500),
    ADD COLUMN check_interval_seconds INTEGER DEFAULT 60,
    ADD COLUMN check_timeout_seconds INTEGER DEFAULT 10,
    ADD COLUMN check_expected_status INTEGER DEFAULT 200,
    ADD COLUMN check_failure_threshold INTEGER DEFAULT 3,
    ADD COLUMN last_check_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN last_check_success BOOLEAN,
    ADD COLUMN last_check_message VARCHAR(1000),
    ADD COLUMN consecutive_failures INTEGER DEFAULT 0;

COMMENT ON COLUMN status_components.check_inherit_from_app IS 'Whether to inherit health check config from parent app';
COMMENT ON COLUMN status_components.check_enabled IS 'Whether automatic health checking is enabled for this component';
COMMENT ON COLUMN status_components.check_type IS 'Type of health check: NONE, PING, HTTP_GET, SPRING_BOOT_HEALTH, TCP_PORT';
COMMENT ON COLUMN status_components.check_url IS 'URL or host:port to check';
COMMENT ON COLUMN status_components.check_interval_seconds IS 'How often to perform the health check in seconds';
COMMENT ON COLUMN status_components.check_timeout_seconds IS 'Timeout for the health check in seconds';
COMMENT ON COLUMN status_components.check_expected_status IS 'Expected HTTP status code for HTTP_GET checks';
COMMENT ON COLUMN status_components.check_failure_threshold IS 'Number of consecutive failures before status change';
COMMENT ON COLUMN status_components.last_check_at IS 'Timestamp of the last health check';
COMMENT ON COLUMN status_components.last_check_success IS 'Whether the last health check was successful';
COMMENT ON COLUMN status_components.last_check_message IS 'Message from the last health check (error details or success info)';
COMMENT ON COLUMN status_components.consecutive_failures IS 'Current count of consecutive failed health checks';
