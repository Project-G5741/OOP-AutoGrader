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
| `TestcaseGrader` | Per-testcase orchestrator: compile-error short-circuit, run invocation or comparison once, evaluate every assertion, pick primary display via `PrimaryAssertionSelector`, emit `PendingTestcaseResult`. |
| `InvocationRunner` | `URLClassLoader` + reflection invoke, stdout capture, configurable timeout, comparison (`equals` / `compareTo`). |
| `AssertionEvaluator` | One evaluator per `AssertionKind`; guards null/error/timeout invocation outcomes before reading return values or fields. |
| `GradingService.buildTestcaseResult` | Upsert `submission_testcase_result` and child `submission_testcase_assertion_result` rows by natural keys (testcase id, assertion id). |

Pillar execution still runs on `pillarExecutor` inside `GradingPipeline`; invocations themselves serialize on a separate single-thread `testcaseInvokeExecutor` bean.

### Rubric loading — avoid LazyInitializationException

**Pattern:** When building `InvocationRubric` / `InstanceRubric`, resolve `className`, `methodName`, and field metadata from maps built while entities are still attached (`classNameByConstructorId`, `classNameByMethodId`, `methodById`, `fieldById`) — not by calling `getClassEntity().getName()` or similar lazy paths after batch queries return.

**Symptom:** `LazyInitializationException: could not initialize proxy … ClassEntity` on upload when `LabRubricService.loadForLab` touches unloaded associations.

### Invocation runner — classloader and timeout

**Pattern:** Create and close `URLClassLoader` **inside** the task submitted to `testcaseInvokeExecutor`, not around `future.get()`. On timeout, call `future.cancel(true)` so the worker thread is interrupted before the loader is closed.

**Anti-pattern:** Opening the classloader in the caller thread and closing it when `future.get` times out — the worker may still be running and can throw obscure errors or leak loaders.

**Stdout:** `System.setOut` is mutated during invoke. The single-thread invoke executor serializes all invocations app-wide so parallel challenge grading does not interleave stdout capture.

**Instance methods:** Non-static methods need a receiver object. When `testcase_invocation.receiver_constructor_id` is set, `InvocationRunner` constructs the receiver via that rubric constructor and `receiver_params` JSON before calling the method. When receiver columns are null, the runner falls back to a no-arg constructor (`instantiateDefault`); missing no-arg ctor surfaces `Instance method requires a no-argument constructor on …`. See `docs/solutions/logic-errors/method-invocation-receiver-constructor.md`.

### Assertion evaluation edge cases

- **Null invocation outcome:** COMPARISON testcases pass `invocationOutcome == null`. Non-comparison assertions must fail with feedback (`Invocation not available for this assertion`), not NPE.
- **Exception matching:** Walk the thrown type's superclass chain so subclasses match the expected simple name (e.g. `NumberFormatException` vs expected `IllegalArgumentException`). Plan R10 asked for simple-name match; subclass tolerance is deliberate.
- **Numeric equality:** `ValueComparator` compares `Number` values via `doubleValue()` so `5` and `5.0` match under EXACT mode.
- **Primary assertion tie-break:** Within the same priority kind, pick the lowest `orderIndex`.
- **Empty assertion list:** Treat as infrastructure `ERROR` (`No assertions configured`), not silent `FAILED`.

### Persistence on re-upload

Upsert assertion children by `testcaseAssertion.id` into a map of existing rows, then `clear()` and re-add the current set. JPA `orphanRemoval = true` on `SubmissionTestcaseResult.assertionResults` deletes rows removed from the rubric. Do not blindly delete-all-and-insert — that breaks stable child row identity across re-grades.

Infrastructure failures (compile error, timeout, missing rubric) currently persist **no** assertion child rows; only the parent testcase row with `ERROR` status and display fields.

### API phase 1

Scores include the testcase pillar; `LabResultAssembler` maps operational testcase I/O cards into upload and revisit bundles. Display fields are persisted at grade time and surfaced on the student Operation Test tab.

## Why This Matters

Operational grading executes untrusted student bytecode in-process. Correct layering keeps reflection, timeout, and stdout concerns out of the orchestrator; rubric loading bugs surface at upload time; persistence bugs corrupt re-upload history. Skipping these patterns reproduces production failures that unit tests on empty rubrics will not catch.

**Known limitations (document, do not "fix" in-JVM):**

- `Future.cancel(true)` cannot stop CPU-bound infinite loops; one hung thread blocks all testcase invocations until it finishes or times out.
- No sandbox — full JVM privileges for student code (acceptable for campus autograder scope).
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

### Classloader inside timeout task (correct)

```java
return runWithTimeout(() -> {
    try (URLClassLoader loader = classLoader(classesDir)) {
        return invokeSingleInternal(loader, invocation);
    }
});
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
