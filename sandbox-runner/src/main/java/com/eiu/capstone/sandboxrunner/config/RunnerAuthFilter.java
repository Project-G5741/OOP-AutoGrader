package com.eiu.capstone.sandboxrunner.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RunnerAuthFilter extends OncePerRequestFilter {

    private final RunnerProperties properties;

    public RunnerAuthFilter(RunnerProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/health") || path.equals("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String expected = properties.getToken();
        if (expected.isBlank()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "SANDBOX_RUNNER_TOKEN not configured");
            return;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.equals("Bearer " + expected)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing Bearer token");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
