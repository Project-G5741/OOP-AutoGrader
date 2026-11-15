package com.eiu.capstone.backend.desktop;

import java.io.IOException;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.eiu.capstone.backend.security.JwtRoleNames;
import com.eiu.capstone.backend.security.JwtUserPrincipal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Profile({"desktop", "desktop-lecturer"})
public class DesktopAuthenticationFilter extends OncePerRequestFilter {

    private final Environment environment;

    public DesktopAuthenticationFilter(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        JwtUserPrincipal principal;
        if (environment.acceptsProfiles(org.springframework.core.env.Profiles.of("desktop-lecturer"))) {
            principal = new JwtUserPrincipal(
                    DesktopLecturerLocalIdentity.EMAIL,
                    DesktopLecturerLocalIdentity.IRN,
                    List.of(JwtRoleNames.LECTURER));
        } else {
            principal = new JwtUserPrincipal(
                    DesktopLocalIdentity.EMAIL,
                    DesktopLocalIdentity.IRN,
                    List.of(JwtRoleNames.STUDENT));
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
