//! Thin Rust + WebView2 practice host (replaces self-contained .NET launcher).

mod backend;

use backend::Backend;
use std::env;
use std::error::Error;
use std::io;
use std::path::PathBuf;
use std::process;
use tao::event::{Event, WindowEvent};
use tao::event_loop::{ControlFlow, EventLoopBuilder};
use tao::window::WindowBuilder;
use wry::http::Request;
use wry::WebViewBuilder;

const PRACTICE_QUIT: &str = "practice-quit";

#[derive(Debug)]
enum UserEvent {
    PracticeQuit,
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
    let mut backend = Backend::start(&install_dir)?;
    backend.wait_until_healthy()?;

    let event_loop = EventLoopBuilder::<UserEvent>::with_user_event().build();
    let proxy = event_loop.create_proxy();

    let window = WindowBuilder::new()
        .with_title("OOP AutoGrader — Practice")
        .with_inner_size(tao::dpi::LogicalSize::new(1280.0, 800.0))
        .build(&event_loop)?;

    let ipc_handler = move |req: Request<String>| {
        if is_practice_quit(req.body()) {
            let _ = proxy.send_event(UserEvent::PracticeQuit);
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
            _ => {}
        }
    });
}

fn is_practice_quit(body: &str) -> bool {
    let trimmed = body.trim();
    if trimmed == PRACTICE_QUIT {
        return true;
    }
    if let Ok(value) = serde_json::from_str::<serde_json::Value>(trimmed) {
        return value.get("type").and_then(|v| v.as_str()) == Some(PRACTICE_QUIT);
    }
    false
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
