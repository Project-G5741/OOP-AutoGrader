# Grading Workflows — Line-by-Line Reference

This document describes every step of the OOP AutoGrader grading pipeline: how student uploads become scores across the three grading pillars (**Java / class reflection**, **MMD diagram**, and **operational testcases**). Each section traces the actual Java source files and explains what each significant line or block does.

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
9. [Pillar 3 — Operational Testcase Grading](#9-pillar-3--operational-testcase-grading)
10. [Scoring Model](#10-scoring-model)
11. [Persistence & Side Effects](#11-persistence--side-effects)
12. [Configuration & Thread Pools](#12-configuration--thread-pools)
13. [File Map](#13-file-map)
14. [Wall-clock cost and time complexity](#14-wall-clock-cost-and-time-complexity)

---

## 1. High-Level Architecture

Each **challenge** in a lab is graded on up to **three independent pillars**. Class is always applicable; MMD applies when `has_mmd` is true; testcase applies when the challenge has at least one operational testcase.

| Pillar | Input | Grader class | What is compared |
|--------|-------|--------------|------------------|
| **Class (Java)** | Compiled `.class` files | `ClassReflectionGrader` | Rubric classes, fields, methods, constructors via reflection |
| **MMD** | Uploaded `.mmd` bytes | `MmdPillarGrader` → `MmdParser` + `MmdComparisonService` | Same rubric elements plus UML relations |
| **Testcase** | Compiled `.class` files + rubric testcase rows | `TestcaseGrader` → `InvocationRunner` | Runtime invoke + assertions (return, stdout, field state, exception, comparison) |

**Challenge score** = weighted mean of applicable pillars (`class_weight` / `mmd_weight` / `testcase_weight`, default 1).

**Lab score** = weighted mean across all rubric challenges using `challenge.weight` (missing challenges count as 0%).

```
POST /api/submissions/{labId}/{attemptNumber}/upload
  │
  ├─ LabRubricCache.get(lab)                      ← load rubric from DB (cached)
  ├─ SubmissionStorageService.processUpload()     ← validate paths, in-memory compile .java per challenge
  ├─ insert new LabSubmission (MAX(attempt_number)+1; path attempt is unused)
  ├─ GradingService.gradeSubmission()
  │    ├─ [parallel per challenge on gradingExecutor]
  │    │    └─ GradingPipeline.gradeChallenge()
  │    │         ├─ ReflectionClassParser.parseClasses()   ← load .class via URLClassLoader
  │    │         ├─ ClassReflectionGrader.grade()            ← sync
  │    │         ├─ MmdPillarGrader.grade()                  ← async on pillarExecutor
  │    │         └─ TestcaseGrader.grade()                   ← async on pillarExecutor; invokes serialize on testcaseInvokeExecutor
  │    ├─ GradingResultStore.saveChallengeScores()  ← challenge scores on the request thread
  │    ├─ persistExecutor: detail UPSERT            ← members/testcases off-request
  │    └─ LabResultAssembler.assemble()             ← in-memory lab_result bundle
  ├─ compileErrorStore / packageNormalizationStore / mmdMetaStore
  ├─ PlagiarismService.inspectUpload()            ← on the request thread; must not fail the upload
  ├─ MmdPersistenceHook.onUploadComplete()        ← no-op by default
  └─ SubmissionStorageService.deleteFolder()      ← finally: wipe temp files
```

---

## 2. Entry Point: Upload Request

**File:** `controller/SubmissionController.java`

### 2.1 Authentication & setup

```java
@PostMapping("/{labId}/{attemptNumber}/upload")
public ResponseEntity<SubmissionUploadResponse> upload(...)
```

1. **`@AuthenticationPrincipal JwtUserPrincipal`** — Spring Security JWT; `requireStudentSubmitter` requires an active student with a usable IRN. Lecturers cannot submit.
2. **`labRepository.findByIdWithTerm(labId)`** — Loads the lab with its term; 404 if missing. `studentTermAccessService.requireCanSubmit` blocks inactive or out-of-term students.
3. **`requestId = UUID.randomUUID()`** — Unique folder name to prevent upload collisions under the same IRN.
4. **`submissionFolderToDelete = null`** — Tracked so the `finally` block can always clean up temp storage.

### 2.2 Rubric load

```java
LabRubricSnapshot rubric = labRubricCache.get(lab);
```

Loads the full immutable rubric graph (challenges → classes → fields/methods/constructors → relations → testcases with invocations/assertions) from PostgreSQL, with in-process TTL caching (`app.grading.rubric-cache-ttl-minutes`, default 30).

### 2.3 Upload processing

```java
SubmissionStorageService.ProcessResult uploadResult =
    submissionStorageService.processUpload(irn, requestId, files);
submissionFolderToDelete = uploadResult.submissionFolder;
```

Validates folder structure, groups files by challenge, compiles Java in parallel. Returns challenge folders, MMD file lists, and compile metadata. See [Phase B](#4-phase-b-upload-processing--java-compilation).

### 2.4 Submission record

Each upload **inserts a new attempt**. `SubmissionAttemptNumbers.next(MAX+1)` assigns the number. The `{attemptNumber}` path segment is not used to locate or overwrite a prior row (a stale client value after a large `lab_result` parse would otherwise freeze counts).

### 2.5 Grading

```java
GradingOutcome gradingOutcome = gradingService.gradeSubmission(
    submission, rubric, uploadResult.challenges, uploadResult.mmdByChallenge);
```

Main grading entry. See [Phase C](#5-phase-c-grading-orchestration). Detail rows UPSERT by natural key; there is no `loadExisting` / `isNewSubmission` flag.

### 2.6 Post-grade persistence

- **`submission.setScore(gradingOutcome.overallScore())`** — Lab-level percentage saved to `lab_submission.score`.
- **`updateStudentProgress(...)`** — Updates `student_lab_progress` (attempts count from `COUNT(lab_submission)`, highest score, timestamps).
- **`compileErrorStore.save(...)`** — Writes per-challenge `{ catastrophic, byClassName }` diagnostics to `{SUBMISSION_BASE_DIR}/_compile_errors/{submissionId}.json`.
- **`packageNormalizationStore.save(...)`** — Non-blocking package-stripped warning when student sources included `package` declarations.
- **`submissionMmdMetaStore.save(...)`** — Writes MMD metadata to `_mmd_meta/{submissionId}.json`.
- **`plagiarismService.inspectUpload(submission, files)`** — Fingerprint + pairwise compare against other students in the lab. Runs on the upload thread; exceptions are swallowed so the student still gets results.
- **`labStatisticsCache.invalidate(labId)`** / **`lecturerOverviewCache.invalidate()`** — Clears lecturer analytics caches.
- **`mmdPersistenceHook.onUploadComplete(...)`** — Extension point for archiving `.mmd` files (default no-op).

### 2.7 Response & cleanup

Returns `SubmissionUploadResponse` with challenge score map and `lab_result` bundle. The `finally` block calls `submissionStorageService.deleteFolder(submissionFolderToDelete)` — compiled classes and temp folders are deleted after grading. Class / MMD / Testcase GETs wait on `SubmissionDetailPersistGate` (60s) for the off-thread detail UPSERT.

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
7. `classRelationRepository.findByClassEntityInWithEndpoints(...)` — UML relations (inheritance/realization also feed the Java class shell).
8. `testcaseRepository.findByChallenge_IdInOrderByOrderIndexAsc(...)` plus invocation / instance / assertion batches — operational testcase graph.

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
   - Root: `IRN_StudentName` plus optional suffix (regex `^(\d+)_([a-z0-9_\s]+)(_.*)?$`)
   - Intermediate segments: `challenge_1`, `challenge-2`, etc. (`challenge[_-]?(\d+)`)
   - Leaf: `.java` or `.mmd` only, **or** `root/.git/**` (accepted for plagiarism, skipped by compile grouping)
2. All files must share the same root folder name.
3. **`extractChallengeKey(path)`** — Finds `challenge[_-]?(\d+)` in path → normalized key `challenge_N`.
4. Routes `.mmd` → `mmdByChallenge`, everything else → `javaByChallenge`.

Invalid structure throws `SubmissionProcessingException` (HTTP 422).

### 4.3 `processChallenge(submissionFolder, challengeName, files)` (lines 168–250)

Per challenge folder:

1. **Create `challengeFolder/classes/`** directories.
2. **If no Java files** → return `ChallengeResult(challengeName, folder, 0)` (zero class files; grading will score 0% on class pillar).
3. **Build in-memory sources**:
   - For each `.java` file: `challengeRelativeJavaPath()` strips path up to challenge folder.
   - Duplicate source paths within a challenge → compile error.
   - `StudentSourceNormalizer.normalizeChallengeSources` strips `package` declarations and same-challenge cross-imports; JDK imports stay.
   - `new MemorySourceJavaFileObject(logicalPath, bytes)` — sources never written to disk.
4. **Compile** (lines 228–235):
   ```java
   CompileOutcome outcome = javaCompilerService.compileSources(sources, classesFolder);
   ```
   Happy path is one group javac. Mixed javac keeps `classes/` and fills `failedClassNames` / `compileErrorsByClassName` (`compileError` stays null). I/O and setup failures still call `failedChallenge(...)` and delete the cleanup target.
5. **Count `.class` files** in `classes/` → `classFileCount`.

### 4.4 `JavaCompilerService.compileSources()` (lines 35–72)

**File:** `service/JavaCompilerService.java`

| Line | Action |
|------|--------|
| `compiler = ToolProvider.getSystemJavaCompiler()` | Requires JDK (not JRE); fails at startup if unavailable |
| `options = ["-d", outputDir, "-encoding", "UTF-8"]` | Output compiled classes to challenge's `classes/` |
| `compiler.getTask(..., sources)` | Compiles in-memory `JavaFileObject` list |
| `task.call()` | Returns `false` on compile failure |
| On failure | Returns `CompileOutcome(succeeded=false)` with first-pass diagnostics; remainder-compiles sources that had no ERROR diagnostic and are not attributed dependents when the first task wrote no `.class` files |

**Important:** Grading never reads `.java` source files. All Java grading uses compiled `.class` output from this step.

### 4.5 MMD files during upload

`.mmd` files are grouped into `mmdByChallenge` but **not written to disk** on the hot path. They remain in memory as `MultipartFile` objects passed directly to `MmdPillarGrader`.

---

## 5. Phase C: Grading Orchestration

**File:** `grading/GradingService.java`

### 5.1 `gradeSubmission()`

```java
public GradingOutcome gradeSubmission(LabSubmission submission,
                                      LabRubricSnapshot rubric,
                                      List<ChallengeResult> challengeFolderResults,
                                      Map<String, List<MultipartFile>> mmdByChallenge)
```

| Step | Method | Purpose |
|------|--------|---------|
| 1 | `emptyExistingResults()` | Always empty maps; UPSERT by natural key, no `loadExisting` |
| 2 | `computeAgainstSnapshot(...)` | Parallel per-challenge grading |
| 3 | `gradingResultStore.saveChallengeScores(computed)` | Challenge scores on the request thread |
| 4 | `parsedSubmissionSnapshotStore.save(...)` | Save display snapshots for Class/MMD tabs |
| 5 | `gradingResultStore.scheduleDetailPersist(...)` | Member/testcase UPSERT on `persistExecutor` |
| 6 | `labResultAssembler.assemble(...)` | In-memory `lab_result` from `LabRubricSnapshot` (no Neon structure reload) |
| 7 | `return new GradingOutcome(...)` | Overall score + challenge summaries + MMD meta + lab_result |

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
    overallChallengeScores.add(new WeightedPercentage(challengeRubric.weight(), challengeScore));
}
result.overallScore = PillarScoreAggregator.weightedLabPercentage(overallChallengeScores);
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
Step 5: ChallengeGradingContext.of(rubric, classesDir, compileError, parsedClasses, failedClassNames, compileErrorsByClassName)
Step 6: classReflectionGrader.grade(context)          ← SYNCHRONOUS
Step 7: mmdPillarGrader.grade(rubric, mmdFiles)       ← ASYNC on pillarExecutor
Step 8: testcaseGrader.grade(context)                 ← ASYNC on pillarExecutor
Step 9: CompletableFuture.allOf(mmdFuture, testcaseFuture).join()
Step 10: PillarScoreAggregator.challengePercentage(class, mmd, testcase) with pillar weights
Step 11: fullyCorrect = all **applicable** pillars == 100%
Step 12: return ChallengePipelineResult(...)
```

**Threading note:** MMD and testcase pillars run in parallel on `pillarExecutor` (separate from `gradingExecutor`) to avoid deadlock when challenge workers block waiting for pillar tasks on a small pool (e.g. Render free tier with 1–2 CPUs).

### 6.2 `ChallengeGradingContext` (record)

**File:** `grading/pipeline/ChallengeGradingContext.java`

| Field | Source | Used by |
|-------|--------|---------|
| `challengeRubric` | Rubric snapshot | All graders |
| `classesDir` | `{submission}/challenge_N/classes/` | Reflection parser |
| `compileError` | Catastrophic I/O/setup only | TestcaseGrader and Class tab (gates every card) |
| `parsedClasses` | Reflection output | Class + Testcase graders |
| `parsedByName` | Map `simpleName → ParsedClass` | Lookup by rubric class name |
| `failedClassNames` | Mixed javac roots + dependents | ClassReflectionGrader zeros those classes; TestcaseGrader ERROR if an invoked type failed |
| `compileErrorsByClassName` | Root javac text or `Compilation Error on {Upstream}` | Class tab `cls.error` and testcase ERROR feedback |

---

## 7. Pillar 1 — Java (Class Reflection) Grading

**Files:** `grading/pipeline/ClassReflectionGrader.java`, `grading/ReflectionClassParser.java`

### 7.1 Reflection parsing — `ReflectionClassParser.parseClasses(classesDir)`

**Lines 23–56:** List `.class` files in `classesDir`. Top-level classes and one-level nested classes (`Outer$Inner`) are loaded; anonymous/local and deeper nesting (`$` more than once) are skipped. Nested classes are matched by qualified rubric identity (`Outer.Inner`).

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

#### 7.3.2 Class shell (binary)

The shell is all-or-nothing: scope, declaring type, abstract, nested static when nested, and an optional Extends/Implements declared-clause check when the class has exactly one inheritance or realization row. Mismatch zeros the class weight and all members. Extra student interfaces do not fail when the required pair matches. `has_mmd=false` does not skip this check.

```java
classChecks.add(HeritageShellMatcher.heritageMatchesOrSkipped(
        expectedClass, parsed, context.challengeRubric()));
double classAccuracy = classChecks.stream().allMatch(Boolean::booleanValue) ? 1.0 : 0.0;
```

#### 7.3.3 Fields (lines 63–78)

For each `FieldRubric`:
- Lookup `parsedFields.get(expectedField.name())`
- If missing → accuracy 0
- If present → accuracy 1.0 only when scope and dataType both match; otherwise 0.0
- `correct = (accuracy >= 1.0)` — stored as boolean in DB; mismatched members do not raise pillar %

#### 7.3.4 Methods (lines 80–91)

`findMatchingMethod(parsed.methods, name, parameterTypes)` — name + parameter type list must match (case-insensitive per type).

If found, accuracy 1.0 only when scope, returnType, isStatic, isAbstract, and isFinal all match; otherwise 0.0.

#### 7.3.5 Constructors (lines 93–102)

`findMatchingConstructor(parsed.constructors, parameterTypes)` — match by parameter types only.

Accuracy 1.0 only when scope matches and the `isDefault` rubric flag is satisfied; otherwise 0.0.

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

## 9. Pillar 3 — Operational Testcase Grading

**Files:** `grading/pipeline/TestcaseGrader.java`, `grading/testcase/InvocationRunner.java`

Testcases **execute student bytecode**. They are not JUnit tests and not structural EXISTENCE/DECLARATION checks. Each rubric row names an invocation (or a two-instance comparison) plus assertions.

### 9.1 Testcase rubric graph

| Table | Role |
|-------|------|
| `testcase` | Type `SINGLE_INVOCATION` or `COMPARISON`, weight, `is_hidden` |
| `testcase_invocation` | Class, constructor or method, JSON params; optional receiver constructor for instance methods |
| `testcase_instance` | Two instances for COMPARISON (`EQUALS` / `COMPARE_TO`) |
| `testcase_assertion` | Kind: RETURN_VALUE, FIELD_STATE, STDOUT, EXCEPTION, COMPARISON_RESULT |

### 9.2 `TestcaseGrader.grade(context)`

Loops **sequentially** over `challengeRubric.testcases()`. Empty list → pillar 0% (the pipeline skips this grader when the list is empty).

For each testcase:

1. Catastrophic `compileError` → every testcase `ERROR`.
2. Mixed javac: `ERROR` only when an invoked type is in `failedClassNames`; independent targets still invoke.
3. `COMPARISON` → `invocationRunner.invokeComparison(...)`; `SINGLE_INVOCATION` → `invokeSingle(...)`.
4. Every assertion is evaluated; the testcase **passes only when all assertions pass** (binary 0/1 × weight).
5. Primary assertion (STDOUT → RETURN_VALUE → FIELD_STATE → EXCEPTION → COMPARISON_RESULT) fills collapsed I/O card display strings.

### 9.3 `InvocationRunner` (the wall-clock cost)

Each invoke:

1. Submits work to the **single-thread** `testcaseInvokeExecutor`.
2. `future.get(timeoutSeconds)` — default **5s** (`app.grading.testcase-invoke-timeout-seconds`).
3. Opens a new `URLClassLoader` on `classes/`, redirects `System.out`, reflects `Constructor.newInstance` / `Method.invoke`.

Because the invoke pool has one worker, **all testcases in the JVM queue behind each other**, including those from parallel challenge workers. Wall-clock for this pillar is the sum of invocation times, not `max` across challenges. Timeout or hang costs up to τ per testcase.

Lecturer dry-run reuses `TestcaseGrader.gradeSingle()` against a temp compile dir (no persistence).

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
- Scale: 2 decimal places, `DOWN` (never round up)

### 10.2 Challenge percentage

```
challengePercentage = Σ (pillarWeight × pillarPct) / Σ pillarWeight
```

Only **applicable** pillars are included: class always; MMD when `has_mmd`; testcase when the challenge has at least one operational testcase. Lecturer-set `class_weight` / `mmd_weight` / `testcase_weight` default to 1 (equal mean when all three apply).

### 10.3 Lab percentage

```
labPercentage = Σ (challenge.weight × challengePct) / Σ challenge.weight
```

Includes all rubric challenges. Challenges without an uploaded folder contribute 0%.

### 10.4 Partial credit evaluator

**File:** `grading/scoring/PartialCreditEvaluator.java`

```java
accuracy(attributeMatches) = count(true) / count(total)
```

String comparison: trim + lowercase (`normalize()`).

`binaryAccuracy(correct)` → 1.0 or 0.0. Class-tab fields, methods, and constructors use this all-or-nothing reduction. `accuracy()` remains for MMD class presence vs type (2-attribute mean). Leftover snapshot member labels `"partial"` display as fail; stored files and numeric scores are not rewritten.

### 10.5 Correctness flags vs percentages

| Storage | Granularity |
|---------|-------------|
| Pillar % | Weighted mean of member accuracies (Class-tab members are 1.0 or 0.0) |
| `SubmissionFieldResult.correct` etc. | Boolean: `accuracy >= 1.0` only |
| `SubmissionChallengeResult.correct` | `fullyCorrect`: all **applicable** pillars == 100% |
| `SubmissionChallengeResult.score` | Challenge percentage (0–100) |

---

## 11. Persistence & Side Effects

### 11.1 `GradingResultStore`

**File:** `grading/GradingResultStore.java`

Challenge scores UPSERT on the upload thread (`submission_challenge_result_key`). Member, relation, testcase, and assertion rows UPSERT on `persistExecutor` via `GradingResultJdbcWriter` (`ON CONFLICT` on the same unique keys). `GET /class`, `/mmd`, and `/testcases` wait on `SubmissionDetailPersistGate` (60s). Re-upload of a **new attempt** inserts a new `lab_submission` row; element UPSERT is per (submission, rubric element), not by overwriting a prior attempt.

| Table / entity | Content |
|----------------|---------|
| `submission_field_result` | Per-field boolean correct |
| `submission_method_result` | Per-method boolean correct |
| `submission_constructor_result` | Per-constructor boolean correct |
| `submission_relation_result` | Per-relation boolean correct (MMD only) |
| `submission_challenge_result` | Per-challenge score + fullyCorrect flag |
| `submission_testcase_result` | Per-testcase status + primary I/O display strings |
| `submission_testcase_assertion_result` | Per-assertion status + actual JSON |

### 11.2 Ephemeral JSON sidecars

| Path | Written by | Content |
|------|-----------|---------|
| `_compile_errors/{submissionId}.json` | `SubmissionCompileErrorStore` | Map challengeId → `{ catastrophic, byClassName }` |
| `_package_normalization/{submissionId}.json` | `SubmissionPackageNormalizationStore` | Per-challenge package-stripped warning |
| `_mmd_meta/{submissionId}.json` | `SubmissionMmdMetaStore` | Per challenge: mmdSubmitted, parseError, classStereotypeCorrect, relationErrors |
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
| `app.grading.parallelism` | 4 | `gradingExecutor` | Max concurrent challenge grading workers (capped at CPU count) |
| `app.compile.parallelism` | 4 | `compileExecutor` | Max concurrent per-challenge compile workers (capped at CPU count) |
| (derived) | `max(2, parallelism×2)` | `pillarExecutor` | MMD + testcase pillars inside each challenge (not CPU-capped) |
| (fixed) | 1 | `testcaseInvokeExecutor` | Serializes student `System.out` capture and invoke timeouts |
| (fixed) | 2 | `persistExecutor` | Off-request detail UPSERT |
| `app.grading.testcase-invoke-timeout-seconds` | 5 | — | Per-invocation timeout |
| `app.grading.rubric-cache-ttl-minutes` | 30 | `LabRubricCache` | Rubric cache TTL |
| `app.grading.timing-log` | false | `TimingLog` | Aligned `[timing]` blocks: upload (`rubric`, `compile`, `grade`, `plagiarism`, `total`), compile, challenge, grade submission |
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
| `grading/pipeline/TestcaseGrader.java` | Operational testcase pillar |
| `grading/testcase/InvocationRunner.java` | Timed reflect invoke + stdout capture |
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
| `config/CompileExecutorConfig.java` | `compileExecutor` bean |
| `config/PillarExecutorConfig.java` | `pillarExecutor` bean |
| `config/TestcaseInvokeExecutorConfig.java` | Single-thread invoke pool |
| `config/PersistExecutorConfig.java` | Off-request detail UPSERT pool |
| `plagiarism/PlagiarismService.java` | Fingerprint, pairwise compare, lab re-evaluate on upload |

---

## 14. Wall-clock cost and time complexity

Enable `app.grading.timing-log=true` to print `[timing]` blocks for upload (`rubric`, `compile`, `grade`, `plagiarism`, `total`), per-challenge compile (`javac`), per-challenge grade (`parse`, `class`, `mmd`, `testcase`), and grade submission (`compute`, `save`, `assemble`). The ranking below is **request-thread wall-clock** — what the student waits on before scores appear.

Symbols: *C* challenges, *K* compile workers (`min(app.compile.parallelism, CPUs)`), *P* grading workers (`min(app.grading.parallelism, CPUs)`), *T* operational testcases in the lab, *τ* invoke timeout (5s), *E* rubric elements, *L* MMD character length, *R*/*D* rubric vs diagram relations, *A* other fingerprints in the lab, *U* other students, *F* hashed `.java`/`.mmd` files, *B* hashed bytes.

| Rank | Stage | Typical dominance | Work | Request-thread wall |
|------|-------|-------------------|------|---------------------|
| 1 | Operational testcases | Large *T*, slow or hanging student code | *Θ(T)* invokes; new `URLClassLoader` per invoke | **Σ invoke times across the whole lab** — `testcaseInvokeExecutor` is 1 thread, so *P* does not help. Worst case *O(T · τ)* |
| 2 | `javac` per challenge | Many/large `.java` files; mixed failure runs javac **twice** | Roughly *O(source size)* per challenge (compiler internals are superlinear in practice) | *Θ(⌈C/K⌉ · max compile in batch)* |
| 3 | Plagiarism inspect | Busy lab (many prior attempts) | SHA-256 *O(B)*; reconstruct `.git`; pairwise *O(A)* with Jaccard *O(F)*; **O(U) DB round-trips** for peer best scores; then re-evaluate all lab matches | On the upload thread after grading (exceptions swallowed) |
| 4 | Rubric cache miss | First upload after TTL / save | ~12 batched queries + *O(E)* graph build | Neon RTT × query count; cache hit is cheap |
| 5 | Class reflection | Rarely vs 1–3 | Parse *O(classes × members)*; grade *O(E)* map lookups | Parallel across *P* challenges; usually milliseconds |
| 6 | MMD parse + compare | Huge diagrams | Tokenize *O(L)*; class match *O(E)*; relations *O(R · D)* | Overlaps testcases on `pillarExecutor`; usually smaller than invoke |
| 7 | `lab_result` assemble | Was a Neon bottleneck; now in-memory | *O(E)* DTO walk from `LabRubricSnapshot` | On the request thread after compute |
| 8 | Challenge-score UPSERT + snapshot | Small vs compute | *O(C)* | On the request thread |
| — | Detail UPSERT | Large *E* | *O(E)* JDBC | **Off-request** (`persistExecutor`); GET tabs wait up to 60s |
| — | Multipart + `.git` | Fat folders | *O(upload bytes)* | Before compile; Spring reads every part including `.git` |

**Why testcases beat compile on wall-clock even when `javac` is “heavier” CPU:** compile parallelizes across challenges; invokes do not. Four challenges with 10 testcases each still run ~40 serial `future.get` calls on one worker.

**Why plagiarism can overtake compile on a large roster:** `inspectUpload` compares the new fingerprint to **every other fingerprint** in the lab (every prior attempt of every other student), then `bestScoresForLabUsers` issues one query per other user.

Cleanup (`deleteFolder`) runs in `finally` after the response is built; it is not on the critical path for JSON generation but still holds the HTTP thread until the delete walk finishes.

---

## Appendix: Converting to DOCX

Pandoc is not installed in the default environment. To generate a `.docx` from this file:

```bash
# Install pandoc, then:
pandoc docs/GRADING_WORKFLOWS.md -o docs/GRADING_WORKFLOWS.docx
```

Alternatively, open the `.md` file in VS Code / Word / Google Docs and export as DOCX.

---

*Generated from codebase state as of 2026-09-09. Source of truth: `backend/src/main/java/com/eiu/capstone/backend/` (grading, service compile path, plagiarism).*
