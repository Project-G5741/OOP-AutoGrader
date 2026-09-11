package support.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.repository.AcademicYearRepository;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.TermEnrollmentRepository;
import com.eiu.capstone.backend.repository.TermRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.service.TermService;

@ExtendWith(MockitoExtension.class)
class TermServiceDeleteTest {

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
    private com.eiu.capstone.backend.analytics.cache.LecturerOverviewCache lecturerOverviewCache;

    private TermService termService;
    private UUID termId;

    @BeforeEach
    void setUp() {
        termService = new TermService(
                termRepository,
                academicYearRepository,
                termEnrollmentRepository,
                userAccountRepository,
                labRepository,
                lecturerOverviewCache);
        termId = UUID.randomUUID();
    }

    @Test
    void deleteTerm_currentTerm_throws409() {
        Term term = new Term();
        term.setCurrent(true);
        when(termRepository.findById(termId)).thenReturn(Optional.of(term));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> termService.deleteTerm(termId));
        assertEquals(409, ex.getStatusCode().value());
        verify(termRepository, never()).delete(term);
    }

    @Test
    void deleteTerm_hasLabs_throws409() {
        Term term = new Term();
        term.setCurrent(false);
        when(termRepository.findById(termId)).thenReturn(Optional.of(term));
        when(labRepository.countByTerm_Id(termId)).thenReturn(2L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> termService.deleteTerm(termId));
        assertEquals(409, ex.getStatusCode().value());
        verify(termRepository, never()).delete(term);
    }

    @Test
    void deleteTerm_ok_removesEnrollmentsAndTerm() {
        Term term = new Term();
        term.setCurrent(false);
        when(termRepository.findById(termId)).thenReturn(Optional.of(term));
        when(labRepository.countByTerm_Id(termId)).thenReturn(0L);

        termService.deleteTerm(termId);

        verify(termEnrollmentRepository).deleteByTerm_Id(termId);
        verify(termRepository).delete(term);
        verify(lecturerOverviewCache).invalidate();
    }
}
