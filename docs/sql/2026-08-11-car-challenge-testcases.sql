-- Operational testcases for Car challenge (Programming Exercise 5 — Car class).
-- Challenge: f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc
--
-- Run the diagnostic block first to confirm rubric IDs exist.
-- Requires operational testcase schema (docs/sql/2026-08-11-operational-testcase-grading.sql)
-- and is_hidden column (docs/sql/2026-08-11-operation-test-io-card.sql).
--
-- Platform note: INSTANCE method invocations (accelerate, brake, getters) currently
-- instantiate Car via a no-arg constructor. This assignment only defines
-- Car(int yearModel, String make), so METHOD rows below are commented out until
-- the runner can seed the receiver from constructor params (or Car gains a no-arg ctor).
-- Constructor + FIELD_STATE rows work today.

-- ---------------------------------------------------------------------------
-- 1. Diagnostic — verify Car rubric members
-- ---------------------------------------------------------------------------
/*
SELECT ce.id AS class_id, ce.name AS class_name
FROM class_entity ce
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc';

SELECT c.id AS constructor_id, c.name,
       (SELECT json_agg(json_build_object('name', p.name, 'type', p.data_type) ORDER BY p.order_index)
        FROM parameter p WHERE p.constructor_id = c.id) AS params
FROM constructor c
JOIN class_entity ce ON ce.id = c.class_id
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc' AND ce.name = 'Car';

SELECT f.id AS field_id, f.name, fd.data_type
FROM field f
JOIN field_declaration fd ON fd.id = f.field_declaration_id
JOIN class_entity ce ON ce.id = f.class_id
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc' AND ce.name = 'Car'
ORDER BY f.name;

SELECT m.id AS method_id, m.name,
       (SELECT json_agg(json_build_object('name', p.name, 'type', p.data_type) ORDER BY p.order_index)
        FROM parameter p WHERE p.method_id = m.id) AS params
FROM method m
JOIN class_entity ce ON ce.id = m.class_id
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc' AND ce.name = 'Car'
ORDER BY m.name;
*/

-- ---------------------------------------------------------------------------
-- 2. Optional reset — uncomment to replace existing testcase rows for this challenge
-- ---------------------------------------------------------------------------
/*
DELETE FROM testcase
WHERE challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc';
*/

-- ---------------------------------------------------------------------------
-- 3. Example testcases (is_hidden = false) — constructor / field state
-- ---------------------------------------------------------------------------

-- TC1: Constructor initializes speed to 0
INSERT INTO testcase (id, challenge_id, testcase_type, name, weight, order_index, is_hidden)
VALUES (
    'a0defc50-3a1b-4ba9-8801-000000000001',
    'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc',
    'SINGLE_INVOCATION',
    'Initial speed is zero',
    1, 0, false
);

INSERT INTO testcase_invocation (testcase_id, invocation_kind, constructor_id, params)
SELECT
    'a0defc50-3a1b-4ba9-8801-000000000001'::uuid,
    'CONSTRUCTOR',
    c.id,
    '[2020, "Toyota"]'::jsonb
FROM constructor c
JOIN class_entity ce ON ce.id = c.class_id
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc'
  AND ce.name = 'Car'
  AND c.name = 'Car'
  AND (SELECT COUNT(*) FROM parameter p WHERE p.constructor_id = c.id) = 2
LIMIT 1;

INSERT INTO testcase_assertion (testcase_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
SELECT
    'a0defc50-3a1b-4ba9-8801-000000000001'::uuid,
    'FIELD_STATE',
    f.id,
    '0'::jsonb,
    'EXACT',
    0
FROM field f
JOIN class_entity ce ON ce.id = f.class_id
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc'
  AND ce.name = 'Car'
  AND f.name = 'speed';

-- TC2: Constructor stores yearModel
INSERT INTO testcase (id, challenge_id, testcase_type, name, weight, order_index, is_hidden)
VALUES (
    'a0defc50-3a1b-4ba9-8801-000000000002',
    'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc',
    'SINGLE_INVOCATION',
    'Constructor stores yearModel',
    1, 1, false
);

INSERT INTO testcase_invocation (testcase_id, invocation_kind, constructor_id, params)
SELECT
    'a0defc50-3a1b-4ba9-8801-000000000002'::uuid,
    'CONSTRUCTOR',
    c.id,
    '[2020, "Toyota"]'::jsonb
FROM constructor c
JOIN class_entity ce ON ce.id = c.class_id
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc'
  AND ce.name = 'Car'
  AND c.name = 'Car'
  AND (SELECT COUNT(*) FROM parameter p WHERE p.constructor_id = c.id) = 2
LIMIT 1;

INSERT INTO testcase_assertion (testcase_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
SELECT
    'a0defc50-3a1b-4ba9-8801-000000000002'::uuid,
    'FIELD_STATE',
    f.id,
    '2020'::jsonb,
    'EXACT',
    0
FROM field f
JOIN class_entity ce ON ce.id = f.class_id
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc'
  AND ce.name = 'Car'
  AND f.name = 'yearModel';

-- TC3: Constructor stores make
INSERT INTO testcase (id, challenge_id, testcase_type, name, weight, order_index, is_hidden)
VALUES (
    'a0defc50-3a1b-4ba9-8801-000000000003',
    'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc',
    'SINGLE_INVOCATION',
    'Constructor stores make',
    1, 2, false
);

INSERT INTO testcase_invocation (testcase_id, invocation_kind, constructor_id, params)
SELECT
    'a0defc50-3a1b-4ba9-8801-000000000003'::uuid,
    'CONSTRUCTOR',
    c.id,
    '[2020, "Toyota"]'::jsonb
FROM constructor c
JOIN class_entity ce ON ce.id = c.class_id
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc'
  AND ce.name = 'Car'
  AND c.name = 'Car'
  AND (SELECT COUNT(*) FROM parameter p WHERE p.constructor_id = c.id) = 2
LIMIT 1;

INSERT INTO testcase_assertion (testcase_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
SELECT
    'a0defc50-3a1b-4ba9-8801-000000000003'::uuid,
    'FIELD_STATE',
    f.id,
    '"Toyota"'::jsonb,
    'EXACT',
    0
FROM field f
JOIN class_entity ce ON ce.id = f.class_id
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc'
  AND ce.name = 'Car'
  AND f.name = 'make';

-- ---------------------------------------------------------------------------
-- 4. Hidden testcases (is_hidden = true) — alternate constructor values
-- ---------------------------------------------------------------------------

-- TC4: Hidden — speed zero with different args
INSERT INTO testcase (id, challenge_id, testcase_type, name, weight, order_index, is_hidden)
VALUES (
    'a0defc50-3a1b-4ba9-8801-000000000004',
    'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc',
    'SINGLE_INVOCATION',
    'Speed zero after construction (2015 Honda)',
    1, 3, true
);

INSERT INTO testcase_invocation (testcase_id, invocation_kind, constructor_id, params)
SELECT
    'a0defc50-3a1b-4ba9-8801-000000000004'::uuid,
    'CONSTRUCTOR',
    c.id,
    '[2015, "Honda"]'::jsonb
FROM constructor c
JOIN class_entity ce ON ce.id = c.class_id
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc'
  AND ce.name = 'Car'
  AND c.name = 'Car'
  AND (SELECT COUNT(*) FROM parameter p WHERE p.constructor_id = c.id) = 2
LIMIT 1;

INSERT INTO testcase_assertion (testcase_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
SELECT
    'a0defc50-3a1b-4ba9-8801-000000000004'::uuid,
    'FIELD_STATE',
    f.id,
    '0'::jsonb,
    'EXACT',
    0
FROM field f
JOIN class_entity ce ON ce.id = f.class_id
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc'
  AND ce.name = 'Car'
  AND f.name = 'speed';

-- TC5: Hidden — yearModel with alternate args
INSERT INTO testcase (id, challenge_id, testcase_type, name, weight, order_index, is_hidden)
VALUES (
    'a0defc50-3a1b-4ba9-8801-000000000005',
    'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc',
    'SINGLE_INVOCATION',
    'yearModel stored (2015 Honda)',
    1, 4, true
);

INSERT INTO testcase_invocation (testcase_id, invocation_kind, constructor_id, params)
SELECT
    'a0defc50-3a1b-4ba9-8801-000000000005'::uuid,
    'CONSTRUCTOR',
    c.id,
    '[2015, "Honda"]'::jsonb
FROM constructor c
JOIN class_entity ce ON ce.id = c.class_id
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc'
  AND ce.name = 'Car'
  AND c.name = 'Car'
  AND (SELECT COUNT(*) FROM parameter p WHERE p.constructor_id = c.id) = 2
LIMIT 1;

INSERT INTO testcase_assertion (testcase_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
SELECT
    'a0defc50-3a1b-4ba9-8801-000000000005'::uuid,
    'FIELD_STATE',
    f.id,
    '2015'::jsonb,
    'EXACT',
    0
FROM field f
JOIN class_entity ce ON ce.id = f.class_id
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc'
  AND ce.name = 'Car'
  AND f.name = 'yearModel';

-- TC6: Hidden — make with alternate args
INSERT INTO testcase (id, challenge_id, testcase_type, name, weight, order_index, is_hidden)
VALUES (
    'a0defc50-3a1b-4ba9-8801-000000000006',
    'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc',
    'SINGLE_INVOCATION',
    'make stored (2015 Honda)',
    1, 5, true
);

INSERT INTO testcase_invocation (testcase_id, invocation_kind, constructor_id, params)
SELECT
    'a0defc50-3a1b-4ba9-8801-000000000006'::uuid,
    'CONSTRUCTOR',
    c.id,
    '[2015, "Honda"]'::jsonb
FROM constructor c
JOIN class_entity ce ON ce.id = c.class_id
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc'
  AND ce.name = 'Car'
  AND c.name = 'Car'
  AND (SELECT COUNT(*) FROM parameter p WHERE p.constructor_id = c.id) = 2
LIMIT 1;

INSERT INTO testcase_assertion (testcase_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
SELECT
    'a0defc50-3a1b-4ba9-8801-000000000006'::uuid,
    'FIELD_STATE',
    f.id,
    '"Honda"'::jsonb,
    'EXACT',
    0
FROM field f
JOIN class_entity ce ON ce.id = f.class_id
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc'
  AND ce.name = 'Car'
  AND f.name = 'make';

-- ---------------------------------------------------------------------------
-- 5. METHOD testcases (accelerate / brake / getters) — COMMENTED OUT
--
-- Uncomment after InvocationRunner can construct the receiver with
-- Car(int, String), or after adding a no-arg constructor to the rubric.
-- Each row invokes one method on a default-constructed instance.
-- ---------------------------------------------------------------------------

/*
-- accelerate once: speed 0 → 5
INSERT INTO testcase (id, challenge_id, testcase_type, name, weight, order_index, is_hidden)
VALUES (
    'a0defc50-3a1b-4ba9-8801-000000000101',
    'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc',
    'SINGLE_INVOCATION',
    'accelerate increases speed by 5',
    1, 6, false
);

INSERT INTO testcase_invocation (testcase_id, invocation_kind, method_id, params)
SELECT
    'a0defc50-3a1b-4ba9-8801-000000000101'::uuid,
    'METHOD',
    m.id,
    '[]'::jsonb
FROM method m
JOIN class_entity ce ON ce.id = m.class_id
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc'
  AND ce.name = 'Car'
  AND m.name = 'accelerate'
  AND NOT EXISTS (SELECT 1 FROM parameter p WHERE p.method_id = m.id)
LIMIT 1;

INSERT INTO testcase_assertion (testcase_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
SELECT
    'a0defc50-3a1b-4ba9-8801-000000000101'::uuid,
    'FIELD_STATE',
    f.id,
    '5'::jsonb,
    'EXACT',
    0
FROM field f
JOIN class_entity ce ON ce.id = f.class_id
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc'
  AND ce.name = 'Car'
  AND f.name = 'speed';

-- brake once from speed 5 → 0 (requires runner to call accelerate first, or seed speed=5)
INSERT INTO testcase (id, challenge_id, testcase_type, name, weight, order_index, is_hidden)
VALUES (
    'a0defc50-3a1b-4ba9-8801-000000000102',
    'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc',
    'SINGLE_INVOCATION',
    'brake decreases speed by 5',
    1, 7, true
);

INSERT INTO testcase_invocation (testcase_id, invocation_kind, method_id, params)
SELECT
    'a0defc50-3a1b-4ba9-8801-000000000102'::uuid,
    'METHOD',
    m.id,
    '[]'::jsonb
FROM method m
JOIN class_entity ce ON ce.id = m.class_id
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc'
  AND ce.name = 'Car'
  AND m.name = 'brake'
  AND NOT EXISTS (SELECT 1 FROM parameter p WHERE p.method_id = m.id)
LIMIT 1;

INSERT INTO testcase_assertion (testcase_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
SELECT
    'a0defc50-3a1b-4ba9-8801-000000000102'::uuid,
    'FIELD_STATE',
    f.id,
    '0'::jsonb,
    'EXACT',
    0
FROM field f
JOIN class_entity ce ON ce.id = f.class_id
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc'
  AND ce.name = 'Car'
  AND f.name = 'speed';

-- getSpeed returns 0 on a new Car
INSERT INTO testcase (id, challenge_id, testcase_type, name, weight, order_index, is_hidden)
VALUES (
    'a0defc50-3a1b-4ba9-8801-000000000103',
    'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc',
    'SINGLE_INVOCATION',
    'getSpeed returns initial speed',
    1, 8, true
);

INSERT INTO testcase_invocation (testcase_id, invocation_kind, method_id, params)
SELECT
    'a0defc50-3a1b-4ba9-8801-000000000103'::uuid,
    'METHOD',
    m.id,
    '[]'::jsonb
FROM method m
JOIN class_entity ce ON ce.id = m.class_id
WHERE ce.challenge_id = 'f0defc50-3a1b-4ba9-88a9-f3fa60e7a5bc'
  AND ce.name = 'Car'
  AND m.name = 'getSpeed'
  AND NOT EXISTS (SELECT 1 FROM parameter p WHERE p.method_id = m.id)
LIMIT 1;

INSERT INTO testcase_assertion (testcase_id, assertion_kind, expected_value, comparison_mode, order_index)
VALUES (
    'a0defc50-3a1b-4ba9-8801-000000000103',
    'RETURN_VALUE',
    '0'::jsonb,
    'EXACT',
    0
);
*/
