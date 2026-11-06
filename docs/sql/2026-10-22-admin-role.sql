-- Admin role row only. Does not create a user or a password.
-- The first Admin account is inserted by an operator, with a lecturer IRN and this role.
-- resolveRoles can also insert this row the first time an Admin grants the role.

INSERT INTO role (id, name)
SELECT gen_random_uuid(), 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE upper(name) = 'ADMIN');
