import { formatNumber, formatText } from '../../utils/formatters';
import SortableTableHeader from '../ui/SortableTableHeader';

const HEADER_CLASS = 'px-4 py-3 text-left text-sm font-medium text-foreground-secondary';

export default function GradeOverviewTable({
  labs,
  students,
  pagination,
  onPageChange,
  loading,
  selectedStudentId,
  onStudentSelect,
  sortState,
  onSort,
}) {
  const labColumns = Array.isArray(labs) ? labs : [];
  const rows = Array.isArray(students) ? students : [];
  const showPagination = pagination && (pagination.totalPages > 1 || pagination.total > pagination.size);

  return (
    <div className="overflow-x-auto rounded-xl border border-border bg-surface shadow-sm transition-colors border-border">
      <table className="w-full min-w-[640px] table-auto">
        <thead>
          <tr className="border-b border-border">
            <SortableTableHeader
              label="Student"
              field="studentName"
              activeField={sortState?.field}
              direction={sortState?.direction}
              onSort={onSort}
              stopRowClick
              className={HEADER_CLASS}
            />
            <SortableTableHeader
              label="IRN"
              field="irn"
              activeField={sortState?.field}
              direction={sortState?.direction}
              onSort={onSort}
              stopRowClick
              className={HEADER_CLASS}
            />
            <SortableTableHeader
              label="Total Score"
              field="score"
              activeField={sortState?.field}
              direction={sortState?.direction}
              onSort={onSort}
              stopRowClick
              className={HEADER_CLASS}
            />
            {labColumns.map((lab) => (
              <SortableTableHeader
                key={lab.labId}
                label={formatText(lab.labName)}
                field={`labScore:${lab.labId}`}
                activeField={sortState?.field}
                direction={sortState?.direction}
                onSort={onSort}
                stopRowClick
                className={HEADER_CLASS}
              />
            ))}
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr>
              <td colSpan={3 + labColumns.length} className="px-4 py-10 text-center text-sm text-foreground-secondary">
                Loading grade overview...
              </td>
            </tr>
          ) : rows.length === 0 ? (
            <tr>
              <td colSpan={3 + labColumns.length} className="px-4 py-10 text-center text-sm text-foreground-secondary">
                No student data found
              </td>
            </tr>
          ) : (
            rows.map((student) => {
              const isSelected = selectedStudentId != null && student.studentId === selectedStudentId;
              return (
              <tr
                key={student.studentId}
                onClick={() => onStudentSelect?.(student)}
                className={`border-b border-border cursor-pointer transition-colors hover:bg-primary-light/70 dark:hover:bg-primary-light ${
                  isSelected ? 'bg-primary-light ' : ''
                }`}
              >
                <td className="px-4 py-3 text-sm text-foreground">{formatText(student.studentName)}</td>
                <td className="px-4 py-3 text-sm text-foreground">{formatText(student.irn)}</td>
                <td className="px-4 py-3 text-sm font-semibold text-foreground">{formatNumber(student.totalScore)}</td>
                {(student.labScores ?? []).map((score, index) => (
                  <td key={`${student.studentId}-${labColumns[index]?.labId ?? index}`} className="px-4 py-3 text-sm text-foreground">
                    {formatNumber(score)}
                  </td>
                ))}
              </tr>
              );
            })
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
