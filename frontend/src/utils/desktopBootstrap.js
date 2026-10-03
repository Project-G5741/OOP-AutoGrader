const API_BASE = import.meta.env.VITE_API_URL
  || (import.meta.env.VITE_APP_MODE === 'desktop' ? 'http://127.0.0.1:18002' : 'http://localhost:8002');

/**
 * Desktop pack bootstrap runs after Tomcat accepts traffic. Wait until status reports
 * bootstrapComplete before reading labs / showing the offline banner settled state.
 */
export async function waitForDesktopBootstrap({ signal, timeoutMs = 120_000 } = {}) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (signal?.aborted) {
      throw new DOMException('Aborted', 'AbortError');
    }
    const response = await fetch(`${API_BASE}/api/desktop/status`, { signal });
    if (!response.ok) {
      throw new Error('Could not read desktop status.');
    }
    const data = await response.json();
    if (data.bootstrapComplete === true) {
      return data;
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  throw new Error('Practice backend did not finish loading packs in time.');
}
