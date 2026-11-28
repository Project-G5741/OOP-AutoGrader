package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.AdminGachaResourceUpdateRequest;
import com.eiu.capstone.backend.DTO.AdminMascotBulkGrantRequest;
import com.eiu.capstone.backend.DTO.AdminMascotBulkGrantResult;
import com.eiu.capstone.backend.DTO.AdminMascotEquipRequest;
import com.eiu.capstone.backend.DTO.AdminMascotGrantRequest;
import com.eiu.capstone.backend.DTO.AdminMascotUpdateRequest;
import com.eiu.capstone.backend.DTO.AdminUserMascotRow;
import com.eiu.capstone.backend.model.StudentMascotPurchase;
import com.eiu.capstone.backend.model.StudentMascotPurchaseKey;
import com.eiu.capstone.backend.model.StudentStreak;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.StudentMascotPurchaseRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.service.AdminMascotManagementService;
import com.eiu.capstone.backend.service.StudentMascotService;
import com.eiu.capstone.backend.service.StudentStreakService;

class AdminMascotManagementServiceTest {

    private JdbcTemplate jdbcTemplate;
    private UserAccountRepository userAccounts;
    private StudentStreakService streakService;
    private StudentMascotPurchaseRepository purchases;
    private AdminMascotManagementService service;

    private final Map<UUID, UserAccount> userStore = new ConcurrentHashMap<>();
    private final Map<UUID, StudentStreak> streakStore = new ConcurrentHashMap<>();
    private final Map<UUID, List<StudentMascotPurchase>> purchaseStore = new ConcurrentHashMap<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        userAccounts = mock(UserAccountRepository.class);
        streakService = mock(StudentStreakService.class);
        purchases = mock(StudentMascotPurchaseRepository.class);

        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), any(Object[].class)))
                .thenReturn(Boolean.TRUE);

        when(userAccounts.findById(any())).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return Optional.ofNullable(userStore.get(id));
        });

        when(streakService.find(any())).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return Optional.ofNullable(streakStore.get(id));
        });

        when(streakService.lock(any())).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return streakStore.computeIfAbsent(id, k -> {
                StudentStreak s = new StudentStreak();
                s.setUserId(k);
                s.setEquippedMascot("fox");
                return s;
            });
        });

        when(streakService.save(any())).thenAnswer(inv -> {
            StudentStreak s = inv.getArgument(0);
            streakStore.put(s.getUserId(), s);
            return s;
        });

        when(purchases.findByIdUserId(any())).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return new ArrayList<>(purchaseStore.getOrDefault(id, List.of()));
        });

        when(purchases.save(any())).thenAnswer(inv -> {
            StudentMascotPurchase row = inv.getArgument(0);
            UUID uid = row.getId().getUserId();
            List<StudentMascotPurchase> list = purchaseStore.computeIfAbsent(uid, k -> new ArrayList<>());
            list.removeIf(existing -> existing.getId().getMascotId().equals(row.getId().getMascotId()));
            list.add(row);
            return row;
        });

        doAnswer(inv -> {
            Iterable<StudentMascotPurchase> toDelete = inv.getArgument(0);
            for (StudentMascotPurchase d : toDelete) {
                UUID uid = d.getId().getUserId();
                List<StudentMascotPurchase> list = purchaseStore.get(uid);
                if (list != null) {
                    list.removeIf(existing -> existing.getId().getMascotId().equals(d.getId().getMascotId()));
                }
            }
            return null;
        }).when(purchases).deleteAll(any());

        service = new AdminMascotManagementService(jdbcTemplate, userAccounts, streakService, purchases);
    }

    private UserAccount registerUser(String studentCode, String email, String fullName, int initialPoints) {
        UUID id = UUID.randomUUID();
        UserAccount user = new UserAccount();
        ReflectionTestUtils.setField(user, "id", id);
        user.setStudentCode(studentCode);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setIsActive(true);
        userStore.put(id, user);

        StudentStreak streak = new StudentStreak();
        streak.setUserId(id);
        streak.setPoints(initialPoints);
        streak.setEquippedMascot("fox");
        streakStore.put(id, streak);

        return user;
    }

    @Test
    void grantRedPanda_addsFreeCompensationWithoutDeductingStudentPoints() {
        UserAccount student = registerUser("2431200010", "s10@eiu.edu.vn", "Nguyen Van A", 1500);

        AdminUserMascotRow updated = service.grantPets(
                student.getId(),
                new AdminMascotGrantRequest(List.of("red-panda"), null, 0, true));

        assertEquals("red_panda", updated.equippedMascot());
        assertEquals(1500, updated.points(), "Student point balance must not be deducted on Admin grant");
        assertEquals(1, updated.owned().size());
        assertEquals("red_panda", updated.owned().get(0).mascotId());
        assertEquals("Red Panda", updated.owned().get(0).displayName());
        assertEquals(0, updated.owned().get(0).pricePaid());
        assertTrue(updated.owned().get(0).equipped());

        ResponseStatusException duplicate = assertThrows(
                ResponseStatusException.class,
                () -> service.grantPets(
                        student.getId(),
                        new AdminMascotGrantRequest(List.of("red_panda"), null, 0, false)));
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());
    }

    @Test
    void updateResources_setsCrystalsTicketsPointsPityAndClampsCrystalSourcedPoints() {
        UserAccount student = registerUser("2431200015", "s15@eiu.edu.vn", "Hoang E", 8000);
        streakStore.get(student.getId()).setCrystalSourcedPoints(6000);

        AdminUserMascotRow updated = service.updateResources(
                student.getId(),
                new AdminGachaResourceUpdateRequest(2500, 12, 45, 78, 7));

        assertEquals(2500, updated.points());
        assertEquals(12, updated.crystals());
        assertEquals(45, updated.gachaTickets());
        assertEquals(78, updated.gachaPityCount());
        assertEquals(7, updated.eggSmashPity());
        assertEquals(
                2500,
                streakStore.get(student.getId()).getCrystalSourcedPoints(),
                "crystalSourcedPoints must clamp when points drop below it");

        ResponseStatusException badEggPity = assertThrows(
                ResponseStatusException.class,
                () -> service.updateResources(
                        student.getId(),
                        new AdminGachaResourceUpdateRequest(null, null, null, null, 9)));
        assertEquals(HttpStatus.BAD_REQUEST, badEggPity.getStatusCode());
    }

    @Test
    void updatePet_swapsMascotAndPreservesEquippedWhenReplaced() {
        UserAccount student = registerUser("2431200020", "s20@eiu.edu.vn", "Tran Thi B", 800);
        service.grantPets(
                student.getId(),
                new AdminMascotGrantRequest(List.of("panda"), null, 0, true));

        AdminUserMascotRow swapped = service.updatePet(
                student.getId(),
                "panda",
                new AdminMascotUpdateRequest("red_panda", 3666, null));

        assertEquals("red_panda", swapped.equippedMascot());
        assertEquals(1, swapped.owned().size());
        assertEquals("red_panda", swapped.owned().get(0).mascotId());
        assertEquals(3666, swapped.owned().get(0).pricePaid());
    }

    @Test
    void deleteEquippedPet_removesPurchaseRowAndResetsEquippedToFox() {
        UserAccount student = registerUser("2431200030", "s30@eiu.edu.vn", "Le Van C", 400);
        service.grantPets(
                student.getId(),
                new AdminMascotGrantRequest(List.of("red_panda"), null, 0, true));
        assertEquals("red_panda", streakStore.get(student.getId()).getEquippedMascot());

        AdminUserMascotRow afterDelete = service.deletePet(student.getId(), "red_panda");

        assertEquals("fox", afterDelete.equippedMascot());
        assertTrue(afterDelete.owned().isEmpty());
        assertEquals("fox", streakStore.get(student.getId()).getEquippedMascot());
    }

    @Test
    void starterFoxAndExclusiveIceQueenGuardrails_areEnforced() {
        UserAccount regular = registerUser("2431200040", "s40@eiu.edu.vn", "Pham D", 500);
        UserAccount authorized = registerUser(
                StudentMascotService.EXCLUSIVE_STUDENT_CODE,
                "kiet.doan.cit23@eiu.edu.vn",
                "Authorized User",
                500);

        ResponseStatusException foxGrant = assertThrows(
                ResponseStatusException.class,
                () -> service.grantPets(
                        regular.getId(),
                        new AdminMascotGrantRequest(List.of("fox"), null, 0, false)));
        assertEquals(HttpStatus.BAD_REQUEST, foxGrant.getStatusCode());

        ResponseStatusException foxDelete = assertThrows(
                ResponseStatusException.class,
                () -> service.deletePet(regular.getId(), "fox"));
        assertEquals(HttpStatus.BAD_REQUEST, foxDelete.getStatusCode());

        ResponseStatusException ineligibleIceQueen = assertThrows(
                ResponseStatusException.class,
                () -> service.grantPets(
                        regular.getId(),
                        new AdminMascotGrantRequest(List.of("ice_queen"), null, 0, true)));
        assertEquals(HttpStatus.BAD_REQUEST, ineligibleIceQueen.getStatusCode());

        AdminUserMascotRow authorizedGrant = service.grantPets(
                authorized.getId(),
                new AdminMascotGrantRequest(List.of("ice_queen"), null, 0, true));
        assertEquals("ice_queen", authorizedGrant.equippedMascot());

        AdminUserMascotRow resetToFox = service.setEquippedMascot(
                authorized.getId(),
                new AdminMascotEquipRequest("fox"));
        assertEquals("fox", resetToFox.equippedMascot());
        assertEquals(1, resetToFox.owned().size());
    }

    @Test
    void bulkGrant_distributesCurrenciesAndPetsAndClampsFloorAtZero() {
        UserAccount u1 = registerUser("2431200051", "s51@eiu.edu.vn", "Student 1", 100);
        UserAccount u2 = registerUser("2431200052", "s52@eiu.edu.vn", "Student 2", 100);
        streakStore.get(u1.getId()).setCrystals(1);
        streakStore.get(u2.getId()).setGachaTickets(5);

        StudentMascotPurchase existing = new StudentMascotPurchase();
        existing.setId(new StudentMascotPurchaseKey(u1.getId(), "red_panda"));
        existing.setPricePaid(3666);
        existing.setPurchasedAt(OffsetDateTime.now());
        purchaseStore.put(u1.getId(), new ArrayList<>(List.of(existing)));

        AdminMascotBulkGrantResult result = service.bulkGrantPets(
                new AdminMascotBulkGrantRequest(
                        List.of(u1.getId(), u2.getId()),
                        List.of("red_panda"),
                        0,
                        false,
                        3,
                        15,
                        -500));

        assertEquals(1, result.petsGranted());
        assertEquals(1, result.skippedAlreadyOwned());
        assertEquals(2, result.usersUpdated());
        assertEquals(4, streakStore.get(u1.getId()).getCrystals());
        assertEquals(3, streakStore.get(u2.getId()).getCrystals());
        assertEquals(15, streakStore.get(u1.getId()).getGachaTickets());
        assertEquals(20, streakStore.get(u2.getId()).getGachaTickets());
        assertEquals(0, streakStore.get(u1.getId()).getPoints(), "Negative delta must clamp at 0");
    }
}
