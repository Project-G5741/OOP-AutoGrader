package com.eiu.capstone.backend.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

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
import com.eiu.capstone.backend.grading.testcase.DryRunWorkerCache;
import com.eiu.capstone.backend.grading.testcase.TestcaseResultMapper;
import com.eiu.capstone.backend.grading.testcase.WorkerSessionFactory;
import com.eiu.capstone.backend.grading.testcase.WorkerSessionHandle;
import com.eiu.capstone.backend.service.compile.CompileClassAttribution;
import com.eiu.capstone.backend.service.compile.CompileOutcome;
import com.eiu.capstone.backend.service.compile.MemorySourceJavaFileObject;
import com.eiu.capstone.backend.service.compile.StudentSourceNormalizer;
import com.eiu.capstone.backend.service.compile.StudentSourceNormalizer.NormalizationResult;
import com.eiu.capstone.backend.service.compile.StudentSourceNormalizer.SourceEntry;
import com.eiu.capstone.backend.utility.TimingLog;

@Service
public class TestcaseDryRunService {

    private static final Logger log = LoggerFactory.getLogger(TestcaseDryRunService.class);
    private static final int MAX_REFERENCE_SOURCES = 20;
    private static final int MAX_SOURCE_BYTES = 256_000;
    private static final int MAX_TOTAL_BYTES = 512_000;
    /** TEMP: remove after dry-run UX timing check */
    private static final boolean TEMP_DRY_RUN_TIMING = true;

    private final TestcaseRubricAssembler testcaseRubricAssembler;
    private final JavaCompilerService javaCompilerService;
    private final TestcaseGrader testcaseGrader;
    private final TestcaseResultMapper testcaseResultMapper;
    private final Semaphore workerJvmSlot;
    private final WorkerSessionFactory workerSessionFactory;
    private final DryRunWorkerCache dryRunWorkerCache;
    private final DryRunCompileCache dryRunCompileCache;
    private final int invokeTimeoutSeconds;

    public TestcaseDryRunService(TestcaseRubricAssembler testcaseRubricAssembler,
                                 JavaCompilerService javaCompilerService,
                                 TestcaseGrader testcaseGrader,
                                 TestcaseResultMapper testcaseResultMapper,
                                 @org.springframework.beans.factory.annotation.Qualifier("workerJvmSlot") Semaphore workerJvmSlot,
                                 WorkerSessionFactory workerSessionFactory,
                                 DryRunWorkerCache dryRunWorkerCache,
                                 DryRunCompileCache dryRunCompileCache,
                                 @org.springframework.beans.factory.annotation.Value("${app.grading.testcase-invoke-timeout-seconds:5}") int invokeTimeoutSeconds) {
        this.testcaseRubricAssembler = testcaseRubricAssembler;
        this.javaCompilerService = javaCompilerService;
        this.testcaseGrader = testcaseGrader;
        this.testcaseResultMapper = testcaseResultMapper;
        this.workerJvmSlot = workerJvmSlot;
        this.workerSessionFactory = workerSessionFactory;
        this.dryRunWorkerCache = dryRunWorkerCache;
        this.dryRunCompileCache = dryRunCompileCache;
        this.invokeTimeoutSeconds = invokeTimeoutSeconds;
    }

    public TestcaseResultDTO dryRun(UUID labId, UUID challengeId, TestcaseDryRunRequest request) {
        long totalStarted = System.currentTimeMillis();
        if (request == null || request.testcase() == null) {
            throw unprocessable("Testcase payload is required");
        }
        if (request.referenceSources() == null || request.referenceSources().isEmpty()) {
            throw unprocessable("Reference Java sources are required");
        }
        validateReferenceSources(request.referenceSources());

        // No standalone Neon access check: lab ownership is enforced on cold assemble only.
        // Warm catalog hits are in-memory (compile + worker + grade only).
        long assembleStarted = System.currentTimeMillis();
        TestcaseRubric rubric = testcaseRubricAssembler.assemble(labId, challengeId, request.testcase());
        long assembleMs = System.currentTimeMillis() - assembleStarted;
        Path requestTempRoot = null;
        boolean classesFromCache = false;
        try {
            List<SourceEntry> rawSources = new ArrayList<>();
            for (ReferenceSourceDTO source : request.referenceSources()) {
                if (source.className() == null || source.className().isBlank()
                        || source.source() == null || source.source().isBlank()) {
                    throw unprocessable("Each reference source requires className and source");
                }
                String path = source.className().replace('.', '/') + ".java";
                rawSources.add(new SourceEntry(path, source.source()));
            }

            NormalizationResult normalization = StudentSourceNormalizer.normalizeChallengeSources(rawSources);
            String compileKey = DryRunCompileCache.fingerprint(normalization.sources());
            DryRunCompileCache.Hit compileHit = dryRunCompileCache.getIfFresh(compileKey);

            Path classesDir;
            CompileOutcome outcome;
            List<SourceEntry> normalizedSources;
            long compileMs;
            if (compileHit != null) {
                classesDir = compileHit.classesDir();
                outcome = compileHit.outcome();
                normalizedSources = compileHit.normalizedSources();
                compileMs = 0;
                classesFromCache = true;
            } else {
                requestTempRoot = Files.createTempDirectory("testcase-dry-run-");
                classesDir = requestTempRoot.resolve("classes");
                Files.createDirectories(classesDir);
                List<JavaFileObject> sources = new ArrayList<>();
                for (SourceEntry entry : normalization.sources()) {
                    sources.add(new MemorySourceJavaFileObject(
                            entry.logicalPath(),
                            entry.source().getBytes(StandardCharsets.UTF_8)));
                }
                long compileStarted = System.currentTimeMillis();
                outcome = javaCompilerService.compileSources(sources, classesDir);
                compileMs = System.currentTimeMillis() - compileStarted;
                normalizedSources = normalization.sources();
                dryRunCompileCache.put(compileKey, classesDir, outcome, normalizedSources);
                classesFromCache = true;
            }

            CompileClassAttribution.Result attributed = CompileClassAttribution.attribute(
                    outcome, normalizedSources);

            ChallengeRubric stubRubric = new ChallengeRubric(
                    challengeId,
                    1,
                    "dry-run",
                    List.of(),
                    List.of(),
                    List.of(rubric),
                    true);

            boolean sandbox = workerSessionFactory.isSandboxEnabled();
            Path openRoot = classesDir.getParent() != null ? classesDir.getParent() : classesDir;
            CompletableFuture<WorkerSessionHandle> localOpen = null;
            if (!sandbox && !dryRunWorkerCache.hasIdleLocal()) {
                localOpen = CompletableFuture.supplyAsync(
                        () -> workerSessionFactory.open(openRoot, invokeTimeoutSeconds));
            }

            long workerStarted = System.currentTimeMillis();
            acquireWorkerSlot();
            long slotMs = System.currentTimeMillis() - workerStarted;
            WorkerSessionHandle workerSession = null;
            long openMs = 0;
            long gradeMs = 0;
            try {
                long openStarted = System.currentTimeMillis();
                if (localOpen != null) {
                    workerSession = localOpen.join();
                } else {
                    workerSession = dryRunWorkerCache.borrow(workerSessionFactory, openRoot, invokeTimeoutSeconds);
                }
                openMs = System.currentTimeMillis() - openStarted;
                long gradeStarted = System.currentTimeMillis();
                TestcaseResultDTO result = gradeDryRun(stubRubric, classesDir, attributed, rubric, workerSession);
                gradeMs = System.currentTimeMillis() - gradeStarted;
                // spawn is diagnostic only (may overlap compile); not part of the wall-clock sum
                TimingLog.block(TEMP_DRY_RUN_TIMING, "OT dry-run",
                        "assemble", assembleMs,
                        "compile", compileMs,
                        "slot", slotMs,
                        "worker", openMs,
                        "grade", gradeMs,
                        "total", System.currentTimeMillis() - totalStarted);
                return result;
            } finally {
                dryRunWorkerCache.release(workerSessionFactory, workerSession);
                workerJvmSlot.release();
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Dry-run failed for challenge {}", challengeId, ex);
            throw unprocessable("Dry-run failed. Check reference Java and testcase configuration.");
        } finally {
            // Cache owns successful compile dirs. Only delete a request temp root that was never cached.
            if (requestTempRoot != null && !classesFromCache) {
                try {
                    deleteRecursively(requestTempRoot);
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

    private TestcaseResultDTO gradeDryRun(ChallengeRubric stubRubric,
                                          Path classesDir,
                                          CompileClassAttribution.Result attributed,
                                          TestcaseRubric rubric,
                                          WorkerSessionHandle workerSession) {
        ChallengeGradingContext context = ChallengeGradingContext.of(
                stubRubric,
                classesDir,
                null,
                List.of(),
                attributed.failedClassNames(),
                attributed.compileErrorsByClassName(),
                workerSession);
        PendingTestcaseResult pending = testcaseGrader.gradeSingle(rubric, context);
        return testcaseResultMapper.mapDryRunResult(rubric, pending);
    }

    private void acquireWorkerSlot() {
        try {
            workerJvmSlot.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unprocessable("Interrupted waiting for isolated worker slot");
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
