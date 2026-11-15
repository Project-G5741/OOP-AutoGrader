package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Adds {@code feedback_item.author_reply_cleared_at} on older PostgreSQL databases.
 * Desktop practice creates the column through Hibernate {@code ddl-auto: update} when scanned.
 */
@Component
@Profile("!desktop & !desktop-lecturer")
public class FeedbackAuthorReplyClearedSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public FeedbackAuthorReplyClearedSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureAuthorReplyClearedAt() {
        jdbcTemplate.execute("""
                ALTER TABLE feedback_item
                    ADD COLUMN IF NOT EXISTS author_reply_cleared_at TIMESTAMPTZ
                """);
    }
}
