package com.eiu.capstone.backend.controller;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.StudentHistoryResponse;
import com.eiu.capstone.backend.DTO.StudentLabSummaryDTO;
import com.eiu.capstone.backend.DTO.SubmissionUploadResponse;
import com.eiu.capstone.backend.analytics.cache.LabStatisticsCache;
import com.eiu.capstone.backend.analytics.cache.LecturerOverviewCache;
import com.eiu.capstone.backend.grading.GradingOutcome;
import com.eiu.capstone.backend.grading.GradingService;
import com.eiu.capstone.backend.grading.rubric.LabRubricCache;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.StudentLabProgress;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.MmdPersistenceHook;
import com.eiu.capstone.backend.service.StudentHistoryService;
import com.eiu.capstone.backend.service.StudentTermAccessService;
import com.eiu.capstone.backend.service.UploadPersistService;
import com.eiu.capstone.backend.service.ChallengeCompileErrors;
import com.eiu.capstone.backend.service.SubmissionCompileErrorStore;
import com.eiu.capstone.backend.service.SubmissionMmdMetaStore;
import com.eiu.capstone.backend.service.SubmissionPackageNormalizationStore;
import com.eiu.capstone.backend.plagiarism.PlagiarismService;
import com.eiu.capstone.backend.plagiarism.PlagiarismSignals;
import com.eiu.capstone.backend.service.SubmissionStorageService;
import com.eiu.capstone.backend.utility.TimeUtil;
import com.eiu.capstone.backend.utility.TimingLog;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private static final Pattern CHALLENGE_NUMBER_PATTERN =
            Pattern.compile("challenge_(\\d+)", Pattern.CASE_INSENSITIVE);

    private final JwtAuthHelper jwtAuthHelper;
    private final SubmissionStorageService submissionStorageService;
    private final LabRepository labRepository;
    private final GradingService gradingService;
    private final LabRubricCache labRubricCache;
    private final MmdPersistenceHook mmdPersistenceHook;
    private final SubmissionCompileErrorStore compileErrorStore;
    private final SubmissionPackageNormalizationStore packageNormalizationStore;
    private final SubmissionMmdMetaStore submissionMmdMetaStore;
    private final StudentHistoryService studentHistoryService;
    private final LabStatisticsCache labStatisticsCache;
    private final LecturerOverviewCache lecturerOverviewCache;
    private final PlagiarismService plagiarismService;
    private final StudentTermAccessService studentTermAccessService;
    private final UploadPersistService uploadPersistService;
    private final ExecutorService persistExecutor;
    private final boolean timingLog;

    public SubmissionController(JwtAuthHelper jwtAuthHelper,
                                 SubmissionStorageService submissionStorageService,
                                 LabRepository labRepository,
                                 GradingService gradingService,
                                 LabRubricCache labRubricCache,
                                 MmdPersistenceHook mmdPersistenceHook,
                                 SubmissionCompileErrorStore compileErrorStore,
                                 SubmissionPackageNormalizationStore packageNormalizationStore,
                                 SubmissionMmdMetaStore submissionMmdMetaStore,
                                 StudentHistoryService studentHistoryService,
                                 LabStatisticsCache labStatisticsCache,
                                 LecturerOverviewCache lecturerOverviewCache,
                                 PlagiarismService plagiarismService,
                                 StudentTermAccessService studentTermAccessService,
                                 UploadPersistService uploadPersistService,
                                 @Qualifier("persistExecutor") ExecutorService persistExecutor,
                                 @Value("${app.grading.timing-log:false}") boolean timingLog) {
        this.jwtAuthHelper = jwtAuthHelper;
        this.submissionStorageService = submissionStorageService;
        this.labRepository = labRepository;
        this.gradingService = gradingService;
        this.labRubricCache = labRubricCache;
        this.mmdPersistenceHook = mmdPersistenceHook;
        this.compileErrorStore = compileErrorStore;
        this.packageNormalizationStore = packageNormalizationStore;
        this.submissionMmdMetaStore = submissionMmdMetaStore;
        this.studentHistoryService = studentHistoryService;
        this.labStatisticsCache = labStatisticsCache;
        this.lecturerOverviewCache = lecturerOverviewCache;
        this.plagiarismService = plagiarismService;
        this.studentTermAccessService = studentTermAccessService;
        this.uploadPersistService = uploadPersistService;
        this.persistExecutor = persistExecutor;
        this.timingLog = timingLog;
    }

    @GetMapping("/my-labs")
    public List<StudentLabSummaryDTO> getMyLabs(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(required = false, defaultValue = "all") String scope) {
        UserAccount user = requireStudentSubmitter(principal).user();
        boolean currentTermOnly = "current".equalsIgnoreCase(scope);
        return studentHistoryService.getLabSummaries(user.getId(), currentTermOnly);
    }

    @GetMapping("/my-history")
    public StudentHistoryResponse getMyHistory(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(required = false) UUID labId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        UserAccount user = requireStudentSubmitter(principal).user();
        if (labId != null && !labRepository.existsById(labId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found");
        }
        return studentHistoryService.getHistory(user.getId(), labId, page, size, sort);
    }

    @PostMapping("/{labId}/{attemptNumber}/upload")
    public ResponseEntity<SubmissionUploadResponse> upload(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable UUID labId,
            @PathVariable Integer attemptNumber,
            @RequestParam("files") List<MultipartFile> files) {

        long totalStart = System.currentTimeMillis();

        if (principal == null || principal.email() == null || principal.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        long accessStart = System.currentTimeMillis();
        StudentTermAccessService.UploadAccess access =
                studentTermAccessService.requireUploadAccess(principal.email(), labId);
        UserAccount userAccount = access.user();
        Lab lab = access.lab();
        String irn = resolveSubmitterIrn(principal, userAccount);
        long accessMs = System.currentTimeMillis() - accessStart;

        String requestId = UUID.randomUUID().toString();
        Path submissionFolderToDelete = null;
        try {
            CompletableFuture<LabRubricSnapshot> rubricFuture = CompletableFuture.supplyAsync(
                    () -> labRubricCache.get(lab), persistExecutor);

            long processStart = System.currentTimeMillis();
            SubmissionStorageService.ProcessResult uploadResult =
                    submissionStorageService.processUpload(irn, requestId, files);
            submissionFolderToDelete = uploadResult.submissionFolder;
            long processMs = System.currentTimeMillis() - processStart;

            long rubricJoinStart = System.currentTimeMillis();
            LabRubricSnapshot rubric = joinRubric(rubricFuture);
            long rubricMs = System.currentTimeMillis() - rubricJoinStart;

            if (attemptNumber == null || attemptNumber < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "attemptNumber must be a positive integer");
            }

            LabSubmission submission = new LabSubmission();
            submission.setId(UUID.randomUUID());
            submission.setUser(userAccount);
            submission.setLab(lab);
            submission.setScore(BigDecimal.ZERO);
            submission.setSubmittedAt(TimeUtil.nowInVietnam());

            long gradeStart = System.currentTimeMillis();
            GradingOutcome gradingOutcome = gradingService.gradeSubmission(
                    submission, rubric, uploadResult.challenges, uploadResult.mmdByChallenge);
            long gradeMs = System.currentTimeMillis() - gradeStart;

            long persistStart = System.currentTimeMillis();
            UploadPersistService.PersistResult persisted = uploadPersistService.persist(
                    submission, gradingOutcome);
            submission = persisted.submission();
            StudentLabProgress progress = persisted.progress();
            int assignedAttempt = submission.getAttemptNumber();
            long persistMs = System.currentTimeMillis() - persistStart;

            UUID persistedId = submission.getId();
            Map<UUID, ChallengeCompileErrors> compileErrors =
                    compileErrorsByChallengeId(rubric, uploadResult.challenges);
            Map<UUID, String> packageNotices =
                    packageNormalizationNoticesByChallengeId(rubric, uploadResult.challenges);
            var mmdMeta = gradingOutcome.mmdMetaByChallengeId();
            CompletableFuture.runAsync(() -> {
                try {
                    compileErrorStore.save(persistedId, compileErrors);
                    packageNormalizationStore.save(persistedId, packageNotices);
                    submissionMmdMetaStore.save(persistedId, mmdMeta);
                } catch (RuntimeException e) {
                    System.out.printf("sidecar persist failed submission=%s%n", persistedId);
                }
            }, persistExecutor);

            long plagiarismStart = System.currentTimeMillis();
            schedulePlagiarismInspect(submission, files);
            long plagiarismMs = System.currentTimeMillis() - plagiarismStart;

            mmdPersistenceHook.onUploadComplete(irn, requestId, uploadResult.mmdByChallenge);
            labStatisticsCache.invalidate(labId);
            lecturerOverviewCache.invalidate();

            Map<UUID, Integer> challengeResult = new LinkedHashMap<>();
            for (var graded : gradingOutcome.gradedChallenges()) {
                challengeResult.put(graded.challengeId(), graded.scorePercent());
            }

            if (timingLog) {
                TimingLog.block(true, "Upload",
                        "access", accessMs,
                        "rubric", rubricMs,
                        "compile", processMs,
                        "grade", gradeMs,
                        "persist", persistMs,
                        "plagiarism", plagiarismMs,
                        "total", System.currentTimeMillis() - totalStart);
            }

            return ResponseEntity.ok(new SubmissionUploadResponse(
                    submission.getId(),
                    irn,
                    requestId,
                    challengeResult,
                    submission.getScore(),
                    assignedAttempt,
                    assignedAttempt,
                    progress.getLastSubmittedAt() == null
                            ? null
                            : TimeUtil.formatLatestSubmission(progress.getLastSubmittedAt()),
                    gradingOutcome.labResult()
            ));
        } finally {
            if (submissionFolderToDelete != null) {
                Path folder = submissionFolderToDelete;
                CompletableFuture.runAsync(() -> submissionStorageService.deleteFolder(folder), persistExecutor);
            }
        }
    }

    private void schedulePlagiarismInspect(LabSubmission submission, List<MultipartFile> files) {
        if (submission == null || submission.getId() == null
                || submission.getLab() == null || submission.getLab().getId() == null
                || submission.getUser() == null || submission.getUser().getId() == null) {
            return;
        }
        UUID inspectLabId = submission.getLab().getId();
        PlagiarismSignals signals;
        try {
            signals = plagiarismService.snapshotSignals(files);
        } catch (RuntimeException e) {
            System.out.printf("plagiarism snapshot failed submission=%s%n", submission.getId());
            return;
        }
        CompletableFuture.runAsync(() -> {
            long inspectStart = System.currentTimeMillis();
            try {
                plagiarismService.inspectUpload(submission, signals);
            } catch (RuntimeException e) {
                System.out.printf("plagiarism inspect failed submission=%s%n", submission.getId());
            }
            TimingLog.line(timingLog, "Plagiarism inspect", System.currentTimeMillis() - inspectStart);
            try {
                labStatisticsCache.invalidate(inspectLabId);
                lecturerOverviewCache.invalidate();
            } catch (RuntimeException e) {
                System.out.printf("plagiarism cache invalidate failed lab=%s%n", inspectLabId);
            }
        }, persistExecutor);
    }

    private LabRubricSnapshot joinRubric(CompletableFuture<LabRubricSnapshot> rubricFuture) {
        try {
            return rubricFuture.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw e;
        }
    }

    private Map<UUID, ChallengeCompileErrors> compileErrorsByChallengeId(
            LabRubricSnapshot rubric,
            List<SubmissionStorageService.ChallengeResult> challenges) {
        Map<UUID, ChallengeCompileErrors> errors = new LinkedHashMap<>();
        for (SubmissionStorageService.ChallengeResult challengeResult : challenges) {
            ChallengeCompileErrors challengeErrors = ChallengeCompileErrors.fromChallengeResult(challengeResult);
            if (challengeErrors.isEmpty()) {
                continue;
            }
            Integer challengeNumber = extractChallengeNumber(challengeResult.challengeName);
            if (challengeNumber == null) {
                continue;
            }
            rubric.challenge(challengeNumber).ifPresent(challengeRubric ->
                    errors.put(challengeRubric.challengeId(), challengeErrors));
        }
        return errors;
    }

    private Map<UUID, String> packageNormalizationNoticesByChallengeId(
            LabRubricSnapshot rubric,
            List<SubmissionStorageService.ChallengeResult> challenges) {
        Map<UUID, String> notices = new LinkedHashMap<>();
        for (SubmissionStorageService.ChallengeResult challengeResult : challenges) {
            if (challengeResult.packageNormalizationNotice == null
                    || challengeResult.packageNormalizationNotice.isBlank()) {
                continue;
            }
            Integer challengeNumber = extractChallengeNumber(challengeResult.challengeName);
            if (challengeNumber == null) {
                continue;
            }
            rubric.challenge(challengeNumber).ifPresent(challengeRubric ->
                    notices.put(challengeRubric.challengeId(), challengeResult.packageNormalizationNotice));
        }
        return notices;
    }

    private Integer extractChallengeNumber(String challengeFolderKey) {
        Matcher matcher = CHALLENGE_NUMBER_PATTERN.matcher(challengeFolderKey);
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private String resolveSubmitterIrn(JwtUserPrincipal principal, UserAccount user) {
        String irn = principal != null ? principal.irn() : null;
        if (irn == null || irn.isBlank()) {
            irn = user.getIrn();
        }
        if (irn == null || irn.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This account has no IRN on file (teacher accounts cannot submit labs)");
        }
        return irn;
    }

    private record StudentSubmitter(UserAccount user, String irn) {}

    private StudentSubmitter requireStudentSubmitter(JwtUserPrincipal principal) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return new StudentSubmitter(user, resolveSubmitterIrn(principal, user));
    }
}