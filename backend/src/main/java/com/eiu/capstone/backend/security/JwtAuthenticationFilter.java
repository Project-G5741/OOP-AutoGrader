package com.eiu.capstone.backend.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.web.filter.OncePerRequestFilter;

import com.eiu.capstone.backend.service.JwtService;
import com.eiu.capstone.backend.service.SessionValidityService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Profile("!desktop")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SessionValidityService sessionValidityService;

    public JwtAuthenticationFilter(JwtService jwtService, SessionValidityService sessionValidityService) {
        this.jwtService = jwtService;
        this.sessionValidityService = sessionValidityService;
    }

    /**
     * Query-param JWT is allowed only for the student practice-folder download so the browser
     * can own the transfer (native progress UI). Authorization header still wins when both are sent.
     */
    private static final String DESKTOP_PRACTICE_BUNDLE_PATH = "/api/students/desktop-practice-bundle";
    private static final String ACCESS_TOKEN_QUERY = "access_token";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String rawToken = resolveBearerOrDownloadQueryToken(request);
        if (rawToken != null) {
            try {
                Claims claims = jwtService.parseToken(rawToken);
                String email = claims.get("email", String.class);
                Integer sessionVersion = readSessionVersion(claims);
                if (sessionValidityService.isSessionValid(email, sessionVersion)) {
                    JwtUserPrincipal principal = new JwtUserPrincipal(
                            email,
                            claims.get("irn", String.class),
                            extractRoles(claims));
                    Collection<SimpleGrantedAuthority> authorities = principal.roles().stream()
                            .map(name -> new SimpleGrantedAuthority("ROLE_" + name))
                            .toList();
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private static String resolveBearerOrDownloadQueryToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String bearer = header.substring(7).trim();
            if (!bearer.isEmpty()) {
                return bearer;
            }
        }
        if (!isDesktopPracticeBundleGet(request)) {
            return null;
        }
        String queryToken = request.getParameter(ACCESS_TOKEN_QUERY);
        if (queryToken == null || queryToken.isBlank()) {
            return null;
        }
        return queryToken.trim();
    }

    private static boolean isDesktopPracticeBundleGet(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return DESKTOP_PRACTICE_BUNDLE_PATH.equals(path);
    }

    private static Integer readSessionVersion(Claims claims) {
        Object raw = claims.get("sv");
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private static List<String> extractRoles(Claims claims) {
        Object raw = claims.get("roles");
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        List<String> roles = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String name) {
                roles.add(name);
            }
        }
        return roles;
    }
}
