---
title: "MMD Class Diagram Grading - Plan"
date: 2026-08-05
type: feat
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
product_contract_preservation: unchanged
---

# MMD Class Diagram Grading - Plan

## Goal Capsule

**Objective:** Grade uploaded Mermaid class diagrams (`.mmd`) against the reference rubric, reuse the existing entity model (Class, Field, Constructor, Method, ClassRelation), merge results into one unified challenge score, and surface pass/fail feedback on the student MMD tab.

**Product authority:** Session brainstorm decisions (2026-08-05). Supersedes the prior stop condition in `docs/plans/2026-08-04-001-feat-challenge-result-upload-plan.md` that kept MMD grading and tab data out of scope.

**Open blockers:** None.

---

## Product Contract

### Summary

Parse each challenge's `.mmd` file into the same rubric entities used for Java reflection grading, compare against the solution using shared rules, and persist outcomes for `GET /mmd`. Challenge percentage is one unified score: class stereotypes, members, and relations each count as rubric elements; when both `.java` and `.mmd` are present, an element must pass in **both** sources to count correct. Missing or unparseable `.mmd` marks all MMD-gradable items wrong. Frontend fetches `/mmd` alongside `/class` on StudentDashboard.

### Problem Frame

Students upload both Java source and Mermaid diagrams per challenge, but only Java is graded today. `ClassStructureService.getMmdData` returns an empty list, the frontend never calls `/mmd`, and `ClassRelation` rows in the rubric are never compared. The MMD tab shows placeholder relation data. Lecturers expect diagram correctness (stereotypes, members, relationships) to affect the same challenge score students see in the sidebar.

### Actors

- A1. **Student** — uploads `.java` + `.mmd`, views MMD tab scores and per-element pass/fail for the latest attempt.
- A2. **Backend grading pipeline** — parses `.mmd` in memory during upload, grades against the rubric snapshot, persists results, serves `/mmd` read API.

### Requirements

**Parsing and comparison**

- R1. Parse a Mermaid `class ClassName { ... }` block into a class rubric match candidate. Class name comes from the declaration line.
- R2. Detect class type from the first stereotype line inside the body: `<<enumerate>>` → Enum, `<<interface>>` → Interface, no stereotype → regular Class. Mismatch against the solution's declaring type marks the class type wrong.
- R3. A solution plain class with no stereotype but a submission stereotype (e.g. `<<interface>>`) is a class-level type error.
- R4. Parse field lines (`- name: String`) into field candidates. Visibility symbol (`-`, `+`, `#`) is required and maps to private, public, protected. Missing symbol marks the field wrong.
- R5. Field name match is case-insensitive; field data type match is case-sensitive (`String` ≠ `string`).
- R6. Generic/collection equivalence applies to field types and method return types: `List~Member~` ≡ `List<Member>` ≡ `ArrayList<Member>`; `HashMap~String, int~` ≡ `HashMap<String, Integer>`. Primitive/wrapper pairs (`int` vs `Integer`) are **not** equivalent outside the documented generic normalization.
- R7. Extra fields in the submission not present in the solution are ignored (not penalized).
- R8. Field order does not affect grading.
- R9. Parse constructor lines (`+ Customer()` or `+ Booking(Customer customer, Session session)`). Constructor name must exactly match the class name. Every solution constructor signature (parameter types) must have a matching submission constructor. Parameter types must match. Extra submission constructors are ignored.
- R10. Parse method lines (`+ getTotalPriceAfterTax() double`). Parameter names are case-insensitive; parameter types and count must match. Return type must match with the same case-sensitivity and generic-equivalence rules as R6.
- R11. Getter/setter shorthand (`+ getter()`, `+ setter()`) satisfies the accessor family: if the class has at least one getter in the solution, one `getter()` in the submission is enough; if none, ignore; same rule for setter.
- R12. Parse relationship lines using the supported Mermaid syntax table (inheritance, composition, aggregation, association, bidirectional association, undirected link, dependency, realization). Relation labels and multiplicities are not graded.
- R13. For directional relationships, the class on the **same side as the relationship symbol** is the target (e.g. `Booking *-- Session` → Booking is target). For undirected `--`, either endpoint may match the solution's target when comparing.
- R14. Relationship type mismatch (e.g. solution Composition, submission Aggregation) is a full miss, not partial credit.

**Scoring integration**

- R15. Challenge percentage uses one unified denominator: all rubric elements — classes (type), fields, constructors, methods, and class relations.
- R16. When both `.java` and `.mmd` are present for a challenge, a rubric element counts correct only if it passes in **both** Java reflection grading and MMD grading.
- R17. When `.mmd` is absent for a challenge, every MMD-gradable rubric element (class stereotypes, relations, and members checked via diagram) counts as incorrect.
- R18. When `.mmd` is present but cannot be parsed, every MMD-gradable rubric element for that challenge counts as incorrect; the upload continues (no 422 rejection).
- R19. Lab overall score remains the simple average of graded challenge percentages (unchanged formula).

**Persistence and API**

- R20. Extend the rubric snapshot to include `ClassRelation` rows for each challenge (batched load, no per-relation query in the grading loop).
- R21. Persist MMD grading outcomes so `GET /api/labs/{labId}/challenges/{challengeId}/mmd?studentId=` can be served from stored results for the latest attempt, following the same batched-read pattern as the Class tab (`SubmissionResultLoader`-style, no N+1).
- R22. `MmdClassDTO` includes class boxes with attribute rows (`MmdAttributeDTO`: field, constructor, method) and per-class `relations` arrays (`from`, `to`, `relType`, `ok`, optional `error`) matching what `StudentUI` already renders.
- R23. Parse `.mmd` from the in-memory upload payload (`mmdByChallenge`); do not require disk persistence on the hot path.

**Frontend**

- R24. `StudentDashboard` fetches `/mmd?studentId=` when loading challenge details (parallel to `/class`), caches per challenge, and clears cache on upload for affected challenges.
- R25. Remove mock/placeholder relation data from `StudentUI` when real `/mmd` data is available; empty state when no submission data exists.
- R26. MMD tab score pill and relations score pill derive from returned `mmdData` (attributes and relations), not hardcoded samples.

**Performance**

- R27. MMD grading runs in the existing upload grading path without introducing N+1 database queries (batch rubric load, batch result save, batch result read).

### Key Flows

- F1. **Upload with both files** — Student drops `challenge_N` folder with `.java` and `.mmd` → backend compiles Java, parses MMD in memory, grades both against rubric snapshot, merges with both-must-pass rule, persists all results, returns updated challenge scores → frontend refreshes sidebar and fetches `/class` + `/mmd` for selected challenge.
- F2. **Upload Java only** — No `.mmd` in folder → Java elements graded normally; all MMD-gradable elements marked wrong → challenge % reflects combined denominator per R15–R17.
- F3. **View MMD tab** — Student selects challenge with prior submission → parallel fetch `/class` and `/mmd` → MMD tab shows class boxes, attribute ticks, and relations table with live scores.
- F4. **Malformed MMD** — Student uploads syntactically invalid `.mmd` → upload succeeds; all MMD-gradable elements fail; Java grading unaffected.

### Acceptance Examples

- AE1. Solution has `class Customer` (plain) and submission tags `<<interface>>` → class type marked wrong on MMD tab; counts against unified score per R16.
- AE2. Solution field `- name: String`, submission `name: String` (no visibility) → field marked wrong.
- AE3. Solution `List<Member>`, submission `List~Member~` → field marked correct (generic equivalence per R6).
- AE4. Solution `int count`, submission `Integer count` → field marked wrong (`int` ≠ `Integer` per R6).
- AE5. Solution has `+ getter()` family with two getters; submission shows one `+ getter()` line → satisfies getter requirement per R11.
- AE6. Solution `Booking *-- Session` (Booking target), submission `Session *-- Booking` → relation marked wrong (direction/target mismatch per R13).
- AE7. Solution composition, submission aggregation between same classes → relation marked wrong (full miss per R14).
- AE8. Student uploads Java only → MMD tab shows all MMD elements failed; challenge score lower than Java-only reflection would produce.
- AE9. Student uploads valid `.mmd` + `.java` where both agree → element marked correct only when both sources pass; sidebar score reflects unified percentage.
- AE10. `GET /mmd` after upload returns class attributes and relations with `ok` flags; no extra `/class` call needed for MMD data shape.

### Key Decisions

- KD1. **Dual-parser, shared comparison** over a separate MMD-only grading service — `MmdParser` and `ReflectionClassParser` both produce comparable structures; shared comparison rules avoid drift. Governs R1–R14, R27.
- KD2. **Combined unified score with both-must-pass** over display-only MMD or separate averaging — one challenge % in the sidebar; stricter when both files present. Governs R15–R19. (session-settled: user chose combined + both must pass.)
- KD3. **Missing/unparseable MMD fails all MMD elements** over skip-or-fallback — incentivizes diagram submission. Governs R17–R18. (session-settled.)
- KD4. **Symbol-side is target** for directional relations — `Booking *-- Session` maps Booking as target. Governs R13. (session-settled.)
- KD5. **Keep separate `/mmd` and `/class` endpoints** — mirrors existing read pattern; frontend fetches both. Governs R21, R24.
- KD6. **Extra submission members ignored** — only solution-required elements are graded; extras do not deduct. Governs R7, R9.
- KD7. **Relation labels/multiplicities not graded** — out of current solution model scope. Governs R12.

### Scope Boundaries

**In scope:** MMD parser, rubric snapshot extension for relations, MMD grading in upload pipeline, result persistence, `/mmd` read path, StudentDashboard/StudentUI wiring, unified score merge.

**Deferred:**
- `.mmd` archival via `MmdPersistenceHook` (remains no-op)
- Testcase tab backend
- Lecturer dashboard MMD views
- Automated test suite
- Relation label/multiplicity grading

**Outside this product's identity:**
- Replacing `/mmd` and `/class` with a unified endpoint
- Changing JWT/auth model

### Dependencies and Assumptions

- Assumption: Each challenge folder contains at most one meaningful `.mmd` file; if multiple exist, planning chooses first-by-name or merge rule.
- Assumption: Solution rubric `ClassRelation` rows and `MasterData` relation types are populated for challenges that expect diagram relations.
- Assumption: Java reflection grading behavior for members (scope, return type case rules) remains the source of truth for the Java side of R16; MMD applies the stricter rules in R4–R6, R10 where they differ from reflection defaults.
- Dependency: Prior upload-response work (`challengeResult` scores, latest-attempt resolution) from `docs/plans/2026-08-04-001-feat-challenge-result-upload-plan.md` is in place.

### Outstanding Questions

- OQ1. **Resolved in planning:** Multiple `.mmd` files in one challenge folder — use the first file sorted by original multipart filename (case-insensitive).
- OQ2. **Resolved in planning:** Class stereotype/type is part of the existing single class-level rubric element (same denominator slot as Java `classAttributesMatch`); combined correct only when both Java class attributes and MMD stereotype pass.

### How This Work Fits Together

<!-- ce-section: work-relationships -->

This brainstorm owns MMD grading end-to-end (parse, grade, persist, display). It builds on the existing Java grading pipeline and the stub `/mmd` endpoint from the August 2026 upload-response work. Testcase backend, lecturer views, and `.mmd` archival remain separate future work.

---

## Planning Contract

### Summary

Add an `MmdParser` that reads `.mmd` bytes from the in-memory upload map, extend the rubric snapshot with `ClassRelation` rows, grade MMD elements in parallel with Java reflection inside `GradingService`, merge with AND semantics, persist relation results in a new `submission_relation_result` table, implement `ClassStructureService.getMmdData`, and wire frontend `/mmd` fetch. Reuse batched-query patterns from `LabRubricService` and `SubmissionResultLoader` to avoid N+1.

### Technical Design

**Pipeline change**

```
SubmissionController.upload
  → processUpload() → mmdByChallenge (in memory)
  → GradingService.gradeSubmission(submission, rubric, challenges, mmdByChallenge, skipExistingLoad)
       per challenge (parallel):
         Java: ReflectionClassParser → element correctness map
         MMD:  MmdParser → MmdComparisonService → element correctness map
         Merge: finalCorrect = javaCorrect && mmdCorrect (members + class)
                relations: mmdCorrect only (false when no/absent/unparseable .mmd)
         Score: correctElements / totalElements (includes relations)
  → GradingResultStore.save (field/method/constructor/relation/challenge)
```

**New backend types**

| Type | Location | Role |
|---|---|---|
| `MmdParser` | `backend/src/main/java/.../grading/MmdParser.java` | Line-oriented parse of class blocks + relationship lines |
| `ParsedMmdClass`, `ParsedMmdRelation` | `backend/src/main/java/.../grading/` | Intermediate parse DTOs (mirror `ParsedClass` pattern) |
| `MmdTypeEquivalence` | `backend/src/main/java/.../grading/MmdTypeEquivalence.java` | Normalize `List~T~` / generics; case-sensitive base types |
| `MmdComparisonService` | `backend/src/main/java/.../grading/MmdComparisonService.java` | Compare parsed MMD against rubric; getter/setter family logic |
| `RelationRubric` | `backend/src/main/java/.../grading/rubric/` | Immutable relation row in snapshot |
| `SubmissionRelationResult` | `backend/src/main/java/.../model/` | Persist relation pass/fail per submission |
| `MmdRelationDTO` | `backend/src/main/java/.../DTO/` | `{ from, to, relType, ok, error? }` for frontend |

**Rubric snapshot extension**

- Add `ClassRelationRepository.findByClassEntityInWithEndpoints(List<ClassEntity>)` with JOIN FETCH on `classEntity`, `targetClassEntity`, `relationType`.
- Extend `ChallengeRubric` with `List<RelationRubric>` (id, sourceClassName, targetClassName, relationTypeName).
- Load in `LabRubricService.loadForLab` in the same batched pass as fields/methods.

**Merge semantics (implements R15–R18)**

| Element | Java grade | MMD grade | Combined |
|---|---|---|---|
| Class (type/attributes) | `classAttributesMatch` | stereotype ↔ `declaringType` | `java && mmd` |
| Field / method / constructor | reflection match | diagram line match | `java && mmd` |
| ClassRelation | n/a (always passes Java) | relation line match | `mmd` only → false if no `.mmd` |

When `.mmd` absent or parse fails: set `mmdCorrect = false` for all MMD-gradable elements; Java grading unchanged.

**MMD read path**

- Extend `SubmissionResultLoader` → `SubmissionMmdCorrectIds` (fields, methods, constructors, relations, class-level flags).
- `ClassStructureService.getMmdData` mirrors `buildClassDataForSubmission`: one submission resolve, batched rubric load, one result load, assemble `MmdClassDTO` + nested `relations`.
- Map `MasterData` relation type names to frontend `relType` strings (`extends`, `composition`, etc.) via existing master data labels.

**Frontend**

- Add `mmdDataCacheRef` parallel to `classDataCacheRef` in `StudentDashboard.jsx`.
- Extract shared `fetchChallengeDetails(labId, challengeId)` that runs `/class` and `/mmd` in `Promise.all`.
- Remove hardcoded fallback relations array in `StudentUI.jsx` (lines 157–161); show empty relations section when `relationData.length === 0`.

### Key Technical Decisions

- KTD1. **In-memory MMD parse** over disk hook — `mmdByChallenge` already available at grade time; avoids `MmdPersistenceHook` changes. Governs U1, U4.
- KTD2. **`MmdComparisonService` separate from `MmdParser`** — parser is pure syntax; comparison encodes R4–R14 rules and generic equivalence. Governs U1, U2, U4.
- KTD3. **New `submission_relation_result` table** — no existing relation result entity; follow `SubmissionFieldResult` unique key pattern `(submission_id, class_relation_id)`. Governs U5.
- KTD4. **Merge inside `GradingService.gradeChallengeFolder`** — single score computation, single persistence pass; avoids split challenge percentages. Governs U4, U6.
- KTD5. **Case-sensitive types for MMD, case-insensitive for Java reflection** — product spec (R5–R6) overrides reflection's `equalsIgnoreCase` on the MMD side only. Governs U2.
- KTD6. **First `.mmd` by filename** when multiple present — resolves OQ1 without upload rejection.

### Assumptions

- `class_relation` rows and `MasterData` relation types exist in DB for challenges that need relation grading.
- No Flyway in repo — `submission_relation_result` DDL is applied externally (document SQL in unit U5).
- Repo has no automated tests; verification is manual per Verification Contract.

### Sequencing

U1 + U2 (parser + type utils) → U3 (rubric relations) → U4 + U5 + U6 (grading merge + persistence) → U7 + U8 (read API) → U9 + U10 (frontend).

---

## Implementation Units

### U1. MMD parser

**Goal:** Parse Mermaid class-diagram syntax from raw `.mmd` bytes into `ParsedMmdClass` / `ParsedMmdRelation` lists.

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/grading/MmdParser.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/grading/ParsedMmdClass.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/grading/ParsedMmdRelation.java` (new)

**Work:**
1. Parse `class Name { ... }` blocks; extract stereotype from first inner line (`<<enumerate>>`, `<<interface>>`).
2. Parse member lines: fields (`[-+#] name: Type`), constructors (`[-+#] Name(params)`), methods (`[-+#] name(params) ReturnType` or `getter()` / `setter()`).
3. Parse relationship lines outside class blocks using the R12 syntax table; record symbol-side class as target per R13.
4. Return empty lists on blank input; throw `MmdParseException` on unrecoverable syntax (caught by caller for R18).

**Patterns:** Follow line-scanner style of `ReflectionClassParser` (no external Mermaid library).

**Test scenarios (manual):**
- Valid multi-class diagram with one relation
- Missing visibility on field → still parsed (comparison marks wrong later)
- `<<interface>>` stereotype detected
- Malformed brace → `MmdParseException`

---

### U2. Type equivalence utility

**Goal:** Shared type normalization for MMD field and return-type comparison.

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/grading/MmdTypeEquivalence.java` (new)

**Work:**
1. Normalize tildes to angle brackets (`List~Member~` → `List<Member>`).
2. Accept `ArrayList<Member>` as matching `List<Member>` (collection implementation equivalence per R6).
3. Apply `HashMap<String, Integer>` ≡ `HashMap~String, int~` normalization.
4. Do **not** equate `int` and `Integer`.
5. Case-sensitive equality after normalization.

**Test scenarios (manual):**
- `List~Member~` vs `List<Member>` → match
- `int` vs `Integer` → no match
- `String` vs `string` → no match

---

### U3. Rubric snapshot relations

**Goal:** Load `ClassRelation` rows into `LabRubricSnapshot` without N+1.

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/repository/ClassRelationRepository.java`
- `backend/src/main/java/com/eiu/capstone/backend/grading/rubric/RelationRubric.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/grading/rubric/ChallengeRubric.java`
- `backend/src/main/java/com/eiu/capstone/backend/grading/rubric/LabRubricService.java`

**Work:**
1. Add `findByClassEntityInWithEndpoints` with JOIN FETCH.
2. Group relations by challenge via owning `ClassEntity`.
3. Add `relations()` to `ChallengeRubric`; include in snapshot immutability.

**Patterns:** Mirror `findByClassEntityInWithDeclaration` batching in `LabRubricService.loadForLab`.

**Test scenarios (manual):**
- Lab with 2 challenges, relations only on challenge 2 → snapshot partitions correctly
- Timing: single rubric load does not log per-relation queries when `app.grading.timing-log=true`

---

### U4. MMD comparison and grading merge

**Goal:** Grade MMD elements and merge with Java results using AND semantics; include relations in score denominator.

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/grading/MmdComparisonService.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/grading/GradingService.java`
- `backend/src/main/java/com/eiu/capstone/backend/grading/PendingGradingResults.java` (add relation pending type)

**Work:**
1. `MmdComparisonService.compare(challengeRubric, parsedMmd)` → per-element boolean map (class, fields, methods, constructors, relations).
2. Implement getter/setter family rule (R11): if solution has ≥1 getter, one `getter()` line suffices.
3. Refactor `gradeChallengeFolder` to compute `javaCorrect` and `mmdCorrect` separately, then `combined = java && mmd` for members/class.
4. Relations: `combined = mmdCorrect`; increment `totalElements` per `RelationRubric`.
5. Parse `.mmd` from first `MultipartFile` bytes in `mmdByChallenge.get(challengeKey)`; on absent file or parse exception, all MMD flags false.

**Patterns:** `fieldAttributesMatch` / `findMatchingMethod` in `GradingService`; comparison tuples from `grading/AGENTS.md`.

**Test scenarios (manual):**
- Java field correct, MMD field wrong → combined field incorrect
- Both correct → combined correct
- No `.mmd` → all MMD elements false; challenge % drops
- Relation type mismatch → full miss
- `Booking *-- Session` with Booking as target matches solution

---

### U5. Relation result persistence

**Goal:** Persist and load relation grading outcomes.

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/model/SubmissionRelationResult.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/repository/SubmissionRelationResultRepository.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/grading/GradingResultStore.java`
- `backend/src/main/java/com/eiu/capstone/backend/service/SubmissionResultLoader.java`
- `backend/AGENTS.md` (persistence table list)

**Work:**
1. Entity mirrors `SubmissionFieldResult`: `(submission_id, class_relation_id)` unique, `is_correct`.
2. Repository: `findBySubmission_IdWithRelation(submissionId)` with JOIN FETCH.
3. Extend `GradingResultStore.loadExisting` / `save` for relation results.
4. Extend `SubmissionResultLoader` with relation correct-ID set.
5. Document external DDL for `submission_relation_result` in unit notes.

**Patterns:** `SubmissionFieldResult.java`, `docs/solutions/database-issues/submission-result-reupload-duplicate-key.md` (upsert on re-upload).

**Test scenarios (manual):**
- Re-upload same attempt updates relation rows in place
- `loadCorrectIds` returns relation IDs in one query

---

### U6. Wire MMD into upload grading

**Goal:** Pass `mmdByChallenge` from controller to grading service.

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/controller/SubmissionController.java`
- `backend/src/main/java/com/eiu/capstone/backend/grading/GradingService.java`
- `backend/src/main/java/com/eiu/capstone/backend/grading/AGENTS.md`

**Work:**
1. Add `Map<String, List<MultipartFile>> mmdByChallenge` parameter to `gradeSubmission`.
2. Pass `uploadResult.mmdByChallenge` from `SubmissionController` line ~142.
3. Thread map into parallel `gradeChallengeFolder` tasks.
4. Update `grading/AGENTS.md` pipeline diagram and comparison docs.

**Test scenarios (manual):**
- Upload with `.java` + `.mmd` → challenge score reflects merged grading
- Upload Java only → lower score than pre-MMD Java-only baseline when relations exist in rubric

---

### U7. MMD read API

**Goal:** Implement `ClassStructureService.getMmdData` to return live grading results.

**Files:**
- `backend/src/main/java/com/eiu/capstone/backend/service/ClassStructureService.java`
- `backend/src/main/java/com/eiu/capstone/backend/DTO/MmdClassDTO.java`
- `backend/src/main/java/com/eiu/capstone/backend/DTO/MmdRelationDTO.java` (new)
- `backend/src/main/java/com/eiu/capstone/backend/DTO/MmdAttributeDTO.java`

**Work:**
1. Add optional `error` field to `MmdAttributeDTO` for StudentUI error list (R22).
2. Extend `MmdClassDTO` with `List<MmdRelationDTO> relations`.
3. `buildMmdDataForSubmission(submissionId, challengeId)` — batched rubric + single result load.
4. Format attribute `name` strings to match existing UI (`speed: double`, `move(): void`, `Vehicle(brand)`).
5. Map relation type master data to frontend labels (`extends`, `composition`, `aggregation`, etc.).

**Patterns:** `buildClassDataForSubmission` in same file.

**Test scenarios (manual):**
- `GET /api/labs/{labId}/challenges/{id}/mmd?studentId=` returns classes with `ok` flags after upload
- Relations array populated with `from`, `to`, `relType`, `ok`
- No submission → `[]`

---

### U8. Frontend MMD fetch and cache

**Goal:** Load and cache MMD data alongside class data.

**Files:**
- `frontend/src/pages/StudentDashboard.jsx`
- `frontend/src/pages/AGENTS.md`
- `frontend/AGENTS.md`

**Work:**
1. Add `mmdDataCacheRef`.
2. Refactor `fetchClassForChallenge` → `fetchChallengeDetails` fetching `/class` and `/mmd` in parallel when `hasSubmissionData`.
3. Invalidate MMD cache on upload for challenges in `challengeResult`.
4. Stop resetting `mmdData` to `[]` on cache hit.

**Patterns:** Existing `classDataCacheRef` and `fetchClassForChallenge`.

**Test scenarios (manual):**
- Select challenge after upload → MMD tab shows data without manual refresh
- Switch challenge and back → cache hit, no duplicate fetch
- Upload updates MMD tab for affected challenges

---

### U9. StudentUI cleanup

**Goal:** Remove mock relation data; render live MMD scores only.

**Files:**
- `frontend/src/components/student/StudentUI.jsx`

**Work:**
1. Remove hardcoded fallback relations (Car/Vehicle/Bike sample).
2. When `relationData.length === 0`, show empty relations table (not mock rows).
3. Confirm `mmdScore` and `relationScore` pills use live data.
4. Error list (`type: 'MMD'`) uses `MmdAttributeDTO.error` when present.

**Test scenarios (manual):**
- No submission → MMD tab empty state, no mock relations
- Partial pass → score pills and ticks match backend

---

## Verification Contract

**Build gates:**
- `cd backend && mvn -q -DskipTests compile`
- `cd frontend && npm run build`

**Manual E2E (primary):**
1. Start app: `npm start` from repo root.
2. Log in as student; select lab with rubric relations.
3. Upload `challenge_N` folder with matching `.java` + `.mmd`.
4. Confirm sidebar challenge % reflects merged score (stricter than Java-only if MMD has errors).
5. Open MMD tab — class boxes, ticks, relations table populated.
6. Re-upload Java only — MMD elements show failed; score drops.
7. Upload invalid `.mmd` — upload succeeds; all MMD elements failed.
8. `curl` `GET /api/labs/{labId}/challenges/{id}/mmd?studentId={uuid}` — non-empty JSON with `relations`.

**Performance check:**
- Enable `app.grading.timing-log=true`; upload should not show per-element SQL in logs; `grade_ms` remains bounded.

---

## Definition of Done

**Global:**
- [ ] All requirements R1–R27 satisfied
- [ ] `artifact_readiness: implementation-ready` checks pass
- [ ] `backend/AGENTS.md` and `grading/AGENTS.md` updated for MMD pipeline
- [ ] No mock relation data in `StudentUI.jsx`
- [ ] External DDL for `submission_relation_result` documented in plan or backend AGENTS

**Per unit:**
- [ ] U1–U2: Parser handles spec examples; type equivalence manual checks pass
- [ ] U3: Relations in rubric snapshot; no N+1 on load
- [ ] U4–U6: Merged score on upload; both-must-pass verified manually
- [ ] U5: Relation results persist across re-upload
- [ ] U7: `/mmd` returns live data
- [ ] U8–U9: Frontend MMD tab wired; cache works
