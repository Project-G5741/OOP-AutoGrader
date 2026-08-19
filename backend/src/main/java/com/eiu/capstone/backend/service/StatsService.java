package com.eiu.capstone.backend.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.DTO.StatsDTO;
import com.eiu.capstone.backend.repository.StatsRepository;
import com.eiu.capstone.backend.utility.TimeUtil;
import com.eiu.capstone.backend.utility.TimingLog;

@Service
public class StatsService {

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

        int submissionCount = row.submissionCount();
        Integer totalSubmissions = submissionCount == 0 ? null : submissionCount;

        String latestSubmission = TimeUtil.formatLatestSubmission(row.latestSubmittedAtOffset());

        StatsDTO result = new StatsDTO(currentGrade, totalSubmissions, latestSubmission);

        TimingLog.line(timingLog, "Read stats", System.currentTimeMillis() - start);
        return result;
    }
}
