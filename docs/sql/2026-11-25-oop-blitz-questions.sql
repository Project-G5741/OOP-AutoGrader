-- Operator mirror of OopBlitzSchemaMigrator custom question bank table (web/cloud PostgreSQL).

CREATE TABLE IF NOT EXISTS oop_blitz_question (
    id            UUID PRIMARY KEY,
    kind          VARCHAR(16) NOT NULL,
    difficulty    VARCHAR(16) NOT NULL,
    prompt        TEXT NOT NULL,
    code_lines    TEXT NOT NULL,
    options_json  TEXT NOT NULL DEFAULT '[]',
    correct_index INTEGER NOT NULL,
    explanation   TEXT NOT NULL,
    topic         VARCHAR(64),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_by    UUID REFERENCES user_account(id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_oop_blitz_question_active_diff
    ON oop_blitz_question (active, difficulty);
