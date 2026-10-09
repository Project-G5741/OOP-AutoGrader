import { useEffect, useMemo, useRef } from 'react';
import { X } from 'lucide-react';
import ModalOverlay from '../ui/ModalOverlay';
import { formatNumber, formatText } from '../../utils/formatters';

const COPY = {
  students: {
    title: 'Total Students',
    empty: 'No students enrolled in the current quarter.',
  },
  average: {
    title: 'Average Score',
    empty: 'No qualifying scores yet.',
  },
  labs: {
    title: 'Total Labs',
    empty: 'No labs in the current quarter.',
  },
  atRisk: {
    title: 'At-Risk Students',
    empty: 'No students are below 70.',
  },
};

function codeLabel(code) {
  const text = String(code ?? '').trim();
  return text || '—';
}

function byName(a, b) {
  return String(a.studentName ?? '').localeCompare(String(b.studentName ?? ''), undefined, { sensitivity: 'base' });
}

function groupScoreRows(rows) {
  const groups = [];
  const index = new Map();
  for (const row of rows) {
    const key = row.studentId ?? row.studentName;
    let group = index.get(key);
    if (!group) {
      group = {
        studentId: row.studentId,
        studentName: row.studentName,
        studentCode: row.studentCode,
        scores: [],
      };
      index.set(key, group);
      groups.push(group);
    }
    group.scores.push(row);
  }
  return groups;
}

function StudentRow({ student, scoreLabel }) {
  return (
    <li className="border-t border-border py-3 first:border-t-0">
      <p className="font-semibold text-foreground">{formatText(student.studentName)}</p>
      <p className="text-sm text-foreground-secondary">
        {codeLabel(student.studentCode)}
        {scoreLabel ? ` · ${scoreLabel}` : ''}
      </p>
    </li>
  );
}

export default function OverviewDetailDialog({ detailId, overview, onClose }) {
  const dialogRef = useRef(null);
  const closeRef = useRef(null);
  const copy = COPY[detailId];
  const students = overview?.students ?? [];
  const labs = overview?.labs ?? [];
  const scoreRows = overview?.scoreRows ?? [];

  const atRiskStudents = useMemo(
    () => students
      .filter((student) => student.atRisk)
      .sort((a, b) => Number(a.totalScore) - Number(b.totalScore) || byName(a, b)),
    [students],
  );

  const scoreGroups = useMemo(() => groupScoreRows(scoreRows), [scoreRows]);

  useEffect(() => {
    const trigger = document.activeElement;
    closeRef.current?.focus();
    return () => {
      if (trigger instanceof HTMLElement) trigger.focus();
    };
  }, []);

  useEffect(() => {
    const onKeyDown = (event) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        onClose();
        return;
      }
      if (event.key !== 'Tab' || !dialogRef.current) return;
      const focusable = [...dialogRef.current.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])')]
        .filter((node) => !node.disabled);
      if (focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  if (!copy) return null;

  let countLabel = '';
  let body = null;
  if (detailId === 'students') {
    countLabel = `${students.length} students`;
    body = students.length === 0 ? null : (
      <ul>
        {students.map((student) => (
          <StudentRow
            key={student.studentId ?? student.studentName}
            student={student}
            scoreLabel={`Total ${formatNumber(student.totalScore)}`}
          />
        ))}
      </ul>
    );
  } else if (detailId === 'atRisk') {
    countLabel = `${atRiskStudents.length} students`;
    body = atRiskStudents.length === 0 ? null : (
      <ul>
        {atRiskStudents.map((student) => (
          <StudentRow
            key={student.studentId ?? student.studentName}
            student={student}
            scoreLabel={`Total ${formatNumber(student.totalScore)}`}
          />
        ))}
      </ul>
    );
  } else if (detailId === 'labs') {
    countLabel = `${labs.length} labs`;
    body = labs.length === 0 ? null : (
      <ul>
        {labs.map((lab) => (
          <li key={lab.labId ?? lab.labName} className="border-t border-border py-3 first:border-t-0">
            <p className="font-semibold text-foreground">{formatText(lab.labName)}</p>
            <p className="text-sm text-foreground-secondary">
              Average {formatNumber(lab.averageScore)} · {formatNumber(lab.studentsSubmitted)} submitted
            </p>
          </li>
        ))}
      </ul>
    );
  } else if (detailId === 'average') {
    countLabel = `Class average ${formatNumber(overview?.averageScore)}`;
    body = scoreGroups.length === 0 ? null : (
      <ul>
        {scoreGroups.map((group) => (
          <li key={group.studentId ?? group.studentName} className="border-t border-border py-3 first:border-t-0">
            <p className="font-semibold text-foreground">{formatText(group.studentName)}</p>
            <p className="text-sm text-foreground-secondary">{codeLabel(group.studentCode)}</p>
            <ul className="mt-2 space-y-1">
              {group.scores.map((row) => (
                <li
                  key={row.labId ?? row.labName}
                  className="flex items-baseline justify-between gap-4 text-sm"
                >
                  <span className="min-w-0 truncate text-foreground-secondary">{formatText(row.labName)}</span>
                  <span className="shrink-0 font-medium text-foreground">{formatNumber(row.score)}</span>
                </li>
              ))}
            </ul>
          </li>
        ))}
      </ul>
    );
  }

  return (
    <ModalOverlay onBackdropClick={onClose}>
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="overview-detail-title"
        className="flex max-h-[min(80vh,40rem)] w-full max-w-lg flex-col overflow-hidden rounded-2xl border border-border bg-surface p-6 shadow-xl"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <h2 id="overview-detail-title" className="text-lg font-semibold text-foreground">
              {copy.title}
            </h2>
            <p className="mt-1 text-sm font-medium text-foreground">{countLabel}</p>
          </div>
          <button
            ref={closeRef}
            type="button"
            onClick={onClose}
            className="inline-flex min-h-11 min-w-11 shrink-0 items-center justify-center rounded-lg text-foreground-secondary hover:bg-surface-secondary hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            aria-label="Close"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
        <div className="mt-4 min-h-0 flex-1 overflow-y-auto pr-1">
          {body ?? <p className="py-6 text-sm text-foreground-secondary">{copy.empty}</p>}
        </div>
      </div>
    </ModalOverlay>
  );
}
