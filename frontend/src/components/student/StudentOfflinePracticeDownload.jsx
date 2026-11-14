import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Check, Download } from 'lucide-react';
import { toFriendlyError } from '../../utils/apiError';
import { getAccessToken } from '../../utils/authRoutes';
import './StudentOfflinePracticeDownload.css';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

/** Optimistic UI only — transfer progress belongs to the browser download bar. */
const BUSY_VISIBLE_MS = 2200;
const DONE_VISIBLE_MS = 1800;

/** Nav control: download pill. Hands the zip GET to the browser via `?access_token=`. */
export default function StudentOfflinePracticeDownload({ onToast, className = '' }) {
  const [downloading, setDownloading] = useState(false);
  const [complete, setComplete] = useState(false);
  const inputRef = useRef(null);
  const resetTimerRef = useRef(null);
  const doneTimerRef = useRef(null);
  const inFlightRef = useRef(false);

  const clearTimers = useCallback(() => {
    if (resetTimerRef.current) {
      window.clearTimeout(resetTimerRef.current);
      resetTimerRef.current = null;
    }
    if (doneTimerRef.current) {
      window.clearTimeout(doneTimerRef.current);
      doneTimerRef.current = null;
    }
  }, []);

  const resetControl = useCallback(() => {
    setDownloading(false);
    setComplete(false);
    if (inputRef.current) {
      inputRef.current.checked = false;
    }
  }, []);

  useEffect(() => () => clearTimers(), [clearTimers]);

  const startBrowserDownload = useCallback(() => {
    const token = getAccessToken();
    if (!token) {
      throw new Error('Please sign in again to download.');
    }
    // Browser owns the transfer so students see native download progress.
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
  }, []);

  const handleLabelClick = (event) => {
    if (downloading || inFlightRef.current) {
      event.preventDefault();
      return;
    }
    event.preventDefault();

    try {
      startBrowserDownload();
    } catch (err) {
      onToast?.({ message: toFriendlyError(err, 'download'), type: 'error' });
      return;
    }

    onToast?.({
      message: 'Download started in your browser. Check Downloads for progress.',
      type: 'success',
    });

    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reducedMotion) {
      return;
    }

    inFlightRef.current = true;
    clearTimers();
    setComplete(false);
    setDownloading(true);
    if (inputRef.current) {
      inputRef.current.checked = true;
    }

    doneTimerRef.current = window.setTimeout(() => {
      setComplete(true);
      resetTimerRef.current = window.setTimeout(() => {
        resetControl();
        inFlightRef.current = false;
      }, DONE_VISIBLE_MS);
    }, BUSY_VISIBLE_MS);
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
            ? 'Download started'
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
        <span className="practice-folder-download__circle" aria-hidden>
          <Download className="practice-folder-download__icon" strokeWidth={2.25} />
        </span>
        <p className="practice-folder-download__title">Practice folder</p>
        <p className="practice-folder-download__title practice-folder-download__title--busy">
          Downloading…
        </p>
        <p className="practice-folder-download__title practice-folder-download__title--done">
          <Check className="practice-folder-download__done-icon" strokeWidth={2.5} aria-hidden />
          <span>Started</span>
        </p>
      </label>
    </div>
  );
}
