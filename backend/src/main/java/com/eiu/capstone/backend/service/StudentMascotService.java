package com.eiu.capstone.backend.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.MascotOfferView;
import com.eiu.capstone.backend.DTO.MascotShopView;
import com.eiu.capstone.backend.DTO.MascotSnapshotView;
import com.eiu.capstone.backend.model.StudentMascotPurchase;
import com.eiu.capstone.backend.model.StudentMascotPurchaseKey;
import com.eiu.capstone.backend.model.StudentStreak;
import com.eiu.capstone.backend.repository.StudentMascotPurchaseRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.utility.TimeUtil;

@Service
@Profile("!desktop & !desktop-lecturer")
public class StudentMascotService {

    public static final String EXCLUSIVE_STUDENT_CODE = "2331200057";

    private final StudentStreakService streakService;
    private final StudentMascotPurchaseRepository purchases;
    private final UserAccountRepository userAccounts;

    public StudentMascotService(
            StudentStreakService streakService,
            StudentMascotPurchaseRepository purchases,
            UserAccountRepository userAccounts) {
        this.streakService = streakService;
        this.purchases = purchases;
        this.userAccounts = userAccounts;
    }

    public boolean isIceQueenEligible(UUID userId) {
        if (userId == null) {
            return false;
        }
        return userAccounts.findById(userId)
                .map(u -> EXCLUSIVE_STUDENT_CODE.equals(u.getStudentCode())
                        || "kiet.doan.cit23@eiu.edu.vn".equalsIgnoreCase(u.getEmail()))
                .orElse(false);
    }

    private static String normalizeMascotId(String mascotId) {
        return MascotCatalog.canonicalId(mascotId);
    }

    private static String resolveEquipped(StudentStreak row, boolean iceQueenEligible) {
        String equipped = row == null ? MascotCatalog.STARTER_ID : normalizeMascotId(row.getEquippedMascot());
        if (!MascotCatalog.isSelectable(equipped)) {
            return MascotCatalog.STARTER_ID;
        }
        if (MascotCatalog.EXCLUSIVE_ICE_QUEEN_ID.equals(equipped) && !iceQueenEligible) {
            return MascotCatalog.STARTER_ID;
        }
        return equipped;
    }

    @Transactional(readOnly = true)
    public MascotShopView list(UUID userId) {
        StudentStreak row = streakService.find(userId).orElse(null);
        int points = row == null ? 0 : row.getPoints();
        boolean iceQueenEligible = isIceQueenEligible(userId);
        String equipped = resolveEquipped(row, iceQueenEligible);
        Set<String> owned = ownedIds(userId);
        if (!MascotCatalog.STARTER_ID.equals(equipped)) {
            owned.add(equipped);
        }
        return toView(points, equipped, owned, iceQueenEligible);
    }

    @Transactional(readOnly = true)
    public MascotSnapshotView snapshot(UUID userId) {
        StudentStreak row = streakService.find(userId).orElse(null);
        boolean iceQueenEligible = isIceQueenEligible(userId);
        String equipped = resolveEquipped(row, iceQueenEligible);
        Set<String> ownedSet = ownedIds(userId);
        if (!MascotCatalog.STARTER_ID.equals(equipped)) {
            ownedSet.add(equipped);
        }
        List<String> owned = ownedSet.stream()
                .filter(MascotCatalog::isSelectable)
                .filter(id -> iceQueenEligible || !MascotCatalog.EXCLUSIVE_ICE_QUEEN_ID.equals(id))
                .toList();
        return new MascotSnapshotView(equipped, owned);
    }

    /**
     * Locks the streak row, subtracts the price, and inserts the purchase.
     * Both writes share this transaction, so a failed insert restores the points.
     */
    @Transactional
    public MascotShopView purchase(UUID userId, String mascotId) {
        String targetId = normalizeMascotId(mascotId);
        boolean iceQueenEligible = isIceQueenEligible(userId);
        if (MascotCatalog.EXCLUSIVE_ICE_QUEEN_ID.equalsIgnoreCase(targetId) && !iceQueenEligible) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Exclusive mascot");
        }
        MascotCatalog.Offer offer = MascotCatalog.forSale(targetId);
        if (offer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown mascot");
        }
        StudentStreak row = streakService.lock(userId);
        Set<String> owned = ownedIds(userId);
        String previousEquipped = resolveEquipped(row, iceQueenEligible);
        boolean previousRecorded = owned.contains(previousEquipped);
        if (!MascotCatalog.STARTER_ID.equals(previousEquipped)) {
            owned.add(previousEquipped);
        }
        boolean alreadyOwned = owned.contains(offer.id())
                || purchases.existsByIdUserIdAndIdMascotId(userId, offer.id());
        try {
            MascotPurchaseRules.charge(row, offer.price(), alreadyOwned);
        } catch (MascotPurchaseException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getCode());
        }
        if (!MascotCatalog.STARTER_ID.equals(previousEquipped)
                && !previousRecorded
                && !purchases.existsByIdUserIdAndIdMascotId(userId, previousEquipped)) {
            saveUnlock(userId, previousEquipped);
        }
        row.setEquippedMascot(offer.id());
        streakService.save(row);
        StudentMascotPurchase bought = new StudentMascotPurchase();
        bought.setId(new StudentMascotPurchaseKey(userId, offer.id()));
        bought.setPricePaid(offer.price());
        bought.setPurchasedAt(OffsetDateTime.now(TimeUtil.VIETNAM_ZONE));
        purchases.save(bought);
        owned.add(offer.id());
        return toView(row.getPoints(), row.getEquippedMascot(), owned, iceQueenEligible);
    }

    @Transactional
    public MascotShopView equip(UUID userId, String mascotId) {
        String targetId = normalizeMascotId(mascotId);
        if (!MascotCatalog.isSelectable(targetId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown mascot");
        }
        boolean iceQueenEligible = isIceQueenEligible(userId);
        if (MascotCatalog.EXCLUSIVE_ICE_QUEEN_ID.equalsIgnoreCase(targetId) && !iceQueenEligible) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Exclusive mascot");
        }
        StudentStreak row = streakService.lock(userId);
        Set<String> owned = ownedIds(userId);
        String previousEquipped = resolveEquipped(row, iceQueenEligible);
        if (!MascotCatalog.STARTER_ID.equals(previousEquipped)
                && !owned.contains(previousEquipped)
                && !purchases.existsByIdUserIdAndIdMascotId(userId, previousEquipped)) {
            saveUnlock(userId, previousEquipped);
            owned.add(previousEquipped);
        }
        if (!MascotCatalog.STARTER_ID.equals(targetId)
                && !owned.contains(targetId)
                && !purchases.existsByIdUserIdAndIdMascotId(userId, targetId)) {
            saveUnlock(userId, targetId);
        }
        if (!MascotCatalog.STARTER_ID.equals(targetId)) {
            owned.add(targetId);
        }
        row.setEquippedMascot(targetId);
        streakService.save(row);
        return toView(row.getPoints(), row.getEquippedMascot(), owned, iceQueenEligible);
    }

    @Transactional
    public void unlockGachaMascots(UUID userId, List<String> mascotIds) {
        if (userId == null || mascotIds == null || mascotIds.isEmpty()) {
            return;
        }
        Set<String> owned = ownedIds(userId);
        for (String raw : mascotIds) {
            String targetId = normalizeMascotId(raw);
            if (!MascotGachaPools.isUnlockable(targetId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid gacha mascot");
            }
            if (MascotCatalog.EXCLUSIVE_ICE_QUEEN_ID.equalsIgnoreCase(targetId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid gacha mascot");
            }
            if (owned.contains(targetId) || purchases.existsByIdUserIdAndIdMascotId(userId, targetId)) {
                continue;
            }
            saveUnlock(userId, targetId);
            owned.add(targetId);
        }
    }

    private void saveUnlock(UUID userId, String mascotId) {
        StudentMascotPurchase unlocked = new StudentMascotPurchase();
        unlocked.setId(new StudentMascotPurchaseKey(userId, mascotId));
        unlocked.setPricePaid(0);
        unlocked.setPurchasedAt(OffsetDateTime.now(TimeUtil.VIETNAM_ZONE));
        purchases.save(unlocked);
    }

    private Set<String> ownedIds(UUID userId) {
        Set<String> ids = new HashSet<>();
        for (StudentMascotPurchase row : purchases.findByIdUserId(userId)) {
            if (row == null || row.getId() == null) {
                continue;
            }
            String canonical = normalizeMascotId(row.getId().getMascotId());
            if (canonical != null && !canonical.isBlank()) {
                ids.add(canonical);
            }
        }
        return ids;
    }

    private static MascotShopView toView(int points, String equipped, Set<String> owned, boolean includeIceQueen) {
        List<MascotOfferView> mascots = new ArrayList<>();
        mascots.add(new MascotOfferView(
                MascotCatalog.STARTER_ID,
                MascotCatalog.STARTER_NAME,
                0,
                true,
                MascotCatalog.STARTER_ID.equals(equipped),
                true));
        if (includeIceQueen) {
            boolean hasIceQueen = owned.contains(MascotCatalog.EXCLUSIVE_ICE_QUEEN_ID);
            mascots.add(new MascotOfferView(
                    MascotCatalog.EXCLUSIVE_ICE_QUEEN_ID,
                    MascotCatalog.EXCLUSIVE_ICE_QUEEN_NAME,
                    0,
                    hasIceQueen,
                    MascotCatalog.EXCLUSIVE_ICE_QUEEN_ID.equals(equipped),
                    false));
        }
        for (MascotCatalog.Offer offer : MascotCatalog.FOR_SALE) {
            boolean has = owned.contains(offer.id());
            mascots.add(new MascotOfferView(
                    offer.id(),
                    offer.name(),
                    offer.price(),
                    has,
                    offer.id().equals(equipped),
                    false));
        }
        return new MascotShopView(points, equipped, mascots);
    }
}
