-- Validated Conceptual AI feedback cache (no API keys).
-- Startup migrator AiFeedbackCacheSchemaMigrator applies the same CREATE on PostgreSQL.

CREATE TABLE IF NOT EXISTS ai_feedback_cache (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    submission_id UUID NOT NULL,
    learning_area VARCHAR(32) NOT NULL,
    language VARCHAR(8) NOT NULL,
    policy_version VARCHAR(64) NOT NULL,
    model_identifier VARCHAR(128) NOT NULL,
    payload_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_ai_feedback_cache_key
        UNIQUE (student_id, submission_id, learning_area, language, policy_version)
);
