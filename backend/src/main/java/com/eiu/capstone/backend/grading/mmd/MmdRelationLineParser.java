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

        Match best = matchKnownArrow(stripped);
        if (best == null) {
            best = matchUnknownRelationArrow(stripped);
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

    private static Match matchKnownArrow(String stripped) {
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
                Match candidate = endpointsAround(stripped, idx, arrow.length(), arrow);
                if (candidate != null && (best == null || arrow.length() > best.arrow.length())) {
                    best = candidate;
                }
                searchFrom = idx + 1;
            }
        }
        return best;
    }

    /**
     * Accepts Mermaid-shaped arrows outside the graded table (e.g. {@code ..o}) as relation lines
     * so the diagram still parses; canonical type becomes {@link MmdRelationTypes#UNKNOWN}.
     */
    private static Match matchUnknownRelationArrow(String stripped) {
        Match best = null;
        for (int i = 0; i < stripped.length() - 1; i++) {
            char a = stripped.charAt(i);
            char b = stripped.charAt(i + 1);
            boolean solid = a == '-' && b == '-';
            boolean dashed = a == '.' && b == '.';
            if (!solid && !dashed) {
                continue;
            }
            int start = i;
            int end = i + 2;
            while (start > 0 && isArrowChar(stripped.charAt(start - 1))) {
                start--;
            }
            while (end < stripped.length() && isArrowChar(stripped.charAt(end))) {
                end++;
            }
            String arrow = stripped.substring(start, end);
            if (arrow.isBlank() || MmdRelationTypes.isCanonicalArrow(arrow)) {
                continue;
            }
            Match candidate = endpointsAround(stripped, start, arrow.length(), arrow);
            if (candidate != null && (best == null || arrow.length() > best.arrow.length())) {
                best = candidate;
            }
        }
        return best;
    }

    private static Match endpointsAround(String stripped, int arrowStart, int arrowLength, String arrow) {
        Endpoint left = parseEndpoint(stripped.substring(0, arrowStart).trim());
        Endpoint right = parseEndpoint(stripped.substring(arrowStart + arrowLength).trim());
        if (left == null || right == null) {
            return null;
        }
        return new Match(arrow, left, right);
    }

    private static boolean isArrowChar(char c) {
        return c == '<' || c == '>' || c == '|' || c == '*' || c == 'o'
                || c == '.' || c == '-' || c == '(' || c == ')';
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
