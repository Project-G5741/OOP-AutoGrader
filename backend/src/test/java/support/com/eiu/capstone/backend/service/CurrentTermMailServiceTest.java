package support.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.CurrentTermMailRequest;
import com.eiu.capstone.backend.DTO.CurrentTermMailResult;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.TermEnrollmentRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.service.CurrentTermMailService;
import com.eiu.capstone.backend.service.TermService;
import com.eiu.capstone.backend.service.TransactionalEmailSender;

@ExtendWith(MockitoExtension.class)
class CurrentTermMailServiceTest {

    @Mock
    private TermService termService;
    @Mock
    private TermEnrollmentRepository termEnrollmentRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private TransactionalEmailSender emailSender;

    private CurrentTermMailService mailService;

    @BeforeEach
    void setUp() {
        mailService = new CurrentTermMailService(
                termService, termEnrollmentRepository, userAccountRepository, emailSender);
    }

    @Test
    void noCurrentTerm_isBadRequest() {
        when(termService.findCurrentTerm()).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> mailService.sendToCurrentTerm(
                new CurrentTermMailRequest("Hello", "Body")));
        verify(emailSender, never()).sendPlainText(any(), any(), any());
    }

    @Test
    void sendsToActiveEnrolledStudentsWithEmail() {
        UUID termId = UUID.randomUUID();
        UUID withMail = UUID.randomUUID();
        UUID noMail = UUID.randomUUID();
        Term term = new Term();
        term.setId(termId);
        when(termService.findCurrentTerm()).thenReturn(Optional.of(term));
        when(termEnrollmentRepository.findActiveStudentIdsByTermId(termId))
                .thenReturn(List.of(withMail, noMail));
        UserAccount mailed = new UserAccount();
        mailed.setEmail("student@eiu.edu.vn");
        UserAccount skipped = new UserAccount();
        skipped.setEmail("  ");
        when(userAccountRepository.findAllById(List.of(withMail, noMail)))
                .thenReturn(List.of(mailed, skipped));

        CurrentTermMailResult result = mailService.sendToCurrentTerm(
                new CurrentTermMailRequest(" Lab notice ", " Please submit. "));

        assertEquals(2, result.recipientCount());
        assertEquals(1, result.sentCount());
        assertEquals(0, result.failedCount());
        assertEquals(1, result.skippedNoEmail());
        verify(emailSender).sendPlainText("student@eiu.edu.vn", "Lab notice", "Please submit.");
    }

    @Test
    void countsFailedSendAndContinues() {
        UUID termId = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Term term = new Term();
        term.setId(termId);
        when(termService.findCurrentTerm()).thenReturn(Optional.of(term));
        when(termEnrollmentRepository.findActiveStudentIdsByTermId(termId))
                .thenReturn(List.of(first, second));
        UserAccount a = new UserAccount();
        a.setEmail("a@eiu.edu.vn");
        UserAccount b = new UserAccount();
        b.setEmail("b@eiu.edu.vn");
        when(userAccountRepository.findAllById(List.of(first, second))).thenReturn(List.of(a, b));
        doThrow(new RuntimeException("brevo down"))
                .when(emailSender).sendPlainText(eq("a@eiu.edu.vn"), any(), any());

        CurrentTermMailResult result = mailService.sendToCurrentTerm(
                new CurrentTermMailRequest("Subject", "Body"));

        assertEquals(1, result.sentCount());
        assertEquals(1, result.failedCount());
        verify(emailSender).sendPlainText(eq("b@eiu.edu.vn"), eq("Subject"), eq("Body"));
    }
}
