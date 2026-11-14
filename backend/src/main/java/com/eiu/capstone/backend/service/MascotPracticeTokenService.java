package com.eiu.capstone.backend.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.MascotPracticeTokenView;
import com.eiu.capstone.backend.model.StudentMascotPracticeToken;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.StudentMascotPracticeTokenRepository;
import com.eiu.capstone.backend.security.JwtRoleNames;
import com.eiu.capstone.backend.security.JwtUserPrincipal;

@Service
@Profile("!desktop")
public class MascotPracticeTokenService {

    private final StudentMascotPracticeTokenRepository tokens;
    private final JwtService jwtService;

    public MascotPracticeTokenService(StudentMascotPracticeTokenRepository tokens, JwtService jwtService) {
        this.tokens = tokens;
        this.jwtService = jwtService;
    }

    public static void requireStudentOrAdmin(JwtUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        if (!principal.hasRole(JwtRoleNames.STUDENT) && !principal.hasRole(JwtRoleNames.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
    }

    @Transactional
    public MascotPracticeTokenView issue(UserAccount user, boolean regenerate) {
        Instant now = Instant.now();
        StudentMascotPracticeToken row = tokens.findById(user.getId()).orElse(null);
        if (!regenerate && row != null && row.getExpiresAt().isAfter(now)) {
            return new MascotPracticeTokenView(row.getCompactToken(), row.getExpiresAt());
        }
        return replace(user, now);
    }

    @Transactional
    public void deleteForUser(UUID userId) {
        if (userId != null) {
            tokens.deleteById(userId);
        }
    }

    public boolean isLiveJti(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return tokens.findByJti(jti)
                .filter(row -> row.getExpiresAt().isAfter(Instant.now()))
                .isPresent();
    }

    private MascotPracticeTokenView replace(UserAccount user, Instant now) {
        Instant expiresAt = now.plusSeconds(JwtService.MASCOT_PRACTICE_TTL_SECONDS);
        String jti = UUID.randomUUID().toString();
        String compact = jwtService.createMascotPracticeToken(user.getEmail(), user.getId(), jti, expiresAt);
        StudentMascotPracticeToken row = tokens.findById(user.getId()).orElseGet(StudentMascotPracticeToken::new);
        row.setUserId(user.getId());
        row.setJti(jti);
        row.setCompactToken(compact);
        row.setExpiresAt(expiresAt);
        row.setCreatedAt(now);
        tokens.save(row);
        return new MascotPracticeTokenView(compact, expiresAt);
    }
}
