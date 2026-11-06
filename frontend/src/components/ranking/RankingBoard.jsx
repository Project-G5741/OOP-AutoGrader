import { useEffect, useMemo, useState } from 'react';
import { formatDateTime, formatNumber, formatText } from '../../utils/formatters';
import { toggleSortState } from '../../utils/sort';
import SortableTableHeader from '../ui/SortableTableHeader';

const AVATAR_TONES = [
  'bg-primary-light text-primary-text',
  'bg-secondary-light text-secondary-text',
  'bg-accent-purple-light text-accent-purple-text',
  'bg-success-bg text-success-text',
  'bg-warning-bg text-warning-text',
  'bg-info-bg text-info-text',
];

const HEADER_CLASS =
  'sticky top-0 z-[1] bg-surface-secondary px-3 py-2 text-left text-xs font-medium tracking-wide text-foreground-muted';

const TOP_LIMIT = 20;
const PAGE_SIZE = 10;

function initials(name) {
  const parts = String(name || '')
    .trim()
    .split(/\s+/)
    .filter(Boolean);
  if (parts.length === 0) return '?';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
}

function avatarTone(key) {
  const text = String(key || '');
  let hash = 0;
  for (let i = 0; i < text.length; i += 1) {
    hash = (hash + text.charCodeAt(i) * (i + 1)) % AVATAR_TONES.length;
  }
  return AVATAR_TONES[hash];
}

function numeric(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

function compareRows(left, right, field, direction) {
  const mul = direction === 'desc' ? -1 : 1;
  if (field === 'name') {
    return mul * String(left.fullName || '').localeCompare(String(right.fullName || ''), undefined, { sensitivity: 'base' });
  }
  if (field === 'irn') {
    return mul * String(left.irn || '').localeCompare(String(right.irn || ''), undefined, { numeric: true, sensitivity: 'base' });
  }
  if (field === 'total') {
    return mul * (numeric(left.total) - numeric(right.total));
  }
  if (field === 'gift') {
    return mul * (numeric(left.giftPoints) - numeric(right.giftPoints));
  }
  return mul * (numeric(left.rank) - numeric(right.rank));
}

function GiftChip({ points }) {
  const n = numeric(points);
  if (n <= 0) {
    return (
      <span className="inline-flex items-center justify-center rounded-full bg-surface-secondary px-2.5 py-1 text-xs font-medium tabular-nums text-foreground-muted">
        —
      </span>
    );
  }
  return (
    <span className="inline-flex items-center justify-center rounded-full bg-success-bg px-2.5 py-1 text-xs font-semibold tabular-nums text-success-text">
      {formatNumber(n)} pts
    </span>
  );
}

function RankingRow({ row, highlight = false }) {
  const name = formatText(row.fullName);
  return (
    <tr className={`border-t border-border-subtle ${highlight ? 'bg-primary-light' : 'bg-surface'}`}>
      <td className="h-11 px-3 align-middle">
        <span className="text-sm font-semibold tabular-nums text-foreground">{row.rank}</span>
      </td>
      <td className="h-11 px-3 align-middle">
        <div className="flex min-w-0 items-center gap-2">
          <span
            className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-semibold ${avatarTone(row.userId || row.irn)}`}
            aria-hidden="true"
          >
            {initials(row.fullName)}
          </span>
          <span className="truncate text-sm font-medium text-foreground">{name}</span>
        </div>
      </td>
      <td className="h-11 px-3 align-middle text-sm tabular-nums text-foreground-secondary">
        {formatText(row.irn)}
      </td>
      <td className="h-11 px-3 align-middle text-sm tabular-nums text-foreground">
        {formatNumber(row.total)}
      </td>
      <td className="h-11 px-3 align-middle">
        <GiftChip points={row.giftPoints} />
      </td>
    </tr>
  );
}

function ColumnHeaders({ sort, onSort }) {
  return (
    <thead>
      <tr>
        <SortableTableHeader
          label="Ranking"
          field="rank"
          activeField={sort.field}
          direction={sort.direction}
          onSort={onSort}
          className={HEADER_CLASS}
        />
        <SortableTableHeader
          label="Fullname"
          field="name"
          activeField={sort.field}
          direction={sort.direction}
          onSort={onSort}
          className={HEADER_CLASS}
        />
        <SortableTableHeader
          label="IRN"
          field="irn"
          activeField={sort.field}
          direction={sort.direction}
          onSort={onSort}
          className={HEADER_CLASS}
        />
        <SortableTableHeader
          label="Total"
          field="total"
          activeField={sort.field}
          direction={sort.direction}
          onSort={onSort}
          className={HEADER_CLASS}
        />
        <SortableTableHeader
          label="Gift"
          field="gift"
          activeField={sort.field}
          direction={sort.direction}
          onSort={onSort}
          className={HEADER_CLASS}
        />
      </tr>
    </thead>
  );
}

function ColumnWidths() {
  return (
    <colgroup>
      <col className="w-[10%]" />
      <col className="w-[30%]" />
      <col className="w-[20%]" />
      <col className="w-[20%]" />
      <col className="w-[20%]" />
    </colgroup>
  );
}

export default function RankingBoard({
  weekStart,
  weekEnd,
  rows,
  loading = false,
  error = null,
  viewerUserId = null,
  emptyMessage = 'No enrolled students in the current quarter.',
}) {
  const list = Array.isArray(rows) ? rows : [];
  const viewer = viewerUserId == null ? '' : String(viewerUserId);
  const [sort, setSort] = useState({ field: 'rank', direction: 'asc' });
  const [page, setPage] = useState(0);

  const topRows = useMemo(() => {
    return [...list]
      .sort((a, b) => numeric(a.rank) - numeric(b.rank))
      .slice(0, TOP_LIMIT);
  }, [list]);

  const sorted = useMemo(() => {
    return [...topRows].sort((a, b) => compareRows(a, b, sort.field, sort.direction));
  }, [topRows, sort]);

  const totalPages = Math.max(Math.ceil(sorted.length / PAGE_SIZE), 1);
  const safePage = Math.min(page, totalPages - 1);
  const pageRows = sorted.slice(safePage * PAGE_SIZE, safePage * PAGE_SIZE + PAGE_SIZE);
  const showPagination = sorted.length > PAGE_SIZE;

  const myRow = useMemo(() => {
    if (!viewer) return null;
    return list.find((row) => String(row.userId) === viewer) ?? null;
  }, [list, viewer]);

  useEffect(() => {
    setPage(0);
  }, [sort]);

  const handleSort = (field) => setSort((current) => toggleSortState(current, field));

  return (
    <div className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-2xl bg-surface">
      <p className="shrink-0 px-3 py-1 text-sm text-foreground-secondary">
        Week: {formatDateTime(weekStart)} – {formatDateTime(weekEnd)} (Vietnam). Live standing; gifts pay at Sunday 23:59:59.
      </p>
      {error ? <p className="shrink-0 px-3 py-1 text-sm text-warning-text">{error}</p> : null}
      <div className="shrink-0 overflow-x-auto">
        {loading ? (
          <div className="flex min-h-[5rem] items-center justify-center">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          </div>
        ) : list.length === 0 ? (
          <p className="px-3 py-4 text-center text-sm text-foreground-muted">{emptyMessage}</p>
        ) : (
          <table className="w-full min-w-[44rem] table-fixed border-collapse">
            <ColumnWidths />
            <ColumnHeaders sort={sort} onSort={handleSort} />
            <tbody>
              {pageRows.map((row) => (
                <RankingRow
                  key={row.userId}
                  row={row}
                  highlight={viewer && String(row.userId) === viewer}
                />
              ))}
            </tbody>
          </table>
        )}
      </div>
      <div className="min-h-0 flex-1" aria-hidden="true" />
      {!loading && sorted.length > 0 && showPagination ? (
        <div className="flex shrink-0 flex-col gap-2 border-t border-border px-3 py-2 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-sm text-foreground-secondary">
            Page {safePage + 1} of {totalPages}
          </p>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={safePage <= 0}
              onClick={() => setPage(safePage - 1)}
              className="rounded-lg border border-border px-3 py-1.5 text-sm disabled:opacity-50"
            >
              Previous
            </button>
            <button
              type="button"
              disabled={safePage >= totalPages - 1}
              onClick={() => setPage(safePage + 1)}
              className="rounded-lg border border-border px-3 py-1.5 text-sm disabled:opacity-50"
            >
              Next
            </button>
          </div>
        </div>
      ) : null}
      {!loading && viewer ? (
        <div className="sticky bottom-0 shrink-0 border-t border-border bg-surface px-3 py-2">
          <p className="mb-1 text-sm font-medium text-foreground">Your ranking</p>
          {myRow ? (
            <div className="overflow-auto rounded-xl border border-border">
              <table className="w-full min-w-[44rem] table-fixed border-collapse">
                <ColumnWidths />
                <tbody>
                  <RankingRow row={myRow} highlight />
                </tbody>
              </table>
            </div>
          ) : (
            <p className="text-sm text-foreground-muted">You are not on this week’s board.</p>
          )}
        </div>
      ) : null}
    </div>
  );
}
