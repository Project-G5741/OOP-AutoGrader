package com.eiu.capstone.backend.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public MascotShopView list(UUID userId) {
        StudentStreak row = streakService.find(userId).orElse(null);
        int points = row == null ? 0 : row.getPoints();
        String equipped = row == null ? MascotCatalog.STARTER_ID : row.getEquippedMascot();
        boolean eligible = isIceQueenEligible(userId);
        return toView(points, equipped, ownedIds(userId), eligible);
    }

    @Transactional(readOnly = true)
    public MascotSnapshotView snapshot(UUID userId) {
        StudentStreak row = streakService.find(userId).orElse(null);
        String equipped = row == null ? MascotCatalog.STARTER_ID : row.getEquippedMascot();
        return new MascotSnapshotView(equipped, List.copyOf(ownedIds(userId)));
    }

    /**
     * Locks the streak row, subtracts the price, and inserts the purchase.
     * Both writes share this transaction, so a failed insert restores the points.
     */
    @Transactional
    public MascotShopView purchase(UUID userId, String mascotId) {
        String targetId = "ice-queen".equalsIgnoreCase(mascotId) ? MascotCatalog.EXCLUSIVE_ICE_QUEEN_ID : mascotId;
        if (MascotCatalog.EXCLUSIVE_ICE_QUEEN_ID.equalsIgnoreCase(targetId) && !isIceQueenEligible(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Exclusive mascot");
        }
        MascotCatalog.Offer offer = MascotCatalog.forSale(targetId);
        if (offer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown mascot");
        }
        StudentStreak row = streakService.lock(userId);
        boolean alreadyOwned = purchases.existsByIdUserIdAndIdMascotId(userId, offer.id());
        try {
            MascotPurchaseRules.charge(row, offer.price(), alreadyOwned);
        } catch (MascotPurchaseException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getCode());
        }
        row.setEquippedMascot(offer.id());
        streakService.save(row);
        StudentMascotPurchase bought = new StudentMascotPurchase();
        bought.setId(new StudentMascotPurchaseKey(userId, offer.id()));
        bought.setPricePaid(offer.price());
        bought.setPurchasedAt(OffsetDateTime.now(TimeUtil.VIETNAM_ZONE));
        purchases.save(bought);
        Set<String> owned = ownedIds(userId);
        owned.add(offer.id());
        return toView(row.getPoints(), row.getEquippedMascot(), owned, isIceQueenEligible(userId));
    }

    @Transactional
    public MascotShopView equip(UUID userId, String mascotId) {
        String targetId = "ice-queen".equalsIgnoreCase(mascotId) ? MascotCatalog.EXCLUSIVE_ICE_QUEEN_ID : mascotId;
        if (!MascotCatalog.isSelectable(targetId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown mascot");
        }
        if (MascotCatalog.EXCLUSIVE_ICE_QUEEN_ID.equalsIgnoreCase(targetId) && !isIceQueenEligible(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Exclusive mascot");
        }
        if (!MascotCatalog.STARTER_ID.equals(targetId) && !purchases.existsByIdUserIdAndIdMascotId(userId, targetId)) {
            StudentMascotPurchase unlocked = new StudentMascotPurchase();
            unlocked.setId(new StudentMascotPurchaseKey(userId, targetId));
            unlocked.setPricePaid(0);
            unlocked.setPurchasedAt(OffsetDateTime.now(TimeUtil.VIETNAM_ZONE));
            purchases.save(unlocked);
        }
        StudentStreak row = streakService.lock(userId);
        row.setEquippedMascot(targetId);
        streakService.save(row);
        return toView(row.getPoints(), row.getEquippedMascot(), ownedIds(userId), isIceQueenEligible(userId));
    }

    private Set<String> ownedIds(UUID userId) {
        return purchases.findByIdUserId(userId).stream()
                .map(row -> row.getId().getMascotId())
                .collect(Collectors.toSet());
    }

    private static MascotShopView toView(int points, String equipped, Set<String> owned, boolean includeExclusive) {
        List<MascotOfferView> mascots = new ArrayList<>();
        mascots.add(new MascotOfferView(
                MascotCatalog.STARTER_ID,
                MascotCatalog.STARTER_NAME,
                0,
                true,
                MascotCatalog.STARTER_ID.equals(equipped),
                true));
        if (includeExclusive) {
            boolean hasExclusive = owned.contains(MascotCatalog.EXCLUSIVE_ICE_QUEEN_ID);
            mascots.add(new MascotOfferView(
                    MascotCatalog.EXCLUSIVE_ICE_QUEEN_ID,
                    MascotCatalog.EXCLUSIVE_ICE_QUEEN_NAME,
                    0,
                    hasExclusive,
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
