package integration.com.eiu.capstone.backend.desktop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.eiu.capstone.backend.EiuCapstoneBackendApplication;
import com.eiu.capstone.backend.config.DesktopSecurityConfig;
import com.eiu.capstone.backend.config.DesktopStreakSchemaCleanupMigrator;
import com.eiu.capstone.backend.controller.StudentMascotController;
import com.eiu.capstone.backend.controller.StudentStreakController;
import com.eiu.capstone.backend.desktop.DesktopPlagiarismService;
import com.eiu.capstone.backend.plagiarism.PlagiarismService;
import com.eiu.capstone.backend.service.StudentMascotService;
import com.eiu.capstone.backend.service.StudentStreakService;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads_withDesktopProfile() {
        assertNotNull(context.getBean(DesktopSecurityConfig.class));
        assertNotNull(context.getBean(PlagiarismService.class));
        assertNotNull(context.getBean(DesktopPlagiarismService.class));
        assertNotNull(context.getBean(DesktopStreakSchemaCleanupMigrator.class));
        assertFalse(context.getBeanNamesForType(StudentStreakController.class).length > 0);
        assertFalse(context.getBeanNamesForType(StudentMascotController.class).length > 0);
        assertFalse(context.getBeanNamesForType(StudentStreakService.class).length > 0);
        assertFalse(context.getBeanNamesForType(StudentMascotService.class).length > 0);
    }

    @Test
    void streakAndShopTables_areDroppedAfterStartup() {
        Integer streakTables = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = CURRENT_SCHEMA()
                          AND LOWER(table_name) IN ('student_daily_streak', 'student_mascot_purchase')
                        """,
                Integer.class);
        assertTrue(streakTables == null || streakTables == 0);
    }
}
