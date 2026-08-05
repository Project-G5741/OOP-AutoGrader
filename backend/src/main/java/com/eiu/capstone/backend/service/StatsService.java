package com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.DTO.StatsDTO;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.StudentLabProgress;
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.repository.StudentLabProgressRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class StatsService {

    private static final DateTimeFormatter LATEST_SUBMISSION_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");

    private final StudentLabProgressRepository studentLabProgressRepository;
    private final LabSubmissionRepository labSubmissionRepository;
    private final boolean timingLog;

    public StatsService(StudentLabProgressRepository studentLabProgressRepository,
                        LabSubmissionRepository labSubmissionRepository,
                        @Value("${app.grading.timing-log:false}") boolean timingLog) {
        this.studentLabProgressRepository = studentLabProgressRepository;
        this.labSubmissionRepository = labSubmissionRepository;
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

        var progress = studentLabProgressRepository.findByUser_IdAndLab_Id(studentId, labId);
        long submissionRows = labSubmissionRepository.countByUser_IdAndLab_Id(studentId, labId);
        if (submissionRows == 0 && progress.isEmpty()) {
            return new StatsDTO(null, null, null);
        }

        StatsDTO result = toDto(progress.orElse(null), labId, studentId);

        if (timingLog) {
            System.out.printf("read_timing stats_ms=%d%n", System.currentTimeMillis() - start);
        }
        return result;
    }

    private StatsDTO toDto(StudentLabProgress progress, UUID labId, UUID studentId) {
        Integer currentGrade = labSubmissionRepository
                .findFirstByUser_IdAndLab_IdOrderByAttemptNumberDesc(studentId, labId)
                .map(LabSubmission::getScore)
                .map(score -> Math.round(score.floatValue()))
                .orElse(null);

        int submissionRows = (int) labSubmissionRepository.countByUser_IdAndLab_Id(studentId, labId);
        int attemptsFromProgress = progress != null && progress.getAttemptsCount() != null
                ? progress.getAttemptsCount()
                : 0;
        int total = Math.max(submissionRows, attemptsFromProgress);
        Integer totalSubmissions = total == 0 ? null : total;

        String latestSubmission = null;
        if (progress != null && progress.getLastSubmittedAt() != null) {
            latestSubmission = LATEST_SUBMISSION_FORMAT.format(progress.getLastSubmittedAt());
        } else {
            latestSubmission = labSubmissionRepository
                    .findFirstByUser_IdAndLab_IdOrderByAttemptNumberDesc(studentId, labId)
                    .map(LabSubmission::getSubmittedAt)
                    .map(LATEST_SUBMISSION_FORMAT::format)
                    .orElse(null);
        }

        return new StatsDTO(currentGrade, totalSubmissions, latestSubmission);
    }
}
