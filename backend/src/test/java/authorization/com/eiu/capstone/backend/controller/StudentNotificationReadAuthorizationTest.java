package authorization.com.eiu.capstone.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.eiu.capstone.backend.DTO.StudentNotificationReadKeysDTO;
import com.eiu.capstone.backend.config.SecurityConfig;
import com.eiu.capstone.backend.controller.StudentNotificationReadController;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtAuthenticationFilter;
import com.eiu.capstone.backend.security.JwtRoleNames;
import com.eiu.capstone.backend.service.JwtService;
import com.eiu.capstone.backend.service.SessionValidityService;
import com.eiu.capstone.backend.service.StudentNotificationDismissService;
import com.eiu.capstone.backend.service.StudentNotificationReadService;

@WebMvcTest(controllers = StudentNotificationReadController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        StudentNotificationReadController.class
})
@TestPropertySource(properties = {
        "jwt.secret=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "jwt.validity-seconds=3600",
        "spring.web.resources.add-mappings=false"
})
class StudentNotificationReadAuthorizationTest {

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
    private com.eiu.capstone.backend.service.MascotPracticeTokenService mascotPracticeTokenService;

    @MockBean
    private StudentNotificationReadService notificationReadService;

    @MockBean
    private StudentNotificationDismissService notificationDismissService;

    @MockBean
    private JwtAuthHelper jwtAuthHelper;

    @BeforeEach
    void allowSessions() {
        when(sessionValidityService.isSessionValid(any(), anyInt())).thenReturn(true);
        when(sessionValidityService.isSessionValid(any(), isNull())).thenReturn(true);
        when(jwtAuthHelper.requireActiveUser(any())).thenReturn(new UserAccount());
        when(notificationReadService.listKeys(any())).thenReturn(new StudentNotificationReadKeysDTO(List.of()));
        doNothing().when(notificationReadService).markRead(any(), any());
        when(notificationDismissService.listKeys(any())).thenReturn(new StudentNotificationReadKeysDTO(List.of()));
        doNothing().when(notificationDismissService).dismiss(any(), any());
        doNothing().when(notificationDismissService).dismissBatch(any(), any());
    }

    @Test
    void anonymousListUnauthorized() throws Exception {
        mockMvc.perform(get("/api/students/notification-reads/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousDismissUnauthorized() throws Exception {
        mockMvc.perform(get("/api/students/notification-reads/dismissed/list"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/students/notification-reads/dismiss")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"deadline-countdown-lab1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCanListMarkAndDismiss() throws Exception {
        String token = jwtService.createToken(
                "student@eiu.edu.vn", "Student", "eiu.edu.vn", List.of(JwtRoleNames.STUDENT), "IRN001");
        mockMvc.perform(get("/api/students/notification-reads/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/students/notification-reads/mark")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"deadline-countdown-lab1\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/students/notification-reads/dismissed/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/students/notification-reads/dismiss")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"deadline-countdown-lab1\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/students/notification-reads/dismiss/batch")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keys\":[\"deadline-countdown-lab1\"]}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void lecturerForbidden() throws Exception {
        String token = jwtService.createToken(
                "lecturer@eiu.edu.vn", "Lecturer", "eiu.edu.vn", List.of(JwtRoleNames.LECTURER), "T001");
        mockMvc.perform(get("/api/students/notification-reads/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/students/notification-reads/dismissed/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/students/notification-reads/dismiss")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"deadline-countdown-lab1\"}"))
                .andExpect(status().isForbidden());
    }
}
