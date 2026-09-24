package unit.com.eiu.capstone.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.jdbc.core.JdbcTemplate;

import com.eiu.capstone.backend.config.ScoringWeightSchemaMigrator;
import com.eiu.capstone.backend.config.TestcaseSchemaMigrator;
import com.eiu.capstone.backend.grading.rubric.LabRubricCache;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.Testcase;
import com.eiu.capstone.backend.model.TestcaseInstance;
import com.eiu.capstone.backend.model.TestcaseInvocation;
import com.eiu.capstone.backend.model.TestcaseType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@ExtendWith(MockitoExtension.class)
class TestcaseSchemaMigratorTest {

    private static final Set<String> RECEIVER_COLUMNS = Set.of(
            "receiver_constructor_id", "receiver_params");
    private static final Set<String> LEFTOVER_TESTCASE_COLUMNS = Set.of(
            "oop_principle_tag", "weight", "comparison_method");
    private static final String OPERATOR_SQL_NAME =
            "2026-09-23-operational-testcase-unit-composition.sql";

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void operatorSql_wipesOperationalTestsAndRewritesUnitComposition() throws Exception {
        String sql = readOperatorSql();
        String upper = sql.toUpperCase(Locale.ROOT);

        assertTrue(upper.contains("TRUNCATE") && upper.contains("SUBMISSION_TESTCASE_ASSERTION_RESULT"));
        assertTrue(upper.contains("TRUNCATE") && upper.contains("SUBMISSION_TESTCASE_RESULT"));
        assertTrue(upper.contains("TRUNCATE") && upper.contains("TESTCASE"));
        assertTrue(upper.contains("CASCADE"));
        assertTrue(sql.contains("'UNIT'"));
        assertTrue(sql.contains("'COMPOSITION'"));
        assertFalse(upper.contains("ADD VALUE"));
        assertFalse(sql.contains("CREATE TYPE oop_principle_tag"));
        assertTrue(sql.contains("oop_principle_tag"));
        assertTrue(upper.contains("DROP") && sql.contains("testcase_instance"));
        assertTrue(sql.contains("COMPARISON_RESULT"));
        assertTrue(assertionKindCreateBlock(sql).contains("'RETURN_VALUE'"));
        assertTrue(assertionKindCreateBlock(sql).contains("'FIELD_STATE'"));
        assertTrue(assertionKindCreateBlock(sql).contains("'STDOUT'"));
        assertTrue(assertionKindCreateBlock(sql).contains("'EXCEPTION'"));
        assertFalse(assertionKindCreateBlock(sql).contains("COMPARISON_RESULT"));
        assertTrue(dropsTestcaseColumn(sql, "oop_principle_tag"));
        assertTrue(dropsTestcaseColumn(sql, "weight"));
        assertFalse(sql.toLowerCase(Locale.ROOT).contains("alter table challenge"));
        assertFalse(dropsChallengeTestcaseWeight(sql));
    }

    @Test
    void oldSchema_executesWipeAndDoesNotCreateOopPrincipleTag() throws Exception {
        stubSchema(true);
        LabRubricCache cache = mock(LabRubricCache.class);

        invokeEnsureSchema(cache);

        verify(jdbcTemplate, atLeastOnce()).execute(contains("TRUNCATE"));
        verify(jdbcTemplate, atLeastOnce()).execute(contains("'UNIT'"));
        verify(jdbcTemplate, atLeastOnce()).execute(contains("'COMPOSITION'"));
        verify(jdbcTemplate, never()).execute(contains("CREATE TYPE oop_principle_tag"));
        verify(cache).invalidateAll();
        verify(jdbcTemplate, never()).execute(contains("ADD COLUMN receiver_constructor_id"));
    }

    @Test
    void newUnitCompositionSchema_isIdempotentAndSkipsTruncate() throws Exception {
        stubSchema(false);

        invokeEnsureSchema(null);

        verify(jdbcTemplate, never()).execute(contains("TRUNCATE"));
        verify(jdbcTemplate, never()).execute(contains("CREATE TYPE oop_principle_tag"));
        verify(jdbcTemplate, never()).execute(contains("ADD COLUMN receiver_constructor_id"));
    }

    @Test
    void testcaseType_isOnlyUnitAndComposition() {
        assertEquals(Set.of("UNIT", "COMPOSITION"), enumNames(TestcaseType.class));
    }

    @Test
    void assertionKind_dropsComparisonResult() {
        assertEquals(
                Set.of("RETURN_VALUE", "FIELD_STATE", "STDOUT", "EXCEPTION"),
                enumNames(AssertionKind.class));
    }

    @Test
    void newTestcase_hasNoOopPrincipleTagDefault_andInvocationOrderZero() {
        assertFalse(hasField(Testcase.class, "oopPrincipleTag"));
        assertFalse(hasField(Testcase.class, "comparisonMethod"));
        assertFalse(hasField(Testcase.class, "weight"));
        assertFalse(TestcaseInstance.class.isAnnotationPresent(Entity.class));
        assertEquals(0, new TestcaseInvocation().getOrderIndex());
    }

    @Test
    void challenge_keepsTestcaseWeight_andScoringMigratorUntouched() throws Exception {
        Field field = Challenge.class.getDeclaredField("testcaseWeight");
        Column column = field.getAnnotation(Column.class);
        assertNotNull(column);
        assertEquals("testcase_weight", column.name());
        assertNotNull(ScoringWeightSchemaMigrator.class.getDeclaredMethod("ensureTestcaseWeight"));
    }

    private void stubSchema(boolean oldSchema) {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    if (sql.contains("pg_enum") || sql.contains("pg_type")) {
                        String typeName = invocation.getArgument(2);
                        String label = invocation.getArgument(3);
                        return oldSchema
                                ? oldEnumLabel(typeName, label)
                                : newEnumLabel(typeName, label);
                    }
                    if (sql.contains("information_schema.columns")) {
                        String table = invocation.getArgument(2);
                        String column = invocation.getArgument(3);
                        if ("testcase".equals(table) && LEFTOVER_TESTCASE_COLUMNS.contains(column)) {
                            return oldSchema;
                        }
                        return RECEIVER_COLUMNS.contains(column) || !"testcase".equals(table);
                    }
                    String constraintName = invocation.getArgument(2);
                    String constraintType = invocation.getArgument(3);
                    if ("FOREIGN KEY".equals(constraintType)
                            && "testcase_invocation_receiver_constructor_id_fkey".equals(constraintName)) {
                        return true;
                    }
                    return "UNIQUE".equals(constraintType)
                            && "testcase_invocation_testcase_id_order_index_key".equals(constraintName);
                });
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any()))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    if (sql.contains("information_schema.tables")) {
                        return oldSchema && "testcase_instance".equals(invocation.getArgument(2));
                    }
                    return false;
                });
    }

    private static Boolean oldEnumLabel(String typeName, String label) {
        if ("testcase_type".equals(typeName)) {
            return "SINGLE_INVOCATION".equals(label) || "COMPARISON".equals(label);
        }
        if ("assertion_kind".equals(typeName)) {
            return "COMPARISON_RESULT".equals(label);
        }
        return false;
    }

    private static Boolean newEnumLabel(String typeName, String label) {
        if ("testcase_type".equals(typeName)) {
            return "UNIT".equals(label) || "COMPOSITION".equals(label);
        }
        if ("assertion_kind".equals(typeName)) {
            return Set.of("RETURN_VALUE", "FIELD_STATE", "STDOUT", "EXCEPTION").contains(label);
        }
        return false;
    }

    private void invokeEnsureSchema(LabRubricCache cache) throws Exception {
        TestcaseSchemaMigrator migrator;
        if (cache == null) {
            migrator = new TestcaseSchemaMigrator(jdbcTemplate);
        } else {
            Constructor<TestcaseSchemaMigrator> ctor = TestcaseSchemaMigrator.class
                    .getDeclaredConstructor(JdbcTemplate.class, LabRubricCache.class);
            migrator = ctor.newInstance(jdbcTemplate, cache);
        }
        Method method = TestcaseSchemaMigrator.class.getDeclaredMethod("ensureSchema");
        method.setAccessible(true);
        method.invoke(migrator);
    }

    private static Set<String> enumNames(Class<? extends Enum<?>> type) {
        return Arrays.stream(type.getEnumConstants()).map(Enum::name).collect(Collectors.toSet());
    }

    private static boolean hasField(Class<?> type, String name) {
        return Arrays.stream(type.getDeclaredFields()).anyMatch(field -> name.equals(field.getName()));
    }

    private static boolean dropsTestcaseColumn(String sql, String column) {
        return sql.matches("(?is).*ALTER TABLE\\s+testcase\\b.*DROP COLUMN[^;]*\\b" + column + "\\b.*");
    }

    private static boolean dropsChallengeTestcaseWeight(String sql) {
        return sql.matches("(?is).*ALTER TABLE\\s+challenge\\b.*testcase_weight.*")
                || sql.matches("(?is).*DROP COLUMN[^;]*challenge\\.testcase_weight.*");
    }

    private static String assertionKindCreateBlock(String sql) {
        int returnValue = sql.indexOf("'RETURN_VALUE'");
        if (returnValue < 0) {
            return "";
        }
        int create = sql.toUpperCase(Locale.ROOT).lastIndexOf("CREATE TYPE", returnValue);
        if (create < 0) {
            return "";
        }
        int end = sql.indexOf(";", returnValue);
        return end < 0 ? sql.substring(create) : sql.substring(create, end);
    }

    private static String readOperatorSql() throws IOException {
        String resource = "/sql/" + OPERATOR_SQL_NAME;
        try (InputStream in = TestcaseSchemaMigratorTest.class.getResourceAsStream(resource)) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        for (Path candidate : new Path[] {
                Path.of("src", "test", "resources", "sql", OPERATOR_SQL_NAME),
                Path.of("..", "docs", "sql", OPERATOR_SQL_NAME),
                Path.of("docs", "sql", OPERATOR_SQL_NAME)
        }) {
            if (Files.exists(candidate)) {
                return Files.readString(candidate);
            }
        }
        throw new IllegalStateException("Missing operator SQL fixture: " + resource);
    }
}
