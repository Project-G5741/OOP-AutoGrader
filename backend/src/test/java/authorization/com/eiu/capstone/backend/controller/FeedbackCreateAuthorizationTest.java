package authorization.com.eiu.capstone.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
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

import com.eiu.capstone.backend.DTO.feedback.FeedbackCreatedDTO;
import com.eiu.capstone.backend.config.SecurityConfig;
import com.eiu.capstone.backend.controller.FeedbackController;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtAuthenticationFilter;
import com.eiu.capstone.backend.security.JwtRoleNames;
import com.eiu.capstone.backend.service.FeedbackService;
import com.eiu.capstone.backend.service.JwtService;
import com.eiu.capstone.backend.service.SessionValidityService;

@WebMvcTest(controllers = FeedbackController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        FeedbackController.class
})
@TestPropertySource(properties = {
        "jwt.secret=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "jwt.validity-seconds=3600",
        "spring.web.resources.add-mappings=false"
})
class FeedbackCreateAuthorizationTest {

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

    @MockBean
    private JwtAuthHelper jwtAuthHelper;

    @BeforeEach
    void allowSessions() {
        when(sessionValidityService.isSessionValid(any(), anyInt())).thenReturn(true);
        when(sessionValidityService.isSessionValid(any(), isNull())).thenReturn(true);
        when(jwtAuthHelper.requireActiveUser(any())).thenReturn(new UserAccount());
        when(feedbackService.create(any(), any(), any(), any())).thenReturn(
                new FeedbackCreatedDTO(UUID.randomUUID(), "BUG", "OPEN", OffsetDateTime.parse("2026-10-20T01:00:00Z")));
        when(feedbackService.listRepliesForAuthor(any())).thenReturn(List.of());
    }

    @Test
    void anonymousCreateIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/feedback/create")
                        .contentType("multipart/form-data")
                        .param("type", "BUG")
                        .param("message", "hello"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCanCreate() throws Exception {
        String token = jwtService.createToken(
                "student@eiu.edu.vn", "Student", "eiu.edu.vn", List.of(JwtRoleNames.STUDENT), "IRN001");
        mockMvc.perform(multipart("/api/feedback/create")
                        .param("type", "BUG")
                        .param("message", "hello")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }

    @Test
    void lecturerCanCreate() throws Exception {
        String token = jwtService.createToken(
                "lecturer@eiu.edu.vn", "Lecturer", "eiu.edu.vn", List.of(JwtRoleNames.LECTURER), "T001");
        mockMvc.perform(multipart("/api/feedback/create")
                        .param("type", "CONTRIBUTION")
                        .param("message", "idea")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }

    @Test
    void studentCanReadReplies() throws Exception {
        String token = jwtService.createToken(
                "student@eiu.edu.vn", "Student", "eiu.edu.vn", List.of(JwtRoleNames.STUDENT), "IRN001");
        mockMvc.perform(get("/api/feedback/replies").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
