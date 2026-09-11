import { Eye, GitCompare, FileCheck2, Users, TrendingUp, ShieldAlert } from 'lucide-react';
import SortableTableHeader from '../ui/SortableTableHeader';
import { formatNumber, formatPercent, formatText } from '../../utils/formatters';
import PlagiarismDangerMark, { studentLabOverlapPercent } from './PlagiarismDangerMark';

const HEADER_CLASS = 'px-4 py-3 text-left text-sm font-medium text-foreground-secondary';

const SUMMARY_ITEMS = [
  {
    key: 'studentsSubmitted',
    label: 'Submitted',
    icon: FileCheck2,
    accent: 'bg-success-bg text-success-text',
    format: (summary) => formatNumber(summary?.studentsSubmitted),
  },
  {
    key: 'studentCount',
    label: 'Enrolled',
    icon: Users,
    accent: 'bg-primary-light text-primary-text',
    format: (summary) => formatNumber(summary?.studentCount),
  },
  {
    key: 'completionRate',
    label: 'Completion',
    icon: TrendingUp,
    accent: 'bg-info-bg text-info-text',
    format: (summary) => formatPercent(summary?.completionRate),
  },
  {
    key: 'plagiarismRate',
    label: 'Plagiarism',
    icon: ShieldAlert,
    accent: 'bg-warning-bg text-warning-text',
    format: (summary) => formatPercent(summary?.plagiarismRate),
    valueClass: (summary) => ((summary?.plagiarismRate ?? 0) > 0 ? 'text-warning-text' : 'text-foreground'),
  },
];

function RosterSummaryBar({ summary }) {
  const items = SUMMARY_ITEMS.filter((item) => summary?.[item.key] != null);
  if (items.length === 0) return null;

  return (
    <div className="border-t border-border bg-surface-secondary/50 px-4 py-3">
      <div className="grid grid-cols-2 gap-2 sm:grid-cols-4 sm:gap-3">
        {items.map((item, index) => {
          const Icon = item.icon;
          const valueClass = item.valueClass?.(summary) ?? 'text-foreground';
          const isLast = index === items.length - 1;
          return (
            <div
              key={item.key}
              className={`flex items-center gap-3 rounded-lg border px-3 py-2.5 shadow-sm ${
                isLast
                  ? 'border-warning-bg/70 bg-warning-bg/30'
                  : 'border-border-subtle bg-surface'
              }`}
            >
              <div className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg ${item.accent}`}>
                <Icon className="h-4 w-4" aria-hidden="true" />
              </div>
              <div className="min-w-0">
                <p className="text-[11px] font-medium uppercase tracking-wide text-foreground-muted">{item.label}</p>
                <p className={`text-lg font-semibold tabular-nums leading-tight ${valueClass}`}>
                  {item.format(summary)}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

const ROSTER_COLUMNS = [
  { key: 'studentName', label: 'Student' },
  { key: 'studentCode', label: 'ID' },
  { key: 'score', label: 'Score' },
  { key: 'attempt', label: 'Attempt', labelKey: 'attemptLabel' },
  { key: 'submittedAt', label: 'Submitted At' },
];

export default function SubmissionTable({
  submissions,
  summary,
  pagination,
  onPageChange,
  onView,
  onViewPlagiarism,
  attemptLabel = 'Attempt',
  viewLabel = 'View Submission',
  requireSubmissionForView = true,
  sortState,
  onSort,
  labId = null,
  overlapByStudentAndLab = null,
}) {
  const rows = Array.isArray(submissions) ? submissions : [];
  const showPagination = pagination && (pagination.totalPages > 1 || pagination.total > pagination.size);

  return (
    <div className="overflow-x-auto rounded-xl border border-border bg-surface shadow-sm transition-colors border-border">
      <table className="w-full min-w-[640px] table-auto">
        <thead>
          <tr className="border-b border-border">
            {ROSTER_COLUMNS.map((col) => (
              <SortableTableHeader
                key={col.key}
                label={col.labelKey === 'attemptLabel' ? attemptLabel : col.label}
                field={col.key}
                activeField={sortState?.field}
                direction={sortState?.direction}
                onSort={onSort}
                className={HEADER_CLASS}
              />
            ))}
            <SortableTableHeader label="Plagiarism" sortable={false} className={HEADER_CLASS} />
            <SortableTableHeader label="Action" sortable={false} className={HEADER_CLASS} />
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr>
              <td colSpan={7} className="px-4 py-10 text-center text-sm text-foreground-secondary">
                No student data found
              </td>
            </tr>
          ) : (
            rows.map((submission, index) => {
              const canView = requireSubmissionForView ? submission.hasSubmission !== false : true;
              const flagged = Boolean(submission.plagiarismFlagged);
              const plagiarismRole = submission.plagiarismRole ?? null;
              const overlapPercent = flagged
                ? studentLabOverlapPercent(submission.studentId, labId, overlapByStudentAndLab)
                : null;
              const showOverlap = flagged && overlapPercent != null && overlapPercent > 0;
              return (
                <tr key={`${submission.studentCode ?? submission.studentId ?? 'row'}-${index}`} className="border-b border-border">
                  <td className="px-4 py-3 text-sm text-foreground">{formatText(submission.studentName)}</td>
                  <td className="px-4 py-3 text-sm text-foreground">{formatText(submission.studentCode)}</td>
                  <td className="px-4 py-3 text-sm font-semibold text-foreground">{formatNumber(submission.score)}</td>
                  <td className="px-4 py-3 text-sm text-foreground">{formatNumber(submission.attempt ?? submission.attempts)}</td>
                  <td className="px-4 py-3 text-sm text-foreground">{submission.submittedAt || '—'}</td>
                  <td className="px-4 py-3 align-middle">
                    {flagged ? (
                      <span className="inline-flex items-center gap-0.5 whitespace-nowrap">
                        <PlagiarismDangerMark show role={plagiarismRole} className="ml-0" />
                        {showOverlap ? (
                          <span className="text-[10px] font-medium text-warning-text dark:text-warning">
                            {Math.round(overlapPercent)}%
                          </span>
                        ) : null}
                      </span>
                    ) : null}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap items-center gap-2">
                      <button
                        type="button"
                        disabled={!canView}
                        onClick={() => onView?.(submission)}
                        className="flex min-h-10 items-center gap-1 rounded-lg bg-success px-3 py-2 text-xs text-white transition-colors hover:bg-success-hover disabled:cursor-not-allowed disabled:bg-foreground-disabled disabled:hover:bg-foreground-disabled sm:text-sm"
                      >
                        <Eye className="h-3 w-3 shrink-0" />
                        <span className="hidden sm:inline">{viewLabel}</span>
                        <span className="sm:hidden">View</span>
                      </button>
                      {flagged ? (
                        <button
                          type="button"
                          onClick={() => onViewPlagiarism?.(submission)}
                          className="flex min-h-10 items-center gap-1 rounded-lg bg-warning px-3 py-2 text-xs text-white transition-colors hover:bg-warning-hover sm:text-sm"
                        >
                          <GitCompare className="h-3 w-3 shrink-0" />
                          <span className="hidden sm:inline">View Plagiarism</span>
                          <span className="sm:hidden">Plagiarism</span>
                        </button>
                      ) : null}
                    </div>
                  </td>
                </tr>
              );
            })
          )}
        </tbody>
      </table>

      <RosterSummaryBar summary={summary} />

      {showPagination && (
        <div className="flex flex-col gap-3 border-t border-border px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-sm text-foreground-secondary">
            Page {pagination.page + 1} of {Math.max(pagination.totalPages, 1)}
          </p>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={pagination.page <= 0}
              onClick={() => onPageChange?.(pagination.page - 1)}
              className="rounded-lg border border-border px-3 py-1.5 text-sm disabled:opacity-50"
            >
              Previous
            </button>
            <button
              type="button"
              disabled={pagination.page >= pagination.totalPages - 1}
              onClick={() => onPageChange?.(pagination.page + 1)}
              className="rounded-lg border border-border px-3 py-1.5 text-sm disabled:opacity-50"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
