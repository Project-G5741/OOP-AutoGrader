package unit.com.eiu.capstone.backend.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.config.StudentDenyStartupValidator;

class StudentDenyStartupValidatorTest {

    @Test
    void localDefaultsPass() {
        assertDoesNotThrow(() -> StudentDenyStartupValidator.validate(false, false));
    }

    @Test
    void enabledPasses() {
        assertDoesNotThrow(() -> StudentDenyStartupValidator.validate(true, true));
    }

    @Test
    void requiredButDisabledFails() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> StudentDenyStartupValidator.validate(false, true));
        assertTrue(ex.getMessage().contains("STUDENT_DENY_REQUIRED"));
    }
}
