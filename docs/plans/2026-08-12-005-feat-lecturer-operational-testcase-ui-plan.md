---
title: Lecturer Operational Testcase UI - Plan
type: feat
date: 2026-08-12
topic: lecturer-operational-testcase-ui
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-plan-bootstrap
execution: code
origin: ce-brainstorm session 2026-08-12 (Solution Management testcase authoring)
---

# Lecturer Operational Testcase UI - Plan

## Goal Capsule

- **Objective:** Let lecturers author operational testcases in Solution Management without SQL — full parity with the existing rubric schema, a problem-level **Operational Testcases** tab beside MMD Relations, separate save, and inline dry-run against pasted reference Java.
- **Product authority:** This plan owns lecturer testcase CRUD API, dry-run preview API, and the Solution Management UI. It does not change student grading semantics or the operational testcase grading engine beyond reusing it for dry-run.
- **Stop conditions:** Do not fold testcase save into `PUT .../structure`. Do not require reference-solution folder upload for dry-run. Do not add stdin injection or multi-call sequence testcases.

---

## Product Contract

**Product Contract preservation:** Bootstrapped from brainstorm dialogue (no prior requirements-only artifact). Stable R/A/F/AE IDs below are authoritative for this feature.

### Summary

Lecturers today seed operational testcases via SQL while the grading engine and student Operation Test tab already work. This feature adds a third challenge-level panel in Solution Management — **Operational Testcases** — where lecturers create, edit, reorder, and save testcases with the same shapes the engine already grades (SINGLE_INVOCATION, COMPARISON, all assertion kinds, receiver construction, hidden flag, weight/order). Saving testcases is independent of **Save Lab Structure**. Lecturers paste reference Java per challenge and run individual testcases to preview I/O before students submit.

### Problem Frame

Operational testcase grading replaced structural checks in August 2026, but authoring still requires hand-written SQL against `testcase`, `testcase_invocation`, `testcase_instance`, and `testcase_assertion`. Lecturers must know internal UUIDs for constructors, methods, and fields. Mistakes surface only on first student upload. The Lab Structure Editor covers classes and MMD but has no testcase surface.

### Key Decisions

- **Full SQL-seed parity in v1** — lecturers can author every testcase shape the engine supports without SQL. (session-settled: user-directed — chosen over phased subset)
- **Problem-level tab (Option B)** — when a challenge (not a class) is selected, main panel tabs: **MMD Relations | Operational Testcases**. Class selection still opens the class editor. (session-settled: user-directed — chosen over sidebar tree or separate page)
- **Separate testcase save** — dedicated **Save Testcases** action and API; structure save unchanged. (session-settled: user-directed)
- **Inline dry-run via pasted Java** — per-challenge reference source textarea; **Run** compiles and invokes without persisting results. (session-settled: user-directed — chosen over full folder upload or no preview)
- **Warn-only on unsaved structure** — switching to Testcases with dirty structure draft shows a non-blocking warning; dry-run/save may fail until structure is persisted. (session-settled: user-directed)
- **Block delete of referenced rubric members** — deleting a method, constructor, or field referenced by a saved testcase is blocked with a message naming affected testcases. Governs R12.
- **Dedicated testcase API** — `GET/PUT` per challenge under `/api/lecturer/labs/{labId}/challenges/{challengeId}/testcases`; not nested in structure PUT. Governs R1, R2.
- **Dry-run reuses grading stack** — compile with `JavaCompilerService`, grade with `TestcaseGrader` + `AssertionEvaluator` + `InvocationRunner` in ephemeral temp dir; no `submission_*` persistence. Governs R10–R11.

### Actors

- **A1. Lecturer** — authors testcases, saves, dry-runs against reference Java.
- **A2. Grading engine** — unchanged for student uploads; reused read-only for dry-run.
- **A3. Student** — no UI changes; benefits from lecturer-authored testcases.

### Requirements

**API — testcase rubric CRUD**

- R1. `GET /api/lecturer/labs/{labId}/challenges/{challengeId}/testcases` returns the full testcase rubric graph for the challenge (testcases ordered by `order_index`, each with invocation/instances/assertions). Lecturer JWT required.
- R2. `PUT /api/lecturer/labs/{labId}/challenges/{challengeId}/testcases` accepts the same shape and upserts the challenge's testcase set (sync-by-presence: omitted testcase IDs are deleted; child rows replaced per testcase). Returns saved payload. Lecturer JWT required.
- R3. Payload supports `SINGLE_INVOCATION` and `COMPARISON` types, all `AssertionKind` values, `comparison_method`, `is_hidden`, `weight`, `order_index`, receiver constructor + params on invocations, and FIELD_STATE `field_id` references.
- R4. Save validates FK integrity: constructor/method/field IDs must belong to classes in the same challenge. Return 422 with field-level errors on violation.
- R5. After successful save, invalidate lab rubric cache (`RubricCacheInvalidationSupport.invalidateLab`).

**API — dry-run**

- R6. `POST /api/lecturer/labs/{labId}/challenges/{challengeId}/testcases/dry-run` accepts `{ referenceSources: [{ className, source }], testcase: <single testcase DTO> }` (or testcase id to load from DB plus optional override). Lecturer JWT required.
- R7. Server compiles `referenceSources` to a temp `classes/` directory via `JavaCompilerService`, runs the testcase through the operational grading path, and returns a `TestcaseResultDTO`-shaped preview (primary I/O displays + per-assertion outcomes). No DB writes.
- R8. Compile failure returns 422 with compiler diagnostics; invoke timeout/error surfaces in preview status consistent with student grading (`ERROR` / `FAILED`).

**Frontend — Solution Management**

- R9. When a challenge is selected (no class selected), show tab bar: **MMD Relations** | **Operational Testcases** (default last-selected tab per challenge in session).
- R10. **Operational Testcases** panel lists testcases with name, type, hidden badge, weight; supports add, reorder (up/down or order index), delete, and expand-to-edit.
- R11. Editor fields mirror R3: invocation picker (constructor/method from challenge classes), JSON param editors, assertion stack (kind, expected value, comparison mode, field picker for FIELD_STATE), COMPARISON instance A/B builders, receiver construction block for METHOD invocations.
- R12. **Save Testcases** button enabled when testcase draft differs from last saved snapshot; calls R2. Independent dirty state from structure save.
- R13. Reference Java section: one or more class source editors (class name + textarea); persisted in `sessionStorage` per `(labId, challengeId)` for convenience (not server-persisted).
- R14. Per-testcase **Run** button calls R6 and shows preview card matching student I/O layout (INPUT / EXPECTED / YOUR).
- R15. If structure draft is dirty when entering Testcases tab or clicking Run/Save Testcases, show dismissible warning that structure must be saved for new methods/fields to be referenceable.

**Integrity**

- R16. Structure save path blocks deletion of method/constructor/field rows referenced by any testcase in that challenge (load testcase refs before delete). Error lists testcase names.

### Flows

- **F1. Author first testcase:** Lecturer saves class structure → selects challenge → Operational Testcases tab → adds SINGLE_INVOCATION testcase → picks method + assertions → Save Testcases.
- **F2. Dry-run before save:** Lecturer pastes reference Java → configures testcase in draft → Run → sees I/O preview → fixes → Save Testcases.
- **F3. Hidden testcase:** Lecturer sets `is_hidden` → saves → student sees pass/fail only (existing student behavior; lecturer preview still shows full I/O in dry-run).

### Acceptance Examples

- **AE1.** Lecturer creates a method return-value testcase equivalent to `docs/sql/2026-08-11-operational-testcase-seed-sample.sql` deposit example via UI, saves, and student upload grades against it.
- **AE2.** Lecturer pastes reference Java, runs dry-run on a FIELD_STATE testcase, sees expected vs actual in preview without any student submission.
- **AE3.** Lecturer attempts to delete a method used by a testcase; UI/backend refuses with testcase name in message.
- **AE4.** COMPARISON testcase with two instances and COMPARISON_RESULT assertion saves and grades identically to SQL seed.

### Scope Boundaries

**In scope:** Lecturer testcase CRUD API, dry-run API, Testcases panel UI, reference-Java paste UX, rubric-member delete guard, cache invalidation, backend/frontend AGENTS.md updates.

**Deferred for later**

- Server-persisted lecturer reference solutions
- Bulk import/export of testcase JSON
- Lecturer Operation Test tab in submission drawer (view student results — separate feature)

**Outside this product's identity**

- Changing testcase scoring weights or pillar math
- stdin injection, multi-invocation sequences

### Open Questions

| ID | Question | Status |
|---|---|---|
| OQ1 | Should dry-run execute on testcase draft not yet saved, or require save first? | **Deferred to planning** — default: allow draft payload in POST body (R6) so lecturers can iterate before save |

---

## Planning Contract

### Assumptions

- Operational testcase schema from `docs/sql/2026-08-11-operational-testcase-grading.sql` is deployed in target environments.
- Lecturers author structure before testcases in practice; warn-only unsaved structure is sufficient.
- Reference Java dry-run uses the same package/class names students will submit (lecturer responsibility).

### Key Technical Decisions

- **KTD1 — `TestcaseRubricService` separate from `LabStructureService`** — owns load/save/sync for testcase tables; keeps structure save unchanged and matches separate-save product decision. Files: new `service/TestcaseRubricService.java`, extend `LecturerRubricController`.
- **KTD2 — Mirror `LabStructureService` sync pattern** — preload challenge testcases, upsert by ID, delete absent parents, replace child collections per testcase in one transaction. Reuse bulk-delete/reinsert pattern from parameter sync where simpler.
- **KTD3 — DTO layer under `DTO/rubric/testcase/`** — `TestcaseStructureDTO`, `InvocationStructureDTO`, `InstanceStructureDTO`, `AssertionStructureDTO`, `ChallengeTestcasesResponse` (list wrapper). Keeps structure DTOs stable.
- **KTD4 — `TestcaseDryRunService`** — creates temp dir, writes/compiles sources, builds minimal `ChallengeGradingContext`, calls `TestcaseGrader.gradeTestcase` (or package-visible orchestration method), maps via `TestcaseResultMapper` without persistence. Deletes temp dir in `finally`.
- **KTD5 — Delete guard in `LabStructureService`** — before `syncMethods`/`syncConstructors`/`syncFields` deletes, query `TestcaseRubricService.findReferences(challengeId, memberId)`; throw `IllegalStateException` → 422 with testcase names.
- **KTD6 — Frontend state** — `testcaseDraft` + `testcaseSnapshot` per challenge alongside existing `draft`/`savedSnapshot`; lazy-load testcases on first tab visit.

### High-Level Technical Design

```mermaid
flowchart LR
  subgraph frontend [SolutionManagement]
    Tabs[MMD | Testcases tabs]
  Panel[TestcasesPanel]
  Tabs --> Panel
  Panel -->|GET/PUT| API
  Panel -->|POST dry-run| DryAPI
  end
  subgraph backend [Backend]
  LRC[LecturerRubricController]
  TRS[TestcaseRubricService]
  TDR[TestcaseDryRunService]
  LRC --> TRS
  LRC --> TDR
  TRS --> DB[(testcase_*)]
  TDR --> JCS[JavaCompilerService]
  TDR --> TG[TestcaseGrader]
  end
  API[Lecturer API]
  DryAPI[Dry-run API]
```

### Sequencing

1. DTOs + `TestcaseRubricService` + tests (U1)
2. Controller endpoints (U2)
3. Dry-run service + endpoint (U3)
4. Delete guard in structure save (U4)
5. Frontend panel + save (U5)
6. Editor forms + dry-run UI (U6)

---

## Implementation Units

### U1. Testcase rubric DTOs and save service

**Goal:** Persist testcase rubric graphs with validation and cache invalidation.

**Requirements:** R1–R5, R16 (reference query helper)

**Dependencies:** None

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/DTO/rubric/testcase/TestcaseStructureDTO.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/DTO/rubric/testcase/InvocationStructureDTO.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/DTO/rubric/testcase/InstanceStructureDTO.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/DTO/rubric/testcase/AssertionStructureDTO.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/DTO/rubric/testcase/ChallengeTestcasesResponse.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/service/TestcaseRubricService.java` (new)
- `backend/src/test/java/com/eiu/capstone/backend/service/TestcaseRubricServiceTest.java` (new)

**Approach:**
1. Define DTO records matching entity fields (UUID ids optional on create; server assigns).
2. `loadForChallenge(labId, challengeId)` — verify lab owns challenge; batch-load testcases + children (same query pattern as `LabRubricService`).
3. `saveForChallenge(labId, challengeId, List<TestcaseStructureDTO>)` — validate types (exactly one invocation for SINGLE_INVOCATION; two instances + COMPARISON_RESULT for COMPARISON), FK membership in challenge classes, assertion rules per kind.
4. Sync: delete testcases not in payload; upsert parents; clear/replace child rows.
5. `findReferencingTestcases(challengeId, memberType, memberId)` for delete guard.
6. Call `rubricCacheInvalidationSupport.invalidateLab(labId)` after save.

**Patterns to follow:** `LabStructureService.saveLabStructure` / `syncMethods`; `LabRubricService` batch maps.

**Test scenarios:**
- Saves SINGLE_INVOCATION with method + RETURN_VALUE assertion.
- Saves COMPARISON with two instances.
- Rejects method_id from another challenge (422).
- Deletes testcase omitted from PUT payload.
- Re-save updates assertions without duplicate child rows.

**Verification:** `mvn test -Dtest=TestcaseRubricServiceTest` passes.

---

### U2. Lecturer testcase CRUD endpoints

**Goal:** Expose testcase rubric load/save over REST.

**Requirements:** R1–R5

**Dependencies:** U1

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/controller/LecturerRubricController.java` (modify)
- `backend/src/test/java/com/eiu/capstone/backend/controller/LecturerRubricControllerTest.java` (new, or extend existing if present)

**Approach:**
1. Add `GET .../challenges/{challengeId}/testcases` → `ChallengeTestcasesResponse`.
2. Add `PUT .../challenges/{challengeId}/testcases` with `List<TestcaseStructureDTO>` body.
3. Map validation errors to 422 via `GlobalExceptionHandler` if not already handled.

**Test scenarios:**
- GET returns empty list for challenge with no testcases.
- PUT round-trip preserves IDs and child graph.
- Non-lecturer JWT → 403.

**Verification:** Controller test green; manual Swagger check.

---

### U3. Testcase dry-run service and endpoint

**Goal:** Preview testcase execution against pasted reference Java without persistence.

**Requirements:** R6–R8, R10–R11 (backend half)

**Dependencies:** U1

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/service/TestcaseDryRunService.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/DTO/rubric/testcase/TestcaseDryRunRequest.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/controller/LecturerRubricController.java` (modify)
- `backend/src/test/java/com/eiu/capstone/backend/service/TestcaseDryRunServiceTest.java` (new)

**Approach:**
1. Request body: `referenceSources` (className + source text), `testcase` DTO (draft or saved).
2. Write sources to temp challenge folder, compile with `JavaCompilerService`.
3. Build `TestcaseRubric` from request DTO (or merge saved + override).
4. Invoke grading path used by `TestcaseGrader` for one testcase; capture `TestcaseResultDTO`.
5. Return preview JSON; cleanup temp dir.

**Patterns to follow:** `InvocationRunnerTest` compile layout; `TestcaseResultMapper` for output shape.

**Test scenarios:**
- Covers AE2: compile + invoke return-value testcase returns matching expected/actual displays.
- Compile error in reference source → 422 with diagnostics.
- COMPARISON dry-run returns comparison result assertion outcome.

**Verification:** `TestcaseDryRunServiceTest` with real `javac` (skip if no JDK in CI — document in Verification Contract).

---

### U4. Rubric member delete guard

**Goal:** Prevent silent breakage when deleting methods/constructors/fields used by testcases.

**Requirements:** R16, AE3

**Dependencies:** U1

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/service/LabStructureService.java` (modify)
- `backend/src/test/java/com/eiu/capstone/backend/service/LabStructureServiceSaveTest.java` (modify)

**Approach:**
1. Before removing method/constructor/field in sync methods, call `findReferencingTestcases`.
2. If non-empty, throw domain exception with testcase names → 422 on structure save.

**Test scenarios:**
- Structure save deleting unreferenced method succeeds.
- Structure save deleting referenced method fails with testcase name in message.

**Verification:** Extended `LabStructureServiceSaveTest` passes.

---

### U5. Testcases tab shell and save flow

**Goal:** Wire Operational Testcases tab with list, dirty tracking, and save.

**Requirements:** R9, R12, R15

**Dependencies:** U2

**Files:**
- `frontend/src/components/lecturer/structure/TestcasesPanel.jsx` (new)
- `frontend/src/components/lecturer/structure/ChallengeTabs.jsx` (new, optional small wrapper)
- `frontend/src/pages/SolutionManagement.jsx` (modify)
- `frontend/src/components/lecturer/AGENTS.md` (modify)
- `frontend/src/pages/AGENTS.md` (modify)

**Approach:**
1. When `selectedChallenge && !selectedClassRef`, render tab bar above `MmdRelationsPanel` / `TestcasesPanel`.
2. Lazy-fetch testcases on first tab open; maintain `testcaseDraft` / `testcaseSnapshot` per challenge.
3. **Save Testcases** calls PUT; toast on success; update snapshot.
4. If `isDirty` (structure), show warning banner on tab enter.

**Patterns to follow:** Existing `draft`/`savedSnapshot`/`handleSave` in `SolutionManagement.jsx`; `MmdRelationsPanel` layout.

**Test scenarios:**
- Tab switch preserves MMD vs Testcases selection.
- Save disabled when testcase draft matches snapshot.
- Warning visible when structure dirty.

**Verification:** Manual — create testcase name, save, reload tab shows persisted row.

---

### U6. Testcase editor forms and dry-run UI

**Goal:** Full authoring UI and inline Run preview.

**Requirements:** R3–R4, R10–R11, R13–R14, AE1, AE4

**Dependencies:** U3, U5

**Files:**
- `frontend/src/components/lecturer/structure/TestcaseEditor.jsx` (new)
- `frontend/src/components/lecturer/structure/AssertionRows.jsx` (new)
- `frontend/src/components/lecturer/structure/ReferenceJavaPanel.jsx` (new)
- `frontend/src/components/lecturer/structure/TestcaseDryRunPreview.jsx` (new)
- `frontend/src/utils/testcaseJson.js` (new — param JSON helpers)

**Approach:**
1. `TestcaseEditor` — type toggle (SINGLE_INVOCATION / COMPARISON), invocation picker populated from challenge classes in structure draft (saved IDs), assertion stack editor.
2. `ReferenceJavaPanel` — multi-class source editors; `sessionStorage` key `ref-java:{labId}:{challengeId}`.
3. **Run** posts dry-run with draft testcase + reference sources; `TestcaseDryRunPreview` mirrors student I/O card layout from `StudentUI.jsx`.
4. Hidden toggle, weight, order controls.

**Patterns to follow:** `ParameterRows.jsx` for param rows; student testcase card in `StudentUI.jsx`.

**Test scenarios:**
- Covers AE1: build deposit return-value testcase end-to-end in UI.
- COMPARISON editor shows instance A/B + comparison method selector.
- METHOD invocation shows receiver constructor block when selected.
- Run displays ERROR state when reference Java won't compile.

**Verification:** Manual dry-run + save + student upload on one seeded lab (or local test lab).

---

### U7. Documentation and integration verification

**Goal:** Update DOX and run end-to-end smoke.

**Requirements:** All AE*

**Dependencies:** U1–U6

**Files:**
- `backend/AGENTS.md` (modify — new endpoints)
- `backend/src/main/java/com/eiu/capstone/backend/grading/AGENTS.md` (modify — dry-run note)
- `frontend/AGENTS.md` (modify — Solution Management testcase tab)
- `docs/solutions/` — optional learning if non-obvious pitfall found during implementation

**Approach:** DOX pass per AGENTS.md root contract; manual AE1–AE4 checklist.

**Test expectation:** none — documentation and manual smoke only.

**Verification:** `npm run build` (frontend) + `mvn test` (backend) green.

---

## Verification Contract

| Check | Command / action |
|---|---|
| Backend unit tests | `cd backend && mvn test` |
| New service tests | `mvn test -Dtest=TestcaseRubricServiceTest,TestcaseDryRunServiceTest` |
| Frontend build | `cd frontend && npm run build` |
| Manual AE1–AE4 | Lecturer login → Solution Management → author, dry-run, save, student upload |
| API auth | Non-lecturer cannot call `/api/lecturer/labs/.../testcases` |

**Note:** `TestcaseDryRunServiceTest` requires JDK (same as existing `InvocationRunnerTest`).

---

## Definition of Done

- [ ] Lecturer can CRUD operational testcases for any challenge via UI without SQL
- [ ] All assertion kinds and both testcase types are authorable (full parity)
- [ ] Separate **Save Testcases** persists independently of structure save
- [ ] Dry-run with pasted reference Java shows I/O preview per testcase
- [ ] Deleting rubric members referenced by testcases is blocked with clear error
- [ ] Rubric cache invalidated on testcase save
- [ ] AGENTS.md updated on affected paths
- [ ] Backend tests and frontend build pass

---

## Risks and Dependencies

| Risk | Mitigation |
|---|---|
| Complex editor UX for multi-assertion + receiver | Reuse `ParameterRows` patterns; ship list + expand editor before polish |
| Dry-run temp compile failures confuse lecturers | Surface compiler errors inline in preview panel |
| FK refs to unsaved structure members | Warn-only + 422 on save with explicit missing-id message |
| Orphaned testcases after challenge delete | Existing DB cascade on challenge delete is acceptable |

**Prerequisites:** Operational testcase schema deployed (`docs/sql/2026-08-11-operational-testcase-grading.sql`).

---

## Sources and Research

- `docs/plans/2026-08-11-001-feat-operational-testcase-grading-plan.md` — grading engine (already shipped)
- `docs/solutions/architecture-patterns/operational-testcase-grading.md` — layer contracts
- `docs/sql/2026-08-11-operational-testcase-seed-sample.sql` — authoring examples
- `frontend/src/pages/SolutionManagement.jsx` — editor patterns
- `backend/src/main/java/com/eiu/capstone/backend/service/LabStructureService.java` — sync pattern
- Brainstorm session 2026-08-12 — product decisions (tab layout, separate save, dry-run, full parity)
