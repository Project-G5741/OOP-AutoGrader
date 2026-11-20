package unit.com.eiu.capstone.backend.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Guards the cloud schema SoT: named startup migrators stay present and
 * desktop-excluding. Inventory mirror: {@code docs/sql/README.md}.
 */
class SchemaMigratorCatalogTest {

    private static final List<String> CLOUD_MIGRATORS = List.of(
            "com.eiu.capstone.backend.config.DeclarationAliasListsSchemaMigrator",
            "com.eiu.capstone.backend.config.DesktopPackVersionSchemaMigrator",
            "com.eiu.capstone.backend.config.FeedbackAuthorReplyClearedSchemaMigrator",
            "com.eiu.capstone.backend.config.FieldDeclarationModifiersSchemaMigrator",
            "com.eiu.capstone.backend.config.FieldDeclarationNullabilitySchemaMigrator",
            "com.eiu.capstone.backend.config.LabAiEnabledSchemaMigrator",
            "com.eiu.capstone.backend.config.LabPrefixSchemaMigrator",
            "com.eiu.capstone.backend.config.AiAssistedGradingSchemaMigrator",
            "com.eiu.capstone.backend.config.AiFeedbackCacheSchemaMigrator",
            "com.eiu.capstone.backend.config.AiStudentCredentialSchemaMigrator",
            "com.eiu.capstone.backend.config.MascotPracticeTokenSchemaMigrator",
            "com.eiu.capstone.backend.config.MmdOutcomesSchemaMigrator",
            "com.eiu.capstone.backend.config.ScoringWeightSchemaMigrator",
            "com.eiu.capstone.backend.config.SessionVersionSchemaMigrator",
            "com.eiu.capstone.backend.config.StudentNotificationReadSchemaMigrator",
            "com.eiu.capstone.backend.config.StudentNotificationDismissSchemaMigrator",
            "com.eiu.capstone.backend.config.StudentStreakSchemaMigrator",
            "com.eiu.capstone.backend.config.GachaRateConfigSchemaMigrator",
            "com.eiu.capstone.backend.config.SubmissionPillarScoreSchemaMigrator",
            "com.eiu.capstone.backend.config.TestcaseSchemaMigrator",
            "com.eiu.capstone.backend.config.WeeklyRankingSchemaMigrator");

    @Test
    void cloudMigratorsAreComponentAndExcludeDesktop() throws ClassNotFoundException {
        for (String name : CLOUD_MIGRATORS) {
            Class<?> type = Class.forName(name);
            assertNotNull(type.getAnnotation(Component.class), name + " missing @Component");
            Profile profile = type.getAnnotation(Profile.class);
            assertNotNull(profile, name + " missing @Profile");
            String joined = String.join(" ", Arrays.asList(profile.value()));
            assertTrue(
                    joined.contains("!desktop"),
                    name + " @Profile must exclude desktop; was: " + joined);
        }
    }
}
