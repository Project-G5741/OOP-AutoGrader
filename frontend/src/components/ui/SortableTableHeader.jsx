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
}) {
  if (!sortable) {
    return (
      <th className={className}>
        {label}
      </th>
    );
  }

  const isActive = activeField === field;
  const ariaSort = isActive ? (direction === 'asc' ? 'ascending' : 'descending') : 'none';
  const ariaLabel = isActive
    ? `Sort by ${label}, ${direction === 'asc' ? 'ascending' : 'descending'}`
    : `Sort by ${label}`;

  const handleClick = (event) => {
    if (stopRowClick) {
      event.stopPropagation();
    }
    onSort?.(field);
  };

  return (
    <th className={className} aria-sort={ariaSort}>
      <button
        type="button"
        onClick={handleClick}
        aria-label={ariaLabel}
        className="inline-flex items-center gap-1.5 rounded-md px-0.5 py-0.5 text-left font-inherit transition-colors hover:text-foreground"
      >
        <span>{label}</span>
        <span className="inline-flex flex-col leading-none" aria-hidden="true">
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
