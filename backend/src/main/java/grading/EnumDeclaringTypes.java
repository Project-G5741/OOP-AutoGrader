package com.eiu.capstone.backend.grading;

import java.util.Locale;

/**
 * ENUM class fields are constants. Upload, bulk grade, and desktop practice
 * score them by name and ignore a stored type or scope.
 */
public final class EnumDeclaringTypes {

    private EnumDeclaringTypes() {}

    public static boolean isEnum(String declaringType) {
        if (declaringType == null || declaringType.isBlank()) {
            return false;
        }
        String upper = declaringType.trim().toUpperCase(Locale.ROOT);
        return "ENUM".equals(upper) || "ENUMERATE".equals(upper) || "ENUMERATION".equals(upper);
    }
}
