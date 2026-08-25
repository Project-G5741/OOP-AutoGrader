package com.eiu.capstone.backend.security;

public final class JwtRoleNames {

    public static final String STUDENT = "STUDENT";
    public static final String LECTURER = "LECTURER";

    private JwtRoleNames() {}

    public static String normalize(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return "";
        }
        String normalized = roleName.trim().toUpperCase();
        if ("TEACHER".equals(normalized) || LECTURER.equals(normalized)) {
            return LECTURER;
        }
        return normalized;
    }
}
