// src/components/student/StudentHistoryPage.jsx
import { Fragment, useState, useEffect, useCallback, useMemo } from 'react';
import { 
  History, TrendingUp, Award, Clock, ChevronDown, ChevronUp, 
  CheckCircle2, XCircle, RefreshCw 
} from 'lucide-react';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

function deriveSubmissionStatus(score, challengeResults, apiStatus) {
  if (apiStatus === 'passed' || apiStatus === 'partial' || apiStatus === 'failed' || apiStatus === 'unknown') {
    return apiStatus;
  }
  if (challengeResults?.length > 0) {
    const correctCount = challengeResults.filter((c) => c.isCorrect).length;
    if (correctCount === challengeResults.length) return 'passed';
    if (correctCount === 0) return 'failed';
    return 'partial';
  }
  const numericScore = score != null ? Number(score) : null;
  if (numericScore == null || Number.isNaN(numericScore)) return 'unknown';
  if (numericScore <= 0) return 'failed';
  if (numericScore >= 100) return 'passed';
  return 'partial';
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
  const color = score >= 90 
    ? 'text-green-600 dark:text-green-300' 
    : score >= 75 
      ? 'text-blue-600 dark:text-blue-300' 
      : score >= 60 
        ? 'text-yellow-600 dark:text-yellow-300' 
        : 'text-red-600 dark:text-red-300';
  return <span className={`font-semibold ${color}`}>{score}%</span>;
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
  const [hasData, setHasData] = useState(false);

  // ===== Fetch Functions =====

  const fetchHistory = useCallback(async (labId = null) => {
    setLoading(true);
    try {
      const url = labId 
        ? `${API_BASE}/api/submissions/my-history?labId=${labId}`
        : `${API_BASE}/api/submissions/my-history`;
      
      const response = await fetch(url, {
        headers: {
          Authorization: `Bearer ${sessionStorage.getItem('accessToken')}`,
        },
      });

      // Nếu API chưa implement (404) hoặc lỗi khác, chỉ set data rỗng
      if (!response.ok) {
        console.info('History API not available yet, using empty data');
        setSubmissions([]);
        setStats({ totalSubmissions: 0, averageScore: null, bestScore: null, labsAttempted: 0 });
        setHasData(false);
        return;
      }

      const data = await response.json();
      setSubmissions(data.submissions || []);
      setStats(data.stats || { totalSubmissions: 0, averageScore: null, bestScore: null, labsAttempted: 0 });
      setHasData(true);
    } catch (err) {
      console.info('Could not fetch submission history:', err.message);
      setSubmissions([]);
      setStats({ totalSubmissions: 0, averageScore: null, bestScore: null, labsAttempted: 0 });
      setHasData(false);
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchLabsSummary = useCallback(async () => {
    try {
      const response = await fetch(`${API_BASE}/api/submissions/my-labs`, {
        headers: {
          Authorization: `Bearer ${sessionStorage.getItem('accessToken')}`,
        },
      });

      if (!response.ok) {
        console.info('Labs summary API not available yet');
        setLabsSummary([]);
        setLabOptions(['All Labs']);
        return;
      }

      const data = await response.json();
      setLabsSummary(data || []);
      
      // Tạo lab options từ dữ liệu
      const labs = ['All Labs', ...data.map(lab => lab.name)];
      setLabOptions(labs);
    } catch (err) {
      console.info('Could not fetch labs summary:', err.message);
      setLabsSummary([]);
      setLabOptions(['All Labs']);
    }
  }, []);

  // ===== Effects =====

  useEffect(() => {
    const loadData = async () => {
      await Promise.all([
        fetchHistory(),
        fetchLabsSummary(),
      ]);
    };
    loadData();
  }, [fetchHistory, fetchLabsSummary]);

  // ===== Handlers =====

  const handleFilterChange = (labName) => {
    setSelectedLab(labName);
    if (labName === 'All Labs') {
      fetchHistory();
    } else {
      const lab = labsSummary.find(l => l.name === labName);
      if (lab) {
        fetchHistory(lab.id);
      }
    }
  };

  const handleRefresh = () => {
    if (selectedLab === 'All Labs') {
      fetchHistory();
    } else {
      const lab = labsSummary.find(l => l.name === selectedLab);
      if (lab) {
        fetchHistory(lab.id);
      }
    }
    fetchLabsSummary();
  };

  const toggleRow = (id) => {
    setExpandedRow(expandedRow === id ? null : id);
  };

  // ===== Computed Values =====

  const filteredSubmissions = useMemo(() => {
    return submissions;
  }, [submissions]);

  // ===== Render =====

  // Format stats value - hiển thị -- nếu null/undefined
  const formatStatValue = (value) => {
    if (value === null || value === undefined) return '--';
    if (typeof value === 'number') {
      if (Number.isInteger(value)) return value;
      return Math.round(value);
    }
    return value;
  };

  // Format stat with percent
  const formatStatPercent = (value) => {
    if (value === null || value === undefined) return '--';
    return `${Math.round(value)}%`;
  };

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
            <p className="text-sm text-gray-500 dark:text-gray-400">Review all your lab attempts and performance</p>
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

      {/* ===== Stats Cards - hiển thị -- khi không có dữ liệu ===== */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[
          { 
            label: 'Labs Attempted', 
            value: stats.labsAttempted ?? 0,
            displayValue: formatStatValue(stats.labsAttempted),
            icon: <Award className="w-4 h-4" />,
            tone: 'text-purple-400', 
            bg: 'bg-purple-900/30' 
          },
          { 
            label: 'Total Submissions', 
            value: stats.totalSubmissions ?? 0,
            displayValue: formatStatValue(stats.totalSubmissions),
            icon: <Clock className="w-4 h-4" />,
            tone: 'text-blue-400', 
            bg: 'bg-blue-900/30' 
          },
          { 
            label: 'Average Score', 
            value: stats.averageScore,
            displayValue: formatStatPercent(stats.averageScore),
            icon: <TrendingUp className="w-4 h-4" />,
            tone: 'text-green-400', 
            bg: 'bg-green-900/30' 
          },
          { 
            label: 'Best Score', 
            value: stats.bestScore,
            displayValue: formatStatPercent(stats.bestScore),
            icon: <Award className="w-4 h-4" />,
            tone: 'text-yellow-400', 
            bg: 'bg-yellow-900/30' 
          },
        ].map((card) => (
          <div key={card.label} className="rounded-3xl border border-gray-200 bg-white dark:bg-[#1e2530] dark:border-gray-700 p-5">
            <div className="flex items-center justify-between gap-4">
              <p className="text-xs uppercase tracking-[0.2em] text-gray-500 dark:text-gray-400">{card.label}</p>
              <div className={`rounded-2xl p-3 ${card.bg}`}>
                {card.icon}
              </div>
            </div>
            <p className={`mt-4 text-3xl font-semibold ${card.tone}`}>{card.displayValue}</p>
          </div>
        ))}
      </div>

      {/* ===== Labs Summary Sidebar + Submissions Table ===== */}
      <div className="grid gap-6 xl:grid-cols-[0.6fr_1fr]">
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
                      }`}>{Math.round(lab.bestScore)}%</span>
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
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                {filteredSubmissions.length} submission{filteredSubmissions.length !== 1 ? 's' : ''} found
              </p>
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
                    <th className="px-4 py-3">Lab</th>
                    <th className="px-4 py-3">Attempt</th>
                    <th className="px-4 py-3">Score</th>
                    <th className="px-4 py-3">Submitted</th>
                    <th className="px-4 py-3">Status</th>
                    <th className="px-4 py-3" />
                  </tr>
                </thead>
                <tbody>
                  {filteredSubmissions.map((item, index) => {
                    const isExpanded = expandedRow === item.id;
                    const status = deriveSubmissionStatus(
                      item.score,
                      item.challengeResults,
                      item.status,
                    );

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
                                              Score: {Math.round(cr.score)}%
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
      </div>
    </div>
  );
}