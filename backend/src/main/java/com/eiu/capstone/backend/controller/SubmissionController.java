package com.eiu.capstone.backend.controller;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.repository.StudentLabProgressRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.service.JwtService;
import com.eiu.capstone.backend.service.MmdPersistenceHook;
import com.eiu.capstone.backend.service.StudentHistoryService;
import com.eiu.capstone.backend.service.SubmissionCompileErrorStore;
import com.eiu.capstone.backend.service.SubmissionMmdMetaStore;
import com.eiu.capstone.backend.service.SubmissionPackageNormalizationStore;
import com.eiu.capstone.backend.plagiarism.PlagiarismService;
import com.eiu.capstone.backend.service.SubmissionStorageService;
import com.eiu.capstone.backend.utility.TimeUtil;
import com.eiu.capstone.backend.utility.TimingLog;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private static final Pattern CHALLENGE_NUMBER_PATTERN =
            Pattern.compile("challenge_(\\d+)", Pattern.CASE_INSENSITIVE);

    private final JwtService jwtService;
    private final SubmissionStorageService submissionStorageService;
    private final UserAccountRepository userAccountRepository;
    private final LabRepository labRepository;
    private final LabSubmissionRepository labSubmissionRepository;
    private final StudentLabProgressRepository studentLabProgressRepository;
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
    private final boolean timingLog;

    public SubmissionController(JwtService jwtService,
                                 SubmissionStorageService submissionStorageService,
                                 UserAccountRepository userAccountRepository,
                                 LabRepository labRepository,
                                 LabSubmissionRepository labSubmissionRepository,
                                 StudentLabProgressRepository studentLabProgressRepository,
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
                                 @Value("${app.grading.timing-log:false}") boolean timingLog) {
        this.jwtService = jwtService;
        this.submissionStorageService = submissionStorageService;
        this.userAccountRepository = userAccountRepository;
        this.labRepository = labRepository;
        this.labSubmissionRepository = labSubmissionRepository;
        this.studentLabProgressRepository = studentLabProgressRepository;
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
        this.timingLog = timingLog;
    }

    @GetMapping("/my-labs")
    public List<StudentLabSummaryDTO> getMyLabs(@RequestHeader("Authorization") String authHeader) {
        UserAccount user = resolveStudentUser(authHeader);
        return studentHistoryService.getLabSummaries(user.getId());
    }

    @GetMapping("/my-history")
    public StudentHistoryResponse getMyHistory(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) UUID labId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        UserAccount user = resolveStudentUser(authHeader);
        if (labId != null && !labRepository.existsById(labId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found");
        }
        return studentHistoryService.getHistory(user.getId(), labId, page, size, sort);
    }

    @PostMapping("/{labId}/{attemptNumber}/upload")
    public ResponseEntity<SubmissionUploadResponse> upload(
            @PathVariable UUID labId,
            @PathVariable Integer attemptNumber,
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("files") List<MultipartFile> files) {

        long totalStart = System.currentTimeMillis();

        UserAccount userAccount = resolveStudentUser(authHeader);
        String irn = parseAuthHeader(authHeader).get("irn", String.class);

        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found"));

        String requestId = UUID.randomUUID().toString();
        Path submissionFolderToDelete = null;
        try {
            long rubricStart = System.currentTimeMillis();
            LabRubricSnapshot rubric = labRubricCache.get(lab);
            long rubricMs = System.currentTimeMillis() - rubricStart;

            long processStart = System.currentTimeMillis();
            SubmissionStorageService.ProcessResult uploadResult =
                    submissionStorageService.processUpload(irn, requestId, files);
            submissionFolderToDelete = uploadResult.submissionFolder;
            long processMs = System.currentTimeMillis() - processStart;

            var existingSubmission = labSubmissionRepository
                    .findByUserAndLabAndAttemptNumber(userAccount, lab, attemptNumber);
            boolean isNewSubmission = existingSubmission.isEmpty();
            LabSubmission submission = existingSubmission.orElseGet(LabSubmission::new);
            submission.setUser(userAccount);
            submission.setLab(lab);
            submission.setAttemptNumber(attemptNumber);
            submission.setScore(BigDecimal.ZERO);
            submission = labSubmissionRepository.save(submission);

            long gradeStart = System.currentTimeMillis();
            GradingOutcome gradingOutcome = gradingService.gradeSubmission(
                    submission, rubric, uploadResult.challenges, uploadResult.mmdByChallenge, isNewSubmission);
            long gradeMs = System.currentTimeMillis() - gradeStart;

            submission.setScore(gradingOutcome.overallScore());
            submission = labSubmissionRepository.save(submission);

            int totalSubmissions = (int) labSubmissionRepository.countByUser_IdAndLab_Id(
                    userAccount.getId(), labId);

            StudentLabProgress progress = updateStudentProgress(
                    userAccount, lab, submission, gradingOutcome.overallScore(), totalSubmissions);

            compileErrorStore.save(submission.getId(), compileErrorsByChallengeId(rubric, uploadResult.challenges));
            packageNormalizationStore.save(
                    submission.getId(),
                    packageNormalizationNoticesByChallengeId(rubric, uploadResult.challenges));
            submissionMmdMetaStore.save(submission.getId(), gradingOutcome.mmdMetaByChallengeId());

            long plagiarismStart = System.currentTimeMillis();
            try {
                plagiarismService.inspectUpload(submission, files);
            } catch (RuntimeException e) {
                System.out.printf("plagiarism inspect failed submission=%s%n", submission.getId());
            }
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
                        "rubric", rubricMs,
                        "compile", processMs,
                        "grade", gradeMs,
                        "plagiarism", plagiarismMs,
                        "total", System.currentTimeMillis() - totalStart);
            }

            return ResponseEntity.ok(new SubmissionUploadResponse(
                    submission.getId(),
                    irn,
                    requestId,
                    challengeResult,
                    submission.getScore(),
                    attemptNumber,
                    totalSubmissions,
                    progress.getLastSubmittedAt() == null
                            ? null
                            : TimeUtil.formatLatestSubmission(progress.getLastSubmittedAt()),
                    gradingOutcome.labResult()
            ));
        } finally {
            if (submissionFolderToDelete != null) {
                submissionStorageService.deleteFolder(submissionFolderToDelete);
            }
        }
    }

    private StudentLabProgress updateStudentProgress(UserAccount userAccount,
                                                     Lab lab,
                                                     LabSubmission submission,
                                                     BigDecimal score,
                                                     int submissionCount) {
        StudentLabProgress progress = studentLabProgressRepository.findByUserAndLab(userAccount, lab)
                .orElseGet(StudentLabProgress::new);
        progress.setUser(userAccount);
        progress.setLab(lab);

        OffsetDateTime now = TimeUtil.nowInVietnam();
        if (progress.getFirstSubmittedAt() == null) {
            progress.setFirstSubmittedAt(now);
        }
        progress.setLastSubmittedAt(now);
        progress.setAttemptsCount(submissionCount);

        if (progress.getHighestScore() == null || score.compareTo(progress.getHighestScore()) > 0) {
            progress.setHighestScore(score);
            progress.setBestSubmissionId(submission.getId());
        }

        return studentLabProgressRepository.save(progress);
    }

    private Map<UUID, String> compileErrorsByChallengeId(
            LabRubricSnapshot rubric,
            List<SubmissionStorageService.ChallengeResult> challenges) {
        Map<UUID, String> errors = new LinkedHashMap<>();
        for (SubmissionStorageService.ChallengeResult challengeResult : challenges) {
            if (challengeResult.compileError == null || challengeResult.compileError.isBlank()) {
                continue;
            }
            Integer challengeNumber = extractChallengeNumber(challengeResult.challengeName);
            if (challengeNumber == null) {
                continue;
            }
            rubric.challenge(challengeNumber).ifPresent(challengeRubric ->
                    errors.put(challengeRubric.challengeId(), challengeResult.compileError));
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

    private UserAccount resolveStudentUser(String authHeader) {
        Claims claims = parseAuthHeader(authHeader);
        String irn = claims.get("irn", String.class);
        if (irn == null || irn.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This account has no IRN on file (teacher accounts cannot submit labs)");
        }
        String email = claims.get("email", String.class);
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    }

    private Claims parseAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }
        try {
            return jwtService.parseToken(authHeader.substring(7));
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
    }
}