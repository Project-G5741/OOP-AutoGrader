package com.eiu.capstone.backend.ai;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Encrypts student Gemini API keys at rest (AES-GCM).
 * Secret: {@code APP_AI_CREDENTIAL_SECRET} or fallback {@code JWT_SECRET} (≥32 bytes).
 */
@Component
@Profile("!desktop")
public class AiCredentialCrypto {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public AiCredentialCrypto(
            @Value("${app.ai.credential-secret:}") String credentialSecret,
            @Value("${jwt.secret}") String jwtSecret) {
        String material = credentialSecret != null && !credentialSecret.isBlank()
                ? credentialSecret
                : jwtSecret;
        if (material == null || material.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "AI credential encryption needs APP_AI_CREDENTIAL_SECRET or JWT_SECRET (≥32 bytes)");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(material.getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(hashed, "AES");
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to initialise AI credential crypto", ex);
        }
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherBytes.length);
            buffer.put(iv);
            buffer.put(cipherBytes);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store API key");
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] all = Base64.getDecoder().decode(encoded);
            ByteBuffer buffer = ByteBuffer.wrap(all);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] cipherBytes = new byte[buffer.remaining()];
            buffer.get(cipherBytes);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read API key");
        }
    }

    public static String hintFor(String apiKey) {
        String trimmed = apiKey == null ? "" : apiKey.trim();
        if (trimmed.length() <= 8) {
            return "••••";
        }
        return trimmed.substring(0, 4) + "…" + trimmed.substring(trimmed.length() - 4);
    }
}
