package com.eiu.capstone.backend.service;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.DTO.StatsDTO;
import com.eiu.capstone.backend.repository.StatsRepository;

@Service
public class StatsService {

    private static final DateTimeFormatter LATEST_SUBMISSION_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");

    private final StatsRepository statsRepository;
    private final boolean timingLog;

    public StatsService(StatsRepository statsRepository,
                        @Value("${app.grading.timing-log:false}") boolean timingLog) {
        this.statsRepository = statsRepository;
        this.timingLog = timingLog;
    }

    /**
     * Stats are tracked per (student, lab) via student_lab_progress, not per
     * challenge — there's no challengeId param here even though the
     * frontend's route includes one; the controller just doesn't forward it.
     */
    public StatsDTO getStats(UUID labId, UUID studentId) {
        long start = System.currentTimeMillis();
        if (studentId == null) {
            return new StatsDTO(null, null, null);
        }

        var statsRow = statsRepository.findStats(studentId, labId);
        if (statsRow.isEmpty()) {
            return new StatsDTO(null, null, null);
        }

        StatsRepository.StatsRow row = statsRow.get();
        if (row.submissionCount() == 0 && row.attemptsFromProgress() == null) {
            return new StatsDTO(null, null, null);
        }

        Integer currentGrade = row.latestScore() == null
                ? null
                : Math.round(row.latestScore().floatValue());

        int attemptsFromProgress = row.attemptsFromProgress() != null ? row.attemptsFromProgress() : 0;
        int total = Math.max(row.submissionCount(), attemptsFromProgress);
        Integer totalSubmissions = total == 0 ? null : total;

        String latestSubmission = null;
        if (row.latestSubmittedAtOffset() != null) {
            latestSubmission = LATEST_SUBMISSION_FORMAT.format(row.latestSubmittedAtOffset());
        } else if (row.latestSubmittedAtLocal() != null) {
            latestSubmission = LATEST_SUBMISSION_FORMAT.format(row.latestSubmittedAtLocal());
        }

        StatsDTO result = new StatsDTO(currentGrade, totalSubmissions, latestSubmission);

        if (timingLog) {
            System.out.printf("read_timing stats_ms=%d%n", System.currentTimeMillis() - start);
        }
        return result;
    }
}
