/** @type {import('tailwindcss').Config} */

function cssVar(name) {
  return `var(--${name})`;
}

export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
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
      animation: {
        'toast-in': 'toast-in 0.3s ease-out forwards',
        'toast-out': 'toast-out 0.3s ease-in forwards',
        'panel-in': 'panel-in 0.22s ease-out forwards',
      },
    },
  },
  plugins: [],
}
