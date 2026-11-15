package unit.com.eiu.capstone.backend.grading.testcase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.testcase.WorkerSessionHandle;

class WorkerSessionTransportWaitTest {

    @Test
    void serialPoolMatchesItemCountTimesTimeoutPlusSlack() {
        assertEquals(6 * 3 + 30, WorkerSessionHandle.transportWaitSeconds(6, 3, 1));
    }

    @Test
    void parallelPoolUsesCeilSlices() {
        // ceil(6/2)=3 slices × 3s + 30
        assertEquals(3 * 3 + 30, WorkerSessionHandle.transportWaitSeconds(6, 3, 2));
        // ceil(5/2)=3
        assertEquals(3 * 3 + 30, WorkerSessionHandle.transportWaitSeconds(5, 3, 2));
    }
}
