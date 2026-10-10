package unit.com.eiu.capstone.backend.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.security.BurstLimitExemptions;

class BurstLimitExemptionsTest {

    @Test
    void bulkGradePost_isExempt() {
        UUID labId = UUID.randomUUID();
        assertTrue(BurstLimitExemptions.isExempt(
                "/api/lecturer/labs/" + labId + "/bulk-grade", "POST"));
        assertFalse(BurstLimitExemptions.isExempt(
                "/api/lecturer/labs/" + labId + "/bulk-grade", "GET"));
    }

    @Test
    void termImportPost_isExempt() {
        UUID termId = UUID.randomUUID();
        assertTrue(BurstLimitExemptions.isExempt(
                "/api/lecturer/terms/" + termId + "/students/import", "POST"));
    }

    @Test
    void dryRunPost_isExempt() {
        UUID labId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        assertTrue(BurstLimitExemptions.isExempt(
                "/api/lecturer/labs/" + labId + "/challenges/" + challengeId + "/testcases/dry-run",
                "POST"));
    }

    @Test
    void solutionImportPost_isExempt() {
        UUID labId = UUID.randomUUID();
        assertTrue(BurstLimitExemptions.isExempt(
                "/api/lecturer/labs/" + labId + "/solution-import", "POST"));
        assertFalse(BurstLimitExemptions.isExempt(
                "/api/lecturer/labs/" + labId + "/solution-import", "GET"));
    }

    @Test
    void ordinaryUpload_isNotExempt() {
        assertFalse(BurstLimitExemptions.isExempt("/api/submissions/upload", "POST"));
    }
}
