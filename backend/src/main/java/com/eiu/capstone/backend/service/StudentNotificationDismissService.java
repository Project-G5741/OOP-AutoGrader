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
import com.eiu.capstone.backend.model.StudentNotificationDismiss;
import com.eiu.capstone.backend.model.StudentNotificationDismissKey;
import com.eiu.capstone.backend.repository.StudentNotificationDismissRepository;
import com.eiu.capstone.backend.repository.StudentNotificationReadRepository;

@Service
@Profile("!desktop & !desktop-lecturer")
public class StudentNotificationDismissService {

    public static final int MAX_BATCH_SIZE = 50;

    private final StudentNotificationDismissRepository dismissRepository;
    private final StudentNotificationReadRepository readRepository;
    private final FeedbackService feedbackService;

    public StudentNotificationDismissService(
            StudentNotificationDismissRepository dismissRepository,
            StudentNotificationReadRepository readRepository,
            FeedbackService feedbackService) {
        this.dismissRepository = dismissRepository;
        this.readRepository = readRepository;
        this.feedbackService = feedbackService;
    }

    @Transactional(readOnly = true)
    public StudentNotificationReadKeysDTO listKeys(UUID userId) {
        List<String> keys = dismissRepository.findByIdUserId(userId).stream()
                .map(row -> row.getId().getNotifKey())
                .toList();
        return new StudentNotificationReadKeysDTO(keys);
    }

    @Transactional
    public void dismiss(UUID userId, String rawKey) {
        String key = StudentNotificationReadService.normalizeAndValidate(rawKey);
        dismissValidated(userId, key);
    }

    @Transactional
    public void dismissBatch(UUID userId, List<String> rawKeys) {
        if (rawKeys == null || rawKeys.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "keys required");
        }
        if (rawKeys.size() > MAX_BATCH_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "too many keys");
        }
        for (String rawKey : rawKeys) {
            String key = StudentNotificationReadService.normalizeAndValidate(rawKey);
            dismissValidated(userId, key);
        }
    }

    private void dismissValidated(UUID userId, String key) {
        if (!readRepository.existsByIdUserIdAndIdNotifKey(userId, key)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "must mark read first");
        }
        if (dismissRepository.existsByIdUserIdAndIdNotifKey(userId, key)) {
            return;
        }
        StudentNotificationDismiss row = new StudentNotificationDismiss();
        row.setId(new StudentNotificationDismissKey(userId, key));
        row.setDismissedAt(OffsetDateTime.now());
        dismissRepository.save(row);
        feedbackService.clearAuthorReplySnapshotFromNotifKey(userId, key);
    }
}
