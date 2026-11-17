-- Operator SQL: Widen ai_feedback_cache.learning_area from VARCHAR(32) to VARCHAR(64)
-- Allows challenge UUIDs to serve as cache keys for challenge-scoped AI feedback.
-- Startup migrator AiFeedbackCacheSchemaMigrator applies the same statement on PostgreSQL.

ALTER TABLE ai_feedback_cache ALTER COLUMN learning_area TYPE VARCHAR(64);
