package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Ensures {@code student_oop_blitz_session} and its indexes exist on PostgreSQL.
 */
@Component
@Profile("!desktop & !desktop-lecturer")
public class OopBlitzSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public OopBlitzSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureOopBlitzSchema() {
        jdbcTemplate.execute("""
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
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_oop_blitz_user_date
                    ON student_oop_blitz_session (user_id, play_date)
                """);
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uq_oop_blitz_active_user
                    ON student_oop_blitz_session (user_id)
                    WHERE status = 'ACTIVE'
                """);
        jdbcTemplate.execute("""
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
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_oop_blitz_question_active_diff
                    ON oop_blitz_question (active, difficulty)
                """);
    }
}
