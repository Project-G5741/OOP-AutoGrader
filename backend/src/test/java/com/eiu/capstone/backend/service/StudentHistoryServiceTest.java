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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
    void deriveStatus_scoreBands() {
        assertEquals("failed", studentHistoryService.deriveStatus(new BigDecimal("49.99"), List.of()));
        assertEquals("failed", studentHistoryService.deriveStatus(BigDecimal.ZERO, List.of()));

        assertEquals("partial", studentHistoryService.deriveStatus(new BigDecimal("50"), List.of()));
        assertEquals("partial", studentHistoryService.deriveStatus(new BigDecimal("74.47"), List.of()));
        assertEquals("partial", studentHistoryService.deriveStatus(new BigDecimal("80"), List.of()));

        assertEquals("passed", studentHistoryService.deriveStatus(new BigDecimal("80.01"), List.of()));
        assertEquals("passed", studentHistoryService.deriveStatus(new BigDecimal("100"), List.of()));

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
    void resolveHistorySort_defaultsToSubmittedAtDesc() {
        Sort sort = studentHistoryService.resolveHistorySort(null);
        assertEquals("submittedAt", sort.iterator().next().getProperty());
        assertEquals(Sort.Direction.DESC, sort.iterator().next().getDirection());
    }

    @Test
    void resolveHistorySort_parsesLabNameAsc() {
        Sort sort = studentHistoryService.resolveHistorySort("labName,asc");
        assertEquals("lab.name", sort.iterator().next().getProperty());
        assertEquals(Sort.Direction.ASC, sort.iterator().next().getDirection());
    }

    @Test
    void getHistory_returnsPagedItemsAndScopeStats() {
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        Lab lab = mock(Lab.class);
        when(lab.getId()).thenReturn(labId);
        when(lab.getName()).thenReturn("Test Lab");

        LabSubmission submission = submissionWithScore(new BigDecimal("85.00"));
        submission.setLab(lab);
        Page<LabSubmission> page = new PageImpl<>(List.of(submission), PageRequest.of(0, 10), 25);

        when(labSubmissionRepository.findHistoryPageByUserId(eq(userId), any(Pageable.class))).thenReturn(page);
        when(labSubmissionRepository.countByUser_Id(userId)).thenReturn(25L);
        when(submissionChallengeResultRepository.findBySubmission_IdInWithChallenge(any())).thenReturn(List.of());
        when(labSubmissionRepository.countDistinctLabsByUserId(userId)).thenReturn(3L);
        when(labSubmissionRepository.averageScoreForUser(userId)).thenReturn(new BigDecimal("82.50"));
        when(labSubmissionRepository.bestScoreForUser(userId)).thenReturn(new BigDecimal("95.00"));

        var response = studentHistoryService.getHistory(userId, null, 0, 10, "submittedAt,desc");

        assertEquals(1, response.submissions().size());
        assertEquals(25, response.totalElements());
        assertEquals(3, response.totalPages());
        assertEquals(25, response.stats().totalSubmissions());
        assertEquals(3, response.stats().labsAttempted());
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
