import { useEffect, useMemo, useState } from 'react';
import { X } from 'lucide-react';
import PlagiarismDangerMark from './PlagiarismDangerMark';
import { formatPercent, formatText } from '../../utils/formatters';
import { friendlyLoadErrorFromResponse, toFriendlyError } from '../../utils/apiError';
import { apiFetch } from '../../utils/apiFetch';
import { authHeaders } from '../../utils/authHeaders';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

export default function PlagiarismInvestigationDrawer({ open, onClose, labId, student, labName }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!open || !labId || !student?.studentId) {
      setData(null);
      setError(null);
      return;
    }

    let cancelled = false;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await apiFetch(
          `${API_BASE}/api/lecturer/labs/${labId}/students/${student.studentId}/plagiarism`,
          { headers: authHeaders() },
        );
        if (!response.ok) {
          throw new Error(await friendlyLoadErrorFromResponse(response));
        }
        const payload = await response.json();
        if (!cancelled) {
          setData(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setData(null);
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

  const edgesByFrom = useMemo(() => {
    const map = new Map();
    for (const edge of data?.edges ?? []) {
      const list = map.get(edge.fromStudentId) ?? [];
      list.push(edge);
      map.set(edge.fromStudentId, list);
    }
    return map;
  }, [data?.edges]);

  if (!open || !student) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/40">
      <div className="flex h-full w-full max-w-xl flex-col bg-surface shadow-xl">
        <div className="flex items-start justify-between border-b border-border px-5 py-4">
          <div>
            <h3 className="text-lg font-semibold text-foreground">Plagiarism investigation</h3>
            <p className="mt-1 text-sm text-foreground-secondary">
              {formatText(student.studentName)} · {formatText(student.studentCode)}
              {labName ? ` · ${labName}` : ''}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-2 text-foreground-secondary hover:bg-surface-hover"
            aria-label="Close"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-5 py-4">
          {loading ? (
            <p className="text-sm text-foreground-secondary">Loading lineage…</p>
          ) : error ? (
            <p className="text-sm text-danger">{error}</p>
          ) : data?.message && !data?.nodes?.length ? (
            <p className="text-sm text-foreground-secondary">{data.message}</p>
          ) : (
            <div className="space-y-5">
              <section>
                <h4 className="text-sm font-semibold text-foreground">Ultimate origin</h4>
                <p className="mt-1 text-sm text-foreground-secondary">
                  {formatText(data?.originStudentName)} ({formatText(data?.originStudentCode)})
                </p>
              </section>

              <section>
                <h4 className="mb-3 text-sm font-semibold text-foreground">Copy lineage</h4>
                <ul className="space-y-3">
                  {(data?.nodes ?? []).map((node) => {
                    const outgoing = edgesByFrom.get(node.studentId) ?? [];
                    return (
                      <li
                        key={node.studentId}
                        className={`rounded-lg border px-3 py-2 ${
                          node.focus ? 'border-primary bg-primary-light' : 'border-border bg-surface'
                        }`}
                      >
                        <div className="flex items-center gap-2">
                          <PlagiarismDangerMark show role={node.role} className="ml-0" />
                          <div className="min-w-0 flex-1">
                            <p className="truncate text-sm font-medium text-foreground">
                              {formatText(node.studentName)}
                              {node.origin ? ' · origin' : ''}
                              {node.focus ? ' · focus' : ''}
                            </p>
                            <p className="text-xs text-foreground-secondary">
                              {formatText(node.studentCode)}
                              {node.earliestSubmittedAt ? ` · first submit ${node.earliestSubmittedAt}` : ''}
                            </p>
                          </div>
                        </div>
                        {outgoing.length > 0 ? (
                          <ul className="mt-2 space-y-1 border-l border-border pl-3">
                            {outgoing.map((edge) => {
                              const target = (data?.nodes ?? []).find((n) => n.studentId === edge.toStudentId);
                              return (
                                <li key={`${edge.fromStudentId}-${edge.toStudentId}`} className="text-xs text-foreground-secondary">
                                  → {formatText(target?.studentName)}
                                  {edge.gitMatch ? ' · git' : ''}
                                  {edge.metadataMatch ? ' · metadata' : ''}
                                  {edge.hashSimilarity != null
                                    ? ` · overlap ${formatPercent(
                                        Number(edge.hashSimilarity) <= 1
                                          ? Number(edge.hashSimilarity) * 100
                                          : Number(edge.hashSimilarity),
                                      )}`
                                    : ''}
                                </li>
                              );
                            })}
                          </ul>
                        ) : null}
                      </li>
                    );
                  })}
                </ul>
              </section>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
