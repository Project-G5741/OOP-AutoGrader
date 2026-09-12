package authorization.com.eiu.capstone.backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.eiu.capstone.backend.config.SecurityConfig;
import com.eiu.capstone.backend.controller.PresenceController;
import com.eiu.capstone.backend.security.JwtAuthenticationFilter;
import com.eiu.capstone.backend.security.JwtRoleNames;
import com.eiu.capstone.backend.service.JwtService;
import com.eiu.capstone.backend.service.PresenceService;

@WebMvcTest(controllers = PresenceController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        PresenceService.class,
        PresenceController.class
})
@TestPropertySource(properties = {
        "jwt.secret=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "jwt.validity-seconds=3600",
        "spring.web.resources.add-mappings=false"
})
class PresenceControllerTest {

    @SpringBootApplication(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    static class PresenceSliceApp {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void signedInGetHeartbeatsOnceAndAnonymousCanReadCount() throws Exception {
        mockMvc.perform(get("/api/presence"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        String token = jwtService.createToken(
                "user@eiu.edu.vn", "User", "eiu.edu.vn", List.of(JwtRoleNames.STUDENT), "IRN001");
        mockMvc.perform(get("/api/presence").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
        mockMvc.perform(get("/api/presence").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
        mockMvc.perform(get("/api/presence"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
        mockMvc.perform(delete("/api/presence").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
        mockMvc.perform(get("/api/presence"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }
}
