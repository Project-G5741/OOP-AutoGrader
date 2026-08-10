// StudentUI.jsx - Không còn dữ liệu cứng, hoàn toàn nhận từ props
import React, { useState, useEffect } from 'react';
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

// Component con dùng chung
function Tick({ ok }) {
  return ok ? <CheckCircle2 className="w-4 h-4 text-green-500 flex-shrink-0" /> : <XCircle className="w-4 h-4 text-red-500 flex-shrink-0" />;
}

function StatusBadge({ status }) {
  const colors = {
    success: 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300',
    error: 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300',
    warning: 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-300',
    info: 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300',
  };
  return (
    <span className={`text-xs px-2 py-0.5 rounded-full ${colors[status] || colors.info}`}>
      {status?.toUpperCase() || 'UNKNOWN'}
    </span>
  );
}

function ScorePill({ ok, total, pct }) {
  const color = pct >= 80
    ? 'bg-green-500/15 text-green-600 dark:text-green-400 border-green-300 dark:border-green-700'
    : pct >= 60
      ? 'bg-yellow-500/15 text-yellow-600 dark:text-yellow-400 border-yellow-300 dark:border-yellow-700'
      : 'bg-red-500/15 text-red-600 dark:text-red-400 border-red-300 dark:border-red-700';

  return (
    <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full border text-xs font-semibold ${color}`}>
      {ok}/{total} · {pct}%
    </span>
  );
}

function relationTypeStyle(type) {
  const normalized = String(type ?? '').toLowerCase();
  if (normalized.includes('extends')) {
    return 'bg-blue-500/10 text-blue-500 dark:bg-blue-500/15 dark:text-blue-300';
  }
  if (normalized.includes('implements')) {
    return 'bg-orange-500/10 text-orange-500 dark:bg-orange-500/15 dark:text-orange-300';
  }
  if (normalized.includes('uses') || normalized.includes('depends')) {
    return 'bg-teal-500/10 text-teal-500 dark:bg-teal-500/15 dark:text-teal-300';
  }
  if (normalized.includes('associates') || normalized.includes('aggregates')) {
    return 'bg-purple-500/10 text-purple-500 dark:bg-purple-500/15 dark:text-purple-300';
  }
  return 'bg-slate-100 text-slate-700 dark:bg-gray-800 dark:text-slate-200';
}

function scoreColor(s) {
  if (s === null || s === undefined) return 'text-gray-400';
  if (s >= 90) return 'text-green-500';
  if (s >= 75) return 'text-blue-500';
  if (s >= 60) return 'text-yellow-500';
  return 'text-red-500';
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
  selectedLabId = null,
  onLabChange = () => {},

  // Dữ liệu challenges/problems
  challenges = [],
  selectedChallengeId = null,
  onChallengeChange = () => {},

  // Dữ liệu chi tiết cho challenge đã chọn
  mmdData = [],
  classData = [],
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

  const tabCls = (t) => `px-5 py-2.5 text-sm font-medium border-b-2 transition-colors ${activeTab === t ? 'border-purple-500 text-purple-500 dark:text-purple-400' : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200'}`;

  // Render loading state
  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-4 border-purple-500 border-t-transparent rounded-full animate-spin" />
          <p className="text-gray-500 dark:text-gray-400">Loading data...</p>
        </div>
      </div>
    );
  }

  // Render error state
  if (error) {
    return (
      <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-xl p-6 text-center">
        <AlertCircle className="w-12 h-12 text-red-500 mx-auto mb-3" />
        <p className="text-red-700 dark:text-red-300 font-medium">{error}</p>
        <p className="text-sm text-red-500 dark:text-red-400 mt-1">Please try refreshing the page.</p>
      </div>
    );
  }

  // Xác định challenge hiện tại
  const currentChallenge = challenges.find(c => c.id === selectedChallengeId) || challenges[0];
  const currentLab = labs.find(l => l.id === selectedLabId) || labs[0];
  const currentBundle = selectedChallengeId ? sessionChallengeBundles[selectedChallengeId] : null;

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
  const scoredTestcases = (() => {
    const hidden = testCasesData.filter((tc) => !tc.isExample);
    return hidden.length > 0 ? hidden : testCasesData;
  })();
  const testScore = bundleScore(currentBundle, 'testcase', {
    ok: scoredTestcases.filter((tc) => tc.passed).length,
    total: scoredTestcases.length,
    pct: (() => {
      const ok = scoredTestcases.filter((tc) => tc.passed).length;
      return scoredTestcases.length ? Math.round((ok / scoredTestcases.length) * 100) : 0;
    })(),
  });

  return (
    <div className="min-h-screen bg-[#F5F5F7] dark:bg-[#1a1f2e] transition-colors">
      <div className="w-full max-w-full mx-auto px-4 py-8 sm:px-6 lg:px-8">
        {/* Lab selector - Từ backend */}
        <div className="bg-white dark:bg-[#1e2530] rounded-xl p-4 shadow-sm dark:shadow-none mb-4">
          <label className="block text-gray-500 dark:text-gray-400 text-xs font-semibold uppercase tracking-wider mb-2">
            Select Lab
          </label>
          <select
            value={selectedLabId || ''}
            onChange={(e) => onLabChange(e.target.value)}
            className="w-full bg-gray-50 dark:bg-[#151b24] text-gray-900 dark:text-white px-4 py-2.5 rounded-lg border border-gray-200 dark:border-gray-700 focus:border-purple-500 focus:outline-none text-sm"
            disabled={labs.length === 0}
          >
            {labs.length === 0 ? (
              <option value="">No labs available</option>
            ) : (
              labs.map((lab) => (
                <option key={lab.id} value={lab.id}>
                  {lab.name}
                </option>
              ))
            )}
          </select>
        </div>

        {/* Upload box */}
        <div className="mb-4">
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
          <div className="bg-gradient-to-br from-green-500 to-green-600 rounded-xl p-5 shadow-lg shadow-green-500/20">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-9 h-9 bg-white/20 rounded-lg flex items-center justify-center">
                <CheckCircle2 className="w-5 h-5 text-white" />
              </div>
              <span className="text-white/90 text-sm">Current Grade</span>
            </div>
            {resultsRevealed && hasValue(sessionOverallScore) ? (
              <div className="flex items-baseline gap-1">
                <span className="text-4xl text-white font-bold">{sessionOverallScore}</span>
                <span className="text-white/70 text-sm">/100</span>
              </div>
            ) : (
              <span className="text-4xl text-white font-bold">--/--</span>
            )}
          </div>
          <div className="bg-white dark:bg-[#1e2530] rounded-xl p-5 shadow-sm dark:shadow-none">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-9 h-9 bg-blue-500/15 rounded-lg flex items-center justify-center">
                <ClipboardList className="w-5 h-5 text-blue-500" />
              </div>
              <span className="text-gray-500 dark:text-gray-400 text-sm">Total Submissions</span>
            </div>
            <span className="text-gray-900 dark:text-white text-3xl font-bold">
              {hasValue(stats.totalSubmissions) ? stats.totalSubmissions : '--/--'}
            </span>
          </div>
          <div className="bg-white dark:bg-[#1e2530] rounded-xl p-5 shadow-sm dark:shadow-none">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-9 h-9 bg-purple-500/15 rounded-lg flex items-center justify-center">
                <CheckCircle2 className="w-5 h-5 text-purple-500" />
              </div>
              <span className="text-gray-500 dark:text-gray-400 text-sm">Latest Submission</span>
            </div>
            <span className="text-gray-900 dark:text-white text-lg font-semibold">
              {hasValue(stats.latestSubmission) ? stats.latestSubmission : '--/--'}
            </span>
          </div>
        </div>

        {/* Overview — sidebar + tab shell always visible; scores and detail fetches after session upload */}
        <div className="relative flex flex-col gap-4 min-h-[560px] lg:flex-row">
          {isRefreshingResults && (
            <div
              className="absolute inset-0 z-10 flex items-center justify-center rounded-xl bg-white/60 dark:bg-[#1a1f2e]/60"
              aria-busy="true"
              aria-label="Refreshing results"
            >
              <div className="w-8 h-8 border-4 border-purple-500 border-t-transparent rounded-full animate-spin" />
            </div>
          )}
          {/* Challenges sidebar - Từ backend */}
          <div className="w-full lg:w-[30%] flex-shrink-0 bg-white dark:bg-[#1e2530] rounded-xl shadow-sm dark:shadow-none overflow-hidden flex flex-col">
            <div className="px-4 py-3 border-b border-gray-100 dark:border-gray-700">
              <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-200">Challenges</h2>
              <p className="text-xs text-gray-400 dark:text-gray-500 mt-0.5">
                {currentLab?.name || 'No lab selected'}
              </p>
            </div>
            <ul className="flex-1 overflow-y-auto divide-y divide-gray-50 dark:divide-gray-800">
              {challenges.length === 0 ? (
                <li className="px-4 py-8 text-center text-gray-400 dark:text-gray-500 text-sm">
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
                          ? 'bg-purple-50 dark:bg-purple-900/20'
                          : 'hover:bg-gray-50 dark:hover:bg-[#151b24]'
                      }`}
                    >
                      <div>
                        <p className={`text-sm font-medium ${
                          selectedChallengeId === ch.id
                            ? 'text-purple-700 dark:text-purple-400'
                            : 'text-gray-800 dark:text-gray-200'
                        }`}>
                          {ch.name}
                        </p>
                        {chHasScore && hasValue(chScore) ? (
                          <p className={`text-xs mt-0.5 font-semibold ${scoreColor(chScore)}`}>
                            {chScore} / 100
                          </p>
                        ) : resultsRevealed ? (
                          <p className="text-xs mt-0.5 text-gray-400 dark:text-gray-600">
                            Not submitted
                          </p>
                        ) : null}
                      </div>
                      {selectedChallengeId === ch.id && (
                        <span className="w-1.5 h-1.5 rounded-full bg-purple-500 flex-shrink-0" />
                      )}
                    </button>
                  </li>
                  );
                })
              )}
            </ul>
          </div>

          {/* Main content - Từ backend */}
          <div className="flex-1 bg-white dark:bg-[#1e2530] rounded-xl shadow-sm dark:shadow-none overflow-hidden flex flex-col">
            {/* Tabs */}
            <div className="flex border-b border-gray-100 dark:border-gray-700 px-2">
              {['mmd', 'class', 'testcase'].map((t) => (
                <button
                  key={t}
                  onClick={() => setActiveTab(t)}
                  className={tabCls(t)}
                >
                  {t === 'mmd' ? 'MMD' : t === 'class' ? 'Class' : 'Testcase'}
                </button>
              ))}
              <div className="flex-1 flex items-center justify-end pr-4">
                <span className="text-xs text-gray-400 dark:text-gray-600">
                  {currentChallenge?.name || 'No challenge selected'}
                </span>
              </div>
            </div>

            {/* Tab content — empty-state UI until upload fetches results for this session */}
            <div className="flex-1 overflow-auto p-5">
              {resultsRevealed && isLoadingDetails ? (
                <div className="flex items-center justify-center h-48">
                  <div className="w-8 h-8 border-4 border-purple-500 border-t-transparent rounded-full animate-spin" />
                </div>
              ) : (
                <>
              {activeTab === 'mmd' && (
                <>
                  <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between mb-4">
                    <div>
                      <p className="text-sm font-semibold text-gray-700 dark:text-gray-200">MMD Score</p>
                      <p className="text-xs text-gray-500 dark:text-gray-400">Object model checks for the selected challenge.</p>
                    </div>
                    {resultsRevealed && mmdScore.total > 0 && (
                      <ScorePill ok={mmdScore.ok} total={mmdScore.total} pct={mmdScore.pct} />
                    )}
                  </div>

                  <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3 mb-4">
                    {mmdData.length > 0 ? mmdData.map((cls) => (
                      <div key={cls.name} className="border border-gray-200 dark:border-gray-700 rounded-xl overflow-hidden">
                        <div className="bg-purple-50 dark:bg-purple-900/20 px-4 py-2 border-b border-gray-200 dark:border-gray-700">
                          <p className="text-sm font-bold text-purple-700 dark:text-purple-300 font-mono">{cls.name}</p>
                        </div>
                        <ul className="divide-y divide-gray-100 dark:divide-gray-800">
                          {cls.attributes?.map((a, i) => (
                            <li key={i} className={`flex items-start gap-3 px-4 py-2 text-xs ${i % 2 === 0 ? '' : 'bg-gray-50 dark:bg-[#151b24]/50'}`}>
                              <span className={`font-mono flex-1 min-w-0 break-words ${a.type === 'field' ? 'text-blue-600 dark:text-blue-400' : a.type === 'method' ? 'text-green-600 dark:text-green-400' : 'text-orange-500 dark:text-orange-400'}`}>
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
                      <div className="rounded-xl border border-dashed border-gray-200 dark:border-gray-700 p-8 text-center text-gray-500 dark:text-gray-400">
                        No MMD class data is available.
                      </div>
                    )}
                  </div>

                  {resultsRevealed && (
                    <div className="overflow-hidden rounded-xl border border-gray-200 dark:border-gray-700">
                      <div className="flex items-center justify-between border-b border-gray-200 bg-gray-50 px-4 py-3 dark:border-gray-700 dark:bg-[#151b24]">
                        <div className="flex items-center gap-2 text-gray-500 dark:text-gray-400">
                          <GitMerge className="w-4 h-4" />
                          <span className="text-xs font-semibold uppercase tracking-[0.2em]">Relations</span>
                        </div>
                        {relationScore.total > 0 && (
                          <ScorePill ok={relationScore.ok} total={relationScore.total} pct={relationScore.pct} />
                        )}
                      </div>
                      <div className="grid grid-cols-4 items-center gap-4 border-b border-gray-200 px-4 py-3 text-[11px] uppercase tracking-[0.25em] text-gray-500 dark:border-gray-700 dark:text-gray-400">
                        <span className="font-semibold">From</span>
                        <div className="flex justify-center"><span className="font-semibold">Relation</span></div>
                        <span className="font-semibold">To</span>
                        <span className="font-semibold text-center">Status</span>
                      </div>
                      <div className="divide-y divide-gray-100 dark:divide-gray-800">
                        {relations.length > 0 ? relations.map((r, index) => (
                          <div key={index}>
                            <div className="grid grid-cols-4 items-center gap-4 px-4 py-3 text-sm text-gray-800 dark:text-gray-200">
                              <span className="font-mono text-purple-600 dark:text-purple-400">{r.from}</span>
                              <div className="flex justify-center">
                                <span className={`inline-flex items-center justify-center rounded-full px-3 py-1 text-[11px] font-semibold ${relationTypeStyle(r.relType)}`}>
                                  {r.relType}
                                </span>
                              </div>
                              <span className="font-mono text-purple-600 dark:text-purple-400">{r.to}</span>
                              <div className="flex justify-center">
                                {r.ok ? (
                                  <CheckCircle2 className="h-5 w-5 text-emerald-500 dark:text-emerald-400" />
                                ) : (
                                  <XCircle className="h-5 w-5 text-red-500" />
                                )}
                              </div>
                            </div>
                            {!r.ok && r.error && (
                              <div className="grid grid-cols-4 px-4 py-2 text-xs font-mono text-red-600 bg-red-50 dark:text-red-300 dark:bg-red-900/10">
                                <div className="col-span-4 text-left">{r.from} → {r.to}: {r.error}</div>
                              </div>
                            )}
                          </div>
                        )) : (
                          <div className="px-4 py-6 text-center text-xs text-gray-500 dark:text-gray-400">
                            No relation data is available.
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                </>
              )}

              {activeTab === 'class' && (
                <>
                  <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between mb-4">
                    <div>
                      <p className="text-sm font-semibold text-gray-700 dark:text-gray-200">Class Score</p>
                      <p className="text-xs text-gray-500 dark:text-gray-400">Tap a class to inspect its members.</p>
                    </div>
                    {resultsRevealed && classScore.total > 0 && (
                      <ScorePill ok={classScore.ok} total={classScore.total} pct={classScore.pct} />
                    )}
                  </div>

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
                          <div key={`${cls.name}-${cls.type}`} className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm dark:border-gray-700 dark:bg-[#1e2530]">
                            <button
                              type="button"
                              onClick={() => setExpandedClassName((current) => (current === cls.name ? null : cls.name))}
                              className="flex w-full items-center justify-between gap-3 bg-gray-50 px-5 py-4 text-left transition hover:bg-gray-100 dark:bg-[#151b24] dark:hover:bg-[#1a2235]"
                            >
                              <div>
                                <span className="text-[10px] uppercase tracking-wider text-gray-400 dark:text-gray-500">{cls.type || 'Class'}</span>
                                <p className="mt-1 font-bold font-mono text-gray-900 dark:text-white">{cls.name}</p>
                              </div>
                              <div className="flex items-center gap-3">
                                <ScorePill ok={passCount} total={allItems.length} pct={clsPct} />
                                {isOpen ? <ChevronUp className="h-4 w-4 text-gray-400" /> : <ChevronDown className="h-4 w-4 text-gray-400" />}
                              </div>
                            </button>

                            {isOpen && (
                              <div className="divide-y divide-gray-100 border-t border-gray-200 dark:divide-gray-800 dark:border-gray-700">
                                {fields.length > 0 && (
                                  <div className="px-5 py-3">
                                    <p className="mb-2 text-[10px] font-bold uppercase tracking-wider text-blue-500">Fields</p>
                                    <div className="space-y-2">
                                      {fields.map((f, i) => (
                                        <div key={i} className={`flex items-center justify-between rounded-lg px-3 py-2 ${f.ok ? 'bg-gray-50 dark:bg-[#0d1117]/40' : 'border border-red-200 bg-red-50 dark:border-red-800/40 dark:bg-red-900/10'}`}>
                                          <div>
                                            <p className="text-xs font-mono font-semibold text-blue-600 dark:text-blue-400">{f.name}: {f.dataType}</p>
                                            <p className="mt-0.5 text-[10px] text-gray-400">{f.scope || '—'}</p>
                                          </div>
                                          <Tick ok={f.ok} />
                                        </div>
                                      ))}
                                    </div>
                                  </div>
                                )}

                                {constructors.length > 0 && (
                                  <div className="px-5 py-3">
                                    <p className="mb-2 text-[10px] font-bold uppercase tracking-wider text-orange-500">Constructors</p>
                                    <div className="space-y-2">
                                      {constructors.map((c, i) => (
                                        <div key={i} className={`flex items-center justify-between rounded-lg px-3 py-2 ${c.ok ? 'bg-gray-50 dark:bg-[#0d1117]/40' : 'border border-red-200 bg-red-50 dark:border-red-800/40 dark:bg-red-900/10'}`}>
                                          <div>
                                            <p className="text-xs font-mono font-semibold text-orange-500 dark:text-orange-400">{c.name}({c.params})</p>
                                            <p className="mt-0.5 text-[10px] text-gray-400">{c.scope || '—'}</p>
                                          </div>
                                          <Tick ok={c.ok} />
                                        </div>
                                      ))}
                                    </div>
                                  </div>
                                )}

                                {methods.length > 0 && (
                                  <div className="px-5 py-3">
                                    <p className="mb-2 text-[10px] font-bold uppercase tracking-wider text-green-500">Methods</p>
                                    <div className="space-y-2">
                                      {methods.map((m, i) => (
                                        <div key={i} className={`flex items-center justify-between rounded-lg px-3 py-2 ${m.ok ? 'bg-gray-50 dark:bg-[#0d1117]/40' : 'border border-red-200 bg-red-50 dark:border-red-800/40 dark:bg-red-900/10'}`}>
                                          <div>
                                            <p className="text-xs font-mono font-semibold text-green-600 dark:text-green-400">{m.name}(): {m.returnType}</p>
                                            <p className="mt-0.5 text-[10px] text-gray-400">{m.scope || '—'}</p>
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
                    <div className="rounded-xl border border-dashed border-gray-200 p-8 text-center text-gray-500 dark:border-gray-700 dark:text-gray-400">
                      No class detail data is available.
                    </div>
                  )}
                </>
              )}

              {activeTab === 'testcase' && (
                <>
                  <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between mb-4">
                    <div>
                      <p className="text-sm font-semibold text-gray-700 dark:text-gray-200">Testcase Score</p>
                      <p className="text-xs text-gray-500 dark:text-gray-400">Structural testcase checks from the grading rubric.</p>
                    </div>
                    {resultsRevealed && testScore.total > 0 && (
                      <ScorePill ok={testScore.ok} total={testScore.total} pct={testScore.pct} />
                    )}
                  </div>

                  {testCasesData.length > 0 ? (
                    <div className="space-y-4">
                      <div className="space-y-2">
                        {testCasesData.map((tc) => (
                          <div key={tc.id} className="border border-gray-200 dark:border-gray-700 rounded-xl overflow-hidden">
                            <button
                              onClick={() => tc.isExample && setExpandedTC(expandedTC === tc.id ? null : tc.id)}
                              disabled={!tc.isExample}
                              className={`w-full flex items-center justify-between px-4 py-3 transition-colors text-left ${tc.isExample ? 'hover:bg-gray-50 dark:hover:bg-[#151b24] cursor-pointer' : 'cursor-default opacity-70'}`}
                            >
                              <div className="flex items-center gap-3">
                                {tc.isExample ? <Tick ok={tc.passed} /> : <Lock className="w-4 h-4 text-gray-400 dark:text-gray-600 flex-shrink-0" />}
                                <span className={`text-sm font-medium ${tc.isExample ? 'text-gray-800 dark:text-gray-200' : 'text-gray-400 dark:text-gray-600'}`}>
                                  {tc.name}
                                </span>
                                {!tc.isExample && <span className="text-[10px] bg-gray-100 dark:bg-gray-800 text-gray-400 px-2 py-0.5 rounded-full">Hidden</span>}
                                {tc.isExample && <StatusBadge status={tc.passed ? 'success' : 'error'} />}
                              </div>
                              <div className="flex items-center gap-2">
                                {tc.isExample && <span className={`text-xs font-semibold ${tc.passed ? 'text-green-500' : 'text-red-500'}`}>{tc.passed ? 'PASS' : 'FAIL'}</span>}
                                {tc.isExample && (expandedTC === tc.id ? <ChevronUp className="w-4 h-4 text-gray-400" /> : <ChevronDown className="w-4 h-4 text-gray-400" />)}
                              </div>
                            </button>

                            {tc.isExample && expandedTC === tc.id && (
                              <div className="border-t border-gray-100 dark:border-gray-700 grid grid-cols-1 gap-4 md:grid-cols-3 md:divide-x md:divide-gray-100 dark:md:divide-gray-700">
                                <div className="p-4">
                                  <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">Input</p>
                                  <pre className="bg-gray-50 dark:bg-[#151b24] rounded-lg p-3 text-xs font-mono text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{tc.input || '—'}</pre>
                                </div>
                                <div className="p-4">
                                  <p className="text-[10px] font-bold text-green-500 uppercase tracking-wider mb-2">Expected Output</p>
                                  <pre className="bg-green-50 dark:bg-green-900/10 border border-green-200 dark:border-green-800/40 rounded-lg p-3 text-xs font-mono text-green-700 dark:text-green-400 whitespace-pre-wrap">{tc.expectedOutput || '—'}</pre>
                                </div>
                                <div className="p-4">
                                  <div className="flex items-center justify-between mb-2">
                                    <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Your Output</p>
                                    <Tick ok={tc.passed} />
                                  </div>
                                  <pre className={`rounded-lg p-3 text-xs font-mono whitespace-pre-wrap border ${tc.passed ? 'bg-green-50 dark:bg-green-900/10 border-green-200 dark:border-green-800/40 text-green-700 dark:text-green-400' : 'bg-red-50 dark:bg-red-900/10 border-red-200 dark:border-red-800/40 text-red-600 dark:text-red-400'}`}>{tc.studentOutput || '—'}</pre>
                                </div>
                              </div>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>
                  ) : (
                    <div className="rounded-xl border border-dashed border-gray-200 dark:border-gray-700 p-8 text-center text-gray-500 dark:text-gray-400">
                      No testcase detail data is available.
                    </div>
                  )}
                </>
              )}
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}