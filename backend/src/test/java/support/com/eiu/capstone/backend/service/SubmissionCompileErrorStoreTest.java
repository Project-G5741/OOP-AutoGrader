package support.com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.service.*;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmissionCompileErrorStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void roundTripPerClassErrors() {
        SubmissionCompileErrorStore store = new SubmissionCompileErrorStore(tempDir.toString());
        UUID submissionId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        ChallengeCompileErrors errors = ChallengeCompileErrors.perClass(
                Map.of("Student", "ERROR: line 1: ';' expected"));

        store.save(submissionId, Map.of(challengeId, errors));

        ChallengeCompileErrors loaded = store.get(submissionId, challengeId);
        assertNull(loaded.catastrophic());
        assertEquals("ERROR: line 1: ';' expected", loaded.byClassName().get("Student"));
    }

    @Test
    void readsLegacyStringJsonAsCatastrophic() throws Exception {
        SubmissionCompileErrorStore store = new SubmissionCompileErrorStore(tempDir.toString());
        UUID submissionId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Path file = tempDir.resolve("_compile_errors").resolve(submissionId + ".json");
        java.nio.file.Files.createDirectories(file.getParent());
        java.nio.file.Files.writeString(file,
                "{\"" + challengeId + "\":\"Compilation failed:\\nERROR: line 1\"}");

        ChallengeCompileErrors loaded = store.get(submissionId, challengeId);
        assertTrue(loaded.catastrophic().contains("Compilation failed"));
        assertTrue(loaded.byClassName().isEmpty());
        assertEquals("Compilation failed:\nERROR: line 1", loaded.messageForClass("Anything"));
    }

    @Test
    void skipsBadChallengeKeyAndKeepsSibling() throws Exception {
        SubmissionCompileErrorStore store = new SubmissionCompileErrorStore(tempDir.toString());
        UUID submissionId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Path file = tempDir.resolve("_compile_errors").resolve(submissionId + ".json");
        java.nio.file.Files.createDirectories(file.getParent());
        java.nio.file.Files.writeString(file,
                "{\"not-a-uuid\":\"ignored\",\"" + challengeId
                        + "\":{\"catastrophic\":null,\"byClassName\":{\"Student\":\"err\"}}}");

        ChallengeCompileErrors loaded = store.get(submissionId, challengeId);
        assertEquals("err", loaded.byClassName().get("Student"));
    }
}
