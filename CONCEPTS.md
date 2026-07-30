# Concepts

Shared domain vocabulary for this project — entities, named processes, and status concepts with project-specific meaning. Seeded with core domain vocabulary, then accretes as ce-compound and ce-compound-refresh process learnings; direct edits are fine. Glossary only, not a spec or catch-all.

## Grading pipeline

### Lab submission
A student's single graded attempt for a lab, keyed by user, lab, and attempt number. One row in `lab_submission`; re-uploading the same attempt updates scores in place rather than creating a new attempt row.

### Submission result
A persisted per-element grading outcome (field, method, constructor, or challenge) tied to one lab submission. Natural key is submission plus rubric element id; re-grades update the same row.

### Rubric snapshot
An in-memory, immutable graph of the lab's expected OOP structure (challenges, classes, members) loaded once per grading request, optionally from cache.

## Relationships

- A **lab submission** owns many **submission results** (one per rubric element graded).
- Grading compares compiled student classes against a **rubric snapshot**, then writes **submission results**.
