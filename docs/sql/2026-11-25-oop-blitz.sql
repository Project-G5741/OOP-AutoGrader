-- Operator mirror of OopBlitzSchemaMigrator (web/cloud PostgreSQL).

CREATE TABLE IF NOT EXISTS student_oop_blitz_session (
    id                 UUID PRIMARY KEY,
    user_id            UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    play_date          DATE NOT NULL,
    mode               VARCHAR(16) NOT NULL,
    status             VARCHAR(16) NOT NULL,
    questions_json     TEXT NOT NULL,
    answers_json       TEXT NOT NULL DEFAULT '[]',
    current_index      INTEGER NOT NULL DEFAULT 0,
    correct_count      INTEGER NOT NULL DEFAULT 0,
    current_combo      INTEGER NOT NULL DEFAULT 0,
    max_combo          INTEGER NOT NULL DEFAULT 0,
    speed_bonus_total  INTEGER NOT NULL DEFAULT 0,
    points_earned      INTEGER NOT NULL DEFAULT 0,
    points_credited    INTEGER NOT NULL DEFAULT 0,
    question_issued_at TIMESTAMPTZ,
    started_at         TIMESTAMPTZ NOT NULL,
    finished_at        TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_oop_blitz_user_date
    ON student_oop_blitz_session (user_id, play_date);

CREATE UNIQUE INDEX IF NOT EXISTS uq_oop_blitz_active_user
    ON student_oop_blitz_session (user_id)
    WHERE status = 'ACTIVE';
