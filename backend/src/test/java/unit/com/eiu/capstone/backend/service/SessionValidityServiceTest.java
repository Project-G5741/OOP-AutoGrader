package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.service.SessionValidityService;

@ExtendWith(MockitoExtension.class)
class SessionValidityServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    private SessionValidityService service;

    @BeforeEach
    void setUp() {
        service = new SessionValidityService(userAccountRepository);
    }

    @Test
    void matchingVersion_isValid() {
        when(userAccountRepository.findSessionVersionByEmailIgnoreCase("a@eiu.edu.vn"))
                .thenReturn(Optional.of(2));
        assertTrue(service.isSessionValid("a@eiu.edu.vn", 2));
    }

    @Test
    void staleVersion_isInvalid() {
        when(userAccountRepository.findSessionVersionByEmailIgnoreCase("a@eiu.edu.vn"))
                .thenReturn(Optional.of(2));
        assertFalse(service.isSessionValid("a@eiu.edu.vn", 1));
    }

    @Test
    void missingUser_isInvalid() {
        when(userAccountRepository.findSessionVersionByEmailIgnoreCase("gone@eiu.edu.vn"))
                .thenReturn(Optional.empty());
        assertFalse(service.isSessionValid("gone@eiu.edu.vn", 0));
    }

    @Test
    void nullClaimSv_treatedAsZero() {
        when(userAccountRepository.findSessionVersionByEmailIgnoreCase("a@eiu.edu.vn"))
                .thenReturn(Optional.of(0));
        assertTrue(service.isSessionValid("a@eiu.edu.vn", null));
    }

    @Test
    void bumpSessionVersion_incrementsAndCaches() {
        UserAccount user = new UserAccount();
        user.setEmail("a@eiu.edu.vn");
        user.setSessionVersion(3);
        when(userAccountRepository.save(user)).thenReturn(user);

        service.bumpSessionVersion(user);

        assertEquals(4, user.getSessionVersion());
        verify(userAccountRepository).save(user);
        assertTrue(service.isSessionValid("a@eiu.edu.vn", 4));
    }
}
