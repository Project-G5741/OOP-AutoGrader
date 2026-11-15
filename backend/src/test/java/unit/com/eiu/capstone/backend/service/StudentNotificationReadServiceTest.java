package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.StudentNotificationReadKeysDTO;
import com.eiu.capstone.backend.model.StudentNotificationRead;
import com.eiu.capstone.backend.model.StudentNotificationReadKey;
import com.eiu.capstone.backend.repository.StudentNotificationReadRepository;
import com.eiu.capstone.backend.service.StudentNotificationReadService;

@ExtendWith(MockitoExtension.class)
class StudentNotificationReadServiceTest {

    @Mock
    private StudentNotificationReadRepository repository;

    private StudentNotificationReadService service;

    @BeforeEach
    void setUp() {
        service = new StudentNotificationReadService(repository);
    }

    @Test
    void listKeysEmpty() {
        UUID userId = UUID.randomUUID();
        when(repository.findByIdUserId(userId)).thenReturn(List.of());
        StudentNotificationReadKeysDTO dto = service.listKeys(userId);
        assertEquals(List.of(), dto.keys());
    }

    @Test
    void markInsertsThenIdempotent() {
        UUID userId = UUID.randomUUID();
        String key = "deadline-countdown-" + UUID.randomUUID();
        when(repository.existsByIdUserIdAndIdNotifKey(userId, key)).thenReturn(false);
        service.markRead(userId, key);
        ArgumentCaptor<StudentNotificationRead> captor = ArgumentCaptor.forClass(StudentNotificationRead.class);
        verify(repository).save(captor.capture());
        assertEquals(userId, captor.getValue().getId().getUserId());
        assertEquals(key, captor.getValue().getId().getNotifKey());

        when(repository.existsByIdUserIdAndIdNotifKey(userId, key)).thenReturn(true);
        service.markRead(userId, key);
        verify(repository).save(any());
    }

    @Test
    void listReturnsOnlyStoredKeys() {
        UUID userId = UUID.randomUUID();
        StudentNotificationRead row = new StudentNotificationRead();
        row.setId(new StudentNotificationReadKey(userId, "no-submit-abc"));
        when(repository.findByIdUserId(userId)).thenReturn(List.of(row));
        assertEquals(List.of("no-submit-abc"), service.listKeys(userId).keys());
    }

    @Test
    void blankKeyRejected() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.markRead(UUID.randomUUID(), "  "));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void oversizedKeyRejected() {
        String key = "deadline-countdown-" + "x".repeat(200);
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.markRead(UUID.randomUUID(), key));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void unknownPrefixRejected() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.markRead(UUID.randomUUID(), "other-key-1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void whitespaceInsideKeyRejected() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.markRead(UUID.randomUUID(), "deadline-countdown-ab cd"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
