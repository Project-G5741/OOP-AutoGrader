# Grading Engine

## Purpose

Grade lab submissions across three equal pillars per challenge: Java `.class` reflection, MMD diagram comparison, and operational testcase checks. Produce per-element results, pillar scores, and an upload-time `lab_result` bundle for the student UI.

## Ownership

| File | Role |
|---|---|
| `GradingService.java` | Thin orchestrator: parallel per-challenge grading, persistence, `lab_result` assembly |
| `grading/pipeline/GradingPipeline.java` | Staged pipeline: class pillar, then parallel MMD + testcase pillars |
| `grading/pipeline/ClassReflectionGrader.java` | `.class` pillar with partial credit on declarations |
| `grading/pipeline/MmdPillarGrader.java` | MMD pillar |
| `grading/pipeline/TestcaseGrader.java` | Operational testcase orchestrator |
| `grading/testcase/InvocationRunner.java` | Load student classes, invoke constructors/methods with timeout + stdout capture |
| `grading/testcase/AssertionEvaluator.java` | Per-kind assertion evaluation (RETURN_VALUE, FIELD_STATE, STDOUT, EXCEPTION, COMPARISON_RESULT) |
| `grading/testcase/TestcaseDisplayFormatter.java` | Primary I/O card display strings + lazy expanded assertion formatting |
| `grading/testcase/PrimaryAssertionSelector.java` | Primary assertion priority for collapsed card |
| `grading/testcase/TestcaseResultMapper.java` | Map rubric + persisted results to student-facing `TestcaseResultDTO` |
| `grading/scoring/PillarScoreAggregator.java` | Pillar, challenge (mean of 3 pillars), and lab percentages |
| `grading/scoring/PartialCreditEvaluator.java` | Per-attribute accuracy for class-reflection DECLARATION checks |
| `grading/LabResultAssembler.java` | Build `lab_result.challenge_<N>` bundles for upload response |
| `ParsedSubmissionSnapshotBuilder.java` | Capture rubric-scoped student display text at grade time |
| `GradingResultStore.java` | Short read/write transactions for submission result tables |
| `grading/rubric/LabRubricService.java` | Load full lab rubric (invocations, instances, assertions) in batched DB queries |
| `grading/rubric/LabRubricCache.java` | In-process TTL cache keyed by lab ID |
| `grading/rubric/LabRubricSnapshot.java` | Immutable rubric graph for grading |
| `MmdParser.java` | Parse uploaded `.mmd` bytes into diagram DTOs |
| `MmdComparisonService.java` | Compare parsed MMD against rubric |
| `ReflectionClassParser.java` | Load `.class` files via `URLClassLoader` |

## Local Contracts

### Pipeline position

```
SubmissionController
  → LabRubricCache.get(lab)
  → SubmissionStorageService.processUpload()
  → GradingService.gradeSubmission()
      → GradingPipeline.gradeChallenge() per folder
          → ClassReflectionGrader (sync)
          → MmdPillarGrader + TestcaseGrader (parallel on `pillarExecutor`, not `gradingExecutor`)
      → GradingResultStore.save()
      → LabResultAssembler.assemble() → upload response lab_result
  → MmdPersistenceHook.onUploadComplete()
  → SubmissionStorageService.deleteFolder() (finally)
```

### Scoring

- **Pillar percentage** = weighted mean of member accuracies (`PillarScoreAggregator.pillarPercentage`)
- **Challenge percentage** = arithmetic mean of class, MMD, and testcase pillar percentages
- **Lab percentage** = mean across all rubric challenges; missing challenges count as 0%
- **Operational testcases** pass only when every assertion passes (binary 0/1 per testcase weight)
- Challenges with zero testcase rows score 0% on the testcase pillar
- Compile errors short-circuit testcase grading: all testcases for that challenge → `ERROR` before invoke

### Operational testcase grading

- Rubric tables: `testcase`, `testcase_invocation` (optional `receiver_constructor_id` + `receiver_params` for METHOD invocations), `testcase_instance`, `testcase_assertion`
- SINGLE_INVOCATION: one invocation + one or more assertions; instance methods may seed the receiver via constructor params instead of a no-arg constructor
- COMPARISON: two `testcase_instance` rows + COMPARISON_RESULT assertion
- Timeout: `app.grading.testcase-invoke-timeout-seconds` (default 5); invocations run on single-thread `testcaseInvokeExecutor`
- Exception matching: exception class simple name only (not message)
- Value types v1: primitives, `String`, null, arrays of primitives

### Result persistence

| Entity | Stores |
|---|---|
| `SubmissionChallengeResult` | Per-challenge score (0–100) |
| `SubmissionFieldResult` / `Method` / `Constructor` / `Relation` | Element match outcomes |
| `SubmissionTestcaseResult` | Testcase rollup + primary `input_display` / `expected_display` / `actual_display` |
| `SubmissionTestcaseAssertionResult` | Per-assertion status, `actual_value` JSONB, feedback |

### Upload `lab_result` bundle

Keyed `challenge_<N>`. Each bundle contains `class`, `mmd`, `testcases` (operational I/O cards; hidden rows omit display strings), and `scores: { class, mmd, testcase, total }`. Revisit reads use `GET /api/labs/{labId}/challenges/{challengeId}/testcases` with the same payload shape.

## Work Guidance

- Parsed classes come from `ReflectionClassParser.parseClasses(classesDir)` only
- Do not grade source `.java` files directly; compilation must succeed first
- Relations are MMD-only; Java reflection does not grade relations
- Rubric writers must call `RubricCacheInvalidationSupport.invalidateLab(labId)` after mutations
- Operator-run SQL migrations live in `docs/sql/` (no Flyway)

## Verification

- `PillarScoreAggregatorTest`, `PartialCreditEvaluatorTest`, `TestcaseGraderTest`, `TestcaseResultMapperTest`, `InvocationRunnerTest`, `GradingServiceTest`
- Manual: upload lab folder; confirm populated `testcases` in `lab_result` and on revisit `/testcases` endpoint

## Child DOX Index

No child docs. All grading code lives in this package.
