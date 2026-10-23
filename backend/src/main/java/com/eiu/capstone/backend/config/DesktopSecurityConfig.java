package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.eiu.capstone.backend.desktop.DesktopAuthenticationFilter;

@Configuration
@Profile("desktop")
public class DesktopSecurityConfig {

    private final DesktopAuthenticationFilter desktopAuthenticationFilter;

    public DesktopSecurityConfig(DesktopAuthenticationFilter desktopAuthenticationFilter) {
        this.desktopAuthenticationFilter = desktopAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain desktopFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/index.html", "/api/desktop/status").permitAll()
                        .requestMatchers("/api/labs/**", "/api/submissions/**", "/api/students/**").permitAll()
                        .anyRequest().permitAll())
                .addFilterBefore(desktopAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
