/**
 * Scans public/brand/ for logo images (any common format) and generates
 * src/theme/brand.assets.generated.js + favicon tags in index.html.
 * Run: npm run theme:sync
 */
import { existsSync, readdirSync, readFileSync, statSync, writeFileSync } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, '..');
const brandDir = path.join(root, 'public/brand');
const outPath = path.join(root, 'src/theme/brand.assets.generated.js');
const indexPath = path.join(root, 'index.html');

const EXT_ORDER = ['png', 'svg', 'webp', 'ico', 'jpg', 'jpeg', 'gif', 'avif'];
const MIME_BY_EXT = {
  png: 'image/png',
  svg: 'image/svg+xml',
  webp: 'image/webp',
  ico: 'image/x-icon',
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  gif: 'image/gif',
  avif: 'image/avif',
};

function publicBrandUrl(filename) {
  return `/brand/${filename.split('/').map(encodeURIComponent).join('/')}`;
}

function isImageFile(name) {
  const ext = path.extname(name).slice(1).toLowerCase();
  return EXT_ORDER.includes(ext);
}

function pickFaviconFile() {
  if (!existsSync(brandDir)) return null;

  const files = readdirSync(brandDir).filter((name) => !name.startsWith('.') && isImageFile(name));
  if (files.length === 0) return null;

  for (const ext of EXT_ORDER) {
    const preferred = `logo.${ext}`;
    if (files.includes(preferred)) return preferred;
  }

  return files.sort(
    (a, b) => statSync(path.join(brandDir, b)).mtimeMs - statSync(path.join(brandDir, a)).mtimeMs,
  )[0];
}

function faviconTags(favicon) {
  if (!favicon) return '';

  const lines = [`    <link rel="icon" type="${favicon.type}" href="${favicon.url}" />`];

  if (favicon.type === 'image/png' || favicon.type === 'image/jpeg' || favicon.type === 'image/webp') {
    lines.push(`    <link rel="apple-touch-icon" href="${favicon.url}" />`);
  }

  return lines.join('\n');
}

function patchIndexHtml(favicon) {
  const start = '    <!-- BRAND_FAVICON_START -->';
  const end = '    <!-- BRAND_FAVICON_END -->';
  const block = `${start}\n${faviconTags(favicon)}\n${end}`;

  let html = readFileSync(indexPath, 'utf8');
  const pattern = /    <!-- BRAND_FAVICON_START -->[\s\S]*?    <!-- BRAND_FAVICON_END -->/;

  if (!pattern.test(html)) {
    throw new Error('index.html is missing BRAND_FAVICON markers');
  }

  html = html.replace(pattern, block);
  writeFileSync(indexPath, html, 'utf8');
}

const filename = pickFaviconFile();
const favicon = filename
  ? {
      filename,
      url: publicBrandUrl(filename),
      type: MIME_BY_EXT[path.extname(filename).slice(1).toLowerCase()],
    }
  : null;

const generated = `/** AUTO-GENERATED — do not edit. Source: public/brand/ | Run: npm run theme:sync */
export const brandAssets = ${JSON.stringify({ favicon }, null, 2)};
`;

writeFileSync(outPath, generated, 'utf8');
patchIndexHtml(favicon);

if (favicon) {
  console.log('Tab favicon:', favicon.url, `(${favicon.type})`);
} else {
  console.log('Tab favicon: none found in public/brand/');
}
console.log('Wrote', path.relative(root, outPath));
