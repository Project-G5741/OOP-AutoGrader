package com.eiu.capstone.backend.controller;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.ChallengeUploadResult;
import com.eiu.capstone.backend.DTO.SubmissionUploadResponse;
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
import com.eiu.capstone.backend.service.SubmissionStorageService;
import com.eiu.capstone.backend.utility.TimeUtil;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final JwtService jwtService;
    private final SubmissionStorageService submissionStorageService;
    private final UserAccountRepository userAccountRepository;
    private final LabRepository labRepository;
    private final LabSubmissionRepository labSubmissionRepository;
    private final StudentLabProgressRepository studentLabProgressRepository;
    private final GradingService gradingService;
    private final LabRubricCache labRubricCache;
    private final MmdPersistenceHook mmdPersistenceHook;
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
        this.timingLog = timingLog;
    }

    @PostMapping("/{labId}/{attemptNumber}/upload")
    public ResponseEntity<SubmissionUploadResponse> upload(
            @PathVariable UUID labId,
            @PathVariable Integer attemptNumber,
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("files") List<MultipartFile> files) {

        long totalStart = System.currentTimeMillis();

        Claims claims = parseAuthHeader(authHeader);
        String irn = claims.get("irn", String.class);
        String email = claims.get("email", String.class);

        if (irn == null || irn.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This account has no IRN on file (teacher accounts cannot submit labs)");
        }

        UserAccount userAccount = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));

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

            LabSubmission submission = labSubmissionRepository
                    .findByUserAndLabAndAttemptNumber(userAccount, lab, attemptNumber)
                    .orElseGet(LabSubmission::new);
            submission.setUser(userAccount);
            submission.setLab(lab);
            submission.setAttemptNumber(attemptNumber);
            submission.setScore(BigDecimal.ZERO);
            submission = labSubmissionRepository.save(submission);

            long gradeStart = System.currentTimeMillis();
            BigDecimal score = gradingService.gradeSubmission(submission, rubric, uploadResult.challenges);
            long gradeMs = System.currentTimeMillis() - gradeStart;

            submission.setScore(score);
            submission = labSubmissionRepository.save(submission);

            updateStudentProgress(userAccount, lab, submission, score);

            mmdPersistenceHook.onUploadComplete(irn, requestId, uploadResult.mmdByChallenge);

            List<ChallengeUploadResult> challengeResults = uploadResult.challenges.stream()
                    .map(c -> new ChallengeUploadResult(
                            c.challengeName,
                            uploadResult.mmdByChallenge.getOrDefault(c.challengeName, List.of()).size(),
                            c.classFileCount))
                    .collect(Collectors.toList());

            if (timingLog) {
                long totalMs = System.currentTimeMillis() - totalStart;
                System.out.printf("grading_timing rubric_ms=%d process_ms=%d grade_ms=%d total_ms=%d%n",
                        rubricMs, processMs, gradeMs, totalMs);
            }

            return ResponseEntity.ok(new SubmissionUploadResponse(
                    submission.getId(),
                    irn,
                    requestId,
                    challengeResults,
                    submission.getScore()
            ));
        } finally {
            if (submissionFolderToDelete != null) {
                submissionStorageService.deleteFolder(submissionFolderToDelete);
            }
        }
    }

    private void updateStudentProgress(UserAccount userAccount, Lab lab, LabSubmission submission, BigDecimal score) {
        StudentLabProgress progress = studentLabProgressRepository.findByUserAndLab(userAccount, lab)
                .orElseGet(StudentLabProgress::new);
        progress.setUser(userAccount);
        progress.setLab(lab);

        OffsetDateTime now = TimeUtil.nowInVietnam();
        if (progress.getFirstSubmittedAt() == null) {
            progress.setFirstSubmittedAt(now);
        }
        progress.setLastSubmittedAt(now);

        int attempts = progress.getAttemptsCount() == null ? 0 : progress.getAttemptsCount();
        progress.setAttemptsCount(attempts + 1);

        if (progress.getHighestScore() == null || score.compareTo(progress.getHighestScore()) > 0) {
            progress.setHighestScore(score);
            progress.setBestSubmissionId(submission.getId());
        }

        studentLabProgressRepository.save(progress);
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
