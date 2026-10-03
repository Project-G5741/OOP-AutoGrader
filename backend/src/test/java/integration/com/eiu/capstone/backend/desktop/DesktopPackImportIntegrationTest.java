package integration.com.eiu.capstone.backend.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
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
import com.eiu.capstone.backend.desktop.DesktopPackBootstrapService;
import com.eiu.capstone.backend.desktop.pack.DesktopPackCrypto;
import com.eiu.capstone.backend.desktop.pack.DesktopPackFile;
import com.eiu.capstone.backend.desktop.pack.DesktopPackFileNames;
import com.eiu.capstone.backend.desktop.pack.DesktopPackInnerPayload;
import com.eiu.capstone.backend.desktop.pack.DesktopPackLabEntry;
import com.eiu.capstone.backend.desktop.pack.DesktopPackLabMeta;
import com.eiu.capstone.backend.desktop.pack.DesktopPackManifest;
import com.eiu.capstone.backend.desktop.pack.DesktopPackSerializer;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.repository.LabRepository;

@SpringBootTest(classes = EiuCapstoneBackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("desktop")
class DesktopPackImportIntegrationTest {

    private static final UUID TERM_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER_TERM_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID LAB_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID OTHER_LAB_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID CHALLENGE_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID OTHER_CHALLENGE_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    private static Path home;
    private static Path rubricDir;
    private static PrivateKey privateKey;
    private static final DesktopPackCrypto CRYPTO = DesktopPackCrypto.withDefaultEmbeddedKey();
    private static final DesktopPackSerializer SERIALIZER = new DesktopPackSerializer();

    @DynamicPropertySource
    static void desktopHome(DynamicPropertyRegistry registry) throws Exception {
        home = Files.createTempDirectory("desktop-import-test");
        rubricDir = home.resolve("rubric");
        Files.createDirectories(rubricDir);
        privateKey = DesktopPackCrypto.decodePrivateKey(Base64.getDecoder().decode(
                "MC4CAQAwBQYDK2VwBCIEIDkzhyy+Z04+u6tsG4rb/qesP7orwumt08BUPG0SZWwz"));
        registry.add("APP_DESKTOP_HOME", () -> home.toAbsolutePath().toString());
        registry.add("app.desktop.install-dir", () -> home.toAbsolutePath().toString());
        registry.add("app.desktop.pack-dir", () -> rubricDir.toAbsolutePath().toString());
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:desktopimport;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("jwt.secret", () -> "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DesktopPackBootstrapService bootstrapService;

    @Autowired
    private LabRepository labRepository;

    @BeforeEach
    void cleanRubricDir() throws Exception {
        if (!Files.isDirectory(rubricDir)) {
            Files.createDirectories(rubricDir);
            return;
        }
        try (Stream<Path> stream = Files.list(rubricDir)) {
            for (Path path : stream.toList()) {
                Files.deleteIfExists(path);
            }
        }
        bootstrapService.clearImportFingerprint();
    }

    @Test
    void labImport_sameNameDifferentId_returns409WithConflicts() throws Exception {
        loadTermWithLab("Shared Lab", LAB_ID);

        byte[] conflicting = buildLabPackBytes(TERM_ID, OTHER_LAB_ID, "Shared Lab", "conflict-v1");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                DesktopPackFileNames.labFilename("Shared Lab"),
                "application/octet-stream",
                conflicting);

        mockMvc.perform(multipart("/api/desktop/packs/import").file(file))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.conflicts").isArray())
                .andExpect(jsonPath("$.conflicts[0].labName").value("Shared Lab"));
    }

    @Test
    void labImport_confirmReplace_materializesAndWritesFingerprint() throws Exception {
        loadTermWithLab("Shared Lab", LAB_ID);

        String filename = DesktopPackFileNames.labFilename("Shared Lab");
        byte[] conflicting = buildLabPackBytes(
                TERM_ID, OTHER_LAB_ID, "Shared Lab", "replace-v1", OTHER_CHALLENGE_ID);
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "application/octet-stream", conflicting);

        mockMvc.perform(multipart("/api/desktop/packs/import").file(file).param("confirmReplace", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value(filename))
                .andExpect(jsonPath("$.requiresRestart").value(true));

        assertTrue(Files.isRegularFile(rubricDir.resolve(filename)));
        assertTrue(Files.isRegularFile(home.resolve("data").resolve("desktop-pack-fingerprint.txt")));
        assertTrue(labRepository.findById(OTHER_LAB_ID).isPresent());

        var skip = bootstrapService.bootstrap();
        assertTrue(skip.ready(), () -> String.valueOf(skip.error()));
    }

    @Test
    void labImport_sameUuid_succeedsWithoutConflict() throws Exception {
        loadTermWithLab("Shared Lab", LAB_ID);

        String filename = DesktopPackFileNames.labFilename("Shared Lab");
        byte[] sameId = buildLabPackBytes(TERM_ID, LAB_ID, "Shared Lab", "same-id-v2");
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "application/octet-stream", sameId);

        mockMvc.perform(multipart("/api/desktop/packs/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value(filename));
    }

    @Test
    void labImport_sameNameOtherTerm_succeedsWithoutConflict() throws Exception {
        loadTermWithLab("Shared Lab", LAB_ID);

        String filename = DesktopPackFileNames.labFilename("Shared Lab");
        byte[] otherTerm = buildLabPackBytes(OTHER_TERM_ID, OTHER_LAB_ID, "Shared Lab", "other-term-v1");
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "application/octet-stream", otherTerm);

        mockMvc.perform(multipart("/api/desktop/packs/import").file(file))
                .andExpect(status().isOk());
    }

    @Test
    void termImport_deletesExistingLabPackFiles() throws Exception {
        String labName = DesktopPackFileNames.labFilename("Leftover Lab");
        Files.write(rubricDir.resolve(labName), buildLabPackBytes(TERM_ID, LAB_ID, "Leftover Lab", "leftover-lab"));
        assertTrue(Files.isRegularFile(rubricDir.resolve(labName)));

        String termName = DesktopPackFileNames.termFilename("2026", 1);
        byte[] termBytes = buildTermPackBytes(TERM_ID, LAB_ID, "Term Lab", "term-clear-v1");
        MockMultipartFile file = new MockMultipartFile(
                "file", termName, "application/octet-stream", termBytes);

        mockMvc.perform(multipart("/api/desktop/packs/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value(termName));

        assertTrue(Files.isRegularFile(rubricDir.resolve(termName)));
        assertFalse(Files.isRegularFile(rubricDir.resolve(labName)));
    }

    @Test
    void bootstrap_emptyRubric_wipesExistingLabs() throws Exception {
        loadTermWithLab("Wipe Me", LAB_ID);
        assertEquals(1, labRepository.findAll().size());

        try (Stream<Path> stream = Files.list(rubricDir)) {
            for (Path path : stream.toList()) {
                Files.deleteIfExists(path);
            }
        }
        bootstrapService.clearImportFingerprint();

        var result = bootstrapService.bootstrap();
        assertFalse(result.ready());
        assertTrue(result.packMissing());
        assertEquals(0, labRepository.findAll().size());
    }

    private void loadTermWithLab(String labName, UUID labId) throws Exception {
        String termName = DesktopPackFileNames.termFilename("2026", 1);
        Files.write(rubricDir.resolve(termName), buildTermPackBytes(TERM_ID, labId, labName, "seed-" + labId));
        bootstrapService.clearImportFingerprint();
        var result = bootstrapService.bootstrap();
        assertTrue(result.ready(), () -> String.valueOf(result.error()));
        assertEquals(1, labRepository.findAll().size());
        assertEquals(
                1,
                labRepository.findByTerm_Id(TERM_ID).size(),
                () -> "labs=" + labRepository.findAll().stream()
                        .map(lab -> lab.getId() + "/" + lab.getName() + "/term="
                                + (lab.getTerm() != null ? lab.getTerm().getId() : null))
                        .toList());
    }

    private static byte[] buildTermPackBytes(UUID termId, UUID labId, String labName, String version)
            throws Exception {
        return serialize(termId, "2026 — Quarter 1", labId, labName, version, List.of(labId), CHALLENGE_ID);
    }

    private static byte[] buildLabPackBytes(UUID termId, UUID labId, String labName, String version)
            throws Exception {
        return buildLabPackBytes(termId, labId, labName, version, CHALLENGE_ID);
    }

    private static byte[] buildLabPackBytes(
            UUID termId, UUID labId, String labName, String version, UUID challengeId) throws Exception {
        return serialize(termId, "2026 — Quarter 1", labId, labName, version, List.of(labId), challengeId);
    }

    private static byte[] serialize(
            UUID termId,
            String termLabel,
            UUID labId,
            String labName,
            String version,
            List<UUID> labIds,
            UUID challengeId) throws Exception {
        LabRubricSnapshot rubric = new LabRubricSnapshot(
                labId,
                Map.of(
                        1,
                        new ChallengeRubric(
                                challengeId,
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
                version,
                termId,
                termLabel,
                List.of(new DesktopPackLabEntry(
                        new DesktopPackLabMeta(labId, labName, true, null, null),
                        rubric)));
        byte[] innerJson = SERIALIZER.toJson(inner);
        DesktopPackManifest manifest = new DesktopPackManifest(
                DesktopPackManifest.CURRENT_FORMAT_VERSION,
                version,
                termId,
                termLabel,
                Instant.parse("2026-09-01T00:00:00Z"),
                labIds,
                "placeholder");
        DesktopPackFile pack = CRYPTO.buildPack(privateKey, manifest, innerJson);
        return CRYPTO.serializeFile(pack);
    }
}
