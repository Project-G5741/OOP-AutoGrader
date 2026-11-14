import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Check, Download } from 'lucide-react';
import { toFriendlyError } from '../../utils/apiError';
import '../student/StudentOfflinePracticeDownload.css';

/** Optimistic UI only — transfer progress belongs to the browser download bar. */
const BUSY_VISIBLE_MS = 2200;
const DONE_VISIBLE_MS = 1800;

const DEFAULT_BUNDLE_URL =
  'https://github.com/Project-G5741/OOP-AutoGrader/releases/download/grading-bundle-v1/OOP-AutoGrader-LecturerBulk.zip';

/**
 * Web lecturer Bulk Grading: same animated pill as student Practice folder, but the
 * browser navigates a public release URL (no JWT / API stream).
 *
 * @param {{ onToast?: Function, className?: string, href?: string }} props
 * `href` overrides `LECTURER_BULK_BUNDLE_URL` / built-in default.
 */
export default function LecturerOfflineBulkDownload({ onToast, className = '', href }) {
  const configured =
    (typeof href === 'string' && href.trim())
    || (typeof import.meta.env.LECTURER_BULK_BUNDLE_URL === 'string'
      && import.meta.env.LECTURER_BULK_BUNDLE_URL.trim())
    || DEFAULT_BUNDLE_URL;

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
    const url = configured.trim();
    if (!url) {
      throw new Error('Offline Bulk download URL is not configured.');
    }
    const link = document.createElement('a');
    link.href = url;
    link.rel = 'noopener';
    document.body.appendChild(link);
    link.click();
    link.remove();
  }, [configured]);

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

  if (!configured.trim()) {
    return null;
  }

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
              : 'Download offline Bulk folder'
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
        <p className="practice-folder-download__title">Bulk folder</p>
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
