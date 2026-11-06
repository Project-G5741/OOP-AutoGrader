-- Lab prefix: Official (counts in lecturer Score total) or Practice (column only).
-- Existing rows stay Official so current Score averages do not change.
-- Startup migrator LabPrefixSchemaMigrator applies the same statement on PostgreSQL.

ALTER TABLE lab
    ADD COLUMN IF NOT EXISTS prefix VARCHAR(16) NOT NULL DEFAULT 'Official';
