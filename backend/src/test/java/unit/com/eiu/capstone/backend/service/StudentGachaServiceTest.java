package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.DTO.GachaRollResponse;
import com.eiu.capstone.backend.model.StudentMascotPurchase;
import com.eiu.capstone.backend.model.StudentMascotPurchaseKey;
import com.eiu.capstone.backend.model.StudentStreak;
import com.eiu.capstone.backend.repository.StudentMascotPurchaseRepository;
import com.eiu.capstone.backend.repository.StudentStreakRepository;
import com.eiu.capstone.backend.service.GachaPools;
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

    @Test
    void randomMascotDrop_persistsPurchaseRowAndUnlocksInOwnedList() {
        when(configService.current()).thenReturn(new GachaRateConfig(
                100, 900, 100, 90,
                0.0, 0.0, 0.0, 1.0,
                0.0, 0.0, 0.0, 0.0));
        StudentStreak row = new StudentStreak();
        row.setUserId(userId);
        row.setGachaTickets(1);
        row.setPoints(500);
        row.setGachaPityCount(0);
        when(streakRepository.lockByUserId(userId)).thenReturn(Optional.of(row));
        when(streakRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GachaRollResponse response = gachaService.rollWithRng(userId, 1, new Random(42L));

        assertEquals(1, response.pulls().size());
        assertEquals("mascot", response.pulls().get(0).type());
        String pulledId = response.pulls().get(0).mascotId();
        assertNotNull(pulledId);
        assertTrue(response.ownedMascotIds().contains(pulledId));

        ArgumentCaptor<StudentMascotPurchase> captor = ArgumentCaptor.forClass(StudentMascotPurchase.class);
        verify(purchases, times(1)).save(captor.capture());
        StudentMascotPurchase saved = captor.getValue();
        assertEquals(userId, saved.getId().getUserId());
        assertEquals(pulledId, saved.getId().getMascotId());
        assertEquals(0, saved.getPricePaid());
        assertNotNull(saved.getPurchasedAt());
    }

    @Test
    void directLegendaryDrop_persistsPurchaseRowAndUnlocksInOwnedList() {
        when(configService.current()).thenReturn(new GachaRateConfig(
                100, 900, 100, 90,
                1.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0));
        StudentStreak row = new StudentStreak();
        row.setUserId(userId);
        row.setGachaTickets(1);
        row.setPoints(500);
        row.setGachaPityCount(0);
        when(streakRepository.lockByUserId(userId)).thenReturn(Optional.of(row));
        when(streakRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GachaRollResponse response = gachaService.rollWithRng(userId, 1, new Random(7L));

        assertEquals(1, response.pulls().size());
        assertEquals("legendary", response.pulls().get(0).type());
        String pulledId = response.pulls().get(0).mascotId();
        assertTrue(GachaPools.LEGENDARY_MASCOT.contains(pulledId));
        assertTrue(response.ownedMascotIds().contains(pulledId));

        ArgumentCaptor<StudentMascotPurchase> captor = ArgumentCaptor.forClass(StudentMascotPurchase.class);
        verify(purchases, times(1)).save(captor.capture());
        assertEquals(pulledId, captor.getValue().getId().getMascotId());
        assertEquals(0, captor.getValue().getPricePaid());
    }

    @Test
    void multiRoll_persistsDistinctMascotsWithoutDuplicates() {
        when(configService.current()).thenReturn(new GachaRateConfig(
                100, 900, 100, 90,
                0.0, 0.0, 0.0, 1.0,
                0.0, 0.0, 0.0, 0.0));
        StudentStreak row = new StudentStreak();
        row.setUserId(userId);
        row.setGachaTickets(5);
        row.setPoints(500);
        row.setGachaPityCount(0);
        when(streakRepository.lockByUserId(userId)).thenReturn(Optional.of(row));
        when(streakRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GachaRollResponse response = gachaService.rollWithRng(userId, 5, new Random(123L));

        assertEquals(5, response.pulls().size());
        ArgumentCaptor<StudentMascotPurchase> captor = ArgumentCaptor.forClass(StudentMascotPurchase.class);
        verify(purchases, times(5)).save(captor.capture());
        Set<String> savedIds = new HashSet<>();
        for (StudentMascotPurchase saved : captor.getAllValues()) {
            savedIds.add(saved.getId().getMascotId());
        }
        assertEquals(5, savedIds.size(), "each roll in a 5-pull should unlock a distinct mascot");
        assertTrue(response.ownedMascotIds().containsAll(savedIds));
    }

    @Test
    void roll_normalizesHyphenatedOwnedIdsToAvoidDuplicateLegendaryDrop() {
        when(configService.current()).thenReturn(new GachaRateConfig(
                100, 900, 100, 90,
                1.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0));
        List<StudentMascotPurchase> existing = GachaPools.LEGENDARY_MASCOT.stream()
                .filter(id -> !"zerotwo".equals(id))
                .map(id -> {
                    StudentMascotPurchase p = new StudentMascotPurchase();
                    String storedId = "stella_witch".equals(id) ? "stella-witch" : id;
                    p.setId(new StudentMascotPurchaseKey(userId, storedId));
                    p.setPricePaid(0);
                    p.setPurchasedAt(OffsetDateTime.now());
                    return p;
                })
                .toList();
        when(purchases.findByIdUserId(userId)).thenReturn(existing);

        StudentStreak row = new StudentStreak();
        row.setUserId(userId);
        row.setGachaTickets(1);
        row.setPoints(500);
        row.setGachaPityCount(0);
        when(streakRepository.lockByUserId(userId)).thenReturn(Optional.of(row));
        when(streakRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GachaRollResponse response = gachaService.rollWithRng(userId, 1, new Random(99L));

        assertEquals("zerotwo", response.pulls().get(0).mascotId());
        ArgumentCaptor<StudentMascotPurchase> captor = ArgumentCaptor.forClass(StudentMascotPurchase.class);
        verify(purchases, times(1)).save(captor.capture());
        assertEquals("zerotwo", captor.getValue().getId().getMascotId());
    }
}