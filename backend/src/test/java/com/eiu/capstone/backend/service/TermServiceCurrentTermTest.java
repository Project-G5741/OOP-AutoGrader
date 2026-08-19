package com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.repository.AcademicYearRepository;
import com.eiu.capstone.backend.repository.TermEnrollmentRepository;
import com.eiu.capstone.backend.repository.TermRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;

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

    private TermService termService;

    @BeforeEach
    void setUp() {
        termService = new TermService(
                termRepository, academicYearRepository, termEnrollmentRepository, userAccountRepository);
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
