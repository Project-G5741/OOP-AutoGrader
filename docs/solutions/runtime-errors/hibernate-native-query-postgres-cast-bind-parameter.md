---
title: Hibernate native query PostgreSQL cast parsed as bind parameter
date: 2026-08-08
category: runtime-errors
module: backend
problem_type: runtime_error
component: database
symptoms:
  - "GET /api/labs/{labId}/stats fails with SQL syntax error at position 127"
  - "Hibernate/JPA parses PostgreSQL COUNT(*)::int cast as named bind parameter :int"
root_cause: wrong_api
resolution_type: code_fix
severity: medium
related_components:
  - service_object
tags:
  - hibernate
  - jpa
  - native-query
  - postgresql
  - bind-parameter
  - stats
---

# Hibernate native query PostgreSQL cast parsed as bind parameter

## Problem

`StatsRepository` runs a native SQL query via `EntityManager.createNativeQuery` for `GET /api/labs/{labId}/stats`. The query used PostgreSQL's `::int` cast on `COUNT(*)`. Hibernate's named-parameter parser treats any `:name` token as a bind placeholder, so `::int` is misread as parameter `:int`. PostgreSQL receives invalid SQL and the stats endpoint fails on every request.

## Symptoms

- `GET /api/labs/{labId}/stats?studentId=<uuid>` returns **500** instead of `StatsDTO`.
- Server log shows Hibernate/Spring `InvalidDataAccessResourceUsageException` wrapping a PostgreSQL syntax error:

  ```
  ERROR: syntax error at or near ":"
    Position: 127
  ```

- Logged SQL shows the cast mangled — e.g. `SELECT COUNT(*):int` — with `:int` consumed as a bind parameter instead of a PostgreSQL type cast.
- Other lab endpoints may work; failure is isolated to the consolidated stats query in `StatsRepository.findStats`.

## What Didn't Work

**Keeping PostgreSQL `::type` casts in the native query string.**

```java
COALESCE((
    SELECT COUNT(*)::int
    FROM lab_submission ls
    WHERE ls.user_id = :studentId AND ls.lab_id = :labId
), 0) AS submission_count
```

This is valid PostgreSQL, but JPA/Hibernate scans the entire SQL for `:parameterName` before execution. The second colon in `::int` starts a named parameter named `int`, which is never bound. The driver sends malformed SQL to PostgreSQL.

**Assuming `::` is safe because real bind parameters use names like `:studentId`.** Any substring matching `:identifier` is a parameter — including the tail of `::int`, `::bigint`, `::text`, etc.

## Solution

Remove SQL-side `::int` casts from native queries. Return `COUNT(*)` as-is and coerce to `int` in Java when mapping the `Object[]` row.

**Before** (`StatsRepository.findStats`):

```java
COALESCE((
    SELECT COUNT(*)::int
    FROM lab_submission ls
    WHERE ls.user_id = :studentId AND ls.lab_id = :labId
), 0) AS submission_count
// ...
int submissionCount = row[3] == null ? 0 : ((Number) row[3]).intValue();
```

**After**:

```java
COALESCE((
    SELECT COUNT(*)
    FROM lab_submission ls
    WHERE ls.user_id = :studentId AND ls.lab_id = :labId
), 0) AS submission_count
// ...
int submissionCount = row[3] == null ? 0 : ((Number) row[3]).intValue();
```

`((Number) row[3]).intValue()` handles JDBC returning `Long` or `BigInteger` for `COUNT(*)`, depending on the driver.

## Why This Works

Hibernate only recognizes intentional `:namedParameter` tokens. With `::int` removed, nothing in the SQL looks like an unbound parameter. PostgreSQL executes `COUNT(*)` normally (typically `bigint`); Java narrows the value at the mapping layer, which is already required for other numeric columns in the same row.

The root cause is a **parser mismatch**: PostgreSQL `::type` is postfix cast syntax; JPA native queries use `:` as the parameter prefix. The two conventions collide on every `::` cast.

## Prevention

- **Rule: avoid `::` casts in JPA/Hibernate native query strings.** If SQL-side typing is required, use standard `CAST(expr AS type)` — e.g. `CAST(COUNT(*) AS integer)` — which does not contain a leading `:`. Prefer casting in Java when mapping `Object[]` or scalar results.
- **Code review checklist for new native SQL:** grep for `::` in strings passed to `createNativeQuery`, `@Query(nativeQuery = true)`, or `NamedParameterJdbcTemplate` only when the template uses the same `:` parameter rules (JDBC named params use `:name` without the PostgreSQL cast collision on `::`).
- **Map counts defensively:** use `((Number) value).intValue()` rather than direct `(Integer)` casts for aggregate results.
- **Smoke-test new native queries against a running DB** before merging perf consolidations; syntax errors surface immediately on the first endpoint hit (`GET /api/labs/{labId}/stats` in this case).

## Related Issues

- [Duplicate key on submission result re-upload](../database-issues/submission-result-reupload-duplicate-key.md) — same Hibernate/PostgreSQL stack, different failure mode (flush ordering vs native-query parameter parsing)
