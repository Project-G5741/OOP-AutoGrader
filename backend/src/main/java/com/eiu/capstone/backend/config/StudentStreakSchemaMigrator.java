package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Ensures {@code student_daily_streak} exists on older PostgreSQL databases.
 * Also rewrites a stored exclusive mascot id from {@code gigi} to {@code goat}.
 * Desktop practice creates the same table through Hibernate {@code ddl-auto: update}.
 */
@Component
@Profile("!desktop & !desktop-lecturer")
public class StudentStreakSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public StudentStreakSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureStudentStreak() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS student_daily_streak (
                    user_id             UUID PRIMARY KEY REFERENCES user_account(id) ON DELETE CASCADE,
                    streak_count        INTEGER NOT NULL DEFAULT 0,
                    points              INTEGER NOT NULL DEFAULT 0,
                    last_completed_on   DATE,
                    mission_date        DATE,
                    logged_in           BOOLEAN NOT NULL DEFAULT FALSE,
                    submit_count        INTEGER NOT NULL DEFAULT 0,
                    pet_count           INTEGER NOT NULL DEFAULT 0,
                    awarded_today       BOOLEAN NOT NULL DEFAULT FALSE,
                    streak_broken       BOOLEAN NOT NULL DEFAULT FALSE,
                    saved_streak        INTEGER NOT NULL DEFAULT 0,
                    restore_year_month  VARCHAR(7)
                )
                """);
        jdbcTemplate.execute("""
                ALTER TABLE student_daily_streak
                    ADD COLUMN IF NOT EXISTS equipped_mascot VARCHAR(32) NOT NULL DEFAULT 'fox'
                """);
        jdbcTemplate.execute("""
                ALTER TABLE student_daily_streak
                    ADD COLUMN IF NOT EXISTS gacha_tickets INTEGER NOT NULL DEFAULT 0
                """);
        jdbcTemplate.execute("""
                ALTER TABLE student_daily_streak
                    ADD COLUMN IF NOT EXISTS mission_tickets_awarded INTEGER NOT NULL DEFAULT 0
                """);
        jdbcTemplate.execute("""
                ALTER TABLE student_daily_streak
                    ADD COLUMN IF NOT EXISTS gacha_launch_gift BOOLEAN NOT NULL DEFAULT FALSE
                """);
        jdbcTemplate.execute("""
                ALTER TABLE student_daily_streak
                    ADD COLUMN IF NOT EXISTS crystals INTEGER NOT NULL DEFAULT 0
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS student_mascot_purchase (
                    user_id       UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
                    mascot_id     VARCHAR(32) NOT NULL,
                    price_paid    INTEGER NOT NULL,
                    purchased_at  TIMESTAMPTZ NOT NULL,
                    PRIMARY KEY (user_id, mascot_id)
                )
                """);
        jdbcTemplate.update("UPDATE student_daily_streak SET equipped_mascot = 'goat' WHERE equipped_mascot = 'gigi'");
        jdbcTemplate.update("UPDATE student_mascot_purchase SET mascot_id = 'goat' WHERE mascot_id = 'gigi'");
    }
}
