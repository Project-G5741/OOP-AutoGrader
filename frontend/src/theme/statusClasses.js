/**
 * Semantic status styles for grading UI — maps to docs/design/color-theory-light-dark-theme.md Section 9.
 */

export const STATUS_STYLES = {
  correct: 'bg-success-bg text-success-text',
  incorrect: 'bg-error-bg text-error-text',
  pending: 'bg-warning-bg text-warning-text',
  selected: 'bg-primary-light text-primary-text',
  info: 'bg-info-bg text-info-text',
  completed: 'bg-secondary-light text-secondary-text',
  neutral: 'bg-surface-secondary text-foreground-muted',
};

/** Solid success CTA (e.g. View Submission): green + white in light; readable green surface in dark. */
export const SUCCESS_ACTION_BUTTON =
  'bg-success text-white hover:bg-success-hover dark:bg-success-bg dark:text-success-text dark:hover:bg-success-panel dark:border dark:border-success/40';

export const STATUS_DOT = {
  correct: 'bg-success',
  incorrect: 'bg-error',
  pending: 'bg-warning',
  selected: 'bg-primary',
  info: 'bg-info',
  completed: 'bg-secondary',
  neutral: 'bg-foreground-muted',
};

export function statusClasses(status) {
  return STATUS_STYLES[status] ?? STATUS_STYLES.neutral;
}

const VIETNAM_TIME_ZONE = 'Asia/Ho_Chi_Minh';

function utcCalendarDay(isoDate) {
  const [year, month, day] = isoDate.split('-').map(Number);
  return Date.UTC(year, month - 1, day);
}

/**
 * Inclusive calendar days until a lab deadline in Vietnam time.
 * Deadline 22/10 → 3 on 20/10, 2 on 21/10, 1 on 22/10 (ends 23:59:59 that day), 0 after.
 * @returns {number|null}
 */
export function deadlineDaysLeft(deadlineDate, now = new Date()) {
  if (!deadlineDate) return null;
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(String(deadlineDate));
  if (!match) return null;
  const todayKey = new Intl.DateTimeFormat('en-CA', {
    timeZone: VIETNAM_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(now);
  const diff = Math.round((utcCalendarDay(match[0]) - utcCalendarDay(todayKey)) / 86_400_000);
  return diff < 0 ? 0 : diff + 1;
}

/** Sidebar chip for the last three calendar days. Null when the countdown has not started. */
export function deadlineCountdownBadge(daysLeft) {
  if (daysLeft === 3) return { variant: 'deadline-3', label: '3 days' };
  if (daysLeft === 2) return { variant: 'deadline-2', label: '2 days' };
  if (daysLeft === 1) return { variant: 'deadline-1', label: '1 day' };
  return null;
}

/** Chip styling for student lab deadline urgency (API urgencyState: OK|WARNING|URGENT|EXPIRED|NONE). */
export const LAB_URGENCY_CHIP = {
  OK: 'border-border bg-surface hover:bg-surface-secondary',
  WARNING: 'border-warning bg-warning-bg/40 hover:bg-warning-bg/60',
  URGENT: 'border-error bg-error-bg/40 hover:bg-error-bg/60',
  EXPIRED: 'border-border bg-surface-secondary text-foreground-muted opacity-90',
  NONE: 'border-border bg-surface hover:bg-surface-secondary',
};

export function labUrgencyChipClasses(urgencyState, selected) {
  const base = LAB_URGENCY_CHIP[urgencyState] ?? LAB_URGENCY_CHIP.NONE;
  return selected ? `${base} ring-2 ring-primary ring-offset-1 ring-offset-background` : base;
}

export function formatLabDeadlineMeta(lab, { withUrgencyHint = true, now = new Date() } = {}) {
  if (!lab?.deadlineDate) return 'No deadline';
  const date = String(lab.deadlineDate).slice(0, 10);
  if (!withUrgencyHint) return `due ${date}`;
  const days = deadlineDaysLeft(date, now);
  if (days === 1) return `due ${date} · 1 day left`;
  if (days === 2 || days === 3) return `due ${date} · ${days} days left`;
  return `due ${date}`;
}

/** Border/background for lab compose select from urgencyState. */
export function labUrgencySelectClasses(urgencyState) {
  switch (urgencyState) {
    case 'WARNING':
      return 'border-warning bg-warning-bg/20 focus:border-warning';
    case 'URGENT':
      return 'border-error bg-error-bg/20 focus:border-error';
    case 'EXPIRED':
      return 'border-border bg-surface-secondary text-foreground-muted';
    default:
      return 'border-border bg-surface-secondary focus:border-primary';
  }
}
