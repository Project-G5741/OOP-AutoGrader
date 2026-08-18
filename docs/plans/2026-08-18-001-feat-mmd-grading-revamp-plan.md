---
title: "MMD Grading Revamp - Plan"
date: 2026-08-18
type: feat
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# MMD Grading Revamp - Plan

## Goal Capsule

**Objective:** Replace the MMD parser with a tokenizer-and-AST implementation that covers the full Mermaid `classDiagram` syntax contract in `grading-mermaid-oop-class-diagrams.md`, wire comparison to consume the richer parse output, and surface clear syntax-error feedback on the student MMD tab while keeping rubric-matching as the score source within the existing three-pillar model.

**Product authority:** Session brainstorm decisions (2026-08-18). Supersedes parser and comparison assumptions in `docs/plans/2026-08-05-001-feat-mmd-class-diagram-grading-plan.md` where they conflict with the reference doc.

**Open blockers:** None.

---

## Product Contract

### Summary

Students submit Mermaid class diagrams that today fail to parse when they use valid syntax from the course reference (implicit classes, colon member lines, labels, standalone stereotypes, and more). This revamp rewrites parsing around a structured grammar, updates comparison to use the expanded model, keeps parse-failure scoring as all-wrong, and shows the parse error on the MMD tab. Lecturer rubrics may be migrated where breaking changes require it.

### Problem Frame

The current `MmdParser` is a line-oriented regex parser that only recognizes `class Name { ... }` blocks and relationship lines. It ignores `classDiagram` structure, colon-syntax members, implicit classes created by relationships, class display labels, standalone stereotype declarations, namespaces, lollipop interface notation, package visibility (`~`), and cardinality tokens. When parsing throws, `MmdPillarGrader` marks every MMD rubric element incorrect but discards the error message — students see generic failures with no syntax guidance. The course reference doc now defines the authoritative syntax and OOP mapping lecturers expect; the autograder must parse that surface completely before rubric comparison can be trustworthy.

### Actors

- A1. **Student** — uploads `.mmd` per challenge; views MMD tab scores, per-element pass/fail, and syntax-error banner when the diagram cannot be parsed.
- A2. **Lecturer** — authors rubric diagrams in the structure editor; may need rubric MMD files updated after migration.
- A3. **Backend grading pipeline** — parses in memory on upload, compares against rubric snapshot, persists outcomes for revisit reads and `lab_result` bundle.

### Key Decisions

- **Rubric-matching stays the score source** (session-settled: user-directed — chosen over OOP-concept linting and hybrid diagnostics: lecturers define the expected diagram; the reference doc governs syntax, not independent semantic linting).
- **AST parser rewrite** (session-settled: user-directed — chosen over evolving the line parser and over an external Mermaid validator: full reference-doc coverage with a maintainable grammar and a comprehensive test matrix).
- **Parse failure → all rubric elements wrong** (session-settled: user-approved — chosen over partial parse and syntax-weight scoring: preserves today's scoring contract; adds frontend error display only). Governs R14, R15.
- **Parse-only feedback enrichment** (session-settled: user-directed — chosen over relation-level and full OOP-aware mismatch messages: scope stays on parser correctness; existing rubric mismatch labels remain). Governs R15, R16.
- **Breaking parser/comparison changes acceptable with rubric migration** (session-settled: user-directed — chosen over strict backward compatibility: enables full alignment with the reference doc). Governs R17, R18.
- **Success = comprehensive reference-doc parser test suite** (session-settled: user-directed — chosen over real-submission regression set and manual spot-check: explicit, automatable done signal). Governs R19.

### Requirements

**Parser — syntax contract**

The parser's authoritative syntax contract is `grading-mermaid-oop-class-diagrams.md`. R1–R13 state this work's commitments; the reference doc owns the full rule text.

- R1. Require and accept a `classDiagram` header line; reject or flag diagrams that omit it or mix foreign diagram types.
- R2. Parse explicit class declarations (`class Animal`) and implicit classes introduced only through relationships (`Vehicle <|-- Car` defines both endpoints).
- R3. Parse class display labels (`class Person["Person Entity"]`) and use the identifier (`Person`) for cross-reference matching.
- R4. Parse members from both block form (`class X { ... }`) and colon form (`X : +double balance`) as equivalent inputs.
- R5. Classify members as attribute vs method by presence of `()` only, per reference §1.3.
- R6. Parse visibility modifiers `+`, `-`, `#`, and `~`; treat missing visibility as unspecified, not public.
- R7. Parse method return types after `)` with required separating space; parse abstract (`*`) and static (`$`) classifiers on members; parse `<<Abstract>>`, `<<Interface>>`, `<<Enumeration>>`, and related stereotype annotations in block, standalone, and nested forms.
- R8. Parse tilde generics (`List~Employee~`, nested tildes) and normalize angle-bracket generics (`List<Employee>`) to equivalent internal representation for type comparison.
- R9. Parse all eight relationship types from reference §1.6 with correct arrow canonicalization: inheritance, composition, aggregation, association, link (solid), dependency, realization, link (dashed).
- R10. Preserve relationship direction per reference §1.6 (arrowhead end determines parent/interface/whole side, not left-to-right reading order).
- R11. Strip optional relationship labels after ` : ` without treating label text as graded content.
- R12. Parse cardinality tokens in quotes on relationship ends; store parsed multiplicity for potential future use but do not grade multiplicity in this revamp.
- R13. Parse namespaces, lollipop interface notation, two-way relation shorthand, notes, `direction` lines, and `%%` comments per reference §1.8–§1.12; ignore cosmetic `style` / `classDef` / `cssClass` directives without error.

**Comparison consumption**

- R14. When parsing succeeds, comparison must resolve rubric classes against implicitly defined classes, labeled classes, and namespace-qualified names using the same name-resolution rules for student submissions and lecturer rubrics.
- R15. On `MmdParseException` (or equivalent fatal parse failure), mark all MMD-applicable rubric elements incorrect; do not attempt partial grading of successfully parsed fragments.
- R16. Preserve existing rubric-mismatch feedback strings for successfully parsed diagrams (e.g. "Relation mismatch", "Missing relationship"); do not add OOP-concept diagnostic categories beyond syntax failure in this revamp.

**Student-facing parse error**

- R17. Persist a human-readable parse-error message per challenge submission when parsing fails, surviving ephemeral upload-folder cleanup (same durability pattern as other submission-sidecar stores).
- R18. Expose the parse-error message through the MMD read path (`GET .../mmd` and upload-time `lab_result` bundle) so the student MMD tab can show a prominent syntax-error banner without an extra round trip.
- R19. When no parse error exists, the MMD tab behaves as today (class cards, attribute ticks, relations table).

**Rubric migration**

- R20. Audit lecturer reference `.mmd` files and in-database rubric diagrams against the new parser; update any that fail to parse or grade differently under the new rules.
- R21. Document migration outcomes so lecturers know which challenges were touched.

**Verification**

- R22. Add a comprehensive automated parser test matrix with one or more focused cases per syntax subsection in `grading-mermaid-oop-class-diagrams.md` (§1.1–§1.12 minimum).
- R23. Extend comparison integration tests to cover at least implicit-class matching, colon-syntax equivalence with block form, and reversed-direction relation parsing.

**Scoring model (unchanged)**

- R24. MMD remains an independent grading pillar; challenge score is the mean of applicable pillars (`has_mmd`, testcase presence). No reintroduction of Java-and-MMD AND-merge at element level.
- R25. `has_mmd=false` challenges continue to omit the MMD pillar and hide the MMD tab.

### Key Flows

**F1. Successful upload with valid syntax**

Student uploads lab folder → grading pipeline parses `.mmd` via new AST parser → `MmdComparisonService` matches rubric elements → pillar score computed → `lab_result` includes MMD data with no `parseError` → student MMD tab shows scores and element results.

**F2. Upload with unparseable syntax**

Student uploads `.mmd` with syntax the parser rejects → parser records specific error → all MMD rubric elements marked incorrect → pillar score reflects all-wrong → `lab_result` and persisted read path include `parseError` message → student MMD tab shows syntax-error banner plus existing empty/low-score states.

**F3. Lecturer rubric migration**

Revamp lands → audit script or manual pass runs against stored rubric MMD → failing diagrams updated to parse under new grammar → structure save invalidates rubric cache → re-grade sample submissions to confirm expected scores.

### Scope Boundaries

**In scope**

- AST-based `MmdParser` replacement and comparison updates needed to consume its output.
- Parse-error persistence and student MMD tab display.
- Lecturer rubric migration for breaking changes.
- Parser and comparison automated tests per R22–R23.

**Out of scope**

- OOP-concept linting independent of rubric rows (composition-vs-aggregation coaching without a rubric relation to match).
- Richer per-element feedback for successfully parsed diagrams (wrong arrow type, reversed inheritance direction, encapsulation coaching).
- Multiplicity/cardinality grading (parsing per R12 is in scope; scoring multiplicity is deferred).
- Visual layout or styling grading.
- Changes to Java reflection pillar, operational testcase pillar, or three-pillar aggregation formula.
- Frontend redesign of the MMD tab beyond the syntax-error banner.

### Acceptance Examples

**AE1. Implicit class via inheritance** — Given rubric relation `Animal <|-- Dog` and student diagram containing only `Animal <|-- Dog` with no explicit `class` blocks, parser produces both classes and comparison marks the inheritance relation correct.

**AE2. Colon-syntax member equivalence** — Given rubric class `BankAccount` with block-form `+double balance`, a student submission using `BankAccount : +double balance` parses to the same member and grades identically.

**AE3. Parse error surfaced** — Given student diagram `classDiagram` followed by `class Bad Name { }` (space in bare name), parser fails with a descriptive error, all MMD elements score wrong, and student MMD tab shows the error message in a visible banner.

**AE4. Angle-bracket generics normalized** — Given rubric field type `List~Member~`, student field `List<Member>` parses and compares as equivalent.

**AE5. Realization direction** — Given rubric `Report ..|> Printable`, student `Report ..|> Printable` and equivalent `<|..` form both parse with implementor at the correct end and grade as realization/implementation match.

**AE6. Unchanged pillar isolation** — Challenge with `has_mmd=true` and a parse failure scores 0% on MMD pillar but does not alter class-reflection or testcase pillar scores.

### Outstanding Questions

None — all items classified during brainstorm.

---

## Planning Contract

### Summary

Replace the line-oriented `MmdParser` with an internal tokenizer + AST pipeline under `backend/src/main/java/com/eiu/capstone/backend/grading/mmd/`, keep `ParsedMmdDiagram` as the comparison boundary, extend `SubmissionMmdMetaStore` with `parseError`, thread the error through grading → `lab_result` → `/mmd` response, and add a student MMD tab banner. Lecturer drawer already has `mmdError` UI — wire it to the same API field.

### Technical Design

**Parser architecture**

```
MmdParser (facade, @Component)
  → MmdTokenizer (char stream → tokens)
  → MmdAstParser (tokens → MmdDiagramAst)
  → MmdAstToParsedMapper (AST → ParsedMmdDiagram)
```

- Keep existing DTOs (`ParsedMmdClass`, `ParsedMmdRelation`, inner `ParsedField`/`ParsedMethod`/`ParsedConstructor`) so `MmdComparisonService` changes stay localized.
- AST nodes live in `grading/mmd/ast/` (e.g. `ClassNode`, `MemberNode`, `RelationNode`, `NamespaceNode`, `StereotypeNode`).
- `MmdParseException` carries line/column when available for student-facing messages.
- `classDiagram` header: warn-only vs hard-fail — **hard-fail** when absent (R1); diagrams without header today may parse accidentally; migration pass catches lecturer rubrics.

**Relationship direction**

Retain `MmdComparisonService.relationMatches` endpoint rules from `backend/src/main/java/com/eiu/capstone/backend/grading/AGENTS.md`: symbol-side source, `link` allows reversed endpoints, realization `..|>` / `<|..` equivalent. AST layer must canonicalize arrows to the same `canonicalRelationType` names the comparison service already expects.

**Parse error persistence**

Extend `SubmissionMmdMetaStore.ChallengeMmdMeta`:

```java
public String parseError; // null when parse succeeded or no file
```

Write path: `GradingService` / `SubmissionController` after `MmdPillarGrader.grade()` — when `MmdParseException` caught, store `ex.getMessage()`. Read path: `ClassStructureService.buildMmdData` and `LabResultAssembler` pass `parseError` outward.

**API shape**

Option A (chosen): wrap `/mmd` response when error present:

```json
{ "classes": [...], "parseError": "Unclosed class block at line 12" }
```

Today `/mmd` returns a bare array. Change to object wrapper **only when** `parseError` is non-null, OR always return object for consistency. **Recommendation: always return object** `{ "classes": [], "parseError": null }` to avoid frontend type branching — breaking change for consumers; update `StudentDashboard`, `LecturerSubmissionDrawer`, and `applyBundleToState` in one pass.

**Frontend**

- `StudentUI.jsx` MMD tab: show `text-warning-text` banner above class cards when `parseError` set (mirror `MmdScoreBreakdown` lecturer pattern).
- `StudentDashboard.jsx`: parse `/mmd` object shape; pass `parseError` to `StudentUI`.
- Post-upload `lab_result` bundle: add `mmdParseError` (or nested under challenge bundle) — align field name with `/mmd` response (`parseError`).

**Rubric migration**

- Grep lecturer structure payloads / seed data for embedded MMD text (structure save stores relations in DB, not always raw `.mmd` files).
- Run new parser against any stored reference diagram text in test fixtures and `MmdParserTest` samples.
- Log challenges whose lecturer diagrams fail parse under new grammar.

### Key Technical Decisions

- KTD1. **AST under `grading/mmd/` with facade `MmdParser`** — preserves Spring injection point and test entry; comparison layer unchanged at type boundaries. Governs U1–U4.
- KTD2. **Always-object `/mmd` response** — `{ classes, parseError }` over conditional array/object. Governs U7, U8. (session-settled: planning default — cleaner contract than dual shapes.)
- KTD3. **Extend `SubmissionMmdMetaStore` for parse errors** — same durability model as `classStereotypeCorrect` / `relationErrors`; no new table. Governs U6.
- KTD4. **Multiplicity parsed, not graded** — store on `ParsedMmdRelation` optional fields; comparison ignores. Governs U2, U5.
- KTD5. **Delete line-parser internals** — no parallel legacy parser; tests define migration expectations. Governs U10, U11.

### Assumptions

- At most one meaningful `.mmd` per challenge folder; first filename case-insensitively (unchanged).
- Lecturer rubric diagrams use the same Mermaid subset as students; migration updates DB-stored structure, not student uploads.
- `MmdTypeEquivalence` primitive/wrapper rules remain (`double` ≡ `Double`); angle-bracket normalization is additive (R8).

### Sequencing

1. U1–U4 (parser) before U5 (comparison) before U6–U7 (persistence/API) before U8–U9 (frontend) in parallel where possible.
2. U10 test matrix grows alongside U1–U4 (test-first per syntax section).
3. U11 migration after parser + comparison stable.

---

## Implementation Units

### U1. Tokenizer and AST skeleton

**Goal:** Char-level tokenizer and empty AST builder; `classDiagram` header validation.

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/grading/mmd/MmdTokenizer.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/grading/mmd/MmdToken.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/grading/mmd/MmdAstParser.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/grading/mmd/ast/MmdDiagramAst.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/grading/MmdParser.java` (refactor to delegate)

**Test file:** `backend/src/test/java/com/eiu/capstone/backend/grading/mmd/MmdTokenizerTest.java`, `MmdAstParserHeaderTest.java`

**Scenarios:**
- Valid `classDiagram` line accepted.
- Missing header throws `MmdParseException` with clear message.
- `%%` comment lines skipped.
- Empty diagram returns empty AST.

### U2. Relationships and implicit classes

**Goal:** Parse all eight relationship types, labels, cardinality tokens, implicit class registration.

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/grading/mmd/ast/RelationNode.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/grading/mmd/MmdAstParser.java` (extend)
- `backend/src/main/java/com/eiu/capstone/backend/grading/ParsedMmdRelation.java` (add optional multiplicity fields)
- `backend/src/main/java/com/eiu/capstone/backend/grading/mmd/MmdAstToParsedMapper.java` (new)

**Test file:** `backend/src/test/java/com/eiu/capstone/backend/grading/mmd/MmdRelationParseTest.java`

**Scenarios:**
- `Animal <|-- Dog` creates classes `Animal`, `Dog` and inheritance relation with correct direction.
- Each arrow variant in reference §1.6 canonicalizes correctly.
- `Company "1" --> "1..*" Employee : employs` strips label; stores cardinality.
- Lollipop `()--` notation parses as realization.
- Two-way `Animal "many" <|--|> "many" Zebra` parses without error.

### U3. Class blocks, colon members, labels, stereotypes

**Goal:** Full member parsing per reference §1.1–§1.7.

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/grading/mmd/ast/ClassNode.java`, `MemberNode.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/grading/mmd/MmdAstParser.java` (extend)
- `backend/src/main/java/com/eiu/capstone/backend/grading/mmd/MmdAstToParsedMapper.java` (extend)
- `backend/src/main/java/com/eiu/capstone/backend/grading/MmdTypeEquivalence.java` (angle-bracket normalization)

**Test file:** `backend/src/test/java/com/eiu/capstone/backend/grading/mmd/MmdMemberParseTest.java`

**Scenarios:**
- Block and colon syntax produce identical `ParsedMmdClass` members.
- `class Person["Person Entity"]` uses `Person` as name.
- Visibility `+ - # ~` mapped to scope strings.
- `+getBalance() double` vs missing space `getBalance()double` — latter fails or recovers per reference (fail preferred).
- `List~Employee~` and `List<Employee>` normalize equivalently.
- `<<Interface>>` standalone and in-block forms set `stereotypeType`.
- Abstract method `+draw()* void` and static `+field$` parsed.

### U4. Namespaces, notes, direction, cosmetic directives

**Goal:** Parse without error; namespaces group classes for name resolution.

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/grading/mmd/ast/NamespaceNode.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/grading/mmd/MmdAstParser.java` (extend)

**Test file:** `backend/src/test/java/com/eiu/capstone/backend/grading/mmd/MmdMiscDirectiveTest.java`

**Scenarios:**
- `namespace Company { class Employee }` registers `Employee` (optionally qualified).
- `note for ClassName "text"` and `direction TB` ignored for grading.
- `style` / `classDef` lines ignored.

### U5. Comparison updates for expanded parse model

**Goal:** `MmdComparisonService` resolves implicit classes and stereotype variants.

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/grading/MmdComparisonService.java`
- `backend/src/main/java/com/eiu/capstone/backend/grading/MmdParser.java` (facade finalize)

**Test file:** `backend/src/test/java/com/eiu/capstone/backend/grading/MmdComparisonServiceTest.java` (new or extend `MmdParserTest`)

**Scenarios:**
- AE1, AE2, AE4, AE5 from Product Contract pass end-to-end.
- Class only in diagram via implicit relation satisfies `setClassPresent`.
- `<<Abstract>>` stereotype maps to rubric `CLASS` declaring type (existing behavior) unless rubric expects abstract marker — document in migration.

### U6. Parse error capture in grading pipeline

**Goal:** Propagate `MmdParseException` message to durable store.

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/grading/pipeline/MmdPillarGrader.java` (return optional `parseError` on result record)
- `backend/src/main/java/com/eiu/capstone/backend/service/SubmissionMmdMetaStore.java`
- `backend/src/main/java/com/eiu/capstone/backend/grading/GradingService.java` or `SubmissionController.java` (write meta)

**Test file:** `backend/src/test/java/com/eiu/capstone/backend/grading/pipeline/MmdPillarGraderTest.java` (new)

**Scenarios:**
- Parse failure sets `parseError` on meta; success leaves null.
- `MmdPillarResult` exposes error for assembler.

### U7. API and lab_result exposure

**Goal:** `{ classes, parseError }` on `/mmd` and upload bundle.

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/DTO/MmdResponseDTO.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/DTO/ChallengeDetailBundleDTO.java`
- `backend/src/main/java/com/eiu/capstone/backend/service/ClassStructureService.java`
- `backend/src/main/java/com/eiu/capstone/backend/grading/LabResultAssembler.java`
- `backend/src/main/java/com/eiu/capstone/backend/controller/ChallengeController.java`

**Test file:** controller or service integration test for `/mmd` shape

**Scenarios:**
- Successful parse: `{ "classes": [...], "parseError": null }`.
- Failed parse: `{ "classes": [...], "parseError": "..." }` with all elements marked wrong in classes array.
- Upload `lab_result.challenge_N` includes same `parseError`.

### U8. Student frontend parse error banner

**Goal:** Display syntax error on student MMD tab.

**Files:**
- `frontend/src/pages/StudentDashboard.jsx`
- `frontend/src/components/student/StudentUI.jsx`
- `frontend/src/components/student/AGENTS.md` (contract note)

**Scenarios:**
- Upload with bad syntax shows banner immediately from `lab_result`.
- Revisit fetch shows banner from `/mmd` object.
- No banner when `parseError` null.

### U9. Lecturer drawer parse error wiring

**Goal:** Show parser error (not just HTTP error) in lecturer MMD breakdown.

**Files:**
- `frontend/src/components/lecturer/LecturerSubmissionDrawer.jsx`
- `frontend/src/components/lecturer/MmdScoreBreakdown.jsx` (may already handle `mmdError`)

**Scenarios:**
- Lecturer View drawer shows parse error text when student submission failed parse.

### U10. Reference-doc test matrix

**Goal:** One test class per reference section; parametrized where helpful.

**Files:**
- `backend/src/test/java/com/eiu/capstone/backend/grading/mmd/MmdReferenceDocMatrixTest.java` (new)
- Retire or refactor overlapping cases in `MmdParserTest.java`

**Scenarios:**
- Parametrized cases for §1.1–§1.12 covering happy path and at least one failure mode per subsection.
- CI: `mvn test -Dtest=MmdReferenceDocMatrixTest,Mmd*Test` passes.

### U11. Rubric migration pass

**Goal:** Ensure lecturer diagrams parse under new grammar.

**Files:**
- `docs/plans/mmd-rubric-migration-log.md` (new, migration outcomes)
- Any lecturer seed/fixture MMD strings found in tests or structure samples

**Scenarios:**
- All in-repo rubric diagram samples parse without error.
- Documented list of DB-only diagrams requiring manual lecturer update (if any).

---

## Verification Contract

**Primary command:**

```bash
cd backend && mvn test -Dtest=MmdReferenceDocMatrixTest,MmdTokenizerTest,MmdRelationParseTest,MmdMemberParseTest,MmdMiscDirectiveTest,MmdParserTest,MmdComparisonServiceTest,MmdPillarGraderTest
```

**Frontend:**

```bash
cd frontend && npm run build
```

**Manual smoke:**
1. Upload lab with valid `.mmd` — MMD tab scores unchanged behavior, no error banner.
2. Upload lab with `class Bad Name` — MMD tab shows parse error banner; MMD pillar 0%.
3. Lecturer drawer View on same student — parse error visible on MMD tab.

**Quality gates:**
- No new N+1 queries on `/mmd` read path.
- `backend/src/main/java/com/eiu/capstone/backend/grading/AGENTS.md` updated for parser package layout and `/mmd` response shape.

---

## Definition of Done

**Global:**
- [ ] All R1–R25 requirements satisfied or explicitly deferred with doc note.
- [ ] Reference-doc test matrix (R22) green in CI.
- [ ] Student and lecturer MMD views show `parseError` when parser fails (R17–R18).
- [ ] Three-pillar scoring unchanged (R24–R25).
- [ ] `grading/AGENTS.md` and `CONCEPTS.md` reflect new parser and parse-error contract.

**Per unit:**
- [ ] U1–U4: Parser covers full `grading-mermaid-oop-class-diagrams.md` syntax surface.
- [ ] U5: Comparison integration tests pass (R23).
- [ ] U6–U7: Parse error survives upload cleanup and appears in API + `lab_result`.
- [ ] U8–U9: Frontend build passes; manual smoke complete.
- [ ] U10: Matrix test file committed with §1.x coverage map in class Javadoc.
- [ ] U11: Migration log written; no known lecturer diagram fails parse in repo fixtures.

