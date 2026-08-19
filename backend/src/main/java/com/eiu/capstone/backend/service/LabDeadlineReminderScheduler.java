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
        labDeadlineEmailService.processThreshold((short) 72);
        labDeadlineEmailService.processThreshold((short) 24);
    }
}
