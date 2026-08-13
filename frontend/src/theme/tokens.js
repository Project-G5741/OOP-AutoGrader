/**
 * ═══════════════════════════════════════════════════════════════════════════
 * THEME CONFIG — edit this file ONLY to change app colors (light + dark).
 * Logo and app naming: src/theme/brand.js
 * After editing, run:  npm run theme:sync   (or npm run dev / npm run build)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Reference doc: docs/design/color-theory-light-dark-theme.md
 */

export const theme = {
  light: {
    // Primary (blue)
    primary: '#2563EB',
    'primary-hover': '#1D4ED8',
    'primary-active': '#1E40AF',
    'primary-light': '#DBEAFE',
    'primary-text': '#1E3A8A',

    // Secondary (teal)
    secondary: '#0D9488',
    'secondary-hover': '#0F766E',
    'secondary-light': '#CCFBF1',
    'secondary-text': '#115E59',

    // Semantic
    success: '#16A34A',
    'success-hover': '#15803D',
    'success-bg': '#DCFCE7',
    'success-text': '#166534',
    'success-panel': '#DCFCE7',
    'success-panel-text': '#166534',

    error: '#DC2626',
    'error-hover': '#B91C1C',
    'error-bg': '#FEE2E2',
    'error-text': '#991B1B',

    warning: '#D97706',
    'warning-hover': '#B45309',
    'warning-bg': '#FEF3C7',
    'warning-text': '#92400E',

    info: '#0284C7',
    'info-hover': '#0369A1',
    'info-bg': '#E0F2FE',
    'info-text': '#075985',

    // Surfaces & text
    background: '#F8FAFC',
    surface: '#FFFFFF',
    'surface-secondary': '#F1F5F9',
    'surface-tertiary': '#E2E8F0',
    border: '#CBD5E1',
    'border-subtle': '#E2E8F0',
    'text-primary': '#0F172A',
    'text-secondary': '#475569',
    'text-muted': '#64748B',
    'text-disabled': '#94A3B8',

    // Accent purple (limited use)
    purple: '#8B5CF6',
    'purple-light': '#EDE9FE',
    'purple-text': '#6D28D9',

    // Chart palette
    'chart-blue': '#2563EB',
    'chart-teal': '#0D9488',
    'chart-green': '#16A34A',
    'chart-amber': '#D97706',
    'chart-red': '#DC2626',
    'chart-cyan': '#0891B2',
    'chart-purple': '#8B5CF6',
    'chart-slate': '#64748B',
  },

  dark: {
    // Deeper blue — less neon glare on dark surfaces (buttons, logo, links)
    primary: '#1E40AF',
    'primary-hover': '#1D4ED8',
    'primary-active': '#1E3A8A',
    'primary-light': '#172554',
    'primary-text': '#7BA7D4',

    secondary: '#2DD4BF',
    'secondary-hover': '#5EEAD4',
    'secondary-light': '#134E4A',
    'secondary-text': '#99F6E4',

    success: '#4ADE80',
    'success-hover': '#86EFAC',
    'success-bg': '#14532D',
    'success-text': '#BBF7D0',
    'success-panel': '#1A472A',
    'success-panel-text': '#A8D5BA',

    error: '#F87171',
    'error-hover': '#FCA5A5',
    'error-bg': '#450A0A',
    'error-text': '#FECACA',

    warning: '#FBBF24',
    'warning-hover': '#FCD34D',
    'warning-bg': '#451A03',
    'warning-text': '#FDE68A',

    info: '#38BDF8',
    'info-hover': '#7DD3FC',
    'info-bg': '#0C4A6E',
    'info-text': '#BAE6FD',

    background: '#0F172A',
    surface: '#111827',
    'surface-secondary': '#1E293B',
    'surface-tertiary': '#334155',
    border: '#1E293B',
    'border-subtle': '#172033',
    'text-primary': '#F8FAFC',
    'text-secondary': '#CBD5E1',
    'text-muted': '#94A3B8',
    'text-disabled': '#64748B',

    purple: '#A78BFA',
    'purple-light': '#312E81',
    'purple-text': '#DDD6FE',

    'chart-blue': '#4B8FCC',
    'chart-teal': '#2DD4BF',
    'chart-green': '#4ADE80',
    'chart-amber': '#FBBF24',
    'chart-red': '#F87171',
    'chart-cyan': '#22D3EE',
    'chart-purple': '#A78BFA',
    'chart-slate': '#94A3B8',
  },
};

/** @deprecated Use theme.light — kept for tailwind/tooling imports */
export const light = theme.light;

/** @deprecated Use theme.dark */
export const dark = theme.dark;

export const chartLight = {
  blue: theme.light['chart-blue'],
  teal: theme.light['chart-teal'],
  green: theme.light['chart-green'],
  amber: theme.light['chart-amber'],
  red: theme.light['chart-red'],
  cyan: theme.light['chart-cyan'],
  purple: theme.light['chart-purple'],
  slate: theme.light['chart-slate'],
};

export const chartDark = {
  blue: theme.dark['chart-blue'],
  teal: theme.dark['chart-teal'],
  green: theme.dark['chart-green'],
  amber: theme.dark['chart-amber'],
  red: theme.dark['chart-red'],
  cyan: theme.dark['chart-cyan'],
  purple: theme.dark['chart-purple'],
  slate: theme.dark['chart-slate'],
};

export function tokenKeys() {
  return Object.keys(theme.light);
}
