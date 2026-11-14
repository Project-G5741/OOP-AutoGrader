package com.eiu.capstone.backend.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Mascots a student can wear. Fox is the starter. The others are bought with reward points.
 */
public final class MascotCatalog {

    public static final String STARTER_ID = "fox";
    public static final String STARTER_NAME = "Fox";

    public record Offer(String id, String name, int price) {
    }

    public static final List<Offer> FOR_SALE = List.of(
            new Offer("bear", "Bear", 333),
            new Offer("panda", "Panda", 999),
            new Offer("penguin", "Penguin", 1111),
            new Offer("bunny", "Bunny", 2222),
            new Offer("vit", "Duck", 2444),
            new Offer("hamster", "Hamster", 2888),
            new Offer("cat", "Cat", 3333),
            new Offer("chuot_tui", "Kangaroo", 3555),
            new Offer("shiba", "Shiba", 3888),
            new Offer("dai_bang", "Eagle", 4222),
            new Offer("cho_soi", "Wolf", 4888),
            new Offer("thanh_long", "Thanh Long", 9999),
            new Offer("bach_ho", "Bạch Hổ", 8888),
            new Offer("chu_tuoc", "Chu Tước", 7777),
            new Offer("huyen_vu", "Huyền Vũ", 6666),
            new Offer("linh_loc", "Linh Lộc", 8888),
            new Offer("ki_lan", "Kì Lân", 8888),
            new Offer("zerotwo", "Zero2", 35000),
            new Offer("con_bang", "Côn Bằng", 35000),
            new Offer("cung_ky", "Cùng Kỳ", 35000));

    private static final Map<String, Offer> BY_ID = FOR_SALE.stream()
            .collect(Collectors.toUnmodifiableMap(Offer::id, Function.identity()));

    private MascotCatalog() {
    }

    public static Offer forSale(String id) {
        if (id == null) {
            return null;
        }
        return BY_ID.get(id);
    }

    public static boolean isSelectable(String id) {
        return STARTER_ID.equals(id) || BY_ID.containsKey(id);
    }

    public static String displayName(String id) {
        if (STARTER_ID.equals(id)) {
            return STARTER_NAME;
        }
        Offer offer = forSale(id);
        return offer == null ? STARTER_NAME : offer.name();
    }
}
