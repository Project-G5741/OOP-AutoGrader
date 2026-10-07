package unit.com.eiu.capstone.backend.grading.testcase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.testcase.SharedLocalWorkerCache;
import com.eiu.capstone.backend.grading.testcase.WorkerSessionFactory;
import com.eiu.capstone.backend.grading.testcase.WorkerSessionHandle;

class SharedLocalWorkerCacheTest {

    private SharedLocalWorkerCache cache;

    @AfterEach
    void tearDown() {
        if (cache != null) {
            cache.shutdown();
        }
    }

    @Test
    void secondBorrowWithinTtlReusesSameHandleWithoutSpawn() {
        cache = new SharedLocalWorkerCache();
        WorkerSessionFactory factory = mock(WorkerSessionFactory.class);
        WorkerSessionHandle handle = mock(WorkerSessionHandle.class);
        when(factory.isSandboxEnabled()).thenReturn(false);
        when(factory.open(any(Path.class), anyInt())).thenReturn(handle);
        when(handle.isAlive()).thenReturn(true);

        Path root = Path.of("tmp", "sub");
        WorkerSessionHandle first = cache.borrow(factory, root, 5);
        cache.release(factory, first);
        assertTrue(cache.hasIdleLocal());

        WorkerSessionHandle second = cache.borrow(factory, root, 5);
        assertSame(handle, second);
        verify(factory, times(1)).open(root, 5);
        verify(handle, never()).close();
    }

    @Test
    void afterIdleExpiryNextBorrowSpawnsCold() throws Exception {
        cache = new SharedLocalWorkerCache(30L);
        WorkerSessionFactory factory = mock(WorkerSessionFactory.class);
        WorkerSessionHandle firstHandle = mock(WorkerSessionHandle.class);
        WorkerSessionHandle secondHandle = mock(WorkerSessionHandle.class);
        when(factory.isSandboxEnabled()).thenReturn(false);
        when(factory.open(any(Path.class), anyInt())).thenReturn(firstHandle, secondHandle);
        when(firstHandle.isAlive()).thenReturn(true);
        when(secondHandle.isAlive()).thenReturn(true);

        Path root = Path.of("tmp", "sub");
        cache.release(factory, cache.borrow(factory, root, 5));
        Thread.sleep(50L);
        assertFalse(cache.hasIdleLocal());

        WorkerSessionHandle next = cache.borrow(factory, root, 5);
        assertSame(secondHandle, next);
        verify(factory, times(2)).open(root, 5);
        verify(firstHandle).close();
    }

    @Test
    void sandboxEnabledNeverCaches() {
        cache = new SharedLocalWorkerCache();
        WorkerSessionFactory factory = mock(WorkerSessionFactory.class);
        WorkerSessionHandle first = mock(WorkerSessionHandle.class);
        WorkerSessionHandle second = mock(WorkerSessionHandle.class);
        when(factory.isSandboxEnabled()).thenReturn(true);
        when(factory.open(any(Path.class), anyInt())).thenReturn(first, second);

        Path root = Path.of("tmp", "sub");
        WorkerSessionHandle borrowed = cache.borrow(factory, root, 5);
        cache.release(factory, borrowed);
        assertFalse(cache.hasIdleLocal());
        verify(first).close();

        WorkerSessionHandle next = cache.borrow(factory, root, 5);
        assertSame(second, next);
        verify(factory, times(2)).open(root, 5);
    }

    @Test
    void deadHandleIsClosedAndNotParked() {
        cache = new SharedLocalWorkerCache();
        WorkerSessionFactory factory = mock(WorkerSessionFactory.class);
        WorkerSessionHandle handle = mock(WorkerSessionHandle.class);
        when(factory.isSandboxEnabled()).thenReturn(false);
        when(handle.isAlive()).thenReturn(false);

        cache.release(factory, handle);
        assertFalse(cache.hasIdleLocal());
        verify(handle).close();
    }
}
