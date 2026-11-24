# Compile

## Purpose

In-memory javac of student sources, per-class error attribution, and the Class-card compile-error convention.

## Ownership

| Type | Responsibility |
|---|---|
| `CompileClassAttribution` | Map ERROR diagnostics to declared types, file stems, and dependents |
| `CompileErrorMessage` | One short student-facing line per failed type |
| `StudentSourceNormalizer` | Strip `package` and same-challenge imports before compile |
| `RubricCompileSourceFilter` | Keep only sources that declare a rubric class simple name (lecturer must list every needed type; extras like `Application` are omitted) |
| `JavaCompilerService` (parent package) | Group javac via `javax.tools`; empty `CLASS_PATH`; `-proc:none`; mixed failure wipes partial `.class` output then remainder-compiles independents |

## Local Contracts

Class tab `ClassDetailDTO.error` and testcase ERROR feedback use `CompileErrorMessage` (one wrapping line, no `ERROR:` prefix).

| javac case | Class-card line |
|---|---|
| `'X' expected` | `Missing X on line N` |
| cannot find symbol | `{Symbol} not found` |
| file named `Observer.java` does not declare Observer | `Wrong class in this file` |
| public `NewsAgency` declared in `Observer.java` | `Declared in Observer.java` |
| nested / static type in a matching outer file | same line as the outer type |
| reached end of file | `Unclosed class on line N` |
| duplicate class | `Duplicate class` |
| illegal start | `Invalid syntax on line N` |
| failed only because another type failed | `See {Upstream}` |
| other | first diagnostic line, `on line N` when known |

`ClassStructureService` re-summarizes stored dumps so older `ERROR: line N:` / `Compilation Error on X` rows follow this table.

## Work Guidance

- Upload/dry-run compile input is filtered by `RubricCompileSourceFilter` when a lab rubric is available: keep a `.java` file only if it declares at least one challenge rubric class simple name (including nested `outerClassName`). Non-rubric files are not compiled and get no Class-card feedback. Solution import does not filter.
- Happy path stays one group javac. On mixed failure, wipe first-pass `.class` output (partial emission is untrusted) and always remainder-compile sources with no ERROR diagnostic that are not attributed dependents. Remainder-compile diagnostics are not root syntax errors.
- Nested/static types that belong in `Outer.java` must not get `Declared in Outer.java`.
- Do not store raw javac dumps on Class cards.

## Verification

- `support` `CompileErrorMessageTest`
- `support` `CompileClassAttributionTest`
- `support` `RubricCompileSourceFilterTest`
- `support` `JavaCompilerServiceTest` (JDK types compile; Spring classpath types do not)
- `support` `SubmissionStorageServiceTest` (`omitsNonRubricApplicationEvenWhenBroken`)

## Child DOX Index

No child docs.
