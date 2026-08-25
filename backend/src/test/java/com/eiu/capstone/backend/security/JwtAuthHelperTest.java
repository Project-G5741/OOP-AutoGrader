package com.eiu.capstone.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class JwtAuthHelperTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    private JwtAuthHelper jwtAuthHelper;

    @BeforeEach
    void setUp() {
        jwtAuthHelper = new JwtAuthHelper(userAccountRepository);
    }

    @Test
    void isStudentOnly_studentWithoutLecturer_true() {
        JwtUserPrincipal principal = new JwtUserPrincipal("s@eiu.edu.vn", "1", List.of("STUDENT"));
        assertTrue(principal.isStudentOnly());
    }

    @Test
    void isStudentOnly_dualRole_false() {
        JwtUserPrincipal principal = new JwtUserPrincipal("s@eiu.edu.vn", "1", List.of("STUDENT", "LECTURER"));
        assertFalse(principal.isStudentOnly());
    }

    @Test
    void hasRole_teacherAlias_isLecturer() {
        JwtUserPrincipal principal = new JwtUserPrincipal("t@eiu.edu.vn", "T1", List.of("TEACHER"));
        assertTrue(principal.hasRole(JwtRoleNames.LECTURER));
        assertFalse(principal.isStudentOnly());
    }

    @Test
    void resolveStudentScope_lecturerWithoutStudentId_returnsNull() {
        JwtUserPrincipal principal = new JwtUserPrincipal("lecturer@eiu.edu.vn", "T1", List.of("LECTURER"));
        UserAccount user = activeUser(UUID.randomUUID());
        when(userAccountRepository.findByEmail("lecturer@eiu.edu.vn")).thenReturn(Optional.of(user));

        UUID scoped = jwtAuthHelper.resolveStudentScope(principal, null);

        assertEquals(null, scoped);
    }

    @Test
    void resolveStudentScope_studentDefaultsToSelf() {
        UUID userId = UUID.randomUUID();
        UserAccount user = activeUser(userId);
        JwtUserPrincipal principal = new JwtUserPrincipal("student@eiu.edu.vn", "S1", List.of("STUDENT"));
        when(userAccountRepository.findByEmail("student@eiu.edu.vn")).thenReturn(Optional.of(user));

        UUID scoped = jwtAuthHelper.resolveStudentScope(principal, null);

        assertEquals(userId, scoped);
    }

    @Test
    void resolveStudentScope_studentCannotReadOther() {
        UUID userId = UUID.randomUUID();
        UserAccount user = activeUser(userId);
        JwtUserPrincipal principal = new JwtUserPrincipal("student@eiu.edu.vn", "S1", List.of("STUDENT"));
        when(userAccountRepository.findByEmail("student@eiu.edu.vn")).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> jwtAuthHelper.resolveStudentScope(principal, UUID.randomUUID()));
        assertEquals(403, ex.getStatusCode().value());
    }

    private static UserAccount activeUser(UUID id) {
        UserAccount user = new UserAccount();
        user.setEmail("user@eiu.edu.vn");
        user.setFullName("Test User");
        user.setPasswordHash("hash");
        user.setIsActive(true);
        try {
            Field idField = UserAccount.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return user;
    }
}
