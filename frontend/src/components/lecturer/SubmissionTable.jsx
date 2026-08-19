import { Eye } from 'lucide-react';
import SortableTableHeader from '../ui/SortableTableHeader';
import { formatNumber, formatText } from '../../utils/formatters';
import PlagiarismDangerMark from './PlagiarismDangerMark';

const HEADER_CLASS = 'px-4 py-3 text-left text-sm font-medium text-foreground-secondary';

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
  attemptLabel = 'Attempt',
  viewLabel = 'View',
  requireSubmissionForView = true,
  sortState,
  onSort,
}) {
  const rows = Array.isArray(submissions) ? submissions : [];
  const showPagination = pagination && (pagination.totalPages > 1 || pagination.total > pagination.size);

  return (
    <div className="overflow-x-auto rounded-xl border border-border bg-surface shadow-sm transition-colors border-border">
      <table className="w-full table-auto">
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
              return (
                <tr key={`${submission.studentCode ?? submission.studentId ?? 'row'}-${index}`} className="border-b border-border">
                  <td className="px-4 py-3 text-sm text-foreground">{formatText(submission.studentName)}</td>
                  <td className="px-4 py-3 text-sm text-foreground">{formatText(submission.studentCode)}</td>
                  <td className="px-4 py-3 text-sm font-semibold text-foreground">{formatNumber(submission.score)}</td>
                  <td className="px-4 py-3 text-sm text-foreground">{formatNumber(submission.attempt ?? submission.attempts)}</td>
                  <td className="px-4 py-3 text-sm text-foreground">{submission.submittedAt || '—'}</td>
                  <td className="px-4 py-3 align-middle">
                    <PlagiarismDangerMark show={Boolean(submission.plagiarismFlagged)} className="ml-0" />
                  </td>
                  <td className="px-4 py-3">
                    <button
                      type="button"
                      disabled={!canView}
                      onClick={() => onView?.(submission)}
                      className="flex items-center gap-1 rounded-lg bg-success px-3 py-1.5 text-xs text-white transition-colors hover:bg-success-hover disabled:cursor-not-allowed disabled:bg-foreground-disabled disabled:hover:bg-foreground-disabled"
                    >
                      <Eye className="h-3 w-3" />
                      {viewLabel}
                    </button>
                  </td>
                </tr>
              );
            })
          )}

          {(summary?.submissionCount != null || summary?.studentCount != null || summary?.completionRate != null) && (
            <tr className="border-t border-primary bg-primary-light">
              <td colSpan={6} className="px-4 py-4">
                <div className="grid grid-cols-4 items-center gap-4 text-sm font-semibold text-primary-text">
                  <span className="font-bold text-primary-text">SUMMARY</span>
                  <span className="text-center">
                    Submissions: <span className="text-primary-text">{formatNumber(summary?.submissionCount)}</span>
                  </span>
                  <span className="text-center">
                    Enrolled: <span className="text-primary-text">{formatNumber(summary?.studentCount)}</span>
                  </span>
                  <span className="text-center">
                    Completion: <span className="text-primary-text">{formatNumber(summary?.completionRate)}</span>
                  </span>
                </div>
              </td>
            </tr>
          )}
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
