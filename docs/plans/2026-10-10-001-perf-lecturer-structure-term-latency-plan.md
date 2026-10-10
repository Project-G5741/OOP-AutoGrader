---
title: Lecturer Structure and Term Latency - Plan
type: perf
date: 2026-10-10
topic: lecturer-structure-term-latency
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Lecturer Structure and Term Latency - Plan

## Goal Capsule

**Objective:** Cut lecturer wait time for structure editor load, term create + lab sync, and Save Lab Structure (especially after solution-import replaces) so local use hits roughly half of today’s ~10s load/sync and keeps worst-case save under 20 seconds — shipping all three clocks in one release via round-trip collapse plus replace-aware rebuild.

**Product authority:** This Product Contract. Adjacent lecturer-tab first-paint work (`docs/plans/2026-10-07-002-perf-lecturer-tab-load-speed-plan.md`) and student/upload grading performance are surrounding work, not active scope for this bar.

**Open blockers:** None.

**Product Contract preservation:** Clarified only — brainstorm Q1–Q3 resolved as KD1–KD3; Outstanding Questions section cleared; no R-ID renumber or scope change.

## Product Contract

### Summary

Speed up three lecturer structure/term operations that still take ~10s or, after many import replaces, minutes of Save: structure editor load to a usable UI, creating a term and syncing ~2 medium labs, and Save Lab Structure. Prefer collapsing request/flush round-trips on the shared structure paths and using a replace-aware rebuild for heavy post-import saves — not async jobs.

### Problem Frame

Lecturers wait out these operations locally with no workaround. Observed today: structure editor fully usable ~10s; term create + sync of 2 medium labs ~10s; Save after drag-drop solution import varies from seconds to minutes when many challenges are replaced — the long wait is on Save, not on extract.

These clocks share the structure graph. Solution first paint already goes through lecturer tab bootstrap plus `loadForEditor` for the default lab. Term create/`sync-labs` deep-clone loads that graph then persists insert-only (up to 4 parallel write transactions). Editor Save after import currently issues one operational-testcase wipe request per replaced challenge, then one structure save; editor save still flushes per batch while the clone insert-only path defers to one commit flush. That combination turns high replace counts into wall-clock dominated by round-trips and sync work, not by the import compile step.

### Key Decisions

- KD1. **All three clocks in one release** (session-settled: user-directed — chosen over fixing one area alone: a partial fix is not worth shipping by itself). **Governs R1–R3, R10.**
- KD2. **Round-trip collapse + replace-aware rebuild (Approach A+B)** (session-settled: user-approved — chosen over A-only, B-heavy, or async/progressive jobs: meet the budget without a job/progress product surface). **Governs R4–R9.**
- KD3. **Primary measurement environment is local** (session-settled: user-directed — chosen over production-first: pain is felt on local UI+API). **Governs R11, SC1.**
- KD4. **Success bar is ~half of today’s ~10s for load/sync, and worst-case save under 20s** (session-settled: user-directed — chosen over interactive &lt;2s/&lt;3s/&lt;5s or adopting the existing Solution &lt;500ms tab-load bar for this sweep). **Governs R1–R3, SC1.**
- KD5. **Post-import minutes are a Save problem** (session-settled: user-directed — chosen over treating solution-import extract as the primary clock: import finishes; Save is where time stretches). **Governs R3, R6–R8, F3.**

### Actors

- A1. **Lecturer** (including dual-role on lecturer routes) authoring Solution Management and Quarters/Terms locally.

### Requirements

**Latency budgets**

- R1. Structure editor reaches a fully loaded, usable UI for the default selected lab in about half of today’s ~10s locally (target ~5s), measured from Solution tab open / editor load start to interactive editor.
- R2. Creating a new term and syncing about 2 medium-sized labs completes in about half of today’s ~10s locally (target ~5s), measured from submit of create-with-copy or sync until the UI reflects success (including per-lab clone errors if any).
- R3. Save Lab Structure after solution-import with many replaced challenges completes in under **20 seconds** worst case locally, measured from Save click to success/error UI. Ordinary small edits must not regress to worse than today’s typical short saves.

**Shared mechanism**

- R4. Structure load paths used by Solution first paint and by lab clone reduce avoidable serial work and round-trips so R1 and R2 can share the same gains.
- R5. Editor structure save reduces flush/request round-trip tax relative to today’s per-batch flush behavior, without requiring a background job.
- R6. After solution-import replaces, Save must not require one separate operational-testcase wipe network request per replaced challenge before the structure save.
- R7. When Save follows import replaces (or equivalent heavy challenge rewrites), those challenges use a **replace-aware rebuild** (wipe + insert-style persist) rather than member-by-member upsert of the old tree.
- R8. Replace-aware rebuild preserves today’s product rule that replaced challenges clear persisted operational testcases; light edits that are not replaces keep normal upsert semantics.
- R9. Term create with `copyLabIds` and Terms **Sync labs** both benefit from the faster clone/load/persist path; submissions remain never copied.

**Release and measurement**

- R10. The release that claims this work done must improve all three clocks (R1–R3); shipping only one is out of contract.
- R11. Acceptance timing is against the lecturer’s local UI + local API setup used for authoring (same class of environment as today’s ~10s / minutes observations). Production Vercel+Render may improve as a side effect but is not the pass bar for this contract.
- R12. Existing Solution “Loading data...” copy and always-fresh bootstrap visit semantics stay intact; this sweep does not reopen stale-while-revalidate.

### Key Flows

- F1. Structure editor load
  - **Trigger:** Lecturer opens Solution / structure editor for the default lab.
  - **Actors:** A1
  - **Steps:** Client requests first-useful Solution payload (or structure for the selected lab); server returns lookups, labs, and structure; editor becomes interactive.
  - **Outcome:** Usable editor within R1.
  - **Covered by:** R1, R4, R12

- F2. Term create + sync labs
  - **Trigger:** Lecturer creates a term with labs to copy, or syncs selected labs into a quarter.
  - **Actors:** A1
  - **Steps:** Term (if create) commits; selected source labs deep-clone into the target quarter (structure + OT; no submissions); UI shows created labs and any per-lab errors.
  - **Outcome:** Completes within R2 for ~2 medium labs.
  - **Covered by:** R2, R4, R9

- F3. Post-import Save Lab Structure
  - **Trigger:** Lecturer drag-drops a solution folder (extract finishes), then clicks Save Lab Structure with one or more replaced challenges.
  - **Actors:** A1
  - **Steps:** Save clears OT for replaced challenges without an N-request wipe waterfall; heavy replaced challenges rebuild insert-style; structure persists; UI returns success/error.
  - **Outcome:** Completes within R3 worst case.
  - **Covered by:** R3, R5–R8

### Acceptance Examples

- AE1. Solution load locally
  - **Covers:** R1, R12
  - **Given:** A current-term lab with a typical multi-challenge structure.
  - **When:** The lecturer opens Solution.
  - **Then:** The structure editor for the default lab is usable in ~5s (about half of prior ~10s), with “Loading data...” while waiting.

- AE2. Create term + sync two labs
  - **Covers:** R2, R9
  - **Given:** An outgoing current quarter with two medium labs (structure + OT).
  - **When:** The lecturer creates a new term copying those labs (or syncs them into a target quarter).
  - **Then:** The operation finishes in ~5s; both labs appear on the target (or clone errors are shown without rolling back the term on create).

- AE3. Save after many import replaces
  - **Covers:** R3, R6–R8
  - **Given:** The lecturer imported a solution folder that replaced several existing challenges; extract already finished.
  - **When:** They click Save Lab Structure.
  - **Then:** Save finishes in under 20s; replaced challenges have no leftover OT from before the replace; the editor shows the saved structure.

- AE4. Light edit save does not pick the heavy path
  - **Covers:** R3, R8
  - **Given:** A small structure tweak with no import-replaced challenges pending.
  - **When:** The lecturer saves.
  - **Then:** Behavior stays upsert-style for that edit and does not get slower than today’s typical short save.

### Scope Boundaries

**In scope**

- Structure editor load (Solution first paint / structure for default lab)
- Term create with lab copy and Terms sync-labs clone path
- Save Lab Structure, including post-import replace worst case
- Shared load/save/clone latency mechanisms needed so all three clocks move together

**Deferred for later**

- Matching the existing lecturer-tab **&lt;500ms** Solution first-useful-data bar (`docs/plans/2026-10-07-002-perf-lecturer-tab-load-speed-plan.md`)
- Async/background clone or save with progress UI
- Speeding solution-import extract/compile itself
- Production-only tuning as the acceptance environment

**Non-goals**

- Student upload/grading pipeline performance
- Changing clone semantics to copy submissions
- Redesigning Structure editor UX beyond latency and save-path behavior needed for R1–R3

### Success Criteria

- SC1. On local authoring setup: load ~5s, term+2-lab sync ~5s, worst-case post-import save &lt;20s (R1–R3), with R10 satisfied in the same release.
- SC2. AE1–AE4 pass manually; no intentional product regression on OT clear-on-replace or clone best-effort errors.

### Assumptions and Dependencies

- ASSUME1. Local API talks to a remote Postgres (Neon-class RTT); round-trip count remains a first-order cost even when the UI is local.
- ASSUME2. “Medium” labs match the lecturer’s current authoring labs used for the ~10s sync observation; planning may pin concrete challenge/class counts when measuring.
- DEP1. Existing Solution bootstrap, `loadForEditor`, clone insert-only persist, and solution-import replace tracking remain the product surfaces this work accelerates.

<!-- ce-section: work-relationships -->
### How This Work Fits Together

This plan owns the **lecturer structure/term latency sweep** (load + term sync + structure save) as one release unit.

- Adjacent: Lecturer tab first-paint under 500ms — `docs/plans/2026-10-07-002-perf-lecturer-tab-load-speed-plan.md`
  - **Shares** Solution bootstrap / first useful data surface with R1
  - **Can proceed independently of** this plan’s ~5s / &lt;20s bar; this plan does not claim the 500ms bar
- Later candidate: Async clone/save progress UX
  - **Still to decide**; only needed if R1–R3 cannot be met after round-trip collapse + replace-aware rebuild

### Sources / Research

- Brainstorm grounding: compound-engineering temp `ce-brainstorm/structure-term-latency-*/grounding.md`
- Learnings: `docs/solutions/architecture-patterns/lab-clone-across-terms.md`
- Adjacent: `docs/plans/2026-10-07-002-perf-lecturer-tab-load-speed-plan.md`, `docs/plans/2026-09-26-001-feat-lab-clone-across-terms-plan.md`
- Hot paths: `LabStructureService`, `LabCloneService`, `TermService`, `LecturerTabBootstrapService`, `TestcaseRubricService.deleteAllForChallenges`, `SolutionManagement.jsx`

---

## Planning Contract

### Key Technical Decisions

- KTD1. **Replace eligibility is client-supplied** (session-settled: user-approved — chosen over server guessing from payload shape: preserves R8 light-edit upsert). Structure PUT carries `replacedChallengeIds` (or equivalent wrapper field); empty/absent means normal upsert for all challenges. **Implements R6–R8; Governs U1, U2.**
- KTD2. **Editor saves defer flushes for all editor saves** (session-settled: user-approved — chosen over replace-path-only deferral: same Neon RTT tax hits light and heavy saves). Keep `loadSaveContext` + upsert for non-replaced challenges; set `deferStructureFlush` for the editor path like insert-only, with one flush at commit. Do not switch the whole editor save to empty insert-only context. **Implements R5; Governs U1.**
- KTD3. **SC1 proof is DevTools + timing-log** (session-settled: user-approved — chosen over a CI timing harness: matches local authoring measurement). Use Network/wall-clock for AE1–AE3; rely on existing `[timing] Save lab structure` when `app.grading.timing-log=true`. **Implements R11, SC1; Governs U4.**
- KTD4. **In-save OT wipe uses `deleteAllForChallenges` once** — Before replace rebuild / member sync, call set-based OT delete for the replaced id set; FE removes the N× `PUT .../testcases` `[]` loop. Order: wipe OT → cascade old challenge tree with `skipTestcaseGuards` → insert-style sync for those challenges. **Implements R6–R8; Governs U1, U2.**
- KTD5. **Replace rebuild is per-challenge, not whole-lab insert-only** — Only ids in `replacedChallengeIds` take wipe+rebuild; other challenges keep upsert against `SaveContext`. Payload challenge ids must belong to the lab. **Implements R7, R8, AE4; Governs U1.**
- KTD6. **Solution bootstrap overlaps independent reads** — In `LecturerTabBootstrapService.solution()`, run master-data category loads, term list, current-term labs, and default `loadForEditor` as short independent read work that can proceed concurrently (dedicated small executor or equivalent bounded fan-out), then join on the request thread — not blocking JDBC “overlap” on one thread, and not nested joins on a shared persistence context / Render CPU-capped pool. Do not reopen multi-minute TTL or change always-fresh visit semantics (R12). **Implements R1, R4, R12; Governs U3.**
- KTD7. **Clone benefits primarily via shared load + existing insert-only write** — Keep short read TX + `REQUIRES_NEW` write + ≤4 parallel clones (`docs/solutions/architecture-patterns/lab-clone-across-terms.md`). Any remaining R2 gap after U1/U3 is clone-local (e.g. OT batch already present); do not introduce async clone jobs. **Implements R2, R9; Governs U3, U4.**
- KTD8. **DTO/request shape** — Prefer extending the structure save request so GET `LabStructureResponse` stays unchanged for load/bootstrap; if a wrapper record is clearer than adding a nullable field to the response DTO, use a dedicated save request type on `PUT .../structure` only. **Implements R6; Governs U1, U2.**

### High-Level Technical Design

```mermaid
sequenceDiagram
  participant FE as SolutionManagement
  participant API as PUT /structure
  participant OT as TestcaseRubricService
  participant Struct as LabStructureService
  participant DB as Postgres

  FE->>API: structure + replacedChallengeIds
  Note over FE: No N× PUT testcases []
  API->>OT: deleteAllForChallenges(replaced)
  OT->>DB: set-based OT DELETE
  API->>Struct: rebuild replaced; upsert others; defer flush
  Struct->>DB: batched writes + one flush
  API-->>FE: saved structure
```

Load/sync share `loadForEditor` / bootstrap overlap (KTD6); clone write path already insert-only (KTD7).

### Assumptions

- ASSUME3. Extending the structure save body is acceptable without a new REST resource.
- ASSUME4. Representative “many replaces” for R3 is on the order of several challenges in one import (same class of labs used for today’s minutes observation).
- Frontend automated tests remain out of scope; `npm run build` + manual AE checks suffice for FE.

### System-Wide Impact

- Lecturers: faster Solution open, quarter sync, and post-import Save.
- Backend: structure save TX does more work but far fewer client round-trips; watch Neon TX duration on huge labs.
- Burst guard: folding wipe PUTs reduces write-burst consumption on Save.
- Docs: `backend/.../service/AGENTS.md` and pages AGENTS note the folded wipe + replace-aware save.

### Risks

| Risk | Mitigation |
|---|---|
| Single heavy save TX times out on Neon | KTD2/KTD4/KTD5 cut RTTs; if still over budget, escalate to deferred async (out of scope) rather than silent half-ship |
| Replace without OT wipe first → 422 guards | KTD4 order: wipe then `skipTestcaseGuards` cascade |
| Client omits/forgets `replacedChallengeIds` | Light path only; OT may linger — FE must send ids whenever `importReplacedChallengeIds` is non-empty; clear only after successful save |
| Bootstrap parallel queries on small Render | KTD6: bounded fan-out + join on request thread; no shared EntityManager across workers; no nested pool joins |
| R10 partial ship | DoD requires AE1–AE3 all green in one release |

### Alternatives Considered

- **Async save/clone jobs** — Rejected in Product Contract (KD2).
- **Server-inferred replace detection** — Rejected (KTD1) for R8 safety.
- **Whole-lab insert-only on every editor save** — Rejected; breaks light upsert and forces full rewrite cost.
- **Keep N× OT wipe PUTs** — Rejected; primary cause of post-import minutes.

### Open Questions

**Deferred to Implementation**

- IQ1. Exact JSON field name / wrapper vs optional list on save DTO — pick the smallest change that keeps GET response stable (KTD8).
- IQ2. Whether `updateLabDeadline` / `updateLabStudentAccess` should stop calling full `buildLabStructureResponse` (adjacent; only if R1 still misses after U3).

---

## Implementation Units

### U1. Structure save: in-TX OT wipe, replace-aware rebuild, deferred flush

**Goal:** Meet R3/R5–R8 on the backend: one structure save handles OT clear + replace rebuild + fewer flushes.

**Files:**
- Modify: `backend/src/main/java/service/LabStructureService.java`
- Modify: `backend/src/main/java/service/TestcaseRubricService.java` (wire/call `deleteAllForChallenges` from save if not already injectable)
- Modify: `backend/src/main/java/DTO/rubric/LabStructureResponse.java` and/or new save-request DTO under `DTO/rubric/`
- Modify: `backend/src/main/java/controller/LecturerRubricController.java`
- Modify: `backend/src/main/java/service/AGENTS.md`
- Modify: `backend/src/test/java/support/com/eiu/capstone/backend/service/LabStructureServiceSaveTest.java`

**Approach:** Accept `replacedChallengeIds` on PUT structure (KTD1/KTD8). At start of editor save: `deleteAllForChallenges` for those ids (KTD4); for each replaced id present in DB, cascade-delete structure with `skipTestcaseGuards` then sync payload as inserts; other challenges keep existing upsert via `loadSaveContext` (KTD5). Enable `deferStructureFlush` for the editor path for the whole save, single flush at end (KTD2). Keep timing-log block. Invalidate rubric cache as today.

**Test scenarios:**
- Save with `replacedChallengeIds` containing a challenge that had OT → OT rows gone after save; new structure persisted.
- Save with empty/absent replaced list → upsert behavior; OT on untouched challenge preserved (AE4).
- Replace path does not 422 on prior OT member refs (guard skipped after wipe).
- Light member edit still updates in place (not full lab wipe).
- Timing path still returns request payload without post-save `loadForEditor`.

**Verification:** `mvn test` from `backend/` focusing `LabStructureServiceSaveTest` (and any new cases).

**Depends on:** None.

**Covers:** R3, R5–R8, F3, AE3, AE4, KTD1, KTD2, KTD4, KTD5, KTD8

---

### U2. Frontend: single Save request after import replaces

**Goal:** Remove N× OT wipe PUTs; send replaced ids with structure save; clear local replace set only on success.

**Files:**
- Modify: `frontend/src/pages/SolutionManagement.jsx`
- Modify: `frontend/src/pages/AGENTS.md` (save/wipe contract note)

**Approach:** In `handleSave`, drop the `for (challengeId of importReplacedChallengeIds)` wipe loop. Include `replacedChallengeIds: importReplacedChallengeIds` (or agreed field) on the structure PUT body. Keep toast/error handling; clear `importReplacedChallengeIds` only after successful save (existing success path).

**Test scenarios:**
- After import replace, Network tab shows one structure PUT and zero testcase wipe PUTs before it.
- Failed structure save leaves `importReplacedChallengeIds` so retry still sends ids.
- `npm run build` succeeds.

**Verification:** Manual Network tab (AE3); `npm run build`.

**Depends on:** U1 (API accepts the field).

**Covers:** R6, F3, AE3, KTD1

---

### U3. Solution bootstrap / loadForEditor round-trip reduction

**Goal:** Cut R1 (and shared R4 load cost for clone) without changing always-fresh bootstrap semantics.

**Files:**
- Modify: `backend/src/main/java/analytics/service/LecturerTabBootstrapService.java`
- Modify: `backend/src/main/java/service/LabStructureService.java` (`buildLabStructureResponse` / repository batch helpers as needed)
- Modify: `backend/src/test/java/authorization/com/eiu/capstone/backend/security/LecturerTabBootstrapAuthTest.java` (only if contract/status changes)
- Optional support test if bootstrap assembly is extracted/testable

**Approach:** Concurrent independent reads in `solution()` per KTD6 (bounded fan-out, join on request thread; no shared EntityManager across workers). Inside `buildLabStructureResponse`, keep batched `findBy*In` queries; remove any remaining avoidable serial hops discovered during implementation. Do not serve bootstrap from multi-minute TTL caches. Preserve “Loading data...” on FE (R12).

**Test scenarios:**
- `GET /api/lecturer/bootstrap/solution` still returns lookups + labs + default structure (auth still lecturer-only).
- Cold Solution open locally ~5s for representative lab (AE1 / KTD3).
- Clone of two medium labs benefits from faster `loadForEditor` (AE2 partial).

**Verification:** Manual AE1; `mvn test` for auth/support tests touched; compare wall-clock before/after.

**Depends on:** None (can parallel U1).

**Covers:** R1, R4, R12, F1, AE1, KTD6, KTD7

---

### U4. Clone/term sync smoke + AGENTS + SC1 checklist

**Goal:** Prove R2/R9/R10 with U1–U3 landed; document contracts; no product regression on clone errors.

**Files:**
- Modify: `backend/src/test/java/support/com/eiu/capstone/backend/service/LabCloneServiceTest.java` (regression if save/load contracts shift)
- Modify: `backend/src/main/java/service/AGENTS.md` (clone + editor save flush/replace notes)
- Modify: `frontend/src/pages/AGENTS.md` if not fully covered in U2
- Optional: short note in `docs/HOW_IT_RUNS.md` only if operator-facing timing guidance is needed (skip if AGENTS suffice)

**Approach:** Re-run/extend clone support tests for insert-only + remap. Manually time term create with `copyLabIds` of 2 medium labs and/or Terms sync-labs (AE2). Confirm all three clocks in one build (R10). Capture timing-log lines for save when diagnosing misses (KTD3).

**Test scenarios:**
- Existing LabCloneServiceTest cases still pass.
- Manual AE2 ~5s for 2 medium labs locally.
- Manual AE1 + AE3 + AE4 in the same build as AE2.

**Verification:** `mvn test`; manual SC1 checklist.

**Depends on:** U1, U3 (U2 for full AE3).

**Covers:** R2, R9, R10, R11, SC1, SC2, F2, AE2, KTD3, KTD7

---

## Verification Contract

- Backend: `mvn test` from `backend/` (at least `LabStructureServiceSaveTest`, `LabCloneServiceTest`, `LecturerTabBootstrapAuthTest` if touched).
- Frontend: `npm run build` from `frontend/` (or root build used by the team).
- Manual local (KTD3): AE1 Solution open; AE2 term create/sync 2 labs; AE3 post-import multi-replace Save &lt;20s; AE4 light edit; Network confirms no N× wipe PUTs on AE3.
- With `app.grading.timing-log=true`, inspect `[timing] Save lab structure` (`load` / `sync` / `total`) when Save still feels slow.

## Definition of Done

- U1–U4 complete; R1–R3 budgets met on local authoring setup in one release (R10).
- Product rules preserved: OT clear on replace; light edit upsert; clone never copies submissions; Solution loading copy and always-fresh bootstrap unchanged (R12).
- AGENTS contracts updated for folded wipe + replace-aware save + editor flush deferral.
- No launch-blocking open questions remain (IQ1–IQ2 are implementation choices only).
