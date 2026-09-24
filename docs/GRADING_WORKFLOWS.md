# Grading Workflows — Line-by-Line Reference

This document describes every step of the OOP AutoGrader grading pipeline: how student uploads become scores across the class-reflection and MMD pillars, and how lecturer operational tests (Unit / Composition) dry-run. Each section traces the actual Java source files and explains what each significant line or block does. Student upload does **not** execute operational tests this ship.

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

Each **challenge** in a lab is graded on up to **three independent pillars**. Class is always applicable; MMD applies when `has_mmd` is true; operational testcases apply when the challenge has at least one authored Unit/Composition row. Student upload runs `TestcaseGrader` for applicable challenges (one worker session per upload when any challenge needs OT). Challenge `testcase_weight` scales the testcase pillar in student totals when applicable.

| Pillar | Input | Grader class | What is compared |
|--------|-------|--------------|------------------|
| **Class (Java)** | Compiled `.class` files | `ClassReflectionGrader` | Rubric classes, fields, methods, constructors via reflection |
| **MMD** | Uploaded `.mmd` bytes | `MmdPillarGrader` → `MmdParser` + `MmdComparisonService` | Same rubric elements plus UML relations |
| **Testcase** | Compiled `.class` files + rubric testcase rows | `TestcaseGrader` → `InvocationRunner` | Runtime invoke + assertions (return, stdout, field state, exception, object check) |

**Challenge score** (student upload) = weighted mean of applicable pillars (`class_weight` / `mmd_weight` / `testcase_weight` when OT rows exist).

**Lab score** = weighted mean across all rubric challenges using `challenge.weight` (missing challenges count as 0%).

```
POST /api/submissions/{labId}/{attemptNumber}/upload
  │
  ├─ requireUploadAccess                          ← one query (cached 30s on success; warmed by GET /api/labs)
  ├─ LabRubricCache.get(lab)                      ← overlaps compile (cache hit is leftover 0ms)
  ├─ SubmissionStorageService.processUpload()     ← validate paths, in-memory compile .java per challenge
  ├─ assign lab_submission.id in memory (path attempt unused)
  ├─ GradingService.gradeSubmission()             ← compute + lab_result assemble only
  │    ├─ [parallel per challenge on gradingExecutor]
  │    │    └─ GradingPipeline.gradeChallenge()
  │    │         ├─ ReflectionClassParser.parseClasses()   ← load .class via URLClassLoader
  │    │         ├─ ClassReflectionGrader.grade()            ← sync
  │    │         ├─ MmdPillarGrader.grade()                  ← async on pillarExecutor
  │    │         └─ TestcaseGrader skipped on student upload  ← lecturer dry-run still uses isolated worker JVM
  │    └─ LabResultAssembler.assemble()             ← in-memory lab_result bundle
  ├─ UploadPersistService.persist()               ← one SQL: insert MAX+1, challenge UPSERT, progress
  │    └─ persistExecutor after that statement: detail UPSERT
  ├─ compileErrorStore / packageNormalizationStore / mmdMetaStore  ← persistExecutor
  ├─ snapshot PlagiarismSignals                     ← request thread (SHA-256 + git parse)
  ├─ PlagiarismService.inspectUpload(signals)       ← persistExecutor after persist; must not fail the upload
  ├─ MmdPersistenceHook.onUploadComplete()        ← no-op by default
  └─ SubmissionStorageService.deleteFolder()      ← finally on persistExecutor: wipe temp files
```

---

## 2. Entry Point: Upload Request

**File:** `controller/SubmissionController.java`

### 2.1 Authentication & setup

```java
@PostMapping("/{labId}/{attemptNumber}/upload")
public ResponseEntity<SubmissionUploadResponse> upload(...)
```

1. **`@AuthenticationPrincipal JwtUserPrincipal`** — Spring Security JWT (`/api/submissions/**` is `STUDENT` only). Blank email is 401.
2. **`studentTermAccessService.requireUploadAccess(email, labId)`** — One query (`UserAccountRepository.findUploadAccess`) loads the user, optional lab+term, and enrollment count. Successful results are cached `app.upload.access-cache-ttl-seconds` (default 30); denials are not. `GET /api/labs` remembers visible labs after enrollment is proven, and student GET challenges/stats call the same method, so the first upload after opening the dashboard usually skips the Neon round-trip. Deadline openness is still checked with `Instant.now()` on a cache hit. Same 401/404/403 families as before (unknown user, missing lab, inactive, not enrolled, lab not in current quarter, lab not open). Compile does not start until this succeeds. IRN is resolved from the JWT or the user row; teacher-only accounts without IRN are 403.
3. **`requestId = UUID.randomUUID()`** — Unique folder name to prevent upload collisions under the same IRN.
4. **`submissionFolderToDelete = null`** — Tracked so the `finally` block can always clean up temp storage.

### 2.2 Rubric load (overlaps compile)

```java
CompletableFuture<LabRubricSnapshot> rubricFuture = CompletableFuture.supplyAsync(
        () -> labRubricCache.get(lab));
```

Loads the full immutable rubric graph (challenges → classes → fields/methods/constructors → relations → testcases with invocations/assertions) from PostgreSQL, with in-process TTL caching (`app.grading.rubric-cache-ttl-minutes`, default 30). The future starts before `processUpload`; `rubric` in the timing log is leftover wait after compile (0 on a warm cache hit).

### 2.3 Upload processing

```java
SubmissionStorageService.ProcessResult uploadResult =
    submissionStorageService.processUpload(irn, requestId, files);
submissionFolderToDelete = uploadResult.submissionFolder;
LabRubricSnapshot rubric = rubricFuture.join();
```

Validates folder structure, groups files by challenge, compiles Java in parallel. Returns challenge folders, MMD file lists, and compile metadata. See [Phase B](#4-phase-b-upload-processing--java-compilation).

### 2.4 Submission record

Each upload **inserts a new attempt after grade**. `lab_submission.id` is assigned in memory so grading can reference it before insert. Attempt number is `MAX+1` inside the persist SQL. The `{attemptNumber}` path segment is not used to locate or overwrite a prior row (a stale client value after a large `lab_result` parse would otherwise freeze counts).

### 2.5 Grading

```java
GradingOutcome gradingOutcome = gradingService.gradeSubmission(
    submission, rubric, uploadResult.challenges, uploadResult.mmdByChallenge);
```

Main grading entry (compute + `lab_result` assemble only). See [Phase C](#5-phase-c-grading-orchestration). Detail rows UPSERT by natural key after persist commits; there is no `loadExisting` / `isNewSubmission` flag.

### 2.6 Post-grade persistence

- **`UploadPersistService.persist(...)`** — One JDBC statement (`GradingResultJdbcWriter.persistUpload`): insert `lab_submission` with `MAX+1` and the final score, UPSERT `submission_challenge_result`, UPSERT `student_lab_progress`. Then write the parsed snapshot and schedule member/testcase UPSERT (the submission row is already committed).
- **`compileErrorStore.save(...)`** — Off-thread after persist: per-challenge `{ catastrophic, byClassName }` diagnostics to `{SUBMISSION_BASE_DIR}/_compile_errors/{submissionId}.json`.
- **`packageNormalizationStore.save(...)`** — Off-thread: package-stripped warning when student sources included `package` declarations.
- **`submissionMmdMetaStore.save(...)`** — Off-thread: MMD metadata to `_mmd_meta/{submissionId}.json`.
- **`plagiarismService.inspectUpload(submission, signals)`** — Snapshot `PlagiarismSignals` from multipart on the upload thread, then compare on `persistExecutor`. Exceptions are swallowed so the student still gets results. Lecturer flags typically appear within ~1–3s (live SQL). Lab statistics / overview caches invalidate after persist and again after inspect.
- **`labStatisticsCache.invalidate(labId)`** / **`lecturerOverviewCache.invalidate()`** — Clears lecturer analytics caches (again after inspect so `plagiarismRate` is not cached without new flags).
- **`mmdPersistenceHook.onUploadComplete(...)`** — Extension point for archiving `.mmd` files (default no-op).

### 2.7 Response & cleanup

Returns `SubmissionUploadResponse` with challenge score map and `lab_result` bundle. The `finally` block schedules `submissionStorageService.deleteFolder(submissionFolderToDelete)` on `persistExecutor` — compiled classes and temp folders are deleted after grading without holding the HTTP response. Class / MMD / Testcase GETs wait on `SubmissionDetailPersistGate` (60s) for the off-thread detail UPSERT. The student dashboard applies the upload payload in place and does not refetch `GET /challenges` or `GET /stats` until the student selects a different lab.

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
8. `testcaseRepository.findByChallenge_IdInOrderByOrderIndexAsc(...)` plus invocation / assertion batches — Unit/Composition graph.

The result is an immutable `LabRubricSnapshot` keyed by challenge number, used read-only throughout grading. Rubric mutations must call `RubricCacheInvalidationSupport.invalidateLab(labId)`. Operator OT wipe (`docs/sql/2026-09-23-operational-testcase-unit-composition.sql` or `TestcaseSchemaMigrator`) must `LabRubricCache.invalidateAll()` or restart the API.

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
| 3 | `labResultAssembler.assemble(...)` | In-memory `lab_result` from `LabRubricSnapshot` (no Neon structure reload) |
| 4 | `return new GradingOutcome(...)` | Overall score + challenge summaries + MMD meta + lab_result + computed (persist is `UploadPersistService`) |

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
- Testcase results stay empty on student upload (pillar not applicable)
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
Step 8: student upload: testcase pillar skipped (no worker). Lecturer dry-run: TestcaseGrader.gradeSingle()
Step 9: join MMD future (upload does not join a testcase future)
Step 10: PillarScoreAggregator.challengePercentage(class, mmd, testcase) — upload treats testcase as not applicable
Step 11: fullyCorrect = all **applicable** pillars == 100%
Step 12: return ChallengePipelineResult(...)
```

**Threading note:** MMD still runs on `pillarExecutor` (separate from `gradingExecutor`) to avoid deadlock when challenge workers block waiting for pillar tasks on a small pool (e.g. Render free tier with 1–2 CPUs). Student upload does not schedule `TestcaseGrader`. Lecturer dry-run acquires `workerJvmSlot` on the HTTP thread.

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
| `compileErrorsByClassName` | One `CompileErrorMessage` line (see `backend/src/main/java/com/eiu/capstone/backend/service/compile/AGENTS.md`) | Class tab `cls.error` and testcase ERROR feedback |

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

Operational tests **execute compiled bytecode**. They are not JUnit tests and not structural EXISTENCE/DECLARATION checks. Types are **Unit** (one invocation) and **Composition** (ordered named-object steps). **Student upload does not run this pillar.** Lecturer dry-run does, against reference Java.

### 9.1 Testcase rubric graph

| Table | Role |
|-------|------|
| `testcase` | Type `UNIT` or `COMPOSITION`, `is_hidden` (no per-testcase weight) |
| `testcase_invocation` | Ordered steps; Unit has exactly one. `instance_name` is a constructor/static product or Composition receiver |
| `testcase_assertion` | Kind: RETURN_VALUE, FIELD_STATE, STDOUT, EXCEPTION. Object checks live in `expected_value` JSON |

There is no `testcase_instance` table and no `COMPARISON_RESULT` kind.

### 9.2 `TestcaseGrader` (dry-run)

`GradingPipeline.gradeChallenge(...)` without a worker is the student upload path (class/MMD only). Lecturer dry-run uses `TestcaseGrader.gradeSingle()` and `invokeScenario` for both UNIT and COMPOSITION.

For each testcase:

1. Catastrophic `compileError` → every testcase `ERROR`.
2. Mixed javac: `ERROR` only when an invoked type is in `failedClassNames`; independent targets still invoke.
3. Worker `scenario` op: Unit is one step (hidden no-arg receiver for instance methods); Composition runs ordered named instances.
4. Every assertion is evaluated; the testcase **passes only when all assertions pass**.
5. Primary assertion (STDOUT → RETURN_VALUE → FIELD_STATE → EXCEPTION) fills collapsed I/O card display strings. Constructors use FIELD_STATE only (no RETURN_VALUE). Composition object-return equals: `{ "$objectCheck": "EQUALS", "$instance": "name" }`.

### 9.3 Isolated testcase worker

Each dry-run or student-upload OT batch:

1. Acquires the host `workerJvmSlot` (one process). Student upload acquires the slot when any challenge in the upload batch has operational testcases; dry-run always acquires.
2. Sends one NDJSON `scenario` request; the worker loads target classes with a platform-parent `URLClassLoader`, invokes, and returns snapshots. Constructor results and static-factory object returns register `instanceName`. Instance-method returns do not overwrite the named receiver.
3. The API waits up to `app.grading.testcase-invoke-timeout-seconds` (default **5s**), then tree-kills and respawns without releasing the host slot.

At most one worker session runs on the host slot (local JVM or remote sandbox session when `app.grading.sandbox.enabled=true`). Other dry-runs wait for that slot on the HTTP thread (`worker_slot_wait_ms`). Class-tab grading still uses `Class.forName(..., initialize=false)` in the API. Sandbox path: `WorkerSessionFactory` → `RemoteWorkerSessionClient` → `sandbox-runner` warm pool; see `docs/SANDBOX_RUNNER_DEPLOY.md`.

Operator wipe SQL is `docs/sql/2026-09-23-operational-testcase-unit-composition.sql`. `TestcaseSchemaMigrator` applies it on startup when leftover types/columns remain, then `LabRubricCache.invalidateAll()`.

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

Only **applicable** pillars are included: class always; MMD when `has_mmd`; operational testcases when the challenge has ≥1 authored testcase row. Lecturer-set `class_weight` / `mmd_weight` / `testcase_weight` default to 1.

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

`UploadPersistService` runs one JDBC statement: insert `lab_submission` (`MAX+1`, final score), UPSERT `submission_challenge_result`, UPSERT `student_lab_progress` (`ON CONFLICT student_lab_progress_user_lab_key`). Borrow the connection with `DataSourceUtils` (never `dataSource.getConnection()`). Snapshot write is local disk after that statement. Member, relation, testcase, and assertion rows UPSERT on `persistExecutor` after the statement succeeds via `GradingResultJdbcWriter` (`ON CONFLICT` on the same unique keys). `GET /class`, `/mmd`, and `/testcases` wait on `SubmissionDetailPersistGate` (60s). Re-upload of a **new attempt** inserts a new `lab_submission` row; element UPSERT is per (submission, rubric element), not by overwriting a prior attempt.

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
  "testcases": [],
  "scores": { "class": 85.0, "mmd": 100.0, "testcase": 0.0, "total": 92.5 },
  "scoreApplicability": { "class": true, "mmd": true, "testcase": false }
}
```

Student upload always sets `testcaseApplicable` false and `testcases: []`. The student UI hides the Operation Test tab. Allows Class/MMD to render immediately without follow-up API calls.

---

## 12. Configuration & Thread Pools

| Property | Default | Bean | Purpose |
|----------|---------|------|---------|
| `app.grading.parallelism` | 4 | `gradingExecutor` | Max concurrent challenge grading workers (capped at CPU count) |
| `app.compile.parallelism` | 4 | `compileExecutor` | Max concurrent per-challenge compile workers (capped at CPU count) |
| (derived) | `max(2, parallelism×2)` | `pillarExecutor` | MMD pillar inside each challenge (not CPU-capped). Upload does not schedule TestcaseGrader |
| (fixed) | 1 | `workerJvmSlot` | Host-wide isolated worker JVM; lecturer dry-run acquires on the HTTP thread. Student upload does not |
| (fixed) | 2, not CPU-capped | `persistExecutor` | Detail UPSERT, rubric overlap, sidecars, plagiarism inspect, temp delete |
| `app.grading.testcase-invoke-timeout-seconds` | 5 | — | Per-invocation timeout (process kill) |
| `app.grading.worker-jar` | `/app/worker.jar` | — | Thin worker JAR |
| `app.grading.worker-java` | `java` | — | Java binary used to spawn the worker |
| `app.grading.rubric-cache-ttl-minutes` | 30 | `LabRubricCache` | Rubric cache TTL |
| `app.upload.access-cache-ttl-seconds` | 30 | `StudentTermAccessService` | Successful upload-access cache; denials not cached |
| `app.grading.timing-log` | false | `TimingLog` | Aligned `[timing]` blocks: upload (`access`, `rubric`, `compile`, `grade`, `persist`, `plagiarism`, `total`), compile, challenge, grade submission |
| `app.storage.submission-base-dir` | `submissions/` | — | Temp upload root |

**Deadlock prevention:** `pillarExecutor` is intentionally separate from `gradingExecutor`. If they shared one pool, a challenge worker waiting for MMD futures could exhaust the pool (documented in `docs/solutions/architecture-patterns/grading-executor-deadlock-render.md`).

---

## 13. File Map

| File | Role |
|------|------|
| `controller/SubmissionController.java` | Upload HTTP entry, JWT auth, post-grade side effects |
| `service/StudentTermAccessService.java` | Upload + student GET challenges/stats `requireUploadAccess` (one query, 30s success cache); other submit paths `requireCanSubmit` |
| `service/UploadPersistService.java` | After grade: one JDBC persist SQL + snapshot; detail persist after that statement |
| `service/SubmissionStorageService.java` | Path validation, parallel compile, folder lifecycle |
| `service/JavaCompilerService.java` | `javax.tools.JavaCompiler` wrapper |
| `grading/GradingService.java` | Top-level orchestrator, parallel challenges, `lab_result` assemble |
| `grading/pipeline/GradingPipeline.java` | Per-challenge staged pipeline |
| `grading/pipeline/ChallengeGradingContext.java` | Shared context record |
| `grading/pipeline/ClassReflectionGrader.java` | Java/.class pillar |
| `grading/pipeline/MmdPillarGrader.java` | MMD pillar orchestration |
| `grading/pipeline/TestcaseGrader.java` | Operational testcase pillar |
| `grading/testcase/InvocationRunner.java` | IPC facade to the isolated worker JVM |
| `grading/testcase/WorkerProcessClient.java` | Spawn, env allowlist, stderr cap, respawn |
| `grading/testcase/worker/WorkerMain.java` | Worker process entry |
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

Enable `app.grading.timing-log=true` to print `[timing]` blocks for upload (`access`, `rubric`, `compile`, `grade`, `persist`, `plagiarism` = signal snapshot + schedule, `total`), off-thread `Plagiarism inspect`, per-challenge compile (`javac`), per-challenge grade (`parse`, `class`, `mmd`, `testcase`), and grade submission (`compute`, `worker_slot_wait_ms`, `worker_spawn_ms`, `worker_respawn_count`, `assemble`). The ranking below is **request-thread wall-clock** — what the student waits on before scores appear.

Symbols: *C* challenges, *K* compile workers (`min(app.compile.parallelism, CPUs)`), *P* grading workers (`min(app.grading.parallelism, CPUs)`), *T* operational testcases (lecturer dry-run only this ship), *τ* invoke timeout (5s), *E* rubric elements, *L* MMD character length, *R*/*D* rubric vs diagram relations, *A* other fingerprints in the lab, *U* other students, *F* hashed `.java`/`.mmd` files, *B* hashed bytes. Student upload does not wait on `workerJvmSlot`. Lecturer dry-run still does (`worker_slot_wait_ms`).

| Rank | Stage | Typical dominance | Work | Request-thread wall |
|------|-------|-------------------|------|---------------------|
| 1 | `javac` per challenge | Many/large `.java` files; mixed failure runs javac **twice** | Roughly *O(source size)* per challenge (compiler internals are superlinear in practice) | *Θ(⌈C/K⌉ · max compile in batch)* |
| 2 | Plagiarism snapshot | Fat `.git` without reflog (rare reconstruct) | SHA-256 *O(B)*; in-memory git `config`+reflog | **Extract + schedule** on the upload thread (milliseconds unless reconstruct). Compare/persist is **off-request** (`persistExecutor`); see `[timing] Plagiarism inspect` |
| 3 | Rubric cache miss | First upload after TTL / save | ~12 batched queries + *O(E)* graph build | Neon RTT × query count; cache hit is cheap |
| 4 | Class reflection | Rarely vs 1–3 | Parse *O(classes × members)*; grade *O(E)* map lookups | Parallel across *P* challenges; usually milliseconds |
| 5 | MMD parse + compare | Huge diagrams | Tokenize *O(L)*; class match *O(E)*; relations *O(R · D)* | On `pillarExecutor`; usually smaller than compile |
| 6 | `lab_result` assemble | Was a Neon bottleneck; now in-memory | *O(E)* DTO walk from `LabRubricSnapshot` | On the request thread after compute |
| 7 | Persist SQL (insert MAX+1 + challenge UPSERT + progress) | One Neon RTT | *O(C)* | On the request thread after assemble |
| — | Lecturer dry-run OT | Large *T*, slow or hanging reference code | *Θ(T)* `scenario` ops on one worker JVM | **Σ invoke times**, plus `worker_slot_wait_ms` under concurrent dry-runs. Worst case *O(T · τ)* plus respawn. Not on the student upload path |
| — | Detail UPSERT | Large *E* | *O(E)* JDBC | **Off-request** (`persistExecutor`); GET tabs wait up to 60s |
| — | Multipart + `.git` | Fat folders | *O(upload bytes)* | Before compile; Spring reads every part including `.git` |

**Why compile now dominates student wait:** upload no longer invokes operational tests, so it does not serialize on the one worker JVM. Lecturer dry-run still does.

**Why plagiarism used to overtake compile on a large roster:** pairwise compare still walks every other fingerprint, but that work is on `persistExecutor`. The student wait is only the signal snapshot. Peer/prior bests are one grouped attempts load; non-matches are not inserted; re-eval is limited to rows where this uploader is the other side.

Cleanup (`deleteFolder`) is scheduled on `persistExecutor` in `finally` after the response is built; it does not hold the HTTP thread.

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
