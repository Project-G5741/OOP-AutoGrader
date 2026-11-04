---
title: "Declaration Multi-Name and Multi-Datatype Aliases - Plan"
date: 2026-10-31
type: feat
topic: declaration-multi-alias
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Declaration Multi-Name and Multi-Datatype Aliases - Plan

## Goal Capsule

**Objective:** Let lecturers author multiple acceptable names and datatypes for Declaration members so students can use assignment-legitimate synonyms without asking for one canonical spelling, with the same alias sets honored on Declaration Test and MMD.

**Product authority:** This Product Contract (`ce-brainstorm`, 2026-10-31). Shared lab-wide synonym tables, paired name+type variants, and constructor/class-name aliasing are not active scope.

**Product Contract preservation:** Product Contract unchanged (R/A/F/AE IDs stable). Outstanding “Deferred to Planning” items resolved into Key Technical Decisions below.

**Open blockers:** None.

**Execution profile:** Standard feature with schema migration. Prefer characterization tests on graders and structure save before changing match/save choke points. Characterization + focused unit tests before expanding OT worker name resolution.

---

## Product Contract

### Summary

Upgrade Solution Management and grading so fields, method names/return types, and method/constructor parameters carry lecturer-authored ordered **name** and **datatype** lists. A student declaration passes when its name is in the name list and its type is in the type list (Cartesian). The first entry in each list is primary for expected/display. Constructor names remain the class name. Aliases are never system-generated.

### Problem Frame

Today each field, method, and parameter accepts exactly one name and one datatype. When an assignment allows several legitimate names or types, students who pick a different but valid spelling must ask the lecturer for the exact rubric string. There is no workaround short of that conversation or false fails.

### Key Decisions

- **Independent alias lists with Cartesian match** `(session-settled: user-directed — chosen over Approach B paired variants and Approach C shared synonym tables: matches lecturer examples such as double/float × cost/fee without extra concepts)` — Governs R1, R2, R3, R8.
- **Both Declaration Test and MMD use the same alias sets** `(session-settled: user-directed — chosen over Declaration-only or deferred MMD: diagram labels must not reimpose a single spelling)` — Governs R9.
- **Primary = first alias for expected/display** `(session-settled: user-directed — chosen over listing all aliases or closest-match display: simple, stable feedback)` — Governs R10.
- **Full dropdown + Add row UI in v1** `(session-settled: user-directed — chosen over a stripped chip-only editor: authoring UX specified with the feature)` — Governs R11, R12.
- **Lecturer-authored aliases only** `(session-settled: user-directed — chosen over system synonym dictionaries: lecturers own every acceptable string)` — Governs R1, R13.

### Actors

- A1. Lecturer — authors alias lists in Solution Management; expects dry-run and student grade to honor them.
- A2. Student — uploads Java + MMD; passes Declaration and MMD when using any allowed name/type combination; sees primary aliases in expected/feedback copy.

### Requirements

**Alias coverage**

- R1. Each field carries an ordered non-empty list of acceptable names and an ordered non-empty list of acceptable datatypes, both authored by the lecturer.
- R2. Each method carries an ordered non-empty list of acceptable names and an ordered non-empty list of acceptable return types.
- R3. Each method parameter and each constructor parameter carries ordered non-empty name and datatype lists with the same authoring and matching rules as a field’s lists.
- R4. Constructor **names** are not aliased; they remain the class name. Only constructor parameters participate in multi-name / multi-datatype lists.
- R5. Class names are not aliased in this work.

**Matching**

- R6. Declaration Test treats a field as name-correct when the student’s field name equals any entry in that field’s name list (existing name-comparison rules apply per entry).
- R7. Declaration Test treats a field’s type as correct when the student’s type matches any entry in that field’s datatype list (existing type-comparison rules apply per entry). Name and type checks are independent (Cartesian).
- R8. Method and parameter matching use the same Cartesian any-of-name and any-of-type rules on their respective lists; method return type uses the return-type list.
- R9. MMD member name and type checks use the same lecturer-authored alias lists as Declaration Test for the corresponding rubric members.
- R10. Student-facing and lecturer expected/display strings for a failed or summarized member use the **first** name and **first** datatype (or return type) in each list as the primary; other aliases are match-only unless a later UI explicitly shows them.

**Authoring UI**

- R11. In Solution Management, each name control and each datatype/return-type control is a focusable box that lists current aliases and ends with an **Add name** or **Add datatype** (or equivalent) last line that appends a new entry.
- R12. Lecturers can add, edit, reorder (so first remains primary), and remove aliases, subject to keeping at least one name and one type (or return type) per list.
- R13. The product does not invent or suggest synonyms; every alias string is entered by the lecturer.

**Consistency with other grading paths**

- R14. Any grading path that resolves a student member by name for a rubric member that has alias lists (including operational-test field/member resolution when it is name-based) must accept any alias in that list so a Declaration/MMD pass cannot fail solely because a non-primary alias was used.

**Compatibility**

- R15. Existing rubric members with a single name and single type continue to work as one-element alias lists (that single value is the primary).

### Key Flows

- F1. Author field aliases then grade
  - **Trigger:** Lecturer wants `cost`/`fee`/`charge` and `double`/`float` accepted for one field.
  - **Actors:** A1, A2
  - **Steps:** Open Solution Management → focus field name box → add aliases → focus datatype box → add types → save → student uploads with e.g. `float fee` → Declaration and MMD pass; expected copy still shows primary `cost` / `double` when feedback needs a single expected label.
  - **Outcome:** Cartesian pass on both pillars; primary used for display.
  - **Covered by:** R1, R6, R7, R9, R10, R11

- F2. Method and constructor parameters
  - **Trigger:** Lecturer allows `calTotal(double cost)` or `calTotal(float fee)`, and `Customer(int age)` or `Customer(byte age)`.
  - **Actors:** A1, A2
  - **Steps:** Set method name aliases and/or return-type aliases as needed → set each parameter’s name and type lists → leave constructor name as class name → save → student uploads matching any allowed combination per position.
  - **Outcome:** Method/ctor param aliases grade; constructor name unchanged.
  - **Covered by:** R2, R3, R4, R8

- F3. Add alias via dropdown last line
  - **Trigger:** Lecturer focuses a name or datatype box that already has entries.
  - **Actors:** A1
  - **Steps:** Focus control → see existing aliases → choose Add name / Add datatype → enter value → primary remains the first entry unless reordered.
  - **Outcome:** List grows; save persists ordered lists.
  - **Covered by:** R11, R12, R13

### Acceptance Examples

- AE1. Field Cartesian pass
  - **Covers:** R1, R6, R7
  - **Given:** Field name list `[cost, fee, charge]` and type list `[double, float]`
  - **When:** Student declares `float fee`
  - **Then:** Declaration Test marks the field correct for name and type

- AE2. Primary display on fail
  - **Covers:** R10
  - **Given:** Same lists as AE1; student declares `int price` (name and type both wrong)
  - **When:** Results show expected labels
  - **Then:** Expected name/type shown are `cost` and `double`, not the full alias lists

- AE3. MMD same aliases
  - **Covers:** R9
  - **Given:** Same field alias lists saved on the rubric
  - **When:** Student MMD uses member name `fee` and an allowed type
  - **Then:** MMD pillar accepts that member the same way Declaration would

- AE4. Constructor name fixed
  - **Covers:** R4
  - **Given:** Class `Customer` with constructor param type list `[int, byte]` and name list `[age]`
  - **When:** Student writes `Customer(byte age)`
  - **Then:** Constructor matches; a differently named constructor type is still invalid

- AE5. No system synonyms
  - **Covers:** R13
  - **Given:** Lecturer authored only `cost` as the name
  - **When:** Student uses `fee` without the lecturer adding it
  - **Then:** Name check fails

### Success Criteria

- Lecturers can author multi-alias fields/methods/parameters in Solution Management and save without losing order or primary.
- Students using any lecturer-allowed name×type combination pass Declaration and MMD for that member; expected copy shows primary only.
- OT FIELD_STATE / method invoke still works when the student used a non-primary alias (R14).
- Existing single-name labs behave as before after migration (one-element lists).

### Scope Boundaries

**In scope**

- Lecturer multi-alias authoring for fields, method names, method return types, and method/constructor parameters
- Cartesian Declaration + MMD matching and primary-for-display
- Dropdown + Add last-line authoring UX
- Name-based resolution elsewhere in grading staying consistent with aliases (R14)
- Parameter structure save upgraded to upsert-by-id so nested alias children stay stable

**Deferred for later**

- Shared lab-wide synonym tables reused across members
- Explicit paired (name, type) variant authoring instead of Cartesian lists
- System- or dictionary-generated synonym suggestions

**Outside this work**

- Aliasing class names or constructor names
- Changing OT authoring UX beyond consistency of name resolution (R14)
- Changing scoring weights or pillar applicability rules

### Dependencies / Assumptions

- Existing single-string name/type comparison rules (trim/case/type equivalence where already defined) apply **per alias entry**, not replaced by a new synonym engine.
- Rubric identity for OT linking remains the member’s stable id; aliases do not create new rubric members.
- Migrating persisted single name/type into one-element lists preserves today’s behavior (R15).
- Rubric cache invalidation on structure save continues to run after alias writes.

### Outstanding Questions

**Resolve Before Planning:** None.

**Deferred to implementation**

- Exact SQL table/column names for alias child rows (follow existing `order_index` + park-delete-compact if unique indexes require it).
- Whether frontend AliasListBox is a shared component under `frontend/src/components/lecturer/structure/` vs inline in `ClassDetailPanel` / `ParameterRows` (prefer shared if both touch the same interaction).

### Sources / Research

- Grounding: one `name` + one type per field/method/parameter in schema, DTOs, Solution Management (`ClassDetailPanel`), Declaration Test exact-name maps, and MMD single-name lookup; no prior multi-alias plan. Adjacent UI: `ParameterRows` Add pattern.
- Repo research 2026-10-31: `LabStructureService` mirrors declaration primary onto `field.name` / `method.name`; graders key by primary; OT uses `field.getName()` → `WorkerInvokeEngine.readField` / `getMethod`; parameter save is delete-all-reinsert today; clone/import/desktop must round-trip lists.
- Learnings: honor client-UUID upsert and sync-by-presence; keep rubric UUIDs stable for OT/`submission_*_result`; four-layer rubric feature pattern (nested-class-grading-support).

---

## Planning Contract

### Key Technical Decisions

- KTD1. **Keep denormalized primary columns; add ordered alias child rows** `(session-settled: user-approved — chosen over JSONB lists on the parent: matches Add/reorder UX, clone remap, and OT/param patterns)` — Retain `field_declaration.name` / `data_type`, `method_declaration.name` / `return_type`, `parameter.name` / `data_type`, and mirrored `field.name` / `method.name` as the **first** alias (primary). Persist additional ordered aliases in child tables (or equivalent child entities) keyed by parent id + `order_index`, with index 0 always equal to the primary column. Backfill: existing rows become one-element lists. Governs R1, R2, R3, R10, R15.

- KTD2. **Upgrade parameter save to upsert-by-id (sync-by-presence)** `(session-settled: user-approved — chosen over keeping delete-all-reinsert: nested alias children must survive save)` — Replace method/constructor parameter delete-all-reinsert with client-UUID upsert + delete absent ids, same spirit as OT invocation save. Alias child sync follows the same presence rules. Governs R3, R12.

- KTD3. **One shared Cartesian alias matcher for Declaration, MMD, and snapshot** — Centralize “any name in list” and “any type in list” using existing normalize / `PartialCreditEvaluator` / `MmdTypeEquivalence` per entry. Field match: find student field whose name matches any alias, then type any-of. Method/ctor match: any method name alias with per-position param name+type any-of (param **names** do not affect Java overload identity; types drive signature match; names still grade when the product grades param names). Wire the same lists from `FieldRubric` / `MethodRubric` (and param alias views) loaded in `LabRubricService`. Governs R6, R7, R8, R9.

- KTD4. **OT R14 resolves student members by trying aliases at the worker / snapshot boundary** — Keep OT rubric links on member UUID. When assembling `AssertionRubric.fieldName` / invoke method name for runtime, prefer primary for display cards; when reading/invoking, try each name alias (and compatible type alias combinations for `getMethod`) until one binds on the student class. Do not expand OT authoring UI. Governs R14.

- KTD5. **Structure DTOs carry ordered `names` / `dataTypes` (or `returnTypes`) lists; primary derived as index 0** — API accepts lists with ≥1 non-blank entries; server writes primary columns from index 0 and syncs alias children. Editor hydrates lists from load; legacy clients sending only single `name`/`dataType` remain acceptable by treating them as one-element lists during transition if needed. Governs R11, R12, R15.

### Assumptions

- All-or-nothing Declaration scoring still requires every graded attribute (scope/static/final/etc.) plus name-in-list and type-in-list.
- Lab clone remaps every new alias-child UUID the same way it remaps parameters.
- Desktop packs and solution import emit/consume the new list shape; import of reflected single members stays one-element lists.
- Existing rubric cache invalidation on structure save is sufficient once alias tables are written in the same transaction.

### Sequencing

1. U1 schema + entities + migrator/backfill
2. U2 structure DTOs + LabStructureService save/load + parameter upsert + cache invalidation
3. U3 rubric load + shared matcher + Declaration/MMD/snapshot/disclosure
4. U4 OT R14 worker/assembly path
5. U5 frontend AliasListBox authoring
6. U6 clone / import / desktop + AGENTS docs

U5 can start once U2 API shape is stable; prefer U3 tests green before shipping UI so save→grade works end-to-end.

### System-Wide Impact

- Solution Management structure editor and lab structure API
- Declaration Test and MMD grading + parsed submission snapshots / disclosure accuracy
- OT dry-run and student upload (name resolution only)
- Lab clone, solution import, desktop pack structure round-trip
- PostgreSQL schema + startup migrator; operator SQL doc under `docs/sql/`

### Risks

- Parameter replace-all left in place would wipe alias children — mitigated by KTD2.
- Updating only Declaration or only MMD reintroduces pillar drift — mitigated by shared matcher (KTD3) and AE3.
- Showing non-primary aliases in expected copy violates R10 — keep display on primary columns / index 0.
- Unique `(parent_id, order_index)` collisions on reorder — use park-delete-compact if indexes require it.
- Forgetting clone/desktop/import → silent data loss on pack export — covered in U6.

### Alternatives considered

- **JSONB arrays on declaration/parameter rows** — fewer tables; weaker ordered edit/clone ergonomics; rejected in favor of child rows (KTD1).
- **Keep parameter delete-all-reinsert** — smaller save diff; rejected because alias children would churn or orphan (KTD2).

---

## Implementation Units

### U1. Schema, entities, migrator, backfill

**Goal:** Persist ordered alias lists while keeping primary columns as first entry; migrate existing single values to one-element lists.

**Requirements:** R1, R2, R3, R15

**Dependencies:** None

**Files:**
- Add: `docs/sql/2026-10-31-declaration-alias-lists.sql` (operator-run CREATE TABLE IF NOT EXISTS for alias child tables)
- Add: startup migrator under `backend/src/main/java/com/eiu/capstone/backend/config/` (mirror `FieldDeclarationModifiersSchemaMigrator` pattern)
- Modify: JPA models under `backend/src/main/java/com/eiu/capstone/backend/model/` (`FieldDeclaration`, `MethodDeclaration`, `Parameter`, plus new alias entities/repos)
- Modify: `AGENTS.md` / `backend/AGENTS.md` Child DOX Index for the new SQL artifact
- Test: migrator/unit or support test proving empty DB gains tables and existing primary columns remain readable as one-element lists after backfill

**Approach:**
1. Define child tables for name aliases and type aliases attached to field declarations, method declarations, and parameters (ordered by `order_index`).
2. On migrate/backfill, for each parent with no alias children, insert order 0 from the existing primary column(s).
3. Keep writing primary columns on every save (U2) so unread paths stay coherent during rollout.

**Patterns to follow:** `docs/sql/2026-10-28-field-declaration-modifiers.sql`; `FieldDeclarationModifiersSchemaMigrator`; parameter `order_index` child-row shape.

**Test scenarios:**
- Fresh schema: alias tables exist; legacy field with `name=cost`, `data_type=double` loads as lists `[cost]` / `[double]`.
- Migrator is idempotent (second boot does not duplicate order-0 rows).

**Verification:** Migrator test green; `mvn test` subset for config/support as applicable.

---

### U2. Structure DTOs + LabStructureService save/load + parameter upsert

**Goal:** Round-trip ordered alias lists through structure API; upsert parameters by client UUID; invalidate rubric cache.

**Requirements:** R1, R2, R3, R4, R11, R12, R13, R15

**Dependencies:** U1

**Files:**
- Modify: `backend/src/main/java/com/eiu/capstone/backend/DTO/rubric/FieldStructureDTO.java`, `MethodStructureDTO.java`, `ParameterStructureDTO.java` (and any nested alias DTO records)
- Modify: `backend/src/main/java/com/eiu/capstone/backend/service/LabStructureService.java` (sync fields/methods/parameters; primary mirror onto `field.name` / `method.name`; alias child sync)
- Modify: related repositories as needed
- Test: `backend/src/test/java/support/com/eiu/capstone/backend/service/LabStructureServiceSaveTest.java`
- Test: `backend/src/test/java/integration/.../LabStructureReplaceSaveIntegrationTest.java` (or extend equivalent)

**Approach:**
1. Extend DTOs with ordered `names` / `dataTypes` / `returnTypes` lists (≥1); derive primary from index 0; reject blanks.
2. Replace parameter delete-all-reinsert with upsert-by-id + delete absent; sync alias children with sync-by-presence (park-delete-compact if unique order indexes collide).
3. Ensure constructor name remains class-tied (no alias lists on constructor name).
4. Keep existing `RubricCacheInvalidationSupport.invalidateLab` on successful save.

**Patterns to follow:** Client-UUID create in solution-management editor; OT sync-by-presence; never delete-all when children exist (`docs/solutions/logic-errors/lecturer-testcase-save-persistence.md`).

**Test scenarios:**
- Save field with three names and two types; reload returns same order; primary columns = first entries; `field.name` matches primary name.
- Save method with name aliases, return-type aliases, and multi-alias parameters; reload preserves parameter ids across save (upsert).
- Removing a middle alias reorders remaining without unique-index failure.
- Reject empty name or type list on save.

**Verification:** Structure save unit/integration tests green.

---

### U3. Rubric load + shared matcher + Declaration/MMD/snapshot/disclosure

**Goal:** Rubric snapshot carries alias lists; Declaration and MMD use one Cartesian matcher; expected/display stay primary.

**Requirements:** R6, R7, R8, R9, R10

**Dependencies:** U2

**Files:**
- Modify: `backend/src/main/java/com/eiu/capstone/backend/grading/rubric/FieldRubric.java`, `MethodRubric.java`, related param views / `RubricParameterMaps.java` as needed
- Modify: `backend/src/main/java/com/eiu/capstone/backend/grading/rubric/LabRubricService.java`
- Add: shared matcher helper under `backend/src/main/java/com/eiu/capstone/backend/grading/` (name TBD at impl)
- Modify: `backend/src/main/java/com/eiu/capstone/backend/grading/pipeline/ClassReflectionGrader.java`
- Modify: `backend/src/main/java/com/eiu/capstone/backend/grading/MmdComparisonService.java`
- Modify: `backend/src/main/java/com/eiu/capstone/backend/grading/ParsedSubmissionSnapshotBuilder.java`
- Modify: `backend/src/main/java/com/eiu/capstone/backend/service/ClassStructureService.java` (accuracy/display helpers that key by primary name)
- Modify: `backend/src/main/java/com/eiu/capstone/backend/grading/AGENTS.md` as needed
- Test: `backend/src/test/java/unit/com/eiu/capstone/backend/grading/pipeline/ClassReflectionGraderTest.java`
- Test: `backend/src/test/java/unit/com/eiu/capstone/backend/grading/MmdComparisonServiceTest.java`
- Test: `backend/src/test/java/unit/com/eiu/capstone/backend/grading/ParsedSubmissionSnapshotBuilderTest.java`
- Test: `backend/src/test/java/support/com/eiu/capstone/backend/service/ClassStructureServiceDisclosureTest.java` (and shell display if labels change)

**Approach:**
1. Extend rubric records with ordered alias lists; `name()` / `dataType()` accessors remain primary (index 0) for display callers.
2. Replace exact-name map lookups with alias-aware find (student name ∈ name list; type any-of with existing equivalence helpers per pillar).
3. Method match: any name alias + positional param type any-of (and param name checks if currently graded).
4. Snapshot/disclosure expected labels use primary only; pass/fail uses matcher.

**Patterns to follow:** `PartialCreditEvaluator.normalize`; `MmdTypeEquivalence.typesMatch`; all-or-nothing member scoring.

**Test scenarios:**
- Covers AE1. Field aliases → `float fee` Declaration pass.
- Covers AE2. Fail still shows primary expected labels.
- Covers AE3. Same aliases pass MMD.
- Covers AE4. Constructor name not aliased; param type aliases work.
- Covers AE5. Unlisted synonym fails.
- Method name alias + param type alias combination grades correctly.
- Scope/static/final still required when specified.

**Verification:** Unit grading tests green for Declaration + MMD + snapshot.

---

### U4. OT R14 — alias-aware field/method resolution

**Goal:** OT field read and method invoke succeed when the student used a non-primary alias.

**Requirements:** R14

**Dependencies:** U3

**Files:**
- Modify: `backend/src/main/java/com/eiu/capstone/backend/grading/rubric/LabRubricService.java` and/or `TestcaseRubricAssembler` (pass alias lists into assertion/invoke rubrics without changing OT authoring)
- Modify: `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/worker/WorkerInvokeEngine.java` (`readField`, `getMethod` retry over aliases)
- Modify: `backend/src/main/java/com/eiu/capstone/backend/grading/pipeline/TestcaseGrader.java` if snapshot field name lists must include aliases
- Test: `backend/src/test/java/unit/com/eiu/capstone/backend/grading/testcase/worker/WorkerInvokeEngineTest.java`
- Test: `backend/src/test/java/unit/com/eiu/capstone/backend/grading/pipeline/TestcaseGraderTest.java` and/or `IsolatedWorkerAeTest.java`

**Approach:**
1. Carry name/type alias lists on invoke/assertion runtime DTOs (display name stays primary).
2. `readField`: try each field name alias via `getDeclaredField` until one succeeds.
3. Method invoke: try name aliases × compatible parameter type alias combinations consistent with positional lists.
4. Leave OT lecturer UI unchanged.

**Patterns to follow:** Existing `AssertionRubric.fieldName` display; worker reflective lookup.

**Test scenarios:**
- FIELD_STATE on primary-named rubric field succeeds when student field is a secondary name alias.
- Method invoke succeeds when student method uses a secondary name alias and allowed param types.
- Missing all aliases still fails clearly.

**Verification:** Worker/OT unit tests green; rebuild worker JAR if engine packaging requires it.

---

### U5. Frontend AliasListBox authoring

**Goal:** Solution Management name/type controls become focusable lists with Add name / Add datatype last line; reorder and remove with ≥1 entry.

**Requirements:** R11, R12, R13

**Dependencies:** U2 (API shape)

**Files:**
- Modify: `frontend/src/components/lecturer/structure/ClassDetailPanel.jsx`
- Modify: `frontend/src/components/lecturer/structure/ParameterRows.jsx`
- Add: shared alias list control under `frontend/src/components/lecturer/structure/` (preferred)
- Modify: draft/save mapping in Solution Management structure state (e.g. `SolutionManagement.jsx` or structure helpers) so lists round-trip
- Modify: `frontend/AGENTS.md` only if structure editor contracts change

**Approach:**
1. Replace single name/type inputs with AliasListBox: shows entries; focus reveals list; last line Add name / Add datatype / Add return type as appropriate.
2. First entry is primary; support reorder/remove with guard against empty lists.
3. Wire save payload to list fields; hydrate from load.

**Patterns to follow:** `ParameterRows` Add row affordance; existing Tailwind/structure editor styles — no new design system.

**Test scenarios:**
- Manual: add two name aliases and two types on a field, save, reload — order and primary preserved.
- Manual: cannot remove the last remaining name or type.
- Manual: method return-type list and parameter lists behave the same.

**Verification:** Lecturer smoke on Solution Management; no frontend automated suite yet.

---

### U6. Clone, import, desktop pack, docs

**Goal:** Alias lists survive lab clone, solution import, and desktop structure mapping; docs/contracts updated.

**Requirements:** R15 (compatibility), plus operational completeness for R1–R3

**Dependencies:** U2, U3

**Files:**
- Modify: `backend/src/main/java/com/eiu/capstone/backend/service/LabCloneService.java`
- Modify: `backend/src/main/java/com/eiu/capstone/backend/service/SolutionImportService.java`
- Modify: `backend/src/main/java/com/eiu/capstone/backend/desktop/DesktopPackStructureMapper.java` (and export/import paths as needed)
- Test: `backend/src/test/java/support/com/eiu/capstone/backend/service/LabCloneServiceTest.java`
- Test: `backend/src/test/java/unit/.../SolutionImportServiceTest.java`
- Test: desktop pack tests under `backend/src/test/java/com/eiu/capstone/backend/desktop/`
- Modify: `backend/AGENTS.md`, root `AGENTS.md` Child DOX Index for SQL; `docs/USER_GUIDE.md` or lecturer structure notes only if user-facing authoring steps change materially

**Approach:**
1. Clone: remap alias-child UUIDs with parents.
2. Import: reflected single name/type → one-element lists.
3. Desktop: serialize/deserialize lists; avoid synthesizing only primary when extras exist.
4. Document alias authoring briefly where Solution Management is described.

**Patterns to follow:** `docs/solutions/architecture-patterns/lab-clone-across-terms.md`.

**Test scenarios:**
- Clone lab with multi-alias field → clone has same ordered lists, new UUIDs, OT field_id remapped if present.
- Import .class field → one-element name/type lists.
- Desktop pack export/import round-trips multi-alias method parameters.

**Verification:** Clone/import/desktop tests green.

---

## Verification Contract

| Gate | Command / check | Applies to |
|------|-----------------|------------|
| Unit / support tests | `mvn test` from `backend/` (or targeted Surefire for U1–U4, U6 classes listed above) | U1–U4, U6 |
| Worker packaging | Rebuild worker JAR if `WorkerInvokeEngine` changes; existing worker isolation tests | U4 |
| Frontend smoke | Lecturer Solution Management: author aliases, save, reload, dry-run/upload with synonym student code | U5 (+ U3/U4) |
| DOX | Update nearest AGENTS.md / SQL index when schema or contracts change | U1, U6 |

---

## Definition of Done

**Global**

- [ ] All R1–R15 satisfied against AE1–AE5 and OT R14 scenarios
- [ ] `artifact_readiness: implementation-ready` plan units implemented or explicitly deferred with user approval
- [ ] Backend tests for matcher, structure save, OT resolution, clone/import green
- [ ] Primary columns remain first alias; empty lists impossible via API/UI
- [ ] AGENTS.md / `docs/sql` index updated for new migrator SQL

**Per unit**

- [ ] U1: Migrator + backfill idempotent; primary readable as one-element lists
- [ ] U2: Save/load lists; parameter upsert preserves ids; cache invalidate
- [ ] U3: Declaration + MMD + snapshot/disclosure use shared matcher; primary display
- [ ] U4: OT field/method resolution accepts aliases
- [ ] U5: AliasListBox Add/reorder/remove works in Solution Management
- [ ] U6: Clone/import/desktop + docs complete
