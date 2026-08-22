// src/components/student/StudentHistoryPage.jsx
import { Fragment, useState, useEffect, useCallback } from 'react';
import { 
  History, TrendingUp, Award, Clock, ChevronDown, ChevronUp, 
  CheckCircle2, XCircle, RefreshCw, AlertTriangle
} from 'lucide-react';
import SortableTableHeader from '../ui/SortableTableHeader';
import { formatNumber } from '../../utils/formatters';
import { buildServerSortParam, toggleSortState } from '../../utils/sort';
import { isInCurrentTerm } from '../../utils/authRoutes';

const HISTORY_PAGE_SIZE = 10;

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

function roundedScore(value) {
  if (value === null || value === undefined) return null;
  const num = Number(value);
  if (Number.isNaN(num)) return null;
  return Math.round(num);
}

function deriveSubmissionStatus(score) {
  const numericScore = roundedScore(score);
  if (numericScore == null) return 'unknown';
  if (numericScore < 50) return 'failed';
  if (numericScore > 80) return 'passed';
  return 'partial';
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
  const numericScore = roundedScore(score);
  if (numericScore == null) {
    return <span className="text-foreground-disabled text-sm">--</span>;
  }
  const color = numericScore >= 90 
    ? 'text-success-text' 
    : numericScore >= 75 
      ? 'text-info-text' 
      : numericScore >= 60 
        ? 'text-warning-text' 
        : 'text-error-text';
  return <span className={`font-semibold ${color}`}>{formatNumber(score)}</span>;
}

function ScoreBar({ score }) {
  const numericScore = roundedScore(score);
  if (numericScore == null) return null;
  const color = numericScore >= 90 
    ? 'bg-gradient-to-r from-success to-success-hover' 
    : numericScore >= 75 
      ? 'bg-gradient-to-r from-info to-info-hover' 
      : numericScore >= 60 
        ? 'bg-gradient-to-r from-warning to-warning-hover' 
        : 'bg-gradient-to-r from-error to-error-hover';
  return (
    <div className="h-2 overflow-hidden rounded-full bg-surface-tertiary">
      <div className={`h-full rounded-full ${color}`} style={{ width: `${Math.min(numericScore, 100)}%` }} />
    </div>
  );
}

// ==================== MAIN COMPONENT ====================

export default function StudentHistoryPage({ user, onLogout, onNavigate, inCurrentTerm }) {
  // ===== States =====
  const [submissions, setSubmissions] = useState([]);
  const [selectedLabId, setSelectedLabId] = useState(null);
  const [stats, setStats] = useState({
    totalSubmissions: 0,
    averageScore: null,
    bestScore: null,
    labsAttempted: 0,
  });
  const [labsSummary, setLabsSummary] = useState([]);
  const [expandedRow, setExpandedRow] = useState(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const [tableLoading, setTableLoading] = useState(false);
  const [historySort, setHistorySort] = useState({ field: 'submittedAt', direction: 'desc' });
  const [pagination, setPagination] = useState({
    page: 0,
    size: HISTORY_PAGE_SIZE,
    total: 0,
    totalPages: 0,
  });

  const emptyStats = {
    totalSubmissions: 0,
    averageScore: null,
    bestScore: null,
    labsAttempted: 0,
  };

  const fetchHistoryData = useCallback(async ({ labId = null, page = 0, sortState = historySort } = {}) => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(HISTORY_PAGE_SIZE),
    });
    if (labId) {
      params.set('labId', labId);
    }
    const sortParam = buildServerSortParam(sortState);
    if (sortParam) {
      params.set('sort', sortParam);
    }

    const response = await fetch(`${API_BASE}/api/submissions/my-history?${params.toString()}`, {
      headers: {
        Authorization: `Bearer ${sessionStorage.getItem('accessToken')}`,
      },
    });

    if (!response.ok) {
      console.info('History API not available yet, using empty data');
      return {
        submissions: [],
        stats: emptyStats,
        page: 0,
        size: HISTORY_PAGE_SIZE,
        totalElements: 0,
        totalPages: 0,
      };
    }

    const data = await response.json();
    return {
      submissions: data.submissions || [],
      stats: data.stats || emptyStats,
      page: data.page ?? 0,
      size: data.size ?? HISTORY_PAGE_SIZE,
      totalElements: data.totalElements ?? 0,
      totalPages: data.totalPages ?? 0,
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
      return [];
    }

    const data = await response.json();
    return data || [];
  }, []);

  const applyHistoryData = useCallback((historyData, { syncStats = false } = {}) => {
    setSubmissions(historyData.submissions);
    setPagination({
      page: historyData.page,
      size: historyData.size,
      total: historyData.totalElements,
      totalPages: historyData.totalPages,
    });
    if (syncStats) {
      setStats(historyData.stats);
    }
  }, []);

  const loadHistorySection = useCallback(async ({
    labId = null,
    page = 0,
    sortState = historySort,
    syncStats = false,
  } = {}) => {
    setTableLoading(true);
    try {
      const historyData = await fetchHistoryData({ labId, page, sortState });
      applyHistoryData(historyData, { syncStats });
    } catch (err) {
      console.info('Could not fetch submission history:', err.message);
      setSubmissions([]);
      if (syncStats) {
        setStats(emptyStats);
      }
      setPagination({ page: 0, size: HISTORY_PAGE_SIZE, total: 0, totalPages: 0 });
    } finally {
      setTableLoading(false);
    }
  }, [applyHistoryData, fetchHistoryData, historySort]);

  const loadFullPage = useCallback(async ({
    labId = null,
    page = 0,
    sortState = historySort,
  } = {}) => {
    setInitialLoading(true);
    try {
      const [historyData, labsData] = await Promise.all([
        fetchHistoryData({ labId, page, sortState }),
        fetchLabsSummaryData(),
      ]);
      applyHistoryData(historyData, { syncStats: true });
      setLabsSummary(labsData);
    } catch (err) {
      console.info('Could not fetch submission history:', err.message);
      setSubmissions([]);
      setStats(emptyStats);
      setPagination({ page: 0, size: HISTORY_PAGE_SIZE, total: 0, totalPages: 0 });
      setLabsSummary([]);
    } finally {
      setInitialLoading(false);
    }
  }, [applyHistoryData, fetchHistoryData, fetchLabsSummaryData, historySort]);

  useEffect(() => {
    void loadFullPage();
    // Initial load only; pagination/sort/filter handlers fetch targeted sections.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleLabSelect = (labId) => {
    const nextId = String(selectedLabId) === String(labId) ? null : labId;
    setSelectedLabId(nextId);
    setExpandedRow(null);
    void loadHistorySection({
      labId: nextId,
      page: 0,
      sortState: historySort,
      syncStats: true,
    });
  };

  const handleRefresh = () => {
    void loadFullPage({
      labId: selectedLabId,
      page: pagination.page,
      sortState: historySort,
    });
  };

  const handlePageChange = (nextPage) => {
    setExpandedRow(null);
    void loadHistorySection({
      labId: selectedLabId,
      page: nextPage,
      sortState: historySort,
    });
  };

  const toggleRow = (id) => {
    setExpandedRow(expandedRow === id ? null : id);
  };

  const handleHistorySort = (field) => {
    const nextSort = toggleSortState(historySort, field);
    setHistorySort(nextSort);
    setExpandedRow(null);
    void loadHistorySection({
      labId: selectedLabId,
      page: 0,
      sortState: nextSort,
    });
  };

  const showPagination = pagination.totalPages > 1 || pagination.total > pagination.size;
  const isPageBusy = initialLoading || tableLoading;

  const formatStatValue = (value) => {
    if (value === null || value === undefined) return '--';
    if (typeof value === 'number') {
      return formatNumber(value);
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

  const enrolledInCurrentTerm = inCurrentTerm ?? isInCurrentTerm(user?.inCurrentTerm);

  return (
    <div className="w-full flex flex-col gap-6 px-4 sm:px-6 lg:px-8 py-8 max-w-full overflow-x-hidden">
      {!enrolledInCurrentTerm && (
        <div
          role="status"
          className="flex items-start gap-3 rounded-xl bg-warning-bg px-4 py-3 text-sm text-warning-text"
        >
          <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-warning" aria-hidden />
          <p>
            You do not belong to any class in this term. If you do, please contact your lecturer for submission permissions.
          </p>
        </div>
      )}

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
        <button
          type="button"
          onClick={handleRefresh}
          className="p-2 rounded-lg border border-border hover:bg-surface-secondary transition-colors"
          title="Refresh"
        >
          <RefreshCw className={`w-4 h-4 text-foreground-muted ${isPageBusy ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* ===== Stats Cards ===== */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {initialLoading ? (
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
      <div className="grid gap-6 xl:grid-cols-[minmax(0,3fr)_minmax(0,7fr)]">
        {initialLoading ? (
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
            <div className="mt-6 space-y-2">
              {labsSummary.map((lab) => {
                const isSelected = String(selectedLabId) === String(lab.id);
                return (
                  <button
                    key={lab.id}
                    type="button"
                    aria-pressed={isSelected}
                    onClick={() => handleLabSelect(lab.id)}
                    className={`w-full rounded-xl px-3 py-2 text-left transition-colors ${
                      isSelected
                        ? 'bg-primary-light'
                        : 'hover:bg-surface-secondary'
                    }`}
                  >
                    <div className="flex items-center justify-between gap-3 text-sm">
                      <span className={isSelected ? 'font-medium text-primary-text' : 'text-foreground-secondary'}>
                        {lab.name}
                      </span>
                      {lab.bestScore !== null && lab.bestScore !== undefined ? (
                        <span className={`font-semibold ${
                          lab.bestScore >= 90 ? 'text-success-text' :
                          lab.bestScore >= 75 ? 'text-info-text' :
                          lab.bestScore >= 60 ? 'text-warning-text' :
                          'text-error-text'
                        }`}>{formatNumber(lab.bestScore)}</span>
                      ) : (
                        <span className="text-foreground-disabled">--</span>
                      )}
                    </div>
                    {lab.bestScore !== null && lab.bestScore !== undefined && (
                      <div className="mt-2">
                        <ScoreBar score={lab.bestScore} />
                      </div>
                    )}
                    <p className="mt-2 text-xs text-foreground-muted">
                      {lab.attempts || 0} attempt{lab.attempts > 1 ? 's' : ''}
                      {lab.lastSubmittedAt && (
                        <> · Last: {new Date(lab.lastSubmittedAt).toLocaleDateString()}</>
                      )}
                    </p>
                  </button>
                );
              })}
            </div>
          )}
        </section>

        {/* Submissions Table */}
        <section className="relative rounded-3xl border border-border bg-surface p-6 overflow-hidden">
          {tableLoading && (
            <div className="absolute inset-0 z-10 flex items-center justify-center rounded-3xl bg-surface/70 backdrop-blur-[1px]">
              <RefreshCw className="h-6 w-6 animate-spin text-foreground-muted" />
            </div>
          )}
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-sm font-semibold uppercase tracking-[0.2em] text-foreground-muted">
                All Submissions
              </h2>
            </div>
          </div>

          {submissions.length === 0 ? (
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
                  {submissions.map((item, index) => {
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
                                              Score: {formatNumber(cr.score)}
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
              {showPagination && (
                <div className="flex items-center justify-between border-t border-border px-4 py-3">
                  <p className="text-sm text-foreground-secondary">
                    Page {pagination.page + 1} of {Math.max(pagination.totalPages, 1)}
                  </p>
                  <div className="flex gap-2">
                    <button
                      type="button"
                      disabled={pagination.page <= 0}
                      onClick={() => handlePageChange(pagination.page - 1)}
                      className="rounded-lg border border-border px-3 py-1.5 text-sm disabled:opacity-50"
                    >
                      Previous
                    </button>
                    <button
                      type="button"
                      disabled={pagination.page >= pagination.totalPages - 1}
                      onClick={() => handlePageChange(pagination.page + 1)}
                      className="rounded-lg border border-border px-3 py-1.5 text-sm disabled:opacity-50"
                    >
                      Next
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </section>
          </>
        )}
      </div>
    </div>
  );
}