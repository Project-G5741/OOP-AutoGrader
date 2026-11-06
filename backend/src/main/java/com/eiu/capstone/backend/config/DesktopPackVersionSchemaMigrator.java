package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Ensures {@code lab_submission.desktop_pack_version} exists on older PostgreSQL databases.
 * Nullable — set only for desktop practice attempts; web uploads leave it null.
 */
@Component
@Profile("!desktop")
public class DesktopPackVersionSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public DesktopPackVersionSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureDesktopPackVersionColumn() {
        if (!columnExists("lab_submission", "desktop_pack_version")) {
            jdbcTemplate.execute("""
                    ALTER TABLE lab_submission
                        ADD COLUMN desktop_pack_version VARCHAR(255)
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
