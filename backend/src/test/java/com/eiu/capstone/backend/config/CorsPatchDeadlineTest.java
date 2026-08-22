package com.eiu.capstone.backend.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

class CorsPatchDeadlineTest {

    private static final class InspectableCorsRegistry extends CorsRegistry {
        Map<String, CorsConfiguration> exposed() {
            return getCorsConfigurations();
        }
    }

    @Test
    void apiCorsAllowsPatchFromFrontend() {
        WebMvcConfigurer configurer = new CorsConfig().webMvcConfigurer();
        InspectableCorsRegistry registry = new InspectableCorsRegistry();
        configurer.addCorsMappings(registry);
        CorsConfiguration cors = registry.exposed().get("/api/**");
        assertNotNull(cors);
        assertTrue(
                cors.getAllowedMethods().contains("PATCH"),
                "PATCH must be allowed so Solution Management can set/clear lab deadlines");
    }
}
