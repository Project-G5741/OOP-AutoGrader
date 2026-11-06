package com.eiu.capstone.backend.service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.UserAccountRepository;

/**
 * Source of truth for JWT {@code sv} checks. DB {@code session_version} wins;
 * in-process cache avoids a round-trip on every request after the first lookup.
 */
@Service
public class SessionValidityService {

    private final UserAccountRepository userAccountRepository;
    private final ConcurrentHashMap<String, Integer> versionByEmail = new ConcurrentHashMap<>();

    public SessionValidityService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public boolean isSessionValid(String email, Integer tokenSessionVersion) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String key = normalize(email);
        int claimed = tokenSessionVersion == null ? 0 : tokenSessionVersion;
        Integer cached = versionByEmail.get(key);
        if (cached != null) {
            return claimed == cached;
        }
        Optional<Integer> fromDb = userAccountRepository.findSessionVersionByEmailIgnoreCase(key);
        if (fromDb.isEmpty()) {
            return false;
        }
        int expected = fromDb.get();
        versionByEmail.put(key, expected);
        return claimed == expected;
    }

    @Transactional
    public void bumpSessionVersion(UserAccount user) {
        if (user == null) {
            return;
        }
        user.setSessionVersion(user.getSessionVersion() + 1);
        userAccountRepository.save(user);
        putCache(user.getEmail(), user.getSessionVersion());
    }

    @Transactional
    public void bumpSessionVersion(UUID userId) {
        if (userId == null) {
            return;
        }
        UserAccount user = userAccountRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        bumpSessionVersion(user);
    }

    /** Drop cached version so the next check reloads (or rejects if the row is gone). */
    public void invalidateCachedEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        versionByEmail.remove(normalize(email));
    }

    private void putCache(String email, int version) {
        if (email == null || email.isBlank()) {
            return;
        }
        versionByEmail.put(normalize(email), version);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
