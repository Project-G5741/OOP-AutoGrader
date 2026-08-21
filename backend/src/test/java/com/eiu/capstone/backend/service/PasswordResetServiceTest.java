package com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.model.PasswordResetToken;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.PasswordResetTokenRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordResetServiceTest {

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordResetEmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private PasswordResetService passwordResetService;

    private UserAccount activeUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                userRepository,
                tokenRepository,
                emailService,
                passwordEncoder,
                15,
                "http://localhost:5173");

        userId = UUID.randomUUID();
        activeUser = mock(UserAccount.class);
        when(activeUser.getId()).thenReturn(userId);
        when(activeUser.getEmail()).thenReturn("student@eiu.edu.vn");
        when(activeUser.getIsActive()).thenReturn(true);
    }

    @Test
    void requestReset_unknownEmail_throwsNotFound() {
        when(userRepository.findByEmailIgnoreCase("missing@eiu.edu.vn")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> passwordResetService.requestReset("missing@eiu.edu.vn"));

        assertEquals(404, ex.getStatusCode().value());
        verify(emailService, never()).sendResetLink(any(), any());
    }

    @Test
    void requestReset_validEmail_sendsLinkAndStoresToken() {
        when(userRepository.findByEmailIgnoreCase("student@eiu.edu.vn")).thenReturn(Optional.of(activeUser));

        passwordResetService.requestReset("Student@eiu.edu.vn ");

        verify(tokenRepository).deleteByUser_IdAndUsedAtIsNull(userId);
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken saved = tokenCaptor.getValue();
        assertEquals(activeUser, saved.getUser());
        verify(emailService).sendResetLink(
                eq("student@eiu.edu.vn"),
                org.mockito.ArgumentMatchers.startsWith("http://localhost:5173?resetToken="));
    }

    @Test
    void requestReset_withProductionOrigin_sendsProductionLink() {
        when(userRepository.findByEmailIgnoreCase("student@eiu.edu.vn")).thenReturn(Optional.of(activeUser));

        passwordResetService.requestReset("student@eiu.edu.vn", "https://oop-autograder.vercel.app");

        verify(emailService).sendResetLink(
                eq("student@eiu.edu.vn"),
                org.mockito.ArgumentMatchers.startsWith("https://oop-autograder.vercel.app?resetToken="));
    }

    @Test
    void completeReset_mismatchedPasswords_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> passwordResetService.completeReset("token", "password1", "password2"));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void completeReset_expiredToken_throwsBadRequest() {
        String rawToken = PasswordResetService.generateRawToken();
        String tokenHash = PasswordResetService.hashToken(rawToken);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(activeUser);
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
        resetToken.setCreatedAt(OffsetDateTime.now().minusMinutes(16));

        when(tokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash)).thenReturn(Optional.of(resetToken));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> passwordResetService.completeReset(rawToken, "newpass", "newpass"));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Reset link has expired. Please request a new one.", ex.getReason());
    }

    @Test
    void completeReset_inactiveUser_throwsNotFound() {
        String rawToken = PasswordResetService.generateRawToken();
        String tokenHash = PasswordResetService.hashToken(rawToken);

        when(activeUser.getIsActive()).thenReturn(false);
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(activeUser);
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
        resetToken.setCreatedAt(OffsetDateTime.now());

        when(tokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash)).thenReturn(Optional.of(resetToken));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> passwordResetService.completeReset(rawToken, "newpass", "newpass"));

        assertEquals(404, ex.getStatusCode().value());
        verify(userRepository, never()).save(any());
        verify(activeUser, never()).setPasswordHash(any());
    }

    @Test
    void completeReset_validToken_updatesPasswordAndMarksUsed() {
        String rawToken = PasswordResetService.generateRawToken();
        String tokenHash = PasswordResetService.hashToken(rawToken);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(activeUser);
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
        resetToken.setCreatedAt(OffsetDateTime.now());

        when(tokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash)).thenReturn(Optional.of(resetToken));
        when(activeUser.getPasswordHash()).thenReturn("old-hash");
        when(passwordEncoder.encode("newpass")).thenReturn("encoded-new");

        passwordResetService.completeReset(rawToken, "newpass", "newpass");

        verify(activeUser).setPasswordHash("encoded-new");
        verify(userRepository).save(activeUser);
        verify(tokenRepository).save(resetToken);
    }
}
