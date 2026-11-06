package com.eiu.capstone.backend.grading.mmd;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.eiu.capstone.backend.grading.mmd.ast.MmdRelationNode;

/**
 * Parses a single Mermaid class-diagram relationship line into an {@link MmdRelationNode}.
 */
public final class MmdRelationLineParser {

    private static final Pattern LOLLIPOP_LEFT = Pattern.compile(
            "^([A-Za-z_]\\w*)\\s+\\(\\)--\\s+([A-Za-z_]\\w*)\\s*$");
    private static final Pattern LOLLIPOP_RIGHT = Pattern.compile(
            "^([A-Za-z_]\\w*)\\s+--\\(\\)\\s+([A-Za-z_]\\w*)\\s*$");
    private static final Pattern ENDPOINT_CLASS_FIRST = Pattern.compile(
            "^([A-Za-z_]\\w*)(?:\\s+\"([^\"]*)\")?\\s*$");
    private static final Pattern ENDPOINT_CARD_FIRST = Pattern.compile(
            "^\"([^\"]*)\"\\s+([A-Za-z_]\\w*)\\s*$");

    public MmdRelationNode parse(String line, int lineNumber) {
        if (line == null || line.isBlank()) {
            return null;
        }

        String label = null;
        String stripped = line.trim();
        int labelIdx = stripped.indexOf(" : ");
        if (labelIdx >= 0) {
            label = stripped.substring(labelIdx + 3).trim();
            stripped = stripped.substring(0, labelIdx).trim();
        }

        Matcher lollipopLeft = LOLLIPOP_LEFT.matcher(stripped);
        if (lollipopLeft.matches()) {
            return new MmdRelationNode(
                    lollipopLeft.group(1), "()--", lollipopLeft.group(2), label, lineNumber);
        }

        Matcher lollipopRight = LOLLIPOP_RIGHT.matcher(stripped);
        if (lollipopRight.matches()) {
            return new MmdRelationNode(
                    lollipopRight.group(1), "--()", lollipopRight.group(2), label, lineNumber);
        }

        Match best = null;
        for (String arrow : MmdRelationTypes.ARROWS) {
            if ("()--".equals(arrow) || "--()".equals(arrow)) {
                continue;
            }
            int searchFrom = 0;
            while (searchFrom <= stripped.length()) {
                int idx = stripped.indexOf(arrow, searchFrom);
                if (idx < 0) {
                    break;
                }
                Endpoint left = parseEndpoint(stripped.substring(0, idx).trim());
                Endpoint right = parseEndpoint(stripped.substring(idx + arrow.length()).trim());
                if (left != null && right != null) {
                    if (best == null || arrow.length() > best.arrow.length()) {
                        best = new Match(arrow, left, right);
                    }
                }
                searchFrom = idx + 1;
            }
        }

        if (best == null) {
            return null;
        }

        return new MmdRelationNode(
                best.left.name(),
                best.arrow(),
                best.right.name(),
                label,
                best.left.cardinality(),
                best.right.cardinality(),
                lineNumber);
    }

    private static Endpoint parseEndpoint(String part) {
        if (part == null || part.isBlank()) {
            return null;
        }
        Matcher classFirst = ENDPOINT_CLASS_FIRST.matcher(part);
        if (classFirst.matches()) {
            return new Endpoint(classFirst.group(1), classFirst.group(2));
        }
        Matcher cardFirst = ENDPOINT_CARD_FIRST.matcher(part);
        if (cardFirst.matches()) {
            return new Endpoint(cardFirst.group(2), cardFirst.group(1));
        }
        return null;
    }

    private record Endpoint(String name, String cardinality) {}

    private record Match(String arrow, Endpoint left, Endpoint right) {}
}
