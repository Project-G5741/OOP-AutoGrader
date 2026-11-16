import { useState } from 'react';
import { Loader2, Sparkles } from 'lucide-react';
import { apiFetch } from '../../utils/apiFetch';
import { readFriendlyApiError } from '../../utils/apiError';
import { getAccessToken } from '../../utils/authRoutes';
import { useToast } from '../ui/Toast';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

const AREAS = [
  { id: 'FIELD_MODELING', label: 'Fields' },
  { id: 'METHODS', label: 'Methods' },
  { id: 'CONSTRUCTORS', label: 'Constructors' },
];

function alertMessage(label) {
  if (label === '50%') return 'Token usage reached 50% of your budget.';
  if (label === '80%') return 'Token usage reached 80% of your budget.';
  if (label === '90%') return 'Token budget exhausted (90%). New hints are paused.';
  return `Token usage alert: ${label}`;
}

export default function AiLearningAssistantPanel({
  labId,
  submissionId,
  enabled = false,
  onOpenSettings,
}) {
  const showToast = useToast();
  const [area, setArea] = useState('FIELD_MODELING');
  const [language, setLanguage] = useState('en');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);

  if (!enabled) return null;

  const handleRequest = async () => {
    if (!labId || !submissionId) {
      setError('Submit this lab first, then request a Conceptual hint.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const token = getAccessToken();
      const res = await apiFetch(`${API_BASE}/api/students/ai/hint`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          labId,
          submissionId,
          learningArea: area,
          language,
        }),
      });
      if (!res.ok) {
        if (res.status === 400 || res.status === 429) {
          onOpenSettings?.();
        }
        throw new Error(await readFriendlyApiError(res, 'read'));
      }
      const data = await res.json();
      setResult(data);
      const alerts = data?.alerts || [];
      alerts.forEach((label) => {
        showToast({
          message: alertMessage(label),
          type: label === '90%' ? 'error' : 'warning',
        });
      });
      if (data?.token?.exhausted && !alerts.includes('90%')) {
        showToast({
          message: 'Token budget exhausted (90%). New Conceptual hints are paused.',
          type: 'error',
        });
      }
    } catch (err) {
      setError(err.message || 'Could not load Conceptual hint.');
      setResult(null);
    } finally {
      setLoading(false);
    }
  };

  const feedback = result?.feedback;
  const usagePercent = result?.token?.usagePercent;

  return (
    <section className="mt-4 rounded-2xl border border-dashed border-border bg-surface-secondary/40 p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex items-start gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg border border-border bg-surface">
            <Sparkles className="h-5 w-5 text-primary" aria-hidden="true" />
          </div>
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-foreground-muted">
              AI Learning Assistant · advisory
            </p>
            <h3 className="text-base font-semibold text-foreground">Conceptual hint</h3>
            <p className="mt-1 text-sm text-foreground-muted">
              Guide your thinking. This never changes your official score and will not name exact
              answers.
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={onOpenSettings}
          className="text-sm font-medium text-primary hover:underline"
        >
          Key & token management
        </button>
      </div>

      <div className="mt-4 flex flex-wrap gap-3">
        <label className="text-sm text-foreground">
          Area
          <select
            className="ml-2 rounded-lg border border-border bg-surface px-2 py-1.5"
            value={area}
            onChange={(e) => setArea(e.target.value)}
          >
            {AREAS.map((item) => (
              <option key={item.id} value={item.id}>
                {item.label}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm text-foreground">
          Language
          <select
            className="ml-2 rounded-lg border border-border bg-surface px-2 py-1.5"
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
          >
            <option value="en">English</option>
            <option value="vi">Tiếng Việt</option>
          </select>
        </label>
        <button
          type="button"
          disabled={loading}
          onClick={handleRequest}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground disabled:opacity-60"
        >
          {loading && <Loader2 className="h-4 w-4 animate-spin" />}
          Request Conceptual hint
        </button>
      </div>

      {typeof usagePercent === 'number' && (
        <p className="mt-3 text-xs text-foreground-muted">Token usage: {usagePercent}%</p>
      )}

      {error && <p className="mt-3 text-sm text-error-text">{error}</p>}

      {feedback && (
        <div className="mt-4 space-y-2 rounded-xl border border-border bg-surface p-4">
          <p className="text-xs font-semibold uppercase tracking-wider text-foreground-muted">
            {feedback.concept || feedback.category}
            {result?.cacheHit ? ' · cached' : ''}
            {result?.status === 'rejected' || result?.status === 'failed' ? ' · safe fallback' : ''}
          </p>
          <p className="text-sm text-foreground">{feedback.summary}</p>
          <p className="text-sm font-medium text-foreground">{feedback.question}</p>
          {feedback.explanation && (
            <p className="text-sm text-foreground-muted">{feedback.explanation}</p>
          )}
        </div>
      )}
    </section>
  );
}
