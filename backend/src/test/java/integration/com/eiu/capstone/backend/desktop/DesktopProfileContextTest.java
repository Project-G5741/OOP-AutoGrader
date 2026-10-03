package integration.com.eiu.capstone.backend.desktop;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.eiu.capstone.backend.EiuCapstoneBackendApplication;
import com.eiu.capstone.backend.config.DesktopSecurityConfig;
import com.eiu.capstone.backend.desktop.DesktopPlagiarismService;
import com.eiu.capstone.backend.plagiarism.PlagiarismService;

@SpringBootTest(classes = EiuCapstoneBackendApplication.class)
@ActiveProfiles("desktop")
@TestPropertySource(properties = {
        "jwt.secret=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "app.desktop.install-dir=${java.io.tmpdir}/oop-autograder-desktop-test",
        "spring.datasource.url=jdbc:h2:mem:desktopctx;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DesktopProfileContextTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads_withDesktopProfile() {
        assertNotNull(context.getBean(DesktopSecurityConfig.class));
        assertNotNull(context.getBean(PlagiarismService.class));
        assertNotNull(context.getBean(DesktopPlagiarismService.class));
    }
}
