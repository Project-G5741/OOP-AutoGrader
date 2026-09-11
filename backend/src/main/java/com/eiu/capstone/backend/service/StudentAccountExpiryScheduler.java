package com.eiu.capstone.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StudentAccountExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(StudentAccountExpiryScheduler.class);

    private final StudentAccountExpiryService studentAccountExpiryService;

    public StudentAccountExpiryScheduler(StudentAccountExpiryService studentAccountExpiryService) {
        this.studentAccountExpiryService = studentAccountExpiryService;
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Ho_Chi_Minh")
    public void purgeExpiredStudentsDaily() {
        try {
            int deleted = studentAccountExpiryService.purgeExpiredStudents();
            if (deleted > 0) {
                log.info("Student account expiry job removed {} account(s)", deleted);
            }
        } catch (Exception ex) {
            log.warn("Student account expiry job skipped: {}", ex.getMessage());
        }
    }
}
