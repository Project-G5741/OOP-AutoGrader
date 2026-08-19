import { ChevronDown, ChevronUp } from 'lucide-react';

export default function SortableTableHeader({
  label,
  field,
  activeField,
  direction = 'asc',
  sortable = true,
  onSort,
  className = '',
  stopRowClick = false,
  after = null,
  sortName,
}) {
  const sortText = sortName ?? (typeof label === 'string' ? label : field);
  if (!sortable) {
    return (
      <th className={`${className} whitespace-nowrap align-middle`}>
        <span className="inline-flex h-4 items-center leading-4 whitespace-nowrap">
          {label}
          {after}
        </span>
      </th>
    );
  }

  const isActive = activeField === field;
  const ariaSort = isActive ? (direction === 'asc' ? 'ascending' : 'descending') : 'none';
  const ariaLabel = isActive
    ? `Sort by ${sortText}, ${direction === 'asc' ? 'ascending' : 'descending'}`
    : `Sort by ${sortText}`;

  const handleClick = (event) => {
    if (stopRowClick) {
      event.stopPropagation();
    }
    onSort?.(field);
  };

  return (
    <th className={`${className} whitespace-nowrap align-middle`} aria-sort={ariaSort}>
      <button
        type="button"
        onClick={handleClick}
        aria-label={ariaLabel}
        className="inline-flex min-w-max flex-nowrap items-center gap-1 whitespace-nowrap rounded-md px-0.5 py-0.5 text-left font-inherit transition-colors hover:text-foreground"
      >
        <span className="inline-flex h-4 items-center leading-4 whitespace-nowrap">
          {label}
          {after}
        </span>
        <span className="inline-flex shrink-0 flex-col leading-none" aria-hidden="true">
          <ChevronUp
            className={`h-3 w-3 -mb-0.5 ${isActive && direction === 'asc' ? 'text-primary' : 'text-foreground-muted/60'}`}
          />
          <ChevronDown
            className={`h-3 w-3 -mt-0.5 ${isActive && direction === 'desc' ? 'text-primary' : 'text-foreground-muted/60'}`}
          />
        </span>
      </button>
    </th>
  );
}
