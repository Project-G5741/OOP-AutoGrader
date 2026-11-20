# Operator SQL archive (`docs/sql/`)

**Source of truth for cloud PostgreSQL schema patches:** startup `*SchemaMigrator` beans under `backend/.../config/` (`@Profile("!desktop & !desktop-lecturer")`, except where noted). Files here are the **operator mirror / archive** — apply on Neon when operators need a one-shot SQL Editor path, or keep for empty-DB reconstruction. New cloud schema changes must ship as a migrator first; add a matching dated `.sql` only as the mirror.

Desktop H2 uses Hibernate `ddl-auto: update` plus desktop-only migrators (`Desktop*`). Do not introduce Flyway.

After applying a mirror SQL that a migrator already covers, restart the API (or rely on next boot) so caches invalidate where migrators do so.

## Inventory

| SQL file | Owner | Notes |
|---|---|---|
| `2026-10-13-schema-baseline-reconstructed.sql` | baseline | Empty-DB reconstruction from JPA + operator SQL; not a live dump |
| `2026-08-07-analytics-indexes.sql` | manual-operator-only | Indexes; no startup migrator |
| `2026-08-09-testcase-grading-schema.sql` | superseded | Historical OT schema; see `TestcaseSchemaMigrator` + later OT SQL |
| `2026-08-10-master-data-categories.sql` | manual-operator-only | Seed / master data |
| `2026-08-11-car-challenge-testcases.sql` | manual-operator-only | Lab content seed |
| `2026-08-11-operational-testcase-grading.sql` | superseded | Early OT; see `TestcaseSchemaMigrator` |
| `2026-08-11-operational-testcase-seed-sample.sql` | manual-operator-only | Sample seed |
| `2026-08-11-operation-test-io-card.sql` | superseded | Historical OT display |
| `2026-08-11-testcase-invocation-receiver.sql` | `TestcaseSchemaMigrator` | Receiver columns covered on boot |
| `2026-08-12-challenge-has-mmd.sql` | manual-operator-only / JPA | Column expected by entities |
| `2026-08-19-drop-lab-weight.sql` | manual-operator-only | One-shot drop |
| `2026-08-19-lab-deadline.sql` | manual-operator-only / JPA | Deadline column |
| `2026-08-19-plagiarism.sql` | manual-operator-only / JPA | Plagiarism tables |
| `2026-08-19-scoring-weights.sql` | `ScoringWeightSchemaMigrator` | Challenge/class weights |
| `2026-08-19-term-current.sql` | manual-operator-only / JPA | `term.is_current` |
| `2026-08-21-class-entity-is-static.sql` | manual-operator-only / JPA | Nested static flag |
| `2026-08-21-class-entity-outer-class.sql` | manual-operator-only / JPA | Outer class FK |
| `2026-08-21-pen-penbuilder-rubric.sql` | manual-operator-only | Lab content |
| `2026-08-22-testcase-weight.sql` | `ScoringWeightSchemaMigrator` | Challenge testcase weight |
| `2026-08-23-plagiarism-sources.sql` | manual-operator-only / JPA | Plagiarism sources |
| `2026-09-09-lab-student-visibility.sql` | manual-operator-only / JPA | `student_visible` |
| `2026-09-17-testcase-scenario-steps.sql` | superseded | See unit/composition migrator |
| `2026-09-23-operational-testcase-unit-composition.sql` | `TestcaseSchemaMigrator` | UNIT/COMPOSITION wipe + rewrite |
| `2026-09-25-user-session-version.sql` | `SessionVersionSchemaMigrator` | JWT `sv` column |
| `2026-09-26-rename-submission-testcase-result-key.sql` | manual-operator-only | One-shot rename |
| `2026-10-06-user-account-create-at.sql` | manual-operator-only / JPA | Account timestamp |
| `2026-10-14-challenge-has-class.sql` | manual-operator-only / JPA | Declaration applicability |
| `2026-10-19-drop-user-account-date-of-birth.sql` | manual-operator-only | PII drop |
| `2026-10-20-feedback-inbox.sql` | manual-operator-only / JPA | Feedback tables |
| `2026-10-20-feedback-lecturer-response.sql` | manual-operator-only / JPA | Reply columns |
| `2026-10-20-feedback-soft-archive.sql` | manual-operator-only / JPA | `archived_at` |
| `2026-11-15-feedback-author-reply-cleared.sql` | `FeedbackAuthorReplyClearedSchemaMigrator` | `author_reply_cleared_at` |
| `2026-10-20-scr-mmd-outcomes.sql` | `MmdOutcomesSchemaMigrator` | MMD revisit JSONB |
| `2026-10-20-student-daily-streak.sql` | `StudentStreakSchemaMigrator` | Streak table |
| `2026-10-20-student-mascot-shop.sql` | `StudentStreakSchemaMigrator` | Equipped mascot + purchases |
| `2026-10-21-lab-prefix.sql` | `LabPrefixSchemaMigrator` | `lab.prefix` |
| `2026-10-21-weekly-ranking-payout.sql` | `WeeklyRankingSchemaMigrator` | Ranking ledger |
| `2026-10-22-admin-role.sql` | manual-operator-only | Insert `ADMIN` role row |
| `2026-10-28-field-declaration-modifiers.sql` | `FieldDeclarationModifiersSchemaMigrator` | `is_static` / `is_final` |
| `2026-10-28-testcase-invocation-repeat-count.sql` | `TestcaseSchemaMigrator` | `repeat_count` |
| `2026-10-31-declaration-alias-lists.sql` | `DeclarationAliasListsSchemaMigrator` | Alias child tables |
| `2026-11-04-composition-default-constructor.sql` | `TestcaseSchemaMigrator` | Null `constructor_id` + dispatch class check |
| `2026-11-04-lab4-operational-testcases.sql` | manual-operator-only | Lab 4 content seed (Supabase) |
| `2026-11-04-drop-lab4-tea-default-constructors.sql` | manual-operator-only | Lab 4 content fix |
| `2026-11-12-submission-pillar-scores.sql` | `SubmissionPillarScoreSchemaMigrator` | Pillar % columns |
| `2026-11-14-gacha-tickets.sql` | `StudentStreakSchemaMigrator` | Ticket columns |
| `2026-11-14-gacha-crystals.sql` | `StudentStreakSchemaMigrator` | `crystals` |
| `2026-11-20-gacha-rate-config.sql` | `GachaRateConfigSchemaMigrator` | `gacha_rate_config` singleton + `gacha_pity_count`, `crystal_sourced_points` on streak |
| `2026-11-14-mascot-practice-token.sql` | `MascotPracticeTokenSchemaMigrator` | Practice JWT tables |
| `2026-11-15-student-notification-read.sql` | `StudentNotificationReadSchemaMigrator` | Durable bell read-receipt keys |
| `2026-11-15-student-notification-dismiss.sql` | `StudentNotificationDismissSchemaMigrator` | Durable bell forever-hide dismiss keys |
| `2026-11-16-lab-ai-enabled.sql` | `LabAiEnabledSchemaMigrator` | `lab.ai_enabled` default false |
| `2026-11-16-ai-feedback-cache.sql` | `AiFeedbackCacheSchemaMigrator` | Validated Conceptual hint cache |
| `2026-11-16-ai-feedback-cache-challenge-scope.sql` | `AiFeedbackCacheSchemaMigrator` | Widen `ai_feedback_cache.learning_area` to VARCHAR(64) for challenge keys |
| `2026-11-16-ai-student-credential.sql` | `AiStudentCredentialSchemaMigrator` | Encrypted Gemini key + token budget per student |
| `2026-11-17-ai-credential-key-invalid.sql` | `AiStudentCredentialSchemaMigrator` | `ai_student_credential.key_invalid` failure flag for early toast warning |

## Cloud migrator catalog (regression)

Asserted by `SchemaMigratorCatalogTest`: every listed class is `@Component` and carries a desktop-excluding `@Profile`.
