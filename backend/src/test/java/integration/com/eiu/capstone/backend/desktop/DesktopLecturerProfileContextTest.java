package integration.com.eiu.capstone.backend.desktop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.eiu.capstone.backend.EiuCapstoneBackendApplication;
import com.eiu.capstone.backend.config.DesktopSecurityConfig;
import com.eiu.capstone.backend.desktop.DesktopAuthenticationFilter;
import com.eiu.capstone.backend.desktop.DesktopLecturerLocalIdentity;
import com.eiu.capstone.backend.desktop.DesktopPlagiarismService;
import com.eiu.capstone.backend.desktop.DesktopRosterImportService;
import com.eiu.capstone.backend.security.JwtRoleNames;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.bulk.LecturerBulkGradingService;

@SpringBootTest(classes = EiuCapstoneBackendApplication.class)
@ActiveProfiles("desktop-lecturer")
@TestPropertySource(properties = {
        "jwt.secret=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "app.desktop.install-dir=${java.io.tmpdir}/oop-autograder-desktop-lecturer-test",
        "spring.datasource.url=jdbc:h2:mem:desktoplecturerctx;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DesktopLecturerProfileContextTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Environment environment;

    @Autowired
    private DesktopAuthenticationFilter desktopAuthenticationFilter;

    @Test
    void contextLoads_withDesktopLecturerProfile() {
        assertTrue(environment.acceptsProfiles(org.springframework.core.env.Profiles.of("desktop-lecturer")));
        assertFalse(environment.acceptsProfiles(org.springframework.core.env.Profiles.of("desktop")));
        assertNotNull(context.getBean(DesktopSecurityConfig.class));
        assertNotNull(context.getBean(DesktopPlagiarismService.class));
        assertNotNull(context.getBean(DesktopRosterImportService.class));
        assertNotNull(context.getBean(LecturerBulkGradingService.class));
    }

    @Test
    void authenticationFilter_exposesLecturerPrincipal() throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        desktopAuthenticationFilter.doFilter(request, response, chain);

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertTrue(principal instanceof JwtUserPrincipal);
        JwtUserPrincipal jwt = (JwtUserPrincipal) principal;
        assertTrue(DesktopLecturerLocalIdentity.EMAIL.equals(jwt.email()));
        assertTrue(DesktopLecturerLocalIdentity.IRN.equals(jwt.irn()));
        List<String> roles = jwt.roles();
        assertTrue(roles.contains(JwtRoleNames.LECTURER));
        assertFalse(roles.contains(JwtRoleNames.STUDENT));
        SecurityContextHolder.clearContext();
    }
}
