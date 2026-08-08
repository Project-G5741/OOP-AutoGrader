package com.eiu.capstone.backend.service;

import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailService {

    private final TransactionalEmailSender emailSender;

    public PasswordResetEmailService(TransactionalEmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void sendResetLink(String toEmail, String resetUrl) {
        emailSender.sendPlainText(
                toEmail,
                "Reset your OOP AutoGrader password",
                """
                You requested a password reset for your OOP AutoGrader account.

                Click the link below to set a new password (valid for 15 minutes):
                %s

                If you did not request this, you can ignore this email.
                """.formatted(resetUrl).trim());
    }
}
