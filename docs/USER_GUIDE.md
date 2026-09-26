# OOP AutoGrader — User Guide

> **Audience:** Lecturers and Students at EIU (Eastern International University).  
> **Access URL:** The application runs on the URL provided by your administrator (default dev: `http://localhost:5173`).

---

## Table of Contents

- [1. Logging In](#1-logging-in)
- [2. Lecturer Guide](#2-lecturer-guide)
  - [2.1 Dashboard — Grading Overview](#21-dashboard--grading-overview)
  - [2.2 Grading — Cross-Lab Grade Matrix](#22-grading--cross-lab-grade-matrix)
  - [2.3 Users — User Management](#23-users--user-management)
  - [2.4 Terms — Term Management](#24-terms--term-management)
  - [2.5 Projects — Solution Management](#25-projects--solution-management)
  - [2.5.1 Operational testcases (detailed guide)](#251-operational-testcases-detailed-guide)
  - [2.6 Reports — Analytics Dashboard](#26-reports--analytics-dashboard)
  - [2.7 Profile & Password](#27-profile--password)
- [3. Student Guide](#3-student-guide)
  - [3.1 Lab Dashboard — Submit Work](#31-lab-dashboard--submit-work)
  - [3.2 Reading Your Results](#32-reading-your-results)
  - [3.3 History — Past Submissions](#33-history--past-submissions)
  - [3.4 Notifications](#34-notifications)
  - [3.5 Profile & Password](#35-profile--password)
- [4. Common Questions](#4-common-questions)

---

## 1. Logging In

There are two ways to sign in:

| Method | How |
|---|---|
| **IRN + Password** | Enter your student/lecturer IRN (ID number) and password, then click **Sign In**. |
| **Google (EIU account)** | Click **Sign in with Google** and choose your `@eiu.edu.vn` Google account. Only EIU accounts are accepted. |

**First-time Google login:** After authenticating with Google for the first time you will be asked to set your IRN and a password. Fill both fields and click **Complete Setup**.

**Forgot password:** Click the **Forgot password?** link, enter your EIU email address, and follow the reset link sent to your inbox.

After a successful login the system routes you automatically:
- **Lecturers** → Lecturer Dashboard (`/lecturer-dashboard`)
- **Students (in current term)** → Student Dashboard (`/student-dashboard`)
- **Students (not in current term)** → Submission History (`/student-history`) — submit dashboard is hidden until you are enrolled in the current term.

---

## 2. Lecturer Guide

The lecturer interface has a top navigation bar with six sections: **Dashboard**, **Grading**, **Users**, **Terms**, **Projects**, and **Reports**.

---

### 2.1 Dashboard — Grading Overview

**What it does:** Central command view showing live class statistics and per-lab student submissions.

#### Summary Cards (top row)

| Card | Meaning |
|---|---|
| Total Students | Number of students enrolled in the current term |
| Average Score | Class-wide average across all graded labs |
| Total Labs | Number of labs configured for the current term |
| At-Risk Students | Students whose average is below the threshold |

#### Selecting a Lab

1. The **Select Lab Assignment** panel on the left lists every lab for the current term.
2. Click a lab name to load its data on the right.
3. A **⚠ red warning icon** next to a lab name means at least one plagiarism flag has been raised for that lab.

#### Lab Tabs — Overview vs. Challenge Tabs

Once a lab is selected, a tab bar appears:

- **Overview tab** — shows lab-wide statistics and the full student roster.
- **Challenge tabs** (e.g., "Challenge 1") — one tab per challenge inside the lab; shows per-challenge submission list.

#### Overview Tab

| Section | Purpose |
|---|---|
| Statistics row | Average Score, Completion Rate, Highest Score, Lowest Score, Total Submissions, Enrolled Students, Students Submitted |
| Grade Distribution chart | Visual bar chart of score bands for the selected lab |
| Export button | Download the current view as **CSV** or **Excel** |
| Student Roster | Paginated list of all enrolled students with their latest lab score |

**Roster actions:**
- **Search** — type a student name or IRN in the search box; results filter as you type.
- **Sort** — click any column header to sort ascending/descending.
- **View (eye icon)** — opens the **Attempt History Drawer** for that student showing every submission attempt with scores and timestamps.
- **⚠ Plagiarism icon** — opens the **Plagiarism Investigation Drawer** showing which student's code overlaps and by how much.

#### Challenge Tab

- Shows a table of all students who submitted that specific challenge.
- **View Submission** — opens a drawer displaying the student's uploaded code output (class declarations, MMD diagram, testcase results) for that challenge.
- Plagiarism flags work the same way as in the Overview tab.

#### Refresh Button

Click the **↻ (Refresh)** button (top-right of the grading section) to reload all data from the server.

---

### 2.2 Grading — Cross-Lab Grade Matrix

**What it does:** A single table showing every student's score for every lab side-by-side.

#### How to Use

1. Click **Grading** in the top navigation.
2. The table loads with students as rows and labs as columns.
3. **Search** — search by student name or IRN using the search box at the top.
4. **Sort** — click any column header to sort.
5. **Plagiarism badges** — cells with a red ⚠ icon indicate a plagiarism flag for that student + lab combination.

#### Viewing a Student's Full History

- Click any student row to expand a **Submission History** panel below the table.
- The panel lists every submission attempt across all labs, with timestamps and scores.
- Use the **Lab** filter dropdown to narrow down to one lab.
- Click column headers to sort the history list.

#### Exporting

Click the **Export** button (top-right of the Grading section) and choose **CSV** or **Excel** to download the full grade matrix for all students and labs.

---

### 2.3 Users — User Management

**What it does:** Create, edit, delete, and suspend user accounts.

#### Viewing Users

The table shows all users with their name, IRN/ID, email, role, and account status. Use the **search box** at the top to filter by name, IRN, or email.

#### Adding a User

1. Click **Add User** (top-right).
2. Fill in the form:
   - **Full Name** — required.
   - **Email** — must be a valid EIU email.
   - **Role** — choose `STUDENT` or `LECTURER` (one or both).
   - **IRN/ID** — student or lecturer ID number.
   - **Password** — optional for Google-only users; required for IRN login.
3. Click **Save**.

#### Editing a User

1. Click the **pencil icon** on a user row.
2. Update any fields in the modal.
3. Click **Save**.

#### Deleting a User

1. Click the **trash icon** on a user row.
2. Confirm the deletion in the dialog.

> ⚠ Deletion is permanent. The user's submission history remains but the account is removed.

#### Suspending / Restoring a Student

- Click **Suspend** (lock icon) on a student row to block their login. Suspended students receive a message when they try to sign in.
- Click **Restore** (unlock icon) to re-enable access.

> Suspend / Restore is only available for STUDENT accounts.

---

### 2.4 Terms — Term Management

**What it does:** Manage academic terms (year + quarter), enroll students, and set which term is currently active.

#### Creating a Term

1. Click **Add quarter** (top-right).
2. Fill in:
   - **Year** — e.g., `2025-2026`.
   - **Quarter** — 1–4.
   - **Start Date** and **End Date** — optional; use the date picker.
   - **Set as Current Term** — check this to make the new term the active term immediately.
   - **Copy labs from current quarter (optional)** — multi-select labs from the outgoing current quarter to deep-copy into the new quarter (rubric + operational testcases; deadlines and student visibility start fresh). Leave unchecked to create an empty quarter.
3. Click **Create**.

#### Setting the Current Term

- In the terms list, click a term to select it, then click **Set as Current** if it is not already active.
- Only one term can be active at a time. The current term is highlighted with a ★ star badge.

#### Enrolling Students

After selecting a term, the right panel shows two lists:
- **Enrolled Students** — students already in this term.
- **Available Students** — students registered in the system but not yet enrolled.

**Manually adding students:**
1. Search the **Available Students** list by name, IRN, or email.
2. Select one or more students using the checkboxes.
3. Click **Add Selected** (or the **+ add** button on an individual row).

**Removing students:**
- Click the **✕ remove** button on an enrolled student row, or select multiple and click **Remove Selected**.

#### Importing Students via Excel

1. Prepare a spreadsheet with columns for **Student ID / IRN** and **Email**. An optional **Fullname** column is used only when showing who could not be added.
2. Drag the file onto the **drop zone** in the term panel, or click the zone to select the file.
3. Students who already have an account are enrolled immediately. Names that are not in the system stay out of the quarter.
4. If anyone is skipped, a warning popup appears. Click **Show details** to see who is not in the system and who is already in the quarter.

> Accepted file types: `.xlsx`, `.xls`, `.csv`. Only existing active student accounts can be enrolled; import does not create new users.

#### Suspending / Restoring from Roster

The enrolled students list includes **Suspend** and **Restore** actions identical to User Management (student accounts only).

---

### 2.5 Projects — Solution Management

**What it does:** Define lab structures (challenges, class rubrics, testcases) and set submission deadlines.

#### Left Sidebar — Lab List

- Lists labs for the **current quarter**.
- Click a lab to load its structure.
- Click **+** to create a blank lab shell (name + quarter assignment required).
- Click the **copy** icon to copy one or more labs from the **previous current quarter** into a chosen target quarter (same deep-copy rules as New quarter: rubric + OT only; deadlines/visibility reset).

#### Structure Tree (center panel)

Each lab has one or more **Challenges**. Each challenge defines what the student must implement.

**Expanding a challenge** shows its component tree:
- **Classes** — Java classes/interfaces/enums the student must declare.
- Each class has **Fields**, **Constructors**, and **Methods** as rubric items.

**Adding a challenge:**
1. With a lab selected, click **+ Add Challenge** in the structure panel.
2. Enter the challenge name and optional weights.

**Adding a class to a challenge:**
1. Click **+ Add Class** under the challenge.
2. Specify: class name, scope (public/private/etc.), declaring type (class / interface / enum / abstract class), and relation type (inheritance, implementation, etc.).

**Adding fields / constructors / methods:**
1. Click a class in the tree to open the **Class Detail Panel** on the right.
2. Use the **+ Field**, **+ Constructor**, or **+ Method** buttons.
3. Fill in the signature details and save.

#### Weights

Each challenge has three weight sliders:
| Weight | Affects |
|---|---|
| MMD Weight | Score contribution from the UML/MMD diagram check |
| Class Weight | Score contribution from declared class structure |
| Testcase Weight | Score contribution from operational testcases when the challenge has authored OT rows |

Weights must sum to 100 within a challenge. Edit them in the **Challenge Detail Panel**.

#### Testcases

1. Select a challenge in the structure tree.
2. In the **Challenge Detail Panel**, switch to the **Operational Testcases** tab.
3. Click **Add Unit** or **Add Composition**.
4. Mark **Hidden** on a testcase to hide input/expected/actual from students (they still see pass/fail). Example testcases (`Hidden` off) show full I/O on the student Operation Test tab.
5. Click **Save Testcases** (in that tab) to persist testcase changes. Click **Save Structure** separately when you change classes, fields, or methods.

For a full walkthrough (Unit worksheet, Composition script, **Call as** for override checks, assertions, object checks, dry-run), see **[Operational testcases — lecturer guide](./LECTURER_OPERATIONAL_TESTCASE_GUIDE.md)**.

#### Lab Deadline

1. Select a lab from the sidebar.
2. In the **Lab Scheduling Panel** (right side), click the **date picker** to choose a deadline date.
3. Click **Save deadline** to apply it. Students will see the deadline in their lab list.
4. Click **Clear deadline** to remove an existing deadline.

> The date picker does not auto-save; you must click **Save deadline** explicitly.

#### Student Visibility

Toggle **Student Visible** on a lab to control whether students can see and submit to that lab.

#### Saving

Click **Save Structure** (top of structure panel) to persist all challenge/class/rubric changes. A green toast notification confirms a successful save.

Operational testcases use a separate **Save Testcases** button in the Operational Testcases tab.

### 2.5.1 Operational testcases (detailed guide)

See **[LECTURER_OPERATIONAL_TESTCASE_GUIDE.md](./LECTURER_OPERATIONAL_TESTCASE_GUIDE.md)** — Unit and Composition authoring, assertions, object checks, reference Java dry-run, and the operator wipe step. Student upload does not run these tests this ship.

---

### 2.6 Reports — Analytics Dashboard

**What it does:** Class-wide performance analytics with AI-generated insights.

#### Metrics Displayed

| Metric | Meaning |
|---|---|
| Overall Average | Mean score across all students and labs |
| Lowest Average Lab | Which lab students struggled with most |
| Most Difficult Topic | Topic with the lowest average score |
| Lab Trend | Score trend over the semester (chart) |
| At-Risk Labs | Labs where many students scored below threshold |
| At-Risk Students | Students at risk of failing (low average scores) |

#### AI Summary

Below the metrics, an **AI Summary** section provides:
- A title summarizing the current term's performance.
- Detailed analysis text.
- **Recommended Resources** — links or topics suggested for struggling areas.

Click the **Refresh** icon on the Reports page to reload all analytics from the server.

---

### 2.7 Profile & Password

Click your name or avatar in the **top-right header** and select **Edit Profile** (or use the header command) to open the **Change Password** modal.

1. Enter your **Current Password**.
2. Enter a **New Password** (minimum 8 characters).
3. **Confirm New Password** — must match the new password.
4. Click **Save**.

---

## 3. Student Guide

The student interface has two main states: the **Lab Dashboard** (for submitting work) and **History** (for reviewing past submissions).

---

### 3.1 Lab Dashboard — Submit Work

> If you are not enrolled in the current term, you will be redirected to **History** automatically. Contact your lecturer to be enrolled.

#### Lab Sidebar (left panel)

The left sidebar lists every lab available in your current term.

| Label | Meaning |
|---|---|
| Lab name | Click to select that lab for submission |
| `Due MM/DD` | Submission deadline for this lab |
| **Urgent** badge | Deadline is approaching (within a short window) |
| **Expired** badge | Deadline has passed |

Click a lab name to load it on the right panel. The currently selected lab is highlighted.

On mobile, tap the **☰ sidebar toggle** (top-left) to open the lab list as an overlay. Selecting a lab closes the overlay automatically.

#### Stats Row

Once a lab is selected, three stats appear above the upload zone:

| Stat | Meaning |
|---|---|
| **Total Submissions** | Number of attempts you have made for this lab (all time) |
| **Latest Submission** | Timestamp of your most recent upload |
| **Current Grade** | Your score from the **current browser session** upload — shows `--/--` until you upload in this session |

#### Uploading a Submission (DropZone)

1. Prepare your submission folder. It must contain:
   - Your Java source files (`.java`) for each challenge.
   - Your MMD diagram file (`.mmd`) if the challenge requires it.
2. **Drag and drop** your folder onto the dashed drop zone, or click **Browse** to pick files.
3. Select the correct lab from the sidebar first — the upload is tied to the currently selected lab.
4. The system compiles, grades, and returns results automatically. A **toast notification** at the bottom confirms grading is complete with your score.

> Each upload counts as one attempt. All attempts are recorded in History.

---

### 3.2 Reading Your Results

After a successful upload, result tabs appear below the stats row. **Declaration Test** is always shown. **MMD** appears when the challenge has a diagram. **Operation Test** appears when the challenge has authored operational testcases and your attempt was graded with OT (including after upload in the same session).

#### MMD Tab

Compares your uploaded UML/MMD diagram against the expected class diagram.

- **Green ✓** — relationship or element matches the rubric.
- **Red ✗** — mismatch or missing element.
- An amber **⚠ warning banner** appears if your MMD file could not be parsed (check the file format).

#### Declaration Test Tab

Tests whether your Java class declarations (class names, fields, constructors, methods) match the rubric.

- Each **class card** shows pass (green) or fail (red) for the class shell.
- A compile problem is **one short red line** under the class name (for example `Missing ; on line 4`, `Observer not found`, `Wrong class in this file`, `Declared in Observer.java`, or `See Subject`). The line wraps; it is not a raw javac dump.
- Expand a card to see individual field / constructor / method results:
  - **Green ✓** — matches rubric.
  - **Red ✗** — wrong or missing.
  - **Orange ─** — partially correct (attribute credit).
- If the class shell itself fails, all members are marked fail regardless of content.

#### Operation Test Tab (Testcases)

When the challenge has operational testcases, this tab lists **Example Testcases** (full input, expected, and your output) and **Other Testcases** (hidden rows: name and pass/fail only). Unit, Composition, and Call-as type labels are never shown to students. Attempts graded before OT shipped have no persisted testcase rows — the tab stays hidden until you upload again.

#### Challenge Sidebar Scores

The left sidebar shows a **score chip** next to each challenge name after you have uploaded in this session. This reflects your current-session grade, not the all-time best.

---

### 3.3 History — Past Submissions

Click **History** in the header (or navigate to `/student-history`) to view all your past submissions.

#### Switching Between Dashboard and History

- **Home** (header command or button) → returns to the Lab Dashboard.
- **History** (header command or button) → goes to Submission History.

> Out-of-term students can only access History; the Home/Lab Dashboard is hidden.

#### Performance by Lab (left column, wide screens)

A card for each lab shows:
- Lab name
- Your best score and latest score
- Number of attempts

**Click a lab card** to filter the Submissions table to that lab only. Click again to show all labs.

#### All Submissions Table (right column)

| Column | Meaning |
|---|---|
| Lab | Which lab was submitted |
| Submitted At | Date and time of the attempt |
| Score | Graded score for that attempt |
| Status | `Passed` (>80), `Partial` (50–80), `Failed` (<50), or `Pending` |

- Click any **column header** to sort ascending / descending (server-side sort).
- Use **← Prev** and **Next →** buttons to page through results (10 per page).
- **Expand a row** (click the row or chevron) to see per-challenge scores for that submission.

#### Out-of-Term Warning

If you are active but not enrolled in the current term, an amber warning banner appears above the table: *"You are not enrolled in the current term…"*. Submissions you made in past terms are still visible.

---

### 3.4 Notifications

The **bell icon** (🔔) in the top-right of the student dashboard shows a red dot when you have unread notifications.

- Click the bell to open the notification dropdown.
- Notifications indicate labs where you have not submitted yet (based on your submission history vs. the lab list).
- Once you have read all items the red dot disappears for the current session.

---

### 3.5 Profile & Password

Click your avatar or name in the **top-right header** and select **Edit Profile** to open the **Change Password** modal.

1. Enter your **Current Password**.
2. Enter a **New Password** (minimum 8 characters).
3. **Confirm New Password** — must match the new password.
4. Click **Save**.

---

## 4. Common Questions

**Q: I signed in with Google but the system shows an error.**  
A: Only `@eiu.edu.vn` Google accounts are accepted. If this is your first login, complete the first-time setup form to link your IRN.

---

**Q: My submission shows "Grading complete" but the score is 0.**  
A: This usually means your Java files did not compile. Check the Declaration Test tab for compilation errors (a red error line is shown at the top of a failed class card). Fix the errors and re-upload.

---

**Q: I cannot see any labs on my dashboard.**  
A: You must be enrolled in the current term. Contact your lecturer or check your term enrollment status. If enrolled, labs may not yet be marked as student-visible — contact your lecturer.

---

**Q: The deadline has passed — can I still submit?**  
A: Labs with an **Expired** badge may still accept uploads depending on lecturer settings. Try uploading; if the server rejects it, contact your lecturer.

---

**Q: A lab shows a ⚠ plagiarism flag next to my name (lecturer view).**  
A: The system detected code similarity between submissions. Flags usually appear within a few seconds of a student upload — refresh the roster if a new upload is not flagged yet. Lecturers can click the flag to see an overlap percentage report. This is an automated check and does not constitute a final ruling.

---

**Q: I want to export grade data.**  
A: Lecturers can export from:  
- **Dashboard → Overview tab** — per-lab student roster (CSV / Excel).  
- **Grading section** — full cross-lab grade matrix (CSV / Excel).

---

**Q: How do I import students into a term?**  
A: Go to **Terms**, select the term, and drag an Excel/CSV file onto the drop zone. The file must have columns for Student ID (or IRN) and Email. Existing accounts are enrolled; click **Details** if some rows are missing from the system. See [Section 2.4](#24-terms--term-management) for details.

---

**Q: What does Active Users in the footer mean?**  
A: It is how many people are currently signed in and using the app. The count refreshes every 10 seconds and drops when someone logs out. The course title stays in the center of the footer.

---

*Last updated: September 2026*
