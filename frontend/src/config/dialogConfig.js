import { ShieldCheck, Warning, WarningOctagon } from '@phosphor-icons/react';

/**
 * Standard Dialog & Popup Severity Levels
 * Matches Corporate EdTech 60-30-10 hierarchy & Swiss Modernism 2.0 specifications.
 */
export const DIALOG_LEVELS = {
  SAFETY: 'safety',
  WARNING: 'warning',
  DANGEROUS: 'dangerous',
};

export const DIALOG_CONFIG = {
  [DIALOG_LEVELS.SAFETY]: {
    id: 'safety',
    label: 'Safety',
    icon: ShieldCheck,
    iconColor: 'text-secondary',
    iconBg: 'bg-secondary-light text-secondary-text',
    borderAccent: 'border-l-4 border-l-secondary',
    badgeClass: 'border-secondary/30 bg-secondary-light text-secondary-text',
    confirmButtonClass: 'bg-primary hover:bg-primary-hover text-white shadow-stage-1',
    defaultTitle: 'Notice',
    defaultConfirmText: 'OK',
    defaultCancelText: 'Cancel',
  },
  [DIALOG_LEVELS.WARNING]: {
    id: 'warning',
    label: 'Warning',
    icon: Warning,
    iconColor: 'text-warning',
    iconBg: 'bg-warning-bg text-warning-text',
    borderAccent: 'border-l-4 border-l-warning',
    badgeClass: 'border-warning/30 bg-warning-bg text-warning-text',
    confirmButtonClass: 'bg-warning hover:bg-warning-hover text-white shadow-stage-1',
    defaultTitle: 'Warning',
    defaultConfirmText: 'Proceed',
    defaultCancelText: 'Cancel',
  },
  [DIALOG_LEVELS.DANGEROUS]: {
    id: 'dangerous',
    label: 'Dangerous',
    icon: WarningOctagon,
    iconColor: 'text-error',
    iconBg: 'bg-error-bg text-error-text',
    borderAccent: 'border-l-4 border-l-error',
    badgeClass: 'border-error/30 bg-error-bg text-error-text',
    confirmButtonClass: 'bg-error hover:bg-error-hover text-white shadow-stage-1',
    defaultTitle: 'Confirm Action',
    defaultConfirmText: 'Confirm',
    defaultCancelText: 'Cancel',
  },
};

/**
 * Resolves configuration for a given dialog severity level.
 * Case-insensitive and normalizes aliases.
 *
 * @param {string} [level='safety'] - 'safety' | 'warning' | 'dangerous'
 * @returns {typeof DIALOG_CONFIG[keyof typeof DIALOG_CONFIG]}
 */
export function resolveDialogConfig(level = 'safety') {
  const normalized = String(level || 'safety').trim().toLowerCase();

  if (
    normalized === 'danger' ||
    normalized === 'dangerous' ||
    normalized === 'destructive' ||
    normalized === 'error'
  ) {
    return DIALOG_CONFIG[DIALOG_LEVELS.DANGEROUS];
  }

  if (
    normalized === 'warning' ||
    normalized === 'warn' ||
    normalized === 'caution'
  ) {
    return DIALOG_CONFIG[DIALOG_LEVELS.WARNING];
  }

  return DIALOG_CONFIG[DIALOG_LEVELS.SAFETY];
}
