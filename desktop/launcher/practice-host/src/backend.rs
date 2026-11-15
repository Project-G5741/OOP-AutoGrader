//! Start/wait/stop the bundled Java practice backend (parity with former .NET BackendLauncher).

use crate::host_config::HostConfig;
use serde::Deserialize;
use std::fs;
use std::io::{self, Write};
use std::path::Path;
use std::process::{Child, Command, Stdio};
use std::thread;
use std::time::{Duration, Instant, SystemTime};

const HEALTH_INTERVAL: Duration = Duration::from_millis(250);
const HEALTH_TIMEOUT: Duration = Duration::from_secs(120);

#[derive(Deserialize)]
struct DesktopStatus {
    #[serde(rename = "bootstrapComplete")]
    bootstrap_complete: bool,
}

pub struct Backend {
    child: Option<Child>,
    port: u16,
    #[cfg(windows)]
    job: Option<JobGuard>,
}

impl Backend {
    pub fn app_url(port: u16) -> String {
        format!("http://127.0.0.1:{port}/")
    }

    pub fn start(install_dir: &Path, config: &HostConfig) -> io::Result<Self> {
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
        let launcher_java = resolve_launcher_java(install_dir);
        let mut cmd = Command::new(&launcher_java);
        cmd.args([
            // Local practice: room for API + parallel OT worker (not Render free-tier).
            "-Xmx512m",
            "-jar",
        ])
        .arg(&backend_jar)
        .current_dir(install_dir)
        .stdin(Stdio::null())
        .stdout(Stdio::null())
        .stderr(Stdio::null())
        .env("APP_DESKTOP_HOME", install_dir)
        .env("SPRING_PROFILES_ACTIVE", &config.spring_profile)
        .env("DESKTOP_SERVER_PORT", config.port.to_string())
        .env("DESKTOP_WORKER_JAVA", &java_exe)
        .env("APP_DESKTOP_CLOUD_API_URL", read_cloud_api_url(install_dir))
        // Never inherit a cloud SANDBOX_* env into offline practice (local worker.jar only).
        .env("SANDBOX_ENABLED", "false")
        .env("APP_GRADING_SANDBOX_ENABLED", "false")
        .env("SANDBOX_RUNNER_URL", "")
        .env("SANDBOX_RUNNER_TOKEN", "");

        let mut child = cmd.spawn().map_err(|e| {
            io::Error::new(
                e.kind(),
                format!("Failed to start the Java backend process ({launcher_java}): {e}"),
            )
        })?;

        #[cfg(windows)]
        let job = match JobGuard::assign(&child) {
            Ok(job) => Some(job),
            Err(err) => {
                let _ = child.kill();
                let _ = child.wait();
                return Err(io::Error::new(
                    err.kind(),
                    format!("Failed to attach the Java backend to a job object: {err}"),
                ));
            }
        };

        Ok(Self {
            child: Some(child),
            port: config.port,
            #[cfg(windows)]
            job,
        })
    }

    pub fn wait_until_healthy(&mut self) -> io::Result<()> {
        let status_url = format!("{}api/desktop/status", Self::app_url(self.port));
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

        #[cfg(windows)]
        {
            if let Some(job) = self.job.take() {
                job.terminate();
            }
        }
        #[cfg(not(windows))]
        {
            let _ = child.kill();
        }
        let _ = child.wait();
    }
}

/// Keeps the Java process and the workers it spawns in one job.
/// Closing the host ends that job, so the grader does not outlive the window.
#[cfg(windows)]
struct JobGuard {
    handle: windows_sys::Win32::Foundation::HANDLE,
}

#[cfg(windows)]
impl JobGuard {
    fn assign(child: &Child) -> io::Result<Self> {
        use std::os::windows::io::AsRawHandle;
        use windows_sys::Win32::Foundation::CloseHandle;
        use windows_sys::Win32::System::JobObjects::{
            AssignProcessToJobObject, CreateJobObjectW, SetInformationJobObject, JOBOBJECT_EXTENDED_LIMIT_INFORMATION,
            JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE, JobObjectExtendedLimitInformation,
        };

        unsafe {
            let handle = CreateJobObjectW(std::ptr::null(), std::ptr::null());
            if handle.is_null() {
                return Err(io::Error::last_os_error());
            }
            let mut info: JOBOBJECT_EXTENDED_LIMIT_INFORMATION = std::mem::zeroed();
            info.BasicLimitInformation.LimitFlags = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE;
            let configured = SetInformationJobObject(
                handle,
                JobObjectExtendedLimitInformation,
                &info as *const _ as *const _,
                std::mem::size_of::<JOBOBJECT_EXTENDED_LIMIT_INFORMATION>() as u32,
            );
            if configured == 0 {
                let err = io::Error::last_os_error();
                CloseHandle(handle);
                return Err(err);
            }
            let assigned = AssignProcessToJobObject(handle, child.as_raw_handle() as _);
            if assigned == 0 {
                let err = io::Error::last_os_error();
                CloseHandle(handle);
                return Err(err);
            }
            Ok(Self { handle })
        }
    }

    fn terminate(&self) {
        use windows_sys::Win32::System::JobObjects::TerminateJobObject;
        unsafe {
            TerminateJobObject(self.handle, 1);
        }
    }
}

#[cfg(windows)]
impl Drop for JobGuard {
    fn drop(&mut self) {
        use windows_sys::Win32::Foundation::CloseHandle;
        unsafe {
            CloseHandle(self.handle);
        }
    }
}

impl Drop for Backend {
    fn drop(&mut self) {
        self.stop();
    }
}

fn read_cloud_api_url(install_dir: &Path) -> String {
    let path = install_dir.join("cloud-api-url.txt");
    fs::read_to_string(path)
        .ok()
        .map(|raw| raw.trim().trim_end_matches('/').to_string())
        .filter(|value| !value.is_empty())
        .unwrap_or_default()
}

fn bundled_bin(install_dir: &Path, name: &str) -> Option<String> {
    let path = install_dir.join("runtime").join("jdk").join("bin").join(name);
    if path.is_file() {
        Some(path.to_string_lossy().into_owned())
    } else {
        None
    }
}

fn resolve_java(install_dir: &Path) -> String {
    bundled_bin(install_dir, "java.exe").unwrap_or_else(|| "java".to_string())
}

/// `javaw` is the no-console Java entry. The worker still uses `java.exe`.
fn resolve_launcher_java(install_dir: &Path) -> String {
    bundled_bin(install_dir, "javaw.exe")
        .or_else(|| bundled_bin(install_dir, "java.exe"))
        .unwrap_or_else(|| "javaw".to_string())
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
