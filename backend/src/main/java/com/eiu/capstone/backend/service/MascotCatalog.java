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
    public static final String EXCLUSIVE_ICE_QUEEN_ID = "ice_queen";
    public static final String EXCLUSIVE_ICE_QUEEN_NAME = "Ice Queen";
    public static final Offer EXCLUSIVE_ICE_QUEEN = new Offer(EXCLUSIVE_ICE_QUEEN_ID, EXCLUSIVE_ICE_QUEEN_NAME, 0);

    public record Offer(String id, String name, int price) {
    }

    public static final List<Offer> FOR_SALE = List.of(
            new Offer("bear", "Bear", 333),
            new Offer("capybara", "Capybara", 666),
            new Offer("panda", "Panda", 999),
            new Offer("penguin", "Penguin", 1111),
            new Offer("alpaca", "Alpaca", 1555),
            new Offer("bunny", "Bunny", 2222),
            new Offer("vit", "Duck", 2444),
            new Offer("hai_cau", "Hải Cẩu", 2666),
            new Offer("hamster", "Hamster", 2888),
            new Offer("cat", "Cat", 3333),
            new Offer("chuot_tui", "Kangaroo", 3555),
            new Offer("red_panda", "Red Panda", 3666),
            new Offer("shiba", "Shiba", 3888),
            new Offer("dai_bang", "Eagle", 4222),
            new Offer("pomeranian", "Pomeranian", 4555),
            new Offer("cho_soi", "Wolf", 4888),
            new Offer("ho_diep", "Tinh Linh Hồ Điệp", 5888),
            new Offer("huyen_vu", "Huyền Vũ", 6666),
            new Offer("thien_nga", "Thiên Nga Ánh Trăng", 7222),
            new Offer("chu_tuoc", "Chu Tước", 7777),
            new Offer("bach_ho", "Bạch Hổ", 8888),
            new Offer("cuu_vi", "Cửu Vĩ Hồ", 9555),
            new Offer("thanh_long", "Thanh Long", 9999),
            new Offer("con_bang", "Côn Bằng", 20000),
            new Offer("cung_ky", "Cùng Kỳ", 20000),
            new Offer("stella_witch", "Pháp Sư Tinh Tú", 30000),
            new Offer("zerotwo", "Zero2", 35000),
            new Offer("linh_loc", "Linh Lộc", 40000),
            new Offer("ki_lan", "Kì Lân", 40000),
            new Offer("sakura_princess", "Công Chúa Nguyệt Hoa", 40000),
            new Offer("floral_fairy", "Hoa Tiên Linh Lan", 75000),
            new Offer("chrono_celestia", "Nữ Thần Thời Không", 75000),
            new Offer("cyber_striker", "Chiến Thần Cơ Giáp", 75000),
            new Offer("kiem_de", "Kiếm Đế Hư Không", 75000));

    private static final Map<String, Offer> BY_ID = FOR_SALE.stream()
            .collect(Collectors.toUnmodifiableMap(Offer::id, Function.identity()));

    private MascotCatalog() {
    }

    public static String canonicalId(String id) {
        if (id == null) {
            return null;
        }
        String normalized = id.trim().toLowerCase().replace('-', '_');
        if (EXCLUSIVE_ICE_QUEEN_ID.equals(normalized)) {
            return EXCLUSIVE_ICE_QUEEN_ID;
        }
        return normalized;
    }

    public static Offer forSale(String id) {
        String key = canonicalId(id);
        if (key == null) {
            return null;
        }
        if (EXCLUSIVE_ICE_QUEEN_ID.equals(key)) {
            return EXCLUSIVE_ICE_QUEEN;
        }
        return BY_ID.get(key);
    }

    public static boolean isSelectable(String id) {
        String key = canonicalId(id);
        if (key == null) {
            return false;
        }
        return STARTER_ID.equals(key)
                || BY_ID.containsKey(key)
                || EXCLUSIVE_ICE_QUEEN_ID.equals(key);
    }

    public static String displayName(String id) {
        String key = canonicalId(id);
        if (STARTER_ID.equals(key)) {
            return STARTER_NAME;
        }
        if (EXCLUSIVE_ICE_QUEEN_ID.equals(key)) {
            return EXCLUSIVE_ICE_QUEEN_NAME;
        }
        Offer offer = forSale(key);
        return offer == null ? STARTER_NAME : offer.name();
    }
}
