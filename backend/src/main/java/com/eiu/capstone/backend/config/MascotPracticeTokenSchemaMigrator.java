package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@Profile("!desktop")
public class MascotPracticeTokenSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public MascotPracticeTokenSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensurePracticeToken() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS student_mascot_practice_token (
                    user_id        UUID PRIMARY KEY REFERENCES user_account(id) ON DELETE CASCADE,
                    jti            VARCHAR(64) NOT NULL,
                    compact_token  VARCHAR(2048) NOT NULL,
                    expires_at     TIMESTAMPTZ NOT NULL,
                    created_at     TIMESTAMPTZ NOT NULL,
                    used_at        TIMESTAMPTZ
                )
                """);
        jdbcTemplate.execute("""
                ALTER TABLE student_mascot_practice_token
                    ADD COLUMN IF NOT EXISTS used_at TIMESTAMPTZ
                """);
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS student_mascot_practice_token_jti_idx
                    ON student_mascot_practice_token (jti)
                """);
    }
}
