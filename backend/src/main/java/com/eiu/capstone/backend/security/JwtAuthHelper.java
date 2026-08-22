package com.eiu.capstone.backend.security;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.service.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

@Component
public class JwtAuthHelper {

    private final JwtService jwtService;
    private final UserAccountRepository userAccountRepository;

    public JwtAuthHelper(JwtService jwtService, UserAccountRepository userAccountRepository) {
        this.jwtService = jwtService;
        this.userAccountRepository = userAccountRepository;
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

    public UserAccount requireActiveUser(Claims claims) {
        String email = claims.get("email", String.class);
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
        if (!user.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is inactive");
        }
        return user;
    }

    public UserAccount requireActiveUser(String authHeader) {
        return requireActiveUser(parseBearerToken(authHeader));
    }

    public boolean hasRole(Claims claims, String requiredRole) {
        if (claims == null) {
            return false;
        }
        Object rawRoles = claims.get("roles");
        if (!(rawRoles instanceof List<?> roles) || roles.isEmpty()) {
            return false;
        }
        String normalizedRequired = normalizeRoleName(requiredRole);
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(this::normalizeRoleName)
                .anyMatch(normalizedRequired::equals);
    }

    public void requireRole(Claims claims, String requiredRole) {
        if (!hasRole(claims, requiredRole)) {
            if (claims == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
    }

    public void requireLecturer(String authHeader) {
        requireRole(parseBearerToken(authHeader), "LECTURER");
    }

    public void requireStudent(String authHeader) {
        Claims claims = parseBearerToken(authHeader);
        requireActiveUser(claims);
        if (!hasRole(claims, "STUDENT")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student access required");
        }
    }

    /**
     * Students may only access their own data; lecturers may read any studentId.
     * When studentId is omitted, students default to self; lecturers must supply studentId.
     */
    public UUID resolveStudentScope(Claims claims, UUID requestedStudentId) {
        UserAccount user = requireActiveUser(claims);
        boolean lecturer = hasRole(claims, "LECTURER");
        boolean student = hasRole(claims, "STUDENT");

        if (!lecturer && !student) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }

        if (requestedStudentId == null) {
            if (student && !lecturer) {
                return user.getId();
            }
            if (lecturer) {
                return null;
            }
            if (student) {
                return user.getId();
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studentId is required");
        }

        if (!requestedStudentId.equals(user.getId()) && !lecturer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot access another student's data");
        }
        return requestedStudentId;
    }

    public UUID resolveStudentScope(String authHeader, UUID requestedStudentId) {
        return resolveStudentScope(parseBearerToken(authHeader), requestedStudentId);
    }

    public boolean isStudentOnly(Claims claims) {
        return hasRole(claims, "STUDENT") && !hasRole(claims, "LECTURER");
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
