package com.eiu.capstone.backend.security;

import java.util.regex.Pattern;

import org.springframework.http.HttpMethod;

/**
 * API paths that intentionally issue many mutating requests in one user action
 * (sequential bulk grade, dry-run all, etc.). Still require normal JWT authorization;
 * only the burst counter is skipped.
 */
public final class BurstLimitExemptions {

    private static final Pattern TERM_STUDENT_IMPORT =
            Pattern.compile("^/api/lecturer/terms/[^/]+/students/import$");

    private BurstLimitExemptions() {
    }

    public static boolean isExempt(String normalizedPath, String httpMethod) {
        if (normalizedPath == null || httpMethod == null) {
            return false;
        }
        if (!HttpMethod.POST.matches(httpMethod)) {
            return false;
        }
        if (normalizedPath.endsWith("/bulk-grade")) {
            return true;
        }
        if ("/api/users/bulk".equals(normalizedPath)) {
            return true;
        }
        if ("/api/lecturer/labs/clone".equals(normalizedPath)) {
            return true;
        }
        if (normalizedPath.endsWith("/testcases/dry-run")) {
            return true;
        }
        return TERM_STUDENT_IMPORT.matcher(normalizedPath).matches();
    }
}
