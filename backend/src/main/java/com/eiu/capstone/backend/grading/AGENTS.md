# Grading Engine

## Purpose

Compare compiled student Java classes against a database rubric using reflection. Produce per-element results and an overall lab score.

## Ownership

| File | Role |
|---|---|
| `GradingService.java` | Orchestrator: map challenge folders → DB challenges, grade, persist results, compute score |
| `ReflectionClassParser.java` | Load `.class` files via `URLClassLoader`, extract structure into parsed DTOs |
| `ParsedClass.java` | Class name, scope, declaring type, abstract flag, members |
| `ParsedField.java` | name, dataType, scope |
| `ParsedMethod.java` | name, returnType, scope, static/abstract/final, parameter types |
| `ParsedConstructor.java` | scope, parameter types |

## Local Contracts

### Pipeline position

```
SubmissionController
  → SubmissionStorageService.processUpload()   // save + compile
  → GradingService.gradeSubmission()
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

Each expected element is matched against parsed student classes using SHA-256 hashes of attribute tuples:

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

## Work Guidance

- `GradingService.gradeSubmission()` is `@Transactional`
- Parsed classes come from `ReflectionClassParser.parseClasses(classesDir)` — only `.class` files in the challenge's `classes/` subfolder
- Do not grade source `.java` files directly; compilation must succeed first
- Inheritance and class relations are out of scope until explicitly implemented

## Verification

- No unit tests; verify via submission upload with known rubric and inspect result tables or API response score

## Child DOX Index

No child docs. All grading code lives in this package.
