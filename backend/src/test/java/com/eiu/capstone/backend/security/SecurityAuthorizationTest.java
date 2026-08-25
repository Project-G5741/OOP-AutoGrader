package com.eiu.capstone.backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.eiu.capstone.backend.config.SecurityConfig;
import com.eiu.capstone.backend.controller.RootController;
import com.eiu.capstone.backend.service.JwtService;

@WebMvcTest(controllers = {
        RootController.class,
        SecurityAuthorizationProbes.AuthProbeController.class,
        SecurityAuthorizationProbes.LecturerProbeController.class,
        SecurityAuthorizationProbes.SubmissionProbeController.class,
        SecurityAuthorizationProbes.LabProbeController.class
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "jwt.secret=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "jwt.validity-seconds=3600"
})
class SecurityAuthorizationTest {

    private static final UUID LAB_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void anonymousOverview_is401Not403() throws Exception {
        mockMvc.perform(get("/api/lecturer/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousMyHistory_is401Not403() throws Exception {
        mockMvc.perform(get("/api/submissions/my-history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentOverview_is403Not401() throws Exception {
        mockMvc.perform(get("/api/lecturer/overview").header("Authorization", bearer(List.of("STUDENT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void lecturerUpload_is403() throws Exception {
        mockMvc.perform(post("/api/submissions/" + LAB_ID + "/1/upload")
                        .header("Authorization", bearer(List.of("LECTURER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void dualRoleOverview_isNotDenied() throws Exception {
        mockMvc.perform(get("/api/lecturer/overview")
                        .header("Authorization", bearer(List.of("STUDENT", "LECTURER"))))
                .andExpect(status().isOk());
    }

    @Test
    void dualRoleMyHistory_isNotDenied() throws Exception {
        mockMvc.perform(get("/api/submissions/my-history")
                        .header("Authorization", bearer(List.of("STUDENT", "LECTURER"))))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousLogin_isNot401FromSecurity() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousRoot_is200() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void studentLabList_isNot403() throws Exception {
        mockMvc.perform(get("/api/labs").header("Authorization", bearer(List.of("STUDENT"))))
                .andExpect(status().isOk());
    }

    @Test
    void studentLabStatistics_is403() throws Exception {
        mockMvc.perform(get("/api/labs/" + LAB_ID + "/statistics")
                        .header("Authorization", bearer(List.of("STUDENT"))))
                .andExpect(status().isForbidden());
    }

    private String bearer(List<String> roles) {
        String token = jwtService.createToken("user@eiu.edu.vn", "User", "eiu.edu.vn", roles, "IRN001");
        return "Bearer " + token;
    }
}
