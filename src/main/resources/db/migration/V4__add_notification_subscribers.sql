-- Notification Subscribers Table
-- Stores email addresses of users who want to be notified about incidents for specific platforms

CREATE TABLE notification_subscribers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_id UUID NOT NULL REFERENCES status_apps(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_token VARCHAR(255),
    verification_token_expires_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(255) NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(255) NOT NULL,
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_date_technical BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000)::BIGINT,
    last_modified_date_technical BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000)::BIGINT,
    UNIQUE(app_id, email)
);

-- Indexes for efficient querying
CREATE INDEX idx_notification_subscribers_app_id ON notification_subscribers(app_id);
CREATE INDEX idx_notification_subscribers_email ON notification_subscribers(email);
CREATE INDEX idx_notification_subscribers_verification_token ON notification_subscribers(verification_token);
CREATE INDEX idx_notification_subscribers_active_verified ON notification_subscribers(app_id, is_active, is_verified);

-- Comments
COMMENT ON TABLE notification_subscribers IS 'Stores notification subscribers for status applications';
COMMENT ON COLUMN notification_subscribers.app_id IS 'Reference to the status application this subscriber is monitoring';
COMMENT ON COLUMN notification_subscribers.email IS 'Email address to send notifications to';
COMMENT ON COLUMN notification_subscribers.name IS 'Optional display name of the subscriber';
COMMENT ON COLUMN notification_subscribers.is_active IS 'Whether the subscriber should receive notifications';
COMMENT ON COLUMN notification_subscribers.is_verified IS 'Whether the email address has been verified';
COMMENT ON COLUMN notification_subscribers.verification_token IS 'Token for email verification';
COMMENT ON COLUMN notification_subscribers.verification_token_expires_at IS 'Expiration time for the verification token';
