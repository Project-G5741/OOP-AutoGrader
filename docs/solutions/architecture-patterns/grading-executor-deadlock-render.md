---
title: Grading executor deadlock on single-CPU Render deploys
date: 2026-08-10
category: architecture-patterns
module: grading pipeline
problem_type: bug
component: service_object
severity: critical
applies_when:
  - "Upload hangs after compile_timing logs on Render or other 1–2 CPU hosts"
  - "Student UI stuck on Uploading with no grading_timing log"
  - "Nested CompletableFuture.join on gradingExecutor from gradingExecutor workers"
tags:
  - grading-executor
  - pillar-executor
  - deadlock
  - render
  - upload-hang
---

# Grading executor deadlock on single-CPU Render deploys

## Symptom

Render logs show `read_timing` and `compile_timing` for all challenges, then silence. The student UI stays on "Uploading…" indefinitely. No `grading_timing` line appears when `app.grading.timing-log=true`.

## Root cause

`GradingService` grades challenges in parallel on `gradingExecutor`. Each worker calls `GradingPipeline.gradeChallenge()`, which submits MMD + testcase pillars back onto the **same** `gradingExecutor` and blocks with `CompletableFuture.join()`.

`FixedExecutorFactory` caps pool size at `min(parallelism, availableProcessors())`. On Render free tier that is often **1 thread**. One challenge worker blocks waiting for pillar tasks; no thread remains to run them → permanent deadlock.

Compile completes because `compileExecutor` has no nested blocking submits on itself.

## Fix

Use a dedicated `pillarExecutor` bean (`PillarExecutorConfig`) for MMD + testcase work inside `GradingPipeline`. Size: `max(2, app.grading.parallelism × 2)` without the CPU cap.

## Verification

After deploy, upload a 3-challenge lab. Expect `grading_timing` (when timing log enabled) within seconds of the last `compile_timing` line, and the upload response to return.
