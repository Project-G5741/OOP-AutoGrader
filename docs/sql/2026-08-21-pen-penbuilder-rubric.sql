-- Pen / PenBuilder rubric fix for nested static class grading.
-- Operator-run after 2026-08-21-class-entity-outer-class.sql and 2026-08-21-class-entity-is-static.sql.
-- Resolves lab/challenge/class IDs by name — adjust filters if your lab naming differs.

-- 1) Locate Pen outer class (challenge 2 of lab containing "lab 1" case-insensitive)
WITH pen_ctx AS (
    SELECT ce.id AS pen_class_id, ce.challenge_id
    FROM class_entity ce
    JOIN challenge ch ON ch.id = ce.challenge_id
    JOIN lab l ON l.id = ch.lab_id
    WHERE lower(ce.name) = 'pen'
      AND ch.challenge_number = 2
      AND lower(l.name) LIKE '%lab%1%'
    LIMIT 1
)
-- 2) Insert PenBuilder if missing
INSERT INTO class_entity (id, name, challenge_id, scope, declaring_type, is_abstract, is_static, weight, outer_class_id)
SELECT
    gen_random_uuid(),
    'PenBuilder',
    pen_ctx.challenge_id,
    (SELECT id FROM master_data WHERE upper(name) = 'PUBLIC' AND category = 'SCOPE' LIMIT 1),
    (SELECT id FROM master_data WHERE upper(name) = 'CLASS' AND category = 'DECLARING_TYPE' LIMIT 1),
    false,
    true,
    1,
    pen_ctx.pen_class_id
FROM pen_ctx
WHERE NOT EXISTS (
    SELECT 1 FROM class_entity nested
    WHERE nested.challenge_id = pen_ctx.challenge_id
      AND lower(nested.name) = 'penbuilder'
);

-- 3) Ensure existing PenBuilder rows are marked static nested
UPDATE class_entity nested
SET is_static = true
FROM class_entity pen, challenge ch, lab l
WHERE nested.outer_class_id = pen.id
  AND lower(nested.name) = 'penbuilder'
  AND lower(pen.name) = 'pen'
  AND pen.challenge_id = ch.id
  AND ch.lab_id = l.id
  AND ch.challenge_number = 2
  AND lower(l.name) LIKE '%lab%1%';

-- NOTE: Add field/method/constructor rows for PenBuilder in a follow-up script
-- after verifying master_data IDs and lecturer solution members in your environment.
-- Members should mirror the Pen.java Builder pattern (setBrand, setModel, setPrice, build, private ctor).
