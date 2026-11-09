//! The on-screen pill, VocaMac's recording overlay for Windows.
//!
//! One small always-on-top window (`overlay.html`) shows:
//! - a "ready" pill for a few seconds after launch, since Windows 11 tucks new
//!   tray icons into the overflow and people cannot tell VocaWin started;
//! - listening (live level), transcribing, and a short error or cancel note.
//!
//! The window must never take focus: text is typed into whatever app is in
//! front, and a focused pill would receive it instead. It is created
//! non-focusable (`WS_EX_NOACTIVATE`) and on Windows is shown with
//! `SWP_NOACTIVATE`, so the foreground window keeps focus.
//!
//! It is not made click-through: tao does that with `WS_EX_LAYERED`, which
//! does not mix with a transparent WebView2 window. Instead the window is
//! sized to the pill the page reports, so it covers nothing else, and a click
//! on a finished pill dismisses it.

use serde::Serialize;
use std::sync::atomic::{AtomicBool, AtomicU32, AtomicU64, Ordering};
use std::sync::Mutex;
use tauri::{AppHandle, Emitter, Manager, WebviewUrl, WebviewWindowBuilder};

pub const LABEL: &str = "overlay";
/// Logical height of the transparent window: generous padding around the 40px pill so edges never clip.
const HEIGHT: f64 = 56.0;
/// Stable canvas width so the native window does not resize/jump on every phase transition.
const DEFAULT_WIDTH: u32 = 280;
const MIN_WIDTH: u32 = 280;
const MAX_WIDTH: u32 = 640;
/// Gap from the screen edge (logical pixels).
const MARGIN: f64 = 28.0;

#[derive(Clone, Debug, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct Payload {
    /// `ready`, `listening`, `processing`, `error`, `cancelled`, `notice`, or `hidden`.
    pub phase: String,
    pub title: String,
    pub detail: String,
}

impl Payload {
    fn hidden() -> Self {
        Self {
            phase: "hidden".into(),
            title: String::new(),
            detail: String::new(),
        }
    }
}

pub enum Phase {
    Idle,
    #[allow(dead_code)]
    Ready {
        title: String,
        detail: String,
    },
    /// `hint` is shown beside the level ("Esc to cancel"), or empty.
    Listening {
        hint: String,
    },
    Processing,
    Copied,
    Error(String),
    Cancelled,
    /// A short note, such as "Nothing to paste yet".
    Notice(String),
}

static CURRENT: Mutex<Option<Payload>> = Mutex::new(None);
static GENERATION: AtomicU64 = AtomicU64::new(0);
/// Logical width of the overlay canvas (clamped to >= DEFAULT_WIDTH so transitions never jump).
static WIDTH: AtomicU32 = AtomicU32::new(DEFAULT_WIDTH);
/// Last applied physical `(x, y, width, height)` while visible, so identical frames never trigger Win32 resize/move jitter.
static LAST_FRAME: Mutex<Option<(i32, i32, u32, u32)>> = Mutex::new(None);

/// Flag set when the user explicitly clicks the overlay close button.
/// While true, the idle pill remains hidden, but reappears whenever dictation is in use.
static IDLE_HIDDEN: AtomicBool = AtomicBool::new(false);

/// What the overlay page renders when it loads after an update was sent.
pub fn current() -> Payload {
    CURRENT
        .lock()
        .ok()
        .and_then(|guard| guard.clone())
        .unwrap_or_else(Payload::hidden)
}

pub fn create(app: &AppHandle) -> tauri::Result<()> {
    let builder = WebviewWindowBuilder::new(app, LABEL, WebviewUrl::App("overlay.html".into()))
        .title("")
        .inner_size(DEFAULT_WIDTH as f64, HEIGHT)
        .decorations(false)
        .resizable(false)
        .always_on_top(true)
        .skip_taskbar(true)
        .shadow(false)
        .focused(false)
        .focusable(false)
        .visible(false);
    #[cfg(not(target_os = "macos"))]
    let builder = builder.transparent(true);
    let window = builder.build()?;
    #[cfg(windows)]
    if let Ok(hwnd) = window.hwnd() {
        native::suppress_dwm_box(hwnd.0);
    }
    Ok(())
}

/// The page measured its pill: fit the window to it and re-centre only if it exceeds the stable canvas width.
pub fn set_width(app: &AppHandle, width: f64) {
    let width = (width.round() as u32).clamp(MIN_WIDTH, MAX_WIDTH);
    let prev = WIDTH.swap(width, Ordering::SeqCst);
    if prev != width && current().phase != "hidden" {
        place_and_show(app);
    }
}

/// A click on the pill. Activates recording if idle; stops if listening.
/// Explicitly hide the pill when the user clicks the close button.
/// Keeps it hidden while idle, but it will automatically reappear when dictation is in use.
pub fn hide_pill(app: &AppHandle) {
    IDLE_HIDDEN.store(true, Ordering::SeqCst);
    let _ = crate::tray_stop_voice(app);
    hide(app);
}

pub fn dismiss(app: &AppHandle) {
    let is_recording = app
        .try_state::<crate::AppState>()
        .and_then(|s| s.recording.lock().ok().map(|r| *r))
        .unwrap_or(false);

    if is_recording {
        let _ = crate::tray_stop_voice(app);
        return;
    }

    let current_phase = current().phase;
    match current_phase.as_str() {
        "idle" | "ready" | "hidden" => {
            let _ = crate::tray_start_voice(app);
        }
        "listening" => {
            let _ = crate::tray_stop_voice(app);
        }
        "processing" => {
            // Processing/transcribing in flight: ignore to prevent duplicate requests
        }
        _ => {
            show(app, Phase::Idle, true);
        }
    }
}

/// Show a phase. `enabled` is the user's overlay setting for this phase.
pub fn show(app: &AppHandle, phase: Phase, enabled: bool) {
    if !enabled {
        hide(app);
        return;
    }
    if matches!(phase, Phase::Idle) && IDLE_HIDDEN.load(Ordering::SeqCst) {
        hide(app);
        return;
    }
    let (payload, hide_after_ms) = match phase {
        Phase::Idle => (
            Payload {
                phase: "idle".into(),
                title: String::new(),
                detail: String::new(),
            },
            None,
        ),
        Phase::Ready { title, detail } => (
            Payload {
                phase: "ready".into(),
                title,
                detail,
            },
            Some(2_000),
        ),
        Phase::Listening { hint } => (
            Payload {
                phase: "listening".into(),
                title: "Listening…".into(),
                detail: hint,
            },
            None,
        ),
        Phase::Processing => (
            Payload {
                phase: "processing".into(),
                title: "Transcribing…".into(),
                detail: String::new(),
            },
            None,
        ),
        Phase::Copied => (
            Payload {
                phase: "copied".into(),
                title: "Copied".into(),
                detail: String::new(),
            },
            Some(1_000), // ~800-1,200 ms per specification
        ),
        Phase::Error(message) => {
            let title = if message.is_empty() {
                "Try again".into()
            } else {
                short(&message)
            };
            let delay = if title.len() > 30 { 2_600 } else { 1_800 };
            (
                Payload {
                    phase: "error".into(),
                    title,
                    detail: String::new(),
                },
                Some(delay),
            )
        },
        Phase::Cancelled => (
            Payload {
                phase: "cancelled".into(),
                title: "Cancelled".into(),
                detail: String::new(),
            },
            Some(600),
        ),
        Phase::Notice(message) => (
            Payload {
                phase: "notice".into(),
                title: short(&message),
                detail: String::new(),
            },
            Some(1_600),
        ),
    };
    WIDTH.store(DEFAULT_WIDTH, Ordering::SeqCst);
    let generation = GENERATION.fetch_add(1, Ordering::SeqCst) + 1;
    set_current(payload.clone());
    let _ = app.emit_to(LABEL, "overlay-phase", payload);
    place_and_show(app);
    if let Some(delay) = hide_after_ms {
        let app = app.clone();
        std::thread::spawn(move || {
            std::thread::sleep(std::time::Duration::from_millis(delay));
            if GENERATION.load(Ordering::SeqCst) == generation {
                let overlay_enabled = if let Some(state) = app.try_state::<crate::AppState>() {
                    state
                        .settings
                        .lock()
                        .map(|s| crate::overlay_on(&s))
                        .unwrap_or(true)
                } else {
                    true
                };
                if overlay_enabled {
                    show(&app, Phase::Idle, true);
                } else {
                    hide(&app);
                }
            }
        });
    }
}

/// Live preview text for the listening pill; ignored in any other phase.
pub fn live_text(app: &AppHandle, text: &str) {
    if current().phase != "listening" {
        return;
    }
    let _ = app.emit_to(LABEL, "overlay-live", text.to_string());
}

pub fn hide(app: &AppHandle) {
    GENERATION.fetch_add(1, Ordering::SeqCst);
    if let Ok(mut guard) = LAST_FRAME.lock() {
        *guard = None;
    }
    set_current(Payload::hidden());
    let _ = app.emit_to(LABEL, "overlay-phase", Payload::hidden());
    let Some(window) = app.get_webview_window(LABEL) else {
        return;
    };
    let _ = app.run_on_main_thread(move || {
        #[cfg(windows)]
        if let Ok(hwnd) = window.hwnd() {
            native::hide(hwnd.0);
            return;
        }
        let _ = window.hide();
    });
}

fn set_current(payload: Payload) {
    if let Ok(mut guard) = CURRENT.lock() {
        *guard = Some(payload);
    }
}

/// One line, short enough for the pill.
fn short(message: &str) -> String {
    let line = message.lines().next().unwrap_or("").trim();
    if line.starts_with("Recording is too short") {
        return "Recording is too short. Hold the hotkey and speak for a moment.".into();
    }
    if line.starts_with("No speech was recognized") {
        return "No speech detected. Hold the hotkey and speak.".into();
    }
    if line.chars().count() <= 70 {
        return line.to_string();
    }
    let cut: String = line.chars().take(67).collect();
    format!("{}…", cut.trim_end())
}

static IS_DRAGGING: std::sync::atomic::AtomicBool = std::sync::atomic::AtomicBool::new(false);

pub fn refresh_position(app: &AppHandle) {
    if let Ok(mut guard) = LAST_FRAME.lock() {
        *guard = None;
    }
    place_and_show(app);
}

pub fn start_drag(app: &AppHandle) {
    if IS_DRAGGING.swap(true, Ordering::SeqCst) {
        return;
    }
    let app = app.clone();
    std::thread::spawn(move || {
        #[cfg(windows)]
        run_drag_loop(&app);
        #[cfg(not(windows))]
        IS_DRAGGING.store(false, Ordering::SeqCst);
    });
}

#[cfg(windows)]
fn run_drag_loop(app: &AppHandle) {
    use windows::Win32::Foundation::{HWND, POINT, RECT};
    use windows::Win32::UI::Input::KeyboardAndMouse::GetAsyncKeyState;
    use windows::Win32::UI::WindowsAndMessaging::{
        GetCursorPos, GetWindowRect, SetWindowPos, HWND_TOPMOST, SWP_NOACTIVATE,
    };

    struct DragGuard;
    impl Drop for DragGuard {
        fn drop(&mut self) {
            IS_DRAGGING.store(false, Ordering::SeqCst);
        }
    }
    let _guard = DragGuard;

    let Some(window) = app.get_webview_window(LABEL) else {
        return;
    };
    let Ok(hwnd) = window.hwnd() else {
        return;
    };

    let mut start_cursor = POINT::default();
    let mut start_rect = RECT::default();
    unsafe {
        let _ = GetCursorPos(&mut start_cursor);
        let _ = GetWindowRect(HWND(hwnd.0), &mut start_rect);
    }

    let offset_x = start_cursor.x - start_rect.left;
    let offset_y = start_cursor.y - start_rect.top;
    let width = start_rect.right - start_rect.left;
    let height = start_rect.bottom - start_rect.top;

    let mut last_x = start_rect.left;
    let mut last_y = start_rect.top;

    // VK_LBUTTON is 0x01
    while (unsafe { GetAsyncKeyState(0x01) } as u16 & 0x8000) != 0 {
        let mut cur = POINT::default();
        if unsafe { GetCursorPos(&mut cur) }.is_ok() {
            let target_x = cur.x - offset_x;
            let target_y = cur.y - offset_y;

            if target_x != last_x || target_y != last_y {
                last_x = target_x;
                last_y = target_y;
                unsafe {
                    let _ = SetWindowPos(
                        HWND(hwnd.0),
                        HWND_TOPMOST,
                        target_x,
                        target_y,
                        width,
                        height,
                        SWP_NOACTIVATE,
                    );
                }
            }
        }
        std::thread::sleep(std::time::Duration::from_millis(8));
    }

    let mut final_rect = RECT::default();
    unsafe {
        let _ = GetWindowRect(HWND(hwnd.0), &mut final_rect);
    }
    let final_width = final_rect.right - final_rect.left;
    let final_height = final_rect.bottom - final_rect.top;
    let center_x = final_rect.left + final_width / 2;
    let center_y = final_rect.top + final_height / 2;

    if let Ok(mut guard) = LAST_FRAME.lock() {
        *guard = Some((final_rect.left, final_rect.top, final_width as u32, final_height as u32));
    }

    save_custom_position(app, center_x, center_y);
    let _ = app.emit_to(LABEL, "overlay-drag-ended", ());
}

fn save_custom_position(app: &AppHandle, center_x: i32, center_y: i32) {
    if let Some(state) = app.try_state::<crate::AppState>() {
        let mut updated = None;
        if let Ok(mut guard) = state.settings.lock() {
            guard.overlay_position = "custom".into();
            guard.overlay_custom_x = Some(center_x);
            guard.overlay_custom_y = Some(center_y);
            updated = Some(guard.clone());
        }
        if let Some(settings) = updated {
            let _ = crate::persist_settings(&state.settings_path, &settings);
            let _ = app.emit("settings-changed", &settings);
        }
    }
}

/// Bottom, top, or free custom centre of the work area, in physical pixels.
fn frame(
    app: &AppHandle,
    position: &str,
    custom_x: Option<i32>,
    custom_y: Option<i32>,
) -> Option<(i32, i32, u32, u32)> {
    let width_val = WIDTH.load(Ordering::SeqCst) as f64;

    if position == "custom" {
        if let (Some(cx), Some(cy)) = (custom_x, custom_y) {
            let monitor = app
                .monitor_from_point(cx as f64, cy as f64)
                .ok()
                .flatten()
                .or_else(|| app.primary_monitor().ok().flatten())?;
            let scale = monitor.scale_factor();
            let area = monitor.work_area();
            let width = (width_val * scale).round() as u32;
            let height = (HEIGHT * scale).round() as u32;

            let x = cx - (width as i32) / 2;
            let y = cy - (height as i32) / 2;

            let min_x = area.position.x;
            let max_x = (area.position.x + area.size.width as i32 - width as i32).max(min_x);
            let min_y = area.position.y;
            let max_y = (area.position.y + area.size.height as i32 - height as i32).max(min_y);

            let clamped_x = x.clamp(min_x, max_x);
            let clamped_y = y.clamp(min_y, max_y);
            return Some((clamped_x, clamped_y, width, height));
        }
    }

    let top = position == "top";
    let cursor = app.cursor_position().ok()?;
    let monitor = app
        .monitor_from_point(cursor.x, cursor.y)
        .ok()
        .flatten()
        .or_else(|| app.primary_monitor().ok().flatten())?;
    let scale = monitor.scale_factor();
    let area = monitor.work_area();
    let width = (width_val * scale).round() as u32;
    let height = (HEIGHT * scale).round() as u32;
    let margin = (MARGIN * scale).round() as i32;
    let x = area.position.x + (area.size.width as i32 - width as i32) / 2;
    let y = if top {
        area.position.y + margin
    } else {
        area.position.y + area.size.height as i32 - height as i32 - margin
    };
    Some((x, y, width, height))
}

fn place_and_show(app: &AppHandle) {
    let Some(window) = app.get_webview_window(LABEL) else {
        return;
    };
    let (position, custom_x, custom_y) = app
        .try_state::<crate::AppState>()
        .and_then(|state| {
            state
                .settings
                .lock()
                .ok()
                .map(|settings| {
                    (
                        settings.overlay_position.clone(),
                        settings.overlay_custom_x,
                        settings.overlay_custom_y,
                    )
                })
        })
        .unwrap_or_else(|| ("bottom".into(), None, None));
    let Some((x, y, width, height)) = frame(app, &position, custom_x, custom_y) else {
        return;
    };
    let target = (x, y, width, height);
    let unchanged = if let Ok(mut guard) = LAST_FRAME.lock() {
        if *guard == Some(target) {
            true
        } else {
            *guard = Some(target);
            false
        }
    } else {
        false
    };
    if unchanged {
        return;
    }
    let _ = app.run_on_main_thread(move || {
        #[cfg(windows)]
        if let Ok(hwnd) = window.hwnd() {
            native::show_at(hwnd.0, x, y, width as i32, height as i32);
            return;
        }
        #[cfg(not(windows))]
        {
            let _ = window.set_size(tauri::PhysicalSize::new(width, height));
            let _ = window.set_position(tauri::PhysicalPosition::new(x, y));
            let _ = window.show();
        }
    });
}

#[cfg(windows)]
mod native {
    use windows::Win32::Foundation::HWND;
    use windows::Win32::UI::WindowsAndMessaging::{
        GetWindowLongW, SetWindowLongW, SetWindowPos, ShowWindow, GWL_EXSTYLE, GWL_STYLE,
        HWND_TOPMOST, SWP_FRAMECHANGED, SWP_NOACTIVATE, SWP_NOMOVE, SWP_NOSIZE, SWP_NOZORDER,
        SWP_SHOWWINDOW, SW_HIDE,
    };

    #[link(name = "dwmapi")]
    extern "system" {
        fn DwmSetWindowAttribute(
            hwnd: *mut core::ffi::c_void,
            dwAttribute: u32,
            pvAttribute: *const core::ffi::c_void,
            cbAttribute: u32,
        ) -> i32;
    }

    /// Suppress Windows 11 DWM caption bar, minimize/maximize buttons, rounded-rectangle frame,
    /// border, and shadow so only the circular/pill WebView2 pixels are visible.
    pub fn suppress_dwm_box(raw: *mut core::ffi::c_void) {
        const DWMWA_NCRENDERING_POLICY: u32 = 2;
        const DWMNCRP_DISABLED: u32 = 1;
        const DWMWA_WINDOW_CORNER_PREFERENCE: u32 = 33;
        const DWMWCP_DONOTROUND: u32 = 1;
        const DWMWA_BORDER_COLOR: u32 = 34;
        const DWMWA_COLOR_NONE: u32 = 0xFFFFFFFE;
        const DWMWA_TRANSITIONS_FORCEDISABLED: u32 = 3;
        const TRUE: u32 = 1;

        // Standard Win32 Window Styles to strip
        const WS_POPUP: u32 = 0x8000_0000;
        const WS_CAPTION: u32 = 0x00C0_0000;
        const WS_THICKFRAME: u32 = 0x0004_0000;
        const WS_MINIMIZEBOX: u32 = 0x0002_0000;
        const WS_MAXIMIZEBOX: u32 = 0x0001_0000;
        const WS_SYSMENU: u32 = 0x0008_0000;
        const WS_BORDER: u32 = 0x0080_0000;
        const WS_DLGFRAME: u32 = 0x0040_0000;

        // Extended styles
        const WS_EX_TOOLWINDOW: u32 = 0x0000_0080;
        const WS_EX_NOACTIVATE: u32 = 0x0800_0000;
        const WS_EX_APPWINDOW: u32 = 0x0004_0000;
        const WS_EX_WINDOWEDGE: u32 = 0x0000_0100;
        const WS_EX_CLIENTEDGE: u32 = 0x0000_0200;
        const WS_EX_DLGMODALFRAME: u32 = 0x0000_0001;
        const WS_EX_STATICEDGE: u32 = 0x0002_0000;

        unsafe {
            let hwnd = HWND(raw);

            // 1. Force the window into pure borderless WS_POPUP, stripping caption, system menu,
            // minimize/maximize buttons, and dialog/thick frames.
            let style = GetWindowLongW(hwnd, GWL_STYLE) as u32;
            let stripped_style = (style
                & !(WS_CAPTION
                    | WS_THICKFRAME
                    | WS_MINIMIZEBOX
                    | WS_MAXIMIZEBOX
                    | WS_SYSMENU
                    | WS_BORDER
                    | WS_DLGFRAME))
                | WS_POPUP;
            if stripped_style != style {
                SetWindowLongW(hwnd, GWL_STYLE, stripped_style as i32);
            }

            // 2. Ensure tool window (hidden from taskbar/Alt-Tab) and non-activating.
            let ex_style = GetWindowLongW(hwnd, GWL_EXSTYLE) as u32;
            let stripped_ex = (ex_style
                & !(WS_EX_APPWINDOW
                    | WS_EX_WINDOWEDGE
                    | WS_EX_CLIENTEDGE
                    | WS_EX_DLGMODALFRAME
                    | WS_EX_STATICEDGE))
                | WS_EX_TOOLWINDOW
                | WS_EX_NOACTIVATE;
            if stripped_ex != ex_style {
                SetWindowLongW(hwnd, GWL_EXSTYLE, stripped_ex as i32);
            }

            // 3. Force non-client frame recalculation so Windows removes any cached caption bar.
            let _ = SetWindowPos(
                hwnd,
                HWND_TOPMOST,
                0,
                0,
                0,
                0,
                SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED | SWP_NOACTIVATE,
            );

            // 4. Disable DWM rounding, non-client rendering, border outline, and transitions.
            let _ = DwmSetWindowAttribute(
                raw,
                DWMWA_NCRENDERING_POLICY,
                &DWMNCRP_DISABLED as *const u32 as *const _,
                4,
            );
            let _ = DwmSetWindowAttribute(
                raw,
                DWMWA_WINDOW_CORNER_PREFERENCE,
                &DWMWCP_DONOTROUND as *const u32 as *const _,
                4,
            );
            let _ = DwmSetWindowAttribute(
                raw,
                DWMWA_BORDER_COLOR,
                &DWMWA_COLOR_NONE as *const u32 as *const _,
                4,
            );
            let _ = DwmSetWindowAttribute(
                raw,
                DWMWA_TRANSITIONS_FORCEDISABLED,
                &TRUE as *const u32 as *const _,
                4,
            );
        }
    }

    /// Show without activating, so the app the user is typing into keeps focus.
    pub fn show_at(raw: *mut core::ffi::c_void, x: i32, y: i32, width: i32, height: i32) {
        suppress_dwm_box(raw);
        unsafe {
            let _ = SetWindowPos(
                HWND(raw),
                HWND_TOPMOST,
                x,
                y,
                width,
                height,
                SWP_NOACTIVATE | SWP_SHOWWINDOW,
            );
        }
    }

    pub fn hide(raw: *mut core::ffi::c_void) {
        unsafe {
            let _ = ShowWindow(HWND(raw), SW_HIDE);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn long_errors_are_cut_to_one_short_line() {
        assert_eq!(short("Model failed\nsecond line"), "Model failed");
        assert_eq!(
            short("Recording is too short. Hold the hotkey and speak for a moment."),
            "Recording is too short. Hold the hotkey and speak for a moment."
        );
        let long = "x".repeat(200);
        let cut = short(&long);
        assert_eq!(cut.chars().count(), 68);
        assert!(cut.ends_with('…'));
    }

    #[test]
    fn nothing_is_shown_before_the_first_update() {
        assert_eq!(current().phase, "hidden");
    }
}
