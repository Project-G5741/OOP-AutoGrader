package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import com.eiu.capstone.backend.model.StudentNotificationDismiss;
import com.eiu.capstone.backend.model.StudentNotificationDismissKey;
import com.eiu.capstone.backend.repository.StudentNotificationDismissRepository;
import com.eiu.capstone.backend.repository.StudentNotificationReadRepository;
import com.eiu.capstone.backend.service.StudentNotificationDismissService;

@ExtendWith(MockitoExtension.class)
class StudentNotificationDismissServiceTest {

    @Mock
    private StudentNotificationDismissRepository dismissRepository;

    @Mock
    private StudentNotificationReadRepository readRepository;

    private StudentNotificationDismissService service;

    @BeforeEach
    void setUp() {
        service = new StudentNotificationDismissService(dismissRepository, readRepository);
    }

    @Test
    void listKeysEmpty() {
        UUID userId = UUID.randomUUID();
        when(dismissRepository.findByIdUserId(userId)).thenReturn(List.of());
        StudentNotificationReadKeysDTO dto = service.listKeys(userId);
        assertEquals(List.of(), dto.keys());
    }

    @Test
    void dismissAfterReadThenIdempotent() {
        UUID userId = UUID.randomUUID();
        String key = "deadline-countdown-" + UUID.randomUUID();
        when(readRepository.existsByIdUserIdAndIdNotifKey(userId, key)).thenReturn(true);
        when(dismissRepository.existsByIdUserIdAndIdNotifKey(userId, key)).thenReturn(false);
        service.dismiss(userId, key);
        ArgumentCaptor<StudentNotificationDismiss> captor = ArgumentCaptor.forClass(StudentNotificationDismiss.class);
        verify(dismissRepository).save(captor.capture());
        assertEquals(userId, captor.getValue().getId().getUserId());
        assertEquals(key, captor.getValue().getId().getNotifKey());

        when(dismissRepository.existsByIdUserIdAndIdNotifKey(userId, key)).thenReturn(true);
        service.dismiss(userId, key);
        verify(dismissRepository).save(any());
    }

    @Test
    void dismissWithoutReadRejected() {
        UUID userId = UUID.randomUUID();
        String key = "no-submit-abc";
        when(readRepository.existsByIdUserIdAndIdNotifKey(userId, key)).thenReturn(false);
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.dismiss(userId, key));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(dismissRepository, never()).save(any());
    }

    @Test
    void listReturnsOnlyStoredKeys() {
        UUID userId = UUID.randomUUID();
        StudentNotificationDismiss row = new StudentNotificationDismiss();
        row.setId(new StudentNotificationDismissKey(userId, "feedback-reply-1-ts"));
        when(dismissRepository.findByIdUserId(userId)).thenReturn(List.of(row));
        assertEquals(List.of("feedback-reply-1-ts"), service.listKeys(userId).keys());
    }

    @Test
    void batchDismissesMultiple() {
        UUID userId = UUID.randomUUID();
        String key1 = "deadline-countdown-a";
        String key2 = "deadline-expired-a";
        when(readRepository.existsByIdUserIdAndIdNotifKey(userId, key1)).thenReturn(true);
        when(readRepository.existsByIdUserIdAndIdNotifKey(userId, key2)).thenReturn(true);
        when(dismissRepository.existsByIdUserIdAndIdNotifKey(userId, key1)).thenReturn(false);
        when(dismissRepository.existsByIdUserIdAndIdNotifKey(userId, key2)).thenReturn(false);
        service.dismissBatch(userId, List.of(key1, key2));
        verify(dismissRepository, times(2)).save(any());
    }

    @Test
    void batchMissingReadFailFast() {
        UUID userId = UUID.randomUUID();
        String key1 = "deadline-countdown-a";
        String key2 = "deadline-expired-a";
        when(readRepository.existsByIdUserIdAndIdNotifKey(userId, key1)).thenReturn(true);
        when(dismissRepository.existsByIdUserIdAndIdNotifKey(userId, key1)).thenReturn(false);
        when(readRepository.existsByIdUserIdAndIdNotifKey(userId, key2)).thenReturn(false);
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.dismissBatch(userId, List.of(key1, key2)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(dismissRepository, times(1)).save(any());
    }

    @Test
    void emptyBatchRejected() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.dismissBatch(UUID.randomUUID(), List.of()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void oversizedBatchRejected() {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < StudentNotificationDismissService.MAX_BATCH_SIZE + 1; i++) {
            keys.add("deadline-countdown-" + i);
        }
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.dismissBatch(UUID.randomUUID(), keys));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(dismissRepository, never()).save(any());
    }

    @Test
    void blankKeyRejected() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.dismiss(UUID.randomUUID(), "  "));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(dismissRepository, never()).save(any());
    }
}
