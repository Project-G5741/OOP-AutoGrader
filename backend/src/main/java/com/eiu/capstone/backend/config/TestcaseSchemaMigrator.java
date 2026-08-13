package com.eiu.capstone.backend.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Ensures optional operational-testcase columns exist on older databases.
 */
@Component
public class TestcaseSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public TestcaseSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureReceiverColumns() {
        if (!columnExists("testcase_invocation", "receiver_constructor_id")) {
            jdbcTemplate.execute("""
                    ALTER TABLE testcase_invocation
                        ADD COLUMN receiver_constructor_id UUID
                    """);
        }
        if (!columnExists("testcase_invocation", "receiver_params")) {
            jdbcTemplate.execute("""
                    ALTER TABLE testcase_invocation
                        ADD COLUMN receiver_params JSONB NOT NULL DEFAULT '[]'
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE testcase_invocation ALTER COLUMN receiver_params DROP DEFAULT
                    """);
        }
        if (!foreignKeyExists("testcase_invocation_receiver_constructor_id_fkey")) {
            jdbcTemplate.execute("""
                    ALTER TABLE testcase_invocation ADD CONSTRAINT testcase_invocation_receiver_constructor_id_fkey
                        FOREIGN KEY (receiver_constructor_id) REFERENCES constructor(id) ON DELETE CASCADE
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

    private boolean foreignKeyExists(String constraintName) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.table_constraints
                    WHERE table_schema = current_schema()
                      AND constraint_name = ?
                      AND constraint_type = 'FOREIGN KEY'
                )
                """, Boolean.class, constraintName);
        return Boolean.TRUE.equals(exists);
    }
}
