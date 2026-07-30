---
title: Duplicate key on submission result re-upload
date: 2026-07-31
category: database-issues
module: grading
problem_type: database_issue
component: database
symptoms:
  - "PostgreSQL duplicate key violation on submission_field_result_key when a student re-uploads the same lab attempt folder"
  - "Re-upload fails with unique constraint violation on (submission_id, field_id) after grading logs show 100%"
root_cause: logic_error
resolution_type: code_fix
severity: high
related_components:
  - service_object
tags:
  - submission
  - reupload
  - upsert
  - duplicate-key
  - grading
  - postgresql
---

# Duplicate key on submission result re-upload

## Problem

When a student re-uploads the same lab attempt (same `LabSubmission` row), grading completes but persistence fails. PostgreSQL enforces one row per `(submission_id, field_id)` via `submission_field_result_key`. A second upload that inserts new rows instead of updating existing ones triggers SQL state `23505` and HTTP 500.

## Symptoms

- First upload succeeds; grading and result rows persist.
- Second upload of the same attempt:
  - Server logs show full grading output (e.g. `Score: 100.00% | Fully correct: true`).
  - SQL error: `duplicate key value violates unique constraint "submission_field_result_key"`.
  - HTTP **500** even though grading logic ran correctly.

The same natural-key pattern exists on method, constructor, and challenge result tables.

## What Didn't Work

**Approach:** `deleteBySubmission(submission)` then `saveAll(newEntities)` in one `@Transactional` method.

```java
submissionFieldResultRepository.deleteBySubmission(submission);
submissionFieldResultRepository.saveAll(computed.fieldResults);
```

**Why it failed:** Hibernate does not guarantee `DELETE` statements flush before `INSERT` in the same persistence context. Inserts can run while old rows still exist, violating the unique constraint on `(submission_id, field_id)`.

## Solution

**Strategy:** Load existing rows keyed by rubric element id, reuse managed entities, then `saveAll` so Hibernate emits `UPDATE` on re-grade and `INSERT` only on first grade.

### `GradingResultStore.loadExisting`

```32:44:backend/src/main/java/com/eiu/capstone/backend/grading/GradingResultStore.java
    @Transactional(readOnly = true)
    GradingService.ExistingResults loadExisting(LabSubmission submission) {
        GradingService.ExistingResults existing = new GradingService.ExistingResults();
        existing.fieldResults = submissionFieldResultRepository.findBySubmission(submission)
                .stream().collect(Collectors.toMap(r -> r.getField().getId(), r -> r));
        // ... method, constructor, challenge maps ...
        return existing;
    }
```

`save()` calls `saveAll` only — no deletes during re-grade.

### `GradingService.buildFieldResult` (and siblings)

```337:345:backend/src/main/java/com/eiu/capstone/backend/grading/GradingService.java
    private SubmissionFieldResult buildFieldResult(Map<UUID, SubmissionFieldResult> existing,
                                                   LabSubmission submission,
                                                   UUID fieldId,
                                                   boolean correct) {
        SubmissionFieldResult result = existing.getOrDefault(fieldId, new SubmissionFieldResult());
        result.setSubmission(submission);
        result.setField(fieldRepository.getReferenceById(fieldId));
        result.setCorrect(correct);
        return result;
    }
```

Same `getOrDefault(elementId, new …())` pattern for method, constructor, and challenge results.

### DB invariant (unchanged)

```17:24:backend/src/main/java/com/eiu/capstone/backend/model/SubmissionFieldResult.java
@Table(
        name = "submission_field_result",
        uniqueConstraints = @UniqueConstraint(
                name = "submission_field_result_key",
                columnNames = {"submission_id", "field_id"}
        )
)
```

## Why This Works

| Aspect | Delete-then-insert | Load-map-upsert |
|--------|-------------------|-----------------|
| SQL on re-grade | `DELETE` + `INSERT` (order not guaranteed) | `UPDATE` (or `INSERT` if first time) |
| Unique constraint | Violated if insert runs before delete flush | One row per `(submission_id, element_id)` |
| Re-upload | HTTP 500 | Idempotent score update |

When `existing.getOrDefault` finds a row, the entity already has a primary key and `saveAll` updates it. First upload inserts once. No delete/insert race.

## Prevention

1. Treat submission result tables as **upsert-by-natural-key** `(submission_id, element_id)`, not append-only.
2. Avoid delete-all-then-insert for uniquely constrained child rows unless you explicitly `flush()` after delete.
3. Regression check: upload the same lab attempt twice; expect HTTP 200 both times.
4. Reserve `deleteBySubmission` for explicit submission cleanup, not re-grade flows.

## Related Issues

- None in `docs/solutions/` yet (first entry).
