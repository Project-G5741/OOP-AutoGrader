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

## Work Guidance

Keep types here free of harness, JPA, and Spring so the thin worker JAR stays self-contained.

## Verification

`WorkerJarIsolationTest` scans this package for Spring descriptors.

## Child DOX Index

No child docs.
