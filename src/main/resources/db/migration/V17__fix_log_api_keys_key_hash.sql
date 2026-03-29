-- Fix: ensure key_hash and key_prefix columns exist on log_api_keys.
-- V15 may not have been applied if the migration was added after the DB was already at a higher version.

ALTER TABLE log_api_keys
    ADD COLUMN IF NOT EXISTS key_hash   VARCHAR(64),
    ADD COLUMN IF NOT EXISTS key_prefix VARCHAR(8);

-- Backfill from existing plaintext api_key if the column still exists.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'log_api_keys' AND column_name = 'api_key'
    ) THEN
        UPDATE log_api_keys
        SET key_prefix = LEFT(api_key, 8),
            key_hash   = 'MIGRATED_INVALIDATED_REHASH_REQUIRED',
            is_active  = FALSE
        WHERE key_hash IS NULL;

        ALTER TABLE log_api_keys DROP COLUMN IF EXISTS api_key;
    END IF;
END $$;

-- Ensure NOT NULL on key_hash and key_prefix for any rows that may still be null.
UPDATE log_api_keys
SET key_hash   = 'MIGRATED_INVALIDATED_REHASH_REQUIRED',
    key_prefix = 'UNKNOWN_'
WHERE key_hash IS NULL OR key_prefix IS NULL;

-- Now safe to enforce NOT NULL.
ALTER TABLE log_api_keys ALTER COLUMN key_hash   SET NOT NULL;
ALTER TABLE log_api_keys ALTER COLUMN key_prefix SET NOT NULL;

-- Add unique constraint if it doesn't already exist.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'log_api_keys' AND constraint_name = 'uq_log_api_keys_key_hash'
    ) THEN
        ALTER TABLE log_api_keys ADD CONSTRAINT uq_log_api_keys_key_hash UNIQUE (key_hash);
    END IF;
END $$;
