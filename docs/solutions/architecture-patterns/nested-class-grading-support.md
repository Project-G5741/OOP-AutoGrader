---
title: "Nested and Static Class Grading Support (Pen.PenBuilder)"
date: 2026-08-21
category: architecture-patterns
module: grading
problem_type: architecture_pattern
component: service_object
severity: high
related_components:
  - database
  - documentation
tags:
  - nested-classes
  - static-nested-classes
  - inner-classes
  - reflection-parsing
  - qualified-name-matching
  - class-rubric
  - outer-class
  - constructor-param-stripping
applies_when:
  - "A student submission's rubric or lab structure includes a nested class (e.g. Outer.Inner such as Pen.PenBuilder)"
  - "The reflection-based class parser needs to load compiled dollar-suffixed nested class files (Outer$Inner.class)"
  - "The grader must match classes by qualified name instead of simple name to avoid collisions"
  - "A non-static inner class constructor has an implicit outer-instance parameter that must be excluded from rubric parameter comparison"
  - "Only one level of nesting needs to be supported (Outer$Inner), not deeper multi-level nesting"
---

# Nested and Static Class Grading Support (Pen.PenBuilder)

> **Status note**: as of this writing, changes described below are on branch
> `feat/nested-class-grading-support` (uncommitted or in-progress). File references
> are to current-tree state, not a specific commit.

## Context

OOP AutoGrader grades student `.class` files via reflection against a rubric stored in `class_entity` and related member tables. Java nested classes compile to files named `Outer$Inner.class` (or `Outer$Inner$1.class` for local/anonymous classes). Two independent gaps made a rubric class like `Pen.PenBuilder` ungradable:

1. `ReflectionClassParser` listed compiled classes and dropped anything with `$` in the filename, so `Pen$PenBuilder.class` was never loaded.
2. `ClassReflectionGrader` resolved rubric classes to parsed classes by **simple name only**. There was no way to express or match "the `PenBuilder` nested inside `Pen`" versus any top-level `PenBuilder`.

There was also no rubric authoring path for "this class is nested inside that class" or "this nested class is static" — `class_entity` had no outer-class link and no static flag.

## Guidance

When a rubric class can be nested, the fix spans four layers that must stay consistent:

1. **Compiled-file filtering** — allow exactly one level of nesting (`Outer$Inner.class`) while still rejecting multi-`$` names (anonymous/local/synthetic classes).
2. **Parsing** — capture outer simple name and static modifier per class (`ParsedClass.outerSimpleName`, `ParsedClass.isStatic`).
3. **Resolution** — key nested classes by **qualified name** (`Outer.Inner` via `ClassRubric.qualifiedName()`), not simple name. Top-level classes still resolve by simple name for backward compatibility. `ParsedClassIndex` builds the two lookup maps; `ChallengeGradingContext.resolve()` picks the correct one.
4. **Constructor comparison** — strip the compiler-synthesized implicit outer-instance first parameter for **non-static** nested classes only, before comparing against the rubric parameter list.

The DB, backend rubric model, and frontend structure editor need `outer_class_id` (self-referencing FK on `class_entity`, `ON DELETE CASCADE`) and `is_static` (boolean, default `false`). Operator SQL lives in `docs/sql/2026-08-21-class-entity-outer-class.sql` and `docs/sql/2026-08-21-class-entity-is-static.sql`.

## Why This Matters

- **Silent ungradability**: any rubric containing a nested class (builder pattern, iterator, node classes) scored the nested class as missing for every student, even with correct code.
- **Inner vs static nested**: whether a nested class is `static` changes its relationship to the enclosing instance. Grading this requires `Modifier.isStatic()` comparison, but only when the rubric class is nested.
- **Constructor false negatives**: non-static inner classes get an invisible first constructor parameter of the enclosing type; without stripping it, constructor rubric rows never match.

## When to Apply

- A rubric or comparison feature must identify a class that can be nested, and simple-name lookup is ambiguous or wrong.
- A grading feature must load `$`-named `.class` files and distinguish user-declared nested classes (one `$`) from anonymous/local/synthetic classes (multiple `$`).
- Constructor grading on non-static inner classes must account for compiler-injected implicit parameters.

Do **not** apply outer-param stripping to static nested classes or top-level classes.

## Examples

### 1. Allow exactly one level of nesting when filtering compiled class files

```58:65:backend/src/main/java/com/eiu/capstone/backend/grading/ReflectionClassParser.java
    private boolean isSupportedClassFile(Path classFile) {
        String baseName = classFile.getFileName().toString().replace(".class", "");
        int firstDollar = baseName.indexOf('$');
        if (firstDollar < 0) {
            return true;
        }
        return baseName.indexOf('$', firstDollar + 1) < 0;
    }
```

### 2. Capture outer name and static modifier while parsing

```67:79:backend/src/main/java/com/eiu/capstone/backend/grading/ReflectionClassParser.java
    private ParsedClass parseClass(Class<?> clazz) {
        ParsedClass parsed = new ParsedClass();
        parsed.simpleName = clazz.getSimpleName();
        Class<?> enclosing = clazz.getEnclosingClass();
        if (enclosing != null) {
            parsed.outerSimpleName = enclosing.getSimpleName();
        }

        int modifiers = clazz.getModifiers();
        parsed.scope = scopeOf(modifiers);
        parsed.declaringType = declaringTypeOf(clazz);
        parsed.isAbstract = Modifier.isAbstract(modifiers) && !clazz.isInterface();
        parsed.isStatic = Modifier.isStatic(modifiers);
```

### 3. Qualified-name resolution

```42:51:backend/src/main/java/com/eiu/capstone/backend/grading/rubric/ClassRubric.java
    public boolean isNested() {
        return outerClassName != null && !outerClassName.isBlank();
    }

    public String qualifiedName() {
        if (outerClassName == null || outerClassName.isBlank()) {
            return name;
        }
        return outerClassName + "." + name;
    }
```

```26:31:backend/src/main/java/com/eiu/capstone/backend/grading/ParsedClassIndex.java
    public ParsedClass resolve(ClassRubric expectedClass) {
        if (expectedClass.isNested()) {
            return byQualifiedName.get(expectedClass.qualifiedName());
        }
        return byName.get(expectedClass.name());
    }
```

### 4. Static check only for nested rubric classes

```57:63:backend/src/main/java/com/eiu/capstone/backend/grading/pipeline/ClassReflectionGrader.java
            List<Boolean> classChecks = new ArrayList<>();
            classChecks.add(PartialCreditEvaluator.matches(expectedClass.scope(), parsed.scope).get(0));
            classChecks.add(PartialCreditEvaluator.matches(expectedClass.declaringType(), parsed.declaringType).get(0));
            classChecks.add(expectedClass.isAbstract() == parsed.isAbstract);
            if (expectedClass.isNested()) {
                classChecks.add(expectedClass.isStatic() == parsed.isStatic);
            }
```

### 5. Strip implicit outer-instance constructor parameter (non-static inner only)

```137:150:backend/src/main/java/com/eiu/capstone/backend/grading/pipeline/ClassReflectionGrader.java
    private ParsedConstructor findMatchingConstructor(List<ParsedConstructor> candidates,
                                                      List<String> expectedParamTypes,
                                                      boolean stripImplicitOuterParam,
                                                      String outerSimpleName) {
        for (ParsedConstructor pc : candidates) {
            List<String> actualParams = pc.parameterTypes;
            if (stripImplicitOuterParam
                    && outerSimpleName != null
                    && !outerSimpleName.isBlank()
                    && !actualParams.isEmpty()
                    && equalsIgnoreCase(actualParams.get(0), outerSimpleName)) {
                actualParams = actualParams.subList(1, actualParams.size());
            }
            if (sameTypes(actualParams, expectedParamTypes)) {
                return pc;
            }
```

### 6. Frontend qualified-name display

```1:6:frontend/src/utils/classNaming.js
export function formatQualifiedClassName(cls, classes) {
  if (!cls?.name) return 'Untitled class';
  if (!cls.outerClassId) return cls.name;
  const outer = (classes || []).find((candidate) => candidate.id === cls.outerClassId);
  return outer ? `${outer.name}.${cls.name}` : cls.name;
}
```

### 7. Operator setup for Pen lab

1. Run SQL migrations in order: `outer_class_id`, then `is_static`, then lab-specific rubric seed (`docs/sql/2026-08-21-pen-penbuilder-rubric.sql`).
2. In Solution Management, set `PenBuilder` outer class to `Pen`, check **Static nested**, save.
3. Re-upload student submissions to re-score (no backfill of historical grades).

## Related

- [In-memory challenge compile path](./in-memory-challenge-compile-path.md) — flat per-challenge compile output directory; nested `.class` files land in the same folder
- [Conditional pillar scoring with dynamic weights](./conditional-pillar-scoring-dynamic-weights.md) — precedent for backward-compatible rubric DTO/schema evolution
- [Operational testcase grading](./operational-testcase-grading.md) — rubric loading discipline (`LabRubricService` pre-fetch pattern)
- `CONCEPTS.md` — **Qualified rubric class name**, **Outer-class link**, **Static nested flag**
