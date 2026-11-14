package unit.com.eiu.capstone.backend.grading.testcase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.grading.testcase.LocalWorkerInfraErrors;
import com.eiu.capstone.backend.grading.testcase.SerializedInvocationOutcome;
import com.eiu.capstone.backend.grading.testcase.WorkerSessionHandle;
import com.eiu.capstone.backend.model.InvocationKind;

class WorkerSessionHandleLocalInfraTest {

    @Test
    void failedLocalWithBlankSpawnMessageUsesLocalWorkerInfraText() {
        WorkerSessionHandle handle = WorkerSessionHandle.failedLocal(null, 1);
        SerializedInvocationOutcome outcome = handle.invoke(
                "/tmp/classes",
                sampleInvocation(),
                List.of());
        assertEquals(SerializedInvocationOutcome.KIND_ERROR, outcome.kind());
        assertEquals(LocalWorkerInfraErrors.STUDENT_MESSAGE, outcome.errorMessage());
    }

    @Test
    void failedLocalBatchUsesLocalWorkerInfraText() {
        WorkerSessionHandle handle = WorkerSessionHandle.failedLocal(null, 1);
        SerializedInvocationOutcome outcome = handle.batch(
                "/tmp/classes",
                List.of(new com.eiu.capstone.backend.grading.testcase.worker.WorkerIpc.BatchItemSpec(
                        List.of(),
                        List.of(),
                        "/tmp/classes")));
        assertEquals(SerializedInvocationOutcome.KIND_ERROR, outcome.kind());
        assertEquals(LocalWorkerInfraErrors.STUDENT_MESSAGE, outcome.errorMessage());
    }

    @Test
    void failedLocalBlankSpawnMessageUsesLocalWorkerInfraText() {
        WorkerSessionHandle handle = WorkerSessionHandle.failedLocal("  ", 1);
        SerializedInvocationOutcome outcome = handle.invoke(
                "/tmp/classes",
                sampleInvocation(),
                List.of());
        assertEquals(SerializedInvocationOutcome.KIND_ERROR, outcome.kind());
        assertEquals(LocalWorkerInfraErrors.STUDENT_MESSAGE, outcome.errorMessage());
    }

    @Test
    void failedLocalPreservesExplicitSpawnMessage() {
        WorkerSessionHandle handle = WorkerSessionHandle.failedLocal("Missing worker jar: x", 1);
        SerializedInvocationOutcome outcome = handle.invoke(
                "/tmp/classes",
                sampleInvocation(),
                List.of());
        assertEquals(SerializedInvocationOutcome.KIND_ERROR, outcome.kind());
        assertEquals("Missing worker jar: x", outcome.errorMessage());
    }

    private static InvocationRubric sampleInvocation() {
        return new InvocationRubric(
                UUID.randomUUID(),
                InvocationKind.METHOD,
                null,
                UUID.randomUUID(),
                "Foo",
                "bar",
                List.of(),
                "[]",
                null,
                null,
                List.of(),
                null);
    }
}
