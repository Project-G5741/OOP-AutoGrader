package com.eiu.capstone.backend.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.exception.GoogleTokenVerificationException;
import com.eiu.capstone.backend.model.GoogleTokenInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GoogleTokenVerifier {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${google.client-id:}")
    private String googleClientId;

    @Value("${allowed-domain}")
    private String allowedDomain;

    public GoogleTokenVerifier(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public GoogleTokenInfo verify(String idToken) {
        try {
            var encodedToken = URLEncoder.encode(idToken, StandardCharsets.UTF_8);
            var uri = URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + encodedToken);
            var request = HttpRequest.newBuilder(uri)
                    .GET()
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new GoogleTokenVerificationException("Google token validation failed.", response.body());
            }

            var tokenInfo = objectMapper.readValue(response.body(), GoogleTokenInfo.class);
            validateTokenInfo(tokenInfo);
            return tokenInfo;
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new GoogleTokenVerificationException("Unable to verify Google token.", ex.getMessage());
        }
    }

    private void validateTokenInfo(GoogleTokenInfo tokenInfo) {
        if (tokenInfo.getEmail() == null || !tokenInfo.isEmailVerified()) {
            throw new GoogleTokenVerificationException("Email is not verified by Google.", "email_verified is false or email is missing.");
        }

        if (googleClientId != null && !googleClientId.isBlank() && !googleClientId.equals(tokenInfo.getAud())) {
            throw new GoogleTokenVerificationException("Token audience mismatch.", "The token was not issued for the configured Google client ID.");
        }

        if (tokenInfo.getExp() != null && Instant.ofEpochSecond(tokenInfo.getExp()).isBefore(Instant.now())) {
            throw new GoogleTokenVerificationException("Google token has expired.", "The token expiration time is in the past.");
        }

        if (!allowedDomain.equalsIgnoreCase(tokenInfo.getDomain())) {
            throw new GoogleTokenVerificationException("Email domain is not allowed.", "Only emails from the allowed domain are accepted.");
        }
    }
}
