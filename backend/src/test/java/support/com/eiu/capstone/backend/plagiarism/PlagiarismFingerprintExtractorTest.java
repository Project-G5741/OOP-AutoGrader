package support.com.eiu.capstone.backend.plagiarism;

import com.eiu.capstone.backend.plagiarism.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class PlagiarismFingerprintExtractorTest {

    @Test
    void extract_readsGitConfigAndReflogWithoutReconstructingObjects() {
        MockMultipartFile config = new MockMultipartFile(
                "files",
                "123_Student/.git/config",
                "text/plain",
                """
                [user]
                \tname = Alice
                \temail = alice@eiu.edu.vn
                """.getBytes(StandardCharsets.UTF_8));
        MockMultipartFile reflog = new MockMultipartFile(
                "files",
                "123_Student/.git/logs/HEAD",
                "text/plain",
                ("0000000000000000000000000000000000000000 "
                        + "cccccccccccccccccccccccccccccccccccccccc "
                        + "Alice <alice@eiu.edu.vn> 1700000000 +0700\tcommit: only\n")
                        .getBytes(StandardCharsets.UTF_8));
        MockMultipartFile java = new MockMultipartFile(
                "files",
                "123_Student/challenge_1/A.java",
                "text/plain",
                "class A {}".getBytes(StandardCharsets.UTF_8));

        PlagiarismSignals signals = PlagiarismFingerprintExtractor.extract(List.of(config, reflog, java));

        assertEquals(List.of("cccccccccccccccccccccccccccccccccccccccc"), signals.gitCommitHashes());
        assertTrue(signals.metadataCanonical().contains("alice@eiu.edu.vn"));
        assertEquals(1, signals.fileHashes().size());
    }
}
