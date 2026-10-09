/** Host IPC for the WebView2 practice EXE. No-op outside the EXE. */

const PRACTICE_QUIT = 'practice-quit';
const PRACTICE_RESTART = 'practice-restart';

function postHostMessage(type) {
  // wry reads WebMessageAsString, so post a JSON string (not a bare object).
  const encoded = JSON.stringify({ type });
  for (const bridge of [window.chrome?.webview, window.ipc]) {
    if (bridge && typeof bridge.postMessage === 'function') {
      bridge.postMessage(encoded);
      return;
    }
  }
}

/** Close the practice host (same as window X / Logout). */
export function quitPracticeApp() {
  postHostMessage(PRACTICE_QUIT);
}

/** Close the practice host and relaunch the EXE (post-import settle). */
export function restartPracticeApp() {
  postHostMessage(PRACTICE_RESTART);
}
