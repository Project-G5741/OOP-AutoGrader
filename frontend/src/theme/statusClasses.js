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
