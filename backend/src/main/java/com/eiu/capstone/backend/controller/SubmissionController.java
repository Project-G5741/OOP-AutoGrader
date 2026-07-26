package com.eiu.capstone.backend.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.service.JwtService;
import com.eiu.capstone.backend.service.SubmissionStorageService;

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

    public SubmissionController(JwtService jwtService,
                                 SubmissionStorageService submissionStorageService,
                                 UserAccountRepository userAccountRepository,
                                 LabRepository labRepository,
                                 LabSubmissionRepository labSubmissionRepository) {
        this.jwtService = jwtService;
        this.submissionStorageService = submissionStorageService;
        this.userAccountRepository = userAccountRepository;
        this.labRepository = labRepository;
        this.labSubmissionRepository = labSubmissionRepository;
    }

    @PostMapping("/{labId}/{attemptNumber}/upload")
    public ResponseEntity<SubmissionUploadResponse> upload(
            @PathVariable UUID labId,
            @PathVariable Integer attemptNumber,
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("files") List<MultipartFile> files) {

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
        SubmissionStorageService.ProcessResult result = null;
        try {
            result = submissionStorageService.processUpload(irn, requestId, files);

            //  run your actual grading/plagiarism-check step against result.challenges
            //  here (each ChallengeResult exposes .folder, .mmdFileCount, .classFileCount),
            //  and use its output to set the real score below instead of BigDecimal.ZERO.

            LabSubmission submission = labSubmissionRepository
                    .findByUserAndLabAndAttemptNumber(userAccount, lab, attemptNumber)
                    .orElseGet(LabSubmission::new);
            submission.setUser(userAccount);
            submission.setLab(lab);
            submission.setAttemptNumber(attemptNumber);
            if (submission.getScore() == null) {
                submission.setScore(BigDecimal.ZERO);
            }
            labSubmissionRepository.save(submission);

            List<ChallengeUploadResult> challengeResults = result.challenges.stream()
                    .map(c -> new ChallengeUploadResult(c.challengeName, c.mmdFileCount, c.classFileCount))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new SubmissionUploadResponse(
                    submission.getId(),
                    irn,
                    requestId,
                    challengeResults,
                    submission.getScore()
            ));
        } finally {
            // Safe to always delete: submissionFolder is unique per request (keyed by
            // requestId), so this can never step on another in-flight submission's files.
            if (result != null) {
                submissionStorageService.deleteFolder(result.submissionFolder);
            }
        }
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