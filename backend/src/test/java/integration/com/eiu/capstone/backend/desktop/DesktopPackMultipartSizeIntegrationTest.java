package integration.com.eiu.capstone.backend.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.eiu.capstone.backend.EiuCapstoneBackendApplication;

/**
 * Exercises the real servlet multipart gate (MockMvc does not).
 * Packs larger than Spring Boot's default 1MB must not become opaque 500s.
 */
@SpringBootTest(
        classes = EiuCapstoneBackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("desktop")
class DesktopPackMultipartSizeIntegrationTest {

    private static Path home;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void desktopHome(DynamicPropertyRegistry registry) throws Exception {
        home = Files.createTempDirectory("desktop-multipart-size");
        Files.createDirectories(home.resolve("rubric"));
        registry.add("APP_DESKTOP_HOME", () -> home.toAbsolutePath().toString());
        registry.add("app.desktop.install-dir", () -> home.toAbsolutePath().toString());
        registry.add("app.desktop.pack-dir", () -> home.resolve("rubric").toAbsolutePath().toString());
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:desktopmultipart;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("jwt.secret", () -> "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }

    @Test
    void import_packLargerThanOneMegabyte_reachesPackValidation() {
        byte[] oversizedJunk = new byte[1_200_000];
        ResponseEntity<Map> response = postImport(oversizedJunk, "Rubric_SizeTest.agpack");

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode(),
                () -> "expected pack validation 400, got " + response.getStatusCode() + " body=" + response.getBody());
        Object message = response.getBody() != null ? response.getBody().get("message") : null;
        assertTrue(
                message != null && message.toString().toLowerCase().contains("invalid"),
                () -> "body=" + response.getBody());
    }

    private ResponseEntity<Map> postImport(byte[] bytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(
                "http://127.0.0.1:" + port + "/api/desktop/packs/import",
                entity,
                Map.class);
    }
}
