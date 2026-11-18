package com.eiu.capstone.backend.service;

import java.util.Set;

/**
 * Mascot ids that gacha may unlock (not shop purchase). Excludes starter, exclusives, and teacher.
 */
public final class MascotGachaPools {

    private static final Set<String> UNLOCKABLE = Set.of(
            "bunny", "cat", "bear", "panda", "penguin",
            "vit", "hamster", "chuot_tui", "shiba", "dai_bang", "cho_soi",
            "thanh_long", "bach_ho", "chu_tuoc", "huyen_vu",
            "zerotwo", "con_bang", "cung_ky", "linh_loc", "ki_lan");

    private MascotGachaPools() {
    }

    public static boolean isUnlockable(String mascotId) {
        if (mascotId == null || mascotId.isBlank()) {
            return false;
        }
        return UNLOCKABLE.contains(mascotId.toLowerCase());
    }
}