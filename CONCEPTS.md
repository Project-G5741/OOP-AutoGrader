# Concepts

Shared domain vocabulary for this project — entities, named processes, and status concepts with project-specific meaning. Seeded with core domain vocabulary, then accretes as ce-compound and ce-compound-refresh process learnings; direct edits are fine. Glossary only, not a spec or catch-all.

## Grading pipeline

### Submission upload compile
The pre-grading slice that receives a multipart folder, validates path structure, compiles each challenge's `.java` files in parallel, and writes `.class` output under `challenge_N/classes/`. Sources are compiled from memory; MMD files stay in the multipart map for grading without disk staging on the hot path.

### Lab submission
A student's single graded attempt for a lab, keyed by user, lab, and attempt number. One row in `lab_submission`; re-uploading the same attempt updates scores in place rather than creating a new attempt row.

### Submission result
A persisted per-element grading outcome (field, method, constructor, or challenge) tied to one lab submission. Natural key is submission plus rubric element id; re-grades update the same row.

### Rubric snapshot
An in-memory, immutable graph of the lab's expected OOP structure (challenges, classes, members, class relations) loaded once per grading request, optionally from cache.

### MMD grading
Diagram-side grading of an uploaded `.mmd` file: parse Mermaid class syntax into the same rubric entity shapes used for Java reflection, compare against the solution, and persist per-element pass/fail for the MMD tab. Under the rebuilt three-pillar model, MMD is one independent grading pillar (not AND-merged with Java at score time).

### Grading pillar
One of up to three equal scoring slices per challenge: `.class` reflection (always applicable), `.mmd` diagram (applicable when the challenge's `has_mmd` flag is true), or operational `testcase` invocations (applicable when the challenge has at least one operational testcase). Challenge score is the arithmetic mean of only the applicable pillar percentages — 3-way, 2-way (50/50), or Declaration-Test-only as pillars drop out. Inapplicable pillars are omitted entirely from the student result tab navigation, not shown as "not scored."

### Operational testcase
A rubric-linked grading check that invokes student code via Java reflection (`Constructor.newInstance` / `Method.invoke`) and evaluates one or more assertions (return value, field state, stdout, exception type, or instance comparison). Rubric shape: `testcase` → `testcase_invocation` or `testcase_instance` + `testcase_assertion`. Outcomes persist in `submission_testcase_result` (rollup) and `submission_testcase_assertion_result` (per-assertion detail).

### is_hidden (testcase)
Rubric flag on `testcase` controlling student visibility. When `false`, the testcase appears in **Example Testcases** with full I/O card expand. When `true`, it appears in **Other Testcases** as pass/fail only — input and output are withheld.

### Primary assertion
The assertion that drives a testcase's collapsed I/O card display (`input_display`, `expected_display`, `actual_display` on `submission_testcase_result`). Selected at grade time by priority: STDOUT → RETURN_VALUE → FIELD_STATE → EXCEPTION → COMPARISON_RESULT; within the same kind, lowest `order_index` wins. Other assertions appear in the expanded stacked view only.

### Receiver construction (testcase)
Optional rubric configuration for METHOD invocations on classes that lack a no-arg constructor. The testcase invocation row names a rubric constructor and JSON parameter list used to build the receiver object before the method call. When absent, the runner falls back to a no-arg constructor on the method's declaring class.

### Testcase invoke executor
Dedicated single-worker executor for operational testcase reflection. All student-code invocations and stdout capture run through this queue so parallel challenge grading does not interleave `System.out` or race on timeout cancellation.

### Assertion kind
The category of check applied to an invoke or comparison outcome: return value, field state, stdout, exception type, or comparison result. A testcase passes only when every configured assertion kind passes.

### Testcase I/O card
Student-facing expandable result card per testcase: INPUT (formatted invocation), EXPECTED OUTPUT, YOUR OUTPUT. Collapsed view uses primary assertion display fields; expanded view stacks every assertion's EXPECTED/YOUR pair under one shared INPUT.

### lab_result bundle
Upload-time JSON payload keyed by `challenge_<N>` where `N` is the challenge's rubric number (`challenge_number`), not the sidebar list index. Each entry contains class, MMD, and operational testcase I/O card arrays so the student UI renders tabs without follow-up read API calls. Revisit read paths return the same testcase shape when the upload cache is absent.

### Parsed submission snapshot
Immutable per-(submission, challenge) capture of rubric-scoped Class and MMD display text as parsed from the student's files at grade time. Result tabs use snapshot text for present items and rubric expected labels for missing items, with existing per-element pass/fail flags.

## Relationships

- A **lab submission** owns many **submission results** (one per rubric element graded).
- **Submission upload compile** produces on-disk `classes/` trees that reflection grading reads; it runs on `compileExecutor`, not the grading pool.
- Grading compares compiled student classes against a **rubric snapshot**, then writes **submission results**.

### Student lab progress
A per-(student, lab) tracking row holding highest score, attempt count, and best/latest submission metadata. Updated on upload; lecturer **grade overview** matrix and lab roster **Score** columns read `highest_score` (when the student has submitted); student dashboard uses latest attempt by design.

### Term enrollment
Maps an active student to a term (`term_enrollment`). The lecturer lab roster paginates enrolled students for the lab's term, then LEFT JOINs `student_lab_progress` and submission/challenge data per student.

### Lecturer lab roster
The unique set of enrolled/active students for a lab's term/course. Challenge and overview tables paginate this population; submission and progress data are LEFT JOINed per student afterward.

### Dual-role user
A `user_account` row with both `STUDENT` and `LECTURER` in `user_role`, optionally holding different `student_code` and `teacher_code` values. Login accepts either code; post-login routing defaults to the lecturer dashboard; student routes remain reachable by URL when the JWT includes both roles.
