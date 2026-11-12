/** @type {import('tailwindcss').Config} */

function cssVar(name) {
  return `rgb(var(--${name}-rgb) / <alpha-value>)`;
}

export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'ui-sans-serif', 'system-ui', '-apple-system', '"Segoe UI"', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'ui-monospace', 'SFMono-Regular', 'Menlo', 'Monaco', 'Consolas', 'monospace'],
      },
      colors: {
        navy: {
          DEFAULT: cssVar('navy'),
          hover: cssVar('navy-hover'),
          light: cssVar('navy-light'),
          text: cssVar('navy-text'),
        },
        gold: {
          DEFAULT: cssVar('gold'),
          hover: cssVar('gold-hover'),
          bg: cssVar('gold-bg'),
          text: cssVar('gold-text'),
        },
        primary: {
          DEFAULT: cssVar('primary'),
          hover: cssVar('primary-hover'),
          active: cssVar('primary-active'),
          light: cssVar('primary-light'),
          text: cssVar('primary-text'),
        },
        secondary: {
          DEFAULT: cssVar('secondary'),
          hover: cssVar('secondary-hover'),
          light: cssVar('secondary-light'),
          text: cssVar('secondary-text'),
        },
        success: {
          DEFAULT: cssVar('success'),
          hover: cssVar('success-hover'),
          bg: cssVar('success-bg'),
          text: cssVar('success-text'),
          panel: cssVar('success-panel'),
          'panel-text': cssVar('success-panel-text'),
        },
        error: {
          DEFAULT: cssVar('error'),
          hover: cssVar('error-hover'),
          bg: cssVar('error-bg'),
          text: cssVar('error-text'),
        },
        warning: {
          DEFAULT: cssVar('warning'),
          hover: cssVar('warning-hover'),
          bg: cssVar('warning-bg'),
          text: cssVar('warning-text'),
        },
        info: {
          DEFAULT: cssVar('info'),
          hover: cssVar('info-hover'),
          bg: cssVar('info-bg'),
          text: cssVar('info-text'),
        },
        surface: {
          DEFAULT: cssVar('surface'),
          secondary: cssVar('surface-secondary'),
          tertiary: cssVar('surface-tertiary'),
        },
        background: cssVar('background'),
        border: {
          DEFAULT: cssVar('border'),
          subtle: cssVar('border-subtle'),
        },
        foreground: {
          DEFAULT: cssVar('text-primary'),
          secondary: cssVar('text-secondary'),
          muted: cssVar('text-muted'),
          disabled: cssVar('text-disabled'),
        },
        accent: {
          purple: cssVar('purple'),
          'purple-light': cssVar('purple-light'),
          'purple-text': cssVar('purple-text'),
        },
        chart: {
          blue: cssVar('chart-blue'),
          teal: cssVar('chart-teal'),
          green: cssVar('chart-green'),
          amber: cssVar('chart-amber'),
          red: cssVar('chart-red'),
          cyan: cssVar('chart-cyan'),
          purple: cssVar('chart-purple'),
          slate: cssVar('chart-slate'),
        },
      },
      keyframes: {
        'toast-in': {
          from: { transform: 'translateX(120%)', opacity: 0 },
          to: { transform: 'translateX(0)', opacity: 1 },
        },
        'toast-out': {
          from: { transform: 'translateX(0)', opacity: 1 },
          to: { transform: 'translateX(120%)', opacity: 0 },
        },
        'panel-in': {
          from: { opacity: 0, transform: 'translateY(6px)' },
          to: { opacity: 1, transform: 'translateY(0)' },
        },
      },
      boxShadow: {
        'stage-1': '0 2px 6px -1px rgba(15, 23, 42, 0.06), 0 1px 3px -1px rgba(15, 23, 42, 0.04)',
        'stage-2': '0 10px 25px -5px rgba(15, 23, 42, 0.08), 0 4px 10px -4px rgba(15, 23, 42, 0.04)',
        'stage-3': '0 22px 44px -12px rgba(15, 23, 42, 0.16), 0 8px 18px -6px rgba(15, 23, 42, 0.06)',
      },
      animation: {
        'toast-in': 'toast-in 0.3s ease-out forwards',
        'toast-out': 'toast-out 0.3s ease-in forwards',
        'panel-in': 'panel-in 0.22s ease-out forwards',
      },
    },
  },
  plugins: [],
}
