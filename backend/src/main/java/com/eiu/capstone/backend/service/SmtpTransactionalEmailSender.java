package com.eiu.capstone.backend.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Profile("!desktop & !desktop-lecturer")
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpTransactionalEmailSender implements TransactionalEmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpTransactionalEmailSender.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpTransactionalEmailSender(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendPlainText(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    @Override
    public int sendPlainTextEach(List<String> recipients, String subject, String body) {
        if (recipients.isEmpty()) {
            return 0;
        }
        SimpleMailMessage[] messages = new SimpleMailMessage[recipients.size()];
        for (int i = 0; i < recipients.size(); i++) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipients.get(i));
            message.setSubject(subject);
            message.setText(body);
            messages[i] = message;
        }
        try {
            mailSender.send(messages);
            return 0;
        } catch (MailSendException ex) {
            Map<Object, Exception> failed = ex.getFailedMessages();
            for (Map.Entry<Object, Exception> entry : failed.entrySet()) {
                log.warn("SMTP mail failed for {}", recipientOf(entry.getKey()), entry.getValue());
            }
            int failedCount = failed.isEmpty() ? recipients.size() : failed.size();
            return Math.min(failedCount, recipients.size());
        }
    }

    private static String recipientOf(Object message) {
        if (message instanceof SimpleMailMessage mail && mail.getTo() != null && mail.getTo().length > 0) {
            return mail.getTo()[0];
        }
        return "unknown";
    }
}
