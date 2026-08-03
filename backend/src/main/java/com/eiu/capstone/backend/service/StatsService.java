package com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.DTO.StatsDTO;
import com.eiu.capstone.backend.model.StudentLabProgress;
import com.eiu.capstone.backend.repository.StudentLabProgressRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class StatsService {

    private static final DateTimeFormatter LATEST_SUBMISSION_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");

    private final StudentLabProgressRepository studentLabProgressRepository;

    public StatsService(StudentLabProgressRepository studentLabProgressRepository) {
        this.studentLabProgressRepository = studentLabProgressRepository;
    }

    /**
     * Stats are tracked per (student, lab) via student_lab_progress, not per
     * challenge — there's no challengeId param here even though the
     * frontend's route includes one; the controller just doesn't forward it.
     */
    public StatsDTO getStats(UUID labId, UUID studentId) {
        if (studentId == null) {
            return new StatsDTO(null, null, null);
        }

        return studentLabProgressRepository.findByUser_IdAndLab_Id(studentId, labId)
                .map(this::toDto)
                .orElse(new StatsDTO(null, null, null));
    }

    private StatsDTO toDto(StudentLabProgress progress) {
        Integer currentGrade = progress.getHighestScore() == null
                ? null
                : Math.round(progress.getHighestScore().floatValue());

        Integer totalSubmissions = progress.getAttemptsCount();

        // getLastSubmittedAt() returns OffsetDateTime, which already carries
        // its own offset — DateTimeFormatter can format it directly, no
        // zone conversion needed.
        String latestSubmission = progress.getLastSubmittedAt() == null
                ? null
                : LATEST_SUBMISSION_FORMAT.format(progress.getLastSubmittedAt());

        return new StatsDTO(currentGrade, totalSubmissions, latestSubmission);
    }
}