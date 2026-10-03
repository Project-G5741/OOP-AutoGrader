import React, { useState } from 'react';
import { Download, Laptop } from 'lucide-react';
import { authHeaders } from '../../utils/authHeaders';
import { readFriendlyApiError, toFriendlyError } from '../../utils/apiError';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

export default function StudentOfflinePracticeDownload({ onToast }) {
  const [downloading, setDownloading] = useState(false);

  const handleDownload = async () => {
    if (downloading) return;
    setDownloading(true);
    try {
      const response = await fetch(`${API_BASE}/api/students/desktop-practice-bundle`, {
        headers: authHeaders(),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'download'));
      }
      const disposition = response.headers.get('Content-Disposition') || '';
      const match = disposition.match(/filename="([^"]+)"/);
      const filename = match?.[1] || 'OOP-AutoGrader-Practice.zip';
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      link.click();
      URL.revokeObjectURL(url);
      onToast?.({ message: 'Offline practice folder downloaded. Unzip and run OOP-AutoGrader-Practice.exe.', type: 'success' });
    } catch (err) {
      onToast?.({ message: toFriendlyError(err, 'download'), type: 'error' });
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="mb-4 rounded-lg border border-border bg-surface-elevated p-4 shadow-sm">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary">
            <Laptop className="h-5 w-5" aria-hidden />
          </div>
          <div>
            <h2 className="text-sm font-semibold text-foreground">Offline practice on your PC</h2>
            <p className="mt-1 max-w-2xl text-sm text-foreground-muted">
              Download a Windows folder with the local grader, and OOP-AutoGrader-Practice.exe to start.
              Practice scores stay on your machine and are not submitted here.
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={handleDownload}
          disabled={downloading}
          className="inline-flex shrink-0 items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-hover disabled:opacity-60"
        >
          <Download className="h-4 w-4" aria-hidden />
          {downloading ? 'Downloading…' : 'Download practice folder'}
        </button>
      </div>
    </div>
  );
}
