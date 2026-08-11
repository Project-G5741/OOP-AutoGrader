-- Sample operational testcase seed (operator-run).
-- Replace UUID placeholders with real challenge/constructor/method/field IDs from your lab rubric.

-- Example: constructor field-state testcase
-- INSERT INTO testcase (id, challenge_id, testcase_type, name, weight, order_index)
-- VALUES ('00000000-0000-0000-0000-000000000101', '<challenge-id>', 'SINGLE_INVOCATION', 'Account balance after deposit', 1, 0);
--
-- INSERT INTO testcase_invocation (testcase_id, invocation_kind, constructor_id, params)
-- VALUES ('00000000-0000-0000-0000-000000000101', 'CONSTRUCTOR', '<constructor-id>', '[100]');
--
-- INSERT INTO testcase_assertion (testcase_id, assertion_kind, field_id, expected_value, comparison_mode, order_index)
-- VALUES ('00000000-0000-0000-0000-000000000101', 'FIELD_STATE', '<balance-field-id>', '100', 'EXACT', 0);

-- Example: method return-value testcase
-- INSERT INTO testcase (id, challenge_id, testcase_type, name, weight, order_index)
-- VALUES ('00000000-0000-0000-0000-000000000102', '<challenge-id>', 'SINGLE_INVOCATION', 'Deposit returns new balance', 1, 1);
--
-- INSERT INTO testcase_invocation (testcase_id, invocation_kind, method_id, params)
-- VALUES ('00000000-0000-0000-0000-000000000102', 'METHOD', '<deposit-method-id>', '[50]');
--
-- INSERT INTO testcase_assertion (testcase_id, assertion_kind, expected_value, comparison_mode, order_index)
-- VALUES ('00000000-0000-0000-0000-000000000102', 'RETURN_VALUE', '150', 'EXACT', 0);

-- Example: stdout testcase
-- INSERT INTO testcase_assertion (testcase_id, assertion_kind, expected_value, comparison_mode, order_index)
-- VALUES ('<testcase-id>', 'STDOUT', '"Hello\\n"', 'EXACT', 0);

-- Example: exception testcase (type only)
-- INSERT INTO testcase_assertion (testcase_id, assertion_kind, expected_value, comparison_mode, order_index)
-- VALUES ('<testcase-id>', 'EXCEPTION', '"IllegalArgumentException"', 'EXACT', 0);

-- Example: comparison testcase
-- INSERT INTO testcase (id, challenge_id, testcase_type, comparison_method, name, weight, order_index)
-- VALUES ('00000000-0000-0000-0000-000000000103', '<challenge-id>', 'COMPARISON', 'EQUALS', 'Equal accounts', 1, 2);
--
-- INSERT INTO testcase_instance (testcase_id, label, constructor_id, params)
-- VALUES
--   ('00000000-0000-0000-0000-000000000103', 'A', '<constructor-id>', '[10]'),
--   ('00000000-0000-0000-0000-000000000103', 'B', '<constructor-id>', '[10]');
--
-- INSERT INTO testcase_assertion (testcase_id, assertion_kind, expected_value, comparison_mode, order_index)
-- VALUES ('00000000-0000-0000-0000-000000000103', 'COMPARISON_RESULT', 'true', 'EXACT', 0);
