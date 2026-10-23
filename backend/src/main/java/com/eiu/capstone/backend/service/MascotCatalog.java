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
            new Offer("cat", "Cat", 3333));

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
