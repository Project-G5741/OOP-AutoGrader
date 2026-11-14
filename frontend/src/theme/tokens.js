/**
 * ═══════════════════════════════════════════════════════════════════════════
 * THEME REGISTRY — add or edit color themes in src/theme/presets/*.js.
 * Logo and app naming: src/theme/brand.js
 * After editing, run:  npm run theme:sync   (or npm run dev / npm run build)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Reference doc: docs/design/color-theory-light-dark-theme.md
 */

import lightPreset from './presets/light.js';
import darkPreset from './presets/dark.js';
import sandPreset from './presets/sand.js';
import evergreenPreset from './presets/evergreen.js';
import graphitePreset from './presets/graphite.js';
import scarletPreset from './presets/scarlet.js';

/** Ordered list of registered theme presets shown in the UI switcher. */
export const themePresets = [
  lightPreset,
  sandPreset,
  evergreenPreset,
  darkPreset,
  graphitePreset,
  scarletPreset,
];

/** Map of themeId -> token object (e.g. theme.light, theme.dark, theme.sand). */
export const theme = Object.fromEntries(
  themePresets.map((preset) => [preset.id, preset.tokens]),
);

/** Map of themeId -> full preset config (metadata + tokens). */
export const themePresetMap = Object.fromEntries(
  themePresets.map((preset) => [preset.id, preset]),
);

/** Lightweight metadata list for UI switchers (id, label, shortLabel, colorScheme, icon, preview). */
export const themeMetaList = themePresets.map(({ tokens, ...meta }) => ({
  ...meta,
  background: tokens.background,
  surface: tokens.surface,
  border: tokens.border,
  navy: tokens.navy,
  primary: tokens.primary,
  orbColor: meta.colorScheme === 'dark' ? tokens['primary-hover'] : tokens.primary,
  textPrimary: tokens['text-primary'],
}));

export function isValidThemeId(id) {
  return Boolean(id && Object.prototype.hasOwnProperty.call(themePresetMap, id));
}

export function getThemePreset(id) {
  return themePresetMap[id] ?? lightPreset;
}

/** @deprecated Use theme.light — kept for tailwind/tooling imports */
export const light = theme.light;

/** @deprecated Use theme.dark */
export const dark = theme.dark;

export function getChartPalette(themeId) {
  const tokens = getThemePreset(themeId).tokens;
  return {
    blue: tokens['chart-blue'],
    teal: tokens['chart-teal'],
    green: tokens['chart-green'],
    amber: tokens['chart-amber'],
    red: tokens['chart-red'],
    cyan: tokens['chart-cyan'],
    purple: tokens['chart-purple'],
    slate: tokens['chart-slate'],
  };
}

export const chartLight = getChartPalette('light');
export const chartDark = getChartPalette('dark');

export function tokenKeys() {
  return Object.keys(theme.light);
}
