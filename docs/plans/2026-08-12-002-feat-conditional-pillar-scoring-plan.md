---
title: Conditional Pillar Scoring - Plan
type: feat
date: 2026-08-12
topic: conditional-pillar-scoring
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Conditional Pillar Scoring - Plan

## Goal Capsule

- **Objective:** Let challenges opt out of MMD grading via a `has_mmd` flag, and rebalance challenge scoring so it averages only the pillars applicable to that challenge — Declaration Test always counts, MMD and Operation Test count only when applicable.
- **Product authority:** this plan (Product Contract below); Product Contract preservation: unchanged from the originating brainstorm.
- **Execution profile:** software implementation, backend (schema + grading engine) and frontend (Lab Structure Editor, student results).
- **Stop conditions:** none outstanding — all product and technical questions resolved during planning (see Planning Contract).
- **Tail ownership:** implementer runs `mvn test` (backend) and `npm run build` (frontend) before calling units done; no CI pipeline changes required.

## Product Contract

### Summary

Add a per-challenge `has_mmd` flag (default `true`, editable in the Lab Structure Editor) so lecturers can mark challenges that don't require an MMD diagram. Generalize challenge scoring to average only the pillars that apply to a given challenge: Declaration Test always counts, MMD counts only when `has_mmd` is true, and Operation Test counts only when the challenge has at least one operational testcase — collapsing to 50/50 across two pillars, or 100% Declaration Test, as pillars drop out.

### Key Decisions

- **`has_mmd` is editable in the Lab Structure Editor, not DB-only or creation-time-only.** Keeps configuration in the same surface lecturers already use to manage challenge structure. *(session-settled: user-directed — chosen over DB-only/admin tooling and creation-time-only: both would fragment where lecturers manage challenge config.)* Governs R1, R2.
- **Toggling `has_mmd` does not retroactively rescore existing submissions.** Avoids surprise score changes on historical submissions from a config change with no explicit re-grade action. *(session-settled: user-directed — chosen over automatic rescoring on toggle.)* Governs R6.
- **`has_mmd` defaults to `true` for both existing (backfilled) and newly created challenges.** Preserves current three-pillar scoring behavior unless a lecturer explicitly opts out. *(session-settled: user-directed — chosen over defaulting new challenges to `false`.)* Governs R1.
- **Inapplicable pillars are hidden entirely from the student result tab navigation — applied identically to MMD and Operation Test.** *(revised 2026-08-12: user-directed — supersedes the original "render as not scored, never hidden" decision after seeing the not-scored state still surface misleading per-item detail; hiding the tab is simpler and avoids that confusion.)* Governs R7, R8.
- **The Lab Structure Editor allows saving `has_mmd=false` even when MMD relations are already authored for that challenge, with no warning or block.** The relations remain valid data; they're simply unscored while the flag is off. *(session-settled: user-directed — chosen over blocking or warning on save.)* Governs R2.
- **No new lecturer-triggered regrade endpoint.** Re-uploading a submission to the same attempt already re-runs full grading with the current `has_mmd` configuration. *(session-settled: user-directed — chosen over building a dedicated regrade action: matches the brainstorm's "manual regrade" decision and the existing re-upload flow already satisfies it.)* Governs R6.

### Actors

- A1. **Lecturer** — configures `has_mmd` per challenge via the Lab Structure Editor.
- A2. **Grading engine** — computes challenge scores at grade time using only the pillars applicable to that challenge.
- A3. **Student** — views challenge results; tabs for inapplicable pillars don't appear in the navigation.

Key Flows are omitted: this change is a scoring-policy and configuration adjustment layered on the existing upload → grade → display flow, not a new multi-step interaction. Actors, Requirements, and Acceptance Examples together specify the behavior without a flow diagram.

### Requirements

**Schema & configuration**

- R1. A `has_mmd` boolean field exists on the challenge entity, defaulting to `true` for both existing (backfilled) and newly created challenges.
- R2. A lecturer can view and toggle `has_mmd` for a challenge from the Lab Structure Editor at any time, independent of whether MMD relations are authored for that challenge.

**Scoring logic**

- R3. A challenge's score is the average of only the pillars applicable to it: Declaration Test (always applicable), MMD (applicable when `has_mmd` is `true`), and Operation Test (applicable when the challenge has at least one operational testcase recorded in the database).
- R4. When exactly two pillars are applicable, each contributes 50% of the challenge score.
- R5. When only Declaration Test is applicable (`has_mmd` is `false` and the challenge has no operational testcases), the challenge score equals the Declaration Test pillar percentage.
- R6. Changing `has_mmd` for a challenge does not automatically recompute scores for submissions already graded under the prior configuration.

**Result display**

- R7. When MMD is not applicable to a challenge (`has_mmd` is `false`), its MMD tab does not appear in the student results tab navigation for that challenge.
- R8. When Operation Test is not applicable to a challenge (no testcases exist), its Operation Test tab does not appear in the student results tab navigation for that challenge, matching the treatment in R7.

### Acceptance Examples

- AE1. **Covers R3, R5.** Given a challenge with `has_mmd=false` and zero operational testcases, when a student submits, then the challenge score equals the Declaration Test pillar percentage alone.
- AE2. **Covers R3, R4.** Given a challenge with `has_mmd=false` and at least one operational testcase, when a student submits, then the challenge score is the average of Declaration Test and Operation Test only (50/50).
- AE3. **Covers R3, R4.** Given a challenge with `has_mmd=true` and zero operational testcases, when a student submits, then the challenge score is the average of Declaration Test and MMD only (50/50).
- AE4. **Covers R3.** Given a challenge with `has_mmd=true` and at least one operational testcase, when a student submits, then the challenge score is the average of all three pillars (unchanged from current behavior).
- AE5. **Covers R6.** Given a challenge that already has graded submissions, when a lecturer toggles `has_mmd` afterward, then previously stored scores for that challenge stay unchanged until a new submission or re-upload occurs.

### Scope Boundaries

- Automatic rescoring of historical submissions when `has_mmd` changes — a lecturer or student may re-upload to re-grade under the new configuration.
- A dedicated lecturer-triggered regrade endpoint — out of scope per the Key Decisions above.
- Validation, warning, or blocking when a lecturer sets `has_mmd=false` while MMD relations are still authored for the challenge — allowed silently.
- Declaration Test itself — it stays guaranteed and unconditionally scored; this plan does not change how it's computed.

### Sources / Research

- `backend/src/main/java/com/eiu/capstone/backend/model/Challenge.java:17-55` — current challenge entity columns (no `has_mmd` today).
- `backend/src/main/java/com/eiu/capstone/backend/grading/scoring/PillarScoreAggregator.java:44-49` — fixed three-pillar `/3` averaging that R3-R5 generalize.
- `backend/src/main/java/com/eiu/capstone/backend/grading/pipeline/GradingPipeline.java:45-96` — call site invoking the aggregator and computing `fullyCorrect`.
- `backend/src/main/java/com/eiu/capstone/backend/grading/pipeline/TestcaseGrader.java:59-61` — existing `testcases().isEmpty()` short-circuit to a 0% pillar, reused as the testcase-applicability check.
- `backend/src/main/java/com/eiu/capstone/backend/grading/rubric/ChallengeRubric.java` — rubric record that needs a `hasMmd` field to carry the flag to grade time.
- `backend/src/main/java/com/eiu/capstone/backend/grading/LabResultAssembler.java:119-126` — builds the `scores` map with `Map.of`, consumed by `ChallengeDetailBundleDTO`.
- `frontend/src/components/ui/ScorePill.jsx:30-34` and `frontend/src/components/student/StudentUI.jsx:83-88` — `bundleScore`/`hasScoreToShow` already treat a `null` pillar score as "fall back to a locally computed percentage," which is why applicability needs its own signal (see KTD4).
- `frontend/src/components/lecturer/structure/MmdRelationsPanel.jsx:52-64` and `frontend/src/pages/SolutionManagement.jsx:382-388` — challenge-level editor panel where the `has_mmd` toggle belongs.
- `docs/sql/2026-08-11-operation-test-io-card.sql` — recent operator-run `ALTER TABLE ... ADD COLUMN IF NOT EXISTS ... BOOLEAN NOT NULL DEFAULT ...` convention to follow for the new migration.

---

## Planning Contract

### Key Technical Decisions

- KTD1. **`PillarScoreAggregator.challengePercentage` divides by the count of applicable pillars, not a fixed 3.** Declaration Test is always applicable; MMD and Operation Test are each conditionally applicable. Governs R3, R4, R5.
- KTD2. **Skip invoking `MmdPillarGrader` entirely for a challenge with `has_mmd=false`**, short-circuiting to a canonical zero/empty MMD result, rather than computing and discarding a real MMD score. Cheaper, and avoids persisting a misleading MMD percentage for a pillar nobody configured. Governs R3, R7.
- KTD3. **Testcase applicability reuses the existing `challengeRubric.testcases().isEmpty()` check** `TestcaseGrader` already uses for its 0% short-circuit — no new testcase-side grading logic, only the aggregator and display layers change. Governs R3, R8.
- KTD4. **Pillar applicability travels to the frontend as an explicit signal, not inferred from a null or zero score value.** Research found the existing `bundleScore` helper treats a `null` pillar score as "fall back to a locally computed percentage" (used for the MMD and Class tabs' local-data fallback) — reusing `null` for "not applicable" would silently show a real computed percentage instead of "not scored." The result payload carries applicability alongside the score values. Governs R7, R8.
- KTD5. **`fullyCorrect` in `GradingPipeline` requires 100% only on applicable pillars**, mirroring the score formula rather than a literal three-pillar check. Governs R3.

### Assumptions

- No automated frontend test suite exists (`frontend/AGENTS.md` verification is `npm run build` + manual); U4 and U5 verification is manual, matching repo convention.
- The migration file follows the existing `docs/sql/` operator-run convention (idempotent `ADD COLUMN IF NOT EXISTS`); no Flyway/Liquibase is introduced.

---

## Implementation Units

### U1. Schema and rubric plumbing for `has_mmd`

**Goal:** Add the `has_mmd` column and thread it from the entity through the structure DTOs, save/load flow, and grading rubric so it's available at grade time.

**Requirements:** R1, R2

**Dependencies:** none

**Files:**
- `docs/sql/2026-08-12-challenge-has-mmd.sql` (new)
- `backend/src/main/java/com/eiu/capstone/backend/model/Challenge.java`
- `backend/src/main/java/com/eiu/capstone/backend/DTO/rubric/ChallengeStructureDTO.java`
- `backend/src/main/java/com/eiu/capstone/backend/service/LabStructureService.java`
- `backend/src/main/java/com/eiu/capstone/backend/grading/rubric/ChallengeRubric.java`
- `backend/src/main/java/com/eiu/capstone/backend/grading/rubric/LabRubricService.java`
- `backend/src/test/java/com/eiu/capstone/backend/service/LabStructureServiceSaveTest.java`

**Approach:**
1. Add `docs/sql/2026-08-12-challenge-has-mmd.sql`: `ALTER TABLE challenge ADD COLUMN IF NOT EXISTS has_mmd BOOLEAN NOT NULL DEFAULT true;` — same shape as `docs/sql/2026-08-11-operation-test-io-card.sql`.
2. Add a `hasMmd` field (default `true`) with getter/setter to `Challenge`.
3. Add a `hasMmd` record component to `ChallengeStructureDTO`.
4. In `LabStructureService`: `toChallengeDto` includes `hasMmd`; `upsertChallenge` sets it from the incoming DTO (default `true` when the payload omits it, e.g. legacy clients); `saveLabStructure`'s save-echo construction carries it through.
5. Add a `hasMmd` field to the `ChallengeRubric` record; keep the existing legacy constructor (5 args) defaulting it to `true` so existing call sites and tests compile unchanged.
6. In `LabRubricService`, read `challenge.isHasMmd()` when building each `ChallengeRubric`.

**Test scenarios:**
- Happy path: saving a challenge with `hasMmd=false` persists and round-trips through the structure read endpoint.
- Edge case: an existing challenge row with the DB default (no explicit value set before migration) loads as `true`.
- Edge case: creating a new challenge without specifying `hasMmd` in the payload defaults to `true`.

**Verification:** `mvn test` (extend `LabStructureServiceSaveTest` for the round-trip); manual: `PUT` a lab structure with `hasMmd=false` on one challenge, then `GET` it back and confirm the value persisted.

---

### U2. Applicable-pillar scoring in the grading pipeline

**Goal:** Change the challenge score to average only applicable pillars, and skip MMD grading when `has_mmd` is false.

**Requirements:** R3, R4, R5

**Dependencies:** U1

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/grading/scoring/PillarScoreAggregator.java`
- `backend/src/main/java/com/eiu/capstone/backend/grading/pipeline/GradingPipeline.java`
- `backend/src/test/java/com/eiu/capstone/backend/grading/scoring/PillarScoreAggregatorTest.java`

**Approach:**
1. Change `PillarScoreAggregator.challengePercentage` (KTD1) to divide by the count of applicable pillars instead of a fixed 3 — pass applicability alongside the three percentages (e.g. `hasMmd`/`hasTestcase` booleans, class always applicable).
2. In `GradingPipeline.gradeChallenge`, read `challengeRubric.hasMmd()`. When `false`, skip submitting the MMD future and use a canonical zero/empty `MmdPillarResult` instead of calling `mmdPillarGrader.grade` (KTD2).
3. Testcase applicability is `!challengeRubric.testcases().isEmpty()` (KTD3) — no `TestcaseGrader` change needed.
4. Update the `fullyCorrect` computation (KTD5) to check 100% only on applicable pillars.

**Test scenarios:**
- Happy path: `hasMmd=true` + testcases present → average of all three pillars (existing behavior unchanged). Covers AE4.
- Edge case: `hasMmd=false` + testcases present → average of class + testcase only (50/50). Covers AE2.
- Edge case: `hasMmd=true` + no testcases → average of class + MMD only (50/50). Covers AE3.
- Edge case: `hasMmd=false` + no testcases → score equals class pillar alone. Covers AE1.
- Edge case: when `hasMmd=false`, `MmdPillarGrader.grade` is never invoked for that challenge.

**Verification:** `mvn test`; `PillarScoreAggregatorTest` covers all four applicability combinations above.

---

### U3. Pillar-applicability signal in the result payload

**Goal:** Carry which pillars were applicable to a challenge through to the API response, alongside their scores.

**Requirements:** R7, R8

**Dependencies:** U2

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/grading/pipeline/GradingPipeline.java` (result record)
- `backend/src/main/java/com/eiu/capstone/backend/grading/GradingService.java` (challenge-scoring section)
- `backend/src/main/java/com/eiu/capstone/backend/grading/LabResultAssembler.java`
- `backend/src/main/java/com/eiu/capstone/backend/DTO/ChallengeDetailBundleDTO.java`

**Approach:**
1. Thread the applicability booleans computed in U2 (MMD, testcase — class is always applicable) through `ChallengePipelineResult` to `GradingService`'s per-challenge score breakdown.
2. Extend `LabResultAssembler`'s `scores` construction to also emit the applicability signal on `ChallengeDetailBundleDTO`, per KTD4 (do not rely on a null or zero score value to imply inapplicable).

**Test scenarios:**
- Happy path: the bundle for a `hasMmd=true`, testcases-present challenge reports both pillars applicable.
- Edge case: the bundle for a `hasMmd=false` challenge reports MMD inapplicable.
- Edge case: the bundle for a zero-testcase challenge reports Operation Test inapplicable.

**Verification:** `mvn test`; manual: upload a submission to a `hasMmd=false` challenge and inspect the upload response JSON for the applicability signal.

---

### U4. Lab Structure Editor: `has_mmd` toggle

**Goal:** Let a lecturer view and change `has_mmd` per challenge.

**Requirements:** R2

**Dependencies:** U1

**Files:**
- `frontend/src/components/lecturer/structure/MmdRelationsPanel.jsx`
- `frontend/src/pages/SolutionManagement.jsx`

**Approach:**
1. Add a checkbox/toggle to `MmdRelationsPanel`'s header block, wired through the existing `onChange({ ...challenge, ... })` pattern already used for relations (e.g. `onChange({ ...challenge, hasMmd: next })`).
2. Default `hasMmd` to `true` when a new challenge is created in `SolutionManagement`.
3. No validation blocks saving when relations exist and `hasMmd` is false, per the Key Decision above.

**Test scenarios:**
- Happy path: toggling `has_mmd` off and saving persists the change; reloading the lab shows it off.
- Edge case: a challenge with existing MMD relations can still be saved with `has_mmd` off, with no warning or block.
- Edge case: a newly created challenge defaults to `has_mmd` on.

**Verification:** Manual — Lab Structure Editor save/reload round-trip (no frontend automated test suite; `npm run build` must still succeed).

---

### U5. Student results: hide inapplicable pillar tabs

**Goal:** Remove the MMD/Operation Test tab from the student result tab navigation entirely when that pillar is inapplicable to the current challenge, instead of showing a "not scored" state within the tab.

**Requirements:** R7, R8

**Dependencies:** U3

**Files:**
- `frontend/src/components/student/StudentUI.jsx`
- `frontend/src/components/ui/ScorePill.jsx`

**Approach:**
1. Read the applicability signal from the bundle (U3, `currentBundle.scoreApplicability`) to compute which of `['mmd', 'class', 'testcase']` tabs are visible for the currently selected challenge. `class` is always visible; `mmd`/`testcase` are visible unless `resultsRevealed` is true and the bundle marks them inapplicable.
2. Filter the tab navigation list to only visible tabs.
3. If `activeTab` refers to a tab that just became hidden (e.g. after switching to a challenge where MMD is inapplicable while the MMD tab was selected), fall back the active tab to the first visible tab.
4. Retired the earlier "not scored" banner/pill approach superseding the previous version of this unit — inapplicable pillars are no longer rendered in any form, they're simply absent from the tab bar.

**Test scenarios:**
- Happy path: a challenge with both pillars applicable shows all three tabs unchanged from current behavior.
- Edge case: a `hasMmd=false` challenge does not show an MMD tab at all; switching to that challenge while MMD was the active tab moves selection to Declaration Test.
- Edge case: a zero-testcase challenge does not show an Operation Test tab at all.
- Edge case: before `resultsRevealed` (no submission yet this session), all three tabs remain visible since applicability isn't known/relevant until graded.

**Verification:** Manual — view a `hasMmd=false` challenge and a zero-testcase challenge in the student UI and confirm the corresponding tab is absent from the tab bar (no frontend automated test suite; `npm run build` must still succeed).

---

## Verification Contract

| Command | Applies to | Notes |
|---|---|---|
| `mvn test` (from `backend/`) | U1, U2, U3 | Covers `LabStructureServiceSaveTest`, `PillarScoreAggregatorTest`, and any new backend tests. Docker build skips tests (`-DskipTests`) — this is a local/CI gate, not part of the deploy image. |
| `npm run build` (from `frontend/`) | U4, U5 | Must succeed; no frontend automated test suite exists in this repo. |
| Manual: Lab Structure Editor save/reload | U1, U4 | Toggle `has_mmd`, save, reload, confirm persisted. |
| Manual: upload to a `has_mmd=false` and to a zero-testcase challenge | U2, U3, U5 | Confirm score matches AE1-AE4 and the student UI hides the corresponding tab per R7/R8. |

## Definition of Done

- `has_mmd` column exists on `challenge` with default `true`; existing and new challenges default to `true` (R1).
- Lecturer can toggle `has_mmd` per challenge in the Lab Structure Editor, independent of authored MMD relations (R2).
- Challenge score is the mean of applicable pillars per R3-R5, verified by AE1-AE4.
- Toggling `has_mmd` does not mutate previously stored scores (R6, AE5); re-upload is the only path to a recomputed score.
- Student results hide the MMD and/or Operation Test tab entirely when inapplicable, rather than showing a not-scored state or a computed/misleading percentage (R7, R8).
- `mvn test` passes; `npm run build` succeeds.
- No dead-end or experimental code from exploration remains in the diff.
