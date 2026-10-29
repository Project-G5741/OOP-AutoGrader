import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Check, Download } from 'lucide-react';
import { toFriendlyError } from '../../utils/apiError';
import { getAccessToken } from '../../utils/authRoutes';
import './StudentOfflinePracticeDownload.css';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

const ANIMATION_MS = 4200;
const DONE_VISIBLE_MS = 1800;

/** Nav control: Uiverse-style download pill (theme tokens via CSS variables). */
export default function StudentOfflinePracticeDownload({ onToast, className = '' }) {
  const [downloading, setDownloading] = useState(false);
  const inputRef = useRef(null);
  const resetTimerRef = useRef(null);

  const clearResetTimer = useCallback(() => {
    if (resetTimerRef.current) {
      window.clearTimeout(resetTimerRef.current);
      resetTimerRef.current = null;
    }
  }, []);

  useEffect(() => () => clearResetTimer(), [clearResetTimer]);

  const startDownload = useCallback(() => {
    try {
      const token = getAccessToken();
      if (!token) {
        throw new Error('Please sign in again to download.');
      }
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
      return true;
    } catch (err) {
      onToast?.({ message: toFriendlyError(err, 'download'), type: 'error' });
      return false;
    }
  }, [onToast]);

  const handleLabelClick = (event) => {
    if (downloading) {
      event.preventDefault();
      return;
    }
    event.preventDefault();
    const ok = startDownload();
    if (!ok) return;

    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reducedMotion) {
      return;
    }

    setDownloading(true);
    if (inputRef.current) {
      inputRef.current.checked = true;
    }

    clearResetTimer();
    resetTimerRef.current = window.setTimeout(() => {
      setDownloading(false);
      if (inputRef.current) {
        inputRef.current.checked = false;
      }
    }, ANIMATION_MS + DONE_VISIBLE_MS);
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
        className={`practice-folder-download__label${downloading ? ' practice-folder-download__label--busy' : ''}`}
        onClick={handleLabelClick}
        onKeyDown={handleKeyDown}
        tabIndex={downloading ? -1 : 0}
        role="button"
        aria-busy={downloading}
        aria-label={downloading ? 'Download in progress' : 'Download practice folder'}
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
