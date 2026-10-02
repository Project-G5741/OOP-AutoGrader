import { useEffect, useState } from 'react';
import { AlertCircle } from 'lucide-react';
import { waitForDesktopBootstrap } from '../../utils/desktopBootstrap';

export default function DesktopPracticeStatusBanner() {
  const [state, setState] = useState({ loading: true, ready: false, error: '', packMissing: false });

  useEffect(() => {
    const controller = new AbortController();
    (async () => {
      try {
        const data = await waitForDesktopBootstrap({ signal: controller.signal });
        if (controller.signal.aborted) return;
        setState({
          loading: false,
          ready: Boolean(data.ready),
          error: data.error || '',
          packMissing: Boolean(data.packMissing),
        });
      } catch (err) {
        if (controller.signal.aborted || err?.name === 'AbortError') return;
        setState({
          loading: false,
          ready: false,
          error: err?.message || 'Practice backend is not responding.',
          packMissing: false,
        });
      }
    })();
    return () => controller.abort();
  }, []);

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
