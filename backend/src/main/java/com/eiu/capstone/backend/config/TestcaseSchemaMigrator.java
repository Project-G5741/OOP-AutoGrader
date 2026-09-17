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
    void ensureSchema() {
        ensureReceiverColumns();
        ensureScenarioColumns();
    }

    private void ensureReceiverColumns() {
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

    private void ensureScenarioColumns() {
        jdbcTemplate.execute("""
                DO $$ BEGIN
                    CREATE TYPE oop_principle_tag AS ENUM (
                        'Unit',
                        'Polymorphism',
                        'Encapsulation',
                        'Composition',
                        'Inheritance'
                    );
                EXCEPTION WHEN duplicate_object THEN NULL;
                END $$
                """);
        if (!columnExists("testcase", "oop_principle_tag")) {
            jdbcTemplate.execute("""
                    ALTER TABLE testcase
                        ADD COLUMN oop_principle_tag oop_principle_tag NOT NULL DEFAULT 'Unit'
                    """);
        }
        if (!columnExists("testcase_invocation", "order_index")) {
            jdbcTemplate.execute("""
                    ALTER TABLE testcase_invocation
                        ADD COLUMN order_index INTEGER NOT NULL DEFAULT 0
                    """);
        }
        if (!columnExists("testcase_invocation", "instance_name")) {
            jdbcTemplate.execute("""
                    ALTER TABLE testcase_invocation
                        ADD COLUMN instance_name TEXT
                    """);
        }
        if (!columnExists("testcase_invocation", "dispatch_class_id")) {
            jdbcTemplate.execute("""
                    ALTER TABLE testcase_invocation
                        ADD COLUMN dispatch_class_id UUID
                    """);
        }
        if (!foreignKeyExists("testcase_invocation_dispatch_class_id_fkey")) {
            jdbcTemplate.execute("""
                    ALTER TABLE testcase_invocation ADD CONSTRAINT testcase_invocation_dispatch_class_id_fkey
                        FOREIGN KEY (dispatch_class_id) REFERENCES class_entity(id) ON DELETE CASCADE
                    """);
        }
        if (uniqueConstraintExists("testcase_invocation_testcase_id_key")) {
            jdbcTemplate.execute("""
                    ALTER TABLE testcase_invocation DROP CONSTRAINT testcase_invocation_testcase_id_key
                    """);
        }
        if (!uniqueConstraintExists("testcase_invocation_testcase_id_order_index_key")) {
            jdbcTemplate.execute("""
                    ALTER TABLE testcase_invocation ADD CONSTRAINT testcase_invocation_testcase_id_order_index_key
                        UNIQUE (testcase_id, order_index)
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
        return constraintExists(constraintName, "FOREIGN KEY");
    }

    private boolean uniqueConstraintExists(String constraintName) {
        return constraintExists(constraintName, "UNIQUE");
    }

    private boolean constraintExists(String constraintName, String constraintType) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.table_constraints
                    WHERE table_schema = current_schema()
                      AND constraint_name = ?
                      AND constraint_type = ?
                )
                """, Boolean.class, constraintName, constraintType);
        return Boolean.TRUE.equals(exists);
    }
}
