package com.eiu.capstone.backend.grading;

import java.util.regex.Pattern;

public final class MmdTypeEquivalence {

    private static final Pattern TILDE_GENERIC = Pattern.compile("([A-Za-z_][\\w]*(?:\\.[A-Za-z_][\\w]*)*)~([^~]+)~");

    private MmdTypeEquivalence() {}

    public static boolean typesMatch(String expected, String actual) {
        if (expected == null && actual == null) return true;
        if (expected == null || actual == null) return false;
        return normalize(expected).equals(normalize(actual));
    }

    public static String normalize(String type) {
        if (type == null) return "";
        String trimmed = type.trim();
        String withAngles = convertTildes(trimmed);
        return canonicalizePrimitiveWrapper(canonicalizeCollection(withAngles));
    }

    private static String canonicalizePrimitiveWrapper(String type) {
        return switch (type) {
            case "int" -> "Integer";
            case "long" -> "Long";
            case "double" -> "Double";
            case "float" -> "Float";
            case "boolean" -> "Boolean";
            case "short" -> "Short";
            case "byte" -> "Byte";
            case "char" -> "Character";
            default -> type;
        };
    }

    private static String convertTildes(String type) {
        String result = type;
        String previous;
        do {
            previous = result;
            result = TILDE_GENERIC.matcher(result).replaceAll("$1<$2>");
        } while (!result.equals(previous));
        return result.replace('~', '<').replace(">>", ">");
    }

    private static String canonicalizeCollection(String type) {
        if (type.startsWith("ArrayList<") && type.endsWith(">")) {
            return "List<" + type.substring("ArrayList<".length(), type.length() - 1) + ">";
        }
        if (type.startsWith("LinkedList<") && type.endsWith(">")) {
            return "List<" + type.substring("LinkedList<".length(), type.length() - 1) + ">";
        }
        if (type.contains("HashMap<")) {
            return normalizeHashMapGenerics(type);
        }
        return type;
    }

    private static String normalizeHashMapGenerics(String type) {
        int start = type.indexOf('<');
        int end = type.lastIndexOf('>');
        if (start < 0 || end <= start) return type;
        String inner = type.substring(start + 1, end);
        String[] parts = splitGenericArgs(inner);
        if (parts.length != 2) return type;
        return "HashMap<" + parts[0].trim() + ", " + normalizePrimitiveInGeneric(parts[1].trim()) + ">";
    }

    private static String normalizePrimitiveInGeneric(String type) {
        return switch (type) {
            case "int" -> "Integer";
            case "long" -> "Long";
            case "double" -> "Double";
            case "float" -> "Float";
            case "boolean" -> "Boolean";
            case "short" -> "Short";
            case "byte" -> "Byte";
            case "char" -> "Character";
            default -> type;
        };
    }

    private static String[] splitGenericArgs(String inner) {
        int depth = 0;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '<') depth++;
            else if (c == '>') depth--;
            else if (c == ',' && depth == 0) {
                return new String[] { inner.substring(0, i), inner.substring(i + 1) };
            }
        }
        return new String[] { inner };
    }
}
