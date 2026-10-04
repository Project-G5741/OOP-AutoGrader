# Testcase kernel

## Purpose

Spring-free JSON coerce and value compare used by both the API scoring path and the isolated worker JAR.

## Ownership

| File | Role |
|---|---|
| `JsonValueCoercer.java` | Coerce rubric JSON to Java values; encode values to JSON |
| `ValueComparator.java` | Compare actual vs expected with `ComparisonMode` |
| `JavaTypeResolver.java` | Map rubric type names to `Class` objects |

## Local Contracts

- No Spring types. This package is packaged into `backend-1.0.0-worker.jar`.
- Student `URLClassLoader` URLs must not include this package.
- API code may wrap `JsonValueCoercer` with a `@Component` subclass in `grading.testcase`.
- `JavaTypeResolver.resolve(String)` still rejects unknown types (v1 scalars/wrappers/`String` and those arrays).
- `resolve(String, ClassLoader)` / `resolveAll(types, loader)` load rubric class names such as `Engine` (and arrays of those) via `Class.forName(..., true, loader)`.
- `JsonValueCoercer.coerceParams` resolves JSON objects `{"$instance":"<name>"}` through a `Function<String,Object>` supplied by the worker engine. Other params stay scalars. Do not JSON-encode live objects.

## Work Guidance

Keep types here free of harness, JPA, and Spring so the thin worker JAR stays self-contained.

## Verification

`WorkerJarIsolationTest` scans this package for Spring descriptors.

## Child DOX Index

No child docs.
