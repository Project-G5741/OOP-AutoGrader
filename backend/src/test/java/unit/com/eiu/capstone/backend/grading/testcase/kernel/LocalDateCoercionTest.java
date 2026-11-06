package unit.com.eiu.capstone.backend.grading.testcase.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.testcase.kernel.JavaTypeResolver;
import com.eiu.capstone.backend.grading.testcase.kernel.JsonValueCoercer;
import com.eiu.capstone.backend.grading.testcase.kernel.ValueComparator;
import com.eiu.capstone.backend.model.ComparisonMode;

class LocalDateCoercionTest {

    private final JsonValueCoercer coercer = new JsonValueCoercer();

    @Test
    void isoDateBecomesLocalDate() {
        Object[] args = coercer.coerceParams("[\"2024-06-13\"]", List.of("LocalDate"));

        assertEquals(LocalDate.of(2024, 6, 13), args[0]);
        assertEquals(LocalDate.class, JavaTypeResolver.resolve("java.time.LocalDate"));
    }

    @Test
    void nullDateStaysNull() {
        Object[] args = coercer.coerceParams("[null]", List.of("LocalDate"));

        assertEquals(1, args.length);
        assertEquals(null, args[0]);
    }

    @Test
    void rejectsNonIsoDate() {
        assertThrows(IllegalArgumentException.class,
                () -> coercer.coerceParams("[\"13/06/2024\"]", List.of("LocalDate")));
    }

    @Test
    void fieldSnapshotStringMatchesParsedExpected() {
        Object expected = coercer.coerceExpectedValue("\"2024-06-13\"", "LocalDate");

        assertTrue(ValueComparator.matches("2024-06-13", expected, ComparisonMode.EXACT));
        assertEquals("\"2024-06-13\"", coercer.toJson(LocalDate.of(2024, 6, 13)));
    }
}
