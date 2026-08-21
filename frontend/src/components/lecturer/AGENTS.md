# Lecturer Components



## Purpose



Grading dashboard widgets used by `LecturerDashboard.jsx`.



## Ownership



| File | Role |

|---|---|

| `DashboardSection.jsx` | Main grading overview layout |

| `LecturerOverviewCard.jsx` | Summary stat cards |

| `SubmissionTable.jsx` | Enrolled-student roster / challenge submission table |

| `ClassScoreBreakdown.jsx` | Expandable Java class/member grading breakdown |

| `MmdScoreBreakdown.jsx` | Expandable MMD class + relations breakdown for lecturer drawer |

| `LecturerSubmissionDrawer.jsx` | Right drawer: Class | MMD tabs, challenge detail + export |

| `LabAttemptHistoryDrawer.jsx` | Right drawer: lab attempt history for roster View |

| `ExportMenu.jsx` | Single Export button with Excel/PDF/SVG picker; auto-flips upward when near viewport bottom; `dropUp` forces upward menu (submission drawer footer) |

| `GradeOverviewTable.jsx` | Cross-lab grade matrix on the **Grading** nav page (student, IRN, total + per-lab scores); clickable rows |
| `PlagiarismDangerMark.jsx` | Lecturer-only warning triangle (`TriangleAlert`) inline after a flagged lab or score |
| `GradeOverviewSubmissionHistory.jsx` | Inline submission history panel below grade matrix (lab filter, date sort) |

| `exportRoster.js` | Shared export helpers for roster, challenge breakdown, and grade overview |

| `UploadPanel.jsx` | Static placeholder — **not imported anywhere** |

| `structure/ChallengeDetailPanel.jsx` | Challenge-level tabs: MMD Relations \| Operational Testcases; challenge / class / MMD weights |
| `structure/WeightInput.jsx` | Integer weight field (min 1) for challenge, class, and MMD pillar |
| `structure/TestcasesPanel.jsx` | Operational testcase list, editor, dry-run, separate Save Testcases |
| `structure/ReferenceJavaFiles.jsx` | Drag/drop or file-picker for reference `.java` sources (dry-run) |
| `structure/MmdRelationsPanel.jsx` | MMD relation editor for selected challenge |



## Local Contracts



### Data source



- `LecturerDashboard.jsx` fetches overview, lab statistics, enrolled-student roster (`GET /api/labs/{labId}/submissions`), per-challenge roster (`GET /api/labs/{labId}/challenges/{challengeId}/students`), and grade overview (`GET /api/lecturer/grade-overview`)
- Lecturer dashboard does not display scoring weights
- Lecturers set challenge / class / MMD weights only in Solution Management (`Save Lab Structure`); defaults are 1. Labs have no weight.

- Roster pagination counts **unique enrolled students** for the lab's term (`term_enrollment`), page size **5**

- `SubmissionTable` renders one row per enrolled student; non-submitters show placeholders (`—`, `0`); **Score** is highest lab score; **Attempt** / **Submitted At** are from the latest attempt; a warning triangle shows when `plagiarismFlagged` is true
- Lecturer-only warning triangle (`PlagiarismDangerMark`) is a 16×16 SVG in the same `h-4` / `leading-4` row as the lab name or score so it shares the text midline. Students are not notified.

- Student roster supports server-side sort via `sort` query param (`studentName`, `studentCode`, `score`, `attempt`, `submittedAt`); default `studentName,asc`; **clickable column headers** on `SubmissionTable` with dual chevrons (no toolbar sort buttons)

- Roster **View** opens `LabAttemptHistoryDrawer` (`GET /api/labs/{labId}/students/{studentId}/attempts`)

- Challenge tab **View** opens `LecturerSubmissionDrawer` with Class | MMD tabs (`GET .../challenges/{id}/class?studentId=` and `GET .../challenges/{id}/mmd?studentId=`; optional `submissionId`)

- Overview export uses `ExportMenu` → `exportRoster.js` (Excel, PDF, SVG)
- Grading tab export uses `ExportMenu` → `exportGradeOverview` in `exportRoster.js` (Excel, PDF, SVG; all students via paginated `GET /api/lecturer/grade-overview` with `size=100`)
- Grade overview per-lab scores and total use **highest lab score** (`student_lab_progress.highest_score`); submission history panel still lists every attempt with its attempt score
- Grade overview supports server-side sort via `sort` query param (`studentName`, `irn`, `score`, `labScore,<labUuid>`); default `studentName,asc`; **clickable column headers** on `GradeOverviewTable` (no toolbar sort buttons)
- Grading tab pagination is **10** students per page (`GET /api/lecturer/grade-overview?size=10`)
- Grading tab row click selects a student and loads `GET /api/analytics/student/{studentId}` → `GradeOverviewSubmissionHistory` (all submissions; lab filter; client-side column-header sort)



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

- `exportOverview` fetches all enrolled students via `GET /api/labs/{labId}/submissions/export`, then exports via `exportRosterRows`

- Challenge export merges incorrect Java methods and incorrect MMD attributes/relations (`Source`, `Item Type`, `Incorrect Item`, `Error` columns); title `Incorrect breakdown — {studentName}`

- `UploadPanel.jsx` is dead code; remove or wire up when lecturer upload flow is defined



## Verification



- Manual: log in as lecturer, confirm roster row count matches enrolled students for the lab term; challenge tab loads paginated students; View drawers open



## Child DOX Index



No child docs.


