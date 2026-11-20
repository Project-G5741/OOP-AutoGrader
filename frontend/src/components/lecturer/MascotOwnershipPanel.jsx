import { useEffect, useMemo, useState } from 'react';
import { apiFetch } from '../../utils/apiFetch';
import { authHeaders } from '../../utils/authHeaders';
import { friendlyLoadErrorFromResponse, toFriendlyError } from '../../utils/apiError';
import { STUDENT_MASCOTS } from '../student/studentMascots';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

export default function MascotOwnershipPanel() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError('');
      try {
        const response = await apiFetch(`${API_BASE}/api/lecturer/mascots/ownership`, {
          headers: authHeaders(),
        });
        if (!response.ok) {
          if (!cancelled) {
            setError(await friendlyLoadErrorFromResponse(response));
          }
          return;
        }
        const body = await response.json();
        if (!cancelled) {
          setRows(Array.isArray(body) ? body : []);
        }
      } catch (err) {
        if (!cancelled) {
          setError(toFriendlyError(err));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter(
      (row) =>
        String(row.studentCode || '').toLowerCase().includes(q)
        || String(row.fullName || '').toLowerCase().includes(q),
    );
  }, [rows, search]);

  const mascotLabel = (id) => STUDENT_MASCOTS[id]?.name || id || '—';

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-bold text-foreground">Mascot ownership (current quarter)</h2>
        <input
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search IRN or name"
          className="min-h-10 min-w-[220px] rounded-xl border border-border bg-surface px-3 text-sm text-foreground"
        />
      </div>

      {loading && (
        <p className="text-sm text-foreground-muted">Loading…</p>
      )}
      {error && (
        <p className="text-sm text-error">{error}</p>
      )}

      {!loading && !error && (
        <div className="overflow-x-auto rounded-2xl border border-border">
          <table className="w-full min-w-[720px] text-left text-sm">
            <thead className="bg-surface-secondary text-foreground-muted">
              <tr>
                <th className="px-3 py-2 font-semibold">IRN</th>
                <th className="px-3 py-2 font-semibold">Name</th>
                <th className="px-3 py-2 font-semibold">Equipped</th>
                <th className="px-3 py-2 font-semibold">Owned mascots</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {filtered.map((row) => (
                <tr key={row.studentCode || row.fullName} className="align-top">
                  <td className="px-3 py-2 font-mono tabular-nums text-foreground">{row.studentCode || '—'}</td>
                  <td className="px-3 py-2 text-foreground">{row.fullName || '—'}</td>
                  <td className="px-3 py-2 text-foreground">{mascotLabel(row.equippedMascot)}</td>
                  <td className="px-3 py-2 text-foreground-secondary">
                    {(row.owned || []).length === 0 ? (
                      <span className="text-foreground-muted">—</span>
                    ) : (
                      <ul className="space-y-0.5">
                        {row.owned.map((o) => (
                          <li key={o.mascotId}>
                            {o.displayName || mascotLabel(o.mascotId)}
                            {o.pricePaid === 0 ? ' (gacha)' : ` (${o.pricePaid} pts)`}
                          </li>
                        ))}
                      </ul>
                    )}
                  </td>
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={4} className="px-3 py-6 text-center text-foreground-muted">
                    No students match.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}