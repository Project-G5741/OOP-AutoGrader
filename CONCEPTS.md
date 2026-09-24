# Concepts

Shared domain vocabulary for this project — entities, named processes, and status concepts with project-specific meaning. Seeded with core domain vocabulary, then accretes as ce-compound and ce-compound-refresh process learnings; direct edits are fine. Glossary only, not a spec or catch-all.

## Grading pipeline

### Submission upload compile
The pre-grading slice that receives a multipart folder, validates path structure, compiles each challenge's `.java` files in parallel, and writes `.class` output under `challenge_N/classes/`. Sources are compiled from memory; MMD files stay in the multipart map for grading without disk staging on the hot path.

### Intra-challenge compile isolation
Compile-failure rule inside one challenge: a class that does not compile, and any class that references it, fail as a whole; classes that still compile keep Class-tab scores and their share of the challenge score. Sibling challenges already isolate independently; this rule is the same-challenge counterpart. Dependent Class-tab lines use `See {Upstream}` rather than repeating the root syntax error. Display copy is the Class-card compile line convention in `backend/src/main/java/com/eiu/capstone/backend/service/compile/AGENTS.md`. When student upload runs operational tests, mixed javac marks ERROR only for tests whose invoked types failed javac (same rule as lecturer dry-run).

### Package normalization
Pre-compile transformation of student Java sources that removes `package ...;` declarations and same-challenge cross-imports so all classes compile into the default package. Grading rubrics and reflection use simple class names against flat `classes/` output; when normalization runs, students see a non-blocking warning that package declarations were ignored.

### Qualified rubric class name
A rubric class entry's grading identity: the simple `name` when no outer class is linked, or `Outer.Inner` when an optional outer-class link points to another class in the same challenge. Used to match compiled nested types (`Outer$Inner.class`) during class-reflection grading.

### Outer-class link
Optional rubric relationship from a nested class entry to its enclosing class within the same challenge. Flat rubric rows (not a nested editor tree); enables qualified-name matching and disambiguates simple-name collisions between nested classes under different outers.

### Declared heritage pair
A class's optional Extends or Implements target, authored on the class editor as the same inheritance or implementation relationship the MMD diagram uses. Java class-shell grading compares the compiled type's declared superclass (Extends) or declared interfaces (Implements) to that target. Independent of the outer-class link used for nested types.

### Static nested flag
Rubric boolean on a nested class entry indicating whether the student's nested type is expected to be `static`. When set, the class-reflection grader compares `Modifier.isStatic()` on the parsed class; when clear, the nested type is treated as a non-static inner class and constructor matching strips the compiler-injected implicit outer-instance parameter.

### Upload hot path
The student-visible wait from `POST .../upload` until scores return. Serial stages on the request thread: one-query access check (cached briefly on success; warmed by `GET /api/labs`) → parallel compile overlapping rubric cache load → parallel Class, MMD, and (when applicable) operational-testcase grade compute → `lab_result` assemble → one persist SQL (insert with `MAX+1` and final score, challenge scores, progress) → snapshot plagiarism signals. When any uploaded challenge has OT, the request acquires `workerJvmSlot` and opens one worker session for the upload. Temp-folder delete, detail UPSERT, and plagiarism inspect run on `persistExecutor` after persist succeeds. Lecturer flags typically appear within a few seconds. Dominating stages and complexity: `docs/GRADING_WORKFLOWS.md` §14.

### Lab submission
A student's single graded attempt for a lab, keyed by user, lab, and attempt number. One row in `lab_submission`. Each upload inserts a new attempt (`MAX(attempt_number)+1`); the URL attempt segment is not used to overwrite a prior row.

### Submission result
A persisted per-element grading outcome (field, method, constructor, or challenge) tied to one lab submission. Natural key is submission plus rubric element id; re-grades update the same row.

### Rubric snapshot
An in-memory, immutable graph of the lab's expected OOP structure (challenges, classes, members, class relations) loaded once per grading request, optionally from cache.

### MMD grading
Diagram-side grading of an uploaded `.mmd` file: parse Mermaid `classDiagram` syntax (per `grading-mermaid-oop-class-diagrams.md`) into rubric entity shapes, compare against the lecturer solution, and persist per-element pass/fail for the MMD tab. Under the rebuilt three-pillar model, MMD is one independent grading pillar (not AND-merged with Java at score time).

### MMD parse error
A fatal parser failure on a submitted `.mmd`. All MMD-applicable rubric elements score incorrect; a human-readable error message is persisted and shown on the student MMD tab. Upload still succeeds.

### Grading pillar
One of up to three scoring slices per challenge: `.class` reflection (always applicable), `.mmd` diagram (applicable when the challenge's `has_mmd` flag is true), or operational `testcase` invocations (applicable when the challenge has at least one authored operational testcase). Challenge score is the weighted mean of applicable pillars using `class_weight`, `mmd_weight`, and `testcase_weight`. Inapplicable pillars are omitted from student result tab navigation, not shown as "not scored."

### Declaration Test
The Class-tab name for the class grading pillar: the class shell plus fields, methods, and constructors compared to the rubric. A member earns its points only when every graded declaration attribute matches; otherwise it earns none. Distinct from MMD grading and from operational testcases. Lecturer breakdowns for the same pillar are titled Declaration Score.

### Scoring weight
A positive integer (default 1) that scales how much a challenge, class shell, MMD pillar, or operational-testcase pillar contributes to the next rollup. Lecturers set weights only in Solution Management. Labs have no weight. A testcase has no per-testcase scoring weight. Challenge-level `testcase_weight` affects student totals when the testcase pillar is applicable.

### Operational testcase
A rubric-linked check that invokes compiled Java via reflection (`Constructor.newInstance` / `Method.invoke`) and evaluates one or more assertions (return value, field state, stdout, exception type, or object check). Types are **Unit** and **Composition** (`testcase_type` `UNIT` | `COMPOSITION`). Rubric shape: `testcase` → ordered `testcase_invocation` rows + `testcase_assertion`. Student upload runs applicable testcases through the isolated worker when the challenge has authored rows; lecturer dry-run uses the same grader path.

### is_hidden (testcase)
Rubric flag on `testcase`. When `false`, the row is an **example** testcase: students see name, pass/fail, and full Input / Expected / Your Output (plus expanded assertion detail). When `true`, the row is **hidden**: students see name and pass/fail only — no input, expected, or actual strings in API or UI. Students never see Unit, Composition, or Call-as labels.

### Primary assertion
The assertion that drives a testcase's collapsed I/O card display (`input_display`, `expected_display`, `actual_display`). For Composition, the grader first picks the earliest invoked step that failed an assertion (or the last run step if every assertion passed), then applies kind priority on that step: STDOUT → RETURN_VALUE → FIELD_STATE → EXCEPTION; within the same kind, lowest `order_index` wins. Other assertions appear in the expanded stacked view only. Student and lecturer result cards use this selection.

### Receiver construction (testcase)
How an instance method gets its `this`. A **Unit** instance method uses a hidden receiver on the declaring class (no-arg constructor when bytecode provides one, otherwise the shortest constructor fillable with literal defaults such as `0` or `""`); that construct is not a step, not named, and the lecturer cannot set `receiver_constructor_id`. **Composition** passes a named receiver already created in an earlier step (`instance_name` is the receiver, not a distinct return-name column).

### Named instance (testcase)
A live student/rubric-class object in a **Composition** test, addressable by a lecturer-chosen name in later steps (including as `{"$instance":"name"}` arguments). Names are required on constructor results and on static factory returns that produce a rubric-class object. Instance-method `instance_name` is the receiver already in the registry; the method return does not overwrite that name. Unit tests have no named instances.

### Unit operational testcase
A lecturer-authored operational test of exactly one constructor or method invocation, plus at least one assertion. Closed worksheet: pick the member, then assertions. No `$instance` arguments, no named objects, no equals(). Constructors use **FIELD_STATE** (and **EXCEPTION**) only. Non-primitive **method** returns on Unit may use **RETURN_VALUE** and/or **FIELD_STATE** on the **return type’s** fields (not the hidden receiver). A Unit static method has no receiver.

### Composition operational testcase
A lecturer-authored operational test of 1–20 ordered constructor and/or method invocations that share **named instances**, with at least one assertion on the testcase as a whole. Setup steps may carry zero assertions. equals() against another live named object is Composition-only. An unaccepted throw stops the sequence; later steps do not run and their assertions fail as not executed.

### Call as (Composition)
Lecturer choice on a Composition method step: look up the method on a selected parent or interface type from the receiver’s Extends/Implements heritage, then invoke on the **same named receiver** so polymorphic override behavior is what is graded. Java shape: `Shape s = circle; s.draw();` — one object, not a second `new`. Leaving Call as unset keeps concrete lookup on the receiver’s class. Call as is a step capability, not a separate testcase type. Unit worksheets do not offer it.

### Testcase type
Lecturer choice at create: `UNIT` or `COMPOSITION`. Two different authoring canvases, not principle tags on a shared scenario. Switching type replaces the other flow’s graph. Students never see the type name. Polymorphism, inheritance, and encapsulation are not operational-testcase types this ship; override proof uses Composition **Call as**, not a third type.



### Testcase rubric graph
The persisted testcase authoring shape: one `testcase` row (`UNIT` or `COMPOSITION`, `is_hidden`, no per-testcase weight) plus ordered `testcase_invocation` rows and `testcase_assertion` rows. Child rows upsert by client UUID. Invocation `order_index` uses park-delete-compact so uniqueness cannot collide mid-save. Lecturers edit this graph in Solution Management and save it via a dedicated PUT endpoint separate from lab structure save. Shipping the rebuild wipes existing operational-test graphs; lecturers re-author. There is no `testcase_instance` table, `oop_principle_tag`, or `COMPARISON_RESULT`.

### Sync-by-presence (testcase save)
The lecturer testcase PUT contract: testcase ids omitted from the payload are deleted from the challenge; ids present are upserted. Child invocation and assertion rows must be updated in place by client UUID — not delete-all-then-reinsert — because graded submissions reference `testcase_assertion.id` with `ON DELETE CASCADE`.

Invocation steps also have a unique dense order per testcase. Compact that order only after kept rows occupy a range above any real step index and omitted rows are gone: uniqueness is checked immediately, so writing final indexes before extras are deleted collides even when the committed graph would be valid.

### Testcase invoke executor
Retired name for serializing student invoke in the API JVM. Operational invoke now runs in the isolated testcase worker; the host allows one worker JVM via `workerJvmSlot` on the HTTP thread.

### Isolated testcase worker
A separate JVM process that executes operational testcase target classes for **student upload** (when the lab batch has applicable OT) and **lecturer dry-run**, so a crash or unkillable loop cannot terminate the API JVM. Both Unit and Composition reuse the `scenario` worker op plus a request-local named-instance registry. Student upload acquires `workerJvmSlot` and opens one worker session per upload when any graded challenge has operational testcases. The worker starts from an allowlisted environment, does not load the grading-harness classpath, and truncates captured stdout. The API scores from serialized untrusted outcomes. Class-tab reflection and javac compile stay in the API process. Distinct from intra-challenge compile isolation (a scoring rule) and from container sandbox invoke.

### Container sandbox invoke
Ephemeral container execution of the isolated testcase worker: network disabled, read-only root filesystem with a scoped writable temp area for student classes, and cgroup CPU/memory limits. A dedicated sandbox runner (not the Render API process) maintains a warm pool and accepts authenticated invoke delegation from the API. Thesis stage 3; invoke-only — Class-tab reflection and javac compile stay in the API.

### Serialized invocation outcome
Untrusted facts returned from the isolated testcase worker over IPC (local process or remote container): return value JSON, stdout, field snapshots, exception names, object-check facts (`objectTypeSimpleName`, `objectFieldSnapshots`, `equalsNamed`), and an error message. The API treats this payload as data only — scoring keys such as `passed` are ignored if present. Kind is a wire-level string (normal, threw, timed out, error) that the grading layer maps to trusted outcome semantics before assertion evaluation.

### Assertion kind
The category of check applied to an invoke outcome: return value, field state, stdout, or exception type. Constructor steps allow **FIELD_STATE** and **EXCEPTION** only (no RETURN_VALUE). Composition non-primitive **method** returns may use RETURN_VALUE with `{ "$objectCheck": "EQUALS", "$instance": "name" }`. Field expectations use **FIELD_STATE** with a rubric field id, not legacy field-map JSON. There is no `COMPARISON_RESULT` kind. A testcase passes only when every configured assertion passes.

### Testcase I/O card
Expandable result card per testcase: INPUT (formatted invocation), EXPECTED OUTPUT, YOUR OUTPUT. Collapsed view uses primary assertion display fields; expanded view stacks every assertion's EXPECTED/YOUR pair under one shared INPUT. Example testcases show full I/O; hidden testcases show pass/fail only.

### lab_result bundle
Upload-time JSON payload keyed by `challenge_<N>` where `N` is the challenge's rubric number (`challenge_number`), not the sidebar list index. Each entry contains class, MMD, `testcases`, `scores`, and `scoreApplicability`. When the challenge has authored operational testcases, upload sets `scoreApplicability.testcase` true and fills student-safe `testcases` (hidden rows omit I/O). Revisit `GET .../testcases` returns the same shape from persisted rows; attempts graded before OT shipped stay without testcase rows (empty tab). Next upload after ship uses the lit path; prior attempts are not retroactively regraded.

### Parsed submission snapshot
Immutable per-(submission, challenge) capture of rubric-scoped Class and MMD display text as parsed from the student's files at grade time. Result tabs use snapshot text for present items. When a snapshot entry is missing, student-facing assembly (`DisclosureMode.STUDENT`) shows generic placeholders instead of rubric expected labels; lecturer drawer (`DisclosureMode.LECTURER`) still uses the full rubric checklist. Pass/fail flags are unchanged.

### DisclosureMode
Display-only switch on Class/MMD result-tab assembly. `STUDENT` never emits rubric expected names, types, or signatures for missing or wrong members (generic messages such as "Missing variable name or datatype"). `LECTURER` keeps the full rubric checklist. Scoring is unchanged. `JwtAuthHelper.resolveDisclosureMode` returns `LECTURER` when the JWT has the lecturer role and `studentId` is present; otherwise `STUDENT`. Upload `lab_result` is always `STUDENT`.

## Relationships

- A **lab submission** owns many **submission results** (one per rubric element graded). Each student upload for a lab inserts a new attempt row (`MAX(attempt_number)+1`).
- **Submission upload compile** produces on-disk `classes/` trees that reflection grading reads; it runs on `compileExecutor`, not the grading pool.
- Grading compares compiled student classes against a **rubric snapshot**, then writes **submission results**.

### Student lab progress
A per-(student, lab) tracking row holding highest score, attempt count, and best/latest submission metadata. Updated on upload; lecturer **grade overview** matrix and lab roster **Score** columns read `highest_score` (when the student has submitted); student dashboard uses latest attempt by design.

### Grade overview
Cross-lab paginated matrix of enrolled students versus labs, showing per-lab highest scores and a total average. Lecturer sorts and exports use server-side ordering; per-lab column sort ranks students by that lab's highest score among rows with a submission (`last_submitted_at` set), matching the displayed cell values.

### Term enrollment
Maps an active student to a term (`term_enrollment`). Lecturers create terms by year, add students (manually or by Excel: match **IRN and email** to an existing account), and mark one term as **current**. Students enrolled in the current term can open the dashboard and submit; other active students only see history. Lecturers can **suspend** a student-only account (`is_active=false`); that student cannot log in until restored. Suspended students are omitted from the term roster and grade overview. Lecturer and dual-role accounts cannot be suspended this way.

Lecturer **dashboard overview**, **lab sidebar**, **lab rosters**, and **grade overview** list only **active students enrolled in the current quarter** and **labs in that quarter** (historical quarters are not shown on the dashboard). On the student side, the **submit dashboard** and `GET /api/submissions/my-labs?scope=current` use only the **current quarter** for lab list, stats, and score summaries; **Submission History** (`my-history`, `my-labs` without scope) keeps **all quarters** so a student who retakes (e.g. Q1 failed → Q2) still sees prior-quarter attempts labeled by quarter. Student-only accounts are **hard-deleted** (submissions, enrollments, progress, and related rows) once the current quarter reaches **three quarters after** their earliest enrolled quarter — e.g. first enrolled Q1 2025-2026 → deleted when Q4 2025-2026 becomes current; first enrolled Q3 2025-2026 → deleted when Q2 2026-2027 becomes current. Purge runs on the daily scheduler only (not when the lecturer changes the current quarter).

The lecturer lab roster paginates enrolled students for the lab's term, then LEFT JOINs `student_lab_progress` and submission/challenge data per student.

### Lecturer lab roster
The unique set of enrolled/active students for a lab's term/course. Challenge and overview tables paginate this population; submission and progress data are LEFT JOINed per student afterward.

### Lab deadline
Optional calendar date on a lab, defaulting to the parent term's end date when set at creation. The effective cutoff is 23:59:59 Vietnam time (UTC+7) on that date. Lecturers manage it in Solution Management and may extend it to a later date.

### Plagiarism check
Three independent comparisons of one lab submission against other students in the same lab: (1) ordered git commit hashes from the uploaded `.git` must match 100% in the same order; (2) git metadata (config user plus ordered author name/email/timestamp) must match 100%; (3) SHA-256 hashes of `.java` and `.mmd` bytes use Jaccard similarity and flag above 90%. Any firing check marks the pair flagged. Fingerprint signals are snapshotted before the upload response; compare/persist runs off the student wait (typically ~1–3s). Students never see flags.

### Score rounding
Grade percentages persist at two decimal places and display as integers by always rounding **down** (never half-up). A repeating third such as 66.666… is stored as `66.66` and shown as `66`. Plagiarism overlap and completion rates are not scores and do not use this rule.

### Lecturer score cutoff
The rule that only lab submissions with a timestamp on or before the lab's active deadline end count toward lecturer-facing scores and aggregates (roster, grade overview, analytics, exports, challenge tabs). Submissions after cutoff still grade and persist for the student; extending the deadline widens the cutoff so lecturer views recalculate from full submission history.

### Dual-role user
A `user_account` row with both `STUDENT` and `LECTURER` in `user_role`, optionally holding different `student_code` and `teacher_code` values. Login accepts either code; post-login routing defaults to the lecturer dashboard; student routes remain reachable by URL when the JWT includes both roles.

Wrong-role *page* navigation still sends the user to that default dashboard (session kept). Wrong-role *API* calls keep the session and show **no-access** instead of logging the user out. Lecturer and student are independent roles: holding one does not grant the other.

### Default-deny
API posture where a request is refused unless an explicit path-and-method rule admits the caller's roles. Callers with no usable session receive unauthenticated denial; signed-in callers with the wrong role receive forbidden denial. Forgotten routes do not stay open by default.

### No-access
The SPA screen for a signed-in user whose API call was forbidden. The session stays valid so they can return to their default dashboard. It is not used for wrong-role page URLs (those use the default-dashboard redirect) and not used for missing or expired sessions (those return to login).

### Active user presence
In-process last-seen map keyed by JWT email. A signed-in footer poll (every 10s) records a heartbeat; unique emails seen within 30 seconds are the public **Active Users** count. Logout and tab close send `DELETE /api/presence` so the user drops immediately. Identities are not exposed. Multi-instance deploys count independently. The same poll is the hard-cut heartbeat for **session revoke**: a 401 on presence clears the SPA session and returns to login.

### Session revoke
Invalidating a signed-in JWT before natural expiry by bumping `user_account.session_version` (claim `sv`) or deleting the account. Hard delete, suspend, and remove-from-current-term bump or remove the row so the filter rejects the old token immediately; the SPA presence poll forces logout within seconds.

## Backend tests

### Aspect-root test home
A first-level folder under the backend test source tree that names the kind of check (unit, integration, authorization, regression, or support). Tests in that home use a Java package that starts with the home name, so they are not in the same package as production code.

## Frontend theme

### Design token
A named semantic color role (primary, secondary, success, surface, etc.) whose hex value is defined once in a central theme config and exposed through CSS custom properties and Tailwind semantic classes. Components reference token names, not raw palette utilities or one-off hex literals.

### Theme preference
The user's light or dark mode choice. First visit follows OS `prefers-color-scheme`; an explicit toggle persists in `localStorage` and overrides system preference on later visits. `ThemeContext` applies the `dark` class on `<html>` for the whole app including auth screens.
