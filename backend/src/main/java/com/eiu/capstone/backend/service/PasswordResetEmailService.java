package com.eiu.capstone.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public PasswordResetEmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendResetLink(String toEmail, String resetUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Reset your OOP AutoGrader password");
        message.setText("""
                You requested a password reset for your OOP AutoGrader account.

                Click the link below to set a new password (valid for 15 minutes):
                %s

                If you did not request this, you can ignore this email.
                """.formatted(resetUrl).trim());
        mailSender.send(message);
    }
}
