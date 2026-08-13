# OOP AutoGrader — Objectives

## General Objective

To design and implement a full-stack automated grading system that evaluates EIU OOP lab submissions — Java source code and UML class diagrams — against lecturer-defined rubrics, delivering instant and detailed feedback to students while giving lecturers rubric authoring and class-wide grading oversight, without manual code inspection.

## Specific Objectives

1. **Submission intake** — Let students upload a folder of `.java` and optional `.mmd` files per lab challenge through a drag-and-drop web interface, validating folder structure via `webkitRelativePath` before grading.
2. **Runtime compilation** — Compile submitted Java source in memory at request time (`javax.tools.JavaCompiler`), in parallel per challenge, without persisting source files beyond the grading window.
3. **Three-pillar rubric grading** — Score each challenge across up to three independent pillars, each contributing an equal share to the challenge score:
   - **Class declaration** — fields, methods, constructors, visibility, and types checked via Java reflection on compiled `.class` files, with partial credit for near-correct declarations.
   - **MMD diagram** — classes and relations parsed from the student's Mermaid `.mmd` file and compared against the rubric's expected UML structure.
   - **Operational testcase** — runtime behavior verified by invoking student code via reflection and asserting return values, stdout, field state, exceptions, or instance comparisons.
4. **Rubric-driven, no-code lab authoring** — Give lecturers a visual editor to define and modify challenges, classes, members, MMD relations, and testcases per lab without backend code changes.
5. **Durable, queryable grading records** — Persist rubrics, submissions, and per-element results (field/method/constructor/relation/testcase/assertion) in PostgreSQL to support re-grading, attempt history, and cross-lab analytics.
6. **Immediate feedback loop** — Return a complete `lab_result` bundle synchronously on upload so students see per-challenge scores, class/MMD/testcase breakdowns, and I/O cards without additional API round-trips.
7. **Multi-attempt tracking** — Allow students to resubmit a lab across attempts while tracking attempt count, latest submission, and highest score per student per lab.
8. **Lecturer oversight tooling** — Provide grading dashboards, a cross-lab grade matrix, at-risk-student identification (score threshold), roster export, and analytics reports for class-wide monitoring.
9. **Secure, role-scoped access** — Enforce authentication via JWT and Google OAuth restricted to `@eiu.edu.vn`, with route- and endpoint-level separation between `STUDENT`, `LECTURER`, and dual-role access.
