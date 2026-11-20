package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.DTO.GachaRollResponse;
import com.eiu.capstone.backend.model.StudentStreak;
import com.eiu.capstone.backend.repository.StudentMascotPurchaseRepository;
import com.eiu.capstone.backend.repository.StudentStreakRepository;
import com.eiu.capstone.backend.service.GachaRateConfig;
import com.eiu.capstone.backend.service.GachaRateConfigService;
import com.eiu.capstone.backend.service.StudentGachaService;
import com.eiu.capstone.backend.service.StudentStreakService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@ExtendWith(MockitoExtension.class)
class StudentGachaServiceTest {

    @Mock
    private StudentStreakRepository streakRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private GachaRateConfigService configService;
    @Mock
    private StudentMascotPurchaseRepository purchases;
    @Mock
    private com.eiu.capstone.backend.service.StudentMascotService mascotService;

    private StudentStreakService streakService;
    private StudentGachaService gachaService;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Query nativeQuery = org.mockito.Mockito.mock(Query.class);
        when(entityManager.createNativeQuery(org.mockito.ArgumentMatchers.anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(nativeQuery);
        when(nativeQuery.executeUpdate()).thenReturn(1);

        streakService = new StudentStreakService(streakRepository, entityManager, mascotService);
        gachaService = new StudentGachaService(streakService, configService, purchases);
        when(configService.current()).thenReturn(new GachaRateConfig(
                100, 900, 100, 90,
                1.0 / 6000, 0.0015, 0.003,
                0.025, 0.06, 0.18, 0.42));
        when(purchases.findByIdUserId(userId)).thenReturn(java.util.List.of());
    }

    @Test
    void roll_spendsTicketsAndReturnsPulls() {
        StudentStreak row = new StudentStreak();
        row.setUserId(userId);
        row.setGachaTickets(5);
        row.setPoints(500);
        row.setGachaPityCount(0);
        when(streakRepository.lockByUserId(userId)).thenReturn(Optional.of(row));
        when(streakRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GachaRollResponse response = gachaService.roll(userId, 1);

        assertEquals(4, response.gachaTickets());
        assertEquals(1, response.pulls().size());
        assertTrue(response.gachaPity() >= 0);
    }

    @Test
    void ticketHardPityGuaranteesCrystalAtRoll100() {
        StudentStreak row = new StudentStreak();
        row.setUserId(userId);
        row.setGachaTickets(1);
        row.setPoints(10_000);
        row.setGachaPityCount(99);
        when(streakRepository.lockByUserId(userId)).thenReturn(Optional.of(row));
        when(streakRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GachaRollResponse response = gachaService.roll(userId, 1);

        assertEquals(0, response.gachaPity());
        assertTrue(response.pulls().stream().anyMatch(p -> "crystal".equals(p.type())));
    }

    @Test
    void pointRollBelowSoftPityUsuallyMissesCrystal() {
        StudentStreak row = new StudentStreak();
        row.setUserId(userId);
        row.setGachaTickets(0);
        row.setPoints(10_000);
        row.setGachaPityCount(50);
        row.setCrystalSourcedPoints(0);
        when(streakRepository.lockByUserId(userId)).thenReturn(Optional.of(row));
        when(streakRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GachaRollResponse response = gachaService.roll(userId, 1);
        assertTrue(
                response.pulls().stream().noneMatch(p -> "crystal".equals(p.type())),
                "pity 51 should use 1/10000 crystal rate");
        assertEquals(51, response.gachaPity());
    }
}