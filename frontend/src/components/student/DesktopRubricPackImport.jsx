import { useRef, useState } from 'react';
import { FolderOpen } from 'lucide-react';
import { toFriendlyError } from '../../utils/apiError';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:18002';

export default function DesktopRubricPackImport({ onToast }) {
  const inputRef = useRef(null);
  const [busy, setBusy] = useState(false);
  const [pendingFile, setPendingFile] = useState(null);
  const [conflicts, setConflicts] = useState(null);

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
      setConflicts(body.conflicts || []);
      setPendingFile(file);
      return null;
    }
    if (!response.ok) {
      let message = '';
      try {
        const body = await response.json();
        message = body.message || body.error || body.detail || '';
      } catch {
        message = await response.text().catch(() => '');
      }
      throw new Error(message || 'Import failed');
    }
    return response.json();
  };

  const handleFile = async (file) => {
    if (!file) return;
    setBusy(true);
    setConflicts(null);
    setPendingFile(null);
    try {
      const result = await upload(file, false);
      if (result) {
        onToast?.({ message: result.message || 'Pack saved. Restart the app to apply.', type: 'success' });
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
        onToast?.({ message: result.message || 'Pack saved. Restart the app to apply.', type: 'success' });
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
        disabled={busy}
        onClick={() => inputRef.current?.click()}
        className="inline-flex items-center gap-2 rounded-full border border-primary/30 bg-primary-light px-3 py-1.5 text-sm font-medium text-primary-text transition hover:bg-primary-light/80 disabled:opacity-50"
        title="Import Rubric_{year}_Q{n}.agpack or Rubric_{name}.agpack — restart required"
      >
        <FolderOpen className="h-4 w-4" />
        Import rubric pack
      </button>

      {conflicts?.length > 0 && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-[100] flex items-center justify-center bg-black/40 p-4"
        >
          <div className="max-w-md rounded-2xl border border-border bg-surface p-5 shadow-lg">
            <h2 className="text-base font-semibold text-foreground">Replace existing lab?</h2>
            <p className="mt-2 text-sm text-foreground-muted">
              A lab named &quot;{conflicts[0].labName}&quot; is already in your offline sidebar. Importing will
              replace it with the pack you selected.
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
                Replace and restart later
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
