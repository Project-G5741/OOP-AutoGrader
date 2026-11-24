-- Operator mirror: holiday_schedule_config singleton for Admin early holiday activation.
-- Startup: HolidayScheduleConfigSchemaMigrator on PostgreSQL.

CREATE TABLE IF NOT EXISTS holiday_schedule_config (
    id              SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    config_json     TEXT NOT NULL DEFAULT '{}',
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO holiday_schedule_config (id, config_json) VALUES (1, '{}')
ON CONFLICT (id) DO NOTHING;
