-- Allow a Composition constructor step to name a class instead of a rubric constructor.
-- dispatch_class_id is the class. The worker builds it like a Unit hidden receiver
-- (no-arg constructor when bytecode has one, otherwise the shortest constructor
-- with default arguments). Startup migrator applies the same check.

ALTER TABLE testcase_invocation DROP CONSTRAINT IF EXISTS testcase_invocation_kind_check;

ALTER TABLE testcase_invocation ADD CONSTRAINT testcase_invocation_kind_check CHECK (
    (invocation_kind = 'CONSTRUCTOR' AND method_id IS NULL AND (
        constructor_id IS NOT NULL
        OR (constructor_id IS NULL AND dispatch_class_id IS NOT NULL)
    ))
    OR (invocation_kind = 'METHOD' AND method_id IS NOT NULL AND constructor_id IS NULL)
);
