/**
 * Shared layout chrome for lecturer right-edge drawers.
 * Main grading tables use `bg-surface`; drawers step up to `surface-secondary` in dark
 * with a visible edge and shadow so the panel reads as a separate layer.
 */

export const LECTURER_DRAWER_OVERLAY =
  'fixed inset-0 flex justify-end bg-black/45 backdrop-blur-[1px] dark:bg-black/70';

export const LECTURER_DRAWER_BACKDROP_HIT = 'min-w-0 flex-1 cursor-default';

export const LECTURER_DRAWER_PANEL =
  'flex h-full w-full flex-col border-l-2 border-border bg-surface shadow-2xl dark:border-surface-tertiary dark:bg-surface-secondary dark:shadow-[-12px_0_40px_rgba(0,0,0,0.55)]';

/** Use on horizontal rules inside drawer headers / tab bars. */
export const LECTURER_DRAWER_DIVIDER = 'border-border dark:border-surface-tertiary';
