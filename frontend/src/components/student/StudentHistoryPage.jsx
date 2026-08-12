// src/components/student/StudentHistoryPage.jsx
import { Fragment, useState, useEffect, useCallback, useMemo } from 'react';
import { 
  History, TrendingUp, Award, Clock, ChevronDown, ChevronUp, 
  CheckCircle2, XCircle, RefreshCw 
} from 'lucide-react';
import SortableTableHeader from '../ui/SortableTableHeader';
import { sortRows, toggleSortState } from '../../utils/sort';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

function deriveSubmissionStatus(score) {
  const numericScore = score != null ? Number(score) : null;
  if (numericScore == null || Number.isNaN(numericScore)) return 'unknown';
  if (numericScore < 50) return 'failed';
  if (numericScore > 80) return 'passed';
  return 'partial';
}

function formatScore(value) {
  if (value === null || value === undefined) return '--';
  const num = Number(value);
  if (Number.isNaN(num)) return '--';
  if (Number.isInteger(num)) return String(num);
  return num.toFixed(2).replace(/\.?0+$/, '');
}

// ==================== SUB-COMPONENTS ====================

function StatusBadge({ status }) {
  const colors = {
    passed: 'bg-green-100 text-green-800 border border-green-200 dark:bg-green-900/30 dark:text-green-300 dark:border-green-500/30',
    partial: 'bg-yellow-100 text-yellow-800 border border-yellow-200 dark:bg-yellow-900/30 dark:text-yellow-300 dark:border-yellow-500/30',
    failed: 'bg-red-100 text-red-800 border border-red-200 dark:bg-red-900/30 dark:text-red-300 dark:border-red-500/30',
    unknown: 'bg-gray-100 text-gray-800 border border-gray-200 dark:bg-gray-900/30 dark:text-gray-300 dark:border-gray-500/30',
  };
  return (
    <span className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${colors[status] || colors.unknown}`}>
      {status?.toUpperCase() || 'UNKNOWN'}
    </span>
  );
}

function ScorePill({ score }) {
  if (score === null || score === undefined) {
    return <span className="text-gray-400 dark:text-gray-600 text-sm">--</span>;
  }
  const numericScore = Number(score);
  const color = numericScore >= 90 
    ? 'text-green-600 dark:text-green-300' 
    : numericScore >= 75 
      ? 'text-blue-600 dark:text-blue-300' 
      : numericScore >= 60 
        ? 'text-yellow-600 dark:text-yellow-300' 
        : 'text-red-600 dark:text-red-300';
  return <span className={`font-semibold ${color}`}>{formatScore(score)}</span>;
}

function ScoreBar({ score }) {
  if (score === null || score === undefined) return null;
  const color = score >= 90 
    ? 'bg-gradient-to-r from-green-500 to-emerald-500' 
    : score >= 75 
      ? 'bg-gradient-to-r from-blue-500 to-sky-500' 
      : score >= 60 
        ? 'bg-gradient-to-r from-yellow-500 to-amber-500' 
        : 'bg-gradient-to-r from-red-500 to-rose-500';
  return (
    <div className="h-2 overflow-hidden rounded-full bg-gray-700">
      <div className={`h-full rounded-full ${color}`} style={{ width: `${Math.min(score, 100)}%` }} />
    </div>
  );
}

// ==================== MAIN COMPONENT ====================

export default function StudentHistoryPage({ user, onLogout, onNavigate }) {
  // ===== States =====
  const [submissions, setSubmissions] = useState([]);
  const [labOptions, setLabOptions] = useState(['All Labs']);
  const [selectedLab, setSelectedLab] = useState('All Labs');
  const [stats, setStats] = useState({
    totalSubmissions: 0,
    averageScore: null,
    bestScore: null,
    labsAttempted: 0,
  });
  const [labsSummary, setLabsSummary] = useState([]);
  const [expandedRow, setExpandedRow] = useState(null);
  const [loading, setLoading] = useState(true);
  const [historySort, setHistorySort] = useState({ field: 'submittedAt', direction: 'desc' });

  const emptyStats = {
    totalSubmissions: 0,
    averageScore: null,
    bestScore: null,
    labsAttempted: 0,
  };

  const fetchHistoryData = useCallback(async (labId = null) => {
    const url = labId 
      ? `${API_BASE}/api/submissions/my-history?labId=${labId}`
      : `${API_BASE}/api/submissions/my-history`;

    const response = await fetch(url, {
      headers: {
        Authorization: `Bearer ${sessionStorage.getItem('accessToken')}`,
      },
    });

    if (!response.ok) {
      console.info('History API not available yet, using empty data');
      return { submissions: [], stats: emptyStats };
    }

    const data = await response.json();
    return {
      submissions: data.submissions || [],
      stats: data.stats || emptyStats,
    };
  }, []);

  const fetchLabsSummaryData = useCallback(async () => {
    const response = await fetch(`${API_BASE}/api/submissions/my-labs`, {
      headers: {
        Authorization: `Bearer ${sessionStorage.getItem('accessToken')}`,
      },
    });

    if (!response.ok) {
      console.info('Labs summary API not available yet');
      return { labsSummary: [], labOptions: ['All Labs'] };
    }

    const data = await response.json();
    const labs = ['All Labs', ...data.map((lab) => lab.name)];
    return { labsSummary: data || [], labOptions: labs };
  }, []);

  const loadPageData = useCallback(async (labId = null) => {
    setLoading(true);
    try {
      const [historyData, labsData] = await Promise.all([
        fetchHistoryData(labId),
        fetchLabsSummaryData(),
      ]);
      setSubmissions(historyData.submissions);
      setStats(historyData.stats);
      setLabsSummary(labsData.labsSummary);
      setLabOptions(labsData.labOptions);
    } catch (err) {
      console.info('Could not fetch submission history:', err.message);
      setSubmissions([]);
      setStats(emptyStats);
      setLabsSummary([]);
      setLabOptions(['All Labs']);
    } finally {
      setLoading(false);
    }
  }, [fetchHistoryData, fetchLabsSummaryData]);

  useEffect(() => {
    void loadPageData();
  }, [loadPageData]);

  const handleFilterChange = (labName) => {
    setSelectedLab(labName);
    if (labName === 'All Labs') {
      void loadPageData();
    } else {
      const lab = labsSummary.find((l) => l.name === labName);
      if (lab) {
        void loadPageData(lab.id);
      }
    }
  };

  const handleRefresh = () => {
    if (selectedLab === 'All Labs') {
      void loadPageData();
    } else {
      const lab = labsSummary.find((l) => l.name === selectedLab);
      if (lab) {
        void loadPageData(lab.id);
      }
    }
  };

  const toggleRow = (id) => {
    setExpandedRow(expandedRow === id ? null : id);
  };

  // ===== Computed Values =====

  const filteredSubmissions = useMemo(() => {
    return sortRows(submissions, historySort.field, historySort.direction, (item) => {
      if (historySort.field === 'labName') return item.lab?.name || '';
      if (historySort.field === 'status') return deriveSubmissionStatus(item.score);
      if (historySort.field === 'attempt') return item.attemptNumber;
      if (historySort.field === 'score') return item.score;
      if (historySort.field === 'submittedAt') return item.submittedAt;
      return item[historySort.field];
    });
  }, [submissions, historySort]);

  const handleHistorySort = (field) => {
    setHistorySort((prev) => toggleSortState(prev, field));
  };

  // ===== Render =====

  const formatStatValue = (value) => {
    if (value === null || value === undefined) return '--';
    if (typeof value === 'number') {
      return formatScore(value);
    }
    return value;
  };

  const statCards = [
    { 
      label: 'Labs Attempted', 
      displayValue: formatStatValue(stats.labsAttempted ?? 0),
      icon: <Award className="w-4 h-4" />,
      tone: 'text-purple-400', 
      bg: 'bg-purple-900/30' 
    },
    { 
      label: 'Total Submissions', 
      displayValue: formatStatValue(stats.totalSubmissions ?? 0),
      icon: <Clock className="w-4 h-4" />,
      tone: 'text-blue-400', 
      bg: 'bg-blue-900/30' 
    },
    { 
      label: 'Average Score', 
      displayValue: formatStatValue(stats.averageScore),
      icon: <TrendingUp className="w-4 h-4" />,
      tone: 'text-green-400', 
      bg: 'bg-green-900/30' 
    },
    { 
      label: 'Best Score', 
      displayValue: formatStatValue(stats.bestScore),
      icon: <Award className="w-4 h-4" />,
      tone: 'text-yellow-400', 
      bg: 'bg-yellow-900/30' 
    },
  ];

  return (
    <div className="w-full flex flex-col gap-6 px-4 sm:px-6 lg:px-8 py-8 max-w-full overflow-x-hidden">
      {/* ===== Header ===== */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div className="flex items-center gap-3">
          <div className="grid h-11 w-11 place-items-center rounded-2xl bg-purple-600 text-white">
            <History className="h-5 w-5" />
          </div>
          <div>
            <h1 className="text-xl font-semibold text-gray-900 dark:text-white">Submission History</h1>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <select
            value={selectedLab}
            onChange={(e) => handleFilterChange(e.target.value)}
            className="rounded-2xl border border-gray-200 bg-white dark:bg-[#1e2530] dark:border-gray-700 px-4 py-2 text-sm text-gray-700 dark:text-gray-200 focus:outline-none focus:ring-2 focus:ring-purple-500"
          >
            {labOptions.map((labName) => (
              <option key={labName} value={labName}>{labName}</option>
            ))}
          </select>
          <button
            onClick={handleRefresh}
            className="p-2 rounded-lg border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-[#151b24] transition-colors"
            title="Refresh"
          >
            <RefreshCw className={`w-4 h-4 text-gray-500 dark:text-gray-400 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* ===== Stats Cards ===== */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {loading ? (
          Array.from({ length: 4 }).map((_, index) => (
            <div key={index} className="rounded-3xl border border-gray-200 bg-white dark:bg-[#1e2530] dark:border-gray-700 p-5 animate-pulse">
              <div className="h-4 w-24 rounded bg-gray-200 dark:bg-gray-700" />
              <div className="mt-4 h-9 w-16 rounded bg-gray-200 dark:bg-gray-700" />
            </div>
          ))
        ) : (
          statCards.map((card) => (
            <div key={card.label} className="rounded-3xl border border-gray-200 bg-white dark:bg-[#1e2530] dark:border-gray-700 p-5">
              <div className="flex items-center justify-between gap-4">
                <p className="text-xs uppercase tracking-[0.2em] text-gray-500 dark:text-gray-400">{card.label}</p>
                <div className={`rounded-2xl p-3 ${card.bg}`}>
                  {card.icon}
                </div>
              </div>
              <p className={`mt-4 text-3xl font-semibold ${card.tone}`}>{card.displayValue}</p>
            </div>
          ))
        )}
      </div>

      {/* ===== Labs Summary Sidebar + Submissions Table ===== */}
      <div className="grid gap-6 xl:grid-cols-[0.6fr_1fr]">
        {loading ? (
          <>
            <section className="rounded-3xl border border-gray-200 bg-white dark:bg-[#1e2530] dark:border-gray-700 p-6 animate-pulse">
              <div className="h-4 w-40 rounded bg-gray-200 dark:bg-gray-700" />
              <div className="mt-6 space-y-4">
                <div className="h-10 rounded bg-gray-200 dark:bg-gray-700" />
                <div className="h-10 rounded bg-gray-200 dark:bg-gray-700" />
              </div>
            </section>
            <section className="rounded-3xl border border-gray-200 bg-white dark:bg-[#1e2530] dark:border-gray-700 p-6 animate-pulse">
              <div className="h-4 w-32 rounded bg-gray-200 dark:bg-gray-700" />
              <div className="mt-6 h-48 rounded bg-gray-200 dark:bg-gray-700" />
            </section>
          </>
        ) : (
          <>
        {/* Labs Summary */}
        <section className="rounded-3xl border border-gray-200 bg-white dark:bg-[#1e2530] dark:border-gray-700 p-6">
          <h2 className="text-sm font-semibold uppercase tracking-[0.2em] text-gray-400 dark:text-gray-500">
            Performance by Lab
          </h2>
          {labsSummary.length === 0 ? (
            <p className="mt-6 text-sm text-gray-400 dark:text-gray-600 text-center">No labs attempted yet</p>
          ) : (
            <div className="mt-6 space-y-4">
              {labsSummary.map((lab) => (
                <div key={lab.id} className="space-y-2">
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-gray-700 dark:text-gray-300">{lab.name}</span>
                    {lab.bestScore !== null && lab.bestScore !== undefined ? (
                      <span className={`font-semibold ${
                        lab.bestScore >= 90 ? 'text-green-600 dark:text-green-300' :
                        lab.bestScore >= 75 ? 'text-blue-600 dark:text-blue-300' :
                        lab.bestScore >= 60 ? 'text-yellow-600 dark:text-yellow-300' :
                        'text-red-600 dark:text-red-300'
                      }`}>{formatScore(lab.bestScore)}</span>
                    ) : (
                      <span className="text-gray-400 dark:text-gray-600">--</span>
                    )}
                  </div>
                  {lab.bestScore !== null && lab.bestScore !== undefined && <ScoreBar score={lab.bestScore} />}
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    {lab.attempts || 0} attempt{lab.attempts > 1 ? 's' : ''}
                    {lab.lastSubmittedAt && (
                      <> · Last: {new Date(lab.lastSubmittedAt).toLocaleDateString()}</>
                    )}
                  </p>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* Submissions Table */}
        <section className="rounded-3xl border border-gray-200 bg-white dark:bg-[#1e2530] dark:border-gray-700 p-6 overflow-hidden">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-sm font-semibold uppercase tracking-[0.2em] text-gray-400 dark:text-gray-500">
                All Submissions
              </h2>
            </div>
          </div>

          {filteredSubmissions.length === 0 ? (
            <div className="py-12 text-center text-gray-400 dark:text-gray-600">
              <p>No submissions found</p>
            </div>
          ) : (
            <div className="overflow-x-auto rounded-3xl border border-gray-200 dark:border-gray-700">
              <table className="w-full min-w-[720px] border-collapse text-left text-sm">
                <thead className="bg-gray-50 dark:bg-[#151b24] text-gray-600 dark:text-gray-400">
                  <tr>
                    <SortableTableHeader label="Lab" field="labName" activeField={historySort.field} direction={historySort.direction} onSort={handleHistorySort} className="px-4 py-3" stopRowClick />
                    <SortableTableHeader label="Attempt" field="attempt" activeField={historySort.field} direction={historySort.direction} onSort={handleHistorySort} className="px-4 py-3" stopRowClick />
                    <SortableTableHeader label="Score" field="score" activeField={historySort.field} direction={historySort.direction} onSort={handleHistorySort} className="px-4 py-3" stopRowClick />
                    <SortableTableHeader label="Submitted" field="submittedAt" activeField={historySort.field} direction={historySort.direction} onSort={handleHistorySort} className="px-4 py-3" stopRowClick />
                    <SortableTableHeader label="Status" field="status" activeField={historySort.field} direction={historySort.direction} onSort={handleHistorySort} className="px-4 py-3" stopRowClick />
                    <SortableTableHeader label="" sortable={false} className="px-4 py-3" />
                  </tr>
                </thead>
                <tbody>
                  {filteredSubmissions.map((item, index) => {
                    const isExpanded = expandedRow === item.id;
                    const status = deriveSubmissionStatus(item.score);

                    return (
                      <Fragment key={item.id}>
                        <tr
                          className={`cursor-pointer border-b border-gray-100 dark:border-gray-800 transition-colors ${
                            index % 2 === 0 ? 'bg-white dark:bg-transparent' : 'bg-gray-50 dark:bg-[#151b24]/50'
                          } hover:bg-gray-100 dark:hover:bg-[#1a1f2e]`}
                          onClick={() => toggleRow(item.id)}
                        >
                          <td className="px-4 py-4 text-gray-900 dark:text-gray-200">{item.lab?.name || 'Unknown Lab'}</td>
                          <td className="px-4 py-4 text-gray-700 dark:text-gray-400">#{item.attemptNumber}</td>
                          <td className="px-4 py-4">
                            <ScorePill score={item.score} />
                          </td>
                          <td className="px-4 py-4 text-gray-700 dark:text-gray-400">
                            {item.submittedAt ? new Date(item.submittedAt).toLocaleString() : '--'}
                          </td>
                          <td className="px-4 py-4">
                            <StatusBadge status={status} />
                          </td>
                          <td className="px-4 py-4 text-gray-700 dark:text-gray-400">
                            {isExpanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                          </td>
                        </tr>

                        {/* Expanded Details */}
                        {isExpanded && (
                          <tr className="bg-gray-50 dark:bg-[#151b24]">
                            <td colSpan={6} className="px-4 py-4">
                              <div className="space-y-4">
                                {/* Challenge Results */}
                                {item.challengeResults && item.challengeResults.length > 0 && (
                                  <div>
                                    <h4 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">Challenge Results</h4>
                                    <div className="grid gap-2 md:grid-cols-2 lg:grid-cols-3">
                                      {item.challengeResults.map((cr, idx) => (
                                        <div 
                                          key={idx}
                                          className={`p-3 rounded-lg border ${
                                            cr.isCorrect 
                                              ? 'border-green-200 dark:border-green-800 bg-green-50 dark:bg-green-900/20' 
                                              : 'border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-900/20'
                                          }`}
                                        >
                                          <div className="flex items-center justify-between">
                                            <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                                              {cr.challengeName || `Challenge ${idx + 1}`}
                                            </span>
                                            {cr.isCorrect ? (
                                              <CheckCircle2 className="w-4 h-4 text-green-500" />
                                            ) : (
                                              <XCircle className="w-4 h-4 text-red-500" />
                                            )}
                                          </div>
                                          {cr.score !== undefined && cr.score !== null && (
                                            <span className="text-xs text-gray-500 dark:text-gray-400">
                                              Score: {formatScore(cr.score)}
                                            </span>
                                          )}
                                        </div>
                                      ))}
                                    </div>
                                  </div>
                                )}

                                {(!item.challengeResults || item.challengeResults.length === 0) && (
                                  <p className="text-sm text-gray-400 dark:text-gray-600 text-center py-4">
                                    No detailed results available for this submission
                                  </p>
                                )}
                              </div>
                            </td>
                          </tr>
                        )}
                      </Fragment>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>
          </>
        )}
      </div>
    </div>
  );
}