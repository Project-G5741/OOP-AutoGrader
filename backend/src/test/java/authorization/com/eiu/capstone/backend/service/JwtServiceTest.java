package authorization.com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.service.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;

class JwtServiceTest {

    private static final String STABLE_SECRET = "a".repeat(32);

    @Test
    void parseToken_afterReinitializationWithSameSecret_stillValidates() {
        JwtService issuer = new JwtService(STABLE_SECRET, 3600);
        String token = issuer.createToken("student@eiu.edu.vn", "Student", "eiu.edu.vn",
                List.of("STUDENT"), "IRN001");

        JwtService verifier = new JwtService(STABLE_SECRET, 3600);
        Claims claims = verifier.parseToken(token);

        assertEquals("student@eiu.edu.vn", claims.get("email", String.class));
        assertEquals("IRN001", claims.get("irn", String.class));
        assertEquals(0, claims.get("sv", Integer.class));
    }

    @Test
    void createToken_includesSessionVersionClaim() {
        JwtService issuer = new JwtService(STABLE_SECRET, 3600);
        String token = issuer.createToken("student@eiu.edu.vn", "Student", "eiu.edu.vn",
                List.of("STUDENT"), "IRN001", 7);
        Claims claims = issuer.parseToken(token);
        assertEquals(7, claims.get("sv", Integer.class));
    }

    @Test
    void constructor_blankSecret_failsFast() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtService("   ", 3600));
        assertTrue(ex.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void constructor_missingSecret_failsFast() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtService(null, 3600));
        assertTrue(ex.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void constructor_shortSecret_failsFast() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtService("too-short", 3600));
        assertTrue(ex.getMessage().contains("32"));
    }

    @Test
    void createMascotPracticeToken_carriesDedicatedClaimsWithoutRolesOrSv() {
        JwtService issuer = new JwtService(STABLE_SECRET, 3600);
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Instant expires = Instant.now().plusSeconds(90L * 24L * 60L * 60L);
        String token = issuer.createMascotPracticeToken("student@eiu.edu.vn", userId, "jti-1", expires);
        Claims claims = issuer.parseToken(token);

        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("student@eiu.edu.vn", claims.get("email", String.class));
        assertEquals(JwtService.MASCOT_PRACTICE_TYP, claims.get("typ", String.class));
        assertEquals(JwtService.MASCOT_PRACTICE_SCOPE, claims.get("scope", String.class));
        assertEquals("jti-1", claims.get("jti", String.class));
        assertEquals(JwtService.MASCOT_PRACTICE_AUD, claims.getAudience());
        assertNull(claims.get("roles"));
        assertNull(claims.get("sv"));
        assertTrue(JwtService.isMascotPractice(claims));
        assertFalse(JwtService.isMascotPractice(issuer.parseToken(
                issuer.createToken("student@eiu.edu.vn", "Student", "eiu.edu.vn",
                        List.of("STUDENT"), "IRN001"))));
    }
}
