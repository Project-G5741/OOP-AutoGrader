package support.com.eiu.capstone.backend.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.analytics.cache.LabStatisticsCache;
import com.eiu.capstone.backend.analytics.cache.LecturerOverviewCache;
import com.eiu.capstone.backend.analytics.repository.LecturerAnalyticsRepository;
import com.eiu.capstone.backend.analytics.service.LecturerAnalyticsService;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.plagiarism.PlagiarismService;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.service.ChallengeService;
import com.eiu.capstone.backend.service.TermService;

class LecturerAnalyticsServiceLabStatisticsTest {

    private LecturerAnalyticsRepository lecturerAnalyticsRepository;
    private LabStatisticsCache labStatisticsCache;
    private TermService termService;
    private LabRepository labRepository;
    private PlagiarismService plagiarismService;
    private LecturerAnalyticsService service;

    private final UUID termId = UUID.randomUUID();
    private final UUID labId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lecturerAnalyticsRepository = mock(LecturerAnalyticsRepository.class);
        labStatisticsCache = mock(LabStatisticsCache.class);
        termService = mock(TermService.class);
        labRepository = mock(LabRepository.class);
        plagiarismService = mock(PlagiarismService.class);

        service = new LecturerAnalyticsService(
                lecturerAnalyticsRepository,
                mock(ChallengeService.class),
                mock(LecturerOverviewCache.class),
                labStatisticsCache,
                plagiarismService,
                termService,
                labRepository);

        when(labStatisticsCache.get(eq(labId), any())).thenAnswer(invocation -> {
            LabStatisticsCache.Loader loader = invocation.getArgument(1);
            return loader.load();
        });

        Term current = new Term();
        current.setId(termId);
        Lab lab = new Lab();
        lab.setId(labId);
        lab.setTerm(current);
        when(termService.findCurrentTerm()).thenReturn(Optional.of(current));
        when(labRepository.findByIdWithTerm(labId)).thenReturn(Optional.of(lab));
        when(lecturerAnalyticsRepository.findGradeDistribution(labId)).thenReturn(List.of());
        when(plagiarismService.countFlaggedStudentsForLab(labId)).thenReturn(0L);
    }

    @Test
    void completionRate_isSubmittedDividedByQuarterEnrolled() {
        when(lecturerAnalyticsRepository.findLabStatisticsSummary(labId)).thenReturn(new Object[] {
                labId, "Lab 2", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, 25L
        });
        when(lecturerAnalyticsRepository.countActiveEnrolledStudentsForLab(labId)).thenReturn(20L);
        when(lecturerAnalyticsRepository.countActiveStudentsSubmittedForLab(labId)).thenReturn(10L);

        var stats = service.getLabStatistics(labId);

        assertEquals(20L, stats.studentCount());
        assertEquals(10L, stats.studentsSubmitted());
        assertEquals(new BigDecimal("50.00"), stats.completionRate());
    }
}
