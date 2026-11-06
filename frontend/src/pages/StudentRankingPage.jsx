import { useEffect, useState } from 'react';
import RankingBoard from '../components/ranking/RankingBoard';
import { apiFetch } from '../utils/apiFetch';
import { authHeaders } from '../utils/authHeaders';
import { friendlyLoadErrorFromResponse, toFriendlyError } from '../utils/apiError';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

export default function StudentRankingPage({ user, inCurrentTerm = true }) {
  const [board, setBoard] = useState({ weekStart: null, weekEnd: null, rows: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const response = await apiFetch(`${API_BASE}/api/students/ranking/current`, {
          headers: authHeaders(),
        });
        if (!response.ok) {
          throw new Error(await friendlyLoadErrorFromResponse(response));
        }
        const data = await response.json();
        if (!cancelled) {
          setBoard({
            weekStart: data.weekStart ?? null,
            weekEnd: data.weekEnd ?? null,
            rows: Array.isArray(data.rows) ? data.rows : [],
          });
        }
      } catch (err) {
        if (!cancelled) {
          setBoard({ weekStart: null, weekEnd: null, rows: [] });
          setError(toFriendlyError(err, 'read'));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="flex min-h-0 flex-1 flex-col gap-2 rounded-xl border border-border bg-surface p-3 shadow-sm sm:p-4">
      <h1 className="text-lg font-semibold text-foreground">Ranking</h1>
      {!inCurrentTerm ? (
        <p className="rounded-xl bg-warning-bg px-4 py-3 text-sm text-warning-text">
          Ranking is for students enrolled in the current quarter.
        </p>
      ) : null}
      <RankingBoard
        weekStart={board.weekStart}
        weekEnd={board.weekEnd}
        rows={board.rows}
        loading={loading}
        error={error}
        viewerUserId={user?.id}
      />
    </div>
  );
}
