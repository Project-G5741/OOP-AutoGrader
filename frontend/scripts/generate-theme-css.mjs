/**
 * Generates CSS custom properties from src/theme/tokens.js + src/theme/presets/*.js.
 * Validates that every registered preset defines the exact same token contract.
 * Run: npm run theme:sync
 */
import { writeFileSync } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { themePresets, getThemePreset } from '../src/theme/tokens.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const outPath = path.join(__dirname, '../src/theme/tokens.generated.css');

function validatePresets(presets) {
  if (!presets.length) {
    throw new Error('themePresets must contain at least one theme.');
  }

  const basePreset = getThemePreset('light');
  const expectedKeys = Object.keys(basePreset.tokens).sort();
  const seenIds = new Set();

  for (const preset of presets) {
    if (!preset.id || typeof preset.id !== 'string') {
      throw new Error('Every theme preset must have a non-empty string id.');
    }
    if (seenIds.has(preset.id)) {
      throw new Error(`Duplicate theme preset id: "${preset.id}"`);
    }
    seenIds.add(preset.id);

    if (preset.colorScheme !== 'light' && preset.colorScheme !== 'dark') {
      throw new Error(`Theme "${preset.id}" must set colorScheme to "light" or "dark".`);
    }

    const keys = Object.keys(preset.tokens || {}).sort();
    const missing = expectedKeys.filter((k) => !keys.includes(k));
    const extra = keys.filter((k) => !expectedKeys.includes(k));
    if (missing.length || extra.length) {
      throw new Error(
        `Theme "${preset.id}" token mismatch. Missing: [${missing.join(', ')}]; Extra: [${extra.join(', ')}]`,
      );
    }
  }
}

function hexToRgbChannels(value) {
  const hex = String(value).trim();
  const match6 = /^#([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})$/.exec(hex);
  if (match6) {
    return `${parseInt(match6[1], 16)} ${parseInt(match6[2], 16)} ${parseInt(match6[3], 16)}`;
  }
  const match3 = /^#([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])$/.exec(hex);
  if (match3) {
    return `${parseInt(match3[1] + match3[1], 16)} ${parseInt(match3[2] + match3[2], 16)} ${parseInt(match3[3] + match3[3], 16)}`;
  }
  return null;
}

function toCssBlock(selector, tokens, colorScheme) {
  const lines = [`  color-scheme: ${colorScheme};`];
  for (const [key, value] of Object.entries(tokens)) {
    lines.push(`  --${key}: ${value};`);
    const rgb = hexToRgbChannels(value);
    if (rgb) {
      lines.push(`  --${key}-rgb: ${rgb};`);
    }
  }
  return `${selector} {\n${lines.join('\n')}\n}`;
}

validatePresets(themePresets);

const lightPreset = getThemePreset('light');
const darkPreset = getThemePreset('dark');

const header = `/* AUTO-GENERATED — do not edit. Source: src/theme/tokens.js & src/theme/presets/*.js | Run: npm run theme:sync */\n`;

const blocks = [
  header,
  toCssBlock(':root', lightPreset.tokens, lightPreset.colorScheme),
  '',
  toCssBlock(':root.dark:not([data-theme])', darkPreset.tokens, darkPreset.colorScheme),
  '',
  ...themePresets.flatMap((preset) => [
    toCssBlock(`:root[data-theme="${preset.id}"]`, preset.tokens, preset.colorScheme),
    '',
  ]),
];

writeFileSync(outPath, blocks.join('\n'), 'utf8');
console.log(
  'Wrote',
  path.relative(path.join(__dirname, '..'), outPath),
  `(${themePresets.length} themes: ${themePresets.map((p) => p.id).join(', ')})`,
);
