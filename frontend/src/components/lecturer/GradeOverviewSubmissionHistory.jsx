import { formatDateTime, formatNumber, formatText } from '../../utils/formatters';
import SortableTableHeader from '../ui/SortableTableHeader';

const HEADER_CLASS = 'px-4 py-3 text-left text-sm font-medium text-foreground-secondary';

const HISTORY_COLUMNS = [
  { key: 'studentName', label: 'Student' },
  { key: 'irn', label: 'ID' },
  { key: 'labName', label: 'Lab' },
  { key: 'submittedAt', label: 'Submitted At' },
  { key: 'score', label: 'Score' },
];

export default function GradeOverviewSubmissionHistory({
  student,
  rows,
  loading,
  error,
  labFilter,
  onLabFilterChange,
  sortState,
  onSort,
  labOptions,
}) {
  const options = Array.isArray(labOptions) && labOptions.length > 0 ? labOptions : ['All Labs'];
  const historyRows = Array.isArray(rows) ? rows : [];

  return (
    <div className="mt-6 rounded-xl border border-border bg-surface shadow-sm transition-colors border-border">
      <div className="flex flex-col gap-4 border-b border-border px-4 py-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h3 className="text-base font-semibold text-foreground">Submission history</h3>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <select
            value={labFilter}
            onChange={(event) => onLabFilterChange?.(event.target.value)}
            className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground-secondary"
          >
            {options.map((labName) => (
              <option key={labName} value={labName}>
                {labName}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="overflow-x-auto">
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          </div>
        ) : error ? (
          <p className="px-4 py-8 text-sm text-warning-text">{error}</p>
        ) : historyRows.length === 0 ? (
          <p className="px-4 py-8 text-center text-sm text-foreground-secondary">No submissions yet.</p>
        ) : (
          <table className="w-full min-w-[640px] table-auto">
            <thead>
              <tr className="border-b border-border">
                {HISTORY_COLUMNS.map((col) => (
                  <SortableTableHeader
                    key={col.key}
                    label={col.label}
                    field={col.key}
                    activeField={sortState?.field}
                    direction={sortState?.direction}
                    onSort={onSort}
                    className={HEADER_CLASS}
                  />
                ))}
              </tr>
            </thead>
            <tbody>
              {historyRows.map((row, index) => (
                <tr key={`${row.labName}-${row.submittedAt}-${index}`} className="border-b border-border">
                  <td className="px-4 py-3 text-sm text-foreground">{formatText(row.studentName)}</td>
                  <td className="px-4 py-3 text-sm text-foreground">{formatText(row.irn)}</td>
                  <td className="px-4 py-3 text-sm text-foreground">{formatText(row.labName)}</td>
                  <td className="px-4 py-3 text-sm text-foreground">{formatDateTime(row.submittedAt)}</td>
                  <td className="px-4 py-3 text-sm font-semibold text-foreground">{formatNumber(row.score)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
