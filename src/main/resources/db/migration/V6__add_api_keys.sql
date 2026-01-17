-- Add API keys to status_apps and status_components for event logging authentication

ALTER TABLE status_apps ADD COLUMN api_key VARCHAR(64);
ALTER TABLE status_components ADD COLUMN api_key VARCHAR(64);

-- Create unique indexes for API key lookups
CREATE UNIQUE INDEX idx_status_apps_api_key ON status_apps(api_key) WHERE api_key IS NOT NULL;
CREATE UNIQUE INDEX idx_status_components_api_key ON status_components(api_key) WHERE api_key IS NOT NULL;
