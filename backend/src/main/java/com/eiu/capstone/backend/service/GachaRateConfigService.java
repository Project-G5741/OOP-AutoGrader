package com.eiu.capstone.backend.service;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("!desktop & !desktop-lecturer")
public class GachaRateConfigService {

    private final JdbcTemplate jdbcTemplate;

    public GachaRateConfigService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public GachaRateConfig current() {
        return jdbcTemplate.queryForObject("""
                SELECT cost_single_points, cost_multi_points, hard_pity_limit, soft_pity_start,
                       direct_legendary_rate, crystal_base_rate, random_mascot_rate,
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
                rs.getDouble(11)));
    }
}