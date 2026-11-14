package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.DTO.MascotPracticeTokenView;
import com.eiu.capstone.backend.model.StudentMascotPracticeToken;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.StudentMascotPracticeTokenRepository;
import com.eiu.capstone.backend.service.JwtService;
import com.eiu.capstone.backend.service.MascotPracticeTokenService;

@ExtendWith(MockitoExtension.class)
class MascotPracticeTokenServiceTest {

    @Mock
    private StudentMascotPracticeTokenRepository tokens;

    @Mock
    private JwtService jwtService;

    private MascotPracticeTokenService service;
    private UserAccount user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new MascotPracticeTokenService(tokens, jwtService);
        userId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        user = mock(UserAccount.class);
        lenient().when(user.getId()).thenReturn(userId);
        lenient().when(user.getEmail()).thenReturn("student@eiu.edu.vn");
    }

    @Test
    void issue_returnsLiveRowWithoutReminting() {
        StudentMascotPracticeToken row = liveRow("compact-live", Instant.now().plusSeconds(3600));
        when(tokens.findById(userId)).thenReturn(Optional.of(row));

        MascotPracticeTokenView view = service.issue(user, false);

        assertEquals("compact-live", view.token());
        verify(jwtService, never()).createMascotPracticeToken(any(), any(), any(), any());
    }

    @Test
    void issue_remintsWhenExpired() {
        StudentMascotPracticeToken row = liveRow("old", Instant.now().minusSeconds(10));
        when(tokens.findById(userId)).thenReturn(Optional.of(row));
        when(jwtService.createMascotPracticeToken(any(), any(), any(), any())).thenReturn("fresh");
        when(tokens.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MascotPracticeTokenView view = service.issue(user, false);

        assertEquals("fresh", view.token());
        ArgumentCaptor<StudentMascotPracticeToken> captor =
                ArgumentCaptor.forClass(StudentMascotPracticeToken.class);
        verify(tokens).save(captor.capture());
        assertNotEquals("old", captor.getValue().getCompactToken());
    }

    @Test
    void regenerate_alwaysMintsANewToken() {
        StudentMascotPracticeToken row = liveRow("compact-live", Instant.now().plusSeconds(3600));
        when(tokens.findById(userId)).thenReturn(Optional.of(row));
        when(jwtService.createMascotPracticeToken(any(), any(), any(), any())).thenReturn("replacement");
        when(tokens.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MascotPracticeTokenView view = service.issue(user, true);

        assertEquals("replacement", view.token());
        verify(jwtService).createMascotPracticeToken(any(), any(), any(), any());
    }

    @Test
    void isLiveJti_rejectsExpiredAndUnknown() {
        StudentMascotPracticeToken live = liveRow("x", Instant.now().plusSeconds(60));
        StudentMascotPracticeToken expired = liveRow("y", Instant.now().minusSeconds(1));
        when(tokens.findByJti("live")).thenReturn(Optional.of(live));
        when(tokens.findByJti("expired")).thenReturn(Optional.of(expired));
        when(tokens.findByJti("missing")).thenReturn(Optional.empty());

        assertTrue(service.isLiveJti("live"));
        assertFalse(service.isLiveJti("expired"));
        assertFalse(service.isLiveJti("missing"));
        assertFalse(service.isLiveJti(" "));
    }

    private StudentMascotPracticeToken liveRow(String compact, Instant expiresAt) {
        StudentMascotPracticeToken row = new StudentMascotPracticeToken();
        row.setUserId(userId);
        row.setJti("jti");
        row.setCompactToken(compact);
        row.setExpiresAt(expiresAt);
        row.setCreatedAt(Instant.now());
        return row;
    }
}
