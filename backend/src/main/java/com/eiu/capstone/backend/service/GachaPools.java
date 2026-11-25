package com.eiu.capstone.backend.service;

import java.util.List;

/** Must stay aligned with frontend `MascotGachaModal` random/legendary pools. */
public final class GachaPools {

    public static final List<String> RANDOM_MASCOT = List.of(
            "bunny", "cat", "bear", "capybara", "panda", "penguin", "alpaca",
            "vit", "hai_cau", "hamster", "chuot_tui", "red_panda",
            "shiba", "dai_bang", "pomeranian", "cho_soi",
            "ho_diep", "huyen_vu", "thien_nga", "chu_tuoc", "bach_ho", "cuu_vi", "thanh_long");

    public static final List<String> LEGENDARY_MASCOT = List.of(
            "zerotwo", "con_bang", "cung_ky", "stella_witch", "linh_loc", "ki_lan", "sakura_princess");

    private GachaPools() {
    }
}