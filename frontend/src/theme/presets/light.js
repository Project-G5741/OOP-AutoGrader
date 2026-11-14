export default {
  id: 'light',
  label: 'Classic Slate',
  shortLabel: 'Slate',
  colorScheme: 'light',
  icon: 'sun',
  preview: ['#F8FAFC', '#1E3A5F', '#2563EB'],
  tokens: {
    // Brand Navy (Corporate EdTech structural anchor — 30% structural tier)
    navy: '#1E3A5F',
    'navy-hover': '#162C47',
    'navy-light': '#E8EEF5',
    'navy-text': '#162C47',
    'on-navy': '#FFFFFF',

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
    'on-primary': '#FFFFFF',

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
    shadow: '#0F172A',

    // Accent purple (strictly <= 10% for AI insights / reports)
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
};
