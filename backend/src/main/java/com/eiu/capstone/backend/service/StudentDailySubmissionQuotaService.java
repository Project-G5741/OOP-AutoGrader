package com.eiu.capstone.backend.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eiu.capstone.backend.DTO.StudentDailySubmissionQuotaView;
import com.eiu.capstone.backend.exception.DailySubmissionQuotaExceededException;
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.utility.TimeUtil;

@Service
@Profile("!desktop")
public class StudentDailySubmissionQuotaService {

    private final LabSubmissionRepository labSubmissionRepository;
    private final int dailyLimit;

    public StudentDailySubmissionQuotaService(
            LabSubmissionRepository labSubmissionRepository,
            @Value("${app.submission.daily-max-per-student:30}") int dailyLimit) {
        this.labSubmissionRepository = labSubmissionRepository;
        this.dailyLimit = Math.max(1, dailyLimit);
    }

    @Transactional(readOnly = true)
    public StudentDailySubmissionQuotaView viewFor(UUID userId) {
        int used = countPersistedToday(userId);
        int remaining = Math.max(0, dailyLimit - used);
        return new StudentDailySubmissionQuotaView(used, dailyLimit, remaining, used < dailyLimit);
    }

    @Transactional(readOnly = true)
    public void requireCanSubmit(UUID userId) {
        if (userId == null) {
            return;
        }
        int used = countPersistedToday(userId);
        if (used >= dailyLimit) {
            throw quotaExceeded(used);
        }
    }

    int dailyLimit() {
        return dailyLimit;
    }

    private int countPersistedToday(UUID userId) {
        VietnamDayBounds bounds = vietnamDayBounds(LocalDate.now(TimeUtil.VIETNAM_ZONE));
        return (int) labSubmissionRepository.countByUser_IdAndSubmittedAtBetween(
                userId, bounds.startInclusive(), bounds.endExclusive());
    }

    private DailySubmissionQuotaExceededException quotaExceeded(int used) {
        String message = String.format(
                "You have reached today's submission limit (%d/%d). You can submit again after midnight (Vietnam time).",
                used,
                dailyLimit);
        return new DailySubmissionQuotaExceededException(message);
    }

    static VietnamDayBounds vietnamDayBounds(LocalDate vietnamDate) {
        OffsetDateTime start = vietnamDate.atStartOfDay(TimeUtil.VIETNAM_ZONE).toOffsetDateTime();
        OffsetDateTime end = vietnamDate.plusDays(1).atStartOfDay(TimeUtil.VIETNAM_ZONE).toOffsetDateTime();
        return new VietnamDayBounds(start, end);
    }

    record VietnamDayBounds(OffsetDateTime startInclusive, OffsetDateTime endExclusive) {}
}
