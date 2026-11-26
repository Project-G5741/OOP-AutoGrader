package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.MascotShopView;
import com.eiu.capstone.backend.DTO.MascotSnapshotView;
import com.eiu.capstone.backend.model.StudentMascotPurchase;
import com.eiu.capstone.backend.model.StudentMascotPurchaseKey;
import com.eiu.capstone.backend.model.StudentStreak;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.StudentMascotPurchaseRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.service.StudentMascotService;
import com.eiu.capstone.backend.service.StudentStreakService;

class StudentMascotServiceTest {

    @Test
    void iceQueen_eligibleUser_canSeeAndEquip() {
        StudentStreakService streakService = mock(StudentStreakService.class);
        StudentMascotPurchaseRepository purchases = mock(StudentMascotPurchaseRepository.class);
        UserAccountRepository userAccounts = mock(UserAccountRepository.class);

        StudentMascotService service = new StudentMascotService(streakService, purchases, userAccounts);

        UUID adminId = UUID.randomUUID();
        UserAccount admin = new UserAccount();
        admin.setStudentCode("2331200057");
        admin.setEmail("kiet.doan.cit23@eiu.edu.vn");
        when(userAccounts.findById(adminId)).thenReturn(Optional.of(admin));

        StudentStreak streak = new StudentStreak();
        streak.setEquippedMascot("ice_queen");
        when(streakService.find(adminId)).thenReturn(Optional.of(streak));
        when(streakService.lock(adminId)).thenReturn(streak);

        MascotShopView view = service.list(adminId);
        assertTrue(view.mascots().stream().anyMatch(m -> "ice_queen".equals(m.id())), "Admin should see ice_queen");
        assertEquals("ice_queen", view.equipped());

        // Ineligible student
        UUID studentId = UUID.randomUUID();
        UserAccount student = new UserAccount();
        student.setStudentCode("2431200001");
        student.setEmail("student@eiu.edu.vn");
        when(userAccounts.findById(studentId)).thenReturn(Optional.of(student));

        MascotShopView studentView = service.list(studentId);
        assertFalse(studentView.mascots().stream().anyMatch(m -> "ice_queen".equals(m.id())), "Other students should NOT see ice_queen");

        assertThrows(ResponseStatusException.class, () -> service.equip(studentId, "ice_queen"));
    }

    @Test
    void retiredGoat_isRejectedAndEquippedFallsBackToFox() {
        StudentStreakService streakService = mock(StudentStreakService.class);
        StudentMascotPurchaseRepository purchases = mock(StudentMascotPurchaseRepository.class);
        UserAccountRepository userAccounts = mock(UserAccountRepository.class);

        StudentMascotService service = new StudentMascotService(streakService, purchases, userAccounts);

        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        user.setStudentCode("2331200041");
        user.setEmail("student2@eiu.edu.vn");
        when(userAccounts.findById(userId)).thenReturn(Optional.of(user));

        StudentStreak streak = new StudentStreak();
        streak.setEquippedMascot("goat");
        when(streakService.find(userId)).thenReturn(Optional.of(streak));

        MascotShopView view = service.list(userId);
        assertFalse(view.mascots().stream().anyMatch(m -> "goat".equals(m.id())));
        assertEquals("fox", view.equipped());

        assertThrows(ResponseStatusException.class, () -> service.equip(userId, "goat"));
        assertThrows(ResponseStatusException.class, () -> service.purchase(userId, "goat"));
    }

    @Test
    void mythicAndNewCatalogMascots_normalizeHyphenAndAppearInShop() {
        StudentStreakService streakService = mock(StudentStreakService.class);
        StudentMascotPurchaseRepository purchases = mock(StudentMascotPurchaseRepository.class);
        UserAccountRepository userAccounts = mock(UserAccountRepository.class);

        StudentMascotService service = new StudentMascotService(streakService, purchases, userAccounts);

        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        user.setStudentCode("2431200099");
        user.setEmail("student99@eiu.edu.vn");
        when(userAccounts.findById(userId)).thenReturn(Optional.of(user));

        StudentStreak streak = new StudentStreak();
        streak.setEquippedMascot("cyber-striker");
        when(streakService.find(userId)).thenReturn(Optional.of(streak));

        MascotShopView view = service.list(userId);
        assertEquals("cyber_striker", view.equipped());
        assertTrue(view.mascots().stream().anyMatch(m -> "floral_fairy".equals(m.id()) && m.price() == 75000));
        assertTrue(view.mascots().stream().anyMatch(m -> "chrono_celestia".equals(m.id()) && m.price() == 75000));
        assertTrue(view.mascots().stream().anyMatch(m -> "cyber_striker".equals(m.id()) && m.price() == 75000));
        assertTrue(view.mascots().stream().anyMatch(m -> "kiem_de".equals(m.id()) && m.price() == 75000));
    }

    @Test
    void listSnapshotAndPurchase_normalizeHyphenatedPurchaseRows() {
        StudentStreakService streakService = mock(StudentStreakService.class);
        StudentMascotPurchaseRepository purchases = mock(StudentMascotPurchaseRepository.class);
        UserAccountRepository userAccounts = mock(UserAccountRepository.class);

        StudentMascotService service = new StudentMascotService(streakService, purchases, userAccounts);

        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        user.setStudentCode("2431200011");
        user.setEmail("student11@eiu.edu.vn");
        when(userAccounts.findById(userId)).thenReturn(Optional.of(user));

        StudentStreak streak = new StudentStreak();
        streak.setPoints(20_000);
        streak.setEquippedMascot("fox");
        when(streakService.find(userId)).thenReturn(Optional.of(streak));
        when(streakService.lock(userId)).thenReturn(streak);

        StudentMascotPurchase legacy = new StudentMascotPurchase();
        legacy.setId(new StudentMascotPurchaseKey(userId, "thanh-long"));
        legacy.setPricePaid(9999);
        legacy.setPurchasedAt(OffsetDateTime.now());
        when(purchases.findByIdUserId(userId)).thenReturn(List.of(legacy));

        MascotShopView view = service.list(userId);
        assertTrue(view.mascots().stream().anyMatch(m -> "thanh_long".equals(m.id()) && m.owned()));

        MascotSnapshotView snap = service.snapshot(userId);
        assertTrue(snap.owned().contains("thanh_long"));

        ResponseStatusException dup = assertThrows(
                ResponseStatusException.class,
                () -> service.purchase(userId, "thanh_long"));
        assertEquals(HttpStatus.CONFLICT, dup.getStatusCode());
        assertEquals(20_000, streak.getPoints());
    }

    @Test
    void listAndEquip_preserveOwnershipForEquippedMascotMissingPurchaseRow() {
        StudentStreakService streakService = mock(StudentStreakService.class);
        StudentMascotPurchaseRepository purchases = mock(StudentMascotPurchaseRepository.class);
        UserAccountRepository userAccounts = mock(UserAccountRepository.class);

        StudentMascotService service = new StudentMascotService(streakService, purchases, userAccounts);

        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        user.setStudentCode("2431200022");
        user.setEmail("student22@eiu.edu.vn");
        when(userAccounts.findById(userId)).thenReturn(Optional.of(user));

        StudentStreak streak = new StudentStreak();
        streak.setPoints(500);
        streak.setEquippedMascot("cat");
        when(streakService.find(userId)).thenReturn(Optional.of(streak));
        when(streakService.lock(userId)).thenReturn(streak);
        when(purchases.findByIdUserId(userId)).thenReturn(List.of());

        MascotShopView beforeSwitch = service.list(userId);
        assertTrue(beforeSwitch.mascots().stream().anyMatch(m -> "cat".equals(m.id()) && m.owned() && m.equipped()));

        MascotShopView afterSwitch = service.equip(userId, "fox");
        assertEquals("fox", afterSwitch.equipped());
        assertTrue(afterSwitch.mascots().stream().anyMatch(m -> "cat".equals(m.id()) && m.owned()));

        ArgumentCaptor<StudentMascotPurchase> captor = ArgumentCaptor.forClass(StudentMascotPurchase.class);
        verify(purchases, times(1)).save(captor.capture());
        assertEquals("cat", captor.getValue().getId().getMascotId());
        assertEquals(0, captor.getValue().getPricePaid());
    }
}
