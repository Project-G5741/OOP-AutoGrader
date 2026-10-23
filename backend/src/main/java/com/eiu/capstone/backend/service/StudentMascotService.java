package com.eiu.capstone.backend.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.MascotOfferView;
import com.eiu.capstone.backend.DTO.MascotShopView;
import com.eiu.capstone.backend.model.StudentMascotPurchase;
import com.eiu.capstone.backend.model.StudentMascotPurchaseKey;
import com.eiu.capstone.backend.model.StudentStreak;
import com.eiu.capstone.backend.repository.StudentMascotPurchaseRepository;
import com.eiu.capstone.backend.utility.TimeUtil;

@Service
public class StudentMascotService {

    private final StudentStreakService streakService;
    private final StudentMascotPurchaseRepository purchases;

    public StudentMascotService(StudentStreakService streakService, StudentMascotPurchaseRepository purchases) {
        this.streakService = streakService;
        this.purchases = purchases;
    }

    @Transactional(readOnly = true)
    public MascotShopView list(UUID userId) {
        StudentStreak row = streakService.find(userId).orElse(null);
        int points = row == null ? 0 : row.getPoints();
        String equipped = row == null ? MascotCatalog.STARTER_ID : row.getEquippedMascot();
        return toView(points, equipped, ownedIds(userId));
    }

    /**
     * Locks the streak row, subtracts the price, and inserts the purchase.
     * Both writes share this transaction, so a failed insert restores the points.
     */
    @Transactional
    public MascotShopView purchase(UUID userId, String mascotId) {
        MascotCatalog.Offer offer = MascotCatalog.forSale(mascotId);
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
        return toView(row.getPoints(), row.getEquippedMascot(), owned);
    }

    @Transactional
    public MascotShopView equip(UUID userId, String mascotId) {
        if (!MascotCatalog.isSelectable(mascotId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown mascot");
        }
        boolean owned = MascotCatalog.STARTER_ID.equals(mascotId)
                || purchases.existsByIdUserIdAndIdMascotId(userId, mascotId);
        if (!owned) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, MascotPurchaseException.NOT_OWNED);
        }
        StudentStreak row = streakService.lock(userId);
        row.setEquippedMascot(mascotId);
        streakService.save(row);
        return toView(row.getPoints(), row.getEquippedMascot(), ownedIds(userId));
    }

    private Set<String> ownedIds(UUID userId) {
        return purchases.findByIdUserId(userId).stream()
                .map(row -> row.getId().getMascotId())
                .collect(Collectors.toSet());
    }

    private static MascotShopView toView(int points, String equipped, Set<String> owned) {
        List<MascotOfferView> mascots = new ArrayList<>();
        mascots.add(new MascotOfferView(
                MascotCatalog.STARTER_ID,
                MascotCatalog.STARTER_NAME,
                0,
                true,
                MascotCatalog.STARTER_ID.equals(equipped),
                true));
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
