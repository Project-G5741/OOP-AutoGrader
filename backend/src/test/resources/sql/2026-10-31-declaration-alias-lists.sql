-- Operator-run: ordered lecturer-authored name/type alias lists for Declaration members.
-- Primary remains on parent columns (field_declaration.name/data_type, method_declaration.name/return_type,
-- parameter.name/data_type). Alias children hold the full ordered list; order_index 0 matches the primary.
-- Startup migrator DeclarationAliasListsSchemaMigrator applies the same CREATE TABLE + backfill on PostgreSQL.

CREATE TABLE IF NOT EXISTS field_declaration_name_alias (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    field_declaration_id UUID NOT NULL REFERENCES field_declaration(id) ON DELETE CASCADE,
    value                TEXT NOT NULL,
    order_index          INTEGER NOT NULL,
    UNIQUE (field_declaration_id, order_index)
);

CREATE TABLE IF NOT EXISTS field_declaration_type_alias (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    field_declaration_id UUID NOT NULL REFERENCES field_declaration(id) ON DELETE CASCADE,
    value                TEXT,
    order_index          INTEGER NOT NULL,
    UNIQUE (field_declaration_id, order_index)
);

CREATE TABLE IF NOT EXISTS method_declaration_name_alias (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    method_declaration_id UUID NOT NULL REFERENCES method_declaration(id) ON DELETE CASCADE,
    value                 TEXT NOT NULL,
    order_index           INTEGER NOT NULL,
    UNIQUE (method_declaration_id, order_index)
);

CREATE TABLE IF NOT EXISTS method_declaration_return_type_alias (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    method_declaration_id UUID NOT NULL REFERENCES method_declaration(id) ON DELETE CASCADE,
    value                 TEXT NOT NULL,
    order_index           INTEGER NOT NULL,
    UNIQUE (method_declaration_id, order_index)
);

CREATE TABLE IF NOT EXISTS parameter_name_alias (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parameter_id UUID NOT NULL REFERENCES parameter(id) ON DELETE CASCADE,
    value        TEXT NOT NULL,
    order_index  INTEGER NOT NULL,
    UNIQUE (parameter_id, order_index)
);

CREATE TABLE IF NOT EXISTS parameter_type_alias (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parameter_id UUID NOT NULL REFERENCES parameter(id) ON DELETE CASCADE,
    value        TEXT NOT NULL,
    order_index  INTEGER NOT NULL,
    UNIQUE (parameter_id, order_index)
);

-- Backfill: one-element lists from primary columns when no alias children exist yet.
INSERT INTO field_declaration_name_alias (id, field_declaration_id, value, order_index)
SELECT gen_random_uuid(), fd.id, fd.name, 0
FROM field_declaration fd
WHERE NOT EXISTS (
    SELECT 1 FROM field_declaration_name_alias a WHERE a.field_declaration_id = fd.id
);

INSERT INTO field_declaration_type_alias (id, field_declaration_id, value, order_index)
SELECT gen_random_uuid(), fd.id, fd.data_type, 0
FROM field_declaration fd
WHERE NOT EXISTS (
    SELECT 1 FROM field_declaration_type_alias a WHERE a.field_declaration_id = fd.id
);

INSERT INTO method_declaration_name_alias (id, method_declaration_id, value, order_index)
SELECT gen_random_uuid(), md.id, md.name, 0
FROM method_declaration md
WHERE NOT EXISTS (
    SELECT 1 FROM method_declaration_name_alias a WHERE a.method_declaration_id = md.id
);

INSERT INTO method_declaration_return_type_alias (id, method_declaration_id, value, order_index)
SELECT gen_random_uuid(), md.id, md.return_type, 0
FROM method_declaration md
WHERE NOT EXISTS (
    SELECT 1 FROM method_declaration_return_type_alias a WHERE a.method_declaration_id = md.id
);

INSERT INTO parameter_name_alias (id, parameter_id, value, order_index)
SELECT gen_random_uuid(), p.id, p.name, 0
FROM parameter p
WHERE NOT EXISTS (
    SELECT 1 FROM parameter_name_alias a WHERE a.parameter_id = p.id
);

INSERT INTO parameter_type_alias (id, parameter_id, value, order_index)
SELECT gen_random_uuid(), p.id, p.data_type, 0
FROM parameter p
WHERE NOT EXISTS (
    SELECT 1 FROM parameter_type_alias a WHERE a.parameter_id = p.id
);
