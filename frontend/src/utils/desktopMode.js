/** True for student practice or lecturer offline desktop builds (not the web SPA). */
export function isDesktopLocalMode() {
  const mode = import.meta.env.VITE_APP_MODE;
  return mode === 'desktop' || mode === 'desktop-lecturer';
}

/** Backend base URL for the active Vite mode. */
export function desktopApiBase() {
  if (import.meta.env.VITE_API_URL) {
    return import.meta.env.VITE_API_URL;
  }
  if (import.meta.env.VITE_APP_MODE === 'desktop-lecturer') {
    return 'http://127.0.0.1:18003';
  }
  if (import.meta.env.VITE_APP_MODE === 'desktop') {
    return 'http://127.0.0.1:18002';
  }
  return 'http://localhost:8002';
}
