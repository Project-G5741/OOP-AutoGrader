package support.com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.service.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.LabDeadlineEmailSentRepository;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.TermEnrollmentRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class LabDeadlineEmailServiceTest {

    @Mock
    private LabRepository labRepository;

    @Mock
    private TermEnrollmentRepository termEnrollmentRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private LabDeadlineEmailSentRepository emailSentRepository;

    @Mock
    private TransactionalEmailSender emailSender;

    @Mock
    private LabDeadlineHelper labDeadlineHelper;

    private LabDeadlineEmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new LabDeadlineEmailService(
                labRepository,
                termEnrollmentRepository,
                userAccountRepository,
                emailSentRepository,
                emailSender,
                labDeadlineHelper,
                "http://localhost:5173");
    }

    @Test
    void sendForLabThreshold_loadsCandidatesOnce_thenFindAllById() {
        UUID labId = UUID.randomUUID();
        UUID termId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        LocalDate deadline = LocalDate.of(2026, 9, 12);
        Term term = org.mockito.Mockito.mock(Term.class);
        when(term.getId()).thenReturn(termId);
        Lab lab = org.mockito.Mockito.mock(Lab.class);
        when(lab.getId()).thenReturn(labId);
        when(lab.getName()).thenReturn("Lab 1");
        when(lab.getDeadlineDate()).thenReturn(deadline);
        when(lab.getTerm()).thenReturn(term);
        when(labRepository.findAllWithDeadlineAndTerm()).thenReturn(List.of(lab));
        when(labDeadlineHelper.cutoffInstant(deadline))
                .thenReturn(Instant.now().plus(Duration.ofHours(72)));

        UserAccount student = org.mockito.Mockito.mock(UserAccount.class);
        when(student.getEmail()).thenReturn("student@eiu.edu.vn");
        when(termEnrollmentRepository.findActiveStudentIdsForDeadlineEmail(termId, labId, (short) 72))
                .thenReturn(List.of(studentId));
        when(userAccountRepository.findAllById(List.of(studentId))).thenReturn(List.of(student));

        emailService.processThreshold((short) 72);

        verify(termEnrollmentRepository).findActiveStudentIdsForDeadlineEmail(termId, labId, (short) 72);
        verify(userAccountRepository).findAllById(List.of(studentId));
        verify(emailSentRepository, never()).existsByLab_IdAndUser_IdAndThresholdHours(any(), any(), anyShort());
        verify(emailSender).sendPlainText(
                eq("student@eiu.edu.vn"),
                eq("Lab deadline in 3 days: Lab 1"),
                org.mockito.ArgumentMatchers.contains("Lab 1"));
        verify(emailSentRepository).save(any());
    }
}
