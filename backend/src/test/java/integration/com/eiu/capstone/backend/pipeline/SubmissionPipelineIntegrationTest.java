package integration.com.eiu.capstone.backend.pipeline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.eiu.capstone.backend.grading.MmdComparisonService;
import com.eiu.capstone.backend.grading.MmdParser;
import com.eiu.capstone.backend.grading.ReflectionClassParser;
import com.eiu.capstone.backend.grading.pipeline.ClassReflectionGrader;
import com.eiu.capstone.backend.grading.pipeline.GradingPipeline;
import com.eiu.capstone.backend.grading.pipeline.MmdPillarGrader;
import com.eiu.capstone.backend.grading.pipeline.TestcaseGrader;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.service.JavaCompilerService;
import com.eiu.capstone.backend.service.SubmissionStorageService;

class SubmissionPipelineIntegrationTest {

    private static final String CHALLENGE_1 = "challenge_1";
    private static final String UPLOAD_ANIMAL =
            "2331200082_Nguyen_Van_A_lab_1/challenge_1/Animal.java";
    private static final String UPLOAD_BROKEN =
            "2331200082_Nguyen_Van_A_lab_1/challenge_1/Broken.java";
    private static final String UPLOAD_MMD =
            "2331200082_Nguyen_Van_A_lab_1/challenge_1/diagram.mmd";

    @TempDir
    Path tempDir;

    private ExecutorService pillarExecutor;

    @BeforeEach
    void startPillarExecutor() {
        pillarExecutor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void stopPillarExecutor() {
        pillarExecutor.shutdownNow();
    }

    @Test
    void happyPathCompilesThenGradesClassPillar() throws IOException {
        try (UploadHarness harness = newHarness(tempDir)) {
            SubmissionStorageService.ProcessResult upload = harness.storage.processUpload(
                    "irn1",
                    "req-happy",
                    List.of(classpathFile("integration/happy/" + UPLOAD_ANIMAL, UPLOAD_ANIMAL)));

            SubmissionStorageService.ChallengeResult folder = findChallenge(upload, CHALLENGE_1);
            assertNull(folder.compileError);
            assertTrue(Files.exists(upload.submissionFolder.resolve("challenge_1/classes/Animal.class")));

            GradingPipeline.ChallengePipelineResult graded = pipeline()
                    .gradeChallenge(snapshot(false), folder, List.of());
            assertNotNull(graded);
            assertNotNull(graded.classResult());
        }
    }

    @Test
    void compileFailureFinishesAsAssertedError() throws IOException {
        try (UploadHarness harness = newHarness(tempDir)) {
            SubmissionStorageService.ProcessResult upload = harness.storage.processUpload(
                    "irn1",
                    "req-compile-fail",
                    List.of(classpathFile("integration/compile-fail/" + UPLOAD_BROKEN, UPLOAD_BROKEN)));

            SubmissionStorageService.ChallengeResult folder = findChallenge(upload, CHALLENGE_1);
            assertNull(folder.compileError);
            assertFalse(folder.failedClassNames.isEmpty());
            assertTrue(folder.compileErrorsByClassName.values().stream()
                    .anyMatch(message -> message.contains("line")));

            GradingPipeline.ChallengePipelineResult graded = pipeline()
                    .gradeChallenge(snapshot(false), folder, List.of());
            assertNotNull(graded);
        }
    }

    @Test
    void fatalMmdParseDoesNotAbortCompile() throws IOException {
        try (UploadHarness harness = newHarness(tempDir)) {
            SubmissionStorageService.ProcessResult upload = harness.storage.processUpload(
                    "irn1",
                    "req-mmd-fail",
                    List.of(
                            classpathFile("integration/happy/" + UPLOAD_ANIMAL, UPLOAD_ANIMAL),
                            classpathFile("integration/mmd-fail/" + UPLOAD_MMD, UPLOAD_MMD)));

            SubmissionStorageService.ChallengeResult folder = findChallenge(upload, CHALLENGE_1);
            assertNull(folder.compileError);

            List<MultipartFile> mmdFiles = upload.mmdByChallenge.getOrDefault(CHALLENGE_1, List.of());
            GradingPipeline.ChallengePipelineResult graded = pipeline()
                    .gradeChallenge(snapshot(true), folder, mmdFiles);
            assertNotNull(graded);
            assertNotNull(graded.mmdResult().parseError());
        }
    }

    private static LabRubricSnapshot snapshot(boolean hasMmd) {
        UUID classId = UUID.randomUUID();
        ChallengeRubric challenge = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Animals",
                List.of(new ClassRubric(
                        classId, "Animal", "public", "CLASS", false, List.of(), List.of(), List.of())),
                List.of(),
                List.of(),
                hasMmd);
        return new LabRubricSnapshot(UUID.randomUUID(), Map.of(1, challenge));
    }

    private GradingPipeline pipeline() {
        return new GradingPipeline(
                new ReflectionClassParser(),
                new ClassReflectionGrader(),
                new MmdPillarGrader(new MmdParser(), new MmdComparisonService()),
                new TestcaseGrader(null, null, null, null),
                pillarExecutor,
                false);
    }

    private static UploadHarness newHarness(Path baseDir) {
        JavaCompilerService compilerService = new JavaCompilerService();
        compilerService.initCompiler();
        ExecutorService compileExecutor = Executors.newSingleThreadExecutor();
        SubmissionStorageService storage = new SubmissionStorageService(compilerService, compileExecutor);
        ReflectionTestUtils.setField(storage, "baseDir", baseDir.toString());
        ReflectionTestUtils.setField(storage, "timingLog", false);
        return new UploadHarness(storage, compileExecutor);
    }

    private static SubmissionStorageService.ChallengeResult findChallenge(
            SubmissionStorageService.ProcessResult result,
            String challengeName) {
        return result.challenges.stream()
                .filter(c -> c.challengeName.equals(challengeName))
                .findFirst()
                .orElseThrow();
    }

    private static MockMultipartFile classpathFile(String resourcePath, String uploadPath) throws IOException {
        try (InputStream in = SubmissionPipelineIntegrationTest.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            Objects.requireNonNull(in, resourcePath);
            return new MockMultipartFile("files", uploadPath, "text/plain", in.readAllBytes());
        }
    }

    private record UploadHarness(SubmissionStorageService storage, ExecutorService compileExecutor)
            implements AutoCloseable {
        @Override
        public void close() {
            compileExecutor.shutdownNow();
        }
    }
}
