package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.eiu.capstone.backend.security.BurstLimitFilter;
import com.eiu.capstone.backend.security.JwtAuthenticationFilter;
import com.eiu.capstone.backend.security.JwtRoleNames;

@Configuration
@Profile("!desktop")
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectProvider<BurstLimitFilter> burstLimitFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectProvider<BurstLimitFilter> burstLimitFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.burstLimitFilter = burstLimitFilter;
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
                    .requestMatchers(HttpMethod.GET, "/api/presence/count").permitAll()
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/users/change-password")
                        .hasAnyRole(JwtRoleNames.STUDENT, JwtRoleNames.LECTURER, JwtRoleNames.ADMIN)
                    .requestMatchers("/api/users/**").hasAnyRole(JwtRoleNames.LECTURER, JwtRoleNames.ADMIN)
                    .requestMatchers("/api/lecturer/**").hasAnyRole(JwtRoleNames.LECTURER, JwtRoleNames.ADMIN)
                    .requestMatchers("/api/analytics/**", "/api/master-data/**", "/api/terms/**")
                        .hasAnyRole(JwtRoleNames.LECTURER, JwtRoleNames.ADMIN)
                    .requestMatchers(HttpMethod.GET, "/api/labs/*/statistics")
                        .hasAnyRole(JwtRoleNames.LECTURER, JwtRoleNames.ADMIN)
                    .requestMatchers(HttpMethod.GET, "/api/labs/*/submissions")
                        .hasAnyRole(JwtRoleNames.LECTURER, JwtRoleNames.ADMIN)
                    .requestMatchers(HttpMethod.GET, "/api/labs/*/submissions/export")
                        .hasAnyRole(JwtRoleNames.LECTURER, JwtRoleNames.ADMIN)
                    .requestMatchers(HttpMethod.GET, "/api/labs/*/students/*/attempts")
                        .hasAnyRole(JwtRoleNames.LECTURER, JwtRoleNames.ADMIN)
                    .requestMatchers(HttpMethod.GET, "/api/labs/*/challenges/*/students")
                        .hasAnyRole(JwtRoleNames.LECTURER, JwtRoleNames.ADMIN)
                    .requestMatchers("/api/labs/**")
                        .hasAnyRole(JwtRoleNames.STUDENT, JwtRoleNames.LECTURER, JwtRoleNames.ADMIN)
                    .requestMatchers("/api/submissions/**", "/api/students/**")
                        .hasAnyRole(JwtRoleNames.STUDENT, JwtRoleNames.ADMIN)
                    .requestMatchers("/api/feedback/**")
                        .hasAnyRole(JwtRoleNames.STUDENT, JwtRoleNames.LECTURER, JwtRoleNames.ADMIN)
                    .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        BurstLimitFilter burst = burstLimitFilter.getIfAvailable();
        if (burst != null) {
            http.addFilterAfter(burst, JwtAuthenticationFilter.class);
        }
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
