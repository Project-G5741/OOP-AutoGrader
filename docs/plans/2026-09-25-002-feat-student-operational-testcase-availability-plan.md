---
title: Student Operational Testcase Availability - Plan
type: feat
date: 2026-09-25
topic: student-operational-testcase-availability
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Student Operational Testcase Availability - Plan

## Goal Capsule

- **Objective:** Light the student operational-testcase (OT) pillar end-to-end — grade OT on upload when a challenge has authored cases, fold the score into challenge totals via existing `testcase_weight`, and show example/hidden I/O results on every student graded surface.
- **Product authority:** This plan owns student OT applicability, upload-time OT execution for grading, student-facing Operation Test disclosure, and docs/vocabulary that currently state the pillar is dark. Lecturer OT authoring, dry-run, Unit/Composition shapes, and Call-as are not active scope.
- **Open blockers:** None — ready for implementation.
- **Stop conditions:** Do not add student practice dry-run. Do not add per-lab OT enable switches. Do not retroactively regrade prior attempts. Do not show Unit/Composition/Call-as labels to students.

## Product Contract

### Summary

When a challenge has at least one authored operational testcase, student upload runs those tests, includes the OT pillar in the challenge score using the lecturer-set `testcase_weight`, and lights the Operation Test tab with Example (full I/O) vs Other (pass/fail only) disclosure. Challenges with no OT rows stay Class + MMD only. Prior submissions are not retroactively regraded.

### Problem Frame

Lecturers already author Unit/Composition OT and dry-run them, but student upload hard-keeps the testcase pillar dark: OT is not invoked, `testcase_weight` has no student effect, and the Operation Test tab stays hidden. Students cannot be graded on behavior the lab already defines, and the Hidden flag does nothing for them.

### Key Decisions

- **Grade + show on upload** (session-settled: user-directed — chosen over show-only or practice-before-submit: OT must count and be visible). Governs R1–R4, R8–R12.
- **Auto-applicable when ≥1 authored OT** (session-settled: user-directed — chosen over per-lab or per-challenge opt-in: no extra switch). Governs R1, R2.
- **Example / hidden disclosure as designed** (session-settled: user-directed — chosen over all-visible or all-opaque I/O: reuse deferred I/O-card contract). Governs R8–R11.
- **Use existing `testcase_weight`** (session-settled: user-directed — chosen over equal or fixed shares). Governs R3.
- **Everywhere graded views** (session-settled: user-directed — chosen over upload-only). Governs R12.
- **Accept next-upload cutover** (session-settled: user-directed — chosen over lecturer warn-only or gated enablement). Governs R13.
- **Approach A — deferred-contract completion** (session-settled: user-directed — chosen over lecturer student-safe parity as a first-class requirement, and over pre-submit Try tests). Governs scope of R1–R14.

### Actors

- A1. **Student** — uploads a lab, sees OT score and Operation Test results where graded work is shown.
- A2. **Grading engine** — invokes OT on applicable challenges during upload, persists results and display fields, assembles `lab_result` with applicability.
- A3. **Lecturer** — already authors OT and `is_hidden` / `testcase_weight`; no new enablement action in this ship.

### Requirements

**Applicability and scoring**

- R1. A challenge is OT-applicable on student upload when it has ≥1 authored operational testcase; otherwise the testcase pillar stays not applicable and is omitted from student result tab navigation (not shown as “not scored”).
- R2. When OT-applicable, upload executes the challenge’s operational testcases and persists per-testcase / assertion outcomes needed for scoring and student display.
- R3. When OT-applicable, the challenge total includes the OT pillar scaled by the existing challenge `testcase_weight` together with Class and (when applicable) MMD weights.
- R4. When OT is not applicable, student challenge totals remain the weighted mean of Class and (when `has_mmd`) MMD only — same as today’s dark behavior for empty-OT challenges.

**Student disclosure**

- R5. Students never see Unit, Composition, or Call-as type labels on OT results.
- R6. Example testcases (`is_hidden = false`) expose name, pass/fail, and Input / Expected / Your Output (and assertion detail when expanded) to the student.
- R7. Hidden testcases (`is_hidden = true`) expose name and pass/fail only — no input, expected, or actual strings in student-facing payloads or UI.
- R8. The student Operation Test tab shows two sections in order: Example Testcases, then Other Testcases (hidden), per the deferred I/O-card contract in `docs/plans/2026-08-11-002-feat-operation-test-io-card-plan.md` (adopt that disclosure UX; deltas only where this contract supersedes darkness).
- R9. Student upload `lab_result` includes a populated `testcases` array and `scoreApplicability.testcase = true` when OT-applicable; when not applicable, `testcases` is empty and applicability is false.
- R10. Student revisit paths that today return empty OT (`GET .../testcases` and equivalent history/challenge-switch loads) return the same student-safe OT payload shape when the submission’s challenge was OT-applicable.

**Surfaces and cutover**

- R11. Any student surface that shows graded challenge results — post-upload, in-session challenge switch, and history revisit — uses the same Operation Test layout and payload rules (R5–R10).
- R12. The Operation Test tab appears in student navigation only when `scoreApplicability.testcase` is true for that challenge result.
- R13. Cutover is next-upload only: new uploads after this ship use R1–R12; prior attempts are not retroactively regraded or rewritten.
- R14. User-facing docs and domain vocabulary that state student OT is dark are updated to match this contract (student guide, lecturer OT guide where it promises darkness, `CONCEPTS.md` entries that freeze the dark ship).

### Key Flows

- F1. Upload with OT present
  - **Trigger:** Student uploads to a lab; at least one challenge has ≥1 OT.
  - **Actors:** A1, A2
  - **Steps:** Access check → compile → Class/MMD grade as today → OT invoke for applicable challenges → assemble scores with `testcase_weight` → return `lab_result` with OT cards → student sees Operation Test tab.
  - **Covered by:** R1–R3, R5–R9, R11–R12
- F2. Upload with no OT on a challenge
  - **Trigger:** Challenge has zero OT rows.
  - **Actors:** A1, A2
  - **Steps:** Class/MMD grade; testcase pillar not applicable; no Operation Test tab for that challenge.
  - **Covered by:** R1, R4, R9, R12
- F3. Revisit graded OT
  - **Trigger:** Student opens a prior attempt (history or challenge switch after reload) for an OT-applicable graded challenge.
  - **Actors:** A1
  - **Steps:** Load student-safe OT payload; Example expands with I/O; Other shows pass/fail only; type names absent.
  - **Covered by:** R5–R8, R10–R12
- F4. Cutover
  - **Trigger:** Ship lands while labs already have authored OT.
  - **Actors:** A1, A3
  - **Steps:** Existing attempts unchanged; next student upload grades and shows OT under R1–R12 with no lecturer enablement step.
  - **Covered by:** R13

### Acceptance Examples

- AE1. OT grades and shows
  - **Covers:** R1–R3, R6, R8–R9, R12
  - **Given:** Challenge has two example OT and one hidden OT; `testcase_weight` is set
  - **When:** Student uploads a solution
  - **Then:** Challenge total includes OT via that weight; Operation Test tab appears; examples show I/O; hidden row is pass/fail only
- AE2. Empty OT stays dark for that challenge
  - **Covers:** R1, R4, R12
  - **Given:** Challenge has Class (and maybe MMD) but zero OT rows
  - **When:** Student uploads
  - **Then:** No Operation Test tab; total is Class (+ MMD) only
- AE3. Hidden leakage blocked
  - **Covers:** R7, R10
  - **Given:** A hidden OT failed
  - **When:** Student views upload or history OT payload
  - **Then:** Name and FAIL are visible; no input/expected/actual strings appear in API or UI
- AE4. Type names stay lecturer-only
  - **Covers:** R5
  - **Given:** Mix of Unit and Composition OT
  - **When:** Student views Operation Test results
  - **Then:** No “Unit”, “Composition”, or “Call as” labels appear
- AE5. Prior attempt unchanged
  - **Covers:** R13
  - **Given:** An attempt graded before this ship (Class+MMD only)
  - **When:** Student opens that attempt after ship
  - **Then:** Scores and tabs match the original grading; OT does not appear unless they upload again

### Success Criteria

- Students with OT-authored challenges see OT score and Operation Test results after upload and on revisit.
- Labs without OT on a challenge behave as today for that challenge.
- Hidden OT never leaks I/O to students.
- Docs no longer claim the student OT pillar is dark.

### Scope Boundaries

**In scope**

- Student upload OT execution and scoring when applicable
- Student Operation Test tab and payloads (example/hidden)
- Revisit/history consistency
- Docs/`CONCEPTS.md` darkness → lit updates

**Deferred for later**

- Pre-submit student “Try tests” / practice dry-run
- Lecturer “view as student” parity as a separate first-class requirement
- Soft cutover, grandfathering, or per-lab OT enable switches
- New OT assertion kinds or authoring UX changes

**Out of scope**

- Changing Unit/Composition authoring or lecturer dry-run
- Per-testcase scoring weights
- Showing Unit/Composition/Call-as labels to students

### Dependencies / Assumptions

- Assumption: Lighting OT is driven by capstone completeness (lecturers already author OT); no separate field evidence was captured this session.
- Dependency: Lecturer OT graphs, `is_hidden`, display fields, and `testcase_weight` already exist from prior ships.
- Dependency: Deferred student I/O-card disclosure in `docs/plans/2026-08-11-002-feat-operation-test-io-card-plan.md` is the UX authority for R6–R8 unless this contract narrows it.
- Assumption: Upload latency and worker capacity changes from invoking OT on student upload are planning/implementation concerns, not product scope forks.

### Outstanding Questions

**Resolve Before Planning**

- None.

**Deferred to Planning** — resolved in Planning Contract KTDs below.

### Sources / Research

- Current dark hard-code: `backend/src/main/java/com/eiu/capstone/backend/grading/pipeline/GradingPipeline.java` (`testcaseApplicable = false`); student GET `/testcases` stub empty in `ClassStructureService`.
- Prior student I/O contract: `docs/plans/2026-08-11-002-feat-operation-test-io-card-plan.md`.
- Darkness as deferred decision: `docs/plans/2026-09-23-001-feat-operational-testcase-unit-composition-plan.md` KTD2; `CONCEPTS.md` OT / `is_hidden` / `lab_result` entries; `docs/LECTURER_OPERATIONAL_TESTCASE_GUIDE.md`; `docs/USER_GUIDE.md`.
- Applicability pattern: `docs/solutions/architecture-patterns/conditional-pillar-scoring-dynamic-weights.md` (`!testcases().isEmpty()`).
- HOW scout: upload passes `workerSession = null`; dry-run acquires `workerJvmSlot`; `StudentUI.jsx` already implements Example/Other gating; `StudentDashboard.jsx` hard-codes revisit `testcaseJson = []`.

## Planning Contract

### Key Technical Decisions

- KTD1. **Applicability = non-empty rubric OT list** — `testcaseApplicable = !challengeRubric.testcases().isEmpty()` in `GradingPipeline.gradeChallenge`. Replaces hard-coded `false`. Governs R1, R4.
- KTD2. **One worker session per upload when any challenge is OT-applicable** — `GradingService.gradeSubmission` acquires `workerJvmSlot`, opens a `WorkerSession` over the submission temp root, passes the handle into each `gradeChallenge`, releases in `finally`. Challenges without OT still skip `TestcaseGrader`. Pattern: `TestcaseDryRunService` slot + session. Governs R2.
- KTD3. **Reuse `TestcaseGrader.grade` on the upload path** — same invoke/assert path as today for dry-run singles; do not invent a second runner. Governs R2.
- KTD4. **Hidden redaction stays in `TestcaseResultMapper.mapOne`** — student payloads never carry I/O for `is_hidden`; ensure student-mapped results omit type/principle tags (R5). Governs R5–R7, R9.
- KTD5. **Revisit OT from persisted rows, not live rubric alone** — `ClassStructureService.getTestcaseData` / `buildTestcaseDataForSubmission` await persist gate, load submission testcase results; if none, return `[]`. Frontend `fetchChallengeDetails` calls `GET .../testcases` and sets applicability from non-empty mapped results **or** from upload `lab_result` scoreApplicability when present. Pre-ship attempts (no rows) stay dark — Covers R10, R13.
- KTD6. **Keep existing `StudentUI` Example/Other layout** — no redesign; wire data only. Governs R8, R11–R12.
- KTD7. **Invert dark-path tests** — replace “must not invoke TestcaseGrader” with “invokes when OT rows exist; skips when empty.” Governs verification of R1–R2.

### Technical Design

Upload: access → compile → if any challenge has OT, acquire worker slot + session → parallel Class/MMD/OT per challenge (OT only when applicable) → assemble `lab_result` with scores + `scoreApplicability.testcase` → persist (existing writers already accept pending testcases).

Revisit: `GET /api/labs/{labId}/challenges/{challengeId}/testcases` returns `TestcaseResultDTO[]` via mapper when submission detail rows exist; dashboard merges into challenge bundle so `StudentUI` tab filter works.

### Assumptions and Dependencies

- `PillarScoreAggregator` already honors `testcaseApplicable` + `testcaseWeight` — no formula change.
- Isolated worker semaphore remains capacity 1; uploads may queue behind dry-run (accepted carrying cost).
- Lecturer drawer is out of first-class scope; student routes stay on `DisclosureMode.STUDENT`.

### Execution Order

1. U1 — Pipeline applicability + worker session on upload  
2. U2 — Revisit GET `/testcases` + assembler path sanity  
3. U3 — Frontend revisit wiring  
4. U4 — Tests invert/extend  
5. U5 — Docs + CONCEPTS + AGENTS darkness → lit  

## Implementation Units

### U1. Upload OT grading and applicability

- **Goal:** Run OT on student upload when rubric cases exist; include weighted OT in challenge totals; assemble student-safe `lab_result.testcases`.
- **Requirements:** R1–R4, R5–R7, R9, R13 (new uploads only)
- **Files:** `backend/src/main/java/com/eiu/capstone/backend/grading/pipeline/GradingPipeline.java`, `backend/src/main/java/com/eiu/capstone/backend/grading/GradingService.java`, `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/TestcaseResultMapper.java` (tag omission if needed), `backend/src/main/java/com/eiu/capstone/backend/grading/AGENTS.md`
- **Approach:** Derive applicability from rubric list; open shared worker session when any challenge needs OT; schedule `testcaseGrader.grade` like Class/MMD futures; keep empty pillar path for zero-OT challenges.
- **Patterns:** `docs/solutions/architecture-patterns/conditional-pillar-scoring-dynamic-weights.md`; `TestcaseDryRunService` slot acquisition.
- **Test scenarios:**
  - Challenge with Unit+Composition rows → `testcaseApplicable` true; `TestcaseGrader` invoked; challenge % uses `testcase_weight`.
  - Challenge with zero OT → applicable false; grader not invoked; Class(+MMD) only.
  - Hidden OT in assemble → DTO has null I/O fields.
  - Worker session null when no lab challenge has OT.
- **Verify:** `mvn test` targeting grading/pipeline tests (full `mvn test` before ship).

### U2. Student revisit GET `/testcases`

- **Goal:** History and challenge-switch reload can load OT cards for submissions graded with OT.
- **Requirements:** R5–R7, R10–R12
- **Files:** `backend/src/main/java/com/eiu/capstone/backend/service/ClassStructureService.java`, `backend/src/test/java/support/com/eiu/capstone/backend/service/ClassStructureServiceDisclosureTest.java`, `backend/src/main/java/com/eiu/capstone/backend/service/AGENTS.md`
- **Approach:** Mirror class/MMD revisit: await `SubmissionDetailPersistGate`, load rubric + persisted testcase/assertion rows, map via `TestcaseResultMapper`. Empty when no persisted OT results (pre-ship attempts).
- **Test scenarios:**
  - Submission with persisted OT results → non-empty student-safe list; hidden omit I/O.
  - Submission without OT results → `[]`.
  - Student disclosure mode — no type tags in JSON.
- **Verify:** unit/support tests for `ClassStructureService`.

### U3. Frontend revisit + tab data

- **Goal:** `fetchChallengeDetails` loads `/testcases` and populates bundle so Operation Test tab works after reload/history.
- **Requirements:** R8, R11–R12
- **Files:** `frontend/src/pages/StudentDashboard.jsx`, `frontend/src/components/student/StudentUI.jsx` (only if R5 leakage), `frontend/src/components/student/AGENTS.md`, `frontend/AGENTS.md`
- **Approach:** Parallel-fetch testcases with class/mmd; map via existing `mapOperationalTestcases`; set `scoreApplicability.testcase` true when results non-empty (or trust upload cache when present). Do not redesign I/O card UI.
- **Test scenarios:** Manual — upload OT lab → tab; reload challenge → tab persists; history drill-down → tab; zero-OT challenge → no tab.
- **Verify:** `npm run build` from `frontend/`.

### U4. Test suite realignment

- **Goal:** Replace dark-path guarantees with lit-path guarantees.
- **Requirements:** R1–R2, R7, R13
- **Files:** `backend/src/test/java/integration/com/eiu/capstone/backend/pipeline/SubmissionPipelineIntegrationTest.java`, `backend/src/test/java/unit/com/eiu/capstone/backend/grading/GradingServiceTest.java`, related assembler/aggregator tests as needed
- **Approach:** Invert `uploadWithUnitAndCompositionRowsDoesNotInvokeOperationalTests`; add empty-OT still-skips; assert hidden JSON fields absent.
- **Verify:** `mvn test` from `backend/`.

### U5. Docs and vocabulary

- **Goal:** Remove “student OT dark this ship” claims; document lit behavior and cutover.
- **Requirements:** R14
- **Files:** `CONCEPTS.md`, `docs/USER_GUIDE.md`, `docs/LECTURER_OPERATIONAL_TESTCASE_GUIDE.md`, `docs/GRADING_WORKFLOWS.md`, `docs/HOW_IT_RUNS.md`, `backend/AGENTS.md`, `backend/src/main/java/com/eiu/capstone/backend/grading/AGENTS.md`, `frontend/src/components/student/AGENTS.md`
- **Approach:** Update “this ship” sentences to student OT applicable when cases exist; Hidden flag now affects students; `testcase_weight` affects totals when applicable.
- **Verify:** Doc skim consistency; no remaining “OT dark” as current-state claim in active guides.

## Verification Contract

- Backend: `mvn test` from `backend/`
- Frontend: `npm run build` from `frontend/`
- Manual smoke: lab with example+hidden OT — upload, switch challenge, reload, history; lab challenge with no OT — no Operation Test tab

## Definition of Done

- [ ] U1–U5 complete against R1–R14
- [ ] Integration tests assert OT runs when rows exist and skips when empty
- [ ] Hidden I/O never present in student API JSON
- [ ] Docs/CONCEPTS no longer claim student OT is dark
- [ ] No commit/PR unless user asks (this run: do not commit)
