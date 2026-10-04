package com.eiu.capstone.backend.grading.mmd;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MmdRelationTypes {

    public static final List<String> ARROWS;

    static {
        List<String> arrows = new ArrayList<>(List.of(
                "..|>", "<|..", "*-->", "--*>", "*--", "--*", "o-->", "--o>", "o--", "--o",
                "<|--|>", "<--|>", "<-->", "<|--", "--|>", "..>", "<..", "-->", "<--",
                "()--", "--()", "..", "--"));
        arrows.sort(Comparator.comparingInt(String::length).reversed());
        ARROWS = List.copyOf(arrows);
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
            case "*--", "--*", "*-->", "--*>" -> "composition";
            case "o--", "--o", "o-->", "--o>" -> "aggregation";
            case "-->", "<--" -> "association";
            case "<-->", "<--|>" -> "bidirectional_association";
            case "--" -> "link";
            case ".." -> "dashed_link";
            case "..>", "<.." -> "dependency";
            case "..|>", "<|..", "()--", "--()" -> "realization";
            default -> "association";
        };
    }
}
