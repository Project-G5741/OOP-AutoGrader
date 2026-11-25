-- Operator mirror: Admin Gacha & Egg Smash drop-rate configuration.
-- Startup: GachaRateConfigSchemaMigrator + EggSmashRateConfigSchemaMigrator on PostgreSQL.

ALTER TABLE gacha_rate_config
    ADD COLUMN IF NOT EXISTS point_crystal_rate DOUBLE PRECISION NOT NULL DEFAULT 0.0001;

CREATE TABLE IF NOT EXISTS egg_smash_rate_config (
    id                      SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    base_crystal_hit_rate   DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    turn6_crystal_hit_rate  DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    turn7_crystal_hit_rate  DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    turn8_crystal_hit_rate  DOUBLE PRECISION NOT NULL DEFAULT 0.3333333333333333,
    turn9_crystal_hit_rate  DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    egg_rate_300            DOUBLE PRECISION NOT NULL DEFAULT 0.05,
    egg_rate_200            DOUBLE PRECISION NOT NULL DEFAULT 0.15,
    egg_rate_100            DOUBLE PRECISION NOT NULL DEFAULT 0.30,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE egg_smash_rate_config
    ALTER COLUMN turn6_crystal_hit_rate SET DEFAULT 0.0,
    ALTER COLUMN turn7_crystal_hit_rate SET DEFAULT 0.0;

INSERT INTO egg_smash_rate_config (id) VALUES (1)
ON CONFLICT (id) DO NOTHING;

UPDATE egg_smash_rate_config
SET turn6_crystal_hit_rate = 0.0,
    turn7_crystal_hit_rate = 0.0,
    updated_at = NOW()
WHERE id = 1
  AND ABS(turn6_crystal_hit_rate - 0.0001) < 1e-9
  AND ABS(turn7_crystal_hit_rate - 0.05) < 1e-9;
