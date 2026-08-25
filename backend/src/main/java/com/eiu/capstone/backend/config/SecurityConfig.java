package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.eiu.capstone.backend.security.JwtAuthenticationFilter;
import com.eiu.capstone.backend.security.JwtRoleNames;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                    .accessDeniedHandler(new AccessDeniedHandlerImpl()))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/").permitAll()
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/users/change-password")
                        .hasAnyRole(JwtRoleNames.STUDENT, JwtRoleNames.LECTURER)
                    .requestMatchers("/api/users/**").hasRole(JwtRoleNames.LECTURER)
                    .requestMatchers("/api/lecturer/**").hasRole(JwtRoleNames.LECTURER)
                    .requestMatchers("/api/analytics/**", "/api/master-data/**", "/api/terms/**")
                        .hasRole(JwtRoleNames.LECTURER)
                    .requestMatchers(HttpMethod.GET, "/api/labs/*/statistics").hasRole(JwtRoleNames.LECTURER)
                    .requestMatchers(HttpMethod.GET, "/api/labs/*/submissions").hasRole(JwtRoleNames.LECTURER)
                    .requestMatchers(HttpMethod.GET, "/api/labs/*/submissions/export").hasRole(JwtRoleNames.LECTURER)
                    .requestMatchers(HttpMethod.GET, "/api/labs/*/students/*/attempts").hasRole(JwtRoleNames.LECTURER)
                    .requestMatchers(HttpMethod.GET, "/api/labs/*/challenges/*/students")
                        .hasRole(JwtRoleNames.LECTURER)
                    .requestMatchers("/api/labs/**").hasAnyRole(JwtRoleNames.STUDENT, JwtRoleNames.LECTURER)
                    .requestMatchers("/api/submissions/**", "/api/students/**").hasRole(JwtRoleNames.STUDENT)
                    .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
