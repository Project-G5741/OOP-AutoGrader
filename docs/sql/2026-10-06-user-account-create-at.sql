-- Account creation timestamp (Vietnam). Application sets on user create and first Google login when null.
ALTER TABLE user_account
    ADD COLUMN IF NOT EXISTS create_at TIMESTAMPTZ;

-- Legacy environments may have a quoted mixed-case column from manual DDL.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'user_account'
          AND column_name = 'CREATE_AT'
    ) THEN
        ALTER TABLE user_account RENAME COLUMN "CREATE_AT" TO create_at;
    END IF;
END $$;

-- If create_at was DATE, widen to timestamptz (midnight Vietnam for existing rows).
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'user_account'
          AND column_name = 'create_at'
          AND data_type = 'date'
    ) THEN
        ALTER TABLE user_account
            ALTER COLUMN create_at TYPE TIMESTAMPTZ
            USING (create_at::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh');
    END IF;
END $$;
