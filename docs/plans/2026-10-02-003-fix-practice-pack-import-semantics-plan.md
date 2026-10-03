---
title: Practice Pack Import Semantics - Plan
type: fix
date: 2026-10-02
topic: practice-pack-import-semantics
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Practice Pack Import Semantics - Plan

## Goal Capsule

- **Objective:** Make desktop practice pack import behave consistently for term wipe, lab add/replace, leftover files, and user-visible errors — including fixing same-name lab import that currently surfaces as “Server Busy.”
- **Product authority:** This Product Contract. Pack filename rules and export speed remain owned by `docs/plans/2026-10-02-002-feat-practice-pack-names-and-speed-plan.md`.
- **Open blockers:** None.
- **Stop conditions:** Do not remove the UI replace-confirm for same-name lab packs. Do not invent a confirm path for manual `rubric/` copies. Do not change pack crypto/signing.
- **Execution:** Code.
- **Product Contract preservation:** Product Contract unchanged in scope; R3 clarified that same-name confirm is scoped to the pack’s term (call-out default affirmed by user confirm without redirect).

---

## Product Contract

### Summary

Students import term and lab `.agpack` files in the desktop practice app with clear replace rules: a term pack wipes labs and clears leftover lab pack files; a lab pack adds or replaces by name (UI confirm when the name already exists); invalid packs and conflict prompts show specific messages instead of “Server Busy.”

### Problem Frame

Term import already wipes labs on restart, and lab import already aims to add or replace by name, but same-name UI import can fail with a generic “Server Busy,” leftover lab pack files can override a freshly imported term pack on restart, and friendly-error mapping hides specific pack rejection reasons.

### Key Decisions

- **Keep UI replace confirm for same-name lab packs** (session-settled: user-directed — chosen over silent auto-replace or UI-only-vs-manual split with auto-replace in UI). **Governs R3, R8.**
- **Term UI import deletes lab pack files in `rubric/`** (session-settled: user-directed — chosen over leaving lab packs to re-apply after term wipe). **Governs R2, R6.**
- **Cross-quarter lab packs are allowed** (session-settled: user-directed — chosen over reject-or-wipe-on-mismatch). **Governs R5.**
- **Replace-by-name may drop local practice history** (session-settled: user-directed — chosen over an extra history-loss warning). **Governs R4.**
- **Same-UUID lab re-import is silent** (session-settled: user-directed — chosen over always confirming updates). **Governs R4.**
- **Manual `rubric/` path stays silent** (session-settled: user-directed — chosen over bootstrap confirm/block). **Governs R7.**
- **Show specific invalid-pack reasons** (session-settled: user-directed — chosen over a single generic invalid message). **Governs R8.**
- **Multiple term packs: newest-by-mtime wins** (session-settled: user-directed — chosen over fail-if-multiple or always-keep-one-on-restart). **Governs R9.**

### Requirements

**Core import rules**

- R1. Importing a term (quarter) pack wipes all current practice labs and loads the labs from that pack on the next successful restart apply.
- R2. When a term pack is imported via **Import rubric pack**, all lab pack files (`Rubric_{name}.agpack`) under `rubric/` are deleted so the term pack alone defines the lab set after restart.
- R3. Importing a lab pack whose lab name does not match an existing practice lab **in that pack’s term** adds that lab; when the name matches case-insensitively **within that term** (and the lab id differs), the UI asks for replace confirmation, then stages the pack and replaces that lab on restart. A same name in another quarter does not trigger confirm (R5).
- R4. Lab replace is by name: different UUID with the same name replaces the existing row (local history for the old row may disappear); same UUID with the same name overwrites the pack file with no confirm and updates on restart.
- R5. A lab pack whose term identity differs from currently loaded labs is allowed: the pack’s term is ensured/switched as needed and only that lab is added or replaced; labs from other quarters may remain.

**Paths and errors**

- R6. UI term import also continues to delete sibling term packs in `rubric/` (keep only the imported term pack file), in addition to R2’s lab-pack cleanup.
- R7. Manual copy into `rubric/` plus restart applies term wipe / lab add-or-replace with no confirm dialog; confirm remains UI-only (R3).
- R8. UI import must surface specific user-visible reasons for same-name conflict (confirm dialog), invalid filename, content/name mismatch, legacy UUID names, and corrupt/unsigned packs — never map those cases to “Server Busy.”
- R9. If multiple term packs remain on disk (e.g. manual copies), bootstrap applies the newest-by-mtime term pack; UI term import per R6 still cleans sibling term packs.

### Actors

- A1. Student (desktop practice) — imports packs via UI or places files under `rubric/`, then restarts.

### Key Flows

- F1. Term pack via UI
  - **Trigger:** Student chooses a `Rubric_{year}_Q{n}.agpack` in **Import rubric pack**.
  - **Actors:** A1
  - **Steps:** Pack validates; sibling term packs and all lab packs in `rubric/` are removed; imported term pack is saved; fingerprint cleared; student restarts; bootstrap wipes labs and loads the term pack.
  - **Covered by:** R1, R2, R6
- F2. Lab pack same name via UI
  - **Trigger:** Student imports a lab pack whose lab name already exists in that pack’s term (different UUID).
  - **Actors:** A1
  - **Steps:** API reports conflict with conflict payload; UI shows replace confirm; on confirm, pack is staged; on restart, existing same-name lab is replaced.
  - **Covered by:** R3, R4, R8
- F3. Lab pack same UUID via UI
  - **Trigger:** Student re-imports an updated pack for a lab already present with the same id.
  - **Actors:** A1
  - **Steps:** No confirm; file overwritten; restart reloads updated rubric.
  - **Covered by:** R4
- F4. Invalid pack via UI
  - **Trigger:** Student selects a legacy, renamed, or corrupt pack.
  - **Actors:** A1
  - **Steps:** Import fails; toast/message shows the specific rejection reason.
  - **Covered by:** R8

### Acceptance Examples

- AE1. Term wipe clears lab files
  - **Covers:** R1, R2, R6
  - **Given:** `rubric/` has a term pack and `Rubric_Lab 2.agpack`
  - **When:** Student imports a new term pack via UI and restarts
  - **Then:** Lab pack files are gone; practice labs match only the new term pack
- AE2. Same-name lab shows confirm, not Server Busy
  - **Covers:** R3, R8
  - **Given:** Labs from a term pack are loaded, including “Lab 2”
  - **When:** Student imports `Rubric_Lab 2.agpack` whose lab id differs from the loaded row
  - **Then:** Replace confirm appears; no “Server Busy” toast
- AE3. Same-UUID update is silent
  - **Covers:** R4
  - **Given:** “Lab 2” is loaded with id X
  - **When:** Student imports a lab pack for “Lab 2” with id X
  - **Then:** Success toast without confirm; restart loads the updated pack
- AE4. Cross-quarter lab allowed
  - **Covers:** R5
  - **Given:** Q1 labs are loaded
  - **When:** Student imports a lab pack whose term is Q2
  - **Then:** Import succeeds; that lab is added/replaced under Q2; Q1 labs may still be present
- AE5. Invalid name shows specific reason
  - **Covers:** R8
  - **Given:** A legacy `{uuid}.lab.agpack` file
  - **When:** Student imports it via UI
  - **Then:** Message indicates the required `Rubric_…` format (not “Server Busy”)

### Success Criteria

- S1. Same-name lab import via UI consistently reaches the replace confirm (or silent same-UUID update) instead of “Server Busy.”
- S2. After a UI term import and restart, no leftover lab pack can override the term’s labs.
- S3. Invalid pack and conflict paths show actionable specific copy.

### Scope Boundaries

- **In:** Desktop UI import staging, bootstrap apply semantics for leftover files, conflict confirm wiring, friendly-error mapping for pack import, docs/`CONCEPTS` copy for these rules.
- **Out:** Lecturer export naming/speed (`docs/plans/2026-10-02-002-feat-practice-pack-names-and-speed-plan.md`); pack crypto/signing changes; web **Download practice folder**; inventing a confirm UX for manual folder drops.

<!-- ce-section: work-relationships -->
### How This Work Fits Together

- **This plan owns:** Desktop practice pack import/replace semantics and user-visible import errors.
- Pack naming + export speed (`docs/plans/2026-10-02-002-feat-practice-pack-names-and-speed-plan.md`) — **Can proceed independently of** this plan; **Shares** `Rubric_…` filename acceptance and desktop import entry points.
- Student runtime zip download (`docs/plans/2026-10-02-001-perf-practice-folder-download-plan.md`) — **Can proceed independently of** this plan.

---

## Planning Contract

### Key Technical Decisions

- KTD1. **Conflict HTTP body must always include `conflicts`** — Prefer characterize-first: POST a same-name different-id import and inspect the live 409 body. `DesktopPackController` already has a local `@ExceptionHandler` for `LabNameConflictException`; only change exception ancestry / global advice if the observed body lacks `conflicts`. Empty `conflicts` leaves the UI silent (no dialog, no toast); that is a distinct failure from “Server Busy.” **Implements R3, R8, AE2.**
- KTD2. **Teach `apiError` an explicit desktop-import allowlist** — `DesktopRubricPackImport` throws `Error(backendMessage)` then calls `toFriendlyError(err, 'import')`, which falls through to “Server Busy.” Add an `import` (or desktop-pack) context in `apiError.js` that surfaces backend messages for 400/409/422 the same way `save`/`delete`/`testcase-*` do, and update `frontend/AGENTS.md` to document that allowlist exception. Prefer `readFriendlyApiError(response, 'import')` on failed responses. Reserve “Server Busy” for network/5xx. **Implements R8, AE5.**
- KTD3. **Term UI staging deletes lab packs** — extend `DesktopPackStageService.stageUpload` for `PackKind.TERM` to delete every on-disk lab pack under `rubric/` (mirror `deleteOtherTermPacks`), then write the new term pack. Do not delete lab packs on bootstrap when a term pack applies from a manual copy (R7). **Implements R2, R6, AE1.**
- KTD4. **Same-name confirm stays term-scoped** (session-settled: user-approved — confirmed without redirect on the cross-quarter call-out; aligns with R5). Keep `findLabNameConflicts` filtered by the pack’s `termId`; do not confirm against other quarters’ labs. **Implements R3, R5.**

### Approach

1. Harden conflict response shape (KTD1) so the existing replace dialog always receives `conflicts`.
2. Fix desktop import friendly errors (KTD2) so 400/409 reasons and dialog path never collapse to “Server Busy.”
3. On UI term import, delete lab pack files as well as sibling term packs (KTD3).
4. Cover with desktop-profile tests; update student-facing desktop docs to match.

### Sequencing

U1 first for conflict body + term file cleanup. U2 may start once the 409 `ImportConflictDTO` contract is agreed (soft-depends on U1); AE2 still needs both. U3 after U1 (optional characterize-before-change may run first as a throwaway check). U4 last.

### Risks

- Observed same-name failure may be silent empty-conflicts 409 and/or non-409→“Server Busy” mapping — characterize the live response before rewriting exception ancestry (KTD1).
- If only the FE is fixed and 409 still omits `conflicts`, the dialog stays broken (silent). U1 and U2 must both land for AE2.
- Manual term drop + leftover lab packs still overlay on restart (product-intended under R7); docs must say UI term import is the path that clears lab packs.

### Research breadcrumbs

- Stage + conflict: `backend/src/main/java/com/eiu/capstone/backend/desktop/DesktopPackStageService.java`, `DesktopPackController.java`
- Global 409 strip risk: `backend/src/main/java/com/eiu/capstone/backend/exception/GlobalExceptionHandler.java` (`ResponseStatusException` → `ErrorResponse`)
- FE mapping: `frontend/src/utils/apiError.js`, `frontend/src/components/student/DesktopRubricPackImport.jsx`
- Bootstrap term/lab apply: `DesktopPackBootstrapService.java` (`importTermPack` then lab packs)
- Existing bootstrap tests: `backend/src/test/java/integration/.../DesktopPackBootstrapIntegrationTest.java`

---

## Implementation Units

### U1. Conflict 409 payload + term import clears lab packs

- **Goal:** Same-name lab import always returns a conflict DTO with `conflicts`; UI term import deletes lab pack files under `rubric/`.
- **Requirements:** R1, R2, R3, R5, R6, R8; AE1, AE2, AE4; KTD1, KTD3, KTD4
- **Files:** `backend/src/main/java/com/eiu/capstone/backend/desktop/DesktopPackStageService.java`, `backend/src/main/java/com/eiu/capstone/backend/desktop/DesktopPackBootstrapService.java` (lab-pack delete helper), `backend/src/main/java/com/eiu/capstone/backend/controller/DesktopPackController.java`, optionally `backend/src/main/java/com/eiu/capstone/backend/exception/GlobalExceptionHandler.java`; tests under U3
- **Approach:**
  1. Characterize live 409 body first (KTD1); only then adjust exception/handler so `ImportConflictDTO(conflicts, message)` is guaranteed.
  2. Add `deleteLabPacks` (or equivalent) on bootstrap/stage: parse filenames with `DesktopPackFileNames`, delete `PackKind.LAB` files; call it from term `stageUpload` alongside `deleteOtherTermPacks`.
  3. Verify/keep `findLabNameConflicts` termId-scoped (KTD4); do not broaden to other quarters.
  4. Leave bootstrap term apply unchanged regarding lab pack file deletion (manual overlay per R7).
- **Patterns to follow:** Existing `deleteOtherTermPacks`; controller `@ExceptionHandler(LabNameConflictException)` shape.
- **Test scenarios:** See U3.
- **Verification:** Conflict staging returns JSON with non-empty `conflicts` when same name/different id exists in term; after staging a term pack, no `Rubric_*.agpack` lab files remain in the test rubric dir.
- **Dependencies:** None

### U2. Desktop import UI error + confirm wiring

- **Goal:** UI shows replace confirm on conflict and specific rejection toasts for invalid packs — never “Server Busy” for those cases.
- **Requirements:** R3, R8; AE2, AE5; KTD2
- **Files:** `frontend/src/utils/apiError.js`, `frontend/src/components/student/DesktopRubricPackImport.jsx`, `frontend/AGENTS.md` (import allowlist exception)
- **Approach:**
  1. Add `import` handling in `readFriendlyApiError` / `toFriendlyError`: for 400/409/422 prefer backend `message` when present; keep network/5xx as “Server Busy.” Document the allowlist in `frontend/AGENTS.md` (KTD2).
  2. In `DesktopRubricPackImport`, on non-OK responses use `readFriendlyApiError(response, 'import')` instead of `throw new Error(rawMessage)` + `toFriendlyError(..., 'import')` that discards the message.
  3. Keep 409 → set `conflicts` + dialog. If 409 body lacks `conflicts`, toast the friendly message only (do not open a confirm that cannot name the lab); U1 should make this path rare.
  4. Align API base fallback with desktop banner (`VITE_API_URL` or desktop `http://127.0.0.1:18002`) so import does not drift from status polling.
- **Patterns to follow:** `readFriendlyApiError` contexts for `save`/`delete`; `DesktopPracticeStatusBanner` API base fallback.
- **Test scenarios:** Manual — AE2 and AE5 in the practice app; `npm run build:desktop` must succeed.
- **Verification:** Same-name different-id import opens replace dialog; legacy/invalid filename toast shows format guidance, not “Server Busy.”
- **Dependencies:** Soft-depends on U1 conflict body for AE2; may proceed once ImportConflictDTO contract is agreed

### U3. Desktop import integration tests

- **Goal:** Automate conflict staging and term-clears-lab-files behavior under the `desktop` profile.
- **Requirements:** R1, R2, R3, R4, R5, R6, R8; AE1, AE2, AE3, AE4; KTD1, KTD3, KTD4
- **Files:** Prefer a new integration test class under `backend/src/test/java/integration/com/eiu/capstone/backend/desktop/` with per-test temp rubric dirs (avoid mutating the shared bootstrap fixture home); reuse signed pack helpers / `DesktopPackFileNames`
- **Approach:**
  1. Optional characterize-first POST against current code before U1 exception changes.
  2. POST `/api/desktop/packs/import` with a lab pack conflicting by name (different id) → 409 + `conflicts`.
  3. POST same pack with `confirmReplace=true` → 200 and file present under rubric dir.
  4. Place a lab pack file, then import a term pack → assert lab pack file deleted and term file present (file half of AE1); optionally restart/bootstrap assert labs match only the term pack.
  5. Same UUID lab import → 200 without 409.
  6. Cross-term same name → 200 without 409.
- **Execution note:** Characterization may run before U1 lands; assertion suite after U1.
- **Test scenarios:**
  - Covers AE2. Same-name different-id lab import → 409 with non-empty `conflicts`
  - Confirm replace → staged file saved; fingerprint cleared
  - Covers AE1 (files). Term import deletes existing lab pack files in rubric dir
  - Covers AE3. Same-UUID lab import → 200, no 409
  - Covers AE4. Cross-term same name (different termId) → 200 without 409 (KTD4)
- **Verification:** Targeted `mvn test` for the new/updated desktop pack integration class(es) from `backend/`
- **Dependencies:** U1 for post-change assertions; characterization may precede U1

### U4. Docs for import semantics

- **Goal:** Student/operator docs match term clears lab packs, UI confirm, and error expectations.
- **Requirements:** R1–R9; S1–S3
- **Files:** `docs/DESKTOP_STUDENT_DIST.md`, `CONCEPTS.md` (Rubric pack — already partially updated), `frontend/AGENTS.md` (import allowlist per KTD2), `frontend/src/components/student/AGENTS.md` if import copy is stale
- **Approach:** Update the import table and “Student: import or update rubrics” section: UI term import removes lab packs; confirm for same-name (different id); same-id silent; manual path silent (preserve R7); newest term wins (preserve R9); invalid packs show specific reasons. Explicitly mark R7/R9 as preserve-existing behavior documented, not newly implemented.
- **Test scenarios:** Test expectation: none — docs-only.
- **Verification:** DOX pass; no remaining claim that leftover lab packs survive a UI term import.
- **Dependencies:** U1–U3 behavior settled

---

## Verification Contract

- Backend: from `backend/`, targeted `mvn test` for desktop pack import/bootstrap integration tests touched in U3; full `mvn test` before merge if time allows.
- Frontend: `npm run build:desktop` from `frontend/`.
- Manual: same-name lab import shows replace dialog; invalid/legacy pack shows specific toast; UI term import then restart leaves no overriding lab pack content.

## Definition of Done

- U1–U4 complete; R1–R6, R8 and AE1–AE5 satisfied by code/tests; R7 and R9 preserved and documented (no behavior change required beyond docs).
- Same-name lab import shows replace confirm when `conflicts` are present; invalid packs show specific reasons — never “Server Busy” for those cases.
- UI term import leaves no lab pack files in `rubric/`.
- Verification Contract commands pass for touched suites / desktop build.
