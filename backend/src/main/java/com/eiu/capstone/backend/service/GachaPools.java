package com.eiu.capstone.backend.service;

import java.util.List;

/** Must stay aligned with frontend `MascotGachaModal` random/legendary pools. */
public final class GachaPools {

    public static final List<String> RANDOM_MASCOT = List.of(
            "bunny", "cat", "bear", "panda", "penguin", "vit", "hamster", "chuot_tui",
            "shiba", "dai_bang", "cho_soi", "thanh_long", "bach_ho", "chu_tuoc", "huyen_vu");

    public static final List<String> LEGENDARY_MASCOT = List.of(
            "zerotwo", "con_bang", "cung_ky", "linh_loc", "ki_lan");

    private GachaPools() {
    }
}