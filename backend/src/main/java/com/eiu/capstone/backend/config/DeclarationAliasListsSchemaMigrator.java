package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Ensures declaration alias child tables exist on older PostgreSQL databases and
 * backfills order_index 0 from primary columns. Desktop practice creates the same
 * tables through Hibernate {@code ddl-auto: update}; load paths treat a missing
 * child row as a one-element list from the primary column.
 */
@Component
@Profile("!desktop")
public class DeclarationAliasListsSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public DeclarationAliasListsSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureAliasLists() {
        createTables();
        backfill();
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS field_declaration_name_alias (
                    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    field_declaration_id UUID NOT NULL REFERENCES field_declaration(id) ON DELETE CASCADE,
                    value                TEXT NOT NULL,
                    order_index          INTEGER NOT NULL,
                    UNIQUE (field_declaration_id, order_index)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS field_declaration_type_alias (
                    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    field_declaration_id UUID NOT NULL REFERENCES field_declaration(id) ON DELETE CASCADE,
                    value                TEXT,
                    order_index          INTEGER NOT NULL,
                    UNIQUE (field_declaration_id, order_index)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS method_declaration_name_alias (
                    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    method_declaration_id UUID NOT NULL REFERENCES method_declaration(id) ON DELETE CASCADE,
                    value                 TEXT NOT NULL,
                    order_index           INTEGER NOT NULL,
                    UNIQUE (method_declaration_id, order_index)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS method_declaration_return_type_alias (
                    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    method_declaration_id UUID NOT NULL REFERENCES method_declaration(id) ON DELETE CASCADE,
                    value                 TEXT NOT NULL,
                    order_index           INTEGER NOT NULL,
                    UNIQUE (method_declaration_id, order_index)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS parameter_name_alias (
                    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    parameter_id UUID NOT NULL REFERENCES parameter(id) ON DELETE CASCADE,
                    value        TEXT NOT NULL,
                    order_index  INTEGER NOT NULL,
                    UNIQUE (parameter_id, order_index)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS parameter_type_alias (
                    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    parameter_id UUID NOT NULL REFERENCES parameter(id) ON DELETE CASCADE,
                    value        TEXT NOT NULL,
                    order_index  INTEGER NOT NULL,
                    UNIQUE (parameter_id, order_index)
                )
                """);
    }

    private void backfill() {
        jdbcTemplate.update("""
                INSERT INTO field_declaration_name_alias (id, field_declaration_id, value, order_index)
                SELECT gen_random_uuid(), fd.id, fd.name, 0
                FROM field_declaration fd
                WHERE NOT EXISTS (
                    SELECT 1 FROM field_declaration_name_alias a WHERE a.field_declaration_id = fd.id
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO field_declaration_type_alias (id, field_declaration_id, value, order_index)
                SELECT gen_random_uuid(), fd.id, fd.data_type, 0
                FROM field_declaration fd
                WHERE NOT EXISTS (
                    SELECT 1 FROM field_declaration_type_alias a WHERE a.field_declaration_id = fd.id
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO method_declaration_name_alias (id, method_declaration_id, value, order_index)
                SELECT gen_random_uuid(), md.id, md.name, 0
                FROM method_declaration md
                WHERE NOT EXISTS (
                    SELECT 1 FROM method_declaration_name_alias a WHERE a.method_declaration_id = md.id
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO method_declaration_return_type_alias (id, method_declaration_id, value, order_index)
                SELECT gen_random_uuid(), md.id, md.return_type, 0
                FROM method_declaration md
                WHERE NOT EXISTS (
                    SELECT 1 FROM method_declaration_return_type_alias a WHERE a.method_declaration_id = md.id
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO parameter_name_alias (id, parameter_id, value, order_index)
                SELECT gen_random_uuid(), p.id, p.name, 0
                FROM parameter p
                WHERE NOT EXISTS (
                    SELECT 1 FROM parameter_name_alias a WHERE a.parameter_id = p.id
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO parameter_type_alias (id, parameter_id, value, order_index)
                SELECT gen_random_uuid(), p.id, p.data_type, 0
                FROM parameter p
                WHERE NOT EXISTS (
                    SELECT 1 FROM parameter_type_alias a WHERE a.parameter_id = p.id
                )
                """);
    }
}
