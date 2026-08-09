# Grading Engine

## Purpose

Grade lab submissions across three equal pillars per challenge: Java `.class` reflection, MMD diagram comparison, and structural testcase checks. Produce per-element results, pillar scores, and an upload-time `lab_result` bundle for the student UI.

## Ownership

| File | Role |
|---|---|
| `GradingService.java` | Thin orchestrator: parallel per-challenge grading, persistence, `lab_result` assembly |
| `grading/pipeline/GradingPipeline.java` | Staged pipeline: class pillar, then parallel MMD + testcase pillars |
| `grading/pipeline/ClassReflectionGrader.java` | `.class` pillar with partial credit on declarations |
| `grading/pipeline/MmdPillarGrader.java` | MMD pillar |
| `grading/pipeline/TestcaseGrader.java` | Structural testcase pillar (EXISTENCE / DECLARATION) |
| `grading/scoring/PillarScoreAggregator.java` | Pillar, challenge (mean of 3 pillars), and lab percentages |
| `grading/scoring/PartialCreditEvaluator.java` | Per-attribute accuracy for DECLARATION checks |
| `grading/LabResultAssembler.java` | Build `lab_result.challenge_<N>` bundles for upload response (batched rubric load + in-memory correct IDs) |
| `GradingResultStore.java` | Short read/write transactions for submission result tables |
| `grading/rubric/LabRubricService.java` | Load full lab rubric (including testcases) in batched DB queries |
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
          → MmdPillarGrader + TestcaseGrader (parallel)
      → GradingResultStore.save()
      → LabResultAssembler.assemble() → upload response lab_result
  → MmdPersistenceHook.onUploadComplete()
  → SubmissionStorageService.deleteFolder() (finally)
```

### Scoring

- **Pillar percentage** = weighted mean of member accuracies (`PillarScoreAggregator.pillarPercentage`)
- **Challenge percentage** = arithmetic mean of class, MMD, and testcase pillar percentages
- **Lab percentage** = mean across all rubric challenges; missing challenges count as 0%
- **EXISTENCE** testcase checks are all-or-nothing; **DECLARATION** checks use per-attribute partial credit
- Challenges with zero testcase rows score 0% on the testcase pillar

### Result persistence

| Entity | Stores |
|---|---|
| `SubmissionChallengeResult` | Per-challenge score (0–100) |
| `SubmissionFieldResult` / `Method` / `Constructor` / `Relation` | Element match outcomes |
| `SubmissionTestcaseResult` | Structural testcase status + feedback |

### Upload `lab_result` bundle

Keyed `challenge_<N>`. Each bundle contains `class`, `mmd`, `testcases`, and `scores: { class, mmd, testcase, total }`. Returned on upload so the student UI does not need follow-up `/class` or `/mmd` fetches for fresh submissions.

## Work Guidance

- Parsed classes come from `ReflectionClassParser.parseClasses(classesDir)` only
- Do not grade source `.java` files directly; compilation must succeed first
- Relations are MMD-only; Java reflection does not grade relations
- Rubric writers must call `RubricCacheInvalidationSupport.invalidateLab(labId)` after mutations

## Verification

- `PillarScoreAggregatorTest`, `PartialCreditEvaluatorTest`, `TestcaseGraderTest`, `GradingServiceTest`
- Manual: upload lab folder; confirm `lab_result` in response and student tabs render without extra API calls

## Child DOX Index

No child docs. All grading code lives in this package.
