package com.eiu.capstone.backend.config;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Enum constants are stored without a type or scope. Older databases marked both columns NOT NULL.
 */
@Component
public class FieldDeclarationNullabilitySchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public FieldDeclarationNullabilitySchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void allowNullTypeAndScope() {
        dropNotNull("data_type");
        dropNotNull("scope");
    }

    private void dropNotNull(String column) {
        if (!columnExists(column) || columnNullable(column)) {
            return;
        }
        jdbcTemplate.execute(
                "ALTER TABLE field_declaration ALTER COLUMN " + column + " DROP NOT NULL");
    }

    private boolean columnExists(String column) {
        List<String> names = jdbcTemplate.query("""
                SELECT column_name FROM information_schema.columns
                WHERE lower(table_name) = 'field_declaration'
                  AND lower(column_name) = ?
                """, (rs, rowNum) -> rs.getString(1), column);
        return !names.isEmpty();
    }

    private boolean columnNullable(String column) {
        List<String> flags = jdbcTemplate.query("""
                SELECT is_nullable FROM information_schema.columns
                WHERE lower(table_name) = 'field_declaration'
                  AND lower(column_name) = ?
                """, (rs, rowNum) -> rs.getString(1), column);
        return flags.stream().anyMatch(flag -> "YES".equalsIgnoreCase(flag));
    }
}
