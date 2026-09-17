---
title: Unique order_index collision when omitting earlier testcase scenario steps
date: 2026-09-17
category: database-issues
module: testcase-rubric
problem_type: database_issue
component: service_object
symptoms:
  - "PUT /api/lecturer/labs/{labId}/challenges/{challengeId}/testcases fails when an earlier scenario step is omitted or reordered"
  - "Postgres UNIQUE (testcase_id, order_index) fires mid-save because two invocation rows briefly share the same order_index"
  - "Lecturer sees HTTP 422 with a generic DataIntegrityViolationException mapping, not a 422 validation message about step order"
root_cause: logic_error
resolution_type: code_fix
severity: high
tags:
  - testcase-scenario
  - order-index
  - unique-constraint
  - reindex
  - sync-by-presence
related_components:
  - database
---

# Unique order_index collision when omitting earlier testcase scenario steps

## Problem

Lecturer save of a multi-step **testcase scenario** failed when an earlier invocation step was omitted or reordered. `TestcaseRubricService.syncInvocations` already upserts child rows in place (sync-by-presence), but it wrote final dense `order_index` values before deleting omitted rows. Postgres checks `UNIQUE (testcase_id, order_index)` immediately, so a kept step moved to index `0` while the extra row still occupied `0`.

This fix is in the working tree on `feat/testcase-pillar-oop-scenarios` and is unmerged as of this writing (local git: no remote tracking branch and the service change is uncommitted; no PR checked).

## Symptoms

- `PUT /api/lecturer/labs/{labId}/challenges/{challengeId}/testcases` (`LecturerRubricController.java:91`) fails after the lecturer deletes the first of two (or more) scenario steps and saves.
- The failure is a `DataIntegrityViolationException` on constraint `testcase_invocation_testcase_id_order_index_key`. `GlobalExceptionHandler` maps that exception to HTTP **422** (`GlobalExceptionHandler.java:58-62`) with a generic "Could not save testcase data" body — the handler does not special-case this unique-index name (`GlobalExceptionHandler.java:65-83`).
- Single-step testcases and append-only edits do not reproduce it: those saves never need a kept row to take an index still held by a row that has not been deleted yet.
- COMPARISON testcases are structurally out of this class: they have zero invocation steps and hit the empty-list `deleteAll` path instead of park-then-compact.

## What Didn't Work

- Writing each kept step's **final** `order_index` (`0..n-1`) in the same pass as upsert, then deleting extras afterward. The end state would be valid, but the first `UPDATE` already collides with a still-present extra row.
- Deleting extras first, then compacting kept rows, still leaves two *kept* rows swapping indexes in one flush (reorder `[A(0), B(1)]` → `[B(0), A(1)]`) under an immediate unique check.
- Making the unique constraint `DEFERRABLE INITIALLY DEFERRED` would hide the transient collision until commit, but would change schema semantics for every writer of `testcase_invocation`. The operator SQL and migrator add a plain `UNIQUE` with no deferral (`docs/sql/2026-09-17-testcase-scenario-steps.sql:32-35`, `TestcaseSchemaMigrator.java:98-102`).

## Solution

Park kept rows outside the real step range, flush, delete extras, flush, then write dense `0..n-1`.

`ORDER_INDEX_PARK` equals `MAX_STEPS` (20). Parked indexes are `20 + i`, so they cannot overlap real indexes `0..19` while payloads stay within the step cap enforced by `validateScenarioSteps` before `syncInvocations` runs (`TestcaseRubricService.java:68-70`, `389-390`). Do not raise `MAX_STEPS` without keeping `ORDER_INDEX_PARK` strictly above every possible existing `order_index`.

```java
applyInvocation(invocation, dto, ORDER_INDEX_PARK + i, memberIds);
// persist or save each kept row
entityManager.flush();

for (TestcaseInvocation extra : existing) {
    if (!kept.containsKey(extra.getId())) {
        testcaseInvocationRepository.delete(extra);
    }
}
entityManager.flush();

int orderIndex = 0;
for (TestcaseInvocation invocation : kept.values()) {
    invocation.setOrderIndex(orderIndex++);
    testcaseInvocationRepository.save(invocation);
}
entityManager.flush();
```

(`TestcaseRubricService.java:573-595`)

JPA unique mapping matches the SQL name (`TestcaseInvocation.java:19-24`).

## Why This Works

Postgres unique constraints that are not `DEFERRABLE` are checked per statement. Each flush emits SQL before the next phase:

1. After park-and-flush, kept rows sit in `[20, 20+n)` while doomed rows still sit in their old `[0, old_n)`. Those ranges cannot overlap because `old_n` cannot exceed `MAX_STEPS`.
2. After delete-and-flush, only parked kept rows remain.
3. The final loop assigns `0..n-1` to rows whose current indexes are all `>= 20`, so no target index is already held by another surviving row.

Skipping any of the three flushes reopens the window where two rows share an `order_index`. This reasoning is single-transaction: concurrent `PUT`s on the same testcase were not separately tested; safety there depends on ordinary row locks under the save `@Transactional`, not on parking.

An empty `steps` list deletes existing invocation rows and returns (`TestcaseRubricService.java:557-560`) — that path only shrinks the occupied set, so it does not need parking. Assertion `order_index` is not uniquely constrained the same way; this pattern applies to `testcase_invocation`, not `syncAssertions`.

No stored rows need backfill: the unique constraint already existed, so failed saves never committed a duplicate. Only the save sequencing changed.

## Prevention

- Keep `saveForChallenge_omittingFirstStep_reindexesWithoutDuplicateOrder` (`TestcaseRubricServiceTest.java:743`). A test that only appends or drops the *last* step will not catch this class of collision.
- The in-memory unique-index assertion on `persist`, `save`, and `flush` (`TestcaseRubricServiceTest.java:131-147`, `895-903`) fails the suite as soon as two stored invocations share `(testcaseId, orderIndex)` mid-save — including during the park phase if parking is omitted. It does not cover concurrent transactions or a combined omit-plus-reorder in one payload.
- When reassigning a unique dense order column under a non-deferrable unique constraint, use park-then-delete-then-compact with a flush between phases. Do not rely on "the transaction would be consistent at commit."
- Prefer this local save-path sequencing over making `testcase_invocation_testcase_id_order_index_key` deferrable.

## Related Issues

- [Lecturer testcase save 500 and assertion history loss on re-save](../logic-errors/lecturer-testcase-save-persistence.md) — same service and sync-by-presence upsert; that learning covers persist vs merge and assertion-id cascade, not unique `order_index` reindex
- [Duplicate key on submission result re-upload](./submission-result-reupload-duplicate-key.md) — analogous unique-key collision from delete-then-insert without flush ordering, different table
- [Operational testcase grading patterns](../architecture-patterns/operational-testcase-grading.md) — grading-time `orderIndex` usage; not the lecturer save path
- GitHub issue search for this constraint was skipped (`gh` CLI not available in the compounding environment)
