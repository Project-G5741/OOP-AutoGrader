package com.eiu.capstone.backend.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.service.JwtService;

import io.jsonwebtoken.Claims;

@ExtendWith(MockitoExtension.class)
class JwtAuthHelperTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private Claims claims;

    private JwtAuthHelper jwtAuthHelper;

    @BeforeEach
    void setUp() {
        jwtAuthHelper = new JwtAuthHelper(jwtService);
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
    void parseBearerToken_validBearer_returnsClaims() {
        when(jwtService.parseToken("token")).thenReturn(claims);
        when(claims.get("email", String.class)).thenReturn("lecturer@eiu.edu.vn");

        Claims parsed = jwtAuthHelper.parseBearerToken("Bearer token");

        assertEquals("lecturer@eiu.edu.vn", parsed.get("email", String.class));
    }
}
