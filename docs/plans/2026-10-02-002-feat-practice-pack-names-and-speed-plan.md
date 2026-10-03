---
title: Practice Pack Names and Download Speed - Plan
type: feat
date: 2026-10-02
topic: practice-pack-names-and-speed
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Practice Pack Names and Download Speed - Plan

## Goal Capsule

- **Objective:** Make lecturer practice-pack downloads fast, and replace UUID pack filenames with human-readable `Rubric_…` names that the desktop app accepts exclusively — with lab names capped at 50 characters.
- **Product authority:** This Product Contract. Student runtime zip download is surrounding work (see `docs/plans/2026-10-02-001-perf-practice-folder-download-plan.md`).
- **Open blockers:** None.
- **Stop conditions:** Do not keep UUID pack filenames as accepted import aliases. Do not silently truncate lab names on download. Do not change pack crypto/signing payload shape.
- **Execution:** Code.
- **Product Contract preservation:** Product Contract unchanged (meaning and R/F/AE IDs preserved). Outstanding Questions resolved into KTDs below.

---

## Product Contract

### Summary

Lecturers download quarter and lab practice packs with clear filenames (`Rubric_2026-2027_Q1.agpack`, `Rubric_Lab 2.agpack`). Export prepare time is significantly faster than today. The desktop practice app imports and boots only packs that match those name patterns. Lab names are limited to 50 characters in the product and in pack filenames.

### Key Decisions

- **Human-readable pack names replace UUID filenames** (session-settled: user-directed — chosen for clarity over `{uuid}.term.agpack` / `{uuid}.lab.agpack`). **Governs R1, R2, R3, R6.**
- **50-character lab Name limit on create/edit and on pack import** (session-settled: user-directed — chosen over filename-only truncate or create/edit-only). **Governs R4, R5.**
- **New-format-only desktop acceptance** (session-settled: user-directed — legacy UUID names rejected). **Governs R6.**
- **No silent truncate on export** — over-limit existing lab names must be shortened before save/export succeeds. **Governs R5.**

### Requirements

**Naming**

- R1. Term (quarter) practice pack download filename is `Rubric_{yearLabel}_Q{termNumber}.agpack` (example: `Rubric_2026-2027_Q1.agpack`).
- R2. Lab practice pack download filename is `Rubric_{labName}.agpack` (examples: `Rubric_Lab 2.agpack`, `Rubric_Midterm.agpack`); spaces in the lab name are preserved.
- R3. On import and on desktop startup load, the on-disk filename must match the pack contents (term identity for term packs; single lab name for lab packs), not a UUID embedded in the filename.
- R4. The lab **Name** (the portion after `Rubric_` in the filename, and the lab's product name) is at most 50 characters.
- R5. Create and edit lab reject names longer than 50 characters; existing over-limit names must be shortened before a successful save or practice-pack export; pack import also rejects over-limit `Rubric_` lab stems.
- R6. Desktop practice accepts only the R1/R2 patterns (plus matching content per R3). Legacy `{uuid}.term.agpack` / `{uuid}.lab.agpack` (and other legacy aliases) are rejected with a clear error.

**Download speed**

- R7. Lecturer **Download practice pack** (Term Management quarter pack and Solution Management lab pack) prepares and starts transferring significantly faster than the current cold rebuild path, especially for multi-lab term packs and when rubrics were recently used.
- R8. Speed work must not change pack payload meaning (signed contents, which labs are included, or restart/import semantics beyond naming).

### Actors

- A1. Lecturer — exports packs from Term Management / Solution Management.
- A2. Student (desktop practice) — imports packs or places them under `rubric/`, then restarts.

### Key Flows

- F1. Lecturer term pack download
  - **Trigger:** Term Management → **Download practice pack** for a quarter with labs.
  - **Actors:** A1
  - **Steps:** Server builds signed term pack; browser saves `Rubric_{yearLabel}_Q{n}.agpack`.
  - **Covered by:** R1, R7, R8
- F2. Lecturer lab pack download
  - **Trigger:** Solution Management → **Practice pack** for a selected lab.
  - **Actors:** A1
  - **Steps:** Server builds signed lab pack; browser saves `Rubric_{labName}.agpack` (lab name ≤50).
  - **Covered by:** R2, R4, R5, R7, R8
- F3. Desktop import / restart
  - **Trigger:** Student imports `.agpack` or copies into `rubric/` and restarts.
  - **Actors:** A2
  - **Steps:** App accepts only R1/R2 names that match pack contents; rejects legacy UUID names and over-limit `Rubric_` lab stems.
  - **Covered by:** R3, R4, R6

### Acceptance Examples

- AE1. Term export naming
  - **Covers:** R1
  - **Given:** Academic year label `2026-2027`, quarter number `1`
  - **When:** Lecturer downloads the quarter practice pack
  - **Then:** Saved file is `Rubric_2026-2027_Q1.agpack`
- AE2. Lab export naming with spaces
  - **Covers:** R2, R4
  - **Given:** Lab named `Lab 2` (length ≤50)
  - **When:** Lecturer downloads the lab practice pack
  - **Then:** Saved file is `Rubric_Lab 2.agpack`
- AE3. Lab name over 50 blocked
  - **Covers:** R4, R5
  - **Given:** Lecturer enters a lab name of 51 characters
  - **When:** They try to save the lab or download its practice pack
  - **Then:** The action fails with a clear validation error; no truncated filename is written
- AE4. Legacy UUID pack rejected
  - **Covers:** R6
  - **Given:** A file named `{uuid}.term.agpack` that would have imported before
  - **When:** Student imports it in desktop practice
  - **Then:** Import fails; message indicates the required `Rubric_…` format
- AE5. Filename must match contents
  - **Covers:** R3
  - **Given:** A valid signed lab pack for lab `Midterm` renamed on disk to `Rubric_Other.agpack`
  - **When:** Student imports or starts the app with that file
  - **Then:** The pack is rejected

### Success Criteria

- S1. Lecturers can identify packs from the filename without opening them (term year/quarter and lab name).
- S2. Desktop practice no longer loads UUID-named packs.
- S3. Term and lab practice-pack downloads feel markedly faster in normal lecturer use (warm and typical cold paths), without changing graded pack contents.

### Scope Boundaries

- **In:** Lecturer download filenames; desktop import/startup name rules; lab name 50-char product limit; export prepare-time speedup for term and lab packs; docs/user-facing copy that describe pack names.
- **Out:** Student **Download practice folder** runtime zip (`docs/plans/2026-10-02-001-perf-practice-folder-download-plan.md`); pack crypto/signing format changes; changing quarter/lab replace-on-restart semantics beyond naming; server-side signed `.agpack` disk cache.

<!-- ce-section: work-relationships -->
### How This Work Fits Together

- **This plan owns:** Lecturer `.agpack` naming + export speed + desktop pack name acceptance.
- Student runtime zip prepare/stream (`docs/plans/2026-10-02-001-perf-practice-folder-download-plan.md`) — **Can proceed independently of** this plan; **Shares** desktop distribution docs only.

---

## Planning Contract

### Key Technical Decisions

- KTD1. **Reuse `LabRubricCache.get(lab)` in pack export** (session-settled: user-approved — chosen over server-side signed `.agpack` disk cache: warm rubric reuse is enough and keeps pack bytes signed on demand). **Implements R7, R8.** Today `DesktopPackExportService.toEntry` calls `labRubricService.loadForLab` (cold N graphs for term packs). Switch to the existing in-process cache; payload JSON/crypto unchanged.
- KTD2. **Centralize new name builders/parse/match in `DesktopPackFileNames`.** Term filename from `AcademicYear.yearLabel` + `Term.termNumber` as `Rubric_{yearLabel}_Q{n}.agpack` (not written via `TermService.buildTermLabel`). Lab filename `Rubric_{name}.agpack`. Term `assertMatchesPayload`: parse `Rubric_{year}_Q{n}` → rebuild expected `termLabel` with the same quarter-label formatter used at export → exact-compare to `manifest`/`inner` `termLabel` (covers Q4 Summer wording without changing payload shape). Lab packs: exact-compare `Rubric_` lab stem to the single lab’s `DesktopPackLabMeta.name`.
- KTD3. **Strict filename only on ingest** — remove `DesktopPackIngest.inferFromPayload` fall-through so a wrong or legacy name cannot be rewritten from contents. Blank or unparseable upload/on-disk names fail closed (intentional under R3/R6). **Implements R3, R6.**
- KTD4. **Reject Windows-illegal characters in lab names** (session-settled: user-approved — chosen over silently sanitizing the download filename). Disallow `\ / : * ? " < > |` (and control chars) on create/edit/export/import alongside the 50-char cap. Spaces allowed.
- KTD5. **Frontend downloads prefer `Content-Disposition` filename** (same pattern as `StudentOfflinePracticeDownload.jsx`), with a client-side fallback built from known term/lab fields — do not hardcode UUID `.agpack` names.
- KTD6. **Lab name validation shared on server** — one helper (max 50 + illegal-char check) owned with U3; `DesktopPackFileNames` parse and pack export/import call that helper (no divergent duplicate rules). Frontend mirrors maxLength/inline check for UX; server remains authoritative.

### Approach

1. Rename rules live in `DesktopPackFileNames`; controllers set `Content-Disposition` from those helpers.
2. Export injects `LabRubricCache` and loads snapshots via `get(lab)`; still signs on each request.
3. Desktop ingest/bootstrap fail closed on parse/match; docs and CONCEPTS updated to the new patterns.
4. Lab create/rename and FE Solution Management enforce the 50-char + illegal-char rules.

### Sequencing

U1 (names + ingest) → U3 (shared name validator + create/edit) → U2 (export speed + headers) and U4 (FE downloads + name UX) in parallel → U5 (docs/AGENTS).

### Risks

- Existing labs with names >50 or with illegal characters block export until renamed (product-intended).
- Spaces in filenames: keep quoted `filename="…"`; avoid unquoted disposition values.
- Warm-cache speedup is large on repeat downloads; first cold term pack still pays N rubric loads once (acceptable; not a disk pack cache).

### Research breadcrumbs

- Export bypasses cache today: `backend/src/main/java/com/eiu/capstone/backend/desktop/pack/DesktopPackExportService.java`
- Ingest soft-accept: `DesktopPackIngest.resolveFilename` / `inferFromPayload`
- Learnings: reuse `LabRubricSnapshot` / invalidate on mutation (`docs/solutions/architecture-patterns/assemble-lab-result-from-rubric-snapshot.md`, `docs/solutions/logic-errors/method-invocation-receiver-constructor.md`)

---

## Implementation Units

### U1. Pack filename parse, match, and strict ingest

- **Goal:** Accept only `Rubric_…` names; filename must match pack contents; reject legacy UUID names.
- **Requirements:** R1–R4, R6; AE1, AE2, AE4, AE5
- **Files:** `backend/src/main/java/com/eiu/capstone/backend/desktop/pack/DesktopPackFileNames.java`, `DesktopPackIngest.java`; `backend/src/test/java/unit/com/eiu/capstone/backend/desktop/pack/DesktopPackFileNamesTest.java`; integration helpers under `backend/src/test/java/integration/com/eiu/capstone/backend/desktop/`
- **Approach:** Replace UUID regex with patterns for `Rubric_{yearLabel}_Q{n}.agpack` and `Rubric_{name}.agpack` (name 1–50 after path strip). Builders take yearLabel/termNumber or labName. Term match rebuilds expected `termLabel` from parsed year/Q (KTD2); lab match compares stem to meta name. Remove `inferFromPayload`; blank names fail closed. Lab-stem length/illegal-char checks call the U3 shared helper (wire helper stub in U1 if U3 not merged yet, then dedupe).
- **Test scenarios:**
  - Parse accepts `Rubric_2026-2027_Q1.agpack` and `Rubric_Lab 2.agpack`
  - Parse rejects `{uuid}.term.agpack` and `{uuid}.lab.agpack`
  - Parse rejects `Rubric_` lab stem longer than 50
  - `assertMatchesPayload` rejects renamed `Rubric_Other.agpack` for Midterm contents
  - Ingest with wrong original filename does not fall back to inferred canonical name
- **Verification:** `mvn test -Dtest=DesktopPackFileNamesTest` from `backend/`
- **Dependencies:** None

### U2. Export naming + warm rubric cache

- **Goal:** Controllers/export emit new filenames; export uses `LabRubricCache` for speed without changing signed payload.
- **Requirements:** R1, R2, R5, R7, R8; F1, F2; AE1–AE3
- **Files:** `DesktopPackExportService.java`, `LecturerTermController.java`, `LecturerRubricController.java`; `authorization/.../LecturerTermDesktopPackTest.java` (plus lab-pack slice if needed)
- **Approach:** Inject `LabRubricCache`; `toEntry` uses `labRubricCache.get(lab)`. Before export, validate lab name(s) via shared helper (fail 400 if over limit / illegal chars). Filename helpers use entity fields. Set `Content-Disposition` with quoted filename from helpers.
- **Test scenarios:**
  - Term download `Content-Disposition` contains `Rubric_…_Q….agpack`
  - Lab export rejects name length >50 before signing
  - Export still returns bytes when signing key present; 503 when missing (existing)
  - Export obtains snapshot via cache API rather than direct `loadForLab` when simulated warm
- **Verification:** `mvn test -Dtest=LecturerTermDesktopPackTest,DesktopPackFileNamesTest` from `backend/`
- **Dependencies:** U1, U3

### U3. Lab name product validation (50 + illegal chars)

- **Goal:** Create/edit lab enforce R4/R5/KTD4 on the server.
- **Requirements:** R4, R5; AE3
- **Files:** `LabStructureService.java` (`createLab`, rename in `saveLabStructure`); shared name validator; FE create/rename in `SolutionManagement.jsx` / structure panels; `frontend/src/utils/validation.js` if a shared rule fits
- **Approach:** Reject blank, length >50, and Windows-illegal characters with HTTP 400 / friendly FE message. No DB migration; existing long names fail on next save/export until shortened.
- **Test scenarios:**
  - Create lab with 51-char name → 400
  - Rename to name containing `:` → 400
  - Create `Lab 2` (space, ≤50) → success
- **Verification:** Targeted `mvn test` for structure/create tests touched; `npm run build` if FE validation added here
- **Dependencies:** None (can share helper with U2)

### U4. Lecturer FE download filenames

- **Goal:** Browser saves the server's new pack names.
- **Requirements:** R1, R2; F1, F2; AE1, AE2
- **Files:** `frontend/src/pages/TermManagement.jsx`, `frontend/src/pages/SolutionManagement.jsx`
- **Approach:** Mirror `StudentOfflinePracticeDownload.jsx`: parse `filename="…"` from `Content-Disposition`; fallback from selected term/lab fields. Wire create-lab `maxLength={50}` and inline invalid-char feedback with U3.
- **Test scenarios:** Manual — download term and lab packs; confirm saved names. Build must pass.
- **Verification:** `npm run build` from `frontend/`
- **Dependencies:** U2, U3

### U5. Docs and DOX contracts

- **Goal:** Published contracts match new names and import rules.
- **Requirements:** R1, R2, R4, R6; S1, S2
- **Files:** `docs/DESKTOP_STUDENT_DIST.md`, `CONCEPTS.md` (Rubric pack entry), `backend/AGENTS.md`, `frontend/AGENTS.md` / student AGENTS; `frontend/src/components/student/DesktopRubricPackImport.jsx`, `DesktopPracticeStatusBanner.jsx` (user-facing UUID copy); bootstrap error strings in `DesktopPackBootstrapService.java` if they still cite UUID names
- **Approach:** Replace UUID filename tables with `Rubric_` patterns; note 50-char + illegal-char rules; state legacy UUID names are rejected; update desktop import/status/bootstrap messages to the new format.
- **Test scenarios:** Doc review — examples match AE1/AE2.
- **Verification:** DOX pass; no stale `{uuid}.term.agpack` acceptance text left in those files
- **Dependencies:** U1–U4 behavior settled

---

## Verification Contract

- Backend: from `backend/`, `mvn test -Dtest=DesktopPackFileNamesTest,LecturerTermDesktopPackTest,DesktopPackBootstrapIntegrationTest` (extend bootstrap fixtures to new filenames); full `mvn test` before merge if time allows.
- Frontend: `npm run build` from `frontend/`.
- Manual: lecturer term + lab pack download (check saved names and prepare feel when rubrics were recently opened); desktop import of new pack; reject a renamed/legacy UUID pack.

## Definition of Done

- All U1–U5 complete; R1–R8 and AE1–AE5 satisfied.
- No UUID pack filename accepted by desktop parse/bootstrap.
- Export path uses `LabRubricCache` (no direct cold `loadForLab` in `toEntry`).
- Docs/CONCEPTS describe `Rubric_` only.
- Verification Contract commands pass for the touched suites.
