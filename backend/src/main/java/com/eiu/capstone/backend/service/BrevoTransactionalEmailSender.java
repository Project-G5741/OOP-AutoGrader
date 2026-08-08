package com.eiu.capstone.backend.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "brevo")
public class BrevoTransactionalEmailSender implements TransactionalEmailSender {

    private static final Logger log = LoggerFactory.getLogger(BrevoTransactionalEmailSender.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestClient restClient;
    private final String fromAddress;
    private final String fromName;

    public BrevoTransactionalEmailSender(
            @Value("${app.mail.brevo.api-key}") String apiKey,
            @Value("${app.mail.from}") String fromAddress,
            @Value("${app.mail.from-name:OOP AutoGrader}") String fromName) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("BREVO_API_KEY is required when MAIL_PROVIDER=brevo");
        }
        if (apiKey.startsWith("xsmtpsib-")) {
            throw new IllegalStateException(
                    "BREVO_API_KEY looks like an SMTP key (xsmtpsib-). "
                            + "Create a v3 API key (xkeysib-) under Brevo → SMTP & API → API keys.");
        }
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.restClient = RestClient.builder()
                .baseUrl(BREVO_API_URL)
                .defaultHeader("api-key", apiKey)
                .build();
    }

    @Override
    public void sendPlainText(String toEmail, String subject, String body) {
        Map<String, Object> payload = Map.of(
                "sender", Map.of("email", fromAddress, "name", fromName),
                "to", List.of(Map.of("email", toEmail)),
                "subject", subject,
                "textContent", body);

        try {
            restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes());
                        throw new MailSendException(
                                "Brevo API returned HTTP " + response.getStatusCode().value() + ": " + errorBody);
                    })
                    .toBodilessEntity();
        } catch (MailSendException ex) {
            log.error("Brevo email send failed for recipient {}", toEmail, ex);
            throw ex;
        } catch (Exception ex) {
            throw new MailSendException("Failed to send email via Brevo", ex);
        }
    }
}
