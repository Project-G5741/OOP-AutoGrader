package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.service.LecturerDesktopBulkBundleService;
import com.eiu.capstone.backend.service.LecturerDesktopBulkBundleService.BundleDownload;

class LecturerDesktopBulkBundleServiceTest {

    @TempDir
    Path tempDir;

    private Path prebuiltZip;
    private LecturerDesktopBulkBundleService service;

    @BeforeEach
    void setUp() throws IOException {
        prebuiltZip = tempDir.resolve("OOP-AutoGrader-LecturerBulk.zip");
        Files.writeString(prebuiltZip, "zip-bytes");
        service = new LecturerDesktopBulkBundleService(prebuiltZip);
    }

    @Test
    void streamsPrebuiltZip() {
        BundleDownload download = service.buildForLecturer();
        assertEquals("OOP-AutoGrader-LecturerBulk.zip", download.filename());
        assertEquals(prebuiltZip.toAbsolutePath().normalize(), download.zipFile());
    }

    @Test
    void secondCall_samePath() {
        BundleDownload first = service.buildForLecturer();
        BundleDownload second = service.buildForLecturer();
        assertEquals(first.zipFile(), second.zipFile());
    }

    @Test
    void missingPrebuiltZip_is503() {
        service = new LecturerDesktopBulkBundleService(tempDir.resolve("missing.zip"));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.buildForLecturer());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }
}
