# Concepts

Shared domain vocabulary for this project — entities, named processes, and status concepts with project-specific meaning. Seeded with core domain vocabulary, then accretes as ce-compound and ce-compound-refresh process learnings; direct edits are fine. Glossary only, not a spec or catch-all.

## Grading pipeline

### Lab submission
A student's single graded attempt for a lab, keyed by user, lab, and attempt number. One row in `lab_submission`; re-uploading the same attempt updates scores in place rather than creating a new attempt row.

### Submission result
A persisted per-element grading outcome (field, method, constructor, or challenge) tied to one lab submission. Natural key is submission plus rubric element id; re-grades update the same row.

### Rubric snapshot
An in-memory, immutable graph of the lab's expected OOP structure (challenges, classes, members, class relations) loaded once per grading request, optionally from cache.

### MMD grading
Diagram-side grading of an uploaded `.mmd` file: parse Mermaid class syntax into the same rubric entity shapes used for Java reflection, compare against the solution, and persist per-element pass/fail for the MMD tab. When both `.java` and `.mmd` are present, a rubric element counts correct only if both sources pass.

## Relationships

- A **lab submission** owns many **submission results** (one per rubric element graded).
- Grading compares compiled student classes against a **rubric snapshot**, then writes **submission results**.

### Student lab progress
A per-(student, lab) tracking row holding highest score, attempt count, and best/latest submission metadata. Updated on upload; used as enrichment for lecturer analytics, not as the roster denominator when enrolled non-submitters must appear.

### Term enrollment
Maps an active student to a term (`term_enrollment`). The lecturer lab roster paginates enrolled students for the lab's term, then LEFT JOINs `student_lab_progress` and submission/challenge data per student.

### Lecturer lab roster
The unique set of enrolled/active students for a lab's term/course. Challenge and overview tables paginate this population; submission and progress data are LEFT JOINed per student afterward.

### Dual-role user
A `user_account` row with both `STUDENT` and `LECTURER` in `user_role`, optionally holding different `student_code` and `teacher_code` values. Login accepts either code; post-login routing defaults to the lecturer dashboard; student routes remain reachable by URL when the JWT includes both roles.
