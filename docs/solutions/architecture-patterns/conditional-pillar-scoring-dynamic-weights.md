---
title: "Conditional Pillar Scoring with Dynamic Weight Redistribution"
date: 2026-08-12
category: architecture-patterns
module: grading
problem_type: architecture_pattern
component: service_object
severity: medium
applies_when:
  - "Adding a new conditionally-applicable scoring pillar to a multi-pillar grading system"
  - "Redistributing weight across only the active subset of scoring dimensions instead of hardcoding fixed percentages per pillar"
  - "Skipping executor/thread-pool submission for a pipeline stage that is already known to be inapplicable, rather than running it and discarding a trivial result"
  - "Hiding UI tabs or sections for inapplicable content entirely, instead of showing them disabled or marked not-applicable"
  - "Evolving a rubric/DTO schema across DB, backend, and frontend while preserving backward compatibility for existing saved data"
tags:
  - mmd-grading
  - pillar-scoring
  - dynamic-weight-redistribution
  - conditional-grading-pipeline
  - tab-visibility
  - backward-compatible-dto
  - grading-pipeline
related_components:
  - database
---

# Conditional Pillar Scoring with Dynamic Weight Redistribution

> **Status note**: as of this writing, all changes described below are uncommitted local
> modifications (`git status` shows them as `M`/`??`, not part of any commit). File references
> are to current-tree state, not a specific commit.

## Context

The grader scores a challenge across three "pillars": Class/Declaration (reflection-based
structure check), MMD (diagram-comparison), and Testcase (operational I/O checks). The original
design assumed every challenge needed all three, so the challenge-level score was a fixed
equal-weighted mean across exactly three numbers. That assumption broke down for two real cases:
some challenges genuinely have no MMD diagram requirement, and some have no operational
testcases configured. Forcing those into the 3-way mean silently penalized challenges for
dimensions that were never supposed to apply, and there was no way to express "this pillar
doesn't apply here" — only "this pillar scored zero."

The fix generalizes the challenge percentage to the mean of whichever pillars are applicable
(1, 2, or 3 of them), threads an explicit applicability signal from a new `has_mmd` DB column
through every layer up to the UI, and — as a UX correction made mid-implementation — hides
inapplicable pillars from the student tab bar entirely rather than showing them as "not scored."

## Guidance

### (a) Dynamic N-applicable-pillar mean, not per-combination special cases

Instead of writing separate formulas for "MMD+Class only," "Testcase+Class only," "all three,"
the aggregator accumulates a running sum and count, adding each pillar only if its flag is true:

```51:67:backend/src/main/java/com/eiu/capstone/backend/grading/scoring/PillarScoreAggregator.java
    public static BigDecimal challengePercentage(BigDecimal classPct,
                                                BigDecimal mmdPct,
                                                boolean mmdApplicable,
                                                BigDecimal testcasePct,
                                                boolean testcaseApplicable) {
        BigDecimal sum = safe(classPct);
        int applicableCount = 1;
        if (mmdApplicable) {
            sum = sum.add(safe(mmdPct));
            applicableCount++;
        }
        if (testcaseApplicable) {
            sum = sum.add(safe(testcasePct));
            applicableCount++;
        }
        return sum.divide(BigDecimal.valueOf(applicableCount), SCALE, RoundingMode.HALF_UP);
    }
```

Class is always applicable (hence `sum`/`applicableCount` start seeded with it); MMD and
Testcase are each folded in only when their flag is true. Adding a fourth conditional pillar
later means adding one more `if (flagApplicable) { sum = sum.add(...); applicableCount++; }`
block — no rewrite of existing branches.

### (b) Explicit boolean applicability flag, never inferred from null/zero

A zero score is ambiguous ("scored zero" vs. "doesn't apply"), so applicability is carried as
its own boolean at every layer instead of being inferred from the score value:

- `Challenge.hasMmd` (JPA entity)
- `ChallengeRubric.hasMmd` — `backend/src/main/java/com/eiu/capstone/backend/grading/rubric/ChallengeRubric.java:13`
- `ChallengeStructureDTO.hasMmd` — `backend/src/main/java/com/eiu/capstone/backend/DTO/rubric/ChallengeStructureDTO.java:12`
- `GradingPipeline` computes both flags explicitly before scoring:

```68:69:backend/src/main/java/com/eiu/capstone/backend/grading/pipeline/GradingPipeline.java
        boolean mmdApplicable = challengeRubric.hasMmd();
        boolean testcaseApplicable = !challengeRubric.testcases().isEmpty();
```

- `ChallengeDetailBundleDTO.scoreApplicability` is a `Map<String, Boolean>` (keys `"mmd"`,
  `"testcase"`) that ships to the frontend as the single source of truth — `backend/src/main/java/com/eiu/capstone/backend/DTO/ChallengeDetailBundleDTO.java:14`.
- The frontend mirrors this discipline instead of re-deriving applicability from the score:

```42:44:frontend/src/components/ui/ScorePill.jsx
export function isPillarNotApplicable(bundle, pillarKey) {
  return bundle?.scoreApplicability?.[pillarKey] === false;
}
```

### (c) Canonical `empty()` / `notApplicable()` static factories to avoid wasted executor work

When a pillar doesn't apply, the pipeline must still produce a result object with the right
shape (zero score, empty list) for downstream code — but it must not submit a real grading task
to the shared thread pool just to get that trivial result. Both pillar result types expose a
static factory for the canonical "not applicable" value, and the pipeline branches on the
applicability flag when building each `CompletableFuture`:

```71:76:backend/src/main/java/com/eiu/capstone/backend/grading/pipeline/GradingPipeline.java
        CompletableFuture<MmdPillarGrader.MmdPillarResult> mmdFuture = mmdApplicable
                ? CompletableFuture.supplyAsync(() -> mmdPillarGrader.grade(challengeRubric, mmdFiles), pillarExecutor)
                : CompletableFuture.completedFuture(MmdPillarGrader.notApplicable());
        CompletableFuture<TestcaseGrader.TestcasePillarResult> testcaseFuture = testcaseApplicable
                ? CompletableFuture.supplyAsync(() -> testcaseGrader.grade(context), pillarExecutor)
                : CompletableFuture.completedFuture(TestcaseGrader.TestcasePillarResult.empty());
```

`notApplicable()` is a static method on `MmdPillarGrader` itself (not on the nested
`MmdPillarResult` record) — a subtlety worth calling out because an early draft mistakenly wrote
`MmdPillarGrader.MmdPillarResult.notApplicable()` and had to be corrected.
`TestcasePillarResult.empty()` is a static factory on the nested record, added later in a
follow-up simplification pass once the same wasted-executor-submission issue was noticed on the
testcase side:

```200:208:backend/src/main/java/com/eiu/capstone/backend/grading/pipeline/TestcaseGrader.java
    public record TestcasePillarResult(BigDecimal pillarPercentage, List<PendingTestcaseResult> results) {
        /**
         * Canonical result for a challenge with no operational testcases — matches what
         * {@link #grade} would return for an empty testcase list, without invoking the grader.
         */
        public static TestcasePillarResult empty() {
            return new TestcasePillarResult(BigDecimal.ZERO, List.of());
        }
    }
```

Both factories are documented as returning exactly what the real grader would have returned for
the degenerate input (no MMD submitted / no testcases configured), so callers never need to
special-case "was this computed or skipped."

### (d) Memoized visible-tabs list + active-tab-reset effect (frontend pattern)

For a tabbed UI where some tabs are conditionally hidden based on data that can change (e.g. when
the user switches to a different challenge), compute the visible tab list with `useMemo` keyed on
the data flag, then use a separate `useEffect` to redirect the active tab if it's no longer in the
visible set:

```150:162:frontend/src/components/student/StudentUI.jsx
  const currentBundle = selectedChallengeId ? sessionChallengeBundles[selectedChallengeId] : null;
  const visibleTabs = useMemo(
    () => TAB_ORDER.filter((t) => !(resultsRevealed && isPillarNotApplicable(currentBundle, t))),
    [resultsRevealed, currentBundle],
  );

  // If the active tab's pillar is inapplicable for the newly selected challenge (hidden from
  // the tab bar), fall back to the first pillar that's still visible.
  useEffect(() => {
    if (visibleTabs.length && !visibleTabs.includes(activeTab)) {
      setActiveTab(visibleTabs[0]);
    }
  }, [visibleTabs, activeTab]);
```

The tab bar then just maps over `visibleTabs` instead of the full `TAB_ORDER`. Note
`resultsRevealed` gates the filter — before results are revealed, all tabs stay visible so the
shell doesn't jump around during upload.

### (e) Legacy-overload constructor for backward-compatible record/DTO field additions

Adding a required field to a widely-used Java `record` breaks every existing call site. The
pattern used here: keep the canonical constructor with the new field, and add overloaded
constructors matching the old call signatures that delegate to the canonical one with a sane
default:

```15:30:backend/src/main/java/com/eiu/capstone/backend/grading/rubric/ChallengeRubric.java
    public ChallengeRubric(UUID challengeId,
                           int challengeNumber,
                           String name,
                           List<ClassRubric> classes,
                           List<RelationRubric> relations) {
        this(challengeId, challengeNumber, name, classes, relations, List.of(), true);
    }

    public ChallengeRubric(UUID challengeId,
                           int challengeNumber,
                           String name,
                           List<ClassRubric> classes,
                           List<RelationRubric> relations,
                           List<TestcaseRubric> testcases) {
        this(challengeId, challengeNumber, name, classes, relations, testcases, true);
    }
```

Both legacy overloads default `hasMmd=true` — matching the migration default, so old callers that
never knew about MMD applicability keep behaving exactly as before. `ChallengeStructureDTO`
follows the identical shape.

## Why This Matters

- **Extensibility**: the sum/count accumulation in `challengePercentage` means a future 4th
  pillar (or a pillar becoming conditional that used to be mandatory) is a small additive change,
  not a rewrite of a combinatorial set of hand-coded cases.
- **Avoiding misleading UI**: the original plan kept inapplicable-pillar tabs visible with a
  "not applicable" badge, but in practice the tab content underneath still rendered red-X/no-data
  states that looked like failures. The requirement was corrected mid-implementation to hide the
  tab entirely — a UI element a user can't click into can't lie to them about their score.
- **Avoiding ambiguous null/zero signals**: a `BigDecimal.ZERO` pillar score is legitimately
  reachable both by "not applicable" and "applicable but the student scored 0%." Carrying a
  separate boolean (`hasMmd`, `mmdApplicable`, `scoreApplicability`) means no code anywhere has to
  guess which case it's in.
- **Avoiding wasted thread-pool work**: submitting a grading task to `pillarExecutor` for a
  pillar that's guaranteed to short-circuit to zero wastes a thread-pool slot and adds needless
  latency/contention under load. The `notApplicable()`/`empty()` static factories make the
  "skip the real work" path just as fast to reach as "do the real work," while keeping the same
  result type for both.
- **Not breaking existing callers**: `ChallengeRubric` and `ChallengeStructureDTO` are constructed
  from several places in the codebase (parsers, test fixtures, other services). The legacy
  overload pattern let the new field ship without hunting down and updating every call site
  immediately — callers migrate to the explicit-flag constructor on their own schedule.
- **No automatic regrade**: changing `has_mmd` on a challenge does not retroactively regrade past
  submissions. Re-upload is the supported path to get a submission scored under the new rubric
  setting; no separate regrade endpoint was built, keeping the scope of this change contained to
  the scoring/pipeline/UI layers.

## When to Apply

- **Multi-dimension/multi-pillar scoring systems** where a dimension may legitimately not apply
  to every scored entity (e.g. optional rubric categories, per-item exemptions) — use the
  applicable-subset mean pattern instead of hard-coding N.
- **Tabbed or sectioned UIs with data-driven optional sections** — use the memoized-visible-list +
  reset-effect pattern whenever the set of valid tabs/sections can change out from under the
  currently active selection (e.g. switching records, filters, or permissions).
- **Adding fields to widely-referenced DTOs, entities, or records** where an unknown number of
  existing callers must keep compiling without changes — add a legacy-overload constructor with a
  safe default rather than making the field mandatory everywhere at once.
- **Any place a "task might be a no-op" decision is being made before dispatching to a thread
  pool/executor** — prefer a canonical pre-built "skip" result via a static factory over
  submitting a trivial task just to get a uniform `Future`.

## Examples

**Before (implicit, always-3-way)**: a hypothetical fixed-arity aggregator would look like
`(classPct + mmdPct + testcasePct) / 3` — no way to express 2-of-3 or 1-of-3 without either
padding the missing pillar with a fake 100% (masks it) or a real 0% (unfairly penalizes it).

**After**: see the accumulation snippet in Guidance (a) — the count of terms in the average is
itself dynamic.

**Before (naive future creation, mirroring the MMD pillar's pre-fix state)**:
```java
CompletableFuture<TestcaseGrader.TestcasePillarResult> testcaseFuture =
        CompletableFuture.supplyAsync(() -> testcaseGrader.grade(context), pillarExecutor);
```
always submits to the executor, even when `challengeRubric.testcases().isEmpty()`.

**After** (current tree, symmetric with the MMD pillar): see Guidance (c) snippet —
conditional on `testcaseApplicable`, using `TestcaseGrader.TestcasePillarResult.empty()`.

## Related

- [`operational-testcase-grading.md`](../architecture-patterns/operational-testcase-grading.md) —
  shares `TestcaseGrader.java` and `GradingPipeline.java`, and the same discipline of keeping
  degenerate/skip cases as explicit typed results instead of null or inferred state. Different
  root cause (reflection-invoke correctness vs. per-challenge pillar applicability).
- [`grading-executor-deadlock-render.md`](../architecture-patterns/grading-executor-deadlock-render.md) —
  establishes the `pillarExecutor` that the short-circuit factories in this doc avoid submitting
  wasted work to.
- [`in-memory-challenge-compile-path.md`](../architecture-patterns/in-memory-challenge-compile-path.md) —
  documents the broader compile/grading executor split for context.
