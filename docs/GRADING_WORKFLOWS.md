# Grading Workflows — Line-by-Line Reference

This document describes every step of the OOP AutoGrader grading pipeline: how student uploads become scores across the three grading pillars (**Java / class reflection**, **MMD diagram**, and **structural testcases**). Each section traces the actual Java source files and explains what each significant line or block does.

**Package root:** `backend/src/main/java/com/eiu/capstone/backend/grading/`

---

## Table of Contents

1. [High-Level Architecture](#1-high-level-architecture)
2. [Entry Point: Upload Request](#2-entry-point-upload-request)
3. [Phase A: Rubric Load](#3-phase-a-rubric-load)
4. [Phase B: Upload Processing & Java Compilation](#4-phase-b-upload-processing--java-compilation)
5. [Phase C: Grading Orchestration](#5-phase-c-grading-orchestration)
6. [Phase D: Per-Challenge Pipeline](#6-phase-d-per-challenge-pipeline)
7. [Pillar 1 — Java (Class Reflection) Grading](#7-pillar-1--java-class-reflection-grading)
8. [Pillar 2 — MMD Diagram Grading](#8-pillar-2--mmd-diagram-grading)
9. [Pillar 3 — Structural Testcase Grading](#9-pillar-3--structural-testcase-grading)
10. [Scoring Model](#10-scoring-model)
11. [Persistence & Side Effects](#11-persistence--side-effects)
12. [Configuration & Thread Pools](#12-configuration--thread-pools)
13. [File Map](#13-file-map)

---

## 1. High-Level Architecture

Each **challenge** in a lab is graded on **three equal pillars**:

| Pillar | Input | Grader class | What is compared |
|--------|-------|--------------|------------------|
| **Class (Java)** | Compiled `.class` files | `ClassReflectionGrader` | Rubric classes, fields, methods, constructors via reflection |
| **MMD** | Uploaded `.mmd` bytes | `MmdPillarGrader` → `MmdParser` + `MmdComparisonService` | Same rubric elements plus UML relations |
| **Testcase** | Compiled `.class` files + rubric testcase rows | `TestcaseGrader` | Targeted EXISTENCE or DECLARATION checks |

**Challenge score** = arithmetic mean of the three pillar percentages.

**Lab score** = arithmetic mean across all rubric challenges (missing challenges count as 0%).

```
POST /api/submissions/{labId}/{attemptNumber}/upload
  │
  ├─ LabRubricCache.get(lab)                    ← load rubric from DB (cached)
  ├─ SubmissionStorageService.processUpload()     ← validate paths, compile .java per challenge
  ├─ GradingService.gradeSubmission()
  │    ├─ [parallel per challenge on gradingExecutor]
  │    │    └─ GradingPipeline.gradeChallenge()
  │    │         ├─ ReflectionClassParser.parseClasses()   ← load .class via URLClassLoader
  │    │         ├─ ClassReflectionGrader.grade()            ← sync
  │    │         ├─ MmdPillarGrader.grade()                  ← async on pillarExecutor
  │    │         └─ TestcaseGrader.grade()                   ← async on pillarExecutor
  │    ├─ GradingResultStore.save()             ← PostgreSQL
  │    └─ LabResultAssembler.assemble()         ← upload response bundle
  ├─ compileErrorStore.save() / mmdMetaStore.save()
  ├─ MmdPersistenceHook.onUploadComplete()      ← no-op by default
  └─ SubmissionStorageService.deleteFolder()    ← finally: wipe temp files
```

---

## 2. Entry Point: Upload Request

**File:** `controller/SubmissionController.java`

### 2.1 Authentication & setup (lines 126–142)

```java
@PostMapping("/{labId}/{attemptNumber}/upload")
public ResponseEntity<SubmissionUploadResponse> upload(...)
```

1. **`resolveStudentUser(authHeader)`** — Parses JWT Bearer token via `JwtService.parseToken()`. Requires a non-blank `irn` claim (students only; lecturers cannot submit).
2. **`labRepository.findById(labId)`** — Loads the lab; 404 if missing.
3. **`requestId = UUID.randomUUID()`** — Unique folder name to prevent upload collisions under the same IRN.
4. **`submissionFolderToDelete = null`** — Tracked so the `finally` block can always clean up temp storage.

### 2.2 Rubric load (lines 144–146)

```java
LabRubricSnapshot rubric = labRubricCache.get(lab);
```

Loads the full immutable rubric graph (challenges → classes → fields/methods/constructors → relations → testcases) from PostgreSQL, with in-process TTL caching (`app.grading.rubric-cache-ttl-minutes`, default 30).

### 2.3 Upload processing (lines 148–152)

```java
SubmissionStorageService.ProcessResult uploadResult =
    submissionStorageService.processUpload(irn, requestId, files);
submissionFolderToDelete = uploadResult.submissionFolder;
```

Validates folder structure, groups files by challenge, compiles Java in parallel. Returns challenge folders, MMD file lists, and compile metadata. See [Phase B](#4-phase-b-upload-processing--java-compilation).

### 2.4 Submission record (lines 154–162)

```java
var existingSubmission = labSubmissionRepository
    .findByUserAndLabAndAttemptNumber(userAccount, lab, attemptNumber);
boolean isNewSubmission = existingSubmission.isEmpty();
LabSubmission submission = existingSubmission.orElseGet(LabSubmission::new);
submission.setScore(BigDecimal.ZERO);
submission = labSubmissionRepository.save(submission);
```

Creates or reuses a `lab_submission` row for this user/lab/attempt. `isNewSubmission` controls whether prior element results are loaded for upsert (re-upload of same attempt reuses existing result rows).

### 2.5 Grading (lines 164–167)

```java
GradingOutcome gradingOutcome = gradingService.gradeSubmission(
    submission, rubric, uploadResult.challenges, uploadResult.mmdByChallenge, isNewSubmission);
```

Main grading entry. See [Phase C](#5-phase-c-grading-orchestration).

### 2.6 Post-grade persistence (lines 169–183)

- **`submission.setScore(gradingOutcome.overallScore())`** — Lab-level percentage saved to `lab_submission.score`.
- **`updateStudentProgress(...)`** — Updates `student_lab_progress` (attempts count, highest score, timestamps).
- **`compileErrorStore.save(...)`** — Writes per-challenge compile errors to `{SUBMISSION_BASE_DIR}/_compile_errors/{submissionId}.json`.
- **`submissionMmdMetaStore.save(...)`** — Writes MMD metadata to `_mmd_meta/{submissionId}.json`.
- **`labStatisticsCache.invalidate(labId)`** — Clears lecturer analytics cache for this lab.
- **`mmdPersistenceHook.onUploadComplete(...)`** — Extension point for archiving `.mmd` files (default no-op).

### 2.7 Response & cleanup (lines 185–213)

Returns `SubmissionUploadResponse` with challenge score map and `lab_result` bundle. The `finally` block calls `submissionStorageService.deleteFolder(submissionFolderToDelete)` — all compiled classes and temp folders are deleted after grading.

---

## 3. Phase A: Rubric Load

**Files:** `grading/rubric/LabRubricCache.java`, `grading/rubric/LabRubricService.java`, `grading/rubric/LabRubricSnapshot.java`

`LabRubricService.loadForLab(Lab)` performs batched DB queries:

1. `challengeRepository.findByLabOrderByChallengeNumberAsc(lab)` — all challenges.
2. `classEntityRepository.findByChallengeInWithAttributes(challenges)` — classes with scope/type attributes.
3. `fieldRepository.findByClassEntityInWithDeclaration(...)` — fields with declarations.
4. `methodRepository.findByClassEntityInWithDeclaration(...)` — methods with declarations.
5. `constructorRepository.findByClassEntityInWithDeclaration(...)` — constructors.
6. `parameterRepository.findByMethodIn(...)` / `findByConstructorEntityIn(...)` — parameter type lists.
7. `classRelationRepository.findByChallengeIn(...)` — UML relations.
8. `testcaseRepository.findByChallengeInOrderByOrderIndexAsc(...)` — structural testcase rows.

The result is an immutable `LabRubricSnapshot` keyed by challenge number, used read-only throughout grading. Rubric mutations must call `RubricCacheInvalidationSupport.invalidateLab(labId)`.

---

## 4. Phase B: Upload Processing & Java Compilation

**File:** `service/SubmissionStorageService.java`

This phase runs **before** grading. It does not score anything; it validates uploads and produces compiled `.class` files.

### 4.1 `processUpload(irn, requestId, files)` (lines 97–126)

| Step | Code | Behavior |
|------|------|----------|
| Sanitize IRN | `sanitize(irn)` | NFC normalize, lowercase, replace spaces/special chars → `_` |
| Create folder | `Path.of(baseDir, irnFolderName, requestId)` | e.g. `submissions/john_doe/a1b2c3.../` |
| Group files | `validateAndGroup(files)` | Split into `javaByChallenge` and `mmdByChallenge` maps |
| Parallel compile | `CompletableFuture.supplyAsync(() -> processChallenge(...), compileExecutor)` | One future per challenge key |
| Return | `new ProcessResult(submissionFolder, results, mmdByChallenge, compileWallMs)` | |

### 4.2 `validateAndGroup(files)` (lines 128–166)

For each `MultipartFile`:

1. **`isValidSubmissionPath(originalName)`** — Path must match:
   - Root: `IRN_StudentName_lab_N` (regex `^(\d+)_([a-z0-9_\s]+)_lab_(\d+)$`)
   - Intermediate segments: `challenge_1`, `challenge-2`, etc.
   - Leaf: `.java` or `.mmd` only
2. All files must share the same root folder name.
3. **`extractChallengeKey(path)`** — Finds `challenge[_-]?(\d+)` in path → normalized key `challenge_N`.
4. Routes `.mmd` → `mmdByChallenge`, everything else → `javaByChallenge`.

Invalid structure throws `SubmissionProcessingException` (HTTP 422).

### 4.3 `processChallenge(submissionFolder, challengeName, files)` (lines 168–250)

Per challenge folder:

1. **Create `challengeFolder/classes/`** directories.
2. **If no Java files** → return `ChallengeResult(challengeName, folder, 0)` (zero class files; grading will score 0% on class pillar).
3. **Build in-memory sources** (lines 197–220):
   - For each `.java` file: `challengeRelativeJavaPath()` strips path up to challenge folder.
   - Duplicate source paths within a challenge → compile error.
   - `new MemorySourceJavaFileObject(sourcePath, file.getBytes())` — sources never written to disk.
4. **Compile** (lines 228–235):
   ```java
   javaCompilerService.compileSources(sources, classesFolder);
   ```
   On failure → `failedChallenge(...)` returns `ChallengeResult` with `compileError` message; cleans up `classes/` folder.
5. **Count `.class` files** in `classes/` → `classFileCount`.

### 4.4 `JavaCompilerService.compileSources()` (lines 35–72)

**File:** `service/JavaCompilerService.java`

| Line | Action |
|------|--------|
| `compiler = ToolProvider.getSystemJavaCompiler()` | Requires JDK (not JRE); fails at startup if unavailable |
| `options = ["-d", outputDir, "-encoding", "UTF-8"]` | Output compiled classes to challenge's `classes/` |
| `compiler.getTask(..., sources)` | Compiles in-memory `JavaFileObject` list |
| `task.call()` | Returns `false` on compile failure |
| On failure | Throws `SubmissionProcessingException("Compilation failed:\n" + diagnostics)` |

**Important:** Grading never reads `.java` source files. All Java grading uses compiled `.class` output from this step.

### 4.5 MMD files during upload

`.mmd` files are grouped into `mmdByChallenge` but **not written to disk** on the hot path. They remain in memory as `MultipartFile` objects passed directly to `MmdPillarGrader`.

---

## 5. Phase C: Grading Orchestration

**File:** `grading/GradingService.java`

### 5.1 `gradeSubmission()` (lines 99–123)

```java
public GradingOutcome gradeSubmission(LabSubmission submission,
                                      LabRubricSnapshot rubric,
                                      List<ChallengeResult> challengeFolderResults,
                                      Map<String, List<MultipartFile>> mmdByChallenge,
                                      boolean skipExistingLoad)
```

| Step | Method | Purpose |
|------|--------|---------|
| 1 | `loadExisting(submission)` or `emptyExistingResults()` | Re-upload reuses existing DB result entity IDs |
| 2 | `computeAgainstSnapshot(...)` | Parallel per-challenge grading |
| 3 | `gradingResultStore.save(computed)` | Persist all element results |
| 4 | `parsedSubmissionSnapshotStore.save(...)` | Save display snapshots for Class/MMD tabs |
| 5 | `labResultAssembler.assemble(...)` | Build `lab_result` response map |
| 6 | `return new GradingOutcome(...)` | Overall score + challenge summaries + MMD meta + lab_result |

### 5.2 `computeAgainstSnapshot()` (lines 136–235)

**Parallel challenge grading** (lines 143–150):

```java
List<CompletableFuture<ChallengeComputation>> futures = challengeFolderResults.stream()
    .map(folderResult -> CompletableFuture.supplyAsync(
        () -> gradeChallengeFolder(rubric, folderResult,
            mmdByChallenge.getOrDefault(folderResult.challengeName, List.of())),
        gradingExecutor))
    .collect(Collectors.toList());
List<ChallengeComputation> challengeComputations = CompletableFutures.joinAll(futures);
```

Each challenge folder is graded on `gradingExecutor` (default parallelism 4, capped at CPU count).

**Result aggregation** (lines 167–222): For each `ChallengeComputation`, merges:
- Field/method/constructor/relation/testcase pending results → JPA entities
- Challenge score row
- Pillar breakdown (`PillarScoreBreakdown`)
- MMD metadata
- Parsed submission snapshot

**Lab score** (lines 224–233):

```java
for (ChallengeRubric challengeRubric : rubric.byChallengeNumber().values().stream()
        .sorted(Comparator.comparingInt(ChallengeRubric::challengeNumber))
        .toList()) {
    BigDecimal challengeScore = percentagesByChallengeNumber.getOrDefault(
        challengeRubric.challengeNumber(), BigDecimal.ZERO);
    overallChallengeScores.add(challengeScore);
}
result.overallScore = PillarScoreAggregator.labPercentage(overallChallengeScores);
```

Challenges with no uploaded folder score **0%** (not skipped).

### 5.3 `gradeChallengeFolder()` (lines 237–283)

Delegates to `gradingPipeline.gradeChallenge()`, then maps pipeline output into `ChallengeComputation`:

- Copies class pillar field/method/constructor results
- Copies MMD relation results
- Copies testcase results
- Builds MMD metadata via `buildMmdMeta()` (class presence, relation error labels)
- Builds parsed snapshot via `ParsedSubmissionSnapshotBuilder.build()`

---

## 6. Phase D: Per-Challenge Pipeline

**File:** `grading/pipeline/GradingPipeline.java`

### 6.1 `gradeChallenge(rubric, folderResult, mmdFiles)` (lines 45–96)

```
Step 1: extractChallengeNumber("challenge_N") → N
Step 2: rubric.challenge(N) → ChallengeRubric (null → return null)
Step 3: classesDir = folderResult.folder.resolve("classes")
Step 4: reflectionClassParser.parseClasses(classesDir) → List<ParsedClass>
Step 5: ChallengeGradingContext.of(rubric, classesDir, compileError, parsedClasses)
Step 6: classReflectionGrader.grade(context)          ← SYNCHRONOUS
Step 7: mmdPillarGrader.grade(rubric, mmdFiles)       ← ASYNC on pillarExecutor
Step 8: testcaseGrader.grade(context)                 ← ASYNC on pillarExecutor
Step 9: CompletableFuture.allOf(mmdFuture, testcaseFuture).join()
Step 10: PillarScoreAggregator.challengePercentage(class, mmd, testcase)
Step 11: fullyCorrect = all three pillars == 100%
Step 12: return ChallengePipelineResult(...)
```

**Threading note:** MMD and testcase pillars run in parallel on `pillarExecutor` (separate from `gradingExecutor`) to avoid deadlock when challenge workers block waiting for pillar tasks on a small pool (e.g. Render free tier with 1–2 CPUs).

### 6.2 `ChallengeGradingContext` (record)

**File:** `grading/pipeline/ChallengeGradingContext.java`

| Field | Source | Used by |
|-------|--------|---------|
| `challengeRubric` | Rubric snapshot | All graders |
| `classesDir` | `{submission}/challenge_N/classes/` | Reflection parser |
| `compileError` | From `ChallengeResult.compileError` | TestcaseGrader (early exit) |
| `parsedClasses` | Reflection output | Class + Testcase graders |
| `parsedByName` | Map `simpleName → ParsedClass` | Lookup by rubric class name |
| `failedClassNames` | Currently always empty set | Reserved |

---

## 7. Pillar 1 — Java (Class Reflection) Grading

**Files:** `grading/pipeline/ClassReflectionGrader.java`, `grading/ReflectionClassParser.java`

### 7.1 Reflection parsing — `ReflectionClassParser.parseClasses(classesDir)`

**Lines 23–56:** List all `.class` files in `classesDir`, excluding inner classes (`$` in filename).

**Lines 39–54:** Create `URLClassLoader` pointing at `classesDir`, load each class by simple name:

```java
Class<?> clazz = Class.forName(className, false, loader);
result.add(parseClass(clazz));
```

`ClassNotFoundException` / `LinkageError` → logged as warning, class treated as missing.

### 7.2 `parseClass(Class<?> clazz)` — per-class extraction (lines 58–108)

| Attribute | Reflection API | `ParsedClass` field |
|-----------|---------------|---------------------|
| Name | `clazz.getSimpleName()` | `simpleName` |
| Scope | `Modifier.isPublic/Private/Protected` | `scope` → `"public"` / `"private"` / `"protected"` / `"default"` |
| Declaring type | `isInterface()`, `isEnum()`, `isRecord()` | `declaringType` → `"interface"` / `"enum"` / `"record"` / `"class"` |
| Abstract | `Modifier.isAbstract() && !isInterface()` | `isAbstract` |

**Fields** (lines 67–76): `clazz.getDeclaredFields()`, skip synthetic. Each → `ParsedField { name, dataType, scope }`. Types via `simpleGenericName()` (handles generics like `List<String>`).

**Methods** (lines 78–93): `getDeclaredMethods()`, skip synthetic/bridge. Each → `ParsedMethod { name, returnType, scope, isStatic, isAbstract, isFinal, parameterTypes }`.

**Constructors** (lines 95–105): `getDeclaredConstructors()`, skip synthetic. Each → `ParsedConstructor { scope, parameterTypes }`.

### 7.3 Class pillar grading — `ClassReflectionGrader.grade(context)`

Iterates every `ClassRubric` in the challenge rubric:

#### 7.3.1 Missing class (lines 40–54)

If `parsedByName.get(expectedClass.name())` is null:
- Class shell weight → accuracy 0
- Every expected field, method, constructor → accuracy 0, `correct = false`
- `continue` to next rubric class

#### 7.3.2 Class shell partial credit (lines 57–61)

```java
double classAccuracy = PartialCreditEvaluator.accuracy(List.of(
    PartialCreditEvaluator.matches(expectedClass.scope(), parsed.scope).get(0),
    PartialCreditEvaluator.matches(expectedClass.declaringType(), parsed.declaringType).get(0),
    expectedClass.isAbstract() == parsed.isAbstract));
weighted.add(new WeightedAccuracy(classWeight, classAccuracy));
```

3 attributes checked; accuracy = matching / 3 (e.g. 2/3 correct → 66.7% for class shell).

#### 7.3.3 Fields (lines 63–78)

For each `FieldRubric`:
- Lookup `parsedFields.get(expectedField.name())`
- If missing → accuracy 0
- If present → accuracy from 3 checks: existence (always true if found), scope match, dataType match
- `correct = (accuracy >= 1.0)` — stored as boolean in DB (partial credit affects pillar % only)

#### 7.3.4 Methods (lines 80–91)

`findMatchingMethod(parsed.methods, name, parameterTypes)` — name + parameter type list must match (case-insensitive per type).

If found, accuracy from 6 checks: existence, scope, returnType, isStatic, isAbstract, isFinal.

#### 7.3.5 Constructors (lines 93–102)

`findMatchingConstructor(parsed.constructors, parameterTypes)` — match by parameter types only.

Accuracy from 3 checks: existence, scope, `isDefault` (rubric) vs `parameterTypes.isEmpty()` (actual).

#### 7.3.6 Pillar percentage (line 105)

```java
BigDecimal pillarPct = PillarScoreAggregator.pillarPercentage(weighted);
```

Weighted mean: `sum(weight × accuracy) / sum(weight) × 100`. All members use `MemberWeightCalculator.defaultMemberWeight()` = 1.

**Relations are NOT graded in the Java pillar.** Relations exist only in MMD.

---

## 8. Pillar 2 — MMD Diagram Grading

**Files:** `grading/pipeline/MmdPillarGrader.java`, `grading/MmdParser.java`, `grading/MmdComparisonService.java`, `grading/MmdTypeEquivalence.java`, `grading/MmdGradingOutcome.java`

### 8.1 `MmdPillarGrader.grade(challengeRubric, mmdFiles)` (lines 34–89)

#### Step 1: Read MMD bytes (lines 38–51)

```java
byte[] content = readFirstMmd(mmdFiles);
boolean mmdSubmitted = content != null && content.length > 0;
```

`readFirstMmd()` sorts files by filename (case-insensitive), takes first, calls `file.getBytes()`.

| Condition | Outcome |
|-----------|---------|
| No MMD submitted | `MmdGradingOutcome.allIncorrect(...)` — everything false |
| Parse throws `MmdParseException` | Same — all incorrect |
| Parse succeeds | `mmdComparisonService.compare(rubric, diagram)` |

#### Step 2: Score rubric elements (lines 53–81)

For each rubric class:
- **Class shell:** `accuracy = mean(present, typeCorrect)` — 2 binary checks
- **Fields/methods/constructors:** `binaryAccuracy(outcome.isXxxCorrect(id))` — all-or-nothing per element

For each rubric relation:
- `binaryAccuracy(outcome.isRelationCorrect(id))`

#### Step 3: Pillar percentage (line 84)

```java
PillarScoreAggregator.pillarPercentage(weighted)
```

MMD uses **binary** (all-or-nothing) scoring per field/method/constructor/relation, but **partial credit** on class presence vs type (2-attribute mean).

---

### 8.2 MMD Parsing — `MmdParser`

#### `parseBytes(content)` (lines 24–30)

Converts bytes to UTF-8 string, delegates to `parse(text)`.

#### `parse(text)` — line-by-line state machine (lines 39–91)

State variables:
- `current` — `ParsedMmdClass` being built
- `braceDepth` — tracks `{` / `}` nesting

**Per line** (after trim, skip empty and `%%` comments):

| Pattern | Action |
|---------|--------|
| `^class\s+(\w+)\s*\{\s*$` | Start new class block; `braceDepth = 1` |
| Inside class block, `}` | Decrement depth; if 0, close class |
| Inside class block, contains `{` | Throw `MmdParseException` (no nested braces) |
| Inside class block, other | `parseClassBodyLine(current, line)` |
| Relation arrow line | `matchRelation(line)` → `parseRelation(...)` |

Unclosed block at EOF → `MmdParseException`.

#### `parseClassBodyLine(current, line)` (lines 93–167)

| Line pattern | Parsed as |
|--------------|-----------|
| `<< enumerate >>` / `<< interface >>` | `stereotypeType` = `"Enumerate"` / `"Interface"` |
| `-getter()` / `+getter()` etc. | Shorthand method `__getter_shorthand__` |
| `-setter()` | Shorthand method `__setter_shorthand__` |
| `scope name(params) returnType` | Method (or constructor if `name == className`) |
| `scope name: type` | Field (`name:type` syntax) |
| `scope type name` | Field (Mermaid-style, e.g. `-int yearModel`) |

**Scope symbols:** `-` → private, `+` → public, `#` → protected.

**Parameter parsing** (`parseParameterTypes`):
- Supports `name: type` and `type name` formats
- Splits on commas respecting generic `<>` depth

#### Relation arrows (lines 19–22, 241–298)

Supported arrows (longest match first):
```
..|>, <|.., *--, --*, o--, --o, <-->, <|--, --|>,
..>, <.., -->, <--, --
```

`canonicalRelationType(arrow)` maps to:
| Arrow(s) | Canonical type |
|----------|----------------|
| `<|--`, `--|>` | `inheritance` |
| `*--`, `--*` | `composition` |
| `o--`, `--o` | `aggregation` |
| `-->`, `<--` | `association` |
| `<-->` | `bidirectional_association` |
| `--` | `link` |
| `..>`, `<..` | `dependency` |
| `..|>`, `<|..` | `realization` |

`parseRelation()` determines source/target based on arrow direction (symbol on left vs right).

---

### 8.3 MMD Comparison — `MmdComparisonService.compare(rubric, diagram)`

#### Per rubric class (lines 30–81)

1. **`parsedByName.get(expectedClass.name())`** — lookup by exact class name.
2. **`setClassPresent(id, parsed != null)`**
3. **`setClass(id, present && classTypeMatches(...))`** — stereotype must match rubric `declaringType`:
   - Expected normalized to uppercase (`CLASS`, `INTERFACE`, `ENUM`, `RECORD`)
   - Actual from `stereotypeType` or defaults to `CLASS`
   - `ENUMERATE` → `ENUM`

4. **If class missing** → all fields/methods/constructors marked incorrect.

5. **Getter/setter shorthand** (lines 42–67):
   - If diagram has `getter()` shorthand AND rubric has getter methods → all getters marked correct
   - Same for setters

6. **Fields** (lines 49–58): Match by name (case-insensitive). Correct if scope matches AND `MmdTypeEquivalence.typesMatch(expectedType, actualType)`.

7. **Methods** (lines 60–72): `findMatchingMethod` by name + parameter types. Correct if scope, returnType, and parameter types all match.

8. **Constructors** (lines 74–80): Match by parameter types. Correct if scope and parameter types match.

#### Per rubric relation (lines 83–87)

```java
boolean correct = diagram.relations.stream().anyMatch(parsed ->
    relationMatches(expectedRelation, parsed));
```

`relationMatches()`:
1. Relation type must match (`normalizeRelationTypeName` handles synonyms like "inheritance", "generalization", "extends").
2. Source/target class names must match in forward direction.
3. For `link` type only: reverse direction also accepted.

`relationPresentInDiagram()` (used for error labels) checks class connectivity regardless of relation type.

---

### 8.4 Type equivalence — `MmdTypeEquivalence`

`typesMatch(expected, actual)`:
1. Trim both sides
2. `convertTildes()` — `List~String~` → `List<String>` (MMD tilde generics)
3. `canonicalizeCollection()`:
   - `ArrayList<T>` / `LinkedList<T>` → `List<T>`
   - `HashMap<K,V>` — normalizes primitive wrapper types in value position (`int` → `Integer`)
4. Case-sensitive equality on normalized strings

---

## 9. Pillar 3 — Structural Testcase Grading

**File:** `grading/pipeline/TestcaseGrader.java`

Testcases are **structural checks** defined in the `testcase` DB table per challenge. They are NOT JUnit tests — no code execution, no assertions on runtime behavior.

### 9.1 Testcase rubric fields

| DB column | Enum | Values |
|-----------|------|--------|
| `check_type` | `TestcaseCheckType` | `EXISTENCE`, `DECLARATION` |
| `target_type` | `TestcaseTargetType` | `CLASS`, `FIELD`, `METHOD`, `CONSTRUCTOR` |
| `target_id` | UUID | Points to rubric class/field/method/constructor row |
| `weight` | int | Pillar weight (default 1, min 1) |

### 9.2 `TestcaseGrader.grade(context)` (lines 33–52)

For each `TestcaseRubric` in challenge:
1. `weight = MemberWeightCalculator.testcaseWeight(testcase.weight())`
2. `evaluation = evaluate(testcase, rubric, context)`
3. Add `WeightedAccuracy(weight, evaluation.accuracy())` to pillar list
4. Add `PendingTestcaseResult(id, status, feedback)`

**Empty testcase list** → pillar percentage = `BigDecimal.ZERO` (not 100%).

### 9.3 `evaluate()` — common pre-check (lines 54–67)

```java
if (context.compileError() != null && !context.compileError().isBlank()) {
    return new Evaluation(0, TestcaseResultStatus.ERROR, "Compilation error: " + context.compileError());
}
```

Any compile failure → all testcases for that challenge get `ERROR` status with 0% accuracy.

Then dispatches by `testcase.targetType()`:
- `CLASS` → `evaluateClass()`
- `FIELD` → `evaluateField()`
- `METHOD` → `evaluateMethod()`
- `CONSTRUCTOR` → `evaluateConstructor()`

### 9.4 `evaluateClass()` (lines 69–89)

1. Resolve `ClassRubric` by `testcase.targetId()`.
2. Lookup `ParsedClass` by rubric class name.
3. If class not found → `FAILED`, "Class not found: {name}".
4. If `EXISTENCE` → `PASSED`, accuracy 1.0.
5. If `DECLARATION` → partial credit on scope, declaringType, isAbstract (same 3 checks as class pillar shell).

### 9.5 `evaluateField()` (lines 91–111)

1. `resolveField(targetId)` — walks rubric to find owning class + field.
2. Lookup parsed class; if missing → `ERROR`.
3. Find field by name in `parsed.fields`.
4. If field not found → `FAILED`.
5. If `EXISTENCE` → `PASSED`.
6. If `DECLARATION` → partial credit on scope + dataType (2 checks).

### 9.6 `evaluateMethod()` (lines 113–136)

1. `resolveMethod(targetId)`.
2. `findMatchingMethod` by name + parameter types.
3. If not found → `FAILED`.
4. If `EXISTENCE` → `PASSED`.
5. If `DECLARATION` → partial credit on scope, returnType, isStatic, isAbstract, isFinal (5 checks).

### 9.7 `evaluateConstructor()` (lines 138–159)

1. `resolveConstructor(targetId)`.
2. `findMatchingConstructor` by parameter types.
3. If not found → `FAILED`.
4. If `EXISTENCE` → `PASSED`.
5. If `DECLARATION` → partial credit on scope + isDefault (2 checks).

### 9.8 `toEvaluation(accuracy, label)` (lines 161–170)

| Accuracy | Status | Feedback |
|----------|--------|----------|
| ≥ 1.0 | `PASSED` | "{label} matches" |
| ≤ 0 | `FAILED` | "{label} mismatch" |
| between | `FAILED` | "{label} partial match (N%)" |

Partial credit on DECLARATION testcases affects pillar percentage but status remains `FAILED` unless 100%.

---

## 10. Scoring Model

**File:** `grading/scoring/PillarScoreAggregator.java`

### 10.1 Pillar percentage

```
pillarPercentage = (Σ weightᵢ × accuracyᵢ) / (Σ weightᵢ) × 100
```

- `accuracy` clamped to [0, 1]
- `weight` minimum 1
- Empty member list → 0%
- Scale: 2 decimal places, `HALF_UP`

### 10.2 Challenge percentage

```
challengePercentage = (classPct + mmdPct + testcasePct) / 3
```

Equal weight across pillars. One pillar at 0% pulls challenge score down significantly.

### 10.3 Lab percentage

```
labPercentage = sum(challengePcts) / challengeCount
```

Includes all rubric challenges. Challenges without an uploaded folder contribute 0%.

### 10.4 Partial credit evaluator

**File:** `grading/scoring/PartialCreditEvaluator.java`

```java
accuracy(attributeMatches) = count(true) / count(total)
```

String comparison: trim + lowercase (`normalize()`).

`binaryAccuracy(correct)` → 1.0 or 0.0 (used in MMD pillar for individual elements).

### 10.5 Correctness flags vs percentages

| Storage | Granularity |
|---------|-------------|
| Pillar % | Weighted mean with partial credit |
| `SubmissionFieldResult.correct` etc. | Boolean: `accuracy >= 1.0` only |
| `SubmissionChallengeResult.correct` | `fullyCorrect`: all three pillars == 100% |
| `SubmissionChallengeResult.score` | Challenge percentage (0–100) |

---

## 11. Persistence & Side Effects

### 11.1 `GradingResultStore`

**File:** `grading/GradingResultStore.java`

| Table / entity | Content |
|----------------|---------|
| `submission_field_result` | Per-field boolean correct |
| `submission_method_result` | Per-method boolean correct |
| `submission_constructor_result` | Per-constructor boolean correct |
| `submission_relation_result` | Per-relation boolean correct (MMD only) |
| `submission_challenge_result` | Per-challenge score + fullyCorrect flag |
| `submission_testcase_result` | Per-testcase status + feedback text |

Re-upload of same attempt: `loadExisting()` fetches prior rows by submission ID; `buildXxxResult()` reuses entity instances (upsert semantics).

### 11.2 Ephemeral JSON sidecars

| Path | Written by | Content |
|------|-----------|---------|
| `_compile_errors/{submissionId}.json` | `SubmissionCompileErrorStore` | Map challengeId → compile error message |
| `_mmd_meta/{submissionId}.json` | `SubmissionMmdMetaStore` | Per challenge: mmdSubmitted, classStereotypeCorrect, relationErrors |
| `_parsed_snapshot/{submissionId}.json` | `ParsedSubmissionSnapshotStore` | Student display text for Class/MMD tabs |

### 11.3 Upload response `lab_result`

**File:** `grading/LabResultAssembler.java`

Keyed `challenge_<N>`. Each bundle:
```json
{
  "class": { ... element details ... },
  "mmd": { ... element details ... },
  "testcases": [ ... ],
  "scores": { "class": 85.0, "mmd": 100.0, "testcase": 50.0, "total": 78.33 }
}
```

Allows student UI to render results immediately without follow-up API calls.

---

## 12. Configuration & Thread Pools

| Property | Default | Bean | Purpose |
|----------|---------|------|---------|
| `app.grading.parallelism` | 4 | `gradingExecutor` | Max concurrent challenge grading workers |
| `app.compile.parallelism` | 4 | `compileExecutor` | Max concurrent per-challenge compile workers |
| (derived) | `max(2, parallelism×2)` | `pillarExecutor` | MMD + testcase pillars inside each challenge |
| `app.grading.rubric-cache-ttl-minutes` | 30 | `LabRubricCache` | Rubric cache TTL |
| `app.grading.timing-log` | false | — | Log `grading_timing` and `compile_timing` to stdout |
| `app.storage.submission-base-dir` | `submissions/` | — | Temp upload root |

**Deadlock prevention:** `pillarExecutor` is intentionally separate from `gradingExecutor`. If they shared one pool, a challenge worker waiting for MMD+testcase futures could exhaust the pool (documented in `docs/solutions/architecture-patterns/grading-executor-deadlock-render.md`).

---

## 13. File Map

| File | Role |
|------|------|
| `controller/SubmissionController.java` | Upload HTTP entry, JWT auth, post-grade side effects |
| `service/SubmissionStorageService.java` | Path validation, parallel compile, folder lifecycle |
| `service/JavaCompilerService.java` | `javax.tools.JavaCompiler` wrapper |
| `grading/GradingService.java` | Top-level orchestrator, parallel challenges, persistence |
| `grading/pipeline/GradingPipeline.java` | Per-challenge staged pipeline |
| `grading/pipeline/ChallengeGradingContext.java` | Shared context record |
| `grading/pipeline/ClassReflectionGrader.java` | Java/.class pillar |
| `grading/pipeline/MmdPillarGrader.java` | MMD pillar orchestration |
| `grading/pipeline/TestcaseGrader.java` | Structural testcase pillar |
| `grading/ReflectionClassParser.java` | URLClassLoader + reflection extraction |
| `grading/MmdParser.java` | Mermaid `.mmd` text parser |
| `grading/MmdComparisonService.java` | Rubric vs parsed diagram comparison |
| `grading/MmdGradingOutcome.java` | Per-element boolean outcome maps |
| `grading/MmdTypeEquivalence.java` | Type normalization for MMD comparison |
| `grading/scoring/PillarScoreAggregator.java` | Pillar/challenge/lab percentage math |
| `grading/scoring/PartialCreditEvaluator.java` | Per-attribute accuracy |
| `grading/scoring/MemberWeightCalculator.java` | Unit weights |
| `grading/GradingResultStore.java` | DB read/write for result tables |
| `grading/LabResultAssembler.java` | Upload response `lab_result` builder |
| `grading/ParsedSubmissionSnapshotBuilder.java` | Display snapshot for result tabs |
| `grading/rubric/LabRubricService.java` | Batched rubric DB load |
| `grading/rubric/LabRubricCache.java` | In-process rubric TTL cache |
| `config/GradingExecutorConfig.java` | `gradingExecutor` bean |
| `config/PillarExecutorConfig.java` | `pillarExecutor` bean |

---

## Appendix: Converting to DOCX

Pandoc is not installed in the default environment. To generate a `.docx` from this file:

```bash
# Install pandoc, then:
pandoc docs/GRADING_WORKFLOWS.md -o docs/GRADING_WORKFLOWS.docx
```

Alternatively, open the `.md` file in VS Code / Word / Google Docs and export as DOCX.

---

*Generated from codebase state as of 2026-08-10. Source of truth: `backend/src/main/java/com/eiu/capstone/backend/grading/`.*
