// src/components/student/StudentHistoryPage.jsx
import { Fragment, useState, useEffect, useCallback } from 'react';
import {
  ChevronDown, ChevronUp,
  CheckCircle2, XCircle, RefreshCw, AlertTriangle
} from 'lucide-react';
import {
  BookOpenText,
  ChartLineUp,
  ClockCounterClockwise,
  Medal,
  Stack,
} from '@phosphor-icons/react';
import SortableTableHeader from '../ui/SortableTableHeader';
import { StageCard3D, StaggerGroup, StaggerItem, AnimatedCounter, EmptyState } from '../ui/MotionPrimitives';
import { formatNumber } from '../../utils/formatters';
import { buildServerSortParam, toggleSortState } from '../../utils/sort';
import { isInCurrentTerm } from '../../utils/authRoutes';
import { apiFetch } from '../../utils/apiFetch';
import { authHeaders } from '../../utils/authHeaders';

const HISTORY_PAGE_SIZE = 10;

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
const IS_DESKTOP_APP = import.meta.env.VITE_APP_MODE === 'desktop';

function roundedScore(value) {
  if (value === null || value === undefined) return null;
  const num = Number(value);
  if (Number.isNaN(num)) return null;
  return Math.floor(num);
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
    ? 'bg-success'
    : numericScore >= 75
      ? 'bg-info'
      : numericScore >= 60
        ? 'bg-warning'
        : 'bg-error';
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
  const [pageError, setPageError] = useState(null);

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

    const response = await apiFetch(`${API_BASE}/api/submissions/my-history?${params.toString()}`, {
      headers: authHeaders(),
    });

    if (!response.ok) {
      throw new Error('Could not load submission history. Please try again.');
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
    const response = await apiFetch(`${API_BASE}/api/submissions/my-labs`, {
      headers: authHeaders(),
    });

    if (!response.ok) {
      throw new Error('Could not load lab summary. Please try again.');
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
    setPageError(null);
    try {
      const historyData = await fetchHistoryData({ labId, page, sortState });
      applyHistoryData(historyData, { syncStats });
    } catch (err) {
      setPageError(err.message || 'Could not load submission history.');
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
    setPageError(null);
    try {
      const [historyData, labsData] = await Promise.all([
        fetchHistoryData({ labId, page, sortState }),
        fetchLabsSummaryData(),
      ]);
      applyHistoryData(historyData, { syncStats: true });
      setLabsSummary(labsData);
    } catch (err) {
      setPageError(err.message || 'Could not load submission history.');
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
      rawValue: stats.labsAttempted ?? 0,
      displayValue: formatStatValue(stats.labsAttempted ?? 0),
      icon: <BookOpenText size={20} weight="duotone" aria-hidden="true" />,
      tone: 'text-navy-text dark:text-primary-text',
      bg: 'bg-navy-light dark:bg-primary-light',
    },
    {
      label: 'Total Submissions',
      rawValue: stats.totalSubmissions ?? 0,
      displayValue: formatStatValue(stats.totalSubmissions ?? 0),
      icon: <Stack size={20} weight="duotone" aria-hidden="true" />,
      tone: 'text-secondary-text',
      bg: 'bg-secondary-light',
    },
    {
      label: 'Average Score',
      rawValue: stats.averageScore,
      displayValue: formatStatValue(stats.averageScore),
      icon: <ChartLineUp size={20} weight="duotone" aria-hidden="true" />,
      tone: 'text-success-text',
      bg: 'bg-success-bg',
    },
    {
      label: 'Best Score',
      rawValue: stats.bestScore,
      displayValue: formatStatValue(stats.bestScore),
      icon: <Medal size={20} weight="duotone" aria-hidden="true" />,
      tone: 'text-gold-text',
      bg: 'bg-gold-bg',
    },
  ];

  const enrolledInCurrentTerm = inCurrentTerm ?? isInCurrentTerm(user?.inCurrentTerm);

  return (
    <div className="flex w-full max-w-full flex-col gap-6 py-4 sm:py-8">
      {!enrolledInCurrentTerm && (
        <div
          role="status"
          className="flex items-start gap-3 rounded-xl bg-warning-bg px-4 py-3 text-sm text-warning-text"
        >
          <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-warning" aria-hidden />
          <p>
            You do not belong to any class in this quarter. If you do, please contact your lecturer for submission permissions.
          </p>
        </div>
      )}

      {pageError && (
        <div
          role="alert"
          className="flex items-start gap-3 rounded-xl bg-error-bg px-4 py-3 text-sm text-error-text"
        >
          <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-error" aria-hidden />
          <p>{pageError}</p>
        </div>
      )}

      {/* ===== Header ===== */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div className="flex items-center gap-3">
          <div className="grid h-11 w-11 place-items-center rounded-2xl bg-navy text-white shadow-stage-1">
            <ClockCounterClockwise size={22} weight="duotone" aria-hidden="true" />
          </div>
          <div>
            <h1 className="text-xl font-bold tracking-tight text-foreground">Submission History</h1>
          </div>
        </div>
        <button
          type="button"
          onClick={handleRefresh}
          className="inline-flex min-h-11 min-w-11 items-center justify-center rounded-xl border border-border bg-surface transition-colors hover:bg-surface-secondary"
          title="Refresh"
        >
          <RefreshCw className={`w-4 h-4 text-foreground-muted ${isPageBusy ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* ===== Stats Cards ===== */}
      <StaggerGroup className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {initialLoading ? (
          Array.from({ length: 4 }).map((_, index) => (
            <div key={index} className="rounded-2xl border border-border bg-surface p-5 animate-pulse">
              <div className="h-4 w-24 rounded bg-surface-tertiary" />
              <div className="mt-4 h-9 w-16 rounded bg-surface-tertiary" />
            </div>
          ))
        ) : (
          statCards.map((card) => (
            <StaggerItem key={card.label} className="h-full">
              <StageCard3D className="h-full flex flex-col justify-between p-5">
                <div className="flex items-center justify-between gap-4">
                  <p className="text-xs font-semibold uppercase tracking-[0.16em] text-foreground-muted">{card.label}</p>
                  <div className={`rounded-xl p-2.5 ${card.bg} ${card.tone}`}>
                    {card.icon}
                  </div>
                </div>
                <p className={`mt-4 text-2xl font-bold tabular-nums sm:text-3xl ${card.tone}`}>
                  {typeof card.rawValue === 'number'
                    ? <AnimatedCounter value={Math.floor(card.rawValue)} formatter={formatNumber} />
                    : card.displayValue}
                </p>
              </StageCard3D>
            </StaggerItem>
          ))
        )}
      </StaggerGroup>

      {/* ===== Labs Summary Sidebar + Submissions Table ===== */}
      <div className="grid gap-6 lg:grid-cols-[minmax(0,3fr)_minmax(0,7fr)]">
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
        <section className="rounded-2xl border border-border bg-surface p-6 shadow-stage-1">
          <h2 className="text-xs font-semibold uppercase tracking-[0.2em] text-foreground-muted">
            Performance by Lab
          </h2>
          {labsSummary.length === 0 ? (
            <EmptyState compact title="No labs attempted yet" description="Submit a lab folder to track progress over the quarter." />
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
                        {lab.termLabel && (
                          <span className="mt-0.5 block text-xs font-normal text-foreground-muted">
                            {lab.termLabel}
                          </span>
                        )}
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
        <section className="relative rounded-2xl border border-border bg-surface p-6 shadow-stage-1 overflow-hidden">
          {tableLoading && (
            <div className="absolute inset-0 z-10 flex items-center justify-center rounded-2xl bg-surface/80">
              <RefreshCw className="h-6 w-6 animate-spin text-foreground-muted" />
            </div>
          )}
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-xs font-semibold uppercase tracking-[0.2em] text-foreground-muted">
                All Submissions
              </h2>
            </div>
          </div>

          {submissions.length === 0 ? (
            <EmptyState compact title="No submissions found" description="Try another lab filter or upload your first submission." />
          ) : (
            <div className="-mx-6 overflow-x-auto border-t border-border-subtle">
              <table className="w-full min-w-[720px] border-collapse text-left text-sm">
                <thead className="bg-surface-secondary text-foreground-muted">
                  <tr>
                    <SortableTableHeader label="Lab" field="labName" activeField={historySort.field} direction={historySort.direction} onSort={handleHistorySort} className="px-4 py-3" stopRowClick />
                    <SortableTableHeader label="Attempt" field="attempt" activeField={historySort.field} direction={historySort.direction} onSort={handleHistorySort} className="px-4 py-3" stopRowClick />
                    <SortableTableHeader label="Score" field="score" activeField={historySort.field} direction={historySort.direction} onSort={handleHistorySort} className="px-4 py-3" stopRowClick />
                    <SortableTableHeader label="Submitted" field="submittedAt" activeField={historySort.field} direction={historySort.direction} onSort={handleHistorySort} className="px-4 py-3" stopRowClick />
                    {IS_DESKTOP_APP && (
                      <th className="px-4 py-3 font-medium">Rubric pack</th>
                    )}
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
                          <td className="px-4 py-4 text-foreground">
                            <span>{item.lab?.name || 'Unknown Lab'}</span>
                            {item.lab?.termLabel && (
                              <span className="mt-0.5 block text-xs text-foreground-muted">
                                {item.lab.termLabel}
                              </span>
                            )}
                          </td>
                          <td className="px-4 py-4 text-foreground-secondary">#{item.attemptNumber}</td>
                          <td className="px-4 py-4">
                            <ScorePill score={item.score} />
                          </td>
                          <td className="px-4 py-4 text-foreground-secondary">
                            {item.submittedAt ? new Date(item.submittedAt).toLocaleString() : '--'}
                          </td>
                          {IS_DESKTOP_APP && (
                            <td className="max-w-[12rem] truncate px-4 py-4 text-xs text-foreground-muted" title={item.desktopPackVersion || ''}>
                              {item.desktopPackVersion || '—'}
                            </td>
                          )}
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
                            <td colSpan={IS_DESKTOP_APP ? 7 : 6} className="px-4 py-4">
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
                <div className="flex flex-col gap-3 border-t border-border px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
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