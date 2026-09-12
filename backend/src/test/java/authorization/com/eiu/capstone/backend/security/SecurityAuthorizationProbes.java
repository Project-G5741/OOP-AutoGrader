package authorization.com.eiu.capstone.backend.security;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Path-only controllers for {@link SecurityAuthorizationTest}. Production controllers are not loaded.
 */
final class SecurityAuthorizationProbes {

    private SecurityAuthorizationProbes() {}

    @RestController
    @RequestMapping("/api/auth")
    static class AuthProbeController {
        @PostMapping("/login")
        ResponseEntity<Void> login() {
            return ResponseEntity.badRequest().build();
        }
    }

    @RestController
    @RequestMapping("/api/lecturer")
    static class LecturerProbeController {
        @GetMapping("/overview")
        String overview() {
            return "ok";
        }
    }

    @RestController
    @RequestMapping("/api/submissions")
    static class SubmissionProbeController {
        @GetMapping("/my-history")
        String history() {
            return "ok";
        }

        @PostMapping("/{labId}/{attemptNumber}/upload")
        String upload(@PathVariable UUID labId, @PathVariable int attemptNumber) {
            return "ok";
        }
    }

    @RestController
    @RequestMapping("/api/presence")
    static class PresenceProbeController {
        @GetMapping
        String presence() {
            return "{\"count\":0}";
        }
    }

    @RestController
    @RequestMapping("/api/labs")
    static class LabProbeController {
        @GetMapping
        String list() {
            return "[]";
        }

        @GetMapping("/{labId}/statistics")
        String statistics(@PathVariable UUID labId) {
            return "ok";
        }
    }
}
