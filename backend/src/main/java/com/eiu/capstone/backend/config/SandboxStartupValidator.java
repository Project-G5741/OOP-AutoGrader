package com.eiu.capstone.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Fail closed on cloud boot when sandbox is enabled without runner credentials,
 * or when {@code SANDBOX_REQUIRED=true} but sandbox is off / incomplete.
 * Desktop profiles omit this bean.
 */
@Component
@Profile("!desktop & !desktop-lecturer")
public class SandboxStartupValidator {

    private final boolean sandboxEnabled;
    private final boolean sandboxRequired;
    private final String runnerUrl;
    private final String runnerToken;

    public SandboxStartupValidator(
            @Value("${app.grading.sandbox.enabled:false}") boolean sandboxEnabled,
            @Value("${app.grading.sandbox.required:false}") boolean sandboxRequired,
            @Value("${app.grading.sandbox.runner-url:}") String runnerUrl,
            @Value("${app.grading.sandbox.runner-token:}") String runnerToken) {
        this.sandboxEnabled = sandboxEnabled;
        this.sandboxRequired = sandboxRequired;
        this.runnerUrl = runnerUrl == null ? "" : runnerUrl.trim();
        this.runnerToken = runnerToken == null ? "" : runnerToken.trim();
    }

    @PostConstruct
    void validate() {
        validate(sandboxEnabled, sandboxRequired, runnerUrl, runnerToken);
    }

    /** Pure check for unit tests and {@link #validate()} boot hook. */
    public static void validate(boolean enabled, boolean required, String url, String token) {
        String trimmedUrl = url == null ? "" : url.trim();
        String trimmedToken = token == null ? "" : token.trim();
        if (required && !enabled) {
            throw new IllegalStateException(
                    "SANDBOX_REQUIRED=true but app.grading.sandbox.enabled is false; "
                            + "set SANDBOX_ENABLED=true with SANDBOX_RUNNER_URL and SANDBOX_RUNNER_TOKEN");
        }
        if (enabled && (trimmedUrl.isBlank() || trimmedToken.isBlank())) {
            throw new IllegalStateException(
                    "app.grading.sandbox.enabled=true requires non-blank SANDBOX_RUNNER_URL and SANDBOX_RUNNER_TOKEN");
        }
        if (required && (trimmedUrl.isBlank() || trimmedToken.isBlank())) {
            throw new IllegalStateException(
                    "SANDBOX_REQUIRED=true requires non-blank SANDBOX_RUNNER_URL and SANDBOX_RUNNER_TOKEN");
        }
    }
}
