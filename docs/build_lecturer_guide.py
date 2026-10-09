from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import cm
from reportlab.lib import colors
from reportlab.platypus import (SimpleDocTemplate, Paragraph, Spacer, Table,
                                TableStyle, PageBreak, ListFlowable, ListItem)

OUT = r"d:\EIU\OOP-AutoGrader\docs\Lecturer_Guide.pdf"
PRIMARY = colors.HexColor("#1d4ed8")

ss = getSampleStyleSheet()
body = ParagraphStyle("body", parent=ss["BodyText"], fontSize=10.5, leading=15, spaceAfter=6)
h1 = ParagraphStyle("h1", parent=ss["Heading1"], textColor=PRIMARY, fontSize=18, spaceBefore=14, spaceAfter=8)
h2 = ParagraphStyle("h2", parent=ss["Heading2"], textColor=colors.HexColor("#111827"), fontSize=13, spaceBefore=10, spaceAfter=6)
title = ParagraphStyle("title", parent=ss["Title"], textColor=PRIMARY, fontSize=28, leading=34)
sub = ParagraphStyle("sub", parent=body, alignment=1, fontSize=13, textColor=colors.grey)
note = ParagraphStyle("note", parent=body, backColor=colors.HexColor("#fef3c7"), borderPadding=6,
                      leftIndent=6, rightIndent=6, spaceBefore=6, spaceAfter=10)
cell = ParagraphStyle("cell", parent=body, fontSize=9.5, leading=13, spaceAfter=0)

def P(t, s=body): return Paragraph(t, s)
def bullets(items):
    return ListFlowable([ListItem(P(i), leftIndent=12) for i in items], bulletType="bullet", leftIndent=14)
def steps(items):
    return ListFlowable([ListItem(P(i), leftIndent=14) for i in items], bulletType="1", leftIndent=16)
def table(rows, widths):
    t = Table([[P(c, cell) for c in r] for r in rows], colWidths=widths, repeatRows=1)
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#dbeafe")),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#9ca3af")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("TOPPADDING", (0, 0), (-1, -1), 4), ("BOTTOMPADDING", (0, 0), (-1, -1), 4)]))
    return t
def footer(c, d):
    c.saveState(); c.setFont("Helvetica", 8); c.setFillColor(colors.grey)
    c.drawString(2 * cm, 1.2 * cm, "OOP AutoGrader - Lecturer Guide")
    c.drawRightString(A4[0] - 2 * cm, 1.2 * cm, f"Page {d.page}"); c.restoreState()

W2 = [5 * cm, 12 * cm]
s = [Spacer(1, 6 * cm), P("OOP AutoGrader", title), P("Lecturer Guide", sub),
     P("Setup, grading, monitoring and reporting", sub), Spacer(1, 1 * cm),
     P("EIU (Eastern International University) - OOP course", sub), PageBreak()]

s += [P("Contents", h1), bullets([
    "1. Overview and typical workflow", "2. Logging in", "3. Terms - quarters and enrollment",
    "4. Users - accounts", "5. Projects - labs, challenges and rubrics", "6. Operational testcases",
    "7. Deadlines and student visibility", "8. Dashboard - grading overview", "9. Grading - cross-lab matrix",
    "10. Plagiarism flags", "11. Reports - analytics", "12. Profile and password",
    "13. Offline practice packs", "14. Troubleshooting and FAQ", "15. Term-start checklist"]), PageBreak()]

s += [P("1. Overview and typical workflow", h1),
      P("The lecturer interface has six sections in the top navigation: <b>Dashboard</b>, <b>Grading</b>, "
        "<b>Users</b>, <b>Terms</b>, <b>Projects</b> and <b>Reports</b>."),
      P("Students upload Java and Mermaid (.mmd) files. The system compiles them and grades them "
        "against the rubric you define in Projects."),
      P("Recommended order for a new term", h2),
      steps(["<b>Terms</b>: create the quarter, set it as current, optionally copy labs from the previous quarter.",
             "<b>Terms</b>: enroll students (manually or by Excel import).",
             "<b>Projects</b>: build or review each lab - challenges, classes, members, weights, testcases.",
             "<b>Projects</b>: set the deadline and switch <b>Student Visible</b> on.",
             "<b>Dashboard / Grading</b>: monitor submissions, scores and plagiarism flags.",
             "<b>Reports</b>: review analytics and at-risk students."])]

s += [P("2. Logging in", h1),
      table([["Method", "How"],
             ["IRN + Password", "Enter your lecturer IRN and password, then click <b>Sign In</b>."],
             ["Google (EIU)", "Click <b>Sign in with Google</b> and choose your <b>@eiu.edu.vn</b> account."]], [4 * cm, 13 * cm]),
      Spacer(1, 6),
      bullets(["First Google login: set your IRN and a password, then click <b>Complete Setup</b>.",
               "Forgot password: use the <b>Forgot password?</b> link and the emailed reset link.",
               "Lecturers land on the Lecturer Dashboard. Accounts with both roles can also open student pages by URL."])]

s += [PageBreak(), P("3. Terms - quarters and enrollment", h1),
      P("Manage academic terms (year + quarter), enroll students and choose the active term. "
        "Only one term is current at a time; it is marked with a star badge."),
      P("3.1 Create a term", h2),
      steps(["Click <b>Add quarter</b>.",
             "Enter <b>Year</b> (for example 2025-2026) and <b>Quarter</b> (1-4); optional start and end dates.",
             "Tick <b>Set as Current Term</b> to activate it immediately.",
             "Optionally choose <b>Copy labs from current quarter</b>: rubric and operational testcases are deep-copied; "
             "deadlines and student visibility start fresh.",
             "Click <b>Create</b>."]),
      P("3.2 Set the current term", h2),
      P("Select a term in the list and click <b>Set as Current</b>."),
      P("3.3 Enroll students", h2),
      P("The right panel lists <b>Enrolled Students</b> and <b>Available Students</b>."),
      steps(["Search Available Students by name, IRN or email.",
             "Tick one or more students and click <b>Add Selected</b> (or the + button on a row).",
             "To remove, use the remove button on a row, or select several and click <b>Remove Selected</b>."]),
      P("3.4 Import students from Excel", h2),
      steps(["Prepare a .xlsx, .xls or .csv with <b>Student ID / IRN</b> and <b>Email</b> columns "
             "(optional <b>Fullname</b>, used only to report skipped rows).",
             "Drag the file onto the drop zone, or click it to browse.",
             "Existing student accounts are enrolled immediately.",
             "If rows are skipped, a warning appears; click <b>Show details</b> to see who is not in the system "
             "and who is already enrolled."]),
      P("Import does not create new users. Create missing accounts in Users first, then import again.", note),
      P("3.5 Suspend / Restore from the roster", h2),
      P("The enrolled list includes Suspend and Restore actions, identical to the Users page (students only).")]

s += [PageBreak(), P("4. Users - accounts", h1),
      P("Create, edit, delete, suspend and restore accounts. Use the search box to filter by name, IRN or email."),
      table([["Action", "How"],
             ["Add user", "Click <b>Add User</b>. Fill Full Name, EIU Email, Role (STUDENT and/or LECTURER), IRN/ID and "
                          "Password (optional for Google-only users, required for IRN login). Click <b>Save</b>."],
             ["Edit", "Click the pencil icon, change fields, <b>Save</b>."],
             ["Delete", "Click the trash icon and confirm. Permanent; submission history remains but the account is removed."],
             ["Suspend", "Lock icon on a student row. The student cannot sign in and sees a message."],
             ["Restore", "Unlock icon on a suspended student to re-enable access."]], W2),
      P("Suspend and Restore are available for STUDENT accounts only.", note)]

s += [P("5. Projects - labs, challenges and rubrics", h1),
      P("Define what students must implement and how it is scored."),
      P("5.1 Lab list (left sidebar)", h2),
      bullets(["Shows labs of the <b>current quarter</b>; click one to load its structure.",
               "<b>+</b> creates a blank lab shell (name and quarter required).",
               "The <b>copy</b> icon copies labs from the previous current quarter into a chosen quarter "
               "(rubric and testcases only; deadlines and visibility reset)."]),
      P("5.2 Structure tree", h2),
      P("Each lab has one or more <b>Challenges</b>. Each challenge contains <b>Classes</b>, and each class has "
        "<b>Fields</b>, <b>Constructors</b> and <b>Methods</b> as rubric items."),
      steps(["Select a lab and click <b>+ Add Challenge</b>; enter the name and optional weights.",
             "Under the challenge click <b>+ Add Class</b>: class name, scope (public/private...), declaring type "
             "(class / interface / enum / abstract class) and relation type (inheritance, implementation...).",
             "Click a class to open the Class Detail Panel and use <b>+ Field</b>, <b>+ Constructor</b>, <b>+ Method</b>.",
             "Fill in signature details and save."]),
      P("5.3 Weights", h2),
      table([["Weight", "Affects"],
             ["MMD Weight", "Score from the UML/MMD diagram check"],
             ["Class Weight", "Score from declared class structure"],
             ["Testcase Weight", "Score from operational testcases (when the challenge has them)"]], W2),
      P("Weights must sum to 100 within a challenge. Edit them in the Challenge Detail Panel.", note),
      P("5.4 Saving", h2),
      P("Click <b>Save Structure</b> to persist challenge, class and rubric changes; a green toast confirms it. "
        "Testcases use their own <b>Save Testcases</b> button.")]

s += [PageBreak(), P("6. Operational testcases", h1),
      P("Operational testcases (OT) check behaviour, not just structure."),
      steps(["Select a challenge in the structure tree.",
             "In the Challenge Detail Panel open the <b>Operational Testcases</b> tab.",
             "Click <b>Add Unit</b> or <b>Add Composition</b> and author the testcase.",
             "Mark a testcase <b>Hidden</b> to hide input, expected and actual output from students "
             "(they still see pass/fail). Non-hidden rows are shown as examples with full I/O.",
             "Click <b>Save Testcases</b>. Use <b>Save Structure</b> separately for class/field/method changes."]),
      bullets(["Students never see whether a testcase is Unit, Composition or Call-as.",
               "Attempts graded before testcases were added have no stored OT rows; students must upload again.",
               "For the Unit worksheet, Composition script, Call-as overrides, assertions, object checks and "
               "reference-Java dry-run, see <b>docs/LECTURER_OPERATIONAL_TESTCASE_GUIDE.md</b>."])]

s += [P("7. Deadlines and student visibility", h1),
      P("7.1 Deadline", h2),
      steps(["Select a lab in the sidebar.",
             "In the <b>Lab Scheduling Panel</b> pick a date with the date picker.",
             "Click <b>Save deadline</b>. Students see it in their lab list.",
             "Click <b>Clear deadline</b> to remove it."]),
      P("The date picker does not auto-save; you must click Save deadline.", note),
      P("7.2 Student Visible", h2),
      P("Toggle <b>Student Visible</b> to control whether students can see and submit to the lab. "
        "A hidden lab does not appear on their dashboard.")]

s += [PageBreak(), P("8. Dashboard - grading overview", h1),
      P("Live statistics and per-lab submissions for the current term."),
      P("8.1 Summary cards", h2),
      table([["Card", "Meaning"],
             ["Total Students", "Students enrolled in the current term"],
             ["Average Score", "Class-wide average across graded labs"],
             ["Total Labs", "Labs configured for the term"],
             ["At-Risk Students", "Students whose total is below 70 (missing labs count as 0)"]], W2),
      P("Click a card to open the list behind the number."),
      P("8.2 Selecting a lab and tabs", h2),
      bullets(["Pick a lab under <b>Select Lab Assignment</b>. A red warning icon means at least one plagiarism flag in that lab.",
               "<b>Overview</b> tab: lab-wide statistics and the student roster.",
               "<b>Challenge</b> tabs: one per challenge, listing who submitted it."]),
      P("8.3 Overview tab", h2),
      bullets(["Statistics: average, completion rate, highest, lowest, total submissions, enrolled, submitted.",
               "Grade Distribution bar chart by score band.",
               "<b>Export</b> the view as CSV or Excel.",
               "Roster: search by name or IRN, sort by column header.",
               "Eye icon opens the <b>Attempt History Drawer</b> (every attempt with score and time).",
               "Warning icon opens the <b>Plagiarism Investigation Drawer</b>."]),
      P("8.4 Challenge tab", h2),
      P("Lists students who submitted that challenge. <b>View Submission</b> opens a drawer with the class declarations, "
        "MMD and testcase results. The refresh button (top-right) reloads all data from the server.")]

s += [P("9. Grading - cross-lab matrix", h1),
      P("One table with students as rows and labs as columns."),
      bullets(["Search by name or IRN; click headers to sort.",
               "A red warning icon in a cell marks a plagiarism flag for that student and lab.",
               "Click a student row to expand the <b>Submission History</b> panel with all attempts across labs; "
               "filter by lab with the dropdown and sort by header.",
               "<b>Export</b> (top-right) downloads the whole matrix as CSV or Excel."])]

s += [P("10. Plagiarism flags", h1),
      P("The system compares submissions automatically. Flags usually appear within seconds of a student upload; "
        "refresh the roster if a new upload is not flagged yet."),
      bullets(["Click a flag to see which students overlap and by what percentage.",
               "This is an automated indicator, not a final ruling - review the code yourself before acting.",
               "Students are never told about plagiarism checks in the upload screen."])]

s += [PageBreak(), P("11. Reports - analytics", h1),
      table([["Metric", "Meaning"],
             ["Overall Average", "Mean score across all students and labs"],
             ["Lowest Average Lab", "The lab students struggled with most"],
             ["Most Difficult Topic", "Topic with the lowest average score"],
             ["Lab Trend", "Score trend over the term (chart)"],
             ["At-Risk Labs", "Labs where many students scored below the threshold"],
             ["At-Risk Students", "Students at risk of failing"]], W2),
      Spacer(1, 6),
      P("The <b>AI Summary</b> adds a title, written analysis and recommended resources for weak areas. "
        "Use the refresh icon to reload analytics."),
      P("12. Profile and password", h1),
      steps(["Click your name or avatar (top-right) and choose <b>Edit Profile</b>.",
             "Enter the <b>Current Password</b>, then a <b>New Password</b> (min 8 characters, different from current).",
             "Confirm the new password and click <b>Save</b>."]),
      P("13. Offline practice packs", h1),
      P("Students can practise offline on Windows. From <b>Terms</b>, the term panel provides a download of the "
        "rubric pack (<b>.agpack</b>, named like Rubric_2025_Q1.agpack). Distribute it to students together with the "
        "runtime zip they download from their dashboard. Details: <b>docs/DESKTOP_STUDENT_DIST.md</b>.")]

s += [PageBreak(), P("14. Troubleshooting and FAQ", h1),
      table([["Problem", "Solution"],
             ["Students cannot see a lab", "Check the student is enrolled in the current term and the lab has Student Visible on."],
             ["Excel import skipped students", "Click Show details. Create the missing accounts in Users, then import again."],
             ["Weights rejected", "MMD + Class + Testcase weights must total 100 per challenge."],
             ["Changes not saved", "Click Save Structure (structure) and Save Testcases (testcases) separately; deadlines need Save deadline."],
             ["Student score 0", "Usually a compile error. Open the student's submission drawer and check the class results."],
             ["Student cannot log in", "The account may be suspended. Restore it in Users or the Terms roster."],
             ["Deadline passed", "Expired labs may still accept uploads. Clear or move the deadline to change this."],
             ["Plagiarism flag not shown", "Wait a few seconds and refresh the roster."],
             ["Need grades in a spreadsheet", "Use Export on the Dashboard Overview (per lab) or Grading (full matrix)."]], W2),
      P("15. Term-start checklist", h1),
      bullets(["New quarter created and set as current.",
               "Labs copied or created; each challenge has classes, members and weights totalling 100.",
               "Operational testcases authored and saved (hidden ones marked).",
               "Deadlines saved for every lab.",
               "Student Visible switched on for labs students should see.",
               "Students enrolled (manual or Excel import) and missing accounts created.",
               "Offline rubric pack distributed if students practise offline.",
               "After launch: check Dashboard, plagiarism flags and Reports regularly."])]

SimpleDocTemplate(OUT, pagesize=A4, leftMargin=2 * cm, rightMargin=2 * cm, topMargin=2 * cm,
                  bottomMargin=2 * cm, title="OOP AutoGrader - Lecturer Guide").build(
    s, onFirstPage=footer, onLaterPages=footer)
print(OUT)
