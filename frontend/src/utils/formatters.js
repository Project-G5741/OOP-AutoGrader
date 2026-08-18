export function formatNumber(value, { suffix = '', round = true } = {}) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '--';
  }
  const numeric = Number(value);
  const display = round ? Math.round(numeric) : numeric;
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

export function formatDateTime(value) {
  if (value === null || value === undefined || String(value).trim() === '') {
    return '—';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '—';
  }
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Asia/Ho_Chi_Minh',
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
