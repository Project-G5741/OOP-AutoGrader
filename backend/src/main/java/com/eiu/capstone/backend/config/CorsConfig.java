package com.eiu.capstone.backend.config;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                List<String> origins = Stream.of(
                                "http://localhost:5174",
                                "http://127.0.0.1:5174",
                                "http://localhost:5173",
                                "http://127.0.0.1:5173"
                        )
                        .collect(Collectors.toList());

                if (allowedOrigins != null && !allowedOrigins.isBlank()) {
                    origins = Stream.concat(origins.stream(), Stream.of(allowedOrigins.split(",")))
                            .map(String::trim)
                            .filter(origin -> !origin.isEmpty())
                            .distinct()
                            .collect(Collectors.toList());
                }

                registry.addMapping("/api/**")
                        .allowedOrigins(origins.toArray(new String[0]))
                        .allowedMethods("GET", "POST", "OPTIONS", "PUT", "DELETE")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}
