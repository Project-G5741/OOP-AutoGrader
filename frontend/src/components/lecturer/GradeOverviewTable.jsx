import { useEffect, useRef } from 'react';
import { formatNumber, formatText } from '../../utils/formatters';
import SortableTableHeader from '../ui/SortableTableHeader';
import PlagiarismDangerMark, {
  labHasPlagiarism,
  studentLabHasPlagiarism,
  studentLabOverlapPercent,
  studentLabPlagiarismRole,
} from './PlagiarismDangerMark';

const HEADER_CLASS =
  'sticky top-0 z-[1] bg-surface px-3 py-0 h-10 text-left text-sm font-medium text-foreground-secondary';
const CELL_CLASS = 'px-3 py-0 h-10 text-sm text-foreground align-middle';
const PANEL_CLASS =
  'flex h-[320px] min-h-0 flex-col overflow-hidden rounded-xl border border-border bg-surface shadow-sm transition-colors';

function LabScoreCell({ score, flagged, overlapPercent, plagiarismRole }) {
  const showOverlap = flagged && overlapPercent != null && overlapPercent > 0;
  return (
    <div className="flex h-full w-full items-center justify-center">
      <span className="relative inline-flex min-w-[2.75ch] items-center justify-center tabular-nums">
        <span className="font-medium text-foreground">{formatNumber(score)}</span>
        {flagged ? (
          <span className="absolute left-full top-1/2 ml-1.5 flex -translate-y-1/2 items-center gap-0.5 whitespace-nowrap">
            <PlagiarismDangerMark show role={plagiarismRole} className="ml-0" />
            {showOverlap ? (
              <span className="text-[10px] font-medium text-warning-text dark:text-warning">{Math.round(overlapPercent)}%</span>
            ) : null}
          </span>
        ) : null}
      </span>
    </div>
  );
}

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
  flaggedLabIds,
  flaggedLabsByStudentId,
  overlapByStudentAndLab,
  rolesByStudentAndLab,
}) {
  const labColumns = Array.isArray(labs) ? labs : [];
  const rows = Array.isArray(students) ? students : [];
  const showPagination = pagination && (pagination.totalPages > 1 || pagination.total > pagination.size);
  const identityRef = useRef(null);
  const labsRef = useRef(null);
  const syncLock = useRef(false);

  useEffect(() => {
    const left = identityRef.current;
    const right = labsRef.current;
    if (!left || !right) {
      return undefined;
    }
    const syncFrom = (source, target) => {
      const onScroll = () => {
        if (syncLock.current) {
          return;
        }
        syncLock.current = true;
        target.scrollTop = source.scrollTop;
        syncLock.current = false;
      };
      source.addEventListener('scroll', onScroll, { passive: true });
      return () => source.removeEventListener('scroll', onScroll);
    };
    const cleanLeft = syncFrom(left, right);
    const cleanRight = syncFrom(right, left);
    return () => {
      cleanLeft();
      cleanRight();
    };
  }, [rows.length, loading]);

  const emptyOrLoading = (
    <div className="px-4 py-10 text-center text-sm text-foreground-secondary">
      {loading ? 'Loading grade overview...' : 'No student data found'}
    </div>
  );

  return (
    <div className="flex flex-col gap-3">
      <div className="grid min-h-[280px] grid-cols-[max-content_minmax(0,1fr)] gap-4">
        <section className={PANEL_CLASS} aria-label="Student identity">
          <div
            ref={identityRef}
            className="min-h-0 flex-1 overflow-x-hidden overflow-y-auto [scrollbar-color:var(--surface-tertiary)_var(--surface)]"
          >
            <table className="w-max table-fixed border-collapse">
              <thead>
                <tr className="border-b border-border">
                  <SortableTableHeader
                    label="Student"
                    field="studentName"
                    activeField={sortState?.field}
                    direction={sortState?.direction}
                    onSort={onSort}
                    stopRowClick
                    className={`${HEADER_CLASS} w-[168px]`}
                  />
                  <SortableTableHeader
                    label="IRN"
                    field="irn"
                    activeField={sortState?.field}
                    direction={sortState?.direction}
                    onSort={onSort}
                    stopRowClick
                    className={`${HEADER_CLASS} w-[112px]`}
                  />
                  <SortableTableHeader
                    label="Total Score"
                    field="score"
                    activeField={sortState?.field}
                    direction={sortState?.direction}
                    onSort={onSort}
                    stopRowClick
                    className={`${HEADER_CLASS} w-[100px] text-center`}
                  />
                </tr>
              </thead>
              <tbody>
                {loading || rows.length === 0 ? (
                  <tr>
                    <td colSpan={3}>{emptyOrLoading}</td>
                  </tr>
                ) : (
                  rows.map((student) => {
                    const isSelected = selectedStudentId != null && student.studentId === selectedStudentId;
                    return (
                      <tr
                        key={`id-${student.studentId}`}
                        onClick={() => onStudentSelect?.(student)}
                        className={`border-b border-border cursor-pointer transition-colors hover:bg-primary-light/70 dark:hover:bg-primary-light ${
                          isSelected ? 'bg-primary-light ' : ''
                        }`}
                      >
                        <td className={`${CELL_CLASS} w-[168px]`}>{formatText(student.studentName)}</td>
                        <td className={`${CELL_CLASS} w-[112px]`}>{formatText(student.irn)}</td>
                        <td className={`${CELL_CLASS} w-[100px] text-center font-semibold tabular-nums`}>
                          {formatNumber(student.totalScore)}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className={PANEL_CLASS} aria-label="Lab scores">
          <div
            ref={labsRef}
            className="min-h-0 flex-1 overflow-auto [scrollbar-color:var(--surface-tertiary)_var(--surface)] [&::-webkit-scrollbar-track]:bg-surface"
          >
            <table className="w-max min-w-full table-auto border-collapse">
              <thead>
                <tr className="border-b border-border">
                  {labColumns.map((lab) => (
                    <SortableTableHeader
                      key={lab.labId}
                      label={formatText(lab.labName)}
                      field={`labScore:${lab.labId}`}
                      activeField={sortState?.field}
                      direction={sortState?.direction}
                      onSort={onSort}
                      stopRowClick
                      className={`${HEADER_CLASS} min-w-[140px] text-center`}
                      after={<PlagiarismDangerMark show={labHasPlagiarism(lab.labId, flaggedLabIds)} />}
                    />
                  ))}
                </tr>
              </thead>
              <tbody>
                {loading || rows.length === 0 ? (
                  <tr>
                    <td colSpan={Math.max(labColumns.length, 1)}>{emptyOrLoading}</td>
                  </tr>
                ) : (
                  rows.map((student) => {
                    const isSelected = selectedStudentId != null && student.studentId === selectedStudentId;
                    return (
                      <tr
                        key={`labs-${student.studentId}`}
                        onClick={() => onStudentSelect?.(student)}
                        className={`border-b border-border cursor-pointer transition-colors hover:bg-primary-light/70 dark:hover:bg-primary-light ${
                          isSelected ? 'bg-primary-light ' : ''
                        }`}
                      >
                        {(student.labScores ?? []).map((score, index) => {
                          const labId = labColumns[index]?.labId;
                          const flagged = studentLabHasPlagiarism(
                            student.studentId,
                            labId,
                            flaggedLabsByStudentId,
                          );
                          const overlapPercent = studentLabOverlapPercent(
                            student.studentId,
                            labId,
                            overlapByStudentAndLab,
                          );
                          const plagiarismRole = studentLabPlagiarismRole(
                            student.studentId,
                            labId,
                            rolesByStudentAndLab,
                          );
                          return (
                            <td
                              key={`${student.studentId}-${labId ?? index}`}
                              className={`${CELL_CLASS} min-w-[140px] text-center`}
                            >
                              <LabScoreCell
                                score={score}
                                flagged={flagged}
                                overlapPercent={overlapPercent}
                                plagiarismRole={plagiarismRole}
                              />
                            </td>
                          );
                        })}
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </section>
      </div>

      {showPagination && (
        <div className="flex items-center justify-between rounded-xl border border-border bg-surface px-4 py-3 shadow-sm">
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
