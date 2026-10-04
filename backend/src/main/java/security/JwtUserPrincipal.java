package com.eiu.capstone.backend.security;

import java.util.List;

public record JwtUserPrincipal(String email, String irn, List<String> roles) {

    public JwtUserPrincipal {
        roles = normalizeRoles(roles);
    }

    public boolean hasRole(String roleName) {
        return roles.contains(JwtRoleNames.normalize(roleName));
    }

    public boolean isStudentOnly() {
        return hasRole(JwtRoleNames.STUDENT) && !hasRole(JwtRoleNames.LECTURER);
    }

    private static List<String> normalizeRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return roles.stream()
                .map(JwtRoleNames::normalize)
                .filter(name -> !name.isEmpty())
                .distinct()
                .toList();
    }
}
