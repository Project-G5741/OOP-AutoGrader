-- Session version for JWT revoke on delete / suspend / current-term remove.
-- Also applied on API startup by SessionVersionSchemaMigrator when missing.

ALTER TABLE user_account
    ADD COLUMN IF NOT EXISTS session_version INTEGER NOT NULL DEFAULT 0;
