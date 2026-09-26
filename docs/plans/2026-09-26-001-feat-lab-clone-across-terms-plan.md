---
title: Lab Clone Across Terms - Plan
type: feat
date: 2026-09-26
topic: lab-clone-across-terms
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Lab Clone Across Terms - Plan

## Goal Capsule

- **Objective:** Let lecturers selectively deep-clone labs from the previous current quarter into a target quarter—rubric structure and operational testcases only—via the New quarter form and a separate Copy lab control in Solution Management.
- **Product authority:** This plan owns selective lab copy across quarters (clone semantics, two lecturer entry points, previous-current source rule). Lab move/reassign, template libraries, export/import, and browsing arbitrary historical quarters are not active scope.
- **Open blockers:** None — ready for implementation.
- **Stop conditions:** Do not move labs. Do not copy submissions. Do not browse arbitrary historical quarters as clone sources.

## Product Contract

### Summary

Lecturers can copy chosen labs from the previous current quarter into another quarter. Each copy is a new lab with the same name and a full deep copy of challenges, classes, members, relations, weights, and operational testcases. Deadline, student visibility, and release date start fresh. Student submissions and progress are never copied. Entry points: optional multi-select on the New quarter form, and a Copy lab control beside the existing Add lab (+) blank-create flow.

### Problem Frame

Labs are bound to one term. Switching the current quarter hides prior labs from Solution Management and student submit. Lecturers must rebuild rubrics and OT by hand; there is no clone or migrate path today.

### Key Decisions

- **Deep clone, never move** (session-settled: user-directed — chosen over reassign `term_id`: preserves prior-quarter history). Governs R1, R6, R7.
- **Rubric + OT only; reset access settings** (session-settled: user-directed — chosen over carrying deadline/visibility/release: new quarter starts clean). Governs R2, R3.
- **New quarter form multi-select** (session-settled: user-directed — chosen over post-create dialog). Governs R8.
- **Separate Copy lab control; + stays blank create** (session-settled: user-directed — chosen over stuffing copy into +). Governs R9, R10.
- **Previous-current source only** (session-settled: user-directed — chosen over any past quarter). Governs R4, R5.

### Requirements

**Clone semantics**

- **R1.** Cloning creates a new lab on the target quarter; the source lab and its term binding are unchanged.
- **R2.** The clone includes the full rubric tree (challenges, classes, fields, methods, constructors, parameters, relations, weights, hasMmd) and all operational testcases for those challenges.
- **R3.** The clone does not copy deadline, student visibility, or release date; those use the same defaults as a newly created blank lab for the target quarter.
- **R4.** Copied labs keep the source lab name.
- **R5.** Lecturers may copy only from the previous current quarter (defined below); they choose which labs to include (zero or more).
- **R6.** Student submissions, progress, plagiarism rows, and attempt history are never part of a clone.
- **R7.** After clone, source and target labs are independent; later edits to one do not affect the other.

**Previous-current source**

- **R8.** On the New quarter form, the optional copy source is the quarter that is current when the form is submitted (the outgoing current quarter). If no current quarter exists, the copy section is unavailable or empty.
- **R9.** For Copy lab in Solution Management, the only selectable source labs are those belonging to the previous current quarter relative to the active current quarter. If that prior quarter cannot be determined or has no labs, Copy lab shows an empty/disabled state with a clear message.

**Entry points**

- **R10.** New quarter form: optional “Copy labs from…” multi-select listing labs from the outgoing current quarter; Create still succeeds with zero labs selected.
- **R11.** Solution Management: Add lab (+) remains blank create (name + quarter). A separate Copy lab control opens a picker for one or more labs from the previous current quarter into a chosen target quarter (default: current quarter when set).
- **R12.** Only lecturers can clone; students have no clone or migrate UI or API.

### Key Flows

1. **Rollover on New quarter** — Lecturer fills New quarter, optionally checks labs from the outgoing current quarter, clicks Create → new quarter exists; each checked lab is deep-cloned onto it (rubric+OT; fresh access settings).
2. **Copy lab later** — Lecturer opens Solution Management → Copy lab → selects labs from previous current → chooses target quarter → confirms → new labs appear when that quarter is current (and immediately in the sidebar if the target is the current quarter).

### Acceptance Examples

- **AE1.** Outgoing current quarter has Lab A and Lab B. Lecturer creates a new quarter, selects only Lab A, Create → new quarter has one lab named Lab A with the same structure and OT as source; Lab B is not on the new quarter; original Lab A remains on the old quarter with submissions intact. Covers R1, R2, R4–R6, R10.
- **AE2.** Lecturer creates a new quarter with no labs selected → new quarter has zero labs. Covers R10.
- **AE3.** Lecturer uses Copy lab to copy one prior-current lab into the current quarter → a new lab with the same name appears in the current-term lab list; deadline/visibility match blank-lab defaults, not the source. Covers R3, R9, R11.
- **AE4.** No current quarter (or no previous-current labs) → New quarter copy section empty/unavailable; Copy lab empty/disabled with clear messaging; blank create via + still works. Covers R8, R9.

### Scope Boundaries

**In scope**

- Deep clone of rubric + OT into a target quarter
- Selective multi-select on New quarter form
- Separate Copy lab control in Solution Management
- Previous-current-only source rule

**Out of scope**

- Moving or reassigning an existing lab’s term
- Copying from arbitrary historical quarters
- Lab template library
- Structure export/import JSON
- Copying student submissions or progress
- Changing student-facing submit rules beyond new labs appearing when their term is current

### Outstanding Questions

**Resolve Before Planning**

- None.

**Deferred to Planning** — resolved in Planning Contract KTDs below.

### Sources / Research

- No server clone API today; frontend `cloneDraft` is editor-state only (`SolutionManagement.jsx`).
- Create lab: `LabStructureService.createLab` — name + `termId`; deadline defaults to term end; empty challenges.
- Structure load/save: `loadForEditor` / `saveLabStructure`; OT separate via `TestcaseRubricService.loadForChallenge` / `saveForChallenge`.
- Term create: `TermService.createTerm` + optional `setCurrent`; New quarter UI in `TermManagement.jsx`.
- Lab list scoped to current term: `LabController.labsVisibleToCaller`.
- Grounding dossier: brainstorm scan for lab/term/OT surfaces.

## Planning Contract

### Key Technical Decisions

- KTD1. **DTO deep-clone via existing save paths** — Load source with `loadForEditor` + per-challenge `loadForChallenge`; create target shell with `createLab` (blank deadline defaults); assign fresh UUIDs for every entity id and rewrite cross-refs (outer class, relations, OT member/dispatch refs); `saveLabStructure` then `saveForChallenge` per challenge. Do not move rows or share ids. Governs R1–R4, R6–R7.
- KTD2. **Bulk clone API** — `POST /api/lecturer/labs/clone` body `{ sourceLabIds: UUID[], targetTermId: UUID }` returns created lab summaries. Single-lab is the one-element case. Lecturer-only. Governs R5, R11–R12.
- KTD3. **Clone-source listing** — `GET /api/lecturer/labs/clone-sources` returns `{ sourceTerm, labs: [{id,name}] }` for the previous-current quarter (empty when none). Governs R8–R9.
- KTD4. **Previous-current resolution** — Sort `findAllWithAcademicYear` by `yearLabel` desc, then `termNumber` desc. Find the current term; the next entry in that list is previous-current. If no current or no next entry → empty sources. New quarter form uses the **outgoing** current at submit time (capture before `setCurrent`), not this ordinal, when attaching `copyLabIds`. Governs R8–R9.
- KTD5. **Term create includes optional `copyLabIds`** — Extend `CreateTermRequest` with `List<UUID> copyLabIds` (optional). Same request: create term → optionally set current → clone each listed lab from the captured outgoing current onto the new term. Invalid ids (not in outgoing current) → 400 before any clone. Governs R10.
- KTD6. **Best-effort multi-clone after term exists** — Term create always commits. Clone failures return as a structured result (created term + per-lab success/error) so a failed OT remapping does not roll back the quarter. Copy-lab endpoint: clone sequentially; return successes + errors. Governs AE1–AE2 partial-failure clarity.
- KTD7. **Same name allowed** — No rename suffix; duplicate names in a term are allowed (no unique constraint today). Governs R4.
- KTD8. **Source authorization** — Clone rejects if any `sourceLabId` is not in the allowed source term for that call (outgoing current for term-create path; previous-current for `/clone`). Governs R5, R12.

### Technical Design

```
cloneLab(sourceId, targetTermId):
  source = loadForEditor(sourceId) + OT per challenge
  target = createLab(source.name, targetTermId)  // fresh deadline/visibility
  idMap = fresh UUIDs for challenges, classes, members, relations, testcases, invocations, assertions
  rewrite cross-refs using idMap
  saveLabStructure(target.id, remappedStructure)
  for each challenge: saveForChallenge(target.id, newChallengeId, remappedOt)
  return target summary
```

New quarter: `createTerm` captures `outgoingCurrent`, creates term, optional `setCurrent`, then `cloneLabs(copyLabIds, newTermId)` with source gate = outgoingCurrent.

Copy lab UI: `GET clone-sources` → multi-select → `POST clone` with `targetTermId` (default current).

### Assumptions and Dependencies

- `saveLabStructure` / `saveForChallenge` accept newly generated client UUIDs as inserts (existing upsert-by-id behavior).
- Lab names need not be unique per term.
- Lecturer JWT already gates `/api/lecturer/**`.

### Execution Order

1. U1 — Backend clone service + ID remap + APIs + term `copyLabIds`
2. U2 — Backend tests for clone + previous-current + term create
3. U3 — TermManagement New quarter multi-select
4. U4 — SolutionManagement Copy lab control
5. U5 — AGENTS / USER_GUIDE notes

## Implementation Units

### U1. Backend lab clone engine and APIs

- **Goal:** Deep-clone rubric+OT into a target term; expose clone + clone-sources; wire optional `copyLabIds` on term create.
- **Requirements:** R1–R12
- **Files:** `backend/src/main/java/com/eiu/capstone/backend/service/LabCloneService.java` (new), `LabStructureService.java`, `TestcaseRubricService.java` (reuse), `LecturerRubricController.java`, `TermService.java`, `DTO` for clone request/response and `CreateTermRequest`, `backend/src/main/java/com/eiu/capstone/backend/service/AGENTS.md`, `backend/AGENTS.md`
- **Approach:** KTD1–KTD8. Keep controllers thin; clone orchestration in `LabCloneService`.
- **Test scenarios:** See U2.
- **Verify:** Compiles; covered by U2 tests.

### U2. Backend clone tests

- **Goal:** Lock clone semantics, source gates, and term-create copy.
- **Requirements:** R1–R10, AE1–AE4
- **Files:** `backend/src/test/java/support/com/eiu/capstone/backend/service/LabCloneServiceTest.java` (new), extend `TermService*` support tests as needed
- **Test scenarios:**
  - Clone structure+OT; source unchanged; target has fresh deadline/visibility defaults; same name.
  - Clone rejects source lab not in allowed source term.
  - `previousCurrent` ordinal: current + older → older returned; only one term → empty.
  - `createTerm` with `copyLabIds` clones selected only; empty list clones none.
  - Multi-clone: one invalid id → 400 before clones (term-create path) or per-lab error on `/clone` after validation.
- **Verify:** `mvn test` from `backend/` (at least new support tests).

### U3. New quarter form — copy labs multi-select

- **Goal:** Optional lab checkboxes from outgoing current on New quarter create.
- **Requirements:** R8, R10, AE1–AE2, AE4
- **Files:** `frontend/src/pages/TermManagement.jsx`, `frontend/src/pages/AGENTS.md`
- **Approach:** When opening New quarter (or when terms load), if a current term exists, `GET /api/lecturer/labs/clone-sources` or list labs for current via existing APIs; pass `copyLabIds` in create body. Hide section when no current / no labs.
- **Verify:** Manual + `npm run build`.

### U4. Solution Management — Copy lab control

- **Goal:** Separate Copy lab UI beside +; blank create unchanged.
- **Requirements:** R9, R11, AE3–AE4
- **Files:** `frontend/src/pages/SolutionManagement.jsx`, `frontend/src/pages/AGENTS.md`, `frontend/AGENTS.md`
- **Approach:** Copy lab opens dialog: source labs from `clone-sources`, multi-select, target quarter from `/api/terms` (default current). POST `/clone`; refresh lab list when target is current.
- **Verify:** Manual + `npm run build`.

### U5. Docs closeout

- **Goal:** Document clone entry points and previous-current rule.
- **Requirements:** product clarity
- **Files:** `docs/USER_GUIDE.md`, `CONCEPTS.md` (if a clone term is warranted), `backend/AGENTS.md`, `frontend/src/pages/AGENTS.md`
- **Verify:** Doc skim; DOX pass on touched AGENTS.

## Verification Contract

- Backend: `mvn test` from `backend/`
- Frontend: `npm run build` from `frontend/`
- Manual: New quarter with partial lab select; Copy lab into current; source labs still on old quarter; student history on old labs intact

## Definition of Done

- [x] U1–U5 complete against R1–R12
- [x] Deep clone covers structure + OT; access settings reset
- [x] New quarter multi-select and Copy lab both work selectively
- [x] No move/reassign; no submission copy
- [x] AGENTS/USER_GUIDE updated
- [ ] No commit/PR unless user asks
