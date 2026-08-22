package com.eiu.capstone.backend.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Ensures scoring-weight columns exist on older databases.
 */
@Component
public class ScoringWeightSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public ScoringWeightSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureTestcaseWeight() {
        if (!columnExists("challenge", "testcase_weight")) {
            jdbcTemplate.execute("""
                    ALTER TABLE challenge
                        ADD COLUMN testcase_weight INTEGER NOT NULL DEFAULT 1
                    """);
        }
        if (!checkConstraintExists("challenge_testcase_weight_positive")) {
            jdbcTemplate.execute("""
                    ALTER TABLE challenge ADD CONSTRAINT challenge_testcase_weight_positive
                        CHECK (testcase_weight >= 1)
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

    private boolean checkConstraintExists(String constraintName) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.table_constraints
                    WHERE table_schema = current_schema()
                      AND constraint_name = ?
                      AND constraint_type = 'CHECK'
                )
                """, Boolean.class, constraintName);
        return Boolean.TRUE.equals(exists);
    }
}
