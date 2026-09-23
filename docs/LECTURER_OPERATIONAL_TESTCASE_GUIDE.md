# Lecturer guide — operational testcases

> **Who this is for:** Lecturers setting up **operational testcases** in Solution Management.  
> **What operational testcases are:** Checks that construct objects and call methods on compiled Java, separate from the Class tab (signatures) and the MMD tab (diagram).  
> **This ship:** You author **Unit** and **Composition** tests and dry-run them against reference Java. Student upload still grades Class and MMD only. Students do not run operational tests and do not see an Operation Test tab.

---

## Table of contents

1. [Where to find the editor](#1-where-to-find-the-editor)
2. [Before you write your first testcase](#2-before-you-write-your-first-testcase)
3. [How the editor is laid out](#3-how-the-editor-is-laid-out)
4. [The two testcase types](#4-the-two-testcase-types)
5. [Unit — closed worksheet](#5-unit--closed-worksheet)
6. [Composition — named-object script](#6-composition--named-object-script)
7. [Assertions — what you can check](#7-assertions--what-you-can-check)
8. [Object checks](#8-object-checks)
9. [Example vs hidden](#9-example-vs-hidden)
10. [Dry-run — test against reference Java](#10-dry-run--test-against-reference-java)
11. [Saving and common mistakes](#11-saving-and-common-mistakes)
12. [Wipe and re-author (operators)](#12-wipe-and-re-author-operators)
13. [What students see this ship](#13-what-students-see-this-ship)
14. [Quick reference](#14-quick-reference)

---

## 1. Where to find the editor

1. Sign in as a lecturer and open **Projects → Solution Management**.
2. Select a **lab** in the left sidebar, then a **challenge** in the structure tree.
3. In the challenge panel on the right, open the **Operational Testcases** tab.
4. Click **Add Unit** or **Add Composition**, or select an existing testcase in the list on the left.

Operational testcases are saved with **Save Testcases** at the bottom of this tab. That is **separate** from **Save Structure** (classes, fields, methods, MMD). You need both when you change rubric members and testcases in the same session.

---

## 2. Before you write your first testcase

| Step | Why it matters |
|------|----------------|
| Define **classes, fields, constructors, and methods** in the structure tree | Dropdowns in the testcase editor only list rubric members you already saved |
| Click **Save Structure** after adding members | Unsaved structure shows a warning; new methods will not appear in testcase dropdowns |
| Prepare a **reference solution** (correct `.java` files) | Dry-run compiles and runs your testcase against this code — not against student uploads |
| Set **Operational testcase weight** on the challenge if you want it later | The field stays on the challenge editor. It has **no student score effect** while the student Operation Test tab is hidden |

---

## 3. How the editor is laid out

```
┌─────────────────────────────────────────────────────────────┐
│  Reference Java (dry-run)     ← upload your solution .java    │
├─────────────────────────────────────────────────────────────┤
│  Testcase list │  Name, Type, Run, Delete                    │
│  (left)        │  Dry-run result card                        │
│                │  Unit worksheet  or  Composition steps      │
├─────────────────────────────────────────────────────────────┤
│                              [ Save Testcases ]             │
└─────────────────────────────────────────────────────────────┘
```

- **Left list:** All testcases for this challenge. Icons show dry-run pass/fail. A small **Unit** or **Composition** label marks the type.
- **Name:** A short label for you (e.g. `deposit increases balance`). Students do not see operational tests this ship.
- **Type:** Switch between Unit and Composition. Switching **replaces** the other flow’s graph (worksheet vs script).
- **Run:** Dry-run **only this** testcase against the reference Java you uploaded.
- **Run all:** Dry-run every testcase in the list (needs reference Java).

---

## 4. The two testcase types

| Type | Plain English | Use when |
|------|---------------|----------|
| **Unit** | Closed worksheet: one constructor or method, then assertions | A single call. No named objects, no object-typed arguments |
| **Composition** | Named-object script: ordered steps that share named objects | Sequences, object arguments, equals() against another live object |

You choose the type when you click **Add Unit** or **Add Composition**. These are two different canvases, not one scenario with a lock.

There is no two-instance comparison type. For equality, write a Composition that constructs two named objects and assert **equals()** on one of them.

---

## 5. Unit — closed worksheet

A Unit test is **exactly one** constructor or method, then at least one assertion. You cannot add a second invocation.

### 5.1 Constructor

**Goal:** Call `new BankAccount(100)` and check that `balance` is `100`.

1. **Member:** pick the `BankAccount` constructor.
2. **Arguments:** `100`.
3. **Assertion:** FIELD_STATE on `balance`, expected `100`. Or a type-only / field-map object check on the constructed instance.

The constructed instance is **not named**.

### 5.2 Instance method

**Goal:** Call `withdraw(30)` on a `BankAccount` and check `balance`.

1. **Member:** pick `withdraw`.
2. Dry-run builds a **hidden no-arg receiver**. That construct is not a step and is not named.
3. **Arguments:** scalars only (e.g. `30`). You cannot pass another rubric-class object.
4. **Assertions:** return value, stdout, field state on the hidden receiver, and/or exception.

If the class has **no no-arg constructor**, that instance method is disabled in the member list. Use Composition instead (construct the receiver yourself).

### 5.3 Static method

Pick a static method as the one invocation. There is no receiver.

### 5.4 What Unit cannot do

- A second invocation
- Named instances or `$instance` arguments
- Rubric-class objects as arguments (those belong in Composition)
- equals() against another live object
- Lecturer-chosen receiver constructor

---

## 6. Composition — named-object script

A Composition test is an ordered list of constructor and/or method steps (1–20) that share **named objects**. A step may have zero assertions. The testcase as a whole must have at least one.

### 6.1 Naming objects

| Step | What you name |
|------|----------------|
| **Constructor** | Required **instance name** for the constructed object (e.g. `acct`) |
| **Static factory** that returns a rubric-class object | Required **product name** (same `instanceName` field; that name is the product) |
| **Instance method** | **Receiver** — pick an earlier name of matching class. That name stays the receiver; the return does **not** overwrite it |

Later steps pass named objects as arguments with `{"$instance":"acct"}` (use the **$instance** control next to a rubric-class parameter).

Literal arguments are primitives, wrappers, String, null, and arrays of those. Arrays or lists of named objects are not valid as one argument.

### 6.2 Example — deposit then check balance

| Step | Kind | What to set |
|------|------|-------------|
| 1 | CONSTRUCTOR | `BankAccount`, instance name `acct`, params `[0]` |
| 2 | METHOD | `deposit`, receiver `acct`, params `[50]` |
| Assertion on step 2 | FIELD_STATE | `balance` = `50` (field on `acct`, or another named object already created) |

**Rule:** A name must exist from an **earlier** constructor or named static return before you use it as a receiver, `$instance` argument, or equals() target.

### 6.3 Example — two objects equal

1. CONSTRUCTOR `Point` → name `a`, params `[3, 4]`
2. CONSTRUCTOR `Point` → name `b`, params `[3, 4]`
3. On step 1 or 2, RETURN_VALUE (or the constructed object) object check **equals()**, `$instance` = the other name

Or call `equals` as a method on one named object and assert RETURN_VALUE `true`.

### 6.4 Unexpected throws

If a step throws and that throw is **not** an accepted EXCEPTION assertion, the sequence **stops**. Later steps do not run. Their assertions fail as not executed.

Cap: **20** steps and **10** named instances per testcase.

---

## 7. Assertions — what you can check

Assertions belong to a **step**. The testcase passes only when every configured assertion passes.

A value-returning method may assert return value, stdout, field state, and thrown exception. A void method may assert stdout, field state, and thrown exception (no return value). A constructor may assert the returned object, field state, and thrown exception (no stdout). An invocation expected to throw may still carry other allowed kinds for that target.

| Kind | Checks | Expected value examples |
|------|--------|-------------------------|
| **RETURN_VALUE** | What the call returned | `42`, `"hello"`, `true`, `null`, or an [object check](#8-object-checks) |
| **FIELD_STATE** | A field after the step | Pick **Field**; expected e.g. `100`. Composition may inspect any named object already created. Unit instance methods inspect the hidden no-arg receiver; Unit constructors inspect the constructed instance |
| **STDOUT** | Text printed to standard output | `"Account opened\n"` (string JSON) |
| **EXCEPTION** | That the call threw a specific exception type (not the message) | `"IllegalArgumentException"` |

### Comparison modes (for text-like expected values)

| Mode | Behaviour |
|------|-----------|
| **EXACT** | Character-for-character match |
| **TRIMMED** | Ignores leading/trailing whitespace |
| **NORMALIZED_WHITESPACE** | Collapses internal whitespace differences |

### Tips

- You can add **multiple assertions** on the same step.
- **FIELD_STATE** must reference a field from the dropdown.
- For **EXCEPTION**, use the simple name (`NullPointerException`), not the full package.
- Empty expected values are treated as `null` when you save.

### JSON in expected values

- Numbers: `0`, `3.14`
- Strings: `"paid"`
- Boolean: `true`, `false`
- Null: `null`

---

## 8. Object checks

For a non-primitive return value or constructor result, pick **one**:

| Check | JSON in `expectedValue` | When |
|-------|-------------------------|------|
| Type only | `{ "$objectCheck": "TYPE" }` | Right class, not null |
| Field map | `{ "$objectCheck": "FIELDS", "fields": { "balance": 100 } }` | One-level literals (same allowlist as arguments) |
| equals() | `{ "$objectCheck": "EQUALS", "$instance": "other" }` | **Composition only** — equals against another live named object |

Unit object checks are type-only or field map. There is no equals() on Unit (no second live instance).

---

## 9. Example vs hidden

Each testcase still has a **Hidden** checkbox. That flag is stored for a later student ship (example vs pass/fail-only). **Students do not see operational tests this ship**, so the checkbox does not change what they see today.

---

## 10. Dry-run — test against reference Java

Dry-run runs your Unit or Composition test against **reference Java**, not student code. It uses the same isolated worker `scenario` op as the grader. Student upload does **not** use that path.

1. In **Reference Java (dry-run)**, add one or more `.java` files (your correct solution). Class names must match the rubric.
2. Configure the testcase.
3. Click **Run** on that testcase, or **Run all**.

Results show PASS/FAIL, input, and per-assertion expected vs actual. Fix the testcase or reference code until dry-run passes.

**Requirements:**

- At least one reference file before Run works.
- Reference code must **compile** against the rubric.
- If you changed lab structure, save structure first so constructors/methods match.

Dry-run results are **not saved**; they clear when you edit the testcase.

---

## 11. Saving and common mistakes

### Save Testcases

- Enabled when you have unsaved edits and validation passes.
- Removing a testcase from the list and saving deletes it from the challenge (sync-by-presence). Child steps and assertions upsert by id; they are not deleted-all and reinserted.

### Common mistakes

| Mistake | What happens | Fix |
|---------|----------------|-----|
| Forgot **Save Structure** before testcase | Dropdowns missing new methods | Save structure, reload testcases tab |
| Unit instance method on a class with no no-arg constructor | Member disabled / save 422 | Use Composition and construct the receiver |
| `$instance` before the name exists | Save 422 | Constructor or named static return first |
| Unit `$instance`, named objects, or equals() | Save 422 | Use Composition |
| Wrong JSON in params | 422 or dry-run error | Valid JSON; quote strings |
| FIELD_STATE without picking a field | Save error | Select field in assertion row |
| Expect dry-run without reference Java | Toast error | Upload reference `.java` files |
| Expect students to see Operation Test results | Tab stays hidden | Class and MMD still grade; OT is lecturer + dry-run only |

---

## 12. Wipe and re-author (operators)

Shipping this rebuild **deletes** existing operational tests. There is no mapping from old one-step or two-instance comparison rows. Lecturers re-author as Unit or Composition.

Operator step:

1. Apply `docs/sql/2026-09-23-operational-testcase-unit-composition.sql` on a database copy first, then on the live DB.
2. Restart the API, or confirm `TestcaseSchemaMigrator` ran on startup (it applies the same wipe when leftover types/columns remain) and called `LabRubricCache.invalidateAll()`.
3. If you applied SQL **without** restarting, invalidate all lab rubric caches. Stale cache after wipe causes FK mismatches on later dry-run.

The SQL does not change Class, MMD, or challenge-level `testcase_weight`.

---

## 13. What students see this ship

Students upload `.java` (and `.mmd` if required). **Class** and **MMD** still grade. Operational tests are **not** invoked. The **Operation Test** tab stays hidden. Students never see the names Unit or Composition.

Hidden vs example remains stored for a later ship.

---

## 14. Quick reference

### Limits

- Unit: exactly one invocation
- Composition: max **20** steps, max **10** named instances
- Steps run in order; an unaccepted throw stops the sequence
- Dry-run first-failing step drives the main I/O card

### Params cheat sheet

```json
[]                          → no arguments
[1, 2, 3]                   → three int arguments
["text"]                    → one String
[null]                      → null argument
[{"$instance": "myObj"}]    → Composition: named object from an earlier step
```

### Assertion cheat sheet

| I want to check… | How |
|------------------|-----|
| Return value | RETURN_VALUE |
| Field after call | FIELD_STATE + pick field |
| println output | STDOUT |
| Exception thrown | EXCEPTION (type only) |
| Right class, not null | Object check TYPE |
| Object field map | Object check FIELDS |
| Two live objects equal | Composition + object check EQUALS (or call `equals`) |

### Related docs

- [USER_GUIDE.md](./USER_GUIDE.md) — full lecturer and student UI tour  
- [CONCEPTS.md](../CONCEPTS.md) — glossary (Unit, Composition, named instance, object check)

---

*Last updated for Unit and Composition authoring (lecturer dry-run; student operational-test pillar dark).*
