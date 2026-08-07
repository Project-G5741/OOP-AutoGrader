# Lecturer Components



## Purpose



Grading dashboard widgets used by `LecturerDashboard.jsx`.



## Ownership



| File | Role |

|---|---|

| `DashboardSection.jsx` | Main grading overview layout |

| `LecturerOverviewCard.jsx` | Summary stat cards |

| `SubmissionTable.jsx` | Enrolled-student roster / challenge submission table |

| `ClassScoreBreakdown.jsx` | Expandable class/member grading breakdown (StudentUI pattern) |

| `LecturerSubmissionDrawer.jsx` | Right drawer: challenge submission detail + export |

| `LabAttemptHistoryDrawer.jsx` | Right drawer: lab attempt history for roster View |

| `ExportMenu.jsx` | Single Export button with Excel/PDF/SVG picker |

| `GradeOverviewTable.jsx` | Cross-lab grade matrix on the **Grading** nav page (student, IRN, total + per-lab scores) |

| `exportRoster.js` | Shared export helpers for roster and challenge breakdown |

| `UploadPanel.jsx` | Static placeholder — **not imported anywhere** |



## Local Contracts



### Data source



- `LecturerDashboard.jsx` fetches overview, lab statistics, enrolled-student roster (`GET /api/labs/{labId}/submissions`), per-challenge roster (`GET /api/labs/{labId}/challenges/{challengeId}/students`), and grade overview (`GET /api/lecturer/grade-overview`)

- Roster pagination counts **unique enrolled students** for the lab's term (`term_enrollment`), page size **5**

- `SubmissionTable` renders one row per enrolled student; non-submitters show placeholders (`—`, `0`)

- Roster **View** opens `LabAttemptHistoryDrawer` (`GET /api/labs/{labId}/students/{studentId}/attempts`)

- Challenge tab **View** opens `LecturerSubmissionDrawer` with class breakdown (`GET .../challenges/{id}/class?studentId=`)

- Overview export uses `ExportMenu` → `exportRoster.js` (Excel, PDF, SVG)



### Composition



```

LecturerDashboard

  → AppShell + NavBar

  → DashboardSection (activeNav === 'dashboard')

       → OverviewPanel

       → DashboardSection (grading overview — lab stats, roster, challenge tabs)

       → ExportMenu (overview export)

       → LabAttemptHistoryDrawer

       → LecturerSubmissionDrawer

```



User management and submission management are separate pages (`UserManagement`, `SubmissionManagement`), not in this folder.



## Work Guidance



- Data fetching stays in `LecturerDashboard.jsx`; keep table/drawer components presentational

- `exportOverview` paginates through `/submissions` until all enrolled students are exported

- Challenge export includes student name, incorrect class, and incorrect methods only

- `UploadPanel.jsx` is dead code; remove or wire up when lecturer upload flow is defined



## Verification



- Manual: log in as lecturer, confirm roster row count matches enrolled students for the lab term; challenge tab loads paginated students; View drawers open



## Child DOX Index



No child docs.


