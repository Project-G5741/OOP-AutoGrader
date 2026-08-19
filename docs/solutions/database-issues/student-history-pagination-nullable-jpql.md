---
title: Student history pagination fails on PostgreSQL nullable JPQL filter
date: 2026-08-19
category: database-issues
module: student submission history
problem_type: database_issue
component: database
symptoms:
  - "GET /api/submissions/my-history returns 500 after pagination was added"
  - "Student Submission History shows empty All Submissions table and zero stat cards while Performance by Lab sidebar still loads"
  - "PostgreSQL logs: could not determine data type of parameter $2"
root_cause: wrong_api
resolution_type: code_fix
severity: high
related_components:
  - service_object
  - frontend_stimulus
tags:
  - student-history
  - pagination
  - jpql
  - postgresql
  - lab-submission
  - my-history
  - spring-data-jpa
---

# Student history pagination fails on PostgreSQL nullable JPQL filter

## Problem

Adding server-side pagination to `GET /api/submissions/my-history` broke the student Submission History page. The API returned **500** for the default unfiltered request (`labId` omitted), while `GET /api/submissions/my-labs` still worked. The UI showed an empty submissions table and zeroed stats even though the Performance by Lab sidebar listed labs with attempts.

## Symptoms

- `GET /api/submissions/my-history?page=0&size=10&sort=submittedAt,desc` fails with **500**.
- Server stack trace includes `org.hibernate.exception.SQLGrammarException` and PostgreSQL:

  ```
  ERROR: could not determine data type of parameter $2
  ```

- Generated SQL resembles:

  ```sql
  ... WHERE ls1_0.user_id = ? AND (? IS NULL OR l1_0.id = ?) ORDER BY ...
  ```

- Frontend treats any non-OK response as empty history (`console.info('History API not available yet, using empty data')`), so the failure looks like "no submissions" rather than an API error.
- `my-labs` continues to succeed, so only the paginated history path is affected.

## What Didn't Work

**Optional-filter JPQL with a nullable `labId` parameter:**

```java
@Query(
    value = "SELECT s FROM LabSubmission s JOIN FETCH s.lab WHERE s.user.id = :userId AND (:labId IS NULL OR s.lab.id = :labId)",
    countQuery = "SELECT COUNT(s) FROM LabSubmission s WHERE s.user.id = :userId AND (:labId IS NULL OR s.lab.id = :labId)")
Page<LabSubmission> findHistoryPage(@Param("userId") UUID userId, @Param("labId") UUID labId, Pageable pageable);
```

When `labId` is `null` (All Labs), PostgreSQL cannot infer the type of the bind parameter used in `? IS NULL`, so the query fails before returning rows.

**Relying on the UI error message to surface the failure.** The history fetch path returns empty arrays on any HTTP error, which masked the 500 during manual testing.

## Solution

### 1. Split repository queries by filter presence

Replace the nullable-parameter pattern with two explicit queries and branch in the service:

```java
// LabSubmissionRepository.java
Page<LabSubmission> findHistoryPageByUserId(@Param("userId") UUID userId, Pageable pageable);

Page<LabSubmission> findHistoryPageByUserIdAndLabId(
    @Param("userId") UUID userId, @Param("labId") UUID labId, Pageable pageable);

// StudentHistoryService.java
Page<LabSubmission> submissionPage = labId == null
    ? labSubmissionRepository.findHistoryPageByUserId(userId, pageable)
    : labSubmissionRepository.findHistoryPageByUserIdAndLabId(userId, labId, pageable);
```

Apply the same split to aggregate stats helpers (`countByUser_Id` vs `countByUser_IdAndLab_Id`, separate `AVG`/`MAX` queries) instead of `(:labId IS NULL OR ...)`.

### 2. Paginated API contract

`SubmissionController` accepts `page` (default 0), `size` (default 10), and optional `sort`. `StudentHistoryResponse` includes `page`, `size`, `totalElements`, and `totalPages` alongside `submissions` and `stats`. Stats are computed over the full filtered scope, not the current page.

### 3. Frontend: partial reload on page change

`StudentHistoryPage.jsx` separates:

- `loadFullPage()` — initial load and refresh: fetches `my-history` + `my-labs`, drives full-page skeleton.
- `loadHistorySection()` — pagination, sort, and lab filter: fetches only `my-history`, shows a table overlay spinner, leaves stats and Performance by Lab unchanged on page next/previous.

Lab filter changes call `loadHistorySection` with `syncStats: true` so stat cards update for the new scope.

## Why This Works

PostgreSQL needs a concrete type for every bind parameter. `(:labId IS NULL OR s.lab.id = :labId)` reuses `:labId` in a boolean expression where the planner cannot infer UUID typing when the Java argument is `null`. Splitting into two queries removes the untyped null check entirely.

Separating full-page vs table-only fetch avoids flashing stat-card and sidebar skeletons on pagination — a UX fix orthogonal to the SQL bug but part of the same feature delivery.

## Prevention

- **Avoid `(:param IS NULL OR column = :param)` in JPQL/native SQL against PostgreSQL** when the parameter can be null. Prefer separate query methods, Spring Data `Specification`, or service-layer branching.
- **When adding pagination to an existing full-list endpoint**, mirror lecturer roster patterns (`page`/`size`/`sort`, offset in repository) and add service tests for page metadata + scope-wide stats.
- **Do not map all non-OK history responses to empty data** for endpoints that are already live — surface or log HTTP status so 500s are visible during development.
- **Smoke-test the unfiltered path** (`labId` omitted) after optional-filter refactors; the filtered path may work while the default path fails.

## Related Issues

- [Hibernate native query PostgreSQL cast parsed as bind parameter](../runtime-errors/hibernate-native-query-postgres-cast-bind-parameter.md) — same Hibernate/PostgreSQL parameter-parsing family, different trigger (`::int` in native SQL)
- [App-wide clickable column-header table sort](../design-patterns/clickable-column-header-table-sort.md) — server-side sort + pagination patterns used on lecturer tables; student history now follows the same `sort` param + page reset conventions
