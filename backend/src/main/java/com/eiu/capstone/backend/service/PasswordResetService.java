package com.eiu.capstone.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.model.PasswordResetToken;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.PasswordResetTokenRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;

import jakarta.transaction.Transactional;

@Service
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final List<String> KNOWN_FRONTEND_ORIGINS = List.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://localhost:5174",
            "http://127.0.0.1:5174",
            "https://oop-autograder.vercel.app");

    private final UserAccountRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetEmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final int expiryMinutes;
    private final String defaultFrontendUrl;
    private final Set<String> allowedResetOrigins;

    public PasswordResetService(
            UserAccountRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordResetEmailService emailService,
            PasswordEncoder passwordEncoder,
            @Value("${app.password-reset.expiry-minutes:15}") int expiryMinutes,
            @Value("${app.password-reset.frontend-url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.expiryMinutes = expiryMinutes;
        this.defaultFrontendUrl = resolveConfiguredFrontendUrl(frontendUrl);
        this.allowedResetOrigins = buildAllowedResetOrigins(frontendUrl, this.defaultFrontendUrl);
    }

    @Transactional
    public void requestReset(String email) {
        requestReset(email, null);
    }

    @Transactional
    public void requestReset(String email, String requestOrigin) {
        String normalizedEmail = normalizeEmail(email);
        UserAccount user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No account found for this email"));
        if (!user.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No account found for this email");
        }

        tokenRepository.deleteByUser_IdAndUsedAtIsNull(user.getId());

        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);
        OffsetDateTime now = OffsetDateTime.now();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(now.plusMinutes(expiryMinutes));
        resetToken.setCreatedAt(now);
        tokenRepository.save(resetToken);

        String resetBaseUrl = resolveResetBaseUrl(requestOrigin);
        String resetUrl = resetBaseUrl + "?resetToken=" + rawToken;
        try {
            emailService.sendResetLink(user.getEmail(), resetUrl);
        } catch (MailException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Unable to send reset email. Please try again later.");
        }
    }

    @Transactional
    public void completeReset(String rawToken, String newPassword, String confirmPassword) {
        validatePasswordPair(newPassword, confirmPassword);

        String tokenHash = hashToken(rawToken);
        PasswordResetToken resetToken = tokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid or expired reset link"));

        OffsetDateTime now = OffsetDateTime.now();
        if (resetToken.getExpiresAt().isBefore(now)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Reset link has expired. Please request a new one.");
        }

        UserAccount user = resetToken.getUser();
        if (user == null || !user.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No account found for this email");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsedAt(now);
        tokenRepository.save(resetToken);
    }

    static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase();
    }

    static void validatePasswordPair(String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password is required");
        }
        if (confirmPassword == null || confirmPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirm password is required");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }
        if (newPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 6 characters");
        }
        if (newPassword.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be less than 100 characters");
        }
    }

    static String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private String resolveResetBaseUrl(String requestOrigin) {
        if (requestOrigin != null && !requestOrigin.isBlank()) {
            String normalized = stripTrailingSlash(requestOrigin);
            if (allowedResetOrigins.contains(normalized)) {
                return normalized;
            }
        }
        return defaultFrontendUrl;
    }

    private static String resolveConfiguredFrontendUrl(String configured) {
        if (configured == null || configured.isBlank()) {
            return "http://localhost:5173";
        }
        return stripTrailingSlash(configured.split(",")[0]);
    }

    private static Set<String> buildAllowedResetOrigins(String configured, String defaultUrl) {
        Set<String> origins = new HashSet<>(KNOWN_FRONTEND_ORIGINS);
        origins.add(defaultUrl);
        if (configured != null && !configured.isBlank()) {
            for (String part : configured.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    origins.add(stripTrailingSlash(trimmed));
                }
            }
        }
        return Set.copyOf(origins);
    }

    private static String stripTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:5173";
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
