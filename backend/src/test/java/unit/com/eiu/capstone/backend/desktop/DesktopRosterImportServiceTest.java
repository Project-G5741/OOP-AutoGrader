package unit.com.eiu.capstone.backend.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.desktop.DesktopRosterImportService;
import com.eiu.capstone.backend.model.AcademicYear;
import com.eiu.capstone.backend.model.Role;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.TermEnrollment;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.AcademicYearRepository;
import com.eiu.capstone.backend.repository.RoleRepository;
import com.eiu.capstone.backend.repository.TermEnrollmentRepository;
import com.eiu.capstone.backend.repository.TermRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

class DesktopRosterImportServiceTest {

    @TempDir
    Path tempDir;

    private TermRepository termRepository;
    private AcademicYearRepository academicYearRepository;
    private TermEnrollmentRepository termEnrollmentRepository;
    private UserAccountRepository userAccountRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private DesktopRosterImportService service;
    private Term currentTerm;

    @BeforeEach
    void setUp() {
        termRepository = mock(TermRepository.class);
        academicYearRepository = mock(AcademicYearRepository.class);
        termEnrollmentRepository = mock(TermEnrollmentRepository.class);
        userAccountRepository = mock(UserAccountRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new DesktopRosterImportService(
                termRepository,
                academicYearRepository,
                termEnrollmentRepository,
                userAccountRepository,
                roleRepository,
                passwordEncoder,
                new ObjectMapper(),
                tempDir.toString());

        currentTerm = new Term();
        currentTerm.setId(java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"));
        currentTerm.setCurrent(true);
        when(termRepository.findCurrent()).thenReturn(Optional.of(currentTerm));
        when(roleRepository.findByName("STUDENT")).thenReturn(Optional.of(new Role("STUDENT")));
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(termEnrollmentRepository.save(any(TermEnrollment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void replaceImport_materializesStubAccountsAndEnrollments() {
        when(userAccountRepository.findByStudentCode("2331200057")).thenReturn(Optional.empty());
        when(userAccountRepository.findByStudentCode("2331200058")).thenReturn(Optional.empty());

        DesktopRosterImportService.ImportResult result =
                service.replaceImport(List.of("2331200057", "2331200058", "2331200057"));

        assertEquals(2, result.importedCount());
        assertFalse(result.blocked());
        verify(termEnrollmentRepository).deleteByTerm_Id(currentTerm.getId());
        verify(termEnrollmentRepository, times(2)).save(any(TermEnrollment.class));
        verify(userAccountRepository, times(2)).save(any(UserAccount.class));
        assertTrue(java.nio.file.Files.isRegularFile(tempDir.resolve("data").resolve("offline-roster-irns.json")));
    }

    @Test
    void replaceImport_emptyList_clearsAndBlocks() {
        DesktopRosterImportService.ImportResult result = service.replaceImport(List.of());

        assertEquals(0, result.importedCount());
        assertTrue(result.blocked());
        verify(termEnrollmentRepository).deleteByTerm_Id(currentTerm.getId());
        verify(userAccountRepository, never()).save(any(UserAccount.class));
        verify(termEnrollmentRepository, never()).save(any(TermEnrollment.class));
    }

    @Test
    void replaceImport_rejectsNonTenDigitIrn() {
        assertThrows(ResponseStatusException.class,
                () -> service.replaceImport(List.of("233120005")));
        verify(termEnrollmentRepository, never()).deleteByTerm_Id(any());
    }

    @Test
    void hasUsableRoster_ignoresNonTenDigitCodes() {
        when(termEnrollmentRepository.findStudentCodesEnrolledInCurrentTerm())
                .thenReturn(List.of("DESKTOP", "LECTURER"));
        assertFalse(service.hasUsableRoster());

        when(termEnrollmentRepository.findStudentCodesEnrolledInCurrentTerm())
                .thenReturn(List.of("DESKTOP", "2331200057"));
        assertTrue(service.hasUsableRoster());
    }

    @Test
    void restorePersistedRoster_reimportsSavedIrns() {
        when(userAccountRepository.findByStudentCode("2331200057")).thenReturn(Optional.empty());
        service.replaceImport(List.of("2331200057"));
        verify(termEnrollmentRepository, times(1)).save(any(TermEnrollment.class));

        service.restorePersistedRosterIntoCurrentTerm();
        verify(termEnrollmentRepository, times(2)).deleteByTerm_Id(currentTerm.getId());
        verify(termEnrollmentRepository, times(2)).save(any(TermEnrollment.class));
    }

    @Test
    void replaceImport_createsOfflineTermWhenNoneCurrent() {
        when(termRepository.findCurrent()).thenReturn(Optional.empty());
        AcademicYear year = new AcademicYear();
        year.setYearLabel("Offline");
        when(academicYearRepository.findByYearLabel("Offline")).thenReturn(Optional.of(year));
        when(termRepository.save(any(Term.class))).thenAnswer(inv -> {
            Term t = inv.getArgument(0);
            t.setId(java.util.UUID.fromString("22222222-2222-2222-2222-222222222222"));
            return t;
        });
        when(userAccountRepository.findByStudentCode("2331200057")).thenReturn(Optional.empty());

        DesktopRosterImportService.ImportResult result =
                service.replaceImport(List.of("2331200057"));

        assertEquals(1, result.importedCount());
        assertFalse(result.blocked());
        verify(termRepository).save(any(Term.class));
        verify(termRepository).clearOtherCurrent(any());
    }
}
