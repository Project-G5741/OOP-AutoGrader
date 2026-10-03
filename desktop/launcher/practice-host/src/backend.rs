//! Start/wait/stop the bundled Java practice backend (parity with former .NET BackendLauncher).

use serde::Deserialize;
use std::fs;
use std::io::{self, Write};
use std::path::Path;
use std::process::{Child, Command, Stdio};
use std::thread;
use std::time::{Duration, Instant, SystemTime};

#[cfg(windows)]
use std::os::windows::process::CommandExt;

const PORT: u16 = 18002;
const CREATE_NO_WINDOW: u32 = 0x0800_0000;
const HEALTH_INTERVAL: Duration = Duration::from_millis(250);
const HEALTH_TIMEOUT: Duration = Duration::from_secs(120);

#[derive(Deserialize)]
struct DesktopStatus {
    #[serde(rename = "bootstrapComplete")]
    bootstrap_complete: bool,
}

pub struct Backend {
    child: Option<Child>,
}

impl Backend {
    pub fn app_url() -> String {
        format!("http://127.0.0.1:{PORT}/")
    }

    pub fn start(install_dir: &Path) -> io::Result<Self> {
        let backend_jar = install_dir.join("backend.jar");
        if !backend_jar.is_file() {
            return Err(io::Error::new(
                io::ErrorKind::NotFound,
                format!(
                    "backend.jar was not found next to the launcher.\n{}",
                    backend_jar.display()
                ),
            ));
        }

        let java_exe = resolve_java(install_dir);
        let mut cmd = Command::new(&java_exe);
        cmd.args([
            // Prefer faster cold start for practice open; peak throughput is not the goal.
            "-XX:TieredStopAtLevel=1",
            "-jar",
        ])
        .arg(&backend_jar)
        .current_dir(install_dir)
        .stdin(Stdio::null())
        .stdout(Stdio::null())
        .stderr(Stdio::null())
        .env("APP_DESKTOP_HOME", install_dir)
        .env("SPRING_PROFILES_ACTIVE", "desktop")
        .env("DESKTOP_WORKER_JAVA", &java_exe);

        #[cfg(windows)]
        {
            cmd.creation_flags(CREATE_NO_WINDOW);
        }

        let child = cmd.spawn().map_err(|e| {
            io::Error::new(
                e.kind(),
                format!("Failed to start the Java backend process ({java_exe}): {e}"),
            )
        })?;

        Ok(Self {
            child: Some(child),
        })
    }

    pub fn wait_until_healthy(&mut self) -> io::Result<()> {
        let status_url = format!("{}api/desktop/status", Self::app_url());
        let deadline = Instant::now() + HEALTH_TIMEOUT;
        let agent = ureq::AgentBuilder::new()
            .timeout_connect(Duration::from_secs(2))
            .timeout_read(Duration::from_secs(2))
            .build();

        while Instant::now() < deadline {
            if let Some(child) = self.child.as_mut() {
                if let Some(status) = child.try_wait()? {
                    return Err(io::Error::new(
                        io::ErrorKind::BrokenPipe,
                        format!("The practice backend exited early (status {status})."),
                    ));
                }
            }

            if let Ok(response) = agent.get(&status_url).call() {
                if let Ok(status) = response.into_json::<DesktopStatus>() {
                    if status.bootstrap_complete {
                        return Ok(());
                    }
                }
            }

            thread::sleep(HEALTH_INTERVAL);
        }

        Err(io::Error::new(
            io::ErrorKind::TimedOut,
            "The practice backend did not become ready in time.",
        ))
    }

    pub fn stop(&mut self) {
        let Some(mut child) = self.child.take() else {
            return;
        };

        if child.try_wait().ok().flatten().is_some() {
            return;
        }

        let pid = child.id();
        // Windowless java has no main window — kill the process tree promptly.
        #[cfg(windows)]
        {
            let _ = Command::new("taskkill")
                .args(["/PID", &pid.to_string(), "/T", "/F"])
                .creation_flags(CREATE_NO_WINDOW)
                .stdin(Stdio::null())
                .stdout(Stdio::null())
                .stderr(Stdio::null())
                .status();
        }
        #[cfg(not(windows))]
        {
            let _ = child.kill();
        }
        let _ = child.wait();
    }
}

impl Drop for Backend {
    fn drop(&mut self) {
        self.stop();
    }
}

fn resolve_java(install_dir: &Path) -> String {
    let bundled = install_dir
        .join("runtime")
        .join("jdk")
        .join("bin")
        .join("java.exe");
    if bundled.is_file() {
        bundled.to_string_lossy().into_owned()
    } else {
        "java".to_string()
    }
}

pub fn append_error_log(install_dir: &Path, message: &str) {
    let path = install_dir.join("launcher-error.log");
    let line = format!("[{}] {}\n", unix_secs_stamp(), message);
    let _ = fs::OpenOptions::new()
        .create(true)
        .append(true)
        .open(path)
        .and_then(|mut f| f.write_all(line.as_bytes()));
}

fn unix_secs_stamp() -> String {
    match SystemTime::now().duration_since(SystemTime::UNIX_EPOCH) {
        Ok(d) => format!("{}s-unix", d.as_secs()),
        Err(_) => "unknown-time".to_string(),
    }
}
