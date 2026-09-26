package support.com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.service.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import com.eiu.capstone.backend.repository.AcademicYearRepository;
import com.eiu.capstone.backend.repository.TermEnrollmentRepository;
import com.eiu.capstone.backend.repository.TermRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.service.LabCloneService;
import com.eiu.capstone.backend.service.LabStructureService;
import com.eiu.capstone.backend.service.SessionValidityService;
import com.eiu.capstone.backend.service.TermService;

@ExtendWith(MockitoExtension.class)
class TermServiceCurrentTermTest {

    @Mock
    private TermRepository termRepository;
    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private TermEnrollmentRepository termEnrollmentRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private com.eiu.capstone.backend.repository.LabRepository labRepository;
    @Mock
    private com.eiu.capstone.backend.analytics.cache.LecturerOverviewCache lecturerOverviewCache;
    @Mock
    private SessionValidityService sessionValidityService;
    @Mock
    private LabCloneService labCloneService;
    @Mock
    private LabStructureService labStructureService;
    @Mock
    private PlatformTransactionManager transactionManager;

    private TermService termService;

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
    }

    @Test
    void isInCurrentTerm_nullUser_false() {
        assertFalse(termService.isInCurrentTerm(null));
    }

    @Test
    void isInCurrentTerm_usesCurrentFlagExistsQuery() {
        UUID userId = UUID.randomUUID();
        when(termEnrollmentRepository.existsByUser_IdAndTerm_CurrentTrue(userId)).thenReturn(true);
        assertTrue(termService.isInCurrentTerm(userId));
    }
}
