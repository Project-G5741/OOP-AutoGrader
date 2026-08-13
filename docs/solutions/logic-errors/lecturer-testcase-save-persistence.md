---
title: Lecturer testcase save 500 and assertion history loss on re-save
date: 2026-08-13
category: logic-errors
module: backend-testcase-rubric
problem_type: logic_error
component: service_object
symptoms:
  - "PUT /api/lecturer/labs/{labId}/challenges/{challengeId}/testcases returns HTTP 500"
  - "PostgreSQL constraint errors mentioning testcase_assertion_field_check or missing receiver_params"
  - "Re-saving an existing testcase after student submissions silently drops per-assertion history rows"
root_cause: logic_error
resolution_type: code_fix
severity: high
tags:
  - lecturer-testcase
  - testcase-rubric
  - jpa-persist
  - assertion-upsert
  - database-constraint
related_components:
  - database
  - frontend_stimulus
---

# Lecturer testcase save 500 and assertion history loss on re-save

## Problem

The lecturer **Operational Testcases** panel could not reliably persist testcase rubrics: saves returned HTTP 500, and even after saves succeeded, editing and re-saving a testcase could destroy historical per-assertion grading rows for past student submissions.

## Symptoms

- `PUT /api/lecturer/labs/{labId}/challenges/{challengeId}/testcases` returned **500** (or 422 with raw SQL fragments before `DataIntegrityViolationException` mapping was added).
- Server logs showed FK violations (`testcase_invocation` inserted before parent `testcase` flushed), Hibernate `merge` errors on entities with client-assigned UUIDs, or PostgreSQL `testcase_assertion_field_check` violations.
- On older databases, errors referenced missing `receiver_params` / `receiver_constructor_id` columns.
- After students had submitted, a lecturer edit + re-save could leave `submission_testcase_assertion_result` rows orphaned or deleted.

## What Didn't Work

- Using `testcaseRepository.save()` for new rows when the UI already assigned UUIDs — Spring Data called `merge()`, which conflicts with `@GeneratedValue` on `Testcase` / `TestcaseInvocation`.
- Flushing only at transaction end — invocation rows inserted before the parent testcase row was visible caused FK failures.
- Sending `fieldId` on every assertion from the frontend — non-`FIELD_STATE` kinds violated the DB check that only `FIELD_STATE` may reference `field_id`.
- `syncAssertions` / `syncInvocation` using **delete-all then re-insert** — new assertion UUIDs triggered `ON DELETE CASCADE` on `submission_testcase_assertion_result.testcase_assertion_id`.

## Solution

### 1. Client UUID upsert with `EntityManager.persist()`

Remove `@GeneratedValue` from entities that receive client IDs (`Testcase`, `TestcaseInvocation`, `TestcaseAssertion`). Branch on whether the row exists:

```java
if (isNew) {
    entityManager.persist(testcase);
} else {
    testcase = testcaseRepository.save(testcase);
}
entityManager.flush();
```

(`TestcaseRubricService.java` — `upsertTestcase`, `syncInvocation`, `syncAssertions`)

### 2. Upsert child rows by DTO id (do not delete-all)

Replace `deleteAll` + insert for assertions and invocations with:

- Resolve existing row by `dto.id()` (or single existing invocation when id omitted).
- Update fields in place; `persist` only when new.
- Delete only rows **omitted** from the payload (`keptIds` set).

This preserves `testcase_assertion.id` across lecturer edits so `submission_testcase_assertion_result` FKs survive.

### 3. Enforce assertion/field pairing

Backend: only call `assertion.setField(...)` when `assertionKind == FIELD_STATE`; set `field` to `null` otherwise. Reject `COMPARISON_RESULT` on `SINGLE_INVOCATION` testcases.

Frontend (`TestcasesPanel.jsx`): `normalizeTestcaseForApi()` clears `fieldId` for non-`FIELD_STATE` assertions before save and dry-run.

### 4. Schema drift for receiver columns

`TestcaseSchemaMigrator` adds `receiver_constructor_id` and `receiver_params` only when `information_schema` reports they are missing (no drop/recreate on every boot).

### 5. Actionable API errors

`GlobalExceptionHandler` maps `DataIntegrityViolationException` to user-facing 422 messages (field check, FK, missing columns). Catch-all `Exception` handler returns a generic 500 body — log details server-side only.

## Why This Works

The UI generates stable UUIDs for testcases, invocations, and assertions so lecturers can edit drafts offline. JPA `save()` on a detached entity with a pre-assigned id is a merge path, not an insert path. `persist()` + explicit `flush()` establishes parent rows before child FKs.

Assertion rows are referenced by graded submissions. Replacing them on every save is not a cheap refresh — it is a destructive cascade. Upsert-by-id matches how `GradingService` keys results on `testcase_assertion_id`.

## Prevention

- **Integration test:** `TestcaseRubricServiceIntegrationTest` saves a testcase graph against real PostgreSQL; extend with a re-save scenario that asserts `submission_testcase_assertion_result` rows remain when assertion content changes but ids are preserved.
- **Save contract:** treat testcase child rows as upsert-by-id; never `deleteAll` + reinsert when FK children exist in `submission_*` tables.
- **Payload sanitization:** share one normalizer for save and dry-run; clear incompatible invocation fields when switching CONSTRUCTOR ↔ METHOD.
- **CHECK constraints:** validate assertion kind / field pairing in `validateTestcaseDto` before persist.

## Related Issues

- [Operational testcase grading patterns](../architecture-patterns/operational-testcase-grading.md) — grading stack reused for dry-run; natural keys on assertion ids
- [Duplicate key on submission result re-upload](../database-issues/submission-result-reupload-duplicate-key.md) — parallel upsert-by-natural-key pattern in submission persistence
- Plan: `docs/plans/2026-08-12-005-feat-lecturer-operational-testcase-ui-plan.md` (R1–R5, R16)
