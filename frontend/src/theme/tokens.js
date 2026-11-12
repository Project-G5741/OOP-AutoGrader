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
    // Brand Navy (Corporate EdTech structural anchor — 30% structural tier)
    navy: '#1E3A5F',
    'navy-hover': '#162C47',
    'navy-light': '#E8EEF5',
    'navy-text': '#162C47',

    // Academic Gold (Honors, Top-1 ranking, streaks — selective highlight)
    gold: '#D97706',
    'gold-hover': '#B45309',
    'gold-bg': '#FEF3C7',
    'gold-text': '#92400E',

    // Primary (Interactive Cobalt — 10% action CTA)
    primary: '#2563EB',
    'primary-hover': '#1D4ED8',
    'primary-active': '#1E40AF',
    'primary-light': '#DBEAFE',
    'primary-text': '#1E3A8A',

    // Secondary (Supporting Teal — active progress, MMD modeling)
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

    // Surfaces & text (60% clean architectural neutrals)
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

    // Accent purple (strictly ≤ 10% for AI insights / reports)
    purple: '#7C3AED',
    'purple-light': '#EDE9FE',
    'purple-text': '#5B21B6',

    // Chart palette
    'chart-blue': '#2563EB',
    'chart-teal': '#0D9488',
    'chart-green': '#16A34A',
    'chart-amber': '#D97706',
    'chart-red': '#DC2626',
    'chart-cyan': '#0891B2',
    'chart-purple': '#7C3AED',
    'chart-slate': '#64748B',
  },

  dark: {
    // Brand Navy (elevated slate-navy anchor)
    navy: '#23456E',
    'navy-hover': '#2B5385',
    'navy-light': '#162840',
    'navy-text': '#C7D9EE',

    // Academic Gold
    gold: '#F59E0B',
    'gold-hover': '#D97706',
    'gold-bg': '#422006',
    'gold-text': '#FDE68A',

    // Interactive Cobalt (accessible on #111A2E and >= 5.1:1 with white button text)
    primary: '#2563EB',
    'primary-hover': '#3B82F6',
    'primary-active': '#1D4ED8',
    'primary-light': '#172B4D',
    'primary-text': '#93C5FD',

    secondary: '#0D9488',
    'secondary-hover': '#14B8A6',
    'secondary-light': '#113A3A',
    'secondary-text': '#5EEAD4',

    success: '#16A34A',
    'success-hover': '#22C55E',
    'success-bg': '#143E29',
    'success-text': '#86EFAC',
    'success-panel': '#17422B',
    'success-panel-text': '#A7F3D0',

    error: '#EF4444',
    'error-hover': '#DC2626',
    'error-bg': '#451419',
    'error-text': '#FECACA',

    warning: '#D97706',
    'warning-hover': '#F59E0B',
    'warning-bg': '#422408',
    'warning-text': '#FDE68A',

    info: '#0284C7',
    'info-hover': '#38BDF8',
    'info-bg': '#0C3757',
    'info-text': '#BAE6FD',

    // Layered dark surfaces & crisp structural borders (no invisible borders)
    background: '#0B1220',
    surface: '#111A2E',
    'surface-secondary': '#18243C',
    'surface-tertiary': '#22304C',
    border: '#2A3956',
    'border-subtle': '#1E2D47',
    'text-primary': '#F8FAFC',
    'text-secondary': '#CBD5E1',
    'text-muted': '#94A3B8',
    'text-disabled': '#64748B',

    purple: '#8B5CF6',
    'purple-light': '#2E1E59',
    'purple-text': '#DDD6FE',

    'chart-blue': '#60A5FA',
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
