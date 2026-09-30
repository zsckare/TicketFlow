ALTER TABLE users
    DROP CONSTRAINT IF EXISTS ck_users_role;

ALTER TABLE users
    ADD CONSTRAINT ck_users_role
    CHECK (role IN ('CUSTOMER', 'STAFF', 'ADMIN'));