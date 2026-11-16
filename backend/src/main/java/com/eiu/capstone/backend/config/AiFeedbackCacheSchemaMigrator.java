package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@Profile("!desktop & !desktop-lecturer")
public class AiFeedbackCacheSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public AiFeedbackCacheSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureAiFeedbackCache() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_feedback_cache (
                    id UUID PRIMARY KEY,
                    student_id UUID NOT NULL,
                    submission_id UUID NOT NULL,
                    learning_area VARCHAR(64) NOT NULL,
                    language VARCHAR(8) NOT NULL,
                    policy_version VARCHAR(64) NOT NULL,
                    model_identifier VARCHAR(128) NOT NULL,
                    payload_json TEXT NOT NULL,
                    created_at TIMESTAMPTZ NOT NULL,
                    expires_at TIMESTAMPTZ NOT NULL,
                    CONSTRAINT uk_ai_feedback_cache_key
                        UNIQUE (student_id, submission_id, learning_area, language, policy_version)
                )
                """);
        jdbcTemplate.execute("""
                ALTER TABLE ai_feedback_cache
                    ALTER COLUMN learning_area TYPE VARCHAR(64)
                """);
    }
}
