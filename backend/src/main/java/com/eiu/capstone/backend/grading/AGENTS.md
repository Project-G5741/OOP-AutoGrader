# Grading Engine

## Purpose

Grade lab submissions across three equal pillars per challenge: Java `.class` reflection, MMD diagram comparison, and operational testcase checks. Produce per-element results, pillar scores, and an upload-time `lab_result` bundle for the student UI.

## Ownership

| File | Role |
|---|---|
| `GradingService.java` | Thin orchestrator: parallel per-challenge grading, `lab_result` assembly (persist is `UploadPersistService`) |
| `grading/pipeline/GradingPipeline.java` | Staged pipeline: class pillar, then parallel MMD + testcase pillars |
| `grading/pipeline/ClassReflectionGrader.java` | `.class` pillar: class shells are binary (all shell attributes match or 0%), including an optional Extends/Implements declared-clause check from the class's inheritance/realization row; when the shell fails, fields/methods/constructors score 0%; otherwise members are all-or-nothing (every graded attribute must match); leftover snapshot `"partial"` labels display as fail and are not rewritten; explicit no-arg constructors are not treated as compiler-default unless the rubric `isDefault` flag is set |
| `grading/pipeline/HeritageShellMatcher.java` | Shared declared-clause Extends/Implements predicate for the class grader and Class-tab shell display |
| `grading/pipeline/MmdPillarGrader.java` | MMD pillar |
| `grading/pipeline/TestcaseGrader.java` | Operational testcase orchestrator |
| `grading/testcase/InvocationRunner.java` | Load student classes, invoke constructors/methods with timeout + stdout capture |
| `grading/testcase/AssertionEvaluator.java` | Per-kind assertion evaluation (RETURN_VALUE, FIELD_STATE, STDOUT, EXCEPTION, COMPARISON_RESULT) |
| `grading/testcase/TestcaseDisplayFormatter.java` | Primary I/O card display strings + lazy expanded assertion formatting |
| `grading/testcase/PrimaryAssertionSelector.java` | Primary assertion priority for collapsed card |
| `grading/testcase/TestcaseResultMapper.java` | Map rubric + persisted results to student-facing `TestcaseResultDTO` |
| `grading/scoring/PillarScoreAggregator.java` | Pillar, challenge (mean of applicable pillars), and lab percentages; two-decimal rounding is always down |
| `grading/scoring/PartialCreditEvaluator.java` | `binaryAccuracy` for Class-tab members and MMD elements; `accuracy()` matching-attribute ratio for MMD class presence vs type |
| `grading/LabResultAssembler.java` | Build `lab_result.challenge_<N>` bundles for upload response |
| `ParsedSubmissionSnapshotBuilder.java` | Capture rubric-scoped student display text at grade time |
| `GradingResultStore.java` | Short read/write transactions for submission result tables |
| `grading/rubric/LabRubricService.java` | Load full lab rubric (invocations, instances, assertions) in batched DB queries |
| `grading/rubric/LabRubricCache.java` | In-process TTL cache keyed by lab ID; `get(UUID)` is a cache-hit with no SQL |
| `grading/rubric/LabRubricSnapshot.java` | Immutable rubric graph for grading |
| `MmdParser.java` | Facade: `MmdTokenizer` → `MmdAstParser` → `MmdAstToParsedMapper` → diagram DTOs |
| `grading/mmd/MmdTokenizer.java` | Character-level tokenizer for Mermaid `classDiagram` source |
| `grading/mmd/MmdAstParser.java` | Builds diagram AST; enforces `classDiagram` header |
| `grading/mmd/MmdAstToParsedMapper.java` | Maps AST to `ParsedMmdDiagram` for comparison |
| `grading/mmd/MmdRelationLineParser.java` | Relationship line parsing (arrows, cardinality, lollipop, two-way) |
| `grading/mmd/MmdRelationTypes.java` | Relation arrow table and canonical type names |
| `grading/mmd/ast/MmdNamespaceNode.java` | Namespace block AST node |
| `MmdComparisonService.java` | Compare parsed MMD against rubric; resolves simple and namespace-qualified class names via `ParsedMmdDiagram.classByName` |
| `DTO/MmdResponseDTO.java` | `/mmd` and `lab_result` MMD payload: `{ classes, parseError }` |
| `grading/rubric/TestcaseRubricAssembler.java` | Build `TestcaseRubric` from lecturer testcase DTOs (dry-run + validation) |
| `service/TestcaseRubricService.java` | Lecturer testcase CRUD; referenced by structure save delete guard |
| `service/TestcaseDryRunService.java` | Compile pasted reference Java + `TestcaseGrader.gradeSingle()` preview (no persistence) |

## Local Contracts

### Pipeline position

```
SubmissionController
  → StudentTermAccessService.requireUploadAccess()  (one query; 30s success cache; warmed by GET /api/labs)
  → LabRubricCache.get(lab)                         (overlaps compile)
  → SubmissionStorageService.processUpload()
  → assign lab_submission.id in memory
  → GradingService.gradeSubmission()   (compute + assemble only)
      → GradingPipeline.gradeChallenge() per folder
          → ClassReflectionGrader (sync)
          → MmdPillarGrader + TestcaseGrader (parallel on `pillarExecutor`, not `gradingExecutor`)
      → LabResultAssembler.assemble() from in-memory LabRubricSnapshot (no loadChallengeStructures)
          → skip MMD/testcase trees when pillar not applicable
  → UploadPersistService.persist()     (one JDBC statement: insert MAX+1 + scores + progress)
      → persistExecutor after that statement: GradingResultJdbcWriter detail UPSERT
  → compile/package/mmd sidecars off-thread
  → PlagiarismService.inspectUpload()   (request thread; after persist; failures swallowed)
  → MmdPersistenceHook.onUploadComplete()
  → SubmissionStorageService.deleteFolder() (finally, off-thread)
```

### Scoring

- **Pillar percentage** = weighted mean of member accuracies (`PillarScoreAggregator.pillarPercentage`); class shells use `class_entity.weight`
- **Challenge percentage** = weighted mean of applicable pillars using `challenge.class_weight`, `challenge.mmd_weight`, and `challenge.testcase_weight`
- **Lab percentage** = weighted mean across rubric challenges using `challenge.weight`; missing challenges count as 0%
- **Score rounding** = always down (`RoundingMode.DOWN` / `Math.floor`): two-decimal stored percentages and integer display scores never round up
- **Operational testcases** pass only when every assertion passes (binary 0/1 per testcase weight)
- Challenges with zero testcase rows score 0% on the testcase pillar
- Compile errors short-circuit testcase grading only when `compileError` is catastrophic I/O/setup: all testcases for that challenge → `ERROR` before invoke. Mixed javac marks ERROR only for testcases whose invoked types are in `failedClassNames`; independent targets still invoke

### Operational testcase grading

- Rubric tables: `testcase`, `testcase_invocation` (optional `receiver_constructor_id` + `receiver_params` for METHOD invocations), `testcase_instance`, `testcase_assertion`
- SINGLE_INVOCATION: one invocation + one or more assertions; instance methods may seed the receiver via constructor params instead of a no-arg constructor
- COMPARISON: two `testcase_instance` rows + COMPARISON_RESULT assertion
- Timeout: `app.grading.testcase-invoke-timeout-seconds` (default 5); invocations run on single-thread `testcaseInvokeExecutor`
- Exception matching: exception class simple name only (not message)
- Value types v1: primitives, `String`, null, arrays of primitives

### Result persistence

Challenge scores UPSERT on the upload thread inside `GradingResultJdbcWriter.persistUpload` (one statement with `lab_submission` insert `MAX+1` and `student_lab_progress` UPSERT). Borrow the connection with `DataSourceUtils` (never `dataSource.getConnection()`). Member, relation, testcase, and assertion rows UPSERT on `persistExecutor` after that statement succeeds via `GradingResultJdbcWriter` (`ON CONFLICT` on the same unique keys). `GET /class`, `/mmd`, and `/testcases` wait on `SubmissionDetailPersistGate` (60s). Re-upload does not `loadExisting`; UPSERT updates in place.

| Entity | Stores |
|---|---|
| `SubmissionChallengeResult` | Per-challenge score (0–100) |
| `SubmissionFieldResult` / `Method` / `Constructor` / `Relation` | Element match outcomes |
| `SubmissionTestcaseResult` | Testcase rollup + primary `input_display` / `expected_display` / `actual_display` |
| `SubmissionTestcaseAssertionResult` | Per-assertion status, `actual_value` JSONB, feedback |

### Upload `lab_result` bundle

Keyed `challenge_<N>`. Each bundle contains `class`, `mmd`, `testcases` (operational I/O cards; hidden rows omit display strings), `scores: { class, mmd, testcase, total }`, and `scoreApplicability`. Upload assemble maps `ChallengeRubric` + snapshot + correct ids (`ClassStructureService.buildClassDataFromRubric` / `buildMmdDataFromRubric`); it does not reload class/member/relation rows from Neon. When `mmdApplicable` or `testcaseApplicable` is false, that tree is empty (`mmd.classes: []` or `testcases: []`) and the corresponding applicability flag is false. GET `/class` `/mmd` `/testcases` use the same from-rubric mappers after `SubmissionDetailPersistGate.await` (`LabRubricCache.get(labId)` + `challengeById`). Student GET and upload `lab_result` pass `DisclosureMode.STUDENT` (generic placeholders when snapshot missing); lecturer drawer passes `DisclosureMode.LECTURER`. Revisit reads use `GET /api/labs/{labId}/challenges/{challengeId}/testcases` with the same payload shape.

## Work Guidance

- Parsed classes come from `ReflectionClassParser.parseClasses(classesDir)` only; loads top-level and one-level nested (`Outer$Inner`) classes; rubric nested entries match by qualified name (`Outer.Inner`) via `ClassRubric.qualifiedName()`; nested rubric rows may set `is_static` to grade static nested vs non-static inner
- Upload `lab_result` assemble and student GET Class/MMD/Testcase tabs must not call `loadChallengeStructures`; lecturer structure GET still uses the JPA bundle load
- Do not grade source `.java` files directly; compilation must succeed first
- Inheritance and realization (`class_relation`) also feed the Java class shell via declared superclass/interfaces; other relation kinds stay MMD-only. Java grades a set Extends/Implements pair even when `has_mmd=false`
- **MMD member syntax:** Mermaid `$` (static) and `*` (abstract) suffixes on fields/methods; leading `static` keyword; parameters accept `int yearModel`, `message String`, and `message: String`; package visibility `~`; colon form (`ClassName : +type field`) equivalent to block members; `class Name["Label"]` uses `Name` as the identifier; method return types accept UML colon (`method(): Type`) and Mermaid space (`method() Type`) as equivalent; glued returns (`method()Type`) are a parse error; omitted return after `()` is `void`; classifiers may sit between `)` and the return type (`method()*: Type`, `method()$ Type`); `List~T~` and `List<T>` compare equivalently via `MmdTypeEquivalence`
- **MMD parser pipeline:** `MmdParser` delegates to `grading/mmd/` tokenizer + AST + mapper; substantive diagrams require a `classDiagram` header line; `namespace { ... }` blocks flatten contained classes under simple names; `ParsedMmdDiagram.classByName` includes qualified aliases (`Company.Employee`)
- **MMD cosmetic directives:** `note`, `note for`, `direction`, `style`, `classDef`, and `cssClass` lines parse as ignored directives (no grading impact)
- **MMD relations:** optional Mermaid labels after ` : ` (e.g. `A o--> B : wraps`); optional quoted cardinality on each endpoint (parsed, not graded); aggregation arrows include `o-->` / `--o>` (diamond-side class is relation source); realization/implementation arrows `..|>`, `<|..`, and lollipop `()--` / `--()` are equivalent (implementor → interface); dashed link `..` canonicalizes to `dashed_link`; two-way `<|--|>` canonicalizes to `bidirectional_inheritance`; UI displays canonical realization as **implementation**
- **MMD comparison** resolves class names by simple or qualified key; undirected relation kinds (`link`, `dashed_link`, bidirectional variants) match either endpoint order; `<<Abstract>>` diagram stereotype satisfies rubric `CLASS` declaring type; `<<enum>>`, `<<enumerate>>`, and `<<enumeration>>` satisfy rubric `ENUM` declaring type
- **MMD parse errors:** `MmdPillarGrader` captures `MmdParseException` message on `MmdPillarResult.parseError`; persisted in `SubmissionMmdMetaStore.ChallengeMmdMeta.parseError`; exposed as `{ classes, parseError }` on `GET .../mmd` and `lab_result.challenge_N.mmd`
- **MMD method comparison** checks scope, return type, parameter types, and rubric `static` / `abstract` / `final` flags when required (extra diagram markers are ignored when the rubric does not require them); methods inside `<<interface>>` blocks count as abstract when the rubric requires it
- **MMD types** treat primitive names and wrappers as equivalent (`double` ≡ `Double`)
- Rubric writers must call `RubricCacheInvalidationSupport.invalidateLab(labId)` after mutations (structure save, testcase save)
- Lecturer dry-run reuses `TestcaseGrader.gradeSingle()` against a temp compile dir; mixed reference javac is a preview (`ERROR` if the testcase touches a failed type), not HTTP 422; does not write `submission_*` rows
- Mixed javac fills `ChallengeGradingContext.failedClassNames` and `compileErrorsByClassName`; `compileError` is catastrophic I/O/setup only
- Operator-run SQL migrations live in `docs/sql/` (no Flyway)
- With `app.grading.timing-log=true` (on in local `application.properties`), print aligned `[timing]` blocks via `TimingLog`: per challenge (`parse`, `class`, `mmd`, `testcase`, `score`, `total`); grade submission (`load existing`, `compute`, `assemble`, `total`); upload (`access`, `rubric`, `compile`, `grade`, `persist`, `plagiarism`, `total`)

## Verification

- Tests under `backend/src/test/java/unit/com/eiu/capstone/backend/grading/`: `PillarScoreAggregatorTest`, `PartialCreditEvaluatorTest`, `TestcaseGraderTest`, `TestcaseResultMapperTest`, `InvocationRunnerTest`, `GradingServiceTest`, `LabResultAssemblerTest`, `MmdParserTest`, `MmdComparisonServiceTest`, `MmdPillarGraderTest`, `MmdTokenizerTest`, `MmdAstParserHeaderTest`, `MmdRelationParseTest`, `MmdMemberParseTest`, `MmdMiscDirectiveTest`, `MmdReferenceDocMatrixTest`, `ClassReflectionGraderTest`, `ReflectionClassParserTest`
- Manual: upload lab folder; confirm populated `testcases` in `lab_result` and on revisit `/testcases` endpoint

## Child DOX Index

No child docs. All grading code lives in this package.
