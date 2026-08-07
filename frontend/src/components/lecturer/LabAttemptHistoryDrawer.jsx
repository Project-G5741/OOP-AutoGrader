import { useEffect, useState } from 'react';
import { X } from 'lucide-react';
import { formatNumber, formatPercent, formatText } from '../../utils/formatters';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

function authHeaders() {
  return {
    Authorization: `Bearer ${sessionStorage.getItem('accessToken')}`,
  };
}

export default function LabAttemptHistoryDrawer({ open, onClose, labId, student, labName }) {
  const [attempts, setAttempts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

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
        const response = await fetch(
          `${API_BASE}/api/labs/${labId}/students/${student.studentId}/attempts`,
          { headers: authHeaders() },
        );
        if (!response.ok) {
          throw new Error('Unable to load submission history');
        }
        const data = await response.json();
        if (!cancelled) {
          setAttempts(Array.isArray(data) ? data : []);
        }
      } catch (err) {
        if (!cancelled) {
          setAttempts([]);
          setError(err.message || 'Unable to load submission history');
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

  if (!open || !student) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-40 flex justify-end bg-black/40">
      <button type="button" className="flex-1" aria-label="Close drawer" onClick={onClose} />
      <aside className="flex h-full w-full max-w-md flex-col border-l border-gray-200 bg-white shadow-2xl dark:border-gray-700 dark:bg-[#1e2530]">
        <div className="flex items-start justify-between gap-3 border-b border-gray-200 px-5 py-4 dark:border-gray-700">
          <div>
            <p className="font-semibold text-gray-900 dark:text-white">{formatText(student.studentName)}</p>
            <p className="text-xs text-gray-500 dark:text-gray-400">ID: {formatText(student.studentCode)}</p>
            <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">{formatText(labName)} submission history</p>
          </div>
          <button type="button" onClick={onClose} className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 dark:hover:bg-[#151b24]">
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-5 py-4">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-purple-500 border-t-transparent" />
            </div>
          ) : error ? (
            <p className="text-sm text-amber-700 dark:text-amber-300">{error}</p>
          ) : attempts.length === 0 ? (
            <p className="py-8 text-center text-sm text-gray-500 dark:text-gray-400">No submission attempts yet.</p>
          ) : (
            <div className="overflow-hidden rounded-xl border border-gray-200 dark:border-gray-700">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-200 bg-gray-50 dark:border-gray-700 dark:bg-[#151b24]">
                    {['Attempt', 'Score', 'Submitted At'].map((col) => (
                      <th key={col} className="px-4 py-3 text-left font-medium text-gray-700 dark:text-gray-300">{col}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {attempts.map((attempt) => (
                    <tr key={`${attempt.attemptNumber}-${attempt.submissionId}`} className="border-b border-gray-100 dark:border-gray-800">
                      <td className="px-4 py-3 text-gray-800 dark:text-gray-200">#{formatNumber(attempt.attemptNumber)}</td>
                      <td className="px-4 py-3 font-semibold text-gray-900 dark:text-white">{formatPercent(attempt.score)}</td>
                      <td className="px-4 py-3 text-gray-800 dark:text-gray-200">{attempt.submittedAt || '—'}</td>
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
