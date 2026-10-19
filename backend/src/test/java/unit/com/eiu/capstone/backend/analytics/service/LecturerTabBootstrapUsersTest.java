package unit.com.eiu.capstone.backend.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.eiu.capstone.backend.analytics.dto.UsersBootstrapResponse;
import com.eiu.capstone.backend.analytics.service.LecturerTabBootstrapService;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.service.UserService;

@ExtendWith(MockitoExtension.class)
class LecturerTabBootstrapUsersTest {

    @Mock
    private UserService userService;

    @Test
    void usersBootstrapReturnsEveryAccountNotATruncatedPage() {
        List<UserAccount> accounts = IntStream.range(0, 51)
                .mapToObj(i -> mock(UserAccount.class))
                .toList();
        when(userService.getAllUser()).thenReturn(accounts);

        LecturerTabBootstrapService service = new LecturerTabBootstrapService(
                null,
                null,
                null,
                userService,
                null,
                null,
                null,
                null,
                null,
                Executors.newSingleThreadExecutor());

        UsersBootstrapResponse response = service.users();

        assertEquals(51, response.content().size());
        assertEquals(51, response.totalElements());
        assertEquals(51, response.size());
        assertEquals(0, response.page());
        assertEquals(1, response.totalPages());
        verify(userService).getAllUser();
        verify(userService, never()).getAllUser(any(Pageable.class));
    }
}
