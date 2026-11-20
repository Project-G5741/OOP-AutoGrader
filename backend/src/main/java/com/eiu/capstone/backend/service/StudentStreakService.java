package com.eiu.capstone.backend.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.GachaSettleRequest;
import com.eiu.capstone.backend.DTO.StudentStreakView;
import com.eiu.capstone.backend.model.StudentStreak;
import com.eiu.capstone.backend.repository.StudentStreakRepository;
import com.eiu.capstone.backend.utility.TimeUtil;

import jakarta.persistence.EntityManager;

@Service
@Profile("!desktop & !desktop-lecturer")
public class StudentStreakService {

    private static final int GACHA_COST_SINGLE_POINTS = 100;
    private static final int GACHA_COST_MULTI_POINTS = 900;
    private static final int GACHA_MAX_PITY = 80;
    private static final int GACHA_MAX_POINTS_CASHBACK_PER_ROLL = 300;

    private final StudentStreakRepository repository;
    private final EntityManager entityManager;
    private final StudentMascotService mascotService;

    public StudentStreakService(
            StudentStreakRepository repository,
            EntityManager entityManager,
            @Lazy StudentMascotService mascotService) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.mascotService = mascotService;
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
        int earnedTickets = DailyStreakRules.missionTickets(row);
        int ticketDelta = earnedTickets - row.getMissionTicketsAwarded();
        if (ticketDelta > 0) {
            row.setGachaTickets(row.getGachaTickets() + ticketDelta);
            row.setMissionTicketsAwarded(earnedTickets);
        }
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
    public StudentStreakView spendGachaTickets(UUID userId, int count) {
        if (count < 1 || count > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Summon count must be from 1 to 10");
        }
        return mutate(userId, row -> {
            if (row.getGachaTickets() < count) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough gacha tickets");
            }
            row.setGachaTickets(row.getGachaTickets() - count);
        });
    }

    @Transactional
    public StudentStreakView spendGachaPoints(UUID userId, int count) {
        if (count < 1 || count > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Summon count must be from 1 to 10");
        }
        int cost = count == 1 ? GACHA_COST_SINGLE_POINTS : GACHA_COST_MULTI_POINTS;
        return mutate(userId, row -> {
            if (row.getPoints() < cost) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough points");
            }
            row.setPoints(row.getPoints() - cost);
        });
    }

    @Transactional
    public StudentStreakView saveGachaPointsSpendPref(UUID userId, boolean allow) {
        return mutate(userId, row -> {
            row.setGachaPointsPrefDate(today());
            row.setGachaPointsPrefAllow(allow);
        });
    }

    @Transactional
    public StudentStreakView settleGacha(UUID userId, GachaSettleRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing settle payload");
        }
        int rollCount = request.rollCount();
        if (rollCount < 1 || rollCount > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Summon count must be from 1 to 10");
        }
        int pity = request.pity();
        if (pity < 0 || pity > GACHA_MAX_PITY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pity counter");
        }
        int pointsCashback = request.pointsCashback();
        if (pointsCashback < 0 || pointsCashback > rollCount * GACHA_MAX_POINTS_CASHBACK_PER_ROLL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid points cashback");
        }
        int crystalsGranted = request.crystalsGranted();
        if (crystalsGranted < 0 || crystalsGranted > rollCount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid crystal grant");
        }
        var mascotIds = request.mascotIds() == null ? java.util.List.<String>of() : request.mascotIds();
        if (mascotIds.size() > rollCount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many mascots in one batch");
        }
        StudentStreakView view = mutate(userId, row -> {
            row.setGachaPityCount(pity);
            if (pointsCashback > 0) {
                row.setPoints(row.getPoints() + pointsCashback);
            }
            if (crystalsGranted > 0) {
                row.setCrystals(row.getCrystals() + crystalsGranted);
            }
        });
        mascotService.unlockGachaMascots(userId, mascotIds);
        return view;
    }

    @Transactional
    public StudentStreakView grantCrystals(UUID userId, int count) {
        if (count < 1 || count > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Crystal count must be from 1 to 10");
        }
        return mutate(userId, row -> row.setCrystals(row.getCrystals() + count));
    }

    @Transactional
    public StudentStreakView spendCrystals(UUID userId, int count) {
        if (count < 1 || count > CrystalExchange.MAX_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Crystal count must be from 1 to 99");
        }
        return mutate(userId, row -> {
            if (row.getCrystals() < count) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough crystals");
            }
            row.setCrystals(row.getCrystals() - count);
        });
    }

    @Transactional
    public StudentStreakView exchangeCrystals(UUID userId, int count) {
        return mutate(userId, row -> {
            try {
                CrystalExchange.exchange(row, count);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Crystal count must be from 1 to 99");
            } catch (IllegalStateException ex) {
                String message = "points".equals(ex.getMessage())
                        ? "Point balance is full"
                        : "Not enough crystals";
                throw new ResponseStatusException(HttpStatus.CONFLICT, message);
            }
        });
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

    @Transactional
    public void addGachaTickets(UUID userId, int amount) {
        if (userId == null || amount <= 0) {
            return;
        }
        StudentStreak row = locked(userId);
        row.setGachaTickets(row.getGachaTickets() + amount);
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
