import { formatPercent, formatText } from '../../utils/formatters';

export default function GradeOverviewTable({
  labs,
  students,
  pagination,
  onPageChange,
  loading,
}) {
  const labColumns = Array.isArray(labs) ? labs : [];
  const rows = Array.isArray(students) ? students : [];
  const showPagination = pagination && (pagination.totalPages > 1 || pagination.total > pagination.size);

  return (
    <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white shadow-sm transition-colors dark:border-gray-700 dark:bg-[#1e2530]">
      <table className="w-full min-w-[640px] table-auto">
        <thead>
          <tr className="border-b border-gray-200 dark:border-gray-700">
            <th className="px-4 py-3 text-left text-sm font-medium text-gray-700 dark:text-gray-300">Student</th>
            <th className="px-4 py-3 text-left text-sm font-medium text-gray-700 dark:text-gray-300">IRN</th>
            <th className="px-4 py-3 text-left text-sm font-medium text-gray-700 dark:text-gray-300">Total Score</th>
            {labColumns.map((lab) => (
              <th key={lab.labId} className="px-4 py-3 text-left text-sm font-medium text-gray-700 dark:text-gray-300">
                {formatText(lab.labName)}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr>
              <td colSpan={3 + labColumns.length} className="px-4 py-10 text-center text-sm text-gray-500 dark:text-gray-400">
                Loading grade overview...
              </td>
            </tr>
          ) : rows.length === 0 ? (
            <tr>
              <td colSpan={3 + labColumns.length} className="px-4 py-10 text-center text-sm text-gray-500 dark:text-gray-400">
                No student data found
              </td>
            </tr>
          ) : (
            rows.map((student) => (
              <tr key={student.studentId} className="border-b border-gray-200 dark:border-gray-700">
                <td className="px-4 py-3 text-sm text-gray-800 dark:text-gray-200">{formatText(student.studentName)}</td>
                <td className="px-4 py-3 text-sm text-gray-800 dark:text-gray-200">{formatText(student.irn)}</td>
                <td className="px-4 py-3 text-sm font-semibold text-gray-900 dark:text-white">{formatPercent(student.totalScore)}</td>
                {(student.labScores ?? []).map((score, index) => (
                  <td key={`${student.studentId}-${labColumns[index]?.labId ?? index}`} className="px-4 py-3 text-sm text-gray-800 dark:text-gray-200">
                    {formatPercent(score)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>

      {showPagination && (
        <div className="flex items-center justify-between border-t border-gray-200 px-4 py-3 dark:border-gray-700">
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Page {pagination.page + 1} of {Math.max(pagination.totalPages, 1)}
          </p>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={pagination.page <= 0}
              onClick={() => onPageChange?.(pagination.page - 1)}
              className="rounded-lg border border-gray-200 px-3 py-1.5 text-sm disabled:opacity-50 dark:border-gray-700"
            >
              Previous
            </button>
            <button
              type="button"
              disabled={pagination.page >= pagination.totalPages - 1}
              onClick={() => onPageChange?.(pagination.page + 1)}
              className="rounded-lg border border-gray-200 px-3 py-1.5 text-sm disabled:opacity-50 dark:border-gray-700"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
