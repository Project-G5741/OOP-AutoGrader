// StudentUI.jsx - Không còn dữ liệu cứng, hoàn toàn nhận từ props
import React, { useState, useEffect, useMemo } from 'react';
import {
  CheckCircle2,
  XCircle,
  Lock,
  ChevronDown,
  ChevronUp,
  ClipboardList,
  AlertCircle,
  GitMerge,
} from 'lucide-react';
import DropZone from '../ui/DropZone';
import { ScorePill, ScoreSectionHeader, hasScoreToShow, isPillarNotApplicable } from '../ui/ScorePill';
import { Separator } from '../ui/separator';
import { SidebarInset, SidebarProvider, SidebarTrigger } from '../ui/sidebar';
import { formatMmdRelationType } from '../../utils/formatters';
import { formatLabDeadlineMeta } from '../../theme/statusClasses';
import StudentLabSidebar from './StudentLabSidebar';
import StudentNotificationBell from './StudentNotificationBell';

const TAB_LABELS = { mmd: 'MMD', class: 'Declaration Test', testcase: 'Operation Test' };
const TAB_ORDER = Object.keys(TAB_LABELS);

/** Strip trailing line endings from captured stdout for display only; grading is unchanged. */
function formatIoDisplay(value) {
  if (value == null || value === '') return '—';
  return String(value).replace(/(?:\\r\\n|\\n|\\r|[\r\n])+$/g, '');
}

// Component con dùng chung
function Tick({ ok }) {
  return ok ? <CheckCircle2 className="w-4 h-4 text-success flex-shrink-0" /> : <XCircle className="w-4 h-4 text-error flex-shrink-0" />;
}

function StatusBadge({ status }) {
  const colors = {
    success: 'bg-success-bg text-success-text',
    error: 'bg-error-bg text-error-text',
    warning: 'bg-warning-bg text-warning-text',
    info: 'bg-info-bg text-info-text',
  };
  return (
    <span className={`text-xs px-2 py-0.5 rounded-full ${colors[status] || colors.info}`}>
      {status?.toUpperCase() || 'UNKNOWN'}
    </span>
  );
}

function relationTypeStyle(type) {
  const normalized = String(type ?? '').toLowerCase();
  if (normalized.includes('extends')) {
    return 'bg-info-bg text-info-text';
  }
  if (normalized.includes('implements')) {
    return 'bg-warning-bg text-warning-text';
  }
  if (normalized.includes('uses') || normalized.includes('depends')) {
    return 'bg-secondary-light text-secondary-text';
  }
  if (normalized.includes('associates') || normalized.includes('aggregates')) {
    return 'bg-primary-light text-primary-text';
  }
  return 'bg-surface-secondary text-foreground-secondary';
}

function scoreColor(s) {
  if (s === null || s === undefined) return 'text-foreground-muted';
  if (s >= 90) return 'text-success';
  if (s >= 75) return 'text-info';
  if (s >= 60) return 'text-warning';
  return 'text-error';
}

// Helper: has a value that isn't null/undefined
const hasValue = (v) => v !== null && v !== undefined;

// Session challenge scores are keyed by challenge id, but ids may arrive as
// either the raw id or its string form depending on the source map, so check both.
function hasSessionChallengeScore(challengeScores, challengeId) {
  if (!challengeScores || challengeId == null) return false;
  return Object.hasOwn(challengeScores, challengeId)
    || Object.hasOwn(challengeScores, String(challengeId));
}

function sessionChallengeScore(challengeScores, challengeId) {
  if (!challengeScores || challengeId == null) return undefined;
  if (Object.hasOwn(challengeScores, challengeId)) return challengeScores[challengeId];
  return challengeScores[String(challengeId)];
}

function bundleScore(bundle, key, fallback) {
  const raw = bundle?.scores?.[key];
  if (raw == null) return fallback;
  const pct = Math.round(Number(raw));
  return { ok: pct, total: 100, pct };
}

function formatScopeDetail(scope, detail) {
  const normalizedScope = scope?.trim();
  const normalizedDetail = detail?.trim() ?? '';
  if (!normalizedScope || normalizedScope === '-') {
    return normalizedDetail;
  }
  return normalizedDetail ? `${normalizedScope} · ${normalizedDetail}` : normalizedScope;
}

export default function StudentUI({
  user,
  // Dữ liệu labs
  labs = [],
  labSummariesById = {},
  selectedLabId = null,
  onLabChange = () => {},

  // Dữ liệu challenges/problems
  challenges = [],
  selectedChallengeId = null,
  onChallengeChange = () => {},

  // Dữ liệu chi tiết cho challenge đã chọn
  mmdData = [],
  classData = [],
  classNormalizationNotice = null,
  testCases = [],

  // Dữ liệu thống kê
  stats = {
    currentGrade: null,
    totalSubmissions: null,
    latestSubmission: null,
  },
  nextAttemptNumber = 1,

  // Upload handler
  onUploadComplete = () => {},

  // Loading/Error states
  isLoading = false,
  isLoadingDetails = false,
  isRefreshingResults = false,
  resultsRevealed = false,
  sessionChallengeScores = {},
  sessionChallengeBundles = {},
  sessionOverallScore = null,
  error = null,
}) {
  const [activeTab, setActiveTab] = useState('mmd');
  const [expandedTC, setExpandedTC] = useState(null);
  const [expandedClassName, setExpandedClassName] = useState(null);

  // Reset expanded test case khi đổi challenge
  useEffect(() => {
    setExpandedTC(null);
    setExpandedClassName(null);
  }, [selectedChallengeId]);

  const currentBundle = selectedChallengeId ? sessionChallengeBundles[selectedChallengeId] : null;
  const effectiveClassNormalizationNotice =
    classNormalizationNotice
    ?? currentBundle?.normalizationNotice
    ?? currentBundle?.normalization_notice
    ?? null;
  const visibleTabs = useMemo(
    () => {
      if (!resultsRevealed) return [];
      return TAB_ORDER.filter((t) => !isPillarNotApplicable(currentBundle, t));
    },
    [resultsRevealed, currentBundle],
  );

  // If the active tab's pillar is inapplicable for the newly selected challenge (hidden from
  // the tab bar), fall back to the first pillar that's still visible.
  useEffect(() => {
    if (visibleTabs.length && !visibleTabs.includes(activeTab)) {
      setActiveTab(visibleTabs[0]);
    }
  }, [visibleTabs, activeTab]);

  const tabCls = (t) => `px-5 py-2.5 text-sm font-medium border-b-2 transition-colors ${activeTab === t ? 'border-primary text-primary' : 'border-transparent text-foreground-muted hover:text-foreground-secondary'}`;

  // Render loading state
  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-4 border-primary border-t-transparent rounded-full animate-spin" />
          <p className="text-foreground-muted">Loading data...</p>
        </div>
      </div>
    );
  }

  // Render error state
  if (error) {
    return (
      <div className="bg-error-bg rounded-xl p-6 text-center">
        <AlertCircle className="w-12 h-12 text-error mx-auto mb-3" />
        <p className="text-error-text font-medium">{error}</p>
        <p className="text-sm text-error mt-1">Please try refreshing the page.</p>
      </div>
    );
  }

  // Xác định challenge hiện tại
  const currentChallenge = challenges.find(c => c.id === selectedChallengeId) || challenges[0];

  const relationData = mmdData.flatMap((cls) => cls.relations ?? []);
  const relations = relationData;

  const relationScore = {
    ok: relations.filter((r) => r.ok).length,
    total: relations.length,
    pct: relations.length ? Math.round((relations.filter((r) => r.ok).length / relations.length) * 100) : 100,
  };

  const mmdScore = bundleScore(currentBundle, 'mmd', {
    ok: mmdData.reduce((sum, cls) => sum + (cls.attributes?.filter((a) => a.ok).length || 0), 0),
    total: mmdData.reduce((sum, cls) => sum + (cls.attributes?.length || 0), 0),
    pct: (() => {
      const total = mmdData.reduce((sum, cls) => sum + (cls.attributes?.length || 0), 0);
      const ok = mmdData.reduce((sum, cls) => sum + (cls.attributes?.filter((a) => a.ok).length || 0), 0);
      return total ? Math.round((ok / total) * 100) : 0;
    })(),
  });

  const classScore = bundleScore(currentBundle, 'class', {
    ok: classData.reduce((sum, cls) => {
      const fieldsOk = cls.fields?.filter((f) => f.ok).length || 0;
      const ctorsOk = cls.constructors?.filter((c) => c.ok).length || 0;
      const methodsOk = cls.methods?.filter((m) => m.ok).length || 0;
      return sum + fieldsOk + ctorsOk + methodsOk;
    }, 0),
    total: classData.reduce((sum, cls) => {
      const fieldCount = cls.fields?.length || 0;
      const ctorCount = cls.constructors?.length || 0;
      const methodCount = cls.methods?.length || 0;
      return sum + fieldCount + ctorCount + methodCount;
    }, 0),
    pct: (() => {
      const total = classData.reduce((sum, cls) => sum + (cls.fields?.length || 0) + (cls.constructors?.length || 0) + (cls.methods?.length || 0), 0);
      const ok = classData.reduce((sum, cls) => sum + (cls.fields?.filter((f) => f.ok).length || 0) + (cls.constructors?.filter((c) => c.ok).length || 0) + (cls.methods?.filter((m) => m.ok).length || 0), 0);
      return total ? Math.round((ok / total) * 100) : 0;
    })(),
  });

  const testCasesData = testCases || [];
  const visibleTestcases = testCasesData.filter((tc) => !tc.isHidden);
  const hiddenTestcases = testCasesData.filter((tc) => tc.isHidden);
  const testScore = bundleScore(currentBundle, 'testcase', { ok: 0, total: 0, pct: 0 });

  const selectedLab = labs.find((lab) => String(lab.id) === String(selectedLabId)) ?? labs[0];

  return (
    <SidebarProvider>
      <StudentLabSidebar
        labs={labs}
        selectedLabId={selectedLabId}
        onSelectLab={onLabChange}
      />
      <SidebarInset>
        <header className="flex h-16 shrink-0 items-center gap-2 border-b border-border bg-surface px-4">
          <SidebarTrigger className="-ml-1" />
          <Separator orientation="vertical" className="mr-2 h-4" />
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-semibold text-foreground">
              {selectedLab?.name ?? 'Select a lab'}
            </p>
            {selectedLab?.deadlineDate && (
              <p className="truncate text-xs text-foreground-muted">
                {formatLabDeadlineMeta(selectedLab)}
              </p>
            )}
          </div>
          <StudentNotificationBell
            labs={labs}
            labSummariesById={labSummariesById}
            onSelectLab={onLabChange}
          />
        </header>

        <div className="flex flex-1 flex-col gap-4 p-4 sm:p-6">
        {/* Upload box */}
        <div>
          <DropZone
            title="Drop your project files here"
            buttonText="Select Project"
            labId={selectedLabId}
            attemptNumber={nextAttemptNumber}
            authToken={user?.accessToken}
            onUploadComplete={onUploadComplete}
          />
        </div>

        {/* Stats row — attempts/latest from DB; grade only after session upload */}
        <div className="grid grid-cols-1 gap-4 mb-6 lg:grid-cols-3">
          <div className="rounded-xl bg-gradient-to-br from-success-bg to-surface-secondary p-5 shadow-lg shadow-black/10">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-9 h-9 bg-success/15 rounded-lg flex items-center justify-center">
                <CheckCircle2 className="w-5 h-5 text-success-text" />
              </div>
              <span className="text-success-text/90 text-sm">Current Grade</span>
            </div>
            {resultsRevealed && hasValue(sessionOverallScore) ? (
              <div className="flex items-baseline gap-1">
                <span className="text-4xl text-success-text font-bold">{sessionOverallScore}</span>
                <span className="text-success-text/70 text-sm">/100</span>
              </div>
            ) : (
              <span className="text-4xl text-success-text font-bold">--/--</span>
            )}
          </div>
          <div className="bg-surface rounded-xl p-5 shadow-sm dark:shadow-none">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-9 h-9 bg-info-bg rounded-lg flex items-center justify-center">
                <ClipboardList className="w-5 h-5 text-info" />
              </div>
              <span className="text-foreground-muted text-sm">Total Submissions</span>
            </div>
            <span className="text-foreground text-3xl font-bold">
              {hasValue(stats.totalSubmissions) ? stats.totalSubmissions : '--/--'}
            </span>
          </div>
          <div className="bg-surface rounded-xl p-5 shadow-sm dark:shadow-none">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-9 h-9 bg-primary-light rounded-lg flex items-center justify-center">
                <CheckCircle2 className="w-5 h-5 text-primary" />
              </div>
              <span className="text-foreground-muted text-sm">Latest Submission</span>
            </div>
            <span className="text-foreground text-lg font-semibold">
              {hasValue(stats.latestSubmission) ? stats.latestSubmission : '--/--'}
            </span>
          </div>
        </div>

        {/* Overview — sidebar + tab shell always visible; scores and detail fetches after session upload */}
        <div className="relative flex flex-col gap-4 min-h-[560px] lg:flex-row">
          {isRefreshingResults && (
            <div
              className="absolute inset-0 z-10 flex items-center justify-center rounded-xl bg-surface/60"
              aria-busy="true"
              aria-label="Refreshing results"
            >
              <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
            </div>
          )}
          {/* Challenges sidebar - Từ backend */}
          <div className="w-full lg:w-[30%] flex-shrink-0 bg-surface rounded-xl shadow-sm dark:shadow-none overflow-hidden flex flex-col">
            <div className="px-4 py-3 border-b border-border">
              <h2 className="text-sm font-semibold text-foreground-secondary">Challenges</h2>
            </div>
            <ul className="flex-1 overflow-y-auto divide-y divide-border">
              {challenges.length === 0 ? (
                <li className="px-4 py-8 text-center text-foreground-muted text-sm">
                  No challenges available
                </li>
              ) : (
                challenges.map((ch) => {
                  const chHasScore = resultsRevealed && hasSessionChallengeScore(sessionChallengeScores, ch.id);
                  const bundle = sessionChallengeBundles[ch.id];
                  const bundleTotal = bundle?.scores?.total;
                  const chScore = chHasScore
                    ? (bundleTotal != null
                      ? Math.round(Number(bundleTotal))
                      : sessionChallengeScore(sessionChallengeScores, ch.id))
                    : null;
                  return (
                  <li key={ch.id}>
                    <button
                      onClick={() => onChallengeChange(ch.id)}
                      className={`w-full text-left px-4 py-3.5 transition-colors flex items-center justify-between group ${
                        selectedChallengeId === ch.id
                          ? 'bg-primary-light'
                          : 'hover:bg-surface-secondary hover:bg-surface-secondary'
                      }`}
                    >
                      <div>
                        <p className={`text-sm font-medium ${
                          selectedChallengeId === ch.id
                            ? 'text-primary-text'
                            : 'text-foreground-secondary'
                        }`}>
                          {ch.name}
                        </p>
                        {chHasScore && hasValue(chScore) ? (
                          <p className={`text-xs mt-0.5 font-semibold ${scoreColor(chScore)}`}>
                            {chScore} / 100
                          </p>
                        ) : resultsRevealed ? (
                          <p className="text-xs mt-0.5 text-foreground-disabled">
                            Not submitted
                          </p>
                        ) : null}
                      </div>
                      {selectedChallengeId === ch.id && (
                        <span className="w-1.5 h-1.5 rounded-full bg-primary flex-shrink-0" />
                      )}
                    </button>
                  </li>
                  );
                })
              )}
            </ul>
          </div>

          {/* Main content - Từ backend */}
          <div className="flex-1 bg-surface rounded-xl shadow-sm dark:shadow-none overflow-hidden flex flex-col">
            {resultsRevealed ? (
              <>
            {/* Tabs */}
            <div className="flex border-b border-border px-2">
              {visibleTabs.map((t) => (
                <button
                  key={t}
                  onClick={() => setActiveTab(t)}
                  className={tabCls(t)}
                >
                  {TAB_LABELS[t]}
                </button>
              ))}
              <div className="flex-1 flex items-center justify-end pr-4">
                <span className="text-xs text-foreground-disabled">
                  {currentChallenge?.name || 'No challenge selected'}
                </span>
              </div>
            </div>

            {/* Tab content */}
            <div className="flex-1 overflow-auto p-5">
              {isLoadingDetails ? (
                <div className="flex items-center justify-center h-48">
                  <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
                </div>
              ) : (
                <>
              {activeTab === 'mmd' && (
                <>
                  <ScoreSectionHeader
                    title="MMD Score"
                    score={mmdScore}
                    showPill={resultsRevealed && hasScoreToShow(mmdScore, currentBundle, 'mmd')}
                  />

                  <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3 mb-4">
                    {mmdData.length > 0 ? mmdData.map((cls) => (
                      <div key={cls.name} className="border border-border rounded-xl overflow-hidden">
                        <div className="bg-primary-light px-4 py-2 border-b border-border">
                          <p className="text-sm font-bold text-primary-text font-mono">{cls.name}</p>
                        </div>
                        <ul className="divide-y divide-border">
                          {cls.attributes?.map((a, i) => (
                            <li key={i} className={`flex items-start gap-3 px-4 py-2 text-xs ${i % 2 === 0 ? '' : 'bg-surface-secondary bg-surface-secondary/50'}`}>
                              <span className={`font-mono flex-1 min-w-0 break-words ${a.type === 'field' ? 'text-info-text' : a.type === 'method' ? 'text-success-text' : 'text-warning-text'}`}>
                                {a.name}
                              </span>
                              <div className="flex-shrink-0 pt-0.5">
                                <Tick ok={a.ok} />
                              </div>
                            </li>
                          ))}
                        </ul>
                      </div>
                    )) : (
                      <div className="rounded-xl border border-dashed border-border-subtle p-8 text-center text-foreground-muted">
                        No MMD class data is available.
                      </div>
                    )}
                  </div>

                  {resultsRevealed && relations.length > 0 && (
                    <div className="overflow-hidden rounded-xl border border-border">
                      <div className="flex items-center justify-between border-b border-border bg-surface-secondary px-4 py-3">
                        <div className="flex items-center gap-2 text-foreground-muted">
                          <GitMerge className="w-4 h-4" />
                          <span className="text-xs font-semibold uppercase tracking-[0.2em]">Relations</span>
                        </div>
                        <ScorePill ok={relationScore.ok} total={relationScore.total} pct={relationScore.pct} />
                      </div>
                      <div className="grid grid-cols-4 items-center gap-4 border-b border-border px-4 py-3 text-[11px] uppercase tracking-[0.25em] text-foreground-muted">
                        <span className="font-semibold">From</span>
                        <div className="flex justify-center"><span className="font-semibold">Relation</span></div>
                        <span className="font-semibold">To</span>
                        <span className="font-semibold text-center">Status</span>
                      </div>
                      <div className="divide-y divide-border">
                        {relations.map((r, index) => (
                          <div key={index}>
                            <div className="grid grid-cols-4 items-center gap-4 px-4 py-3 text-sm text-foreground-secondary">
                              <span className="font-mono text-primary">{r.from}</span>
                              <div className="flex justify-center">
                                <span className={`inline-flex items-center justify-center rounded-full px-3 py-1 text-[11px] font-semibold ${relationTypeStyle(r.relType)}`}>
                                  {formatMmdRelationType(r.relType)}
                                </span>
                              </div>
                              <span className="font-mono text-primary">{r.to}</span>
                              <div className="flex justify-center">
                                {r.ok ? (
                                  <CheckCircle2 className="h-5 w-5 text-success" />
                                ) : (
                                  <XCircle className="h-5 w-5 text-error" />
                                )}
                              </div>
                            </div>
                            {!r.ok && r.error && (
                              <div className="grid grid-cols-4 px-4 py-2 text-xs font-mono text-error-text bg-error-bg">
                                <div className="col-span-4 text-left">{r.from} → {r.to}: {r.error}</div>
                              </div>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </>
              )}

              {activeTab === 'class' && (
                <>
                  <ScoreSectionHeader
                    title="Declaration Score"
                    score={classScore}
                    showPill={resultsRevealed && hasScoreToShow(classScore, currentBundle, 'class')}
                  />

                  {effectiveClassNormalizationNotice && (
                    <div className="mb-4 rounded-lg border border-warning/40 bg-warning-bg px-4 py-3 text-sm text-warning-text">
                      {effectiveClassNormalizationNotice}
                    </div>
                  )}

                  {classData.length > 0 ? (
                    <div className="space-y-3">
                      {classData.map((cls) => {
                        const fields = cls.fields ?? [];
                        const constructors = cls.constructors ?? [];
                        const methods = cls.methods ?? [];
                        const isOpen = expandedClassName === cls.name;
                        const allItems = [...fields, ...constructors, ...methods];
                        const passCount = allItems.filter((item) => item.ok).length;
                        const clsPct = allItems.length ? Math.round((passCount / allItems.length) * 100) : 100;

                        return (
                          <div key={`${cls.name}-${cls.type}`} className="overflow-hidden rounded-xl border border-border bg-surface shadow-sm">
                            <button
                              type="button"
                              onClick={() => setExpandedClassName((current) => (current === cls.name ? null : cls.name))}
                              className="flex w-full items-center justify-between gap-3 bg-surface-secondary px-5 py-4 text-left transition hover:bg-surface-secondary bg-surface-secondary hover:bg-surface-tertiary"
                            >
                              <div>
                                <span className="text-[10px] uppercase tracking-wider text-foreground-muted">{cls.type || 'Class'}</span>
                                <p className="mt-1 font-bold font-mono text-foreground">{cls.name}</p>
                              </div>
                              <div className="flex items-center gap-3">
                                <ScorePill ok={passCount} total={allItems.length} pct={clsPct} />
                                {isOpen ? <ChevronUp className="h-4 w-4 text-foreground-muted" /> : <ChevronDown className="h-4 w-4 text-foreground-muted" />}
                              </div>
                            </button>

                            {isOpen && (
                              <div className="divide-y divide-border border-t border-border">
                                {fields.length > 0 && (
                                  <div className="px-5 py-3">
                                    <p className="mb-2 text-[10px] font-bold uppercase tracking-wider text-info">Fields</p>
                                    <div className="space-y-2">
                                      {fields.map((f, i) => (
                                        <div key={i} className={`flex items-center justify-between rounded-lg px-3 py-2 ${f.ok ? 'bg-surface-secondary bg-background/40' : 'bg-error-bg'}`}>
                                          <div>
                                            <p className="text-xs font-mono font-semibold text-info-text">{f.name}: {f.dataType}</p>
                                            <p className="mt-0.5 text-[10px] text-foreground-muted">{f.scope || '—'}</p>
                                          </div>
                                          <Tick ok={f.ok} />
                                        </div>
                                      ))}
                                    </div>
                                  </div>
                                )}

                                {constructors.length > 0 && (
                                  <div className="px-5 py-3">
                                    <p className="mb-2 text-[10px] font-bold uppercase tracking-wider text-warning">Constructors</p>
                                    <div className="space-y-2">
                                      {constructors.map((c, i) => (
                                        <div key={i} className={`flex items-center justify-between rounded-lg px-3 py-2 ${c.ok ? 'bg-surface-secondary bg-background/40' : 'bg-error-bg'}`}>
                                          <div>
                                            <p className="text-xs font-mono font-semibold text-warning-text">{c.name}({c.params})</p>
                                            <p className="mt-0.5 text-[10px] text-foreground-muted">{c.scope || '—'}</p>
                                          </div>
                                          <Tick ok={c.ok} />
                                        </div>
                                      ))}
                                    </div>
                                  </div>
                                )}

                                {methods.length > 0 && (
                                  <div className="px-5 py-3">
                                    <p className="mb-2 text-[10px] font-bold uppercase tracking-wider text-success">Methods</p>
                                    <div className="space-y-2">
                                      {methods.map((m, i) => (
                                        <div key={i} className={`flex items-center justify-between rounded-lg px-3 py-2 ${m.ok ? 'bg-surface-secondary bg-background/40' : 'bg-error-bg'}`}>
                                          <div>
                                            <p className="text-xs font-mono font-semibold text-success-text">{m.name}(): {m.returnType}</p>
                                            <p className="mt-0.5 text-[10px] text-foreground-muted">{m.scope || '—'}</p>
                                          </div>
                                          <Tick ok={m.ok} />
                                        </div>
                                      ))}
                                    </div>
                                  </div>
                                )}
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  ) : (
                    <div className="rounded-xl border border-dashed border-border-subtle p-8 text-center text-foreground-muted">
                      No class detail data is available.
                    </div>
                  )}
                </>
              )}

              {activeTab === 'testcase' && (
                <>
                  <ScoreSectionHeader
                    title="I/O Score"
                    score={testScore}
                    showPill={resultsRevealed && hasScoreToShow(testScore, currentBundle, 'testcase')}
                  />

                  {testCasesData.length > 0 ? (
                    <div className="space-y-6">
                      {visibleTestcases.length > 0 && (
                        <div>
                          <p className="text-xs font-bold text-foreground-muted uppercase tracking-wider mb-3">
                            Example Testcases
                          </p>
                          <div className="space-y-2">
                            {visibleTestcases.map((tc) => (
                              <div key={tc.id} className="border border-border rounded-xl overflow-hidden">
                                <button
                                  type="button"
                                  onClick={() => setExpandedTC(expandedTC === tc.id ? null : tc.id)}
                                  className="w-full flex items-center justify-between px-4 py-3 hover:bg-surface-secondary hover:bg-surface-secondary transition-colors text-left"
                                >
                                  <div className="flex items-center gap-3">
                                    <Tick ok={tc.passed} />
                                    <span className="text-sm font-medium text-foreground-secondary">{tc.name}</span>
                                  </div>
                                  <div className="flex items-center gap-2 flex-shrink-0">
                                    <span className={`text-xs font-semibold ${tc.passed ? 'text-success' : 'text-error'}`}>
                                      {tc.passed ? 'PASS' : 'FAIL'}
                                    </span>
                                    <span className="text-xs text-foreground-disabled">Click to view details</span>
                                    {expandedTC === tc.id ? (
                                      <ChevronUp className="w-4 h-4 text-foreground-muted" />
                                    ) : (
                                      <ChevronDown className="w-4 h-4 text-foreground-muted" />
                                    )}
                                  </div>
                                </button>

                                {expandedTC === tc.id && (
                                  <div className="border-t border-border">
                                    <div className="grid grid-cols-1 md:grid-cols-3 divide-y md:divide-y-0 md:divide-x divide-border">
                                      <div className="p-4">
                                        <p className="text-[10px] font-bold text-foreground-muted uppercase tracking-wider mb-2">Input</p>
                                        <pre className="bg-surface-secondary bg-surface-secondary rounded-lg p-3 text-xs font-mono text-foreground-secondary whitespace-pre-wrap">{tc.input || '—'}</pre>
                                      </div>
                                      <div className="p-4">
                                        <p className="text-[10px] font-bold text-success uppercase tracking-wider mb-2">Expected Output</p>
                                        <pre className="bg-success-bg rounded-lg p-3 text-xs font-mono text-success-text whitespace-pre-wrap">{tc.expectedOutput || '—'}</pre>
                                      </div>
                                      <div className="p-4">
                                        <div className="flex items-center justify-between mb-2">
                                          <p className="text-[10px] font-bold text-foreground-muted uppercase tracking-wider">Your Output</p>
                                          <Tick ok={tc.passed} />
                                        </div>
                                        <pre className={`rounded-lg p-3 text-xs font-mono whitespace-pre-wrap ${
                                            tc.passed
                                            ? 'bg-success-bg text-success-text'
                                            : 'bg-error-bg text-error-text'
                                        }`}>{formatIoDisplay(tc.actualOutput)}</pre>
                                      </div>
                                    </div>
                                    {tc.assertions?.length > 1 && (
                                      <div className="border-t border-border p-4 space-y-3">
                                        {tc.assertions.slice(1).map((assertion, index) => (
                                          <div key={`${tc.id}-assertion-${index}`} className="grid grid-cols-1 md:grid-cols-2 gap-3">
                                            <div>
                                              <p className="text-[10px] font-bold text-success uppercase tracking-wider mb-1">
                                                {assertion.kind} Expected
                                              </p>
                                              <pre className="bg-success-bg rounded-lg p-2 text-xs font-mono text-success-text whitespace-pre-wrap">
                                                {assertion.expected_output ?? assertion.expectedOutput ?? '—'}
                                              </pre>
                                            </div>
                                            <div>
                                              <p className="text-[10px] font-bold text-foreground-muted uppercase tracking-wider mb-1">
                                                {assertion.kind} Your Output
                                              </p>
                                              <pre className={`rounded-lg p-2 text-xs font-mono whitespace-pre-wrap ${
                                                assertion.result === 'PASS'
                                                  ? 'bg-success-bg text-success-text'
                                                  : 'bg-error-bg text-error-text'
                                              }`}>
                                                {formatIoDisplay(assertion.actual_output ?? assertion.actualOutput)}
                                              </pre>
                                            </div>
                                          </div>
                                        ))}
                                      </div>
                                    )}
                                  </div>
                                )}
                              </div>
                            ))}
                          </div>
                        </div>
                      )}

                      {hiddenTestcases.length > 0 && (
                        <div>
                          <p className="text-xs font-bold text-foreground-muted uppercase tracking-wider mb-3">
                            Other Testcases
                            <span className="ml-2 font-normal text-foreground-disabled normal-case">(input &amp; output hidden)</span>
                          </p>
                          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                            {hiddenTestcases.map((tc) => (
                              <div
                                key={tc.id}
                                className={`flex items-center gap-3 px-4 py-3 rounded-xl ${
                                  tc.passed
                                    ? 'bg-success-bg'
                                    : 'bg-error-bg'
                                }`}
                              >
                                <Tick ok={tc.passed} />
                                <span className="text-sm text-foreground-secondary font-medium">{tc.name}</span>
                                <div className="ml-auto flex items-center gap-2">
                                  <Lock className="w-3.5 h-3.5 text-foreground-disabled" />
                                  <span className={`text-xs font-bold ${tc.passed ? 'text-success-text' : 'text-error-text'}`}>
                                    {tc.passed ? 'PASS' : 'FAIL'}
                                  </span>
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  ) : (
                    <div className="rounded-xl border border-dashed border-border-subtle p-8 text-center text-foreground-muted">
                      No testcase detail data is available.
                    </div>
                  )}
                </>
              )}
                </>
              )}
            </div>
              </>
            ) : (
              <div className="flex flex-1 flex-col items-center justify-center p-8 text-center">
                <p className="text-sm font-medium text-foreground-secondary">
                  {currentChallenge?.name || 'Challenge'}
                </p>
                <p className="mt-2 max-w-sm text-sm text-foreground-muted">
                  Submit your project to view MMD, Declaration, and Operation results.
                </p>
              </div>
            )}
          </div>
        </div>
        </div>
      </SidebarInset>
    </SidebarProvider>
  );
}