# Grading Engine

## Purpose

Compare compiled student Java classes against a database rubric using reflection. Produce per-element results and an overall lab score.

## Ownership

| File | Role |
|---|---|
| `GradingService.java` | Orchestrator: grade against rubric snapshot, compute score |
| `GradingResultStore.java` | Short read/write transactions for prior and new submission results |
| `grading/rubric/LabRubricService.java` | Load full lab rubric in batched DB queries |
| `grading/rubric/LabRubricCache.java` | In-process TTL cache keyed by lab ID |
| `grading/rubric/LabRubricSnapshot.java` | Immutable rubric graph for grading |
| `ReflectionClassParser.java` | Load `.class` files via `URLClassLoader`, extract structure into parsed DTOs |
| `ParsedClass.java` | Class name, scope, declaring type, abstract flag, members |
| `ParsedField.java` | name, dataType, scope |
| `ParsedMethod.java` | name, returnType, scope, static/abstract/final, parameter types |
| `ParsedConstructor.java` | scope, parameter types |

## Local Contracts

### Pipeline position

```
SubmissionController
  → LabRubricCache.get(lab)              // cached rubric snapshot
  → SubmissionStorageService.processUpload()   // parallel per-challenge compile
  → GradingService.gradeSubmission(snapshot)
  → MmdPersistenceHook.onUploadComplete()      // no-op until MMD archival
  → SubmissionStorageService.deleteFolder()    // cleanup (finally block)
```

### Challenge folder mapping

- Upload folders named `challenge_<N>` (regex: `challenge_(\d+)`)
- Each folder maps to a `Challenge` row for the submitted `Lab`
- Unmapped or missing challenges are skipped

### Rubric source (database)

```
Lab → Challenge → ClassEntity → Field / Method / Constructor
```

Attribute metadata resolved via `MasterData` and `*Declaration` entities (scopes, types).

`ClassRelation` entity exists but is **not graded yet**.

### Comparison method

Each expected element is matched against parsed student classes using **direct attribute comparison** (case-insensitive) on the same tuples previously hashed:

- **Class**: scope + declaring type + abstract flag
- **Field**: scope + data type (lookup by name)
- **Method**: name + param types first, then scope + return type + static/abstract/final
- **Constructor**: param types first, then scope + default-constructor heuristic (`parameterTypes.isEmpty()`)

### Scoring

- Each expected class, field, method, constructor counts as one element
- Challenge percentage = `correctElements / totalElements * 100`
- Overall score = simple average across graded challenges (unmapped folders excluded)

### Result persistence

| Entity | Stores |
|---|---|
| `SubmissionChallengeResult` | Per-challenge score |
| `SubmissionFieldResult` | Field match outcome |
| `SubmissionMethodResult` | Method match outcome |
| `SubmissionConstructorResult` | Constructor match outcome |

Each upload upserts result rows keyed by `(submission_id, element_id)` via `GradingResultStore.loadExisting` + `saveAll`.

### Rubric cache invalidation

- `LabRubricCache.invalidate(labId)` drops the in-process snapshot; TTL is the fallback.
- Rubric writers (future admin APIs) must call `RubricCacheInvalidationSupport.invalidateLab(labId)` after mutations.

## Work Guidance

- `GradingService.gradeSubmission()` loads and saves via `GradingResultStore`; CPU work runs outside those transactions
- Parsed classes come from `ReflectionClassParser.parseClasses(classesDir)` — only `.class` files in the challenge's `classes/` subfolder
- Do not grade source `.java` files directly; compilation must succeed first
- Inheritance and class relations are out of scope until explicitly implemented

## Verification

- No unit tests; verify via submission upload with known rubric and inspect result tables or API response score

## Child DOX Index

No child docs. All grading code lives in this package.
