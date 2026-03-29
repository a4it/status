-- MED-03: Replace plaintext API key storage with a SHA-256 hash + key prefix.
-- The plaintext api_key column is replaced by:
--   key_hash   VARCHAR(64) — SHA-256 hex digest of the API key (for secure lookup)
--   key_prefix VARCHAR(8)  — First 8 characters for display purposes only
--
-- EXISTING KEYS: Existing plaintext keys cannot be rehashed without knowing the
-- original value. They are marked inactive and their prefix is preserved so
-- operators can identify which keys need to be re-created.

ALTER TABLE log_api_keys
    ADD COLUMN IF NOT EXISTS key_hash   VARCHAR(64),
    ADD COLUMN IF NOT EXISTS key_prefix VARCHAR(8);

-- Backfill prefix from existing plaintext keys; set a sentinel hash value.
UPDATE log_api_keys
SET key_prefix = LEFT(api_key, 8),
    key_hash   = 'MIGRATED_INVALIDATED_REHASH_REQUIRED',
    is_active  = FALSE
WHERE key_hash IS NULL;

-- Drop old plaintext column.
ALTER TABLE log_api_keys DROP COLUMN IF EXISTS api_key;

-- Enforce NOT NULL now that all rows are populated.
ALTER TABLE log_api_keys ALTER COLUMN key_hash   SET NOT NULL;
ALTER TABLE log_api_keys ALTER COLUMN key_prefix SET NOT NULL;

-- Unique constraint on the hash column (replaces old UNIQUE on api_key).
ALTER TABLE log_api_keys ADD CONSTRAINT uq_log_api_keys_key_hash UNIQUE (key_hash);
