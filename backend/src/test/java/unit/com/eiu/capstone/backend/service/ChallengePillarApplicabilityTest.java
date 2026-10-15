package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.service.ChallengePillarApplicability;

class ChallengePillarApplicabilityTest {

    @Test
    void requireAtLeastOne_allowsClassOnly() {
        assertDoesNotThrow(() -> ChallengePillarApplicability.requireAtLeastOne(true, false, 0, "P1"));
    }

    @Test
    void requireAtLeastOne_allowsMmdOnly() {
        assertDoesNotThrow(() -> ChallengePillarApplicability.requireAtLeastOne(false, true, 0, "P1"));
    }

    @Test
    void requireAtLeastOne_allowsOtOnly() {
        assertDoesNotThrow(() -> ChallengePillarApplicability.requireAtLeastOne(false, false, 1, "P1"));
    }

    @Test
    void requireAtLeastOne_rejectsZeroPillars() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> ChallengePillarApplicability.requireAtLeastOne(false, false, 0, "Problem 2"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals(
                "\"Problem 2\" must keep at least one scoring pillar "
                        + "(Requires Declaration Test, Requires MMD diagram, or operational testcases).",
                ex.getReason());
    }
}
