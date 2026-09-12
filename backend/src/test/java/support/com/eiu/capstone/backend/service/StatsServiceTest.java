package support.com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.service.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.DTO.StatsDTO;
import com.eiu.capstone.backend.repository.StatsRepository;
import com.eiu.capstone.backend.utility.TimeUtil;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private StatsRepository statsRepository;

    private StatsService statsService;

    @BeforeEach
    void setUp() {
        statsService = new StatsService(statsRepository, false);
    }

    @Test
    void getStatsForLabs_mapsSubmissionCountAndTimestamp() {
        UUID studentId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        OffsetDateTime submittedAt = OffsetDateTime.parse("2026-09-12T15:06:00+07:00");
        when(statsRepository.findStatsByLabIds(studentId, List.of(labId)))
                .thenReturn(Map.of(labId, new StatsRepository.StatsRow(68, BigDecimal.TEN, submittedAt, 68)));

        Map<UUID, StatsDTO> byLab = statsService.getStatsForLabs(studentId, List.of(labId));

        StatsDTO stats = byLab.get(labId);
        assertEquals(68, stats.totalSubmissions());
        assertEquals(TimeUtil.formatLatestSubmission(submittedAt), stats.latestSubmission());
        assertEquals(10, stats.currentGrade());
    }

    @Test
    void getStatsForLabs_emptyLabs_skipsQuery() {
        assertTrue(statsService.getStatsForLabs(UUID.randomUUID(), List.of()).isEmpty());
        verifyNoInteractions(statsRepository);
    }

    @Test
    void getStatsForLabs_missingProgress_omitsLab() {
        UUID studentId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        when(statsRepository.findStatsByLabIds(studentId, List.of(labId))).thenReturn(Map.of());

        Map<UUID, StatsDTO> byLab = statsService.getStatsForLabs(studentId, List.of(labId));

        assertTrue(byLab.isEmpty());
        assertNull(byLab.get(labId));
    }
}
