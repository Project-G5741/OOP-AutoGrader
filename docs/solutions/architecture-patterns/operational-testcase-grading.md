---
title: Operational testcase grading patterns and pitfalls
date: 2026-08-11
category: architecture-patterns
module: backend-grading
problem_type: architecture_pattern
component: service_object
severity: high
applies_when:
  - "Extending or debugging the testcase grading pillar after the structural-to-operational migration"
  - "Upload or grading fails around rubric loading, reflection invoke, or assertion persistence"
tags:
  - operational-testcase
  - testcase-grader
  - invocation-runner
  - lazy-initialization
  - assertion-evaluator
  - grading-pipeline
---

# Operational testcase grading patterns and pitfalls

## Context

The testcase pillar moved from structural EXISTENCE/DECLARATION checks to **operational** grading: load compiled student classes, invoke constructors or methods (or build two instances for comparison), evaluate multiple assertion kinds, persist rollup I/O display fields plus per-assertion rows. Implementation spans rubric loading (`LabRubricService`), an orchestrator (`TestcaseGrader`), a reflection runner (`InvocationRunner`), per-kind evaluators (`AssertionEvaluator`), and grade-time persistence (`GradingService`).

This learning captures the layered shape and the non-obvious failures hit during first implementation and code review — not the full plan (see `docs/plans/2026-08-11-001-feat-operational-testcase-grading-plan.md`).

## Guidance

### Layered responsibilities

| Layer | Role |
|-------|------|
| `LabRubricService` | Batch-load invocation, instance, and assertion graph into immutable rubric records (`TestcaseRubric`, `InvocationRubric`, `AssertionRubric`). Resolve names and param types from pre-fetched maps — never traverse lazy associations after the repository session closes. |
| `TestcaseGrader` | Per-challenge orchestrator: compile-error short-circuit, one `batch` round-trip for runnable OT, evaluate every assertion, pick primary display via `PrimaryAssertionSelector`, emit `PendingTestcaseResult`. |
| `InvocationRunner` | IPC facade: send one NDJSON `batch` (or legacy `invoke`/`scenario`) to the isolated worker JVM and decode snapshots. Does not `Class.forName` student types in the API. |
| `AssertionEvaluator` | One evaluator per `AssertionKind`; FIELD_STATE and EXCEPTION use serialized snapshots, not live student objects. |
| `GradingService.buildTestcaseResult` | Upsert `submission_testcase_result` and child `submission_testcase_assertion_result` rows by natural keys (testcase id, assertion id). |

Pillar execution still runs on `pillarExecutor` inside `GradingPipeline`. The host allows at most one worker JVM; `GradingService` / `TestcaseDryRunService` acquire that slot on the HTTP request thread. Invokes of one request share that JVM under a per-session mutex.

### Invocation runner — isolated worker

**Pattern:** Operational invoke runs in a thin worker JAR. The API kills the process tree on timeout and respawns without releasing the slot. Student `URLClassLoader` parent is the platform loader. Stdout is capped at 65536 bytes. Worker env is allowlisted (not a `/proc` or filesystem jail).

**Stage 3 (optional):** When `app.grading.sandbox.enabled=true`, `WorkerSessionFactory` tars the submission root and opens a remote session on `sandbox-runner`, which runs `worker.jar` inside a hardened Docker container (`network none`, read-only root, tmpfs `/work`, cgroup limits). See `docs/SANDBOX_RUNNER_DEPLOY.md`. No Docker socket on the Render API image.

**Anti-pattern:** Invoking student methods in the API JVM, launching the worker with `PropertiesLauncher` / the API fat JAR, or treating env allowlist as host secret safety.

**Instance methods:** When `testcase_invocation.receiver_constructor_id` is set, the worker constructs the receiver via that rubric constructor and `receiver_params` JSON. When receiver columns are null, it falls back to a no-arg constructor. See `docs/solutions/logic-errors/method-invocation-receiver-constructor.md`.

### Rubric loading — avoid LazyInitializationException

**Pattern:** When building `InvocationRubric` / `InstanceRubric`, resolve `className`, `methodName`, and field metadata from maps built while entities are still attached (`classNameByConstructorId`, `classNameByMethodId`, `methodById`, `fieldById`) — not by calling `getClassEntity().getName()` or similar lazy paths after batch queries return.

**Symptom:** `LazyInitializationException: could not initialize proxy … ClassEntity` on upload when `LabRubricService.loadForLab` touches unloaded associations.

### Assertion evaluation edge cases

- **Null invocation outcome:** COMPARISON testcases pass `invocationOutcome == null`. Non-comparison assertions must fail with feedback (`Invocation not available for this assertion`), not NPE.
- **Exception matching:** Walk serialized exception simple names (thrown type plus superclasses) so subclasses match (e.g. `NumberFormatException` vs expected `IllegalArgumentException`).
- **Numeric equality:** `ValueComparator` compares `Number` values via `doubleValue()` so `5` and `5.0` match under EXACT mode.
- **Primary assertion tie-break:** Within the same priority kind, pick the lowest `orderIndex`.
- **Empty assertion list:** Treat as infrastructure `ERROR` (`No assertions configured`), not silent `FAILED`.

### Persistence on re-upload

Upsert assertion children by `testcaseAssertion.id` into a map of existing rows, then `clear()` and re-add the current set. JPA `orphanRemoval = true` on `SubmissionTestcaseResult.assertionResults` deletes rows removed from the rubric. Do not blindly delete-all-and-insert — that breaks stable child row identity across re-grades.

Infrastructure failures (compile error, timeout, missing rubric) currently persist **no** assertion child rows; only the parent testcase row with `ERROR` status and display fields.

### API phase 1

Scores include the testcase pillar; `LabResultAssembler` maps operational testcase I/O cards into upload and revisit bundles. Display fields are persisted at grade time and surfaced on the student Operation Test tab.

## Why This Matters

Operational invoke runs in an isolated worker JVM (local stage 2) or container sandbox (stage 3 when enabled). Class-tab load still uses `Class.forName(..., false, ...)` in the API and does not initialize student classes. With sandbox disabled, remaining gaps are filesystem, network, same-UID `/proc`, and cgroup jail. Env allowlist is not host secret safety.

**Known limitations (document, do not "fix" in-JVM):**

- Timeout tree-kills the worker JVM (and Linux children) and respawns without releasing the host slot. `Future.cancel` is not the invoke timeout path.
- Remaining stage-3 gaps: filesystem, network, same-UID `/proc`, and cgroup jail. Env allowlist is not host secret safety.
- Timeout currently maps to testcase-level `ERROR` before per-assertion evaluation; plan AE6-style per-assertion timeout rows are not fully implemented.

## When to Apply

- Adding a new `AssertionKind` or changing invoke semantics.
- Debugging upload failures in `LabRubricService` or grading failures in `TestcaseGrader` / `InvocationRunner`.
- Changing how testcase results persist on re-upload.
- Writing rubric seed SQL — METHOD rows on classes without no-arg constructors need `receiver_constructor_id` + `receiver_params`; void mutators need `FIELD_STATE` assertions; COMPARISON testcases need exactly two instances and a `COMPARISON_RESULT` assertion.

## Examples

### Rubric name resolution (correct)

```java
// LabRubricService — resolve from pre-built maps, not lazy entity graph
context.classNameByMethodId().get(methodId)
context.methodById().get(methodId).getName()
```

### Isolated worker invoke (correct)

```java
// GradingService / TestcaseDryRunService acquire workerJvmSlot on the HTTP thread
SerializedInvocationOutcome facts = workerSession.invoke(classesDir, invocation, snapshotFields);
```

### Assertion upsert on re-upload (correct)

```java
Map<UUID, SubmissionTestcaseAssertionResult> existingAssertions =
    result.getAssertionResults().stream()
        .collect(Collectors.toMap(
            row -> row.getTestcaseAssertion().getId(),
            row -> row,
            (left, right) -> left));
result.getAssertionResults().clear();
// reuse or create per assertionPending.assertionId()
```

## Related

- `docs/plans/2026-08-11-001-feat-operational-testcase-grading-plan.md` — full requirements and acceptance scenarios (AE1–AE7).
- `docs/sql/2026-08-11-operational-testcase-grading.sql` — destructive schema migration.
- `docs/solutions/architecture-patterns/grading-executor-deadlock-render.md` — why MMD + testcase pillars use `pillarExecutor`, not `gradingExecutor`.
- `docs/solutions/architecture-patterns/in-memory-challenge-compile-path.md` — compile vs grading executor split.
- `docs/solutions/logic-errors/method-invocation-receiver-constructor.md` — receiver construction for METHOD invocations without no-arg constructors
