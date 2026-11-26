import { useCallback, useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import {
  AlertCircle,
  Bug,
  Check,
  Coins,
  Flame,
  GitBranch,
  Lock,
  Play,
  RotateCcw,
  ShieldCheck,
  Sparkles,
  Terminal,
  Trophy,
  X,
  Zap,
} from 'lucide-react';
import ModalOverlay from '../ui/ModalOverlay';
import { useToast } from '../ui/Toast';
import { useDialog } from '../../context/DialogContext';
import { apiFetch } from '../../utils/apiFetch';
import { authHeaders } from '../../utils/authHeaders';
import { readFriendlyApiError, toFriendlyError } from '../../utils/apiError';
import { formatNumber } from '../../utils/formatters';
import {
  OOP_BLITZ_DAILY_CAP,
  OOP_BLITZ_MAX_MATCH_POINTS,
  OOP_BLITZ_MAX_RUNS,
  OOP_BLITZ_QUESTIONS_PER_MATCH,
  computeBlitzRunsRemaining,
  formatBlitzTimer,
  tokenizeJavaLine,
} from '../../utils/oopBlitzHelpers';
import { Mascot } from './MascotSprite';
import { mascotArt } from './studentMascots';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

const KIND_META = {
  BUG: {
    label: 'Bug Hunter',
    Icon: Bug,
    pillClass: 'bg-error-bg text-error-text border border-error/40',
    solidPillClass: 'bg-error text-primary-foreground',
    barClass: 'bg-error',
    accentBorder: 'border-error',
    promptClass: 'border-l-4 border-error bg-error-bg/45',
    dotClass: 'bg-error',
  },
  OUTPUT: {
    label: 'Output',
    Icon: Terminal,
    pillClass: 'bg-primary-light text-primary-text border border-primary/40',
    solidPillClass: 'bg-primary text-primary-foreground',
    barClass: 'bg-primary',
    accentBorder: 'border-primary',
    promptClass: 'border-l-4 border-primary bg-primary-light/45',
    dotClass: 'bg-primary',
  },
  UML: {
    label: 'UML / MMD',
    Icon: GitBranch,
    pillClass: 'bg-secondary-light text-secondary-text border border-secondary/40',
    solidPillClass: 'bg-secondary text-primary-foreground',
    barClass: 'bg-secondary',
    accentBorder: 'border-secondary',
    promptClass: 'border-l-4 border-secondary bg-secondary-light/45',
    dotClass: 'bg-secondary',
  },
  MODIFIER: {
    label: 'Modifier',
    Icon: ShieldCheck,
    pillClass: 'bg-gold-bg text-gold-text border border-gold/50',
    solidPillClass: 'bg-gold-bg text-gold-text border border-gold',
    barClass: 'bg-gold',
    accentBorder: 'border-gold',
    promptClass: 'border-l-4 border-gold bg-gold-bg/45',
    dotClass: 'bg-gold',
  },
};

const CHOICE_KEY_CLASSES = [
  'bg-primary-light text-primary-text border-primary/40',
  'bg-secondary-light text-secondary-text border-secondary/40',
  'bg-gold-bg text-gold-text border-gold/50',
  'bg-info-bg text-info-text border-info/40',
];

const CODE_TOKEN_CLASSES = {
  kw: 'text-primary font-semibold',
  tp: 'text-secondary-text font-semibold',
  str: 'text-success-text font-medium',
  num: 'text-gold-text font-semibold',
  cmt: 'text-foreground-muted italic',
  plain: 'text-foreground',
};

function difficultyLabel(raw) {
  if (!raw) return 'Easy';
  const upper = String(raw).toUpperCase();
  return upper.charAt(0) + upper.slice(1).toLowerCase();
}

function difficultyPillClass(raw) {
  const upper = String(raw || 'EASY').toUpperCase();
  if (upper === 'HARD') return 'bg-error-bg text-error-text border border-error/40';
  if (upper === 'MEDIUM') return 'bg-info-bg text-info-text border border-info/40';
  return 'bg-success-bg text-success-text border border-success/40';
}

function computeMatchRank(correctCount, totalQuestions) {
  const ratio = totalQuestions > 0 ? correctCount / totalQuestions : 0;
  if (ratio >= 0.875) {
    return {
      grade: 'S',
      badgeClass: 'bg-gold-bg text-gold-text border-2 border-gold',
      barClass: 'bg-gold',
    };
  }
  if (ratio >= 0.625) {
    return {
      grade: 'A',
      badgeClass: 'bg-secondary-light text-secondary-text border-2 border-secondary',
      barClass: 'bg-secondary',
    };
  }
  if (ratio >= 0.375) {
    return {
      grade: 'B',
      badgeClass: 'bg-primary-light text-primary-text border-2 border-primary',
      barClass: 'bg-primary',
    };
  }
  return {
    grade: 'C',
    badgeClass: 'bg-surface-tertiary text-foreground-secondary border-2 border-border',
    barClass: 'bg-primary',
  };
}

function MascotSpotlightDecor() {
  return (
    <svg
      viewBox="0 0 100 100"
      aria-hidden="true"
      className="pointer-events-none absolute inset-0 h-full w-full"
    >
      <circle cx="50" cy="50" r="44" fill="rgb(var(--gold-rgb))" fillOpacity="0.16" />
      <circle cx="50" cy="50" r="32" fill="rgb(var(--primary-rgb))" fillOpacity="0.14" />
      <polygon
        points="16,18 19,24 25,27 19,30 16,36 13,30 7,27 13,24"
        fill="rgb(var(--gold-rgb))"
        fillOpacity="0.85"
      />
      <polygon
        points="83,16 85,20 89,22 85,24 83,28 81,24 77,22 81,20"
        fill="rgb(var(--secondary-rgb))"
        fillOpacity="0.85"
      />
      <circle cx="84" cy="78" r="3.2" fill="rgb(var(--gold-rgb))" fillOpacity="0.7" />
      <circle cx="15" cy="76" r="2.6" fill="rgb(var(--primary-rgb))" fillOpacity="0.65" />
    </svg>
  );
}

function renderCodeTokens(line) {
  return tokenizeJavaLine(line).map((tok, idx) => (
    <span key={idx} className={CODE_TOKEN_CLASSES[tok.type] || 'text-foreground'}>
      {tok.text}
    </span>
  ));
}

export default function OopBlitzModal({
  open,
  onClose,
  mascotId = 'fox',
  points = 0,
  onPointsChange,
  onStatusSync,
}) {
  const showToast = useToast();
  const dialog = useDialog();
  const reducedMotion = useReducedMotion();

  const [screen, setScreen] = useState('lobby'); // 'lobby' | 'playing' | 'summary'
  const [status, setStatus] = useState(null);
  const [loadingStatus, setLoadingStatus] = useState(false);
  const [busy, setBusy] = useState(false);

  const [sessionMeta, setSessionMeta] = useState(null);
  const [question, setQuestion] = useState(null);
  const [scoreSoFar, setScoreSoFar] = useState(0);
  const [correctCount, setCorrectCount] = useState(0);
  const [combo, setCombo] = useState(0);
  const [remainingMs, setRemainingMs] = useState(0);
  const [pickedIndex, setPickedIndex] = useState(null);
  const [answerResult, setAnswerResult] = useState(null);
  const [summary, setSummary] = useState(null);
  const [summaryFilter, setSummaryFilter] = useState('wrong');

  const mountedRef = useRef(true);
  const rafRef = useRef(null);
  const holdTimerRef = useRef(null);
  const inFlightRef = useRef(false);
  const onPointsChangeRef = useRef(onPointsChange);
  const onStatusSyncRef = useRef(onStatusSync);
  const onCloseRef = useRef(onClose);
  const submitAnswerRef = useRef(null);

  useEffect(() => {
    onPointsChangeRef.current = onPointsChange;
  }, [onPointsChange]);

  useEffect(() => {
    onStatusSyncRef.current = onStatusSync;
  }, [onStatusSync]);

  useEffect(() => {
    onCloseRef.current = onClose;
  }, [onClose]);

  const clearTimers = useCallback(() => {
    if (rafRef.current != null) {
      cancelAnimationFrame(rafRef.current);
      rafRef.current = null;
    }
    if (holdTimerRef.current != null) {
      window.clearTimeout(holdTimerRef.current);
      holdTimerRef.current = null;
    }
  }, []);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      clearTimers();
    };
  }, [clearTimers]);

  const applyStatus = useCallback((nextStatus) => {
    if (!nextStatus) return;
    setStatus(nextStatus);
    onStatusSyncRef.current?.(nextStatus);
    if (typeof nextStatus.currentPoints === 'number') {
      onPointsChangeRef.current?.(nextStatus.currentPoints);
    }
  }, []);

  const fetchStatus = useCallback(async () => {
    setLoadingStatus(true);
    try {
      const response = await apiFetch(`${API_BASE}/api/students/oop-blitz/status`, {
        headers: authHeaders(),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'oop-blitz'));
      }
      const data = await response.json();
      if (!mountedRef.current) return null;
      applyStatus(data);
      return data;
    } catch (err) {
      if (mountedRef.current) {
        showToast(toFriendlyError(err, 'oop-blitz'), 'error');
      }
      return null;
    } finally {
      if (mountedRef.current) {
        setLoadingStatus(false);
      }
    }
  }, [applyStatus, showToast]);

  useEffect(() => {
    if (!open) {
      clearTimers();
      inFlightRef.current = false;
      setBusy(false);
      setScreen('lobby');
      setSessionMeta(null);
      setQuestion(null);
      setPickedIndex(null);
      setAnswerResult(null);
      setSummary(null);
      return undefined;
    }
    setScreen('lobby');
    setPickedIndex(null);
    setAnswerResult(null);
    setSummary(null);
    void fetchStatus();
    return undefined;
  }, [open, clearTimers, fetchStatus]);

  const advanceFromResult = useCallback((res) => {
    clearTimers();
    if (!mountedRef.current || !res) return;
    if (res.finished) {
      const rev = res.summary?.review || [];
      const hasWrong = rev.some((item) => !item.correct);
      setSummary(res.summary || null);
      setSummaryFilter(hasWrong ? 'wrong' : 'all');
      setPickedIndex(null);
      setAnswerResult(null);
      setQuestion(null);
      setSessionMeta(null);
      setScreen('summary');
      return;
    }
    setPickedIndex(null);
    setAnswerResult(null);
    setQuestion(res.nextQuestion || null);
  }, [clearTimers]);

  const submitAnswer = useCallback(async (selectedIndex) => {
    if (
      !open
      || screen !== 'playing'
      || !sessionMeta?.sessionId
      || !question
      || answerResult
      || inFlightRef.current
    ) {
      return;
    }
    inFlightRef.current = true;
    clearTimers();
    setPickedIndex(selectedIndex);
    setBusy(true);

    try {
      const response = await apiFetch(`${API_BASE}/api/students/oop-blitz/answer`, {
        method: 'POST',
        headers: { ...authHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify({
          sessionId: sessionMeta.sessionId,
          questionIndex: question.index,
          selectedIndex,
        }),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'oop-blitz'));
      }
      const data = await response.json();
      if (!mountedRef.current) return;

      setAnswerResult(data);
      setScoreSoFar(data.totalScoreSoFar ?? 0);
      setCorrectCount(data.correctCount ?? 0);
      setCombo(data.combo ?? 0);

      if (data.finished && data.status) {
        applyStatus(data.status);
      }

      const holdMs = Math.max(400, Number(data.feedbackHoldMs) || 1800);
      holdTimerRef.current = window.setTimeout(() => {
        advanceFromResult(data);
      }, holdMs);
    } catch (err) {
      if (mountedRef.current) {
        showToast(toFriendlyError(err, 'oop-blitz'), 'error');
        setScreen('lobby');
        setQuestion(null);
        setAnswerResult(null);
        void fetchStatus();
      }
    } finally {
      inFlightRef.current = false;
      if (mountedRef.current) {
        setBusy(false);
      }
    }
  }, [
    open,
    screen,
    sessionMeta,
    question,
    answerResult,
    clearTimers,
    applyStatus,
    advanceFromResult,
    showToast,
    fetchStatus,
  ]);

  useEffect(() => {
    submitAnswerRef.current = submitAnswer;
  }, [submitAnswer]);

  // Countdown loop per active question
  useEffect(() => {
    if (!open || screen !== 'playing' || !question || answerResult) {
      return undefined;
    }
    const durationMs = Math.max(0, Number(question.remainingMs) || 0);
    setRemainingMs(durationMs);
    if (durationMs <= 0) {
      void submitAnswerRef.current?.(null);
      return undefined;
    }

    const deadline = performance.now() + durationMs;
    const tick = () => {
      if (!mountedRef.current) return;
      const left = Math.max(0, deadline - performance.now());
      setRemainingMs(left);
      if (left <= 0) {
        rafRef.current = null;
        void submitAnswerRef.current?.(null);
        return;
      }
      rafRef.current = requestAnimationFrame(tick);
    };
    rafRef.current = requestAnimationFrame(tick);
    return () => {
      if (rafRef.current != null) {
        cancelAnimationFrame(rafRef.current);
        rafRef.current = null;
      }
    };
  }, [open, screen, question, answerResult]);

  const startMatch = useCallback(async (mode) => {
    if (busy) return;
    clearTimers();
    setBusy(true);
    try {
      const response = await apiFetch(`${API_BASE}/api/students/oop-blitz/start`, {
        method: 'POST',
        headers: { ...authHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify({ mode }),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'oop-blitz'));
      }
      const data = await response.json();
      if (!mountedRef.current) return;

      if (data.status) {
        applyStatus(data.status);
      }
      setSessionMeta({
        sessionId: data.sessionId,
        mode: data.mode,
        totalQuestions: data.totalQuestions ?? OOP_BLITZ_QUESTIONS_PER_MATCH,
      });
      setScoreSoFar(data.scoreSoFar ?? 0);
      setCorrectCount(data.correctCount ?? 0);
      setCombo(data.combo ?? 0);
      setAnswerResult(null);
      setPickedIndex(null);
      setSummary(null);
      setQuestion(data.question);
      setScreen('playing');
    } catch (err) {
      if (mountedRef.current) {
        showToast(toFriendlyError(err, 'oop-blitz'), 'error');
        void fetchStatus();
      }
    } finally {
      if (mountedRef.current) {
        setBusy(false);
      }
    }
  }, [busy, clearTimers, applyStatus, showToast, fetchStatus]);

  const handleAbandonSession = useCallback(async (sessionId, { confirmFirst = true } = {}) => {
    if (!sessionId || busy) return;
    if (confirmFirst) {
      const confirmed = await dialog.confirm({
        title: 'Abandon active run?',
        message: 'Leaving this run forfeits the used daily slot.',
        confirmText: 'Abandon',
        cancelText: 'Keep',
        level: 'warning',
      });
      if (!confirmed || !mountedRef.current) return;
    }

    setBusy(true);
    try {
      const response = await apiFetch(`${API_BASE}/api/students/oop-blitz/abandon`, {
        method: 'POST',
        headers: { ...authHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify({ sessionId }),
      });
      if (!response.ok) {
        throw new Error(await readFriendlyApiError(response, 'oop-blitz'));
      }
      const nextStatus = await response.json();
      if (!mountedRef.current) return;
      applyStatus(nextStatus);
    } catch (err) {
      if (mountedRef.current) {
        showToast(toFriendlyError(err, 'oop-blitz'), 'error');
      }
    } finally {
      if (mountedRef.current) {
        setBusy(false);
      }
    }
  }, [busy, dialog, applyStatus, showToast]);

  const handleQuitPlaying = useCallback(async () => {
    clearTimers();
    if (answerResult) {
      if (answerResult.finished) {
        advanceFromResult(answerResult);
        return;
      }
    }
    const mode = sessionMeta?.mode;
    const sessionId = sessionMeta?.sessionId;
    setPickedIndex(null);
    setAnswerResult(null);
    setQuestion(null);
    setSessionMeta(null);
    if (mode === 'PRACTICE' && sessionId) {
      await handleAbandonSession(sessionId, { confirmFirst: false });
    } else {
      void fetchStatus();
    }
    setScreen('lobby');
  }, [clearTimers, answerResult, advanceFromResult, sessionMeta, handleAbandonSession, fetchStatus]);

  // Keyboard shortcuts during Playing screen + Escape handling
  useEffect(() => {
    if (!open) return undefined;
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        if (screen === 'playing') {
          event.preventDefault();
          void handleQuitPlaying();
        } else if (!busy) {
          event.preventDefault();
          onCloseRef.current?.();
        }
        return;
      }
      if (screen !== 'playing' || !question) return;

      if (answerResult) {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          advanceFromResult(answerResult);
        }
        return;
      }

      if (busy) return;
      const num = Number(event.key);
      if (Number.isInteger(num) && num >= 1 && num <= 9) {
        const idx = num - 1;
        const maxChoice = question.kind === 'BUG'
          ? (question.codeLines?.length ?? 0)
          : (question.options?.length ?? 0);
        if (idx < maxChoice) {
          event.preventDefault();
          void submitAnswer(idx);
        }
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [open, screen, question, answerResult, busy, handleQuitPlaying, advanceFromResult, submitAnswer]);

  if (!open) return null;

  const art = mascotArt(mascotId);
  const runsUsed = status?.rewardedRunsUsed ?? 0;
  const runsAllowed = status?.rewardedRunsAllowed ?? 1;
  const runsMax = status?.rewardedRunsMax ?? OOP_BLITZ_MAX_RUNS;
  const runsLeft = computeBlitzRunsRemaining(status);
  const earnedToday = status?.pointsEarnedToday ?? 0;
  const dailyCap = status?.dailyPointsCap ?? OOP_BLITZ_DAILY_CAP;
  const walletPoints = typeof status?.currentPoints === 'number' ? status.currentPoints : points;
  const activeRewardedId = status?.activeSessionMode === 'REWARDED' ? status?.activeSessionId : null;
  const canPlayRewarded = Boolean(activeRewardedId) || (runsLeft > 0 && earnedToday < dailyCap);

  const isPracticeView =
    (screen === 'playing' && sessionMeta?.mode === 'PRACTICE')
    || (screen === 'summary' && summary?.mode === 'PRACTICE');

  const totalLimitMs = Math.max(1000, (question?.timeLimitSec ?? 15) * 1000);
  const timerRatio = Math.max(0, Math.min(1, remainingMs / totalLimitMs));
  const kindMeta = KIND_META[question?.kind] || KIND_META.OUTPUT;
  const KindIcon = kindMeta.Icon;

  const isTimerDanger = remainingMs <= 3000;
  const isTimerWarn = !isTimerDanger && remainingMs <= 5000;
  const timerBarClass = isTimerDanger
    ? 'bg-error'
    : isTimerWarn
    ? 'bg-warning'
    : kindMeta.barClass;
  const timerPillClass = isTimerDanger
    ? 'bg-error text-primary-foreground'
    : isTimerWarn
    ? 'bg-warning-bg text-warning-text border border-warning/50'
    : 'bg-primary-light text-primary-text border border-primary/30';

  const comboBadgeClass = combo >= 5
    ? 'bg-error text-primary-foreground ring-2 ring-gold'
    : combo >= 3
    ? 'bg-gold-bg text-gold-text border border-gold'
    : 'bg-secondary-light text-secondary-text border border-secondary/40';

  const dailyCapRatio = Math.min(100, Math.round((earnedToday / Math.max(1, dailyCap)) * 100));
  const reviewItems = summary?.review || [];
  const visibleReview = summaryFilter === 'wrong'
    ? reviewItems.filter((item) => !item.correct)
    : reviewItems;
  const summaryTotal = summary?.totalQuestions || OOP_BLITZ_QUESTIONS_PER_MATCH;
  const summaryRatioPct = Math.min(
    100,
    Math.round(((summary?.correctCount ?? 0) / Math.max(1, summaryTotal)) * 100)
  );
  const rankMeta = computeMatchRank(summary?.correctCount ?? 0, summaryTotal);
  const totalQCount = sessionMeta?.totalQuestions ?? OOP_BLITZ_QUESTIONS_PER_MATCH;

  return (
    <ModalOverlay
      onBackdropClick={() => {
        if (screen !== 'playing' && !busy) {
          onClose();
        }
      }}
    >
      <motion.div
        role="dialog"
        aria-modal="true"
        aria-label="OOP Blitz"
        onClick={(e) => e.stopPropagation()}
        initial={reducedMotion ? false : { opacity: 0, scale: 0.97, y: 8 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        transition={{ duration: 0.18, ease: 'easeOut' }}
        className="w-full max-w-2xl overflow-hidden rounded-2xl border border-border bg-surface text-foreground shadow-stage-3"
      >
        {/* Arcade Modal Banner Header */}
        <div className="flex items-center justify-between gap-2 border-b-4 border-gold/85 bg-navy px-5 py-3 text-navy-foreground">
          <div className="flex items-center gap-2.5">
            <span className="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-gold-bg text-gold ring-2 ring-gold/70">
              <Zap className="h-5 w-5 fill-current" aria-hidden="true" />
            </span>
            <h2 className="text-base font-extrabold tracking-tight">OOP Blitz</h2>
            {isPracticeView && (
              <span className="rounded-full border border-secondary/50 bg-secondary-light px-2.5 py-0.5 text-xs font-bold text-secondary-text">
                Practice
              </span>
            )}
          </div>

          <div className="flex items-center gap-2">
            <span className="inline-flex h-8 items-center gap-1.5 rounded-full border border-gold/60 bg-gold-bg px-3 text-xs font-extrabold text-gold-text tabular-nums">
              <Coins className="h-3.5 w-3.5 text-gold" aria-hidden="true" />
              <span>{formatNumber(walletPoints)} pts</span>
            </span>
            <button
              type="button"
              onClick={() => {
                if (screen === 'playing') {
                  void handleQuitPlaying();
                } else if (!busy) {
                  onClose();
                }
              }}
              aria-label="Close OOP Blitz"
              className="inline-flex h-8 w-8 cursor-pointer items-center justify-center rounded-lg border border-navy-foreground/20 bg-navy-hover text-navy-foreground transition-colors hover:border-gold hover:text-gold"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>

        {/* ==================== LOBBY ==================== */}
        {screen === 'lobby' && (
          <div className="space-y-4 p-5">
            {/* 3 Color-Coded Arcade KPI Tiles (flush single-layer) */}
            <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-3">
              <div className="flex items-center gap-3 rounded-xl border-l-4 border-primary bg-primary-light/45 px-3.5 py-2.5">
                <span className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-primary text-primary-foreground">
                  <Zap className="h-4 w-4 fill-current" aria-hidden="true" />
                </span>
                <div className="min-w-0">
                  <div className="text-[11px] font-semibold uppercase tracking-wide text-primary-text">
                    Rewarded Runs
                  </div>
                  <div className="flex items-baseline gap-1.5">
                    <span className="text-lg font-extrabold tabular-nums">
                      {runsUsed} / {runsAllowed}
                    </span>
                    <span className="text-xs font-bold text-primary-text">
                      {activeRewardedId
                        ? 'Active'
                        : runsLeft > 0
                        ? `${runsLeft} ready`
                        : runsAllowed < runsMax
                        ? 'Submit Lab'
                        : 'Done'}
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-3 rounded-xl border-l-4 border-gold bg-gold-bg/55 px-3.5 py-2.5">
                <span className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-navy text-gold">
                  <Coins className="h-4 w-4" aria-hidden="true" />
                </span>
                <div className="min-w-0 flex-1">
                  <div className="text-[11px] font-semibold uppercase tracking-wide text-gold-text">
                    Earned Today
                  </div>
                  <div className="flex items-baseline gap-1">
                    <span className="text-lg font-extrabold tabular-nums">{formatNumber(earnedToday)}</span>
                    <span className="text-xs font-semibold text-gold-text tabular-nums">/ {dailyCap}</span>
                  </div>
                  <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-surface/75">
                    <div
                      className="h-full rounded-full bg-gold transition-all duration-300"
                      style={{ width: `${dailyCapRatio}%` }}
                    />
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-3 rounded-xl border-l-4 border-secondary bg-secondary-light/45 px-3.5 py-2.5">
                <span className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-secondary text-primary-foreground">
                  <Trophy className="h-4 w-4" aria-hidden="true" />
                </span>
                <div className="min-w-0">
                  <div className="text-[11px] font-semibold uppercase tracking-wide text-secondary-text">
                    Today Best
                  </div>
                  <div className="flex items-baseline gap-1">
                    <span className="text-lg font-extrabold tabular-nums">
                      {status?.todayBestScore != null ? formatNumber(status.todayBestScore) : '—'}
                    </span>
                    {status?.todayBestScore != null && (
                      <span className="text-xs font-bold text-secondary-text">pts</span>
                    )}
                  </div>
                </div>
              </div>
            </div>

            {/* Mascot Arena Spotlight + Mode Tags + Tactile Arcade Actions */}
            <div className="flex flex-col justify-between gap-4 rounded-xl border border-border bg-navy-light/55 p-4 sm:flex-row sm:items-center">
              <div className="flex items-center gap-4">
                <motion.div
                  animate={reducedMotion ? undefined : { y: [0, -3, 0] }}
                  transition={{ repeat: Infinity, duration: 2.8, ease: 'easeInOut' }}
                  className="relative flex h-20 w-20 shrink-0 items-center justify-center"
                >
                  <MascotSpotlightDecor />
                  <Mascot
                    directions={art.directions}
                    reactions={art.reactions}
                    tier={art.tier}
                    size={64}
                    interactive
                    label={art.name}
                    className="relative z-10"
                  />
                </motion.div>

                <div className="space-y-2">
                  <div className="flex flex-wrap items-center gap-1.5 text-xs">
                    <span className="rounded-md bg-navy px-2 py-0.5 font-bold text-navy-foreground">
                      {OOP_BLITZ_QUESTIONS_PER_MATCH} Questions
                    </span>
                    <span className="rounded-md border border-info/40 bg-info-bg px-2 py-0.5 font-bold text-info-text">
                      15–20s / Q
                    </span>
                    <span className="rounded-md border border-gold/50 bg-gold-bg px-2 py-0.5 font-bold text-gold-text">
                      Max {OOP_BLITZ_MAX_MATCH_POINTS} pts
                    </span>
                  </div>
                  <div className="flex flex-wrap items-center gap-1.5 text-xs font-bold">
                    {Object.entries(KIND_META).map(([key, meta]) => {
                      const CatIcon = meta.Icon;
                      return (
                        <span
                          key={key}
                          className={`inline-flex items-center gap-1 rounded-md px-2 py-0.5 ${meta.pillClass}`}
                        >
                          <CatIcon className="h-3 w-3" aria-hidden="true" />
                          <span>{meta.label}</span>
                        </span>
                      );
                    })}
                  </div>
                </div>
              </div>

              <div className="flex shrink-0 flex-wrap items-center gap-2">
                {activeRewardedId && (
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => void handleAbandonSession(activeRewardedId)}
                    className="inline-flex h-10 cursor-pointer items-center gap-1 rounded-xl border border-error/40 bg-error-bg px-3 text-xs font-bold text-error-text transition-colors hover:bg-error hover:text-primary-foreground disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    <RotateCcw className="h-3.5 w-3.5" aria-hidden="true" />
                    <span>Abandon</span>
                  </button>
                )}
                <button
                  type="button"
                  disabled={busy || loadingStatus || Boolean(activeRewardedId)}
                  onClick={() => void startMatch('PRACTICE')}
                  className="inline-flex h-10 cursor-pointer items-center rounded-xl border border-secondary/50 border-b-4 bg-secondary-light px-3.5 text-xs font-extrabold text-secondary-text transition-all hover:bg-secondary hover:text-primary-foreground active:translate-y-[2px] active:border-b-2 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  Practice (0 pts)
                </button>
                <button
                  type="button"
                  disabled={busy || loadingStatus || !canPlayRewarded}
                  onClick={() => void startMatch('REWARDED')}
                  className="inline-flex h-10 cursor-pointer items-center gap-1.5 rounded-xl border-b-4 border-primary-active bg-primary px-5 text-xs font-extrabold text-primary-foreground transition-all hover:bg-primary-hover active:translate-y-[2px] active:border-b-2 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  <Play className="h-3.5 w-3.5 fill-current" aria-hidden="true" />
                  <span>
                    {activeRewardedId
                      ? `Resume Q${(status?.activeQuestionIndex ?? 0) + 1}/${OOP_BLITZ_QUESTIONS_PER_MATCH}`
                      : earnedToday >= dailyCap
                      ? 'Daily Cap Reached'
                      : runsLeft > 0
                      ? 'Play'
                      : 'Locked'}
                  </span>
                </button>
              </div>
            </div>

            {/* Visual Daily Slots Track */}
            <div className="flex flex-wrap items-center justify-between gap-2 pt-0.5 text-xs">
              <span className="inline-flex items-center gap-1.5 font-bold text-foreground-secondary">
                <Sparkles className="h-3.5 w-3.5 text-gold" aria-hidden="true" />
                <span>Daily Slots</span>
              </span>
              <div className="flex flex-wrap items-center gap-2">
                {Array.from({ length: runsMax }, (_, idx) => {
                  const slotNumber = idx + 1;
                  if (idx < runsUsed) {
                    const isActiveSlot = Boolean(activeRewardedId) && idx === runsUsed - 1;
                    return (
                      <span
                        key={slotNumber}
                        className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 font-bold ${
                          isActiveSlot
                            ? 'border-gold bg-gold-bg text-gold-text'
                            : 'border-success/50 bg-success-bg text-success-text'
                        }`}
                      >
                        <Check className="h-3 w-3 stroke-[3]" aria-hidden="true" />
                        <span>{isActiveSlot ? `Run ${slotNumber} · Live` : `Run ${slotNumber}`}</span>
                      </span>
                    );
                  }
                  if (idx < runsAllowed) {
                    return (
                      <span
                        key={slotNumber}
                        className="inline-flex items-center gap-1.5 rounded-full border border-primary/50 bg-primary-light px-3 py-1 font-bold text-primary-text"
                      >
                        <span
                          className={`h-2 w-2 rounded-full bg-primary ${reducedMotion ? '' : 'animate-pulse'}`}
                          aria-hidden="true"
                        />
                        <span>Run {slotNumber} Ready</span>
                      </span>
                    );
                  }
                  return (
                    <span
                      key={slotNumber}
                      className="inline-flex items-center gap-1 rounded-full border border-border bg-surface-secondary px-3 py-1 font-semibold text-foreground-muted"
                    >
                      <Lock className="h-3 w-3" aria-hidden="true" />
                      <span>Lab Submit #{idx}</span>
                    </span>
                  );
                })}
              </div>
            </div>
          </div>
        )}

        {/* ==================== PLAYING ==================== */}
        {screen === 'playing' && question && (
          <AnimatePresence mode="wait">
            <motion.div
              key={question.index}
              initial={reducedMotion ? false : { opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={reducedMotion ? undefined : { opacity: 0, y: -6 }}
              transition={{ duration: 0.16, ease: 'easeOut' }}
              className="space-y-3 p-5"
            >
              {/* Colorful Arcade HUD Row */}
              <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
                <div className="flex flex-wrap items-center gap-1.5">
                  <span className="rounded-md bg-navy px-2.5 py-0.5 text-xs font-extrabold tabular-nums text-navy-foreground">
                    Q {question.index + 1}/{totalQCount}
                  </span>
                  <span className={`inline-flex items-center gap-1 rounded-md px-2 py-0.5 font-bold ${kindMeta.pillClass}`}>
                    <KindIcon className="h-3 w-3" aria-hidden="true" />
                    <span>{kindMeta.label}</span>
                  </span>
                  <span className={`rounded-md px-2 py-0.5 font-bold ${difficultyPillClass(question.difficulty)}`}>
                    {difficultyLabel(question.difficulty)}
                  </span>

                  {/* Question Step Dots */}
                  <div className="hidden items-center gap-1 pl-1 sm:flex" aria-hidden="true">
                    {Array.from({ length: totalQCount }, (_, dotIdx) => {
                      const isCurrent = dotIdx === question.index;
                      const isPast = dotIdx < question.index;
                      return (
                        <span
                          key={dotIdx}
                          className={`rounded-full transition-all ${
                            isCurrent
                              ? `h-2.5 w-4 ${kindMeta.dotClass}`
                              : isPast
                              ? 'h-2 w-2 bg-primary'
                              : 'h-2 w-2 bg-surface-tertiary'
                          }`}
                        />
                      );
                    })}
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  {combo >= 2 && (
                    <motion.span
                      key={combo}
                      initial={reducedMotion ? false : { scale: 0.75 }}
                      animate={{ scale: 1 }}
                      transition={{ type: 'spring', stiffness: 450, damping: 18 }}
                      className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 font-extrabold tabular-nums ${comboBadgeClass}`}
                    >
                      <Flame className="h-3.5 w-3.5 fill-current" aria-hidden="true" />
                      <span>×{combo}</span>
                    </motion.span>
                  )}
                  <span className="rounded-full border border-gold/50 bg-gold-bg px-2.5 py-0.5 font-extrabold tabular-nums text-gold-text">
                    {sessionMeta?.mode === 'REWARDED'
                      ? `+${scoreSoFar} pts`
                      : `${correctCount}/${question.index + (answerResult ? 1 : 0)} ok`}
                  </span>
                  <span className={`min-w-[54px] rounded-full px-2.5 py-0.5 text-center font-mono font-extrabold tabular-nums ${timerPillClass}`}>
                    {formatBlitzTimer(remainingMs)}
                  </span>
                  <button
                    type="button"
                    onClick={() => void handleQuitPlaying()}
                    className="cursor-pointer rounded-lg border border-error/30 bg-error-bg/50 px-2 py-0.5 text-xs font-bold text-error-text transition-colors hover:bg-error hover:text-primary-foreground"
                  >
                    Quit
                  </button>
                </div>
              </div>

              {/* Vivid Countdown Bar */}
              <div className="h-2.5 w-full overflow-hidden rounded-full bg-surface-tertiary">
                <div
                  className={`h-full rounded-full ${timerBarClass} ${reducedMotion ? '' : 'transition-none'}`}
                  style={{ width: `${timerRatio * 100}%` }}
                />
              </div>

              {/* Category-Colored Prompt Callout */}
              <div className={`rounded-r-xl px-3.5 py-2 text-sm font-bold leading-snug text-foreground ${kindMeta.promptClass}`}>
                {question.prompt}
              </div>

              {/* IDE Code Window (single bordered shell) */}
              <div className="overflow-hidden rounded-xl border border-border bg-surface-secondary">
                <div className={`flex items-center justify-between border-b-2 ${kindMeta.accentBorder} bg-navy px-3.5 py-1.5 text-[11px] font-semibold text-navy-foreground`}>
                  <div className="flex items-center gap-2">
                    <span className="inline-flex items-center gap-1" aria-hidden="true">
                      <span className="h-2.5 w-2.5 rounded-full bg-error" />
                      <span className="h-2.5 w-2.5 rounded-full bg-gold" />
                      <span className="h-2.5 w-2.5 rounded-full bg-success" />
                    </span>
                    <span className="font-mono">BlitzChallenge.java</span>
                  </div>
                  <span className="text-gold">
                    {question.kind === 'BUG'
                      ? 'Tap buggy line'
                      : `${question.codeLines?.length ?? 0} lines`}
                  </span>
                </div>

                <div className="py-2 font-mono text-xs leading-relaxed">
                  {(question.codeLines || []).map((line, lineIdx) => {
                    const isBugMode = question.kind === 'BUG';
                    const isCorrectLine = Boolean(answerResult) && lineIdx === answerResult.correctIndex;
                    const isWrongPick = Boolean(answerResult)
                      && !answerResult.correct
                      && !answerResult.timedOut
                      && isBugMode
                      && pickedIndex === lineIdx;

                    if (isBugMode) {
                      const lineClass = answerResult
                        ? isCorrectLine
                          ? 'bg-success-bg border-l-4 border-success font-semibold'
                          : isWrongPick
                          ? 'bg-error-bg border-l-4 border-error'
                          : 'border-l-4 border-transparent opacity-60'
                        : 'border-l-4 border-transparent hover:border-primary hover:bg-primary-light/65 cursor-pointer group';

                      return (
                        <motion.button
                          key={lineIdx}
                          type="button"
                          disabled={busy || Boolean(answerResult)}
                          animate={isWrongPick && !reducedMotion ? { x: [0, -5, 5, -3, 3, 0] } : undefined}
                          transition={{ duration: 0.3 }}
                          onClick={() => {
                            if (!answerResult && !busy) {
                              void submitAnswer(lineIdx);
                            }
                          }}
                          className={`flex w-full items-center gap-3 px-3 py-1 text-left transition-colors ${lineClass}`}
                        >
                          <span
                            className={`inline-flex h-5 w-5 shrink-0 select-none items-center justify-center rounded font-bold tabular-nums ${
                              isCorrectLine
                                ? 'bg-success text-primary-foreground'
                                : isWrongPick
                                ? 'bg-error text-primary-foreground'
                                : 'bg-surface text-foreground-secondary group-hover:bg-primary group-hover:text-primary-foreground'
                            }`}
                          >
                            {lineIdx + 1}
                          </span>
                          <span className="min-w-0 flex-1 overflow-x-auto whitespace-pre">
                            {renderCodeTokens(line)}
                          </span>
                        </motion.button>
                      );
                    }

                    return (
                      <div key={lineIdx} className="flex items-start gap-3 px-3 py-0.5">
                        <span className="w-5 shrink-0 select-none text-right font-semibold text-foreground-muted tabular-nums">
                          {lineIdx + 1}
                        </span>
                        <span className="min-w-0 flex-1 overflow-x-auto whitespace-pre">
                          {renderCodeTokens(line)}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* 2x2 Tactile Arcade Choice Tiles */}
              {question.kind !== 'BUG' && (
                <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
                  {(question.options || []).map((option, optIdx) => {
                    const isCorrectOpt = Boolean(answerResult) && optIdx === answerResult.correctIndex;
                    const isWrongOpt = Boolean(answerResult)
                      && !answerResult.correct
                      && !answerResult.timedOut
                      && pickedIndex === optIdx;

                    let btnClass =
                      'border-border bg-surface hover:border-primary hover:bg-primary-light/35 cursor-pointer active:translate-y-[2px] active:border-b-2';
                    let keyCapClass = CHOICE_KEY_CLASSES[optIdx % CHOICE_KEY_CLASSES.length];

                    if (answerResult) {
                      if (isCorrectOpt) {
                        btnClass = 'border-success bg-success-bg text-success-text font-bold';
                        keyCapClass = 'bg-success text-primary-foreground border-success';
                      } else if (isWrongOpt) {
                        btnClass = 'border-error bg-error-bg text-error-text font-semibold';
                        keyCapClass = 'bg-error text-primary-foreground border-error';
                      } else {
                        btnClass = 'border-border-subtle bg-surface opacity-45';
                      }
                    }

                    return (
                      <motion.button
                        key={optIdx}
                        type="button"
                        disabled={busy || Boolean(answerResult)}
                        whileHover={!answerResult && !busy && !reducedMotion ? { y: -1 } : undefined}
                        animate={
                          isWrongOpt && !reducedMotion
                            ? { x: [0, -5, 5, -3, 3, 0] }
                            : isCorrectOpt && !reducedMotion
                            ? { scale: [1, 1.02, 1] }
                            : undefined
                        }
                        transition={{ duration: 0.25 }}
                        onClick={() => {
                          if (!answerResult && !busy) {
                            void submitAnswer(optIdx);
                          }
                        }}
                        className={`flex items-center gap-2.5 rounded-xl border-2 border-b-4 px-3 py-2.5 text-left text-xs font-semibold transition-colors ${btnClass}`}
                      >
                        <span
                          className={`inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-lg border font-mono text-xs font-extrabold ${keyCapClass}`}
                        >
                          {optIdx + 1}
                        </span>
                        <span className="min-w-0 flex-1 break-words font-mono">{option}</span>
                      </motion.button>
                    );
                  })}
                </div>
              )}

              {/* Bottom Mascot Cheer & Instant Feedback Stage */}
              <div
                className={`flex min-h-[54px] items-center justify-between gap-3 rounded-xl px-3 py-2 transition-colors ${
                  answerResult
                    ? answerResult.correct
                      ? 'bg-success-bg/75'
                      : 'bg-error-bg/75'
                    : 'bg-surface-secondary'
                }`}
              >
                <div className="flex min-w-0 items-center gap-3">
                  <motion.div
                    animate={
                      !answerResult || reducedMotion
                        ? undefined
                        : answerResult.correct
                        ? { y: [0, -6, 0], scale: [1, 1.08, 1] }
                        : { x: [0, -4, 4, -2, 2, 0] }
                    }
                    transition={{ duration: 0.35 }}
                    className="relative flex h-12 w-12 shrink-0 items-center justify-center"
                  >
                    <Mascot
                      directions={art.directions}
                      reactions={art.reactions}
                      tier={art.tier}
                      size={44}
                      interactive={false}
                      label={art.name}
                    />
                  </motion.div>

                  <div className="min-w-0">
                    {answerResult ? (
                      <>
                        <div className="flex flex-wrap items-center gap-1.5 text-xs font-extrabold">
                          {answerResult.correct ? (
                            <>
                              <motion.span
                                initial={reducedMotion ? false : { scale: 0.8 }}
                                animate={{ scale: 1 }}
                                className="inline-flex items-center gap-1 rounded-full bg-success px-2.5 py-0.5 text-primary-foreground"
                              >
                                <Check className="h-3.5 w-3.5 stroke-[3]" aria-hidden="true" />
                                <span>
                                  {sessionMeta?.mode === 'REWARDED'
                                    ? `+${answerResult.questionPts} pts`
                                    : 'Correct'}
                                </span>
                              </motion.span>
                              {sessionMeta?.mode === 'REWARDED' && answerResult.comboBonus > 0 && (
                                <span className="rounded-full border border-gold bg-gold-bg px-2 py-0.5 text-[11px] text-gold-text">
                                  Combo +{answerResult.comboBonus}
                                </span>
                              )}
                              {sessionMeta?.mode === 'REWARDED' && answerResult.speedBonus > 0 && (
                                <span className="rounded-full border border-info/50 bg-info-bg px-2 py-0.5 text-[11px] text-info-text">
                                  Fast +{answerResult.speedBonus}
                                </span>
                              )}
                            </>
                          ) : (
                            <span className="inline-flex items-center gap-1 rounded-full bg-error px-2.5 py-0.5 text-primary-foreground">
                              <AlertCircle className="h-3.5 w-3.5" aria-hidden="true" />
                              <span>
                                {answerResult.timedOut ? 'Time up' : 'Incorrect'} · Combo reset
                              </span>
                            </span>
                          )}
                        </div>
                        <div className="mt-1 truncate text-xs font-medium text-foreground" title={answerResult.explanation}>
                          {answerResult.explanation}
                        </div>
                      </>
                    ) : (
                      <div className="text-xs font-semibold text-foreground-secondary">
                        {question.kind === 'BUG'
                          ? `Pick buggy line (keys 1–${question.codeLines?.length ?? 0})`
                          : 'Pick option (keys 1–4)'}
                      </div>
                    )}
                  </div>
                </div>

                {answerResult && (
                  <button
                    type="button"
                    onClick={() => advanceFromResult(answerResult)}
                    className="inline-flex h-9 shrink-0 cursor-pointer items-center rounded-xl border-b-4 border-primary-active bg-primary px-4 text-xs font-extrabold text-primary-foreground transition-all hover:bg-primary-hover active:translate-y-[2px] active:border-b-2"
                  >
                    {answerResult.finished ? 'Finish ↵' : 'Next ↵'}
                  </button>
                )}
              </div>
            </motion.div>
          </AnimatePresence>
        )}

        {/* ==================== SUMMARY ==================== */}
        {screen === 'summary' && summary && (
          <motion.div
            initial={reducedMotion ? false : { opacity: 0, scale: 0.98 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.2, ease: 'easeOut' }}
            className="space-y-3.5 p-5"
          >
            {/* Celebration Hero Card */}
            <div className="rounded-xl border border-border bg-navy-light/55 p-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-3.5">
                  <div className="relative flex h-16 w-16 shrink-0 items-center justify-center">
                    <MascotSpotlightDecor />
                    <Mascot
                      directions={art.directions}
                      reactions={art.reactions}
                      tier={art.tier}
                      size={54}
                      interactive={false}
                      label={art.name}
                      className="relative z-10"
                    />
                  </div>
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <span
                        className={`inline-flex h-8 w-8 items-center justify-center rounded-xl text-base font-black ${rankMeta.badgeClass}`}
                        aria-label={`Rank ${rankMeta.grade}`}
                      >
                        {rankMeta.grade}
                      </span>
                      <span className="text-2xl font-black tabular-nums text-primary">
                        {summary.mode === 'PRACTICE'
                          ? 'Practice'
                          : `+${formatNumber(summary.creditedPoints)} pts`}
                      </span>
                      {summary.cappedByDailyLimit && (
                        <span className="rounded-full border border-warning/50 bg-warning-bg px-2.5 py-0.5 text-xs font-bold text-warning-text">
                          Daily cap reached
                        </span>
                      )}
                    </div>
                    <div className="mt-1.5 flex flex-wrap items-center gap-2 text-xs font-bold tabular-nums">
                      <span className="rounded-md bg-success-bg px-2 py-0.5 text-success-text">
                        {summary.correctCount}/{summaryTotal} Correct
                      </span>
                      <span className="inline-flex items-center gap-1 rounded-md bg-gold-bg px-2 py-0.5 text-gold-text">
                        <Flame className="h-3 w-3" aria-hidden="true" />
                        <span>×{summary.maxCombo} Combo</span>
                      </span>
                      <span className="rounded-md bg-info-bg px-2 py-0.5 text-info-text">
                        +{summary.speedBonusTotal} Fast
                      </span>
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-1.5">
                  <button
                    type="button"
                    onClick={() => setSummaryFilter('wrong')}
                    className={`h-8 cursor-pointer rounded-lg border px-3 text-xs font-bold transition-colors ${
                      summaryFilter === 'wrong'
                        ? 'border-primary bg-primary text-primary-foreground'
                        : 'border-border bg-surface text-foreground-secondary hover:border-primary'
                    }`}
                  >
                    Wrong
                  </button>
                  <button
                    type="button"
                    onClick={() => setSummaryFilter('all')}
                    className={`h-8 cursor-pointer rounded-lg border px-3 text-xs font-bold transition-colors ${
                      summaryFilter === 'all'
                        ? 'border-primary bg-primary text-primary-foreground'
                        : 'border-border bg-surface text-foreground-secondary hover:border-primary'
                    }`}
                  >
                    All {summaryTotal}
                  </button>
                </div>
              </div>

              {/* Accuracy Progress Meter */}
              <div className="mt-3 h-2 w-full overflow-hidden rounded-full bg-surface-tertiary">
                <div
                  className={`h-full rounded-full ${rankMeta.barClass} transition-all duration-500`}
                  style={{ width: `${summaryRatioPct}%` }}
                />
              </div>
            </div>

            {/* Review List */}
            <div className="max-h-[210px] divide-y divide-border-subtle overflow-y-auto rounded-xl border border-border bg-surface">
              {visibleReview.length === 0 ? (
                <div className="flex items-center justify-center gap-2 bg-success-bg/50 px-4 py-5 text-xs font-extrabold text-success-text">
                  <Trophy className="h-4 w-4 text-gold" aria-hidden="true" />
                  <span>Perfect {summaryTotal}/{summaryTotal} — zero mistakes!</span>
                </div>
              ) : (
                visibleReview.map((item) => {
                  const meta = KIND_META[item.kind];
                  const itemKind = meta?.label || item.kind;
                  const itemPillClass = meta?.pillClass || 'bg-surface-secondary text-foreground-secondary';
                  return (
                    <div
                      key={item.index}
                      className="flex items-start justify-between gap-3 px-3.5 py-2.5 text-xs"
                    >
                      <div className="min-w-0 space-y-1">
                        <div className="flex flex-wrap items-center gap-1.5">
                          <span
                            className={`rounded-md px-1.5 py-0.5 font-extrabold ${
                              item.correct
                                ? 'bg-success-bg text-success-text'
                                : 'bg-error-bg text-error-text'
                            }`}
                          >
                            {item.correct ? '✓' : '✕'} Q{item.index + 1}
                          </span>
                          <span className={`rounded-md px-1.5 py-0.5 font-bold ${itemPillClass}`}>
                            {itemKind}
                          </span>
                          {!item.correct && (
                            <>
                              <span className="font-mono font-semibold text-error-text">{item.chosenLabel}</span>
                              <span className="text-foreground-muted">→</span>
                              <span className="font-mono font-bold text-success-text">
                                {item.correctLabel}
                              </span>
                            </>
                          )}
                        </div>
                        <div className="text-foreground-secondary">{item.explanation}</div>
                      </div>
                      <span
                        className={`shrink-0 rounded-full px-2 py-0.5 font-extrabold tabular-nums ${
                          item.correct
                            ? 'bg-gold-bg text-gold-text'
                            : 'bg-surface-secondary text-foreground-muted'
                        }`}
                      >
                        {summary.mode === 'REWARDED'
                          ? `+${item.pointsEarned}`
                          : item.correct
                          ? 'OK'
                          : '0'}
                      </span>
                    </div>
                  );
                })
              )}
            </div>

            <div className="flex items-center justify-between gap-2 pt-0.5">
              <button
                type="button"
                onClick={() => {
                  setScreen('lobby');
                  void fetchStatus();
                }}
                className="h-9 cursor-pointer rounded-xl border border-border bg-surface px-3.5 text-xs font-bold text-foreground transition-colors hover:border-primary hover:bg-surface-secondary"
              >
                Lobby
              </button>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => void startMatch('PRACTICE')}
                  className="h-9 cursor-pointer rounded-xl border border-secondary/50 border-b-4 bg-secondary-light px-3.5 text-xs font-extrabold text-secondary-text transition-all hover:bg-secondary hover:text-primary-foreground active:translate-y-[2px] active:border-b-2 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  Practice
                </button>
                <button
                  type="button"
                  disabled={busy || runsLeft <= 0 || earnedToday >= dailyCap}
                  onClick={() => void startMatch('REWARDED')}
                  className="h-9 cursor-pointer rounded-xl border-b-4 border-primary-active bg-primary px-4 text-xs font-extrabold text-primary-foreground transition-all hover:bg-primary-hover active:translate-y-[2px] active:border-b-2 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  {runsLeft > 0 && earnedToday < dailyCap
                    ? `Play Again (${runsLeft} left)`
                    : 'No Runs Left'}
                </button>
              </div>
            </div>
          </motion.div>
        )}
      </motion.div>
    </ModalOverlay>
  );
}
