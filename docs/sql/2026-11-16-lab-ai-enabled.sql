-- Lab AI Learning Assistant eligibility (default off until lecturer enables).
-- Startup migrator LabAiEnabledSchemaMigrator applies the same statement on PostgreSQL.

ALTER TABLE lab
    ADD COLUMN IF NOT EXISTS ai_enabled BOOLEAN NOT NULL DEFAULT false;
