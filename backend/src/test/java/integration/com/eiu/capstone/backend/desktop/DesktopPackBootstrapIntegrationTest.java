package integration.com.eiu.capstone.backend.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.eiu.capstone.backend.EiuCapstoneBackendApplication;
import com.eiu.capstone.backend.desktop.DesktopRuntimeState;
import com.eiu.capstone.backend.desktop.pack.DesktopPackCrypto;
import com.eiu.capstone.backend.desktop.pack.DesktopPackFile;
import com.eiu.capstone.backend.desktop.pack.DesktopPackInnerPayload;
import com.eiu.capstone.backend.desktop.pack.DesktopPackLabEntry;
import com.eiu.capstone.backend.desktop.pack.DesktopPackLabMeta;
import com.eiu.capstone.backend.desktop.pack.DesktopPackFileNames;
import com.eiu.capstone.backend.desktop.pack.DesktopPackManifest;
import com.eiu.capstone.backend.desktop.pack.DesktopPackSerializer;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.repository.LabRepository;

@SpringBootTest(classes = EiuCapstoneBackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("desktop")
class DesktopPackBootstrapIntegrationTest {

    private static final UUID TERM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID LAB_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID CHALLENGE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static Path home;

    @DynamicPropertySource
    static void desktopHome(DynamicPropertyRegistry registry) throws Exception {
        home = Files.createTempDirectory("desktop-bootstrap-test");
        writePack(home);
        registry.add("APP_DESKTOP_HOME", () -> home.toAbsolutePath().toString());
        registry.add("app.desktop.install-dir", () -> home.toAbsolutePath().toString());
        registry.add("app.desktop.pack-dir", () -> home.resolve("rubric").toAbsolutePath().toString());
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:desktopbootstrap;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("jwt.secret", () -> "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }

    private static void writePack(Path installHome) throws Exception {
        Files.createDirectories(installHome.resolve("rubric"));
        DesktopPackCrypto crypto = DesktopPackCrypto.withDefaultEmbeddedKey();
        var privateKey = DesktopPackCrypto.decodePrivateKey(Base64.getDecoder().decode(
                "MC4CAQAwBQYDK2VwBCIEIDkzhyy+Z04+u6tsG4rb/qesP7orwumt08BUPG0SZWwz"));
        DesktopPackSerializer serializer = new DesktopPackSerializer();
        LabRubricSnapshot rubric = new LabRubricSnapshot(
                LAB_ID,
                Map.of(
                        1,
                        new ChallengeRubric(
                                CHALLENGE_ID,
                                1,
                                "Warmup",
                                List.of(),
                                List.of(),
                                List.of(),
                                false,
                                1,
                                1,
                                0,
                                1)));
        DesktopPackInnerPayload inner = new DesktopPackInnerPayload(
                "bootstrap-test-v1",
                TERM_ID,
                "2026 — Quarter 1",
                List.of(new DesktopPackLabEntry(
                        new DesktopPackLabMeta(LAB_ID, "Bootstrap Lab", true, null, null),
                        rubric)));
        byte[] innerJson = serializer.toJson(inner);
        DesktopPackManifest manifest = new DesktopPackManifest(
                DesktopPackManifest.CURRENT_FORMAT_VERSION,
                "bootstrap-test-v1",
                TERM_ID,
                "2026 — Quarter 1",
                Instant.parse("2026-09-01T00:00:00Z"),
                List.of(LAB_ID),
                "placeholder");
        DesktopPackFile pack = crypto.buildPack(privateKey, manifest, innerJson);
        Files.write(installHome.resolve("rubric").resolve(DesktopPackFileNames.termFilename("2026", 1)), crypto.serializeFile(pack));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DesktopRuntimeState runtimeState;

    @Autowired
    private LabRepository labRepository;

    @Test
    void bootstrap_loadsLabs_andUploadWorksWithoutAuthHeader() throws Exception {
        assertTrue(runtimeState.isReady(), () -> String.valueOf(runtimeState.bootstrapError()));
        assertFalse(runtimeState.isPackMissing());
        assertEquals(1, labRepository.findAll().size());

        mockMvc.perform(get("/api/desktop/status"))
                .andExpect(status().isOk());
    }
}
