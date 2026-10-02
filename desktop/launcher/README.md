# WebView2 practice launcher (U7)

.NET 8 WinForms host at `OOPAutoGraderPractice/`. Builds `OOP-AutoGrader-Practice.exe`.

## Build

```powershell
dotnet publish desktop/launcher/OOPAutoGraderPractice/OOPAutoGraderPractice.csproj `
  -c Release -r win-x64 --self-contained false `
  -o desktop/launcher/publish
```

Copy `OOP-AutoGrader-Practice.exe` (and WebView2 loader DLLs from publish output) into the student practice folder root next to `backend.jar`.

Requires **WebView2 Runtime** (preinstalled on most Windows 10/11) and **Java 17+** (or `runtime/jdk` in the install folder).

## Runtime behavior

- Sets `APP_DESKTOP_HOME` to the executable directory.
- Starts `java -jar backend.jar` with `SPRING_PROFILES_ACTIVE=desktop`.
- Polls `GET /api/desktop/status` on port **18002** (desktop profile; does not use dev/cloud **8002**).
- Opens a single WebView2 window at `http://127.0.0.1:18002/` (no external browser).
- Stops the Java process when the window closes.

See `docs/DESKTOP_STUDENT_DIST.md` and `scripts/assemble-student-desktop.ps1`.
