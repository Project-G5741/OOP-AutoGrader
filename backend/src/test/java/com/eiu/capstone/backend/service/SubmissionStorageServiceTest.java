package com.eiu.capstone.backend.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.eiu.capstone.backend.exception.SubmissionProcessingException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmissionStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void acceptsMatchingSubmissionFolderAndChallengeNames() {
        assertTrue(SubmissionStorageService.isValidSubmissionPath(
                "2331200082_Nguyen_Van_A_lab_1/challenge_1/Student.java"));
    }

    @Test
    void rejectsInvalidSubmissionFolderNames() {
        assertFalse(SubmissionStorageService.isValidSubmissionPath(
                "invalid-folder/challenge_1/Student.java"));
    }

    @Test
    void rejectsInvalidChallengeFolderNames() {
        assertFalse(SubmissionStorageService.isValidSubmissionPath(
                "2331200082_Nguyen_Van_A_lab_1/ChallengeA/Student.java"));
    }

    @Test
    void rejectsMixedRootFolders() {
        SubmissionStorageService storage = newStorage(tempDir);
        ExecutorService compileExecutor = (ExecutorService) ReflectionTestUtils.getField(storage, "compileExecutor");
        SubmissionProcessingException ex = assertThrows(SubmissionProcessingException.class, () ->
                storage.processUpload("irn1", "req-1", List.of(
                        javaFile("2331200082_Nguyen_Van_A_lab_1/challenge_1/A.java", "public class A {}"),
                        javaFile("9999999999_Other_lab_1/challenge_1/B.java", "public class B {}"))));
        assertTrue(ex.getMessage().contains("Invalid folder structure"));
        compileExecutor.shutdownNow();
    }

    @Test
    void validUploadCompilesClassesWithoutSourcesTmp() throws IOException {
        SubmissionStorageService storage = newStorage(tempDir);
        ExecutorService compileExecutor = (ExecutorService) ReflectionTestUtils.getField(storage, "compileExecutor");
        try {
            SubmissionStorageService.ProcessResult result = storage.processUpload(
                    "irn1",
                    "req-ok",
                    List.of(
                            javaFile(
                                    "2331200082_Nguyen_Van_A_lab_1/challenge_1/Good.java",
                                    "public class Good {}"),
                            javaFile(
                                    "2331200082_Nguyen_Van_A_lab_1/challenge_2/Other.java",
                                    "public class Other {}")));

            SubmissionStorageService.ChallengeResult challenge1 = findChallenge(result, "challenge_1");
            SubmissionStorageService.ChallengeResult challenge2 = findChallenge(result, "challenge_2");

            assertNull(challenge1.compileError);
            assertEquals(1, challenge1.classFileCount);
            assertNull(challenge2.compileError);
            assertEquals(1, challenge2.classFileCount);

            assertTrue(Files.exists(result.submissionFolder.resolve("challenge_1/classes/Good.class")));
            assertTrue(Files.exists(result.submissionFolder.resolve("challenge_2/classes/Other.class")));
            assertFalse(Files.exists(result.submissionFolder.resolve("challenge_1/_sources_tmp")));
            assertFalse(Files.exists(result.submissionFolder.resolve("challenge_2/_sources_tmp")));
        } finally {
            compileExecutor.shutdownNow();
        }
    }

    @Test
    void compileErrorOnOneChallengePreservesOtherChallengeClasses() throws IOException {
        SubmissionStorageService storage = newStorage(tempDir);
        ExecutorService compileExecutor = (ExecutorService) ReflectionTestUtils.getField(storage, "compileExecutor");
        try {
            SubmissionStorageService.ProcessResult result = storage.processUpload(
                    "irn1",
                    "req-mixed",
                    List.of(
                            javaFile(
                                    "2331200082_Nguyen_Van_A_lab_1/challenge_1/Broken.java",
                                    "public class Broken {"),
                            javaFile(
                                    "2331200082_Nguyen_Van_A_lab_1/challenge_2/Good.java",
                                    "public class Good {}")));

            SubmissionStorageService.ChallengeResult challenge1 = findChallenge(result, "challenge_1");
            SubmissionStorageService.ChallengeResult challenge2 = findChallenge(result, "challenge_2");

            assertNotNull(challenge1.compileError);
            assertTrue(challenge1.compileError.contains("Compilation failed"));
            assertEquals(0, challenge1.classFileCount);
            assertFalse(Files.exists(result.submissionFolder.resolve("challenge_1/classes/Broken.class")));
            assertFalse(Files.exists(result.submissionFolder.resolve("challenge_1/_sources_tmp")));

            assertNull(challenge2.compileError);
            assertEquals(1, challenge2.classFileCount);

            Path goodClass = result.submissionFolder.resolve("challenge_2/classes/Good.class");
            assertTrue(Files.exists(goodClass));
        } finally {
            compileExecutor.shutdownNow();
        }
    }

    @Test
    void mmdOnlyChallengeSkipsCompile() throws IOException {
        SubmissionStorageService storage = newStorage(tempDir);
        ExecutorService compileExecutor = (ExecutorService) ReflectionTestUtils.getField(storage, "compileExecutor");
        try {
            SubmissionStorageService.ProcessResult result = storage.processUpload(
                    "irn1",
                    "req-mmd",
                    List.of(mmdFile("2331200082_Nguyen_Van_A_lab_1/challenge_1/diagram.mmd")));

            SubmissionStorageService.ChallengeResult challenge1 = findChallenge(result, "challenge_1");
            assertEquals(0, challenge1.classFileCount);
            assertNull(challenge1.compileError);
            assertFalse(Files.exists(result.submissionFolder.resolve("challenge_1/_sources_tmp")));
            assertEquals(1, result.mmdByChallenge.get("challenge_1").size());
        } finally {
            compileExecutor.shutdownNow();
        }
    }

    private SubmissionStorageService newStorage(Path baseDir) {
        JavaCompilerService compilerService = new JavaCompilerService();
        compilerService.initCompiler();
        ExecutorService compileExecutor = Executors.newFixedThreadPool(2);
        SubmissionStorageService storage = new SubmissionStorageService(compilerService, compileExecutor);
        ReflectionTestUtils.setField(storage, "baseDir", baseDir.toString());
        ReflectionTestUtils.setField(storage, "timingLog", false);
        return storage;
    }

    private static SubmissionStorageService.ChallengeResult findChallenge(
            SubmissionStorageService.ProcessResult result,
            String challengeName) {
        return result.challenges.stream()
                .filter(c -> c.challengeName.equals(challengeName))
                .findFirst()
                .orElseThrow();
    }

    private static MockMultipartFile javaFile(String path, String source) {
        return new MockMultipartFile("files", path, "text/plain", source.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile mmdFile(String path) {
        return new MockMultipartFile("files", path, "text/plain", "class Diagram".getBytes(StandardCharsets.UTF_8));
    }
}
