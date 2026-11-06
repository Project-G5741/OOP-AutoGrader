package com.eiu.capstone.backend.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!desktop")
@ConditionalOnProperty(name = "app.deadline-reminder.enabled", havingValue = "true", matchIfMissing = true)
public class LabDeadlineReminderScheduler {

    private final LabDeadlineEmailService labDeadlineEmailService;

    public LabDeadlineReminderScheduler(LabDeadlineEmailService labDeadlineEmailService) {
        this.labDeadlineEmailService = labDeadlineEmailService;
    }

    @Scheduled(fixedRateString = "${app.deadline-reminder.fixed-rate-ms:3600000}")
    public void sendDeadlineReminders() {
        try {
            labDeadlineEmailService.processThreshold((short) 72);
            labDeadlineEmailService.processThreshold((short) 24);
        } catch (Exception ex) {
            // Schema drift or transient DB errors should not spam the log on every tick.
            org.slf4j.LoggerFactory.getLogger(LabDeadlineReminderScheduler.class)
                    .warn("Deadline reminder job skipped: {}", ex.getMessage());
        }
    }
}
