/** Close the WebView2 practice host (same as window X / Logout). No-op outside the EXE. */
const PRACTICE_QUIT = 'practice-quit';

export function quitPracticeApp() {
  // wry reads WebMessageAsString, so post a JSON string (not a bare object).
  const encoded = JSON.stringify({ type: PRACTICE_QUIT });
  for (const bridge of [window.chrome?.webview, window.ipc]) {
    if (bridge && typeof bridge.postMessage === 'function') {
      bridge.postMessage(encoded);
      return;
    }
  }
}
