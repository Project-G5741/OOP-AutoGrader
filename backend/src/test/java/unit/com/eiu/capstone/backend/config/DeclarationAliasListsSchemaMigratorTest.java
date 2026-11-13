package unit.com.eiu.capstone.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.eiu.capstone.backend.config.DeclarationAliasListsSchemaMigrator;
import com.eiu.capstone.backend.model.FieldDeclarationNameAlias;
import com.eiu.capstone.backend.model.FieldDeclarationTypeAlias;
import com.eiu.capstone.backend.model.MethodDeclarationNameAlias;
import com.eiu.capstone.backend.model.MethodDeclarationReturnTypeAlias;
import com.eiu.capstone.backend.model.ParameterNameAlias;
import com.eiu.capstone.backend.model.ParameterTypeAlias;

import jakarta.persistence.Table;

class DeclarationAliasListsSchemaMigratorTest {

    private static final String OPERATOR_SQL = "2026-10-31-declaration-alias-lists.sql";

    private static final String[] ALIAS_TABLES = {
            "field_declaration_name_alias",
            "field_declaration_type_alias",
            "method_declaration_name_alias",
            "method_declaration_return_type_alias",
            "parameter_name_alias",
            "parameter_type_alias"
    };

    @Test
    void operatorSql_createsAllAliasTablesAndBackfillsPrimaryAsOrderZero() throws Exception {
        String sql = readOperatorSql();
        String upper = sql.toUpperCase(Locale.ROOT);

        for (String table : ALIAS_TABLES) {
            assertTrue(upper.contains("CREATE TABLE IF NOT EXISTS " + table.toUpperCase(Locale.ROOT)),
                    "missing CREATE for " + table);
        }
        assertTrue(upper.contains("ORDER_INDEX"));
        assertTrue(upper.contains("UNIQUE"));
        assertTrue(upper.contains("INSERT INTO FIELD_DECLARATION_NAME_ALIAS"));
        assertTrue(upper.contains("NOT EXISTS"));
        assertTrue(sql.contains("fd.name"));
        assertTrue(sql.contains("fd.data_type"));
        assertTrue(sql.contains("md.return_type"));
        assertTrue(sql.contains("p.data_type"));
    }

    @Test
    void ensureAliasLists_createsTablesThenBackfills_idempotentSqlUsesNotExists() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        DeclarationAliasListsSchemaMigrator migrator = new DeclarationAliasListsSchemaMigrator(jdbc);

        migrator.ensureAliasLists();
        migrator.ensureAliasLists();

        for (String table : ALIAS_TABLES) {
            verify(jdbc, times(2)).execute(contains("CREATE TABLE IF NOT EXISTS " + table));
        }
        verify(jdbc, atLeast(6)).update(contains("NOT EXISTS"));
        verify(jdbc, atLeast(2)).update(contains("field_declaration_name_alias"));
        verify(jdbc, atLeast(2)).update(contains("parameter_type_alias"));
    }

    @Test
    void entityTableNames_matchOperatorSql() {
        assertEquals("field_declaration_name_alias",
                FieldDeclarationNameAlias.class.getAnnotation(Table.class).name());
        assertEquals("field_declaration_type_alias",
                FieldDeclarationTypeAlias.class.getAnnotation(Table.class).name());
        assertEquals("method_declaration_name_alias",
                MethodDeclarationNameAlias.class.getAnnotation(Table.class).name());
        assertEquals("method_declaration_return_type_alias",
                MethodDeclarationReturnTypeAlias.class.getAnnotation(Table.class).name());
        assertEquals("parameter_name_alias",
                ParameterNameAlias.class.getAnnotation(Table.class).name());
        assertEquals("parameter_type_alias",
                ParameterTypeAlias.class.getAnnotation(Table.class).name());
    }

    private static String readOperatorSql() throws Exception {
        Path cwd = Path.of(System.getProperty("user.dir"));
        Path path = cwd.resolve(Path.of("..", "docs", "sql", OPERATOR_SQL)).normalize();
        if (!Files.isRegularFile(path)) {
            path = cwd.resolve(Path.of("docs", "sql", OPERATOR_SQL)).normalize();
        }
        assertTrue(Files.isRegularFile(path), "operator SQL not found: " + path);
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
