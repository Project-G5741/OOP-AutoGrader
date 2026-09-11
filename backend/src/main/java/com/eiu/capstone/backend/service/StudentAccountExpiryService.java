package com.eiu.capstone.backend.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.TermEnrollmentRepository;
import com.eiu.capstone.backend.repository.TermRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;

@Service
public class StudentAccountExpiryService {

    private static final Logger log = LoggerFactory.getLogger(StudentAccountExpiryService.class);

    /** Quarters after first enrollment when the account is removed (delete in the 4th quarter). */
    static final int RETENTION_QUARTERS = 3;

    private final TermRepository termRepository;
    private final TermEnrollmentRepository termEnrollmentRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserService userService;

    public StudentAccountExpiryService(TermRepository termRepository,
                                       TermEnrollmentRepository termEnrollmentRepository,
                                       UserAccountRepository userAccountRepository,
                                       UserService userService) {
        this.termRepository = termRepository;
        this.termEnrollmentRepository = termEnrollmentRepository;
        this.userAccountRepository = userAccountRepository;
        this.userService = userService;
    }

    /**
     * Runs {@link #purgeExpiredStudents()} after the surrounding transaction commits so
     * quarter create/set-current APIs return immediately.
     */
    public void schedulePurgeAfterCurrentTermChange() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runPurgeAsync();
                }
            });
            return;
        }
        runPurgeAsync();
    }

    private void runPurgeAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                int deleted = purgeExpiredStudents();
                if (deleted > 0) {
                    log.info("Background student account expiry removed {} account(s)", deleted);
                }
            } catch (Exception ex) {
                log.warn("Background student account expiry failed: {}", ex.getMessage());
            }
        });
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int purgeExpiredStudents() {
        Term current = termRepository.findCurrentWithAcademicYear().orElse(null);
        if (current == null) {
            return 0;
        }
        int currentOrdinal = TermOrdinal.fromTerm(current);
        int maxFirstOrdinal = currentOrdinal - RETENTION_QUARTERS;
        if (maxFirstOrdinal < 0) {
            return 0;
        }

        List<UUID> candidateIds = termEnrollmentRepository.findStudentIdsWithFirstEnrollmentOrdinalAtMost(maxFirstOrdinal);
        int deleted = 0;
        for (UUID userId : candidateIds) {
            UserAccount user = userAccountRepository.findAllWithRolesByIdIn(List.of(userId)).stream()
                    .findFirst()
                    .orElse(null);
            if (user == null || !user.getIsActive() || !isStudentOnly(user)) {
                continue;
            }
            userService.deleteUser(userId);
            deleted++;
            log.info("Deleted expired student account {} (IRN {})", userId, user.getStudentCode());
        }
        return deleted;
    }

    private boolean isStudentOnly(UserAccount user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return false;
        }
        boolean student = false;
        boolean lecturer = false;
        for (var role : user.getRoles()) {
            String name = role.getName() == null ? "" : role.getName().trim().toUpperCase(Locale.ROOT);
            if ("STUDENT".equals(name)) {
                student = true;
            }
            if ("LECTURER".equals(name) || "TEACHER".equals(name)) {
                lecturer = true;
            }
        }
        return student && !lecturer;
    }
}
