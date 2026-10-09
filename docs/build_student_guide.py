from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import cm
from reportlab.lib import colors
from reportlab.platypus import (SimpleDocTemplate, Paragraph, Spacer, Table,
                                TableStyle, PageBreak, ListFlowable, ListItem)

OUT = r"d:\EIU\OOP-AutoGrader\docs\Student_Guide.pdf"
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
code = ParagraphStyle("code", parent=body, fontName="Courier", fontSize=9.5, leading=13,
                      backColor=colors.HexColor("#f3f4f6"), borderPadding=6, leftIndent=6)

def P(t, s=body): return Paragraph(t, s)

def bullets(items):
    return ListFlowable([ListItem(P(i), leftIndent=12) for i in items], bulletType="bullet", leftIndent=14)

def steps(items):
    return ListFlowable([ListItem(P(i), leftIndent=14) for i in items], bulletType="1", leftIndent=16)

def table(rows, widths):
    data = [[P(c, cell) for c in r] for r in rows]
    t = Table(data, colWidths=widths, repeatRows=1)
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#dbeafe")),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#9ca3af")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("TOPPADDING", (0, 0), (-1, -1), 4), ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
    ]))
    return t

def footer(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(colors.grey)
    canvas.drawString(2 * cm, 1.2 * cm, "OOP AutoGrader - Student Guide")
    canvas.drawRightString(A4[0] - 2 * cm, 1.2 * cm, f"Page {doc.page}")
    canvas.restoreState()

s = []
s += [Spacer(1, 6 * cm), P("OOP AutoGrader", title), P("Student Guide", sub),
      P("From login to submission and results", sub), Spacer(1, 1 * cm),
      P("EIU (Eastern International University) - OOP course", sub), PageBreak()]

s += [P("Contents", h1), bullets([
    "1. Overview", "2. Before you start", "3. Logging in", "4. The Lab Dashboard",
    "5. Preparing your submission folder", "6. Uploading your work",
    "7. Reading your results", "8. Submission History", "9. Notifications",
    "10. Profile and password", "11. Offline practice (optional)",
    "12. Troubleshooting and FAQ", "13. Submission checklist"]), PageBreak()]

s += [P("1. Overview", h1),
      P("OOP AutoGrader checks your Java assignments automatically. You upload a project folder "
        "containing your <b>.java</b> files and, where required, a <b>.mmd</b> (Mermaid class diagram). "
        "The system compiles your code, compares it with the lecturer's rubric, and returns a score "
        "in a few seconds."),
      P("Each upload is one <b>attempt</b>. You can submit as many times as the lab allows, and every "
        "attempt is saved in your History."),
      table([["Part of the score", "What is checked"],
             ["Declaration Test", "Class names, fields, constructors and methods match the rubric"],
             ["MMD", "Your UML/Mermaid diagram matches the expected class diagram"],
             ["Operation Test", "Behaviour testcases (only when the lecturer added them)"]],
            [5 * cm, 12 * cm])]

s += [P("2. Before you start", h1), bullets([
    "An <b>@eiu.edu.vn</b> Google account, or your IRN and password.",
    "Your lecturer must have <b>enrolled you in the current term</b>. If not, you can only see History.",
    "A modern browser (Chrome or Edge recommended). Drag-and-drop folder upload works best there.",
    "The lab must be marked visible by your lecturer; otherwise it will not appear in your list."])]

s += [P("3. Logging in", h1),
      table([["Method", "How"],
             ["IRN + Password", "Enter your IRN (student ID) and password, then click <b>Sign In</b>."],
             ["Google (EIU)", "Click <b>Sign in with Google</b> and choose your <b>@eiu.edu.vn</b> account. "
                              "Other Google accounts are rejected."]],
            [4 * cm, 13 * cm]),
      Spacer(1, 8),
      P("3.1 First-time Google login", h2),
      steps(["Sign in with your EIU Google account.",
             "A setup form appears. Enter your <b>IRN</b> and choose a <b>password</b>.",
             "Click <b>Complete Setup</b>. From now on you can use either login method."]),
      P("3.2 Forgot password", h2),
      steps(["Click <b>Forgot password?</b> on the login page.",
             "Enter your EIU email address.",
             "Open the email you receive and follow the reset link."]),
      P("3.3 Where you land after login", h2),
      bullets(["<b>Enrolled in current term</b> - Student Dashboard (submit page).",
               "<b>Not enrolled</b> - Submission History only, with an amber warning banner. "
               "Ask your lecturer to enroll you.",
               "<b>Suspended account</b> - you cannot sign in; contact your lecturer."]),
      P("Your session lasts while the browser tab is open. If you are signed out unexpectedly, "
        "just log in again.", note)]

s += [PageBreak(), P("4. The Lab Dashboard", h1),
      P("This is where you submit work. It has a lab list on the left and the main work area on the right."),
      P("4.1 Lab sidebar", h2),
      table([["Item", "Meaning"],
             ["Lab name", "Click to select the lab you want to submit to"],
             ["Due MM/DD", "Submission deadline"],
             ["Urgent badge", "Deadline is close"],
             ["Expired badge", "Deadline has passed (uploads may still be accepted depending on the lecturer)"]],
            [4 * cm, 13 * cm]),
      Spacer(1, 6),
      P("On a phone or narrow window, tap the sidebar toggle (top-left) to open the lab list. "
        "On desktop the same button collapses or expands it."),
      P("4.2 Stats row", h2),
      table([["Stat", "Meaning"],
             ["Total Submissions", "Number of attempts you made for this lab"],
             ["Latest Submission", "Time of your most recent upload"],
             ["Current Grade", "Score from the upload you made in this browser session. "
                               "Shows --/-- until you upload in this session."]],
            [4.5 * cm, 12.5 * cm]),
      P("4.3 Challenges", h2),
      P("A lab contains one or more <b>challenges</b>. After you upload, each challenge shows a score "
        "chip in the sidebar. Click a challenge to see its detailed results.")]

s += [P("5. Preparing your submission folder", h1),
      P("The upload expects one <b>root folder</b> that contains one sub-folder per challenge. "
        "Name it <b>IRN_YourName</b> and name the challenge folders <b>challenge_1</b>, "
        "<b>challenge_2</b>, and so on."),
      P("IRN_YourName/<br/>"
        "&nbsp;&nbsp;challenge_1/<br/>"
        "&nbsp;&nbsp;&nbsp;&nbsp;Student.java<br/>"
        "&nbsp;&nbsp;&nbsp;&nbsp;Course.java<br/>"
        "&nbsp;&nbsp;&nbsp;&nbsp;diagram.mmd<br/>"
        "&nbsp;&nbsp;challenge_2/<br/>"
        "&nbsp;&nbsp;&nbsp;&nbsp;Main.java", code),
      Spacer(1, 6),
      bullets(["Only <b>.java</b> and <b>.mmd</b> files are used. Other files are ignored.",
               "Challenge folders must match <b>challenge_N</b> (or challenge-N). Any other folder name "
               "causes an \"Invalid folder structure\" error.",
               "Text after the name (for example <b>_lab_1</b>) on the root folder is ignored.",
               "A <b>.git</b> folder beside the challenge folders is allowed.",
               "Include a .mmd file only for challenges that require a diagram.",
               "Make sure each Java file's class name matches its file name, and the code compiles "
               "on your own machine first."])]

s += [PageBreak(), P("6. Uploading your work", h1),
      steps(["<b>Select the lab</b> in the left sidebar. The upload is tied to the selected lab.",
             "Open the <b>drop zone</b> (\"Drop your project files here\").",
             "<b>Drag your root folder</b> (IRN_YourName) onto it, or click <b>Select Project</b> "
             "and choose the folder.",
             "Wait a few seconds while the system compiles and grades. Do not click upload again.",
             "A green message appears: <b>\"Grading complete. Your score: N/100\"</b>.",
             "Result tabs and challenge scores appear below the stats row."]),
      P("Each upload is a new attempt. The system does not overwrite older attempts, and the attempt "
        "number is assigned automatically.", note),
      P("6.1 Upload errors", h2),
      table([["Message", "What to do"],
             ["Please drop a valid project folder with .mmd/.java files...", "Your folder has no .java/.mmd files in challenge folders."],
             ["Invalid folder structure", "Rename the root to IRN_YourName and the challenge folders to challenge_1, challenge_2..."],
             ["Missing lab or attempt info", "Select a lab in the sidebar first."],
             ["You must be signed in to upload", "Log in again."],
             ["Server Busy", "Wait a moment and retry. If it persists, tell your lecturer."]],
            [7 * cm, 10 * cm])]

s += [PageBreak(), P("7. Reading your results", h1),
      P("Result tabs are hidden until you complete an upload in the current session. Pick a challenge "
        "in the sidebar, then choose a tab."),
      P("7.1 Declaration Test", h2),
      P("Checks the structure of your Java classes."),
      bullets(["Each <b>class card</b> is green (passed) or red (failed).",
               "Expand a card to see fields, constructors and methods. Each is <b>pass</b> or <b>fail</b>.",
               "If the class itself is wrong (name, type such as class/interface/enum, or it does not compile), "
               "all its members are marked failed.",
               "A compile problem appears as a short red line under the class name, for example "
               "<i>Missing ; on line 4</i> or <i>Observer not found</i>.",
               "For missing or wrong members you see generic messages; the exact expected names are not revealed."]),
      P("7.2 MMD", h2),
      bullets(["Shown only when the challenge needs a diagram.",
               "Green tick = element or relationship matches. Red cross = wrong or missing.",
               "An amber warning banner means your .mmd file could not be parsed - check its syntax."]),
      P("7.3 Operation Test", h2),
      bullets(["Shown only when the lecturer added operational testcases.",
               "<b>Example Testcases</b> show input, expected output and your output.",
               "<b>Other Testcases</b> are hidden: only the name and pass/fail are shown."]),
      P("7.4 Score status", h2),
      table([["Status", "Score"], ["Passed", "above 80"], ["Partial", "50 to 80"],
             ["Failed", "below 50"], ["Pending", "not graded yet"]], [5 * cm, 12 * cm]),
      P("Scores are rounded down to whole numbers everywhere in the app.", note)]

s += [P("8. Submission History", h1),
      P("Open <b>History</b> from the header to review every past attempt. Use <b>Home</b> to return "
        "to the Lab Dashboard."),
      P("8.1 Performance by Lab", h2),
      P("A card per lab shows your best score, latest score and number of attempts. "
        "Click a card to filter the table to that lab; click again to clear the filter."),
      P("8.2 All Submissions table", h2),
      table([["Column", "Meaning"], ["Lab", "Which lab"], ["Attempt", "Attempt number (#1, #2...)"],
             ["Submitted At", "Date and time"], ["Score", "Score for that attempt"],
             ["Status", "Passed / Partial / Failed / Pending"]], [4 * cm, 13 * cm]),
      bullets(["Click a column header to sort ascending or descending.",
               "Use <b>Prev</b> and <b>Next</b> to page through results (10 per page).",
               "Click a row to expand per-challenge scores for that attempt."]),
      P("Note: the dashboard shows your <b>latest attempt</b> for each lab. History also shows your best score.", note)]

s += [P("9. Notifications", h1),
      P("The bell icon (top-right) shows a red dot when you have not yet submitted to a lab in the current term. "
        "Opening the dropdown marks the current notifications as read, and the dot disappears until a new one appears."),
      P("10. Profile and password", h1),
      steps(["Click your name or avatar in the top-right header and choose <b>Edit Profile</b>.",
             "Enter your <b>Current Password</b>.",
             "Enter a <b>New Password</b> (at least 8 characters, different from the current one).",
             "Re-enter it in <b>Confirm New Password</b>.",
             "Click <b>Save</b>. A toast confirms the change."]),
      P("11. Offline practice (optional)", h1),
      P("Your lecturer may provide a Windows offline practice bundle so you can test without the web server."),
      steps(["On the web dashboard, click <b>Download practice folder</b> to get the runtime zip.",
             "Get the rubric pack file from your lecturer (named like <b>Rubric_2025_Q1.agpack</b>).",
             "Start the practice app and click <b>Import rubric pack</b>.",
             "When the import finishes, press <b>Okay</b>; the app closes. Reopen it manually.",
             "Upload folders exactly as you would on the web version."]),
      P("Offline attempts are for practice only and are not recorded in the official History.", note)]

s += [PageBreak(), P("12. Troubleshooting and FAQ", h1),
      table([["Problem", "Solution"],
             ["Google login shows an error", "Use an @eiu.edu.vn account. On first login, complete the setup form with your IRN."],
             ["I cannot see any labs", "You must be enrolled in the current term and the lab must be visible. Ask your lecturer."],
             ["Only History is available", "You are not enrolled in the current term. Ask your lecturer to enroll you."],
             ["Score is 0 after grading", "Your code probably did not compile. Open Declaration Test and read the red error line. Fix and re-upload."],
             ["MMD tab missing", "That challenge has no diagram requirement."],
             ["Operation Test tab missing", "The challenge has no testcases, or this attempt was graded before they were added. Upload again."],
             ["Current Grade shows --/--", "Normal until you upload during this session. Check History for earlier scores."],
             ["Lab shows Expired", "Try uploading; if rejected, contact your lecturer."],
             ["Upload seems stuck", "Wait up to a minute, then check History before retrying to avoid duplicate attempts."],
             ["Signed out suddenly", "Your session ended. Log in again and re-upload if needed."]],
            [5 * cm, 12 * cm]),
      P("13. Submission checklist", h1),
      bullets(["Logged in and enrolled in the current term.",
               "Correct lab selected in the sidebar.",
               "Root folder named IRN_YourName.",
               "Challenge folders named challenge_1, challenge_2, ...",
               "Only .java and .mmd files inside; code compiles locally.",
               "Class names match file names; .mmd syntax is valid.",
               "Uploaded before the deadline and saw the \"Grading complete\" message.",
               "Reviewed Declaration Test, MMD and Operation Test results.",
               "Confirmed the attempt appears in History."])]

SimpleDocTemplate(OUT, pagesize=A4, leftMargin=2 * cm, rightMargin=2 * cm,
                  topMargin=2 * cm, bottomMargin=2 * cm,
                  title="OOP AutoGrader - Student Guide").build(s, onFirstPage=footer, onLaterPages=footer)
print(OUT)
