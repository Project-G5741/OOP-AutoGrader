import { useRef, useState } from 'react';
import { FolderOpen } from 'lucide-react';
import { readFriendlyApiError, toFriendlyError } from '../../utils/apiError';
import { quitPracticeApp } from '../../utils/desktopQuit';

const API_BASE = import.meta.env.VITE_API_URL
  || (import.meta.env.VITE_APP_MODE === 'desktop' ? 'http://127.0.0.1:18002' : 'http://localhost:18002');

export default function DesktopRubricPackImport({ onToast }) {
  const inputRef = useRef(null);
  const [busy, setBusy] = useState(false);
  const [pendingFile, setPendingFile] = useState(null);
  const [conflicts, setConflicts] = useState(null);
  const [restartRequired, setRestartRequired] = useState(false);

  const upload = async (file, confirmReplace) => {
    const form = new FormData();
    form.append('file', file, file.name);
    const qs = confirmReplace ? '?confirmReplace=true' : '';
    const response = await fetch(`${API_BASE}/api/desktop/packs/import${qs}`, {
      method: 'POST',
      body: form,
    });
    if (response.status === 409) {
      const body = await response.json().catch(() => ({}));
      const list = Array.isArray(body.conflicts) ? body.conflicts : [];
      if (list.length > 0) {
        setConflicts(list);
        setPendingFile(file);
        return null;
      }
      throw new Error(
        (typeof body.message === 'string' && body.message.trim())
          || 'A lab with the same name already exists in offline practice',
      );
    }
    if (!response.ok) {
      throw new Error(await readFriendlyApiError(response, 'import'));
    }
    return response.json();
  };

  const showRestartRequired = () => {
    setRestartRequired(true);
  };

  const handleFile = async (file) => {
    if (!file) return;
    setBusy(true);
    setConflicts(null);
    setPendingFile(null);
    try {
      const result = await upload(file, false);
      if (result) {
        showRestartRequired();
      }
    } catch (err) {
      onToast?.({ message: toFriendlyError(err, 'import'), type: 'error' });
    } finally {
      setBusy(false);
      if (inputRef.current) inputRef.current.value = '';
    }
  };

  const confirmReplace = async () => {
    if (!pendingFile) return;
    setBusy(true);
    try {
      const result = await upload(pendingFile, true);
      if (result) {
        setConflicts(null);
        setPendingFile(null);
        showRestartRequired();
      }
    } catch (err) {
      onToast?.({ message: toFriendlyError(err, 'import'), type: 'error' });
    } finally {
      setBusy(false);
    }
  };

  const cancelConflict = () => {
    setConflicts(null);
    setPendingFile(null);
  };

  return (
    <>
      <input
        ref={inputRef}
        type="file"
        accept=".agpack"
        className="hidden"
        onChange={(e) => handleFile(e.target.files?.[0])}
      />
      <button
        type="button"
        disabled={busy || restartRequired}
        onClick={() => inputRef.current?.click()}
        className="inline-flex items-center gap-2 rounded-full border border-primary/30 bg-primary-light px-3 py-1.5 text-sm font-medium text-primary-text transition hover:bg-primary-light/80 disabled:opacity-50"
        title={busy
          ? 'Importing pack into local practice…'
          : 'Import Rubric_{year}_Q{n}.agpack or Rubric_{name}.agpack — app closes after import'}
      >
        <FolderOpen className={`h-4 w-4 ${busy ? 'animate-pulse' : ''}`} />
        {busy ? 'Importing…' : 'Import rubric pack'}
      </button>

      {conflicts?.length > 0 && !restartRequired && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-[100] flex items-center justify-center bg-black/40 p-4"
        >
          <div className="max-w-md rounded-2xl border border-border bg-surface p-5 shadow-lg">
            <h2 className="text-base font-semibold text-foreground">Replace existing lab?</h2>
            <p className="mt-2 text-sm text-foreground-muted">
              A lab named &quot;{conflicts[0].labName}&quot; is already in your offline sidebar. Importing will
              replace it with the pack you selected. The app will then close so you can reopen it.
            </p>
            <div className="mt-4 flex justify-end gap-2">
              <button
                type="button"
                onClick={cancelConflict}
                className="rounded-lg border border-border px-3 py-1.5 text-sm hover:bg-surface-secondary"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={busy}
                onClick={confirmReplace}
                className="rounded-lg bg-primary px-3 py-1.5 text-sm text-white hover:bg-primary-hover disabled:opacity-50"
              >
                Replace
              </button>
            </div>
          </div>
        </div>
      )}

      {restartRequired && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="desktop-import-restart-title"
          className="fixed inset-0 z-[110] flex items-center justify-center bg-black/50 p-4"
        >
          <div className="max-w-md rounded-2xl border border-border bg-surface p-5 shadow-lg">
            <h2 id="desktop-import-restart-title" className="text-base font-semibold text-foreground">
              Restart required
            </h2>
            <p className="mt-2 text-sm text-foreground-muted">
              Pack imported successfully. The practice app will close now. Open it again manually to use
              the updated labs.
            </p>
            <div className="mt-4 flex justify-end">
              <button
                type="button"
                onClick={quitPracticeApp}
                className="rounded-lg bg-primary px-4 py-1.5 text-sm text-white hover:bg-primary-hover"
              >
                Okay
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
