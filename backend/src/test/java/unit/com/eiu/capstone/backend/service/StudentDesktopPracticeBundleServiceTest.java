package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.service.StudentDesktopPracticeBundleService;
import com.eiu.capstone.backend.service.StudentDesktopPracticeBundleService.BundleDownload;
import com.eiu.capstone.backend.service.StudentTermAccessService;
import com.eiu.capstone.backend.service.TermService;

class StudentDesktopPracticeBundleServiceTest {

    @TempDir
    Path tempDir;

    private TermService termService;
    private StudentTermAccessService studentTermAccessService;
    private Path prebuiltZip;
    private StudentDesktopPracticeBundleService service;
    private UserAccount student;
    private Term currentTerm;

    @BeforeEach
    void setUp() throws IOException {
        termService = mock(TermService.class);
        studentTermAccessService = mock(StudentTermAccessService.class);
        prebuiltZip = tempDir.resolve("OOP-AutoGrader-Practice.zip");
        Files.writeString(prebuiltZip, "zip-bytes");
        service = new StudentDesktopPracticeBundleService(
                termService, studentTermAccessService, prebuiltZip);
        student = new UserAccount();
        currentTerm = new Term();
        currentTerm.setTermNumber(2);
        when(studentTermAccessService.isInCurrentTerm(student)).thenReturn(true);
        when(termService.findCurrentTerm()).thenReturn(Optional.of(currentTerm));
    }

    @Test
    void enrolledStudent_streamsPrebuiltZip() {
        BundleDownload download = service.buildForStudent(student);
        assertEquals("OOP-AutoGrader-Practice-Q2.zip", download.filename());
        assertEquals(prebuiltZip.toAbsolutePath().normalize(), download.zipFile());
    }

    @Test
    void secondCall_samePathNoRebuild() {
        BundleDownload first = service.buildForStudent(student);
        BundleDownload second = service.buildForStudent(student);
        assertEquals(first.zipFile(), second.zipFile());
    }

    @Test
    void missingPrebuiltZip_is503() {
        service = new StudentDesktopPracticeBundleService(
                termService, studentTermAccessService, tempDir.resolve("missing.zip"));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.buildForStudent(student));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    void outOfTermStudent_forbiddenWithoutReadingZip() {
        when(studentTermAccessService.isInCurrentTerm(student)).thenReturn(false);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.buildForStudent(student));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(termService, never()).findCurrentTerm();
    }

    @Test
    void noCurrentTerm_is404() {
        when(termService.findCurrentTerm()).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.buildForStudent(student));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
