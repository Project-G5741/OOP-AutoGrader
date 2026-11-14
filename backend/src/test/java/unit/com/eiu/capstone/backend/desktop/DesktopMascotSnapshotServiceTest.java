package unit.com.eiu.capstone.backend.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.MascotSnapshotView;
import com.eiu.capstone.backend.desktop.DesktopMascotSnapshotService;
import com.fasterxml.jackson.databind.ObjectMapper;

class DesktopMascotSnapshotServiceTest {

    @TempDir
    Path installDir;

    @Test
    void read_missingFile_returnsFox() {
        DesktopMascotSnapshotService service = new DesktopMascotSnapshotService(
                installDir.toString(), "", new ObjectMapper());
        MascotSnapshotView view = service.read();
        assertEquals("fox", view.equipped());
        assertEquals(List.of(), view.owned());
    }

    @Test
    void read_persistedSnapshot_returnsEquipped() throws Exception {
        Path file = installDir.resolve("data").resolve("mascot-snapshot.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{\"equipped\":\"ki_lan\",\"owned\":[\"fox\",\"ki_lan\"]}");
        DesktopMascotSnapshotService service = new DesktopMascotSnapshotService(
                installDir.toString(), "https://example.test", new ObjectMapper());
        MascotSnapshotView view = service.read();
        assertEquals("ki_lan", view.equipped());
        assertEquals(List.of("fox", "ki_lan"), view.owned());
    }

    @Test
    void sync_blankToken_is400() {
        DesktopMascotSnapshotService service = new DesktopMascotSnapshotService(
                installDir.toString(), "https://example.test", new ObjectMapper());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.sync("  "));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void sync_missingCloudUrl_is503() {
        DesktopMascotSnapshotService service = new DesktopMascotSnapshotService(
                installDir.toString(), "", new ObjectMapper());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.sync("token"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    void resolveCloudApiUrl_readsFileEachCall_overStartupValue() throws Exception {
        DesktopMascotSnapshotService service = new DesktopMascotSnapshotService(
                installDir.toString(), "https://baked.example", new ObjectMapper());
        assertEquals("https://baked.example", service.resolveCloudApiUrl());

        Files.writeString(installDir.resolve("cloud-api-url.txt"), "  https://oop-autograder.onrender.com/ \n");
        assertEquals("https://oop-autograder.onrender.com", service.resolveCloudApiUrl());

        Files.writeString(installDir.resolve("cloud-api-url.txt"), "https://oop-autograder-production.up.railway.app");
        assertEquals("https://oop-autograder-production.up.railway.app", service.resolveCloudApiUrl());
    }

    @Test
    void resolveCloudApiUrl_blankFile_fallsBackToStartupValue() throws Exception {
        Files.writeString(installDir.resolve("cloud-api-url.txt"), "   \n");
        DesktopMascotSnapshotService service = new DesktopMascotSnapshotService(
                installDir.toString(), "https://baked.example", new ObjectMapper());
        assertEquals("https://baked.example", service.resolveCloudApiUrl());
    }
}
