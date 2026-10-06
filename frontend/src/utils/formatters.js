export function formatNumber(value, { suffix = '', round = true } = {}) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '--';
  }
  const numeric = Number(value);
  const display = round ? Math.floor(numeric) : numeric;
  return suffix ? `${display}${suffix}` : `${display}`;
}

export function formatPercent(value) {
  return formatNumber(value, { suffix: '%' });
}

export function formatText(value) {
  if (value === null || value === undefined) {
    return 'Data not found';
  }
  const text = String(value).trim();
  return text.length > 0 ? text : 'Data not found';
}

export function hasItems(array) {
  return Array.isArray(array) && array.length > 0;
}

const VIETNAM_TZ = 'Asia/Ho_Chi_Minh';
const VIETNAM_UTC_OFFSET = '+07:00';

/** Parse API date/time for display in Vietnam (UTC+7). Naive ISO strings are treated as +07:00. */
function parseInstant(value) {
  const raw = String(value).trim();
  if (!raw) {
    return null;
  }
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) {
    return new Date(`${raw}T00:00:00${VIETNAM_UTC_OFFSET}`);
  }
  const hasExplicitZone = /[Zz]$|[+-]\d{2}:\d{2}$/.test(raw);
  if (/^\d{4}-\d{2}-\d{2}T/.test(raw) && !hasExplicitZone) {
    return new Date(`${raw}${VIETNAM_UTC_OFFSET}`);
  }
  return new Date(raw);
}

export function formatDateTime(value) {
  if (value === null || value === undefined || String(value).trim() === '') {
    return '—';
  }
  const date = parseInstant(value);
  if (!date || Number.isNaN(date.getTime())) {
    return '—';
  }
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: VIETNAM_TZ,
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(date);
  const get = (type) => parts.find((part) => part.type === type)?.value ?? '';
  return `${get('day')}/${get('month')}/${get('year')} ${get('hour')}:${get('minute')}`;
}

/** Preferred MMD tab label for relation types (realization → implementation). */
export function formatMmdRelationType(type) {
  const normalized = String(type ?? '').trim().toLowerCase();
  if (normalized.includes('realiz') || normalized === 'implementation') {
    return 'implementation';
  }
  return String(type ?? '').trim();
}

/** Plain-language operational-test errors (also applied on backend for new grades). */
export function formatStudentTestcaseMessage(value) {
  if (value == null || value === '') return value;
  const text = String(value).trim();

  const ctorMatch = /^[\w$.]*(\w+)\.<init>\(([^)]*)\)$/.exec(text);
  if (ctorMatch) {
    const className = ctorMatch[1];
    const params = formatJavaParamList(ctorMatch[2]);
    if (params === 'no parameters') {
      return `Your ${className} class does not have a no-argument constructor.`;
    }
    return `Your ${className} class does not have a constructor with parameters (${params}).`;
  }

  const methodMatch = /^[\w$.]*(\w+)\.(\w+)\(([^)]*)\)$/.exec(text);
  if (methodMatch && methodMatch[2] !== 'init') {
    const [, className, methodName, rawParams] = methodMatch;
    const params = formatJavaParamList(rawParams);
    if (params === 'no parameters') {
      return `Your ${className} class does not have a method ${methodName}().`;
    }
    return `Your ${className} class does not have a method ${methodName}(${params}).`;
  }

  const unknownInstance = /^Unknown named instance:\s*(.+)$/i.exec(text);
  if (unknownInstance) {
    return `This step uses object "${unknownInstance[1].trim()}", but it was not created in an earlier step.`;
  }

  return text;
}

function formatJavaParamList(raw) {
  const trimmed = String(raw ?? '').trim();
  if (!trimmed) return 'no parameters';
  return trimmed
    .split(',')
    .map((part) => part.trim().replace(/^java\.lang\./, ''))
    .filter(Boolean)
    .join(', ');
}

/** First non-empty line of a class compile diagnostic. Backend already shortens via CompileErrorMessage. */
export function firstCompileErrorLine(error) {
  if (error == null) {
    return null;
  }
  const line = String(error).split(/\r?\n/).find((part) => part.trim().length > 0);
  return line ?? null;
}
