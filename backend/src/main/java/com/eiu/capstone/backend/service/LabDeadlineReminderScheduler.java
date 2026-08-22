package com.eiu.capstone.backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LabDeadlineReminderScheduler {

    private final LabDeadlineEmailService labDeadlineEmailService;

    public LabDeadlineReminderScheduler(LabDeadlineEmailService labDeadlineEmailService) {
        this.labDeadlineEmailService = labDeadlineEmailService;
    }

    @Scheduled(fixedRate = 60_000)
    public void sendDeadlineReminders() {
        try {
            labDeadlineEmailService.processThreshold((short) 72);
            labDeadlineEmailService.processThreshold((short) 24);
        } catch (Exception ex) {
            // Schema drift or transient DB errors should not spam the log every minute.
            org.slf4j.LoggerFactory.getLogger(LabDeadlineReminderScheduler.class)
                    .warn("Deadline reminder job skipped: {}", ex.getMessage());
        }
    }
}
