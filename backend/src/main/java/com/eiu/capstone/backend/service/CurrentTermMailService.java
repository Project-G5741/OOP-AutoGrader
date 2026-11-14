package com.eiu.capstone.backend.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
        List<UserAccount> admins = userAccountRepository.findActiveAdmins();
        int skippedNoEmail = 0;
        int recipientCount = 0;
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<String> recipients = new ArrayList<>();
        for (UserAccount account : students) {
            recipientCount++;
            addRecipient(account, seen, recipients);
        }
        for (UserAccount admin : admins) {
            String email = normalizeEmail(admin.getEmail());
            if (email == null) {
                skippedNoEmail++;
                recipientCount++;
                continue;
            }
            if (seen.contains(email)) {
                continue;
            }
            recipientCount++;
            seen.add(email);
            recipients.add(email);
        }
        skippedNoEmail += students.size() - countAddresses(students);
        int sent = 0;
        int failed = 0;
        if (!recipients.isEmpty()) {
            try {
                failed = emailSender.sendPlainTextEach(recipients, subject, body);
                sent = recipients.size() - failed;
            } catch (RuntimeException ex) {
                log.warn("Current-term mail failed for {} recipients", recipients.size(), ex);
                failed = recipients.size();
            }
        }
        return new CurrentTermMailResult(recipientCount, sent, failed, skippedNoEmail);
    }

    private static void addRecipient(UserAccount account, LinkedHashSet<String> seen, List<String> recipients) {
        String email = normalizeEmail(account.getEmail());
        if (email == null || !seen.add(email)) {
            return;
        }
        recipients.add(email);
    }

    private static int countAddresses(List<UserAccount> accounts) {
        int count = 0;
        for (UserAccount account : accounts) {
            if (normalizeEmail(account.getEmail()) != null) {
                count++;
            }
        }
        return count;
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
