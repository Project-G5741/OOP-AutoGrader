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
    passed: 'bg-success-bg text-success-text',
    partial: 'bg-warning-bg text-warning-text',
    failed: 'bg-error-bg text-error-text',
    unknown: 'bg-surface-secondary text-foreground-secondary',
  };
  return (
    <span className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${colors[status] || colors.unknown}`}>
      {status?.toUpperCase() || 'UNKNOWN'}
    </span>
  );
}

function ScorePill({ score }) {
  if (score === null || score === undefined) {
    return <span className="text-foreground-disabled text-sm">--</span>;
  }
  const numericScore = Number(score);
  const color = numericScore >= 90 
    ? 'text-success-text' 
    : numericScore >= 75 
      ? 'text-info-text' 
      : numericScore >= 60 
        ? 'text-warning-text' 
        : 'text-error-text';
  return <span className={`font-semibold ${color}`}>{formatScore(score)}</span>;
}

function ScoreBar({ score }) {
  if (score === null || score === undefined) return null;
  const color = score >= 90 
    ? 'bg-gradient-to-r from-success to-success-hover' 
    : score >= 75 
      ? 'bg-gradient-to-r from-info to-info-hover' 
      : score >= 60 
        ? 'bg-gradient-to-r from-warning to-warning-hover' 
        : 'bg-gradient-to-r from-error to-error-hover';
  return (
    <div className="h-2 overflow-hidden rounded-full bg-surface-tertiary">
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
      tone: 'text-primary', 
      bg: 'bg-primary-light' 
    },
    { 
      label: 'Total Submissions', 
      displayValue: formatStatValue(stats.totalSubmissions ?? 0),
      icon: <Clock className="w-4 h-4" />,
      tone: 'text-info', 
      bg: 'bg-info-bg' 
    },
    { 
      label: 'Average Score', 
      displayValue: formatStatValue(stats.averageScore),
      icon: <TrendingUp className="w-4 h-4" />,
      tone: 'text-success', 
      bg: 'bg-success-bg' 
    },
    { 
      label: 'Best Score', 
      displayValue: formatStatValue(stats.bestScore),
      icon: <Award className="w-4 h-4" />,
      tone: 'text-warning', 
      bg: 'bg-warning-bg' 
    },
  ];

  return (
    <div className="w-full flex flex-col gap-6 px-4 sm:px-6 lg:px-8 py-8 max-w-full overflow-x-hidden">
      {/* ===== Header ===== */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div className="flex items-center gap-3">
          <div className="grid h-11 w-11 place-items-center rounded-2xl bg-primary text-white">
            <History className="h-5 w-5" />
          </div>
          <div>
            <h1 className="text-xl font-semibold text-foreground">Submission History</h1>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <select
            value={selectedLab}
            onChange={(e) => handleFilterChange(e.target.value)}
            className="rounded-2xl border border-border bg-surface px-4 py-2 text-sm text-foreground-secondary focus:outline-none focus:ring-2 focus:ring-primary"
          >
            {labOptions.map((labName) => (
              <option key={labName} value={labName}>{labName}</option>
            ))}
          </select>
          <button
            onClick={handleRefresh}
            className="p-2 rounded-lg border border-border hover:bg-surface-secondary hover:bg-surface-secondary transition-colors"
            title="Refresh"
          >
            <RefreshCw className={`w-4 h-4 text-foreground-muted ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* ===== Stats Cards ===== */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {loading ? (
          Array.from({ length: 4 }).map((_, index) => (
            <div key={index} className="rounded-3xl border border-border bg-surface p-5 animate-pulse">
              <div className="h-4 w-24 rounded bg-surface-tertiary" />
              <div className="mt-4 h-9 w-16 rounded bg-surface-tertiary" />
            </div>
          ))
        ) : (
          statCards.map((card) => (
            <div key={card.label} className="rounded-3xl border border-border bg-surface p-5">
              <div className="flex items-center justify-between gap-4">
                <p className="text-xs uppercase tracking-[0.2em] text-foreground-muted">{card.label}</p>
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
            <section className="rounded-3xl border border-border bg-surface p-6 animate-pulse">
              <div className="h-4 w-40 rounded bg-surface-tertiary" />
              <div className="mt-6 space-y-4">
                <div className="h-10 rounded bg-surface-tertiary" />
                <div className="h-10 rounded bg-surface-tertiary" />
              </div>
            </section>
            <section className="rounded-3xl border border-border bg-surface p-6 animate-pulse">
              <div className="h-4 w-32 rounded bg-surface-tertiary" />
              <div className="mt-6 h-48 rounded bg-surface-tertiary" />
            </section>
          </>
        ) : (
          <>
        {/* Labs Summary */}
        <section className="rounded-3xl border border-border bg-surface p-6">
          <h2 className="text-sm font-semibold uppercase tracking-[0.2em] text-foreground-muted">
            Performance by Lab
          </h2>
          {labsSummary.length === 0 ? (
            <p className="mt-6 text-sm text-foreground-disabled text-center">No labs attempted yet</p>
          ) : (
            <div className="mt-6 space-y-4">
              {labsSummary.map((lab) => (
                <div key={lab.id} className="space-y-2">
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-foreground-secondary">{lab.name}</span>
                    {lab.bestScore !== null && lab.bestScore !== undefined ? (
                      <span className={`font-semibold ${
                        lab.bestScore >= 90 ? 'text-success-text' :
                        lab.bestScore >= 75 ? 'text-info-text' :
                        lab.bestScore >= 60 ? 'text-warning-text' :
                        'text-error-text'
                      }`}>{formatScore(lab.bestScore)}</span>
                    ) : (
                      <span className="text-foreground-disabled">--</span>
                    )}
                  </div>
                  {lab.bestScore !== null && lab.bestScore !== undefined && <ScoreBar score={lab.bestScore} />}
                  <p className="text-xs text-foreground-muted">
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
        <section className="rounded-3xl border border-border bg-surface p-6 overflow-hidden">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-sm font-semibold uppercase tracking-[0.2em] text-foreground-muted">
                All Submissions
              </h2>
            </div>
          </div>

          {filteredSubmissions.length === 0 ? (
            <div className="py-12 text-center text-foreground-disabled">
              <p>No submissions found</p>
            </div>
          ) : (
            <div className="overflow-x-auto rounded-3xl border border-border">
              <table className="w-full min-w-[720px] border-collapse text-left text-sm">
                <thead className="bg-surface-secondary bg-surface-secondary text-foreground-muted">
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
                          className={`cursor-pointer border-b border-border transition-colors ${
                            index % 2 === 0 ? 'bg-surface' : 'bg-surface-secondary/50'
                          } hover:bg-surface-secondary hover:bg-surface-tertiary`}
                          onClick={() => toggleRow(item.id)}
                        >
                          <td className="px-4 py-4 text-foreground">{item.lab?.name || 'Unknown Lab'}</td>
                          <td className="px-4 py-4 text-foreground-secondary">#{item.attemptNumber}</td>
                          <td className="px-4 py-4">
                            <ScorePill score={item.score} />
                          </td>
                          <td className="px-4 py-4 text-foreground-secondary">
                            {item.submittedAt ? new Date(item.submittedAt).toLocaleString() : '--'}
                          </td>
                          <td className="px-4 py-4">
                            <StatusBadge status={status} />
                          </td>
                          <td className="px-4 py-4 text-foreground-secondary">
                            {isExpanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                          </td>
                        </tr>

                        {/* Expanded Details */}
                        {isExpanded && (
                          <tr className="bg-surface-secondary bg-surface-secondary">
                            <td colSpan={6} className="px-4 py-4">
                              <div className="space-y-4">
                                {/* Challenge Results */}
                                {item.challengeResults && item.challengeResults.length > 0 && (
                                  <div>
                                    <h4 className="text-sm font-semibold text-foreground-secondary mb-2">Challenge Results</h4>
                                    <div className="grid gap-2 md:grid-cols-2 lg:grid-cols-3">
                                      {item.challengeResults.map((cr, idx) => (
                                        <div 
                                          key={idx}
                                          className={`p-3 rounded-lg ${
                                            cr.isCorrect 
                                              ? 'bg-success-bg' 
                                              : 'bg-error-bg'
                                          }`}
                                        >
                                          <div className="flex items-center justify-between">
                                            <span className="text-sm font-medium text-foreground-secondary">
                                              {cr.challengeName || `Challenge ${idx + 1}`}
                                            </span>
                                            {cr.isCorrect ? (
                                              <CheckCircle2 className="w-4 h-4 text-success" />
                                            ) : (
                                              <XCircle className="w-4 h-4 text-error" />
                                            )}
                                          </div>
                                          {cr.score !== undefined && cr.score !== null && (
                                            <span className="text-xs text-foreground-muted">
                                              Score: {formatScore(cr.score)}
                                            </span>
                                          )}
                                        </div>
                                      ))}
                                    </div>
                                  </div>
                                )}

                                {(!item.challengeResults || item.challengeResults.length === 0) && (
                                  <p className="text-sm text-foreground-disabled text-center py-4">
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