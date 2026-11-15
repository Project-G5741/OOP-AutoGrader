package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Hibernate {@code ddl-auto: update} on H2 leaves {@code input_display} (and siblings) as
 * {@code VARCHAR(255)}. Composition OT scripts exceed that and roll back the whole detail
 * persist transaction — so Declaration/OT/MMD fallbacks lose member flags too. Widen to TEXT.
 */
@Component
@Profile({"desktop", "desktop-lecturer"})
public class DesktopSubmissionDisplaySchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public DesktopSubmissionDisplaySchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void widenDisplayColumns() {
        for (String column : new String[] {"input_display", "expected_display", "actual_display", "feedback"}) {
            if (isShortVarchar("submission_testcase_result", column)) {
                jdbcTemplate.execute(
                        "ALTER TABLE submission_testcase_result ALTER COLUMN " + column + " SET DATA TYPE TEXT");
            }
        }
    }

    private boolean isShortVarchar(String table, String column) {
        Integer maxLen = jdbcTemplate.query(
                """
                        SELECT character_maximum_length
                        FROM information_schema.columns
                        WHERE table_schema = CURRENT_SCHEMA()
                          AND table_name = ?
                          AND column_name = ?
                          AND data_type IN ('CHARACTER VARYING', 'VARCHAR')
                        """,
                rs -> rs.next() ? rs.getObject(1, Integer.class) : null,
                table,
                column);
        return maxLen != null && maxLen > 0 && maxLen <= 255;
    }
}
