import { useEffect, useRef, useState } from 'react';
import { Upload } from 'lucide-react';
import { readFriendlyApiError, toFriendlyError } from '../../utils/apiError';
import { desktopApiBase } from '../../utils/desktopMode';
import { isSpreadsheetFile, parseOfflineRosterFile } from '../../utils/offlineRosterImport';

const API_BASE = desktopApiBase();

export default function DesktopRosterImport({ onToast, onRosterChange }) {
  const inputRef = useRef(null);
  const [busy, setBusy] = useState(false);
  const [ready, setReady] = useState(false);
  const [statusLoaded, setStatusLoaded] = useState(false);

  const refreshStatus = async () => {
    const response = await fetch(`${API_BASE}/api/desktop/roster/status`);
    if (!response.ok) {
      throw new Error(await readFriendlyApiError(response, 'import'));
    }
    const body = await response.json();
    const nextReady = Boolean(body.ready);
    setReady(nextReady);
    setStatusLoaded(true);
    onRosterChange?.(nextReady);
    return nextReady;
  };

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        await refreshStatus();
      } catch (err) {
        if (!cancelled) {
          setStatusLoaded(true);
          setReady(false);
          onToast?.({ message: toFriendlyError(err, 'import'), type: 'error' });
        }
      }
    })();
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- mount-only status load
  }, []);

  const handleFile = async (file) => {
    if (!file) return;
    if (!isSpreadsheetFile(file)) {
      onToast?.({
        message: 'Choose a CSV or Excel file with a student_code (IRN) column.',
        type: 'error',
      });
      return;
    }
    setBusy(true);
    try {
      let irns;
      try {
        irns = await parseOfflineRosterFile(file);
      } catch (parseErr) {
        onToast?.({
          message: toFriendlyError(parseErr, 'import'),
          type: 'error',
        });
        return;
      }
      const response = await fetch(`${API_BASE}/api/desktop/roster/import`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ studentCodes: irns }),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'import'));
      }
      const body = await response.json();
      const nextReady = Boolean(body.ready);
      setReady(nextReady);
      onRosterChange?.(nextReady);
      if (nextReady) {
        onToast?.({
          message: `Imported ${body.importedCount} enrolled IRN${body.importedCount === 1 ? '' : 's'}.`,
          type: 'success',
        });
      } else {
        onToast?.({
          message: 'Roster cleared. Bulk Grading stays blocked until you import enrolled IRNs.',
          type: 'warning',
        });
      }
    } catch (err) {
      onToast?.({ message: toFriendlyError(err, 'import'), type: 'error' });
    } finally {
      setBusy(false);
      if (inputRef.current) inputRef.current.value = '';
    }
  };

  return (
    <div className="flex flex-wrap items-center gap-3">
      <input
        ref={inputRef}
        type="file"
        accept=".csv,.xlsx,.xls,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        className="hidden"
        onChange={(e) => handleFile(e.target.files?.[0])}
      />
      <button
        type="button"
        disabled={busy}
        onClick={() => inputRef.current?.click()}
        className="inline-flex items-center gap-2 rounded-md border border-border bg-surface px-3 py-1.5 text-sm font-medium text-text hover:bg-surface-secondary disabled:opacity-50"
      >
        <Upload className="h-4 w-4" aria-hidden />
        {busy ? 'Importing…' : 'Import roster'}
      </button>
      {statusLoaded && (
        <p className={`text-sm ${ready ? 'text-success' : 'text-warning-text'}`}>
          {ready
            ? 'Current-term roster ready for Bulk Grading.'
            : 'No usable roster — Bulk Grading stays blocked.'}
        </p>
      )}
    </div>
  );
}
