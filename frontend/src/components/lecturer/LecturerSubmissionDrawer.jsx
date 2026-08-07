import { useEffect, useState } from 'react';
import { X } from 'lucide-react';
import ClassScoreBreakdown from './ClassScoreBreakdown';
import ExportMenu from './ExportMenu';
import { exportChallengeBreakdown } from './exportRoster';
import { formatNumber, formatPercent, formatText } from '../../utils/formatters';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

function authHeaders() {
  return {
    Authorization: `Bearer ${sessionStorage.getItem('accessToken')}`,
  };
}

export default function LecturerSubmissionDrawer({
  open,
  onClose,
  labId,
  challengeId,
  challengeLabel,
  student,
}) {
  const [classData, setClassData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!open || !labId || !challengeId || !student?.studentId) {
      setClassData([]);
      setError(null);
      return;
    }

    let cancelled = false;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const params = new URLSearchParams({ studentId: student.studentId });
        if (student.submissionId) {
          params.set('submissionId', student.submissionId);
        }
        const response = await fetch(
          `${API_BASE}/api/labs/${labId}/challenges/${challengeId}/class?${params.toString()}`,
          { headers: authHeaders() },
        );
        if (!response.ok) {
          throw new Error('Unable to load class breakdown');
        }
        const data = await response.json();
        if (!cancelled) {
          setClassData(Array.isArray(data) ? data : []);
        }
      } catch (err) {
        if (!cancelled) {
          setClassData([]);
          setError(err.message || 'Unable to load class breakdown');
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
  }, [open, labId, challengeId, student?.studentId, student?.submissionId]);

  if (!open || !student) {
    return null;
  }

  const initials = (student.studentName || '?')
    .split(' ')
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

  const handleExport = async (format) => {
    const fileBase = `${String(student.studentCode || 'student').replace(/\s+/g, '_')}_${String(challengeLabel || 'challenge').replace(/\s+/g, '_')}`;
    await exportChallengeBreakdown(format, {
      studentName: student.studentName,
      classData,
      fileBase,
    });
  };

  return (
    <div className="fixed inset-0 z-40 flex justify-end bg-black/40">
      <button type="button" className="flex-1" aria-label="Close drawer" onClick={onClose} />
      <aside className="flex h-full w-full max-w-md flex-col border-l border-gray-200 bg-white shadow-2xl dark:border-gray-700 dark:bg-[#1e2530]">
        <div className="flex items-start justify-between gap-3 border-b border-gray-200 px-5 py-4 dark:border-gray-700">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-purple-100 text-sm font-bold text-purple-700 dark:bg-purple-900/30 dark:text-purple-200">
              {initials}
            </div>
            <div>
              <p className="font-semibold text-gray-900 dark:text-white">{formatText(student.studentName)}</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">ID: {formatText(student.studentCode)}</p>
            </div>
          </div>
          <button type="button" onClick={onClose} className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 dark:hover:bg-[#151b24]">
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="space-y-4 border-b border-gray-200 px-5 py-4 text-sm dark:border-gray-700">
          <div className="flex items-center justify-between gap-3">
            <span className="text-gray-500 dark:text-gray-400">Challenge</span>
            <span className="font-medium text-gray-900 dark:text-white">{formatText(challengeLabel)}</span>
          </div>
          <div className="flex items-center justify-between gap-3">
            <span className="text-gray-500 dark:text-gray-400">Overall Score</span>
            <span className="font-semibold text-gray-900 dark:text-white">{formatPercent(student.score)}</span>
          </div>
          <div className="flex items-center justify-between gap-3">
            <span className="text-gray-500 dark:text-gray-400">Attempts</span>
            <span className="font-medium text-gray-900 dark:text-white">{formatNumber(student.attempt ?? student.attempts)}</span>
          </div>
          <div className="flex items-center justify-between gap-3">
            <span className="text-gray-500 dark:text-gray-400">Last Submitted</span>
            <span className="font-medium text-gray-900 dark:text-white">{student.submittedAt || '—'}</span>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto px-5 py-4">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-purple-500 border-t-transparent" />
            </div>
          ) : error ? (
            <p className="text-sm text-amber-700 dark:text-amber-300">{error}</p>
          ) : (
            <ClassScoreBreakdown classData={classData} overallScore={student.score} />
          )}
        </div>

        <div className="border-t border-gray-200 px-5 py-4 dark:border-gray-700">
          <ExportMenu onExport={handleExport} disabled={loading} />
        </div>
      </aside>
    </div>
  );
}
