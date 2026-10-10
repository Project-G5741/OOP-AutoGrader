package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.eiu.capstone.backend.DTO.rubric.ChallengeStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.SolutionImportChallengeResult;
import com.eiu.capstone.backend.DTO.rubric.SolutionImportResponse;
import com.eiu.capstone.backend.grading.MmdParser;
import com.eiu.capstone.backend.grading.ReflectionClassParser;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.MasterData;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.MasterDataRepository;
import com.eiu.capstone.backend.service.JavaCompilerService;
import com.eiu.capstone.backend.service.SolutionImportService;

class SolutionImportServiceTest {

    private static final UUID LAB_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    private LabRepository labRepository;
    private MasterDataRepository masterDataRepository;
    private SolutionImportService service;

    @BeforeEach
    void setUp() {
        labRepository = mock(LabRepository.class);
        masterDataRepository = mock(MasterDataRepository.class);
        JavaCompilerService compiler = new JavaCompilerService();
        compiler.initCompiler();
        service = new SolutionImportService(
                labRepository,
                masterDataRepository,
                compiler,
                new ReflectionClassParser(),
                new MmdParser());

        Lab lab = new Lab();
        lab.setId(LAB_ID);
        lab.setName("Import Lab");
        when(labRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        stubMasterData();
    }

    @Test
    void partialCompile_appliesOkAndSkipsFail() {
        List<MultipartFile> files = List.of(
                javaFile("LabRoot/challenge_1/Ok.java", "public class Ok { public int n; }"),
                javaFile("LabRoot/challenge_2/Bad.java", "public class Bad { int x = ; }"));

        SolutionImportResponse response = service.importSolution(LAB_ID, files);
        assertEquals(LAB_ID, response.labId());
        assertEquals(2, response.challenges().size());

        SolutionImportChallengeResult c1 = response.challenges().get(0);
        SolutionImportChallengeResult c2 = response.challenges().get(1);
        assertEquals(1, c1.challengeNumber());
        assertEquals("applied", c1.status());
        assertNotNull(c1.challenge());
        assertTrue(c1.challenge().classes().stream().anyMatch(c -> "Ok".equals(c.name())));
        assertFalse(c1.challenge().hasMmd());

        assertEquals(2, c2.challengeNumber());
        assertEquals("skipped", c2.status());
        assertNull(c2.challenge());
        assertNotNull(c2.message());
        assertFalse(c2.message().isBlank());
    }

    @Test
    void javaOnly_hasMmdFalse_mmdOnlySkipped() {
        List<MultipartFile> files = List.of(
                javaFile("challenge_1/Solo.java", "public class Solo {}"),
                mmdFile("challenge_2/diagram.mmd", "classDiagram\n  class Ghost"));

        SolutionImportResponse response = service.importSolution(LAB_ID, files);
        assertEquals(2, response.challenges().size());

        SolutionImportChallengeResult applied = response.challenges().get(0);
        assertEquals("applied", applied.status());
        assertFalse(applied.challenge().hasMmd());

        SolutionImportChallengeResult skipped = response.challenges().get(1);
        assertEquals("skipped", skipped.status());
        assertTrue(skipped.message().toLowerCase().contains("java"));
    }

    @Test
    void javaAndMmdInheritance_doesNotDuplicateExtends() {
        String java = """
                public class Shape {}
                """;
        String child = """
                public class Circle extends Shape {}
                """;
        String mmd = """
                classDiagram
                class Circle
                class Shape
                Circle --|> Shape
                """;
        List<MultipartFile> files = List.of(
                javaFile("challenge_1/Shape.java", java),
                javaFile("challenge_1/Circle.java", child),
                mmdFile("challenge_1/diagram.mmd", mmd));

        SolutionImportResponse response = service.importSolution(LAB_ID, files);
        SolutionImportChallengeResult row = response.challenges().get(0);
        assertEquals("applied", row.status());
        ChallengeStructureDTO challenge = row.challenge();
        assertTrue(challenge.hasMmd());

        var circle = challenge.classes().stream()
                .filter(c -> "Circle".equals(c.name()))
                .findFirst()
                .orElseThrow();
        long inheritanceCount = challenge.relations().stream()
                .filter(r -> circle.id().equals(r.sourceClassId()))
                .filter(r -> Integer.valueOf(20).equals(r.relationTypeId()))
                .count();
        assertEquals(1, inheritanceCount, "Circle must have exactly one inheritance relation after import");
    }

    @Test
    void nestedStaticClass_mapsOuterLink() {
        String source = """
                public class Pen {
                    public static class PenBuilder {
                        private String brand;
                        public PenBuilder setBrand(String brand) { this.brand = brand; return this; }
                    }
                }
                """;
        List<MultipartFile> files = List.of(javaFile("challenge_3/Pen.java", source));

        SolutionImportResponse response = service.importSolution(LAB_ID, files);
        SolutionImportChallengeResult row = response.challenges().get(0);
        assertEquals("applied", row.status());
        ChallengeStructureDTO challenge = row.challenge();
        var builder = challenge.classes().stream()
                .filter(c -> "PenBuilder".equals(c.name()))
                .findFirst()
                .orElseThrow();
        var pen = challenge.classes().stream()
                .filter(c -> "Pen".equals(c.name()))
                .findFirst()
                .orElseThrow();
        assertEquals(pen.id(), builder.outerClassId());
        assertTrue(builder.isStatic());
    }

    private void stubMasterData() {
        when(masterDataRepository.findByCategoryOrderByNameAsc(eq("SCOPE")))
                .thenReturn(List.of(
                        md(1, "public", "SCOPE"),
                        md(2, "private", "SCOPE"),
                        md(3, "protected", "SCOPE")));
        when(masterDataRepository.findByCategoryOrderByNameAsc(eq("DECLARING_TYPE")))
                .thenReturn(List.of(
                        md(10, "class", "DECLARING_TYPE"),
                        md(11, "interface", "DECLARING_TYPE"),
                        md(12, "enum", "DECLARING_TYPE")));
        when(masterDataRepository.findByCategoryOrderByNameAsc(eq("RELATION_TYPE")))
                .thenReturn(List.of(
                        md(20, "Inheritance", "RELATION_TYPE"),
                        md(21, "Realization", "RELATION_TYPE"),
                        md(22, "Association", "RELATION_TYPE"),
                        md(23, "Composition", "RELATION_TYPE")));
    }

    private static MasterData md(int id, String name, String category) {
        MasterData row = new MasterData();
        row.setId(id);
        row.setName(name);
        row.setCategory(category);
        return row;
    }

    private static MockMultipartFile javaFile(String path, String source) {
        return new MockMultipartFile(
                "files", path, "text/plain", source.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile mmdFile(String path, String source) {
        return new MockMultipartFile(
                "files", path, "text/plain", source.getBytes(StandardCharsets.UTF_8));
    }
}
