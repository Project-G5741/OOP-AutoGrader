package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.service.MascotGachaPools;

class MascotGachaPoolsTest {

    @Test
    void rejects_exclusive_mythic_and_unknown_ids() {
        assertFalse(MascotGachaPools.isUnlockable("ice_queen"));
        assertFalse(MascotGachaPools.isUnlockable("fox"));
        assertFalse(MascotGachaPools.isUnlockable("floral_fairy"));
        assertFalse(MascotGachaPools.isUnlockable("cyber_striker"));
        assertFalse(MascotGachaPools.isUnlockable("not_a_mascot"));
    }

    @Test
    void allows_catalog_gacha_drops() {
        assertTrue(MascotGachaPools.isUnlockable("bunny"));
        assertTrue(MascotGachaPools.isUnlockable("zerotwo"));
        assertTrue(MascotGachaPools.isUnlockable("capybara"));
        assertTrue(MascotGachaPools.isUnlockable("cuu_vi"));
        assertTrue(MascotGachaPools.isUnlockable("stella-witch"));
    }
}