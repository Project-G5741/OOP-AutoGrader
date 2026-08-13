package com.eiu.capstone.backend.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.tools.JavaFileObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.TestcaseResultDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.ReferenceSourceDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseDryRunRequest;
import com.eiu.capstone.backend.grading.pipeline.ChallengeGradingContext;
import com.eiu.capstone.backend.grading.pipeline.TestcaseGrader;
import com.eiu.capstone.backend.grading.pipeline.TestcaseGrader.PendingTestcaseResult;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubricAssembler;
import com.eiu.capstone.backend.grading.testcase.TestcaseResultMapper;
import com.eiu.capstone.backend.service.compile.MemorySourceJavaFileObject;

@Service
public class TestcaseDryRunService {

    private static final Logger log = LoggerFactory.getLogger(TestcaseDryRunService.class);
    private static final int MAX_REFERENCE_SOURCES = 20;
    private static final int MAX_SOURCE_BYTES = 256_000;
    private static final int MAX_TOTAL_BYTES = 512_000;

    private final TestcaseRubricService testcaseRubricService;
    private final TestcaseRubricAssembler testcaseRubricAssembler;
    private final JavaCompilerService javaCompilerService;
    private final TestcaseGrader testcaseGrader;
    private final TestcaseResultMapper testcaseResultMapper;

    public TestcaseDryRunService(TestcaseRubricService testcaseRubricService,
                                 TestcaseRubricAssembler testcaseRubricAssembler,
                                 JavaCompilerService javaCompilerService,
                                 TestcaseGrader testcaseGrader,
                                 TestcaseResultMapper testcaseResultMapper) {
        this.testcaseRubricService = testcaseRubricService;
        this.testcaseRubricAssembler = testcaseRubricAssembler;
        this.javaCompilerService = javaCompilerService;
        this.testcaseGrader = testcaseGrader;
        this.testcaseResultMapper = testcaseResultMapper;
    }

    public TestcaseResultDTO dryRun(UUID labId, UUID challengeId, TestcaseDryRunRequest request) {
        if (request == null || request.testcase() == null) {
            throw unprocessable("Testcase payload is required");
        }
        if (request.referenceSources() == null || request.referenceSources().isEmpty()) {
            throw unprocessable("Reference Java sources are required");
        }
        validateReferenceSources(request.referenceSources());

        testcaseRubricService.loadForChallenge(labId, challengeId);

        TestcaseRubric rubric = testcaseRubricAssembler.assemble(challengeId, request.testcase());
        Path tempRoot = null;
        try {
            tempRoot = Files.createTempDirectory("testcase-dry-run-");
            Path classesDir = tempRoot.resolve("classes");
            Files.createDirectories(classesDir);

            List<JavaFileObject> sources = new ArrayList<>();
            for (ReferenceSourceDTO source : request.referenceSources()) {
                if (source.className() == null || source.className().isBlank()
                        || source.source() == null || source.source().isBlank()) {
                    throw unprocessable("Each reference source requires className and source");
                }
                String path = source.className().replace('.', '/') + ".java";
                sources.add(new MemorySourceJavaFileObject(
                        path, source.source().getBytes(StandardCharsets.UTF_8)));
            }

            List<String> errors = javaCompilerService.compileSources(sources, classesDir);
            if (!errors.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Compilation failed: " + String.join("; ", errors));
            }

            ChallengeRubric stubRubric = new ChallengeRubric(
                    challengeId,
                    1,
                    "dry-run",
                    List.of(),
                    List.of(),
                    List.of(rubric),
                    true);
            ChallengeGradingContext context = ChallengeGradingContext.of(
                    stubRubric, classesDir, null, List.of());

            PendingTestcaseResult pending = testcaseGrader.gradeSingle(rubric, context);
            return testcaseResultMapper.mapDryRunResult(rubric, pending);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Dry-run failed for challenge {}", challengeId, ex);
            throw unprocessable("Dry-run failed. Check reference Java and testcase configuration.");
        } finally {
            if (tempRoot != null) {
                try {
                    deleteRecursively(tempRoot);
                } catch (Exception ignored) {
                    // best effort cleanup
                }
            }
        }
    }

    private void validateReferenceSources(List<ReferenceSourceDTO> sources) {
        if (sources.size() > MAX_REFERENCE_SOURCES) {
            throw unprocessable("Too many reference sources (max " + MAX_REFERENCE_SOURCES + ")");
        }
        int totalBytes = 0;
        for (ReferenceSourceDTO source : sources) {
            if (source.source() == null) {
                continue;
            }
            int bytes = source.source().getBytes(StandardCharsets.UTF_8).length;
            if (bytes > MAX_SOURCE_BYTES) {
                throw unprocessable("Reference source exceeds size limit (" + MAX_SOURCE_BYTES + " bytes)");
            }
            totalBytes += bytes;
        }
        if (totalBytes > MAX_TOTAL_BYTES) {
            throw unprocessable("Reference sources exceed total size limit (" + MAX_TOTAL_BYTES + " bytes)");
        }
    }

    private static ResponseStatusException unprocessable(String message) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    private void deleteRecursively(Path root) throws java.io.IOException {
        if (Files.isDirectory(root)) {
            try (var stream = Files.list(root)) {
                for (Path child : stream.toList()) {
                    deleteRecursively(child);
                }
            }
        }
        Files.deleteIfExists(root);
    }
}
