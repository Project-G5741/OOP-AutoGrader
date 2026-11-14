//! Optional install-side launcher config (`desktop-host.json`).
//! Student practice ships without the file and keeps port 18002 / profile `desktop`.

use serde::Deserialize;
use std::fs;
use std::path::Path;

const DEFAULT_PORT: u16 = 18002;
const DEFAULT_PROFILE: &str = "desktop";
const DEFAULT_WINDOW_TITLE: &str = "OOP AutoGrader — Practice";
const DEFAULT_STARTING_TITLE: &str = "OOP AutoGrader — Starting…";
const DEFAULT_FATAL_CAPTION: &str = "OOP AutoGrader Practice";

#[derive(Debug, Clone)]
pub struct HostConfig {
    pub port: u16,
    pub spring_profile: String,
    pub window_title: String,
    pub starting_title: String,
    pub fatal_caption: String,
}

impl Default for HostConfig {
    fn default() -> Self {
        Self {
            port: DEFAULT_PORT,
            spring_profile: DEFAULT_PROFILE.to_string(),
            window_title: DEFAULT_WINDOW_TITLE.to_string(),
            starting_title: DEFAULT_STARTING_TITLE.to_string(),
            fatal_caption: DEFAULT_FATAL_CAPTION.to_string(),
        }
    }
}

#[derive(Debug, Deserialize)]
struct HostConfigFile {
    #[serde(default)]
    port: Option<u16>,
    #[serde(rename = "springProfile", default)]
    spring_profile: Option<String>,
    #[serde(rename = "windowTitle", default)]
    window_title: Option<String>,
    #[serde(rename = "startingTitle", default)]
    starting_title: Option<String>,
    #[serde(rename = "fatalCaption", default)]
    fatal_caption: Option<String>,
}

pub fn load(install_dir: &Path) -> HostConfig {
    let path = install_dir.join("desktop-host.json");
    let Ok(raw) = fs::read_to_string(&path) else {
        return HostConfig::default();
    };
    let Ok(parsed) = serde_json::from_str::<HostConfigFile>(&raw) else {
        return HostConfig::default();
    };
    let mut cfg = HostConfig::default();
    if let Some(port) = parsed.port.filter(|p| *p > 0) {
        cfg.port = port;
    }
    if let Some(profile) = parsed
        .spring_profile
        .map(|s| s.trim().to_string())
        .filter(|s| !s.is_empty())
    {
        cfg.spring_profile = profile;
    }
    if let Some(title) = parsed
        .window_title
        .map(|s| s.trim().to_string())
        .filter(|s| !s.is_empty())
    {
        cfg.window_title = title;
    }
    if let Some(title) = parsed
        .starting_title
        .map(|s| s.trim().to_string())
        .filter(|s| !s.is_empty())
    {
        cfg.starting_title = title;
    }
    if let Some(caption) = parsed
        .fatal_caption
        .map(|s| s.trim().to_string())
        .filter(|s| !s.is_empty())
    {
        cfg.fatal_caption = caption;
    }
    cfg
}
