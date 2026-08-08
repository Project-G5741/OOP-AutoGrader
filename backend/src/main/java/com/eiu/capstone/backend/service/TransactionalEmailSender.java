package com.eiu.capstone.backend.service;

public interface TransactionalEmailSender {

    void sendPlainText(String toEmail, String subject, String body);
}
