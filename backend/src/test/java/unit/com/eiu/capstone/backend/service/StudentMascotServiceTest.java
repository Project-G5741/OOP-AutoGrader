package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.MascotShopView;
import com.eiu.capstone.backend.model.Role;
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
}
