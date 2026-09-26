package support.com.eiu.capstone.backend.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import com.eiu.capstone.backend.analytics.cache.LecturerOverviewCache;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.TermEnrollment;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.AcademicYearRepository;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.TermEnrollmentRepository;
import com.eiu.capstone.backend.repository.TermRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.service.SessionValidityService;
import com.eiu.capstone.backend.service.TermService;

@ExtendWith(MockitoExtension.class)
class TermServiceRemoveStudentSessionTest {

    @Mock
    private TermRepository termRepository;
    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private TermEnrollmentRepository termEnrollmentRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private LabRepository labRepository;
    @Mock
    private LecturerOverviewCache lecturerOverviewCache;
    @Mock
    private SessionValidityService sessionValidityService;
    @Mock
    private com.eiu.capstone.backend.service.LabCloneService labCloneService;
    @Mock
    private com.eiu.capstone.backend.service.LabStructureService labStructureService;
    @Mock
    private PlatformTransactionManager transactionManager;

    private TermService termService;
    private UUID termId;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        termService = new TermService(
                termRepository,
                academicYearRepository,
                termEnrollmentRepository,
                userAccountRepository,
                labRepository,
                lecturerOverviewCache,
                sessionValidityService,
                labCloneService,
                labStructureService,
                transactionManager);
        termId = UUID.randomUUID();
        studentId = UUID.randomUUID();
    }

    @Test
    void removeStudent_currentTerm_bumpsSession() {
        Term term = new Term();
        term.setCurrent(true);
        TermEnrollment enrollment = new TermEnrollment();
        when(termRepository.findById(termId)).thenReturn(Optional.of(term));
        when(termEnrollmentRepository.findByUser_IdAndTerm_Id(studentId, termId))
                .thenReturn(Optional.of(enrollment));

        termService.removeStudent(termId, studentId);

        verify(termEnrollmentRepository).delete(enrollment);
        verify(sessionValidityService).bumpSessionVersion(studentId);
    }

    @Test
    void removeStudent_nonCurrentTerm_doesNotBumpSession() {
        Term term = new Term();
        term.setCurrent(false);
        TermEnrollment enrollment = new TermEnrollment();
        when(termRepository.findById(termId)).thenReturn(Optional.of(term));
        when(termEnrollmentRepository.findByUser_IdAndTerm_Id(studentId, termId))
                .thenReturn(Optional.of(enrollment));

        termService.removeStudent(termId, studentId);

        verify(termEnrollmentRepository).delete(enrollment);
        verify(sessionValidityService, never()).bumpSessionVersion(studentId);
        verify(sessionValidityService, never()).bumpSessionVersion(org.mockito.ArgumentMatchers.any(UserAccount.class));
    }
}
