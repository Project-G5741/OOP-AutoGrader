---
title: "feat: Student submission history pagination"
date: 2026-08-19
type: feat
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# feat: Student submission history pagination - Plan

## Goal Capsule

**Objective:** Add paginated loading to the student Submission History view so each page shows 10 submissions, fetched from the server on demand, while summary stats and column sorting continue to reflect the student's full in-scope history.

**Product authority:** Session brainstorm decisions (2026-08-19 dialogue). Supersedes the original student-history plan's no-pagination decision (KD4 in `docs/plans/2026-08-08-001-feat-student-history-apis-plan.md`).

**Open blockers:** None.

---

## Product Contract

### Summary

Paginate the authenticated student's submission history at 10 rows per page with server-side sort and offset pagination on `my-history`. Stat cards stay computed over all submissions in the active filter scope. Lab filter and sort changes reset to page 1.

### Problem Frame

The student Submission History page currently loads every submission in one API call and sorts in the browser. Students with many attempts across labs pay a growing payload and render cost. The product needs bounded fetches without losing meaningful stats or sort-across-all-history behavior.

### Actors

- A1. **Authenticated student** — JWT bearer; reads only their own submission history.

### Requirements

- R1. `GET /api/submissions/my-history` accepts `page` (zero-based, default 0) and `size` (default 10, fixed at 10 for the student UI in v1).
- R2. The response returns only the requested page of submissions, not the full filtered list.
- R3. The response includes pagination metadata: total matching submissions in scope, current page index, page size, and total page count.
- R4. Default sort is newest submission first (`submittedAt` descending). When `labId` is set, sort within that lab's attempts by `submittedAt` descending unless a sort param overrides.
- R5. The endpoint accepts a `sort` query param for server-side ordering. Supported fields match the history table columns: lab name, attempt number, score, submitted timestamp, and status. Sort direction is ascending or descending.
- R6. Changing sort re-orders across the full filtered history before pagination is applied; the client resets to page 0 after a sort change.
- R7. The stats block (`labsAttempted`, `totalSubmissions`, `averageScore`, `bestScore`) is computed over all submissions matching the current filter scope, independent of the current page.
- R8. Optional `labId` filter behavior is unchanged: when present, submissions and stats are scoped to that lab only; `my-labs` remains independent.
- R9. Each submission item in a page retains the existing shape: id, lab, attemptNumber, score, submittedAt, status, and challengeResults for expanded rows.
- R10. The student history UI shows prev/next (or equivalent) pagination controls, displays which page the user is on, and fetches the next page only when the user navigates.
- R11. Changing the lab filter resets pagination to page 0 and refetches stats plus the first page.
- R12. Both `my-history` and `my-labs` continue to require JWT; existing auth error semantics are unchanged.

### Key Flows

- F1. Student opens Submission History → UI loads `my-labs`, stats, and page 0 (10 newest submissions) from `my-history`.
- F2. Student clicks next page → UI requests `my-history?page=1&size=10` with the current sort and lab filter → table updates; stat cards unchanged.
- F3. Student clicks a column header → UI sends new `sort` param, resets to page 0, and replaces the table with the first page of the newly ordered full history.
- F4. Student selects a lab in the dropdown → UI calls `my-history?labId=…&page=0`, stats recompute for that lab, table shows first page of that lab's submissions.

### Acceptance Examples

- AE1. Student with 25 submissions, no lab filter, default sort: page 0 shows 10 rows (newest 10); pagination shows 3 pages; `totalSubmissions=25`; stats reflect all 25.
- AE2. Same student navigates to page 2: response contains submissions 21–25 (5 rows); stats unchanged from AE1.
- AE3. Student filters to one lab with 3 attempts: one page only; `labsAttempted=1`, `totalSubmissions=3`; all 3 rows visible.
- AE4. Student sorts score ascending on 15 submissions: page 0 shows the 10 lowest scores; page 1 shows the remaining 5.
- AE5. Student with zero submissions: empty table, stats zeros/nulls, pagination hidden or disabled.

### Scope Boundaries

**In scope:** Student `my-history` pagination and sort params, service/repository paging, `StudentHistoryPage` pagination UI and fetch wiring, AGENTS.md updates for student history contract.

**Deferred for later:** Lecturer grade-overview submission history panel pagination, text search, infinite scroll, configurable page size, lazy-loaded challenge detail on row expand.

**Outside this product's identity:** Upload/grading pipeline changes, lecturer roster pagination (already paginated), Reports analytics.

### Key Decisions

- KD1. **Offset pagination (page + size)** — chosen over cursor pagination to match existing lecturer roster and grade-overview patterns.
  Governs R1, R2, R3.

- KD2. **Server-side sort with pagination** — replaces the original full-list plus client sort design; sort applies to the full filtered set, then pages.
  session-settled: user-directed — chosen over client-only sort on the visible page — sort must work across all history without loading every row.
  Governs R4, R5, R6.

- KD3. **Stats independent of page** — stat cards always reflect all in-scope submissions, not the current page.
  session-settled: user-directed — chosen over per-page stats — summary cards stay meaningful while browsing pages.
  Governs R7.

- KD4. **Fixed page size 10** — no user-configurable page size in v1.
  Governs R1, R10.

- KD5. **Filter or sort resets to page 0** — standard pagination behavior when scope or ordering changes.
  Governs R6, R11.

### How This Work Fits Together

This change extends the existing student history feature shipped under `docs/plans/2026-08-08-001-feat-student-history-apis-plan.md`. It does not alter `my-labs`, upload flows, or lecturer-facing history views.

**Product Contract preservation:** restructured KD4 supersession only; no scope change beyond pagination.

---

## Planning Contract

Extend `GET /api/submissions/my-history` with offset pagination and server-side sort (mirror lecturer roster `page`/`size`/`sort`). Stats use aggregate queries over full scope. Frontend drops client-side `sortRows` and adds prev/next controls like `SubmissionTable`.

---

## Implementation Units

### U1. Backend paginated my-history

**Goal:** Return one page of submissions plus pagination metadata and scope-wide stats.

**Requirements:** R1–R9, R12; KD1–KD4.

**Files:** `backend/src/main/java/com/eiu/capstone/backend/DTO/StudentHistoryResponse.java`, `backend/src/main/java/com/eiu/capstone/backend/repository/LabSubmissionRepository.java`, `backend/src/main/java/com/eiu/capstone/backend/service/StudentHistoryService.java`, `backend/src/main/java/com/eiu/capstone/backend/controller/SubmissionController.java`, `backend/src/test/java/com/eiu/capstone/backend/service/StudentHistoryServiceTest.java`

**Approach:** Add pagination fields to `StudentHistoryResponse`. Repository: count + paginated fetch with JOIN FETCH lab; stats via aggregate queries. Service resolves `sort` param (`labName`, `attempt`, `score`, `submittedAt`, `status`) with default `submittedAt,desc`. Status sort uses score-band CASE ordering matching `deriveStatus`.

**Test scenarios:** Page 0 size 10 returns at most 10 items; stats over full scope; empty history; lab filter scopes stats and total.

### U2. Student history pagination UI

**Goal:** Fetch pages on demand; server sort via column headers.

**Requirements:** R10–R11; KD2, KD5.

**Files:** `frontend/src/components/student/StudentHistoryPage.jsx`, `frontend/src/components/student/AGENTS.md`, `frontend/AGENTS.md`, `backend/AGENTS.md`

**Approach:** Pagination state (`page`, `totalPages`, `totalElements`). `fetchHistoryData` sends `page`, `size=10`, `sort` via `buildServerSortParam`. Remove `filteredSubmissions` client sort. Prev/next footer like `SubmissionTable`. Reset page on lab filter or sort change.

**Test scenarios:** Manual — 10 rows per page, next loads page 2, sort resets to page 1.

---

## Verification Contract

- `mvn test` in `backend/` passes (including new pagination tests).
- `npm run build` in `frontend/` succeeds.
- Manual: student history with 11+ submissions shows pagination; stats unchanged across pages.

## Definition of Done

- `my-history` paginated with server sort; student UI loads 10 rows per page; docs updated.
