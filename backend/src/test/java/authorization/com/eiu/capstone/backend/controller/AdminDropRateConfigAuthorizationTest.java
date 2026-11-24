package authorization.com.eiu.capstone.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.eiu.capstone.backend.config.SecurityConfig;
import com.eiu.capstone.backend.controller.AdminDropRateConfigController;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtAuthenticationFilter;
import com.eiu.capstone.backend.security.JwtRoleNames;
import com.eiu.capstone.backend.service.EggSmashRateConfig;
import com.eiu.capstone.backend.service.EggSmashRateConfigService;
import com.eiu.capstone.backend.service.GachaRateConfig;
import com.eiu.capstone.backend.service.GachaRateConfigService;
import com.eiu.capstone.backend.service.JwtService;
import com.eiu.capstone.backend.service.MascotPracticeTokenService;
import com.eiu.capstone.backend.service.SessionValidityService;

@WebMvcTest(controllers = AdminDropRateConfigController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        AdminDropRateConfigController.class
})
@TestPropertySource(properties = {
        "jwt.secret=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "jwt.validity-seconds=3600",
        "spring.web.resources.add-mappings=false"
})
class AdminDropRateConfigAuthorizationTest {

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
    private GachaRateConfigService gachaRateConfigService;

    @MockBean
    private EggSmashRateConfigService eggSmashRateConfigService;

    @BeforeEach
    void setUp() {
        when(sessionValidityService.isSessionValid(any(), anyInt())).thenReturn(true);
        when(sessionValidityService.isSessionValid(any(), isNull())).thenReturn(true);
        when(jwtAuthHelper.requireActiveUser(any())).thenReturn(new UserAccount());
        when(gachaRateConfigService.current()).thenReturn(GachaRateConfig.defaults());
        when(gachaRateConfigService.update(any())).thenReturn(GachaRateConfig.defaults());
        when(gachaRateConfigService.resetDefaults()).thenReturn(GachaRateConfig.defaults());
        when(eggSmashRateConfigService.current()).thenReturn(EggSmashRateConfig.defaults());
        when(eggSmashRateConfigService.update(any())).thenReturn(EggSmashRateConfig.defaults());
        when(eggSmashRateConfigService.resetDefaults()).thenReturn(EggSmashRateConfig.defaults());
    }

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/lecturer/drop-rates/config"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/lecturer/drop-rates/gacha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentIsForbidden() throws Exception {
        String token = jwtService.createToken(
                "student@eiu.edu.vn", "Student", "eiu.edu.vn", List.of(JwtRoleNames.STUDENT), "2231200001");
        mockMvc.perform(get("/api/lecturer/drop-rates/config")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void lecturerOnlyIsForbidden() throws Exception {
        String token = jwtService.createToken(
                "lecturer@eiu.edu.vn", "Lecturer", "eiu.edu.vn", List.of(JwtRoleNames.LECTURER), "T001");
        mockMvc.perform(get("/api/lecturer/drop-rates/config")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/lecturer/drop-rates/egg-smash")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadUpdateAndResetDropRates() throws Exception {
        String token = jwtService.createToken(
                "admin@eiu.edu.vn", "Admin", "eiu.edu.vn", List.of(JwtRoleNames.ADMIN), "A001");

        mockMvc.perform(get("/api/lecturer/drop-rates/config")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/lecturer/drop-rates/gacha")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hardPityLimit": 100,
                                  "softPityStart": 90,
                                  "directLegendaryRate": 0.001,
                                  "crystalBaseRate": 0.002,
                                  "pointCrystalRate": 0.0002,
                                  "randomMascotRate": 0.01,
                                  "cashbackRate300": 0.05,
                                  "cashbackRate150": 0.1,
                                  "cashbackRate60": 0.2,
                                  "cashbackRate20": 0.4
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lecturer/drop-rates/gacha/reset")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/lecturer/drop-rates/egg-smash")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "baseCrystalHitRate": 0.01,
                                  "turn6CrystalHitRate": 0.02,
                                  "turn7CrystalHitRate": 0.1,
                                  "turn8CrystalHitRate": 0.4,
                                  "turn9CrystalHitRate": 1.0,
                                  "eggRate300": 0.1,
                                  "eggRate200": 0.2,
                                  "eggRate100": 0.3
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lecturer/drop-rates/egg-smash/reset")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
