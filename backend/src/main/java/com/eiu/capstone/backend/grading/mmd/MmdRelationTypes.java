package com.eiu.capstone.backend.grading.mmd;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MmdRelationTypes {

    /** Canonical type for Mermaid arrows outside the graded syntax table. */
    public static final String UNKNOWN = "unknown";

    /**
     * Graded Mermaid arrows only (table SoT). Longest-match order is applied in the static block.
     */
    private static final List<String> CANONICAL_ARROWS = List.of(
            "<|--|>",
            "..|>", "<|..",
            "<-->",
            "<|--", "--|>",
            "*--", "--*",
            "o--", "--o",
            "()--", "--()",
            "..>", "<..",
            "-->", "<--",
            "..", "--");

    /**
     * Extra Mermaid-shaped arrows we still parse as relation lines so they do not blow up the
     * diagram; {@link #canonicalRelationType} maps them to {@link #UNKNOWN} (wrong relationship).
     */
    private static final List<String> NON_CANONICAL_ARROWS = List.of(
            "*-->", "--*>", "o-->", "--o>",
            "o..>", "..o>", "*..>", "..*>",
            "o..", "..o", "*..", "..*",
            "<--|>",
            "o--o", "*--*", "o--*", "*--o",
            "o..o", "*..*",
            "<|-->", "--|>|",
            "..--", "--..");

    public static final List<String> ARROWS;

    static {
        Set<String> arrows = new LinkedHashSet<>();
        arrows.addAll(CANONICAL_ARROWS);
        arrows.addAll(NON_CANONICAL_ARROWS);
        List<String> ordered = new ArrayList<>(arrows);
        ordered.sort(Comparator.comparingInt(String::length).reversed());
        ARROWS = List.copyOf(ordered);
    }

    private MmdRelationTypes() {}

    public static String matchArrowAt(String text, int index) {
        String best = null;
        for (String arrow : ARROWS) {
            if (text.regionMatches(index, arrow, 0, arrow.length())
                    && (best == null || arrow.length() > best.length())) {
                best = arrow;
            }
        }
        return best;
    }

    public static String canonicalRelationType(String arrow) {
        return switch (arrow) {
            case "<|--", "--|>" -> "inheritance";
            case "<|--|>" -> "bidirectional_inheritance";
            case "*--", "--*" -> "composition";
            case "o--", "--o" -> "aggregation";
            case "-->", "<--", "--" -> "association";
            case "<-->" -> "bidirectional_association";
            case ".." -> "dashed_link";
            case "..>", "<.." -> "dependency";
            case "..|>", "<|..", "()--", "--()" -> "realization";
            default -> UNKNOWN;
        };
    }

    public static boolean isCanonicalArrow(String arrow) {
        return arrow != null && !UNKNOWN.equals(canonicalRelationType(arrow));
    }
}
