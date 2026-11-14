import { isSpreadsheetFile, normalizeStudentCode } from './studentImport';

const ID_HEADERS = new Set([
  'studentcode',
  'studentid',
  'studentirn',
  'irn',
  'ird',
  'mssv',
  'id',
]);

const UNPARSEABLE = 'Could not find a student_code (IRN) column in that file.';

function normalizeHeader(value) {
  return String(value ?? '')
    .trim()
    .toLowerCase()
    .replace(/[\s_\-./]/g, '');
}

function findIdColumn(headerRow) {
  if (!Array.isArray(headerRow)) return -1;
  for (let index = 0; index < headerRow.length; index += 1) {
    if (ID_HEADERS.has(normalizeHeader(headerRow[index]))) {
      return index;
    }
  }
  return -1;
}

function guessIdColumn(rows) {
  const sample = rows.find((row) => Array.isArray(row) && row.some((cell) => String(cell).trim()));
  if (!sample) return -1;
  for (let index = 0; index < sample.length; index += 1) {
    const code = normalizeStudentCode(sample[index]);
    if (/^\d{6,12}$/.test(code)) {
      return index;
    }
  }
  return -1;
}

function collectIrns(grid, idIndex, startAt) {
  const seen = new Set();
  const irns = [];
  for (let i = startAt; i < grid.length; i += 1) {
    const line = grid[i];
    if (!Array.isArray(line)) continue;
    const raw = normalizeStudentCode(line[idIndex]);
    if (!raw) continue;
    if (!/^\d{10}$/.test(raw)) {
      throw new Error(`Each student_code must be exactly 10 digits: ${raw}`);
    }
    if (seen.has(raw)) continue;
    seen.add(raw);
    irns.push(raw);
  }
  return irns;
}

/**
 * Extract 10-digit IRNs for offline lecturer roster import.
 * Header-only / empty data → empty array (successful clear).
 * Unparseable structure → throws (caller keeps prior roster).
 */
export function extractOfflineRosterIrns(grid) {
  const lines = (Array.isArray(grid) ? grid : []).filter((row) => Array.isArray(row));
  if (lines.length === 0) {
    throw new Error(UNPARSEABLE);
  }

  const headerId = findIdColumn(lines[0]);
  if (headerId >= 0) {
    return collectIrns(lines, headerId, 1);
  }

  const guessedId = guessIdColumn(lines);
  if (guessedId >= 0) {
    return collectIrns(lines, guessedId, 0);
  }

  throw new Error(UNPARSEABLE);
}

export { isSpreadsheetFile };

export async function parseOfflineRosterFile(file) {
  if (!file || !isSpreadsheetFile(file)) {
    throw new Error(UNPARSEABLE);
  }
  const XLSX = await import('xlsx');
  const data = await file.arrayBuffer();
  const workbook = XLSX.read(data, { type: 'array' });
  const sheetName = workbook.SheetNames[0];
  if (!sheetName) {
    throw new Error(UNPARSEABLE);
  }
  const sheet = workbook.Sheets[sheetName];
  const grid = XLSX.utils.sheet_to_json(sheet, { header: 1, raw: false, defval: '' });
  return extractOfflineRosterIrns(grid);
}
