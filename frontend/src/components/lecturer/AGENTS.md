# Lecturer Components



## Purpose



Grading dashboard widgets used by `LecturerDashboard.jsx`.



## Ownership



| File | Role |

|---|---|

| `DashboardSection.jsx` | Main grading overview layout |

| `LecturerOverviewCard.jsx` | Summary stat cards |

| `SubmissionTable.jsx` | Lab submitter roster / challenge submission table |
| `GradeDistributionChart.jsx` | Horizontal bar chart for lab overview grade distribution |

| `ClassScoreBreakdown.jsx` | Expandable Java class/member grading breakdown |

| `MmdScoreBreakdown.jsx` | Expandable MMD class + relations breakdown for lecturer drawer |

| `LecturerSubmissionDrawer.jsx` | Right drawer: Class | MMD tabs (MMD hidden when `has_mmd=false`), challenge detail + export |

| `LabAttemptHistoryDrawer.jsx` | Right drawer: lab attempt history for roster View Submission |
| `PlagiarismInvestigationDrawer.jsx` | Right drawer: flagged-student copy lineage + match signals (`GET .../students/{id}/plagiarism`) |

| `ExportMenu.jsx` | Single Export button with Excel/PDF/SVG picker; auto-flips upward when near viewport bottom; `dropUp` forces upward menu (submission drawer footer) |

| `GradeOverviewTable.jsx` | Cross-lab grade matrix on the **Grading** nav page: two panels (Student/IRN/Total fixed left; labs scroll right), synced vertical scroll, clickable rows |
| `PlagiarismDangerMark.jsx` | Lecturer-only marks: yellow warning (victim / lab presence), red warning (plagiarizer); roles mutually exclusive; helpers for flags, roles, overlap % |
| `GradeOverviewSubmissionHistory.jsx` | Inline submission history panel below grade matrix (lab filter, column sort, client-side pagination at 10) |

| `exportRoster.js` | Shared export helpers for roster, challenge breakdown, and grade overview |

| `UploadPanel.jsx` | Static placeholder — **not imported anywhere** |

| `structure/ClassDetailPanel.jsx` | Class Definition editor: members plus optional Extends/Implements pair (shared inheritance/realization row); Outer class stays for nested identity |
| `structure/ChallengeDetailPanel.jsx` | Challenge-level tabs: MMD Relations \| Operational Testcases; challenge / class / MMD / testcase weights |
| `structure/WeightInput.jsx` | Integer weight field (min 1) for challenge, class, MMD, and operational-testcase pillars |
| `structure/TestcasesPanel.jsx` | Operational testcase list, editor, dry-run, separate Save Testcases |
| `structure/ReferenceJavaFiles.jsx` | Drag/drop or file-picker for reference `.java` sources (dry-run) |
| `structure/MmdRelationsPanel.jsx` | MMD relation editor for selected challenge |



## Local Contracts



### Data source



- `LecturerDashboard.jsx` fetches overview, lab statistics, submitter roster (`GET /api/labs/{labId}/submissions`), per-challenge roster (`GET /api/labs/{labId}/challenges/{challengeId}/students`), and grade overview (`GET /api/lecturer/grade-overview`)
- Lecturer dashboard does not display scoring weights
- Lecturers set challenge / class / MMD / operational-testcase weights only in Solution Management (`Save Lab Structure`); defaults are 1. Labs have no weight.

- Student roster and Grading tables support server-side `search` (name or student ID/IRN; case-insensitive).

- Lab Student roster and challenge tab roster pagination both count **students who submitted** (lab-level or challenge-graded), page size **5**

- `SubmissionTable` on the lab overview lists submitters only; **Score** is highest lab score; **Attempt** / **Submitted At** are from the latest attempt; when `plagiarismFlagged` is true the Plagiarism column shows one role icon from `plagiarismRole` (`ORIGINAL` or `PLAGIARIZER`) plus overlap % (same `overlapByStudentAndLab` source as the grade matrix); SUMMARY still shows Submitted / Enrolled / Completion / Plagiarism % (`plagiarismRate` from lab statistics)
- Lecturer-only `PlagiarismDangerMark`: victim (earlier first submit in lab) = yellow warning; plagiarizer (later first submit) = red warning; lab presence = yellow. Roles are mutually exclusive and use earliest lab submission time (a victim's later re-upload does not flip them to plagiarizer). On the grade matrix, the score is centered first; mark + overlap % (from `GET /api/lecturer/plagiarism/flags` → `overlapByStudentAndLab` / `rolesByStudentAndLab`) sit to the right on one line when flagged. Roster / challenge tables use the same marks in the Plagiarism column. Display-only (no click-to-details). Students are not notified.
- `GradeOverviewTable` uses two matching `bg-surface` panels with a gutter: identity has no horizontal scroll; labs scroll horizontally; vertical `scrollTop` is synced; one shared pagination footer.

- Student roster supports server-side sort via `sort` query param (`studentName`, `studentCode`, `score`, `attempt`, `submittedAt`); default `studentName,asc`; **clickable column headers** on `SubmissionTable` with dual chevrons (no toolbar sort buttons)

- Roster **View Submission** opens `LabAttemptHistoryDrawer`; flagged rows also show **View Plagiarism** → `PlagiarismInvestigationDrawer` (lineage rooted at earliest first-submit; match signals on edges; no code diffs)

- Challenge tab **View** opens `LecturerSubmissionDrawer` with Class | MMD tabs when `has_mmd` is true (`GET .../challenges/{id}/class?studentId=` and `GET .../challenges/{id}/mmd?studentId=`; optional `submissionId`); those GETs use `DisclosureMode.LECTURER` (full rubric labels). MMD tab and `/mmd` fetch are omitted when `has_mmd` is false (from `GET /api/labs/{labId}/challenges`). Do not apply `studentDisplayConsolidation`.
- Challenge tab lists **submitters only**; **Score** is the student's **highest qualifying challenge score** (deadline-aware); **Attempts** / **Submitted At** are from the latest graded attempt for that challenge; **View** opens the latest attempt's submission
- `ClassScoreBreakdown` treats a type with no fields/constructors/methods as one shell check (`1/1 · 100%` or `0/1 · 0%`) with a status icon; do not display `0/1 · 100%`
- Declaration Score member rows are pass or fail only; leftover `partial` flags render as fail, not a warning tick
- `ClassScoreBreakdown` keeps `cls.error` from GET `/class` and shows one wrapping compile line under the class name; do not repeat it as an expanded banner and do not CSS-truncate it

- Overview export uses `ExportMenu` → `exportRoster.js` (Excel, PDF, SVG)
- Grading tab export uses `ExportMenu` → `exportGradeOverview` in `exportRoster.js` (Excel, PDF, SVG; all students via paginated `GET /api/lecturer/grade-overview` with `size=100`)
- Grade overview per-lab scores and total use **highest lab score** (`student_lab_progress.highest_score`); submission history panel still lists every attempt with its attempt score
- Grade overview supports server-side sort via `sort` query param (`studentName`, `irn`, `score`, `labScore,<labUuid>`); default `studentName,asc`; **clickable column headers** on `GradeOverviewTable` (no toolbar sort buttons)
- Grading tab pagination is **10** students per page (`GET /api/lecturer/grade-overview?size=10`)
- Grading tab row click selects a student and loads `GET /api/analytics/student/{studentId}` → `GradeOverviewSubmissionHistory` (all submissions; lab filter; client-side column-header sort; client-side pagination **10** rows per page after filter/sort; filter/sort/student change resets to page 0)



### Composition



```

LecturerDashboard

  → AppShell + NavBar

  → DashboardSection (activeNav === 'dashboard')

       → OverviewPanel

       → DashboardSection (grading overview — lab stats, roster, challenge tabs)

       → ExportMenu (overview export)

  → DashboardSection (activeNav === 'grading')

       → GradeOverviewTable

       → ExportMenu (grade overview export)

       → GradeOverviewSubmissionHistory (row click)

       → LabAttemptHistoryDrawer

       → LecturerSubmissionDrawer

```



User management and submission management are separate pages (`UserManagement`, `SubmissionManagement`), not in this folder.

**Solution Management** (`SolutionManagement.jsx`, `/lecturer-solution`) uses `structure/*` for lab structure and operational testcase authoring. Testcase API: `GET/PUT /api/lecturer/labs/{labId}/challenges/{challengeId}/testcases`, dry-run `POST .../testcases/dry-run`. Reference Java is loaded via drag/drop or file picker (`ReferenceJavaFiles.jsx`) and kept in `sessionStorage` per lab/challenge.



## Work Guidance



- Data fetching stays in `LecturerDashboard.jsx`; keep table/drawer components presentational

- `exportOverview` fetches all submitters via `GET /api/labs/{labId}/submissions/export`, then exports via `exportRosterRows`

- Challenge export merges incorrect Java methods and incorrect MMD attributes/relations (`Source`, `Item Type`, `Incorrect Item`, `Error` columns); title `Incorrect breakdown — {studentName}`

- `UploadPanel.jsx` is dead code; remove or wire up when lecturer upload flow is defined



## Verification



- Manual: log in as lecturer, confirm lab Student roster and challenge Submissions tables show only submitters; View drawers open



## Child DOX Index



No child docs.


