---
title: Bulk Grading Challenge Tabs - Plan
type: feat
date: 2026-10-06
topic: bulk-grading-challenge-tabs
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
parent_plan: docs/plans/2026-10-06-001-feat-lecturer-bulk-folder-grading-plan.md
---

# Bulk Grading Challenge Tabs - Plan

## Goal Capsule

- **Objective:** After ephemeral bulk grading finishes, let the lecturer switch Overview vs challenge tabs (same chrome spirit as Dashboard lab view) so Score reflects lab total or that challenge’s score, and View Submission opens the drawer already on the active challenge.
- **Product authority:** This Product Contract.
- **Open blockers:** None.
- **Stop conditions:** Do not add Dashboard-style stats, grade distribution, search, summary cards, Attempt, or Submitted At. Do not write bulk results to Score/roster.

## Product Contract

### Problem

Bulk results today are a single table with overall score only. Multi-challenge labs need a Dashboard-like way to inspect per-challenge scores without leaving the Grading (Bulk) page.

### Users

- Lecturers on the **Grading** nav (Bulk) after a batch completes.

### Requirements

| ID | Requirement |
|---|---|
| R1 | Results live on the **Grading (Bulk)** page only — not inside Dashboard. |
| R2 | After grading (and while rows exist), show tabs: **Overview** plus one tab per selected-lab challenge (labels follow Dashboard challenge naming). |
| R3 | **Overview** Score column = ephemeral lab total (existing overall score). |
| R4 | **Challenge** tab Score column = that challenge’s score from the in-memory `lab_result` / challenge result payload for the student. Missing challenge → dash. |
| R5 | Columns stay **Student, ID, Score, Plagiarism, Action** (no Attempt / Submitted At). Plagiarism stays within-batch and does not change by challenge tab. |
| R6 | **View Submission** from a challenge tab opens the ephemeral drawer with that challenge already selected. Overview → first challenge. |
| R7 | No stats strip, grade-distribution chart, roster search, or summary cards in this slice. |
| R8 | Exam / single-challenge labs still show Overview + the one challenge tab (do not collapse tabs away). |
| R9 | Ephemeral semantics unchanged: no roster/Score/plagiarism DB writes. |

### Key Decisions

- KD1. **Tabs + roster only** (session-settled) — not full Dashboard Overview chrome. **Governs R7.**
- KD2. **Placement on Bulk page** (session-settled) — not Dashboard. **Governs R1.**
- KD3. **Drawer opens on the active challenge tab** (session-settled). **Governs R6.**

### Acceptance Examples

- AE1. Multi-challenge lab batch → Overview shows lab totals; switching to Challenge 2 changes Score cells to challenge_2 scores; Plagiarism unchanged.
- AE2. View from Challenge 2 opens drawer with Challenge 2 selected and its Class/MMD breakdown.
- AE3. View from Overview opens drawer on the first challenge.
- AE4. Single-challenge exam batch still shows Overview + one challenge tab.

### Non-goals

- Dashboard embedding of bulk results
- Batch stats / distribution / search / export
- Server API changes (reuse existing bulk grade payload)
- Persisting ephemeral results

### Outstanding Questions

None.

---

## Planning Contract

### Key Technical Decisions

- KTD1. **Client-only score projection** — derive challenge score from `payload.labResult.challenge_N.scores.total` (fallback `payload.challengeResult[challengeId]` if present). No new API. **Implements R3–R4; Governs U1.**
- KTD2. **Reuse Dashboard tab chrome classes** — same `tabClass` / Overview + challenge button row pattern as `LecturerDashboard` lab detail (not a shared component extraction unless trivial). **Implements R2; Governs U1.**
- KTD3. **Pass `initialChallengeId` into `BulkSubmissionDrawer`** — Overview passes first challenge id; challenge tab passes that challenge’s id. **Implements R6; Governs U2.**

### Implementation Units

### U1. Bulk results Overview + challenge tabs + score column

- **Goal:** Results table switches Score by active tab.
- **Requirements:** R1–R5, R7–R8; AE1, AE4
- **Files:** `frontend/src/components/lecturer/BulkGradingPanel.jsx`; optional tiny helper in same file or `frontend/src/utils/`
- **Approach:**
  1. State `resultsTab`: `'overview' | challengeId`.
  2. When `rows.length > 0`, render Overview + `selectedLab.challenges` tabs above the table.
  3. Display score: overview → `row.score`; challenge → extract from `row.payload.labResult` / `challengeResult`.
  4. Reset tab to overview on new parse / lab change / new grade run start.
- **Test scenarios:** AE1, AE4; missing challenge score shows dash.
- **Verification:** Manual tab switch; `npm run build`.

### U2. Drawer opens on active challenge

- **Goal:** View Submission respects the results tab.
- **Requirements:** R6; AE2, AE3
- **Files:** `BulkSubmissionDrawer.jsx`, `BulkGradingPanel.jsx`
- **Approach:** Add `initialChallengeId` prop; on open, set challenge selector to that id (fallback first). Panel passes active challenge or first when Overview.
- **Test scenarios:** AE2, AE3.
- **Verification:** Manual View from Overview vs challenge tab; `npm run build`.

### U3. Docs touch

- **Goal:** Lecturer AGENTS notes Bulk tabs.
- **Requirements:** R2
- **Files:** `frontend/src/components/lecturer/AGENTS.md`
- **Verification:** Doc mentions Overview + challenge score tabs on Bulk results.

## Verification Contract

| Gate | Check |
|---|---|
| Frontend build | `npm run build` from `frontend/` |
| Manual | Multi-challenge: Overview vs challenge Score; drawer focus; plagiarism stable |

## Definition of Done

- U1–U3 complete; build green; AE1–AE4 satisfied.
