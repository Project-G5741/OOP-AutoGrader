---
title: Operational METHOD invocations fail without no-arg constructor
date: 2026-08-11
category: logic-errors
module: backend-grading
problem_type: logic_error
component: service_object
symptoms:
  - "METHOD testcase invocations fail on student classes with only parameterized constructors (e.g. Car(int, String))"
  - "Re-seeding testcase rows after rubric edits triggers FK violations when stale LabRubricCache still references old testcase UUIDs"
  - "Void-method testcases seeded with RETURN_VALUE assertions mis-grade; FIELD_STATE is the correct assertion kind for post-invoke field checks"
  - "Student I/O card stdout may show literal \\r\\n when JSON-escaped values are not unescaped or trailing line endings are not trimmed"
root_cause: logic_error
resolution_type: code_fix
severity: high
tags:
  - operational-testcase
  - invocation-runner
  - receiver-constructor
  - lab-rubric-cache
  - assertion-kind
  - stdout-display
---

# Operational METHOD invocations fail without no-arg constructor

## Problem

Operational testcase `METHOD` invocations on classes that only define parameterized constructors (e.g. `Car(int yearModel, String make)`) failed at grade time. `InvocationRunner` attempted to call `instantiateDefault(clazz)`, which requires a no-arg constructor, and surfaced `NoSuchMethodException: Instance method requires a no-argument constructor on Car` instead of invoking the method on a properly constructed receiver.

## Symptoms

- Upload/grading failed for `SINGLE_INVOCATION` testcases whose `testcase_invocation.invocation_kind = 'METHOD'` targeted instance methods on classes without a default constructor.
- Error message was explicit once wrapped: `Instance method requires a no-argument constructor on Car` (`InvocationRunner.java:157-158`).
- Car challenge METHOD rows (`accelerate`, `brake`, getters) could not be seeded or graded; seed script documented the platform limitation (`docs/sql/2026-08-11-car-challenge-testcases.sql:8-12`, `290-295`).
- After enabling receiver columns, re-seeding testcase rows without restarting the backend could produce FK violations when persisted results still referenced deleted testcase UUIDs held in stale `LabRubricCache` entries.
- Void methods like `accelerate()` graded incorrectly when assertions used `RETURN_VALUE` or `STDOUT` instead of `FIELD_STATE` on the `speed` field.
- Captured stdout displayed literal `\r\n` escape sequences in the student UI instead of rendered line endings.

## What Didn't Work

### Assuming every instance method runs on a default-constructed receiver

Before the fix, non-static method invocations always went through `instantiateDefault(clazz)` when `hasReceiver()` was false (`InvocationRunner.java:108-111`). That path calls `clazz.getDeclaredConstructor()` with no parameters and throws the wrapped `NoSuchMethodException` when none exists (`InvocationRunner.java:151-158`). Adding a no-arg constructor to the student rubric was not acceptable for assignments that intentionally require `Car(int, String)`.

### Re-seeding testcases without restarting the backend

`LabRubricCache` holds an in-process `LabRubricSnapshot` per lab with a configurable TTL (default 30 minutes; `LabRubricCache.java:21-22`, `application.properties:19`). Operator SQL that deletes and re-inserts testcase rows changes UUIDs, but a warm backend process can still serve the cached snapshot containing old testcase IDs. Subsequent grading or persistence then hits foreign-key violations against the new rows. The cache exposes `invalidate(UUID labId)` for rubric mutations (`LabRubricCache.java:48-55`), and `RubricCacheInvalidationSupport.invalidateLab` is the application-level hook — but raw SQL re-seeds bypass that path unless the operator restarts the backend or waits for TTL expiry.

### Using RETURN_VALUE or STDOUT for void mutators

`accelerate()` returns `void`; there is no return value to assert. The Car seed documents that METHOD rows for `accelerate` must use `FIELD_STATE` on the `speed` field (`docs/sql/2026-08-11-car-challenge-testcases.sql:322-328`). `RETURN_VALUE` applies to methods with a meaningful return; `STDOUT` only matches captured `System.out`, which `accelerate()` does not produce unless the student prints.

### Displaying raw JSON-escaped stdout

Stdout assertions store captured output as JSON strings. Without backend unescaping, embedded `\r\n` can appear literally in API payloads; without frontend trimming, trailing line endings may still show. `TestcaseDisplayFormatter.stripQuotes` parses JSON string values via `JsonValueCoercer.coerceExpectedValue` before display (`TestcaseDisplayFormatter.java:141-149`), and `StudentUI.jsx` `formatIoDisplay` strips **trailing** line-ending characters (including escaped forms) for presentation only (`StudentUI.jsx:16-19`).

## Solution

### 1. Schema: receiver constructor + params on `testcase_invocation`

Migration `docs/sql/2026-08-11-testcase-invocation-receiver.sql` adds:

- `receiver_constructor_id UUID` → FK to `constructor(id) ON DELETE CASCADE`
- `receiver_params JSONB NOT NULL` for constructor arguments

JPA entity `TestcaseInvocation` maps both columns.

Example seed shape for `Car(2020, "Toyota")` then `accelerate()`:

```sql
INSERT INTO testcase_invocation
    (testcase_id, invocation_kind, method_id, receiver_constructor_id, receiver_params, params)
VALUES
    ('<testcase-id>', 'METHOD', '<accelerate-method-id>', '<car-constructor-id>', '[2020, "Toyota"]', '[]');
```

See `docs/sql/2026-08-11-operational-testcase-seed-sample.sql:24-28`.

### 2. Rubric loading: `LabRubricService.methodInvocationRubric()`

When building `InvocationRubric` for `METHOD` invocations, `methodInvocationRubric` resolves `receiverConstructorId`, `receiverClassName`, `receiverParameterTypes`, and `receiverParamsJson` from the invocation's receiver constructor FK and pre-fetched maps — avoiding lazy loads after the repository session closes (`LabRubricService.java:323-347`).

### 3. `InvocationRubric.hasReceiver()`

```java
public boolean hasReceiver() {
    return receiverConstructorId != null && receiverClassName != null && !receiverClassName.isBlank();
}
```

(`InvocationRubric.java:22-24`)

### 4. `InvocationRunner`: construct receiver before method invoke

For non-static methods, the runner chooses the receiver path based on `hasReceiver()`:

```java
if (!Modifier.isStatic(method.getModifiers())) {
    receiver = invocation.hasReceiver()
            ? instantiateReceiver(loader, invocation)
            : instantiateDefault(clazz);
}
```

(`InvocationRunner.java:108-111`)

`instantiateReceiver` delegates to `instantiateWithConstructor` with `receiverClassName`, `receiverParameterTypes`, and `receiverParamsJson` (`InvocationRunner.java:132-148`).

### 5. INPUT display shows receiver setup

`TestcaseDisplayFormatter.formatInvocationInput` renders a two-line INPUT when a receiver is configured:

```java
if (invocation.hasReceiver()) {
    String receiverSetup = "new " + invocation.receiverClassName()
            + "(" + formatArgs(invocation.receiverParamsJson()) + ")";
    return receiverSetup + "\n" + invocation.className() + "." + invocation.methodName() + "(" + args + ")";
}
```

(`TestcaseDisplayFormatter.java:91-95`)

### 6. Assertion kind for void mutators

Use `FIELD_STATE` on the mutated field (e.g. `speed = 5` after one `accelerate()` call), not `RETURN_VALUE` or `STDOUT`.

### 7. Stdout display fixes

- Backend: `stripQuotes` in `TestcaseDisplayFormatter` JSON-unescapes quoted `STDOUT` values before they reach the API
- Frontend: `formatIoDisplay` in `StudentUI.jsx` trims trailing line endings on I/O card output (`StudentUI.jsx:16-19`)

## Why This Works

The root cause was a runner assumption: every instance method could be invoked on `new ClassName()` via a no-arg constructor. Classes like `Car` violate that assumption by design.

The fix separates **how to obtain the receiver** from **which method to call**:

| Concern | Mechanism |
|---------|-----------|
| Receiver construction | `receiver_constructor_id` + `receiver_params` identify which rubric constructor and JSON args to use |
| Rubric graph | `LabRubricService.methodInvocationRubric()` materializes receiver metadata into `InvocationRubric` while entities are still attached |
| Runtime invoke | `InvocationRunner.instantiateReceiver()` uses the same `instantiateWithConstructor` path as CONSTRUCTOR invocations |
| Fallback | When `hasReceiver()` is false, behavior is unchanged — `instantiateDefault` for classes that do provide a no-arg ctor |
| Student visibility | INPUT card shows `new Car(2020, "Toyota")` on one line and `Car.accelerate()` on the next |

`FIELD_STATE` assertions read the receiver object's field after invocation, which is the correct observable for void mutators. Display-layer unescaping fixes presentation without changing grading semantics.

## Prevention

1. **Seed METHOD testcases with receiver columns when the class lacks a no-arg constructor.** Follow `docs/sql/2026-08-11-operational-testcase-seed-sample.sql`. Pair void methods with `FIELD_STATE` on the field they mutate.

2. **After operator SQL that replaces testcase rows, invalidate or refresh the rubric cache.** Either restart the backend, call `LabRubricCache.invalidate(labId)` via application mutation paths, or wait for TTL expiry (`app.grading.rubric-cache-ttl-minutes`, default 30). Raw SQL re-seeds do not trigger `RubricCacheInvalidationSupport.invalidateLab` automatically.

3. **Choose assertion kind by what the code actually produces:**
   - Method returns a value → `RETURN_VALUE`
   - Method mutates fields → `FIELD_STATE`
   - Method prints to stdout → `STDOUT`
   - Method throws → `EXCEPTION`

4. **When debugging I/O display, distinguish grading from presentation.** Grading compares raw captured stdout; `stripQuotes` and `formatIoDisplay` only affect what students see.

5. **Repository fetch for invocations should JOIN receiver constructor** so `methodInvocationRubric` has the FK available (`TestcaseInvocationRepository` — `LEFT JOIN FETCH i.receiverConstructor`).

## Related Issues

- [Operational testcase grading patterns and pitfalls](../architecture-patterns/operational-testcase-grading.md) — pillar architecture; update stale no-arg-constructor guidance when extending receiver construction
- `docs/sql/2026-08-11-testcase-invocation-receiver.sql` — schema migration
- `docs/sql/2026-08-11-car-challenge-testcases.sql` — Car challenge seed with receiver + FIELD_STATE examples
- `backend/src/main/java/com/eiu/capstone/backend/grading/AGENTS.md` — local grading contracts including receiver constructor support
