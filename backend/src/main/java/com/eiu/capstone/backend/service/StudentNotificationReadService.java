package com.eiu.capstone.backend.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.StudentNotificationReadKeysDTO;
import com.eiu.capstone.backend.model.StudentNotificationRead;
import com.eiu.capstone.backend.model.StudentNotificationReadKey;
import com.eiu.capstone.backend.repository.StudentNotificationReadRepository;

@Service
@Profile("!desktop & !desktop-lecturer")
public class StudentNotificationReadService {

    static final int MAX_KEY_LENGTH = 191;

    private static final String[] ALLOWED_PREFIXES = {
            "deadline-countdown-",
            "deadline-expired-",
            "no-submit-",
            "feedback-reply-"
    };

    private final StudentNotificationReadRepository repository;

    public StudentNotificationReadService(StudentNotificationReadRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public StudentNotificationReadKeysDTO listKeys(UUID userId) {
        List<String> keys = repository.findByIdUserId(userId).stream()
                .map(row -> row.getId().getNotifKey())
                .toList();
        return new StudentNotificationReadKeysDTO(keys);
    }

    @Transactional
    public void markRead(UUID userId, String rawKey) {
        String key = normalizeAndValidate(rawKey);
        if (repository.existsByIdUserIdAndIdNotifKey(userId, key)) {
            return;
        }
        StudentNotificationRead row = new StudentNotificationRead();
        row.setId(new StudentNotificationReadKey(userId, key));
        row.setReadAt(OffsetDateTime.now());
        repository.save(row);
    }

    static String normalizeAndValidate(String rawKey) {
        if (rawKey == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "key required");
        }
        String key = rawKey.trim();
        if (key.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "key required");
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "key too long");
        }
        if (!hasAllowedPrefix(key)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "key not allowed");
        }
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c < 0x20 || c > 0x7E || Character.isWhitespace(c)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "key not allowed");
            }
        }
        return key;
    }

    private static boolean hasAllowedPrefix(String key) {
        for (String prefix : ALLOWED_PREFIXES) {
            if (key.startsWith(prefix) && key.length() > prefix.length()) {
                return true;
            }
        }
        return false;
    }
}
