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
