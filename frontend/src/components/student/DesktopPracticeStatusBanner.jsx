import { useCallback, useEffect, useState } from 'react';
import { AlertCircle } from 'lucide-react';

const API_BASE = import.meta.env.VITE_API_URL
  || (import.meta.env.VITE_APP_MODE === 'desktop' ? 'http://127.0.0.1:18002' : 'http://localhost:8002');

async function fetchDesktopStatus() {
  const response = await fetch(`${API_BASE}/api/desktop/status`);
  if (!response.ok) {
    throw new Error('Could not read desktop status.');
  }
  return response.json();
}

export default function DesktopPracticeStatusBanner() {
  const [state, setState] = useState({ loading: true, ready: false, error: '', packMissing: false });

  const refresh = useCallback(async () => {
    try {
      const data = await fetchDesktopStatus();
      setState({
        loading: false,
        ready: Boolean(data.ready),
        error: data.error || '',
        packMissing: Boolean(data.packMissing),
      });
    } catch {
      setState({
        loading: false,
        ready: false,
        error: 'Practice backend is not responding.',
        packMissing: false,
      });
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      await refresh();
      if (cancelled) return;
    })();
    const interval = window.setInterval(() => {
      refresh();
    }, 3000);
    const onFocus = () => refresh();
    window.addEventListener('focus', onFocus);
    return () => {
      cancelled = true;
      window.clearInterval(interval);
      window.removeEventListener('focus', onFocus);
    };
  }, [refresh]);

  if (state.loading || state.ready) {
    return null;
  }

  const message = state.error
    || (state.packMissing
      ? 'Import a quarter or lab pack (Rubric_{year}_Q{n}.agpack or Rubric_{name}.agpack) using the header button, then restart the app.'
      : 'Practice rubric could not be loaded. Import a valid pack and restart the app.');

  return (
    <div className="mb-4 flex gap-3 rounded-md border border-error/40 bg-error-bg p-4 text-sm text-error-text">
      <AlertCircle className="h-5 w-5 shrink-0" aria-hidden />
      <div>
        <p className="font-semibold">Offline practice is not ready</p>
        <p className="mt-1">{message}</p>
      </div>
    </div>
  );
}
