# Lecturer offline Bulk Grading assemble (stub — completed in plan U7).
# Parallel to assemble-student-desktop.ps1; produces a distinct LecturerBulk folder/EXE.
#
# Intended usage (U7):
#   .\scripts\assemble-lecturer-desktop.ps1 -OutputDir "backend\target\LecturerBulk"
#
# Prerequisites sketched for U7:
#   - mvn -DskipTests package -Pdesktop-dist
#   - npm run build:desktop-lecturer  -> ui/dist-desktop-lecturer
#   - SPRING_PROFILES_ACTIVE=desktop-lecturer, APP_DESKTOP_HOME=<install>, port 18003

param(
    [string]$OutputDir = "backend\target\LecturerBulk"
)

Write-Host "assemble-lecturer-desktop.ps1: stub only (U1). Full assemble lands in U7."
Write-Host "Planned output: $OutputDir"
Write-Host "Profile: desktop-lecturer | UI: dist-desktop-lecturer | default port: 18003"
exit 0
