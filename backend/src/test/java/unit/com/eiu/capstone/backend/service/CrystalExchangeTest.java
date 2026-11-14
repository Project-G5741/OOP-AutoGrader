package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.model.StudentStreak;
import com.eiu.capstone.backend.service.CrystalExchange;

class CrystalExchangeTest {

    @Test
    void oneCrystalPaysFourThousandPoints() {
        StudentStreak row = new StudentStreak();
        row.setCrystals(3);
        row.setPoints(100);

        CrystalExchange.exchange(row, 1);

        assertEquals(2, row.getCrystals());
        assertEquals(4100, row.getPoints());
    }

    @Test
    void severalCrystalsPayTheSameRate() {
        StudentStreak row = new StudentStreak();
        row.setCrystals(4);
        row.setPoints(0);

        CrystalExchange.exchange(row, 4);

        assertEquals(0, row.getCrystals());
        assertEquals(16000, row.getPoints());
    }

    @Test
    void rejectsABalanceThatCannotCoverTheCount() {
        StudentStreak row = new StudentStreak();
        row.setCrystals(1);
        row.setPoints(50);

        assertThrows(IllegalStateException.class, () -> CrystalExchange.exchange(row, 2));
        assertEquals(1, row.getCrystals());
        assertEquals(50, row.getPoints());
    }
}
