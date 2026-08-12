import { formatDateTime, formatNumber, formatText } from '../../utils/formatters';
import SortableTableHeader from '../ui/SortableTableHeader';

const HEADER_CLASS = 'px-4 py-3 text-left text-sm font-medium text-gray-700 dark:text-gray-300';

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
    <div className="mt-6 rounded-xl border border-gray-200 bg-white shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
      <div className="flex flex-col gap-4 border-b border-gray-200 px-4 py-4 dark:border-gray-700 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h3 className="text-base font-semibold text-gray-900 dark:text-white">Submission history</h3>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <select
            value={labFilter}
            onChange={(event) => onLabFilterChange?.(event.target.value)}
            className="rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-700 dark:border-gray-700 dark:bg-[#151b24] dark:text-gray-200"
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
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-purple-500 border-t-transparent" />
          </div>
        ) : error ? (
          <p className="px-4 py-8 text-sm text-amber-700 dark:text-amber-300">{error}</p>
        ) : historyRows.length === 0 ? (
          <p className="px-4 py-8 text-center text-sm text-gray-500 dark:text-gray-400">No submissions yet.</p>
        ) : (
          <table className="w-full min-w-[640px] table-auto">
            <thead>
              <tr className="border-b border-gray-200 dark:border-gray-700">
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
                <tr key={`${row.labName}-${row.submittedAt}-${index}`} className="border-b border-gray-200 dark:border-gray-700">
                  <td className="px-4 py-3 text-sm text-gray-800 dark:text-gray-200">{formatText(row.studentName)}</td>
                  <td className="px-4 py-3 text-sm text-gray-800 dark:text-gray-200">{formatText(row.irn)}</td>
                  <td className="px-4 py-3 text-sm text-gray-800 dark:text-gray-200">{formatText(row.labName)}</td>
                  <td className="px-4 py-3 text-sm text-gray-800 dark:text-gray-200">{formatDateTime(row.submittedAt)}</td>
                  <td className="px-4 py-3 text-sm font-semibold text-gray-900 dark:text-white">{formatNumber(row.score)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
