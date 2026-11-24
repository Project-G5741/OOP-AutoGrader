package com.eiu.capstone.backend.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.EggSmashCellView;
import com.eiu.capstone.backend.DTO.EggSmashResponse;
import com.eiu.capstone.backend.model.StudentStreak;
import com.eiu.capstone.backend.utility.TimeUtil;

@Service
@Profile("!desktop & !desktop-lecturer")
public class StudentEggSmashService {

    public static final int TURN_COST = 1000;
    public static final int BOARD_SIZE = 9;
    public static final int PICKS_REQUIRED = 3;
    public static final int MAX_POINTS_PER_EGG = 300;
    public static final int MAX_POINTS_PER_TURN = PICKS_REQUIRED * MAX_POINTS_PER_EGG;
    public static final int FIRST_ELIGIBLE_TURN = 6;
    public static final int TURN_7_RAMP = 7;
    public static final int SOFT_PITY_TURN = 8;
    public static final int HARD_PITY_TURN = 9;
    /** Default Turn 6 hit rate = 0.01% (0.0001) => 9-egg board spawn rate = 3 * 0.0001 = 0.0003. */
    public static final double TURN_6_HIT_RATE = 0.0001;
    public static final double TURN_6_BOARD_SPAWN_RATE = TURN_6_HIT_RATE * (BOARD_SIZE / (double) PICKS_REQUIRED);
    /** Default Turn 7 ramped hit rate = 5.0% (0.05) => 9-egg board spawn rate = 3 * 0.05 = 0.15. */
    public static final double TURN_7_HIT_RATE = 0.05;
    public static final double TURN_7_BOARD_SPAWN_RATE = TURN_7_HIT_RATE * (BOARD_SIZE / (double) PICKS_REQUIRED);

    private static final double UNIFORM_BOARD_HIT_RATE = PICKS_REQUIRED / (double) BOARD_SIZE;
    private static final double RATE_EPSILON = 1e-6;

    private final StudentStreakService streakService;
    private final Supplier<EggSmashRateConfig> configSupplier;

    public StudentEggSmashService(StudentStreakService streakService) {
        this.streakService = streakService;
        this.configSupplier = EggSmashRateConfig::defaults;
    }

    @Autowired
    public StudentEggSmashService(
            StudentStreakService streakService,
            EggSmashRateConfigService configService) {
        this.streakService = streakService;
        this.configSupplier = configService::current;
    }

    @Transactional
    public EggSmashResponse smash(UUID userId, List<Integer> picks) {
        return smashWithRng(userId, picks, ThreadLocalRandom.current());
    }

    public EggSmashResponse smashWithRng(UUID userId, List<Integer> picks, RandomGenerator rng) {
        List<Integer> validatedPicks = validatePicks(picks);
        Set<Integer> pickedSet = new HashSet<>(validatedPicks);
        EggSmashRateConfig cfg = configSupplier.get();

        LocalDate today = LocalDate.now(TimeUtil.VIETNAM_ZONE);
        StudentStreak row = streakService.lock(userId);
        DailyStreakRules.roll(row, today);

        if (row.getPoints() < TURN_COST) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough points");
        }

        row.setPoints(row.getPoints() - TURN_COST);
        int consumedCrystalPts = Math.min(TURN_COST, Math.max(0, row.getCrystalSourcedPoints()));
        row.setCrystalSourcedPoints(Math.max(0, row.getCrystalSourcedPoints() - consumedCrystalPts));

        int currentPity = Math.max(0, Math.min(HARD_PITY_TURN - 1, row.getEggSmashPity()));
        int turn = currentPity + 1;

        int crystalSlot = resolveCrystalSlot(turn, validatedPicks, pickedSet, cfg, rng);
        List<EggSmashCellView> cells = new ArrayList<>(BOARD_SIZE);
        int pointsEarned = 0;
        int crystalsEarned = 0;

        for (int idx = 0; idx < BOARD_SIZE; idx++) {
            boolean picked = pickedSet.contains(idx);
            if (idx == crystalSlot) {
                cells.add(new EggSmashCellView(idx, picked, "crystal", 0, 1));
                if (picked) {
                    crystalsEarned += 1;
                }
            } else {
                int eggPts = rollPointEgg(cfg, rng);
                cells.add(new EggSmashCellView(idx, picked, "points", eggPts, 0));
                if (picked) {
                    pointsEarned += eggPts;
                }
            }
        }

        pointsEarned = Math.min(MAX_POINTS_PER_TURN, Math.max(0, pointsEarned));
        crystalsEarned = Math.min(1, Math.max(0, crystalsEarned));

        row.setPoints(safeAdd(row.getPoints(), pointsEarned));
        if (crystalsEarned > 0) {
            row.setCrystals(safeAdd(row.getCrystals(), crystalsEarned));
            row.setEggSmashPity(0);
        } else {
            row.setEggSmashPity(Math.min(HARD_PITY_TURN - 1, currentPity + 1));
        }

        streakService.save(row);

        return new EggSmashResponse(
                List.copyOf(cells),
                validatedPicks,
                pointsEarned,
                crystalsEarned,
                row.getPoints(),
                row.getCrystals());
    }

    private static List<Integer> validatePicks(List<Integer> picks) {
        if (picks == null || picks.size() != PICKS_REQUIRED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose exactly 3 eggs");
        }
        Set<Integer> seen = new HashSet<>(PICKS_REQUIRED);
        List<Integer> copy = new ArrayList<>(PICKS_REQUIRED);
        for (Integer idx : picks) {
            if (idx == null || idx < 0 || idx >= BOARD_SIZE || !seen.add(idx)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose 3 distinct eggs from 0 to 8");
            }
            copy.add(idx);
        }
        return List.copyOf(copy);
    }

    private static int resolveCrystalSlot(
            int turn,
            List<Integer> picks,
            Set<Integer> pickedSet,
            EggSmashRateConfig cfg,
            RandomGenerator rng) {
        double hitRate = cfg.crystalHitRateForTurn(turn);
        if (hitRate <= 0.0) {
            return -1;
        }
        if (hitRate >= 1.0 - RATE_EPSILON) {
            return picks.get(rng.nextInt(picks.size()));
        }
        if (Math.abs(hitRate - UNIFORM_BOARD_HIT_RATE) <= RATE_EPSILON) {
            return rng.nextInt(BOARD_SIZE);
        }
        if (hitRate < UNIFORM_BOARD_HIT_RATE) {
            double boardSpawnRate = hitRate * (BOARD_SIZE / (double) PICKS_REQUIRED);
            return rng.nextDouble() < boardSpawnRate ? rng.nextInt(BOARD_SIZE) : -1;
        }
        // When configured hit rate exceeds 1/3 (33.33%), always place 1 crystal on the board,
        // biased into chosen picks with exact probability `hitRate`.
        if (rng.nextDouble() < hitRate) {
            return picks.get(rng.nextInt(picks.size()));
        }
        List<Integer> unpicked = new ArrayList<>(BOARD_SIZE - picks.size());
        for (int idx = 0; idx < BOARD_SIZE; idx++) {
            if (!pickedSet.contains(idx)) {
                unpicked.add(idx);
            }
        }
        return unpicked.isEmpty() ? picks.get(rng.nextInt(picks.size())) : unpicked.get(rng.nextInt(unpicked.size()));
    }

    static int rollPointEgg(EggSmashRateConfig cfg, RandomGenerator rng) {
        double r = rng.nextDouble();
        double r300 = cfg.eggRate300();
        double r200 = r300 + cfg.eggRate200();
        double r100 = r200 + cfg.eggRate100();
        if (r < r300) {
            return 300;
        }
        if (r < r200) {
            return 200;
        }
        if (r < r100) {
            return 100;
        }
        return 50;
    }

    private static int safeAdd(int current, int delta) {
        long next = (long) current + delta;
        return next > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) next;
    }
}
