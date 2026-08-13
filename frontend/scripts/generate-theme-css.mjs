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

function toCssBlock(selector, tokens) {
  const lines = Object.entries(tokens).map(([key, value]) => `  --${key}: ${value};`);
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
