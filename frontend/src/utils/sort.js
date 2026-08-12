export function toggleSortState(current, field) {
  if (current.field === field) {
    return { field, direction: current.direction === 'asc' ? 'desc' : 'asc' };
  }
  return { field, direction: 'asc' };
}

const DISPLAY_TIMESTAMP_RE = /^(\d{2})\/(\d{2})\/(\d{4})(?: (\d{2}):(\d{2}))?$/;
const ISO_DATE_PREFIX_RE = /^\d{4}-\d{2}-\d{2}/;

function isDateLikeString(value) {
  return typeof value === 'string' && (ISO_DATE_PREFIX_RE.test(value) || DISPLAY_TIMESTAMP_RE.test(value));
}

export function parseDisplayTimestamp(value) {
  if (value == null) return null;
  if (value instanceof Date) return value.getTime();
  if (typeof value !== 'string') return null;

  const match = value.match(DISPLAY_TIMESTAMP_RE);
  if (match) {
    const [, day, month, year, hour = '0', minute = '0'] = match;
    return new Date(Number(year), Number(month) - 1, Number(day), Number(hour), Number(minute)).getTime();
  }

  const parsed = Date.parse(value);
  return Number.isNaN(parsed) ? null : parsed;
}

function compareValues(left, right) {
  if (left == null && right == null) return 0;
  if (left == null) return 1;
  if (right == null) return -1;

  if (typeof left === 'number' && typeof right === 'number') {
    return left - right;
  }

  if (isDateLikeString(left) && isDateLikeString(right)) {
    const leftTime = parseDisplayTimestamp(left);
    const rightTime = parseDisplayTimestamp(right);
    if (leftTime != null && rightTime != null) {
      return leftTime - rightTime;
    }
  }

  return String(left).localeCompare(String(right), undefined, { sensitivity: 'base', numeric: true });
}

export function sortRows(rows, field, direction, getValue) {
  const accessor = getValue ?? ((row) => row?.[field]);
  const sorted = [...rows].sort((a, b) => compareValues(accessor(a), accessor(b)));
  return direction === 'desc' ? sorted.reverse() : sorted;
}

export function buildServerSortParam(sortState) {
  if (!sortState?.field) return '';
  return `${sortState.field},${sortState.direction ?? 'asc'}`;
}

export function formatGradeOverviewSortParam(sortState) {
  if (!sortState?.field) return 'studentName,asc';
  if (sortState.field.startsWith('labScore:')) {
    const labId = sortState.field.slice('labScore:'.length);
    return `labScore,${labId},${sortState.direction ?? 'asc'}`;
  }
  return buildServerSortParam(sortState);
}
