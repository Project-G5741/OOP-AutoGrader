import React from 'react';
import { Download, Laptop } from 'lucide-react';
import { toFriendlyError } from '../../utils/apiError';
import { getAccessToken } from '../../utils/authRoutes';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

export default function StudentOfflinePracticeDownload({ onToast }) {
  const handleDownload = () => {
    try {
      const token = getAccessToken();
      if (!token) {
        throw new Error('Please sign in again to download.');
      }
      // Hand the transfer to the browser so students see native download progress.
      // Query-token auth is allowed only for this GET (see JwtAuthenticationFilter).
      const url =
        `${API_BASE}/api/students/desktop-practice-bundle` +
        `?access_token=${encodeURIComponent(token)}`;
      const link = document.createElement('a');
      link.href = url;
      link.rel = 'noopener';
      document.body.appendChild(link);
      link.click();
      link.remove();
      onToast?.({
        message: 'Download started in your browser. Check Downloads for progress.',
        type: 'success',
      });
    } catch (err) {
      onToast?.({ message: toFriendlyError(err, 'download'), type: 'error' });
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
          className="inline-flex shrink-0 items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-hover"
        >
          <Download className="h-4 w-4" aria-hidden />
          Download practice folder
        </button>
      </div>
    </div>
  );
}
