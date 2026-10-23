package com.eiu.capstone.backend.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.StudentStreakView;
import com.eiu.capstone.backend.model.StudentStreak;
import com.eiu.capstone.backend.repository.StudentStreakRepository;
import com.eiu.capstone.backend.utility.TimeUtil;

import jakarta.persistence.EntityManager;

@Service
public class StudentStreakService {

    private final StudentStreakRepository repository;
    private final EntityManager entityManager;

    public StudentStreakService(StudentStreakRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Transactional
    public StudentStreakView visit(UUID userId) {
        return mutate(userId, row -> row.setLoggedIn(true));
    }

    @Transactional
    public StudentStreakView pet(UUID userId) {
        return mutate(userId, DailyStreakRules::addPet);
    }

    @Transactional
    public StudentStreakView restore(UUID userId) {
        return mutate(userId, row -> {
            try {
                DailyStreakRules.restore(row, today());
            } catch (IllegalStateException ex) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
            }
        });
    }

    @Transactional
    public void recordSubmission(UUID userId) {
        if (userId == null) {
            return;
        }
        mutate(userId, DailyStreakRules::addSubmit);
    }

    private StudentStreakView mutate(UUID userId, Consumer<StudentStreak> action) {
        LocalDate today = today();
        StudentStreak row = locked(userId);
        DailyStreakRules.roll(row, today);
        boolean loggedInBefore = row.isLoggedIn();
        int submitBefore = row.getSubmitCount();
        int petBefore = row.getPetCount();
        action.accept(row);
        DailyStreakRules.awardIfReady(row, today);
        boolean missionCompletedNow = DailyStreakRules.missionJustFinished(
                loggedInBefore, submitBefore, petBefore, row);
        repository.save(row);
        return DailyStreakRules.toView(row, today, missionCompletedNow);
    }

    @Transactional(readOnly = true)
    public Optional<StudentStreak> find(UUID userId) {
        return repository.findById(userId);
    }

    @Transactional
    public StudentStreak lock(UUID userId) {
        return locked(userId);
    }

    @Transactional
    public StudentStreak save(StudentStreak row) {
        return repository.save(row);
    }

    @Transactional
    public void addPoints(UUID userId, int amount) {
        if (userId == null || amount <= 0) {
            return;
        }
        StudentStreak row = locked(userId);
        row.setPoints(row.getPoints() + amount);
        repository.save(row);
    }

    private StudentStreak locked(UUID userId) {
        entityManager.createNativeQuery("""
                INSERT INTO student_daily_streak (
                    user_id, streak_count, points, logged_in, submit_count, pet_count,
                    awarded_today, streak_broken, saved_streak, equipped_mascot
                ) VALUES (?1, 0, 0, FALSE, 0, 0, FALSE, FALSE, 0, 'fox')
                ON CONFLICT (user_id) DO NOTHING
                """)
                .setParameter(1, userId)
                .executeUpdate();
        entityManager.flush();
        return repository.lockByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Streak row missing"));
    }

    private static LocalDate today() {
        return LocalDate.now(TimeUtil.VIETNAM_ZONE);
    }
}
