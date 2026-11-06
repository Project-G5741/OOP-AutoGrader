package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.eiu.capstone.backend.exception.SubmissionProcessingException;
import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.LabPrefix;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.service.SubmissionUploadPathService;

class SubmissionUploadPathServiceTest {

    private final ChallengeRepository challengeRepository = mock(ChallengeRepository.class);
    private final SubmissionUploadPathService service = new SubmissionUploadPathService(challengeRepository);

    @Test
    void prepareUploadFiles_officialLab_returnsFilesUnchanged() {
        Lab lab = new Lab();
        lab.setPrefix(LabPrefix.OFFICIAL);
        List<MultipartFile> files = List.of(javaFile("123_Name/challenge_1/A.java"));
        assertSame(files, service.prepareUploadFiles(lab, files));
    }

    @Test
    void prepareUploadFiles_midtermLab_insertsChallengeFolder() {
        UUID labId = UUID.randomUUID();
        Lab lab = new Lab();
        lab.setId(labId);
        lab.setPrefix(LabPrefix.MIDTERM);
        Challenge challenge = new Challenge();
        challenge.setChallengeNumber(1);
        when(challengeRepository.findByLab_IdOrderByChallengeNumberAsc(labId)).thenReturn(List.of(challenge));

        List<MultipartFile> files = List.of(
                javaFile("123_Name/Animal.java"),
                mmdFile("123_Name/diagram.mmd"));

        var remapped = service.prepareUploadFiles(lab, files);
        assertEquals(2, remapped.size());
        assertEquals("123_Name/challenge_1/Animal.java", remapped.get(0).getOriginalFilename());
        assertEquals("123_Name/challenge_1/diagram.mmd", remapped.get(1).getOriginalFilename());
    }

    @Test
    void prepareUploadFiles_midtermLab_requiresSingleChallenge() {
        UUID labId = UUID.randomUUID();
        Lab lab = new Lab();
        lab.setId(labId);
        lab.setPrefix(LabPrefix.MIDTERM);
        when(challengeRepository.findByLab_IdOrderByChallengeNumberAsc(labId))
                .thenReturn(List.of(new Challenge(), new Challenge()));

        assertThrows(
                SubmissionProcessingException.class,
                () -> service.prepareUploadFiles(lab, List.of(javaFile("123_Name/A.java"))));
    }

    private static MockMultipartFile javaFile(String path) {
        return new MockMultipartFile("files", path, "text/plain", "public class A {}".getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile mmdFile(String path) {
        return new MockMultipartFile("files", path, "text/plain", "classDiagram".getBytes(StandardCharsets.UTF_8));
    }
}
