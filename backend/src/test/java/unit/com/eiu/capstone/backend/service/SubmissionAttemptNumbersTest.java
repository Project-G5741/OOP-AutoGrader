package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.service.SubmissionAttemptNumbers;

class SubmissionAttemptNumbersTest {

    @Test
    void firstUploadIsAttemptOne() {
        assertEquals(1, SubmissionAttemptNumbers.next(null));
        assertEquals(1, SubmissionAttemptNumbers.next(0));
    }

    @Test
    void nextIsMaxPlusOne() {
        assertEquals(2, SubmissionAttemptNumbers.next(1));
        assertEquals(11, SubmissionAttemptNumbers.next(10));
    }
}
