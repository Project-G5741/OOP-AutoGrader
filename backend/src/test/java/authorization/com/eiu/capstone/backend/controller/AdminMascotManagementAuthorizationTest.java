package authorization.com.eiu.capstone.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.eiu.capstone.backend.DTO.AdminMascotBulkGrantResult;
import com.eiu.capstone.backend.DTO.AdminUserMascotRow;
import com.eiu.capstone.backend.config.SecurityConfig;
import com.eiu.capstone.backend.controller.AdminMascotManagementController;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtAuthenticationFilter;
import com.eiu.capstone.backend.security.JwtRoleNames;
import com.eiu.capstone.backend.service.AdminMascotManagementService;
import com.eiu.capstone.backend.service.JwtService;
import com.eiu.capstone.backend.service.MascotPracticeTokenService;
import com.eiu.capstone.backend.service.SessionValidityService;

@WebMvcTest(controllers = AdminMascotManagementController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        AdminMascotManagementController.class
})
@TestPropertySource(properties = {
        "jwt.secret=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "jwt.validity-seconds=3600",
        "spring.web.resources.add-mappings=false"
})
class AdminMascotManagementAuthorizationTest {

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
    private MascotPracticeTokenService mascotPracticeTokenService;

    @MockBean
    private JwtAuthHelper jwtAuthHelper;

    @MockBean
    private AdminMascotManagementService mascotManagementService;

    private UUID sampleUserId;

    @BeforeEach
    void setUp() {
        sampleUserId = UUID.randomUUID();
        AdminUserMascotRow sampleRow = new AdminUserMascotRow(
                sampleUserId,
                "2331200001",
                null,
                "Sample Student",
                "sample@eiu.edu.vn",
                true,
                true,
                false,
                "red_panda",
                1200,
                3,
                15,
                25,
                2,
                List.of());

        when(sessionValidityService.isSessionValid(any(), anyInt())).thenReturn(true);
        when(sessionValidityService.isSessionValid(any(), isNull())).thenReturn(true);
        when(jwtAuthHelper.requireActiveUser(any())).thenReturn(new UserAccount());
        when(mascotManagementService.listUsers()).thenReturn(List.of(sampleRow));
        when(mascotManagementService.updateResources(any(), any())).thenReturn(sampleRow);
        when(mascotManagementService.grantPets(any(), any())).thenReturn(sampleRow);
        when(mascotManagementService.bulkGrantPets(any()))
                .thenReturn(new AdminMascotBulkGrantResult(1, 1, 0, List.of(sampleRow)));
        when(mascotManagementService.updatePet(any(), any(), any())).thenReturn(sampleRow);
        when(mascotManagementService.setEquippedMascot(any(), any())).thenReturn(sampleRow);
        when(mascotManagementService.deletePet(any(), any())).thenReturn(sampleRow);
    }

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/lecturer/admin-mascots/users"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/lecturer/admin-mascots/users/" + sampleUserId + "/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"crystals\":5,\"gachaTickets\":10}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/lecturer/admin-mascots/users/" + sampleUserId + "/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mascotIds\":[\"red_panda\"]}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/lecturer/admin-mascots/users/" + sampleUserId + "/pets/red_panda"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentIsForbidden() throws Exception {
        String token = jwtService.createToken(
                "student@eiu.edu.vn", "Student", "eiu.edu.vn", List.of(JwtRoleNames.STUDENT), "2231200001");
        mockMvc.perform(get("/api/lecturer/admin-mascots/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/lecturer/admin-mascots/users/" + sampleUserId + "/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"crystals\":5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void lecturerOnlyIsForbidden() throws Exception {
        String token = jwtService.createToken(
                "lecturer@eiu.edu.vn", "Lecturer", "eiu.edu.vn", List.of(JwtRoleNames.LECTURER), "T001");
        mockMvc.perform(get("/api/lecturer/admin-mascots/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/lecturer/admin-mascots/users/" + sampleUserId + "/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gachaTickets\":20}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCrudUserGachaResourcesAndMascots() throws Exception {
        String token = jwtService.createToken(
                "admin@eiu.edu.vn", "Admin", "eiu.edu.vn", List.of(JwtRoleNames.ADMIN), "A001");

        mockMvc.perform(get("/api/lecturer/admin-mascots/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/lecturer/admin-mascots/users/" + sampleUserId + "/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"crystals\":10,\"gachaTickets\":30,\"points\":5000,\"gachaPityCount\":40,\"eggSmashPity\":3}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lecturer/admin-mascots/users/" + sampleUserId + "/pets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mascotIds\":[\"red_panda\"],\"pricePaid\":0,\"equip\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lecturer/admin-mascots/bulk-grant")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[\"" + sampleUserId + "\"],\"mascotIds\":[\"red_panda\"],\"crystalsDelta\":2,\"ticketsDelta\":10,\"pointsDelta\":500}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/lecturer/admin-mascots/users/" + sampleUserId + "/pets/red_panda")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetMascotId\":\"shiba\",\"pricePaid\":0,\"equip\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/lecturer/admin-mascots/users/" + sampleUserId + "/equipped")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mascotId\":\"fox\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/lecturer/admin-mascots/users/" + sampleUserId + "/pets/red_panda")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
