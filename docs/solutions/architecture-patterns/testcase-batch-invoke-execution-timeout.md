---
title: Per-challenge OT batch invoke and execution-only timeout
date: 2026-09-26
category: architecture-patterns
module: backend-grading
problem_type: performance_issue
component: service_object
symptoms:
  - "Sandbox OT grading feels slow even when student code is fast"
  - "TIMED_OUT appears when code finished but result return or RTT was slow"
root_cause: async_timing
resolution_type: code_fix
severity: high
tags:
  - operational-testcase
  - sandbox
  - batch-invoke
  - timeout
  - rtt
---

# Per-challenge OT batch invoke and execution-only timeout

## Problem

With remote sandbox enabled, each operational testcase paid a full HTTP round-trip. Labs with many OT rows were dominated by RTT. The same timeout also wrapped the entire IPC/`readLine` wait, so slow result return could mark a finished run as `TIMED_OUT`.

## Symptoms

- Upload or dry-run OT latency scales with testcase count × RTT, not just code time
- False `TIMED_OUT` when execution finished inside the budget but transport/return was slow

## What Didn't Work

- Keeping one HTTP invoke per testcase and only tuning the timeout number — RTT still dominates
- Using the bare invoke timeout as the HTTP client deadline — return/serialization still races the budget

## Solution

1. **`batch` IPC op** — `TestcaseGrader.grade` sends one round-trip per challenge with all runnable OT items; dry-run uses a one-item batch.
2. **Worker execution clock** — each batch item runs under `Future.get(timeoutSeconds)`; on timeout emit `TIMED_OUT`, **stop the batch in that JVM**, then `Runtime.halt(0)` so interrupt-immune loops die with the process.
3. **Transport slack + respawn continue** — API/runner wait is `itemCount × timeoutSeconds + 30s`. After a hang abort the API respawns (local process or remote session reopen) and runs remaining items on a clean worker.

Plan: `docs/plans/2026-09-26-002-perf-testcase-batch-invoke-timeout-plan.md`.

## Why This Works

RTT cost collapses to one trip per challenge. The execution budget only measures student code inside the worker. Successful on-time code can still return over a slow network without becoming `TIMED_OUT`.

## Prevention

- Do not set HTTP/`readLine` timeout equal to bare `app.grading.testcase-invoke-timeout-seconds` when result return can be slow
- Prefer challenge-scoped batch over per-testcase remote invokes when the session is already open
- Keep regression coverage for hang-then-continue (`WorkerInvokeEngineTest` batch timeout + `IsolatedWorkerAeTest`)

## Related Issues

- `docs/solutions/architecture-patterns/operational-testcase-grading.md`
- `docs/plans/2026-09-15-001-feat-container-sandbox-invoke-plan.md`
