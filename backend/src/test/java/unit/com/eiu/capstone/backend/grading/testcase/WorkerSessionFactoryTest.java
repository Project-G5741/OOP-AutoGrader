package unit.com.eiu.capstone.backend.grading.testcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.testcase.RemoteWorkerSessionClient;
import com.eiu.capstone.backend.grading.testcase.SandboxInfraErrors;
import com.eiu.capstone.backend.grading.testcase.SerializedInvocationOutcome;
import com.eiu.capstone.backend.grading.testcase.WorkerProcessClient;
import com.eiu.capstone.backend.grading.testcase.WorkerSessionFactory;
import com.eiu.capstone.backend.grading.testcase.WorkerSessionHandle;

class WorkerSessionFactoryTest {

    @Test
    void sandboxEnabledWithoutRootReturnsFailedRemote() {
        WorkerSessionFactory factory = new WorkerSessionFactory(
                true,
                new WorkerProcessClient("java", java.nio.file.Path.of("missing-worker.jar")),
                mock(RemoteWorkerSessionClient.class));
        try (WorkerSessionHandle handle = factory.open(null, 5)) {
            SerializedInvocationOutcome outcome = handle.invoke(
                    "/tmp/classes",
                    new com.eiu.capstone.backend.grading.rubric.InvocationRubric(
                            java.util.UUID.randomUUID(),
                            com.eiu.capstone.backend.model.InvocationKind.METHOD,
                            null,
                            java.util.UUID.randomUUID(),
                            "Foo",
                            "bar",
                            java.util.List.of(),
                            "[]",
                            null,
                            null,
                            java.util.List.of(),
                            null),
                    java.util.List.of());
            assertEquals(SerializedInvocationOutcome.KIND_ERROR, outcome.kind());
            assertEquals(SandboxInfraErrors.STUDENT_MESSAGE, outcome.errorMessage());
        }
    }

    @Test
    void sandboxDisabledUsesLocalWorker() {
        WorkerSessionFactory factory = new WorkerSessionFactory(
                false,
                new WorkerProcessClient("java", java.nio.file.Path.of("missing-worker.jar")),
                mock(RemoteWorkerSessionClient.class));
        assertFalse(factory.isSandboxEnabled());
        try (WorkerSessionHandle handle = factory.open(Path.of("."), 5)) {
            assertTrue(handle.spawnMs() >= 0);
        }
    }
}
