/** Close the WebView2 practice host (same as window X / Logout). No-op outside the EXE. */
export function quitPracticeApp() {
  const webview = window.chrome?.webview;
  if (webview && typeof webview.postMessage === 'function') {
    webview.postMessage({ type: 'practice-quit' });
  }
}
