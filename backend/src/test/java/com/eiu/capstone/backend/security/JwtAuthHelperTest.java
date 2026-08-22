package com.eiu.capstone.backend.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.service.JwtService;

import io.jsonwebtoken.Claims;

@ExtendWith(MockitoExtension.class)
class JwtAuthHelperTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private Claims claims;

    private JwtAuthHelper jwtAuthHelper;

    @BeforeEach
    void setUp() {
        jwtAuthHelper = new JwtAuthHelper(jwtService, userAccountRepository);
    }

    @Test
    void parseBearerToken_missingHeader_throws401() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> jwtAuthHelper.parseBearerToken(null));
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void requireRole_lecturerRole_passes() {
        when(claims.get("roles")).thenReturn(List.of("LECTURER"));
        assertDoesNotThrow(() -> jwtAuthHelper.requireRole(claims, "LECTURER"));
    }

    @Test
    void requireRole_studentOnly_throws403() {
        when(claims.get("roles")).thenReturn(List.of("STUDENT"));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> jwtAuthHelper.requireRole(claims, "LECTURER"));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void requireLecturer_missingHeader_throws401() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> jwtAuthHelper.requireLecturer(null));
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void isStudentOnly_studentWithoutLecturer_true() {
        when(claims.get("roles")).thenReturn(List.of("STUDENT"));
        assertTrue(jwtAuthHelper.isStudentOnly(claims));
    }

    @Test
    void isStudentOnly_dualRole_false() {
        when(claims.get("roles")).thenReturn(List.of("STUDENT", "LECTURER"));
        assertFalse(jwtAuthHelper.isStudentOnly(claims));
    }

    @Test
    void parseBearerToken_validBearer_returnsClaims() {
        when(jwtService.parseToken("token")).thenReturn(claims);
        when(claims.get("email", String.class)).thenReturn("lecturer@eiu.edu.vn");

        Claims parsed = jwtAuthHelper.parseBearerToken("Bearer token");

        assertEquals("lecturer@eiu.edu.vn", parsed.get("email", String.class));
    }

    @Test
    void resolveStudentScope_lecturerWithoutStudentId_returnsNull() {
        when(claims.get("email", String.class)).thenReturn("lecturer@eiu.edu.vn");
        when(claims.get("roles")).thenReturn(List.of("LECTURER"));
        UserAccount user = mock(UserAccount.class);
        when(user.getIsActive()).thenReturn(true);
        when(userAccountRepository.findByEmail("lecturer@eiu.edu.vn")).thenReturn(Optional.of(user));

        UUID scoped = jwtAuthHelper.resolveStudentScope(claims, null);

        assertEquals(null, scoped);
    }

    @Test
    void resolveStudentScope_studentDefaultsToSelf() {
        UUID userId = UUID.randomUUID();
        UserAccount user = mock(UserAccount.class);
        when(user.getId()).thenReturn(userId);
        when(user.getIsActive()).thenReturn(true);
        when(claims.get("email", String.class)).thenReturn("student@eiu.edu.vn");
        when(claims.get("roles")).thenReturn(List.of("STUDENT"));
        when(userAccountRepository.findByEmail("student@eiu.edu.vn")).thenReturn(Optional.of(user));

        UUID scoped = jwtAuthHelper.resolveStudentScope(claims, null);

        assertEquals(userId, scoped);
    }

    @Test
    void resolveStudentScope_studentCannotReadOther() {
        UUID userId = UUID.randomUUID();
        UserAccount user = mock(UserAccount.class);
        when(user.getId()).thenReturn(userId);
        when(user.getIsActive()).thenReturn(true);
        when(claims.get("email", String.class)).thenReturn("student@eiu.edu.vn");
        when(claims.get("roles")).thenReturn(List.of("STUDENT"));
        when(userAccountRepository.findByEmail("student@eiu.edu.vn")).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> jwtAuthHelper.resolveStudentScope(claims, UUID.randomUUID()));
        assertEquals(403, ex.getStatusCode().value());
    }
}
