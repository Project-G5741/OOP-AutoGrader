package com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.DTO.ImportStudentRow;
import com.eiu.capstone.backend.DTO.ImportStudentsRequest;
import com.eiu.capstone.backend.DTO.ImportStudentsResult;
import com.eiu.capstone.backend.model.Role;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.AcademicYearRepository;
import com.eiu.capstone.backend.repository.TermEnrollmentRepository;
import com.eiu.capstone.backend.repository.TermRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class TermServiceImportTest {

    @Mock
    private TermRepository termRepository;
    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private TermEnrollmentRepository termEnrollmentRepository;
    @Mock
    private UserAccountRepository userAccountRepository;

    private TermService termService;
    private UUID termId;
    private UUID userId;
    private Term term;

    @BeforeEach
    void setUp() {
        termService = new TermService(
                termRepository, academicYearRepository, termEnrollmentRepository, userAccountRepository);
        termId = UUID.randomUUID();
        userId = UUID.randomUUID();
        term = new Term();
        setTermId(term, termId);
        when(termRepository.findById(termId)).thenReturn(Optional.of(term));
        when(termEnrollmentRepository.findByTermIdWithUser(termId)).thenReturn(List.of());
        when(termEnrollmentRepository.findUserIdsByTermId(termId)).thenReturn(List.of());
    }

    @Test
    void importStudents_enrollsWhenIrnAndEmailMatch() {
        UserAccount user = activeStudent("2331200082", "student@eiu.edu.vn");
        when(userAccountRepository.findByStudentCodeLowerIn(anyList())).thenReturn(List.of(user));

        ImportStudentsResult result = termService.importStudents(termId, request("2331200082", "student@eiu.edu.vn"));

        assertEquals(1, result.enrolled());
        assertEquals(0, result.notFound());
        verify(termEnrollmentRepository).saveAll(any());
    }

    @Test
    void importStudents_skipsWhenEmailDoesNotMatchIrn() {
        UserAccount user = activeStudent("2331200082", "student@eiu.edu.vn");
        when(userAccountRepository.findByStudentCodeLowerIn(anyList())).thenReturn(List.of(user));

        ImportStudentsResult result = termService.importStudents(termId, request("2331200082", "other@eiu.edu.vn"));

        assertEquals(0, result.enrolled());
        assertEquals(1, result.notFound());
        verify(termEnrollmentRepository, never()).saveAll(any());
    }

    @Test
    void importStudents_countsAlreadyEnrolled() {
        UserAccount user = activeStudent("2331200082", "student@eiu.edu.vn");
        when(userAccountRepository.findByStudentCodeLowerIn(anyList())).thenReturn(List.of(user));
        when(termEnrollmentRepository.findUserIdsByTermId(termId)).thenReturn(List.of(userId));

        ImportStudentsResult result = termService.importStudents(termId, request("2331200082", "student@eiu.edu.vn"));

        assertEquals(0, result.enrolled());
        assertEquals(1, result.alreadyInTerm());
        verify(termEnrollmentRepository, never()).saveAll(any());
    }

    private ImportStudentsRequest request(String studentCode, String email) {
        return new ImportStudentsRequest(List.of(new ImportStudentRow(studentCode, email)));
    }

    private UserAccount activeStudent(String studentCode, String email) {
        UserAccount user = new UserAccount();
        setUserId(user, userId);
        user.setStudentCode(studentCode);
        user.setEmail(email);
        user.setFullName("Test Student");
        user.setIsActive(true);
        user.setRoles(Set.of(new Role("STUDENT")));
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
