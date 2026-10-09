import { useEffect, useMemo, useState } from 'react';
import { X } from 'lucide-react';
import SortableTableHeader from '../ui/SortableTableHeader';
import { formatNumber, formatPercent, formatText } from '../../utils/formatters';
import { parseDisplayTimestamp, sortRows, toggleSortState } from '../../utils/sort';
import { friendlyLoadErrorFromResponse, toFriendlyError } from '../../utils/apiError';
import { apiFetch } from '../../utils/apiFetch';
import { authHeaders } from '../../utils/authHeaders';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

import {
  LECTURER_DRAWER_BACKDROP_HIT,
  LECTURER_DRAWER_DIVIDER,
  LECTURER_DRAWER_OVERLAY,
  LECTURER_DRAWER_PANEL,
} from './lecturerDrawerChrome';

const HEADER_CLASS = 'px-4 py-3 text-left font-medium text-foreground-secondary';

export default function LabAttemptHistoryDrawer({ open, onClose, labId, student, labName }) {
  const [attempts, setAttempts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [sortState, setSortState] = useState({ field: 'submittedAt', direction: 'desc' });

  useEffect(() => {
    if (!open || !labId || !student?.studentId) {
      setAttempts([]);
      setError(null);
      return;
    }

    let cancelled = false;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await apiFetch(
          `${API_BASE}/api/labs/${labId}/students/${student.studentId}/attempts`,
          { headers: authHeaders() },
        );
        if (!response.ok) {
          throw new Error(await friendlyLoadErrorFromResponse(response));
        }
        const data = await response.json();
        if (!cancelled) {
          setAttempts(Array.isArray(data) ? data : []);
        }
      } catch (err) {
        if (!cancelled) {
          setAttempts([]);
          setError(toFriendlyError(err, 'read'));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    void load();
    return () => {
      cancelled = true;
    };
  }, [open, labId, student?.studentId]);

  const sortedAttempts = useMemo(() => sortRows(attempts, sortState.field, sortState.direction, (attempt) => {
    if (sortState.field === 'attempt') return attempt.attemptNumber;
    if (sortState.field === 'score') return attempt.score;
    if (sortState.field === 'submittedAt') return parseDisplayTimestamp(attempt.submittedAt);
    return attempt[sortState.field];
  }), [attempts, sortState]);

  const handleSort = (field) => {
    setSortState((prev) => toggleSortState(prev, field));
  };

  if (!open || !student) {
    return null;
  }

  return (
    <div className={`${LECTURER_DRAWER_OVERLAY} z-40`}>
      <button type="button" className={LECTURER_DRAWER_BACKDROP_HIT} aria-label="Close drawer" onClick={onClose} />
      <aside className={`${LECTURER_DRAWER_PANEL} max-w-md`}>
        <div className={`flex items-start justify-between gap-3 border-b px-5 py-4 ${LECTURER_DRAWER_DIVIDER}`}>
          <div>
            <p className="font-semibold text-foreground">{formatText(student.studentName)}</p>
            <p className="text-xs text-foreground-secondary">ID: {formatText(student.studentCode)}</p>
          </div>
          <button type="button" onClick={onClose} className="rounded-lg p-2 text-foreground-secondary hover:bg-surface-secondary">
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-5 py-4">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
            </div>
          ) : error ? (
            <p className="text-sm text-warning-text">{error}</p>
          ) : attempts.length === 0 ? (
            <p className="py-8 text-center text-sm text-foreground-secondary">No submission attempts yet.</p>
          ) : (
            <div className="overflow-hidden rounded-xl border border-border">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border bg-surface-secondary">
                    <SortableTableHeader label="Attempt" field="attempt" activeField={sortState.field} direction={sortState.direction} onSort={handleSort} className={HEADER_CLASS} />
                    <SortableTableHeader label="Score" field="score" activeField={sortState.field} direction={sortState.direction} onSort={handleSort} className={HEADER_CLASS} />
                    <SortableTableHeader label="Submitted At" field="submittedAt" activeField={sortState.field} direction={sortState.direction} onSort={handleSort} className={HEADER_CLASS} />
                  </tr>
                </thead>
                <tbody>
                  {sortedAttempts.map((attempt) => (
                    <tr key={`${attempt.attemptNumber}-${attempt.submissionId}`} className="border-b border-border dark:border-border">
                      <td className="px-4 py-3 text-foreground">#{formatNumber(attempt.attemptNumber)}</td>
                      <td className="px-4 py-3 font-semibold text-foreground">{formatPercent(attempt.score)}</td>
                      <td className="px-4 py-3 text-foreground">{attempt.submittedAt || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </aside>
    </div>
  );
}
