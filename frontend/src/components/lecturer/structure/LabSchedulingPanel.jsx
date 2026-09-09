import React, { useMemo } from 'react';
import { CalendarClock, Eye, EyeOff, Loader2 } from 'lucide-react';
import Card from '../../ui/Card';
import DatePicker from '../../ui/DatePicker';
import Switch from '../../ui/Switch';
import { Badge } from '../../ui/badge';
import { Separator } from '../../ui/separator';

function formatDisplayDate(value) {
  if (!value) return null;
  const raw = typeof value === 'string' ? value.slice(0, 10) : value;
  const [year, month, day] = String(raw).split('-').map(Number);
  if (!year || !month || !day) return null;
  const date = new Date(Date.UTC(year, month - 1, day));
  return new Intl.DateTimeFormat('en-GB', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(date);
}

function parseDateOnly(value) {
  if (!value) return null;
  const [year, month, day] = String(value).slice(0, 10).split('-').map(Number);
  if (!year || !month || !day) return null;
  return new Date(year, month - 1, day);
}

function isFutureDate(value) {
  const date = parseDateOnly(value);
  if (!date) return false;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return date > today;
}

function SectionHeader({ icon: Icon, title, description }) {
  return (
    <div className="flex gap-3">
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg border border-border bg-surface-secondary">
        <Icon className="h-5 w-5 text-primary" aria-hidden="true" />
      </div>
      <div className="min-w-0">
        <h3 className="text-sm font-semibold text-foreground">{title}</h3>
        <p className="mt-0.5 text-sm text-foreground-secondary">{description}</p>
      </div>
    </div>
  );
}

function FieldLabel({ htmlFor, title, hint }) {
  return (
    <div className="space-y-1">
      <label htmlFor={htmlFor} className="block text-sm font-medium text-foreground">
        {title}
      </label>
      {hint && <p className="text-sm text-foreground-muted">{hint}</p>}
    </div>
  );
}

export default function LabSchedulingPanel({
  labName,
  compact = false,
  savedStudentVisible,
  savedReleaseDate,
  savedDeadlineDate,
  studentVisible,
  releaseDate,
  deadlineDate,
  studentAccessSaving,
  deadlineSaving,
  onStudentVisibleChange,
  onReleaseDateChange,
  onDeadlineChange,
  onSaveStudentAccess,
  onClearReleaseDate,
  onSaveDeadline,
  onClearDeadline,
}) {
  const accessStatus = useMemo(() => {
    if (!savedStudentVisible) {
      return { label: 'Hidden', variant: 'destructive', detail: 'Students cannot see or submit this lab.' };
    }
    if (isFutureDate(savedReleaseDate)) {
      return {
        label: 'Scheduled',
        variant: 'warning',
        detail: `Goes live on ${formatDisplayDate(savedReleaseDate)} at 00:00 (VN).`,
      };
    }
    return { label: 'Live', variant: 'default', detail: 'Visible on the student dashboard now.' };
  }, [savedStudentVisible, savedReleaseDate]);

  const deadlineStatus = useMemo(() => {
    if (!savedDeadlineDate) {
      return { label: 'Open-ended', variant: 'secondary', detail: 'Submissions always count for lecturer grading.' };
    }
    return {
      label: 'Deadline set',
      variant: 'outline',
      detail: `Counts until ${formatDisplayDate(savedDeadlineDate)} 23:59 (VN).`,
    };
  }, [savedDeadlineDate]);

  const studentAccessDirty = studentVisible !== savedStudentVisible
    || (releaseDate || '') !== (savedReleaseDate ? String(savedReleaseDate).slice(0, 10) : '');
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
              <p className="max-w-2xl text-sm text-foreground-secondary">
                Control when students can discover and submit work, and when submissions stop counting
                toward official grading views.
              </p>
            </div>
            <div className="flex flex-wrap gap-2">
              <Badge variant={accessStatus.variant}>{accessStatus.label}</Badge>
              <Badge variant={deadlineStatus.variant}>{deadlineStatus.label}</Badge>
            </div>
          </div>
        </div>
      )}

      <div className="grid gap-0 lg:grid-cols-2">
        <section className={`space-y-5 px-5 py-5 lg:border-r lg:border-border ${compact ? '' : 'space-y-6 px-6 py-6'}`}>
          <SectionHeader
            icon={savedStudentVisible ? Eye : EyeOff}
            title="Student access"
            description="Stakeholder view: enrolled students see this lab on their dashboard and can upload when access is live."
          />

          <div className="rounded-xl border border-border bg-surface-secondary/40 p-4">
            <div className="flex items-center justify-between gap-4">
              <div className="space-y-1">
                <p className="text-sm font-medium text-foreground">Visible to students</p>
                <p className="text-sm text-foreground-muted">
                  Turn off to hide the lab entirely from the student experience.
                </p>
              </div>
              <Switch
                id="student-visible-toggle"
                checked={studentVisible}
                disabled={studentAccessSaving}
                onChange={onStudentVisibleChange}
                aria-label="Visible to students"
              />
            </div>
          </div>

          <div className="space-y-3">
            <FieldLabel
              htmlFor="release-date"
              title="Release date"
              hint="Optional. Lab appears at 00:00 Vietnam time on this date. Leave empty to open immediately when visible."
            />
            <DatePicker
              id="release-date"
              value={releaseDate}
              disabled={studentAccessSaving || !studentVisible}
              placeholder="Available immediately"
              onChange={onReleaseDateChange}
              className="w-full max-w-sm"
            />
          </div>

          <p className="rounded-lg bg-info-bg px-3 py-2 text-sm text-info-text">
            {accessStatus.detail}
          </p>

          <div className="flex flex-wrap items-center justify-end gap-2 pt-1">
            <button
              type="button"
              className="rounded-lg border border-border px-4 py-2 text-sm font-medium text-foreground-secondary transition-colors hover:bg-surface-secondary disabled:cursor-not-allowed disabled:opacity-50"
              disabled={studentAccessSaving || !releaseDate}
              onClick={onClearReleaseDate}
            >
              Clear release date
            </button>
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

        <section className={`space-y-5 px-5 py-5 ${compact ? '' : 'space-y-6 px-6 py-6'}`}>
          <SectionHeader
            icon={CalendarClock}
            title="Submission deadline"
            description="Business rule: after the deadline, students may still practice, but lecturer rosters and grade views freeze at the cutoff."
          />

          <div className="space-y-3">
            <FieldLabel
              htmlFor="deadline-date"
              title="Deadline date"
              hint="Optional. Submissions count for lecturer grading until 23:59 Vietnam time on this date."
            />
            <DatePicker
              id="deadline-date"
              value={deadlineDate}
              disabled={deadlineSaving}
              placeholder="No deadline"
              onChange={onDeadlineChange}
              className="w-full max-w-sm"
            />
          </div>

          <p className="rounded-lg bg-surface-secondary px-3 py-2 text-sm text-foreground-secondary">
            {deadlineStatus.detail}
          </p>

          <div className="flex flex-wrap items-center justify-end gap-2 pt-1">
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
      </div>

      {!compact && (
        <>
          <Separator />
          <div className="px-6 py-3 text-xs text-foreground-muted">
            Changes apply after you save each section. Times use Vietnam (UTC+7).
          </div>
        </>
      )}
    </Card>
  );
}
