package com.eiu.capstone.backend.security;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.service.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

@Component
public class JwtAuthHelper {

    private final JwtService jwtService;

    public JwtAuthHelper(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public Claims parseBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }
        try {
            return jwtService.parseToken(authHeader.substring(7));
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
    }

    public void requireRole(Claims claims, String requiredRole) {
        if (claims == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }
        Object rawRoles = claims.get("roles");
        if (!(rawRoles instanceof List<?> roles) || roles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
        String normalizedRequired = normalizeRoleName(requiredRole);
        boolean allowed = roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(this::normalizeRoleName)
                .anyMatch(normalizedRequired::equals);
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
    }

    private String normalizeRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return "";
        }
        String normalized = roleName.trim().toUpperCase();
        if ("TEACHER".equals(normalized) || "LECTURER".equals(normalized)) {
            return "LECTURER";
        }
        return normalized;
    }
}
