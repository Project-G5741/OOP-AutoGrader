@echo off
setlocal EnableExtensions
set "APP_DESKTOP_HOME=%~dp0"
if "%APP_DESKTOP_HOME:~-1%"=="\" set "APP_DESKTOP_HOME=%APP_DESKTOP_HOME:~0,-1%"
set "SPRING_PROFILES_ACTIVE=desktop"
set "JWT_SECRET=desktop-local-dev-secret-minimum-32-bytes!!"
cd /d "%APP_DESKTOP_HOME%"
if not exist "%APP_DESKTOP_HOME%\backend.jar" (
  echo Missing backend.jar in this folder.
  pause
  exit /b 1
)
if not exist "%APP_DESKTOP_HOME%\rubric" (
  mkdir "%APP_DESKTOP_HOME%\rubric"
)
echo Import packs from the app header: Rubric_{year}_Q{n}.agpack or Rubric_{name}.agpack - restart after import.
if exist "%APP_DESKTOP_HOME%\runtime\jdk\bin\java.exe" (
  set "JAVA_EXE=%APP_DESKTOP_HOME%\runtime\jdk\bin\java.exe"
) else (
  set "JAVA_EXE=java"
  where java >nul 2>&1 || (
    echo Java 17 or newer is required. Install a JDK or add runtime\jdk under this folder.
    pause
    exit /b 1
  )
)
set "DESKTOP_WORKER_JAVA=%JAVA_EXE%"
echo Starting OOP AutoGrader offline practice...
echo Keep this window open while you practice. Close it to stop the grader.
echo For the practice window, run OOP-AutoGrader-Practice.exe in this folder.
"%JAVA_EXE%" -Xmx512m -jar "%APP_DESKTOP_HOME%\backend.jar"
pause
