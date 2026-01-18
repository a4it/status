-- Add platform_id foreign key to status_uptime_history table
ALTER TABLE status_uptime_history ADD COLUMN IF NOT EXISTS platform_id UUID;

-- Add foreign key constraint (drop first if exists to make idempotent)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_status_uptime_history_platform'
        AND table_name = 'status_uptime_history'
    ) THEN
        ALTER TABLE status_uptime_history ADD CONSTRAINT fk_status_uptime_history_platform
            FOREIGN KEY (platform_id) REFERENCES status_platforms(id) ON DELETE CASCADE;
    END IF;
END $$;
