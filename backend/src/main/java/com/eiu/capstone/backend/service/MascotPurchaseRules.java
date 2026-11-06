package com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.model.StudentStreak;

/**
 * Point spend for one mascot. Callers persist the streak row and the purchase row in the same transaction.
 */
public final class MascotPurchaseRules {

    private MascotPurchaseRules() {
    }

    public static void charge(StudentStreak row, int price, boolean alreadyOwned) {
        if (alreadyOwned) {
            throw new MascotPurchaseException(MascotPurchaseException.ALREADY_OWNED);
        }
        if (price < 0 || row.getPoints() < price) {
            throw new MascotPurchaseException(MascotPurchaseException.INSUFFICIENT_POINTS);
        }
        row.setPoints(row.getPoints() - price);
    }
}
