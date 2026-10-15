---
title: Disableable Declaration Pillar - Plan
type: feat
date: 2026-10-14
topic: disableable-declaration-pillar
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Disableable Declaration Pillar - Plan

## Goal Capsule

- **Objective:** Let lecturers turn off the Declaration (Class) pillar per challenge the same way they turn off MMD today, including MMD-only student upload when Declaration and OT both do not apply, and block saving a challenge with zero applicable pillars.
- **Product authority:** this plan (Product Contract below); Product Contract preservation: unchanged from the originating brainstorm (Outstanding Questions resolved into Planning Contract KTDs only).
- **Execution profile:** software implementation — PostgreSQL column + backend grading/structure/OT save + Solution Management toggle + student/lecturer tab hide.
- **Stop conditions:** none outstanding — deferred brainstorm questions resolved in KTDs below.
- **Tail ownership:** implementer runs `mvn test` (backend) and `npm run build` (frontend) before calling units done; operator applies new SQL on Neon when deploying.

## Product Contract

### Summary

Add a per-challenge Declaration applicability toggle (default on) in Solution Management, mirroring “Requires MMD diagram.” When off, Declaration is omitted from scoring and result tabs, class reflection is not graded, and students may submit `.mmd` without `.java` for that challenge when OT also does not apply. Structure (and related) saves reject a challenge with no applicable pillars left.

### Problem Frame

Conditional pillar scoring already lets lecturers disable MMD and drop OT when no testcases exist, but Declaration stays always on. Labs that only need an MMD diagram still force class scoring (and effectively Java upload), which misgrades MMD-only challenges.

### Key Decisions

- **Reuse the existing `has_mmd` pattern for Declaration** — per-challenge flag, default on, Structure Editor toggle, skip grader when off, hide tab, explicit applicability signal. *(session-settled: user-approved — chosen over inferring from an empty class list or a lab-wide switch: matches how lecturers disable MMD today.)* Governs R1, R2, R4, R7, R8.
- **MMD-only upload when Declaration is off and OT does not apply** — students may omit `.java` for that challenge; grading skips compile/reflection for Declaration. *(session-settled: user-directed — chosen over score-only (still require Java): real MMD-only labs should not need Java.)* Governs R9, R10.
- **Block save when a challenge has zero applicable pillars** — at least one of Declaration, MMD, or OT must apply. *(session-settled: user-directed — chosen over allowing a 0% empty challenge.)* Governs R11.
- **OT still requires Java when OT applies, even if Declaration is off.** MMD-only upload is only for the no-OT case. Governs R10.
- **Structure classes may remain when Declaration is off** — needed for MMD relations; only grading and upload rules change. Governs R3.
- **No automatic regrade of past submissions when the flag changes** — same policy as `has_mmd`. Governs R12.

### Actors

- A1. **Lecturer** — toggles Declaration applicability per challenge in Solution Management; saves structure (and OT) under the zero-pillar gate.
- A2. **Grading / upload pipeline** — applies Declaration applicability to compile, class grading, challenge score, and upload file rules.
- A3. **Student** — uploads per challenge rules; sees only applicable pillar tabs.

### Requirements

**Configuration**

- R1. Each challenge has a Declaration applicability flag that defaults to `true` for existing (backfilled) and newly created challenges.
- R2. A lecturer can view and toggle that flag in Solution Management at any time, with the same semantics as “Requires MMD diagram” but placed on challenge chrome beside Class weight (checkbox labeled “Requires Declaration Test”; the MMD toggle remains on the MMD Relations panel).
- R3. Saving with Declaration off is allowed even when the challenge still has authored classes or members; those rows remain for MMD (and future OT) authoring and are simply not graded on the Declaration pillar while the flag is off.

**Scoring & grading**

- R4. Declaration is applicable only when the flag is `true`. Challenge score is the weighted mean of applicable pillars only (Declaration, MMD when `has_mmd`, OT when the challenge has at least one authored operational testcase), using existing pillar weights.
- R5. When Declaration is not applicable, the pipeline does not run class-reflection grading for that challenge and does not treat Declaration as contributing to the challenge total or to `fullyCorrect`.
- R6. When only one pillar is applicable, the challenge score equals that pillar’s percentage (subject to its weight normalization already used for single-pillar cases).
- R12. Changing the Declaration flag does not automatically recompute scores for submissions already graded under the prior configuration.

**Result display**

- R7. When Declaration is not applicable, the Declaration Test tab does not appear in student or lecturer result navigation for that challenge (same hide-tab rule as inapplicable MMD / OT).
- R8. Result payloads carry an explicit Declaration applicability signal alongside scores (not inferred from null/zero), consistent with existing `scoreApplicability` for MMD and OT.

**Upload**

- R9. When Declaration is not applicable and OT is not applicable for a challenge, a student may upload that challenge with `.mmd` and without `.java`; upload must not fail solely for missing Java on that challenge.
- R10. When OT is applicable for a challenge, Java remains required for that challenge even if Declaration is not applicable. When Declaration is applicable, existing Java upload/compile expectations for that challenge remain unchanged.

**Zero-pillar gate**

- R11. Persisting a challenge configuration where Declaration is off, MMD is off (`has_mmd` false), and the challenge has no authored operational testcases must be rejected with a clear lecturer-facing error. The gate applies wherever those flags and OT presence can leave a challenge with zero applicable pillars (structure save and OT save paths as applicable).

### Acceptance Examples

- AE1. **Covers R4, R5, R6, R7, R9.** Given a challenge with Declaration off, `has_mmd=true`, and no OT, when a student uploads only `.mmd` for that challenge, then upload succeeds, Declaration is not scored or shown, and the challenge score equals the MMD pillar percentage.
- AE2. **Covers R4, R10.** Given a challenge with Declaration off, `has_mmd=false`, and at least one OT, when a student uploads, then Java is still required, OT (and only OT) contributes to the challenge score, and the Declaration tab is hidden.
- AE3. **Covers R4.** Given Declaration on, `has_mmd=true`, and OT present, when a student uploads, then all three pillars behave as today.
- AE4. **Covers R11.** Given a lecturer sets Declaration off and `has_mmd=false` on a challenge with no OT, when they save, then the save is rejected and the challenge is not left with zero applicable pillars.
- AE5. **Covers R3, R5.** Given Declaration off but classes still defined for MMD relations, when a student uploads a valid MMD-only submission (no OT), then MMD grades normally and Declaration does not contribute.
- AE6. **Covers R12.** Given graded submissions exist, when a lecturer toggles Declaration off afterward, then stored scores stay unchanged until a new submission or re-upload.

### Scope Boundaries

- Inferring Declaration applicability from an empty class list — out of scope; flag is explicit.
- Lab-wide Declaration kill-switch — out of scope.
- Changing how OT applicability is determined (still: ≥1 authored OT row).
- Automatic historical regrade when the flag changes.
- Redesigning student DropZone UX beyond accepting MMD-only challenge folders under R9.
- Auto-deriving Declaration off from Java absence on solution import — out of scope for this ship (flag stays default on unless lecturer sets it).

### Dependencies / Assumptions

- Builds on conditional pillar scoring (`has_mmd`, `scoreApplicability`, hide inapplicable tabs, weighted applicable-pillar mean) documented in `docs/plans/2026-08-12-002-feat-conditional-pillar-scoring-plan.md` and `docs/solutions/architecture-patterns/conditional-pillar-scoring-dynamic-weights.md`.
- Assumes MMD grading does not require a successful Java compile when Declaration and OT are both not applicable.
- Upload grouping already accepts MMD-only challenge folders (`SubmissionStorageService` empty-Java → `classFileCount=0`); R9 is satisfied by not rejecting that path and by skipping Declaration grading (U2), not by redesigning DropZone.

### Outstanding Questions

None blocking. Former deferred items resolved in KTD1–KTD4.

### Sources / Research

- `docs/plans/2026-08-12-002-feat-conditional-pillar-scoring-plan.md` — prior product rule that Declaration was always applicable; this plan revises that.
- `frontend/src/components/lecturer/structure/MmdRelationsPanel.jsx` — “Requires MMD diagram” checkbox pattern to parallel in challenge chrome.
- `backend/src/main/java/grading/scoring/PillarScoreAggregator.java` — challenge percentage currently seeds Declaration as always applicable.
- `backend/src/main/java/grading/LabResultAssembler.java` — `scoreApplicability` currently hardcodes `"class": true`.
- `backend/src/main/java/model/Challenge.java` — `has_mmd` column pattern for the new flag.
- `docs/solutions/architecture-patterns/conditional-pillar-scoring-dynamic-weights.md` — applicability signal, skip grader, hide-tab guidance.

<!-- ce-section: work-relationships -->
### How This Work Fits Together

This plan owns making Declaration conditionally applicable and the upload/save rules that follow. Broader understanding (not a committed roadmap):

- Extends conditional pillar scoring (`docs/plans/2026-08-12-002-feat-conditional-pillar-scoring-plan.md`) — **Depends on** that MMD/OT applicability machinery already shipping.
- Does not reopen OT authoring or MMD grading internals — **Can proceed independently of** OT feature expansions beyond the zero-pillar save gate.

---

## Planning Contract

### Key Technical Decisions

- KTD1. **Persist Declaration applicability as `challenge.has_class` (boolean, NOT NULL, default `true`), exposed as `hasClass` on DTOs/rubric** — same shape as `has_mmd` / `hasMmd`. UI label remains “Requires Declaration Test.” Governs R1.
- KTD2. **Place the lecturer checkbox on `ChallengeDetailPanel` next to the Class weight field** (always visible at challenge level), not on `ClassDetailPanel`. *(session-settled: user-approved — chosen over Class-editor-only: MMD-only labs may never open a class row.)* Governs R2.
- KTD3. **`PillarScoreAggregator.challengePercentage` takes `classApplicable` and only folds Declaration into the weighted mean when true**; if no pillars apply, return `0` (save gate should prevent this at config time). Mirror MMD: skip `ClassReflectionGrader.grade` when `hasClass` is false; **add** `ClassReflectionGrader.notApplicable()` (static factory: 0% + empty pending lists, like `MmdPillarGrader.notApplicable()`). Governs R4, R5, R6.
- KTD4. **Zero-pillar gate in both `LabStructureService` (structure PUT) and `TestcaseRubricService.saveForChallenge` (OT PUT)** — reject with `400`/`422` and a clear message when `!hasClass && !hasMmd && OT count == 0` after the proposed write. Structure save counts OT from DB (or replaced-challenge empty wipe). OT save that would empty the last pillar for a Declaration-off / MMD-off challenge is rejected. Governs R11.
- KTD5. **`scoreApplicability.class` is dynamic** (from `hasClass`), not hardcoded `true`; extend `PillarScoreBreakdown` with `classApplicable`. Student/lecturer UIs hide the Declaration tab via existing `isPillarNotApplicable`. Governs R7, R8.
- KTD6. **`fullyCorrect` requires 100% only on applicable pillars**, including optional Declaration (same pattern already used for MMD/OT). Governs R5.
- KTD7. **When OT is applicable and the challenge folder has no `.java`, fail with a clear `SubmissionProcessingException` (422)** — enforce in `GradingService.gradeSubmission` (covers student upload and lecturer bulk grade) using `LabRubricSnapshot` + per-challenge `classFileCount` from `ProcessResult`; do not put the guard in `SubmissionStorageService.processUpload` alone (rubric is not available there). When Declaration and OT are both not applicable, keep today’s MMD-only acceptance (R9). Governs R9, R10.
- KTD8. **Thread `hasClass` through clone, desktop pack mapper, structure canonicalize/echo, `ChallengeDTO`, and new-challenge defaults** the same way as `hasMmd`. Solution import leaves `hasClass=true` (does not auto-off when Java is absent). Governs R1, R12.

### Assumptions

- No automated frontend test suite; U4/U5 verification is `npm run build` + manual, matching repo convention.
- Operator-run SQL in `docs/sql/` (idempotent `ADD COLUMN IF NOT EXISTS`); no Flyway.
- Schema baseline `docs/sql/2026-10-13-schema-baseline-reconstructed.sql` should gain `has_class` for empty-DB rebuilds.
- Changing `has_class` does not invalidate rubric cache beyond existing structure-save invalidation.

### Sequencing

1. U1 schema + DTO plumbing (unblocks everything).
2. U2 scoring/pipeline + U3 payload signal (can land together after U1).
3. U4 zero-pillar save gates (structure + OT) — needs U1 flags.
4. U5 lecturer toggle + clone/desktop/import defaults.
5. U6 student/lecturer tab hide + upload Java-required-when-OT check.
6. Docs/AGENTS vocabulary pass last.

### Risks

- **Zero-pillar race:** lecturer turns both flags off then empties OT on a second request — mitigated by KTD4 on both save paths.
- **OT without Java:** historically graded to ERROR/0; KTD7 makes missing Java a hard upload failure when OT applies so students get a clear message.
- **Stale frontend:** lecturer drawers that assume Declaration always exists must read `hasClass` / `scoreApplicability.class` (same pitfall previously fixed for MMD).

---

## Implementation Units

### U1. Schema and rubric plumbing for `has_class`

**Goal:** Add `has_class` and thread it from entity through structure DTOs, save/load, sidebar DTO, and grading rubric.

**Requirements:** R1

**Dependencies:** none

**Files:**
- `docs/sql/2026-10-14-challenge-has-class.sql` (new)
- `docs/sql/2026-10-13-schema-baseline-reconstructed.sql`
- `backend/src/main/java/model/Challenge.java`
- `backend/src/main/java/DTO/rubric/ChallengeStructureDTO.java`
- `backend/src/main/java/DTO/ChallengeDTO.java`
- `backend/src/main/java/service/LabStructureService.java`
- `backend/src/main/java/service/ChallengeService.java`
- `backend/src/main/java/grading/rubric/ChallengeRubric.java`
- `backend/src/main/java/grading/rubric/LabRubricService.java`
- `backend/src/test/java/support/com/eiu/capstone/backend/service/LabStructureServiceSaveTest.java` (or extend nearest structure-save test)

**Approach:**
1. Operator SQL: `ALTER TABLE challenge ADD COLUMN IF NOT EXISTS has_class BOOLEAN NOT NULL DEFAULT true;`
2. Entity field `hasClass` default `true`; getters/setters matching `hasMmd`.
3. `ChallengeStructureDTO` / `ChallengeRubric` / `ChallengeDTO` gain `hasClass` with legacy overloads defaulting `true`.
4. `LabStructureService` upsert/read/canonicalize/echo; `LabRubricService` maps `challenge.isHasClass()`; `ChallengeService` sidebar includes it.

**Test scenarios:**
- Happy path: save `hasClass=false`, GET structure round-trips false.
- Edge case: omitted `hasClass` on save defaults to true.
- Edge case: DB default after migration loads as true.

**Verification:** `mvn test` covering structure save round-trip.

---

### U2. Applicable-pillar scoring and skip Declaration grading

**Goal:** Score only applicable pillars; skip class reflection when `hasClass` is false.

**Requirements:** R4, R5, R6

**Dependencies:** U1

**Files:**
- `backend/src/main/java/grading/scoring/PillarScoreAggregator.java`
- `backend/src/main/java/grading/pipeline/GradingPipeline.java`
- `backend/src/main/java/grading/pipeline/ClassReflectionGrader.java`
- `backend/src/main/java/grading/GradingService.java`
- `backend/src/main/java/grading/PillarScoreBreakdown.java`
- `backend/src/test/java/unit/com/eiu/capstone/backend/grading/scoring/PillarScoreAggregatorTest.java`
- `backend/src/test/java/unit/com/eiu/capstone/backend/grading/GradingServiceTest.java` (if aggregator call sites need updates)

**Approach:**
1. Add `classApplicable` to aggregator overloads; do not seed Declaration when false; empty applicable set → `0`.
2. `classApplicable = challengeRubric.hasClass()`; when false, call newly added `ClassReflectionGrader.notApplicable()` (0%, empty pending lists) instead of `grade`.
3. Update `fullyCorrect` to require Declaration 100% only when applicable (KTD6).
4. Thread `classApplicable` on `PillarScoreBreakdown` and class+MMD phase helpers in `GradingService`.

**Test scenarios:**
- Declaration off + MMD on + no OT → score equals MMD (AE1).
- Declaration off + MMD off + OT on → score equals OT (AE2).
- All three on → unchanged three-pillar weighted mean (AE3).
- Declaration on + MMD off + no OT → score equals Declaration.
- When `hasClass=false`, `ClassReflectionGrader.grade` is never invoked.
- `fullyCorrect` true with Declaration off when remaining applicable pillars are 100%.

**Verification:** `mvn test` (`PillarScoreAggregatorTest` + pipeline/unit coverage).

---

### U3. `scoreApplicability.class` in result payload

**Goal:** Emit dynamic Declaration applicability on `lab_result` bundles.

**Requirements:** R7, R8

**Dependencies:** U2

**Files:**
- `backend/src/main/java/grading/LabResultAssembler.java`
- `backend/src/test/java/unit/com/eiu/capstone/backend/grading/LabResultAssemblerTest.java`

**Approach:**
1. Replace hardcoded `"class", true` with `pillarScores.classApplicable()`.
2. When class not applicable, keep class tree empty/minimal consistent with MMD skip-on-assemble pattern.
3. Update assembler tests that assert `"class": true` for `hasClass=false` cases.

**Test scenarios:**
- `hasClass=false` → `scoreApplicability.class == false`.
- `hasClass=true` → `scoreApplicability.class == true`.
- MMD-only challenge bundle still carries MMD scores and empty/unused class tree.

**Verification:** `mvn test` (`LabResultAssemblerTest`).

---

### U4. Zero-pillar save gate (structure + OT)

**Goal:** Reject configs that leave a challenge with no applicable pillars.

**Requirements:** R11

**Dependencies:** U1

**Files:**
- `backend/src/main/java/service/LabStructureService.java`
- `backend/src/main/java/service/TestcaseRubricService.java`
- Extend `backend/src/test/java/support/com/eiu/capstone/backend/service/LabStructureServiceSaveTest.java` and `TestcaseRubricServiceTest` (or nearest support-home save tests)

**Approach:**
1. Shared helper (e.g. package-private or small util) evaluating `hasClass || hasMmd || otCount > 0`.
2. After computing proposed challenge flags on structure sync, load OT counts per challenge (account for `replacedChallengeIds` wiping OT) and reject with clear message if any challenge fails.
3. On OT PUT, after computing resulting row count for that challenge, load `hasClass`/`hasMmd` from challenge entity and reject emptying OT when both flags are false.

**Test scenarios:**
- Structure save with Declaration off, `hasMmd=false`, no OT → rejected (AE4).
- Structure save with Declaration off, `hasMmd=true`, no OT → allowed.
- OT save that deletes all cases when both flags false → rejected.
- OT save that deletes all cases when Declaration still on → allowed.

**Verification:** `mvn test`.

---

### U5. Lecturer Structure Editor toggle + copy paths

**Goal:** Lecturers can toggle Declaration; clone/desktop/new challenge carry the flag.

**Requirements:** R1, R2, R3

**Dependencies:** U1

**Files:**
- `frontend/src/components/lecturer/structure/ChallengeDetailPanel.jsx`
- `frontend/src/pages/SolutionManagement.jsx`
- `backend/src/main/java/service/LabCloneService.java`
- `backend/src/main/java/desktop/DesktopPackStructureMapper.java`
- `backend/src/main/java/service/SolutionImportService.java` (default `hasClass=true` only; no auto-off)

**Approach:**
1. Checkbox “Requires Declaration Test” beside Class weight on `ChallengeDetailPanel` (`hasClass !== false` default).
2. New challenge defaults `hasClass: true` in `SolutionManagement`.
3. Clone + desktop mapper copy `hasClass` like `hasMmd`.
4. No block when classes exist and Declaration is off (R3).

**Test scenarios:**
- Toggle off, save, reload → still off.
- Classes still editable with Declaration off.
- New challenge defaults on.
- Clone preserves `hasClass=false`.

**Verification:** `npm run build`; manual Solution Management round-trip; `mvn test` for clone/desktop unit coverage if present.

---

### U6. Hide Declaration tab + upload Java rule when OT applies

**Goal:** Hide Declaration UI when inapplicable; enforce Java when OT applies; keep MMD-only upload when Declaration+OT off.

**Requirements:** R7, R9, R10

**Dependencies:** U3, U5 (sidebar `hasClass` helpful for lecturer drawers)

**Files:**
- `frontend/src/components/student/StudentUI.jsx`
- `frontend/src/components/ui/ScorePill.jsx` (comment/docs if class no longer always applicable)
- `frontend/src/components/lecturer/LecturerSubmissionDrawer.jsx`
- `frontend/src/components/lecturer/BulkSubmissionDrawer.jsx`
- `frontend/src/pages/LecturerDashboard.jsx` (pass `hasClass` like `hasMmd` into drawers)
- `backend/src/main/java/grading/GradingService.java` (Java-required-when-OT check per KTD7)
- `frontend/src/components/student/AGENTS.md`, `frontend/src/components/lecturer/AGENTS.md`, `backend/src/main/java/grading/AGENTS.md`, `backend/src/main/java/service/AGENTS.md`, `docs/GRADING_WORKFLOWS.md`, `CONCEPTS.md` (Declaration no longer always applicable; MMD-only folders are not automatically 0% when Declaration is off)
- `docs/solutions/architecture-patterns/conditional-pillar-scoring-dynamic-weights.md` (brief update: class also conditional)

**Approach:**
1. Student tab filter: treat `scoreApplicability.class === false` like MMD (hide; remount active tab if needed). Prefer first remaining applicable tab (MMD or OT), never assume Declaration remains.
2. Lecturer/Bulk drawers: compute `classApplicable` with the same stale-override pattern as MMD (`hasClass` wins over missing/`scoreApplicability.class`); set `showPillarTabs = classApplicable || mmdApplicable || testcaseApplicable`; conditionally render Declaration tab; skip `/class` fetch when not applicable; replace every `activeTab` fallback that hardcodes `'class'` with the first applicable tab.
3. In `GradingService.gradeSubmission`, if any challenge has OT and that challenge’s `classFileCount` is 0 → `SubmissionProcessingException` (KTD7).
4. Update DOX/docs/vocabulary that still say Declaration is always applicable or that MMD-only folders always yield 0% challenge totals.

**Test scenarios:**
- MMD-only graded challenge: no Declaration tab; score = MMD (AE1).
- Declaration off + OT: Java missing → upload 422 (AE2 enforcement).
- Declaration off + OT: Java present → OT grades; Declaration tab hidden.
- Active tab was Declaration when switching to Declaration-off challenge → falls back to visible tab.

**Verification:** `mvn test` for upload/OT Java guard if unit-testable; `npm run build`; manual student + lecturer drawers.

---

## Verification Contract

| Command | Applies to | Notes |
|---|---|---|
| `mvn test` (from `backend/`) | U1–U4, U6 (backend) | Aggregator, assembler, structure/OT save gate, upload Java-when-OT |
| `npm run build` (from `frontend/`) | U5, U6 | No frontend automated tests |
| Manual: Solution Management toggle + zero-pillar save | U4, U5 | AE4 |
| Manual: MMD-only upload + tab hide | U2, U3, U6 | AE1, AE5 |
| Manual: Declaration off + OT without Java | U6 | Expect 422 |
| Operator SQL on Neon | U1 | `docs/sql/2026-10-14-challenge-has-class.sql` |

## Definition of Done

- `has_class` exists default `true`; structure/sidebar/rubric/clone/desktop carry `hasClass` (R1, KTD8).
- Lecturer can toggle “Requires Declaration Test” on challenge chrome; classes may remain when off (R2, R3).
- Challenge score and `fullyCorrect` use only applicable pillars; Declaration grader skipped when off (R4–R6).
- `scoreApplicability.class` is accurate; Declaration tab hidden when false (R7, R8).
- MMD-only upload works when Declaration and OT both off; Java required when OT applies (R9, R10).
- Structure and OT saves reject zero-pillar challenges (R11).
- Past scores unchanged until re-upload (R12).
- Docs/AGENTS/CONCEPTS no longer claim Declaration is always applicable.
- `mvn test` and `npm run build` pass.
