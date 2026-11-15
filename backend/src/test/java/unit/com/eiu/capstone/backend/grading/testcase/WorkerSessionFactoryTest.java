package unit.com.eiu.capstone.backend.grading.testcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.grading.testcase.RemoteWorkerSessionClient;
import com.eiu.capstone.backend.grading.testcase.SandboxInfraErrors;
import com.eiu.capstone.backend.grading.testcase.SerializedInvocationOutcome;
import com.eiu.capstone.backend.grading.testcase.WorkerProcessClient;
import com.eiu.capstone.backend.grading.testcase.WorkerSessionFactory;
import com.eiu.capstone.backend.grading.testcase.WorkerSessionHandle;
import com.eiu.capstone.backend.grading.testcase.WorkerSpawnException;
import com.eiu.capstone.backend.model.InvocationKind;

class WorkerSessionFactoryTest {

    @Test
    void sandboxEnabledNeverSpawnsLocalWorker() {
        WorkerProcessClient local = mock(WorkerProcessClient.class);
        RemoteWorkerSessionClient remote = mock(RemoteWorkerSessionClient.class);
        WorkerSessionHandle failed = WorkerSessionHandle.failedRemote(
                SandboxInfraErrors.STUDENT_MESSAGE, 0);
        when(remote.open(any(), anyInt())).thenReturn(failed);

        WorkerSessionFactory factory = new WorkerSessionFactory(true, local, remote);
        Path root = Path.of("/tmp/classes");
        WorkerSessionHandle handle = factory.open(root, 3);

        assertTrue(factory.isSandboxEnabled());
        SerializedInvocationOutcome outcome = handle.invoke(
                "/tmp/classes", sampleMethod(), List.of());
        assertEquals(SerializedInvocationOutcome.KIND_ERROR, outcome.kind());
        assertEquals(SandboxInfraErrors.STUDENT_MESSAGE, outcome.errorMessage());
        verify(remote).open(root, 3);
        verify(local, never()).start();
        verify(local, never()).startCommand(any());
    }

    @Test
    void sandboxDisabledDoesNotCallRemote() {
        WorkerProcessClient local = mock(WorkerProcessClient.class);
        RemoteWorkerSessionClient remote = mock(RemoteWorkerSessionClient.class);
        when(local.start()).thenThrow(new WorkerSpawnException("no jar"));

        WorkerSessionFactory factory = new WorkerSessionFactory(false, local, remote);

        assertFalse(factory.isSandboxEnabled());
        factory.open(Path.of("/tmp/classes"), 3);
        verify(local).start();
        verify(remote, never()).open(any(), anyInt());
    }

    @Test
    void sandboxEnabledNullRootReturnsFailedRemoteWithoutLocal() {
        WorkerProcessClient local = mock(WorkerProcessClient.class);
        RemoteWorkerSessionClient remote = mock(RemoteWorkerSessionClient.class);
        WorkerSessionFactory factory = new WorkerSessionFactory(true, local, remote);

        WorkerSessionHandle handle = factory.open(null, 3);

        SerializedInvocationOutcome outcome = handle.invoke(
                "/tmp/classes", sampleMethod(), List.of());
        assertEquals(SandboxInfraErrors.STUDENT_MESSAGE, outcome.errorMessage());
        verify(remote, never()).open(any(), anyInt());
        verify(local, never()).start();
    }

    private static InvocationRubric sampleMethod() {
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
