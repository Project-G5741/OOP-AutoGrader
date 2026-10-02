package authorization.com.eiu.capstone.backend.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.config.SecurityConfig;
import com.eiu.capstone.backend.controller.LecturerTermController;
import com.eiu.capstone.backend.desktop.pack.DesktopPackExportService;
import com.eiu.capstone.backend.security.JwtAuthenticationFilter;
import com.eiu.capstone.backend.security.JwtRoleNames;
import com.eiu.capstone.backend.service.JwtService;
import com.eiu.capstone.backend.service.SessionValidityService;
import com.eiu.capstone.backend.service.TermService;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;

@WebMvcTest(controllers = LecturerTermController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        LecturerTermController.class
})
@TestPropertySource(properties = {
        "jwt.secret=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "jwt.validity-seconds=3600",
        "spring.web.resources.add-mappings=false"
})
class LecturerTermDesktopPackTest {

    @SpringBootApplication(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    static class DesktopPackSliceApp {
    }

    private static final UUID TERM_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private TermService termService;

    @MockBean
    private DesktopPackExportService desktopPackExportService;

    @MockBean
    private SessionValidityService sessionValidityService;

    @BeforeEach
    void allowSessions() {
        when(sessionValidityService.isSessionValid(any(), anyInt())).thenReturn(true);
        when(sessionValidityService.isSessionValid(any(), isNull())).thenReturn(true);
    }

    @Test
    void lecturerDownload_returns200() throws Exception {
        when(desktopPackExportService.exportTermPack(TERM_ID)).thenReturn(new byte[] { 1, 2, 3 });
        mockMvc.perform(get("/api/lecturer/terms/" + TERM_ID + "/desktop-pack")
                        .header("Authorization", bearer(List.of(JwtRoleNames.LECTURER))))
                .andExpect(status().isOk());
    }

    @Test
    void studentDownload_is403() throws Exception {
        mockMvc.perform(get("/api/lecturer/terms/" + TERM_ID + "/desktop-pack")
                        .header("Authorization", bearer(List.of(JwtRoleNames.STUDENT))))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingSigningKey_returns503() throws Exception {
        when(desktopPackExportService.exportTermPack(TERM_ID))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Desktop practice pack export is not configured on this server"));
        mockMvc.perform(get("/api/lecturer/terms/" + TERM_ID + "/desktop-pack")
                        .header("Authorization", bearer(List.of(JwtRoleNames.LECTURER))))
                .andExpect(status().isServiceUnavailable());
    }

    private String bearer(List<String> roles) {
        String token = jwtService.createToken("lec@eiu.edu.vn", "Lecturer", "eiu.edu.vn", roles, null);
        return "Bearer " + token;
    }
}
