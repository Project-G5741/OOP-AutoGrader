---
title: Lecturer Roster Plagiarism Rate - Plan
type: feat
date: 2026-08-23
topic: lecturer-roster-plagiarism-rate
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-plan-bootstrap
execution: code
---

# Lecturer Roster Plagiarism Rate - Plan

## Goal Capsule

- **Objective:** Show a lab-level plagiarism rate in the Student roster SUMMARY row on the lecturer dashboard, next to Submitted / Enrolled / Completion.
- **Product authority:** Product Contract below. Detection rules and per-row plagiarism marks stay unchanged.
- **Open blockers:** None.

---

## Product Contract

### Summary

Add a **Plagiarism** percentage to the roster SUMMARY overview so lecturers see how many submitters for the selected lab are involved in a flagged plagiarism match, without paging the roster.

### Problem Frame

The roster already marks flagged students with a warning triangle, but the SUMMARY row only shows submission volume and completion. Lecturers cannot see the lab-wide plagiarism rate at a glance, especially when the roster is paginated.

### Key Decisions

- **Rate = unique flagged students ÷ students submitted** — Governs R1, R2. (session-settled: user-directed — chosen over ÷ enrolled, which dilutes the signal with non-submitters who cannot be flagged.)
- **Source of truth: lab statistics API** — Governs R3. (session-settled: user-directed — chosen over client-only page math, which cannot see students outside the current page.)
- **SUMMARY-only UI for this change** — Governs R4. (session-settled: user-directed — screenshot targets the SUMMARY row; lab statistics cards stay as they are.)

### Actors

- A1. Lecturer viewing the selected lab's Student roster on the dashboard

### Key Flows

- F1. Scan plagiarism overview for a lab
  - **Trigger:** Lecturer opens dashboard overview for a lab with roster data.
  - **Actors:** A1
  - **Steps:** SUMMARY shows Submitted, Enrolled, Completion, and Plagiarism % for the whole lab.
  - **Outcome:** Lecturer knows the share of submitters involved in flagged matches without paging.
  - **Covered by:** R1, R2, R3, R4

### Requirements

- R1. The Student roster SUMMARY includes a **Plagiarism** value formatted as a percentage (same style as Completion).
- R2. Plagiarism rate = (unique students involved in at least one flagged match for this lab) ÷ (students submitted) × 100, rounded like Completion; when students submitted is 0, show `--`.
- R3. The rate is computed server-side for the selected lab and returned with existing lab statistics used by the SUMMARY row (not derived from the current page of roster rows alone).
- R4. Challenge-tab `SubmissionTable` instances without a SUMMARY stay unchanged; no new plagiarism investigation UI.

### Scope Boundaries

**In scope**

- `LabStatisticsResponse` + load path for plagiarism rate
- Passing the field into roster `SubmissionTable` summary
- Rendering the fourth metric in SUMMARY

**Out of scope**

- Changing plagiarism detection / match rules
- Student-facing plagiarism signals
- Adding plagiarism to overview cards or grade matrix
- New endpoints solely for this metric (prefer extending statistics)

### Acceptance Examples

- AE1. Lab with 6 submitters and 2 unique students in flagged matches → SUMMARY shows `Plagiarism: 33%` (or equivalent rounded display).
- AE2. Lab with enrollments but 0 submitters → SUMMARY shows `Plagiarism: --` (or omits a misleading 0% when rate is null).
- AE3. Lab with submitters and no flagged matches → SUMMARY shows `Plagiarism: 0%`.

---

## Planning Contract

### Key Technical Decisions

- KTD1. Extend `LabStatisticsResponse` with `BigDecimal plagiarismRate` (nullable), computed in `LecturerAnalyticsService.loadLabStatistics` via `PlagiarismService` unique-student count for the lab. Prefer this over a separate endpoint so SUMMARY keeps one data source.
- KTD2. Count unique user IDs from flagged matches for the lab (both sides of each match), not only the roster row's displayed `submissionId`. Overview answers “who is implicated,” which is stable across latest/best submission display choices.
- KTD3. Keep frontend presentational: `LecturerDashboard` passes `plagiarismRate` into `summary`; `SubmissionTable` adds the SUMMARY cell and widens the summary grid to five columns.

### Assumptions

- Existing `findByLabIdAndFlaggedTrue` is acceptable for lab-scale match lists (same path as lecturer plagiarism report).
- Lab statistics cache TTL / invalidation-on-upload already covers plagiarism appearing after upload.

### Open Questions

None blocking. Deferred: whether to also surface the rate on overview stat cards later.

### Implementation Units

### U1. Backend plagiarism rate on lab statistics

**Goal:** Return `plagiarismRate` on `GET /api/labs/{labId}/statistics`.

**Requirements:** R2, R3

**Files:**
- Modify: `backend/src/main/java/com/eiu/capstone/backend/analytics/dto/LabStatisticsResponse.java`
- Modify: `backend/src/main/java/com/eiu/capstone/backend/analytics/service/LecturerAnalyticsService.java`
- Modify: `backend/src/main/java/com/eiu/capstone/backend/plagiarism/PlagiarismService.java`
- Modify: `backend/src/main/java/com/eiu/capstone/backend/plagiarism/AGENTS.md` (contract note)
- Modify: `backend/AGENTS.md` if statistics contract bullet needs the new field

**Approach:**
- Add `countFlaggedStudentsForLab(UUID labId)` (or equivalent) on `PlagiarismService` that resolves unique student IDs from flagged matches.
- In `loadLabStatistics` / `emptyLabStatistics`, set rate null when `studentsSubmitted == 0`; otherwise `(flaggedStudents / studentsSubmitted) * 100` with same scale/rounding as completion.

**Test scenarios:**
- Happy path: two unique flagged students, six submitted → rate 33.33 (or HALF_UP 2 dp as completion).
- Edge: zero submitted → null rate.
- Edge: submitted but zero flags → 0.00 rate.
- Integration: empty statistics path still constructs the record with null rate.

**Verification:** `mvn test` for any touched plagiarism/analytics tests; at least compile-safe record construction sites updated.

### U2. Frontend SUMMARY plagiarism metric

**Goal:** Show Plagiarism % in the Student roster SUMMARY.

**Requirements:** R1, R4

**Files:**
- Modify: `frontend/src/pages/LecturerDashboard.jsx`
- Modify: `frontend/src/components/lecturer/SubmissionTable.jsx`
- Modify: `frontend/src/components/lecturer/AGENTS.md`

**Approach:**
- Pass `plagiarismRate` from `labStatistics` into roster `summary`.
- Render `Plagiarism: {formatPercent(...)}` in SUMMARY; update summary-row visibility condition and grid columns.

**Test scenarios:**
- Happy path: statistics include rate → SUMMARY shows percent.
- Edge: null rate → `--`.
- Regression: challenge tab table without summary still has no SUMMARY row.

**Verification:** `npm run build` in `frontend/`.

---

## Verification Contract

- Backend: `mvn test` from `backend/` (or at minimum compile + existing plagiarism tests).
- Frontend: `npm run build` from `frontend/`.
- Manual: lecturer dashboard → select lab with known flags → SUMMARY Plagiarism matches flagged-student count ÷ submitted.

## Definition of Done

- [ ] U1 and U2 complete
- [ ] SUMMARY shows Plagiarism rate for lab roster overview
- [ ] Docs updated for statistics / SUMMARY contract
- [ ] Build / tests above pass
