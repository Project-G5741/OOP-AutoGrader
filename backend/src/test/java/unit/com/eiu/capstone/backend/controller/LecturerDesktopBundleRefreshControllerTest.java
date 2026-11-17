package unit.com.eiu.capstone.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.controller.LecturerDesktopBundleRefreshController;
import com.eiu.capstone.backend.desktop.bundle.DesktopBundlePrefetchService;
import com.eiu.capstone.backend.desktop.bundle.DesktopBundlePrefetchService.DesktopBundleRefreshReport;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtRoleNames;
import com.eiu.capstone.backend.security.JwtUserPrincipal;

@ExtendWith(MockitoExtension.class)
class LecturerDesktopBundleRefreshControllerTest {

    @Mock
    private JwtAuthHelper jwtAuthHelper;

    @Mock
    private DesktopBundlePrefetchService prefetchService;

    @InjectMocks
    private LecturerDesktopBundleRefreshController controller;

    @Test
    void refresh_forbiddenForLecturerWithoutAdmin() {
        JwtUserPrincipal principal = new JwtUserPrincipal("t@eiu.edu.vn", "IRN", List.of(JwtRoleNames.LECTURER));
        when(jwtAuthHelper.requireActiveUser(principal)).thenReturn(new UserAccount());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> controller.refreshDesktopBundles(principal));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void refresh_okForAdmin() {
        JwtUserPrincipal principal = new JwtUserPrincipal("a@eiu.edu.vn", "IRN", List.of(JwtRoleNames.ADMIN));
        when(jwtAuthHelper.requireActiveUser(principal)).thenReturn(new UserAccount());
        var report = new DesktopBundleRefreshReport(List.of());
        when(prefetchService.refreshAll()).thenReturn(report);

        assertEquals(report, controller.refreshDesktopBundles(principal));
    }
}