import { useState } from 'react';
import { PawPrint } from 'lucide-react';
import { readFriendlyApiError, toFriendlyError } from '../../utils/apiError';

const API_BASE = import.meta.env.VITE_API_URL
  || (import.meta.env.VITE_APP_MODE === 'desktop' ? 'http://127.0.0.1:18002' : 'http://localhost:18002');

export default function DesktopMascotImport({ onToast, onSynced }) {
  const [open, setOpen] = useState(false);
  const [token, setToken] = useState('');
  const [busy, setBusy] = useState(false);

  const sync = async () => {
    const trimmed = token.trim();
    if (!trimmed) {
      onToast?.({ message: 'Paste the mascot code from the web avatar menu.', type: 'error' });
      return;
    }
    setBusy(true);
    try {
      const response = await fetch(`${API_BASE}/api/desktop/mascot/sync`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: trimmed }),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'import'));
      }
      const body = await response.json();
      onSynced?.(typeof body.equipped === 'string' ? body.equipped : 'fox');
      onToast?.({ message: 'Mascot imported from the web.', type: 'success' });
      setToken('');
      setOpen(false);
    } catch (err) {
      onToast?.({ message: toFriendlyError(err, 'import'), type: 'error' });
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <button
        type="button"
        disabled={busy}
        onClick={() => setOpen(true)}
        className="inline-flex items-center gap-2 rounded-full border border-primary/30 bg-primary-light px-3 py-1.5 text-sm font-medium text-primary-text transition hover:bg-primary-light/80 disabled:opacity-50"
        title="Paste the web mascot code to show your equipped mascot here"
      >
        <PawPrint className={`h-4 w-4 ${busy ? 'animate-pulse' : ''}`} />
        Import mascot
      </button>
      {open && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-[100] flex items-center justify-center bg-black/40 p-4"
        >
          <div className="w-full max-w-md rounded-2xl border border-border bg-surface p-5 shadow-lg">
            <h2 className="text-base font-semibold text-foreground">Import mascot</h2>
            <p className="mt-2 text-sm text-foreground-muted">
              On the website, open the avatar menu, copy the mascot code, then paste it here.
              Scores stay on this PC.
            </p>
            <textarea
              value={token}
              onChange={(event) => setToken(event.target.value)}
              rows={4}
              className="mt-3 w-full rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm text-foreground"
              placeholder="Paste mascot code"
              autoComplete="off"
              spellCheck={false}
            />
            <div className="mt-4 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => {
                  if (busy) return;
                  setOpen(false);
                }}
                className="rounded-lg border border-border px-3 py-1.5 text-sm hover:bg-surface-secondary"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={busy}
                onClick={sync}
                className="rounded-lg bg-primary px-3 py-1.5 text-sm font-medium text-white disabled:opacity-50"
              >
                {busy ? 'Importing…' : 'Import'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
