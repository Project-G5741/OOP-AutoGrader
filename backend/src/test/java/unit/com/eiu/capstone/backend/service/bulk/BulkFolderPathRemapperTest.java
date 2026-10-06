package unit.com.eiu.capstone.backend.service.bulk;

import com.eiu.capstone.backend.service.bulk.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

class BulkFolderPathRemapperTest {

    @Test
    void examInsertsLabChallengeNumberUnderStudentRoot() {
        MultipartFile file = file("2331200057_DOAN TUAN KIET/Main.java", "class Main {}");
        List<MultipartFile> remapped = BulkFolderPathRemapper.remap(List.of(file), BulkGradeMode.EXAM, 2);
        assertEquals(1, remapped.size());
        assertEquals(
                "2331200057_DOAN TUAN KIET/challenge_2/Main.java",
                remapped.get(0).getOriginalFilename());
        assertEquals("2331200057_DOAN TUAN KIET", BulkFolderPathRemapper.extractStudentFolder(remapped));
        assertEquals("2331200057", BulkFolderPathRemapper.extractIrn("2331200057_DOAN TUAN KIET"));
    }

    @Test
    void examStripsMainPrefixThenInsertsChallenge1() {
        MultipartFile file = file("Main/2331200057_Student/Foo.java", "class Foo {}");
        List<MultipartFile> remapped = BulkFolderPathRemapper.remap(List.of(file), BulkGradeMode.EXAM);
        assertEquals(
                "2331200057_Student/challenge_1/Foo.java",
                remapped.get(0).getOriginalFilename());
    }

    @Test
    void labKeepsChallengeFoldersAndStripsMain() {
        MultipartFile file = file("Main/2331200057_Student/challenge_2/Bar.java", "class Bar {}");
        List<MultipartFile> remapped = BulkFolderPathRemapper.remap(List.of(file), BulkGradeMode.LAB);
        assertEquals(
                "2331200057_Student/challenge_2/Bar.java",
                remapped.get(0).getOriginalFilename());
    }

    @Test
    void labRejectsFlatExamShape() {
        MultipartFile file = file("2331200057_Student/Main.java", "class Main {}");
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> BulkFolderPathRemapper.remap(List.of(file), BulkGradeMode.LAB));
        assertTrue(ex.getReason().contains("challenge_n"));
    }

    @Test
    void examRejectsChallengeFolders() {
        MultipartFile file = file("2331200057_Student/challenge_1/Main.java", "class Main {}");
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> BulkFolderPathRemapper.remap(List.of(file), BulkGradeMode.EXAM));
        assertTrue(ex.getReason().toLowerCase().contains("exam"));
    }

    private static MockMultipartFile file(String path, String content) {
        return new MockMultipartFile("files", path, "text/plain", content.getBytes());
    }
}
