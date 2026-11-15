package unit.com.eiu.capstone.backend.grading.testcase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.testcase.WorkerProcessClient;
import com.eiu.capstone.backend.grading.testcase.WorkerSessionFactory;
import com.eiu.capstone.backend.grading.testcase.WorkerSpawnException;

class WorkerSessionFactoryTest {

    @Test
    void openAlwaysStartsTheLocalWorker() {
        WorkerProcessClient local = mock(WorkerProcessClient.class);
        org.mockito.Mockito.when(local.start()).thenThrow(new WorkerSpawnException("no jar"));
        WorkerSessionFactory factory = new WorkerSessionFactory(local);

        assertFalse(factory.isSandboxEnabled());
        factory.open(Path.of("/tmp/classes"), 3);

        verify(local).start();
    }
}
