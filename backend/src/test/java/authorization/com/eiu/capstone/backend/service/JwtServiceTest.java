package authorization.com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.service.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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
}
