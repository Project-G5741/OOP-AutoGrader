---
title: In-memory per-challenge compile on the submission upload path
date: 2026-08-09
category: architecture-patterns
module: submission pipeline
problem_type: architecture_pattern
component: service_object
severity: medium
applies_when:
  - "Optimizing student folder upload before grading"
  - "Adding parallel javac workers without stealing the grading pool"
  - "Isolating compile failures per challenge folder"
tags:
  - submission-upload
  - javac
  - compile-executor
  - performance
  - memory-source
---

# In-memory per-challenge compile on the submission upload path

## Context

Student uploads arrive as a multipart folder of `.java` and `.mmd` files. Before reflection grading runs, each challenge's Java sources must compile to `.class` files in that challenge's classes output directory. The legacy path copied every `.java` into a temporary sources folder, invoked `javac` from disk, deleted the staging folder, and ran parallel compiles on the **grading** executor — adding redundant I/O and pool contention with the grading phase.

## Guidance

### Keep compiled output on disk; compile sources from memory

Grading still reads `.class` files from disk via `ReflectionClassParser`. Only the **source** staging step moves in-memory. Compiled output stays under each challenge folder's `classes` subdirectory (layout: submission request id → challenge folder → `classes`).

1. `SubmissionStorageService.validateAndGroup()` groups multipart files by challenge in one pass and rejects mixed root folders early.
2. Each challenge compiles on `compileExecutor` (bean `app.compile.parallelism`, default 4).
3. `MemorySourceJavaFileObject` wraps multipart bytes as `JavaFileObject` sources — no `_sources_tmp` directory.
4. `JavaCompilerService.compileSources()` writes output to the challenge classes directory via `-d`.

### Reuse compiler infrastructure per worker thread

`JavaCompilerService` holds one `ToolProvider.getSystemJavaCompiler()` instance and a `ThreadLocal<StandardJavaFileManager>`. Pass `null` as the listener when creating the file manager; per-task diagnostics go through the `DiagnosticCollector` passed to `getTask()`. On compile failure or `task.call()` runtime errors, close and remove the thread-local manager so the worker thread can recover.

### Wire executors explicitly

Two `ExecutorService` beans exist: `compileExecutor` and `gradingExecutor`. Inject with `@Qualifier("compileExecutor")` on `SubmissionStorageService` and `@Qualifier("gradingExecutor")` on `GradingService`. Pool sizing is shared via `FixedExecutorFactory` (CPU-capped fixed pool, daemon worker threads).

### Isolate failures per challenge

`processChallenge` must **never throw** to `CompletableFutures.joinAll`. Catch `RuntimeException` at the challenge boundary and return `ChallengeResult` with `compileError` set. Use `failedChallenge(..., cleanupTarget, ...)` to delete only the failed subtree (the classes directory for javac errors, whole challenge folder for I/O/setup failures).

Do **not** delete the entire submission folder when one worker throws — sibling challenges with successful compiles must keep their compiled class trees for grading.

Source paths use challenge-relative logical names (for example a nested model/Student.java path under one challenge) with duplicate-path detection; `MemorySourceJavaFileObject.toSourceUri()` encodes path segments for safe string URIs (spaces and reserved characters).

### Count and timing

`countClassFiles` uses flat `Files.list` on the challenge `classes` directory (matches `ReflectionClassParser` flat assumption). Propagate `IOException` from listing as `compileError`, not silent `classFileCount=0`. Optional `compile_timing` logs when `app.grading.timing-log=true`.

## Why This Matters

Upload latency was dominated by per-challenge disk copy + fresh `javac` bootstrap. In-memory sources and reused file managers remove the staging I/O; a dedicated compile pool prevents upload bursts from starving grading workers. Per-challenge error results preserve partial success — one syntax error must not wipe compiled output for other challenges.

## When to Apply

- Changing `SubmissionStorageService`, `JavaCompilerService`, or upload parallelism
- Tuning Render/small-instance memory (`app.compile.parallelism` alongside `app.grading.parallelism`)
- Debugging missing compiled classes or misleading zero class counts after compile

## Examples

**Before (disk staging):**

```text
challenge_1/_sources_tmp/Student.java  → javac → challenge_1/classes/
                                         → delete _sources_tmp
```

**After (in-memory):**

```java
sources.add(new MemorySourceJavaFileObject(sourcePath, file.getBytes()));
javaCompilerService.compileSources(sources, classesFolder);
```

**Per-challenge failure (do not abort upload):**

```java
try {
    javaCompilerService.compileSources(sources, classesFolder);
} catch (RuntimeException e) {
    return failedChallenge(challengeName, challengeFolder, start, classesFolder,
            buildSourcesMs, javacMs, 0, runtimeErrorMessage(e));
}
```

## Related

- Plan: `docs/plans/2026-08-09-001-perf-submission-compile-path-plan.md`
- Ops: `backend/DEPLOY_RENDER.md` (`app.compile.parallelism`)
- Grading still consumes compiled classes via reflection — see `CONCEPTS.md` (Grading pipeline)
