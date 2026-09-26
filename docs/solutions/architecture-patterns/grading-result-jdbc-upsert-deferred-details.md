---
title: JDBC UPSERT and deferred detail persist for grading results
date: 2026-08-27
category: architecture-patterns
module: grading engine
problem_type: architecture_pattern
component: service_object
severity: high
applies_when:
  - "Upload grade_ms is dominated by Grade submission save on large rubrics"
  - "Re-upload hits unique constraints on submission_*_result tables"
  - "Class tab needs member rows that are written after the upload HTTP response"
tags:
  - grading-persist
  - jdbc-upsert
  - neon
  - performance
  - on-conflict
---

# JDBC UPSERT and deferred detail persist for grading results

## Context

Upload `grade` time is almost entirely Postgres writes of per-field, per-method, per-constructor, per-relation, per-testcase, and per-assertion rows. Hibernate `saveAll` plus `loadExisting` on re-upload made Large labs tens of seconds. Challenge count is a weak size proxy; row count is the real one.

## Guidance

1. **Hot path:** `INSERT … ON CONFLICT` into `submission_challenge_result` only, plus the parsed snapshot files. Return `lab_result` from in-memory compute.
2. **After the request is scheduled:** `GradingResultJdbcWriter` UPSERTs member and testcase rows on `persistExecutor`. Prefer `ON CONFLICT (submission_id, testcase_id)` (or the matching unique columns) over `ON CONFLICT ON CONSTRAINT <name>` when the live DB may still have Postgres-default names from unnamed `UNIQUE (...)` DDL. Named constraints must match JPA (`submission_testcase_result_key`, etc.) — Neon historically had `submission_testcase_result_submission_id_testcase_id_key`, which made deferred OT detail persist fail after a successful upload score write.
3. **Revisit:** `GET /class`, `/mmd`, `/testcases` call `SubmissionDetailPersistGate.await` (60s) so tabs do not read an empty member set.
4. **Sidebar:** `ChallengeService.getChallengesForLab` uses stored challenge scores when those rows exist so dashboard refresh does not wait on details.
5. Do **not** `loadExisting` to merge entity IDs; UPSERT owns re-upload.

## Do not

- Put detail UPSERT on `gradingExecutor` (nested pool deadlock risk).
- Assume Class-tab GET can skip the gate because the upload response already had `lab_result` — a refresh hits the database.
