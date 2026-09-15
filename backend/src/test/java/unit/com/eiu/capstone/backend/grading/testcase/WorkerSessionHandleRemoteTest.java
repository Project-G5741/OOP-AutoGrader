package unit.com.eiu.capstone.backend.grading.testcase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.grading.testcase.SandboxInfraErrors;
import com.eiu.capstone.backend.grading.testcase.SerializedInvocationOutcome;
import com.eiu.capstone.backend.grading.testcase.WorkerSessionHandle;
import com.eiu.capstone.backend.model.InvocationKind;

class WorkerSessionHandleRemoteTest {

    @Test
    void failedRemoteReturnsGenericInfraErrorOnInvoke() {
        WorkerSessionHandle handle = WorkerSessionHandle.failedRemote(
                SandboxInfraErrors.STUDENT_MESSAGE, 1);
        SerializedInvocationOutcome outcome = handle.invoke(
                "/tmp/classes",
                new InvocationRubric(
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
                        null),
                List.of());
        assertEquals(SerializedInvocationOutcome.KIND_ERROR, outcome.kind());
        assertEquals(SandboxInfraErrors.STUDENT_MESSAGE, outcome.errorMessage());
    }
}
