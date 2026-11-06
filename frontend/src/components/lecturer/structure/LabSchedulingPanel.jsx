import React, { useCallback, useMemo, useState } from 'react';
import { CalendarClock, Eye, EyeOff, Loader2 } from 'lucide-react';
import Card from '../../ui/Card';
import DatePicker from '../../ui/DatePicker';
import Switch from '../../ui/Switch';
import { Badge } from '../../ui/badge';
import SolutionImportPanel, {
  schedulingPrimaryBtnClass,
  schedulingSecondaryBtnClass,
} from './SolutionImportPanel';

const colPad = (compact) => (compact ? 'px-4 py-4' : 'px-5 py-5');

function SectionHeader({ icon: Icon, title }) {
  return (
    <div className="flex items-center gap-3">
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg border border-border bg-surface-secondary">
        <Icon className="h-5 w-5 text-primary" aria-hidden="true" />
      </div>
      <h3 className="text-sm font-semibold text-foreground">{title}</h3>
    </div>
  );
}

export default function LabSchedulingPanel({
  labName,
  compact = false,
  savedStudentVisible,
  savedDeadlineDate,
  savedLabPrefix = 'Official',
  studentVisible,
  deadlineDate,
  studentAccessSaving,
  deadlineSaving,
  prefixSaving = false,
  structureLoading = false,
  onStudentVisibleChange,
  onDeadlineChange,
  onSaveScheduling,
  onImportKindChange,
  onSolutionImport,
}) {
  const [importActions, setImportActions] = useState({
    hasSelection: false,
    importing: false,
    clearSelection: () => {},
    handleImport: () => {},
  });

  const handleImportActionsChange = useCallback((next) => {
    setImportActions(next);
  }, []);

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
  const schedulingDirty = studentAccessDirty || deadlineDirty;
  const schedulingSaving = studentAccessSaving || deadlineSaving;

  const shellClass = compact
    ? 'overflow-hidden rounded-xl border border-border bg-surface'
    : '!p-0 overflow-hidden';

  const panelDisabled = schedulingSaving || prefixSaving || structureLoading;
  const importBusy = importActions.importing;
  const pad = colPad(compact);
  const AccessIcon = studentVisible ? Eye : EyeOff;

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

      <div className={`grid gap-4 lg:grid-cols-2 lg:items-stretch ${pad}`}>
        <section className={`flex min-h-0 flex-col gap-5 lg:border-r lg:border-border lg:pr-5 ${compact ? '' : 'lg:pr-6'}`}>
          <div className="shrink-0 space-y-3">
            <SectionHeader icon={AccessIcon} title="Student access" />
            <div className="rounded-xl border border-border bg-surface-secondary/40 p-5">
              <div className="flex items-center justify-between gap-4">
                <p className="text-sm font-medium text-foreground">Visible to students</p>
                <Switch
                  id="student-visible-toggle"
                  checked={studentVisible}
                  disabled={schedulingSaving}
                  onChange={onStudentVisibleChange}
                  aria-label="Visible to students"
                />
              </div>
            </div>
          </div>

          <div className="flex min-h-0 flex-1 flex-col gap-3">
            <SectionHeader icon={CalendarClock} title="Submission deadline" />
            <div className="flex flex-col gap-2">
              <label htmlFor="deadline-date" className="text-sm font-medium text-foreground">
                Deadline date:
              </label>
              <DatePicker
                id="deadline-date"
                value={deadlineDate}
                disabled={schedulingSaving}
                placeholder="No deadline"
                onChange={onDeadlineChange}
                className="w-full min-w-0"
              />
            </div>
          </div>

          <div className="flex shrink-0 justify-end pt-1">
            <button
              type="button"
              className={schedulingPrimaryBtnClass}
              disabled={panelDisabled || !schedulingDirty}
              onClick={onSaveScheduling}
            >
              {schedulingSaving && <Loader2 className="h-4 w-4 animate-spin" />}
              Save
            </button>
          </div>
        </section>

        <section className={`flex min-h-0 flex-col ${compact ? 'lg:pl-0' : 'lg:pl-1'}`}>
          <div className="flex min-h-0 flex-1 flex-col">
            <SolutionImportPanel
              actionBar="external"
              disabled={panelDisabled}
              savedLabPrefix={savedLabPrefix}
              onImportActionsChange={handleImportActionsChange}
              onImportKindChange={onImportKindChange}
              onImport={onSolutionImport}
            />
          </div>
          <div className="flex shrink-0 flex-wrap items-center justify-end gap-2 pt-3">
            <button
              type="button"
              className={schedulingSecondaryBtnClass}
              disabled={panelDisabled || importBusy || !importActions.hasSelection}
              onClick={importActions.clearSelection}
            >
              Clear
            </button>
            <button
              type="button"
              className={schedulingPrimaryBtnClass}
              disabled={panelDisabled || importBusy || !importActions.hasSelection}
              onClick={importActions.handleImport}
            >
              {importBusy && <Loader2 className="h-4 w-4 animate-spin" />}
              Import solution
            </button>
          </div>
        </section>
      </div>
    </Card>
  );
}
