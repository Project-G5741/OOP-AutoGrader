package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Ensures {@code submission_challenge_result.mmd_outcomes} exists on older databases.
 */
@Component
@Profile("!desktop")
public class MmdOutcomesSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public MmdOutcomesSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureMmdOutcomes() {
        if (!columnExists("submission_challenge_result", "mmd_outcomes")) {
            jdbcTemplate.execute("""
                    ALTER TABLE submission_challenge_result
                        ADD COLUMN mmd_outcomes JSONB
                    """);
        }
    }

    private boolean columnExists(String table, String column) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.columns
                    WHERE table_schema = current_schema()
                      AND table_name = ?
                      AND column_name = ?
                )
                """, Boolean.class, table, column);
        return Boolean.TRUE.equals(exists);
    }
}
