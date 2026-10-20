package authorization.com.eiu.capstone.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.eiu.capstone.backend.config.SecurityConfig;
import com.eiu.capstone.backend.controller.LecturerFeedbackController;
import com.eiu.capstone.backend.security.JwtAuthenticationFilter;
import com.eiu.capstone.backend.security.JwtRoleNames;
import com.eiu.capstone.backend.service.FeedbackService;
import com.eiu.capstone.backend.service.JwtService;
import com.eiu.capstone.backend.service.SessionValidityService;

@WebMvcTest(controllers = LecturerFeedbackController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        LecturerFeedbackController.class
})
@TestPropertySource(properties = {
        "jwt.secret=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "jwt.validity-seconds=3600",
        "spring.web.resources.add-mappings=false"
})
class LecturerFeedbackAuthorizationTest {

    @SpringBootApplication(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    static class SliceApp {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private SessionValidityService sessionValidityService;

    @MockBean
    private FeedbackService feedbackService;

    @BeforeEach
    void allowSessions() {
        when(sessionValidityService.isSessionValid(any(), anyInt())).thenReturn(true);
        when(sessionValidityService.isSessionValid(any(), isNull())).thenReturn(true);
        when(feedbackService.list(any())).thenReturn(List.of());
    }

    @Test
    void studentCannotListInbox() throws Exception {
        String token = jwtService.createToken(
                "student@eiu.edu.vn", "Student", "eiu.edu.vn", List.of(JwtRoleNames.STUDENT), "IRN001");
        mockMvc.perform(get("/api/lecturer/feedback/list").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void lecturerCanListInbox() throws Exception {
        String token = jwtService.createToken(
                "lecturer@eiu.edu.vn", "Lecturer", "eiu.edu.vn", List.of(JwtRoleNames.LECTURER), "T001");
        mockMvc.perform(get("/api/lecturer/feedback/list").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousListIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/lecturer/feedback/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotDeleteResolved() throws Exception {
        String token = jwtService.createToken(
                "student@eiu.edu.vn", "Student", "eiu.edu.vn", List.of(JwtRoleNames.STUDENT), "IRN001");
        mockMvc.perform(delete("/api/lecturer/feedback/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void lecturerCanDeleteResolved() throws Exception {
        String token = jwtService.createToken(
                "lecturer@eiu.edu.vn", "Lecturer", "eiu.edu.vn", List.of(JwtRoleNames.LECTURER), "T001");
        mockMvc.perform(delete("/api/lecturer/feedback/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void studentCannotReply() throws Exception {
        String token = jwtService.createToken(
                "student@eiu.edu.vn", "Student", "eiu.edu.vn", List.of(JwtRoleNames.STUDENT), "IRN001");
        mockMvc.perform(patch("/api/lecturer/feedback/" + UUID.randomUUID() + "/response")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"response\":\"Thanks\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void lecturerCanReply() throws Exception {
        String token = jwtService.createToken(
                "lecturer@eiu.edu.vn", "Lecturer", "eiu.edu.vn", List.of(JwtRoleNames.LECTURER), "T001");
        mockMvc.perform(patch("/api/lecturer/feedback/" + UUID.randomUUID() + "/response")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"response\":\"Thanks\"}"))
                .andExpect(status().isOk());
    }
}
