# Practice WebView2 host (Rust)

Thin `tao` + `wry` supervisor at `practice-host/`. Builds `OOP-AutoGrader-Practice.exe`.

## Build

Requires **Rust stable** (`cargo`) and **MSVC Build Tools** (Windows WebView2 / wry link deps).

```powershell
cargo build --release --manifest-path desktop/launcher/practice-host/Cargo.toml
```

Copy `desktop/launcher/practice-host/target/release/OOP-AutoGrader-Practice.exe` into the student practice folder root next to `backend.jar`.

`scripts/assemble-student-desktop.ps1` runs this release build and copies the EXE (fails if `cargo` is missing).

Requires **WebView2 Runtime** (Evergreen; preinstalled on most Windows 10/11) and **Java 17+** (or `runtime/jdk` in the install folder). Students do **not** need Rust, .NET, or a system Java install when `runtime/jdk` is present.

## Runtime behavior

- Sets `APP_DESKTOP_HOME` to the executable directory.
- Starts `java -jar backend.jar` with `SPRING_PROFILES_ACTIVE=desktop` (no console window / taskbar entry for the Java process).
- Polls `GET /api/desktop/status` on port **18002** until `bootstrapComplete` (2-minute bound).
- Opens a single WebView2 window at `http://127.0.0.1:18002/` (no external browser).
- Stops the Java process tree when the window closes or the UI posts `practice-quit`.

See `docs/DESKTOP_STUDENT_DIST.md` and `scripts/assemble-student-desktop.ps1`.
