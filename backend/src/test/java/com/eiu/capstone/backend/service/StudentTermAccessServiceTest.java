package com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

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

@ExtendWith(MockitoExtension.class)
class StudentTermAccessServiceTest {

    @Mock
    private TermService termService;

    private StudentTermAccessService accessService;

    @BeforeEach
    void setUp() {
        accessService = new StudentTermAccessService(termService);
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

    private static void setUserId(UserAccount user, UUID id) {
        try {
            var field = UserAccount.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
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
