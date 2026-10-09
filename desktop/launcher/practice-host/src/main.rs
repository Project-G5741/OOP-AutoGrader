#![windows_subsystem = "windows"]
//! Thin Rust + WebView2 practice host (replaces self-contained .NET launcher).

mod backend;

use backend::Backend;
use std::env;
use std::error::Error;
use std::io;
use std::path::{Path, PathBuf};
use std::process::{self, Command};
use std::thread;
use std::time::{Duration, Instant};
use tao::event::{Event, WindowEvent};
use tao::event_loop::{ControlFlow, EventLoopBuilder};
use tao::window::WindowBuilder;
use wry::http::Request;
use wry::WebViewBuilder;

#[cfg(windows)]
use std::os::windows::process::CommandExt;

const PRACTICE_QUIT: &str = "practice-quit";
const PRACTICE_RESTART: &str = "practice-restart";
const BACKEND_PORT: u16 = 18002;
const PORT_FREE_TIMEOUT: Duration = Duration::from_secs(15);
const PORT_FREE_POLL: Duration = Duration::from_millis(100);

#[cfg(windows)]
const CREATE_NEW_PROCESS_GROUP: u32 = 0x0000_0200;
#[cfg(windows)]
const DETACHED_PROCESS: u32 = 0x0000_0008;

#[derive(Debug)]
enum UserEvent {
    PracticeQuit,
    PracticeRestart,
}

fn main() {
    if let Err(err) = run() {
        let install_dir = install_dir_from_exe();
        let message = err.to_string();
        backend::append_error_log(&install_dir, &message);
        let hint = if is_missing_backend(err.as_ref()) {
            format!("{message}\n\nTry OOP-AutoGrader-Practice.bat to see Java errors.")
        } else {
            message
        };
        show_fatal(&hint);
        process::exit(1);
    }
}

fn run() -> Result<(), Box<dyn Error>> {
    let install_dir = install_dir_from_exe();

    // Show a real HWND before starting Java. A GUI-subsystem host with no
    // visible window is Efficiency-Mode throttled on Windows 11, which roughly
    // doubles/triples Spring bootstrap time. Console builds avoided that only
    // because the terminal window appeared immediately.
    let event_loop = EventLoopBuilder::<UserEvent>::with_user_event().build();
    let proxy = event_loop.create_proxy();

    let window = WindowBuilder::new()
        .with_title("OOP AutoGrader — Starting…")
        .with_inner_size(tao::dpi::LogicalSize::new(1280.0, 800.0))
        .with_visible(true)
        .build(&event_loop)?;
    force_window_interactive(&window);

    let mut backend = Backend::start(&install_dir)?;
    if let Err(err) = backend.wait_until_healthy() {
        backend.stop();
        return Err(err.into());
    }

    window.set_title("OOP AutoGrader — Practice");

    let ipc_handler = move |req: Request<String>| {
        match host_ipc_type(req.body()) {
            Some(PRACTICE_RESTART) => {
                let _ = proxy.send_event(UserEvent::PracticeRestart);
            }
            Some(PRACTICE_QUIT) => {
                let _ = proxy.send_event(UserEvent::PracticeQuit);
            }
            _ => {}
        }
    };

    let builder = WebViewBuilder::new()
        .with_url(Backend::app_url())
        .with_ipc_handler(ipc_handler)
        .with_devtools(false);

    let webview = match builder.build(&window) {
        Ok(wv) => wv,
        Err(err) => {
            backend.stop();
            return Err(format!(
                "Failed to create the practice WebView (is WebView2 Runtime installed?).\n{err}"
            )
            .into());
        }
    };

    let mut backend = Some(backend);
    let mut webview = Some(webview);
    let install_dir_for_restart = install_dir.clone();

    event_loop.run(move |event, _, control_flow| {
        *control_flow = ControlFlow::Wait;

        match event {
            Event::WindowEvent {
                event: WindowEvent::CloseRequested,
                ..
            }
            | Event::UserEvent(UserEvent::PracticeQuit) => {
                backend.take();
                webview.take();
                *control_flow = ControlFlow::Exit;
            }
            Event::UserEvent(UserEvent::PracticeRestart) => {
                backend.take();
                let _ = wait_until_port_free(BACKEND_PORT, PORT_FREE_TIMEOUT);
                relaunch_self(&install_dir_for_restart);
                webview.take();
                *control_flow = ControlFlow::Exit;
            }
            _ => {}
        }
    });
}

fn host_ipc_type(body: &str) -> Option<&'static str> {
    let trimmed = body.trim();
    if trimmed == PRACTICE_QUIT {
        return Some(PRACTICE_QUIT);
    }
    if trimmed == PRACTICE_RESTART {
        return Some(PRACTICE_RESTART);
    }
    if let Ok(value) = serde_json::from_str::<serde_json::Value>(trimmed) {
        return match value.get("type").and_then(|v| v.as_str()) {
            Some(PRACTICE_QUIT) => Some(PRACTICE_QUIT),
            Some(PRACTICE_RESTART) => Some(PRACTICE_RESTART),
            _ => None,
        };
    }
    None
}

fn wait_until_port_free(port: u16, timeout: Duration) -> bool {
    let deadline = Instant::now() + timeout;
    while Instant::now() < deadline {
        if std::net::TcpListener::bind(("127.0.0.1", port)).is_ok() {
            return true;
        }
        thread::sleep(PORT_FREE_POLL);
    }
    false
}

fn relaunch_self(install_dir: &Path) {
    let Ok(exe) = env::current_exe() else {
        return;
    };
    let mut cmd = Command::new(&exe);
    cmd.current_dir(install_dir)
        .stdin(std::process::Stdio::null())
        .stdout(std::process::Stdio::null())
        .stderr(std::process::Stdio::null());

    #[cfg(windows)]
    {
        cmd.creation_flags(DETACHED_PROCESS | CREATE_NEW_PROCESS_GROUP);
    }

    let _ = cmd.spawn();
}

fn is_missing_backend(err: &(dyn Error + 'static)) -> bool {
    err.downcast_ref::<io::Error>()
        .is_some_and(|e| e.kind() == io::ErrorKind::NotFound)
}

fn install_dir_from_exe() -> PathBuf {
    env::current_exe()
        .ok()
        .and_then(|p| p.parent().map(|d| d.to_path_buf()))
        .unwrap_or_else(|| env::current_dir().unwrap_or_else(|_| PathBuf::from(".")))
}

/// Make the HWND visible/foreground before the blocking Java health wait so the
/// process tree is not treated as a background workload.
#[cfg(windows)]
fn force_window_interactive(window: &tao::window::Window) {
    use tao::platform::windows::WindowExtWindows;

    #[link(name = "user32")]
    extern "system" {
        fn ShowWindow(hwnd: *mut std::ffi::c_void, cmd: i32) -> i32;
        fn UpdateWindow(hwnd: *mut std::ffi::c_void) -> i32;
        fn SetForegroundWindow(hwnd: *mut std::ffi::c_void) -> i32;
    }

    const SW_SHOW: i32 = 5;
    let hwnd = window.hwnd() as *mut std::ffi::c_void;
    if hwnd.is_null() {
        return;
    }
    unsafe {
        ShowWindow(hwnd, SW_SHOW);
        UpdateWindow(hwnd);
        SetForegroundWindow(hwnd);
    }
}

#[cfg(not(windows))]
fn force_window_interactive(_window: &tao::window::Window) {}

#[cfg(windows)]
fn show_fatal(message: &str) {
    use std::ffi::OsStr;
    use std::os::windows::ffi::OsStrExt;
    use std::ptr;

    fn wide(s: &str) -> Vec<u16> {
        OsStr::new(s).encode_wide().chain(Some(0)).collect()
    }

    #[link(name = "user32")]
    extern "system" {
        fn MessageBoxW(
            hwnd: *mut std::ffi::c_void,
            text: *const u16,
            caption: *const u16,
            flags: u32,
        ) -> i32;
    }

    const MB_OK: u32 = 0x0000_0000;
    const MB_ICONERROR: u32 = 0x0000_0010;
    let text = wide(message);
    let caption = wide("OOP AutoGrader Practice");
    unsafe {
        MessageBoxW(
            ptr::null_mut(),
            text.as_ptr(),
            caption.as_ptr(),
            MB_OK | MB_ICONERROR,
        );
    }
}

#[cfg(not(windows))]
fn show_fatal(message: &str) {
    eprintln!("OOP AutoGrader Practice: {message}");
}
