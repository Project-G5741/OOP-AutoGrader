package unit.com.eiu.capstone.backend.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.config.SandboxStartupValidator;

class SandboxStartupValidatorTest {

    @Test
    void localDefaultsPass() {
        assertDoesNotThrow(() -> SandboxStartupValidator.validate(false, false, "", ""));
    }

    @Test
    void enabledWithoutCredentialsFails() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> SandboxStartupValidator.validate(true, false, "", "token"));
        assertTrue(ex.getMessage().contains("SANDBOX_RUNNER_URL"));
    }

    @Test
    void enabledWithoutTokenFails() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> SandboxStartupValidator.validate(true, false, "http://localhost:8090", "  "));
        assertTrue(ex.getMessage().contains("SANDBOX_RUNNER_TOKEN"));
    }

    @Test
    void enabledWithCredentialsPasses() {
        assertDoesNotThrow(() -> SandboxStartupValidator.validate(
                true, false, "http://localhost:8090", "dev-token"));
    }

    @Test
    void requiredButDisabledFails() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> SandboxStartupValidator.validate(false, true, "http://x", "tok"));
        assertTrue(ex.getMessage().contains("SANDBOX_REQUIRED"));
    }

    @Test
    void requiredEnabledWithCredentialsPasses() {
        assertDoesNotThrow(() -> SandboxStartupValidator.validate(
                true, true, "https://sandbox.example.com", "secret"));
    }

    @Test
    void requiredEnabledMissingUrlFails() {
        assertThrows(IllegalStateException.class,
                () -> SandboxStartupValidator.validate(true, true, "", "secret"));
    }
}
