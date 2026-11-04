-- Lab 4 operational testcases (Supabase SQL editor).
-- Lab: 0c88b14e-6c4e-4de0-9cbf-7f8dd1a92fca
-- Counts: challenge 1 → 4, 2 → 4, 3 → 8, 4 → 10, 5 → 10.
--
-- Re-run replaces operational testcases for this lab only (submission OT rows
-- for those testcases cascade). Concrete teas are constructed with the
-- Composition default constructor (dispatch_class_id, no rubric constructor).
-- Any public no-arg constructors previously inserted for those four classes
-- are removed so Structure can stay without them.
--
-- Each testcase asserts a different method or constructor. A composition may
-- still construct the receiver as setup; that setup is not a separate testcase.
--
-- Grader limits this file respects:
--   * LocalDate arguments and expecteds are ISO-8601 dates (yyyy-MM-dd).
--   * UUID.randomUUID() and Discount.startDate (LocalDate.now()) are not asserted.
--   * Whole double expecteds are written as 18.75 / 756000.0 so they stay in
--     the floating family (an integer JSON number does not match a double).
--
-- Formulas the complex cases require (from the Lab 4 sheet plus the rubric):
--   * Employee number is XXX-L: three digits, a hyphen, one letter A–M.
--   * ShiftSupervisor.calculateTotalSalary() = annualSalary + annualProductionBonus.
--   * TeamLeader.calculateTotalSalary(monthlyHours)
--       = payRate * monthlyHours + monthlyBonus.
--     Attended hours are at least the required hours, so a solution that pays
--     the bonus only after training still matches.
--   * Ballpoint tax is Tax._8 (0.08). Fountain tax is Tax._10 (0.1).
--   * getDiscountPrice() = price * (1 - percent) when discounted, else 0.
--   * getPriceAfterTax() = price * (1 + tax) - getDiscountPrice().
--     125000 at 15% off, ballpoint tax: 135000 - 106250 = 28750.
--   * OrderedItem price = teaPrice * (1 + 0.08) * weight.
--   * Tea.equals compares name and price, not identity.
--   * OrderedItem.equals compares tea only, so different weights still match
--     when the teas are equal. Cart.add rejects null and a tea already ordered.
--
-- getShiftName is not tested: the sheet names day/night shifts but does not
-- define the string that method must return.
--
-- After this runs, restart the API or wait out the rubric cache (about 2 minutes)
-- before dry-run or student upload.

BEGIN;

ALTER TABLE testcase_invocation DROP CONSTRAINT IF EXISTS testcase_invocation_kind_check;
ALTER TABLE testcase_invocation ADD CONSTRAINT testcase_invocation_kind_check CHECK (
    (invocation_kind = 'CONSTRUCTOR' AND method_id IS NULL AND (
        constructor_id IS NOT NULL
        OR (constructor_id IS NULL AND dispatch_class_id IS NOT NULL)
    ))
    OR (invocation_kind = 'METHOD' AND method_id IS NOT NULL AND constructor_id IS NULL)
);

DELETE FROM testcase t
USING challenge c
WHERE t.challenge_id = c.id
  AND c.lab_id = '0c88b14e-6c4e-4de0-9cbf-7f8dd1a92fca';

DO $$
DECLARE
    tc uuid;
    s0 uuid;
    s1 uuid;
    s2 uuid;
    s3 uuid;
    s4 uuid;
    s5 uuid;
    s6 uuid;
    s7 uuid;
    s8 uuid;
    s9 uuid;
BEGIN
    -- ========== Challenge 1 (4) ==========

    -- ProductionWorker constructor stores superclass and subclass fields.
    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '3f902fc3-9844-496a-ae63-95e4a6d27b3e', 'UNIT',
            'ProductionWorker stores Gawr Gura on night shift', 0, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, repeat_count)
    VALUES (gen_random_uuid(), tc, 'CONSTRUCTOR', '67fe95ea-4aac-4f80-8459-2e78ef9d9f39',
            '["Gawr Gura", "478-C", "2024-06-13", 2, 18.75]'::jsonb, '[]'::jsonb, 0, 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
    VALUES
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', '8d5da6e3-99b3-412c-b8fa-6a39dec7c54e', '"Gawr Gura"'::jsonb, 'EXACT', 0),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', '4ae1b75c-0d9a-4bfd-affe-1e0506dc7211', '"478-C"'::jsonb, 'EXACT', 1),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', 'a9f9f009-2e7c-4a85-9bda-011c1c7a20ea', '"2024-06-13"'::jsonb, 'EXACT', 2),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', '7ab4af07-1cae-4633-bdf6-cbac0196368e', '2'::jsonb, 'EXACT', 3),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', '557d3bcc-df8f-45b4-9ca1-cabd326b94f9', '18.75'::jsonb, 'EXACT', 4);

    -- 000-A is valid: leading zeros count, and A is inside A–M.
    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '3f902fc3-9844-496a-ae63-95e4a6d27b3e', 'COMPOSITION',
            'Employee number 000-A is valid', 1, true)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'CONSTRUCTOR', '04239d34-e0f1-4a8f-9fad-a80be8140ef5',
            '["Tokino Sora", "100-A", "2019-04-22"]'::jsonb, '[]'::jsonb, 0, 'employee', 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', 'b4ab7d35-14d3-4974-b010-2874722b56b8',
            '["000-A"]'::jsonb, '[]'::jsonb, 1, 'employee', 1)
    RETURNING id INTO s1;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s1, 'RETURN_VALUE', 'true'::jsonb, 'EXACT', 0);

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '3f902fc3-9844-496a-ae63-95e4a6d27b3e', 'COMPOSITION',
            'setShift moves Gawr Gura to the day shift', 2, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'CONSTRUCTOR', '67fe95ea-4aac-4f80-8459-2e78ef9d9f39',
            '["Gawr Gura", "478-C", "2024-06-13", 2, 18.75]'::jsonb, '[]'::jsonb, 0, 'worker', 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', '976de053-045d-4965-93ed-40dd0ae133ea',
            '[1]'::jsonb, '[]'::jsonb, 1, 'worker', 1)
    RETURNING id INTO s1;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s1, 'FIELD_STATE', '7ab4af07-1cae-4633-bdf6-cbac0196368e', '1'::jsonb, 'EXACT', 0);

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '3f902fc3-9844-496a-ae63-95e4a6d27b3e', 'COMPOSITION',
            'setPayRate updates hourly pay to 22.5', 3, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'CONSTRUCTOR', '67fe95ea-4aac-4f80-8459-2e78ef9d9f39',
            '["Gawr Gura", "478-C", "2024-06-13", 2, 18.75]'::jsonb, '[]'::jsonb, 0, 'worker', 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', '0785c67c-ba62-405b-ac16-afffa80fc70e',
            '[22.5]'::jsonb, '[]'::jsonb, 1, 'worker', 1)
    RETURNING id INTO s1;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s1, 'FIELD_STATE', '557d3bcc-df8f-45b4-9ca1-cabd326b94f9', '22.5'::jsonb, 'EXACT', 0);

    -- ========== Challenge 2 (4) ==========

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '9f4e853e-e025-411e-b0dd-79536f2f1d0c', 'UNIT',
            'ShiftSupervisor stores Houshou Marine salary and bonus', 0, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, repeat_count)
    VALUES (gen_random_uuid(), tc, 'CONSTRUCTOR', '5b4438f2-01fb-4825-85b2-7ee177b39862',
            '["Houshou Marine", "204-B", "2022-08-16", 85000.5, 4200.25]'::jsonb, '[]'::jsonb, 0, 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
    VALUES
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', 'f0f00350-4777-4b9e-9e63-53953654e3ac', '"Houshou Marine"'::jsonb, 'EXACT', 0),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', '00c2dae5-8782-4a46-a35c-45a91f9ad69b', '"204-B"'::jsonb, 'EXACT', 1),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', '21ad126b-10b2-4d79-9925-95321093551d', '"2022-08-16"'::jsonb, 'EXACT', 2),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', '9c838a4c-f056-4e6e-88de-b09726f537ce', '85000.5'::jsonb, 'EXACT', 3),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', 'b39484fa-9659-4aa1-b374-211fbc4a2984', '4200.25'::jsonb, 'EXACT', 4);

    -- 123-N fails because N is outside A–M.
    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '9f4e853e-e025-411e-b0dd-79536f2f1d0c', 'COMPOSITION',
            'Employee number 123-N is rejected', 1, true)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'CONSTRUCTOR', 'edcc6cbe-3697-451d-8529-13781af818be',
            '["Houshou Marine", "204-B", "2022-08-16"]'::jsonb, '[]'::jsonb, 0, 'employee', 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', '695d470a-9ba8-409b-adcc-231bfa6301b5',
            '["123-N"]'::jsonb, '[]'::jsonb, 1, 'employee', 1)
    RETURNING id INTO s1;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s1, 'RETURN_VALUE', 'false'::jsonb, 'EXACT', 0);

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '9f4e853e-e025-411e-b0dd-79536f2f1d0c', 'COMPOSITION',
            'setAnnualSalary replaces the salary with 64000.5', 2, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'CONSTRUCTOR', '5b4438f2-01fb-4825-85b2-7ee177b39862',
            '["Houshou Marine", "204-B", "2022-08-16", 85000.5, 4200.25]'::jsonb, '[]'::jsonb, 0, 'boss', 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', '8f32ee36-7396-4004-b31e-f6e2be23ddff',
            '[64000.5]'::jsonb, '[]'::jsonb, 1, 'boss', 1)
    RETURNING id INTO s1;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s1, 'FIELD_STATE', '9c838a4c-f056-4e6e-88de-b09726f537ce', '64000.5'::jsonb, 'EXACT', 0);

    -- 85000.5 + 4200.25 = 89200.75
    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '9f4e853e-e025-411e-b0dd-79536f2f1d0c', 'COMPOSITION',
            'Total salary adds the production bonus', 3, true)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'CONSTRUCTOR', '5b4438f2-01fb-4825-85b2-7ee177b39862',
            '["Houshou Marine", "204-B", "2022-08-16", 85000.5, 4200.25]'::jsonb, '[]'::jsonb, 0, 'boss', 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', 'd2ab459e-8417-4bb8-9038-61de6eb5ac0e',
            '[]'::jsonb, '[]'::jsonb, 1, 'boss', 1)
    RETURNING id INTO s1;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s1, 'RETURN_VALUE', '89200.75'::jsonb, 'EXACT', 0);

    -- ========== Challenge 3 (8) ==========

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '5a11fc73-f14d-405b-8775-85b32324d273', 'UNIT',
            'TeamLeader stores Shirakami Fubuki pay, bonus, and training', 0, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, repeat_count)
    VALUES (gen_random_uuid(), tc, 'CONSTRUCTOR', '16c7cc8f-6a76-4122-b32b-0c9e823f8799',
            '["Shirakami Fubuki", "315-G", "2021-05-15", 1, 20.5, 500.75, 40.0, 48.0]'::jsonb, '[]'::jsonb, 0, 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
    VALUES
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', 'c09ecdab-d092-424f-9031-f3011551608a', '"Shirakami Fubuki"'::jsonb, 'EXACT', 0),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', 'c168bc05-06e5-449b-9407-49846b5131a5', '"315-G"'::jsonb, 'EXACT', 1),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', '4764419a-35b1-4d5b-a295-8686ba6c5ed6', '"2021-05-15"'::jsonb, 'EXACT', 2),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', 'a3dfb636-bc9b-497c-bd1f-1fd7766babd2', '1'::jsonb, 'EXACT', 3),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', '5480503c-b1ae-40e5-9b8a-c1d4a0bb0ed6', '20.5'::jsonb, 'EXACT', 4),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', '354eec9c-229b-4b60-a19f-3d47e13d427a', '500.75'::jsonb, 'EXACT', 5),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', '61c2f620-5fd9-4c69-8e1a-b44e47b0b282', '40.0'::jsonb, 'EXACT', 6),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', 'e245cfd6-d051-4fc4-9d36-901c038bc6c6', '48.0'::jsonb, 'EXACT', 7);

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '5a11fc73-f14d-405b-8775-85b32324d273', 'UNIT',
            'ShiftSupervisor stores Usada Pekora salary and bonus', 1, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, repeat_count)
    VALUES (gen_random_uuid(), tc, 'CONSTRUCTOR', '6b6469c1-efaf-4727-be02-46a00444d9f9',
            '["Usada Pekora", "880-K", "2020-01-17", 72000.0, 3500.25]'::jsonb, '[]'::jsonb, 0, 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
    VALUES
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', 'c09ecdab-d092-424f-9031-f3011551608a', '"Usada Pekora"'::jsonb, 'EXACT', 0),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', 'c168bc05-06e5-449b-9407-49846b5131a5', '"880-K"'::jsonb, 'EXACT', 1),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', '4764419a-35b1-4d5b-a295-8686ba6c5ed6', '"2020-01-17"'::jsonb, 'EXACT', 2),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', '0cda1904-319e-44e1-bc83-24c07be8084e', '72000.0'::jsonb, 'EXACT', 3),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', '6550dca7-4131-46a5-bb3d-c78a4456fd0a', '3500.25'::jsonb, 'EXACT', 4);

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '5a11fc73-f14d-405b-8775-85b32324d273', 'COMPOSITION',
            'setMonthlyBonus updates the team leader bonus', 2, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'CONSTRUCTOR', '16c7cc8f-6a76-4122-b32b-0c9e823f8799',
            '["Shirakami Fubuki", "315-G", "2021-05-15", 1, 20.5, 500.75, 40.0, 48.0]'::jsonb, '[]'::jsonb, 0, 'leader', 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', '9310cae7-db27-475b-a48c-1cb36f943a27',
            '[640.5]'::jsonb, '[]'::jsonb, 1, 'leader', 1)
    RETURNING id INTO s1;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s1, 'FIELD_STATE', '354eec9c-229b-4b60-a19f-3d47e13d427a', '640.5'::jsonb, 'EXACT', 0);

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '5a11fc73-f14d-405b-8775-85b32324d273', 'COMPOSITION',
            'setShift changes the team leader to night shift', 3, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'CONSTRUCTOR', '16c7cc8f-6a76-4122-b32b-0c9e823f8799',
            '["Shirakami Fubuki", "315-G", "2021-05-15", 1, 20.5, 500.75, 40.0, 48.0]'::jsonb, '[]'::jsonb, 0, 'leader', 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', '6ffa8527-5298-4e58-8301-3334098840bf',
            '[2]'::jsonb, '[]'::jsonb, 1, 'leader', 1)
    RETURNING id INTO s1;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s1, 'FIELD_STATE', 'a3dfb636-bc9b-497c-bd1f-1fd7766babd2', '2'::jsonb, 'EXACT', 0);

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '5a11fc73-f14d-405b-8775-85b32324d273', 'COMPOSITION',
            'setPayRate updates the team leader hourly rate', 4, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'CONSTRUCTOR', '16c7cc8f-6a76-4122-b32b-0c9e823f8799',
            '["Shirakami Fubuki", "315-G", "2021-05-15", 1, 20.5, 500.75, 40.0, 48.0]'::jsonb, '[]'::jsonb, 0, 'leader', 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', '8fc38fb7-f7ae-4e28-a2e6-b16a630d6988',
            '[21.75]'::jsonb, '[]'::jsonb, 1, 'leader', 1)
    RETURNING id INTO s1;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s1, 'FIELD_STATE', '5480503c-b1ae-40e5-9b8a-c1d4a0bb0ed6', '21.75'::jsonb, 'EXACT', 0);

    -- Lowercase m is outside A–M.
    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '5a11fc73-f14d-405b-8775-85b32324d273', 'COMPOSITION',
            'Employee number 123-m is rejected', 5, true)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'CONSTRUCTOR', '9da92a9c-6b1d-4819-a446-3a7b6ddee6e7',
            '["Shirakami Fubuki", "315-G", "2021-05-15"]'::jsonb, '[]'::jsonb, 0, 'employee', 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', '467cfb96-b26e-4c7b-bd1a-c7fc87a74c83',
            '["123-m"]'::jsonb, '[]'::jsonb, 1, 'employee', 1)
    RETURNING id INTO s1;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s1, 'RETURN_VALUE', 'false'::jsonb, 'EXACT', 0);

    -- 72000 + 3500.25 = 75500.25
    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '5a11fc73-f14d-405b-8775-85b32324d273', 'COMPOSITION',
            'Supervisor total salary adds Usada Pekora bonus', 6, true)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'CONSTRUCTOR', '6b6469c1-efaf-4727-be02-46a00444d9f9',
            '["Usada Pekora", "880-K", "2020-01-17", 72000.0, 3500.25]'::jsonb, '[]'::jsonb, 0, 'boss', 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', '8027ee68-c5d9-4815-9249-c06384f7c9b9',
            '[]'::jsonb, '[]'::jsonb, 1, 'boss', 1)
    RETURNING id INTO s1;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s1, 'RETURN_VALUE', '75500.25'::jsonb, 'EXACT', 0);

    -- 20.5 * 160 + 500.75 = 3780.75. Attended 48 >= required 40.
    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '5a11fc73-f14d-405b-8775-85b32324d273', 'COMPOSITION',
            'Team leader pay is hourly wages plus the monthly bonus', 7, true)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'CONSTRUCTOR', '16c7cc8f-6a76-4122-b32b-0c9e823f8799',
            '["Shirakami Fubuki", "315-G", "2021-05-15", 1, 20.5, 500.75, 40.0, 48.0]'::jsonb, '[]'::jsonb, 0, 'leader', 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', '29d6c9f2-3f54-4aa5-aa50-8ecc08f24470',
            '[160.0]'::jsonb, '[]'::jsonb, 1, 'leader', 1)
    RETURNING id INTO s1;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s1, 'RETURN_VALUE', '3780.75'::jsonb, 'EXACT', 0);

    -- ========== Challenge 4 (10) ==========

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '3896bc53-0e4f-4706-9e9d-c366c00b5562', 'COMPOSITION',
            'Brand toString is name - country', 0, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'CONSTRUCTOR', '576703be-e0fd-4d1e-9e58-e34e6fb22505',
            '["Hololive", "South Korea"]'::jsonb, '[]'::jsonb, 0, 'brand', 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', '44940f36-bccd-4a68-93e0-209e4c00c5ec',
            '[]'::jsonb, '[]'::jsonb, 1, 'brand', 1)
    RETURNING id INTO s1;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
    VALUES
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', 'd543769e-51ff-4d6f-9844-3196ae81d696', '"Hololive"'::jsonb, 'EXACT', 0),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', '590164ce-380b-43f6-b891-dac46435a02e', '"South Korea"'::jsonb, 'EXACT', 1);
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s1, 'RETURN_VALUE', '"Hololive - South Korea"'::jsonb, 'EXACT', 0);

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '3896bc53-0e4f-4706-9e9d-c366c00b5562', 'UNIT',
            'Discount stores 15 percent until 2026-12-31', 1, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, repeat_count)
    VALUES (gen_random_uuid(), tc, 'CONSTRUCTOR', '2ffe5861-91e6-454d-8407-10f08cb8d6b5',
            '[0.15, "2026-12-31"]'::jsonb, '[]'::jsonb, 0, 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
    VALUES
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', '101c793f-97e5-4c49-a9de-eeaa85902881', '0.15'::jsonb, 'EXACT', 0),
        (gen_random_uuid(), tc, s0, 'FIELD_STATE', 'a83991ea-ef8a-415f-be01-37846b98388c', '"2026-12-31"'::jsonb, 'EXACT', 1);

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '3896bc53-0e4f-4706-9e9d-c366c00b5562', 'COMPOSITION',
            'Ballpoint without a discount is type BALLPOINT', 2, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'CONSTRUCTOR', '576703be-e0fd-4d1e-9e58-e34e6fb22505',
            '["Hololive", "Japan"]'::jsonb, '[]'::jsonb, 0, 'brand', 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'CONSTRUCTOR', 'f95fbc14-ccfe-42dc-9913-e4b86254436c',
            '["Comet", {"$instance":"brand"}, 125000.0]'::jsonb, '[]'::jsonb, 1, 'pen', 1)
    RETURNING id INTO s1;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
    VALUES
        (gen_random_uuid(), tc, s1, 'FIELD_STATE', 'c759996b-852b-42d1-92df-8b40f107a021', '"Comet"'::jsonb, 'EXACT', 0),
        (gen_random_uuid(), tc, s1, 'FIELD_STATE', 'c3f87215-8d3d-4acc-8785-bbe6a0b55d2d', '125000.0'::jsonb, 'EXACT', 1),
        (gen_random_uuid(), tc, s1, 'FIELD_STATE', '39479090-0491-48d3-80a7-202f566e7564', '"BALLPOINT"'::jsonb, 'EXACT', 2),
        (gen_random_uuid(), tc, s1, 'FIELD_STATE', '98f3fd96-b39a-4ec2-99d8-a75a20c411e1', '{"$instance":"brand"}'::jsonb, 'EXACT', 3),
        (gen_random_uuid(), tc, s1, 'FIELD_STATE', '4b5f1153-2dbf-4816-b919-81792cc459ae', 'null'::jsonb, 'EXACT', 4);

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '3896bc53-0e4f-4706-9e9d-c366c00b5562', 'COMPOSITION',
            'Ballpoint with a discount keeps that discount', 3, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '576703be-e0fd-4d1e-9e58-e34e6fb22505',
            '["Hololive", "Japan"]'::jsonb, '[]'::jsonb, 0, 'brand', 1),
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '2ffe5861-91e6-454d-8407-10f08cb8d6b5',
            '[0.15, "2026-12-31"]'::jsonb, '[]'::jsonb, 1, 'sale', 1),
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '6da83233-b00a-4ba0-9062-a3cb23712819',
            '["Starlight", {"$instance":"brand"}, 125000.0, {"$instance":"sale"}]'::jsonb, '[]'::jsonb, 2, 'pen', 1);
    SELECT id INTO s2 FROM testcase_invocation WHERE testcase_id = tc AND order_index = 2;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
    VALUES
        (gen_random_uuid(), tc, s2, 'FIELD_STATE', '39479090-0491-48d3-80a7-202f566e7564', '"BALLPOINT"'::jsonb, 'EXACT', 0),
        (gen_random_uuid(), tc, s2, 'FIELD_STATE', '4b5f1153-2dbf-4816-b919-81792cc459ae', '{"$instance":"sale"}'::jsonb, 'EXACT', 1),
        (gen_random_uuid(), tc, s2, 'FIELD_STATE', 'c3f87215-8d3d-4acc-8785-bbe6a0b55d2d', '125000.0'::jsonb, 'EXACT', 2);

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '3896bc53-0e4f-4706-9e9d-c366c00b5562', 'COMPOSITION',
            'Fountain pen is type FOUNTAIN', 4, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '576703be-e0fd-4d1e-9e58-e34e6fb22505',
            '["Hololive", "Japan"]'::jsonb, '[]'::jsonb, 0, 'brand', 1),
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '5607d7a2-cfec-4951-ae62-57433c810076',
            '["Aurora", {"$instance":"brand"}, 480000.0]'::jsonb, '[]'::jsonb, 1, 'pen', 1);
    SELECT id INTO s1 FROM testcase_invocation WHERE testcase_id = tc AND order_index = 1;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
    VALUES
        (gen_random_uuid(), tc, s1, 'FIELD_STATE', 'c759996b-852b-42d1-92df-8b40f107a021', '"Aurora"'::jsonb, 'EXACT', 0),
        (gen_random_uuid(), tc, s1, 'FIELD_STATE', '39479090-0491-48d3-80a7-202f566e7564', '"FOUNTAIN"'::jsonb, 'EXACT', 1),
        (gen_random_uuid(), tc, s1, 'FIELD_STATE', 'c3f87215-8d3d-4acc-8785-bbe6a0b55d2d', '480000.0'::jsonb, 'EXACT', 2),
        (gen_random_uuid(), tc, s1, 'FIELD_STATE', '98f3fd96-b39a-4ec2-99d8-a75a20c411e1', '{"$instance":"brand"}'::jsonb, 'EXACT', 3);

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '3896bc53-0e4f-4706-9e9d-c366c00b5562', 'COMPOSITION',
            'Ballpoint tax is 8 percent', 5, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '576703be-e0fd-4d1e-9e58-e34e6fb22505',
            '["Hololive", "Japan"]'::jsonb, '[]'::jsonb, 0, 'brand', 1),
        (gen_random_uuid(), tc, 'CONSTRUCTOR', 'f95fbc14-ccfe-42dc-9913-e4b86254436c',
            '["Comet", {"$instance":"brand"}, 80.0]'::jsonb, '[]'::jsonb, 1, 'pen', 1);
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', '4ccced96-6821-4893-aab2-5e1f1c70de08',
            '[]'::jsonb, '[]'::jsonb, 2, 'pen', 1)
    RETURNING id INTO s2;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s2, 'RETURN_VALUE', '0.08'::jsonb, 'EXACT', 0);

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '3896bc53-0e4f-4706-9e9d-c366c00b5562', 'COMPOSITION',
            'Fountain tax is 10 percent', 6, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '576703be-e0fd-4d1e-9e58-e34e6fb22505',
            '["Hololive", "Japan"]'::jsonb, '[]'::jsonb, 0, 'brand', 1),
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '5607d7a2-cfec-4951-ae62-57433c810076',
            '["Aurora", {"$instance":"brand"}, 480000.0]'::jsonb, '[]'::jsonb, 1, 'pen', 1);
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', '181a1966-b87b-4a56-a067-5f306cbb5f3f',
            '[]'::jsonb, '[]'::jsonb, 2, 'pen', 1)
    RETURNING id INTO s2;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s2, 'RETURN_VALUE', '0.1'::jsonb, 'EXACT', 0);

    -- Null discount must not throw when id is read.
    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '3896bc53-0e4f-4706-9e9d-c366c00b5562', 'COMPOSITION',
            'A fountain pen with no discount is not discounted', 7, true)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '576703be-e0fd-4d1e-9e58-e34e6fb22505',
            '["Hololive", "Japan"]'::jsonb, '[]'::jsonb, 0, 'brand', 1),
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '5607d7a2-cfec-4951-ae62-57433c810076',
            '["Aurora", {"$instance":"brand"}, 480000.0]'::jsonb, '[]'::jsonb, 1, 'pen', 1);
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', '2f2ea4e8-e7d5-4b39-adee-ba88d94a444c',
            '[]'::jsonb, '[]'::jsonb, 2, 'pen', 1)
    RETURNING id INTO s2;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s2, 'RETURN_VALUE', 'false'::jsonb, 'EXACT', 0);

    -- 125000 * (1 - 0.15) = 106250
    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '3896bc53-0e4f-4706-9e9d-c366c00b5562', 'COMPOSITION',
            'Discounted ballpoint price is 106250', 8, true)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '576703be-e0fd-4d1e-9e58-e34e6fb22505',
            '["Hololive", "Japan"]'::jsonb, '[]'::jsonb, 0, 'brand', 1),
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '2ffe5861-91e6-454d-8407-10f08cb8d6b5',
            '[0.15, "2026-12-31"]'::jsonb, '[]'::jsonb, 1, 'sale', 1),
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '6da83233-b00a-4ba0-9062-a3cb23712819',
            '["Starlight", {"$instance":"brand"}, 125000.0, {"$instance":"sale"}]'::jsonb, '[]'::jsonb, 2, 'pen', 1);
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', '0c1f2e2e-d7ff-4b93-9082-ac9ddc7c151f',
            '[]'::jsonb, '[]'::jsonb, 3, 'pen', 1)
    RETURNING id INTO s3;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s3, 'RETURN_VALUE', '106250.0'::jsonb, 'EXACT', 0);

    -- 125000 * 1.08 - 106250 = 28750
    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), '3896bc53-0e4f-4706-9e9d-c366c00b5562', 'COMPOSITION',
            'Price after tax subtracts the discounted price', 9, true)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '576703be-e0fd-4d1e-9e58-e34e6fb22505',
            '["Hololive", "Japan"]'::jsonb, '[]'::jsonb, 0, 'brand', 1),
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '2ffe5861-91e6-454d-8407-10f08cb8d6b5',
            '[0.15, "2026-12-31"]'::jsonb, '[]'::jsonb, 1, 'sale', 1),
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '6da83233-b00a-4ba0-9062-a3cb23712819',
            '["Starlight", {"$instance":"brand"}, 125000.0, {"$instance":"sale"}]'::jsonb, '[]'::jsonb, 2, 'pen', 1);
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', 'f53bbed8-30f8-48e5-aece-ba7cbd75aa33',
            '[]'::jsonb, '[]'::jsonb, 3, 'pen', 1)
    RETURNING id INTO s3;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s3, 'RETURN_VALUE', '28750.0'::jsonb, 'EXACT', 0);

    -- ========== Challenge 5 (10) ==========

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), 'c5096bc4-cda0-4cb0-bee5-be3b914ba813', 'UNIT',
            'Moc Cau tea name', 0, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', '9d7ed118-78a7-4a57-a947-568293794191',
            '[]'::jsonb, '[]'::jsonb, 0, 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s0, 'RETURN_VALUE', '"Moc Cau Tea"'::jsonb, 'EXACT', 0);

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), 'c5096bc4-cda0-4cb0-bee5-be3b914ba813', 'UNIT',
            'Moc Cau tea flavor', 1, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', '9798e566-f7e6-4ace-aec9-34f8a2156bf6',
            '[]'::jsonb, '[]'::jsonb, 0, 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s0, 'RETURN_VALUE', '"Grassy, fresh corn aroma"'::jsonb, 'EXACT', 0);

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), 'c5096bc4-cda0-4cb0-bee5-be3b914ba813', 'UNIT',
            'Moc Cau tea price before tax', 2, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', '1fdf2627-793f-4874-a0b7-f2928e3a9364',
            '[]'::jsonb, '[]'::jsonb, 0, 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s0, 'RETURN_VALUE', '350000.0'::jsonb, 'EXACT', 0);

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), 'c5096bc4-cda0-4cb0-bee5-be3b914ba813', 'UNIT',
            'Dinh Ngoc Tra tea price before tax', 3, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', '75a6b4bc-5f6a-45ae-b987-a80ff2cbcdcb',
            '[]'::jsonb, '[]'::jsonb, 0, 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s0, 'RETURN_VALUE', '2500000.0'::jsonb, 'EXACT', 0);

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), 'c5096bc4-cda0-4cb0-bee5-be3b914ba813', 'UNIT',
            'Lotus tea flavor', 4, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', 'de488464-9d47-4a3c-883e-59dd2d32787f',
            '[]'::jsonb, '[]'::jsonb, 0, 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s0, 'RETURN_VALUE', '"Delicate lotus fragrance, mild & calming sweet"'::jsonb, 'EXACT', 0);

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), 'c5096bc4-cda0-4cb0-bee5-be3b914ba813', 'UNIT',
            'Jasmine tea name', 5, false)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', 'abe84f31-4a4d-41a6-a33e-f60393d01b4d',
            '[]'::jsonb, '[]'::jsonb, 0, 1)
    RETURNING id INTO s0;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s0, 'RETURN_VALUE', '"Jasmine Tea"'::jsonb, 'EXACT', 0);

    -- 350000 * 1.08 * 1.25 = 472500
    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), 'c5096bc4-cda0-4cb0-bee5-be3b914ba813', 'COMPOSITION',
            'Moc Cau order of 1.25 kg includes tea tax', 6, true)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, dispatch_class_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES
        (gen_random_uuid(), tc, 'CONSTRUCTOR', NULL, '0025c54f-e441-4d23-a3e5-944f53046283',
            '[]'::jsonb, '[]'::jsonb, 0, 'moc', 1),
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '7bd247a8-5bfb-4732-9c37-ed52b7e0ff50', NULL,
            '[{"$instance":"moc"}, 1.25]'::jsonb, '[]'::jsonb, 1, 'line', 1);
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', '43bde16b-ed16-403a-b6c7-3b7262dfafac',
            '[]'::jsonb, '[]'::jsonb, 2, 'line', 1)
    RETURNING id INTO s2;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s2, 'RETURN_VALUE', '472500.0'::jsonb, 'EXACT', 0);

    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), 'c5096bc4-cda0-4cb0-bee5-be3b914ba813', 'COMPOSITION',
            'Two Moc Cau teas are equal', 7, true)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, dispatch_class_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES
        (gen_random_uuid(), tc, 'CONSTRUCTOR', NULL, '0025c54f-e441-4d23-a3e5-944f53046283',
            '[]'::jsonb, '[]'::jsonb, 0, 'left', 1),
        (gen_random_uuid(), tc, 'CONSTRUCTOR', NULL, '0025c54f-e441-4d23-a3e5-944f53046283',
            '[]'::jsonb, '[]'::jsonb, 1, 'right', 1);
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', 'cd04e8bf-d83f-4b52-b68b-e320f305eb39',
            '[{"$instance":"right"}]'::jsonb, '[]'::jsonb, 2, 'left', 1)
    RETURNING id INTO s2;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s2, 'RETURN_VALUE', 'true'::jsonb, 'EXACT', 0);

    -- Different LotusTea instances and different weights. equals looks at the tea only.
    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), 'c5096bc4-cda0-4cb0-bee5-be3b914ba813', 'COMPOSITION',
            'Ordered items match when the teas match, even if the weight differs', 8, true)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, dispatch_class_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES
        (gen_random_uuid(), tc, 'CONSTRUCTOR', NULL, '661ab25f-66ce-4e18-85bf-e740ff6faadd',
            '[]'::jsonb, '[]'::jsonb, 0, 'lotusA', 1),
        (gen_random_uuid(), tc, 'CONSTRUCTOR', NULL, '661ab25f-66ce-4e18-85bf-e740ff6faadd',
            '[]'::jsonb, '[]'::jsonb, 1, 'lotusB', 1),
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '7bd247a8-5bfb-4732-9c37-ed52b7e0ff50', NULL,
            '[{"$instance":"lotusA"}, 0.5]'::jsonb, '[]'::jsonb, 2, 'light', 1),
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '7bd247a8-5bfb-4732-9c37-ed52b7e0ff50', NULL,
            '[{"$instance":"lotusB"}, 3.0]'::jsonb, '[]'::jsonb, 3, 'heavy', 1);
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', 'ea1a66b4-467c-4b15-8ecd-03acd8acfe32',
            '[{"$instance":"heavy"}]'::jsonb, '[]'::jsonb, 4, 'light', 1)
    RETURNING id INTO s4;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s4, 'RETURN_VALUE', 'true'::jsonb, 'EXACT', 0);

    -- 350000 * 1.08 * 2 = 756000. The 10 kg duplicate and the null add are ignored.
    -- 600000 * 1.08 * 0.25 = 162000. Total = 918000.
    INSERT INTO testcase (id, challenge_id, testcase_type, name, order_index, is_hidden)
    VALUES (gen_random_uuid(), 'c5096bc4-cda0-4cb0-bee5-be3b914ba813', 'COMPOSITION',
            'Cart total ignores a null and a duplicate tea', 9, true)
    RETURNING id INTO tc;
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, constructor_id, dispatch_class_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES
        (gen_random_uuid(), tc, 'CONSTRUCTOR', 'c9e1c5ad-cc9f-4c6e-9739-4303aa7a0577', NULL,
            '[]'::jsonb, '[]'::jsonb, 0, 'cart', 1),
        (gen_random_uuid(), tc, 'CONSTRUCTOR', NULL, '0025c54f-e441-4d23-a3e5-944f53046283',
            '[]'::jsonb, '[]'::jsonb, 1, 'moc', 1),
        (gen_random_uuid(), tc, 'CONSTRUCTOR', NULL, '42e631d5-a8d5-4944-a83a-cf203bef3d5e',
            '[]'::jsonb, '[]'::jsonb, 2, 'jasmine', 1),
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '7bd247a8-5bfb-4732-9c37-ed52b7e0ff50', NULL,
            '[{"$instance":"moc"}, 2.0]'::jsonb, '[]'::jsonb, 3, 'cup', 1),
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '7bd247a8-5bfb-4732-9c37-ed52b7e0ff50', NULL,
            '[{"$instance":"moc"}, 10.0]'::jsonb, '[]'::jsonb, 4, 'extra', 1),
        (gen_random_uuid(), tc, 'CONSTRUCTOR', '7bd247a8-5bfb-4732-9c37-ed52b7e0ff50', NULL,
            '[{"$instance":"jasmine"}, 0.25]'::jsonb, '[]'::jsonb, 5, 'pot', 1);
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES
        (gen_random_uuid(), tc, 'METHOD', '4ecb78a8-9298-401a-9f1d-596a40f71939',
            '[{"$instance":"cup"}]'::jsonb, '[]'::jsonb, 6, 'cart', 1),
        (gen_random_uuid(), tc, 'METHOD', '4ecb78a8-9298-401a-9f1d-596a40f71939',
            '[{"$instance":"extra"}]'::jsonb, '[]'::jsonb, 7, 'cart', 1),
        (gen_random_uuid(), tc, 'METHOD', '4ecb78a8-9298-401a-9f1d-596a40f71939',
            '[null]'::jsonb, '[]'::jsonb, 8, 'cart', 1),
        (gen_random_uuid(), tc, 'METHOD', '4ecb78a8-9298-401a-9f1d-596a40f71939',
            '[{"$instance":"pot"}]'::jsonb, '[]'::jsonb, 9, 'cart', 1);
    INSERT INTO testcase_invocation (id, testcase_id, invocation_kind, method_id, params, receiver_params, order_index, instance_name, repeat_count)
    VALUES (gen_random_uuid(), tc, 'METHOD', 'b4c1bed8-ff26-4786-b5b2-63307da9bbc3',
            '[]'::jsonb, '[]'::jsonb, 10, 'cart', 1)
    RETURNING id INTO s9;
    INSERT INTO testcase_assertion (id, testcase_id, invocation_id, assertion_kind, expected_value, comparison_mode, order_index)
    VALUES (gen_random_uuid(), tc, s9, 'RETURN_VALUE', '918000.0'::jsonb, 'EXACT', 0);

    IF (
        SELECT count(*)
        FROM testcase t
        JOIN challenge c ON c.id = t.challenge_id
        WHERE c.lab_id = '0c88b14e-6c4e-4de0-9cbf-7f8dd1a92fca'
    ) <> 36 THEN
        RAISE EXCEPTION 'Expected 36 operational testcases for Lab 4';
    END IF;
END $$;

-- Drop the no-arg tea constructors from the earlier run, after invocations no longer reference them.
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
