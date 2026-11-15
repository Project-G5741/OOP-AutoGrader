package unit.com.eiu.capstone.backend.grading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.eiu.capstone.backend.grading.ParsedClass;
import com.eiu.capstone.backend.grading.ReflectionClassParser;
import com.eiu.capstone.backend.grading.testcase.SerializedInvocationOutcome;
import com.eiu.capstone.backend.grading.testcase.WorkerProcessClient;
import com.eiu.capstone.backend.grading.testcase.WorkerSessionHandle;

class ReflectionClassParserLeasedSessionTest {

    @Test
    void failedLeaseFallsBackToInProcessParse(@TempDir Path tempDir) throws Exception {
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(classesDir);
        Path source = tempDir.resolve("Box.java");
        Files.writeString(source, "public class Box {}\n");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertEquals(0, compiler.run(null, null, null, "-d", classesDir.toString(), source.toString()));

        ReflectionClassParser parser = new ReflectionClassParser(null);
        List<ParsedClass> parsed = parser.parseClasses(
                classesDir, WorkerSessionHandle.failedRemote("sandbox down", 0));

        assertEquals(1, parsed.size());
        assertEquals("Box", parsed.get(0).simpleName);
    }

    @Test
    void leasedSessionReflectDoesNotAcquireWorkerSlot(@TempDir Path tempDir) throws Exception {
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(classesDir);

        Semaphore slot = mock(Semaphore.class);
        WorkerProcessClient client = mock(WorkerProcessClient.class);
        WorkerSessionHandle leased = mock(WorkerSessionHandle.class);
        when(leased.reflect(anyString())).thenReturn(new SerializedInvocationOutcome(
                SerializedInvocationOutcome.KIND_NORMAL,
                "[{\"simpleName\":\"Box\",\"isAbstract\":false,\"isStatic\":false,\"fields\":[],\"methods\":[],\"constructors\":[],\"interfaceSimpleNames\":[]}]",
                "",
                false,
                java.util.Map.of(),
                null,
                List.of(),
                null,
                null));

        ReflectionClassParser parser = new ReflectionClassParser(client, slot);
        List<ParsedClass> parsed = parser.parseClasses(classesDir, leased);

        assertEquals(1, parsed.size());
        assertEquals("Box", parsed.get(0).simpleName);
        verify(slot, never()).acquire();
        verify(slot, never()).release();
        verify(client, never()).productionCommand(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void nullLeaseFallsBackWithoutLeasedReflect(@TempDir Path tempDir) throws Exception {
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(classesDir);
        Files.writeString(classesDir.resolve("Empty.class"), "not-a-real-class");

        Semaphore slot = mock(Semaphore.class);
        WorkerProcessClient client = mock(WorkerProcessClient.class);
        when(client.productionCommand(org.mockito.ArgumentMatchers.any())).thenThrow(new IllegalStateException("no jar"));

        ReflectionClassParser parser = new ReflectionClassParser(client, slot);
        List<ParsedClass> parsed = parser.parseClasses(classesDir, null);

        assertTrue(parsed.isEmpty() || parsed.stream().noneMatch(pc -> "Box".equals(pc.simpleName)));
        verify(slot, never()).acquire();
    }
}
