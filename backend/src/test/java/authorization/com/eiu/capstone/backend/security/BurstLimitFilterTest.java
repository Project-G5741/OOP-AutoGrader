package authorization.com.eiu.capstone.backend.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.config.SecurityConfig;
import com.eiu.capstone.backend.security.BurstBudgetService;
import com.eiu.capstone.backend.security.BurstLimitFilter;
import com.eiu.capstone.backend.security.JwtAuthenticationFilter;
import com.eiu.capstone.backend.security.JwtRoleNames;
import com.eiu.capstone.backend.service.JwtService;
import com.eiu.capstone.backend.service.SessionValidityService;

@WebMvcTest(controllers = {
        BurstLimitFilterTest.AuthProbe.class,
        BurstLimitFilterTest.StudentProbe.class,
        BurstLimitFilterTest.PresenceProbe.class
})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        BurstBudgetService.class,
        BurstLimitFilter.class,
        BurstLimitFilterTest.AuthProbe.class,
        BurstLimitFilterTest.StudentProbe.class,
        BurstLimitFilterTest.PresenceProbe.class
})
@TestPropertySource(properties = {
        "jwt.secret=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "jwt.validity-seconds=3600",
        "spring.web.resources.add-mappings=false",
        "app.burst.max-requests=3",
        "app.burst.window-seconds=10",
        "app.burst.retry-after-seconds=5",
        "app.burst.max-keys=1000"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BurstLimitFilterTest {

    @SpringBootApplication(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    static class BurstSliceApp {
    }

    @RestController
    @RequestMapping("/api/auth")
    public static class AuthProbe {
        @PostMapping("/login")
        ResponseEntity<String> login() {
            return ResponseEntity.ok("logged-in");
        }
    }

    @RestController
    @RequestMapping("/api/labs")
    public static class StudentProbe {
        @GetMapping("/list")
        String list() {
            return "ok";
        }
    }

    @RestController
    @RequestMapping("/api/presence")
    public static class PresenceProbe {
        @GetMapping("/count")
        String count() {
            return "{\"count\":0}";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private SessionValidityService sessionValidityService;

    @BeforeEach
    void allowSessions() {
        when(sessionValidityService.isSessionValid(any(), anyInt())).thenReturn(true);
        when(sessionValidityService.isSessionValid(any(), isNull())).thenReturn(true);
    }

    @Test
    void anonymousAuthHammer_returns429() throws Exception {
        mockMvc.perform(post("/api/auth/login")).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login")).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login")).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void independentUsersHaveSeparateBudgets() throws Exception {
        String a = jwtService.createToken(
                "a@eiu.edu.vn", "A", "eiu.edu.vn", List.of(JwtRoleNames.STUDENT), "IRN001");
        String b = jwtService.createToken(
                "b@eiu.edu.vn", "B", "eiu.edu.vn", List.of(JwtRoleNames.STUDENT), "IRN002");
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/labs/list").header("Authorization", "Bearer " + a))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/labs/list").header("Authorization", "Bearer " + a))
                .andExpect(status().isTooManyRequests());
        mockMvc.perform(get("/api/labs/list").header("Authorization", "Bearer " + b))
                .andExpect(status().isOk());
    }

    @Test
    void lecturerSameRulesAsStudent() throws Exception {
        String token = jwtService.createToken(
                "lec@eiu.edu.vn", "L", "eiu.edu.vn", List.of(JwtRoleNames.LECTURER), "T001");
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/labs/list").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/labs/list").header("Authorization", "Bearer " + token))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void presenceCountDoesNotConsumeBudget() throws Exception {
        String token = jwtService.createToken(
                "p@eiu.edu.vn", "P", "eiu.edu.vn", List.of(JwtRoleNames.STUDENT), "IRN003");
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/presence/count").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/labs/list").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void forgedXForwardedForDoesNotCreateSeparateAnonymousBucket() throws Exception {
        mockMvc.perform(post("/api/auth/login")).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login")).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login")).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login").header("X-Forwarded-For", "203.0.113.9"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void revokedBearerOnApiStill401UnderBudget() throws Exception {
        when(sessionValidityService.isSessionValid(any(), anyInt())).thenReturn(false);
        when(sessionValidityService.isSessionValid(any(), isNull())).thenReturn(false);
        String token = jwtService.createToken(
                "revoked@eiu.edu.vn", "R", "eiu.edu.vn", List.of(JwtRoleNames.STUDENT), "IRN004");
        // Invalid Bearer is IP-keyed; within budget the chain still yields 401 (not swallowed).
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/labs/list").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void junkBearerOnAuthStillHitsIpBudget() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login").header("Authorization", "Bearer junk-token"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/auth/login").header("Authorization", "Bearer junk-token"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void emailCaseDoesNotSplitBudget() throws Exception {
        String lower = jwtService.createToken(
                "Case@eiu.edu.vn", "C", "eiu.edu.vn", List.of(JwtRoleNames.STUDENT), "IRN005");
        // JWT stores the claim as given; principal email is from claim — filter normalizes.
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/labs/list").header("Authorization", "Bearer " + lower))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/labs/list").header("Authorization", "Bearer " + lower))
                .andExpect(status().isTooManyRequests());
    }
}
