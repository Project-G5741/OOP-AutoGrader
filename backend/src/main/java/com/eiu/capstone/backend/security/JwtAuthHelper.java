package com.eiu.capstone.backend.security;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.UserAccountRepository;

@Component
public class JwtAuthHelper {

    private final UserAccountRepository userAccountRepository;

    public JwtAuthHelper(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccount requireActiveUser(JwtUserPrincipal principal) {
        if (principal == null || principal.email() == null || principal.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        UserAccount user = userAccountRepository.findByEmail(principal.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
        if (!user.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is inactive");
        }
        return user;
    }

    /**
     * Students may only access their own data; lecturers may read any studentId.
     * When studentId is omitted, students default to self; lecturers must supply studentId.
     */
    public UUID resolveStudentScope(JwtUserPrincipal principal, UUID requestedStudentId) {
        UserAccount user = requireActiveUser(principal);
        boolean lecturer = principal.hasRole(JwtRoleNames.LECTURER);
        boolean student = principal.hasRole(JwtRoleNames.STUDENT);

        if (!lecturer && !student) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }

        if (requestedStudentId == null) {
            if (student && !lecturer) {
                return user.getId();
            }
            if (lecturer) {
                return null;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studentId is required");
        }

        if (!requestedStudentId.equals(user.getId()) && !lecturer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot access another student's data");
        }
        return requestedStudentId;
    }
}
