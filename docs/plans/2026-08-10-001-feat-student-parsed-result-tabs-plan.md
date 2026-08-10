---
title: Student-Parsed Result Tabs - Plan
type: feat
date: 2026-08-10
topic: student-parsed-result-tabs
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Student-Parsed Result Tabs - Plan

## Goal Capsule

- **Objective:** Replace rubric-template text in the Class and MMD result tabs with rubric-scoped rows populated from what the grading pipeline parsed in the student's latest submission, while keeping per-item pass/fail feedback.
- **Product authority:** This plan owns student and lecturer Class/MMD tab display semantics for upload and history paths. Testcase tab behavior, raw file viewing, and side-by-side expected-vs-actual comparison are not active scope.
- **Open blockers:** None — ready for implementation.

## Product Contract

### Summary

Class and MMD tabs will show rubric-scoped items using the student's parsed submission content when an item is present, and the rubric expected label when an item is missing, each with existing ✓/✗ grading feedback. A parsed submission snapshot captured at grade time will power both fresh upload and history views for students and lecturers.

### Problem Frame

After upload, students open Class and MMD tabs to understand their grade. Today those tabs list the lab rubric structure from PostgreSQL with pass/fail flags attached. The labels read as what the lab expects — not what the student actually wrote — so students cannot tell what they submitted versus what was expected. Lecturers reviewing submissions see the same rubric-shaped view. Student source files are compiled and parsed during grading but not retained; only boolean per-element results and small sidecar metadata survive, so the UI has no student-authored display text to show after the fact.

### Key Decisions

- **Persist a parsed submission snapshot at grade time, not raw student files** — aligns with the existing no-long-term-source retention policy while enabling history. Governs R1, R2, R3, R4.
- **Rubric-scoped rows only; no extras section** (session-settled: user-directed — chosen over showing extra student items or a separate extras section: keeps tabs aligned to lab grading scope). Governs R5.
- **Present items show student-parsed text; missing items show rubric expected label with ✗** (session-settled: user-directed — chosen over omitting missing items or generic placeholders: makes absence explicit while surfacing actual student content when found). Governs R6, R7.
- **Keep per-item ✓/✗ and score summaries on parsed rows** (session-settled: user-directed — chosen over parsed-only view: preserves grading feedback students and lecturers rely on). Governs R8.
- **Both student dashboard and lecturer submission drawer** (session-settled: user-directed). Governs R9.
- **Upload session and history/review paths** (session-settled: user-directed — chosen over upload-only: history must show the same semantics). Governs R10.

### Actors

- A1. **Student** — uploads a lab, views Class/MMD tabs after upload, and revisits past attempts from the student dashboard history flow.
- A2. **Lecturer** — opens a student's challenge from the lab roster and reviews Class/MMD tabs in the submission drawer (including past submissions when `submissionId` is pinned).

### Requirements

**Snapshot capture**

- R1. At grade time for each challenge, the system captures a parsed submission snapshot of rubric-scoped Class and MMD elements derived from the same parsing used for grading (Java reflection for Class pillar, MMD parser for MMD pillar).
- R2. The snapshot is stored per lab submission and challenge and treated as immutable for that attempt.
- R3. The snapshot includes, for each rubric-scoped element, enough display text to render what the student had when present (e.g. member signature, relation endpoints) without re-reading deleted source files.
- R4. The snapshot is written during the existing grading pipeline — not on tab read — so history views do not depend on ephemeral upload folders.

**Tab display — Class**

- R5. The Class tab lists only rubric-scoped classes, fields, constructors, and methods — not extra classes or members the student added beyond the lab spec.
- R6. When a rubric-scoped member is present in the student's parsed submission, the tab shows the student-parsed display text for that row.
- R7. When a rubric-scoped member is absent from the student's parsed submission, the tab shows the rubric expected label with ✗.
- R8. Each Class row retains pass/fail status and contributes to existing Class score summaries the same way today's tabs do.

**Tab display — MMD**

- R9. The MMD tab applies the same present-vs-missing rules (R6–R8) to rubric-scoped classes, attributes, and relations parsed from the student's `.mmd` file.

**Surfaces and paths**

- R10. Student dashboard Class/MMD tabs after upload (via `lab_result` bundle) and when loading past submissions (via read APIs) use the snapshot-backed display semantics.
- R11. Lecturer submission drawer Class/MMD tabs use the same snapshot-backed semantics, including when `submissionId` pins a specific attempt.
- R13. Lecturer challenge export rows that derive from Class/MMD breakdown data reflect snapshot-backed display text, not rubric-template labels for present items.

**Preserved behavior**

- R12. Existing compile-error surfacing on the Class tab (when compilation failed for a challenge) remains unchanged in intent — errors still appear alongside class breakdown data.

### Key Flows

- F1. **Fresh upload — student sees parsed tabs**
  - **Trigger:** Student uploads a lab folder; grading completes successfully.
  - **Actors:** A1
  - **Steps:** Grading parses student Java and MMD files, writes per-element results and the parsed submission snapshot, assembles `lab_result` with snapshot-backed Class/MMD arrays, returns response; student dashboard renders tabs from the bundle without showing rubric-template text for present items.
  - **Covered by:** R1–R4, R6–R8, R10

- F2. **History — student revisits a past attempt**
  - **Trigger:** Student selects a past submission on the dashboard and opens a challenge's Class or MMD tab.
  - **Actors:** A1
  - **Steps:** Read API resolves the submission (latest or pinned), loads the stored snapshot for that challenge, returns snapshot-backed rows; UI matches post-upload semantics.
  - **Covered by:** R2, R4, R6–R8, R10

- F3. **Lecturer reviews a student's challenge**
  - **Trigger:** Lecturer opens View on a roster row for a challenge (optionally for a specific `submissionId`).
  - **Actors:** A2
  - **Steps:** Drawer fetches Class/MMD data; backend returns snapshot-backed rubric-scoped rows with pass/fail; lecturer sees student-parsed text for present items.
  - **Covered by:** R6–R9, R11

### Acceptance Examples

- AE1. **Wrong field name — student content visible**
  - **Covers R6, R8.**
  - **Given:** Rubric expects `private int age` in `Person`; student wrote `private String name`.
  - **When:** Student opens the Class tab after upload.
  - **Then:** The row shows the student's parsed member text (e.g. `name : String`) with ✗, not the rubric's `age : int` label.

- AE2. **Missing constructor**
  - **Covers R7, R8.**
  - **Given:** Rubric requires a no-arg constructor on `Person`; student's class has none.
  - **When:** Student expands `Person` on the Class tab.
  - **Then:** The rubric expected constructor label appears with ✗.

- AE3. **MMD relation present but wrong type**
  - **Covers R6, R9.**
  - **Given:** Rubric expects `Car --|> Vehicle` (inheritance); student diagram shows `Car --> Vehicle` (association).
  - **When:** Student opens the MMD Relations section.
  - **Then:** The row reflects the student's parsed relation (endpoints and type as parsed) with ✗ and any existing error detail.

- AE4. **History attempt matches upload semantics**
  - **Covers R2, R10.**
  - **Given:** A student completed attempt 2 yesterday; snapshot was written at grade time.
  - **When:** Student loads attempt 2 from history and opens the MMD tab.
  - **Then:** Display matches what they saw immediately after that upload — student-parsed text for present items, rubric label for missing items.

- AE5. **No extra rubric items**
  - **Covers R5.**
  - **Given:** Student added an extra class `Helper` not in the rubric.
  - **When:** Either actor opens Class or MMD tabs.
  - **Then:** `Helper` does not appear in the tab lists.

### Scope Boundaries

**In scope**

- Parsed submission snapshot capture at grade time
- Snapshot-backed Class and MMD tab APIs and `lab_result` bundle fields
- Student dashboard and lecturer submission drawer tab rendering
- Upload and history/review paths
- Lecturer export rows that derive from Class/MMD tab data (must reflect new display text)

**Out of scope**

- Raw `.java` / `.mmd` file viewer or download
- Side-by-side "expected vs actual" columns
- Listing student items beyond rubric scope
- Testcase tab changes
- `StudentHistoryPage` expanded-row detail (today shows challenge scores only — no Class/MMD tabs there unless separately scoped later)

### Dependencies / Assumptions

- Grading already extracts student structure via Java reflection (`GradingPipeline`) and MMD parsing (`MmdPillarGrader`); snapshot capture reuses that extraction, not a second divergent parser.
- Upload temp folders are deleted after grading (`SubmissionController`); snapshot persistence is required for history — raw file retention is not assumed.
- Existing per-element `is_correct` results in the database remain the authority for pass/fail flags; snapshot supplies display text only.
- **Assumption:** Snapshot format version can evolve; planning chooses storage shape (DB table vs sidecar JSON) without changing product semantics above.

### Outstanding Questions

All prior open questions are resolved in the Planning Contract (KTD1–KTD3).

### Sources / Research

- `backend/src/main/java/com/eiu/capstone/backend/service/ClassStructureService.java` — current tab assembly from rubric + `correctIds`
- `backend/src/main/java/com/eiu/capstone/backend/grading/LabResultAssembler.java` — upload `lab_result` uses same rubric pattern today
- `backend/src/main/java/com/eiu/capstone/backend/grading/pipeline/MmdPillarGrader.java` — MMD parse at grade time
- `frontend/src/components/student/StudentUI.jsx` — student Class/MMD tab rendering
- `frontend/src/components/lecturer/LecturerSubmissionDrawer.jsx` — lecturer tab fetches
- `CONCEPTS.md` — `lab_result bundle` and `Parsed submission snapshot` definitions

---

## Planning Contract

### Key Technical Decisions

- **KTD1. Sidecar JSON store for snapshots** — persist under `{SUBMISSION_BASE_DIR}/_parsed_snapshot/{submissionId}.json`, mirroring `SubmissionMmdMetaStore` and `SubmissionCompileErrorStore`. Avoids a DB migration in a schema-managed-externally repo. Governs U1.
- **KTD2. Snapshot built during grading, not on tab read** — `ParsedSubmissionSnapshotBuilder` consumes in-memory `ParsedClass` / `ParsedMmdDiagram` already available in `GradingPipeline` / `MmdPillarResult`. Governs U1, U2.
- **KTD3. Display-text rule per rubric element** — when the grader found a parsed student counterpart by rubric name/signature (including incorrect partial matches), snapshot stores student-formatted display text; when absent (`pf == null` in `ClassReflectionGrader`, or MMD element not present in diagram), store `null` and `ClassStructureService` falls back to rubric label. Governs U2, U3. Note: AE1 applies to same-name wrong-type/signature cases; a different member name (e.g. student wrote `name` when rubric expects `age`) is treated as absent and shows the rubric label per R7.
- **KTD4. Legacy fallback** — submissions without a snapshot file keep today's rubric-template display (no backfill). Governs U3.
- **KTD5. No API shape change** — reuse existing `ClassDetailDTO` / `MmdClassDTO` JSON; only the string fields change source. Frontend renders unchanged. Governs U4.

### Technical Design

**Snapshot file shape** (per submission, keyed by challenge UUID):

```json
{
  "<challengeId>": {
    "class": {
      "fields": { "<rubricFieldId>": { "name": "age", "scope": "private", "dataType": "String" } },
      "methods": { "<rubricMethodId>": { "name": "getAge", "scope": "public", "returnType": "int" } },
      "constructors": { "<rubricConstructorId>": { "name": "Person", "scope": "public", "params": "int age" } }
    },
    "mmd": {
      "attributes": { "<rubricMmdElementId>": { "name": "+ age : int", "type": "field" } },
      "relations": { "<rubricRelationId>": { "from": "Car", "to": "Vehicle", "relType": "association" } }
    }
  }
}
```

Only rubric-scoped element IDs are stored. `null` or missing entry → rubric fallback at read time.

**Write path:** `GradingService` (after each challenge grades) calls `ParsedSubmissionSnapshotBuilder.build(...)` with challenge rubric, `List<ParsedClass>`, `ParsedMmdDiagram`, and grading context; accumulates per-challenge maps; `ParsedSubmissionSnapshotStore.save(submissionId, map)` once before `LabResultAssembler`.

**Read path:** `ClassStructureService.buildClassData` / `buildMmdData` accept optional snapshot slice. For each rubric row: if snapshot has student display fields → format using same helpers as today (`resolveMasterDataLabel` for scope, `formatParams` for constructors); else → current rubric-derived strings. `ok` flags unchanged (`SubmissionCorrectIds`).

**Formatting:** Reuse existing `ClassStructureService` private formatters extracted or shared with `ParsedSubmissionSnapshotBuilder` so upload bundle and read APIs stay consistent.

### Assumptions

- Parsed structures from `ReflectionClassParser` and `MmdParser` at grade time are sufficient; no second parse on read.
- Ephemeral storage wipe on Render may delete snapshot files same as `_mmd_meta`; degraded fallback to rubric labels is acceptable (same class of risk as today).

### Sequencing

U1 (store + builder) → U2 (grading wire-up) → U3 (read-path merge) → U4 (frontend verification, no code expected) → U5 (tests).

---

## Implementation Units

### U1. Snapshot store and builder

**Covers:** R1–R4

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/service/ParsedSubmissionSnapshotStore.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/grading/ParsedSubmissionSnapshotBuilder.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/grading/ParsedSubmissionSnapshot.java` (new — typed model for JSON)

**Work:**
- Implement JSON read/write following `SubmissionMmdMetaStore` patterns.
- Builder walks rubric classes/members like `ClassReflectionGrader` and MMD comparison inputs; for each rubric element ID, capture student display fields when parsed counterpart exists by name/signature match.
- Add unit tests: `ParsedSubmissionSnapshotBuilderTest.java` with fixture rubric + parsed classes/diagram; assert present vs absent display entries.

**Test scenarios:**
- Same-name field with wrong type → snapshot stores student `name`, `scope`, `dataType`.
- Missing field → no snapshot entry for that rubric field ID.
- MMD relation present with wrong `relType` → snapshot stores student `from`, `to`, `relType`.
- Empty submission → snapshot file absent or empty challenge map.

### U2. Grade-time persistence wire-up

**Covers:** R1, R2, R4, R10

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/grading/GradingService.java`
- `backend/src/main/java/com/eiu/capstone/backend/grading/pipeline/GradingPipeline.java` (expose parsed class list on result if not already)
- `backend/src/main/java/com/eiu/capstone/backend/grading/AGENTS.md`

**Work:**
- After per-challenge grading, pass `ChallengeGradingContext.parsedByName()` and `MmdPillarResult.diagram()` into builder.
- Accumulate challenge snapshots; call `store.save(submissionId, ...)` before `LabResultAssembler.assemble`.
- Ensure re-upload of same attempt overwrites snapshot file (immutable per attempt, but replaceable on re-grade).

**Test scenarios:**
- `GradingServiceTest` or integration-style test: after grade, snapshot file exists with expected challenge keys.
- Re-grade same submission replaces prior snapshot content.

### U3. Read-path display merge

**Covers:** R5–R13, R12

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/service/ClassStructureService.java`
- `backend/src/main/java/com/eiu/capstone/backend/grading/LabResultAssembler.java`
- `backend/AGENTS.md`

**Work:**
- Load snapshot in `buildClassDataForSubmission` / `buildMmdDataForSubmission`.
- Merge snapshot display fields into DTO construction; preserve `compileError` and `SubmissionMmdMetaStore` behavior.
- When snapshot missing (legacy submissions): retain current rubric-template behavior (KTD4).
- Export paths (`exportRoster.js` / lecturer breakdown) consume API JSON — no change if DTO strings are correct.

**Test scenarios:**
- Snapshot present: `buildClassData` returns student `dataType` on wrong-type field with `ok=false`.
- Snapshot absent: output matches pre-change rubric labels (regression).
- Missing rubric member: DTO uses rubric name/scope/type with `ok=false`.

### U4. Frontend verification

**Covers:** R10, R11

**Files:**
- `frontend/src/components/student/StudentUI.jsx` (verify only)
- `frontend/src/components/lecturer/LecturerSubmissionDrawer.jsx` (verify only)
- `frontend/src/components/student/AGENTS.md`
- `frontend/src/components/lecturer/AGENTS.md`

**Work:**
- No component changes expected — DTO shape unchanged.
- Manual verify: upload lab with intentional wrong member type; confirm Class tab shows student type string.
- Manual verify: lecturer drawer matches student view for same `submissionId`.

### U5. Documentation

**Files:**
- `CONCEPTS.md` (already updated)
- `backend/src/main/java/com/eiu/capstone/backend/service/AGENTS.md`

**Work:**
- Document `_parsed_snapshot/` sidecar directory and fallback behavior.

---

## Verification Contract

| Check | Command / action |
|---|---|
| Backend unit tests | `mvn test` from `backend/` — must include new `ParsedSubmissionSnapshotBuilderTest` |
| Existing grading tests | `GradingServiceTest`, `PillarScoreAggregatorTest` — no regressions |
| Frontend build | `npm run build` from `frontend/` |
| Manual — student upload | Upload challenge folder; Class/MMD tabs show student-parsed text for present same-name members |
| Manual — history | Re-open past submission via dashboard; tabs match upload-time display when snapshot file exists |
| Manual — lecturer | Open submission drawer; export incorrect breakdown uses new display strings |

---

## Definition of Done

- [ ] `_parsed_snapshot/{submissionId}.json` written on every successful grade
- [ ] Class and MMD tab APIs return student-parsed display text for present rubric-scoped elements (R6, R9)
- [ ] Missing elements show rubric expected label with ✗ (R7)
- [ ] Legacy submissions without snapshot degrade to current behavior (KTD4)
- [ ] Student dashboard and lecturer drawer verified manually
- [ ] `mvn test` and `npm run build` pass
- [ ] `backend/AGENTS.md` documents snapshot sidecar
