package unit.com.eiu.capstone.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.jdbc.core.JdbcTemplate;

import com.eiu.capstone.backend.config.TestcaseSchemaMigrator;
import com.eiu.capstone.backend.model.OopPrincipleTag;
import com.eiu.capstone.backend.model.Testcase;
import com.eiu.capstone.backend.model.TestcaseInvocation;

@ExtendWith(MockitoExtension.class)
class TestcaseSchemaMigratorTest {

    private static final Set<String> RECEIVER_COLUMNS = Set.of(
            "receiver_constructor_id", "receiver_params");

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void missingScenarioColumns_addTagStepsAndDispatchFk_dropSingleInvocationUnique() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    if (sql.contains("information_schema.columns")) {
                        String column = invocation.getArgument(3);
                        return RECEIVER_COLUMNS.contains(column);
                    }
                    String constraintName = invocation.getArgument(2);
                    String constraintType = invocation.getArgument(3);
                    if ("FOREIGN KEY".equals(constraintType)
                            && "testcase_invocation_receiver_constructor_id_fkey".equals(constraintName)) {
                        return true;
                    }
                    return "UNIQUE".equals(constraintType)
                            && "testcase_invocation_testcase_id_key".equals(constraintName);
                });

        invokeEnsureSchema();

        verify(jdbcTemplate).execute(contains("CREATE TYPE oop_principle_tag"));
        verify(jdbcTemplate).execute(contains("ADD COLUMN oop_principle_tag"));
        verify(jdbcTemplate).execute(contains("ADD COLUMN order_index"));
        verify(jdbcTemplate).execute(contains("ADD COLUMN instance_name"));
        verify(jdbcTemplate).execute(contains("ADD COLUMN dispatch_class_id"));
        verify(jdbcTemplate).execute(contains("testcase_invocation_dispatch_class_id_fkey"));
        verify(jdbcTemplate).execute(contains("DROP CONSTRAINT testcase_invocation_testcase_id_key"));
        verify(jdbcTemplate).execute(contains("UNIQUE (testcase_id, order_index)"));
        verify(jdbcTemplate, never()).execute(contains("ADD COLUMN receiver_constructor_id"));
    }

    @Test
    void existingScenarioSchema_isIdempotentAsideFromCreateType() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    if (sql.contains("information_schema.columns")) {
                        return true;
                    }
                    String constraintName = invocation.getArgument(2);
                    String constraintType = invocation.getArgument(3);
                    if ("UNIQUE".equals(constraintType)) {
                        return "testcase_invocation_testcase_id_order_index_key".equals(constraintName);
                    }
                    return true;
                });

        invokeEnsureSchema();

        verify(jdbcTemplate).execute(contains("CREATE TYPE oop_principle_tag"));
        verify(jdbcTemplate, never()).execute(contains("ADD COLUMN oop_principle_tag"));
        verify(jdbcTemplate, never()).execute(contains("DROP CONSTRAINT testcase_invocation_testcase_id_key"));
        verify(jdbcTemplate, never()).execute(contains("UNIQUE (testcase_id, order_index)"));
    }

    @Test
    void operatorSql_isAdditiveAndBackfillsUnitAtOrderZero() throws Exception {
        String sql = readOperatorSql();

        assertTrue(sql.contains("CREATE TYPE oop_principle_tag"));
        assertTrue(sql.contains("'Unit'"));
        assertTrue(sql.contains("'Polymorphism'"));
        assertTrue(sql.contains("'Encapsulation'"));
        assertTrue(sql.contains("'Composition'"));
        assertTrue(sql.contains("'Inheritance'"));
        assertTrue(sql.contains("NOT NULL DEFAULT 'Unit'"));
        assertTrue(sql.contains("order_index INTEGER NOT NULL DEFAULT 0"));
        assertTrue(sql.contains("instance_name TEXT"));
        assertTrue(sql.contains("dispatch_class_id UUID"));
        assertTrue(sql.contains("REFERENCES class_entity(id) ON DELETE CASCADE"));
        assertTrue(sql.contains("DROP CONSTRAINT IF EXISTS testcase_invocation_testcase_id_key"));
        assertTrue(sql.contains("UNIQUE (testcase_id, order_index)"));
        assertFalse(sql.lines().anyMatch(line -> line.trim().toUpperCase().startsWith("TRUNCATE")));
        assertFalse(sql.contains("testcase_instance"));
        assertFalse(sql.contains("comparison_method"));
    }

    @Test
    void newEntities_defaultToUnitTagAndInvocationOrderZero() {
        assertEquals(OopPrincipleTag.Unit, new Testcase().getOopPrincipleTag());
        assertEquals(0, new TestcaseInvocation().getOrderIndex());
    }

    private void invokeEnsureSchema() throws Exception {
        Method method = TestcaseSchemaMigrator.class.getDeclaredMethod("ensureSchema");
        method.setAccessible(true);
        method.invoke(new TestcaseSchemaMigrator(jdbcTemplate));
    }

    private static String readOperatorSql() throws IOException {
        String resource = "/sql/2026-09-17-testcase-scenario-steps.sql";
        try (InputStream in = TestcaseSchemaMigratorTest.class.getResourceAsStream(resource)) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        Path fromBackend = Path.of("..", "docs", "sql", "2026-09-17-testcase-scenario-steps.sql");
        if (Files.exists(fromBackend)) {
            return Files.readString(fromBackend);
        }
        Path fromRoot = Path.of("docs", "sql", "2026-09-17-testcase-scenario-steps.sql");
        if (Files.exists(fromRoot)) {
            return Files.readString(fromRoot);
        }
        throw new IllegalStateException("Missing operator SQL fixture: " + resource);
    }
}
