# Lecturer guide — operational testcases

> **Who this is for:** Lecturers setting up **operational testcases** in Solution Management for the first time.  
> **What operational testcases do:** They run the student’s compiled Java code (construct objects, call methods) and check whether the results match what you expect — separate from the Class tab (signatures) and the MMD tab (diagram).

---

## Table of contents

1. [Where to find the editor](#1-where-to-find-the-editor)
2. [Before you write your first testcase](#2-before-you-write-your-first-testcase)
3. [How the editor is laid out](#3-how-the-editor-is-laid-out)
4. [The two testcase types](#4-the-two-testcase-types)
5. [Type A — scenario test (`SINGLE_INVOCATION`)](#5-type-a--scenario-test-single_invocation)
6. [Assertions — what you can check](#6-assertions--what-you-can-check)
7. [OOP principle tags (what students see)](#7-oop-principle-tags-what-students-see)
8. [Recipes by principle](#8-recipes-by-principle)
9. [Type B — comparison test (`COMPARISON`)](#9-type-b--comparison-test-comparison)
10. [Example vs hidden testcases](#10-example-vs-hidden-testcases)
11. [Dry-run — test before students submit](#11-dry-run--test-before-students-submit)
12. [Saving and common mistakes](#12-saving-and-common-mistakes)
13. [Quick reference](#13-quick-reference)

---

## 1. Where to find the editor

1. Sign in as a lecturer and open **Projects → Solution Management**.
2. Select a **lab** in the left sidebar, then a **challenge** in the structure tree.
3. In the challenge panel on the right, open the **Operational Testcases** tab.
4. Click **+ Add testcase** to create one, or select an existing testcase in the list on the left.

Operational testcases are saved with **Save Testcases** at the bottom of this tab. That is **separate** from **Save Structure** (classes, fields, methods, MMD). You need both when you change rubric members and testcases in the same session.

---

## 2. Before you write your first testcase

| Step | Why it matters |
|------|----------------|
| Define **classes, fields, constructors, and methods** in the structure tree | Dropdowns in the testcase editor only list rubric members you already saved |
| Click **Save Structure** after adding members | Unsaved structure shows a warning; new methods will not appear in testcase dropdowns |
| Prepare a **reference solution** (correct `.java` files) | Dry-run compiles and runs your testcase against this code — not against student uploads |
| Set **Operational testcase weight** on the challenge (if needed) | Controls how much testcase results contribute vs Class and MMD |

You can author up to **20 steps** per scenario testcase. Each testcase can have many assertions.

---

## 3. How the editor is laid out

```
┌─────────────────────────────────────────────────────────────┐
│  Reference Java (dry-run)     ← upload your solution .java    │
├─────────────────────────────────────────────────────────────┤
│  Testcase list │  Name, Run, Delete                         │
│  (left)        │  Dry-run result card                       │
│                │  Type, OOP principle, steps, assertions    │
├─────────────────────────────────────────────────────────────┤
│                              [ Save Testcases ]             │
└─────────────────────────────────────────────────────────────┘
```

- **Left list:** All testcases for this challenge. Icons show dry-run pass/fail. Tags like `Polymorphism` or `hidden` appear as small labels.
- **Name:** A short label for you (e.g. `deposit increases balance`). Students see this on example testcases.
- **Run:** Dry-run **only this** testcase against the reference Java you uploaded.
- **Run all:** Dry-run every testcase in the list (needs reference Java).

---

## 4. The two testcase types

| Type | Plain English | Use when |
|------|---------------|----------|
| **SINGLE_INVOCATION** | A **scenario**: one or more ordered steps that build and use named objects | Most tests — one call, a sequence, encapsulation, composition, inheritance behaviour, polymorphism |
| **COMPARISON** | Build **two objects** (A and B) and check whether they are equal or how `compareTo` behaves | `equals` / `compareTo` contracts, symmetry, consistency |

You pick the type in the **Type** dropdown. Switching to **COMPARISON** replaces scenario steps with two instance builders. Switching back to **SINGLE_INVOCATION** gives you scenario steps again.

---

## 5. Type A — scenario test (`SINGLE_INVOCATION`)

A scenario is a **script** that runs top to bottom. Each **step** is either:

- **CONSTRUCTOR** — create an object and optionally give it a **name**
- **METHOD** — call a method (on a receiver you build in that step, or via a named instance in params)

Later steps can use objects created in earlier steps.

### 5.1 Single-step test (simplest case)

**Goal:** Call `new BankAccount(100)` and check that balance is `100`.

| Field | Value |
|-------|--------|
| Step 1 — Kind | CONSTRUCTOR |
| Constructor | `BankAccount.<init>(...)` |
| Instance name | `account` (optional for one step, but good habit) |
| Params | `[100]` |
| Assertion | FIELD_STATE on `BankAccount.balance`, expected `100` |

This is the same as the old “one invocation” style — one step is a valid scenario.

### 5.2 Multi-step test (sequence)

**Goal:** Create an account, deposit, then check balance.

| Step | Kind | What to set |
|------|------|-------------|
| 1 | CONSTRUCTOR | `BankAccount`, instance name `acct`, params `[0]` |
| 2 | METHOD | `deposit`, params `[50]` — pass the receiver by putting `{"$instance":"acct"}` in params (use the **$instance** button) |
| Assertion on step 2 | FIELD_STATE | `balance` = `50` |

**Rule:** A **named instance** must be created in an **earlier** CONSTRUCTOR step before you reference it with `{"$instance":"name"}`.

### 5.3 Step fields explained

#### CONSTRUCTOR step

| Field | Meaning |
|-------|---------|
| **Constructor** | Which rubric constructor to call |
| **Instance name** | A label you choose (e.g. `car`, `parent`, `wallet`). Used in later steps. |
| **Params (JSON array)** | Arguments in order, e.g. `[10, "Alice"]`, `[null]`, `[]` |

#### METHOD step

| Field | Meaning |
|-------|---------|
| **Method** | Which rubric method to call |
| **Receiver constructor (optional)** | If the class has **no** no-arg constructor, pick the constructor that builds the object you call the method on |
| **Receiver params (JSON array)** | Arguments for that receiver constructor |
| **Dispatch class** | Parent class or interface to invoke **through** (for polymorphism — see below). Leave as **Concrete class** for normal calls. |
| **Params (JSON array)** | Method arguments. Use scalars or `{"$instance":"name"}` for object arguments. |

**Receiver vs params:** The **receiver** is the object you call the method **on** (`account.deposit(50)` → receiver is `account`). **Params** are the method’s parameters (`50`).

If the class has a no-arg constructor, leave **Receiver constructor** as “No-arg ctor on class” and receiver params as `[]`.

### 5.4 Passing a named object as an argument

Params are a **JSON array**. For a rubric object type, insert a reference:

```json
[{"$instance": "acct"}]
```

Or mix scalars and instances:

```json
[100, {"$instance": "other"}]
```

In the UI, click **$instance** next to Params to append a reference to a name from earlier constructor steps.

### 5.5 Dispatch class (polymorphism)

When students should use **dynamic dispatch** (override on a subclass, not `instanceof` chains):

1. Set **OOP principle** to **Polymorphism**.
2. On at least one **METHOD** step, set **Dispatch class** to the **parent class or interface** from the rubric (not only the concrete class).
3. The grader looks up the method on that type and runs the student’s override.

**Save is blocked** until a Polymorphism testcase has at least one METHOD step with a dispatch class. Constructor steps do not count.

**Example:** Rubric has `Shape` (interface) and `Circle` (implements Shape). Step calls `draw()` with dispatch class `Shape` on a `Circle` instance — student must override `draw`, not switch on type.

---

## 6. Assertions — what you can check

Each assertion belongs to a **step** (for scenario tests) and has a **kind**, **expected value**, and **comparison mode**.

### Assertion kinds

| Kind | Checks | Expected value examples |
|------|--------|-------------------------|
| **RETURN_VALUE** | What the call returned | `42`, `"hello"`, `true`, `null` |
| **FIELD_STATE** | A field on the object after the step | Pick **Field** from dropdown; expected e.g. `100` or `"open"` |
| **STDOUT** | Text printed to standard output | `"Account opened\n"` (string JSON) |
| **EXCEPTION** | That the call threw a specific exception | `"IllegalArgumentException"` or `{"type":"IllegalArgumentException"}` |
| **COMPARISON_RESULT** | Only for **COMPARISON** testcases | `true`/`false` (equals) or `-1`/`0`/`1` (compareTo) |

### Comparison modes (for text-like expected values)

| Mode | Behaviour |
|------|-----------|
| **EXACT** | Character-for-character match |
| **TRIMMED** | Ignores leading/trailing whitespace |
| **NORMALIZED_WHITESPACE** | Collapses internal whitespace differences |

### Tips

- You can add **multiple assertions** on the same step (e.g. return value **and** a field).
- **FIELD_STATE** must reference a field from the dropdown — only field assertions may set a field.
- For **EXCEPTION**, use the simple name (`NullPointerException`), not the full package.
- Empty expected values are treated as `null` when you save.

### JSON in expected values

Use JSON literals:

- Numbers: `0`, `3.14`
- Strings: `"paid"`
- Boolean: `true`, `false`
- Null: `null`

---

## 7. OOP principle tags (what students see)

Every testcase has one **OOP principle** tag. It is a **label you choose** — the system does not guess the student’s design.

| Tag | Typical use | Extra rule |
|-----|-------------|------------|
| **Unit** | Default; single behaviour or simple call | None |
| **Polymorphism** | Override + call through parent/interface | Must set **dispatch class** on at least one METHOD step before save |
| **Encapsulation** | Illegal update rejected; defensive copies; getters do not leak internals | None |
| **Composition** | Object owns another; state follows “has-a” | None |
| **Inheritance** | Subclass behaviour beyond empty override | None |

On **example** testcases (`Hidden` unchecked), students see the tag on the I/O card (e.g. a **Polymorphism** badge). It names the topic; it is not auto-generated feedback.

---

## 8. Recipes by principle

### Unit — “does the method work?”

**BankAccount withdraw**

1. CONSTRUCTOR `BankAccount` → name `a`, params `[100]`
2. METHOD `withdraw` → params `[30]` with receiver built from `a` via `$instance`
3. Assert FIELD_STATE `balance` = `70` on step 2

---

### Polymorphism — “does override run through the parent type?”

**Shapes**

Rubric: interface `Drawable` with `draw()`, class `Square` implements `Drawable`.

1. CONSTRUCTOR `Square` → name `sq`, params `[]`
2. METHOD `draw` on `Square` → dispatch class **`Drawable`**, params use `{"$instance":"sq"}` as needed for receiver/args per your API
3. Assert STDOUT or RETURN_VALUE as you designed

Tag: **Polymorphism**. Without dispatch class on step 2, **Save Testcases** stays disabled.

---

### Encapsulation — “bad update must not stick”

**Immutable or validated setter**

1. CONSTRUCTOR → name `item`, params `[10]`
2. METHOD `setValue` with invalid arg (e.g. `[-1]`) using `$instance` receiver
3. Assert FIELD_STATE `value` still `10` on step 2  
4. (Optional) Assert EXCEPTION `IllegalArgumentException` on the same step

Students who only check in `if` without enforcing state still fail the field assertion.

---

### Composition — “part and whole stay in sync”

**Engine inside Car**

1. CONSTRUCTOR `Engine` → name `eng`, params `[200]`
2. CONSTRUCTOR `Car` → name `car`, params with `{"$instance":"eng"}` if constructor takes an Engine
3. METHOD on `car` that uses the engine
4. Assert FIELD_STATE on `car` or `eng` showing composed state

---

### Inheritance — “subclass really behaves”

**Animal / Dog**

1. CONSTRUCTOR `Dog` → name `d`, params `[]`
2. METHOD `speak` on `d`
3. Assert RETURN_VALUE or STDOUT matches what a proper override produces (not the parent default if you expect override)

Tag: **Inheritance**. Declaration Test already checks that `speak` exists; this testcase checks **behaviour**.

---

## 9. Type B — comparison test (`COMPARISON`)

Use when the learning goal is **equality or ordering**, not a sequence of calls.

1. Set **Type** to **COMPARISON**.
2. Choose **EQUALS** or **COMPARE_TO**.
3. Configure **Instance A** and **Instance B**:
   - Pick a **constructor** for each
   - Set **params** JSON for each (e.g. `[1, "x"]` and `[1, "x"]`)
4. Add assertion **COMPARISON_RESULT**:
   - For **EQUALS**: expected `true` or `false`
   - For **COMPARE_TO**: expected `-1`, `0`, or `1`

**Note:** COMPARISON testcases do **not** use scenario steps or named instances. OOP principle tag still applies for student display on example tests.

**Example:** Two `Point` objects with same coordinates should be equal — both constructors `[3, 4]`, method EQUALS, assertion COMPARISON_RESULT `true`.

---

## 10. Example vs hidden testcases

| Setting | Student sees |
|---------|----------------|
| **Hidden** unchecked (example testcase) | Name, **OOP principle** tag, expandable **input / expected / actual** I/O |
| **Hidden** checked | **Pass or fail only** — no inputs, outputs, or principle tag |

Use **example** testcases to teach what you are checking. Use **hidden** for exam-style checks you do not want to reveal.

---

## 11. Dry-run — test before students submit

Dry-run runs your testcase against **reference Java**, not student code.

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

## 12. Saving and common mistakes

### Save Testcases

- Enabled when you have unsaved edits and validation passes.
- **Polymorphism** without dispatch class → save blocked; yellow warning explains why.
- Removing a testcase from the list and saving deletes it from the challenge (sync-by-presence).

### Common mistakes

| Mistake | What happens | Fix |
|---------|----------------|-----|
| Forgot **Save Structure** before testcase | Dropdowns missing new methods | Save structure, reload testcases tab |
| Use `$instance` before constructor | Save error / dry-run error | Add constructor step with that name first |
| Polymorphism tag, no dispatch class | Cannot save | Set dispatch class on a METHOD step |
| Wrong JSON in params | 422 or dry-run error | Use valid JSON arrays; quote strings |
| FIELD_STATE without picking a field | Save error | Select field in assertion row |
| COMPARISON_RESULT on scenario test | Invalid | Use only on COMPARISON type, or use RETURN_VALUE/FIELD_STATE |
| Expect dry-run without reference Java | Toast error | Upload reference `.java` files |

### What students need

Students upload their own `.java` (and `.mmd` if required). The grader runs the **same** testcase definitions you saved, against **their** compiled code. Example testcases help them understand what “good” behaviour looks like; hidden ones do not.

---

## 13. Quick reference

### Scenario step limits

- Max **20** steps per testcase
- Steps run in order; if an early constructor throws, later steps may be skipped and the testcase fails
- First failing step drives the main I/O card students see

### Params and instances cheat sheet

```json
[]                          → no arguments
[1, 2, 3]                   → three int arguments
["text"]                    → one String
[null]                      → null argument
[{"$instance": "myObj"}]    → pass named instance from earlier step
```

### Assertion cheat sheet

| I want to check… | Assertion kind |
|------------------|----------------|
| Return value | RETURN_VALUE |
| Field after call | FIELD_STATE + pick field |
| println output | STDOUT |
| Exception thrown | EXCEPTION |
| two instances equal / compareTo | COMPARISON type + COMPARISON_RESULT |

### Related docs

- [USER_GUIDE.md](./USER_GUIDE.md) — full lecturer and student UI tour  
- [CONCEPTS.md](../CONCEPTS.md) — glossary (operational testcase, scenario, dispatch type, etc.)

---

*Last updated for the scenario-step operational testcase editor (named instances, OOP principle tags, dispatch class).*
