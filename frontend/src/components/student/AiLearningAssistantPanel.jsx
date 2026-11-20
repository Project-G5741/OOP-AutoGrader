import { useState, useEffect, useRef, useCallback } from 'react';
import {
  KeyRound,
  ChevronDown,
  ChevronUp,
  RotateCcw,
  CheckCircle2,
  MinusCircle,
  XCircle,
  Loader2,
} from 'lucide-react';
import { Sparkle } from '@phosphor-icons/react';
import { apiFetch } from '../../utils/apiFetch';
import { readFriendlyApiError } from '../../utils/apiError';
import { getAccessToken } from '../../utils/authRoutes';
import { useToast } from '../ui/Toast';
import { firstCompileErrorLine } from '../../utils/formatters';
import {
  consolidateStudentClassMembers,
  formatStudentFieldLine,
  formatStudentConstructorLine,
  formatStudentMethodLine,
} from '../../utils/studentDisplayConsolidation';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

function alertMessage(label) {
  if (label === '50%') return 'Token usage reached 50% of your budget.';
  if (label === '80%') return 'Token usage reached 80% of your budget.';
  if (label === '90%') return 'Token budget exhausted (90%). New hints are paused.';
  return `Token usage alert: ${label}`;
}

function ClassStatusIcon({ status }) {
  if (status === 'success' || status === 'info') {
    return <CheckCircle2 className="h-5 w-5 shrink-0 text-success" />;
  }
  if (status === 'warning') {
    return <MinusCircle className="h-5 w-5 shrink-0 text-warning" />;
  }
  return <XCircle className="h-5 w-5 shrink-0 text-error" />;
}

function extractFailingClasses(classes) {
  if (!Array.isArray(classes)) return [];
  return classes
    .filter((cls) => {
      const hasCompileErr = Boolean(cls.error);
      const hasFieldErr = (cls.fields || []).some((f) => !f.ok);
      const hasCtorErr = (cls.constructors || []).some((c) => !c.ok);
      const hasMethodErr = (cls.methods || []).some((m) => !m.ok);
      const isErrStatus = cls.status === 'error' || cls.status === 'warning';
      return hasCompileErr || hasFieldErr || hasCtorErr || hasMethodErr || isErrStatus;
    })
    .map((cls) => ({
      className: cls.name,
      classType: cls.type || 'CLASS',
      status: cls.status || 'error',
      compileError: cls.error || null,
      fieldIssues: consolidateStudentClassMembers(cls.fields || [])
        .filter((f) => !f.ok)
        .map((f) => formatStudentFieldLine(f)),
      constructorIssues: consolidateStudentClassMembers(cls.constructors || [])
        .filter((c) => !c.ok)
        .map((c) => formatStudentConstructorLine(c)),
      methodIssues: consolidateStudentClassMembers(cls.methods || [])
        .filter((m) => !m.ok)
        .map((m) => formatStudentMethodLine(m)),
    }));
}

export default function AiLearningAssistantPanel({
  labId,
  submissionId,
  challengeId,
  challengeName,
  classData = [],
  enabled = false,
  onOpenSettings,
  // Shared ref from parent so the cache survives unmount/remount across tab switches.
  // Falls back to a local ref when not provided.
  externalCacheRef = null,
  isLoadingDetails = false,
}) {
  const showToast = useToast();
  const [loading, setLoading] = useState(false);
  const [checkingKey, setCheckingKey] = useState(true);
  const [keyConnected, setKeyConnected] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);
  const [expandedClasses, setExpandedClasses] = useState(new Set());

  // Use the shared external cache ref when available so feedback survives tab-switching.
  // The parent (StudentUI) clears this ref whenever the lab changes.
  const localCacheRef = useRef({});
  const feedbackCacheRef = externalCacheRef ?? localCacheRef;

  // Active challenge tracking and cancellation
  const activeChallengeIdRef = useRef(challengeId);
  activeChallengeIdRef.current = challengeId;
  const abortControllerRef = useRef(null);
  const fetchingChallengeIdRef = useRef(null);

  // Check key connection status on mount or when settings modal might have changed
  const checkKeyStatus = useCallback(async () => {
    try {
      const token = getAccessToken();
      if (!token) return false;
      const res = await apiFetch(`${API_BASE}/api/students/ai/token`, {
        authHandling: 'self',
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) return false;
      const data = await res.json();
      const connected = Boolean(data?.connected);
      setKeyConnected(connected);
      return connected;
    } catch {
      return false;
    } finally {
      setCheckingKey(false);
    }
  }, []);

  useEffect(() => {
    if (!enabled) return;
    checkKeyStatus();
  }, [enabled, checkKeyStatus]);

  // Synchronize result state immediately whenever challenge or lab changes:
  // If cached, display cached result; if not, clear old result so we never show
  // results from another challenge.
  useEffect(() => {
    if (!labId || !challengeId) {
      setResult(null);
      setLoading(false);
      setError('');
      setExpandedClasses(new Set());
      return;
    }
    const cacheKey = `${submissionId ?? 'latest'}:${challengeId}`;
    const cached = feedbackCacheRef.current[cacheKey];
    if (cached) {
      setResult(cached);
      setLoading(false);
      setError('');
      const classNames = (cached?.feedback?.classes || []).map((c) => c.className);
      setExpandedClasses(new Set(classNames));
    } else {
      setResult(null);
      setError('');
      setExpandedClasses(new Set());
    }
  }, [labId, submissionId, challengeId, feedbackCacheRef]);

  // Cleanup on unmount or when challenge/lab switches: abort any in-flight request
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }
    };
  }, [labId, challengeId]);

  const fetchHint = useCallback(
    async (isManualRefresh = false) => {
      if (!labId || !challengeId) {
        return;
      }

      const targetChallengeId = challengeId;
      const targetSubmissionId = submissionId;
      const cacheKey = `${targetSubmissionId ?? 'latest'}:${targetChallengeId}`;

      // If already fetching for this exact challenge and not a manual refresh, do not duplicate
      if (!isManualRefresh && fetchingChallengeIdRef.current === targetChallengeId) {
        return;
      }

      // Abort any prior in-flight request (e.g. from switching challenges quickly)
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }

      if (!isManualRefresh && feedbackCacheRef.current[cacheKey]) {
        const cached = feedbackCacheRef.current[cacheKey];
        if (activeChallengeIdRef.current === targetChallengeId) {
          setResult(cached);
          setLoading(false);
          setError('');
          const classNames = (cached?.feedback?.classes || []).map((c) => c.className);
          setExpandedClasses(new Set(classNames));
        }
        return;
      }

      const controller = new AbortController();
      abortControllerRef.current = controller;
      fetchingChallengeIdRef.current = targetChallengeId;

      setLoading(true);
      setError('');

      try {
        const failingClasses = extractFailingClasses(classData);
        const token = getAccessToken();
        const res = await apiFetch(`${API_BASE}/api/students/ai/hint`, {
          method: 'POST',
          signal: controller.signal,
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            labId,
            submissionId: targetSubmissionId,
            challengeId: targetChallengeId,
            challengeName: challengeName || 'Challenge',
            language: 'en',
            failingClasses,
          }),
        });

        if (controller.signal.aborted) return;

        if (!res.ok) {
          // Read backend message first (invalid key, budget exhausted, lab disabled, etc.)
          // before falling back to a generic string ΓÇö avoids showing "Server Busy" for
          // legitimate 400/403/429 application errors.
          let errMsg = '';
          try {
            const payload = await res.clone().json();
            if (typeof payload?.message === 'string' && payload.message.trim()) {
              errMsg = payload.message.trim();
            }
          } catch {
            /* fall through */
          }
          if (!errMsg) {
            errMsg = await readFriendlyApiError(res, 'read');
          }
          if (res.status === 400 || res.status === 429) {
            setKeyConnected(false);
            onOpenSettings?.();
          }
          throw new Error(errMsg);
        }

        const data = await res.json();
        if (controller.signal.aborted) return;

        // Cache the result under this challenge's key
        feedbackCacheRef.current[cacheKey] = data;

        // IMPORTANT: Only update the UI state if the student is still viewing this challenge!
        if (activeChallengeIdRef.current === targetChallengeId) {
          setResult(data);
          setLoading(false);
          setError('');

          // Auto-expand all failing classes so the student immediately sees the guidance
          const classNames = (data?.feedback?.classes || []).map((c) => c.className);
          setExpandedClasses(new Set(classNames));

          const alerts = data?.alerts || [];
          alerts.forEach((label) => {
            showToast({
              message: alertMessage(label),
              type: label === '90%' ? 'error' : 'warning',
            });
          });
        }
      } catch (err) {
        if (err.name === 'AbortError' || controller.signal.aborted) {
          // Cancelled because the user navigated away or re-analyzed — ignore silently
          return;
        }
        if (activeChallengeIdRef.current === targetChallengeId) {
          setError(err.message || 'Could not load AI guidance.');
          setResult(null);
          setLoading(false);
        }
      } finally {
        if (fetchingChallengeIdRef.current === targetChallengeId) {
          fetchingChallengeIdRef.current = null;
        }
        if (abortControllerRef.current === controller) {
          abortControllerRef.current = null;
        }
      }
    },
    [labId, submissionId, challengeId, challengeName, classData, onOpenSettings, showToast, feedbackCacheRef],
  );

  // Automatic trigger: Run as soon as lab, challenge, and class data are available.
  // Won't fire while details are still loading to ensure we don't send incomplete class data.
  useEffect(() => {
    if (!enabled || !labId || !challengeId || checkingKey || isLoadingDetails) {
      return;
    }
    if (keyConnected) {
      fetchHint(false);
    }
  }, [enabled, labId, submissionId, challengeId, keyConnected, checkingKey, isLoadingDetails, fetchHint]);

  if (!enabled) return null;

  const toggleClass = (name) => {
    setExpandedClasses((prev) => {
      const next = new Set(prev);
      if (next.has(name)) {
        next.delete(name);
      } else {
        next.add(name);
      }
      return next;
    });
  };

  const feedback = result?.feedback;
  const classesList = feedback?.classes || [];
  const usagePercent = result?.token?.usagePercent;

  return (
    <div className="space-y-5">
      {/* Top Header / Bar */}
      <div className="flex flex-wrap items-center justify-between gap-3 pb-4 border-b border-border">
        <div className="flex items-center gap-2.5">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary-light text-primary-text">
            <Sparkle size={18} weight="duotone" aria-hidden="true" />
          </div>
          <div>
            <h3 className="text-sm font-semibold text-foreground">AI Learning Assistant</h3>
            {challengeName && (
              <p className="text-[11px] text-foreground-muted">{challengeName}</p>
            )}
          </div>
        </div>

        <div className="flex items-center gap-4">
          {typeof usagePercent === 'number' && (
            <span className="text-xs text-foreground-muted tabular-nums">
              Token usage: <span className="font-semibold text-foreground">{usagePercent}%</span>
            </span>
          )}
          {keyConnected && !loading && (
            <button
              type="button"
              onClick={() => fetchHint(true)}
              className="inline-flex items-center gap-1.5 text-xs text-foreground-muted hover:text-foreground transition-colors"
              title="Re-analyze challenge with AI"
            >
              <RotateCcw className="h-3.5 w-3.5" aria-hidden="true" />
              <span>Re-analyze</span>
            </button>
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

      {/* No submission yet */}
      {!submissionId && (
        <div className="flex flex-col items-center justify-center py-12 text-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-surface-secondary text-primary">
            <Sparkle size={24} weight="duotone" aria-hidden="true" />
          </div>
          <p className="mt-3 text-sm font-semibold text-foreground">
            Submit your solution to receive AI guidance
          </p>
          <p className="mt-1 max-w-sm text-xs text-foreground-muted">
            Once graded, the AI Assistant will automatically analyze your challenge classes and provide targeted suggestions.
          </p>
        </div>
      )}

      {/* API Key prompt if not connected */}
      {submissionId && !checkingKey && !keyConnected && !loading && (
        <div className="flex flex-col items-center justify-center py-12 text-center rounded-xl bg-surface-secondary/40 border border-border p-6">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary-light text-primary-text">
            <KeyRound className="h-6 w-6" aria-hidden="true" />
          </div>
          <p className="mt-3 text-sm font-semibold text-foreground">
            Connect Gemini API Key
          </p>
          <p className="mt-1 max-w-md text-xs text-foreground-muted">
            Import your free Gemini API key to receive automatic, challenge-specific guidance whenever your solution is graded.
          </p>
          {onOpenSettings && (
            <button
              type="button"
              onClick={onOpenSettings}
              className="mt-4 inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-white shadow-stage-1 hover:bg-primary-hover transition-colors"
            >
              <KeyRound className="h-4 w-4" aria-hidden="true" />
              <span>Import API Key</span>
            </button>
          )}
        </div>
      )}

      {/* Loading state */}
      {loading && (
        <div className="flex flex-col items-center justify-center py-12 text-center">
          <Loader2 className="h-8 w-8 animate-spin text-primary" aria-hidden="true" />
          <p className="mt-3 text-sm font-semibold text-foreground">
            Analyzing {challengeName || 'challenge'} with AI...
          </p>
          <p className="mt-1 text-xs text-foreground-muted">
            Evaluating class declarations and generating guidance
          </p>
        </div>
      )}

      {/* Error state */}
      {error && !loading && (
        <div className="rounded-xl border border-error/30 bg-error-bg/40 p-4 text-sm text-error-text">
          {error}
        </div>
      )}

      {/* Result presentation */}
      {feedback && !loading && (
        <div className="space-y-4">
          {/* Challenge Summary Banner */}
          <div className="rounded-xl border-l-4 border-purple bg-surface-secondary/70 p-4 space-y-1">
            <div className="flex items-center justify-between gap-2">
              <span className="text-xs font-bold uppercase tracking-wider text-purple">
                {challengeName || 'Challenge Overview'}
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
            <p className="text-sm font-medium text-foreground">
              {feedback.challengeSummary}
            </p>
          </div>

          {/* When all classes passed (no failing classes) */}
          {classesList.length === 0 && (
            <div className="flex items-center gap-3 rounded-xl border border-success/30 bg-success-bg/40 p-4 text-success-text">
              <CheckCircle2 className="h-5 w-5 shrink-0 text-success" />
              <div>
                <p className="text-sm font-semibold text-success-text">All classes passed</p>
                <p className="text-xs text-foreground-secondary mt-0.5">
                  No structural or declaration issues detected in this challenge.
                </p>
              </div>
            </div>
          )}

          {/* Failing Classes Accordion (reusing Declaration Test design) */}
          {classesList.length > 0 && (
            <div className="space-y-3">
              {classesList.map((cls) => {
                const isOpen = expandedClasses.has(cls.className);
                const guidanceItems = cls.guidance || [];
                const compileErrorLine = cls.compileError
                  ? firstCompileErrorLine(cls.compileError)
                  : null;

                return (
                  <div
                    key={cls.className}
                    className="overflow-hidden rounded-xl bg-surface-secondary/60"
                  >
                    <button
                      type="button"
                      onClick={() => toggleClass(cls.className)}
                      className="flex w-full items-center justify-between gap-3 bg-surface-secondary px-5 py-4 text-left transition-colors hover:bg-surface-tertiary"
                    >
                      <div className="flex min-w-0 items-start gap-3">
                        <ClassStatusIcon status={cls.status || 'error'} />
                        <div className="min-w-0">
                          <span className="text-[10px] uppercase tracking-wider text-foreground-muted">
                            {cls.classType || 'Class'}
                          </span>
                          <p className="mt-1 font-bold font-mono text-foreground">
                            {cls.className}
                          </p>
                          {compileErrorLine && (
                            <p className="mt-1 break-words font-mono text-[11px] leading-snug text-error-text">
                              {compileErrorLine}
                            </p>
                          )}
                        </div>
                      </div>

                      <div className="flex items-center gap-3">
                        <span className="rounded-md border border-border-subtle bg-surface px-2.5 py-1 text-xs font-semibold text-foreground-secondary">
                          {guidanceItems.length}{' '}
                          {guidanceItems.length === 1 ? 'suggestion' : 'suggestions'}
                        </span>
                        {isOpen ? (
                          <ChevronUp className="h-4 w-4 text-foreground-muted" />
                        ) : (
                          <ChevronDown className="h-4 w-4 text-foreground-muted" />
                        )}
                      </div>
                    </button>

                    {isOpen && (
                      <div className="divide-y divide-border border-t border-border">
                        {guidanceItems.length > 0 ? (
                          guidanceItems.map((item, idx) => (
                            <div key={idx} className="p-4 sm:px-5 space-y-2">
                              <div className="flex items-center gap-2">
                                <div className="h-1.5 w-1.5 rounded-full bg-purple" />
                                <h4 className="text-xs font-bold uppercase tracking-wider text-purple">
                                  {item.title}
                                </h4>
                              </div>
                              <p className="text-sm text-foreground leading-relaxed">
                                {item.explanation}
                              </p>
                              {item.guidingQuestion && (
                                <div className="mt-2 rounded-lg border border-border-subtle bg-surface p-3">
                                  <p className="mb-1 text-[11px] font-semibold text-foreground-muted">
                                    Guiding Question
                                  </p>
                                  <p className="text-xs font-medium text-foreground italic">
                                    {item.guidingQuestion}
                                  </p>
                                </div>
                              )}
                            </div>
                          ))
                        ) : (
                          <div className="p-4 text-xs text-foreground-muted italic">
                            No specific suggestions for this class.
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
