import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Check, Download } from 'lucide-react';
import { readFriendlyApiError, toFriendlyError } from '../../utils/apiError';
import { apiFetch } from '../../utils/apiFetch';
import { authHeaders } from '../../utils/authHeaders';
import { getAccessToken } from '../../utils/authRoutes';
import './StudentOfflinePracticeDownload.css';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

const DONE_VISIBLE_MS = 1800;

function parseDownloadFilename(contentDisposition) {
  const match = (contentDisposition || '').match(/filename="([^"]+)"/);
  return match?.[1] || 'OOP-AutoGrader-Practice.zip';
}

function saveBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.rel = 'noopener';
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

/** Nav control: Uiverse-style download pill (theme tokens via CSS variables). */
export default function StudentOfflinePracticeDownload({ onToast, className = '' }) {
  const [downloading, setDownloading] = useState(false);
  const [complete, setComplete] = useState(false);
  const inputRef = useRef(null);
  const resetTimerRef = useRef(null);
  const inFlightRef = useRef(false);

  const clearResetTimer = useCallback(() => {
    if (resetTimerRef.current) {
      window.clearTimeout(resetTimerRef.current);
      resetTimerRef.current = null;
    }
  }, []);

  const resetControl = useCallback(() => {
    setDownloading(false);
    setComplete(false);
    if (inputRef.current) {
      inputRef.current.checked = false;
    }
  }, []);

  useEffect(() => () => clearResetTimer(), [clearResetTimer]);

  const runDownload = useCallback(async () => {
    const token = getAccessToken();
    if (!token) {
      throw new Error('Please sign in again to download.');
    }
    const response = await apiFetch(`${API_BASE}/api/students/desktop-practice-bundle`, {
      headers: authHeaders(),
    });
    if (!response.ok) {
      throw new Error(await readFriendlyApiError(response, 'download'));
    }
    const filename = parseDownloadFilename(response.headers.get('Content-Disposition'));
    const blob = await response.blob();
    saveBlob(blob, filename);
  }, []);

  const scheduleReset = useCallback(() => {
    clearResetTimer();
    resetTimerRef.current = window.setTimeout(() => {
      resetControl();
    }, DONE_VISIBLE_MS);
  }, [clearResetTimer, resetControl]);

  const handleLabelClick = async (event) => {
    if (downloading || inFlightRef.current) {
      event.preventDefault();
      return;
    }
    event.preventDefault();

    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    inFlightRef.current = true;
    setDownloading(true);
    if (!reducedMotion && inputRef.current) {
      inputRef.current.checked = true;
    }

    try {
      await runDownload();
      onToast?.({ message: 'Practice folder downloaded.', type: 'success' });
      if (reducedMotion) {
        resetControl();
        return;
      }
      setComplete(true);
      scheduleReset();
    } catch (err) {
      resetControl();
      onToast?.({ message: toFriendlyError(err, 'download'), type: 'error' });
    } finally {
      inFlightRef.current = false;
    }
  };

  const handleKeyDown = (event) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      handleLabelClick(event);
    }
  };

  return (
    <div className={`practice-folder-download ${className}`.trim()}>
      <label
        className={[
          'practice-folder-download__label',
          downloading ? 'practice-folder-download__label--busy' : '',
          complete ? 'practice-folder-download__label--complete' : '',
        ]
          .filter(Boolean)
          .join(' ')}
        onClick={handleLabelClick}
        onKeyDown={handleKeyDown}
        tabIndex={downloading ? -1 : 0}
        role="button"
        aria-busy={downloading && !complete}
        aria-label={
          complete
            ? 'Download complete'
            : downloading
              ? 'Download in progress'
              : 'Download practice folder'
        }
      >
        <input
          ref={inputRef}
          type="checkbox"
          className="practice-folder-download__input"
          tabIndex={-1}
          aria-hidden
          readOnly
        />
        <span className="practice-folder-download__circle">
          <Download className="practice-folder-download__icon" strokeWidth={2.25} aria-hidden />
        </span>
        <p className="practice-folder-download__title">Practice folder</p>
        <p className="practice-folder-download__title practice-folder-download__title--done">
          <Check className="practice-folder-download__done-icon" strokeWidth={2.5} aria-hidden />
          <span>Done</span>
        </p>
      </label>
    </div>
  );
}
