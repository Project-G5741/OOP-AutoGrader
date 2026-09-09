package support.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.model.AcademicYear;
import com.eiu.capstone.backend.model.Role;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.TermEnrollmentRepository;
import com.eiu.capstone.backend.repository.TermRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.service.StudentAccountExpiryService;
import com.eiu.capstone.backend.service.UserService;

@ExtendWith(MockitoExtension.class)
class StudentAccountExpiryServiceTest {

    @Mock
    private TermRepository termRepository;
    @Mock
    private TermEnrollmentRepository termEnrollmentRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private UserService userService;

    private StudentAccountExpiryService service;

    @BeforeEach
    void setUp() {
        service = new StudentAccountExpiryService(
                termRepository, termEnrollmentRepository, userAccountRepository, userService);
    }

    @Test
    void purgeExpiredStudents_noCurrentTerm_doesNothing() {
        when(termRepository.findCurrentWithAcademicYear()).thenReturn(Optional.empty());

        assertEquals(0, service.purgeExpiredStudents());

        verify(termEnrollmentRepository, never()).findStudentIdsWithFirstEnrollmentOrdinalAtMost(anyInt());
    }

    @Test
    void purgeExpiredStudents_deletesStudentOnlyAccounts() {
        Term current = term("2025-2026", 4);
        when(termRepository.findCurrentWithAcademicYear()).thenReturn(Optional.of(current));

        UUID studentId = UUID.randomUUID();
        int maxFirstOrdinal = 100; // Q1 2025-2026 when current is Q4 2025-2026
        when(termEnrollmentRepository.findStudentIdsWithFirstEnrollmentOrdinalAtMost(maxFirstOrdinal))
                .thenReturn(List.of(studentId));
        when(userAccountRepository.findAllWithRolesByIdIn(List.of(studentId)))
                .thenReturn(List.of(studentOnly(studentId)));

        assertEquals(1, service.purgeExpiredStudents());

        verify(userService).deleteUser(studentId);
    }

    private static Term term(String yearLabel, int termNumber) {
        AcademicYear year = new AcademicYear();
        year.setYearLabel(yearLabel);
        Term term = new Term();
        term.setAcademicYear(year);
        term.setTermNumber(termNumber);
        return term;
    }

    private static UserAccount studentOnly(UUID id) {
        UserAccount user = new UserAccount();
        setUserId(user, id);
        user.setIsActive(true);
        Role student = new Role();
        student.setName("STUDENT");
        Set<Role> roles = new HashSet<>();
        roles.add(student);
        user.setRoles(roles);
        return user;
    }

    private static void setUserId(UserAccount user, UUID id) {
        try {
            var field = UserAccount.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
