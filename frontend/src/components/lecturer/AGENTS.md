# Lecturer Components



## Purpose



Grading dashboard widgets used by `LecturerDashboard.jsx`.



## Ownership



| File | Role |

|---|---|

| `DashboardSection.jsx` | Main grading overview layout |

| `OverviewPanel.jsx` | Clickable summary stat cards; **View list** opens `OverviewDetailDialog` |
| `OverviewDetailDialog.jsx` | Dialog for the four overview cards: enrolled students, at-risk students (total under 70), labs, and qualifying scores behind the average |
| `LecturerOverviewCard.jsx` | Summary stat cards |

| `SubmissionTable.jsx` | Lab submitter roster / challenge submission table |
| `GradeDistributionChart.jsx` | Horizontal bar chart for lab overview grade distribution |

| `ClassScoreBreakdown.jsx` | Expandable Java class/member grading breakdown |

| `MmdScoreBreakdown.jsx` | Expandable MMD class + relations breakdown for lecturer drawer |

| `LecturerSubmissionDrawer.jsx` | Right drawer: Declaration Test | MMD | Operation Test (MMD when `has_mmd`; OT when `/testcases` returns rows), challenge detail + export |

| `LabAttemptHistoryDrawer.jsx` | Right drawer: lab attempt history for roster View Submission |
| `PlagiarismInvestigationDrawer.jsx` | Right drawer: flagged-student copy lineage + match signals (`GET .../students/{id}/plagiarism`) |

| `ExportMenu.jsx` | Export dropdown (default Excel/PDF/SVG; Bulk Grading passes Excel/PDF/CSV via `formats`); auto-flips upward when near viewport bottom; `dropUp` forces upward menu (submission drawer footer) |

| `GradeOverviewTable.jsx` | Cross-lab grade matrix on the **Score** nav page: two panels (Student/IRN/Total fixed left; labs scroll right), synced vertical scroll, clickable rows |
| `BulkGradingPanel.jsx` | **Grading** nav: ephemeral Main-folder bulk grade — labs from `GET /api/lecturer/bootstrap/grading` (each lab must include `challenges` for Lab/Exam mode + results tabs), parse-then-confirm, sequential `/bulk-grade`, within-batch plagiarism |
| `BulkSubmissionDrawer.jsx` | Ephemeral Score-style Declaration / MMD / Operation Test drawer from in-memory `lab_result`; from a Bulk **challenge** results tab (`lockToChallenge`) shows only that challenge (Dashboard-style); from Overview allows challenge switching |
| `lecturerDrawerChrome.js` | Shared overlay, elevated panel (`dark:bg-surface-secondary`), edge border, and internal dividers for lecturer right drawers |
| `OperationalTestcaseBreakdown.jsx` | Expandable OT I/O cards for lecturer drawers; full rubric order with I/O for hidden rows (`DisclosureMode.LECTURER` on API / bulk `lab_result`) |
| `PlagiarismDangerMark.jsx` | Lecturer-only marks: yellow warning (victim / lab presence), red warning (plagiarizer); roles mutually exclusive; helpers for flags, roles, overlap % |
| `GradeOverviewSubmissionHistory.jsx` | Inline submission history panel below grade matrix (lab filter, column sort, client-side pagination at 12) |

| `exportRoster.js` | Shared export helpers for roster, challenge breakdown, and grade overview |

| `UploadPanel.jsx` | Static placeholder — **not imported anywhere** |

| `structure/ClassDetailPanel.jsx` | Class Definition editor: members plus optional Extends/Implements pair (shared inheritance/realization row); Outer class stays for nested identity |
| `structure/ChallengeDetailPanel.jsx` | Challenge-level tabs: MMD Relations \| Operational Testcases; challenge / class / MMD / testcase pillar weights (no per-testcase weight) |
| `structure/WeightInput.jsx` | Integer weight field (min 1) for challenge, class, MMD, and operational-testcase pillars |
| `structure/TestcasesPanel.jsx` | Operational testcase list; **Add new testcase** (default UNIT) plus per-testcase Type dropdown; dry-run I/O, Run all, Save Testcases; shared `normalizeTestcaseForApi` for save and dry-run; list shows lecturer shorthand **Ex** (example, not hidden) or **TS*n*** (hidden) via `lecturerTestcaseShortLabels`; list/editor split is draggable on `lg+`; editor column stays `flex-1 min-w-0` so the panel cannot overflow the viewport |
| `structure/UnitTestcaseWorksheet.jsx` | Closed Unit canvas: one member picker, scalar params, allowed assertions; no step list, instance names, `$instance`, or equals() |
| `structure/CompositionTestcaseScript.jsx` | Composition canvas: ordered named-object steps, required names on constructs and static object returns, `$instance` args, optional **Call as** (`dispatchClassId`) on instance-method receivers, optional per-step assertions, equals() |
| `structure/testcaseAuthoring.js` | Shared empty/hydrate/normalize helpers and member catalog for Unit and Composition; Composition preserves `dispatchClassId`; Unit always nulls it; Call-as ancestor helpers from Extends/Implements |
| `structure/TestcaseParamFields.jsx` | Per-parameter scalar or `$instance` argument editors |
| `structure/TestcaseAssertionFields.jsx` | Assertion kinds; Composition object-return equals() picker; constructors FIELD_STATE + EXCEPTION only |
| `structure/DryRunResultCard.jsx` | Lecturer dry-run I/O card (input, expected, actual) |
| `structure/ReferenceJavaFiles.jsx` | Drag/drop or file-picker for reference `.java` sources (dry-run) |
| `structure/SolutionImportPanel.jsx` | Solution tab: drag/drop or folder picker for nested `.java` / `.mmd` sources; `onImport(selection)` posts via parent; clears selection on success |
| `structure/LabSchedulingPanel.jsx` | Lab scheduling card: student access, submission deadline, solution import (`onSolutionImport` → `SolutionImportPanel`) |
| `structure/MmdRelationsPanel.jsx` | MMD relation editor for selected challenge |



## Local Contracts



### Data source



- `LecturerDashboard.jsx` fetches overview, lab statistics, submitter roster (`GET /api/labs/{labId}/submissions`), per-challenge roster (`GET /api/labs/{labId}/challenges/{challengeId}/students`), and grade overview (`GET /api/lecturer/grade-overview`)
- Overview card click uses `students`, `labs`, and `scoreRows` already on `GET /api/lecturer/overview` (no second request). At-risk rows are `atRisk` (total under 70, missing labs as 0).
- Lecturer dashboard does not display scoring weights
- Lecturers set challenge / class / MMD / operational-testcase weights only in Solution Management (`Save Lab Structure`); defaults are 1. Labs have no weight.

- Student roster and Score matrix tables support server-side `search` (name or student ID/IRN; case-insensitive).

- Lab Student roster and challenge tab roster pagination both count **students who submitted** (lab-level or challenge-graded), page size **5**

- `SubmissionTable` on the lab overview lists submitters only; **Score** is highest lab score; **Attempt** / **Submitted At** are from the latest attempt; when `plagiarismFlagged` is true the Plagiarism column shows one role icon from `plagiarismRole` (`ORIGINAL` or `PLAGIARIZER`) plus overlap % (same `overlapByStudentAndLab` source as the grade matrix); SUMMARY still shows Submitted / Enrolled / Completion / Plagiarism % (`plagiarismRate` from lab statistics)
- Lecturer-only `PlagiarismDangerMark`: victim (earlier first submit in lab) = yellow warning; plagiarizer (later first submit) = red warning; lab presence = yellow. Roles are mutually exclusive and use earliest lab submission time (a victim's later re-upload does not flip them to plagiarizer). On the grade matrix, the score is centered first; mark + overlap % (from `GET /api/lecturer/plagiarism/flags` → `overlapByStudentAndLab` / `rolesByStudentAndLab`) sit to the right on one line when flagged. Roster / challenge tables use the same marks in the Plagiarism column. Display-only (no click-to-details). Students are not notified.
- `GradeOverviewTable` uses two matching `bg-surface` panels with a gutter: identity has no horizontal scroll; labs scroll horizontally; vertical `scrollTop` is synced; one shared pagination footer.

- Student roster supports server-side sort via `sort` query param (`studentName`, `studentCode`, `score`, `attempt`, `submittedAt`); default `studentName,asc`; **clickable column headers** on `SubmissionTable` with dual chevrons (no toolbar sort buttons)

- Roster **View Submission** opens `LabAttemptHistoryDrawer`; flagged rows also show **View Plagiarism** → `PlagiarismInvestigationDrawer` (lineage rooted at earliest first-submit; match signals on edges; no code diffs)

- Challenge tab **View** opens `LecturerSubmissionDrawer` with Declaration Test | MMD | Operation Test when applicable (`GET .../challenges/{id}/class?studentId=`, `/mmd`, `/testcases`; optional `submissionId`). MMD tab and `/mmd` fetch are omitted when `has_mmd` is false. Operation Test tab appears when `/testcases` returns at least one row. Wrong/missing Class and MMD members use the same generic student-facing labels and consolidation as the student dashboard (`studentDisplayConsolidation.js`); MMD class cards use the student primary-light list layout.
- Challenge tab lists **submitters only**; **Score** is the student's **highest qualifying challenge score** (deadline-aware); **Attempts** / **Submitted At** are from the latest graded attempt for that challenge; **View** opens the latest attempt's submission
- `ClassScoreBreakdown` treats a type with no fields/constructors/methods as one shell check (`1/1 · 100%` or `0/1 · 0%`) with a status icon; do not display `0/1 · 100%`
- Declaration Score member rows are pass or fail only; leftover `partial` flags render as fail, not a warning tick
- `ClassScoreBreakdown` keeps `cls.error` from GET `/class` and shows one wrapping compile line under the class name; do not repeat it as an expanded banner and do not CSS-truncate it

- Overview export uses `ExportMenu` → `exportRoster.js` (Excel, PDF, SVG)
- Score tab export uses `ExportMenu` → `exportGradeOverview` in `exportRoster.js` (Excel, PDF, SVG; all students via paginated `GET /api/lecturer/grade-overview` with `size=100`)
- Grade overview per-lab scores and total use **highest lab score** (`student_lab_progress.highest_score`); submission history panel still lists every attempt with its attempt score
- Grade overview supports server-side sort via `sort` query param (`studentName`, `irn`, `score`, `labScore,<labUuid>`); default `studentName,asc`; **clickable column headers** on `GradeOverviewTable` (no toolbar sort buttons)
- Score tab pagination is **12** students per page (`GET /api/lecturer/grade-overview?size=12`)
- Score tab row click selects a student and loads `GET /api/analytics/student/{studentId}` → `GradeOverviewSubmissionHistory` (all submissions; lab filter; client-side column-header sort; client-side pagination **12** rows per page after filter/sort; filter/sort/student change resets to page 0)
- Bulk **Grading** tab (`BulkGradingPanel`) posts each accepted student to `POST /api/lecturer/labs/{labId}/bulk-grade?mode=LAB|EXAM`; holds `lab_result` in memory for `BulkSubmissionDrawer`; within-batch plagiarism via client Jaccard on file hashes (server may return `fileHashes`)
- Bulk results show **Overview** + one tab per lab challenge (Dashboard-style labels); Overview Score = lab total; challenge tab Score = that challenge’s `lab_result` total; Plagiarism is within-batch and tab-invariant; View Submission passes `initialChallengeId` from the active results tab; results table paginates **12** student rows per page (client-side; resets on new folder or grading run); **Export** (`ExportMenu`) downloads the full results list for the active tab as Excel, PDF, or CSV (Student, ID, Score, Plagiarism, Error)



### Composition



```

LecturerDashboard

  → AppShell + NavBar

  → DashboardSection (activeNav === 'dashboard')

       → OverviewPanel
       → OverviewDetailDialog (card click)

       → DashboardSection (grading overview — lab stats, roster, challenge tabs)

       → ExportMenu (overview export)

  → DashboardSection (activeNav === 'score')

       → GradeOverviewTable

       → ExportMenu (grade overview export)

       → GradeOverviewSubmissionHistory (row click)

       → LabAttemptHistoryDrawer

       → LecturerSubmissionDrawer

  → BulkGradingPanel (activeNav === 'grading')

       → BulkSubmissionDrawer (ephemeral lab_result)

```



User management and submission management are separate pages (`UserManagement`, `SubmissionManagement`), not in this folder.

**Solution Management** (`SolutionManagement.jsx`, `/lecturer-solution`) uses `structure/*` for lab structure and operational testcase authoring. Testcase API: `GET/PUT /api/lecturer/labs/{labId}/challenges/{challengeId}/testcases`, dry-run `POST .../testcases/dry-run` (isolated worker `scenario` op). Types are `UNIT` and `COMPOSITION`. PUT body uses `invocations` (Unit: exactly one; Composition: ordered named-object steps). Constructors assert via **FIELD_STATE** (not RETURN_VALUE). Composition non-primitive method returns may use RETURN_VALUE with `{ "$objectCheck": "EQUALS", "$instance": "name" }`. No OOP principle tag, COMPARISON builder, or per-testcase weight. Challenge `testcase_weight` on `ChallengeDetailPanel` scales the student OT pillar when the challenge has authored OT. Hidden vs example (`is_hidden`) drives student Example vs Other disclosure. Students never see Unit/Composition labels. Reference Java is loaded via drag/drop or file picker (`ReferenceJavaFiles.jsx`) and kept in `sessionStorage` per lab/challenge. **Solution import** (`SolutionImportPanel` → `POST .../solution-import`): parses lab-root or single `challenge_n` via `solutionFolderParse.js`, merges applied fragments into the Structure draft by `challengeNumber` (replace keeps id/name/weights; add uses defaults), clears editor OT for replaced challenges (`suppressServerLoad`), skips OT GET for draft-only challenge ids (not yet in `savedSnapshot`) so the tab does not toast Server Busy on 404, and on **Save Lab Structure** empty-PUTs OT for replaced ids before structure PUT.



## Work Guidance



- Data fetching stays in `LecturerDashboard.jsx`; keep table/drawer components presentational

- `exportOverview` fetches all submitters via `GET /api/labs/{labId}/submissions/export`, then exports via `exportRosterRows`

- Challenge export merges incorrect Java methods and incorrect MMD attributes/relations (`Source`, `Item Type`, `Incorrect Item`, `Error` columns); title `Incorrect breakdown — {studentName}`

- `UploadPanel.jsx` is dead code; Bulk Grading uses `BulkGradingPanel` + `POST .../bulk-grade` instead



## Verification



- Manual: log in as lecturer, confirm lab Student roster and challenge Submissions tables show only submitters; View drawers open



## Child DOX Index



No child docs.


