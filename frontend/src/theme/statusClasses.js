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

export function formatLabDeadlineMeta(lab) {
  if (!lab?.deadlineDate) return 'No deadline';
  const date = lab.deadlineDate;
  const hint = {
    WARNING: ' · 3 days left',
    URGENT: ' · 1 day left',
    EXPIRED: ' · expired · practice OK',
  }[lab.urgencyState] ?? '';
  return `due ${date}${hint}`;
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
