package unit.com.eiu.capstone.backend.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.eiu.capstone.backend.desktop.DesktopBulkBatchStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

class DesktopBulkBatchStoreTest {

    @TempDir
    Path tempDir;

    private DesktopBulkBatchStore store;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        store = new DesktopBulkBatchStore(tempDir.toString(), mapper);
    }

    @Test
    void saveLoadClear_roundTrip() throws Exception {
        assertFalse(store.hasBatch());
        assertNull(store.load());

        ObjectNode batch = mapper.createObjectNode();
        batch.put("labId", "lab-1");
        batch.put("mode", "EXAM");
        batch.putArray("rows").addObject().put("studentId", "2331200057");

        store.save(batch);
        assertTrue(store.hasBatch());
        assertTrue(Files.isRegularFile(tempDir.resolve("data").resolve("bulk-last-batch.json")));

        var loaded = store.load();
        assertNotNull(loaded);
        assertEquals("lab-1", loaded.get("labId").asText());
        assertEquals(1, loaded.get("rows").size());

        store.clear();
        assertFalse(store.hasBatch());
        assertNull(store.load());
    }

    @Test
    void save_replacesPreviousBatch() {
        ObjectNode first = mapper.createObjectNode().put("labId", "a");
        ObjectNode second = mapper.createObjectNode().put("labId", "b");
        store.save(first);
        store.save(second);
        assertEquals("b", store.load().get("labId").asText());
    }
}
