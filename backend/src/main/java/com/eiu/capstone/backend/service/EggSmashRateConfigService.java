package com.eiu.capstone.backend.service;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.EggSmashRateUpdateRequest;

@Service
@Profile("!desktop & !desktop-lecturer")
public class EggSmashRateConfigService {

    private static final double EPSILON = 1e-6;

    private final JdbcTemplate jdbcTemplate;

    public EggSmashRateConfigService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public EggSmashRateConfig current() {
        List<EggSmashRateConfig> rows = jdbcTemplate.query("""
                SELECT base_crystal_hit_rate, turn6_crystal_hit_rate, turn7_crystal_hit_rate,
                       turn8_crystal_hit_rate, turn9_crystal_hit_rate,
                       egg_rate_300, egg_rate_200, egg_rate_100
                FROM egg_smash_rate_config WHERE id = 1
                """, (rs, rowNum) -> new EggSmashRateConfig(
                rs.getDouble(1),
                rs.getDouble(2),
                rs.getDouble(3),
                rs.getDouble(4),
                rs.getDouble(5),
                rs.getDouble(6),
                rs.getDouble(7),
                rs.getDouble(8)));
        return rows.isEmpty() ? EggSmashRateConfig.defaults() : rows.get(0);
    }

    @Transactional
    public EggSmashRateConfig update(EggSmashRateUpdateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Egg Smash rate payload");
        }
        double baseCrystal = requireRate(request.baseCrystalHitRate(), "Turns 1–5 Crystal rate");
        double turn6Crystal = requireRate(request.turn6CrystalHitRate(), "Turn 6 Crystal rate");
        double turn7Crystal = requireRate(request.turn7CrystalHitRate(), "Turn 7 Crystal rate");
        double turn8Crystal = requireRate(request.turn8CrystalHitRate(), "Turn 8 Crystal rate");
        double turn9Crystal = requireRate(request.turn9CrystalHitRate(), "Turn 9 Crystal rate");
        double r300 = requireRate(request.eggRate300(), "+300 Points egg rate");
        double r200 = requireRate(request.eggRate200(), "+200 Points egg rate");
        double r100 = requireRate(request.eggRate100(), "+100 Points egg rate");

        double eggSum = r300 + r200 + r100;
        if (eggSum > 1.0 + EPSILON) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Point egg rates (+300/+200/+100) cannot exceed 100%");
        }

        EggSmashRateConfig next = new EggSmashRateConfig(
                baseCrystal,
                turn6Crystal,
                turn7Crystal,
                turn8Crystal,
                turn9Crystal,
                r300,
                r200,
                r100);
        persist(next);
        return next;
    }

    @Transactional
    public EggSmashRateConfig resetDefaults() {
        EggSmashRateConfig defaults = EggSmashRateConfig.defaults();
        persist(defaults);
        return defaults;
    }

    private void persist(EggSmashRateConfig cfg) {
        jdbcTemplate.update("""
                INSERT INTO egg_smash_rate_config (
                    id, base_crystal_hit_rate, turn6_crystal_hit_rate, turn7_crystal_hit_rate,
                    turn8_crystal_hit_rate, turn9_crystal_hit_rate,
                    egg_rate_300, egg_rate_200, egg_rate_100, updated_at
                ) VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                ON CONFLICT (id) DO UPDATE SET
                    base_crystal_hit_rate  = EXCLUDED.base_crystal_hit_rate,
                    turn6_crystal_hit_rate = EXCLUDED.turn6_crystal_hit_rate,
                    turn7_crystal_hit_rate = EXCLUDED.turn7_crystal_hit_rate,
                    turn8_crystal_hit_rate = EXCLUDED.turn8_crystal_hit_rate,
                    turn9_crystal_hit_rate = EXCLUDED.turn9_crystal_hit_rate,
                    egg_rate_300           = EXCLUDED.egg_rate_300,
                    egg_rate_200           = EXCLUDED.egg_rate_200,
                    egg_rate_100           = EXCLUDED.egg_rate_100,
                    updated_at             = NOW()
                """,
                cfg.baseCrystalHitRate(),
                cfg.turn6CrystalHitRate(),
                cfg.turn7CrystalHitRate(),
                cfg.turn8CrystalHitRate(),
                cfg.turn9CrystalHitRate(),
                cfg.eggRate300(),
                cfg.eggRate200(),
                cfg.eggRate100());
    }

    private static double requireRate(Double val, String label) {
        if (val == null || !Double.isFinite(val) || val < 0.0 || val > 1.0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    label + " must be between 0% and 100%");
        }
        return val;
    }
}
