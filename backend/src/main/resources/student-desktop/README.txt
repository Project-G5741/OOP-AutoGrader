OOP AutoGrader — offline practice (Windows)

This folder runs locally on your PC. Scores here are for practice only and are not submitted to the website.

Quick start
1. Unzip the whole folder to a writable location (e.g. Desktop\OOP-Practice).
2. Double-click OOP-AutoGrader-Practice.exe (preferred) or OOP-AutoGrader-Practice.bat
3. Use the student dashboard in the window to upload Java + MMD files.

Requirements
- Windows 10/11 x64
- Java 17+ on PATH, OR a portable JRE in runtime\jdk\ (optional; not included in this download)
- Microsoft Edge WebView2 (usually already installed on Windows 11)

If OOP-AutoGrader-Practice.exe does nothing, unzip a fresh copy from the website (all .dll files must sit next to the .exe), or run OOP-AutoGrader-Practice.bat and read the console. Check launcher-error.log in this folder for details.

Updating rubrics
- In the app: click Import rubric pack in the header, choose a file, then restart the app.
- Or copy packs into rubric\ using names like Rubric_{year}_Q{n}.agpack (full quarter) or Rubric_{labName}.agpack (one lab).
- Quarter packs replace all labs; lab packs add or replace one lab. See docs\DESKTOP_STUDENT_DIST.md in the repo for details.

Notes
- Do not delete the data\ folder while practicing; it stores your local attempts.
- Practice API port is 127.0.0.1:18002 (not the website dev port 8002).
