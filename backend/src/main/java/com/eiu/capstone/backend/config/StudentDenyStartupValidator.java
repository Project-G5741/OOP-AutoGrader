package com.eiu.capstone.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Cloud boot fails when Render requires the student deny policy but the flag is off.
 * Desktop profiles omit this bean and force the flag false.
 */
@Component
@Profile("!desktop & !desktop-lecturer")
public class StudentDenyStartupValidator {

    private final boolean enabled;
    private final boolean required;

    public StudentDenyStartupValidator(
            @Value("${app.grading.student-deny.enabled:false}") boolean enabled,
            @Value("${app.grading.student-deny.required:false}") boolean required) {
        this.enabled = enabled;
        this.required = required;
    }

    @PostConstruct
    void validate() {
        validate(enabled, required);
    }

    public static void validate(boolean enabled, boolean required) {
        if (required && !enabled) {
            throw new IllegalStateException(
                    "STUDENT_DENY_REQUIRED=true but app.grading.student-deny.enabled is false; "
                            + "set STUDENT_DENY_ENABLED=true");
        }
    }
}
