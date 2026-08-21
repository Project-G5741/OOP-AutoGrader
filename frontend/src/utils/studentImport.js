const ID_HEADERS = new Set([
  'studentid',
  'studentcode',
  'studentirn',
  'irn',
  'ird',
  'mssv',
]);

const EMAIL_HEADERS = new Set([
  'email',
  'mail',
  'e-mail',
  'studentemail',
  'schoolemail',
]);

function normalizeHeader(value) {
  return String(value ?? '')
    .trim()
    .toLowerCase()
    .replace(/[\s_\-./]/g, '');
}

export function normalizeStudentCode(value) {
  let text = String(value ?? '').trim();
  if (!text) return '';
  if (/^\d+\.0+$/.test(text)) {
    return text.slice(0, text.indexOf('.'));
  }
  if (/^\d+(\.\d+)?e[+-]?\d+$/i.test(text)) {
    const parsed = Number(text);
    if (Number.isFinite(parsed)) {
      return String(Math.round(parsed));
    }
  }
  return text;
}

export function normalizeEmail(value) {
  return String(value ?? '').trim();
}

function looksLikeEmail(value) {
  return normalizeEmail(value).includes('@');
}

function looksLikeStudentCode(value) {
  return /^\d{6,12}$/.test(normalizeStudentCode(value));
}

function findColumnIndexes(headerRow) {
  let idIndex = -1;
  let emailIndex = -1;
  headerRow.forEach((cell, index) => {
    const header = normalizeHeader(cell);
    if (idIndex < 0 && ID_HEADERS.has(header)) {
      idIndex = index;
    }
    if (emailIndex < 0 && EMAIL_HEADERS.has(header)) {
      emailIndex = index;
    }
  });
  if (idIndex < 0) {
    headerRow.forEach((cell, index) => {
      const header = normalizeHeader(cell);
      if (header === 'id' || header === 'student') {
        idIndex = index;
      }
    });
  }
  return { idIndex, emailIndex };
}

function guessColumnIndexes(rows) {
  const sample = rows.find((row) => Array.isArray(row) && row.some((cell) => String(cell).trim()));
  if (!sample) {
    return { idIndex: -1, emailIndex: -1 };
  }
  let idIndex = -1;
  let emailIndex = -1;
  sample.forEach((cell, index) => {
    if (emailIndex < 0 && looksLikeEmail(cell)) {
      emailIndex = index;
    } else if (idIndex < 0 && looksLikeStudentCode(cell)) {
      idIndex = index;
    }
  });
  return { idIndex, emailIndex };
}

function collectRows(grid, idIndex, emailIndex, startAt) {
  const seen = new Set();
  const rows = [];
  for (let i = startAt; i < grid.length; i += 1) {
    const line = grid[i];
    if (!Array.isArray(line)) continue;
    const studentCode = normalizeStudentCode(line[idIndex]);
    const email = normalizeEmail(line[emailIndex]);
    if (!studentCode && !email) continue;
    const key = `${studentCode.toLowerCase()}|${email.toLowerCase()}`;
    if (seen.has(key)) continue;
    seen.add(key);
    rows.push({ studentCode, email });
  }
  return rows;
}

export function extractStudentImportRows(grid) {
  const lines = (Array.isArray(grid) ? grid : []).filter((row) => Array.isArray(row));
  if (lines.length === 0) {
    throw new Error('Could not find Student ID (IRN) and Email columns in that file.');
  }

  const headerMatch = findColumnIndexes(lines[0]);
  if (headerMatch.idIndex >= 0 && headerMatch.emailIndex >= 0) {
    return collectRows(lines, headerMatch.idIndex, headerMatch.emailIndex, 1);
  }

  const guessed = guessColumnIndexes(lines);
  if (guessed.idIndex >= 0 && guessed.emailIndex >= 0) {
    return collectRows(lines, guessed.idIndex, guessed.emailIndex, 0);
  }

  throw new Error('Could not find Student ID (IRN) and Email columns in that file.');
}

export function isSpreadsheetFile(file) {
  const name = String(file?.name || '').toLowerCase();
  const type = String(file?.type || '').toLowerCase();
  return (
    name.endsWith('.xlsx')
    || name.endsWith('.xls')
    || name.endsWith('.csv')
    || type.includes('spreadsheet')
    || type.includes('excel')
    || type === 'text/csv'
  );
}

export async function parseStudentImportFile(file) {
  if (!file) {
    throw new Error('Could not find Student ID (IRN) and Email columns in that file.');
  }
  const XLSX = await import('xlsx');
  const data = await file.arrayBuffer();
  const workbook = XLSX.read(data, { type: 'array' });
  const sheetName = workbook.SheetNames[0];
  if (!sheetName) {
    throw new Error('Could not find Student ID (IRN) and Email columns in that file.');
  }
  const sheet = workbook.Sheets[sheetName];
  const grid = XLSX.utils.sheet_to_json(sheet, { header: 1, raw: false, defval: '' });
  const rows = extractStudentImportRows(grid);
  if (rows.length === 0) {
    throw new Error('Could not find Student ID (IRN) and Email columns in that file.');
  }
  return rows.slice(0, 2000);
}
