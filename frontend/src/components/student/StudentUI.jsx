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

function scoreColor(s) {
  if (s === null || s === undefined) return 'text-gray-400';
  if (s >= 90) return 'text-green-500';
  if (s >= 75) return 'text-blue-500';
  if (s >= 60) return 'text-yellow-500';
  return 'text-red-500';
}

// Helper: has a value that isn't null/undefined
const hasValue = (v) => v !== null && v !== undefined;

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

  // Upload handler
  onUploadComplete = () => {},

  // Loading/Error states
  isLoading = false,
  isLoadingDetails = false,
  isRefreshingResults = false,
  error = null,
}) {
  const [activeTab, setActiveTab] = useState('mmd');
  const [expandedTC, setExpandedTC] = useState(null);

  // Reset expanded test case khi đổi challenge
  useEffect(() => {
    setExpandedTC(null);
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
            onChange={(e) => onLabChange(Number(e.target.value))}
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
            attemptNumber={(stats.totalSubmissions ?? 0) + 1}
            authToken={user?.accessToken}
            onUploadComplete={onUploadComplete}
          />
        </div>

        {/* Stats row - Từ backend, hiển thị "--/--" khi không có dữ liệu */}
        <div className="grid grid-cols-1 gap-4 mb-6 lg:grid-cols-3">
          <div className="bg-gradient-to-br from-green-500 to-green-600 rounded-xl p-5 shadow-lg shadow-green-500/20">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-9 h-9 bg-white/20 rounded-lg flex items-center justify-center">
                <CheckCircle2 className="w-5 h-5 text-white" />
              </div>
              <span className="text-white/90 text-sm">Current Grade</span>
            </div>
            {hasValue(stats.currentGrade) ? (
              <div className="flex items-baseline gap-1">
                <span className="text-4xl text-white font-bold">{stats.currentGrade}</span>
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
            <span className="text-gray-900 dark:text-white text-sm font-medium">
              {hasValue(stats.latestSubmission) ? stats.latestSubmission : '--/--'}
            </span>
          </div>
        </div>

        {/* Overview — sidebar + result panel; only this section refreshes after upload */}
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
                challenges.map((ch) => (
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
                        {hasValue(ch.score) ? (
                          <p className={`text-xs mt-0.5 font-semibold ${scoreColor(ch.score)}`}>
                            {ch.score} / 100
                          </p>
                        ) : (
                          <p className="text-xs mt-0.5 text-gray-400 dark:text-gray-600">
                            Not submitted
                          </p>
                        )}
                      </div>
                      {selectedChallengeId === ch.id && (
                        <span className="w-1.5 h-1.5 rounded-full bg-purple-500 flex-shrink-0" />
                      )}
                    </button>
                  </li>
                ))
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

            {/* Tab content - renders nothing when there's no data from the backend */}
            <div className="flex-1 overflow-auto p-5">
              {isLoadingDetails ? (
                <div className="flex items-center justify-center h-48">
                  <div className="w-8 h-8 border-4 border-purple-500 border-t-transparent rounded-full animate-spin" />
                </div>
              ) : (
                <>
              {activeTab === 'mmd' && mmdData.length > 0 && (
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                  {mmdData.map((cls) => (
                    <div key={cls.name} className="border border-gray-200 dark:border-gray-700 rounded-xl overflow-hidden">
                      <div className="bg-purple-50 dark:bg-purple-900/20 px-4 py-2 border-b border-gray-200 dark:border-gray-700">
                        <p className="text-sm font-bold text-purple-700 dark:text-purple-300 font-mono">
                          {cls.name}
                        </p>
                      </div>
                      <ul className="divide-y divide-gray-100 dark:divide-gray-800">
                        {cls.attributes.map((a, i) => (
                          <li key={i} className={`flex items-center justify-between px-4 py-2 text-xs ${
                            i % 2 === 0 ? '' : 'bg-gray-50 dark:bg-[#151b24]/50'
                          }`}>
                            <span className={`font-mono ${
                              a.type === 'field'
                                ? 'text-blue-600 dark:text-blue-400'
                                : a.type === 'method'
                                  ? 'text-green-600 dark:text-green-400'
                                  : 'text-orange-500 dark:text-orange-400'
                            }`}>
                              {a.name}
                            </span>
                            <Tick ok={a.ok} />
                          </li>
                        ))}
                      </ul>
                    </div>
                  ))}
                </div>
              )}

              {activeTab === 'class' && classData.length > 0 && (
                <div className="space-y-5">
                  {classData.map((cls) => (
                    <div key={cls.name} className="border border-gray-200 dark:border-gray-700 rounded-xl overflow-hidden">
                      <div className="bg-gray-50 dark:bg-[#151b24] px-5 py-3 flex items-center justify-between border-b border-gray-200 dark:border-gray-700">
                        <div>
                          <span className="text-xs text-gray-400 dark:text-gray-500 uppercase tracking-wider">
                            {cls.type || 'Class'}
                          </span>
                          <p className="font-bold text-gray-900 dark:text-white font-mono">
                            {cls.name}
                          </p>
                        </div>
                        <StatusBadge status={cls.status} />
                      </div>
                      {cls.error && (
                        <div className="border-b border-red-200 dark:border-red-800/40 bg-red-50 dark:bg-red-900/20 px-5 py-3">
                          <p className="text-xs font-semibold text-red-600 dark:text-red-400 mb-1">
                            Compilation failed
                          </p>
                          <pre className="text-xs text-red-700 dark:text-red-300 whitespace-pre-wrap font-mono">
                            {cls.error}
                          </pre>
                        </div>
                      )}
                      <div className="grid grid-cols-1 gap-4 md:grid-cols-3 md:divide-x md:divide-gray-100 dark:md:divide-gray-800">
                        <div>
                          <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider px-4 py-2 border-b border-gray-100 dark:border-gray-800">
                            Fields
                          </p>
                          {cls.fields?.map((f, i) => (
                            <div key={i} className={`flex items-center justify-between px-4 py-2 ${
                              i % 2 === 0 ? '' : 'bg-gray-50 dark:bg-[#151b24]/50'
                            }`}>
                              <div>
                                <p className="text-xs font-mono text-blue-600 dark:text-blue-400">
                                  {f.name}
                                </p>
                                <p className="text-[10px] text-gray-400">
                                  {formatScopeDetail(f.scope, f.dataType)}
                                </p>
                              </div>
                              <Tick ok={f.ok} />
                            </div>
                          ))}
                        </div>
                        <div>
                          <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider px-4 py-2 border-b border-gray-100 dark:border-gray-800">
                            Constructors
                          </p>
                          {cls.constructors?.map((c, i) => (
                            <div key={i} className={`flex items-center justify-between px-4 py-2 ${
                              i % 2 === 0 ? '' : 'bg-gray-50 dark:bg-[#151b24]/50'
                            }`}>
                              <div>
                                <p className="text-xs font-mono text-orange-500 dark:text-orange-400">
                                  {c.name}()
                                </p>
                                <p className="text-[10px] text-gray-400 truncate max-w-[110px]">
                                  {c.params}
                                </p>
                              </div>
                              <Tick ok={c.ok} />
                            </div>
                          ))}
                        </div>
                        <div>
                          <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider px-4 py-2 border-b border-gray-100 dark:border-gray-800">
                            Methods
                          </p>
                          {cls.methods?.map((m, i) => (
                            <div key={i} className={`flex items-center justify-between px-4 py-2 ${
                              i % 2 === 0 ? '' : 'bg-gray-50 dark:bg-[#151b24]/50'
                            }`}>
                              <div>
                                <p className="text-xs font-mono text-green-600 dark:text-green-400">
                                  {m.name}()
                                </p>
                                <p className="text-[10px] text-gray-400">
                                  {formatScopeDetail(m.scope, m.returnType)}
                                </p>
                              </div>
                              <Tick ok={m.ok} />
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {activeTab === 'testcase' && testCases.length > 0 && (
                <div className="space-y-2">
                  {testCases.map((tc) => (
                    <div key={tc.id} className="border border-gray-200 dark:border-gray-700 rounded-xl overflow-hidden">
                      <button
                        onClick={() => tc.isExample && setExpandedTC(expandedTC === tc.id ? null : tc.id)}
                        disabled={!tc.isExample}
                        className={`w-full flex items-center justify-between px-4 py-3 transition-colors text-left ${
                          tc.isExample
                            ? 'hover:bg-gray-50 dark:hover:bg-[#151b24] cursor-pointer'
                            : 'cursor-default opacity-60'
                        }`}
                      >
                        <div className="flex items-center gap-3">
                          {tc.isExample ? (
                            <Tick ok={tc.passed} />
                          ) : (
                            <Lock className="w-4 h-4 text-gray-400 dark:text-gray-600 flex-shrink-0" />
                          )}
                          <span className={`text-sm font-medium ${
                            tc.isExample
                              ? 'text-gray-800 dark:text-gray-200'
                              : 'text-gray-400 dark:text-gray-600'
                          }`}>
                            {tc.name}
                          </span>
                          {!tc.isExample && (
                            <span className="text-[10px] bg-gray-100 dark:bg-gray-800 text-gray-400 px-2 py-0.5 rounded-full">
                              Hidden
                            </span>
                          )}
                          {tc.isExample && (
                            <StatusBadge status={tc.passed ? 'success' : 'error'} />
                          )}
                        </div>
                        <div className="flex items-center gap-2">
                          {tc.isExample && (
                            <span className={`text-xs font-semibold ${
                              tc.passed ? 'text-green-500' : 'text-red-500'
                            }`}>
                              {tc.passed ? 'PASS' : 'FAIL'}
                            </span>
                          )}
                          {tc.isExample && (
                            expandedTC === tc.id
                              ? <ChevronUp className="w-4 h-4 text-gray-400" />
                              : <ChevronDown className="w-4 h-4 text-gray-400" />
                          )}
                        </div>
                      </button>

                      {tc.isExample && expandedTC === tc.id && (
                        <div className="border-t border-gray-100 dark:border-gray-700 grid grid-cols-1 gap-4 md:grid-cols-3 md:divide-x md:divide-gray-100 dark:md:divide-gray-700">
                          <div className="p-4">
                            <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">
                              Input
                            </p>
                            <pre className="bg-gray-50 dark:bg-[#151b24] rounded-lg p-3 text-xs font-mono text-gray-700 dark:text-gray-300 whitespace-pre-wrap">
                              {tc.input || '—'}
                            </pre>
                          </div>
                          <div className="p-4">
                            <p className="text-[10px] font-bold text-green-500 uppercase tracking-wider mb-2">
                              Expected Output
                            </p>
                            <pre className="bg-green-50 dark:bg-green-900/10 border border-green-200 dark:border-green-800/40 rounded-lg p-3 text-xs font-mono text-green-700 dark:text-green-400 whitespace-pre-wrap">
                              {tc.expectedOutput || '—'}
                            </pre>
                          </div>
                          <div className="p-4">
                            <div className="flex items-center justify-between mb-2">
                              <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">
                                Your Output
                              </p>
                              <Tick ok={tc.passed} />
                            </div>
                            <pre className={`rounded-lg p-3 text-xs font-mono whitespace-pre-wrap border ${
                              tc.passed
                                ? "bg-green-50 dark:bg-green-900/10 border-green-200 dark:border-green-800/40 text-green-700 dark:text-green-400"
                                : "bg-red-50 dark:bg-red-900/10 border-red-200 dark:border-red-800/40 text-red-600 dark:text-red-400"
                            }`}>
                              {tc.studentOutput || '—'}
                            </pre>
                          </div>
                        </div>
                      )}
                    </div>
                  ))}
                  <p className="text-xs text-gray-400 dark:text-gray-600 text-center pt-2">
                    Only example testcases are viewable. Normal testcases are hidden.
                  </p>
                </div>
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