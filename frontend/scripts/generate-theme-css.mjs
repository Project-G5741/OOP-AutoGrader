/**
 * Generates CSS custom properties from src/theme/tokens.js (single source of truth).
 * Run: npm run theme:sync
 */
import { writeFileSync } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { theme } from '../src/theme/tokens.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const outPath = path.join(__dirname, '../src/theme/tokens.generated.css');

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

function toCssBlock(selector, tokens) {
  const lines = [];
  for (const [key, value] of Object.entries(tokens)) {
    lines.push(`  --${key}: ${value};`);
    const rgb = hexToRgbChannels(value);
    if (rgb) {
      lines.push(`  --${key}-rgb: ${rgb};`);
    }
  }
  return `${selector} {\n${lines.join('\n')}\n}`;
}

const header = `/* AUTO-GENERATED — do not edit. Source: src/theme/tokens.js | Run: npm run theme:sync */\n`;

const css = [
  header,
  toCssBlock(':root', theme.light),
  '',
  toCssBlock('.dark', theme.dark),
  '',
].join('\n');

writeFileSync(outPath, css, 'utf8');
console.log('Wrote', path.relative(path.join(__dirname, '..'), outPath));
