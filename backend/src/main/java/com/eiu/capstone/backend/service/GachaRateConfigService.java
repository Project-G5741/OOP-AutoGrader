package com.eiu.capstone.backend.service;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.GachaRateUpdateRequest;

@Service
@Profile("!desktop & !desktop-lecturer")
public class GachaRateConfigService {

    private static final double EPSILON = 1e-6;

    private final JdbcTemplate jdbcTemplate;

    public GachaRateConfigService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public GachaRateConfig current() {
        List<GachaRateConfig> rows = jdbcTemplate.query("""
                SELECT cost_single_points, cost_multi_points, hard_pity_limit, soft_pity_start,
                       direct_legendary_rate, crystal_base_rate, point_crystal_rate, random_mascot_rate,
                       cashback_rate_300, cashback_rate_150, cashback_rate_60, cashback_rate_20
                FROM gacha_rate_config WHERE id = 1
                """, (rs, rowNum) -> new GachaRateConfig(
                rs.getInt(1),
                rs.getInt(2),
                rs.getInt(3),
                rs.getInt(4),
                rs.getDouble(5),
                rs.getDouble(6),
                rs.getDouble(7),
                rs.getDouble(8),
                rs.getDouble(9),
                rs.getDouble(10),
                rs.getDouble(11),
                rs.getDouble(12)));
        return rows.isEmpty() ? GachaRateConfig.defaults() : rows.get(0);
    }

    @Transactional
    public GachaRateConfig update(GachaRateUpdateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing gacha rate payload");
        }
        GachaRateConfig existing = current();

        int costSingle = request.costSinglePoints() != null ? request.costSinglePoints() : existing.costSinglePoints();
        int costMulti = request.costMultiPoints() != null ? request.costMultiPoints() : existing.costMultiPoints();
        if (costSingle < 1 || costSingle > 100_000 || costMulti < 1 || costMulti > 1_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid roll point cost");
        }

        int hardPity = requireInt(request.hardPityLimit(), "Hard pity limit", 1, 1000);
        int softPity = requireInt(request.softPityStart(), "Soft pity start", 1, hardPity);

        double directLegendary = requireRate(request.directLegendaryRate(), "Direct Legendary mascot rate");
        double crystalBase = requireRate(request.crystalBaseRate(), "Ticket Crystal rate");
        double pointCrystal = requireRate(request.pointCrystalRate(), "Point Crystal rate");
        double randomMascot = requireRate(request.randomMascotRate(), "Random mascot rate");
        double cb300 = requireRate(request.cashbackRate300(), "+300 Points rate");
        double cb150 = requireRate(request.cashbackRate150(), "+150 Points rate");
        double cb60 = requireRate(request.cashbackRate60(), "+60 Points rate");
        double cb20 = requireRate(request.cashbackRate20(), "+20 Points rate");

        double cashbackSum = cb300 + cb150 + cb60 + cb20;
        if (cashbackSum > 1.0 + EPSILON) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Points cashback rates (+300/+150/+60/+20) cannot exceed 100%");
        }

        GachaRateConfig next = new GachaRateConfig(
                costSingle,
                costMulti,
                hardPity,
                softPity,
                directLegendary,
                crystalBase,
                pointCrystal,
                randomMascot,
                cb300,
                cb150,
                cb60,
                cb20);
        persist(next);
        return next;
    }

    @Transactional
    public GachaRateConfig resetDefaults() {
        GachaRateConfig defaults = GachaRateConfig.defaults();
        persist(defaults);
        return defaults;
    }

    private void persist(GachaRateConfig cfg) {
        jdbcTemplate.update("""
                INSERT INTO gacha_rate_config (
                    id, cost_single_points, cost_multi_points, hard_pity_limit, soft_pity_start,
                    direct_legendary_rate, crystal_base_rate, point_crystal_rate, random_mascot_rate,
                    cashback_rate_300, cashback_rate_150, cashback_rate_60, cashback_rate_20, updated_at
                ) VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                ON CONFLICT (id) DO UPDATE SET
                    cost_single_points    = EXCLUDED.cost_single_points,
                    cost_multi_points     = EXCLUDED.cost_multi_points,
                    hard_pity_limit       = EXCLUDED.hard_pity_limit,
                    soft_pity_start       = EXCLUDED.soft_pity_start,
                    direct_legendary_rate = EXCLUDED.direct_legendary_rate,
                    crystal_base_rate     = EXCLUDED.crystal_base_rate,
                    point_crystal_rate    = EXCLUDED.point_crystal_rate,
                    random_mascot_rate    = EXCLUDED.random_mascot_rate,
                    cashback_rate_300     = EXCLUDED.cashback_rate_300,
                    cashback_rate_150     = EXCLUDED.cashback_rate_150,
                    cashback_rate_60      = EXCLUDED.cashback_rate_60,
                    cashback_rate_20      = EXCLUDED.cashback_rate_20,
                    updated_at            = NOW()
                """,
                cfg.costSinglePoints(),
                cfg.costMultiPoints(),
                cfg.hardPityLimit(),
                cfg.softPityStart(),
                cfg.directLegendaryRate(),
                cfg.crystalBaseRate(),
                cfg.pointCrystalRate(),
                cfg.randomMascotRate(),
                cfg.cashbackRate300(),
                cfg.cashbackRate150(),
                cfg.cashbackRate60(),
                cfg.cashbackRate20());
    }

    private static int requireInt(Integer val, String label, int min, int max) {
        if (val == null || val < min || val > max) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    label + " must be between " + min + " and " + max);
        }
        return val;
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
