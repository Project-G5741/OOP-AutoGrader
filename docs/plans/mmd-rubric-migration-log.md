# MMD rubric migration log

Date: 2026-08-18  
Plan: `docs/plans/2026-08-18-001-feat-mmd-grading-revamp-plan.md` (U11)

## In-repo diagram samples

| Source | Status | Notes |
|---|---|---|
| `grading-mermaid-oop-class-diagrams.md` §5 cheat sheet | ✅ Parses | Verified via `MmdReferenceDocMatrixTest` |
| `MmdParserTest` fixtures | ✅ Parses | Existing integration tests green |
| `MmdComparisonServiceTest` AE1–AE5 | ✅ Parses | End-to-end acceptance |
| `MmdRelationParseTest` | ✅ Parses | §1.6 arrows, cardinality, lollipop |
| `MmdMemberParseTest` | ✅ Parses | §1.1–§1.7 members |
| `MmdMiscDirectiveTest` | ✅ Parses | §1.11–§1.12 directives |

## Parser contract changes (lecturer rubrics)

| Change | Migration action |
|---|---|
| `classDiagram` header required for substantive content | Add header line to any bare `class X` lecturer diagrams |
| Bare class names with spaces | Use `class Name["Display Name"]` label syntax |
| Angle-bracket generics in members | Prefer `List~T~`; `List<T>` still compares via `MmdTypeEquivalence` |
| Missing space before return type (`method()type`) | Fix spacing to `method() type` |
| `<<Abstract>>` on diagram vs rubric `CLASS` declaring type | No rubric change — comparison maps abstract stereotype to `CLASS` |

## Database-only lecturer diagrams

No `.mmd` seed files are checked into this repository. Lecturer solution diagrams stored only in PostgreSQL must be re-validated manually after deploy:

1. Open Solution Management → each challenge MMD tab.
2. Confirm diagram still renders and saves without parser errors.
3. If parse fails, apply header/label/generic fixes above and re-save.

## Deferred (not graded in this revamp)

- Multiplicity/cardinality on relations (parsed, not scored)
- OOP-concept linting beyond rubric element match
