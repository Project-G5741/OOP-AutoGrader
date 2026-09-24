---
title: Method Overriding Testcases - Plan
type: feat
date: 2026-09-25
topic: method-overriding-testcases
artifact_contract: ce-unified-plan/v1
artifact_readiness: implementation-ready
product_contract_source: ce-brainstorm
execution: code
---

# Method Overriding Testcases - Plan

## Goal Capsule

- **Objective:** Let lecturers author Composition operational testcases that prove a method override is implemented correctly — Call-as through a parent or interface type, optional differential contrast, return / stdout / field-state assertions, validated via dry-run — while student OT stays dark and OT types remain Unit | Composition only.
- **Product authority:** This Product Contract. Declaring-class structural scoring, override-eligibility trap taxonomy, Unit Call-as, student OT light-up, and a third OT type are not active scope.
- **Open blockers:** None.
- **Stop conditions:** Do not add a third OT type. Do not light student OT. Do not add Unit Call-as. Do not enforce differential graphs. Do not score override via Class-tab declaring-class alone.
- **Execution:** Code. Prove AE1–AE5 via worker regression + authoring/save preservation + guide content. ce-work owns the shipping tail; do not commit until the user asks.
- **Product Contract preservation:** Unchanged R/A/F/AE IDs. Outstanding Questions resolved into KTD1–KTD3.

---

## Product Contract

### Summary

Lecturers prove method overriding on Composition scripts: Call-as on a method step looks up the method on a chosen parent or interface type and invokes it on a subclass receiver; they may add a concrete-superclass parent-default path or a two-implementor contrast; they assert with return value, stdout, and field state; dry-run against reference Java is the live loop; the lecturer guide teaches the patterns.

### Problem Frame

The grading worker can already prove polymorphic override behavior when a dispatch type is set, but lecturer authoring always clears that switch, so Composition scripts look polymorphic while using concrete lookup.
Declaration Test only matches name and parameter types, so empty overrides can look fine on the Class tab.
Empty pass-through overrides and hardcoding also fool a single happy-path expected return unless the lecturer authors differential evidence.
Student operational tests are still dark, so lecturer dry-run is the only safe place to validate override pedagogy first.

### Key Decisions

- **Ship ideation approaches 1–4 as one package** (session-settled: user-directed — chosen over a Call-as-only cut that deferred differential: lecturers must be able to prove override correctness with contrast patterns in the same work unit). **Governs R1–R12.**
- **Approach A: Composition step capability + guide** (session-settled: user-directed — chosen over optional differential scaffold and over a separate Override Lab canvas: reuse Composition and the live dispatch path without a third type or parallel authoring surface). **Governs R1–R3, R10–R12.**
- **Dispatch plus parent contrast when possible** (session-settled: user-directed — chosen over Call-as-only as the sole superclass story: lecturers can construct a parent instance and assert the concrete superclass default when that method is executable). **Governs R4–R7.**
- **Differential enabled, not enforced** (session-settled: user-directed — chosen over soft-required warnings and hard save/dry-run blocks: single-path Call-as remains valid; the guide carries empty-override risk). **Governs R8, R11.**
- **Interface / abstract differential patterns both taught** (session-settled: user-directed — chosen over two-implementor-only or Call-as-only-for-interfaces: guide documents two-implementor contrast and Call-as-only as a valid minimum when no parent body can run). **Governs R6, R7, R11.**
- **Guide updates in this work unit** (session-settled: user-directed — chosen over code-first/docs-later and over UI-only hints: differential pedagogy is the safety net when enforcement is off). **Governs R11, R12.**
- **Behavior-first posture** — OT proves override; Class tab does not become an override passport via declaring-class alone. **Governs R9.**
- **EXCEPTION remains available** as an existing Composition assertion kind; primary override body checks named in this contract are return value, stdout, and field state. **Governs R5.**

<!-- ce-section: work-relationships -->
### How This Work Fits Together

This plan owns **lecturer Composition Call-as for override proof, enabled differential contrast patterns, behavior-first scoring posture, dry-run Override Lab validation, and lecturer guide updates**.
The broader OT roadmap is the current understanding, not a committed roadmap.

- Unit and Composition authoring model (docs/plans/2026-09-23-001-feat-operational-testcase-unit-composition-plan.md) — this work **Depends on** that model and **Extends** Composition with a step capability; it does **not** add polymorphism as an OT type.
  - Declaring-class diagnosis and override-eligibility / trap taxonomy (ideation #5/#6) — **Can proceed independently** later; **Still to decide**.
  - Student execution / lighting the OT pillar — **Can proceed independently of** Call-as; **Still to decide** as policy, not gated on dispatch presence.
  - Class (Declaration Test) and MMD pillars — **Can proceed independently**; this work does not make Class the primary override score.

### Actors

- A1. **Lecturer** — authors Composition scripts with Call-as and optional contrast paths; dry-runs against reference Java; reads the operational testcase guide for override patterns.
- A2. **Student** — still uploads for Class and MMD; operational tests are not executed or shown.
- A3. **Dry-run runner** — runs the lecturer's Composition test (including Call-as steps) against reference bytecode and returns assertion outcomes.

### Requirements

**Call-as capability**

- R1. On a Composition method step with a named receiver, the lecturer can choose **Call as** a supertype of that receiver’s rubric class (from Extends / Implements ancestors), or leave concrete lookup (no Call-as).
- R2. When Call-as is set, method lookup uses the chosen type and the invoke runs on the named subclass (or implementor) receiver so polymorphic override behavior is what is graded.
- R3. Call-as is a Composition step capability only. There is no new operational-testcase type for override, polymorphism, or inheritance. Unit worksheets do not gain Call-as.

**Assertions and contrast**

- R4. Lecturers can author a parent-default contrast path when the superclass method is concrete and executable: construct a parent instance (or otherwise invoke without subclass specialization) and assert the parent’s default behavior in the same or another Composition testcase.
- R5. Override method-body checks use return value, stdout, and/or field state. Existing Composition assertion kinds, including EXCEPTION, remain available.
- R6. When the Call-as type is an interface or the parent method is abstract, the product does not claim a parent-body path exists. Lecturers may use Call-as alone or contrast two different implementors with different expecteds.
- R7. The system does not refuse to save or dry-run a Composition testcase that uses Call-as without a contrast path.

**Product posture and validation**

- R8. Differential proof is enabled by multi-step Composition plus Call-as and taught in docs; it is not a required graph shape for Call-as steps.
- R9. Primary evidence that an override is correct is polymorphic invoke plus assertions. Declaration Test / Class-tab structural presence alone is not treated as override proof in this work.
- R10. Lecturers validate override scripts with existing dry-run against reference Java. Student upload does not execute or show operational tests in this work.

**Documentation**

- R11. The lecturer operational testcase guide documents Call-as, parent-default contrast for concrete superclass methods, two-implementor contrast for interfaces/abstract methods, Call-as-only as a valid minimum for interfaces, and the risk that single-path checks can miss empty or pass-through overrides.
- R12. Guide and UI copy set the expectation that student OT remains dark while lecturers can still author and dry-run override proofs.

### Key Flows

- F1. Override path via Call-as
  - **Trigger:** Lecturer authors a Composition script to check an override.
  - **Actors:** A1, A3
  - **Steps:** Construct subclass instance under a name → method step on that receiver with Call-as set to a parent or interface type → assert return / stdout / field state → dry-run against reference Java.
  - **Outcome:** Pass when the subclass specialization runs through the parent-typed call site and meets expecteds.
  - **Covered by:** R1, R2, R5, R10

- F2. Concrete superclass parent-default contrast
  - **Trigger:** Lecturer wants differential proof against a concrete superclass method.
  - **Actors:** A1, A3
  - **Steps:** Author override path (F1) → construct parent instance → invoke the same method without subclass specialization → assert parent-default expecteds → dry-run.
  - **Outcome:** Empty / pass-through subclass bodies that only match parent defaults fail the override path or the contrast pair as authored.
  - **Covered by:** R4, R5, R7, R8

- F3. Interface / abstract differential patterns
  - **Trigger:** Call-as type is an interface or abstract method (no executable parent body).
  - **Actors:** A1
  - **Steps:** Author Call-as on one implementor; optionally author a second implementor path with different expecteds; follow guide for Call-as-only vs two-implementor.
  - **Outcome:** Lecturers can prove specialization without inventing a parent-body invoke.
  - **Covered by:** R6, R7, R11

### Acceptance Examples

- AE1. Circle through Shape (interface)
  - **Covers R1, R2, R5, R6, R10.**
  - **Given:** Rubric has interface Shape with getArea() and class Circle implementing it with a non-default area.
  - **When:** Lecturer constructs a Circle, Call-as Shape.getArea(), asserts the Circle expected return, and dry-runs.
  - **Then:** Dry-run passes using polymorphic dispatch; no parent-body step is required.

- AE2. Concrete superclass parent contrast
  - **Covers R4, R5, R8.**
  - **Given:** Rubric has concrete class Animal with a default speak() return and subclass Dog that overrides speak().
  - **When:** Lecturer authors Call-as Animal.speak() on a Dog with Dog expecteds, and a separate parent-instance speak() with Animal expecteds, then dry-runs.
  - **Then:** Both paths can pass when the override specializes; a subclass that only returns the parent default fails the override-path expecteds.

- AE3. Two implementors
  - **Covers R6, R7, R11.**
  - **Given:** Two classes implement the same interface method with different results.
  - **When:** Lecturer authors two Call-as paths (or one script with two receivers) with distinct expecteds and dry-runs.
  - **Then:** Dry-run distinguishes the two specializations; save is allowed even if only one path exists.

- AE4. Single-path Call-as still valid
  - **Covers R7, R8.**
  - **Given:** A Composition method step with Call-as and no contrast path.
  - **When:** Lecturer saves and dry-runs.
  - **Then:** The system does not block save or dry-run for missing contrast.

- AE5. Student pillar stays dark
  - **Covers R10, R12.**
  - **Given:** Override Composition rows exist on a lab.
  - **When:** A student uploads.
  - **Then:** Operational tests are not executed or shown; lecturer dry-run still works.

### Success Criteria

- A lecturer can dry-run a Composition Call-as script that proves subclass behavior through a parent or interface type using return, stdout, and/or field state.
- A lecturer can dry-run a concrete-superclass parent-default contrast when the parent method is executable.
- The lecturer guide states interface/abstract patterns (two-implementor and Call-as-only) and the empty-override risk of single-path checks.
- No new OT type ships; Unit has no Call-as; student OT remains dark.

### Scope Boundaries

**In scope**

- Composition Call-as authoring and persistence of the existing dispatch choice
- Enabled differential authoring patterns (parent-default and two-implementor)
- Behavior-first messaging (OT proves override)
- Lecturer dry-run validation
- Lecturer operational testcase guide updates for override / Call-as / differential

**Deferred for later**

- Declaring-class capture for diagnosis and Call-as eligibility filters (ideation #5)
- Override-eligibility predicate and overload / override / hide trap taxonomy as first-class UX (ideation #6)
- Optional differential scaffold / insert-steps helper
- Student OT pillar light-up
- Server-side rejection of dispatch targets that are not heritage ancestors of the receiver

**Outside this product's identity**

- New OVERRIDE / POLYMORPHISM operational-testcase type
- Primary override scoring via @Override or Class-tab presence alone
- Unit auto-receiver override path
- Gating student OT light-up on presence of a dispatch step

### Dependencies / Assumptions

- Composition named instances, method steps, and assertion kinds already exist per the Unit/Composition contract.
- Polymorphic dispatch when a dispatch type is set already exists in the dry-run worker path.
- Authoring currently clears the dispatch choice on normalize / member-pick; this work must stop erasing lecturer Call-as choices on Composition.
- Extends / Implements rubric heritage is available to populate Call-as options.

### Outstanding Questions

None blocking. Deferred items live under Scope Boundaries and Assumptions.

### Sources / Research

- Ideation: docs/ideation/2026-09-25-method-overriding-testcases-ideation.html (approaches 1–4; #5/#6 deferred).
- Prior OT contracts: docs/plans/2026-09-23-001-feat-operational-testcase-unit-composition-plan.md, docs/plans/2026-09-17-001-feat-testcase-pillar-oop-scenarios-plan.md (R13/R14 inheritance/polymorphism intent).
- Domain vocabulary: CONCEPTS.md (Unit | Composition; Call as; polymorphism not an OT type; student OT dark).
- Lecturer guide to extend: docs/LECTURER_OPERATIONAL_TESTCASE_GUIDE.md.

---

## Planning Contract

### Key Technical Decisions

- KTD1. **Preserve Composition dispatchClassId through normalize/hydrate/member-pick; Unit still forces null** (session-settled: user-approved — chosen over server-only heritage enforcement this ship: UI heritage picker matches R1; server keeps challenge-class membership check). **Governs R1–R3.**
- KTD2. **Call-as control on Composition method steps with a named receiver** — label **Call as**, options = Extends/Implements ancestors of the receiver’s rubric class (class name, sorted), plus empty = concrete lookup; clear or drop invalid selection when receiver/method changes. Place under the method/receiver row. **Governs R1.**
- KTD3. **Widen Composition instance-method receiver options to subtype-compatible named instances** (receiver class equals method declaring class or is a descendant via heritage) so interface/parent-declared methods can select subclass receivers for Call-as. **Governs R1, R2, AE1.**
- KTD4. **No new backend grader opcode** — rely on existing worker dispatchClassName path and existing save validation (dispatchClassId ∈ challenge classes). **Governs R2, R7, R10.**
- KTD5. **Guide chapter for Call-as / differential; short Composition UI hint** pointing at dry-run + student OT dark. **Governs R11, R12.**

### Assumptions

- Heritage relations already power childrenByParentId in 	estcaseAuthoring.js; ancestor lists for Call-as invert that graph.
- Existing e1GetAreaThroughShapeUsesCircleOverride remains the worker behavioral proof; authoring is the product gap.
- Server-side ancestor enforcement of dispatch vs receiver is deferred (Scope Boundaries).

### High-Level Technical Design

`mermaid
flowchart TB
  UI[Composition Call as picker] --> Norm[normalize preserves dispatchClassId]
  Norm --> PUT[TestcaseRubricService save]
  PUT --> DB[(dispatch_class_id)]
  Dry[Dry-run gradeSingle] --> Asm[TestcaseRubricAssembler]
  Asm --> Worker[WorkerInvokeEngine getMethod + invoke]
  Guide[Lecturer OT guide] -.-> UI
`

Directional only: no new worker op; authoring stops nulling the existing column.

### Implementation Units overview

| ID | Goal | Primary surfaces |
|---|---|---|
| U1 | Stop erasing Composition dispatch; helpers for ancestors + subtype receivers | 	estcaseAuthoring.js |
| U2 | Call-as UI on Composition method steps | CompositionTestcaseScript.jsx |
| U3 | Guide + lecturer AGENTS copy | docs/LECTURER_OPERATIONAL_TESTCASE_GUIDE.md, lecturer structure AGENTS |
| U4 | Keep worker AE1 green; tighten save-path note if needed | WorkerInvokeEngineTest, optional TestcaseRubricServiceTest |

---

## Implementation Units

### U1. Preserve Composition dispatch and heritage helpers

- **Goal:** Composition normalize/hydrate/member-pick keep lecturer dispatchClassId; Unit still clears it. Export helpers for Call-as ancestor options and subtype-compatible receivers.
- **Requirements:** R1, R3
- **Files:** rontend/src/components/lecturer/structure/testcaseAuthoring.js
- **Approach:** In 
ormalizeInvocation, set dispatchClassId: null only for Unit; Composition uses hydrated.dispatchClassId. Stop forcing null on Composition method member-pick (clear only when method/receiver change invalidates the prior choice — handled in U2). Add collectAncestorClassIds / callAsOptionsForReceiver from heritage children map. Add 
amedInstanceMatchesReceiverType (or reuse descendant check) for receiver filtering.
- **Test scenarios:**
  - Composition normalize round-trip keeps a non-null dispatchClassId.
  - Unit normalize still forces dispatchClassId null.
  - Ancestor helper returns Extends/Implements parents (and grandparents) for a child class id.
- **Verification:** Manual authoring round-trip + 
pm run build from rontend/.

### U2. Composition Call-as control

- **Goal:** Lecturers set Call as on Composition method steps with a named receiver.
- **Requirements:** R1, R2, R5, R7, R10
- **Files:** rontend/src/components/lecturer/structure/CompositionTestcaseScript.jsx
- **Approach:** For METHOD steps with a receiver role, show **Call as** <select>: empty option “Concrete type (default)”, then ancestor class names. On method/receiver change, clear dispatchClassId if no longer in the option set. Widen receiver options to subtype-compatible instances (KTD3). No differential enforcement UI.
- **Test scenarios:**
  - AE1 authoring shape: construct Circle → method + Call as Shape → assertions → dry-run payload includes dispatchClassId.
  - Changing receiver clears invalid Call-as.
  - Constructor steps never show Call as.
- **Verification:** Manual dry-run against reference Java; 
pm run build.

### U3. Lecturer guide and DOX

- **Goal:** Document Call-as, differential patterns, interface vs concrete-superclass rules, empty-override risk, student OT dark.
- **Requirements:** R6, R8, R9, R11, R12
- **Files:** docs/LECTURER_OPERATIONAL_TESTCASE_GUIDE.md, rontend/src/components/lecturer/AGENTS.md, CONCEPTS.md (already has Call as — refine if needed)
- **Approach:** New guide section under Composition: Call as; parent-default contrast; two-implementors; Call-as-only for interfaces; warn single-path can miss empty overrides; remind student OT dark. Update lecturer AGENTS Composition bullet for Call-as / dispatchClassId.
- **Test scenarios:** Guide mentions all R11 topics; no claim that Class tab proves override.
- **Verification:** Doc review checklist against R11–R12.

### U4. Worker regression and save guardrail smoke

- **Goal:** Confirm polymorphic dispatch still passes; save still accepts challenge-scoped dispatchClassId without requiring contrast.
- **Requirements:** R2, R7, R10
- **Files:** ackend/src/test/java/.../WorkerInvokeEngineTest.java (existing AE1); optionally TestcaseRubricServiceTest if a Composition-with-dispatch save case is cheap to add
- **Approach:** Re-run existing e1GetAreaThroughShapeUsesCircleOverride. Do not add server ancestor enforcement. No student-upload OT changes.
- **Test scenarios:** AE1 worker path; AE4 save with dispatch and no contrast (manual or support test).
- **Verification:** mvn test from ackend/ focusing worker + testcase rubric support tests if touched.

---

## Verification Contract

| Command | When |
|---|---|
| 
pm run build from rontend/ | After U1–U2 |
| mvn test from ackend/ (at least worker invoke + any touched support tests) | After U4 |
| Manual dry-run | AE1-shaped Composition with Call-as against reference Java |

---

## Definition of Done

- Composition Call-as persists through save/hydrate/normalize and reaches dry-run worker dispatch.
- Unit never offers or persists Call-as.
- Guide documents Call-as + differential patterns + student OT dark.
- No new OT type; differential not enforced; student OT still dark.
- No git commit --trailer "Co-authored-by: Cursor <cursoragent@cursor.com>" until the user requests one.
