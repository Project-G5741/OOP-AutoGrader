package com.eiu.capstone.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.DTO.ChallengeBreakdownDTO;
import com.eiu.capstone.backend.DTO.StudentChallengeResultDTO;
import com.eiu.capstone.backend.DTO.StudentHistoryResponse;
import com.eiu.capstone.backend.DTO.StudentHistoryStatsDTO;
import com.eiu.capstone.backend.DTO.StudentLabRefDTO;
import com.eiu.capstone.backend.DTO.StudentLabSummaryDTO;
import com.eiu.capstone.backend.DTO.StudentSubmissionHistoryItemDTO;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.StudentLabProgress;
import com.eiu.capstone.backend.model.SubmissionChallengeResult;
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.repository.StudentLabProgressRepository;
import com.eiu.capstone.backend.repository.SubmissionChallengeResultRepository;

@Service
public class StudentHistoryService {

    private static final BigDecimal PASS_THRESHOLD = new BigDecimal("80");
    private static final BigDecimal FAIL_THRESHOLD = new BigDecimal("50");

    private final StudentLabProgressRepository studentLabProgressRepository;
    private final LabSubmissionRepository labSubmissionRepository;
    private final SubmissionChallengeResultRepository submissionChallengeResultRepository;
    private final ChallengeService challengeService;

    public StudentHistoryService(StudentLabProgressRepository studentLabProgressRepository,
                                 LabSubmissionRepository labSubmissionRepository,
                                 SubmissionChallengeResultRepository submissionChallengeResultRepository,
                                 ChallengeService challengeService) {
        this.studentLabProgressRepository = studentLabProgressRepository;
        this.labSubmissionRepository = labSubmissionRepository;
        this.submissionChallengeResultRepository = submissionChallengeResultRepository;
        this.challengeService = challengeService;
    }

    public List<StudentLabSummaryDTO> getLabSummaries(UUID userId) {
        List<StudentLabSummaryDTO> summaries = new ArrayList<>();
        for (StudentLabProgress progress : studentLabProgressRepository.findByUser_IdWithLabOrderByLastSubmittedAtDesc(userId)) {
            summaries.add(toLabSummary(progress));
        }
        return summaries;
    }

    public StudentHistoryResponse getHistory(UUID userId, UUID labId) {
        List<LabSubmission> submissions = labId == null
                ? labSubmissionRepository.findByUser_IdWithLabOrderBySubmittedAtDesc(userId)
                : labSubmissionRepository.findByUser_IdAndLab_IdWithLabOrderByAttemptNumberDesc(userId, labId);

        Map<UUID, List<SubmissionChallengeResult>> challengeResultsBySubmission =
                loadChallengeResultsBySubmission(submissions);

        List<StudentSubmissionHistoryItemDTO> items = submissions.stream()
                .map(submission -> toHistoryItem(submission, challengeResultsBySubmission.getOrDefault(
                        submission.getId(), List.of())))
                .toList();

        StudentHistoryStatsDTO stats = computeStats(submissions, labId);
        return new StudentHistoryResponse(items, stats);
    }

    private Map<UUID, List<SubmissionChallengeResult>> loadChallengeResultsBySubmission(List<LabSubmission> submissions) {
        if (submissions.isEmpty()) {
            return Map.of();
        }
        List<UUID> submissionIds = submissions.stream().map(LabSubmission::getId).toList();
        List<SubmissionChallengeResult> allResults =
                submissionChallengeResultRepository.findBySubmission_IdInWithChallenge(submissionIds);
        Map<UUID, List<SubmissionChallengeResult>> grouped = new HashMap<>();
        for (SubmissionChallengeResult result : allResults) {
            UUID submissionId = result.getSubmission().getId();
            grouped.computeIfAbsent(submissionId, ignored -> new ArrayList<>()).add(result);
        }
        for (List<SubmissionChallengeResult> results : grouped.values()) {
            results.sort(Comparator.comparing(r -> r.getChallenge().getChallengeNumber()));
        }
        return grouped;
    }

    private StudentLabSummaryDTO toLabSummary(StudentLabProgress progress) {
        return new StudentLabSummaryDTO(
                progress.getLab().getId(),
                progress.getLab().getName(),
                progress.getHighestScore(),
                progress.getAttemptsCount() != null ? progress.getAttemptsCount() : 0,
                formatTimestamp(progress.getLastSubmittedAt()));
    }

    private StudentSubmissionHistoryItemDTO toHistoryItem(
            LabSubmission submission,
            List<SubmissionChallengeResult> challengeResults) {
        List<StudentChallengeResultDTO> challengeDtos = resolveChallengeResults(submission, challengeResults);
        String status = deriveStatus(submission.getScore(), challengeDtos);
        return new StudentSubmissionHistoryItemDTO(
                submission.getId(),
                new StudentLabRefDTO(submission.getLab().getId(), submission.getLab().getName()),
                submission.getAttemptNumber(),
                submission.getScore(),
                formatTimestamp(submission.getSubmittedAt()),
                status,
                challengeDtos);
    }

    private List<StudentChallengeResultDTO> resolveChallengeResults(
            LabSubmission submission,
            List<SubmissionChallengeResult> storedResults) {
        if (!storedResults.isEmpty()) {
            return storedResults.stream().map(this::toChallengeResult).toList();
        }

        List<ChallengeBreakdownDTO> breakdown = challengeService.getChallengeBreakdownForSubmission(
                submission.getId(), submission.getLab().getId());

        if (breakdown.isEmpty() && submission.getScore() != null) {
            int overall = submission.getScore().setScale(0, RoundingMode.HALF_UP).intValue();
            breakdown = List.of(new ChallengeBreakdownDTO("Lab total", overall >= 100, overall));
        }

        return breakdown.stream()
                .map(b -> new StudentChallengeResultDTO(b.challengeName(), b.isCorrect(), b.score()))
                .toList();
    }

    String deriveStatus(BigDecimal overallScore, List<StudentChallengeResultDTO> challengeResults) {
        if (overallScore == null) {
            return "unknown";
        }
        if (overallScore.compareTo(FAIL_THRESHOLD) < 0) {
            return "failed";
        }
        if (overallScore.compareTo(PASS_THRESHOLD) > 0) {
            return "passed";
        }
        return "partial";
    }

    private StudentChallengeResultDTO toChallengeResult(SubmissionChallengeResult result) {
        Integer score = result.getScore() == null
                ? null
                : result.getScore().setScale(0, RoundingMode.HALF_UP).intValue();
        return new StudentChallengeResultDTO(
                result.getChallenge().getName(),
                result.isCorrect(),
                score);
    }

    StudentHistoryStatsDTO computeStats(List<LabSubmission> submissions, UUID filterLabId) {
        if (submissions.isEmpty()) {
            return new StudentHistoryStatsDTO(0, 0, null, null);
        }

        int totalSubmissions = submissions.size();
        int labsAttempted = filterLabId != null
                ? 1
                : submissions.stream()
                        .map(s -> s.getLab().getId())
                        .collect(Collectors.toSet())
                        .size();

        List<BigDecimal> scores = submissions.stream()
                .map(LabSubmission::getScore)
                .filter(Objects::nonNull)
                .toList();

        BigDecimal averageScore = null;
        if (!scores.isEmpty()) {
            BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            averageScore = sum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
        }

        BigDecimal bestScore = scores.stream().max(BigDecimal::compareTo).orElse(null);

        return new StudentHistoryStatsDTO(labsAttempted, totalSubmissions, averageScore, bestScore);
    }

    private String formatTimestamp(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
