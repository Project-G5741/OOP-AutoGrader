package com.eiu.capstone.backend.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.GachaConfigView;
import com.eiu.capstone.backend.DTO.GachaRollItemView;
import com.eiu.capstone.backend.DTO.GachaRollResponse;
import com.eiu.capstone.backend.model.StudentMascotPurchase;
import com.eiu.capstone.backend.model.StudentMascotPurchaseKey;
import com.eiu.capstone.backend.model.StudentStreak;
import com.eiu.capstone.backend.repository.StudentMascotPurchaseRepository;
import com.eiu.capstone.backend.utility.TimeUtil;

@Service
@Profile("!desktop & !desktop-lecturer")
public class StudentGachaService {

    private final StudentStreakService streakService;
    private final GachaRateConfigService configService;
    private final StudentMascotPurchaseRepository purchases;

    public StudentGachaService(
            StudentStreakService streakService,
            GachaRateConfigService configService,
            StudentMascotPurchaseRepository purchases) {
        this.streakService = streakService;
        this.configService = configService;
        this.purchases = purchases;
    }

    @Transactional(readOnly = true)
    public GachaConfigView publicConfig() {
        GachaRateConfig cfg = configService.current();
        return toConfigView(cfg);
    }

    @Transactional
    public GachaRollResponse roll(UUID userId, int count) {
        return rollWithRng(userId, count, ThreadLocalRandom.current());
    }

    @Transactional
    public GachaRollResponse rollWithRng(UUID userId, int count, RandomGenerator rng) {
        if (count < 1 || count > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Summon count must be from 1 to 10");
        }
        GachaRateConfig cfg = configService.current();
        LocalDate today = LocalDate.now(TimeUtil.VIETNAM_ZONE);
        StudentStreak row = streakService.lock(userId);
        DailyStreakRules.roll(row, today);

        int pointCost = cfg.pointCost(count);
        boolean useTickets = row.getGachaTickets() >= count;
        if (useTickets) {
            row.setGachaTickets(row.getGachaTickets() - count);
        } else {
            if (row.getPoints() < pointCost) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough Roll Tickets or Points");
            }
            row.setPoints(row.getPoints() - pointCost);
            int consumed = Math.min(pointCost, row.getCrystalSourcedPoints());
            row.setCrystalSourcedPoints(row.getCrystalSourcedPoints() - consumed);
            boolean allowCrystalDrop = consumed == 0;
            boolean payWithPoints = true;
            return executeRolls(userId, row, count, cfg, allowCrystalDrop, payWithPoints, rng);
        }
        return executeRolls(userId, row, count, cfg, true, false, rng);
    }

    private GachaRollResponse executeRolls(
            UUID userId,
            StudentStreak row,
            int count,
            GachaRateConfig cfg,
            boolean allowCrystalDrop,
            boolean payWithPoints,
            RandomGenerator rng) {
        Set<String> owned = new HashSet<>(ownedIds(userId));
        owned.add(MascotCatalog.STARTER_ID);
        String equipped = MascotCatalog.canonicalId(row.getEquippedMascot());
        if (equipped != null && MascotCatalog.isSelectable(equipped)) {
            owned.add(equipped);
        }
        Set<String> ownedReadOnly = Collections.unmodifiableSet(owned);
        List<GachaRollItemView> pulls = new ArrayList<>();
        int totalCashback = 0;

        for (int i = 0; i < count; i++) {
            RollOutcome outcome = rollSingle(
                    row.getGachaPityCount(), ownedReadOnly, cfg, allowCrystalDrop, payWithPoints, rng);
            row.setGachaPityCount(outcome.nextPity);
            pulls.add(outcome.item);
            if ("crystal".equals(outcome.item.type())) {
                row.setCrystals(row.getCrystals() + outcome.item.crystalAmount());
            }
            if ("mascot".equals(outcome.item.type()) || "legendary".equals(outcome.item.type())) {
                String mascotId = MascotCatalog.canonicalId(outcome.item.mascotId());
                if (mascotId != null && !owned.contains(mascotId)) {
                    recordUnlock(userId, mascotId);
                    owned.add(mascotId);
                }
            }
            totalCashback += outcome.item.pointsRefund();
        }

        if (payWithPoints && !allowCrystalDrop && totalCashback > 0) {
            long next = (long) row.getCrystalSourcedPoints() + totalCashback;
            row.setCrystalSourcedPoints(next > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) next);
        }
        row.setPoints(row.getPoints() + totalCashback);
        streakService.save(row);

        return new GachaRollResponse(
                List.copyOf(pulls),
                row.getGachaPityCount(),
                row.getPoints(),
                row.getGachaTickets(),
                row.getCrystals(),
                List.copyOf(owned));
    }

    private void recordUnlock(UUID userId, String mascotId) {
        String canonical = MascotCatalog.canonicalId(mascotId);
        if (canonical == null || canonical.isBlank() || purchases.existsByIdUserIdAndIdMascotId(userId, canonical)) {
            return;
        }
        StudentMascotPurchase unlocked = new StudentMascotPurchase();
        unlocked.setId(new StudentMascotPurchaseKey(userId, canonical));
        unlocked.setPricePaid(0);
        unlocked.setPurchasedAt(OffsetDateTime.now(TimeUtil.VIETNAM_ZONE));
        purchases.save(unlocked);
    }

    private Set<String> ownedIds(UUID userId) {
        Set<String> set = new HashSet<>();
        purchases.findByIdUserId(userId).forEach(row -> {
            if (row == null || row.getId() == null) {
                return;
            }
            String canonical = MascotCatalog.canonicalId(row.getId().getMascotId());
            if (canonical != null && !canonical.isBlank()) {
                set.add(canonical);
            }
        });
        return set;
    }

    private record RollOutcome(GachaRollItemView item, int nextPity) {
    }

    private static RollOutcome rollSingle(
            int currentPity,
            Set<String> owned,
            GachaRateConfig cfg,
            boolean allowCrystalDrop,
            boolean payWithPoints,
            RandomGenerator rng) {
        int rollNumber = currentPity + 1;
        // Hard pity guarantee applies to ticket rolls only; point rolls use soft rates (no lock at 100).
        if (!payWithPoints && rollNumber >= cfg.hardPityLimit()) {
            return new RollOutcome(
                    new GachaRollItemView(
                            "crystal",
                            null,
                            "Crystal Stone",
                            "crystal",
                            0,
                            1,
                            true),
                    0);
        }

        double legendaryRoll = rng.nextDouble();
        List<String> legendaryPool = GachaPools.LEGENDARY_MASCOT.stream().filter(id -> !owned.contains(id)).toList();
        if (!legendaryPool.isEmpty() && legendaryRoll < cfg.directLegendaryRate()) {
            String picked = legendaryPool.get(rng.nextInt(legendaryPool.size()));
            return new RollOutcome(
                    new GachaRollItemView(
                            "legendary",
                            picked,
                            MascotCatalog.displayName(picked),
                            "legendary",
                            0,
                            0,
                            false),
                    currentPity + 1);
        }

        double crystalRate;
        boolean mayRollCrystal;
        if (payWithPoints) {
            mayRollCrystal = true;
            crystalRate = cfg.crystalRateForPointRoll(rollNumber);
        } else {
            mayRollCrystal = allowCrystalDrop;
            crystalRate = cfg.crystalBaseRate();
            if (rollNumber >= cfg.softPityStart()) {
                crystalRate = Math.min(
                        1.0,
                        cfg.crystalBaseRate()
                                + (rollNumber - cfg.softPityStart()) * cfg.softPityRateStep());
            }
        }
        if (mayRollCrystal && rng.nextDouble() < crystalRate) {
            return new RollOutcome(
                    new GachaRollItemView(
                            "crystal",
                            null,
                            "Crystal Stone",
                            "crystal",
                            0,
                            1,
                            false),
                    0);
        }

        List<String> mascotPool = GachaPools.RANDOM_MASCOT.stream().filter(id -> !owned.contains(id)).toList();
        if (!mascotPool.isEmpty() && rng.nextDouble() < cfg.randomMascotRate()) {
            String picked = mascotPool.get(rng.nextInt(mascotPool.size()));
            String rarity = legendaryTier(picked) ? "epic" : "rare";
            return new RollOutcome(
                    new GachaRollItemView(
                            "mascot",
                            picked,
                            MascotCatalog.displayName(picked),
                            rarity,
                            0,
                            0,
                            false),
                    currentPity + 1);
        }

        double pointRng = rng.nextDouble();
        int pts = 5;
        if (pointRng < cfg.cashbackRate300()) {
            pts = 300;
        } else if (pointRng < cfg.cashbackRate300() + cfg.cashbackRate150()) {
            pts = 150;
        } else if (pointRng < cfg.cashbackRate300() + cfg.cashbackRate150() + cfg.cashbackRate60()) {
            pts = 60;
        } else if (pointRng < cfg.cashbackRate300() + cfg.cashbackRate150() + cfg.cashbackRate60() + cfg.cashbackRate20()) {
            pts = 20;
        }
        return new RollOutcome(
                new GachaRollItemView("points", null, "+" + pts + " Points", "point", pts, 0, false),
                currentPity + 1);
    }

    private static boolean legendaryTier(String id) {
        return switch (id) {
            case "thanh_long", "bach_ho", "chu_tuoc", "huyen_vu",
                    "ho_diep", "thien_nga", "cuu_vi" -> true;
            default -> false;
        };
    }

    private static GachaConfigView toConfigView(GachaRateConfig cfg) {
        return cfg.toView();
    }
}