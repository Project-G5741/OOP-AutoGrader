package com.eiu.capstone.backend.service;

import java.util.List;

public interface TransactionalEmailSender {

    void sendPlainText(String toEmail, String subject, String body);

    /**
     * Same subject and body to each address. One recipient failure does not stop the rest.
     *
     * @return how many addresses failed
     */
    default int sendPlainTextEach(List<String> recipients, String subject, String body) {
        int failed = 0;
        for (String recipient : recipients) {
            try {
                sendPlainText(recipient, subject, body);
            } catch (RuntimeException ex) {
                failed++;
            }
        }
        return failed;
    }
}
