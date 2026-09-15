package com.eiu.capstone.sandboxrunner.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.Map;

import com.eiu.capstone.sandboxrunner.config.RunnerAuthFilter;
import com.eiu.capstone.sandboxrunner.config.RunnerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SessionControllerTest {

    private MockMvc mockMvc;
    private final StubSessionService sessionService = new StubSessionService();

    @BeforeEach
    void setUp() {
        RunnerProperties properties = new RunnerProperties();
        properties.setToken("test-secret");
        mockMvc = MockMvcBuilders.standaloneSetup(new SessionController(sessionService))
                .addFilters(new RunnerAuthFilter(properties))
                .build();
    }

    @Test
    void createRejectsMissingBearerToken() throws Exception {
        mockMvc.perform(post("/sessions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createAcceptsValidToken() throws Exception {
        MockMultipartFile file = new MockMultipartFile("classes", "classes.tar.gz", "application/gzip", new byte[] {1, 2, 3});
        mockMvc.perform(multipart("/sessions")
                        .file(file)
                        .header("Authorization", "Bearer test-secret"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteRequiresAuth() throws Exception {
        mockMvc.perform(delete("/sessions/s1"))
                .andExpect(status().isUnauthorized());
    }

    private static final class StubSessionService extends SessionService {
        StubSessionService() {
            super(null, null, null);
        }

        @Override
        public Map<String, String> createSession(InputStream classesTarGz) {
            return Map.of("sessionId", "s1", "containerRoot", SessionService.CONTAINER_SUBMISSION_ROOT);
        }
    }
}
