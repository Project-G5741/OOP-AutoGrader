package com.eiu.capstone.backend.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.CurrentTermMailRequest;
import com.eiu.capstone.backend.DTO.CurrentTermMailResult;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.TermEnrollmentRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;

@Service
@Profile("!desktop")
public class CurrentTermMailService {

    private static final Logger log = LoggerFactory.getLogger(CurrentTermMailService.class);

    private final TermService termService;
    private final TermEnrollmentRepository termEnrollmentRepository;
    private final UserAccountRepository userAccountRepository;
    private final TransactionalEmailSender emailSender;

    public CurrentTermMailService(
            TermService termService,
            TermEnrollmentRepository termEnrollmentRepository,
            UserAccountRepository userAccountRepository,
            TransactionalEmailSender emailSender) {
        this.termService = termService;
        this.termEnrollmentRepository = termEnrollmentRepository;
        this.userAccountRepository = userAccountRepository;
        this.emailSender = emailSender;
    }

    public CurrentTermMailResult sendToCurrentTerm(CurrentTermMailRequest request) {
        String subject = request.subject().trim();
        String body = request.body().trim();
        Term current = termService.findCurrentTerm()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "No current quarter is set"));
        List<UUID> studentIds = termEnrollmentRepository.findActiveStudentIdsByTermId(current.getId());
        if (studentIds.isEmpty()) {
            return new CurrentTermMailResult(0, 0, 0, 0);
        }
        List<UserAccount> students = userAccountRepository.findAllById(studentIds);
        int sent = 0;
        int failed = 0;
        int skippedNoEmail = 0;
        for (UserAccount student : students) {
            String email = student.getEmail();
            if (email == null || email.isBlank()) {
                skippedNoEmail++;
                continue;
            }
            try {
                emailSender.sendPlainText(email.trim(), subject, body);
                sent++;
            } catch (RuntimeException ex) {
                log.warn("Current-term mail failed for {}", email, ex);
                failed++;
            }
        }
        return new CurrentTermMailResult(students.size(), sent, failed, skippedNoEmail);
    }
}
