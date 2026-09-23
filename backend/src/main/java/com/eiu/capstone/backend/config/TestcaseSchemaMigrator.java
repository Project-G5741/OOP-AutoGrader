package com.eiu.capstone.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.rubric.LabRubricCache;

import jakarta.annotation.PostConstruct;

/**
 * Ensures receiver columns exist, then wipes leftover COMPARISON / tag / per-testcase
 * weight schema and rewrites testcase types to UNIT / COMPOSITION.
 */
@Component
public class TestcaseSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;
    private final LabRubricCache labRubricCache;

    public TestcaseSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, null);
    }

    @Autowired
    public TestcaseSchemaMigrator(JdbcTemplate jdbcTemplate, LabRubricCache labRubricCache) {
        this.jdbcTemplate = jdbcTemplate;
        this.labRubricCache = labRubricCache;
    }

    @PostConstruct
    void ensureSchema() {
        ensureReceiverColumns();
        if (needsUnitCompositionWipe()) {
            applyUnitCompositionWipe();
            if (labRubricCache != null) {
                labRubricCache.invalidateAll();
            }
        }
        ensureKeptScenarioColumns();
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

    private boolean needsUnitCompositionWipe() {
        return enumHasLabel("testcase_type", "SINGLE_INVOCATION")
                || enumHasLabel("testcase_type", "COMPARISON")
                || enumHasLabel("assertion_kind", "COMPARISON_RESULT")
                || columnExists("testcase", "oop_principle_tag")
                || columnExists("testcase", "weight")
                || columnExists("testcase", "comparison_method")
                || tableExists("testcase_instance");
    }

    private void applyUnitCompositionWipe() {
        jdbcTemplate.execute("TRUNCATE TABLE submission_testcase_assertion_result CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE submission_testcase_result CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE testcase CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS testcase_instance CASCADE");
        jdbcTemplate.execute("ALTER TABLE testcase DROP CONSTRAINT IF EXISTS testcase_comparison_method_check");
        jdbcTemplate.execute("ALTER TABLE testcase DROP COLUMN IF EXISTS comparison_method");
        jdbcTemplate.execute("ALTER TABLE testcase DROP COLUMN IF EXISTS oop_principle_tag");
        jdbcTemplate.execute("ALTER TABLE testcase DROP COLUMN IF EXISTS weight");
        jdbcTemplate.execute("DROP TYPE IF EXISTS oop_principle_tag");
        jdbcTemplate.execute("DROP TYPE IF EXISTS testcase_comparison_method");
        jdbcTemplate.execute("""
                DO $$ BEGIN
                    CREATE TYPE testcase_type_new AS ENUM ('UNIT', 'COMPOSITION');
                EXCEPTION WHEN duplicate_object THEN NULL;
                END $$
                """);
        jdbcTemplate.execute("""
                ALTER TABLE testcase ALTER COLUMN testcase_type DROP DEFAULT
                """);
        jdbcTemplate.execute("""
                ALTER TABLE testcase
                    ALTER COLUMN testcase_type TYPE testcase_type_new
                    USING 'UNIT'::testcase_type_new
                """);
        jdbcTemplate.execute("DROP TYPE testcase_type");
        jdbcTemplate.execute("ALTER TYPE testcase_type_new RENAME TO testcase_type");
        jdbcTemplate.execute("""
                DO $$ BEGIN
                    CREATE TYPE assertion_kind_new AS ENUM (
                        'RETURN_VALUE', 'FIELD_STATE', 'STDOUT', 'EXCEPTION'
                    );
                EXCEPTION WHEN duplicate_object THEN NULL;
                END $$
                """);
        jdbcTemplate.execute("""
                ALTER TABLE testcase_assertion ALTER COLUMN assertion_kind DROP DEFAULT
                """);
        jdbcTemplate.execute("""
                ALTER TABLE testcase_assertion
                    ALTER COLUMN assertion_kind TYPE assertion_kind_new
                    USING (
                        CASE
                            WHEN assertion_kind::text = 'COMPARISON_RESULT' THEN 'RETURN_VALUE'
                            ELSE assertion_kind::text
                        END
                    )::assertion_kind_new
                """);
        jdbcTemplate.execute("DROP TYPE assertion_kind");
        jdbcTemplate.execute("ALTER TYPE assertion_kind_new RENAME TO assertion_kind");
    }

    private void ensureKeptScenarioColumns() {
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

    private boolean enumHasLabel(String typeName, String label) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM pg_catalog.pg_type t
                    JOIN pg_catalog.pg_enum e ON e.enumtypid = t.oid
                    WHERE t.typname = ?
                      AND e.enumlabel = ?
                )
                """, Boolean.class, typeName, label);
        return Boolean.TRUE.equals(exists);
    }

    private boolean tableExists(String table) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.tables
                    WHERE table_schema = current_schema()
                      AND table_name = ?
                )
                """, Boolean.class, table);
        return Boolean.TRUE.equals(exists);
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
