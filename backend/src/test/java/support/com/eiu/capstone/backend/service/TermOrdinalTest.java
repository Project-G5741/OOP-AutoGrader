package support.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.model.AcademicYear;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.service.TermOrdinal;

class TermOrdinalTest {

    @Test
    void fromTerm_q1ToQ4SameYear_isThreeQuartersApart() {
        Term q1 = term("2025-2026", 1);
        Term q4 = term("2025-2026", 4);
        assertEquals(TermOrdinal.fromTerm(q4), TermOrdinal.fromTerm(q1) + 3);
    }

    @Test
    void fromTerm_q3ToQ2NextYear_isThreeQuartersApart() {
        Term q3 = term("2025-2026", 3);
        Term q2 = term("2026-2027", 2);
        assertEquals(TermOrdinal.fromTerm(q2), TermOrdinal.fromTerm(q3) + 3);
    }

    @Test
    void fromTerm_blankYearLabel_throws() {
        Term term = new Term();
        term.setAcademicYear(new AcademicYear());
        term.setTermNumber(1);
        assertThrows(IllegalArgumentException.class, () -> TermOrdinal.fromTerm(term));
    }

    private static Term term(String yearLabel, int termNumber) {
        AcademicYear year = new AcademicYear();
        year.setYearLabel(yearLabel);
        Term term = new Term();
        term.setAcademicYear(year);
        term.setTermNumber(termNumber);
        return term;
    }
}
