package support.com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.service.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class StudentTermAccessServiceTest {

    @Mock
    private TermService termService;

    private StudentTermAccessService accessService;

    @Mock
    private LabDeadlineHelper labDeadlineHelper;

    @Mock
    private UserAccountRepository userAccountRepository;

    @BeforeEach
    void setUp() {
        accessService = new StudentTermAccessService(termService, labDeadlineHelper, userAccountRepository, 30);
    }

    @Test
    void requireCanSubmit_rejectsInactiveStudent() {
        UserAccount user = new UserAccount();
        user.setIsActive(false);
        Lab lab = new Lab();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessService.requireCanSubmit(user, lab));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void requireCanSubmit_rejectsStudentOutsideCurrentTerm() {
        UUID userId = UUID.randomUUID();
        UUID termId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        user.setIsActive(true);
        setUserId(user, userId);

        Term current = new Term();
        setTermId(current, termId);
        Lab lab = new Lab();
        lab.setTerm(current);

        when(termService.findCurrentTerm()).thenReturn(Optional.of(current));
        when(termService.isEnrolled(userId, termId)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessService.requireCanSubmit(user, lab));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void requireUploadAccess_rejectsInactiveStudent() {
        UserAccount user = new UserAccount();
        user.setIsActive(false);
        user.setEmail("a@eiu.edu.vn");
        Term term = currentTerm();
        Lab lab = new Lab();
        lab.setTerm(term);
        UUID labId = UUID.randomUUID();
        when(userAccountRepository.findUploadAccess("a@eiu.edu.vn", labId))
                .thenReturn(uploadAccessRow(user, lab, term, 1L));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessService.requireUploadAccess("a@eiu.edu.vn", labId));
        assertEquals(403, ex.getStatusCode().value());
        assertEquals("This account is inactive", ex.getReason());
    }

    @Test
    void requireUploadAccess_rejectsUnknownUser() {
        UUID labId = UUID.randomUUID();
        when(userAccountRepository.findUploadAccess("a@eiu.edu.vn", labId)).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessService.requireUploadAccess("a@eiu.edu.vn", labId));
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void requireUploadAccess_rejectsMissingLab() {
        UserAccount user = activeUser();
        UUID labId = UUID.randomUUID();
        when(userAccountRepository.findUploadAccess("a@eiu.edu.vn", labId))
                .thenReturn(uploadAccessRow(user, null, null, 0L));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessService.requireUploadAccess("a@eiu.edu.vn", labId));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void requireUploadAccess_rejectsLabNotInCurrentQuarter() {
        UserAccount user = activeUser();
        Term term = new Term();
        term.setCurrent(false);
        Lab lab = new Lab();
        lab.setTerm(term);
        UUID labId = UUID.randomUUID();
        when(userAccountRepository.findUploadAccess("a@eiu.edu.vn", labId))
                .thenReturn(uploadAccessRow(user, lab, term, 1L));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessService.requireUploadAccess("a@eiu.edu.vn", labId));
        assertEquals(403, ex.getStatusCode().value());
        assertEquals("This lab is not in the current quarter", ex.getReason());
    }

    @Test
    void requireUploadAccess_rejectsWhenNotEnrolled() {
        UserAccount user = activeUser();
        Term term = currentTerm();
        Lab lab = new Lab();
        lab.setTerm(term);
        UUID labId = UUID.randomUUID();
        when(userAccountRepository.findUploadAccess("a@eiu.edu.vn", labId))
                .thenReturn(uploadAccessRow(user, lab, term, 0L));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessService.requireUploadAccess("a@eiu.edu.vn", labId));
        assertEquals(403, ex.getStatusCode().value());
        assertEquals("You are not enrolled in the current quarter", ex.getReason());
    }

    @Test
    void requireUploadAccess_returnsUserAndLabWhenAllowed() {
        UserAccount user = activeUser();
        Term term = currentTerm();
        Lab lab = new Lab();
        lab.setStudentVisible(true);
        lab.setTerm(term);
        UUID labId = UUID.randomUUID();
        when(userAccountRepository.findUploadAccess("a@eiu.edu.vn", labId))
                .thenReturn(uploadAccessRow(user, lab, term, 1L));
        when(labDeadlineHelper.isOpenForStudentSubmission(
                org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any())).thenReturn(true);

        StudentTermAccessService.UploadAccess access =
                accessService.requireUploadAccess("a@eiu.edu.vn", labId);
        assertSame(user, access.user());
        assertSame(lab, access.lab());
    }

    @Test
    void requireUploadAccess_reusesSuccessfulCheckWithinTtl() {
        UserAccount user = activeUser();
        Term term = currentTerm();
        Lab lab = new Lab();
        lab.setStudentVisible(true);
        lab.setTerm(term);
        UUID labId = UUID.randomUUID();
        when(userAccountRepository.findUploadAccess("a@eiu.edu.vn", labId))
                .thenReturn(uploadAccessRow(user, lab, term, 1L));
        when(labDeadlineHelper.isOpenForStudentSubmission(
                org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any())).thenReturn(true);

        accessService.requireUploadAccess("a@eiu.edu.vn", labId);
        StudentTermAccessService.UploadAccess again =
                accessService.requireUploadAccess("a@eiu.edu.vn", labId);

        assertSame(user, again.user());
        org.mockito.Mockito.verify(userAccountRepository, org.mockito.Mockito.times(1))
                .findUploadAccess("a@eiu.edu.vn", labId);
        org.mockito.Mockito.verify(labDeadlineHelper, org.mockito.Mockito.times(2))
                .isOpenForStudentSubmission(
                        org.mockito.ArgumentMatchers.eq(true),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rememberSuccessfulAccess_letsFirstUploadSkipQuery() {
        UserAccount user = activeUser();
        Term term = currentTerm();
        Lab lab = new Lab();
        lab.setStudentVisible(true);
        lab.setTerm(term);
        UUID labId = UUID.randomUUID();
        setLabId(lab, labId);
        when(labDeadlineHelper.isOpenForStudentSubmission(
                org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any())).thenReturn(true);

        accessService.rememberSuccessfulAccess(user, lab);
        StudentTermAccessService.UploadAccess access =
                accessService.requireUploadAccess("a@eiu.edu.vn", labId);

        assertSame(user, access.user());
        org.mockito.Mockito.verify(userAccountRepository, org.mockito.Mockito.never())
                .findUploadAccess(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private static List<Object[]> uploadAccessRow(Object... cols) {
        return Collections.singletonList(cols);
    }

    private static UserAccount activeUser() {
        UserAccount user = new UserAccount();
        user.setIsActive(true);
        user.setEmail("a@eiu.edu.vn");
        return user;
    }

    private static Term currentTerm() {
        Term term = new Term();
        term.setCurrent(true);
        return term;
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

    private static void setLabId(Lab lab, UUID id) {
        try {
            var field = Lab.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(lab, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setTermId(Term term, UUID id) {
        try {
            var field = Term.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(term, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
