package com.eiu.capstone.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.model.FeedbackItem;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.UserAccountRepository;

@Service
@Profile("!desktop")
public class FeedbackAdminMailService {

    public static final String SUBJECT = "Feedback OOP - AutoGrader";

    private static final Logger log = LoggerFactory.getLogger(FeedbackAdminMailService.class);

    private final UserAccountRepository userAccountRepository;
    private final TransactionalEmailSender emailSender;

    public FeedbackAdminMailService(
            UserAccountRepository userAccountRepository,
            TransactionalEmailSender emailSender) {
        this.userAccountRepository = userAccountRepository;
        this.emailSender = emailSender;
    }

    public void notifyAdmins(FeedbackItem item) {
        List<UserAccount> admins = userAccountRepository.findActiveAdmins();
        String body = buildBody(item);
        for (UserAccount admin : admins) {
            String email = admin.getEmail();
            if (email == null || email.isBlank()) {
                continue;
            }
            try {
                emailSender.sendPlainText(email.trim(), SUBJECT, body);
            } catch (RuntimeException ex) {
                log.warn("Feedback admin mail failed for {}", email, ex);
            }
        }
    }

    static String buildBody(FeedbackItem item) {
        String type = item.getItemType() == null ? "" : item.getItemType();
        String name = item.getAuthorName() == null ? "" : item.getAuthorName();
        String irn = item.getAuthorIrn() == null ? "" : item.getAuthorIrn();
        String message = item.getMessage() == null ? "" : item.getMessage();
        return """
                Type: %s
                Author: %s (%s)

                %s
                """.formatted(type, name, irn, message).trim();
    }
}
