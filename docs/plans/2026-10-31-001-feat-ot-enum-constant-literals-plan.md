---
title: "Operational Testcase Enum Constants - Plan"
date: 2026-10-31
type: feat
topic: ot-enum-constant-literals
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Operational Testcase Enum Constants - Plan

## Goal Capsule

**Objective:** Let lecturers author operational testcases that pass and expect Java enum constants (e.g. `Event.BIRTHDAY` as free-text `BIRTHDAY`) on Unit and Composition steps, so labs like Cake(Customer, Event, …) are no longer OT dead ends.

**Product authority:** This Product Contract (`ce-brainstorm`, 2026-10-31). Named-instance enum binding, rubric dropdowns, and enum arrays are not active scope.

**Product Contract preservation:** Product Contract unchanged (R/A/F/AE IDs stable). Outstanding “Deferred to Planning” items resolved into Key Technical Decisions below.

**Open blockers:** None.

**Execution profile:** Standard feature across frontend OT authoring, save validation, and worker kernel coerce. Prefer characterization + focused unit tests before changing coerce/validation choke points.

---

## Product Contract

### Summary

Treat rubric **ENUM**-typed slots as required free-text constant-name literals on Unit and Composition: constructor/method arguments and FIELD_STATE / RETURN_VALUE expected values. Resolve the name on the compiled enum at dry-run and grade. Ordinary classes keep Named instance; `null` is not allowed in ENUM slots.

### Problem Frame

Composition and Unit argument UI only offers Named instance for non-scalar rubric types. Enums are excluded from constructable CONSTRUCTOR steps, so lecturers cannot create an `Event` value. The current workaround is to skip enum-related OT entirely, which leaves enum-heavy challenges without behavioral checks.

### Key Decisions

- **Typed constant literal over tagged `$enum` wire** `(session-settled: user-directed — chosen over Approach B tagged wire: same mental model as int/String when type context is already ENUM)` — Governs R1, R2, R5.
- **Free-text constant name over rubric-field dropdown** `(session-settled: user-directed — chosen over Structure ENUM field list as source of truth: resolve on compiled enum so student/reference constants can differ from Structure)` — Governs R3, R6.
- **Args and assertion expected values in one ship** `(session-settled: user-directed — chosen over args-only: FIELD_STATE on enum fields such as Cake.type otherwise stays blocked)` — Governs R1, R2.
- **Unit and Composition both** `(session-settled: user-directed — chosen over Composition-only: Unit has no `$instance` path for object args)` — Governs R1, R4.
- **Constant name always required; no `null` in ENUM slots** `(session-settled: user-directed — chosen over allowing null: enum slots are never empty)` — Governs R7.

### Actors

- A1. Lecturer — authors OT in Solution Management; dry-runs against reference sources.
- A2. Student — uploads solutions graded by the same OT path (no new student authoring UI).

### Requirements

**Authoring**

- R1. On Unit and Composition, when a constructor or method parameter type is a rubric ENUM class, the lecturer enters a non-blank constant name as a literal (text control), not a Named instance picker.
- R2. When a FIELD_STATE field type or RETURN_VALUE return type is a rubric ENUM class, the expected value is the same kind of non-blank constant-name literal.
- R3. The authored value is the constant’s simple name (e.g. `BIRTHDAY`), not a qualified `Event.BIRTHDAY` form required by the product.
- R4. Non-ENUM rubric class parameters and object expected checks keep today’s Named instance / equals() rules; this work does not change how ordinary classes are passed.

**Resolution and failure**

- R5. At lecturer dry-run and student grade, each ENUM-typed literal resolves to the matching constant on the compiled enum under test (reference sources for dry-run; student classes for upload).
- R6. A missing or misspelled constant fails at dry-run or grade (invoke/assertion failure), not as a Structure-rubric save validation against ENUM field rows.
- R7. ENUM-typed argument and expected-value slots reject `null` and blank names at authoring/save.

**Outcomes**

- R8. After this ships, a Composition (or Unit) step can construct `Cake` with an `Event` argument and assert ENUM-typed field or return state using constant names, without creating an enum via CONSTRUCTOR.

### Key Flows

- F1. Author enum arg then assert enum field
  - **Trigger:** Lecturer builds a Composition (or Unit) test that constructs a class with an ENUM parameter and checks an ENUM-typed field.
  - **Actors:** A1
  - **Steps:** Pick constructor/method → for ENUM params type constant names → for ordinary class params choose Named instances as today → add FIELD_STATE / RETURN_VALUE with constant-name expected where types are ENUM → dry-run → save.
  - **Outcome:** Dry-run resolves constants; save persists; student upload uses the same resolution.
  - **Covered by:** R1, R2, R5, R7, R8

- F2. Bad constant name
  - **Trigger:** Lecturer (or persisted OT) supplies a name that does not exist on the compiled enum.
  - **Actors:** A1, A2
  - **Steps:** Author/save may succeed → dry-run or grade attempts resolve → resolution fails.
  - **Outcome:** That invoke or assertion fails; no silent coerce to null.
  - **Covered by:** R6, R7

### Acceptance Examples

- AE1. Cake constructor with Event
  - **Covers R1, R5, R8.**
  - **Given:** Rubric has ENUM `Event` with constants including `BIRTHDAY`, and `Cake` ctor taking `Customer`, `Event`, …
  - **When:** Lecturer authors Composition: named `Customer`, Event arg text `BIRTHDAY`, remaining scalar args; dry-runs against a solution where `Event.BIRTHDAY` exists.
  - **Then:** Dry-run constructs `Cake` successfully with that enum constant.

- AE2. Assert enum field after construct
  - **Covers R2, R5, R8.**
  - **Given:** AE1 setup with field `type` of type `Event`.
  - **When:** Lecturer adds FIELD_STATE on `type` with expected `BIRTHDAY`.
  - **Then:** Dry-run/grade passes when the field holds that constant; fails when it holds another.

- AE3. Typo only fails at run
  - **Covers R3, R6.**
  - **Given:** Lecturer types `BIRTHDAYY` for an Event arg.
  - **When:** Save is attempted, then dry-run.
  - **Then:** Save is not rejected for mismatch against Structure ENUM fields; dry-run fails resolution.

- AE4. Null forbidden
  - **Covers R7.**
  - **Given:** An ENUM-typed argument or expected-value slot.
  - **When:** Lecturer clears the value or tries to set null.
  - **Then:** Authoring/save rejects it.

- AE5. Ordinary class unchanged
  - **Covers R4.**
  - **Given:** Parameter type `Customer` (non-enum class).
  - **When:** Lecturer fills that argument.
  - **Then:** UI still requires Named instance; free-text constant entry is not offered.

### Success Criteria

- Lecturers can author and dry-run OT for Lab 2 challenge 4–style Cake + Event without avoiding enum OT.
- Students are graded with the same constant resolution path; no separate student-facing enum authoring.
- Existing non-enum OT behavior is unchanged.

### Scope Boundaries

**In scope**

- ENUM-typed args and FIELD_STATE / RETURN_VALUE expected values on Unit and Composition
- Free-text constant names resolved on compiled enums at dry-run/grade

**Deferred for later**

- Binding enum constants into the Composition named-instance registry
- Rubric dropdown or autocomplete as source of truth
- Arrays of enums
- Tagged `$enum` wire shape (research found no force for a tag; keep deferred)

**Outside this product's identity**

- Making enums constructable via CONSTRUCTOR steps (`new Event()`)
- Changing Declaration/MMD enum-constant grading (name-only fields stay as today)

### Dependencies / Assumptions

- Rubric Structure already distinguishes ENUM declaring types and lists constants as fields for Declaration; OT constant lists are **not** required to match those rows at save (per R6).
- Parameter, field, and return type names already identify ENUM vs ordinary class in the lecturer OT editor once catalog helpers split ENUM from ordinary classes (KTD1).
- Dry-run compiles reference sources; upload grades student classes — both expose a loadable enum type for resolution.
- Structure declaring type must be ENUM for the type to take the constant-literal path; a mislabeled CLASS shell keeps Named instance behavior.

### Outstanding Questions

**Resolve Before Planning:** None.

**Deferred to implementation**

- Exact user-visible error string when `Enum.valueOf` fails (reuse existing invoke/assertion ERROR messaging unless copy is unclear).
- Whether nested enum type names need extra `Outer$Kind` fixtures beyond `JavaTypeResolver.loadStudentClass` (add a test only if a lab uses nested enums).

### Sources / Research

- Repo patterns: `isRubricClassType` + `TestcaseParamFields` force ENUM type names onto `$instance`; `JsonValueCoercer` rejects non-scalars; `JavaTypeResolver.resolve(name, loader)` already loads enum classes for method lookup; `EnumDeclaringTypes` exists for Declaration but not OT validation.
- Learnings: no `docs/solutions/` doc for OT enum constant literals; use CONCEPTS terms **enum constant literal**, **named instance**, Unit/Composition.
- Example lab: challenge_4 `Event` enum + `Cake` constructor taking `Event`.

---

## Planning Contract

### Key Technical Decisions

- KTD1. **Split ENUM from “rubric class object” membership in OT.** Frontend catalog and backend `TestcaseRubricService` treat type names whose Structure declaring type is ENUM (via `declaringTypeOptions` / `EnumDeclaringTypes`) as literal-eligible, not `$instance`-required. Ordinary classes stay object/`$instance`. Governs R1, R2, R4, R7.
- KTD2. **Wire format is a JSON string constant name** (e.g. `"BIRTHDAY"` via existing `parseScalarInput`). No `$enum` tag. Governs R3, R5.
- KTD3. **Coerce with ClassLoader + `Enum.valueOf`.** Extend kernel `JsonValueCoercer` so when the parameter/expected type loads as an enum under the student/reference loader, a non-blank textual JSON node resolves to that constant; blank/`null` rejected. `WorkerInvokeEngine` passes the loader into coerce (method lookup already uses `JavaTypeResolver.resolve(..., loader)`). Governs R5, R6, R7.
- KTD4. **FIELD_STATE / RETURN_VALUE ENUM paths stay scalar.** Carve ENUM field types and ENUM method returns out of object-field `$instance` and object-return equals() UI/validation; expected values use the same constant-name literal. After IPC, enum actuals that decode as JSON strings compare name-to-name; avoid running v1 typed coerce against an enum class name in a way that throws `Unsupported type in v1` before compare. Governs R2, R4.
- KTD5. **Do not validate constant names against Structure ENUM fields at save** (product R6). Client readiness still blocks empty/null ENUM slots.

### Assumptions

- Master-data declaring type name `ENUM` (case-insensitive) remains the Structure signal; backend already accepts ENUM/ENUMERATE/ENUMERATION in `EnumDeclaringTypes`.
- Rebuild worker JAR after kernel changes (`mvn package` / root `npm run backend` packaging) remains required for dry-run/upload OT.

### Sequencing

1. U1 kernel coerce + worker wire (runtime foundation)
2. U2 save validation + assertion/display (API + grade path)
3. U3 frontend authoring (lecturer can exercise end-to-end)
4. U4 docs (guide + AGENTS contracts)

U2 save validation can start once ENUM membership rules are agreed (KTD1); prefer landing U1 tests first so dry-run can prove resolve before UI ships.

### System-Wide Impact

- Lecturer Solution Management OT authoring and dry-run
- Student upload OT grade (same worker path)
- Desktop practice OT (same worker JAR) — no separate UI work
- No DB/schema migration

### Risks

- Structure mislabeled as CLASS → lecturer still sees Named instance (document prerequisite; not auto-fixed).
- `Enum.valueOf` is case-sensitive — typos fail at dry-run/grade by design.
- Forgetting worker JAR rebuild → local dry-run appears broken after kernel edits.

---

## Implementation Units

### U1. Kernel enum coerce + worker ClassLoader wire

**Goal:** Resolve ENUM-typed JSON string literals to live enum constants at invoke time; reject null/blank.

**Requirements:** R5, R6, R7

**Dependencies:** None

**Files:**
- Modify: `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/kernel/JsonValueCoercer.java`
- Modify: `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/kernel/AGENTS.md`
- Modify: `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/worker/WorkerInvokeEngine.java`
- Test: `backend/src/test/java/unit/com/eiu/capstone/backend/grading/testcase/worker/WorkerInvokeEngineTest.java`
- Optional kernel unit test if easier than worker fixture: new or existing coercer-focused unit under `backend/src/test/java/unit/com/eiu/capstone/backend/grading/testcase/`

**Approach:**
1. Add ClassLoader-aware coerce path for params/expected values: when type resolves via `JavaTypeResolver.resolve(typeName, loader)` and `clazz.isEnum()`, coerce textual JSON to `Enum.valueOf` (or equivalent); reject null/blank.
2. Keep `$instance` and scalar primitive branches unchanged.
3. Update `WorkerInvokeEngine` call sites that currently `coerceParams(paramsJson, parameterTypes, namedInstances)` to supply the student/reference loader.
4. Missing constant → invoke ERROR / failed step (no silent null).

**Patterns to follow:** `$instance` branch in `JsonValueCoercer`; `JavaTypeResolver.resolve(name, loader)`; existing `Unsupported type in v1` test in `WorkerInvokeEngineTest` (~283–292) as inverse case.

**Test scenarios:**
- Covers AE1. Constructor/method param type enum + JSON `"BIRTHDAY"` → live constant; invoke succeeds.
- Unknown constant name → invoke fails (not null arg).
- JSON `null` or blank for enum param → rejected/fails coerce.
- Ordinary scalar + `$instance` paths unchanged.
- Non-enum rubric class without `$instance` still unsupported.

**Verification:** Targeted worker/coerce unit tests green; worker JAR rebuild still packages kernel classes (`WorkerJarIsolationTest` unchanged unless API forces it).

---

### U2. Save validation + assertion/display enum expecteds

**Goal:** Persist ENUM literal args/expecteds; evaluate and display FIELD_STATE / RETURN_VALUE enum expecteds without throwing unsupported-type on enum field/return types.

**Requirements:** R1, R2, R4, R6, R7

**Dependencies:** U1 (for runtime/display coerce behavior)

**Files:**
- Modify: `backend/src/main/java/com/eiu/capstone/backend/service/TestcaseRubricService.java`
- Modify: `backend/src/main/java/com/eiu/capstone/backend/grading/EnumDeclaringTypes.java` (reuse; extend only if OT needs a shared name-set helper)
- Modify: `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/AssertionEvaluator.java`
- Modify: `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/TestcaseDisplayFormatter.java`
- Modify: `backend/src/main/java/com/eiu/capstone/backend/grading/AGENTS.md`
- Test: `backend/src/test/java/support/com/eiu/capstone/backend/service/TestcaseRubricServiceTest.java`
- Test: `backend/src/test/java/unit/com/eiu/capstone/backend/grading/testcase/AssertionEvaluatorTest.java`
- Test: `backend/src/test/java/unit/com/eiu/capstone/backend/grading/testcase/TestcaseDisplayFormatterTest.java`

**Approach:**
1. Build ENUM simple-name set from challenge classes’ declaring types (`EnumDeclaringTypes`); exclude those names from “must be `$instance`” / “UNIT cannot pass rubric-class object”.
2. Accept non-blank JSON string literals for ENUM param types; reject null/blank; keep ordinary class `$instance` 422s.
3. Accept non-blank string expecteds for ENUM FIELD_STATE field types and ENUM RETURN_VALUE returns; do not require `$instance` / equals() object checks for those.
4. Ensure FIELD_STATE typed coerce and display formatting do not throw `Unsupported type in v1` when `fieldDataType` is an enum class name — compare constant names (string or enum-aware coerce per KTD4).
5. Do not save-validate against Structure enum field rows (KTD5 / R6).

**Patterns to follow:** Existing UNIT object-arg 422 tests; Composition `$instance` requirement; `EnumDeclaringTypes` for Declaration.

**Test scenarios:**
- Covers AE4. UNIT save with enum string arg succeeds; null enum arg → 422.
- Composition enum string arg succeeds; ordinary class non-`$instance` still 422.
- Covers AE2. FIELD_STATE expected `"BIRTHDAY"` vs enum field snapshot/name passes/fails correctly.
- RETURN_VALUE enum expected string compares after decode.
- Display formatting for FIELD_STATE enum expected does not throw.
- Typo constant still saves (Covers AE3 save half).

**Verification:** `mvn test` for listed classes; dry-run path can wait for U3 for full Cake UX.

---

### U3. Frontend ENUM literal authoring

**Goal:** Lecturer UI and client readiness treat ENUM-typed params/fields/returns as required free-text literals on Unit and Composition.

**Requirements:** R1, R2, R3, R4, R7, R8

**Dependencies:** U2 for save to accept the payloads the UI produces (can develop against U2 in parallel once contracts match).

**Files:**
- Modify: `frontend/src/components/lecturer/structure/testcaseAuthoring.js`
- Modify: `frontend/src/components/lecturer/structure/TestcaseParamFields.jsx`
- Modify: `frontend/src/components/lecturer/structure/TestcaseAssertionFields.jsx`
- Modify: `frontend/src/components/lecturer/AGENTS.md`
- Touch if needed: `UnitTestcaseWorksheet.jsx`, `CompositionTestcaseScript.jsx`, `TestcasesPanel.jsx` (catalog already receives `declaringTypeOptions`)

**Approach:**
1. Add catalog helper(s) e.g. `isRubricEnumType` / `enumClassNames` from `declaringTypeOptions` + class declaringTypeId (mirror `isConcreteRubricClass` / `ClassDetailPanel` ENUM check).
2. Route ENUM params to scalar text input (existing `parseScalarInput` / `displayScalar`); keep non-enum rubric classes on `$instance`.
3. Unblock Unit members that only have ENUM-typed object-looking params (carve out of `hasRubricClassArgument` / `unitMemberBlockedReason`).
4. FIELD_STATE on ENUM-typed fields and RETURN_VALUE on ENUM returns: scalar expected text, not Named instance / equals() picker (`isObjectReturnStep` carve-out).
5. Dry-run readiness: require non-blank ENUM literals; still reject empty/null.

**Patterns to follow:** `TestcaseParamFields` scalar branch; `isConcreteRubricClass` declaring-type keying; `parseScalarInput` already yields JSON strings for unquoted tokens.

**Test scenarios:**
- Test expectation: none automated — frontend has no OT unit test runner; verify via manual Solution Management checklist below and `npm run build`.
- Manual Covers AE1/AE2/AE5: Event arg text + Customer Named instance; enum FIELD_STATE; Customer still Named instance.
- Manual Covers AE4: blank enum slot blocked in readiness.

**Verification:** `npm run build` from `frontend/`; manual dry-run on a challenge with ENUM Structure type + Cake-style ctor.

---

### U4. Lecturer guide and contract docs

**Goal:** Document that literals include ENUM constant names; update OT value-type contracts.

**Requirements:** R8 (operability), Success Criteria

**Dependencies:** U1–U3 behavior settled

**Files:**
- Modify: `docs/LECTURER_OPERATIONAL_TESTCASE_GUIDE.md`
- Modify: `backend/src/main/java/com/eiu/capstone/backend/grading/AGENTS.md` (if not fully covered in U1/U2)
- Modify: `backend/src/main/java/com/eiu/capstone/backend/grading/testcase/kernel/AGENTS.md` (if not fully covered in U1)
- `CONCEPTS.md` already defines **enum constant literal** — only tweak if implementation naming drifts

**Approach:**
1. Extend literal-args section: primitives, wrappers, String, null (non-ENUM), arrays of those, **and ENUM constant names when the param/field/return type is a rubric ENUM**.
2. Note Structure ENUM declaring type prerequisite; typos fail at dry-run/grade.
3. Align AGENTS value-types bullets with KTD2–KTD4.

**Test scenarios:**
- Test expectation: none — docs only. Spot-check guide examples against AE1/AE2 wording.

**Verification:** Guide examples match shipped UI labels; no stale “literals = primitives only” contradiction in touched AGENTS.

---

## Verification Contract

| Gate | Command / action |
|---|---|
| Backend unit/support tests | From `backend/`: `mvn test` (at minimum the U1–U2 listed test classes) |
| Worker packaging | After kernel edits: `mvn package -DskipTests` (or root backend script that rebuilds `backend-1.0.0-worker.jar`) before manual dry-run |
| Frontend compile | From `frontend/`: `npm run build` |
| Manual dry-run | Solution Management: Structure ENUM `Event` + Composition/Unit Cake ctor with `BIRTHDAY`; FIELD_STATE on enum field; typo constant fails dry-run; Customer still Named instance |
| Student path | Optional: upload reference solution against saved OT; same pass/fail as dry-run |

No `release:validate` / behavioral skill eval required for this feature.

---

## Definition of Done

- All U1–U4 complete; Product Contract R1–R8 satisfied.
- Listed automated tests green; frontend build green; manual AE1/AE2/AE4/AE5 dry-run checklist passed.
- Worker JAR used for dry-run includes the new coerce code.
- Abandoned experiment code (e.g. `$enum` tag prototypes) not left in the diff.
- Lecturer guide and AGENTS value-type lines match shipped behavior.
