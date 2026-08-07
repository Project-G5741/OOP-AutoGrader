package com.eiu.capstone.backend.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.DTO.StudentChallengeResultDTO;
import com.eiu.capstone.backend.DTO.StudentHistoryStatsDTO;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.repository.StudentLabProgressRepository;
import com.eiu.capstone.backend.repository.SubmissionChallengeResultRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentHistoryServiceTest {

    @Mock
    private StudentLabProgressRepository studentLabProgressRepository;

    @Mock
    private LabSubmissionRepository labSubmissionRepository;

    @Mock
    private SubmissionChallengeResultRepository submissionChallengeResultRepository;

    @Mock
    private ChallengeService challengeService;

    @InjectMocks
    private StudentHistoryService studentHistoryService;

    @Test
    void deriveStatus_allChallengesFailed_returnsFailed() {
        List<StudentChallengeResultDTO> challenges = List.of(
                new StudentChallengeResultDTO("Challenge 1", false, 0),
                new StudentChallengeResultDTO("Challenge 2", false, 0));

        assertEquals("failed", studentHistoryService.deriveStatus(BigDecimal.ZERO, challenges));
    }

    @Test
    void deriveStatus_mixedChallenges_returnsPartial() {
        List<StudentChallengeResultDTO> challenges = List.of(
                new StudentChallengeResultDTO("Challenge 1", true, 100),
                new StudentChallengeResultDTO("Challenge 2", false, 40));

        assertEquals("partial", studentHistoryService.deriveStatus(new BigDecimal("70.00"), challenges));
    }

    @Test
    void deriveStatus_noChallengeData_usesOverallScore() {
        assertEquals("partial", studentHistoryService.deriveStatus(new BigDecimal("74.47"), List.of()));
        assertEquals("failed", studentHistoryService.deriveStatus(BigDecimal.ZERO, List.of()));
        assertEquals("unknown", studentHistoryService.deriveStatus(null, List.of()));
    }

    @Test
    void computeStats_emptySubmissions_returnsZerosAndNulls() {
        StudentHistoryStatsDTO stats = studentHistoryService.computeStats(List.of(), null);

        assertEquals(0, stats.labsAttempted());
        assertEquals(0, stats.totalSubmissions());
        assertNull(stats.averageScore());
        assertNull(stats.bestScore());
    }

    @Test
    void computeStats_filteredLab_countsOneLabAndAveragesScores() {
        UUID labId = UUID.randomUUID();
        List<LabSubmission> submissions = List.of(
                submissionWithScore(new BigDecimal("80.00")),
                submissionWithScore(new BigDecimal("90.00")),
                submissionWithScore(new BigDecimal("70.00")));

        StudentHistoryStatsDTO stats = studentHistoryService.computeStats(submissions, labId);

        assertEquals(1, stats.labsAttempted());
        assertEquals(3, stats.totalSubmissions());
        assertEquals(0, stats.averageScore().compareTo(new BigDecimal("80.00")));
        assertEquals(0, stats.bestScore().compareTo(new BigDecimal("90.00")));
    }

    @Test
    void computeStats_unfiltered_countsDistinctLabs() {
        UUID labA = UUID.randomUUID();
        UUID labB = UUID.randomUUID();
        List<LabSubmission> submissions = List.of(
                submissionForLab(labA, new BigDecimal("80.00")),
                submissionForLab(labB, new BigDecimal("60.00")),
                submissionForLab(labA, new BigDecimal("100.00")));

        StudentHistoryStatsDTO stats = studentHistoryService.computeStats(submissions, null);

        assertEquals(2, stats.labsAttempted());
        assertEquals(3, stats.totalSubmissions());
        assertEquals(0, stats.bestScore().compareTo(new BigDecimal("100.00")));
    }

    private LabSubmission submissionWithScore(BigDecimal score) {
        LabSubmission submission = new LabSubmission();
        submission.setScore(score);
        submission.setAttemptNumber(1);
        submission.setSubmittedAt(OffsetDateTime.parse("2026-08-01T10:00:00+07:00"));
        return submission;
    }

    private LabSubmission submissionForLab(UUID labId, BigDecimal score) {
        Lab lab = mock(Lab.class);
        when(lab.getId()).thenReturn(labId);

        LabSubmission submission = submissionWithScore(score);
        submission.setLab(lab);
        return submission;
    }
}
