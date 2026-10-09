import React, { useMemo } from 'react';
import { CalendarClock, Eye, EyeOff, FileInput, Loader2 } from 'lucide-react';
import Card from '../../ui/Card';
import DatePicker from '../../ui/DatePicker';
import Switch from '../../ui/Switch';
import { Badge } from '../../ui/badge';
import { Separator } from '../../ui/separator';
import SolutionImportPanel from './SolutionImportPanel';

function SectionHeader({ icon: Icon, title }) {
  return (
    <div className="flex items-center gap-3">
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg border border-border bg-surface-secondary">
        <Icon className="h-5 w-5 text-primary" aria-hidden="true" />
      </div>
      <div className="min-w-0">
        <h3 className="text-sm font-semibold text-foreground">{title}</h3>
      </div>
    </div>
  );
}

export default function LabSchedulingPanel({
  labName,
  compact = false,
  savedStudentVisible,
  savedDeadlineDate,
  studentVisible,
  deadlineDate,
  studentAccessSaving,
  deadlineSaving,
  onStudentVisibleChange,
  onDeadlineChange,
  onSaveStudentAccess,
  onSaveDeadline,
  onClearDeadline,
}) {
  const accessStatus = useMemo(() => {
    if (!savedStudentVisible) {
      return { label: 'Hidden', variant: 'destructive' };
    }
    return { label: 'Live', variant: 'default' };
  }, [savedStudentVisible]);

  const deadlineStatus = useMemo(() => {
    if (!savedDeadlineDate) {
      return { label: 'Open-ended', variant: 'secondary' };
    }
    return { label: 'Deadline set', variant: 'outline' };
  }, [savedDeadlineDate]);

  const studentAccessDirty = studentVisible !== savedStudentVisible;
  const deadlineDirty = (deadlineDate || '') !== (savedDeadlineDate ? String(savedDeadlineDate).slice(0, 10) : '');

  const shellClass = compact
    ? 'overflow-hidden rounded-xl border border-border bg-surface'
    : '!p-0 overflow-hidden';

  return (
    <Card className={shellClass}>
      {!compact && (
        <div className="border-b border-border px-6 py-5">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div className="space-y-1">
              <p className="text-xs font-semibold uppercase tracking-wider text-foreground-muted">
                Lab scheduling
              </p>
              <h2 className="text-lg font-semibold text-foreground">{labName}</h2>
            </div>
            <div className="flex flex-wrap gap-2">
              <Badge variant={accessStatus.variant}>{accessStatus.label}</Badge>
              <Badge variant={deadlineStatus.variant}>{deadlineStatus.label}</Badge>
            </div>
          </div>
        </div>
      )}

      <div className="grid gap-0 lg:grid-cols-3">
        <section className={`space-y-7 px-5 py-6 lg:border-r lg:border-border ${compact ? '' : 'space-y-8 px-6 py-7'}`}>
          <SectionHeader
            icon={savedStudentVisible ? Eye : EyeOff}
            title="Student access"
          />

          <div className="rounded-xl border border-border bg-surface-secondary/40 p-5">
            <div className="flex items-center justify-between gap-4">
              <p className="text-sm font-medium text-foreground">Visible to students</p>
              <Switch
                id="student-visible-toggle"
                checked={studentVisible}
                disabled={studentAccessSaving}
                onChange={onStudentVisibleChange}
                aria-label="Visible to students"
              />
            </div>
          </div>

          <div className="flex flex-wrap items-center justify-end gap-2 pt-4">
            <button
              type="button"
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50"
              disabled={studentAccessSaving || !studentAccessDirty}
              onClick={onSaveStudentAccess}
            >
              {studentAccessSaving && <Loader2 className="h-4 w-4 animate-spin" />}
              Save access
            </button>
          </div>
        </section>

        <section className={`space-y-7 px-5 py-6 lg:border-r lg:border-border ${compact ? '' : 'space-y-8 px-6 py-7'}`}>
          <SectionHeader
            icon={CalendarClock}
            title="Submission deadline"
          />

          <div className="flex flex-wrap items-center gap-x-3 gap-y-3 py-1">
            <label htmlFor="deadline-date" className="shrink-0 text-sm font-medium text-foreground">
              Deadline date:
            </label>
            <DatePicker
              id="deadline-date"
              value={deadlineDate}
              disabled={deadlineSaving}
              placeholder="No deadline"
              onChange={onDeadlineChange}
              className="min-w-[10rem] max-w-xs flex-1"
            />
          </div>

          <div className="flex flex-wrap items-center justify-end gap-2 pt-4">
            <button
              type="button"
              className="rounded-lg border border-border px-4 py-2 text-sm font-medium text-foreground-secondary transition-colors hover:bg-surface-secondary disabled:cursor-not-allowed disabled:opacity-50"
              disabled={deadlineSaving || !deadlineDate}
              onClick={onClearDeadline}
            >
              Clear deadline
            </button>
            <button
              type="button"
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50"
              disabled={deadlineSaving || !deadlineDirty}
              onClick={onSaveDeadline}
            >
              {deadlineSaving && <Loader2 className="h-4 w-4 animate-spin" />}
              Save deadline
            </button>
          </div>
        </section>

        <section className={`space-y-7 px-5 py-6 ${compact ? '' : 'space-y-8 px-6 py-7'}`}>
          <SectionHeader icon={FileInput} title="Solution import" />
          <SolutionImportPanel disabled={studentAccessSaving || deadlineSaving} />
        </section>
      </div>

      {!compact && <Separator />}
    </Card>
  );
}
