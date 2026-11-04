-- One-time: stop Lab 4 tea compositions from referencing the inserted no-arg
-- constructors, then delete those rows so Structure save can remove them.
-- Requires the Composition default-constructor check (dispatch_class_id, null constructor_id).

BEGIN;

ALTER TABLE testcase_invocation DROP CONSTRAINT IF EXISTS testcase_invocation_kind_check;

ALTER TABLE testcase_invocation ADD CONSTRAINT testcase_invocation_kind_check CHECK (
    (invocation_kind = 'CONSTRUCTOR' AND method_id IS NULL AND (
        constructor_id IS NOT NULL
        OR (constructor_id IS NULL AND dispatch_class_id IS NOT NULL)
    ))
    OR (invocation_kind = 'METHOD' AND method_id IS NOT NULL AND constructor_id IS NULL)
);

UPDATE testcase_invocation AS step
SET constructor_id = NULL,
    dispatch_class_id = ctor.class_id,
    params = '[]'::jsonb
FROM constructor AS ctor
WHERE step.constructor_id = ctor.id
  AND ctor.id IN (
      'a4e10001-0001-4000-8000-000000000002',
      'a4e10001-0001-4000-8000-000000000004',
      'a4e10001-0001-4000-8000-000000000006',
      'a4e10001-0001-4000-8000-000000000008'
  );

UPDATE testcase_invocation AS step
SET receiver_constructor_id = NULL
WHERE step.receiver_constructor_id IN (
    'a4e10001-0001-4000-8000-000000000002',
    'a4e10001-0001-4000-8000-000000000004',
    'a4e10001-0001-4000-8000-000000000006',
    'a4e10001-0001-4000-8000-000000000008'
);

DELETE FROM parameter
WHERE constructor_id IN (
    'a4e10001-0001-4000-8000-000000000002',
    'a4e10001-0001-4000-8000-000000000004',
    'a4e10001-0001-4000-8000-000000000006',
    'a4e10001-0001-4000-8000-000000000008'
);

DELETE FROM submission_constructor_result
WHERE constructor_id IN (
    'a4e10001-0001-4000-8000-000000000002',
    'a4e10001-0001-4000-8000-000000000004',
    'a4e10001-0001-4000-8000-000000000006',
    'a4e10001-0001-4000-8000-000000000008'
);

DELETE FROM constructor
WHERE id IN (
    'a4e10001-0001-4000-8000-000000000002',
    'a4e10001-0001-4000-8000-000000000004',
    'a4e10001-0001-4000-8000-000000000006',
    'a4e10001-0001-4000-8000-000000000008'
);

DELETE FROM constructor_declaration
WHERE id IN (
    'a4e10001-0001-4000-8000-000000000001',
    'a4e10001-0001-4000-8000-000000000003',
    'a4e10001-0001-4000-8000-000000000005',
    'a4e10001-0001-4000-8000-000000000007'
);

COMMIT;
