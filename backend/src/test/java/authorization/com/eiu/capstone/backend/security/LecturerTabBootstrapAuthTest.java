package authorization.com.eiu.capstone.backend.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.eiu.capstone.backend.analytics.dto.LecturerOverviewResponse;
import com.eiu.capstone.backend.analytics.service.LecturerTabBootstrapService;
import com.eiu.capstone.backend.config.SecurityConfig;
import com.eiu.capstone.backend.controller.LecturerTabBootstrapController;
import com.eiu.capstone.backend.security.JwtAuthenticationFilter;
import com.eiu.capstone.backend.security.JwtRoleNames;
import com.eiu.capstone.backend.service.JwtService;
import com.eiu.capstone.backend.service.SessionValidityService;

@WebMvcTest(controllers = LecturerTabBootstrapController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        LecturerTabBootstrapController.class
})
@TestPropertySource(properties = {
        "jwt.secret=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "jwt.validity-seconds=3600",
        "spring.web.resources.add-mappings=false"
})
class LecturerTabBootstrapAuthTest {

    @SpringBootApplication(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    static class BootstrapSliceApp {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private LecturerTabBootstrapService bootstrapService;

    @MockBean
    private SessionValidityService sessionValidityService;

    @BeforeEach
    void allowSessions() {
        when(sessionValidityService.isSessionValid(any(), anyInt())).thenReturn(true);
        when(sessionValidityService.isSessionValid(any(), isNull())).thenReturn(true);
        when(bootstrapService.dashboard()).thenReturn(
                new LecturerOverviewResponse(0L, 0L, null, 0L, List.of(), 0L, List.of(), List.of(), List.of()));
        when(bootstrapService.gradingLabs()).thenReturn(List.of());
    }

    @Test
    void anonymousDashboardBootstrapIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/lecturer/bootstrap/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentDashboardBootstrapIsForbidden() throws Exception {
        String token = jwtService.createToken(
                "s@eiu.edu.vn", "Student", "eiu.edu.vn", List.of(JwtRoleNames.STUDENT), "S1");
        mockMvc.perform(get("/api/lecturer/bootstrap/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void lecturerDashboardBootstrapIsOk() throws Exception {
        String token = jwtService.createToken(
                "l@eiu.edu.vn", "Lecturer", "eiu.edu.vn", List.of(JwtRoleNames.LECTURER), "T1");
        mockMvc.perform(get("/api/lecturer/bootstrap/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void lecturerGradingBootstrapIsOk() throws Exception {
        String token = jwtService.createToken(
                "l@eiu.edu.vn", "Lecturer", "eiu.edu.vn", List.of(JwtRoleNames.LECTURER), "T1");
        mockMvc.perform(get("/api/lecturer/bootstrap/grading").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
