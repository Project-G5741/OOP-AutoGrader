package com.eiu.capstone.backend.service;

import java.util.Set;

/**
 * Mascot ids that gacha may unlock (not shop purchase). Excludes starter, exclusives, and teacher.
 */
public final class MascotGachaPools {

    private static final Set<String> UNLOCKABLE = Set.of(
            "bunny", "cat", "bear", "capybara", "panda", "penguin", "alpaca",
            "vit", "hai_cau", "hamster", "chuot_tui", "red_panda",
            "shiba", "dai_bang", "pomeranian", "cho_soi",
            "ho_diep", "huyen_vu", "thien_nga", "chu_tuoc", "bach_ho", "cuu_vi", "thanh_long",
            "zerotwo", "con_bang", "cung_ky", "stella_witch", "linh_loc", "ki_lan", "sakura_princess");

    private MascotGachaPools() {
    }

    public static boolean isUnlockable(String mascotId) {
        if (mascotId == null || mascotId.isBlank()) {
            return false;
        }
        return UNLOCKABLE.contains(mascotId.trim().toLowerCase().replace('-', '_'));
    }
}