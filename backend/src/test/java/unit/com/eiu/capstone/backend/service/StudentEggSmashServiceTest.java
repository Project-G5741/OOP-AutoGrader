package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.EggSmashResponse;
import com.eiu.capstone.backend.model.StudentStreak;
import com.eiu.capstone.backend.repository.StudentStreakRepository;
import com.eiu.capstone.backend.service.StudentEggSmashService;
import com.eiu.capstone.backend.service.StudentMascotService;
import com.eiu.capstone.backend.service.StudentStreakService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@ExtendWith(MockitoExtension.class)
class StudentEggSmashServiceTest {

    @Mock
    private StudentStreakRepository streakRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private StudentMascotService mascotService;

    private StudentStreakService streakService;
    private StudentEggSmashService eggSmashService;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Query nativeQuery = org.mockito.Mockito.mock(Query.class);
        org.mockito.Mockito.lenient()
                .when(entityManager.createNativeQuery(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(nativeQuery);
        org.mockito.Mockito.lenient()
                .when(nativeQuery.setParameter(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(nativeQuery);
        org.mockito.Mockito.lenient().when(nativeQuery.executeUpdate()).thenReturn(1);

        streakService = new StudentStreakService(streakRepository, entityManager, mascotService);
        eggSmashService = new StudentEggSmashService(streakService);
    }

    @Test
    void smash_rejectsInvalidPickListsWithoutLocking() {
        ResponseStatusException dup = assertThrows(
                ResponseStatusException.class,
                () -> eggSmashService.smash(userId, List.of(1, 1, 4)));
        assertEquals(HttpStatus.BAD_REQUEST, dup.getStatusCode());

        ResponseStatusException outOfRange = assertThrows(
                ResponseStatusException.class,
                () -> eggSmashService.smash(userId, List.of(0, 2, 9)));
        assertEquals(HttpStatus.BAD_REQUEST, outOfRange.getStatusCode());

        ResponseStatusException wrongCount = assertThrows(
                ResponseStatusException.class,
                () -> eggSmashService.smash(userId, List.of(0, 2)));
        assertEquals(HttpStatus.BAD_REQUEST, wrongCount.getStatusCode());

        verify(streakRepository, never()).lockByUserId(any());
    }

    @Test
    void smash_rejectsInsufficientPoints() {
        StudentStreak row = new StudentStreak();
        row.setUserId(userId);
        row.setPoints(999);
        row.setCrystals(2);
        row.setEggSmashPity(4);
        when(streakRepository.lockByUserId(userId)).thenReturn(Optional.of(row));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eggSmashService.smash(userId, List.of(0, 4, 8)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(999, row.getPoints());
        assertEquals(2, row.getCrystals());
        assertEquals(4, row.getEggSmashPity());
        verify(streakRepository, never()).save(any());
    }

    @Test
    void smash_turns1To7AlwaysShowOneUnpickedCrystalAndNeverAwardIt() {
        RandomGenerator maxPointsRng = new RandomGenerator() {
            private int intStep = 0;

            @Override
            public int nextInt(int bound) {
                return (intStep++) % bound;
            }

            @Override
            public double nextDouble() {
                return 0.0; // Luckiest possible point roll (300 pts on every picked egg)
            }

            @Override
            public long nextLong() {
                return 0L;
            }
        };

        for (int pity = 0; pity <= 6; pity++) {
            StudentStreak row = new StudentStreak();
            row.setUserId(userId);
            row.setPoints(5_000);
            row.setCrystals(0);
            row.setEggSmashPity(pity); // Turns 1..7
            when(streakRepository.lockByUserId(userId)).thenReturn(Optional.of(row));
            when(streakRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            EggSmashResponse res = eggSmashService.smashWithRng(userId, List.of(1, 4, 7), maxPointsRng);

            assertEquals(0, res.crystalsEarned(), "Turn " + (pity + 1) + " must never award crystal");
            assertEquals(
                    1,
                    res.cells().stream().filter(c -> "crystal".equals(c.type())).count(),
                    "Every turn 1..7 must display exactly 1 crystal on the 9-egg board");
            assertEquals(
                    0,
                    res.cells().stream().filter(c -> c.picked() && "crystal".equals(c.type())).count(),
                    "Crystal on turns 1..7 must sit in an unpicked slot");
            assertEquals(900, res.pointsEarned());
            assertTrue(res.pointsEarned() < StudentEggSmashService.TURN_COST);
            assertEquals(4_900, res.points());
            assertEquals(pity + 1, row.getEggSmashPity());
        }
    }

    @Test
    void smash_turn8SoftPityAlwaysPlacesOneCrystalOnNineEggBoard() {
        StudentStreak row = new StudentStreak();
        row.setUserId(userId);
        row.setPoints(10_000);
        row.setCrystals(1);
        row.setEggSmashPity(7); // Turn 8 (Soft Pity)
        when(streakRepository.lockByUserId(userId)).thenReturn(Optional.of(row));
        when(streakRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Force crystal into slot 6 (unpicked when player picks 0, 1, 2)
        RandomGenerator missOnTurn8Rng = new RandomGenerator() {
            @Override
            public int nextInt(int bound) {
                return 6;
            }

            @Override
            public double nextDouble() {
                return 0.60; // 50 pts per point egg
            }

            @Override
            public long nextLong() {
                return 0L;
            }
        };

        EggSmashResponse missed = eggSmashService.smashWithRng(userId, List.of(0, 1, 2), missOnTurn8Rng);
        assertEquals(0, missed.crystalsEarned());
        assertEquals(1, missed.cells().stream().filter(c -> "crystal".equals(c.type())).count());
        assertEquals("crystal", missed.cells().get(6).type());
        assertEquals(8, row.getEggSmashPity()); // Advances to Turn 9 Hard Pity!
    }

    @Test
    void smash_turn9HardPityGuaranteesCrystalInChosenPicksAndResetsPity() {
        StudentStreak row = new StudentStreak();
        row.setUserId(userId);
        row.setPoints(3_000);
        row.setCrystals(2);
        row.setEggSmashPity(8); // Turn 9 (Hard Pity)
        when(streakRepository.lockByUserId(userId)).thenReturn(Optional.of(row));
        when(streakRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EggSmashResponse res = eggSmashService.smashWithRng(userId, List.of(2, 5, 8), new Random(42L));

        assertEquals(1, res.crystalsEarned());
        assertEquals(3, res.crystals());
        assertEquals(0, row.getEggSmashPity());
        assertTrue(res.pointsEarned() >= 100 && res.pointsEarned() <= 600);
        assertEquals(
                1,
                res.cells().stream().filter(c -> c.picked() && "crystal".equals(c.type())).count());
    }

    @Test
    void smash_usesAdminConfiguredEggSmashRates() {
        com.eiu.capstone.backend.service.EggSmashRateConfigService cfgService =
                org.mockito.Mockito.mock(com.eiu.capstone.backend.service.EggSmashRateConfigService.class);
        when(cfgService.current()).thenReturn(new com.eiu.capstone.backend.service.EggSmashRateConfig(
                1.0, 1.0, 1.0, 1.0, 1.0,
                1.0, 0.0, 0.0));
        StudentEggSmashService customService = new StudentEggSmashService(streakService, cfgService);

        StudentStreak row = new StudentStreak();
        row.setUserId(userId);
        row.setPoints(2_000);
        row.setCrystals(0);
        row.setEggSmashPity(0); // Turn 1 with Admin-configured 100% Crystal + 100% +300pt eggs
        when(streakRepository.lockByUserId(userId)).thenReturn(Optional.of(row));
        when(streakRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EggSmashResponse res = customService.smashWithRng(userId, List.of(0, 4, 8), new Random(7L));
        assertEquals(1, res.crystalsEarned());
        assertEquals(600, res.pointsEarned());
        assertEquals(1_600, res.points());
        assertEquals(1, res.crystals());
        assertEquals(0, row.getEggSmashPity());

        // Even with a custom intermediate rate (e.g. 5%) that misses, the 9-egg board still shows 1 unpicked crystal
        when(cfgService.current()).thenReturn(new com.eiu.capstone.backend.service.EggSmashRateConfig(
                0.05, 0.05, 0.05, 1.0 / 3.0, 1.0,
                0.05, 0.15, 0.30));
        row.setPoints(2_000);
        row.setEggSmashPity(5); // Turn 6
        RandomGenerator missRng = new RandomGenerator() {
            @Override
            public int nextInt(int bound) {
                return 2 % bound;
            }

            @Override
            public double nextDouble() {
                return 0.50; // > 0.05 -> misses Crystal; also rolls +50pt eggs
            }

            @Override
            public long nextLong() {
                return 0L;
            }
        };
        EggSmashResponse customMiss = customService.smashWithRng(userId, List.of(0, 4, 8), missRng);
        assertEquals(0, customMiss.crystalsEarned());
        assertEquals(1, customMiss.cells().stream().filter(c -> "crystal".equals(c.type())).count());
        assertEquals(0, customMiss.cells().stream().filter(c -> c.picked() && "crystal".equals(c.type())).count());
    }
}
