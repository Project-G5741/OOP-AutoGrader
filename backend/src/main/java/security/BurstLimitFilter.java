package com.eiu.capstone.backend.security;

import java.io.IOException;
import java.util.Locale;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Web-only global API burst guard. Runs after {@link JwtAuthenticationFilter}.
 */
@Component
@Profile("!desktop")
public class BurstLimitFilter extends OncePerRequestFilter {

    private final BurstBudgetService burstBudgetService;

    public BurstLimitFilter(BurstBudgetService burstBudgetService) {
        this.burstBudgetService = burstBudgetService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Presence paths are already skipped above. Invalid/revoked Bearer on other
        // routes is IP-keyed so junk Authorization cannot bypass the budget (auth hammer).

        String key = resolveKey(request);
        BurstBudgetService.Decision decision;
        try {
            decision = burstBudgetService.tryConsume(key);
        } catch (RuntimeException ex) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Server Busy\",\"detail\":\"Burst guard unavailable\"}");
            return;
        }

        if (!decision.allowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Too Many Requests\",\"detail\":\"Slow down and try again\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static boolean shouldSkip(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = normalizedPath(request);
        if ("/".equals(path) && HttpMethod.GET.matches(request.getMethod())) {
            return true;
        }
        if ("/api/presence/count".equals(path) && HttpMethod.GET.matches(request.getMethod())) {
            return true;
        }
        if ("/api/presence/leave".equals(path) && HttpMethod.DELETE.matches(request.getMethod())) {
            return true;
        }
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            return true;
        }
        return !path.startsWith("/api/");
    }

    private String resolveKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtUserPrincipal principal) {
            String email = normalizeEmail(principal.email());
            if (email != null) {
                return "user:" + email;
            }
        }
        return "ip:" + clientIp(request);
    }

    static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Prefer container/proxy remote address after forward-headers. Do not trust a raw
     * client X-Forwarded-For when reading headers directly (spoofable on direct access).
     */
    static String clientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? "unknown" : remote;
    }

    private static String normalizedPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.isEmpty()) {
            return "/";
        }
        return path;
    }
}
