package com.eiu.capstone.backend.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Ensures {@code user_account.session_version} exists on older databases.
 */
@Component
public class SessionVersionSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public SessionVersionSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureSessionVersion() {
        if (!columnExists("user_account", "session_version")) {
            jdbcTemplate.execute("""
                    ALTER TABLE user_account
                        ADD COLUMN session_version INTEGER NOT NULL DEFAULT 0
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
