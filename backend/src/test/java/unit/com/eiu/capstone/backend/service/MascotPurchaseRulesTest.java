package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.model.StudentStreak;
import com.eiu.capstone.backend.service.MascotCatalog;
import com.eiu.capstone.backend.service.MascotPurchaseException;
import com.eiu.capstone.backend.service.MascotPurchaseRules;

class MascotPurchaseRulesTest {

    @Test
    void buyingPandaLeavesTheRemainder() {
        StudentStreak row = new StudentStreak();
        row.setPoints(1000);
        MascotPurchaseRules.charge(row, 999, false);
        assertEquals(1, row.getPoints());
    }

    @Test
    void refusesWhenPointsAreShort() {
        StudentStreak row = new StudentStreak();
        row.setPoints(998);
        MascotPurchaseException ex = assertThrows(
                MascotPurchaseException.class,
                () -> MascotPurchaseRules.charge(row, 999, false));
        assertEquals(MascotPurchaseException.INSUFFICIENT_POINTS, ex.getCode());
        assertEquals(998, row.getPoints());
    }

    @Test
    void refusesASecondPurchaseOfTheSameMascot() {
        StudentStreak row = new StudentStreak();
        row.setPoints(2000);
        MascotPurchaseException ex = assertThrows(
                MascotPurchaseException.class,
                () -> MascotPurchaseRules.charge(row, 999, true));
        assertEquals(MascotPurchaseException.ALREADY_OWNED, ex.getCode());
        assertEquals(2000, row.getPoints());
    }

    @Test
    void shopPricesMatchTheCatalog() {
        assertEquals(333, MascotCatalog.forSale("bear").price());
        assertEquals(999, MascotCatalog.forSale("panda").price());
        assertEquals(1111, MascotCatalog.forSale("penguin").price());
        assertEquals(2222, MascotCatalog.forSale("bunny").price());
        assertEquals(2444, MascotCatalog.forSale("vit").price());
        assertEquals(2888, MascotCatalog.forSale("hamster").price());
        assertEquals(3333, MascotCatalog.forSale("cat").price());
        assertEquals(3555, MascotCatalog.forSale("chuot_tui").price());
        assertEquals(3888, MascotCatalog.forSale("shiba").price());
        assertEquals(4222, MascotCatalog.forSale("dai_bang").price());
        assertEquals(4888, MascotCatalog.forSale("cho_soi").price());
        assertEquals(9999, MascotCatalog.forSale("thanh_long").price());
        assertEquals(8888, MascotCatalog.forSale("bach_ho").price());
        assertEquals(7777, MascotCatalog.forSale("chu_tuoc").price());
        assertEquals(6666, MascotCatalog.forSale("huyen_vu").price());
        assertEquals(35000, MascotCatalog.forSale("zerotwo").price());
        assertEquals(20000, MascotCatalog.forSale("con_bang").price());
        assertEquals(20000, MascotCatalog.forSale("cung_ky").price());
        assertEquals(40000, MascotCatalog.forSale("linh_loc").price());
        assertEquals(40000, MascotCatalog.forSale("ki_lan").price());
    }
}
