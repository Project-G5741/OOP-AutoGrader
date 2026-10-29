package unit.com.eiu.capstone.backend.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.analytics.service.GradeOverviewOfficialTotal;

class GradeOverviewOfficialTotalTest {

    @Test
    void average_ignoresMidtermAndPractice() {
        BigDecimal total = GradeOverviewOfficialTotal.average(
                Arrays.asList("Official", "Midterm", "Practice"),
                Arrays.asList(new BigDecimal("80"), new BigDecimal("60"), new BigDecimal("100")));
        assertEquals(new BigDecimal("80.00"), total);
    }

    @Test
    void average_ignoresPracticeAndTreatsMissingOfficialAsZero() {
        BigDecimal total = GradeOverviewOfficialTotal.average(
                Arrays.asList("Official", "Official", "Practice"),
                Arrays.asList(new BigDecimal("80"), new BigDecimal("100"), null));
        assertEquals(new BigDecimal("90.00"), total);
    }

    @Test
    void average_ignoresASubmittedPracticeScore() {
        BigDecimal total = GradeOverviewOfficialTotal.average(
                Arrays.asList("Official", "Official", "Practice"),
                Arrays.asList(new BigDecimal("80"), new BigDecimal("100"), new BigDecimal("40")));
        assertEquals(new BigDecimal("90.00"), total);
    }

    @Test
    void average_isNullWhenEveryLabIsPractice() {
        assertNull(GradeOverviewOfficialTotal.average(
                List.of("Practice"),
                List.of(new BigDecimal("100"))));
    }

    @Test
    void average_countsUnsubmittedOfficialAsZero() {
        BigDecimal total = GradeOverviewOfficialTotal.average(
                Arrays.asList("Official", "Official"),
                Arrays.asList(new BigDecimal("80"), null));
        assertEquals(new BigDecimal("40.00"), total);
    }
}
