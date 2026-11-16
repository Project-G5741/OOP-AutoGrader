import { useState } from 'react';
import { Loader2, KeyRound } from 'lucide-react';
import { Sparkle } from '@phosphor-icons/react';
import Combobox from '../ui/Combobox';
import { apiFetch } from '../../utils/apiFetch';
import { readFriendlyApiError } from '../../utils/apiError';
import { getAccessToken } from '../../utils/authRoutes';
import { useToast } from '../ui/Toast';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

const AREA_OPTIONS = [
  { value: 'FIELD_MODELING', label: 'Fields' },
  { value: 'METHODS', label: 'Methods' },
  { value: 'CONSTRUCTORS', label: 'Constructors' },
];

const LANGUAGE_OPTIONS = [
  { value: 'en', label: 'English' },
  { value: 'vi', label: 'Tiếng Việt' },
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
    <div className="space-y-5">
      {/* Top Header / Bar */}
      <div className="flex flex-wrap items-center justify-between gap-3 pb-4 border-b border-border">
        <div className="flex items-center gap-2.5">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary-light text-primary-text">
            <Sparkle size={18} weight="duotone" aria-hidden="true" />
          </div>
          <h3 className="text-sm font-semibold text-foreground">AI Learning Assistant</h3>
        </div>
        <div className="flex items-center gap-4">
          {typeof usagePercent === 'number' && (
            <span className="text-xs text-foreground-muted tabular-nums">
              Token usage: <span className="font-semibold text-foreground">{usagePercent}%</span>
            </span>
          )}
          {onOpenSettings && (
            <button
              type="button"
              onClick={onOpenSettings}
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-primary hover:underline"
            >
              <KeyRound className="h-3.5 w-3.5" aria-hidden="true" />
              <span>Key & Token Management</span>
            </button>
          )}
        </div>
      </div>

      {/* Dropdown controls conforming strictly to design system */}
      <div className="flex flex-wrap items-end gap-3 sm:gap-4">
        <div className="w-full sm:w-48">
          <label className="block mb-1.5 text-xs font-semibold uppercase tracking-wider text-foreground-muted">
            Learning Area
          </label>
          <Combobox
            value={area}
            onChange={setArea}
            options={AREA_OPTIONS}
            allowEmpty={false}
            disabled={loading}
            aria-label="Learning Area"
            className="w-full min-w-0"
          />
        </div>

        <div className="w-full sm:w-40">
          <label className="block mb-1.5 text-xs font-semibold uppercase tracking-wider text-foreground-muted">
            Language
          </label>
          <Combobox
            value={language}
            onChange={setLanguage}
            options={LANGUAGE_OPTIONS}
            allowEmpty={false}
            disabled={loading}
            aria-label="Language"
            className="w-full min-w-0"
          />
        </div>

        <button
          type="button"
          disabled={loading}
          onClick={handleRequest}
          className="inline-flex h-10 w-full sm:w-auto items-center justify-center gap-2 rounded-xl bg-primary px-4 text-sm font-semibold text-white shadow-stage-1 transition-all hover:bg-primary-hover active:scale-[0.98] disabled:opacity-60 disabled:pointer-events-none"
        >
          {loading ? (
            <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
          ) : (
            <Sparkle size={16} weight="duotone" aria-hidden="true" />
          )}
          <span>Request Conceptual Hint</span>
        </button>
      </div>

      {error && (
        <div className="rounded-xl border border-error/30 bg-error-bg/40 p-3 text-sm text-error-text">
          {error}
        </div>
      )}

      {/* Result feedback */}
      {feedback && (
        <div className="space-y-3 rounded-xl border-l-4 border-purple bg-surface-secondary/70 p-4">
          <div className="flex items-center justify-between gap-2">
            <span className="text-xs font-bold uppercase tracking-wider text-purple">
              {feedback.concept || feedback.category}
            </span>
            <div className="flex items-center gap-1.5">
              {result?.cacheHit && (
                <span className="rounded-md border border-border-subtle bg-surface px-2 py-0.5 text-[11px] font-semibold text-foreground-muted">
                  Cached
                </span>
              )}
              {(result?.status === 'rejected' || result?.status === 'failed') && (
                <span className="rounded-md bg-warning-bg px-2 py-0.5 text-[11px] font-semibold text-warning-text">
                  Safe Fallback
                </span>
              )}
            </div>
          </div>
          {feedback.summary && (
            <p className="text-sm font-medium text-foreground">{feedback.summary}</p>
          )}
          {feedback.question && (
            <div className="rounded-lg border border-border-subtle bg-surface p-3">
              <p className="mb-1 text-xs font-semibold text-foreground-muted">Guiding Question</p>
              <p className="text-sm font-medium text-foreground">{feedback.question}</p>
            </div>
          )}
          {feedback.explanation && (
            <p className="text-xs text-foreground-secondary leading-relaxed">{feedback.explanation}</p>
          )}
        </div>
      )}

      {/* Clean initial empty prompt if no feedback yet and not loading */}
      {!feedback && !loading && !error && (
        <div className="flex flex-col items-center justify-center py-12 text-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-surface-secondary text-primary">
            <Sparkle size={24} weight="duotone" aria-hidden="true" />
          </div>
          <p className="mt-3 text-sm font-semibold text-foreground">
            Need conceptual guidance on this lab?
          </p>
          <p className="mt-1 max-w-sm text-xs text-foreground-muted">
            Choose a learning area above and click Request Conceptual Hint to receive personalized advice.
          </p>
        </div>
      )}
    </div>
  );
}
