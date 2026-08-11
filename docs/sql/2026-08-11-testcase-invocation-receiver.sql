-- Optional receiver for METHOD invocations: construct the instance before calling the method.
-- Example: new Car(2020, "Toyota") then accelerate().

ALTER TABLE testcase_invocation
    ADD COLUMN IF NOT EXISTS receiver_constructor_id UUID;

ALTER TABLE testcase_invocation DROP CONSTRAINT IF EXISTS testcase_invocation_receiver_constructor_id_fkey;
ALTER TABLE testcase_invocation ADD CONSTRAINT testcase_invocation_receiver_constructor_id_fkey
    FOREIGN KEY (receiver_constructor_id) REFERENCES constructor(id) ON DELETE CASCADE;

ALTER TABLE testcase_invocation
    ADD COLUMN IF NOT EXISTS receiver_params JSONB NOT NULL DEFAULT '[]';

ALTER TABLE testcase_invocation ALTER COLUMN receiver_params DROP DEFAULT;
